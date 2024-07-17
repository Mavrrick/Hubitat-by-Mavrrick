// Hubitat driver for Govee Appliances Child Light Device
// Version 2.1.0
//
// 05/07/2024 2.1.0 update to support Nested devices under Parent devices

// Includes of library objects
#include Mavrrick.Govee_Cloud_API
#include Mavrrick.Govee_Cloud_RGB
#include Mavrrick.Govee_Cloud_Level
// #include Mavrrick.Govee_Cloud_Life

import groovy.json.JsonSlurper 

metadata {
	definition(name: "Govee v2 Life Child Light Device", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
        capability "ColorControl"
		capability "Actuator"
        capability "Light"
        capability "Initialize" 
        capability "SwitchLevel"
        capability "LightEffects"
        capability "ColorMode" 
        capability "LightEffects"
        
        attribute "online", "string"        
        attribute "cloudAPI", "string"
		attribute "colorName", "string"
        attribute "colorRGBNum", "number"
        attribute "effectNum", "integer" 
        
    }

	preferences {		
		section("Device Info") {
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

}

// linital setup when device is installed.
def installed(){
    retNightlightScenes()
}

// initialize devices upon install and reboot.
def initialize() {
     if (device.currentValue("cloudAPI") == "Retry") {
        if (debugLog) {log.error "initialize(): Cloud API in retry state. Reseting "}
        sendEvent(name: "cloudAPI", value: "Initialized")
    }
    unschedule()
    if (logEnable) runIn(1800, logsOff)
    retNightlightScenes()
}

// update data for the device
def refresh() {
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

def postEvent(evt = 50, value = 50){
    sendEvent(name: evt, value: value)
}

def retNightlightScenes() {
    state.remove("scenes")
    state.remove("nightlightScene") 
    state.scenes = [:]
    scenes = parent.retNightlightScene()
    if (debugLog) {log.info "retNightlightScenes(): ${scenes}"}
    scenes.each() {
        if (debugLog) {log.info "retNightlightScenes(): ${it.name}, ${it.value}"}
        state.scenes.put(it.value,it.name)         
    }
}

//////////////////////
// Driver Commands // 
/////////////////////

def on() {
        nightLighton_off("On")
}

def off() {
        nightLighton_off("Off")
}

def setLevel(float v,duration = 0) {
    cloudSetLevel( v, 0)
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

def  setEffect(effectNo) {
                log.debug ("setEffect(): Setting effect via cloud api to scene number  ${effectNo}")
                sendCommand("nightlightScene", effectNo,"devices.capabilities.mode")
                   
}

/*
def setNextEffect() {
        if (debugLog) {log.debug ("setNextEffect(): current Name ${state.nightlightScene.get(state.sceneValue).name} value ${state.nightlightScene.get(state.sceneValue).value}")}
            if (state.sceneValue == 0) {
            if (debugLog) {log.debug ("setNextEffect(): Current scene value is 0 setting to first scene in list")}
            setEffect(state.nightlightScene.get(state.sceneValue).value)
            state.sceneValue = state.sceneValue + 1
        } else {
            if (debugLog) {log.debug ("setNextEffect(): Increment to next scene")}
            setEffect(state.nightlightScene.get(state.sceneValue).value)
            state.sceneValue = state.sceneValue + 1
        }  
} 
      
def setPreviousEffect() {
        if (debugLog) {log.debug ("setPreviousEffect(): current Name ${state.nightlightScene.get(state.sceneValue).name} value ${state.nightlightScene.get(state.sceneValue).value}")}
            if (state.sceneValue == 0) {
            if (debugLog) {log.debug ("setPreviousEffect(): Current scene value is 0 setting to first scene in list")}
            setEffect(state.nightlightScene.get(state.sceneValue).value)
            state.sceneValue = state.sceneMax 
        } else {
            if (debugLog) {log.debug ("setPreviousEffect(): Increment to next scene")}
            setEffect(state.nightlightScene.get(state.sceneValue).value)
            state.sceneValue = state.sceneValue - 1
        }          
} 
*/

def setNextEffect () {
        keys = []
        names = []
        keys.addAll(state.scenes.keySet())
        names.addAll(state.scenes.values())
//        if (debugLog) {log.debug ("setNextEffect(): current Name ${names.get(state.sceneValue)} value ${keys.get(state.sceneValue)}")}
        if (state.sceneValue == state.sceneMax) state.sceneValue = 0  
        if (state.sceneValue == 0) {
            if (debugLog) {log.debug ("setNextEffect(): Current scene value is 0 setting to first scene in list")}
            setEffect(keys.get(state.sceneValue))
            state.sceneValue = state.sceneValue + 1
        } else {
            if (debugLog) {log.debug ("setNextEffect(): Increment to next scene")}
            setEffect(keys.get(state.sceneValue))
            state.sceneValue = state.sceneValue + 1
        }  
}

def setPreviousEffect () {
        keys = []
        names = []
        keys.addAll(state.scenes.keySet())
        names.addAll(state.scenes.values())
//        if (debugLog) {log.debug ("setPreviousEffect(): current Name ${names.get(state.sceneValue)} value ${keys.get(state.sceneValue)}")}       
            if (state.sceneValue == 0) {
            if (debugLog) {log.debug ("setPreviousEffect(): Current scene value is 0 setting to first scene in list")}
            setEffect(keys.get(state.sceneValue))
            state.sceneValue = state.sceneMax-1 
        } else {
            if (debugLog) {log.debug ("setPreviousEffect(): Increment to next scene")}
            setEffect(keys.get(state.sceneValue))
            state.sceneValue = state.sceneValue - 1
        }          
}

