// Hubitat driver for Govee Appliances using Cloud API
// Version 1.0.20
//
// 2022-11-03 Initial Driver release for Govee Heating Appliance devices
// 2022-11-20 Added a pending change condition and validation that the call was successful
// ---------- A retry of the last call will be attempted if rate limit is the cause for it failing
// ---------- Included code to update parent app for rate limit consumption.
// 2022-11-21 Moved status of cloud api call to the it's own attribute so it can be monitored easily
// 2022-12-19 Added Actuator capbility to more easily integrate with RM
// 2023-4-4   API key update now possible
// 2023-4-7   Update Initialize and getDeviceStatus routine to reset CloudAPI Attribute
// 2024-4-10  Added polling on/off time periods, modeDescription, activePollingPeriod, lastPollActivity, stopAllPolling command, bug fixes for missing gear value (SanderSoft)

#include Mavrrick.Govee_Cloud_API
import groovy.time.TimeCategory

//#include Mavrrick.Govee_Cloud_RGB
//#include Mavrrick.Govee_Cloud_Level


metadata {
	definition(name: "Govee v2 Kettle Driver", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Switch"
		capability "Actuator"
        capability "Initialize"
		capability "Refresh"
        capability "TemperatureMeasurement"
        capability "Configuration"

        attribute "activePollingPeriod", "string"
        attribute "lastPollActivity", "string"
        attribute "online", "string"
        attribute "mode", "number"
        attribute "modeDescription", "enum", ['Unknown','DIY','Boiling','Tea','Coffee']
        attribute "cloudAPI", "string"
        attribute "online", "string"
        attribute "tempSetPoint", "string"

        command "stopAllPolling"
        command "workingMode", [[name: "mode", type: "ENUM", constraints: [ 'DIY',      'Boiling',       'Tea',   'Coffee'], description: "Mode of device"],
            [name: "gearMode", type: "NUMBER",  description: "Mode Value", range: 1..4, required: false]]
        command "tempSetPoint", [[type: "NUMBER", description: "Entered your desired temp. Celsius range is 40-100, Fahrenheit range is 104-212", required: true],
            [name: "unit", type: "ENUM", constraints: [ 'Celsius',      'Fahrenheit'],  description: "Celsius or Fahrenheit", defaultValue: "Celsius", required: true]]
    }

	preferences {
        section("Device Info and Preferences") {
            input(name: "pollRate", type: "number", title: "<b>Polling Rate</b> (seconds)\nDefault:300 seconds.", description: "Minimum level is 15 seconds.", required: true, defaultValue:300, submitOnChange: true, width:4)
            input(name: "pollStartDateTime", type: "time", title: "<b>Start time for the Polling Rate period.</b>.", required: true, description: "When to start polling the kettle." , submitOnChange: true)
            input(name: "pollEndDateTime"  , type: "time", title: "<b>End time for the Polling Rate period.</b>."  , required: true, description: "When to end polling the kettle."   , submitOnChange: true)
            input(name: "debugLog", type: "bool", title: "Debug Logging for 30 minutes", defaultValue: false)
        }
    }
		}

Boolean isValidPollingTime() {
    boolean isPollingPeriod = timeOfDayIsBetween(toDateTime(pollStartDateTime), toDateTime(pollEndDateTime), new Date())
}

void stopAllPolling() {
    log.warn "stopAllPolling(): All scheduled polling jobs have been UNscheduled"
    unschedule()
    if (debugLog) runIn(1800, logsOff)
}

void setPollingPeriodStateON() {
    if (pollRate > 0) {
        if (debugLog) {log.info "${device.name} polling period will be STARTED for every ${pollRate} seconds.  Polling will STOP at ${toDateTime(pollEndDateTime).format('hh:mm a')}"}
        runIn(pollRate+60, 'poll')
    } else {
        if (debugLog) log.info "${device.name}: setPollingInterval(): pollRate= ${pollRate} seconds, NO polling of this device is occuring."
        unschedule('poll')
    }
}

void setPollingPeriodStateOFF() {
    if (debugLog) {log.info "${device.name} polling time period has ended.  Polling will resume at ${toDateTime(pollStartDateTime).format('hh:mm a')}"}
    unschedule('poll')
}

void setPollingCronJobs() {
    if (debugLog) log.info "setPollingCronJobs()"
    def cronStartStr = "0 ${toDateTime(pollStartDateTime).format('mm')} ${toDateTime(pollStartDateTime).format('HH')} * * ?"
    schedule (cronStartStr, 'setPollingPeriodStateON')

    def cronEndStr   = "0 ${toDateTime(pollEndDateTime).format('mm')} ${toDateTime(pollEndDateTime).format('HH')} * * ?"
    schedule (cronEndStr  , 'setPollingPeriodStateOFF')
}

void setPollingInterval() {
    if (pollRate > 0) {
        setPollingCronJobs()
        if (debugLog) log.info "${device.name}: setPollingInterval(): pollRate= ${pollRate} seconds"
    } else {
        if (debugLog) log.info "${device.name}: setPollingInterval(): pollRate= ${pollRate} seconds, NO polling of this device is occuring."
        unschedule()
        if (debugLog) runIn(1800, logsOff)
	}
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

def workingMode(mode, gear=0){
    log.debug "workingMode(): Processing Working Mode command. ${mode} ${gear}"
    sendEvent(name: "cloudAPI", value: "Pending")
    switch(mode){
        case "DIY":
            modenum = 1;
            gearNum = gear;
        break;
        case "Boiling":
            modenum = 2;
            gearNum = 0;
        break;
        case "Tea":
            modenum = 3;
            gearNum = gear;
        break;
        case "Coffee":
            modenum = 4;
            gearNum = gear;
        break;
    default:
    log.debug "not valid value for mode";
    break;
    }
    values = '{"workMode":'+modenum+',"modeValue":'+gearNum+'}'
    sendCommand("workMode", values, "devices.capabilities.work_mode")
    sendEvent(name: "modeDescription", value: mode)
}

def tempSetPoint(setpoint, unit) {
    values = '{"temperature": '+setpoint+',"unit": "'+unit+'"}'
    sendCommand("sliderTemperature", values, "devices.capabilities.temperature_setting")
}

def updated() {
    if (debugLog) runIn(1800, logsOff) else logsOff()
    if (pollRate > 0 && pollRate < 15) {
        log.error "Polling rate of '${pollRate}' is too frequent and will degrade your Hubitat hub.  The pollRate value must be >= 15 seconds. The PollRate has been reset to the default of 300 seconds (4x per min)."
        device.updateSetting('pollRate', [type: "number", value: 300])
    }
    def startDateTime = toDateTime(pollStartDateTime)
    def endDateTime   = toDateTime(pollEndDateTime)
    use(groovy.time.TimeCategory) {
        //        def duration = TimeCategory.minus(endDateTime, startDateTime)
        def duration = (endDateTime - startDateTime)
        def activePollingPeriod = (pollRate>0)?"Polling every ${(pollRate<=60)?pollRate + ' secs':pollRate/60 + ' mins'} from ${toDateTime(pollStartDateTime).format('h:mm a')} to ${toDateTime(pollEndDateTime).format('h:mm a')} (${duration})":"NOT Polling, change Polling Rate > 0"
        if (duration.hours < 0) {
            def errMsg = "<font color=red>The 'Polling End Time ${toDateTime(pollEndDateTime).format('hh:mm a')}' is before 'Polling Start time ${toDateTime(pollStartDateTime).format('hh:mm a')}'.  The invalid 'Polling End Time' value of was deleted. Polling is inactivated.</font>"
            log.error errMsg
            sendEvent(name: 'activePollingPeriod', value: errMsg)
            device.removeSetting('pollEndDateTime')
            return
        }
        sendEvent(name: 'activePollingPeriod', value: activePollingPeriod)
    }
    setPollingInterval()
retrieveStateData()
    poll()
}


def installed(){
    // Set up defaults for polling
    device.updateSetting('pollRate', [type: "number", value: 300])
    device.updateSetting('pollStartDateTime', [type: "time", value: timeToday("06:00")])
    device.updateSetting('pollEndDateTime',   [type: "time", value: timeToday("18:00")])
    getDeviceState()
    poll()
}

def initialize() {
    if (device.currentValue("cloudAPI") == "Retry") {
        if (debugLog) {log.error "initialize(): Cloud API in retry state. Reseting "}
        sendEvent(name: "cloudAPI", value: "Initialized")
    }
    if (pollRate==null) device.updateSetting('pollRate', [type: "number", value: 300])
    setPollingInterval()
    poll()
}

def logsOff() {
    unschedule ('logsOff')
    log.warn "debug logging has been disabled..."
    device.updateSetting("debugLog", [value: "false", type: "bool"])
}

def poll() {
    if (pollRate > 0 && isValidPollingTime()) {
        sendEvent(name: "lastPollActivity", value: new Date())
        if (debugLog) {log.info "poll(): A re-Polling will be initated in ${pollRate} seconds"}
        runIn(pollRate, 'poll')
        getDeviceState()
    } else {
        if (debugLog) {log.warn "poll(): A One-Time Polling will be initated because is not a valid polling time period as specified in device preferences."}
        getDeviceState()
    }
}

def refresh() {
    if (debugLog) {log.warn "refresh(): Performing refresh"}
    setPollingInterval()
    poll()
}

def configure() {
    setPollingInterval()
    retrieveStateData()
    poll()
}
