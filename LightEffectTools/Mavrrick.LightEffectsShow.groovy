/* groovylint-disable LineLength, DuplicateNumberLiteral, DuplicateStringLiteral, ImplementationAsType, ImplicitClosureParameter, InvertedCondition, LineLength, MethodReturnTypeRequired, MethodSize, NestedBlockDepth, NglParseError, NoDef, NoJavaUtilDate, NoWildcardImports, ParameterReassignment, UnnecessaryGString, UnnecessaryObjectReferences, UnnecessaryToString, UnusedImport, VariableTypeRequired */
/**
 *  Govee LightEffects Show
 *
 *  Copyright 2018 CRAIG KING
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License
 */

definition(
    name:        "Light Effects Show",
    namespace:   "Mavrrick",
    author:      "Craig King",
    description: "Easy way to stup Light effect rotation between devices",
    category:    "lighting",
    parent:      "Mavrrick:Light Effects Tools",
    importUrl:   "https://raw.githubusercontent.com/Mavrrick/Hubitat-by-Mavrrick/refs/heads/main/LightEffectTools/Mavrrick.LightEffectsShow.groovy",
    iconUrl:     "",
    iconX2Url:   ""
)

import groovy.json.JsonSlurper
import java.time.*
import java.time.ZoneId    
import java.time.format.DateTimeFormatter
import java.time.Instant

// ======== Preferences ========
preferences {
    page(name: "setupPage")
    page(name: "intervalConfigPage")
    page(name: "deviceConfigPage")
}

// ======== Page 1 – Setup ========
def setupPage() {
    dynamicPage(name: "setupPage", uninstall: true, install: true) {
        section("Give this app a name?") {
            label(name: "label",
            title: "Give this app a name?",
            required: true,
            multiple: false)
	    }
        section("What type of LightEffect app to create") {
            input 'selectionType', 'enum', 
            title: 'Selection type configuration', 
                options: ['0':'Simple - Random Scenes from selected list',
                          '1':'Advanced - Specify Scenes per device and interval'],
            required: true, 
            submitOnChange: true
        }
        section("Pick Devices to set Light Effects") {
            // Pick any device that has the "switch" capability (you can change to the cap you need)
            input "selectedDevices", "capability.lightEffects",
                title: "Choose Devices",
                multiple: true,
                required: true,
                submitOnChange: true

        input "standardBrightness", "number",
                title: "Enter brightness level to be used across all selected Devices",
                defaultValue: 80, 
                required: true,
                range: "1..100",
                submitOnChange: true
        }
        section("Number of different Effect intervals/groups") {
            input "numIntervals", "number",
                title: "Number of intervals/groups",
                required: true,
                defaultValue: 2,
                submitOnChange: true
            input 'cycleRepeat', 'bool', 
                title: 'Repeat intervals/groups until stoped', 
                required: false, 
                defaultValue: false
        }
        section {
            // Next button – goes to the second page
            href name: "intervalConfigPage",
                 title: "Configure Light Effect intervals",
                 description: "${settings?.numIntervals ?: 'Choose a number'} intervals",
                 page: "intervalConfigPage",
                 submitOnChange: true
        }
                
        section("<b>Triggers</b>") {
            // Pick any device that has the "switch" capability (you can change to the cap you need)
            paragraph "What do you want to use to trigger Light Effect Cycle"
            input 'switchTrigger', 'bool', 
                title: 'Use Switch to activate Effect cycle', 
                required: false, 
                defaultValue: false,
                submitOnChange: true
            if (switchTrigger){     
                paragraph "Select a Switch what will be used to trigger the light effect to start and stop"
                input "triggerSwitch", "capability.switch",
                    title: "Choose switch device",
                    multiple: false,
                    required: false,
                    submitOnChange: true
            }
            input 'hvTrigger', 'bool', 
                title: 'Use a Hub Variable (boolean) to activate Effect cycle', 
                required: false, 
                defaultValue: false,
                submitOnChange: true
            if (hvTrigger) {
                Map hvBol = getGlobalVarsByType("boolean")
                if(debugEnable) log.debug "hv present ar  ${hvBol}"
                if(debugEnable) log.debug "hv present ar  ${hvBol.keySet()}"
                input "hvTriggerVar",
                        "enum",
                        title: "Hub Variable",
                        options: hvBol.keySet(),
                        multiple: false,
                        required: true
            }
            input 'datetimeTrigger', 'bool', 
                title: 'Use Date and Time to activate Effect cycle', 
                required: false, 
                submitOnChange: true
            if (datetimeTrigger) {
            input 'datetimeTriggerType', 'enum', 
            title: 'Date/Time trigger type', 
                options: ['0':'No Enabled',
                          '1':'Specific date with On/Off time',
                          '2':'Between dates with On/Off times',
                          '3':'Days of week with On/Off times'],
            required: true, 
            submitOnChange: true
            if (datetimeTriggerType == '1' || datetimeTriggerType == '2' || datetimeTriggerType == '3' ){
                if (datetimeTriggerType == '1'){
                    paragraph "Enter Date which effects routine will activate"
                    input "startDate", "date",
                        title: "Choose activation date",
                        multiple: false,
                        required: false,
                        submitOnChange: false                
                } else if (datetimeTriggerType == '2'){
                    paragraph "Enter Date which effects routine will start"
                    input "startDate", "date",
                        title: "Choose starting date",
                        multiple: false,
                        required: false,
                        submitOnChange: false
                    paragraph "Enter Date which effects routine will end"
                    input "endDate", "date",
                        title: "Choose ending date",
                        multiple: false,
                        required: false,
                        submitOnChange: false
                } else if (datetimeTriggerType == '3') {
                    input 'datetimeTriggerDays', 'enum', 
                    title: 'Date/Time trigger type', 
                        options: ['0':'Sundy',
                            1:'Monday',
                            2:'Tuesday',
                            3:'Wednesday',
                            4:'Thursday',
                            5:'Friday',
                            6:'Saturday',
                            7:'Sunday' ],
                        multiple: true,
                        required: true, 
                        submitOnChange: false
                } 
                input 'startTimeSelection', 'enum', 
                    title: 'Start Time Type', 
                        options: ['0':'Specific Time',
                            1:'Sunrise',
                            2:'Sunset'],
                        multiple: false,
                        required: true, 
                        submitOnChange: false
                if (startTimeSelection == '0'){
                paragraph "Enter time which effects will be activated"
                input "startTime", "time",
                    title: "Enter starting time of day",
                    multiple: false,
                    required: false,
                    submitOnChange: true
                }
                else if (startTimeSelection == '1' || startTimeSelection == '2') {
                    input "startOffset", "long",
                title: "Time in Minutes to offset the staring of the Effects",
                defaultValue: 0, 
                required: true,
                range: "1..120",
                submitOnChange: false
                }
                input 'endTimeSelection', 'enum', 
                    title: 'End Time Type', 
                        options: ['0':'Specific Time',
                            1:'Sunrise',
                            2:'Sunset'],
                        multiple: false,
                        required: true, 
                        submitOnChange: true
                if (endTimeSelection == '0'){
                paragraph "Enter time which effects will be de-activated"
                input "endTime", "time",
                    title: "Enter ending time of day",
                    multiple: false,
                    required: false,
                    submitOnChange: false
                } else if (endTimeSelection =='1' || endTimeSelection =='2') {
                    input "endOffset", "integer",
                title: "Time in Minutes to offset the ending of the Effects",
                defaultValue: 0, 
                required: true,
                range: "1..100",
                submitOnChange: false
            }
            }
            }
        }
        section ("<b>Overrides</b>"){
            paragraph "Use these buttons to Activate or stop the Effect processing at any time."
            input "startcycle" , "button",  title: "Override - Activate effects Now"
            input "stopcycle" , "button",  title: "Override - Stop effects Now"
        }
        section {
            // Next button – goes to the second page
            input("debugEnable", "bool",title:"Enable Debug Logging",width:4,submitOnChange:true)
        }
    }
}

// ======== Page 2 – Interval configuration ========
def intervalConfigPage() {
    // Pull the number of intervals & selected devices from settings
    int nIntervals = (settings?.numIntervals?.toInteger() ?: 1)
    List devices   = (settings?.selectedDevices ?: []) as List

    dynamicPage(name: "intervalConfigPage", uninstall: false, install: false, nextPage: "setupPage") {
        
        if(debugEnable) log.debug "Selection type is  ${selectionType}"
        if (selectionType == '0') {
            section("Scene Selection Page") {
//                (1..nIntervals).each { i ->
                devices.each { dev ->
                    if(debugEnable) log.debug "Scenes for device are ${dev.currentValue("lightEffects")}"
                    def jsonSlurper = new JsonSlurper()
                    def lightEffects = jsonSlurper.parseText(dev.currentValue("lightEffects")).sort { it.value }
                    if(debugEnable) log.debug "Scenes for device are ${lightEffects}"
                    // Scene selection
                    input "scene_${dev.id}",
                        "enum",
                        title: "${dev.displayName} – Scene",
                        options: lightEffects,
                        multiple: true,
                        required: false,
                        defaultValue: '0'          // “None”
                }
                    // Duration (seconds)
                     input "duration_interval",
                        "number",
                        title: "Interval ${i} – Duration (seconds)",
                        required: false,
                        defaultValue: 10,
                        range: "1..3600"          // 1‑60 min
//                }
            }
        } else if (selectionType == '1') {
            section("Interval Scene Selection Page") {
                // Loop over intervals
                (1..nIntervals).each { i ->
                    paragraph "<b>Interval ${i}</b>"
                    // Loop over devices
                    devices.each { dev ->
                        if(debugEnable) log.debug "Scenes for device are ${dev.currentValue("lightEffects")}"
                        def jsonSlurper = new JsonSlurper()
                        def lightEffects = jsonSlurper.parseText(dev.currentValue("lightEffects")).sort { it.value }
                        if(debugEnable) log.debug "Scenes for device are ${lightEffects}"
                        // Scene selection
                        input "scene_${dev.id}_interval_${i}",
                            "enum",
                            title: "${dev.displayName} – Scene",
                            options: lightEffects,
                            required: false,
                            defaultValue: '0'          // “None”
                    }
                        // Duration (seconds)
                         input "duration_interval_${i}",
                            "number",
                            title: "Interval ${i} – Duration (seconds)",
                            required: false,
                            defaultValue: 10,
                            range: "1..3600"          // 1‑60 min
                }
            }
            section("How should the scene intervals/groups be executed.") {
                input 'selectionOrder', 'enum', 
                title: 'Selection type configuration', 
                    options: ['0':'In Order', '1':'Randomized'],
                required: true, 
                submitOnChange: true
            }
        }
    }
}


// ======== SmartApp Lifecycle ========
def installed() {
    log.debug "installed() called"
    initialize()
}

def updated() {
    log.debug "updated() called"
    initialize()
}

def initialize() {
    settingsCleanup()
    unsubscribe()
    unschedule()
    state.curInt = 0
    if (triggerSwitch) {
        subscribe(triggerSwitch, "switch", switchAction)
    }
    if (hvTrigger) {
        removeAllInUseGlobalVar()
        addInUseGlobalVar(hvTriggerVar)
        subscribe(location, "variable:${hvTriggerVar}", switchAction)
    }
    if (datetimeTrigger) {
        log.debug " In Initialize routine"
        if (startTimeSelection == '1') {
            subscribe(location, "sunriseTime", sunriseSunsetEvent)
        } else if (startTimeSelection == '2') {
            subscribe(location, "sunsetTime", sunriseSunsetEvent)
        } else {
        }
    } 
        scheduleByDate()
}

def uninstalled() {
    removeAllInUseGlobalVar()
}

void scheduleByDate(){

    ZoneId targetZone = ZoneId.of(location.timeZone.getID())
    if (datetimeTriggerType == '1') {
        if(LocalDate.parse(startDate) != LocalDate.now())
    	return    
    } else if (datetimeTriggerType == '2') {
        if(LocalDate.parse(endDate) < LocalDate.now())
            incToNextYear()
/*            endNextYear = LocalDate.parse(endDate).plusYears(1)
            startNextYear = LocalDate.parse(startDate).plusYears(1)
            log.debug "scheduleByDate() rotation completed for the year. Incrementing to next year. new Start Date is ${LocalDate.parse(startDate).plusYears(1)}, New End Date is ${LocalDate.parse(endDate).plusYears(1)}"
            app.updateSetting('startDate', [type: "date", value: startNextYear])
            app.updateSetting('endDate', [type: "date", value: endNextYear]) */
        return
    } else if (datetimeTriggerType == '3') {
        log.debug "scheduleByDate() ${LocalDate.now().getDayOfWeek().getValue()}"
        List selectedDays   = (settings?.datetimeTriggerDays ?: []) as List
        log.debug "scheduleByDate() Selected days of the week are ${selectedDays} Day of week is ${LocalDate.now().getDayOfWeek().getValue()}"
        if(!selectedDays.contains(LocalDate.now().getDayOfWeek().getValue().toString())) {
            log.debug "scheduleByDate() The Day of week is ${LocalDate.now().getDayOfWeek()} and matches "
    	    return
        }
    }
    if (datetimeTriggerType == '1' || datetimeTriggerType == '2') {
	    if(LocalDate.parse(startDate) <= LocalDate.now()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX")
            if (startTimeSelection == '0') {
                sTime = LocalTime.parse(startTime, formatter)
            } else if (startTimeSelection == '1') { //sunrise 
                instant = Instant.ofEpochMilli(getSunriseAndSunset().sunrise.time)
                if (startOffset >= '0')
                    sTime = instant.atZone(targetZone).toLocalTime().plusMinutes(startOffset.toLong())
                else
                    sTime = instant.atZone(targetZone).toLocalTime().minusMinutes(Math.abs(startOffset.toLong()))
            } else if (startTimeSelection == '2') { //sunset 
                instant = Instant.ofEpochMilli(getSunriseAndSunset().sunset.time)
                if (startOffset >= '0')
                    sTime = instant.atZone(targetZone).toLocalTime().plusMinutes(startOffset.toLong())
                else
                    sTime = instant.atZone(targetZone).toLocalTime().minusMinutes(Math.abs(startOffset.toLong()))
            }  
            if (endTimeSelection == '0') {
                eTime = LocalTime.parse(endTime, formatter)
            } else if (endTimeSelection == '1') {
                instant = Instant.ofEpochMilli(getSunriseAndSunset().sunrise.time)
                if (endOffset >= '0')
                    eTime = instant.atZone(targetZone).toLocalTime().plusMinutes(endOffset.toLong())
                else
                    eTime = instant.atZone(targetZone).toLocalTime().minusMinutes(Math.abs(endOffset.toLong()))
            } else if (endTimeSelection == '2') {
                instant = Instant.ofEpochMilli(getSunriseAndSunset().sunset.time)
                if (endOffset >= '0') 
                    eTime = instant.atZone(targetZone).toLocalTime().plusMinutes(endOffset.toLong())
                else
                    eTime = instant.atZone(targetZone).toLocalTime().minusMinutes(Math.abs(endOffset.toLong()))
            }
		    if(sTime > LocalTime.now())
			    tDate = LocalDate.now()
		    else
			    tDate = LocalDate.now().plusDays(1)
            if(eTime > sTime)
			    xDate = tDate
		    else
			    xDate = tDate.plusDays(1) 
		    if(debugEnable)
			    log.debug "${tDate.getYear()} ${tDate.getMonthValue()} ${new Date(tDate.getYear()-1900,tDate.getMonthValue()-1,tDate.getDayOfMonth(), sTime.getHour(), sTime.getMinute(), 0)}"
		    runOnce(new Date(tDate.getYear()-1900,tDate.getMonthValue()-1,tDate.getDayOfMonth(), sTime.getHour(), sTime.getMinute(), 0), "startSequence")
            runOnce(new Date(xDate.getYear()-1900,xDate.getMonthValue()-1,xDate.getDayOfMonth(), eTime.getHour(), eTime.getMinute(), 0), "endAction")
	    } else {
            log.debug "Start Date is ${startDate}" 
            tDate = LocalDate.parse(startDate.toString())
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX")
            if (startTimeSelection == '0' && endTimeSelection == '0') {
                sTime = LocalTime.parse(startTime, formatter)
                eTime = LocalTime.parse(endTime, formatter)
            
		        if(debugEnable)
			        log.debug "${tDate.getYear()} ${tDate.getMonthValue()}<br>${new Date(tDate.getYear()-1900,tDate.getMonthValue()-1,tDate.getDayOfMonth(), sTime.getHour(), sTime.getMinute(), 0)}"		
                runOnce(new Date(tDate.getYear()-1900,tDate.getMonthValue()-1,tDate.getDayOfMonth(), sTime.getHour(), sTime.getMinute(), 0), "startSequence")
                runOnce(new Date(xDate.getYear()-1900,xDate.getMonthValue()-1,xDate.getDayOfMonth(), eTime.getHour(), eTime.getMinute(), 0), "endAction")
            } else {
                log.debug "Start Date is ${startDate} and sunset or Sunrise are selected as triggered. Scheduling reschedule day of activity" 
                runOnce(new Date(tDate.getYear()-1900,tDate.getMonthValue()-1,tDate.getDayOfMonth(), 0, 0, 0), "scheduleByDate")
                
            }
        }
    } else if (datetimeTriggerType == '3') {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX")
            if (startTimeSelection == '0') {
                sTime = LocalTime.parse(startTime, formatter)
            } else if (startTimeSelection == '1') { //sunrise 
                instant = Instant.ofEpochMilli(getSunriseAndSunset().sunrise.time)
                sTime = instant.atZone(targetZone).toLocalTime()
            } else if (startTimeSelection == '2') { //sunset 
                instant = Instant.ofEpochMilli(getSunriseAndSunset().sunset.time)
                sTime = instant.atZone(targetZone).toLocalTime()
            }  
            if (endTimeSelection == '0') {
                eTime = LocalTime.parse(endTime, formatter)
            } else if (endTimeSelection == '1') {
                instant = Instant.ofEpochMilli(getSunriseAndSunset().sunrise.time)
                eTime = instant.atZone(targetZone).toLocalTime()
            } else if (endTimeSelection == '2') {
                instant = Instant.ofEpochMilli(getSunriseAndSunset().sunset.time)
                eTime = instant.atZone(targetZone).toLocalTime()
            }

        List selectedDays   = (settings?.datetimeTriggerDays ?: []) as List
        if(sTime > LocalTime.now() && selectedDays.contains(LocalDate.now().getDayOfWeek().getValue().toString()))
	        tDate = LocalDate.now()
		else           
	        tDate = LocalDate.now().plusDays(1)
            while(!selectedDays.contains(tDate.getDayOfWeek().getValue().toString())) {
                tDate = tDate.plusDays(1)
            }
            if (eTime > sTime)
                xDate = tDate
            else 
                xDate = tDate.plusDays(1) 
		    if(debugEnable)
			    log.debug "${tDate.getYear()} ${tDate.getMonthValue()}<br>${new Date(tDate.getYear()-1900,tDate.getMonthValue()-1,tDate.getDayOfMonth(), sTime.getHour(), sTime.getMinute(), 0)}"		
            runOnce(new Date(tDate.getYear()-1900,tDate.getMonthValue()-1,tDate.getDayOfMonth(), sTime.getHour(), sTime.getMinute(), 0), "startSequence")
            runOnce(new Date(xDate.getYear()-1900,xDate.getMonthValue()-1,xDate.getDayOfMonth(), eTime.getHour(), eTime.getMinute(), 0), "endAction")
    }
}

/**
 * Start Cycles  
 */
def startSequence() {    
    if(debugEnable) log.debug "startSequence(): Automation."
//    unschedule()
    if (state.curInt != 0) {
        if(debugEnable) log.debug "startSequence(): Resetting. Interval is not at starting point."
    }
     if (selectionType == '0') {
         startSequenceSmp()
     } else if (selectionType == '1') {
         startSequenceAdv()
     }
}

def startSequenceSmp() {
    if (state.curInt == null) {
        if(debugEnable) log.debug "startSequence(): Resetting. Interval is not at starting point."
        state.curInt = 0
    }
		state.curInt = state.curInt + 1
        List devices   = (settings?.selectedDevices ?: []) as List
//        Random random = new Random()
//        scenePos = random.nextInt(pollRateInt) // see explanation below
        if(debugEnable) log.debug "startSequenceSmp(): Processing Interval 1."
        devices.each { dev ->
            List scenes = settings."scene_${dev.id}" as List
            sceneCount = scenes.size()
            Random random = new Random()
            scenePos = random.nextInt(sceneCount) // see explanation below
            if(debugEnable) log.debug "startSequenceSmp(): Processing effect for ${dev} with ${sceneCount} ${scenes}scenes selected. scene number ${scenes.get(scenePos)}."
            dev.setEffect(scenes.get(scenePos))
            dev.setLevel(standardBrightness, 0)
        }
    def duration = settings.duration_interval
    
    runIn(duration, "runNextActionSmp")
}

def startSequenceAdv() {    
    state.curInt = state.curInt + 1
    scenePos = 0	
    List devices   = (settings?.selectedDevices ?: []) as List
    if(debugEnable) log.debug "startSequenceAdv(): Processing Interval 1."
    if (selectionOrder == '1') {
        Random random = new Random()
        scenePos = random.nextInt(settings.numIntervals.toInteger()) +1 // see explanation below        
    }
    devices.each { dev ->
        def sKey = "scene"
        if (selectionOrder == '1') {
            sKey = "scene_${dev.id}_interval_${scenePos}"    
            if(debugEnable) log.debug "startSequenceAdv(): Processing effect for ${dev} scene number ${"scene_"+dev.id+"_interval_"+state.curInt}."
        } else if (selectionOrder == '0') {
            sKey = "scene_${dev.id}_interval_${state.curInt}"    
            if(debugEnable) log.debug "startSequenceAdv(): Processing effect for ${dev} scene number ${"scene_"+dev.id+"_interval_"+state.curInt}."
        }
        def sceneId  = (settings[sKey]?.toInteger() ?: 0)
        dev.setEffect(sceneId)
        dev.setLevel(standardBrightness, 0)
    }
    def dKey = "duration_interval_${state.curInt}"
//    def dKey = "duration_interval_${i}"
    def duration = (settings[dKey]?.toInteger() ?: 0)
    if (settings.numIntervals == 1) {
        if(debugEnable) log.debug "startSequenceAdv(): Only 1 interval in enabled. Do not submit repeat actions."
    } else {
        runIn(duration, "runNextActionAdv")
    }
}

/**
 * Helper that Manages cycle duration and submitting commands for next effect.
 */

def runNextActionSmp() {
    if(debugEnable) log.debug "runNextActionSmp(): proessing next cycle."
    
    state.curInt = state.curInt + 1
    List devices   = (settings?.selectedDevices ?: []) as List
    if(debugEnable) log.debug "runNextActionSmp(): Processing Interval ${state.curInt}."
    devices.each { dev ->
            List scenes = settings."scene_${dev.id}" as List
            sceneCount = scenes.size()
            Random random = new Random()
            scenePos = random.nextInt(sceneCount) // see explanation below
            if(debugEnable) log.debug "runNextActionSmp(): Processing effect for ${dev} with ${sceneCount} ${scenes}scenes selected. scene number ${scenes.get(scenePos)}."
            dev.setEffect(scenes.get(scenePos))
    }
    def duration = settings.duration_interval
    if (state.curInt == numIntervals && cycleRepeat == false) { 
        if(debugEnable) log.debug "Last interval Scheduling off in x seconds"
        runIn(duration, "endAction")
    } else if (state.curInt == numIntervals && cycleRepeat == true) {
        state.curInt = 0
        runIn(duration, "runNextActionSmp")
    } else {
    runIn(duration, "runNextActionSmp")
    }
}

def runNextActionAdv() {
    if(debugEnable) log.debug "runNextActionAdv(): proessing next cycle."
    
    state.curInt = state.curInt + 1
    List devices   = (settings?.selectedDevices ?: []) as List
    if(debugEnable) log.debug "runNextActionAdv(): Processing Interval ${state.curInt}."
    if (selectionOrder == '1') {
        Random random = new Random()
        scenePos = random.nextInt(settings.numIntervals.toInteger()) +1 // see explanation below        
    }
    devices.each { dev ->
        def sKey = "scene"
        if (selectionOrder == '1') {
            sKey = "scene_${dev.id}_interval_${scenePos}"    
            if(debugEnable) log.debug "runNextActionAdv(): Processing effect for ${dev} scene number ${"scene_"+dev.id+"_interval_"+state.curInt}."
        } else if (selectionOrder == '0') {
            sKey = "scene_${dev.id}_interval_${state.curInt}"    
            if(debugEnable) log.debug "runNextActionAdv(): Processing effect for ${dev} scene number ${"scene_"+dev.id+"_interval_"+state.curInt}."
        }
        def sceneId  = (settings[sKey]?.toInteger() ?: 0)
        dev.setEffect(sceneId)
    }
    def dKey = "duration_interval_${state.curInt}"
    def duration = (settings[dKey]?.toInteger() ?: 0)
    if (state.curInt == numIntervals && cycleRepeat == false) { 
        if(debugEnable) log.debug "Last interval Scheduling off in x seconds"
        runIn(duration, "endAction")
    } else if (state.curInt == numIntervals && cycleRepeat == true) {
        state.curInt = 0
        runIn(duration, "runNextActionAdv")
    } else {
    runIn(duration, "runNextActionAdv")
    }
}

def switchAction(evt) {
    if(debugEnable) log.debug "switchAction(): Event is ${evt.value}"
    if (evt.value == "on" || evt.value == "true") {
        startSequence()
    } else {
        endAction()
    }
}

def sunriseSunsetEvent() {
    log.debug "Sunrise or Sunset Event has been capture"
}

private def endAction(){
	unschedule()
    if(LocalDate.parse(endDate) == LocalDate.now()) {
        incToNextYear()
    }
    if (datetimeTrigger) {
        scheduleByDate()
    }
    state.curInt = 0
    List devices   = (settings?.selectedDevices ?: []) as List
    devices.each { dev ->
        dev.off()
    }
}

def incToNextYear(){
    endNextYear = LocalDate.parse(endDate).plusYears(1)
    startNextYear = LocalDate.parse(startDate).plusYears(1)
    log.debug "scheduleByDate() rotation completed for the year. Incrementing to next year. new Start Date is ${LocalDate.parse(startDate).plusYears(1)}, New End Date is ${LocalDate.parse(endDate).plusYears(1)}"
    app.updateSetting('startDate', [type: "date", value: startNextYear])
    app.updateSetting('endDate', [type: "date", value: endNextYear]) 
	
}

private def appButtonHandler(button) {
    if(debugEnable) log.debug "appButtonHandler() ${button}"
    if (button == "startcycle") {
        startSequence()
    }  else if (button == "stopcycle") {
        endAction()
    }
}


private def settingsCleanup(){
    
    int nIntervals = (settings?.numIntervals?.toInteger() ?: 1)
    List devices   = (settings?.selectedDevices ?: []) as List
    
    if(debugEnable) log.debug "settingsCleanup() ${settings}"
    if(debugEnable) log.debug "settingsCleanup() Current list of settings: ${settings.keySet()}"
    curSettingList = settings.keySet() // list of settings as currently in app
    /// Build list of valid settings based on how the app is running
    validSettingList =  ["selectionType", "cycleRepeat", "standardBrightness", "switchTrigger", "numIntervals", "selectionOrder", "hvTrigger", "debugEnable", "selectedDevices", "datetimeTrigger"]
    if (switchTrigger) {
        validSettingList.add("triggerSwitch")
    }
    if (hvTrigger) {
        validSettingList.add("hvTriggerVar")
    }
    if (datetimeTrigger){
        validSettingList.add("datetimeTriggerType")
        if (datetimeTriggerType == '1' || datetimeTriggerType == '2') {
            validSettingList.add("startDate")
            validSettingList.add("startTimeSelection")
            validSettingList.add("endDate")
            validSettingList.add("endTimeSelection")
        } else {
            validSettingList.add("startTimeSelection")
            validSettingList.add("endTimeSelection")
            validSettingList.add("datetimeTriggerDays")
        }
        if (startTimeSelection == '0') {
            validSettingList.add("startTime")
        } else if (startTimeSelection == '1' || startTimeSelection == '2') {
            validSettingList.add("startOffset")
        }
        if (endTimeSelection == '0') {
            validSettingList.add("endTime")
        } else if (endTimeSelection =='1' || endTimeSelection =='2') {
            validSettingList.add("endOffset")
            }
    }
    if (selectionType == '0') {
        validSettingList.add("duration_interval")
        devices.each { dev ->
            validSettingList.add("scene_${dev.id}")    
        } 
    } else if (selectionType == '1')
    (1..nIntervals).each { i ->
        validSettingList.add("duration_interval_${i}")
        devices.each { dev ->
            validSettingList.add("scene_${dev.id}_interval_${i}")    
        }        
    }
    if(debugEnable) log.debug "settingsCleanup() Valid settings are: ${validSettingList}"
    listToRemove = curSettingList - validSettingList
    if(debugEnable) log.debug "settingsCleanup() List to be removed: ${listToRemove}"
    listToRemove.each {
        if(debugEnable) log.debug "settingsCleanup() removing setting: ${it}"
        app.removeSetting(it)
    }
    
}    

