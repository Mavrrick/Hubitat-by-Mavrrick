/*
 * Govee Light Driver
 *
 * Calls URIs with HTTP GET for switch on or off
 * 
 */

import hubitat.helper.InterfaceUtils
import hubitat.helper.HexUtils
import groovy.transform.Field
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition(name: "Govee Light Driver", namespace: "mavrrick", author: "mavrrick", importUrl: "https://raw.githubusercontent.com/Mavrrick/Hubitat-by-Mavrrick/main/govee_driver.groovy") {
        capability "Actuator"
        capability "Switch"
        capability "SwitchLevel"
        capability "ColorTemperature"
        capability "ColorControl"
        capability "Refresh"
    }
}

preferences {
    section("URIs") {
//        input "onURI", "text", title: "On URI", required: false
        input(name: "authorization", type: "string", title:"Authorization", description: "Enter the Authorization string", displayDuringSetup: true)
//        input(name: "body2", type: "string", title:"enter string here", description: "enter string", displayDuringSetup: true)
        input(name: "deviceID", type: "string", title:"Device ID", description: "Device", displayDuringSetup: true)
        input(name: "deviceModel", type: "enum", title: "Please select the Govee Model Number", options: ["H6160", "H6163", "H6104", "H6109", "H6110", "H6117",
            "H6159", "H7021", "H7022", "H6086", "H6089", "H6182", "H6085",
            "H7014", "H5081", "H6188", "H6135", "H6137", "H6141", "H6142",
            "H6195", "H7005", "H6083", "H6002", "H6003",
            "H6148","H6052","H6143","H6144","H6050","H6199","H6054","H5001"], defaultValue: "POST", required: true, displayDuringSetup: true)
        input(name: "deviceContent", type: "enum", title: "Content-Type", options: getCtype(), defaultValue: "application/x-www-form-urlencoded", required: true, displayDuringSetup: true)
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

def getCtype() {
    def cType = []
    cType = ["application/x-www-form-urlencoded","application/json"]
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    if (logEnable) runIn(1800, logsOff)
}

def parse(String description) {
    if (logEnable) log.debug(description)
}


def on() {
    def goveeUrl = "https://developer-api.govee.com/v1/devices/control"
    def headers = [:] 
    headers.put("Govee-API-Key", authorization)
    headers.put("Content-Type", deviceContent)
    def body = [:]
    body.put("device", deviceID) 
    body.put("model", deviceModel)
    def gvCmd = [:]
    gvCmd.put("name", "turn")
    gvCmd.put("value", "on")
    body.put("cmd",gvCmd)
    if (logEnable) log.debug "Sending on put request to [${goveeUrl}] to turn on device"
    if (logEnable) log.debug "JSON String to Govee : ${body}"
    try {
        httpPutJson([uri: goveeUrl,
                  headers: headers,
                  body: body,
                  ignoreSSLIssues: "true"]) 
        { resp ->
            if (resp.success) {
                sendEvent(name: "switch", value: "on", isStateChange: true)
            }
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def off() {
    def goveeUrl = "https://developer-api.govee.com/v1/devices/control"
    def headers = [:] 
    headers.put("Govee-API-Key", authorization)
    headers.put("Content-Type", deviceContent)
    def body = [:]
    body.put("device", deviceID) 
    body.put("model", deviceModel)
    def gvCmd = [:]
    gvCmd.put("name", "turn")
    gvCmd.put("value", "off")
    body.put("cmd",gvCmd)
    if (logEnable) log.debug "Sending on put request to [${goveeUrl}] to turn on device"
    if (logEnable) log.debug "JSON String to Govee : ${body}"
    try {
        httpPutJson([uri: goveeUrl, headers: headers, body: body]) { resp ->
            if (resp.success) {
                sendEvent(name: "switch", value: "off", isStateChange: true)
            }
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to off failed: ${e.message}"
    }
}


def refresh() {
    def goveeUrl = "https://developer-api.govee.com/v1/devices"
    def headers = [:] 
    headers.put("Govee-API-Key", authorization)
    if (logEnable) log.debug "Sending on GET request to [${goveeUrl}] to retrieve Govee Devices"
    try {
        httpGet([uri: goveeUrl, headers: headers]) { resp ->
            if (resp.success) {
                sendEvent(name: "Govee", value: "refresh", isStateChange: true)
            }
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to refresh failed: ${e.message}"
    }
}

def setLevel(BigDecimal lev,BigDecimal duration=0)  {
    def goveeUrl = "https://developer-api.govee.com/v1/devices/control"
    def headers = [:] 
    headers.put("Govee-API-Key", authorization)
    headers.put("Content-Type", deviceContent)
    def body = [:]
    body.put("device", deviceID) 
    body.put("model", deviceModel)
    def gvCmd = [:]
    gvCmd.put("name", "brightness")
    gvCmd.put("value", lev)
    body.put("cmd",gvCmd)
    if (logEnable) log.debug "Sending on put request to [${goveeUrl}] to turn on device"
    if (logEnable) log.debug "JSON String to Govee : ${body}"
    try {
        httpPutJson([uri: goveeUrl, headers: headers, body: body]) { resp ->
            if (resp.success) {
                sendEvent(name: "Level", value: lev, isStateChange: true)
            }
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to off failed: ${e.message}"
    }
}

def setColorTemperature(ct)   {
    def goveeUrl = "https://developer-api.govee.com/v1/devices/control"
    def headers = [:] 
    headers.put("Govee-API-Key", authorization)
    headers.put("Content-Type", deviceContent)
    def body = [:]
    body.put("device", deviceID) 
    body.put("model", deviceModel)
    def gvCmd = [:]
    gvCmd.put("name", "colorTem")
    gvCmd.put("value", ct)
    body.put("cmd",gvCmd)
    if (logEnable) log.debug "Sending on put request to [${goveeUrl}] to turn on device"
    if (logEnable) log.debug "JSON String to Govee : ${body}"
    try {
        httpPutJson([uri: goveeUrl, headers: headers, body: body]) { resp ->
            if (resp.success) {
                sendEvent(name: "colorTemperature", value: ct, isStateChange: true)
            }
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to off failed: ${e.message}"
    }
}

def setColor(hsv) {
//    def rgb   
//      rgb = hubitat.helper.ColorUtils.hsvToRGB([hsv.hue, hsv.saturation, hsv.level])
    if (logEnable) log.debug "RGB value is  : ${rgb}"

    if(logEnabled) log.debug "Attempt to set RGB color w/NaN value - ignoring."
    def goveeUrl = "https://developer-api.govee.com/v1/devices/control"
    def headers = [:] 
    headers.put("Govee-API-Key", authorization)
    headers.put("Content-Type", deviceContent)
    def body = [:]
    body.put("device", deviceID) 
    body.put("model", deviceModel)
    def gvCmd = [:]
    gvCmd.put("name", "color")
    def clrmap = [:]
    def rgb
    rgb = hubitat.helper.ColorUtils.hsvToRGB([hsv.hue, hsv.saturation, hsv.level])
        if (logEnable) log.debug "RGB value is  : ${rgb}"
    put.clrmap("r", rgb[0])
    put.clrmap("g", rgb[1])
    put.clrmap("b", rgb[2])
    gvCmd.put("value", clrmap)
    body.put("cmd",gvCmd)
    if (logEnable) log.debug "Sending on put request to [${goveeUrl}] to turn on device"
    if (logEnable) log.debug "JSON String to Govee : ${body}"
    try {
        httpPutJson([uri: goveeUrl, headers: headers, body: body]) { resp ->
            if (resp.success) {
                sendEvent(name: "colorTemperature", value: ct, isStateChange: true)
            }
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to off failed: ${e.message}"
    }
//    WizCommandSet(["r":rgb[0],"g":rgb[1],"b":rgb[2]]) 
    
//    updateCurrentStatus(hsv,null,null)
}
