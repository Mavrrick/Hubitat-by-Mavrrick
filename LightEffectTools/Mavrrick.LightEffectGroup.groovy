// Hubitat driver for Govee Appliances Child Light Device
// Version 2.1.0
//
// 05/07/2024 2.1.0 update to support Nested devices under Parent devices

// Includes of library objectse

import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

metadata {
	definition(name: "LightEffect Group ", namespace: "Mavrrick", author: "Mavrrick") {
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
//    hue, saturation, color, colorName, level
    sendEvent(name: "hue", value: 0)
    sendEvent(name: "saturation", value: 100)
    sendEvent(name: "colorName", value: "Red")
    sendEvent(name: "level", value: 100)
    getlightEffects()
}

// initialize devices upon install and reboot.
def initialize() {
    getlightEffects()

}

// update data for the device
def refresh() {
}

// retrieve setup values and initialize polling and logging
def configure() {
    if (debugLog) {log.info "configure(): Driver Updated"}
    unschedule()  
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


//////////////////////
// Driver Commands // 
/////////////////////

def on() {
    parent.on()
}

def off() {
    parent.off()
}

def setLevel(float v,duration = 0) {
    cloudSetLevel( v, 0)
}

def setEffect(effectnum) {
    parent.setEffect(effectnum)
}

def setLevel(brightness, transitiontime = 0){
    parent.setColor(brightness, 0)
}

def setColor(colorMap){
    parent.setColor(colorMap)
}


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

def getlightEffects() {
    scenes = parent.buildLightEffectJson()
        def le = new groovy.json.JsonBuilder(scenes)
    sendEvent(name: "lightEffects", value: le)
}
