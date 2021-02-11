###############################################################################
# Makefile for the project AVRcam
###############################################################################

## General Flags
PROJECT = AVRcam
MCU = atmega328p
TARGET = AVRcam.elf
CC = avr-gcc

CPP = avr-g++

## Options common to compile, link and assembly rules
COMMON = -mmcu=$(MCU)

## Compile options common for all C compilation units.
CFLAGS = $(COMMON)
CFLAGS += -Wall -gdwarf-2 -std=gnu99      -DF_CPU=17700000UL -Os -funsigned-char -funsigned-bitfields -fpack-struct -fshort-enums
CFLAGS += -MD -MP -MT $(*F).o -MF dep/$(@F).d 

## Assembly specific flags
ASMFLAGS = $(COMMON)
ASMFLAGS += $(CFLAGS)
ASMFLAGS += -x assembler-with-cpp -Wa,-gdwarf2

## Linker flags
LDFLAGS = $(COMMON)
LDFLAGS +=  -Wl,-Map=AVRcam.map
LDFLAGS += -Wl,-section-start=.noinit=0x800400


## Intel Hex file production flags
HEX_FLASH_FLAGS = -R .eeprom -R .fuse -R .lock -R .signature

HEX_EEPROM_FLAGS = -j .eeprom
HEX_EEPROM_FLAGS += --set-section-flags=.eeprom="alloc,load"
HEX_EEPROM_FLAGS += --change-section-lma .eeprom=0 --no-change-warnings


## Objects that must be built in order to link
OBJECTS = Utility.o CamConfig.o CamInterface.o CamInterface2.o CamInterfaceAsm.o DebugInterface.o Executive.o FrameMgr.o I2CInterface.o Main.o UartInterface.o UIMgr.o 

## Objects explicitly added by the user
LINKONLYOBJECTS = 

## Build
all: $(TARGET) AVRcam.hex AVRcam.eep AVRcam.lss size

## Compile
Utility.o: ../Utility.c
	$(CC) $(INCLUDES) $(CFLAGS) -c  $<

CamConfig.o: ../CamConfig.c
	$(CC) $(INCLUDES) $(CFLAGS) -c  $<

CamInterface.o: ../CamInterface.c
	$(CC) $(INCLUDES) $(CFLAGS) -c  $<

CamInterface2.o: ../CamInterface2.S
	$(CC) $(INCLUDES) $(ASMFLAGS) -c  $<

CamInterfaceAsm.o: ../CamInterfaceAsm.S
	$(CC) $(INCLUDES) $(ASMFLAGS) -c  $<

DebugInterface.o: ../DebugInterface.c
	$(CC) $(INCLUDES) $(CFLAGS) -c  $<

Executive.o: ../Executive.c
	$(CC) $(INCLUDES) $(CFLAGS) -c  $<

FrameMgr.o: ../FrameMgr.c
	$(CC) $(INCLUDES) $(CFLAGS) -c  $<

I2CInterface.o: ../I2CInterface.c
	$(CC) $(INCLUDES) $(CFLAGS) -c  $<

Main.o: ../Main.c
	$(CC) $(INCLUDES) $(CFLAGS) -c  $<

UartInterface.o: ../UartInterface.c
	$(CC) $(INCLUDES) $(CFLAGS) -c  $<

UIMgr.o: ../UIMgr.c
	$(CC) $(INCLUDES) $(CFLAGS) -c  $<

##Link
$(TARGET): $(OBJECTS)
	 $(CC) $(LDFLAGS) $(OBJECTS) $(LINKONLYOBJECTS) $(LIBDIRS) $(LIBS) -o $(TARGET)

%.hex: $(TARGET)
	avr-objcopy -O ihex $(HEX_FLASH_FLAGS)  $< $@

%.eep: $(TARGET)
	-avr-objcopy $(HEX_EEPROM_FLAGS) -O ihex $< $@ || exit 0

%.lss: $(TARGET)
	avr-objdump -h -S $< > $@

size: ${TARGET}
	@echo
	@avr-size -C --mcu=${MCU} ${TARGET}

## Clean target
.PHONY: clean
clean:
	-rm -rf $(OBJECTS) AVRcam.elf dep/* AVRcam.hex AVRcam.eep AVRcam.lss AVRcam.map


## Other dependencies
-include $(shell mkdir dep 2>/dev/null) $(wildcard dep/*)

