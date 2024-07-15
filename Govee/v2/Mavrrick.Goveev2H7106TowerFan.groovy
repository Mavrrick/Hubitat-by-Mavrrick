// Hubitat driver for Govee H7106 Fan Driver using Cloud API
// Version 2.1.0
//
// 05/07/2024 2.1.0 update to support Nested devices under Parent devices

// Includes of library objects
#include Mavrrick.Govee_Cloud_API

import groovy.json.JsonSlurper
import groovy.transform.Field

@Field Map getFanLevel = [
    "off": 0
    ,"on": 1
    ,"ultra-low": 12
	,"low": 25
    ,"medium-low": 35
	,"medium": 50
    ,"medium-high": 75
	,"high": 100
    ,"turbo": 100
    ,"auto": 150
]

metadata {
	definition(name: "Govee v2 H7106 Tower Fan", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
		capability "Actuator"
        capability "Initialize"
		capability "Refresh" 
        capability "TemperatureMeasurement"
        capability "Configuration"         

        attribute "online", "string"
        attribute "mode", "number"
        attribute "modeValue", "number"
        attribute "modeDescription", "string"
        attribute "pollInterval", "number"
        attribute "cloudAPI", "string"
        attribute "online", "string"
        attribute "oscillation", "string"

        command "airDeflectoron_off", [[name: "Oscillation", type: "ENUM", constraints: ['On',      'Off'] ] ]
        command "setSpeed", [[name: "Fan speed*",type:"ENUM", description:"Fan speed to set", constraints: getFanLevel.collect {k,v -> k}]]
/*        command "workingMode", [[name: "mode", type: "ENUM", constraints: [ 'FanSpeed',      'Custom',       'Auto',    'Sleep',    'Nature'], description: "Mode of device"],
                          [name: "gearMode", type: "NUMBER", description: "Only used when mode is FanSpeed"]] */
        command "natureMode", [[name: "natureMode", type: "NUMBER",  description: "Enter nature mode ", range: 0-8, required: true]]
        command "changeInterval", [[name: "changeInterval", type: "NUMBER",  description: "Change Polling interval range from 0-600", range: 0-600, required: true]]
        command "autoMode"
        command "sleepMode", [[name: "sleepMode", type: "NUMBER",  description: "Enter nature mode ", range: 0-8, required: true]]
    }

	preferences {		
		section("Device Info") {
            input("pollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true, width:4)            
            input(name: "debugLog", type: "bool", title: "Debug Logging", defaultValue: false)
            input("descLog", "bool", title: "Enable descriptionText logging", required: true, defaultValue: true) 
		}
		
	}
}

//////////////////////////////////////
// Standard Methods for all drivers //
//////////////////////////////////////

Update // reset of device settings when preferences updated.
def updated() {
    unschedule()
    if (debugLog) runIn(1800, logsOff)
    retrieveStateData()
    poll()
}

Installed // linital setup when device is installed.
def installed(){
    retrieveStateData()
    poll()
}

Initialize // initialize devices upon install and reboot.
def initialize() {
     if (device.currentValue("cloudAPI") == "Retry") {
        if (debugLog) {log.error "initialize(): Cloud API in retry state. Reseting "}
        sendEvent(name: "cloudAPI", value: "Initialized")
    }
    unschedule()
    if (pollRate > 0) {
        pollRateInt = pollRate.toInteger()
        randomOffset(pollRateInt)
        runIn(offset,poll)
    }
//    if (debugLog) runIn(1800, logsOff)
    poll()
}

Refresh // update data for the device
def refresh() {
    if (debugLog) {log.info "refresh(): Performing refresh"}
    unschedule(poll)
    poll()
    if (device.currentValue("connectionState") == "connected") {
    }
}

Configure // retrieve setup values and initialize polling and logging
def configure() {
    if (debugLog) {log.info "configure(): Driver Updated"}
    unschedule()
    if (pollRate > 0) runIn(pollRate,poll)     
    retrieveStateData()    
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
    if (pollRate > 0) runIn(pollRate,poll)
}	

//////////////////////
// Driver Commands // 
/////////////////////

def on() {
    if (lanControl) {
        lanOn() }
    else {
        cloudOn()
        }
}

def off() {
    if (lanControl) {
        lanOff() }
    else {
        cloudOff()
        }
}

def airDeflectoron_off(evt) {
    log.debug "airDeflectoron_off(): Processing Air Deflector command. ${evt}"
        if (device.currentValue("cloudAPI") == "Retry") {
             log.error "airDeflectoron_off(): CloudAPI already in retry state. Aborting call." 
         } else {
        sendEvent(name: "cloudAPI", value: "Pending")
            if (device.getDataValue("commands").contains("airDeflectorToggle")) {
                if (evt == "On") sendCommand("airDeflectorToggle", 1 ,"devices.capabilities.toggle")
                if (evt == "Off") sendCommand("airDeflectorToggle", 0 ,"devices.capabilities.toggle")
            } else if (device.getDataValue("commands").contains("oscillationToggle")) {
                if (evt == "On") sendCommand("oscillationToggle", 1 ,"devices.capabilities.toggle")
                if (evt == "Off") sendCommand("oscillationToggle", 0 ,"devices.capabilities.toggle")
            }
        }
}

def setSpeed(fanspeed) {
    log.debug "setFanSpeed(): Processing Working Mode command 'setFanSpeed' to ${fanspeed} "
    sendEvent(name: "cloudAPI", value: "Pending")
    switch(fanspeed){
        case "ultra-low":
            gearmode = 1;
            gear = 2;
        break;
        case "low":
            gearmode = 1;
            gear = 3;
        break;
        case "medium-low":
            gearmode = 1;
            gear = 4;
        break;
        case "medium":
            gearmode = 1;
            gear = 5;
        break;
        case "medium-high":
            gearmode = 1;
            gear = 6;
        break;
        case "high":
            gearmode = 1;
            gear = 7;
        break;
        case "turbo":
            gearmode = 1;
            gear = 8;
        break;
        case "auto":
            gearmode = 2;
            gear = 0;
        break;        
    }
    if (fanspeed == "on") {
        cloudOn()
    } else if (fanspeed == "off") {
        cloudOff()
    } else {
        values = '{"workMode":'+gearmode+',"modeValue":'+gear+'}'  // This is the string that will need to be modified based on the potential values
        sendCommand("workMode", values, "devices.capabilities.work_mode")
    }
}

def natureMode(gear) {
    log.debug "natureMode(): Processing Working Mode command 'natureMode' to ${gear} "
    sendEvent(name: "cloudAPI", value: "Pending")
    values = '{"workMode":4,"modeValue":'+gear+'}'  // This is the string that will need to be modified based on the potential values
    sendCommand("workMode", values, "devices.capabilities.work_mode")
}

def autoMode() {
    log.debug "autoMode(): Processing Working Mode command 'Auto' "
    sendEvent(name: "cloudAPI", value: "Pending")
    values = '{"workMode":2,"modeValue":0}'  // This is the string that will need to be modified based on the potential values
    sendCommand("workMode", values, "devices.capabilities.work_mode")
}

def sleepMode(gear) {
    log.debug "sleep(): Processing Working Mode command 'sleepMode' "
    sendEvent(name: "cloudAPI", value: "Pending")
    values = '{"workMode":3,"modeValue":'+gear+'}' // This is the string that will need to be modified based on the potential values
    sendCommand("workMode", values, "devices.capabilities.work_mode")
}
