/*
 * Timer.c
 *
 *  Created on: Apr 14, 2014
 *      Author: Todd
 */
#include <string.h>
#include "Timer.h"

static bool (*userTimerCallBack)(void) = NULL;

void Timer_Start(unsigned short delaySec, bool (*timerCallBack)(void)) {
	userTimerCallBack = timerCallBack;

	unsigned short delay = delaySec * 2400;

	BCSCTL3 |= LFXT1S_2;                      // LFXT1 = VLO
	TACCTL0 = CCIE;                          // TACCR0 interrupt enabled
	TACCR0 = delay;
	TACTL = TASSEL_1 +                    // ACLK
			ID_3 +                        // devided by 4
			MC_1;                         // up
}

void Timer_Stop() {
	TACTL = MC_0;
}

#pragma vector=TIMERA0_VECTOR
__interrupt void Timer_A (void)
{
	if(userTimerCallBack())
		__bic_SR_register_on_exit(CPUOFF);        // Clear CPUOFF bit from 0(SR)
}
