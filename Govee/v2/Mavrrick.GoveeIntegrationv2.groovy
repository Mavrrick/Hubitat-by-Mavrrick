/* groovylint-disable LineLength, DuplicateNumberLiteral, DuplicateStringLiteral, ImplementationAsType, ImplicitClosureParameter, InvertedCondition, LineLength, MethodReturnTypeRequired, MethodSize, NestedBlockDepth, NglParseError, NoDef, NoJavaUtilDate, NoWildcardImports, ParameterReassignment, UnnecessaryGString, UnnecessaryObjectReferences, UnnecessaryToString, UnusedImport, VariableTypeRequired */
/**
 *  Govee Integration
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
    name: 'Govee Integration v2',
    namespace: 'Mavrrick',
    author: 'CRAIG KING',
    description: 'Govee Integration for HE',
    category: 'Lighting',
    iconUrl: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg',
    iconX2Url: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg',
    iconX3Url: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg',
    singleInstance: true)

/*
* Initial release v1.0.0
* 2.1.4  Update to fix DIYEffect Bug
* 2.1.5  Bug fixes
* 2.1.6  Update to add ability to Save/Restore DIY data to a flat file
* 2.1.7  Many update to integration app to simplify UI and improve experience
* 2.1.8  Bug Fix for issue with Device Select Page
* 2.1.9  Enhancements to Scene management to function from flat files 
*/

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field

@Field static List child = []
@Field static List childDNI = []
@Field static final String goveeDIYScenesFileBackup = "GoveeLanDIYScenes_Backup.json"
@Field static final String goveeDIYScenesFile = "GoveeLanDIYScenes.json"
@Field static String statusMessage = ""


preferences
{
    page(name: 'mainPage', title: 'Govee Integration')
    page(name: 'deviceSelect', title: 'Select Light, Switch, Plug devices')
    page(name: 'deviceSelect2', title: 'Device Install Process')
    page(name: 'deviceLanManual', title: 'Manual LAN Setup')
    page(name: 'deviceLanManual2', title: 'Complete device manual add')
    page(name: 'sceneManagement', title: 'Scene Management')
    page(name: 'sceneExtract', title: 'Extract scenes')
    page(name: 'sceneExtract2', title: 'Govee Home Creds')
    page(name: 'sceneManualAdd', title: 'Maunally Add Scenes')
    page(name: 'sceneManualAdd2', title: 'Maunally Add Results')
    page(name: 'sceneManualUpdate', title: 'Update Scene data') 
    page(name: 'sceneManualUpdate2', title: 'Update Scene data')    
    page(name: 'about', title: 'About')
}

/*
    mainPage

    UI Page: Main menu for the app.
*/
def mainPage() {
    atomicState.backgroundActionInProgress = null
    app.clearSetting("goveeDevName")
    app.clearSetting("goveeModel")
    app.clearSetting("goveeManLanIP")
    statusMessage = ""
    if (state.isInstalled == true) {     
        mqttDevice = getChildDevice('Govee_v2_Device_Manager')
        if (mqttDevice == null) {
            logger("goveeDevAdd()  configuring Govee_v2_Device_Manager", 'info')
            addChildDevice('Mavrrick', 'Govee v2 Device Manager', "Govee_v2_Device_Manager" , location.hubs[0].id, [
                'name': 'Govee v2 Device Manager',
                'label': 'Govee v2 Device Manager',
                 'data': [
                    'apiKey': settings.APIKey
                 ],
                 'completedSetup': true,
             ])
            mqttDevice = getChildDevice('Govee_v2_Device_Manager')
        }
        child = getChildDevices() + mqttDevice.getChildDevices()
        childDNI = child.deviceNetworkId
    }

    def int childCount = child.size()
    dynamicPage(name: 'mainPage', title: 'Govee integration Main menu', uninstall: true, install: true, submitOnChange: true)
    {
        section('<b>API Configuration</b>')
        {
            if (settings.APIKey == null) {
            paragraph "A Govee API key is recommended for this integration. It is suggested to obtain this value before moving forward to ensure " +
                "you can see all of your devices. You can obtain the Govee API from the Govee Home app 'About Us' section. Until this is added " +
                "you can only setup local LAN API api devices, and they will not be able to retrieve status from Govee. Additional options will " +
                "appear once the api key is entered and saved by clicking done at the bottom of the window"
            }
            input 'APIKey', 'string', title: 'Enter Your API Key', required: false
            
        }
        section('<b>Govee Device Management</b>') {
            if (settings.APIKey != null) {
            paragraph "Use the Standard Device setup option to enable all device features possible. The option for Manual Setup of LAN API Devices should be used as a last restore for devices that can not be setup using the Standard Device setup method."
            href 'deviceSelect', title: 'Standard Device Setup' //, description: 'Select Govee devices to add to your environment'
            }
            href 'deviceLanManual', title: 'Manual Setup for Lan API Only Devices' //, description: 'Use this option to setup devices that only support LAN API'
        }
        section('<b>Current Integrated Device Information</b>') {
            paragraph "There are <mark>${childCount}</mark> devices integrated"
            paragraph "Your current integrated devices are ${child.label}"
            if (settings.APIKey != null) {
                if (childCount > 0) { def maxPoll = 86400 / ((10000 - (50 * childCount)) / childCount).toDouble()
                maxPoll = maxPoll.round(1)
                paragraph "With the number of devices have you can poll as frequently as every <mark>${maxPoll}</mark> seconds" }
//            paragraph "You have <mark>${state.dailyLimit}</mark> of your 10k Daily APIv1 Calls left."
//            paragraph "You have <mark>${state.dailyAppLimit}</mark> of your 100 Daily APIv2 Calls left."
                    }
        }
/*        if (settings.APIKey != null) {
        section('<b>Notification Options</b>') {
            input 'notifyEnabled', 'bool', title: 'Enable Notification', required: false, defaultValue: false
            input name: 'notificationDevice', type: 'capability.notification', title: 'Send notification of rate limit consumption :', multiple: true
            input 'apiV1threshold', 'number', title: 'Daily Rate Limit threshold to send out notification for Lights, Switches, and Plugs.', required: false , range: '1..500', defaultValue: 3
            input 'apiV2threshold', 'number', title: 'Daily Rate Limit threshold to send out notification for Appliances.', required: false , range: '1..100', defaultValue: 3
        }
        } */
        section('<b>Govee LAN API Scene Management</b>') {
            href 'sceneManagement', title: 'Lan API Scene Management Menu', description: 'Click to setup extraction credential, extract scenes, and manage device association.'
        }
        section('<b>Logging Options</b>') {
            input(
                name: 'configLoggingLevelIDE',
                title: 'IDE Live Logging Level:\nMessages with this level and higher will be logged to the IDE.',
                type: 'enum',
                options: [
                    '0' : 'None',
                    '1' : 'Error',
                    '2' : 'Warning',
                    '3' : 'Info',
                    '4' : 'Debug',
                    '5' : 'Trace'
                ],
                defaultValue: '1',
                displayDuringSetup: true,
                required: false
            )
        }
        section('About') {
            href 'about', title: 'About Information menu', description: 'About Menu for Govee Integration'
        }
    }
}

def deviceSelect() {
    Map options = [:] 
    if (state.goveeAppAPI == null) {
        retrieveGoveeAPIData()
    }
    if (state.goveeAppAPIdate == null) {
        logger('deviceSelect() goveeAppAPIdate is not present. Populating with data to force refresh', 'debug')
        atomicState.goveeAppAPIdate = now() - 1801
    }
    if (1800 < (now() - state.goveeAppAPIdate)) {
       logger('deviceSelect() More then 30 min have passed since last refresh. Retrieving device data from Govee API', 'debug') 
       retrieveGoveeAPIData() 
    }
    logger('deviceSelect() DEVICE INFORMATION', 'debug')
                state.goveeAppAPI.each {
                    String deviceName = it.deviceName
                    logger("deviceSelect() $deviceName found", 'debug')
                    options["${deviceName}"] = deviceName
                } 
                logger(" deviceSelect() $options", 'debug')
    dynamicPage(name: 'deviceSelect', title: 'Add Devices page', uninstall: false, install: false, nextPage: "deviceSelect2")
    {
        
        section('<b>Device Add</b>')
        {
            paragraph 'Please select the devices you wish to integrate. If the device is not present in the list please click on the Device Refresh button below.'
            input(name: 'goveeDev', type: 'enum', required:false, description: 'Please select the devices you wish to integrate.', multiple:true,
                options: options.sort() , width: 8, height: 1)
        }
        
        section('<b>Device list refresh</b>') {
            paragraph "Click the button below to refresh device list from Govee API"
            input "deviceListRefresh" , "button",  title: "Device Refresh"
        }
    }
}

def deviceSelect2() {
    logger("deviceSelect2: Install chosen devices ${atomicState.backgroundActionInProgress}", 'debug')
	if (atomicState.backgroundActionInProgress == null) {
		logger("deviceSelect2: Install chosen devices", 'debug')
		atomicState.backgroundActionInProgress = true
         logger("deviceSelect2: Background action in progress status should be true =  ${atomicState.backgroundActionInProgress}", 'debug')
		runInMillis(1,goveeDevAdd)
	}
	if (atomicState.backgroundActionInProgress != false) {
        logger("deviceSelect2: Install chosen devices ${atomicState.backgroundActionInProgress}", 'debug')
		return dynamicPage(name: "deviceSelect2", title: "", nextPage: "deviceSelect2", install: false, uninstall: false, refreshInterval: 2) {
			section {
				paragraph "<b>Processing Setup of devices from Selected Devices</b>"
				paragraph "Installing new devices from selected list... Please wait..."
				paragraph getBackgroundStatusMessage()
				showHideNextButton(false)
			}
		}
	}
	else if (atomicState.backgroundActionInProgress == false) {
        logger("deviceSelect2: Install chosen devices ${atomicState.backgroundActionInProgress}", 'debug')
			return dynamicPage(name: "deviceSelect2", title: "", nextPage: "mainPage", install: false, uninstall: false) {
				section {
					paragraph "<b>Device install status</b>"
                    paragraph getBackgroundStatusMessage()
					paragraph "Device setup completed."
/*				section {
					paragraph "<hr>"
					input "btnMainMenu", "button", title: "Main Menu", width: 3
				} */
			}
        }
	}
//    statusMessage = ""
}


def deviceLanManual() {
    dynamicPage(name: 'deviceLanManual', title: 'Manual Setup for LAN API Enabled Devices', uninstall: false, install: false, nextPage: "deviceLanManual2" )
    {
        section('<b>***Warning***</b> Using the manual Addd option will potentially severaly limit your use of the device. This should be a last resort and only used if the device does not support adding with the normal method using the Cloud API. LAN API control can be enable on traditionally added devices as well.')
        {
            paragraph 'Please enter the needed parameters below to create your device '
            input(name: 'goveeDevName', type: 'string', required:false, title: 'Name of device.', description: 'E.g. Bedroom Lights')
            input(name: 'goveeModel', type: 'string', required:false, title: 'Enter Govee Device Model Number.', description: 'E.g. H####')
            input(name: 'goveeManLanIP', type: 'string', required:false, title: 'Enter Govee Device Ip Address.', description: 'E.g. 192.168.1.2')
            paragraph 'Click the next button when you are ready to create the device. '
        }
    }
}

def deviceLanManual2() {
    if (settings.goveeModel && settings.goveeManLanIP && settings.goveeDevName) {
        goveeLightManAdd(settings.goveeModel, settings.goveeManLanIP, settings.goveeDevName)
        lightEffectSetup()
    }
    dynamicPage(name: 'deviceLanManual2', title: 'Results of manual device add', uninstall: false, install: false, nextPage: "mainPage")
    {
        if (settings.goveeModel && settings.goveeManLanIP && settings.goveeDevName) {
            section('<b>Device Manual Add</b>') {
            paragraph "Attempted manual add of ${settings.goveeDevName} at ip ${settings.goveeManLanIP}."
            paragraph "Click Next to return to the main menu."
            } 
        } else {
            section('<b>Device Manual Add</b>') {
            paragraph "Please try again and fill in all needed values."
            }    
        }
    }
}

def sceneManagement() {
    app.clearSetting("devsku")
    app.clearSetting("sceneName")
    app.clearSetting("sceneNum")
    app.clearSetting("command")
    dynamicPage(name: 'sceneManagement', title: 'Govee Scene Management menu', uninstall: false, install: false, submitOnChange: false, nextPage: "mainPage")
    {
        section('<b>Govee Home Creds</b>')
        {
            paragraph "Please provide your gove home token."
            href 'sceneExtract2', title: 'Govee Home Creds', description: 'Setup Gove Home Creds to obtain token'
            if (now() > state.goveeHomeExpiry) {                
            paragraph "Token has <mark>expired</mark>. Attempting to auto-renew"
            appButtonHandler("goveeHomeLogin")    
            } else {
                paragraph "Token is <mark>valid</mark>"
            }
        }
        section('<b>Govee Home Group</b>')
        {
            paragraph "Please provide the group used to extract scenes"
            input 'goveeGroup', 'string', title: 'Please enter your Govee home Group name used to extract scenes. This is case sensative', required: false, defaultValue: 'Default'
        }
        section('<b>Govee Scene Extract</b>') {
            href 'sceneExtract', title: 'Extract Scene', description: 'Click here to perform scene extract'
//            paragraph "Click button below to refresh scenes to all children device"
//            input "pushScenesUpdate" , "button",  title: "Refresh Device Scene Awareness"
//            paragraph "Click button below to reload preload scenes"
//            input "sceneInitialize" , "button",  title: "Reload Preloaded Scene Data"
            paragraph "Click button below to clear DIY scenes"
            input "sceneDIYInitialize" , "button",  title: "Clear/Initialize DIY Scene Information"
            paragraph "Click button below to save DIY scenes"
            input "savDIYScenes" , "button",  title: "Backup DIY Scene Information to file"
            paragraph "Click button below to restore DIY scenes"
            input "resDIYScenes" , "button",  title: "Restore DIY Scene Information from file"
        }
        section('<b>Scene Manual Add</b>')
        {
            href 'sceneManualAdd', title: 'Manual DIY Scene Add', description: 'Click to perform a manual add'
            href 'sceneManualUpdate', title: 'Manual DIY Scene update', description: 'Click to perform a update'
        }
    }
}

def sceneManualAdd() {
    dynamicPage(name: 'sceneManualAdd', title: 'Govee Manual Add for DIY Scenes', uninstall: false, install: false, submitOnChange: false, nextPage: "sceneManualAdd2")
    {
        section('<b>Scene Manual Add</b>')
        {
            paragraph "Three items are needed to perform a manual Add. You will need the Device model number, a name for the Scene, and the command"
            input 'devsku', 'string', title: '5 charecter Model (ie H6172)', required: false, default: ""
            input 'sceneName', 'string', title: 'Provide text name for scene', required: false, default: ""
            input 'command', 'string', title: 'String provided by other user Should begin and end with  [ ]', required: false, default: ""
        }
    }
}

def sceneManualAdd2() {
    if (settings.devsku && settings.sceneName && settings.command) {
        diyAddManual(settings.devsku, settings.sceneName, settings.command)
    }
    dynamicPage(name: 'sceneManualAdd2', title: 'Results of manual Add', uninstall: false, install: false, submitOnChange: false, nextPage: "sceneManagement")
    {
            if (settings.devsku && settings.sceneName && settings.command) {
                section('<b>Scene Manual Add</b>') {
                    paragraph "Attempted manual add of ${settings.sceneName} command: ${settings.command} for ${settings.devsku}"
                } 
            } else {
                section('<b>Scene Manual Add</b>') {
                paragraph "Please try again and fill in all needed values"
                
            }
        }
    }
}

def sceneManualUpdate() {
    dynamicPage(name: 'sceneManualUpdate', title: 'Govee Manual update for DIY Scenes', uninstall: false, install: false, submitOnChange: false, nextPage: "sceneManualUpdate2")
    {
        section('<b>Scene Manual Add</b>')
        {
            paragraph "Three items are needed to perform a manual update. You will need the Device model number, a name for the Scene, and the command"
            input 'devsku', 'string', title: '5 charecter Model (ie H6172)', required: false, default: ""
            input 'sceneNum', 'string', title: 'Provide number of scene', required: false, default: ""
            input 'sceneName', 'string', title: 'Provide text name for scene', required: false, default: ""
            input 'command', 'string', title: 'String provided by other user Should begin and end with  [ ]', required: false, default: ""
        }
    }
}

def sceneManualUpdate2() {
    if (settings.devsku && settings.sceneName && settings.command && settings.sceneNum) {
        diyUpdateManual(settings.devsku, settings.sceneNum, settings.sceneName, settings.command)
    }
    dynamicPage(name: 'sceneManualUpdate2', title: 'Results of manual Add', uninstall: false, install: false, submitOnChange: false, nextPage: "sceneManagement")
    {
            if (settings.devsku && settings.sceneName && settings.command && settings.sceneNum) {
                section('<b>Scene Manual update</b>') {
                    paragraph "Attempted manual update of ${settings.sceneName} with Scene number ${settings.sceneNum} command: ${settings.command} for ${settings.devsku}"
                } 
            } else {
                section('<b>Scene Manual Adupdated</b>') {
                paragraph "Please try again and fill in all needed values"
                
            }
        }
    }
}

def sceneExtract() {

    logger('sceneExtract() DEVICE INFORMATION', 'debug')
    if (state.goveeHomeToken != null) {
    String goveeHomeToken = "Bearer " + state.goveeHomeToken

    def params = [
            uri   : 'https://app2.govee.com',
            path  : '/bff-app/v1/exec-plat/home',
            headers: ['Authorization': goveeHomeToken, 'Content-Type': 'application/json', 'appVersion': '5.6.01'],
        ]
        logger("sceneExtract(): Calling HTTP server", 'debug')
    try {
        httpGet(params) { resp ->
            def slurper = new JsonSlurper()
                resp.data.data.components.forEach {
                    String name = it.name.toString()
                    if (name == settings.goveeGroup) {
                        logger("sceneExtract(): found ${settings.goveeGroup} group moving forward", 'debug')
                        it.oneClicks.iotRules.forEach {  
                            if (it.get(0).containsKey("deviceObj")) {
                            logger("sceneExtract(): Tap to Run has deviceObj ${it.get(0).containsKey("deviceObj")} ", 'debug')
                            logger("sceneExtract(): Response data ${it}", 'debug')    
                            logger("sceneExtract(): found ${it.deviceObj.feastType} feastType", 'debug')
                            int feastType = it.deviceObj.feastType.get(0)
                            if (feastType == 0) {
                            devName = it.deviceObj.name.get(0) 
                            devSku = it.deviceObj.sku.get(0)
                            logger("sceneExtract(): Looking at ${devName} ${devSku}", 'info')  
                            logger("sceneExtract(): Number of rules are  ${it.rule.get(0).size()}", 'debug')
                           if (it.rule.get(0).get(0).cmdType == 3 || it.rule.get(0).get(0).cmdType == 4 || ((it.rule.get(0).get(0).cmdType >= 16 && it.rule.get(0).get(0).cmdType <= 19) && (slurper.parseText(it.rule.get(0).get(0).iotMsg)).msg.cmd == "ptReal" ) || it.rule.get(0).get(0).cmdType == 32) {    
                                logger("sceneExtract(): First rule is scene or DIY ${it.rule.get(0).get(0).cmdType}", 'debug')
                                if ((slurper.parseText(it.rule.get(0).get(0).cmdVal)).scenesStr == null && (slurper.parseText(it.rule.get(0).get(0).cmdVal)).startBri == null && (slurper.parseText(it.rule.get(0).get(0).cmdVal)).snapshotName == null) {
                                    sceneName = (slurper.parseText(it.rule.get(0).get(0).cmdVal)).diyName }
                                else if ((slurper.parseText(it.rule.get(0).get(0).cmdVal)).scenesStr == null && (slurper.parseText(it.rule.get(0).get(0).cmdVal)).diyName == null && (slurper.parseText(it.rule.get(0).get(0).cmdVal)).snapshotName == null) {
                                    sceneName = "Sleep starting at "+ (slurper.parseText(it.rule.get(0).get(0).cmdVal)).startBri + "% for " + (slurper.parseText(it.rule.get(0).get(0).cmdVal)).closeTime + " min"
                                    } 
                                else {
                                sceneName = (slurper.parseText(it.rule.get(0).get(0).cmdVal)).scenesStr
                                }
                                command = (slurper.parseText(it.rule.get(0).get(0).iotMsg)).msg.data.command
                            } else if (it.rule.get(0).get(1).cmdType == 3 || it.rule.get(0).get(1).cmdType == 4 || ((it.rule.get(0).get(1).cmdType >= 16 && it.rule.get(0).get(1).cmdType <= 19) && (slurper.parseText(it.rule.get(0).get(1).iotMsg)).msg.cmd == "ptReal" ) || it.rule.get(0).get(1).cmdType == 32) {
                                logger("sceneExtract(): Second rule is scene or DIY ${it.rule.get(0).get(1).cmdType}", 'debug')
                                logger("sceneExtract(): Second rule is scene or DIY ${(slurper.parseText(it.rule.get(0).get(1).iotMsg)).msg.cmd}", 'debug')
                                if ((slurper.parseText(it.rule.get(0).get(1).cmdVal)).scenesStr == null && (slurper.parseText(it.rule.get(0).get(1).cmdVal)).startBri == null && (slurper.parseText(it.rule.get(0).get(1).cmdVal)).snapshotName == null) {
                                    sceneName = (slurper.parseText(it.rule.get(0).get(1).cmdVal)).diyName }
                                else if ((slurper.parseText(it.rule.get(0).get(1).cmdVal)).scenesStr == null && (slurper.parseText(it.rule.get(0).get(1).cmdVal)).diyName == null && (slurper.parseText(it.rule.get(0).get(1).cmdVal)).snapshotName == null) {
                                    sceneName = "Sleep starting at "+ (slurper.parseText(it.rule.get(0).get(1).cmdVal)).startBri + "% for " + (slurper.parseText(it.rule.get(0).get(1).cmdVal)).closeTime + " min"
                                    }
                                else if ((slurper.parseText(it.rule.get(0).get(1).cmdVal)).scenesStr == null && (slurper.parseText(it.rule.get(0).get(1).cmdVal)).startBri == null && (slurper.parseText(it.rule.get(0).get(1).cmdVal)).diyName == null) {
                                    sceneName = "Snapshot "+ (slurper.parseText(it.rule.get(0).get(1).cmdVal)).snapshotName 
                                    }
                                else {
                                sceneName = (slurper.parseText(it.rule.get(0).get(1).cmdVal)).scenesStr
                                }
                            command = (slurper.parseText(it.rule.get(0).get(1).iotMsg)).msg.data.command                                
                            } else if ( it.rule.get(0).size() > 2 ) {
                                logger("sceneExtract(): First two rules failed falling back to third rule ${it.rule.get(0).get(2).cmdType}", 'debug')
                                if ((slurper.parseText(it.rule.get(0).get(2).cmdVal)).scenesStr == null && (slurper.parseText(it.rule.get(0).get(2).cmdVal)).startBri == null && (slurper.parseText(it.rule.get(0).get(2).cmdVal)).snapshotName == null) {
                                    logger("sceneExtract(): Processing third rule collect DIY name", 'debug')
                                    sceneName = (slurper.parseText(it.rule.get(0).get(2).cmdVal)).diyName }
                                else if ((slurper.parseText(it.rule.get(0).get(2).cmdVal)).scenesStr == null && (slurper.parseText(it.rule.get(0).get(2).cmdVal)).diyName == null && (slurper.parseText(it.rule.get(0).get(2).cmdVal)).snapshotName == null) {
                                    logger("sceneExtract(): Processing third rule collect sleep timer name", 'debug')
                                    sceneName = "Sleep starting at "+ (slurper.parseText(it.rule.get(0).get(2).cmdVal)).startBri + "% for " + (slurper.parseText(it.rule.get(0).get(2).cmdVal)).closeTime + " min"
                                    }
                                else if ((slurper.parseText(it.rule.get(0).get(2).cmdVal)).scenesStr == null && (slurper.parseText(it.rule.get(0).get(2).cmdVal)).startBri == null && (slurper.parseText(it.rule.get(0).get(2).cmdVal)).diyName == null) {
                                    logger("sceneExtract(): Processing third rule collect Snapshot", 'debug')
                                    sceneName = "Snapshot "+ (slurper.parseText(it.rule.get(0).get(2).cmdVal)).snapshotName 
                                    }
                                else {
                                logger("sceneExtract(): Processing third rule collect scene name", 'debug')    
                                sceneName = (slurper.parseText(it.rule.get(0).get(2).cmdVal)).scenesStr
                                }
                                command = (slurper.parseText(it.rule.get(0).get(2).iotMsg)).msg.data.command
                            } else {
                                logger("sceneExtract(): No Third rule to process. No valid data to extract", 'debug')
                            }
                            logger("sceneExtract(): Scene Name is ${sceneName}: command is ${command}", 'debug')
                            diyAdd(devSku, sceneName, command)
                            } else {
                                logger("sceneExtract(): Found scene that is not extractable. Moving on", 'debug')
                            }
                            } else {
                               logger("sceneExtract(): Tap to run does not contain device only action. Ignoring", 'debug') 
                            }
                        }
                    } 
                }
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        logger("deviceSelect() Error: e.statusCode ${e.statusCode}", 'error')
        logger("deviceSelect() ${e}", 'error')

        return 'unknown'
    }
    } 
    dynamicPage(name: 'sceneExtract', title: 'Scene Extract', uninstall: false, install: false, submitOnChange: true, nextPage: "sceneManagement")
    {
        if (state.goveeHomeToken != null) {
            section('<b>Extracted command below:</b>') {
                paragraph "Device name ${devName}"
                paragraph "Scene name is ${sceneName}"
                paragraph "Command is <mark>${command.inspect().replaceAll("\'", "\"")}</mark>"
                paragraph "This command will work with any device with model ${devSku}"
            }
            
        } else {
            section('<b>Extracted command below:</b>') {
                paragraph "You either have not logging into with your Govee Home creds or the login has expired"
                paragraph "Please return to the Scene Management Menu by clicking next below. Then click on the button to enter your Govee Home Account credentials"
                paragraph "Once the Credentials are setup you should be able to extract Scenes"

            }
        }
    }
}

def sceneExtract2() {

    logger('sceneExtract2() Credential Controll', 'debug')
    dynamicPage(name: 'sceneExtract2', title: 'API Credential configuration', uninstall: false, install: false, submitOnChange: true, nextPage: "sceneManagement")
    {
        section('Govee Home Creds')
        {
            paragraph "Please provide your gove home token."
            input 'goveeEmail', 'string', title: 'Please enter your Email used to log into Govee Home', required: false, submitOnChange: true
            input 'goveePassword', 'password', title: 'Please enter your Govee Home password', required: false, submitOnChange: true
            input "goveeHomeLogin" , "button",  title: "Click here to attempt login"
            if (settings.goveeEmail && settings.goveePassword) {
                if (now() > state.goveeHomeExpiry) {
                    paragraph "Token has <mark>not valid</mark>. Please Login"
                } else {
                    Date dateExpire = new Date(state.goveeHomeExpiry)
                paragraph "Token is <mark>valid</mark>"
                paragraph "Token expires on ${dateExpire}"
                }
            } else {
                paragraph "Please add a Email and Password to Obtain your Govee Home Token"
            }
            paragraph "To clear token clear Email address and password and then click button below to clear token"
            input "goveeHomeTokenClear" , "button",  title: "Click here to reset Govee Home token"
        }
    }
}

def about() {
    dynamicPage(name: 'about', title: 'About Govee Integration with HE', uninstall: false, install: false, nextPage: "mainPage")
    {
        section()
        {
            paragraph image: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg', 'Govee Integration'
        }
        section('Support the Project')
        {
            paragraph 'Govee is provided free for personal and non-commercial use.  I have worked on this app in my free time to fill the needs I have found for myself and others like you.  I will continue to make improvements where I can. If you would like you can donate to continue to help with development please use the link below.'
            href(name: 'donate', style:'embedded', title: "Consider making a \$5 or \$10 donation today to support my ongoing effort to continue improving this integration.", url: 'https://www.paypal.me/mavrrick58')
            paragraph("<style>/* The icon */ .help-tip{ 	position: absolute; 	top: 50%; 	left: 50%; 	transform: translate(-50%, -50%); 	margin: auto; 	text-align: center; 	border: 2px solid #444; 	border-radius: 50%; 	width: 40px; 	height: 40px; 	font-size: 24px; 	line-height: 42px; 	cursor: default; } .help-tip:before{     content:'?';     font-family: sans-serif;     font-weight: normal;     color:#444; } .help-tip:hover p{     display:block;     transform-origin: 100% 0%;     -webkit-animation: fadeIn 0.3s ease;     animation: fadeIn 0.3s ease; } /* The tooltip */ .help-tip p {    	display: none; 	font-family: sans-serif; 	text-rendering: optimizeLegibility; 	-webkit-font-smoothing: antialiased; 	text-align: center; 	background-color: #FFFFFF; 	padding: 12px 16px; 	width: 178px; 	height: auto; 	position: absolute; 	left: 50%; 	transform: translate(-50%, 5%); 	border-radius: 3px; /* 	border: 1px solid #E0E0E0; */ 	box-shadow: 0 0px 20px 0 rgba(0,0,0,0.1); 	color: #37393D; 	font-size: 12px; 	line-height: 18px; 	z-index: 99; } .help-tip p a { 	color: #067df7; 	text-decoration: none; } .help-tip p a:hover { 	text-decoration: underline; } /* The pointer of the tooltip */ .help-tip p:before { 	position: absolute; 	content: ''; 	width: 0; 	height: 0; 	border: 10px solid transparent; 	border-bottom-color:#FFFFFF; 	top: -9px; 	left: 50%; 	transform: translate(-50%, -50%); }  /* Prevents the tooltip from being hidden */ .help-tip p:after { 	width: 10px; 	height: 40px; 	content:''; 	position: absolute; 	top: -40px; 	left: 0; } /* CSS animation */ @-webkit-keyframes fadeIn {     0% { opacity:0; }     100% { opacity:100%; } } @keyframes fadeIn {     0% { opacity:0; }     100% { opacity:100%; } }</style><div class='help-tip'><p>This is the inline help tip! It can contain all kinds of HTML. Style it as you please.<br /><a href='#'>Here is a link</a></p></div>")
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
            addChildDevice('Mavrrick', 'Govee v2 Device Manager', "Govee_v2_Device_Manager" , location.hubs[0].id, [
            'name': 'Govee v2 Device Manager',
            'label': 'Govee v2 Device Manager',
             'data': [
                'apiKey': settings.APIKey
             ],
             'completedSetup': true,
         ])
    state.isInstalled = true
    state.diyEffects = [:]
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    List childDNI = getChildDevices().deviceNetworkId
    if (childDNI.contains("Govee_v2_Device_Manager") == false) {
        logger("goveeDevAdd()  configuring Govee v2 Device Manager", 'info')
        addChildDevice('Mavrrick', 'Govee v2 Device Manager', "Govee_v2_Device_Manager" , location.hubs[0].id, [
            'name': 'Govee v2 Device Manager',
            'label': 'Govee v2 Device Manager',
             'data': [
                'apiKey': settings.APIKey
             ],
             'completedSetup': true,
         ])
    }
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 3
    if (settings.APIKey != state.APIKey ) {
        child.each {
            logger('initialize() API key has been updated. Calling child devices to udpate', 'debug')
            it.apiKeyUpdate()
        }
        state?.APIKey = settings.APIKey
    }
/*    if (goveeDev) {
        def goveeAdd = settings.goveeDev - child.label
        logger("initialize() Found child devices ${child}", 'debug')
        logger("initialize() Govee Light/Switch/Plugs to add ${goveeAdd}.", 'info')
        if (goveeAdd) {
            goveeDevAdd(goveeAdd)
                }
        }
          else {
        logger('initialize() No devices to add', 'info')
          } */
}

def uninstalled() {
    // external cleanup. No need to unsubscribe or remove scheduled jobs
    // 1.4 Remove dead virtual devices
    getChildDevices()?.each
    { childDevice ->
            deleteChildDevice(childDevice.deviceNetworkId)
    }
}

def sendnotification (type, value) {
    if (notifyEnabled) {
        String notificationText = "${type} has reached threshold of ${value}. You will have issues if this get to zero"
        notificationDevice?.each {
            it.deviceNotification(notificationText)
        }
    }
}

def diyAdd(devSKU, diyName, command) {
    logger("diyAdd(): Attempting add DIY Scene ${devSKU}:${diyName}:${command}", 'trace')
    command = command.inspect().replaceAll("\'", "\"")
    Map diyEntry = [:]
    diyEntry.put("name", diyName)
    diyEntry.put("cmd", command)
    logger("diyAdd(): Trying to add ${diyEntry}", 'debug')
    logger("diyAdd(): keys are  ${state.diyEffects.keySet()}", 'debug')
    if (state.diyEffects.containsKey(devSKU) == false) {
        logger("diyAdd(): Device ${devSKU} not found", 'debug')
        logger("diyAdd(): New Device. Starting at 1001", 'debug')
        int diyAddNum = 1001
        Map diyEntry2 = [:]
        diyEntry2.put(diyAddNum,diyEntry)
        state.diyEffects.put(devSKU,diyEntry2)
    } else {
        compare = state.diyEffects."${devSKU}".toString()
        matchVal = compare.indexOf(diyName)
        if (matchVal > 0) {
            logger("diyAdd(): Scene with same name already present", 'debug')
            } else {
            logger("diyAdd(): Device ${devSKU} was found. Adding Scene to existing scene list", 'debug')
            diySize = state.diyEffects."${devSKU}".size()
            diyAddNum = (diySize + 1001).toInteger()
            logger("diyAdd(): Current DiY size is ${diySize}", 'debug')
            state.diyEffects."${devSKU}".put(diyAddNum,diyEntry)
        }
    }
    writeDIYFile()
}

/**
 *  diyAddManual()
 *
 *  Method to manually add shared Scenes to Hubitat.
 **/

def diyAddManual(String devSKU, String diyName, String command) {
    logger("diyAdd(): Attempting add DIY Scene ${devSKU}:${diyName}:${command}", 'trace')
    Map diyEntry = [:]
    diyEntry.put("name", diyName)
    diyEntry.put("cmd", command)
    logger("diyAdd(): Trying to add ${diyEntry}", 'debug')
    logger("diyAdd(): keys are  ${state.diyEffects.keySet()}", 'debug')
    if (state.diyEffects.containsKey(devSKU) == false) {
        logger("diyAdd(): Device ${devSKU} not found", 'debug')
        logger("diyAdd(): New Device. Starting at 1001", 'debug')
        int diyAddNum = 1001
        Map diyEntry2 = [:]
        diyEntry2.put(diyAddNum,diyEntry)
        state.diyEffects.put(devSKU,diyEntry2)
    } else {
        compare = state.diyEffects."${devSKU}".toString()
        matchVal = compare.indexOf(diyName)
        if (matchVal > 0) {
            logger("diyAdd(): Scene with same name already present", 'debug')
            } else {
            logger("diyAdd(): Device ${devSKU} was found. Adding Scene to existing scene list", 'debug')
            diySize = state.diyEffects."${devSKU}".size()
            diyAddNum = (diySize + 1001).toInteger()
            logger("diyAdd(): Current DiY size is ${diySize}", 'debug')
            state.diyEffects."${devSKU}".put(diyAddNum,diyEntry)
        }
    }
    writeDIYFile()
}

/**
 *  diyUpdateManual()
 *
 *  Method to manually add shared Scenes to Hubitat.
 **/
def diyUpdateManual(String devSKU, int diyAddNum, String diyName, String command) {
    logger("diyUpdateManual(): Attempting add DIY Scene ${devSKU}:${diyAddNum}:${diyName}:${command}", 'trace')
    Map diyEntry = [:]
    diyEntry.put("name", diyName)
    diyEntry.put("cmd", command)
    logger("diyUpdateManual(): Trying to add ${diyEntry}", 'debug')
    logger("diyUpdateManual(): keys are  ${state.diyEffects.keySet()}", 'debug')
    if (state.diyEffects.containsKey(devSKU) == false) {
        logger("diyUpdateManual(): Device ${devSKU} not found.", 'debug')
        Map diyEntry2 = [:]
        diyEntry2.put(diyAddNum,diyEntry)
        state.diyEffects.put(devSKU,diyEntry2)
    } else {
            logger("diyUpdateManual(): Device ${devSKU} was found. Updating scene", 'debug')
            state.diyEffects."${devSKU}".put(diyAddNum,diyEntry)
    }
    writeDIYFile()
}

/**
 *  logger()
 *
 *  Wrapper function for all logging.
 **/
private logger(msg, level = 'debug') {
    switch (level) {
        case 'error':
            if (state.loggingLevelIDE >= 1) { log.error msg }
            break

        case 'warn':
            if (state.loggingLevelIDE >= 2)  { log.warn msg }
            break

        case 'info':
            if (state.loggingLevelIDE >= 3) { log.info msg }
            break

        case 'debug':
            if (state.loggingLevelIDE >= 4) { log.debug msg }
            break

        case 'trace':
            if (state.loggingLevelIDE >= 5) { log.trace msg }
            break

        default:
            log.debug msg
            break
    }
}


/**
 *  goveeDevAdd()
 *
 *  Wrapper function to create devices.
 **/
def goveeDevAdd() { //testing

// private goveeDevAdd(goveeAdd) {
//    def goveeAdd = settings.goveeDev - child.label    // testing
//    def devices = goveeAdd
    def devices = settings.goveeDev - child.label
    def drivers = getDriverList()
    mqttDevice = getChildDevice('Govee_v2_Device_Manager')
    logger("goveeDevAdd() drivers detected are ${drivers}", 'debug')
    logger("goveeDevAdd() Childred DNI  ${childDNI} MQTT device DNI ${mqttChildredDNI}", 'debug')
    logger("goveeDevAdd() $devices are selcted to be integrated", 'info')
    logger('goveeDevAdd() DEVICE INFORMATION', 'info')
    state.goveeAppAPI.each {
        def String dniCompare = "Govee_"+it.device
        def String deviceName = it.deviceName        
        if (childDNI.contains(dniCompare) == false) {
            logger("goveeDevAdd(): ${deviceName} is a new DNI. Passing to driver setup if selected.", 'debug')
            if (devices.contains(deviceName) == true) {
                def String deviceID = it.device
                def String deviceModel = it.sku
                def String devType = it.type
                def commands = []
                def capType = []
                def int ctMin = 0
                def int ctMax = 0
                it.capabilities.each {
                    logger ("goveeDevAdd(): ${it} instance is ${it.instance}",'trace')
                    commands.add(it.instance)
                    capType.add(it.type)
                    if (it.instance == "colorTemperatureK") {
                        logger ("goveeDevAdd(): ${it} instance is ${it.instance} Parms is ${it.parameters} range is ${it.parameters.range} min is ${it.parameters.range.min}",'trace')
                        ctMin = it.parameters.range.min
                        ctMax = it.parameters.range.max
                        logger ("goveeDevAdd(): Min is ${ctMin} Max is ${ctMax}",'trace')
                    }
                }
                logger ("goveeDevAdd(): ${deviceID} ${deviceModel} ${deviceName} ${devType} ${commands}",'trace')  
//                setBackgroundStatusMessage("Processing device ${deviceName}")
                if (devType == "devices.types.light") {
                    if (commands.contains("colorRgb") && commands.contains("colorTemperatureK") && commands.contains("segmentedBrightness") && commands.contains("segmentedColorRgb") && commands.contains("dreamViewToggle")) {
                        String driver = "Govee v2 Color Lights Dreamview Sync"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                        setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    } else if (commands.contains("colorRgb") && commands.contains("colorTemperatureK") && commands.contains("segmentedBrightness") && commands.contains("segmentedColorRgb")) {
                        String driver = "Govee v2 Color Lights 3 Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    } else if (commands.contains("colorRgb") && commands.contains("colorTemperatureK")  && commands.contains("segmentedBrightness")) {
                        String driver = "Govee v2 Color Lights 2 Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    } else if (commands.contains("colorRgb") && commands.contains("colorTemperatureK")  && commands.contains("segmentedColorRgb") && commands.contains("dreamViewToggle")) {
                        String driver = "Govee v2 Color Lights 4 Dreamview Sync"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    } else if (commands.contains("colorRgb") && commands.contains("colorTemperatureK")  && commands.contains("segmentedColorRgb")) {
                        String driver = "Govee v2 Color Lights 4 Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    } else if (commands.contains("colorRgb") == true && commands.contains("colorTemperatureK")) {
                        String driver = "Govee v2 Color Lights Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        } 
                    } else if (commands.contains("colorTemperatureK")) {
                        String driver = "Govee v2 White Lights with CT Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }     
                    } else if (deviceModel == "H6091" || deviceModel == "H6092") {
                        String driver = "Govee v2 Galaxy Projector"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    } else {
                        String driver = "Govee v2 White Light Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)              
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }                    
                    }    
                } else if (devType == "devices.types.air_purifier") {
                    if (commands.contains("colorRgb") == true) {
                        String driver = "Govee v2 Air Purifier with RGB Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    } else {
                        String driver = "Govee v2 Air Purifier Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    }    
                } else if (devType == "devices.types.heater") {
                    if (deviceModel == "H7133") {
                        String driver = "Govee v2 H7133 Heating Appliance Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    } else if (commands.contains("colorRgb")) {
                        String driver = "Govee v2 Heating Appliance with RGB Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    } else {
                        String driver = "Govee v2 Heating Appliance Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    }
                } else if (devType == "devices.types.humidifier") {
                    if (commands.contains("colorRgb")) {
                        String driver = "Govee v2 Humidifier with RGB Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    } else {
                        String driver = "Govee v2 Humidifier Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    }
                } else if (devType == "devices.types.fan") {
                    if (commands.contains("colorRgb")) {
                        String driver = "Govee v2 Fan with RGB Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    } else {
                        String driver = "Govee v2 Fan Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }     
                    }
                } else if (devType == "devices.types.socket") {
                    String driver = "Govee v2 Sockets Driver"
                    if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                    } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                        setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                    }         
                } else if (devType == "devices.types.ice_maker") {
                    String driver = "Govee v2 Ice Maker"
                    if (drivers.contains(driver)) {
                        logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                        setBackgroundStatusMessage("Installing device ${deviceName}")
                        mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                    } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                        setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                    }     
                } else if (devType == "devices.types.kettle") {
                    String driver = "Govee v2 Kettle Driver"
                    if (drivers.contains(driver)) {
                        logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                        setBackgroundStatusMessage("Installing device ${deviceName}")
                        mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                    } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                        setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                    } 
                } else if (devType == "devices.types.thermometer") {
                    String driver = "Govee v2 Thermo/Hygrometer Driver"
                    if (drivers.contains(driver)) {
                        logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                        setBackgroundStatusMessage("Installing device ${deviceName}")
                        mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                    } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                        setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                    }
                } else if (devType == "devices.types.sensor") {
                    String driver = "Govee v2 Presence Sensor"
                    if (drivers.contains(driver)) {
                        logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                        setBackgroundStatusMessage("Installing device ${deviceName}")
                        mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                    } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                        setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                    }    
                } else if (devType == "devices.types.aroma_diffuser") {
                    String driver = "Govee v2 Aroma Diffuser Driver with Lights"
                    if (drivers.contains(driver)) {
                        logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                        setBackgroundStatusMessage("Installing device ${deviceName}")
                        mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                    } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                        setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                    }     
                } else {
                    String driver = "Govee v2 Research Driver"
                    if (drivers.contains(driver)) {
                        logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                        addDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver ${driver} is not installed. Please correct and try again")
                    } else {
                    logger('goveeDevAdd(): The device does not have a driver and you do not have the '+driver+' loaded. Please load it and forward the device details to the developer', 'info')    
                    }
                }
            } else {
                logger("goveeDevAdd(): Device is not selected to be added. ${deviceName} not being installed", 'debug')
            }
        } else {
            logger("goveeDevAdd(): Device ID matches child DNI. ${deviceName} already installed", 'debug')
            setBackgroundStatusMessage("Device ${deviceName} is already installed. Ignored")
        }                
    }
    state?.installDev = goveeDev
    atomicState.backgroundActionInProgress = false
    logger('goveeDevAdd() Govee devices integrated', 'info')
}


/**
 *  goveeLightManAdd()
 *
 *  Wrapper function for all logging.
 **/
private goveeLightManAdd(String model, String ip, String name) {
    def newDNI = "Govee_" + ip
    logger("goveeLightManAdd() Adding ${name} Model: ${model} at ${ip} with ${newDNI}", 'info')
    logger('goveeLightManAdd() DEVICE INFORMATION', 'info')
    if (childDNI.contains(newDNI) == false) {
        String driver = "Govee Manual LAN API Device"
        logger("goveeLightManAdd(): ${deviceName} is a new DNI. Passing to driver setup if selected.", 'debug') 
        logger("goveeLightManAdd():  configuring ${deviceName}", 'info')
        addManLightDeviceHelper(driver, ip, name, model)
        } else { 
        logger("goveeLightManAdd(): Manual add request ignored as device is already added.", 'info')
        }
    }



/**
 *  appButtonHandler()
 *
 *  Handler for when Buttons are pressed in App. These are used for the Scene extract page.
 **/

private def appButtonHandler(button) {
    if (button == "sceneDIYInitialize") {
        state?.diyEffects = [:]
    } else if (button == "goveeHomeLogin") {
        if (settings.goveeEmail && settings.goveePassword) {
            bodyParm = '{"email": "'+settings.goveeEmail+'", "password": "'+settings.goveePassword+'"}'
            logger("appButtonHandler(): bodyparm to be passed:  ${bodyParm}", 'trace')
            def params = [
                uri   : 'https://community-api.govee.com',
                path  : '/os/v1/login',
                headers: ['Content-Type': 'application/json'],
                body: bodyParm
            ]
            logger("appButtonHandler(): parms to be passed:  ${params}", 'trace')
            try {
                httpPost(params) { resp ->
                    logger("appButtonHandler(): response is ${resp.data}", 'trace')
                    status = resp.data.status
                    msg = resp.data.message
                    logger("appButtonHandler(): status is ${status}: Message ${msg}", 'info')
                    if (status == 200) {
                        state.goveeHomeToken = resp.data.data.token
                        state.goveeHomeExpiry = resp.data.data.expiredAt.value
                        logger("appButtonHandler(): response is ${resp.data}", 'trace')
                        logger("appButtonHandler(): token is ${state.goveeHomeToken} and expires at ${state.goveeHomeExpiry}", 'info')
                    } else {
                        logger("appButtonHandler(): Login failed check error above and correct", 'info')
                    }
                }
                } catch (groovyx.net.http.HttpResponseException e) {
                    logger("appButtonHandler(): Error: e.statusCode ${e.statusCode}", 'error')
                    logger("appButtonHandler(): ${e}", 'error')

                return 'unknown'
            }
        }        
    } else if (button == "goveeHomeTokenClear") {
        state?.goveeHomeToken = null
        state?.goveeHomeExpiry = 0
    } else if (button == "deviceListRefresh") {
        retrieveGoveeAPIData()
    } else if (button == "savDIYScenes") {
        saveFile()
    } else if (button == "resDIYScenes") {
        loadFile()
    }
    
}

def apiRateLimits(type, value) {
    logger("apiRateLimits($type, $value)", 'info')
    if (type == 'DailyLimitRemaining') {
        state.dailyLimit = value
        if ( state.dailyLimit.toInteger() < apiV1threshold) {
            sendnotification('Govee Lights, Plugs, Switches APi Rate Limit', state.dailyAppLimit)
        }
    }
    else if (type == 'DailyLimitRemainingV2') {
        state.dailyAppLimit = value
        log.debug "${state.dailyAppLimit}"
        if ( state.dailyAppLimit.toInteger() < apiV2threshold) {
            log.debug 'validated api limit to low. Sending notificatoin'
            sendnotification('Govee Appliance API rate Limit', state.dailyAppLimit)
        }
    }
}


///////////////////////////////////////////
// Helper methods for certain tasks // 
///////////////////////////////////////////


private String escapeStringForPassword(String str) {
    //logger("$str", "info")
    if (str) {
//        str = str.replaceAll(" ", "\\\\ ") // Escape spaces.
//        str = str.replaceAll(",", "\\\\,") // Escape commas.
        str = str.replaceAll("=", "\u003D") // Escape equal signs.
//        str = str.replaceAll("\"", "\u0022") // Escape double quotes.
//    str = str.replaceAll("'", "_")  // Replace apostrophes with underscores.
    }
    else {
        str = 'null'
    }
    return str
}

def getDriverList() {
    logger('getDriverList(): Attempting to obtain Driver List', 'debug')
	def result = []
	if (location.hub.firmwareVersionString >= "2.3.6.126") {
		def params = [
			uri: getBaseUrl(),
			path: "/hub2/userDeviceTypes",
			headers: [
				Cookie: state.cookie
			],
		  ignoreSSLIssues: true
		  ]

		try {
			httpGet(params) { resp ->
			resp.data.each { 
                if (it.namespace == "Mavrrick") {
                result += it.name
                    }
                } 
			}
		} catch (e) {
			log.error "Error retrieving installed drivers: ${e}"
		}

	}
	return result
}

def getBaseUrl() {
	def scheme = sslEnabled ? "https" : "http"
	def port = sslEnabled ? "8443" : "8080"
	return "$scheme://127.0.0.1:$port"
}

private def addManLightDeviceHelper( String driver, String ip, String deviceName, String deviceModel) {
    
            addChildDevice('Mavrrick', driver, "Govee_${ip}" , location.hubs[0].id, [
                'name': 'Govee Manual LAN API Device',
                'label': deviceName,
                'data': [
                    'IP': ip,
                    'deviceModel': deviceModel,
                    'ctMin': 2000,
                    'ctMax': 9000
                        ],
                'completedSetup': true,
                ])    
}  


///////////////////////////////////////////////////////////////////////////
// Method to return the Govee API Data for specific device from Prent App //
///////////////////////////////////////////////////////////////////////////

def retrieveGoveeAPI(deviceid) {
    if (debugLog) "retrieveGoveeAPI(): ${deviceid}"
    def goveeAppAPI = state.goveeAppAPI.find{it.device==deviceid}
    return goveeAppAPI
}

def retrieveGoveeAPIData() {
        logger('appButtonHandler() DEVICE INFORMATION', 'debug')
        def params = [
            uri   : 'https://openapi.api.govee.com',
            path  : '/router/api/v1/user/devices',
            headers: ['Govee-API-Key': settings.APIKey, 'Content-Type': 'application/json'],
            ]

        try {
            httpGet(params) { resp ->
                //List each device assigned to current API key
                state.goveeAppAPI = resp.data.data
                state.goveeAppAPIdate = now()
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        logger("appButtonHandler() Error: e.statusCode ${e.statusCode}", 'error')
        logger("appButtonHandler() ${e}", 'error')

        return 'unknown'
    }
}


void saveFile() {
    log.debug ("saveFile: Backing up ${state.diyEffects} for DIY Scene data")
    String listJson = "["+JsonOutput.toJson(state.diyEffects)+"]" as String
    uploadHubFile("$goveeDIYScenesFileBackup",listJson.getBytes())
}

void loadFile() {
    byte[] dBytes = downloadHubFile("$goveeDIYScenesFileBackup")
    tmpEffects = (new JsonSlurper().parseText(new String(dBytes))) as List
    log.debug ("loadFile: Restored ${tmpEffects.get(0)} from ${goveeDIYScenesFile }")
    state.diyEffects = tmpEffects.get(0)
    log.debug ("loadFile: Restored ${state.diyEffects?.size() ?: 0} disabled records")
    writeDIYFile()
}

void writeDIYFile() {
    log.debug ("writeDIYFile: Writing DIY Scenes to flat file for Drivers")
    String listJson = "["+JsonOutput.toJson(state.diyEffects)+"]" as String
    uploadHubFile("$goveeDIYScenesFile",listJson.getBytes())
}

def setBackgroundStatusMessage(msg, level="info") {
	if (statusMessage == null)
		statusMessage = ""
	if (level == "warn") log.warn msg
	if (settings?.txtEnable != false && level == "info") log.info msg
	statusMessage += "${msg}<br>"
}

def getBackgroundStatusMessage() {
	return statusMessage
}

def showHideNextButton(show) {
	if(show) paragraph "<script>\$('button[name=\"_action_next\"]').show()</script>"
	else paragraph "<script>\$('button[name=\"_action_next\"]').hide()</script>"
}
