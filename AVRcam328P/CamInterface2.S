;       Module Name: CanInterface.S
;       Module Date: 04/14/2004
;       Module Auth: John O.
;
;       Description: This module provides the low-level interface
;       to the OV6620 camera hardware.  It is responsible for
;   	acquiring each pixel block (RG,GB), performing the mapping
;       into an actual color (orange, purple, etc), run-length
;       encoding the data, and storing the info off to the appropriate
;       line buffer.  This routine is synchronized with the pixel data
;       so that no polling of the camera data needs to be done (the
;       OV6620 is clocked off of the same crystal source as the mega8,
;       thus providing inherent synchronization between the two).
;
;	4/17/2012				Updated for modern compilation
;							Ported to ATmega328P
;							Michael Shimniok
#include <avr/io.h>

#define PCLK_IE 0

        .section .text

        .global CamInt_waitForNewFrame
		.global CamInt_acquireLine
		//.global SIG_INTERRUPT0
		//.global SIG_INTERRUPT1
		
;*****************************************************************		
;   	Function Name: CamInt_waitForNewFrame
;       Function Description: This function is responsible for
;       going to sleep until a new frame begins (indicated by
;    	VSYNC transitioning from high to low.  This will wake
;       the "VSYNC sleep" up and allow it to continue with 
;       the acquireLine function, where the system waits for
;       a "PCLK sleep" that we use to synchronize with the
;       data.  
;       Inputs:  none
;       Outputs: none
;       NOTES: This function doesn't really return...it sorta just
;       floats into the acquireLine function after the "VSYNC sleep"
;       is awoken.
;*****************************************************************
		
CamInt_waitForNewFrame:
		sleep     			; sleep mode already set to sleep idle

CamInt_acquireLine:
		in 	r16,_SFR_IO_ADDR(EIMSK)  ;enable the PCLK interrupt
		ori	r16,(1<<PCLK_IE)
		out	_SFR_IO_ADDR(EIMSK),r1
		; should probably enable sleep just prior to this (see CamInterface.c)
		sleep
		
		in r16,_SFR_IO_ADDR(EIMSK)  ;disable the PCLK interrupt
		andi r16,(1<<PCLK_IE)
		out _SFR_IO_ADDR(EIMSK),r1
		
_acquirePixelBlock:		


_exit:
		ret



        .global __vector_default                ; Note [10]
__vector_default:
        reti

        .end
