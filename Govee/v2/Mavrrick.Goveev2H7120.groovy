// Hubitat driver for Govee Appliances using Cloud API
// Version 2.1.0
//
// 05/07/2024 2.1.0 update to support Nested devices under Parent devices

// Includes of library objects
#include Mavrrick.Govee_Cloud_API
#include Mavrrick.Govee_Cloud_RGB
#include Mavrrick.Govee_Cloud_Level
#include Mavrrick.Govee_Cloud_Life

import groovy.json.JsonSlurper 

metadata {
	definition(name: "Govee v2 H7120", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
        capability "ColorControl"
		capability "Actuator"
        capability "Initialize"
		capability "Refresh" 
        capability "SwitchLevel"
        capability "LightEffects"
        capability "Configuration" 
        
        attribute "online", "string"
        attribute "mode", "number"
        attribute "modeValue", "number"
        attribute "modeDescription", "string"
        attribute "pollInterval", "number"         
        attribute "cloudAPI", "string"
        attribute "filterStatus", "string"  
        
        command "nightLighton_off", [[name: "Night Light", type: "ENUM", constraints: [ 'On',      'Off'] ] ]        
        command "setFanSpeed", [[name: "gearMode", type: "ENUM", constraints: [ 'Low',      'Medium',       'High'], description: "Default speed of Fan using GearMode"]]
        command "sleep"
        command "changeInterval", [[name: "changeInterval", type: "NUMBER",  description: "Change Polling interval range from 0-600", range: 0-600, required: true]]
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

// reset of device settings when preferences updated.
def updated() {
    unschedule()
    if (debugLog) runIn(1800, logsOff)
    poll()
    retrieveStateData()
}

// linital setup when device is installed.
def installed(){
    retrieveStateData()
    poll ()
}

// initialize devices upon install and reboot.
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
    retrieveStateData()    
    if (debugLog) runIn(1800, logsOff) 
}

////////////////////
// Helper methods //
////////////////////

// turn off logging for the device
def logsOff() {
    log.info "debug logging disabled..."
    device.updateSetting("debugLog", [value: "false", type: "bool"])
}

// retrieve device status
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

def setColorTemperature(value,level = null,transitionTime = null) {
    unschedule(fadeUp)
    unschedule(fadeDown)
    if (debugLog) { log.debug "setColorTemperature(): ${value}"}
    if (value < device.getDataValue("ctMin").toInteger()) { value = device.getDataValue("ctMin")}
    if (value > device.getDataValue("ctMax").toInteger()) { value = device.getDataValue("ctMax")}
    if (debugLog) { log.debug "setColorTemperate(): ColorTemp = " + value }
    cloudCT(value, level, transitionTime)
}

def setLevel(float v,duration = 0) {
    cloudSetLevel( v, 0)
}

def sleep() {
    log.debug "sleep(): Processing Working Mode command 'Auto' "
    sendEvent(name: "cloudAPI", value: "Pending")
    values = '{"workMode":5,"modeValue":0}'  // This is the string that will need to be modified based on the potential values
    sendCommand("workMode", values, "devices.capabilities.work_mode")
}

def setFanSpeed(gear) {
    log.debug "setFanSpeed(): Processing Working Mode command 'Auto' "
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

/* def workingMode(mode, gear, speed=0){
    log.debug "workingMode(): Processing Working Mode command. ${mode} ${gear}"
    sendEvent(name: "cloudAPI", value: "Pending")
    switch(mode){
        case "gearMode":
            modenum = 1;
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
                    default:
                    gear = 0;
                break;
                }
        break;
        case "Custom":
            modenum = 2;
            gear = speed;
        break;
        case "Auto":
            modenum = 3;
            gear = 0;
        break;
        case "Sleep":
            modenum = 5;
            gear = 0;
        break;
    default:
    log.debug "not valid value for mode";
    break;
    }
    values = '{"workMode":'+modenum+',"modeValue":'+gear+'}'
    sendCommand("workMode", values, "devices.capabilities.work_mode")
} */
