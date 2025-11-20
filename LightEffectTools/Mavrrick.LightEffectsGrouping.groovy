/* groovylint-disable LineLength, DuplicateNumberLiteral, DuplicateStringLiteral, ImplementationAsType, ImplicitClosureParameter, InvertedCondition, LineLength, MethodReturnTypeRequired, MethodSize, NestedBlockDepth, NglParseError, NoDef, NoJavaUtilDate, NoWildcardImports, ParameterReassignment, UnnecessaryGString, UnnecessaryObjectReferences, UnnecessaryToString, UnusedImport, VariableTypeRequired */
/**
 *  Govee LightEffects Grouping
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
    name:        "Light Effects Grouping",
    namespace:   "Mavrrick",
    author:      "Craig King",
    description: "Group Devices together to Standardize Scenes with a single list",
    category:    "lighting",
    parent:      "Mavrrick:Light Effects Tools",
    importUrl:   "https://raw.githubusercontent.com/Mavrrick/Hubitat-by-Mavrrick/refs/heads/main/Govee/show/GoveeLightEffectsShow",
    iconUrl:     "",
    iconX2Url:   ""
)

import groovy.json.JsonSlurper

// ======== Preferences ========
preferences {
    page(name: "setupPage")
    page(name: "commandConfigPage")
    page(name: "intervalConfigPage")
    page(name: "deviceConfigPage")
}

commandConfigPage
// ======== Page 1 – Setup ========
def setupPage() {
    dynamicPage(name: "setupPage", uninstall: true, install: true) {
        section("Give this app a name?") {
            label(name: "label",
            title: "Give this app a name?",
            required: true,
            multiple: false)
	    }
        section("<b>Pick Devices to set standard Settings</b>") {
            // Pick any device that has the "switch" capability (you can change to the cap you need)
            input "selectedDevices", "capability.lightEffects",
                title: "Choose Devices",
                multiple: true,
                required: true,
                showFilter: true,
                submitOnChange: true

/*        input "standardBrightness", "number",
                title: "Enter brightness level to be used across all selected Devices",
                defaultValue: 80, 
                required: true,
                range: "1..100",
                submitOnChange: true */
        }
        section("<b>Number of different scene effect groups</b>") {
            input "numGroups", "number",
                title: "",
                required: true,
                defaultValue: 2,
                submitOnChange: true
        }
        section ("<b>Configure Light Effect Groups</b>"){
            href name: "commandConfigPage",
                 title: "Configure Light Effect Groups",
                 description: "${settings?.numGroups ?: 'Choose a number'} scene groups",
                 page: "commandConfigPage",
                 submitOnChange: true
        } 
        
        section("<b>Select Button Below to create Control device</b>") {
            input "createDevice" , "button",  title: "Create Virtial control device"
        }
        section ("<b>Overrides</b>"){
            paragraph "Use these buttons to activate or stop the Effect processing at any time."
            input "startcycle" , "button",  title: "Override - Activate effects Now"
            input "stopcycle" , "button",  title: "Override - Stop effects Now"
        }
        section {
            // Next button – goes to the second page
            input("debugEnable", "bool",title:"Enable Debug Logging",width:4,submitOnChange:true)
        }
    }
}

// ======== Page 2 – Command by Device Selection ========
def commandConfigPage() {
    // Pull the number of intervals & selected devices from settings
    int numGroups = (settings?.numGroups?.toInteger() ?: 1)
    List devices   = (settings?.selectedDevices ?: []) as List

    dynamicPage(name: "commandConfigPage", uninstall: false, install: false, nextPage: "setupPage") {
       
            section("Command Selection Page") {
            (1..numGroups).each { i ->
            // Pick any device that has the "switch" capability (you can change to the cap you need)
                input "sceneName_group_${i}", "text",
                    title: "Group ${i} scene name",
//                multiple: true,
                required: true
                paragraph "<b>Device Command selection in Group ${i}</b>"
                devices.each { dev ->
                    input "command_${dev.id}_group_${i}", 'enum', 
                    title: "${dev.displayName} –  Command", 
                        options: [0:'setEffect',
                            1:'colorTemperature',
                            2:'color' ],
                        multiple: false,
                        required: true, 
                        submitOnChange: true
                    if (settings."command_${dev.id}_group_${i}" == '0') {
                        if(debugEnable) log.debug "Scenes for device are ${dev.currentValue("lightEffects")}"
                        def jsonSlurper = new JsonSlurper()
                        def lightEffects = jsonSlurper.parseText(dev.currentValue("lightEffects")).sort { it.value }
                        if(debugEnable) log.debug "Scenes for device are ${lightEffects}"
                        // Scene selection
                        input "scene_${dev.id}_group_${i}",
                            "enum",
                            title: "${dev.displayName} – Scene",
                            options: lightEffects,
                            required: false,
                            defaultValue: '0'          // “None”
                    } else if (settings."command_${dev.id}_group_${i}" == '1') {
                        input "colorTemperature_${dev.id}_group_${i}", "number",
                            title: "Enter Color Temperature to be used for device in grouping action 2000-6500",
                            defaultValue: 2000, 
                            required: true,
                            range: "2000..6500",
                            submitOnChange: false 
                    } else if (settings."command_${dev.id}_group_${i}" == '2') {
                        input "color_${dev.id}_group_${i}", "color",
                            title: "Select Color to apply on device in group action",
//                            defaultValue: 80, 
                            required: true,
//                            range: "2000..6500",
                            submitOnChange: false 
                    }
                }
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
    buildLightEffectJson ()
}

def initialize() {
    unsubscribe()
    unschedule()
    settingsCleanup()
    if (triggerSwitch) {
        subscribe(triggerSwitch, "switch", switchAction)
    }
}


/**
 *  Control Helpers 
 */

def on() {    
    if(debugEnable) log.debug "on(): Automation."
    List devices   = (settings?.selectedDevices ?: []) as List
    
    devices.each { dev ->
            List scenes = settings."scene_${dev.id}" as List
            if(debugEnable) log.debug "on(): Processing on for ${dev}."
            dev.on()            
        }
}

def off() {    
    if(debugEnable) log.debug "off(): Automation."
    List devices   = (settings?.selectedDevices ?: []) as List
    
    devices.each { dev ->
            List scenes = settings."scene_${dev.id}" as List
            if(debugEnable) log.debug "off(): Processing off for ${dev}."
            dev.off()            
        }
}

def setColorTemperature(value,level = null,transitionTime = null) {    
    if(debugEnable) log.debug "setColorTemperature(): Automation."
    List devices   = (settings?.selectedDevices ?: []) as List
    
    devices.each { dev ->
//            List scenes = settings."scene_${dev.id}" as List
//            if(debugEnable) log.debug "setColorTemperature(): Processing effect for ${dev} with ${sceneCount} ${scenes}scenes selected. scene number ${scenes.get(scenePos)}."
            dev.setColorTemperature(value, level, transitionTime)            
        }
}

def setLevel(brightness, transitiontime = 0) {    
    if(debugEnable) log.debug "setLevel(): Automation."
    List devices   = (settings?.selectedDevices ?: []) as List
    
    devices.each { dev ->
//            List scenes = settings."scene_${dev.id}" as List
//            if(debugEnable) log.debug "setColorTemperature(): Processing effect for ${dev} with ${sceneCount} ${scenes}scenes selected. scene number ${scenes.get(scenePos)}."
            dev.setLevel(brightness, 0)          
        }
}

def setColor(colorMap) {    
    if(debugEnable) log.debug "setColor(): Automation. with value ${colorMap}"
    List devices   = (settings?.selectedDevices ?: []) as List
    
    devices.each { dev ->
//            List scenes = settings."scene_${dev.id}" as List
//            if(debugEnable) log.debug "setColorTemperature(): Processing effect for ${dev} with ${sceneCount} ${scenes}scenes selected. scene number ${scenes.get(scenePos)}."
            dev.setColor(colorMap)          
        }
}


def setEffect(effectNo) {
    if(debugEnable) log.debug "setEffect(): Automation."
    
    List devices   = (settings?.selectedDevices ?: []) as List
    devices.each { dev ->
        def cKey = "command_${dev.id}_group_${effectNo}" //        command_3428_group_1
        def command = (settings[cKey]?.toInteger() ?: 0)
        if (command == 0) {
            def sKey = "scene_${dev.id}_group_${effectNo}"
            def sceneId  = (settings[sKey]?.toInteger() ?: 0)
            if(debugEnable) log.debug "startSequenceAdv(): Processing effect for ${dev} scene number ${sceneId}."  
            dev.setEffect(sceneId)
        } else if (command == 1) {
            def sKey = "colorTemperature_${dev.id}_group_${effectNo}" // colorTemperature_3428_group_1
            def ctvalue  = (settings[sKey]?.toInteger() ?: 0)
            dev.setColorTemperature(ctvalue,level = null,transitionTime = null)
        } else if (command == 2) { 
            def sKey = "color_${dev.id}_group_${effectNo}" // color_3428_group_1
            colorHexValue  = (settings[sKey]?.toString() ?: 0)
            if(debugEnable) log.debug "startSequenceAdv(): Hex Value is ${colorHexValue}."
            def rgb = hubitat.helper.ColorUtils.hexToRGB(colorHexValue)
            def hsv = hubitat.helper.ColorUtils.rgbToHSV(rgb) // Converts RGB to HSV
            
            def hslmap = [:]
	        hslmap.hue = hsv[0]
            hslmap.level = hsv[1]
	        hslmap.saturation = hsv[2] 
            
            if(debugEnable) log.debug "startSequenceAdv(): Processing effect ${effectNo} for ${dev} HSB ${hsv} HSLMap ${hslmap}."
            dev.setColor(hslmap)
        }
    }
}


def switchAction(evt) {
    if(debugEnable) log.debug "switchAction(): Event is ${evt.value}"
    if (evt.value == "on") {
        startSequence()
    } else {
        endAction()
    }
}

private def appButtonHandler(button) {
    if(debugEnable) log.debug "appButtonHandler() ${button}"
    if (button == "startcycle") {
        startSequence()
    }  else if (button == "stopcycle") {
        endAction()
    } else if (button == "createDevice") {
    	childCount = getChildDevices().size()
        if(debugEnable) log.debug "Child count is ${childCount}."
        if (childCount == 0) {
            addChildDevice('Mavrrick', 'LightEffect Group', app.getLabel()+'_Device' , location.hubs[0].id, [
                'name': 'LightEffect Group Device ', 
                'label': app.getLabel()+'_Device',
                 'data': [                    
                  ],
                 'completedSetup': true,
            ])
        } else {
            if(debugEnable) log.debug "Child Device already created."
        }
    }
}

def buildLightEffectJson () {
    scenes = [:]
    (1..numGroups).each { i ->
        scenes[i] = settings."sceneName_group_${i}"
    }
    if(debugEnable) log.debug "buildLightEffectJson(): LightEffect map is ${scenes}"
    return scenes
}

private def settingsCleanup(){
    
    int nGroups = (settings?.numGroups?.toInteger() ?: 1)
    List devices   = (settings?.selectedDevices ?: []) as List
    
    if(debugEnable) log.debug "settingsCleanup() ${settings}"
    if(debugEnable) log.debug "settingsCleanup() Current list of settings: ${settings.keySet()}"
    curSettingList = settings.keySet() // list of settings as currently in app
    /// Build list of valid settings based on how the app is running
    validSettingList =  ["standardBrightness", "numGroups", "debugEnable", "selectedDevices"]
    
    (1..nGroups).each { i ->
        validSettingList.add("sceneName_group_${i}")
        devices.each { dev ->
            validSettingList.add("command_${dev.id}_group_${i}")
            if (settings."command_${dev.id}_group_${i}" == '0') {
                validSettingList.add("scene_${dev.id}_group_${i}")
            }
            if (settings."command_${dev.id}_group_${i}" == '1') {
                validSettingList.add("colorTemperature_${dev.id}_group_${i}")
            }
            if (settings."command_${dev.id}_group_${i}" == '2') {
                validSettingList.add("color_${dev.id}_group_${i}")
            }
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
