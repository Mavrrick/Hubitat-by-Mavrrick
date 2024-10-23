// Hubitat driver for Govee Appliances using Cloud API
// Version 2.1.0
//
// 05/07/2024 2.1.0 update to support Nested devices under Parent devices

// Includes of library objects
#include Mavrrick.Govee_Cloud_API
//#include Mavrrick.Govee_Cloud_RGB
//#include Mavrrick.Govee_Cloud_Level
#include Mavrrick.Govee_Cloud_Life

import groovy.json.JsonSlurper
import groovy.transform.Field

@Field Map getFanLevel = [
    "off": 0
    ,"on": 1
	,"low": 25
	,"medium": 50
	,"high": 100
]

metadata {
	definition(name: "Govee v2 H7120 Air Purifier", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
		capability "Actuator"
        capability "Initialize"
		capability "Refresh" 
        capability "Configuration"
        capability "FanControl"
        capability "FilterStatus"
        
        attribute "online", "string"
        attribute "mode", "number"
        attribute "modeValue", "number"
        attribute "modeDescription", "string"
        attribute "pollInterval", "number"         
        attribute "cloudAPI", "string"
        attribute "filterLifeTime", "number"  
        
        command "sleepMode"
        command "setSpeed", [[name: "Fan speed*",type:"ENUM", description:"Fan speed to set", constraints: getFanLevel.collect {k,v -> k}]]
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
    if (getChildDevices().size() == 0) {
        if (device.getDataValue("commands").contains("nightlightToggle")) {
            runIn(10, addLightDeviceHelper)
        }
    }
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

// initialize devices upon install and reboot.
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

def sleepMode() {
    log.debug "sleep(): Processing Working Mode command 'Sleep' "
    sendEvent(name: "cloudAPI", value: "Pending")
    values = '{"workMode":5,"modeValue":0}'  // This is the string that will need to be modified based on the potential values
    sendCommand("workMode", values, "devices.capabilities.work_mode")
}

def setSpeed(fanspeed) {
    log.debug "setSpeed(): Processing Working Mode command 'setSpeed' to ${fanspeed}"
    sendEvent(name: "cloudAPI", value: "Pending")
    switch(fanspeed){
        case "low":
            gear = 1;
        break;
        case "medium":
            gear = 2;
        break;
        case "high":
            gear = 3;
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

///////////////////////////////////////////////////
// Heler routine to create child devices         //
///////////////////////////////////////////////////

def addLightDeviceHelper() {
	//Driver Settings
    driver = "Govee v2 Life Child Light Device"
    deviceID = device.getDataValue("deviceID")
    deviceName = device.label+"_Nightlight"
    deviceModel = device.getDataValue("deviceModel")
	Map deviceType = [namespace:"Mavrrick", typeName: driver]
	Map deviceTypeBak = [:]
	String devModel = deviceModel
	String dni = "Govee_${deviceID}_Nightlight"
    APIKey = device.getDataValue("apiKey")
	Map properties = [name: driver, label: deviceName, deviceID: deviceID, deviceModel: deviceModel, apiKey: APIKey]
//    log.debug "Setup detail '${properties}' driver failed"
    if (debugLog) { log.debug "Creating Child Device"}

	def childDev
	try {
		childDev = addChildDevice(deviceType.namespace, deviceType.typeName, dni, properties)
	}
	catch (e) {
		log.warn "The '${deviceType}' driver failed"
		if (deviceTypeBak) {
			logWarn "Defaulting to '${deviceTypeBak}' instead"
			childDev = addChildDevice(deviceTypeBak.namespace, deviceTypeBak.typeName, dni, properties)
		}
	} 
}

def retNightlightScene(){
    scenes = state.nightlightScene 
    if (debugLog) { log.debug "retNightlightScene(): Nightlight Scenes are  " + scenes }
    return scenes
}

