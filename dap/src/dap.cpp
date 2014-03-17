/*
 ============================================================================
 Name        : dap.c
 Author      : Doug Johnson
 Version     :
 Copyright   : GPL
 Description : I2C Data Acquisition
 ============================================================================
 */

#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <fcntl.h>
#include <linux/i2c.h>
#include <linux/i2c-dev.h>
#include <sys/ioctl.h>
#include <stropts.h>
#include <math.h>
#include "LSM303Accelerometer.h"

int main(int argc, char* argv[]) {
	printf("DAP - I2C Data Acquisition Program\n");

	LSM303Accelerometer *d = new LSM303Accelerometer(1,0x19,"LSM303 Accelerometer");

	printf("Device: %s at Bus %i, Address %#x.\n",d->getDeviceName(),d->getBus(),d->getDeviceAddress());

	int status = d->openDevice();
	if (status < 0) {
		return EXIT_FAILURE;
	}
	printf("Device opened.  Handle: %i\n",d->getHandle());

	status = d->readFullSensorState();
	if (status < 0) {
		return EXIT_FAILURE;
	}

	status = d->closeDevice();
	if (status < 0) {
		return EXIT_FAILURE;
	}

	// set control registers

	// 0x20 CTRL_REF1_A - power, rate

	return EXIT_SUCCESS;
}

