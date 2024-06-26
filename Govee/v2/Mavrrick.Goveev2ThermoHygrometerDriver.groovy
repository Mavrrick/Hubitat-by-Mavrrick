// Hubitat driver for Govee Appliances using Cloud API
// Version 2.1.0
//
// 05/07/2024 2.1.0 update to support Nested devices under Parent devices

// Includes of library objects
#include Mavrrick.Govee_Cloud_API

import groovy.json.JsonSlurper

metadata {
	definition(name: "Govee v2 Thermo/Hygrometer Driver", namespace: "Mavrrick", author: "Mavrrick") {
        capability "Initialize"
		capability "Refresh" 
        capability "TemperatureMeasurement"         
        capability "RelativeHumidityMeasurement"
        attribute "online", "string"
        attribute "cloudAPI", "string"
    }

	preferences {		
		section("Device Info") {  
            input("tempUnit", "enum", title: "Temp Unit Selection", defaultValue: 'Fahrenheit', options: [    "Fahrenheit",     "Celsius"], required: true)
            input("pollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true, width:4)            
            input(name: "debugLog", type: "bool", title: "Debug Logging", defaultValue: false)
            input("descLog", "bool", title: "Enable descriptionText logging", required: true, defaultValue: true) 
		}
		
	}
}

//////////////////////////////////////
// Standard Methods for all drivers //
//////////////////////////////////////

// reset of device settings when preferences updated.
def updated() {
    unschedule()
    if (debugLog) runIn(1800, logsOff)
    poll()
}

// linital setup when device is installed.
def installed(){
    poll ()
}

// initialize devices upon install and reboot.
def initialize() {
    unschedule()
    if (debugLog) runIn(1800, logsOff)
    if (pollRate > 0) {
        pollRateInt = pollRate.toInteger()
        randomOffset(pollRateInt)
        runIn(offset,poll)
    }
//    poll()
}

// update data for the device
def refresh() {
    if (debugLog) {log.info "refresh(): Performing refresh"}
    unschedule(poll)
    poll()
    if (device.currentValue("connectionState") == "connected") {
    }
}

// retrieve setup values and initialize polling and logging
def configure() {
    if (debugLog) {log.info "configure(): Driver Updated"}
    unschedule()
    if (pollRate > 0) runIn(pollRate,poll)         
    if (debugLog) runIn(1800, logsOff) 
}

////////////////////
// Helper methods //
////////////////////

logsOff  // turn off logging for the device
def logsOff() {
    log.info "debug logging disabled..."
    device.updateSetting("debugLog", [value: "false", type: "bool"])
}

poll // retrieve device status
def poll() {
    if (debugLog) {log.info "poll(): Poll Initated"}
    getDeviceState()    
//	getDeviceTempHumid()
    if (pollRate > 0) runIn(pollRate,poll)
}	
