// Hubitat driver for Govee v2 H7133 Appliances using Cloud API
// Version 2.1.0
//
// 05/07/2024 2.1.0 update to support Nested devices under Parent devices

// Includes of library objects
#include Mavrrick.Govee_Cloud_API
#include Mavrrick.Govee_Cloud_Life

import groovy.json.JsonSlurper 

metadata {
	definition(name: "Govee v2 H7133 Heating Appliance Driver", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
		capability "Actuator"
        capability "Initialize"
		capability "Refresh" 
        capability "TemperatureMeasurement"
        capability "Configuration"
        capability "ThermostatHeatingSetpoint"

        attribute "online", "string"
        attribute "mode", "number"
        attribute "modeValue", "number"
        attribute "modeDescription", "string"
        attribute "pollInterval", "number"  
        attribute "cloudAPI", "string"
        attribute "online", "string"
        attribute "airDeflector", "string"
        attribute "targetTemp", "string"

        command "airDeflectoron_off", [[name: "Air Deflector", type: "ENUM", constraints: ['On',      'Off'] ] ]
        command "heatingMode", [[name: "mode", type: "ENUM", constraints: [ 'low',      'medium',       'high',      'fan',       'auto'], description: "Mode of device"]]
        command "targetTemperature", [[type: "NUMBER", description: "Entered your desired temp. Celsius range is 40-100, Fahrenheit range is 104-212", required: true],
            [name: "unit", type: "ENUM", constraints: [ 'Celsius',      'Fahrenheit'],  description: "Celsius or Fahrenheit", defaultValue: "Celsius", required: true],
            [name: "autoStop", type: "ENUM", constraints: [ 'Auto Stop',      'Maintain'],  description: "Stop Mode", defaultValue: "Maintain", required: true]]
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
            addLightDeviceHelper()
        }
    }
    poll()
}

Installed // linital setup when device is installed.
def installed(){
    retrieveStateData()
    if (getChildDevices().size() == 0) {
        if (device.getDataValue("commands").contains("nightlightToggle")) {
        addLightDeviceHelper()
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
    if (debugLog) runIn(1800, logsOff)
    pollRateInt = pollRate.toInteger()
    randomOffset(pollRateInt)
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

def targetTemperature(setpoint, unit, autostop) {
    if (autostop == "Auto Stop") { autoStopVal = 1}
    if (autostop == "Maintain") { autoStopVal = 0}                                  
    values = '{"autoStop": '+autoStopVal+',"temperature": '+setpoint+',"unit": "'+unit+'"}'
    sendCommand("targetTemperature", values, "devices.capabilities.temperature_setting")
}

def setHeatingSetpoint(temperature) {
    values = '{"autoStop": 0,"temperature": '+temperature+',"unit": "Celsius"}'
    sendCommand("targetTemperature", values, "devices.capabilities.temperature_setting")
}

def workingMode(mode, gear){
    log.debug "workingMode(): Processing Working Mode command. ${mode} ${gear}"
    sendEvent(name: "cloudAPI", value: "Pending")
    switch(mode){
        case "low":
            modenum = 1;
            gearnum = 1;
        break;
        case "medium":
            modenum = 1;
            gearnum = 2;
        break;
        case "high":
             modenum = 1;
             gearnum = 3;
        break;
        case "fan":
            modenum = 9;
            gearnum = 0
        break;
        case "auto":
            modenum = 3;
            gearnum = 0
        break;
    default:
    log.debug "not valid value for mode";
    break;
    }

    values = '{"workMode":'+modenum+',"modeValue":'+gearnum+'}'
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

