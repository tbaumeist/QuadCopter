/*
 * Timer.h
 *
 *  Created on: Apr 14, 2014
 *      Author: Todd
 */

#ifndef TIMER_H_
#define TIMER_H_

#include <msp430.h>
#include <stdbool.h>

void Timer_Start(unsigned short delay, bool (*timerCallBack)(void));
void Timer_Stop();

#endif /* TIMER_H_ */
