#include <string.h>
#include <stdbool.h>

/******************************************************************************/
// Virtual Com Port Communication
/******************************************************************************/
#ifndef VIRTUALCOMCMDS_H_
#define VIRTUALCOMCMDS_H_

void COM_Init(bool (*userCommandCallback)(char* text, int size));
void TXString(char* string, int length);

#endif /* VIRTUALCOMCMDS_H_ */
