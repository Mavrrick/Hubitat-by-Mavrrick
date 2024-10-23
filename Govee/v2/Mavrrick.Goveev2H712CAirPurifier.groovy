// Hubitat driver for Govee H712C Air Purifier
// Version 2.0.0
//
// 05/07/2024 2.1.0 update to support Nested devices under Parent devices


// Includes of library objects
#include Mavrrick.Govee_Cloud_API
#include Mavrrick.Govee_Cloud_Life

import groovy.json.JsonSlurper 
import groovy.transform.Field

@Field Map getFanLevel = [
    "off": 0
    ,"on": 1
	,"low": 25
	,"medium": 50
	,"high": 100    
    ,"auto": 150
]

metadata {
	definition(name: "Govee v2 H712C Air Purifier", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
		capability "Actuator"
        capability "Initialize"
		capability "Refresh"
        capability "Configuration"
        capability "FanControl"
        
        attribute "online", "string"
        attribute "mode", "number"
        attribute "modeValue", "number"
        attribute "modeDescription", "string"
        attribute "pollInterval", "number"
        attribute "cloudAPI", "string"
        attribute "filterLifeTime", "number"        
        attribute "airQuality", "number"
        
        command "changeInterval", [[name: "changeInterval", type: "NUMBER",  description: "Change Polling interval range from 0-600", range: 0-600, required: true]]
        command "setSpeed", [[name: "Fan speed*",type:"ENUM", description:"Fan speed to set", constraints: getFanLevel.collect {k,v -> k}]]
        command "autoMode"
        command "sleepMode"
        command "turboMode"
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

// linital setup when device is installed.
def installed(){
    if (getChildDevices().size() == 0) {
        if (device.getDataValue("commands").contains("nightlightToggle")) {
            runIn(10, addLightDeviceHelper)
        }
    }
    sendEvent(name: "speed", value: "off")
    retrieveStateData()
    poll ()
}

Initialize // initialize devices upon install and reboot.
def initialize() {
    sendEvent(name: "supportedFanSpeeds", value: groovy.json.JsonOutput.toJson(getFanLevel.collect {k,v -> k}))
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

def turboMode() {
    log.debug "sleep(): Processing Working Mode command 'turboMode' "
    sendEvent(name: "cloudAPI", value: "Pending")
    values = '{"workMode":7,"modeValue":0}'  // This is the string that will need to be modified based on the potential values
    sendCommand("workMode", values, "devices.capabilities.work_mode")
}

def setSpeed(fanspeed) {
    log.debug "setFanSpeed(): Processing Working Mode command 'setFanSpeed' to ${fanspeed} "
    sendEvent(name: "cloudAPI", value: "Pending")
    switch(fanspeed){
        case "low":
            gearmode = 1;
            gear = 1;
        break;
        case "medium":
            gearmode = 1;
            gear = 2;
        break;
        case "high":
            gearmode = 1;
            gear = 3;
        break;
        case "auto":
            gearmode = 3;
            gear = 0;
        break;       
    }
    if (fanspeed == "on") {
        cloudOn()
        sendEvent(name: "speed", value: fanspeed)
    } else if (fanspeed == "off") {
        cloudOff()
        sendEvent(name: "speed", value: fanspeed)
    } else {
        values = '{"workMode":1,"modeValue":'+gear+'}'  // This is the string that will need to be modified based on the potential values
        sendCommand("workMode", values, "devices.capabilities.work_mode")
        sendEvent(name: "speed", value: fanspeed)
    }
}

void cycleSpeed() {
    cycleChange()
}

void cycleChange() {
    Integer randomSpeed = Math.abs(new Random().nextInt() % 3) + 1
//    String newSpeed = "speed "+randomSpeed
        values = '{"workMode":1,"modeValue":'+randomSpeed+'}'  // This is the string that will need to be modified based on the potential values
        sendCommand("workMode", values, "devices.capabilities.work_mode")
        sendEvent(name: "speed", value: "cycle")    
//    setSpeed(newSpeed)
    runIn(cycleInterval, cycleChange)
    
}
