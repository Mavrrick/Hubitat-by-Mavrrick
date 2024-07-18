// Hubitat driver for Govee Appliances using Cloud API
// Version 2.1.0
//
// 05/07/2024 2.1.0 update to support Nested devices under Parent devices


// Includes of library objects
#include Mavrrick.Govee_Cloud_API
#include Mavrrick.Govee_Cloud_Life

import groovy.json.JsonSlurper 

metadata {
	definition(name: "Govee v2 Air Purifier Driver", namespace: "Mavrrick", author: "Mavrrick") {
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
        attribute "filterStatus", "string"    
        
        command "workingMode", [[name: "mode", type: "ENUM", constraints: [ 'gearMode',      'Custom',       'Auto',       'Sleep'], description: "Mode of device"],
                                [name: "gearMode", type: "ENUM", constraints: [ 'Low',      'Medium',       'High', 'N/A'], description: "Default speed of Fan using GearMode. Ignored in any other mode"],
                                [name: "speed",  type: "INTEGER", description: "This will adjust your fan speed when using custom Mode.", default: 0]]        
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

Update // reset of device settings when preferences updated.
def updated() {
    unschedule()
    if (debugLog) runIn(1800, logsOff)
    retrieveStateData()
    if (getChildDevices().size() == 0) {
        if (device.getDataValue("commands").contains("nightlightToggle")) {
        runIn(10, addLightDeviceHelper)
        }
    }
    poll()
}

Installed // linital setup when device is installed.
def installed(){
    retrieveStateData()
    if (getChildDevices().size() == 0) {
        if (device.getDataValue("commands").contains("nightlightToggle")) {
        runIn(10, addLightDeviceHelper)
        }
    }
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

def workingMode(mode, gear, speed=0){
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
