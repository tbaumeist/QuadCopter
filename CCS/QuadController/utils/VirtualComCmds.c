#include <msp430.h>
#include "VirtualComCmds.h"

#define BUFFER_SIZE 64
static char inputBuffer[BUFFER_SIZE + 1];
static int inputBufferIndex = 0;

static bool (*callback_UserCommand)(char* text, int size) = NULL;

void clearInputBuffer();

/******************************************************************************/
// End Virtual Com Port Communication
/******************************************************************************/
void COM_Init(bool (*userCommandCallback)(char* text, int size)) {
	callback_UserCommand = userCommandCallback; // register the user command call back
	clearInputBuffer();

	P3SEL |= BIT4 + BIT5;                            // P3.4,5 = USCI_A0 TXD/RXD
	UCA0CTL1 = UCSSEL_2;                      // SMCLK

	// Assumes clock running at 8 Mhz
	UCA0BR0 = 0x41;                           // 9600 from 8Mhz
	UCA0BR1 = 0x3;
	UCA0MCTL = UCBRS_2;

	UCA0CTL1 &= ~UCSWRST;                   // **Initialize USCI state machine**
	IE2 |= UCA0RXIE;                          // Enable USCI_A0 RX interrupt
}

void TXString(char* string, int length) {
	int pointer;
	for (pointer = 0; pointer < length; pointer++) {
		volatile int i;
		UCA0TXBUF = string[pointer];
		while (!(IFG2 & UCA0TXIFG))
			;              // USCI_A0 TX buffer ready?
	}
}

/*------------------------------------------------------------------------------
 * Private functions
 ------------------------------------------------------------------------------*/
void clearInputBuffer() {
	inputBufferIndex = 0;
	memset(inputBuffer, NULL, BUFFER_SIZE + 1);
}

/*------------------------------------------------------------------------------
 * USCIA interrupt service routine
 ------------------------------------------------------------------------------*/
#pragma vector=USCIAB0RX_VECTOR
__interrupt void USCI0RX_ISR(void) {

	//check if buffer is full
	if (inputBufferIndex >= (BUFFER_SIZE - 1))
		return;

	char rx = UCA0RXBUF;

	// echo input back on output
	//TXString(&rx, 1);

	// enter was pressed
	if (rx == 13 && callback_UserCommand(inputBuffer, inputBufferIndex)) {
		clearInputBuffer();  // command was consumed by the registered callback
		__bic_SR_register_on_exit(CPUOFF);        // Clear CPUOFF bit from 0(SR)
		return;
	}

	inputBuffer[inputBufferIndex++] = rx;
}

