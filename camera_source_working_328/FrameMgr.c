/*
    Copyright (C) 2004    John Orlando
    
   AVRcam: a small real-time image processing engine.

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public
    License as published by the Free Software Foundation; either
    version 2 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public
    License along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

   For more information on the AVRcam, please contact:

   john@jrobot.net

   or go to www.jrobot.net for more details regarding the system.
*/
/*********************************************************
	Module Name: FrameMgr.c
	Module Date: 04/10/2004
	Module Auth: John Orlando
	
	Description: This modules is responsible for performing
	both medium and high level processing on image data.
	This is performed at both the line level as well as
	the frame level.  It controls the main flow of the
	system, adhering to all the critical timing 
	requirements (such as when serial data can be transferred,
	etc).
    
    Revision History:
    Date        Rel Ver.    Notes
    4/10/2004      0.1     Module created
    6/30/2004      1.0     Initial release for Circuit Cellar
                           contest.
    11/15/2004     1.2     Updated processLine() function so 
                           it will remove objects less than
                           a specified length/width (reduces
                           shot noise)
    all mods tagged with jcd and the date
    8/24/09             mods for ov7620 around line 165,169,213,290,397,408,464
    9/15/09             around line 166,170,291,465 changed for 176x480 dump with ov7620
    9/16/09             around line 88,140,142,153,291,467,483,532,588,591,593,596,675,790,795 for 176x480 track with ov7620
    10/15/09            misc changes to allow for int sizes in trackedObject
*********************************************************/

/*	Includes */
#include <stdlib.h>
#include <string.h>
#include <avr/io.h>
#include "Executive.h"
#include "UIMgr.h"
#include "FrameMgr.h"
#include "CamInterface.h"
#include "UartInterface.h"
#include "Utility.h"
#include "I2CInterface.h"
#include "CamConfig.h"
#include "CommonDefs.h"
#include "DebugInterface.h"

/* 	Local Structures and Typedefs */
enum
{
	ST_FrameMgr_idle,
	ST_FrameMgr_TrackingFrame,
	ST_FrameMgr_DumpingFrame
};

typedef unsigned char FrameMgr_State_t;

/*  Definitions */
/* The most objects that can be tracked at any one time is 8.  
This number is determined by the number of bytes that can be
sent out during a frame (one byte per line, 144 lines per frame) 
with the number of bytes in a tracked object (7) + some wiggle
room :-) ... I guess this could be increased to around 20 if
we had enough room and cycles to process objects between lines */
#define MAX_TRACKED_OBJECTS	4

/* This defines the number of ints that make up a trackedObject_t
structure... */ // 9/16/09 jcd larger 'cause of the ints in Y position
// jcd 10/15/09 all switched to ints now, color and valid too
#define SIZE_OF_TRACKED_OBJECT 8 

/* This define is used to turn off the timer overflow interrupt
that is generated when the PCLK overflows TIMER1 */
//#define DISABLE_PCLK_TIMER1_OVERFLOW_BITMASK 0xFB jcd 10/8/09
#define DISABLE_PCLK_TIMER1_OVERFLOW_BITMASK 0xFE

/* This define is used to determine if a run length is too small
to be concerned with.  This helps to reduce the number of false
positives. */
#define MIN_OBJECT_TRACKING_WIDTH 3

/* This define is used to determine if an object has enough
height to be considered worth tracking...it is used to reduce
shot noise */
#define MIN_OBJECT_TRACKING_HEIGHT 3

/* This define is used to indicate how often the filter routine
that removes objects less than MIN_OBJECT_TRACKING_HEIGHT should
be executed.  It is measured in a number of lines (7 nominally). */
#define RUN_OBJECT_FILTER_MASK 0x07

/* This enum describes the possible colors that can
be tracked by the system.  This can't be represented as
simple color names (red, brown, etc) due to the fact that
the user sets which colors will be associated with which
bits.  Remember...after the AND operation of the indexed
color map values executes, either a single bit indicating
the color should be set, or no bits indicating that the
color isn't represented in the color map (notTracked). */
enum
{
	notTracked,
	color1,		/* bit 1 color */
	color2,		/* bit 2 color */
	color3,		/* bit 3 color */
	color4,		/* bit 4 color */
	color5,		/* bit 5 color */
	color6,		/* bit 6 color */
	color7,		/* bit 7 color */
	color8		/* bit 8 color */
};

typedef unsigned int trackedColor_t;    // jcd 10/15/09

/* This structure defines the info that needs to be
maintained for each trackedObject in the trackingTable */
// all changed to ints now, color and valid too jcd 10/15/09
typedef struct
{
	trackedColor_t  color;
	unsigned int lastLineXStart;
	unsigned int lastLineXFinish;
	unsigned int x_upperLeft;
	unsigned int y_upperLeft;  // jcd 9/16/09 want 0..479 for Y position  - 10/15/09 X also
	unsigned int x_lowerRight;
	unsigned int y_lowerRight; //  jcd 9/16/09 want 0 ...479 for Y position
	unsigned int objectValid;  /* used to be a fill byte...now it is
                                     used to determine if the object is valid
                                     or not...it gets invalidated if it is
                                     determined that it is too small, or
                                     that the object is within another object */
} trackedObject_t;

/* These defines are used to index into each individual element in the
trackedObject_t structure.  This seems to be MUCH more efficient than
accessing the elements in GCC. */
// offsets back as before, now unsigned int values - jcd
#define COLOR_OFFSET                0
#define LAST_LINE_X_START_OFFSET    1
#define LAST_LINE_X_FINISH_OFFSET   2
#define X_UPPER_LEFT_OFFSET         3
#define Y_UPPER_LEFT_OFFSET         4
#define X_LOWER_RIGHT_OFFSET        5
#define Y_LOWER_RIGHT_OFFSET        6
#define VALID_OBJECT_OFFSET         7

/*  Local Variables */
/* The trackedObjectTable is used to hold up to eight tracked objects
while they are being acquired. */
static trackedObject_t trackedObjectTable[MAX_TRACKED_OBJECTS];
static trackedObject_t *pCurrentTrackedObjectTable = trackedObjectTable;
static unsigned int lineCount = 0; // jcd 9/16/09
static FrameMgr_State_t currentState = ST_FrameMgr_idle;
static unsigned char numCurrTrackedObjects = 0;
static unsigned char numPrevTrackedObjects = 0;
static unsigned int trackedLineCount = 0; // jcd 9/16/09

/*  Local Functions  */
static void FrameMgr_findConnectedness(void);

/*  Extern Functions */
/* These functions are located in assembly files, and thus
must be externed here so they can be referenced in the source below. */
extern void CamIntAsm_waitForNewTrackingFrame(unsigned char *pBuffer, unsigned char *pMemLookup);
extern void CamIntAsm_waitForNewDumpFrame(unsigned char *pCurrBuffer, unsigned char *pPrevBuffer);
extern void CamIntAsm_acquireTrackingLine(unsigned char *pBuffer, unsigned char *pMemLookup);
extern void CamIntAsm_acquireDumpLine(unsigned char *pCurrBuffer, unsigned char *pPrevBuffer);
extern void CamIntAsm_toggleDebugLine(void);
/***********************************************************
	Function Name: FrameMgr_init
	Function Description: This function is responsible
	for initializing the FrameMgr.  This includes 
	setting up the various buffers and data needed to 
	process each frame of image data.
	Inputs:  none
	Outputs: none
***********************************************************/	
void FrameMgr_init(void)
{
	memset(trackedObjectTable,0x00,sizeof(trackedObjectTable));
    DDRD |= 0x40; // set debug line active
}


/***********************************************************
	Function Name: FrameMgr_dispatchEvent
	Function Description: This function is responsible for
	taking an incoming event and performing the needed
	actions with it as pertains to the FrameMgr.
	Inputs:  event - the generated event
	Outputs: none
***********************************************************/	
void FrameMgr_dispatchEvent(unsigned char event)
{	
	switch(event)
	{
		case EV_DUMP_FRAME:
            CamConfig_setCamReg(0x11,0x03);  /* reduce the frame rate for dumping (ov7620) was 0x 02 jcd 8/24/09 */
			CamConfig_sendFifoCmds();
			Utility_delay(1000);		/* allow the new frame rate to settle */
			lineCount = 0;
			currentState = ST_FrameMgr_DumpingFrame;
            FrameMgr_acquireLine();
			break;
		
		case EV_ENABLE_TRACKING:
			currentState = ST_FrameMgr_TrackingFrame;					
			FrameMgr_acquireFrame();
			break;
			
		case EV_ACQUIRE_FRAME_COMPLETE:
			FrameMgr_processFrame();
			break;
		
		case EV_PROCESS_FRAME_COMPLETE:
			FrameMgr_acquireFrame();
			break;

		case EV_SERIAL_DATA_RECEIVED:
			if (currentState != ST_FrameMgr_idle)
			{
				/* we need to go back to processing line data, since
				serial data reception interrupted us....just trash the
				frame and act like the frame has been processed, which
				will kick off the system to wait for the next line */
				PUBLISH_EVENT(EV_PROCESS_FRAME_COMPLETE);
			}
			break;
			
		case EV_DISABLE_TRACKING:
			/* tracking needs to be turned off */
			currentState = ST_FrameMgr_idle;
			break;
	}
}

/***********************************************************
	Function Name: FrameMgr_acquireFrame
	Function Description: This function is responsible for
	beginning of the acquisition of a new frame of data
	from the camera interface. The acquisition of this line 
	depends on the current state of the FrameMgr.
	Inputs:  none
	Outputs: none
***********************************************************/	
void FrameMgr_acquireFrame(void)
{
	if (currentState == ST_FrameMgr_TrackingFrame)
	{
		trackedLineCount = 0;
		numPrevTrackedObjects = numCurrTrackedObjects;
		numCurrTrackedObjects = 0;
		
		/* clear out the tracking table, and wait for the new frame
		to start */
		memset(trackedObjectTable,0x00,sizeof(trackedObjectTable)); // 10/21/09 jcd 
		//CamIntAsm_waitForNewTrackingFrame(currentLineBuffer,colorMap);
        WAIT_FOR_VSYNC_HIGH();
        CamIntAsm_acquireTrackingLine(currentLineBuffer,colorMap);
	}
}

/***********************************************************
	Function Name: FrameMgr_acquireLine
	Function Description: This function is responsible for
	acquiring a line of data from the camera interface.
	The acquisition of this line depends on the current
	state of the FrameMgr.
	Inputs:  none
	Outputs: none
***********************************************************/	
void FrameMgr_acquireLine(void)
{
	unsigned int tmpLineCount;  // jcd 9/16/09
	
	/* clearing out the buffers takes too long...we should
	just overwrite the data here without a problem when
	we start acquiring...at no point do we check for 
	a 0x00 value in the current or previous lineBuffers,
	so it was a bit excessive :-)  */
	
	/* check which state we are in and proceed as needed */
	if (currentState == ST_FrameMgr_DumpingFrame)
	{
		tmpLineCount = lineCount*2;
     
        /* clearing out the line data in dump mode is ok, and actually
        is needed, since it is possible for the first dump line in
        a frame to come back with the last line captured of the
        last capture session...*/
        memset(currentLineBuffer,0x00,LENGTH_OF_LINE_BUFFER);
        memset(previousLineBuffer,0x00,LENGTH_OF_LINE_BUFFER);
		/* wait for another VSYNC so we know which frame to use 
		to start looking for a line to receive */
		WAIT_FOR_VSYNC_HIGH();  
		WAIT_FOR_VSYNC_LOW();	
		/* look at lineCount to determine how many HREFs we should
		wait before we start sampling */
		while(tmpLineCount != 0)
		{
			WAIT_FOR_HREF_HIGH(); 
			tmpLineCount--;
			WAIT_FOR_HREF_LOW(); 
		}
		/*  we should now be ready to sample our line...*/
		CamIntAsm_acquireDumpLine(currentLineBuffer,previousLineBuffer); 
	}		
	else if (currentState == ST_FrameMgr_TrackingFrame)
	{
		WAIT_FOR_HREF_LOW();
		CamIntAsm_acquireTrackingLine(currentLineBuffer,colorMap); 
	}
}

/***********************************************************
	Function Name: FrameMgr_processLine
	Function Description: This function is responsible for
	parsing the received image line and performing either
	connected region mapping (if in the Tracking state) or
	sending out the raw sampled data (if in the Dumping
	state).
	Inputs:  none
	Outputs: none
***********************************************************/	
void FrameMgr_processLine(void)
{
	unsigned int i; // jcd 10/13/09
	volatile unsigned char dataToSend;
	unsigned int *pTrackedObjectData = (unsigned int*)pCurrentTrackedObjectTable;

	if (currentState == ST_FrameMgr_DumpingFrame)
	{
		/* we want to sit in a tight loop and send the acquired data
		sitting in current and previous line buffers out the serial
		port...it is sent out the serial port immediately instead
		of going into the UIMgr tx fifo because we can't do anything
		until its sent out anyway...may as well just get it out now	*/
		
		/* currentLineBuffer is getting "g" previousLineBuffer is getting "b-r" */
		UartInt_txByte(0x0B);			/* send the header byte */
		UartInt_txByte(lineCount);		/* send the line count */ 
		for (i=0; i<NUM_PIXELS_IN_A_DUMP_LINE; i+=2)
		{
			/* when a dump line is sampled, the upper byte can potentially
			have garbage in it...we don't have time to mask it off as we're
			sampling, so it is done here before we send it out...we also
			combine the samples together so we really are sending up a
			sample for line N as well as line N+1 */
			dataToSend = currentLineBuffer[i];
			dataToSend &= 0x0F;
			dataToSend <<= 4;
			dataToSend |= (previousLineBuffer[i] & 0x0F);
			
			/* dataToSend should be packed now */
			UartInt_txByte(dataToSend);
			
			/* flip the colors around since we are doing all G on Y and BR on UV */
			dataToSend = previousLineBuffer[i+1];
			dataToSend &= 0x0F;
			dataToSend <<= 4;
			dataToSend |= (currentLineBuffer[i+1] & 0x0F);
			
			/* dataToSend should be packed now */
			UartInt_txByte(dataToSend);
		}
		UartInt_txByte(0x0F);  /* send line end */
		/* once all the data is sent, increment out line count by 2 since
		we really get 2 lines worth of pixels on each pass */
		/* Update...increment only by 1, but only send 72 double-lines */
		lineCount++;
		
		/* check to see if we have retrieved all of the needed lines */
		//if (lineCount >= 72)  /* half 144, since we send two lines at a time */
        if (lineCount >= ACTUAL_NUM_LINES_IN_A_FRAME/2)  /* half of 480, still keeps us unsigned byte sized jcd 8/31/09*/
		{
			/* we're done, so send the dump complete?...nope, just change
			states and we should be fine */
			lineCount = 0;
			currentState = ST_FrameMgr_idle;
			
			/* disable the PCLK counting overflow interrupt */
			//TIMSK &= DISABLE_PCLK_TIMER1_OVERFLOW_BITMASK; 
			TIMSK1 &= DISABLE_PCLK_TIMER1_OVERFLOW_BITMASK; // change for m328 9/20/09 jcd

			//CamConfig_setCamReg(0x11,0x00);  /* reset the frame rate to normal ov6620 176x144 */
            CamConfig_setCamReg(0x11,0x01);  /* reset the frame rate to normal (track mode) ov7620  jcd 8/24/09 */
			CamConfig_sendFifoCmds();
		}
		else
		{
			/* we have more lines to acquire in this frame, so keep on truckin...*/
			PUBLISH_FAST_EVENT(FEV_PROCESS_LINE_COMPLETE);
		}
	}
	else if (currentState == ST_FrameMgr_TrackingFrame)
	{
        /* determine if any of the RLE blocks overlap */
		FrameMgr_findConnectedness();
        
        /* we also want to remove any objects that are less than
        a minimum height...we already removed portions of the 
        run-length that are less than MIN_PIXEL_WIDTH in the
        findConnectedness() routine...doing it here instead of 
        a function to speed things up...this may end up slowing down the
        frame rate slightly, and can be removed if this isn't needed */
  
        /* run this routine once every 8 lines */   // 9/16/09 jcd trackedLineCount now an int so mask it   
        if ( (trackedLineCount & RUN_OBJECT_FILTER_MASK & 0xff) == RUN_OBJECT_FILTER_MASK) 
        {
            for (i=0; i<MAX_TRACKED_OBJECTS; i++)
            {
                if ( *(pTrackedObjectData + VALID_OBJECT_OFFSET) == TRUE)
                {
                    /* check to see if the object is already in
                    our past...i.e., its last */
                    if ( (*(pTrackedObjectData + Y_LOWER_RIGHT_OFFSET) - 
                          *(pTrackedObjectData + Y_UPPER_LEFT_OFFSET)) < MIN_OBJECT_TRACKING_HEIGHT)
                    {
                        /* the object is less than the minimum height...see if it is adjacent
                        to the current line we just processed...if so, leave it here...otherwise,
                        it needs to be invalidated since its too small */
                        if ( (trackedLineCount) - *(pTrackedObjectData + Y_LOWER_RIGHT_OFFSET) > 2)
                        {
                            /* invalidate the object */
                            *(pTrackedObjectData + VALID_OBJECT_OFFSET) = FALSE;
                            numCurrTrackedObjects--;
                        }
                    }
                }
                pTrackedObjectData += SIZE_OF_TRACKED_OBJECT;
            }
        }     
 
		trackedLineCount++;
		if (trackedLineCount == ACTUAL_NUM_LINES_IN_A_FRAME)
		{
			/* an entire frame of tracking data has been acquired, so
			publish an event letting the system know this fact */
			PUBLISH_EVENT(EV_ACQUIRE_FRAME_COMPLETE);
			/* disable the PCLK counting overflow interrupt */
			//TIMSK &= DISABLE_PCLK_TIMER1_OVERFLOW_BITMASK;
            TIMSK1 &= DISABLE_PCLK_TIMER1_OVERFLOW_BITMASK; //changed for M328 9/20/09 jcd
			trackedLineCount = 0;
		}
		else
		{
			PUBLISH_FAST_EVENT(FEV_PROCESS_LINE_COMPLETE);
		}	
	}
	else
	{
		/* ...and here? */
	}
}

/***********************************************************
	Function Name: FrameMgr_processFrame
	Function Description: This function is responsible for
	parsing the completed frame and performing all actions
	needed at this level.
	Inputs:  none
	Outputs: none
***********************************************************/	
void FrameMgr_processFrame(void)
{
	unsigned char i,k;
    unsigned int color;
	unsigned int *pTableData = (unsigned int*)pCurrentTrackedObjectTable;// 10/15/09 jcd
	unsigned char tmpUpperLeftXHigh,tmpUpperLeftXLow,tmpLowerRightXHigh,tmpLowerRightXLow;
    unsigned char tmpUpperLeftYHigh,tmpUpperLeftYLow,tmpLowerRightYHigh,tmpLowerRightYLow;// 10/15/09 jcd 
	
	/* we only send tracking packets if there are tracked objects */	        
    
	if (numCurrTrackedObjects > 0)
	{		
		UIMgr_writeTxFifo(0x0A);					/* header byte for a tracking packet */
        /* reset the pointer */
        pTableData = (unsigned int*)pCurrentTrackedObjectTable;
        
		UIMgr_writeTxFifo(numCurrTrackedObjects);	/* num of objects tracked */
		for (i=0; i<MAX_TRACKED_OBJECTS; i++)
		{
            /* we only want to process objects that have their objectValid flag
            set to TRUE */
            if ( *(pTableData + VALID_OBJECT_OFFSET) == TRUE)
            {
                /* the object is valid...convert the color from bit position to value...remember, 
                each bit in the "color" byte corresponds to a color */
                k=0;
                color = *(pTableData + COLOR_OFFSET);
                if (color == 128) k=0;
                else if (color == 64) k=1;
                else if (color == 32) k=2;
                else if (color == 16) k=3;
                else if (color == 8)  k=4;
                else if (color == 4)  k=5;
                else if (color == 2)  k=6;
                else if (color == 1)  k=7;
                
                //jcd 10/15/09 send the int x,y values as bytes
                // 
                tmpUpperLeftXHigh = (unsigned char) (*(pTableData + X_UPPER_LEFT_OFFSET)/256);	    // get the upper left X high byte
                tmpUpperLeftXLow = (unsigned char) (*(pTableData + X_UPPER_LEFT_OFFSET) & 0x00FF);	// get the upper left X low byte

                tmpUpperLeftYHigh = (unsigned char)(*(pTableData + Y_UPPER_LEFT_OFFSET)/256);		// get the upper left Y high byte		
                tmpUpperLeftYLow = (unsigned char)(*(pTableData + Y_UPPER_LEFT_OFFSET) & 0x00FF);		// get the upper left Y low byte 	
	
                tmpLowerRightXHigh = (unsigned char)(*(pTableData + X_LOWER_RIGHT_OFFSET)/256);		// get the lower right X high byte
                tmpLowerRightXLow = (unsigned char)(*(pTableData + X_LOWER_RIGHT_OFFSET) & 0x00FF);	// get the lower right X low byte

                tmpLowerRightYHigh = (unsigned char)(*(pTableData + Y_LOWER_RIGHT_OFFSET)/256);		// get the lower right Y high byte     
                tmpLowerRightYLow = (unsigned char)(*(pTableData + Y_LOWER_RIGHT_OFFSET) & 0x00FF);	// get the lower right Y low byte 	 
 
if (tmpUpperLeftXLow == 0xFF) tmpUpperLeftXLow = 0xFE;    // reserve 0xFF for the terminator byte
if (tmpUpperLeftYLow == 0xFF) tmpUpperLeftYLow = 0xFE;   // this won't change the x/y values by much 
if (tmpLowerRightXLow == 0xFF) tmpLowerRightXLow = 0xFE;    // and will make the communications more robust
if (tmpLowerRightYLow == 0xFF) tmpLowerRightYLow = 0xFE;   

                // jcd 10/15/09 need 2 bytes of position now -
                UIMgr_writeTxFifo(k);				  	/* send the color first */
                UIMgr_writeTxFifo(tmpUpperLeftXHigh);
                UIMgr_writeTxFifo(tmpUpperLeftXLow);
                UIMgr_writeTxFifo(tmpUpperLeftYHigh);       
                UIMgr_writeTxFifo(tmpUpperLeftYLow);    // msByte, then lsByte
                UIMgr_writeTxFifo(tmpLowerRightXHigh);
                UIMgr_writeTxFifo(tmpLowerRightXLow);
                UIMgr_writeTxFifo(tmpLowerRightYHigh);    
                UIMgr_writeTxFifo(tmpLowerRightYLow); 			
            }

            /* move our pointer up to the beginning of the next object */
            pTableData += SIZE_OF_TRACKED_OBJECT;
        }
		
		/* all done...send the end of tracking packets char - */
		UIMgr_writeTxFifo(0xFF);
	}		

    /* the tracked object table will be cleared out right before we start
    to wait for VSYNC to indicate a new frame...so it doesn't need to be
    done now */
    
	/* schedule the next action to acquire a new frame */	
	PUBLISH_EVENT(EV_PROCESS_FRAME_COMPLETE);
}

/***********************************************************
	Function Name: FrameMgr_findConnectedness
	Function Description: This function is responsible for
	finding the connectedness between two particular run-
	length encoded lines of pixel data.  It updates the
	trackingTable as needed.
	Inputs:  none
	Outputs: none
***********************************************************/	
static void FrameMgr_findConnectedness(void)
{
	trackedColor_t currColor;
	unsigned char *pCurrLineColorInfo = currentLineBuffer;
    // jcd 10/15/09 all to ints
	unsigned int *pTrackedObjectData;
	unsigned int currPixelRunStart=0;
	unsigned int currPixelRunFinish=0;     unsigned int lastLineXStart=0;
	unsigned int lastLineXFinish=0;  
	unsigned int runLength=1;
	unsigned char i;
	bool_t colorConnected;	
	
	do
	{
		/* grab both the current color and the number of pixels
		in the run...remember, pixels start at 1, not 0! */
		colorConnected = FALSE;
		currColor = (unsigned int)*pCurrLineColorInfo++;
        // version for old acquirePixelBlock in CamInterfaceAsm 10/26/09 jcd 
        // 
		//currPixelRunStart += runLength;
		//runLength = (unsigned int)*pCurrLineColorInfo++;
		//currPixelRunFinish += runLength;

        // new, non working code  - 
        currPixelRunStart = currPixelRunFinish;
        currPixelRunFinish = ((unsigned int)*pCurrLineColorInfo++);
        currPixelRunFinish += ((unsigned int)*pCurrLineColorInfo++)*256; 
        currPixelRunFinish -= 0xffff - ACTUAL_NUM_PIXELS_IN_A_LINE	; // minus TCNT1 counter start value 

        runLength = currPixelRunFinish - currPixelRunStart;

        /* make sure that the run-length is at least as wide as
        the minimum horizontal tracking width, and we care about the color */ 
        
		if ( (currColor != notTracked) && (runLength > MIN_OBJECT_TRACKING_WIDTH) )
		{			
            /* this run contains a color we care about, so 
			either it will begin a new tracked object, or it
			is connected to a currently tracked object...
			compare it with each object in the tracking
			table...we can't just look at the numTrackedObjects because
            it is entirely possible that the first couple of objects could
            be invalid...

            NOTE: Instead of accessing each element in the trackedObjectTable
            through the 'i' index, and then accessing the fields in each structure,
            a pointer to each entry is established each time through the loop, followed
            by accessing the elements through specified offsets.  GCC seems to be
            able to optimize this code much better than simply accessing the elements
            of each structure in the array the more normal way...*/
            
            pTrackedObjectData = (unsigned int*)pCurrentTrackedObjectTable;
			for (i=0; i<MAX_TRACKED_OBJECTS; i++)
			{ 
				if ( (currColor == *(pTrackedObjectData + COLOR_OFFSET)) && 
                     (*(pTrackedObjectData + VALID_OBJECT_OFFSET) == TRUE) &&
                     (*(pTrackedObjectData + Y_LOWER_RIGHT_OFFSET) == ((trackedLineCount) - 1)) )
				{
					/* found a color match and the object is valid...check to see if there is
					connectedness */
					lastLineXStart = *(pTrackedObjectData + LAST_LINE_X_START_OFFSET);
					lastLineXFinish = *(pTrackedObjectData + LAST_LINE_X_FINISH_OFFSET);
					
					/* Check for the 5 following types of line connectedness:
					---------------------
					|                   |
					---------------------
					         -------------------------
							 |                       |
							 -------------------------  */
					if ( (	(currPixelRunStart >= lastLineXStart) &&
							(currPixelRunStart <= lastLineXFinish) )  ||
							
					/*               ---------------------
					                 |                   |
									 ---------------------
						-------------------
						|                 |
						-------------------  
						                   OR
						     ------------------------------
							 |                            |
							 ------------------------------
							              ---------
										  |       |
										  ---------  */
						 (	(currPixelRunFinish >= lastLineXStart) && 
							(currPixelRunFinish <= lastLineXFinish) ) ||
							
							
					/*     -------------------------------
					       |                             |
						   -------------------------------
						   -------------------------------
						   |                             |
						   -------------------------------
						                  OR
								     -------------
									 |           |
									 -------------
							-------------------------------
							|                             |
							-------------------------------   */
						 (  (currPixelRunStart <= lastLineXStart) &&
							(currPixelRunFinish >= lastLineXFinish) ) )
					{
						/* THERE IS CONNECTEDNESS...update the lastLineXStart and lastLineXFinish
						data pointed to by pTrackedObjectData */
						*(pTrackedObjectData + LAST_LINE_X_START_OFFSET) = currPixelRunStart;
						*(pTrackedObjectData + LAST_LINE_X_FINISH_OFFSET) = currPixelRunFinish;
						
						/* check if the bounding box needs to be updated */
						if (*(pTrackedObjectData + X_UPPER_LEFT_OFFSET) > currPixelRunStart)
						{
							/* need to update the bounding box for the upper left point to 
							enclose this new left-most point...we never have to update the
							upper left Y point, since each scan line we process moves from
							top to bottom */
							*(pTrackedObjectData + X_UPPER_LEFT_OFFSET) = currPixelRunStart;
						}

						if ( *(pTrackedObjectData + X_LOWER_RIGHT_OFFSET) < currPixelRunFinish)
						{
							/* need to update the bounding box for the lower right X point to
							enclose this new right-most point */
							*(pTrackedObjectData + X_LOWER_RIGHT_OFFSET) = currPixelRunFinish;
						}
						
						/* the lower right 'y' point always gets updated when connectedness is found */
						*(pTrackedObjectData + Y_LOWER_RIGHT_OFFSET) = trackedLineCount;
						
						/* set a flag indicating that that color run is part of another
						object and thus doesn't need to be added as a new entry into the
						tracking table */
						colorConnected = TRUE;
						break;
					}
				}
                
                /* go to the next object */
                pTrackedObjectData += SIZE_OF_TRACKED_OBJECT;
			}
			
			if (colorConnected == FALSE)
			{
				/* a new entry needs to be made to the tracking table, since we have
				a run-length with a color, and it isn't connected to anything...but we
				can only do this if there is space left in the trackedObject table */
				if (numCurrTrackedObjects < MAX_TRACKED_OBJECTS)
				{                
                    /* space is available...add the object...but first we need to find an
                    invalid object in the object tracking table */
                    pTrackedObjectData = (unsigned int*)pCurrentTrackedObjectTable; // jcd 10/15/09
                    for (i=0; i<MAX_TRACKED_OBJECTS; i++)
                    {
                        if ( *(pTrackedObjectData + VALID_OBJECT_OFFSET) == FALSE)  break;
                        
                        /* if we haven't broken above, then the object must have been valid...
                        go ahead and move the pointer to the next object to check it */
                        pTrackedObjectData += SIZE_OF_TRACKED_OBJECT;
                    }
                    
					
					/* now that we have a pointer to the tracked object to be updated, update all
					the fields */ // jcd 10/15/09 integer x and y
					*(pTrackedObjectData + COLOR_OFFSET)                = currColor;			/* color */
					*(pTrackedObjectData + LAST_LINE_X_START_OFFSET)    = currPixelRunStart; 	/* lastLineXStart */
					*(pTrackedObjectData + LAST_LINE_X_FINISH_OFFSET)   = currPixelRunFinish;	/* lastLineXFinish */
					*(pTrackedObjectData + X_UPPER_LEFT_OFFSET)         = currPixelRunStart;	/* x_upperLeft */
					*(pTrackedObjectData + Y_UPPER_LEFT_OFFSET)         = trackedLineCount ;	/* y_upperLeft */
					*(pTrackedObjectData + X_LOWER_RIGHT_OFFSET)        = currPixelRunFinish;	/* x_lowerRight */
					*(pTrackedObjectData + Y_LOWER_RIGHT_OFFSET)        = trackedLineCount;	    /* y_lowerRight  */
                    *(pTrackedObjectData + VALID_OBJECT_OFFSET)         = TRUE;                     /* objectValid flag */
						
					numCurrTrackedObjects++;
				}
			}
            
            /* move the pointer to the beginning of the next tracked object */
            pTrackedObjectData += SIZE_OF_TRACKED_OBJECT;
		}
	} while(currPixelRunFinish < ACTUAL_NUM_PIXELS_IN_A_LINE);
}

