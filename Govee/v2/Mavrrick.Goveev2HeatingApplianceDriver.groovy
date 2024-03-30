// Hubitat driver for Govee Appliances using Cloud API
// Version 1.0.19
//
// 2022-11-03 Initial Driver release for Govee Heating Appliance devices
// 2022-11-20 Added a pending change condition and validation that the call was successful
// ---------- A retry of the last call will be attempted if rate limit is the cause for it failing
// ---------- Included code to update parent app for rate limit consumption.
// 2022-11-21 Moved status of cloud api call to the it's own attribute so it can be monitored easily
// 2022-12-19 Added Actuator capbility to more easily integrate with RM
// 2023-4-4   API key update now possible
// 2023-4-7   Update Initialize and getDeviceStatus routine to reset CloudAPI Attribute

// Includes of library objects
#include Mavrrick.Govee_Cloud_API
#include Mavrrick.Govee_Cloud_Life

import groovy.json.JsonSlurper 

metadata {
	definition(name: "Govee v2 Heating Appliance Driver", namespace: "Mavrrick", author: "Mavrrick") {
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
        attribute "airDeflector", "string"
        attribute "targetTemp", "string"

        command "airDeflectoron_off", [[name: "Air Deflector", type: "ENUM", constraints: ['On',      'Off'] ] ]
        command "workingMode", [[name: "mode", type: "ENUM", constraints: [ 'gearMode',      'Fan',       'Auto'], description: "Mode of device"],
                          [name: "gearMode", type: "ENUM", constraints: [ 'Low',      'Medium',       'High', 'N/A'], description: "Amount of heating"]]
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
    if (debugLog) runIn(1800, logsOff)
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
    device.updateSetting("logEnable", [value: "false", type: "bool"])
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

def workingMode(mode, gear){
    log.debug "workingMode(): Processing Working Mode command. ${mode} ${gear}"
    sendEvent(name: "cloudAPI", value: "Pending")
    switch(mode){
        case "gearMode":
            modenum = 1;
              switch(gear){
                  case "Low":
                      gearnum = 1;
                  break;
                  case "Medium":
                      gearnum = 2;
                  break;
                  case "High":
                      gearnum = 3;
                  break;
                  default:
                  gearnum = 0;
                  break;
              }
        break;
        case "Fan":
            modenum = 9;
            gearnum = 0
        break;
        case "Auto":
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