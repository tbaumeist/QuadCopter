/*
 * BoardStats.c
 *
 *  Created on: Mar 30, 2014
 *      Author: Todd
 */

#include <msp430.h>
#include "BoardStats.h"

volatile int * tempOffset = (int *) 0x10F4;

static char temp_string[] = { " XX.XF" };
static char volt_string[] = { "X.XV" };

/*------------------------------------------------------------------------------
* ADC10 interrupt service routine
------------------------------------------------------------------------------*/
#pragma vector=ADC10_VECTOR
__interrupt void ADC10_ISR(void)
{
  __bic_SR_register_on_exit(CPUOFF);        // Clear CPUOFF bit from 0(SR)
}

int BoardTemperature() {
	volatile long temp = 0;
	int degree = 0;

	/* Get temperature */
	ADC10CTL1 = INCH_10 + ADC10DIV_4;       // Temp Sensor ADC10CLK/5
	ADC10CTL0 = SREF_1 + ADC10SHT_3 + REFON + ADC10ON + ADC10IE + ADC10SR;
	/* Allow ref voltage to settle for at least 30us (30us * 8MHz = 240 cycles)
	 * See SLAS504D for settling time spec
	 */
	__delay_cycles(240);
	ADC10CTL0 |= ENC + ADC10SC;             // Sampling and conversion start
	__bis_SR_register(CPUOFF + GIE);        // LPM0 with interrupts enabled
	temp = ADC10MEM;                  // Retrieve result

	/* Stop and turn off ADC */
	ADC10CTL0 &= ~ENC;
	ADC10CTL0 &= ~(REFON + ADC10ON);

	degree = ((temp - 673) * 4230) / 1024;
	if ((*tempOffset) != 0xFFFF) {
		degree += (*tempOffset);
	}

	// convert to F
	degree = (int) (((float) degree) * 1.8) + 320;

	return degree;
}

char* ToStringTemperature(int temperature) {
	int temp = temperature;
	if (temp < 0) {
		temp_string[0] = '-';
		temp = temp * -1;
	} else if (((temp / 1000) % 10) != 0) {
		temp_string[0] = '0' + ((temp / 1000) % 10);
	}
	temp_string[4] = '0' + (temp % 10);
	temp_string[2] = '0' + ((temp / 10) % 10);
	temp_string[1] = '0' + ((temp / 100) % 10);
	return temp_string;
}

int BoardVoltage() {
	int voltage = 0;

	/* Get voltage */
	ADC10CTL1 = INCH_11;                     // AVcc/2
	ADC10CTL0 = SREF_1 + ADC10SHT_2 + REFON + ADC10ON + ADC10IE + REF2_5V;
	__delay_cycles(240);
	ADC10CTL0 |= ENC + ADC10SC;             // Sampling and conversion start
	__bis_SR_register(CPUOFF + GIE);        // LPM0 with interrupts enabled
	voltage = ADC10MEM;                  // Retrieve result

	/* Stop and turn off ADC */
	ADC10CTL0 &= ~ENC;
	ADC10CTL0 &= ~(REFON + ADC10ON);

	voltage = (voltage * 25) / 512;

	return voltage;
}

char* ToStringVoltage(int voltage) {
	volt_string[0] = '0' + (voltage / 10) % 10;
	volt_string[2] = '0' + (voltage % 10);
	return volt_string;
}
