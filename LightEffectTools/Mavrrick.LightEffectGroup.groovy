// Hubitat driver for Light Effect Group Driver
// Version 2.1.0
//
// 05/07/2024 2.1.0 update to support Nested devices under Parent devices

// Includes of library objectse

import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

metadata {
	definition(name: "LightEffect Group", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
        capability "ColorControl"
		capability "Actuator"
        capability "Light"
        capability "Initialize" 
        capability "SwitchLevel"
        capability "LightEffects"
        capability "ColorMode" 
        capability "ColorTemperature"
        
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
    sendEvent(name: "effectNum", value: 0)
    sendEvent(name: "switch", value: "on")
}

def off() {
    parent.off()
    sendEvent(name: "effectNum", value: 0)
    sendEvent(name: "switch", value: "off")
}

def setEffect(effectnum) {
    parent.setEffect(effectnum)
    sendEvent(name: "effectNum", value: effectnum)
    sendEvent(name: "switch", value: "on")
    def jsonSlurper = new JsonSlurper()
    def scenes = jsonSlurper.parseText(device.currentValue("lightEffects"))
    sendEvent(name: "effectName", value: "${scenes."${effectnum}"}") 
}

def setLevel(brightness, transitiontime = 0){
    parent.setLevel(brightness, 0)
    sendEvent(name: "level", value: brightness)
}

def setColorTemperature(colortemperature, level = 0, transitionTime = 0){
    parent.setColorTemperature(colortemperature, level, transitionTime)
    sendEvent(name: "colorTemperature", value: colortemperature)
}

def setColor(colorMap){
    parent.setColor(colorMap)
}


def setNextEffect () {
    if (device.currentValue("effectNum") == '0' || device.currentValue("effectNum") == state.sceneCount) {
        setEffect(1)
    } else  {
        nextEffect = device.currentValue("effectNum").toInteger() + 1
        setEffect(nextEffect)
    }
}

def setPreviousEffect () {
    if (device.currentValue("effectNum") == '0' || device.currentValue("effectNum") == '1') {
        setEffect(21)
    } else  {
        nextEffect = device.currentValue("effectNum").toInteger() - 1
        setEffect(nextEffect)
    } 
}

def getlightEffects() {
    scenes = parent.buildLightEffectJson()
    def le = new groovy.json.JsonBuilder(scenes)
    sendEvent(name: "lightEffects", value: le)
    state.sceneCount = scenes.size()
}
