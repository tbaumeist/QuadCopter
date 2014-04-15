/*
 * main2.c
 *
 *  Created on: Apr 4, 2014
 *      Author: Todd
 */
#include <msp430.h>
#include "radios/family1/mrfi_spi.h"
#include "mrfi.h"
#include "VirtualComCmds.h"
#include "LEDController.h"
#include "UserButton.h"
#include "Timer.h"

#define MAX_SIGNAL_SAMPLE 4
#define COMMAND_TYPE 9

typedef enum TASKS {
	UNKNOWN, INIT, TRANSMIT, RESET
} TASKS;

void print_rssi(int8_t rssi);
bool callback_UserCommand(char* text, int size);
void onButtonPressed();
void init();
void waitNextTask();
void initializeQuadCopter();
uint8_t findLeastInterference();
void changeChannel(uint8_t newChannel);

TASKS task;
uint8_t signalSamples[MAX_SIGNAL_SAMPLE];
int signalSampleCount;
bool commChannelChanged;

int main() {
	init();
	waitNextTask();
}

void init() {
	memset(signalSamples, NULL, sizeof(signalSamples));
	signalSampleCount = 0;

	BSP_Init();
	MRFI_RxIdle();  // make sure to turn off first in case it was on
	MRFI_Init();
	MRFI_SetLogicalChannel(0);
	MRFI_SetRFPwr(MRFI_NUM_POWER_SETTINGS - 1); // set to max power

	COM_Init(callback_UserCommand);
	LED_Init();
	Button_Init(onButtonPressed);

	MRFI_WakeUp();

	_EINT(); // turn on interrupts

	MRFI_RxOn();
}

void onButtonPressed() {
	task = RESET;
}

void waitNextTask() {
	while (true) {

		// go sleep waiting for command to start the main loop
		SetGreenLED(true);
		task = UNKNOWN;
		__bis_SR_register(CPUOFF + GIE);
		SetGreenLED(false);

		switch (task) {
		case INIT:
			initializeQuadCopter();
			break;
		case TRANSMIT:
			break;
		case RESET:
			init();
			TXString("RESET\n", 6);
			break;
		}
	}
}

bool onTimerChangeChannel() {
	return true;
}

void initializeQuadCopter() {
	// find the best communication channel
	changeChannel(findLeastInterference());

	// start timer and wait for a confirmation
	Timer_Start(1, onTimerChangeChannel);
	__bis_SR_register(CPUOFF + GIE);

	if (!commChannelChanged){
		TXString("ERROR: negotiating comm channel\n",32);
		return;
	}

	TXString("INITIALIZED\n", 12);
}

void changeChannel(uint8_t newChannel) {
	commChannelChanged = false;
}

uint8_t findLeastInterference() {
	int sampleCount;
	int maxSamples = 3;
	int channel;
	uint8_t samples[MRFI_NUM_LOGICAL_CHANS];

	memset(samples, NULL, sizeof(samples));

	for (sampleCount = 0; sampleCount < maxSamples; sampleCount++) {
		for (channel = 0; channel < MRFI_NUM_LOGICAL_CHANS; channel++) {

			MRFI_SetLogicalChannel(channel);
			samples[channel] += MRFI_Rssi();
		}
		ToggleRedLED();
	}

	int smallestChannel;
	uint8_t smallestInter = samples[0];

	for (channel = 0; channel < MRFI_NUM_LOGICAL_CHANS; channel++) {
		if (samples[channel] < smallestInter) {
			smallestInter = samples[channel];
			smallestChannel = channel;
		}
	}
	return smallestChannel;
}

bool callback_UserCommand(char* text, int size) {

	if (strcmp("INIT", text) == 0) {
		task = INIT;
	} else if (strcmp("RESET", text) == 0) {
		task = RESET;
	} else if (strcmp("TRANSMIT", text) == 0) {
		task = TRANSMIT;
	}
	if (task != UNKNOWN)
		ToggleRedLED();
	else
		TXString("UNKNOWN\n",8);

	return true;
}

void setLocalCommChannel(uint8_t channel) {
	MRFI_SetLogicalChannel(channel);
	commChannelChanged = true;
	char channelChr = channel + '0';
	TXString("CHANNEL:", 8);
	TXString(&channelChr, 1);
	TXString("\n", 1);
}

void MRFI_RxCompleteISR() {

	mrfiPacket_t packet;
	MRFI_Receive(&packet);

	switch (packet.frame[COMMAND_TYPE]) {
	case 'A': // change communication channel
		setLocalCommChannel(packet.frame[COMMAND_TYPE + 1]);
		break;
	}

//	char output[] = { "                                     \n" };
//	for (i = 9; i < 29; i++) {
//		output[i - 9] = packet.frame[i];
	//if (packet.frame[i] == '\r') {
	//	output[i - 9] = '\n';
	//	output[i - 8] = '\r';
	//}
//	}
//	TXString("ECHO:", 5);
//	TXString(output, (sizeof output));
//TXString("\n", 1);
//	rssi = MRFI_Rssi();
//	print_rssi(rssi);
//	TXString("\n", 1);

//	if (!sender)
//		MRFI_Transmit(&packet, MRFI_TX_TYPE_FORCED);
}

