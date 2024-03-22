// Hubitat driver for Govee Plug, Switch driver using Cloud API
// Version 1.0.19
//
// 11-3-22  Initial release
// 11-20-22 Added code to udpate parent app for rate limit chnages.
// 11-21-22 Added retry cloud api Attribute and retry logic to ensure devices change state as expected.
// 12-19-22 Modifieid polling to properly allow a value of 0 for no polling
// 1-30-23  Added check to see if device is in Retry state and abort new commands until cleared.
// 4-4-23   API Update now possible
// 2023-4-7   Update Initialize and getDeviceStatus routine to reset CloudAPI Attribute

import hubitat.helper.InterfaceUtils
import hubitat.helper.HexUtils
import groovy.transform.Field
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.json.JsonBuilder

#include Mavrrick.Govee_Cloud_API

def commandPort() { "4003" }

metadata {
	definition(name: "Govee v2 Research Driver", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
        capability "Configuration"
		capability "Refresh"
        capability "Initialize"
        
        attribute "cloudAPI", "string"
        attribute "online", "string"
        
    }

	preferences {		
		section("Device Info") {
            input("pollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true, width:4)
            input(name: "lanControl", type: "bool", title: "Enable Local LAN control", description: "This is a advanced feature that only worked with some devices. Do not enable unless you are sure your device supports it", defaultValue: false)
            if (lanControl) {
            input("ip", "text", title: "IP Address", description: "IP address of your Govee light", required: false)}
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
    if (type=="ct") {
        if (debugLog) {log.debug "GoveeCommandBuilder(): Color temp action"}
        JsonBuilder cmd1 = new JsonBuilder() 
        cmd1.msg {
        cmd command1
        data {
            color {
            r 0
            g 0
            b 0
        }
            colorTemInKelvin value1}
    }
    def  command = cmd1.toString()
        if (debugLog) {log.debug "GoveeCommandBuilder():json output ${command}"}
  return command    
    }
   else if (type=="rgb") {
       if (debugLog) {log.debug "GoveeCommandBuilder(): rgb"}
        JsonBuilder cmd1 = new JsonBuilder() 
        cmd1.msg {
        cmd command1
        data {
            color {
            r value1.r
            g value1.g
            b value1.b
                
        }
            colorTemInKelvin 0}
    }
    def  command = cmd1.toString()
       if (debugLog) {log.debug "GoveeCommandBuilder():json output ${command}"}
  return command    
    }
       else if (type=="status") {
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
