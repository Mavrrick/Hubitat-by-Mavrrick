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

#include Mavrrick.Govee_Cloud_API
//#include Mavrrick.Govee_Cloud_RGB
//#include Mavrrick.Govee_Cloud_Level

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
    }

	preferences {		
		section("Device Info") {
            input("pollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true, width:4)            
            input(name: "debugLog", type: "bool", title: "Debug Logging", defaultValue: false)
		}
		
	}
}

def parse(String description) {

}

def on() {
         if (device.currentValue("cloudAPI") == "Retry") {
             log.error "on(): CloudAPI already in retry state. Aborting call." 
         } else {
        sendEvent(name: "cloudAPI", value: "Pending")
	    sendCommand("powerSwitch", 1 ,"devices.capabilities.on_off")
            }
}

def off() {
        if (device.currentValue("cloudAPI") == "Retry") {
             log.error "off(): CloudAPI already in retry state. Aborting call." 
         } else {
        sendEvent(name: "cloudAPI", value: "Pending")
	    sendCommand("powerSwitch", 0 ,"devices.capabilities.on_off")
            }
} 

def nightLighton_off(evt) {
    log.debug "nightLighton_off(): Processing Night Light command. ${evt}"
        if (device.currentValue("cloudAPI") == "Retry") {
             log.error "nightLighton_off(): CloudAPI already in retry state. Aborting call." 
         } else {
        sendEvent(name: "cloudAPI", value: "Pending")
            if (evt == "On") sendCommand("nightlightToggle", 1 ,"devices.capabilities.toggle")
            if (evt == "Off") sendCommand("nightlightToggle", 0 ,"devices.capabilities.toggle")
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
        break;
        case "Fan":
            modenum = 9;
        break;
        case "Auto":
            modenum = 3;
        break;
    default:
    log.debug "not valid value for mode";
    break;
    }
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
    values = '{"workMode":'+modenum+',"modeValue":'+gearnum+'}'
    sendCommand("workMode", values, "devices.capabilities.work_mode")
}    

private def sendCommandLegacy(command, payload) {


     def params = [
            uri   : "https://developer-api.govee.com",
            path  : '/v1/appliance/devices/control',
			headers: ["Govee-API-Key": device.getDataValue("apiKey"), "Content-Type": "application/json"],
            contentType: "application/json",      
			body: [device: device.getDataValue("deviceID"), model: device.getDataValue("deviceModel"), cmd: ["name": command, "value": payload]],
        ]
    

try {

			httpPut(params) { resp ->
				
                if (debugLog) {log.debug "response.data="+resp.data}
                if (debugLog) {log.debug "response.data=" + resp.data.code }
                if (resp.data.code == 200 && command == "turn") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "switch", value: payload)
                    }    
                else if (resp.data.code == 200 && command == "mode") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "switch", value: "on")
                    retrieveCmd(command, payload)
                    }        
                resp.headers.each {
                    if (debugLog) {log.debug "${it.name}: ${it.value}"}                    
                    name = it.name
                    value=it.value
                    if (name == "X-RateLimit-Remaining") {
                        state.DailyLimitRemaining = value
                        parent.apiRateLimits("DailyLimitRemainingV2", value)
                    }
                    if (name == "API-RateLimit-Remaining") {
                        state.MinRateLimitRemainig = value
                        parent.apiRateLimits("MinRateLimitRemainigV2", value)
                    }
            }
				return resp
		}
	} catch (groovyx.net.http.HttpResponseException e) {
        log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}"
        if (e.statusCode == 429) {
            log.error "sendCommand():Cloud API Returned code 429, Rate Limit exceeded. Attempting again in one min."
            sendEvent(name: "cloudAPI", value: "Retry")
            pauseExecution(60000)
            sendCommand(command, payload)
        } 
        else {
          log.error "sendCommand():Unknwon Error. Attempting again in one min." 
            sendEvent(name: "cloudAPI", value: "Retry")
            pauseExecution(60000)
            sendCommand(command, payload)
        }    
		return 'unknown'
	}
}


def updated() {
if (logEnable) runIn(1800, logsOff)
retrieveStateData()
}


def installed(){
    if (pollRate > 0) runIn(pollRate,poll)
    getDeviceState()
}

def initialize() {
    if (device.currentValue("cloudAPI") == "Retry") {
        if (debugLog) {log.error "initialize(): Cloud API in retry state. Reseting "}
        sendEvent(name: "cloudAPI", value: "Initialized")
    }
        unschedule(poll)
        if (pollRate > 0) runIn(pollRate,poll)
        getDeviceState()
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def poll() {
    if (debugLog) {log.warn "poll(): Poll Initated"}
	refresh()
}

def refresh() {
    if (debugLog) {log.warn "refresh(): Performing refresh"}
    unschedule(poll)
    if (pollRate > 0) runIn(pollRate,poll)
    getDeviceState()
    if (debugLog) runIn(1800, logsOff)
}

def configure() {
    if (debugLog) {log.warn "configure(): Driver Updated"}
    unschedule()
    if (pollRate > 0) runIn(pollRate,poll)     
    retrieveStateData()    
    if (debugLog) runIn(1800, logsOff) 
}
