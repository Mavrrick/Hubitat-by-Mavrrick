// Hubitat driver for Govee Fan Driver using Cloud API
// Version 2.1.0
//
// 05/07/2024 2.1.0 update to support Nested devices under Parent devices

// Includes of library objects
#include Mavrrick.Govee_Cloud_API

import groovy.json.JsonSlurper 

metadata {
	definition(name: "Govee v2 Fan Driver", namespace: "Mavrrick", author: "Mavrrick") {
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
        command "workingMode", [[name: "mode", type: "ENUM", constraints: [ 'FanSpeed',      'Custom',       'Auto',    'Sleep',    'Nature'], description: "Mode of device"],
                          [name: "gearMode", type: "NUMBER", description: "Only used when mode is FanSpeed"]]
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

def workingMode(mode, gear){
    log.debug "workingMode(): Processing Working Mode command. ${mode} ${gear}"
    sendEvent(name: "cloudAPI", value: "Pending")
    switch(mode){
        case "FanSpeed":
            modenum = 1;
        break;
        case "Custom":
            modenum = 2;
            gear = 0;
        break;
        case "Auto":
            modenum = 3;
            gear = 0;
        break;
        case "Sleep":
            modenum = 5;
            gear = 0;
        break;
        case "Nature":
            modenum = 6;
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

