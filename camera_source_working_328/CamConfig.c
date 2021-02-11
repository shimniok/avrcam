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
/**********************************************************
	Module Name: CamConfig.c
	Module Date: 04/10/2004
    Module Auth: John Orlando 
	
	Description: This module is responsible for the 
	high-level configuration activities of the OV6620
	camera module.  This module interfaces with the
	I2CInterface module to perform this configuration.
    
    Revision History:
    Date        Rel Ver.    Notes
    4/10/2004      0.1     Module created
    6/30/2004      1.0     Initial release for Circuit Cellar
                           contest.
    11/15/2004     1.2     Added code to un-tri-state the
                           OV6620's pixel data busses at
                           startup after four seconds.  
                           This was added in to 
                           allow the user to re-program the
                           mega8 at startup if needed.
    8/24/09                mods for 7620 in CamConfig_init by jcd 
***********************************************************/

/*  Includes */
#include <avr/io.h>
#include "CamConfig.h"
#include "I2CInterface.h"
#include "CommonDefs.h"
#include "Utility.h"
#include <avr/pgmspace.h>
#include "DebugInterface.h"

/**********************************************************/
/*  Definitions */
/* The length of an I2C command is made up of a register address
plus the actual value of the register */
#define SIZE_OF_I2C_CMD 2
#define MAX_NUM_CONFIG_CMDS 8
#define CAM_CONFIG_TX_FIFO_SIZE MAX_NUM_CONFIG_CMDS 
#define CAM_CONFIG_TX_FIFO_MASK CAM_CONFIG_TX_FIFO_SIZE-1

/*  Local Variables */

/*  Local Structures and Typedefs */

/*  Local Function Prototypes */
static i2cCmd_t CamConfig_readTxFifo(void);

/*  Extern Variables */
i2cCmd_t 		CamConfig_txFifo[CAM_CONFIG_TX_FIFO_SIZE];
unsigned char CamConfig_txFifoHead=0;
unsigned char CamConfig_txFifoTail=0;

// reg settings for 7620 from alpha.dyndns.org
static unsigned char initial7620regvals[63][2] PROGMEM = {
	{ 0x12, 0x24 }, // agc, YCrCb data,awb initially
	{ 0x11, 0x01 }, // reduce clk by 2
    { 0x14, 0x84 }, // full frame size, gamma on defined by reg 62
	{ 0x28, 0x24 }, // progressive scan, Y = ggg, UV = brbr
    { 0x71, 0x40 }, // gate pclk with href
	{ 0x00, 0x00 }, // default agc gain
	{ 0x01, 0x80 }, // default blue gain
	{ 0x02, 0x80 }, // default red gain
	{ 0x03, 0xc0 }, // saturation
    // reg 4,5 reserved
	{ 0x06, 0x60 }, // brightness

	{ 0x07, 0x00 }, // analog sharpness
    // reg 8..c reserved
	{ 0x0c, 0x24 }, // white balance blue chan
	{ 0x0d, 0x24 }, // white balance red chan
    // reg e,f reserved
// 0x10 used for manual auto exposure
    // reg 11,12 at top
	{ 0x13, 0x01 }, // default, enable auto adjust
	// reg 14 at top
	{ 0x15, 0x01 }, // default, polarity, data order
	{ 0x16, 0x03 }, // href asserted every frame
	{ 0x17, 0x2f }, // horiz window start
	{ 0x18, 0xcf }, // horiz window end
	{ 0x19, 0x06 }, // vert window start
	{ 0x1a, 0xf5 }, // vert window end

	{ 0x1b, 0x00 }, // no pixel shift
    // 1c,1d mfg data
    // 1e, 1f reserved
	{ 0x20, 0x18 }, // aperture correction, awb smart mode enabled no windowing yet
	{ 0x21, 0x80 }, // default y chan offset
	{ 0x22, 0x80 }, // default u chan offset
	{ 0x23, 0x00 }, // default crystal current
    // 24,25 auto exposure ratios - use defaults?
	{ 0x26, 0xa2 }, // default digital sharpness
	{ 0x27, 0xea }, // common cont g
    // reg 28 at top
	{ 0x29, 0x00 }, // default exposure control
	{ 0x2a, 0x10 }, // frame rate adjust uv component delay - change me? <<<<<<<<<<<<<<<<
	{ 0x2b, 0x00 }, // default frame rate adjust

	{ 0x2c, 0x88 }, // default expanding reg
	{ 0x2d, 0x91 }, // auto bright
	{ 0x2e, 0x80 }, // default V chan offset
	{ 0x2f, 0x44 }, // reserved, unknown
	{ 0x60, 0x27 }, // default, no grn averaging
	{ 0x61, 0x02 }, // default brightness target
	{ 0x62, 0x5f }, // gama control
	{ 0x63, 0xd5 }, // reserved, unknown
	{ 0x64, 0x57 }, // gamma curve selection
	{ 0x65, 0x83 }, // a/d mode, reference

	{ 0x66, 0x55 }, // default awb
	{ 0x67, 0x92 }, // color space
	{ 0x68, 0xcf }, // brightness target
	{ 0x69, 0x76 }, // analog sharpness
	{ 0x6a, 0x22 }, // edge enhancement
	{ 0x6b, 0x00 }, // reserved, unknown
	{ 0x6c, 0x02 }, // reserved, unknown
	{ 0x6d, 0x44 }, // reserved, unknown
	{ 0x6e, 0x80 }, // reserved, unknown
	{ 0x6f, 0x1d }, // noise compensation

	{ 0x70, 0x8b }, // saturation, awb
	{ 0x72, 0x14 }, // default horiz synch edge
	{ 0x73, 0x54 }, //  "      "      "     "
	{ 0x74, 0x00 }, // agc gain
	{ 0x75, 0x8e }, // auto brightness range
	{ 0x76, 0x00 }, // default common cont O
	{ 0x77, 0xff }, // reserved unknown
	{ 0x78, 0x80 }, // reserved unknown
	{ 0x79, 0x80 }, // reserved unknown
	{ 0x7a, 0x80 }, // reserved unknown

	{ 0x7b, 0xe2 }, // reserved unknown
	{ 0x7c, 0x00 }, // default field average
	{ 0xff, 0xff }	// END MARKER 
};



/***********************************************************
	Function Name: CamConfig_init
	Function Description: This function is responsible for
	performing the initial configuration of the camera.
	Inputs:  none
	Outputs: none
***********************************************************/	
void CamConfig_init(void)
{


// true and false don't seem to be defined, 
unsigned char index =  0;
unsigned char done = 0;
int i;
unsigned char reg;
unsigned char regValue;
// mainly for debug, should be read from the array when that's working
/*
	CamConfig_setCamReg(0x12,0x24);  // agc, YCrCb data,awb initially  
	CamConfig_setCamReg(0x11, 0x01);    // reduce clk by 2
	CamConfig_setCamReg(0x14, 0x84) ; // full frame size, gamma on defined by reg 62
	CamConfig_setCamReg(0x28, 0x24); // progressive scan, Y = ggg, UV = brbr
    CamConfig_setCamReg(0x71, 0x40) ; // gate pclk with href
//CamConfig_sendFifoCmds();
*/

// set the regs from the array above
// the 5 vs 8 register problem below was probably 'cause the memory was so close to
// full the fifo was getting clobbered...might as well leave it at 5

while (done == 0)
    {
    for (i=0;i<5;i++) // this doesn't seem to work with 8 regs, 5 seems ok
        {
        reg = pgm_read_byte(&initial7620regvals[index][0]);
        regValue = pgm_read_byte(&initial7620regvals[index][1]);
        
        if (reg == 0xff)
            {
            done = 1;
            }
        else
            {
            CamConfig_setCamReg(reg,regValue); 
            index++;
            }
        } // end for
    CamConfig_sendFifoCmds();
    } // end !done
} 


/***********************************************************
	Function Name: CamConfig_setCamReg
	Function Description: This function is responsible for
	creating an I2C cmd structure and placing it into the
	cmd fifo.
	Inputs:  reg - the register to modify
	         val - the new value of the register
	Outputs: none
***********************************************************/	
void CamConfig_setCamReg(unsigned char reg, unsigned char val)
{
	i2cCmd_t cmd;
	
	cmd.configReg = reg;
	cmd.data = val;
#ifndef SIMULATION	
	CamConfig_writeTxFifo(cmd);
#endif	
}
/***********************************************************
	Function Name: CamConfig_sendFifoCmds
	Function Description: This function is responsible for
	sending the entire contents of the config fifo.  This
	function won't return until the configuration process
	is complete (or an error is encountered).
	Inputs:  none
	Outputs: none
	Note: Since this function is written to use the TWI
	interrupt in the I2CInterface module, there will be 
	some busy-waiting here...no big deal, since we end up
	having to trash the frame that we are executing this
	slave write in anyway (since we can't meet the strict
	timing requirements and write i2c at the same time).
***********************************************************/	
void CamConfig_sendFifoCmds(void)
{
	i2cCmd_t cmd;
	
	while (CamConfig_txFifoHead != CamConfig_txFifoTail)
	{
		cmd = CamConfig_readTxFifo();
		I2CInt_writeData(CAM_ADDRESS,&cmd.configReg,SIZE_OF_I2C_CMD);
		Utility_delay(100);		
		/* wait for the I2C transaction to complete */
		while(I2CInt_isI2cBusy() == TRUE);
	} 
}

/***********************************************************
	Function Name: CamConfig_writeTxFifo
	Function Description: This function is responsible for
	adding a new command to the tx fifo.  It adjusts all
	needed pointers.
	Inputs:  cmd - the i2cCmd_t to add to the fifo
	Outputs: bool_t - indicating if writing to the fifo
	         causes it to wrap
***********************************************************/	
bool_t CamConfig_writeTxFifo(i2cCmd_t cmd)
{
	unsigned char tmpHead;
	bool_t retVal = TRUE;
 	
	CamConfig_txFifo[CamConfig_txFifoHead] = cmd;
		
	/* see if we need to wrap */
	tmpHead = (CamConfig_txFifoHead+1) & (CAM_CONFIG_TX_FIFO_MASK);
	CamConfig_txFifoHead = tmpHead;
	
	/* check to see if we have filled up the queue */
	if (CamConfig_txFifoHead == CamConfig_txFifoTail)
	{
		/* we wrapped the fifo...return false */
		retVal = FALSE;
	}
	return(retVal);
}

/***********************************************************
	Function Name: CamConfig_readTxFifo
	Function Description: This function is responsible for
	reading a cmd out of the tx fifo.
	Inputs:  none
	Outputs: i2cCmd_t - the cmd read from the fifo
***********************************************************/	
static i2cCmd_t CamConfig_readTxFifo(void)
{
	i2cCmd_t cmd;
	unsigned char tmpTail;
	
	/* just return the current tail from the rx fifo */
	cmd = CamConfig_txFifo[CamConfig_txFifoTail];	
	tmpTail = (CamConfig_txFifoTail+1) & (CAM_CONFIG_TX_FIFO_MASK);
	CamConfig_txFifoTail = tmpTail;
	
	return(cmd);
}
