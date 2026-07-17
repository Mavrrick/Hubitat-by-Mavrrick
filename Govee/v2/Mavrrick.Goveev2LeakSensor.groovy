// Hubitat driver for Govee Water Leak Sensor using Cloud API
// Version 2.1.0
//
// 05/07/2024 2.1.0 update to support Nested devices under Parent devices

#include Mavrrick.Govee_Cloud_API
#include Mavrrick.Govee_Cloud_MQTT

import groovy.json.JsonSlurper 

metadata {
    definition (name: "Govee v2 Leak Sensor", namespace: "Mavrrick", author: "Mavrrick") {
        capability "WaterSensor"
        capability "MotionSensor"
        capability "PresenceSensor"
        capability "Sensor"
        
        attribute "probeBottom", "string"
        attribute "probeTop", "string"
        attribute "lastUpdate", "string"

        command "testWet"
        command "testDry"
    }

    preferences {
        input name: "descLog", type: "boolean", title: "Enable descriptionText logging", defaultValue: true
        input name: "debugLog", type: "boolean", title: "Enable debug logging", defaultValue: false
    }
}

// MQTT GATEWAY: Called by Govee parent driver
def mqttPost(String instance, String state) {
    if (debugLog) { log.debug "mqttPost() received in child. Instance: ${instance} | State: ${state}" }
    parseLeakState(instance, state)
}

// Backup Status Update handler
def statusUpdate(Map descMap) {
    if (debugLog) { log.debug "statusUpdate(): Received map: ${descMap}" }
    parseLeakState(descMap.instance, descMap.state ?: "")
}

// Core Parsing Engine
private void parseLeakState(String instance, String stateData) {
    if (instance == "bodyAppearedEvent" || instance == "waterLeakEvent") {
        
        // Normalize the string to uppercase so spelling/hyphen mismatches don't break logic
        def normalizedState = stateData.toUpperCase()
        
        // 1. Determine if it is WET
        def isWet = (normalizedState.contains("LEAKED") && !normalizedState.contains("UN_LEAKED")) || 
                    normalizedState.contains("VALUE:1") || 
                    normalizedState.contains("BOT:1") || 
                    normalizedState.contains("TOP:1")
        
        // 2. Explicitly override to DRY if we see any clear dry signals
        if (normalizedState.contains("UN_LEAKED") || 
            normalizedState.contains("VALUE:2") || 
            normalizedState.contains("VALUE:0") || 
            normalizedState.contains("ABSENCE") || 
            (normalizedState.contains("BOT:0") && normalizedState.contains("TOP:0"))) {
            isWet = false
        }
        
        // Set up standard Hubitat states
        def waterStatus = isWet ? "wet" : "dry"
        def motionStatus = isWet ? "active" : "inactive"
        def presenceStatus = isWet ? "present" : "not present"
        
        // Parse individual probes for diagnostic attributes
        def botState = "dry"
        def topState = "dry"
        if (normalizedState.contains("BOT:1")) { botState = "wet" }
        if (normalizedState.contains("TOP:1")) { topState = "wet" }
        
        // Send events if state has actually changed
        if (device.currentValue("water") != waterStatus) {
            if (descLog) log.warn "${device.displayName} has transitioned to ${waterStatus.toUpperCase()}!"
            sendEvent(name: "water", value: waterStatus, descriptionText: "Water is ${waterStatus}", isStateChange: true)
        }
        
        sendEvent(name: "motion", value: motionStatus, isStateChange: true)
        sendEvent(name: "presence", value: presenceStatus, isStateChange: true)
        sendEvent(name: "probeBottom", value: botState)
        sendEvent(name: "probeTop", value: topState)
        sendEvent(name: "lastUpdate", value: new Date().format("MM/dd/yyyy HH:mm:ss"))
    }
}

// Manual Testing Commands
def testWet() {
    log.info "Manually forcing WET state"
    parseLeakState("waterLeakEvent", "LEAKED")
}

def testDry() {
    log.info "Manually forcing DRY state"
    parseLeakState("waterLeakEvent", "UN_LEAKED")
}

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
    if (descLog) log.info "Initializing Govee v2 Leak Sensor..."
}