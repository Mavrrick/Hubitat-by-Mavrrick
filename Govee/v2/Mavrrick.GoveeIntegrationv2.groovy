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
    documentationLink: "https://docs.google.com/document/d/e/2PACX-1vRsjfv0eefgPGKLYffNpbZWydtp0VqxFL_Xcr-xjRKgl8vga18speyGITyCQOqlQmyiO0_xLJ9_wRqU/pub",
    iconUrl: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg',
    iconX2Url: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg',
    iconX3Url: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg',
    singleThreaded: true,
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
import hubitat.helper.HexUtils

@Field static List child = []
@Field static List childDNI = []
@Field static Map goveeScene = [:]
@Field static def goveeApiRespons = {}
@Field static final String goveeDIYScenesFileBackup = "GoveeLanDIYScenes_Backup.json"
@Field static final String goveeDIYScenesFile = "GoveeLanDIYScenes.json"
@Field static String statusMessage = ""
@Field static String BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

@Field static final Map deviceTag =  // a300 line device specific codes
	["H6008":"01",
     "H6022":"585a", 
     "H6052":"01",
     "H6078":"0c09", 
     "H6061":"04", 
     "H6065":"04", 
     "H6066":"04", 
     "H6067":"04", 
     "H6069":"04", 
     "H606A":"04", 
     "H6079":"", 
     "H610A":"", 
     "H6092":"560b",
     "H6093":"560b",
     "H6094":"560b",
     "H6095":"560b",
     "H609D":"560b"]

@Field static final Map deviceTagll = // Final Line code for special device types
	["H6061":"2d", 
     "H6065":"47", 
     "H6066":"2d", 
     "H6067":"2d", 
     "H6069":"2d", 
     "H606A":"2d"]

@Field static final Map goveeDevOffsets = // values to properly extract device hex string data
	["H6008":["start":0, "line1End":28, "offset":0],
     "H6022":["start":2, "line1End":28, "offset":0],
     "H6052":["start":4, "line1End":32, "offset":4],
     "H6061":["start":10, "line1End":38, "offset":10],
     "H6065":["start":10, "line1End":38, "offset":10],
     "H6066":["start":10, "line1End":38, "offset":10],
     "H6067":["start":10, "line1End":38, "offset":10],
     "H6069":["start":10, "line1End":38, "offset":10],
     "H606A":["start":10, "line1End":38, "offset":10],
     "H6078":["start":2, "line1End":28, "offset":0],
     "H6079":["start":0, "line1End":30, "offset":0],
     "H6092":["start":2, "line1End":28, "offset":0],
     "H6093":["start":2, "line1End":28, "offset":0],
     "H6094":["start":2, "line1End":28, "offset":0],
     "H6095":["start":2, "line1End":28, "offset":0],
     "H609D":["start":2, "line1End":28, "offset":0],
     "H610A":["start":0, "line1End":30, "offset":0]]

@Field static final List goveeDevPtURL = // devices that use ptURL command and may cause extraction process to fail.
	["H6800","H6810","H6811","H6840","H70B1","H70B3","H70B4","H70BC"]

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
    page(name: 'sceneExtract3', title: 'Extract Lan Scenes')
    page(name: 'sceneManualAdd', title: 'Maunally Add Scenes')
    page(name: 'sceneManualAdd2', title: 'Maunally Add Results')
    page(name: 'sceneManualUpdate', title: 'Update Scene data') 
    page(name: 'sceneManualUpdate2', title: 'Update Scene data') 
    page(name: 'sceneGoveeExtract', title: 'Update Scene data')
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
//		logger("deviceSelect2: Install chosen devices", 'debug')
		atomicState.backgroundActionInProgress = true
         logger("deviceSelect2: Background action in progress status should be true =  ${atomicState.backgroundActionInProgress}", 'debug')
		runInMillis(1,goveeDevAdd)
	}
	if (atomicState.backgroundActionInProgress != false) {
//        logger("deviceSelect2: Install chosen devices ${atomicState.backgroundActionInProgress}", 'debug')
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
//        logger("deviceSelect2: Install chosen devices ${atomicState.backgroundActionInProgress}", 'debug')
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
    Map options = [:] 
    lanApiDevices = getChildDevice('Govee_v2_Device_Manager').retrieveApiDevices()
    if (lanApiDevices != null) {
        lanApiDevicesId = lanApiDevices.keySet() as List
        logger("deviceLanManual()  Device id's to match with ${lanApiDevicesId}", 'debug')
            lanApiDevices.keySet().each {
            String deviceName = it 
            String deviceip = lanApiDevices."${it}".ip
            logger("deviceLanManual() $deviceName ${lanApiDevices."${it}".ip}", 'debug')
            options["${deviceip}"] = deviceip
            }            
    } else {
        logger("deviceLanManual() No LAN API device currently detected", 'debug')    
    }
        logger("deviceLanManual() $options", 'debug')
    dynamicPage(name: 'deviceLanManual', title: 'Manual Setup for LAN API Enabled Devices', uninstall: false, install: false, nextPage: "deviceLanManual2" )
    {
        section('<b>***Warning***</b> Using the manual Addd option will potentially severaly limit your use of the device. This should be a last resort and only used if the device does not support adding with the normal method using the Cloud API. LAN API control can be enable on traditionally added devices as well.')
        {
            if (lanApiDevices != null) {
                paragraph 'Please enter the needed parameters below to create your device '
                input(name: 'goveeDevName', type: 'string', required:false, title: 'Name of device.', description: 'E.g. Bedroom Lights')
                input(name: 'goveeManSelection', type: 'enum', required:false, description: 'Please select the devices you wish to integrate.', multiple:false,
                    options: options.sort() , width: 8, height: 1)
                paragraph 'Click the next button when you are ready to create the device. '
            } else {
                paragraph 'There are currently no LAN API devices detected on your network. Please ensure the device Settings are updated in the Govee Home app to turn on "LAN Control" '
            }
        }
    }
}

def deviceLanManual2() {
    mqttDevice = getChildDevice('Govee_v2_Device_Manager')
    String deviceID = "default"
    String ip = "default"
    String deviceModel = "default"
    List childDNI = []
    if (mqttDevice.getChildDevices()) {
        childDNI = mqttDevice.getChildDevices().deviceNetworkId
    }
    if (settings.goveeManSelection && settings.goveeDevName) {
        lanApiDevices = getChildDevice('Govee_v2_Device_Manager').retrieveApiDevices()

        lanApiDevices.keySet().each {
                if (lanApiDevices."${it}".ip == settings.goveeManSelection) {
                    deviceID = it 
                    ip = lanApiDevices."${it}".ip
                    deviceModel = lanApiDevices."${it}".sku
                    logger("deviceLanManual2() DEVICE INFORMATION Name: Matched device and found ip: ${ip}, sku: ${deviceModel}, deviceid: ${deviceID}", 'debug')
                    
            }

        } 
        logger("deviceLanManual2() Device Add information Name: ${settings.goveeDevName}, SKU: ${deviceModel}, ip: ${ip}, Deviceid: ${deviceID}", 'debug')
        if (!childDNI.contains("Govee_"+deviceID)) {
            String driver = "Govee Manual LAN API Device"
            mqttDevice.addManLightDeviceHelper(driver,  deviceID, ip, settings.goveeDevName, deviceModel)
        } else {
            logger("deviceLanManual2() Add Aborted for  Name: ${settings.goveeDevName} deviceid: ${deviceID}", 'debug')
        }
    }
    dynamicPage(name: 'deviceLanManual2', title: 'Results of manual device add', uninstall: false, install: false, nextPage: "mainPage")
    {
        if (settings.goveeManSelection && settings.goveeDevName) {
            if (!childDNI.contains("Govee_"+deviceID)) {
                section('<b>Device Manual Add</b>') {
                paragraph "Attempted manual add of ${settings.goveeDevName} at ip ${settings.goveeManSelection}."
                paragraph "Click Next to return to the main menu."
                }
            } else {
                section('<b>Device Manual Add</b>') {
                paragraph "Manual add of device aborted as it was already present"
                paragraph "Click Next to return to the main menu."
                }
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
            href 'sceneExtract', title: 'Extract Scene from Tap to Run', description: 'Click here to perform Tap-To-Run analysis and extract DIY and Snapshots'
            href 'sceneGoveeExtract', title: 'Extract Govee Scenes from API (New Method, May not work for new device types that are not tested)', description: 'Click here to extract Govee Default scenes from Govee API for a specific device'
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
            paragraph "Retrieve scenes for all devices"
            input "resGovScenes" , "button",  title: "Retrieve all default Govee scense to local files"
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

def sceneGoveeExtract() {
    goveeScene = [:]
    dynamicPage(name: 'sceneGoveeCreate', title: 'Extract All Govee Scenes from Govee API', uninstall: false, install: false, submitOnChange: false, nextPage: "sceneExtract3")
    {
        section('<b>Govee Scene Retrieval</b>')
        {
            paragraph "Enter the device Model number."
            input 'devsku', 'string', title: '5 charecter Model (ie H6172)', required: false, default: ""

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
    addedScenes = "<table> <tr> <th>Device Model</th> <th>Name</th> <th>Command</th> </tr>"

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
                        logger("sceneExtract(): Processing IOTrules found  ${it.oneClicks.iotRules}", 'debug')
                        it.oneClicks.iotRules.forEach {
                            if (it == null) {
                                logger("sceneExtract(): iotRule is blank, skipping", 'debug')
                            } else {
                            logger("sceneExtract(): Response data ${it}", 'debug')
                                it.forEach() {    
                            if (it.containsKey("deviceObj")) {
                            logger("sceneExtract(): Tap to Run has deviceObj ${it.containsKey("deviceObj")} ", 'debug')                                
                            logger("sceneExtract(): found ${it.deviceObj.feastType} feastType", 'debug')
                            int feastType = it.deviceObj.feastType
                            if (feastType == 0) {
                            devName = it.deviceObj.name 
                            devSku = it.deviceObj.sku
                            logger("sceneExtract(): Looking at ${devName} ${devSku}", 'info')  
                            logger("sceneExtract(): Number of rules are  ${it.rule.size()}", 'debug')
                           if (it.rule.get(0).cmdType == 3 || it.rule.get(0).cmdType == 4 || ((it.rule.get(0).cmdType >= 16 && it.rule.get(0).cmdType <= 19) && (slurper.parseText(it.rule.get(0).iotMsg)).msg.cmd == "ptReal" ) || it.rule.get(0).cmdType == 32) {    
                                logger("sceneExtract(): First rule is scene or DIY ${it.rule.get(0).cmdType}", 'debug')
                                logger("sceneExtract(): First rule is scene or DIY ${(slurper.parseText(it.rule.get(0).iotMsg)).msg.cmd}", 'debug')
                                if ((slurper.parseText(it.rule.get(0).cmdVal)).scenesStr == null && (slurper.parseText(it.rule.get(0).cmdVal)).startBri == null && (slurper.parseText(it.rule.get(0).cmdVal)).snapshotName == null) {
                                    sceneName = (slurper.parseText(it.rule.get(0).cmdVal)).diyName }
                                else if ((slurper.parseText(it.rule.get(0).cmdVal)).scenesStr == null && (slurper.parseText(it.rule.get(0).cmdVal)).diyName == null && (slurper.parseText(it.rule.get(0).cmdVal)).snapshotName == null) {
                                    sceneName = "Sleep starting at "+ (slurper.parseText(it.rule.get(0).cmdVal)).startBri + "% for " + (slurper.parseText(it.rule.get(0).cmdVal)).closeTime + " min"
                                    } 
                                else {
                                sceneName = (slurper.parseText(it.rule.get(0).cmdVal)).scenesStr
                                }
                                command = (slurper.parseText(it.rule.get(0).iotMsg)).msg.data.command
                                logger("sceneExtract(): Second rule data collected is NAME: ${sceneName}, Command: ${command}", 'debug')
                            } else if (it.rule.get(1).cmdType == 3 || it.rule.get(1).cmdType == 4 || ((it.rule.get(1).cmdType >= 16 && it.rule.get(1).cmdType <= 19) && (slurper.parseText(it.rule.get(1).iotMsg)).msg.cmd == "ptReal" ) || it.rule.get(1).cmdType == 32) {
                                logger("sceneExtract(): Second rule is scene or DIY ${it.rule.get(1).cmdType}", 'debug')
                                logger("sceneExtract(): Second rule is scene or DIY ${(slurper.parseText(it.rule.get(1).iotMsg)).msg.cmd}", 'debug')
                                if ((slurper.parseText(it.rule.get(1).cmdVal)).scenesStr == null && (slurper.parseText(it.rule.get(1).cmdVal)).startBri == null && (slurper.parseText(it.rule.get(1).cmdVal)).snapshotName == null) {
                                    sceneName = (slurper.parseText(it.rule.get(1).cmdVal)).diyName }
                                else if ((slurper.parseText(it.rule.get(1).cmdVal)).scenesStr == null && (slurper.parseText(it.rule.get(1).cmdVal)).diyName == null && (slurper.parseText(it.rule.get(1).cmdVal)).snapshotName == null) {
                                    sceneName = "Sleep starting at "+ (slurper.parseText(it.rule.get(1).cmdVal)).startBri + "% for " + (slurper.parseText(it.rule.get(1).cmdVal)).closeTime + " min"
                                    }
                                else if ((slurper.parseText(it.rule.get(1).cmdVal)).scenesStr == null && (slurper.parseText(it.rule.get(1).cmdVal)).startBri == null && (slurper.parseText(it.rule.get(1).cmdVal)).diyName == null) {
                                    sceneName = "Snapshot "+ (slurper.parseText(it.rule.get(1).cmdVal)).snapshotName 
                                    }
                                else {
                                sceneName = (slurper.parseText(it.rule.get(1).cmdVal)).scenesStr
                                }
                                command = (slurper.parseText(it.rule.get(1).iotMsg)).msg.data.command
                                logger("sceneExtract(): Second rule data collected is NAME: ${sceneName}, Command: ${command}", 'debug')   
                            } else if ( it.rule.size() > 2 ) {
                                logger("sceneExtract(): First two rules failed falling back to third rule ${it.rule.get(2).cmdType}", 'debug')
                                if ((slurper.parseText(it.rule.get(2).cmdVal)).scenesStr == null && (slurper.parseText(it.rule.get(2).cmdVal)).startBri == null && (slurper.parseText(it.rule.get(2).cmdVal)).snapshotName == null) {
                                    logger("sceneExtract(): Processing third rule collect DIY name", 'debug')
                                    sceneName = (slurper.parseText(it.rule.get(2).cmdVal)).diyName }
                                else if ((slurper.parseText(it.rule.get(2).cmdVal)).scenesStr == null && (slurper.parseText(it.rule.get(2).cmdVal)).diyName == null && (slurper.parseText(it.rule.get(2).cmdVal)).snapshotName == null) {
                                    logger("sceneExtract(): Processing third rule collect sleep timer name", 'debug')
                                    sceneName = "Sleep starting at "+ (slurper.parseText(it.rule.get(2).cmdVal)).startBri + "% for " + (slurper.parseText(it.rule.get(2).cmdVal)).closeTime + " min"
                                    }
                                else if ((slurper.parseText(it.rule.get(2).cmdVal)).scenesStr == null && (slurper.parseText(it.rule.get(2).cmdVal)).startBri == null && (slurper.parseText(it.rule.get(2).cmdVal)).diyName == null) {
                                    logger("sceneExtract(): Processing third rule collect Snapshot", 'debug')
                                    sceneName = "Snapshot "+ (slurper.parseText(it.rule.get(2).cmdVal)).snapshotName 
                                    }
                                else {
                                logger("sceneExtract(): Processing third rule collect scene name", 'debug')    
                                sceneName = (slurper.parseText(it.rule.get(2).cmdVal)).scenesStr
                                }
                                command = (slurper.parseText(it.rule.get(2).iotMsg)).msg.data.command
                                logger("sceneExtract(): Second rule data collected is NAME: ${sceneName}, Command: ${command}", 'debug')
                            } else {
                                logger("sceneExtract(): No Third rule to process. No valid data to extract", 'debug')
                            }
                            if ( sceneName == null || command == null) {
                                    logger("sceneExtract(): Either Scene Name Or command is Null. Ignoring extracted scene", 'debug')
                                } else {
                                    logger("sceneExtract(): Scene Name is ${sceneName}: command is ${command}", 'debug')
                                    diyAdd(devSku, sceneName, command)
                               addedScenes = addedScenes + "<tr> <td>"+ devSku + "</td> <td>" + sceneName + "</td> <td>" + command.inspect().replaceAll("\'", "\"") + "</td> </tr>"
                                }
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
                }
            addedScenes = addedScenes + "</table>"
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
            section('<b>Extracted scenes are shown below:</b>') {
//                paragraph "Device name ${devName}"
//                paragraph "Scene name is ${sceneName}"
//                paragraph "Command is <mark>${command.inspect().replaceAll("\'", "\"")}</mark>"
//                paragraph "This command will work with any device with model ${devSku}"
                paragraph addedScenes
                paragraph "If you want to backup the scenes pelase download the GoveeLanDIYScenes.json file from your hub."
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

def sceneExtract3() {
    
    goveeScene.clear()
    goveeSceneRetrieve(settings.devsku)
    
    dynamicPage(name: 'sceneExtract3', title: 'Govee API Scene Extract', uninstall: false, install: false, submitOnChange: true, nextPage: "sceneManagement")
    {
            section('<b>Scene extraction completed</b>') {
                paragraph "All Scenes for Devices with model Number ${settings.devsku} have been extracted"
//                paragraph "Scene name is ${sceneName}"
//                paragraph "Command is <mark>${command.inspect().replaceAll("\'", "\"")}</mark>"
//                paragraph "This command will work with any device with model ${devSku}"
//                paragraph addedScenes
                paragraph "The Scenes have been extracted and written to GoveeLanScenes_${settings.devsku}.json."
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
    if (!state.goveeAppAPI && settings.APIKey) {
        retrieveGoveeAPIData()
    }
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 3
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
            if (it != null ) {
                logger("updated() API key has been updated. Calling child devices: ${it} to udpate", 'debug')            
                it.apiKeyUpdate()
            }
        }
        state?.APIKey = settings.APIKey
    }
    if (!state.goveeAppAPI && settings.APIKey) {
        retrieveGoveeAPIData()
    }
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

def goveeSceneRetrieve(String model) {
    if (goveeDevPtURL.contains(model)) {
        logger("goveeSceneRetrieve() Device is not eligiable to be extracted ignoring", 'debug')                                   
    } else {
    logger("goveeSceneRetrieve() Processing Scene retrieval for models ${model}", 'debug')
    def params = [
        uri   : 'https://app2.govee.com',
        path  : '/appsku/v1/light-effect-libraries',
        headers: [ 'appVersion': '9999999'],
        query: ['sku': model],
        ]
    logger("goveeSceneRetrieve(): Calling HTTP server with ${params}", 'debug')
    try {
        httpGet(params) { resp ->
            goveeApiRespons = resp.data.data.categories.scenes
            sceneNames = []
            sceneCodes = []
            sceneParms = []
            String convrtCmd = "" 
            goveeApiRespons.forEach {     
                sceneNames = sceneNames.plus(it.sceneName)                
                sceneCodes = sceneCodes.plus(it.lightEffects.sceneCode)
                sceneParms = sceneParms.plus(it.lightEffects.scenceParam)
            }
            logger("goveeSceneRetrieve(): Size of scene data fields ${sceneNames.size()} size ${sceneCodes.size()} size ${sceneParms.size()}", 'trace')
            recNum = 0
            sceneNames.forEach {
                logger("goveeSceneRetrieve(): records for each variable Name: ${sceneNames.get(recNum)} Scene Code: ${sceneCodes.get(recNum).get(0)} Parm: ${sceneParms.get(recNum).get(0)}", 'debug')                                
				String strSceneParm = sceneParms.get(recNum).get(0)
                def sccode = HexUtils.integerToHexString(sceneCodes.get(recNum).get(0),2)
                def hexString = base64ToHex(strSceneParm)
                def hexSize = hexString.length() // each line is 35 charters except the first one which is 6 less
				if (goveeDevOffsets.containsKey(model)) { // if present subtract offset value from string for calculations
                    hexSize = hexSize - goveeDevOffsets."${model}".offset
            	}
                int splits = 0
                if (isWholeNumber((hexSize - 28) / 34)) {
                    logger("goveeSceneRetrieve(): Split is a whole number ${(hexSize - 28) / 34}", 'trace')
                    splits = (int) Math.floor(((hexSize - 28) / 34) -1)
                } else {
                    logger("goveeSceneRetrieve(): Split is not whole number ${(hexSize - 28) / 34}", 'trace')
                    splits = (int) Math.floor((hexSize - 28) / 34) 
                }                              
                int action = 0
                def position = 28
                if (goveeDevOffsets.containsKey(model)) { // if present set position of next line to start at appropriate location
                    position =  goveeDevOffsets."${model}".line1End
            	}
                convrtCmd = ""
                logger("goveeSceneRetrieve(): SceneParm converted to hex:  ${hexString} Lenght: ${hexSize} Splits ${splits}", 'trace')
                if (strSceneParm != null && strSceneParm != "") {
                	while(splits + 1 >= action) {
                    	logger("goveeSceneRetrieve(): SceneParm converted to on total splits:  ${splits} on action : ${action} ", 'trace')
                    	if (action == 0) {
                        	String section = ""
                            String id = ""
                        	String lineHeader = "a"+ (300 + action)
                            if (deviceTag.containsKey(model)) {
                            	id = ("01" + HexUtils.integerToHexString(splits+2,1) + deviceTag."${model}").toLowerCase()
                                if (hexSize < 28) {
                                    section = hexString.substring(goveeDevOffsets."${model}".start)
                                } else {
                                	section = hexString.substring(goveeDevOffsets."${model}".start,goveeDevOffsets."${model}".line1End)
                                }
                            } else {
                                id = ("01" + HexUtils.integerToHexString(splits+2,1) +"02").toLowerCase()
                                if (hexSize < 28) {
                                    section = hexString.substring(0)
                                } else {
                                	section = hexString.substring(0,28)
                                }
                            }
                        	action = action + 1
                            String minusChkSum = lineHeader+id+section
                            logger("goveeSceneRetrieve(): Minus Checksum :  ${minusChkSum} ", 'trace')
                            checksum = calculateChecksum8Xor(minusChkSum).toLowerCase()
                            hexConvString = lineHeader+id+section+checksum
                        	logger("goveeSceneRetrieve(): Parsing first line :  ${hexConvString} ", 'trace')                        
                        	logger("goveeSceneRetrieve(): Parsing first line :  ${lineHeader}${id}${section}${checksum} ", 'trace')
                            base64String = hexToBase64(hexConvString)
                            logger("goveeSceneRetrieve(): Base64 Command first line :  ${base64String} ", 'trace')
                            convrtCmd = '"'+ base64String  +'"'                        
                    	} else if (action > 0 && action <= (splits )) {
                        	String section = hexString.substring(position , position+34)
                        	String lineHeader = "a3" + (HexUtils.integerToHexString(action,1)) 
                        	action = action +1
                        	position = position + 34
                            String minusChkSum = lineHeader+section
                            checksum = calculateChecksum8Xor(minusChkSum).toLowerCase()
                            hexConvString = lineHeader+section+checksum                       
                        	logger("goveeSceneRetrieve(): Parsing Middle line :  ${lineHeader}${section}${checksum} ", 'trace')
                            base64String = hexToBase64(hexConvString)
                            logger("goveeSceneRetrieve(): Base64 Command Middle line :  ${base64String} ", 'trace')
                            convrtCmd = convrtCmd + ',"' + base64String + '"'
                    	}  else if (action > splits) {
                        	action = action + 1
                        	String section = hexString.substring(position)
                        	def sectionLen = section.length()
                        	def needLen  = 37 - sectionLen
                        	def sectionPad = section.padRight(34,'0')
                        	String lineHeader = "a3ff"
                            String minusChkSum = lineHeader+sectionPad
                            checksum = calculateChecksum8Xor(minusChkSum).toLowerCase()
                            hexConvString = lineHeader+sectionPad+checksum
                        	logger("goveeSceneRetrieve(): Parsing last line padding review : Section data${section}, Section Length ${sectionLen}, padding needed ${needLen}, padded value ${sectionPad} ", 'trace')                        
                        	logger("goveeSceneRetrieve(): Parsing last line :  ${lineHeader}${sectionPad}${checksum} ", 'trace')
                            base64String = hexToBase64(hexConvString)
                            logger("goveeSceneRetrieve(): Base64 Command last command line :  ${base64String} ", 'trace')
                            convrtCmd = convrtCmd + ',"' + base64String + '"'
                        } else {
                        	logger("goveeSceneRetrieve(): Parsing error aborting ", 'trace')
                        }
                    }    
                }
                logger("goveeSceneRetrieve(): scene code :  ${sccode} ", 'trace')
                String lastLine = ""
                if (deviceTagll.containsKey(model)) {
                    lastLine = ("330504"+sccode.substring(2)+sccode.substring(0,2)+"00"+deviceTagll."${model}"+"000000000000000000000000").toLowerCase()
                } else {
                    lastLine = ("330504"+sccode.substring(2)+sccode.substring(0,2)+"0000000000000000000000000000").toLowerCase()
                }
                checksum = calculateChecksum8Xor(lastLine).toLowerCase()
                hexConvString = lastLine+checksum
                logger("goveeSceneRetrieve(): final line to complete command is needed. :  ${lastLine}${checksum} ", 'trace')
                base64String = hexToBase64(hexConvString)
                logger("goveeSceneRetrieve(): Base64 Command fine line :  ${base64String} ", 'trace')
                if (convrtCmd == "") {
                   diyAddManual = '["'+ base64String + '"]' 
                } else {               
                	diyAddManual = "["+convrtCmd + ',"' + base64String + '"]'
                }
                logger("goveeSceneRetrieve(): Base64 command list:  ${diyAddManual} ", 'debug')
                sceneFileCreate(model, sceneNames.get(recNum), diyAddManual)
                recNum = recNum + 1
                }
            if  (resp.data.data.categories.isEmpty()) {
                logger("goveeSceneRetrieve(): Device ${model} does not have scenes. Ignoring", 'debug')
            } else {
		    sceneFileMax(model, sceneNames.size())
            }                
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        logger("goveeSceneRetrieve() Error: e.statusCode ${e.statusCode}", 'error')
        logger("goveeSceneRetrieve() ${e}", 'error')

        return 'unknown'
    }
    logger("goveeSceneRetrieve(): Device Extraction complete for model ${model} ", 'debug')
    }
}

def sceneFileCreate(devSKU, diyName, command) {
    logger("sceneFileCretae(): Attempting add DIY Scene ${devSKU}:${diyName}:${command}", 'trace')
//    command = command.inspect().replaceAll("\'", "\"")
	Map diyEntry = [:]
    diyEntry.put("name", diyName)
    diyEntry.put("cmd", command)
    logger("sceneFileCretae(): Trying to add ${diyEntry}", 'debug')
    logger("sceneFileCretae(): keys are  ${goveeScene.keySet()}", 'trace')
    diySize = goveeScene.size()
    if (diySize == 0){
        int diyAddNum = 101
        Map diyEntry2 = [:]
        diyEntry2.put(diyAddNum,diyEntry)
        goveeScene.put(devSKU,diyEntry2)
    } else {
        diySize = goveeScene."${devSKU}".size()
        int diyAddNum = (diySize + 101).toInteger()
        goveeScene."${devSKU}".put(diyAddNum,diyEntry)
    }
    writeGoveeSceneFile(devSKU)
}

def sceneFileMax(devSKU, int maxNum) {
    Map diyEntry = [:]
    int maxScene = 100 + maxNum
    diyEntry.put("maxScene", maxScene)
    int diyAddNum = 999
    goveeScene."${devSKU}".put(diyAddNum, diyEntry)
    writeGoveeSceneFile(devSKU)
}

def diyAdd(devSKU, diyName, command) {
    def slurper = new JsonSlurper()
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
        logger("diyAdd(): keys are  ${state.diyEffects."${devSKU}".keySet()}", 'debug')
        nameList  = []
        scenelist = state.diyEffects."${devSKU}".keySet()
        scenelist.forEach {
            logger("diyAdd(): Adding Scene ${state.diyEffects."${devSKU}"[it].name} to compare list", 'debug')
            nameList.add(state.diyEffects."${devSKU}"[it].name)    
        }
        logger("diyAdd(): Scene Name Compare list ${nameList}", 'debug')
       
        if (nameList.contains(diyName)) {
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
        logger("diyAdd(): keys are  ${state.diyEffects."${devSKU}".keySet()}", 'debug')
        nameList  = []
        scenelist = state.diyEffects."${devSKU}".keySet()
        scenelist.forEach {
            logger("diyAdd(): Adding Scene ${state.diyEffects."${devSKU}"."${it}".name} to compare list", 'debug')
            nameList.add(state.diyEffects."${devSKU}"."${it}".name)    
        }
        logger("diyAdd(): Scene Name Compare list ${nameList}", 'debug')
       
        if (nameList.contains(diyName)) {
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
            if (state.loggingLevelIDE >= 1) { log.error msg };
            break;
        case 'warn':
            if (state.loggingLevelIDE >= 2)  { log.warn msg };
            break;
        case 'info':
            if (state.loggingLevelIDE >= 3) { log.info msg };
            break;
        case 'debug':
            if (state.loggingLevelIDE >= 4) { log.debug msg };
            break;
        case 'trace':
            if (state.loggingLevelIDE >= 5) { log.trace msg };
            break;
        default:
            log.debug msg;
            break;
    }
}


/**
 *  goveeDevAdd()
 *
 *  Wrapper function to create devices.
 **/
/* def goveeDevAdd() { //testing

// private goveeDevAdd(goveeAdd) {
//    def goveeAdd = settings.goveeDev - child.label    // testing
//    def devices = goveeAdd
    if (settings.goveeDev != null) {
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
                            mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    } else if (deviceModel == "H6093") {
                        String driver = "Govee v2 H6093 Starlight Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    } else {
                        String driver = "Govee v2 White Light Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)              
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }                    
                    }    
                } else if (devType == "devices.types.air_purifier") {
                    if (deviceModel == "H7120") {
                        String driver = "Govee v2 H7120 Air Purifier"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    } else if (deviceModel == "H7122") {
                        String driver = "Govee v2 H7122 Air Purifier"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    } else if (deviceModel == "H7123") {
                        String driver = "Govee v2 H7123 Air Purifier"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    } else if (deviceModel == "H7126") {
                        String driver = "Govee v2 H7126 Air Purifier"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    }else if (deviceModel == "H712C") {
                        String driver = "Govee v2 H712C Air Purifier"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }
                    }  else {                    
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
                    if (deviceModel == "H7131" || deviceModel == "H7134") {
                        String driver = "Govee v2 H7131 Space Heater"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    } else if (deviceModel == "H7133") {
                        String driver = "Govee v2 H7133 Space Heater Pro"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    }  else {                                                
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
                        String driver = "Govee v2 Humidifier Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                } else if (devType == "devices.types.fan") {
                    if (deviceModel == "H7102") {
                        String driver = "Govee v2 H7102 Tower Fan"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    }  else if (deviceModel == "H7106") {
                        String driver = "Govee v2 H7106 Tower Fan"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            setBackgroundStatusMessage("Installing device ${deviceName}")
                            mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                            setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                        }    
                    }  else { 
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
                } else if (devType == "devices.types.air_quality_monitor") {
                    String driver = "Govee v2 Air Quality Sensor with CO2"
                    if (drivers.contains(driver)) {
                        logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                        setBackgroundStatusMessage("Installing device ${deviceName}")
                        mqttDevice.addMQTTDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                    } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')
                        setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver  <mark>${driver} is not installed</mark>. Please correct and try again")
                    }
                }else if (devType == "devices.types.sensor") {
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
                } else if (!devType) {
                    String driver = "Govee v2 Group Light Driver"
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
                        mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        setBackgroundStatusMessage("Device ${deviceName} was selected for install and is of Unknown device type. Installing with research driver.")
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
    } else { 
        setBackgroundStatusMessage("No devices selected. No action")
    }
    state?.installDev = goveeDev
    atomicState.backgroundActionInProgress = false
    logger('goveeDevAdd() Govee devices integrated', 'info')
} */


def goveeDevAdd() { // AI Enhanced code for Govee Device add process

    if (settings.goveeDev == null) {
        setBackgroundStatusMessage("No devices selected. No action")
        // Assuming 'settings.goveeDev' is the intended reference for 'goveeDev'
        state?.installDev = settings.goveeDev
        atomicState.backgroundActionInProgress = false
        logger('goveeDevAdd() Govee devices integration complete.', 'info')
        return
    }

    def devices = settings.goveeDev - child.label
    def drivers = getDriverList()
    def mqttDevice = getChildDevice('Govee_v2_Device_Manager')

    logger("goveeDevAdd() drivers detected are ${drivers}", 'debug')
    logger("goveeDevAdd() Children DNI  ${childDNI} MQTT device DNI ${mqttChildredDNI}", 'debug') // Fix mqttChildredDNI if it's undefined
    logger("goveeDevAdd() $devices are selected to be integrated", 'info')
    logger('goveeDevAdd() DEVICE INFORMATION', 'info')

    def childDNIset = childDNI as Set // For faster lookups

    // Define driver mapping rules
    // Each rule is a map. The 'condition' closure evaluates to true if the rule applies.
    // The 'driver' field holds the driver name.
    // The 'helper' field indicates which helper method to call.
    // Rules are ordered by specificity, most specific first.
    def driverRules = [
        // Light Devices (more specific capability combinations first)
        [
            condition: { dev -> dev.type == "devices.types.light" && dev.commands.containsAll(["colorRgb", "colorTemperatureK", "segmentedBrightness", "segmentedColorRgb", "dreamViewToggle"]) },
            driver: "Govee v2 Color Lights Dreamview Sync",
            helper: "addLightDeviceHelper"
        ],
        [
            condition: { dev -> dev.type == "devices.types.light" && dev.commands.containsAll(["colorRgb", "colorTemperatureK", "segmentedBrightness", "segmentedColorRgb"]) },
            driver: "Govee v2 Color Lights 3 Driver",
            helper: "addLightDeviceHelper"
        ],
        [
            condition: { dev -> dev.type == "devices.types.light" && dev.commands.containsAll(["colorRgb", "colorTemperatureK", "segmentedBrightness"]) },
            driver: "Govee v2 Color Lights 2 Driver",
            helper: "addLightDeviceHelper"
        ],
        [
            condition: { dev -> dev.type == "devices.types.light" && dev.commands.containsAll(["colorRgb", "colorTemperatureK", "segmentedColorRgb", "dreamViewToggle"]) },
            driver: "Govee v2 Color Lights 4 Dreamview Sync",
            helper: "addLightDeviceHelper"
        ],
        [
            condition: { dev -> dev.type == "devices.types.light" && dev.commands.containsAll(["colorRgb", "colorTemperatureK", "segmentedColorRgb"]) },
            driver: "Govee v2 Color Lights 4 Driver",
            helper: "addLightDeviceHelper"
        ],
        [
            condition: { dev -> dev.type == "devices.types.light" && dev.commands.containsAll(["colorRgb", "colorTemperatureK"]) },
            driver: "Govee v2 Color Lights Driver",
            helper: "addLightDeviceHelper"
        ],
        [
            condition: { dev -> dev.type == "devices.types.light" && dev.commands.contains("colorTemperatureK") },
            driver: "Govee v2 White Lights with CT Driver",
            helper: "addLightDeviceHelper"
        ],
        // Specific Light Models (order matters if capabilities overlap with generic light rules)
        [
            condition: { dev -> dev.type == "devices.types.light" && (dev.sku == "H6091" || dev.sku == "H6092" || dev.sku == "H609D") },
            driver: "Govee v2 Galaxy Projector",
            helper: "addLightDeviceHelper"
        ],
        [
            condition: { dev -> dev.type == "devices.types.light" && (dev.sku == "H6093" || dev.sku == "H6094" || dev.sku == "H6095") },
            driver: "Govee v2 H6093 Starlight Driver",
            helper: "addLightDeviceHelper"
        ],
        // Default Light Driver
        [
            condition: { dev -> dev.type == "devices.types.light" },
            driver: "Govee v2 White Light Driver",
            helper: "addLightDeviceHelper"
        ],

        // Air Quality Monitor
        [
            condition: { dev -> dev.type == "devices.types.air_quality_monitor" && dev.sku == "H5140" },
            driver: "Govee v2 Air Quality Sensor with CO2",
            helper: "addMQTTDeviceHelper"
        ],        
        
        // Air Purifier Devices
        [
            condition: { dev -> dev.type == "devices.types.air_purifier" && dev.sku == "H7120" },
            driver: "Govee v2 H7120 Air Purifier",
            helper: "addMQTTDeviceHelper"
        ],
        [
            condition: { dev -> dev.type == "devices.types.air_purifier" && dev.sku == "H7122" },
            driver: "Govee v2 H7122 Air Purifier",
            helper: "addMQTTDeviceHelper"
        ],
        [
            condition: { dev -> dev.type == "devices.types.air_purifier" && dev.sku == "H7123" },
            driver: "Govee v2 H7123 Air Purifier",
            helper: "addMQTTDeviceHelper"
        ],
        [
            condition: { dev -> dev.type == "devices.types.air_purifier" && dev.sku == "H7126" },
            driver: "Govee v2 H7126 Air Purifier",
            helper: "addMQTTDeviceHelper"
        ],
        [
            condition: { dev -> dev.type == "devices.types.air_purifier" && dev.sku == "H712C" },
            driver: "Govee v2 H712C Air Purifier",
            helper: "addMQTTDeviceHelper"
        ],
        [
            condition: { dev -> dev.type == "devices.types.air_purifier" },
            driver: "Govee v2 Air Purifier Driver",
            helper: "addMQTTDeviceHelper"
        ],

        // Heater Devices
        [
            condition: { dev -> dev.type == "devices.types.heater" && (dev.sku == "H7131" || dev.sku == "H7134") },
            driver: "Govee v2 H7131 Space Heater",
            helper: "addMQTTDeviceHelper"
        ],
        [
            condition: { dev -> dev.type == "devices.types.heater" && dev.sku == "H7133" },
            driver: "Govee v2 H7133 Space Heater Pro",
            helper: "addMQTTDeviceHelper"
        ],
        [
            condition: { dev -> dev.type == "devices.types.heater" },
            driver: "Govee v2 Heating Appliance Driver",
            helper: "addMQTTDeviceHelper"
        ],

        // Fan Devices
        [
            condition: { dev -> dev.type == "devices.types.fan" && dev.sku == "H7102" },
            driver: "Govee v2 H7102 Tower Fan",
            helper: "addMQTTDeviceHelper"
        ],
        [
            condition: { dev -> dev.type == "devices.types.fan" && dev.sku == "H7106" },
            driver: "Govee v2 H7106 Tower Fan",
            helper: "addMQTTDeviceHelper"
        ],
        [
            condition: { dev -> dev.type == "devices.types.fan" },
            driver: "Govee v2 Fan Driver",
            helper: "addMQTTDeviceHelper"
        ],

        // Single-driver Types
        [
            condition: { dev -> dev.type == "devices.types.socket" },
            driver: "Govee v2 Sockets Driver",
            helper: "addMQTTDeviceHelper"
        ],
        [
            condition: { dev -> dev.type == "devices.types.ice_maker" },
            driver: "Govee v2 Ice Maker",
            helper: "addMQTTDeviceHelper"
        ],
        [
            condition: { dev -> dev.type == "devices.types.kettle" },
            driver: "Govee v2 Kettle Driver",
            helper: "addMQTTDeviceHelper"
        ],
        [
            condition: { dev -> dev.type == "devices.types.thermometer" },
            driver: "Govee v2 Thermo/Hygrometer Driver",
            helper: "addMQTTDeviceHelper"
        ],
        [
            condition: { dev -> dev.type == "devices.types.sensor" },
            driver: "Govee v2 Presence Sensor",
            helper: "addMQTTDeviceHelper"
        ],
        [
            condition: { dev -> dev.type == "devices.types.aroma_diffuser" },
            driver: "Govee v2 Aroma Diffuser Driver with Lights",
            helper: "addMQTTDeviceHelper"
        ],

        // Fallback for no specific devType (e.g., groups)
        [
            condition: { dev -> !dev.type }, // `dev.type` is null or empty
            driver: "Govee v2 Group Light Driver",
            helper: "addMQTTDeviceHelper"
        ],

        // Research Driver (Catch-all for unknown types) - This should be the very last rule
        [
            condition: { dev -> true }, // Matches any remaining device
            driver: "Govee v2 Research Driver",
            helper: "addLightDeviceHelper" // Original code uses addLightDeviceHelper for this
        ]
    ]

    state.goveeAppAPI.each { apiDevice -> // Renamed 'it' to 'apiDevice' for clarity
        def dniCompare = "Govee_${apiDevice.device}"
        def deviceName = apiDevice.deviceName

        if (!childDNIset.contains(dniCompare)) {
            logger("goveeDevAdd(): ${deviceName} is a new DNI. Passing to driver setup if selected.", 'debug')

            if (devices.contains(deviceName)) {
                def deviceID = apiDevice.device
                def deviceModel = apiDevice.sku
                def devType = apiDevice.type
                def commands = []
                def capType = []
                def int ctMin = 0
                def int ctMax = 0

                apiDevice.capabilities.each { cap ->
                    logger ("goveeDevAdd(): ${cap} instance is ${cap.instance}",'trace')
                    commands.add(cap.instance)
                    capType.add(cap.type)
                    if (cap.instance == "colorTemperatureK") {
                        logger ("goveeDevAdd(): ${cap} instance is ${cap.instance} Parms is ${cap.parameters} range is ${cap.parameters.range} min is ${cap.parameters.range.min}",'trace')
                        ctMin = cap.parameters.range.min
                        ctMax = cap.parameters.range.max
                        logger ("goveeDevAdd(): Min is ${ctMin} Max is ${ctMax}",'trace')
                    }
                }

                logger ("goveeDevAdd(): ${deviceID} ${deviceModel} ${deviceName} ${devType} ${commands}",'trace')

                def foundRule = driverRules.find { rule ->
                    // Pass a context object to the condition closure
                    rule.condition.call([
                        type: devType,
                        sku: deviceModel, // 'sku' is deviceModel
                        commands: commands // List of extracted commands
                    ])
                }

                if (foundRule) {
                    def selectedDriver = foundRule.driver
                    def helperMethod = foundRule.helper

                    if (drivers.contains(selectedDriver)) {
                        logger("goveeDevAdd() configuring ${deviceName} with driver: ${selectedDriver}", 'info')
                        setBackgroundStatusMessage("Installing device ${deviceName}")

                        // Dynamically call the appropriate helper method
                        if (helperMethod == "addLightDeviceHelper") {
                            // Ensure addLightDeviceHelper can handle ctMin, ctMax (even if 0) and capType
                            mqttDevice.addLightDeviceHelper(selectedDriver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else { // Assuming it's addMQTTDeviceHelper for others
                            mqttDevice.addMQTTDeviceHelper(selectedDriver, deviceID, deviceName, deviceModel, commands, capType)
                        }
                    } else {
                        logger("goveeDevAdd(): You selected device '${deviceName}' which needs driver '${selectedDriver}'. Please load it.", 'warn')
                        setBackgroundStatusMessage("Device ${deviceName} was selected for install but driver <mark>${selectedDriver}</mark> is not installed. Please correct and try again.")
                    }
                } else {
                    logger("goveeDevAdd(): No suitable rule found for device ${deviceName} (Type: ${devType}, Model: ${deviceModel}). This should not happen if the last 'Research Driver' rule is present.", 'error')
                    setBackgroundStatusMessage("Device ${deviceName} was selected for install but no suitable driver could be determined. Please contact support.")
                }

            } else {
                logger("goveeDevAdd(): Device '${deviceName}' not selected for installation. Skipping.", 'debug')
            }
        } else {
            logger("goveeDevAdd(): Device ID '${deviceName}' (DNI: ${dniCompare}) already installed. Skipping.", 'debug')
            setBackgroundStatusMessage("Device ${deviceName} is already installed. Ignored.")
        }
    }

    state?.installDev = settings.goveeDev
    atomicState.backgroundActionInProgress = false
    logger('goveeDevAdd() Govee devices integration complete.', 'info')
}

/**
 *  goveeLightManAdd()
 *
 *  Wrapper function for all logging.
 **/
private goveeLightManAdd(String model, String ip, String name) {
    def newDNI = "Govee_" + ip
    mqttDevice = getChildDevice('Govee_v2_Device_Manager')
    logger("goveeLightManAdd() Adding ${name} Model: ${model} at ${ip} with ${newDNI}", 'info')
    logger('goveeLightManAdd() DEVICE INFORMATION', 'info')
    if (childDNI.contains(newDNI) == false) {
        String driver = "Govee Manual LAN API Device"
        logger("goveeLightManAdd(): ${deviceName} is a new DNI. Passing to driver setup if selected.", 'debug') 
        logger("goveeLightManAdd():  configuring ${deviceName}", 'info')
        mqttDevice.addManLightDeviceHelper(driver, ip, name, model)
      //  mqttDevice.addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
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
        writeDIYFile()
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
    } else if (button == "resGovScenes") {
        mqttDevice = getChildDevice('Govee_v2_Device_Manager')
        models = (mqttDevice.getChildDevices().data.deviceModel).unique()
        logger("appButtonHandler(): child device models are ${models}", 'info')
        models.forEach {
            logger("appButtonHandler(): Processing  ${it}", 'info')
            goveeScene.clear()
            goveeSceneRetrieve(it)
        }
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

void writeGoveeSceneFile(model) { // create and store lan scene files from Govee API
    log.debug ("writeDIYFile: Writing DIY Scenes to flat file for Drivers")
    String listJson = "["+JsonOutput.toJson(goveeScene)+"]" as String
    uploadHubFile("GoveeLanScenes_$model"+".json",listJson.getBytes())
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

def base64ToHex(base64Str) { // Proper conversion from Base64 to Hex for scene creation.
    // Decode Base64 string to byte array
    byte[] decodedBytes = base64Str.decodeBase64()

    // Convert byte array to hex string
    def hexString = decodedBytes.collect { String.format("%02x", it) }.join('')
    
    return hexString
}


def calculateChecksum8Xor(String hexString) {
    int checksum = 0
    for (int i = 0; i < hexString.length(); i += 2) {
        String byteStr = hexString.substring(i, Math.min(i + 2, hexString.length()))
        int byteValue = Integer.parseInt(byteStr, 16)
        checksum ^= byteValue
    }
    return String.format("%02X", checksum) // Format as two-digit hex
}

def hexToBase64(String hexString) {
    if (!hexString) {
        return null
    }

    try {
        byte[] bytes = new byte[hexString.length() / 2]
        for (int i = 0; i < hexString.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(hexString.substring(i, i + 2), 16)
        }

        return base64Encode(bytes)

    } catch (NumberFormatException e) {
        log.error "Invalid hex string: ${e.message}"
        return null
    } catch (IllegalArgumentException e) {
        log.error "Invalid hex string length: ${e.message}"
        return null
    }
}



private String base64Encode(byte[] data) {
    StringBuilder sb = new StringBuilder();
    int b;
    int dataLen = data.length;
    int i = 0;
    while (i < dataLen) {
        b = data[i++] & 0xff;
        sb.append(BASE64_CHARS.charAt(b >> 2));
        if (i == dataLen) {
            sb.append(BASE64_CHARS.charAt((b & 0x3) << 4));
            sb.append("==");
            break;
        }
        b = (b & 0x3) << 4 | (data[i] & 0xff) >> 4;
        sb.append(BASE64_CHARS.charAt(b));
        if (++i == dataLen) {
            sb.append(BASE64_CHARS.charAt((data[i - 1] & 0xf) << 2));
            sb.append("=");
            break;
        }
        b = (data[i - 1] & 0xf) << 2 | (data[i] & 0xff) >> 6;
        sb.append(BASE64_CHARS.charAt(b));
        sb.append(BASE64_CHARS.charAt(data[i++] & 0x3f));
    }
    return sb.toString();
}

def isWholeNumber(number) {
    if (number == null) {
        return false // Handle null case
    }
    return number == number.intValue() // Compare to int value
}
