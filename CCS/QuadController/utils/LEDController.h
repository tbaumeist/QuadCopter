/*
 * LEDController.h
 *
 *  Created on: Mar 30, 2014
 *      Author: Todd
 */
#include <stdbool.h>

#ifndef LEDCONTROLLER_H_
#define LEDCONTROLLER_H_

void LED_Init();
void SetRedLED(bool turnOn);
void ToggleRedLED();
void SetGreenLED(bool turnOn);
void ToggleGreenLED();

#endif /* LEDCONTROLLER_H_ */
