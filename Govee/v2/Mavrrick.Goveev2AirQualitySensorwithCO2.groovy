// Hubitat driver for Govee Appliances using Cloud API
// Version 2.1.0
//
// 05/07/2024 2.1.0 update to support Nested devices under Parent devices

// Includes of library objects
#include Mavrrick.Govee_Cloud_API

import groovy.json.JsonSlurper

metadata {
	definition(name: "Govee v2 Air Quality Sensor with CO2", namespace: "Mavrrick", author: "Mavrrick") {
        capability "Initialize"
		capability "Refresh" 
        capability "TemperatureMeasurement"         
        capability "RelativeHumidityMeasurement"
        capability "CarbonDioxideMeasurement"
        attribute "online", "string"
        attribute "cloudAPI", "string"
        attribute "dewPoint", "number"
        attribute "vpd", "number"
    }

	preferences {		
		section("Device Info") {  
            input("tempUnit", "enum", title: "Temp Unit Selection", defaultValue: 'Fahrenheit', options: [    "Fahrenheit",     "Celsius"], required: true)
            input("pollRate", "number", title: "Polling Rate (seconds)\nDefault:300", defaultValue:300, submitOnChange: true, width:4)            
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
    poll()
}

// linital setup when device is installed.
def installed(){
    poll ()
}

// initialize devices upon install and reboot.
def initialize() {
    unschedule()
    if (debugLog) runIn(1800, logsOff)
    if (pollRate > 0) {
        pollRateInt = pollRate.toInteger()
        randomOffset(pollRateInt)
        runIn(offset,poll)
    }
}

// update data for the device
def refresh() {
    if (debugLog) {log.info "refresh(): Performing refresh"}
    unschedule(poll)
    poll()
    runCalculations()
    if (device.currentValue("connectionState") == "connected") {
    }
}

// retrieve setup values and initialize polling and logging
def configure() {
    if (debugLog) {log.info "configure(): Driver Updated"}
    unschedule()
    if (pollRate > 0) runIn(pollRate,poll)         
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

/**
 * Calculates the Saturation Vapor Pressure (SVP) in Pascals (Pa).
 * Uses the Magnus formula.
 * @param T_c Temperature in Celsius.
 * @return SVP in Pascals (Pa).
 */
private double calculateSVP(double T_c) {
    // Constants for the Magnus formula (for water vapor pressure over water/ice)
    // Over water (T >= 0 deg C)
    def A = 6.1078
    def B = 17.27
    def C = 237.3

    // Using Math.pow(a, b) or 'a**b' for exponentiation
    // SVP (hPa) = 6.1078 * exp((17.27 * T) / (T + 237.3))
    // SVP (Pa) = 100 * SVP (hPa)
    // SVP (Pa) = 610.78 * exp((17.27 * T_c) / (T_c + 237.3))
    return 610.78 * Math.exp((B * T_c) / (T_c + C))
}

/**
 * Calculates the Dew Point temperature (Td) in Celsius (°C).
 * Uses the simplified Goffs-Gratch/August-Roche-Magnus approximation.
 * @param T_c Temperature in Celsius.
 * @param RH_percent Relative Humidity as a percentage (e.g., 65).
 * @return Dew Point temperature in Celsius (°C).
 */
public double calculateDewPointC(double T_c, double RH_percent) {
    // Constants for the approximation
    def b = 17.27
    def c = 237.3

    // Convert RH to a fraction (e.g., 65 -> 0.65)
    def RH_frac = RH_percent / 100.0

    // Gamma parameter
    def gamma = (b * T_c) / (c + T_c) + Math.log(RH_frac)

    // Calculate Dew Point (Td_c)
    def Td_c = (c * gamma) / (b - gamma)
    
    return Td_c
}

/**
 * Converts Celsius to Fahrenheit.
 * @param T_c Temperature in Celsius.
 * @return Temperature in Fahrenheit (°F).
 */
private double cToF(double T_c) {
    return (T_c * 9 / 5) + 32
}

/**
 * Converts Fahrenheit to Celsius.
 * @param T_f Temperature in Fahrenheit.
 * @return Temperature in Celsius (°C).
 */
private double fToC(double T_f) {
    return (T_f - 32) * 5 / 9
}

/**
 * Calculates the Vapor Pressure Deficit (VPD) in kilopascals (kPa).
 * Assumes air temperature is the relevant temperature.
 * @param T_c Temperature in Celsius.
 * @param RH_percent Relative Humidity as a percentage (e.g., 65).
 * @return VPD in kilopascals (kPa).
 */

public double calculateVPD_kPa(double T_c, double RH_percent) {
    // 1. Calculate Saturation Vapor Pressure (SVP) in Pascals (Pa)
    def SVP_Pa = calculateSVP(T_c)
    
    // 2. Calculate Actual Vapor Pressure (AVP) in Pascals (Pa)
    // AVP = SVP * (RH / 100)
    def AVP_Pa = SVP_Pa * (RH_percent / 100.0)
    
    // 3. Calculate VPD in Pascals (Pa)
    // VPD = SVP - AVP
    def VPD_Pa = SVP_Pa - AVP_Pa
    
    // VPD = SVP * (1 - RH / 100) - This is the simplified formula

    // 4. Convert VPD to kilopascals (kPa) (1 kPa = 1000 Pa)
    def VPD_kPa = VPD_Pa / 1000.0
    
    return VPD_kPa
}

void runCalculations() {
    tempUnit = getTemperatureScale()
    tempInput = device.currentValue("temperature")
    rhInput = device.currentValue("humidity")
    
    // Ensure Temp is in Celsius for the calculations
    def T_c = tempUnit.toLowerCase() == "f" ? fToC(tempInput) : tempInput

    // 1. Calculate Dew Point
    def Td_c = calculateDewPointC(T_c, rhInput)
    def Td_f = cToF(Td_c)

    // 2. Calculate VPD
    def VPD_kPa = calculateVPD_kPa(T_c, rhInput)
    
    if (debugLog) {log.info "Input Temp: ${tempInput}°${tempUnit}, RH: ${rhInput}%"}
    if (debugLog) {log.info "Dew Point: ${Td_c.round(2)}°C / ${Td_f.round(2)}°F"}
    if (debugLog) {log.info "VPD: ${VPD_kPa.round(2)} kPa"}
    
    
   if (getTemperatureScale() == "C") sendEvent(name: "dewPoint", value: Td_c.round(2), unit: "C");
   if (getTemperatureScale() == "F") sendEvent(name: "dewPoint", value: Td_f.round(2), unit: "F");
    
    sendEvent(name: "vpd", value: VPD_kPa.round(2), unit: "kPa")
}
