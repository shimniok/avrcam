;**** A P P L I C A T I O N   N O T E   A V R 3 0 0 ************************
;*
;* Title		: I2C (Single) Master Implementation
;* Version		: 1.0 (BETA)
;* Last updated		: 97.08.27
;* Target		: AT90Sxxxx (any AVR device)
;*
;* Support email	: avr@atmel.com
;* Modified by:       John Orlando (for usage with the AVRcam)
;*                    john@jrobot.net   www.jrobot.net
;*
;* DESCRIPTION
;* 	Basic routines for communicating with I2C slave devices. This
;*	"single" master implementation is limited to one bus master on the
;*	I2C bus. Most applications do not need the multimaster ability
;*	the I2C bus provides. A single master implementation uses, by far,
;*	less resources and is less XTAL frequency dependent.
;*
;*	Some features :
;*	* All interrupts are free, and can be used for other activities.
;*	* Supports normal and fast mode.
;*	* Supports both 7-bit and 10-bit addressing.
;*	* Supports the entire AVR microcontroller family.
;*
;*	Main I2C functions :
;*	'i2c_start' -		Issues a start condition and sends address
;*				and transfer direction.
;*	'i2c_rep_start' -	Issues a repeated start condition and sends
;*				address and transfer direction.
;*	'i2c_do_transfer' -	Sends or receives data depending on
;*				direction given in address/dir byte.
;*	'i2c_stop' -		Terminates the data transfer by issue a
;*				stop condition.
;*
;* USAGE
;*	Transfer formats is described in the AVR300 documentation.
;*	(An example is shown in the 'main' code).	
;*
;* NOTES
;*	The I2C routines can be called either from non-interrupt or
;*	interrupt routines, not both.
;*
;* STATISTICS
;*	Code Size	: 81 words (maximum)
;*	Register Usage	: 4 High, 0 Low
;*	Interrupt Usage	: None
;*	Other Usage	: Uses two I/O pins on port D
;*	XTAL Range	: N/A
;*
;***************************************************************************

;**** Includes ****

.include "tn12def.inc"			; change if an other device is used

;**** Global I2C Constants ****

.equ	SCL	= 1			; SCL Pin number (orig port D...port B on AVRcam)
.equ	SDA	= 2			; SDA Pin number (orig port D...port B on AVRcam)
.equ    ResetCtrl = 0
.equ    DEBUG = 3

.equ	b_dir	= 0			; transfer direction bit in i2cadr

.equ	i2crd	= 1
.equ	i2cwr	= 0

;**** Global Register Variables ****

.def	loop_count= r16			; Delay loop variable
.def	num_delays= r17         ; number of delays to execute
.def	i2cdata	= r18			; I2C data transfer register
.def	i2cstat	= r19			; I2C bus status register
.def    temp    = r20			; temp reg

;**** Interrupt Vectors ****

	rjmp	RESET			; Reset handle
;	( rjmp	EXT_INT0 )		; ( IRQ0 handle )
;	( rjmp	TIM0_OVF )		; ( Timer 0 overflow handle )
;	( rjmp	ANA_COMP )		; ( Analog comparator handle )


;***************************************************************************
;*
;* FUNCTION
;*	i2c_hp_delay
;*	i2c_qp_delay
;*
;* DESCRIPTION
;*	hp - half i2c clock period delay (normal: 5.0us / fast: 1.3us)
;*	qp - quarter i2c clock period delay (normal: 2.5us / fast: 0.6us)
;*
;*	SEE DOCUMENTATION !!!
;*
;* USAGE
;*	no parameters
;*
;* RETURN
;*	none
;*
;***************************************************************************

i2c_delay_hp:
;	rjmp 	t1
;t1:	rjmp 	t2
;t2: rjmp	t3
;t3: rjmp	t4
;t4: rjmp	t5
;t5: rjmp	t6
;t6: nop
	ret

;***************************************************************************
;*
;* FUNCTION
;*	i2c_start
;*
;* DESCRIPTION
;*	Generates start condition and sends slave address.
;*
;* USAGE
;*	i2cadr - Contains the slave address and transfer direction.
;*
;* RETURN
;*	Carry flag - Cleared if a slave responds to the address.
;*
;* NOTE
;*	IMPORTANT! : This funtion must be directly followed by i2c_write.
;*
;***************************************************************************

i2c_start:	
	sbi	DDRB,SDA	; force SDA low
	rcall	i2c_delay_hp
	rcall	i2c_write	; write address
	ret

;***************************************************************************
;*
;* FUNCTION
;*	i2c_write
;*
;* DESCRIPTION
;*	Writes data (one byte) to the I2C bus. Also used for sending
;*	the address.
;*
;* USAGE
;*	i2cdata - Contains data to be transmitted.
;*
;* RETURN
;*	Carry flag - Set if the slave respond transfer.
;*
;* NOTE
;*	IMPORTANT! : This funtion must be directly followed by i2c_get_ack.
;*
;***************************************************************************

i2c_write:
	sec				; set carry flag
	rol	i2cdata			; shift in carry and out bit one
	rjmp	i2c_write_first
i2c_write_bit:
	lsl	i2cdata			; if transmit register empty
i2c_write_first:
	breq	i2c_get_ack		;	goto get acknowledge
	sbi	DDRB,SCL		; force SCL low

	brcc	i2c_write_low		; if bit high
	nop				;	(equalize number of cycles)
	cbi	DDRB,SDA		;	release SDA
	rjmp	i2c_write_high
i2c_write_low:				; else
	sbi	DDRB,SDA		;	force SDA low
	rjmp	i2c_write_high		;	(equalize number of cycles)
i2c_write_high:
	rcall	i2c_delay_hp		; half period delay
	cbi	DDRB,SCL		; release SCL
	rcall	i2c_delay_hp		; half period delay

	rjmp	i2c_write_bit

i2c_get_ack:
	sbi	DDRB,SCL
	cbi	DDRB,SDA
	rcall	i2c_delay_hp
	cbi	DDRB,SCL
i2c_ack_wait:
	sbis	PINB,SCL
	rjmp	i2c_ack_wait

	clr	i2cstat
	sbic	PINB,SDA
	ldi	i2cstat,1
	rcall	i2c_delay_hp
	nop
	ret

;***************************************************************************
;*
;* FUNCTION
;*	i2c_stop
;*
;* DESCRIPTION
;*	Assert stop condition.
;*
;* USAGE
;*	No parameters.
;*
;* RETURN
;*	None.
;*
;***************************************************************************

i2c_stop:

	sbi	DDRB,SCL		; force SCL low
	sbi	DDRB,SDA		; force SDA low
	rcall	i2c_delay_hp		; half period delay
	cbi	DDRB,SCL		; release SCL
	rcall	i2c_delay_hp
	cbi	DDRB,SDA		; release SDA
	rcall	i2c_delay_hp		; half period delay
	ret


;***************************************************************************
;*
;* FUNCTION
;*	i2c_init
;*
;* DESCRIPTION
;*	Initialization of the I2C bus interface.
;*
;* USAGE
;*	Call this function once to initialize the I2C bus. No parameters
;*	are required.
;*
;* RETURN
;*	None
;*
;* NOTE
;*	PORTB and DDRB pins not used by the I2C bus interface will be
;*	set to Hi-Z (!).
;*
;* COMMENT
;*	This function can be combined with other PORTB initializations.
;*
;***************************************************************************

i2c_init:
	cbi	DDRB,SDA
	cbi	DDRB,SCL
	cbi	PORTB,SDA
	cbi PORTB,SCL
	ret


;***************************************************************************
; Use this to execute short delays...simply write a value to num_delays and
; call the main_delay routine
;***************************************************************************
main_delay:		
	ldi	loop_count,$FF
delay_loop1:
	dec	loop_count
	nop
	nop
	nop
	nop
	nop
	nop
	nop
	nop
	nop
	nop
	brne	delay_loop1
	dec	num_delays
	brne	main_delay
	ret
	
	
;***************************************************************************
;*
;* PROGRAM
;*	main - Test of I2C master implementation
;*
;* DESCRIPTION
;*	Initializes I2C interface and shows an example of using it.
;*
;***************************************************************************

RESET:

; need to make sure that the mega8 is held in reset by driving
; PB3 high
	
	sbi DDRB,ResetCtrl
	sbi PORTB,ResetCtrl

	ldi	num_delays,$FF
	rcall	main_delay

main:	rcall	i2c_init		; initialize I2C interface


;**** Write data => Adr($13) = 0x05 ****
; This will tri-state the Y and UV busses on the OV6620, which
; will allow re-programming of the mega8 to proceed at startup
; if needed.
	ldi	i2cdata,$C0+i2cwr	; Set device address and write
	rcall	i2c_start		; Send start condition and address

	ldi	i2cdata,$13		; Write reg address (0x13)
	rcall	i2c_write		; Execute transfer

	ldi	i2cdata,$05		; Set write data 0x05
	rcall	i2c_write		; Execute transfer

	rcall	i2c_stop		; Send stop condition


	ldi	num_delays,$FF
	rcall	main_delay

;**** Write data => Adr($3F) = 0x42 ****
; This will turn on the external clock for the mega8 to use.

	ldi	i2cdata,$C0+i2cwr	; Set device address and write
	rcall	i2c_start		; Send start condition and address

	ldi	i2cdata,$3F		; Write reg address (0x3F)
	rcall	i2c_write		; Execute transfer

	ldi	i2cdata,$42		; Set write data 0x42
	rcall	i2c_write		; Execute transfer

	rcall	i2c_stop		; Send stop condition

; Wait for a short amount of time for the external clock
; to stabilize
	ldi	num_delays,$FF
	rcall	main_delay

; turn on the mega8 by releasing the reset line
	cbi PORTB,ResetCtrl

	sbi DDRB,DEBUG
wait_forever:
	sbi PORTB,DEBUG
	cbi PORTB,DEBUG
	rjmp	wait_forever			; Loop forewer

;**** End of File ****


