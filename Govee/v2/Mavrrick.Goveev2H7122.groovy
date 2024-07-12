// Hubitat driver for Govee H7122 Air Purifier
// Version 2.0.0
//
// 05/07/2024 2.1.0 update to support Nested devices under Parent devices


// Includes of library objects
#include Mavrrick.Govee_Cloud_API
#include Mavrrick.Govee_Cloud_Life

import groovy.json.JsonSlurper 

metadata {
	definition(name: "Govee v2 H7122", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
		capability "Actuator"
        capability "Initialize"
		capability "Refresh"
        capability "Configuration"
        
        attribute "online", "string"
        attribute "mode", "number"
        attribute "modeValue", "number"
        attribute "modeDescription", "string"
        attribute "pollInterval", "number"
        attribute "cloudAPI", "string"
        attribute "filterLifeTime", "number"        
        attribute "airQuality", "number"
        
        command "changeInterval", [[name: "changeInterval", type: "NUMBER",  description: "Change Polling interval range from 0-600", range: 0-600, required: true]]
        command "setFanSpeed", [[name: "gearMode", type: "ENUM", constraints: [ 'Low',      'Medium',       'High'], description: "Default speed of Fan using GearMode"]]
        command "customSpeed", [[name: "gearMode", type: "NUMBER",  description: "Customized speed between 1 and 13", range: 1-13, required: true]]
        command "autoMode"
        command "sleepMode"
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
    if (logEnable) runIn(1800, logsOff)
    if (pollRate > 0) {
        pollRateInt = pollRate.toInteger()
        randomOffset(pollRateInt)
        runIn(offset,poll)
    }
//    poll()
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
        cloudOn()
}

def off() {
        cloudOff()
}

def autoMode() {
    log.debug "autoMode(): Processing Working Mode command 'Auto' "
    sendEvent(name: "cloudAPI", value: "Pending")
    values = '{"workMode":3,"modeValue":0}'  // This is the string that will need to be modified based on the potential values
    sendCommand("workMode", values, "devices.capabilities.work_mode")
}

def sleepMode() {
    log.debug "sleep(): Processing Working Mode command 'sleepMode' "
    sendEvent(name: "cloudAPI", value: "Pending")
    values = '{"workMode":5,"modeValue":0}'  // This is the string that will need to be modified based on the potential values
    sendCommand("workMode", values, "devices.capabilities.work_mode")
}

def setFanSpeed(gear) {
    log.debug "setFanSpeed(): Processing Working Mode command 'setFanSpeed' to ${gear} "
    sendEvent(name: "cloudAPI", value: "Pending")
    switch(gear){
        case "Low":
            gear = 1;
        break;
        case "Medium":
            gear = 2;
        break;
        case "High":
            gear = 3;
        break;
    }
    values = '{"workMode":1,"modeValue":'+gear+'}'  // This is the string that will need to be modified based on the potential values
    sendCommand("workMode", values, "devices.capabilities.work_mode")
}

def customSpeed(gear) {
    log.debug "setFanSpeed(): Processing Working Mode command 'Custom' speed with a value of ${gear} "
    sendEvent(name: "cloudAPI", value: "Pending")
    values = '{"workMode":1,"modeValue":'+gear+'}'  // This is the string that will need to be modified based on the potential values
    sendCommand("workMode", values, "devices.capabilities.work_mode")
}
