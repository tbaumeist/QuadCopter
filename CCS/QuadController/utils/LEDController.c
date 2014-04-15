/*
 * LEDController.c
 *
 *  Created on: Mar 30, 2014
 *      Author: Todd
 */
#include <msp430.h>
#include "LEDController.h"

void LED_Init() {
	P1DIR |= BIT0 + BIT1; // set P1.0 and P1.1 direction to be out
	P1OUT = 0x0; // Turn off LEDs initially
}

void SetRedLED(bool turnOn) {
	if(turnOn)
		P1OUT |= BIT0;
	else
		P1OUT &= ~BIT0;
}
void ToggleRedLED() {
	P1OUT ^= BIT0;
}
void SetGreenLED(bool turnOn) {
	if(turnOn)
			P1OUT |= BIT1;
		else
			P1OUT &= ~BIT1;
}
void ToggleGreenLED() {
	P1OUT ^= BIT1;
}
