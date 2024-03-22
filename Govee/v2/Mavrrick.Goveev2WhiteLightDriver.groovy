// Hubitat driver for Govee Light, Plug, Switch driver using Cloud API
// Version 1.0.19
//
// 11-5-22  Initial release
// 11-21-22- Added a pending change condition and validation that the call was successful
// ----------- A retry of the last call will be attempted if rate limit is the cause for it failing
// 12-19-22 Modifieid polling to properly allow a value of 0 for no polling
// 1-30-23  Added check to see if device is in Retry state and abort new commands until cleared.
// 4-4-23   API Key update  possible
// 2023-4-7   Update Initialize and getDeviceStatus routine to reset CloudAPI Attribute


import hubitat.helper.InterfaceUtils
import hubitat.helper.HexUtils
import groovy.transform.Field
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.json.JsonBuilder

#include Mavrrick.Govee_Cloud_API
#include Mavrrick.Govee_Cloud_Level

def commandPort() { "4003" }

metadata {
	definition(name: "Govee v2 White Light Driver", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
		capability "Light"
		capability "SwitchLevel"
		capability "Refresh"
        capability "Initialize"
        
        attribute "cloudAPI", "string"
        attribute "online", "string"        
        
    }

	preferences {		
		section("Device Info") {
            input("pollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true, width:4)
			input(name: "aRngBright", type: "bool", title: "Alternate Brightness Range", description: "For devices that expect a brightness range of 0-254", defaultValue: false)
            input(name: "lanControl", type: "bool", title: "Enable Local LAN control", description: "This is a advanced feature that only worked with some devices. Do not enable unless you are sure your device supports it", defaultValue: false)
            if (lanControl) {
            input("ip", "text", title: "IP Address", description: "IP address of your Govee light", required: false)
            input("fadeInc", "decimal", title: "% Change each Increment of fade", defaultValue: 1)
            }
            input(name: "debugLog", type: "bool", title: "Debug Logging", defaultValue: false)
		}
		
	}
}

def on() {
    if (lanControl) {
        sendCommandLan(GoveeCommandBuilder("turn",1, "turn"))
        sendEvent(name: "switch", value: "on")}
    else {
         if (device.currentValue("cloudAPI") == "Retry") {
             log.error "on(): CloudAPI already in retry state. Aborting call." 
         } else {
        sendEvent(name: "cloudAPI", value: "Pending")
	    sendCommand("powerSwitch", 1 ,"devices.capabilities.on_off")
            }
        }
}

def off() {
    if (lanControl) {
        sendCommandLan(GoveeCommandBuilder("turn",0, "turn"))
        sendEvent(name: "switch", value: "off")}
    else {
        if (device.currentValue("cloudAPI") == "Retry") {
             log.error "off(): CloudAPI already in retry state. Aborting call." 
         } else {
        sendEvent(name: "cloudAPI", value: "Pending")
	    sendCommand("powerSwitch", 0 ,"devices.capabilities.on_off")
            }
        }
}

private def sendCommandLegacy(String command, payload) {


     def params = [
            uri   : "https://developer-api.govee.com",
            path  : '/v1/devices/control',
			headers: ["Govee-API-Key": device.getDataValue("apiKey"), "Content-Type": "application/json"],
            contentType: "application/json",      
			body: [device: device.getDataValue("deviceID"), model: device.getDataValue("deviceModel"), cmd: ["name": command, "value": payload]],
        ]
    

try {

			httpPut(params) { resp ->
				
                if (debugLog) { log.debug "sendCommand(): response.data="+resp.data}
                code = resp.data.code
                if (code == 200 && command == "turn") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "switch", value: payload)
                    } 
                 else if (code == 200 && command == "brightness") {
                    sendEvent(name: "cloudAPI", value: "Success")
                    sendEvent(name: "switch", value: "on")
                     sendEvent(name: "level", value: payload)
                    }
                resp.headers.each {
                    if (debugLog) { log.debug "sendCommand(): ${it.name}: ${it.value}" }                   
                    name = it.name
                    value=it.value
                    if (name == "X-RateLimit-Remaining") {
                        state.DailyLimitRemaining = value
                        parent.apiRateLimits("DailyLimitRemaining", value)
                    }
                    if (name == "API-RateLimit-Remaining") {
                        state.MinRateLimitRemainig = value
                        parent.apiRateLimits("MinRateLimitRemainig", value)
                    }
            }
                return resp.data
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}"
        if (debugLog) {log.debug "sendCommand(): ${resp.header}"}
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


def poll() {
    if (debugLog) {log.warn "poll(): Poll Initated"}
	refresh()
}

def refresh() {
    if (debugLog) {log.warn "refresh(): Performing refresh"}
    if(device.getDataValue("retrievable") =='true'){
        if (debugLog) {log.warn "refresh(): Device is retrievable. Setting up Polling"}
        unschedule(poll)
        if (pollRate > 0) runIn(pollRate,poll)
        getDeviceState()
    }
    if (debugLog) runIn(1800, logsOff)
}

def updated() {
    configure()
}

def configure() {
    if (debugLog) {log.warn "configure(): Configuration Changed"}
    if(device.getDataValue("retrievable") =='true'){
        if (debugLog) {log.warn "configure(): Device is retrievable. Setting up Polling"}
        unschedule()
        if (pollRate > 0) runIn(pollRate,poll)
        getDeviceState()
    }
    if (debugLog) runIn(1800, logsOff) 
}

def initialize(){
    if (debugLog) {log.warn "initialize(): Driver Initializing"}
    if (device.currentValue("cloudAPI") == "Retry") {
        if (debugLog) {log.error "initialize(): Cloud API in retry state. Reseting "}
        sendEvent(name: "cloudAPI", value: "Initialized")
    }
    if(device.getDataValue("retrievable") =='true'){
        if (debugLog) {log.warn "initialize(): Device is retrievable. Setting up Polling"}
        unschedule()
        if (pollRate > 0) runIn(pollRate,poll)
        getDeviceState()
    }
    if (debugLog) runIn(1800, logsOff)
}


def installed(){
    if (debugLog) {log.warn "installed(): Driver Installed"}
    if(device.getDataValue("commands").contains("color")) {
        sendEvent(name: "hue", value: 0)
        sendEvent(name: "saturation", value: 100)
        sendEvent(name: "level", value: 100) }
    if(device.getDataValue("retrievable") =='true'){
        if (debugLog) {log.warn "installed(): Device is retrievable. Setting up Polling"}
        unschedule()
        if (pollRate > 0) runIn(pollRate,poll)
        getDeviceState()
    }
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("debugLog", [value: "false", type: "bool"])
}

def GoveeCommandBuilder(String command1, value1, String type) {   
    if (type=="status") {
           if (debugLog) {log.debug "GoveeCommandBuilder():status"}
        JsonBuilder cmd1 = new JsonBuilder() 
        cmd1.msg {
        cmd command1
        data {
            }
    }
    def  command = cmd1.toString()
           if (debugLog) {log.debug "GoveeCommandBuilder():json output ${command}"}
  return command    
    }
    else { 
        if (debugLog) {log.debug "GoveeCommandBuilder():other action"}
    JsonBuilder cmd1 = new JsonBuilder() 
        cmd1.msg {
        cmd command1
        data {
            value value1}
        }
    def  command = cmd1.toString()
        if (debugLog) {log.debug "GoveeCommandBuilder():json output ${command}"}
  return command
}
}

def sendCommandLan(String cmd) {
  def addr = getIPString();
    if (debugLog) {log.debug ("sendCommandLan(): ${cmd}")}

  pkt = new hubitat.device.HubAction(cmd,
                     hubitat.device.Protocol.LAN,
                     [type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,
                     ignoreResponse    : false,
                     callback: parse,
                     parseWarning: true,
                     destinationAddress: addr])  
  try {    
      if (debugLog) {log.debug("sendCommandLan(): ${pkt} to ip ${addr}")}
    sendHubCommand(pkt) 
    
  }
  catch (Exception e) {      
      logDebug e
  }      
}

def getIPString() {
   return ip+":"+commandPort()
}


def parse(message) {  
  log.error "Got something to parseUDP"
  log.error "UDP Response -> ${message}"    
}
