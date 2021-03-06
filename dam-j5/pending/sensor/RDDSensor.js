// This module defines a Johnny-Five Controller object for use with the
// J-5 Sensor Component and a DDSensor device driver on an Arduino.
//
// This program is strict-mode throughout.
//
// Doug Johnson, April 2016

const log4js = require("log4js");
const five = require("johnny-five");

const RDD = require("../lib/RemoteDeviceDriver");
const rddErr = require("../lib/RDDStatus");
const rddCmd = require("../lib/RDDCommand");

let logger = log4js.getLogger("RDDSensor");

/**
 * Create an RDDSensor Controller object for use with a Sensor Component.
 */
let RDDSensor = {

  initialize: {
    value: function(opts) {
      // Can an externally defined Controller get at the private state Map?
      // let state = five.Sensor.priv.get(this);
      // For the time being, I'll use a single property 'rdd' instead ...

      this.rdd = {};

      // Define register numbers specific to this device.  See also the Common
      // Device Registers (CDR) defined in RDDCommands.js.

      let reg = {
        PIN: 256
      };
      this.rdd.reg = reg;

      this.rdd.openFlags = opts.custom.flags || 1;
      this.rdd.unit = opts.custom.unit || "Chan:0";
      this.rdd.board = opts.board || five.Board.mount();
      logger.trace(`Mode check: isAnalog(${this.pin}) is ${this.board.pins.isAnalog(this.pin)}`);

      let dd =  new RDD.RemoteDeviceDriver({board: this.rdd.board, skipCapabilities: false});
      this.rdd.dd = dd;
      this.rdd.handle = 0;
      dd.open(this.rdd.unit,this.rdd.openFlags,(response) => {
        logger.trace(`Callback openCB invoked.`);
        logger.trace(`Property keys of 'this' are ${Object.keys(this)}.`);
        logger.trace(`Property keys of 'this.rdd' are ${Object.keys(this.rdd)}.`);
        if (response.status >= 0) {
          logger.debug(`Status value from open() is ${response.status}`);
          this.rdd.handle = response.status;
          dd.read(this.rdd.handle,rddCmd.CDR.DriverVersion,256,(response) => {
            logger.trace(`readCB callback invoked.`);
            if (response.status >= 0) {
              logger.debug(`Status value from read() is ${response.status}`);
              this.rdd.sv = new rddCmd.SemVer(response.datablock);
              logger.info(`DeviceDriver '${this.rdd.sv.toString()}' is open on logical unit '${this.rdd.unit}' with handle ${this.rdd.handle}`);
              dd.write(this.rdd.handle,reg.PIN,2,[this.pin,0],(response) => {
                logger.trace(`writeCB callback invoked after setting pin = ${this.pin}.`);
                if (response.status >= 0) {
                  logger.debug(`Status value from write() is ${response.status}`);
                  logger.info(`Logical unit '${this.rdd.unit}' (handle ${this.rdd.handle}) is attached to pin ${this.pin}.`);
                } else {
                  logger.error(`Error value from write() is ${response.status}`);
                }
              });
            } else {
              logger.error(`Error value from read() is ${response.status}`);
            }
          });
        } else {
          logger.error(`Error value from open() is ${response.status}`);
        }
      });
    }
  },

  doAnalogRead: {
    value: function() {
      let result = analogRead(this.pin);
      return result;
    }
  },

  doDigitalRead: {
    value: function() {
      let result = digitalRead(this.pin);
      return result;
    }
  }
};

module.exports = {RDDSensor};
