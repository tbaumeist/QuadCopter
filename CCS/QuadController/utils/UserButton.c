/*
 * Button.c
 *
 *  Created on: Apr 3, 2014
 *      Author: Todd
 */
#include <msp430.h>
#include <string.h>
#include "UserButton.h"

static void (*userButtonPressCallBack)(void) = NULL;

void Button_Init(void (*userCallback)(void)) {
	userButtonPressCallBack = userCallback;

	P1REN |= BIT2;
	P1OUT = BIT2;
	P1IE |= BIT2;
	P1IES |= BIT2;
	P1IFG &= ~BIT2;
}


#pragma vector=PORT1_VECTOR
__interrupt void Port_1(void) {
	if (P1IFG & BIT2) {
		P1IFG &= ~BIT2;
		userButtonPressCallBack();
		__bic_SR_register_on_exit(CPUOFF);        // Clear CPUOFF bit from 0(SR)
	}
}

