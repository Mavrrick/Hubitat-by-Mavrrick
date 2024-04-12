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
* 2.0.24 Add change tracking to individual integration item
* 2.0.25 Updated device add routine to check for driver on hub and alert if driver is not present.
* 2.0.26 Added use of @field static variables to optimize various code elements.
*        Modified Device add routine to optimize size of code and remove heavy processes.
*        Broke out device add to own method to reduce redundant code
*        Updated Manual device add routine to use new device add method.
*        Cleaned up various code locations to remove variable setting that would conflict with new @field static variables
*/

import groovy.json.JsonSlurper
import groovy.transform.Field

#include Mavrrick.Govee_Lan_Scenes

@Field static Integer childCount = 0
@Field static List child = []
@Field static def options = [:]
@Field static String deviceID = ""
@Field static String dniCompare = "" 
@Field static String deviceModel = ""
@Field static String deviceName = ""
@Field static String devType = ""
@Field static String driver = ""
@Field static String ctMin = ""
@Field static String ctMax = ""
@Field static List commands = []
@Field static List capType = []
@Field static List childDNI = []
@Field static List drivers = []



preferences
{
    page(name: 'mainPage', title: 'Govee Integration')
    page(name: 'deviceSelect', title: 'Select Light, Switch, Plug devices')
    page(name: 'deviceSelect2', title: 'Select Appliances')
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
    app.clearSetting("goveeDevName")
    app.clearSetting("goveeModel")
    app.clearSetting("goveeManLanIP")
    child = getChildDevices()
    childDNI = child.deviceNetworkId
    childCount = child.size()
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
        if (settings.APIKey != null) {
        section('<b>Govee Cloud Device Management</b>') {
            paragraph "Click button below to retrieve Govee Cloud Device data."
            input "deviceListRefresh2" , "button",  title: "Refresh Device List, DIY, and Snapshots"
            paragraph "Click button to trigger all installed devices to check for updates."
            input "pushScenesUpdate" , "button",  title: "Retrieve device updates from cloud"
            paragraph ""
            href 'deviceSelect', title: 'Device selection', description: 'Select Govee devices to add to your environment'
        }
        }
        section('<b>Manual Setup of LAN Enabled device</b>') {
            href 'deviceLanManual', title: 'Manual Setup Lan Device', description: 'Manual Setup for LAN API Enabled Device'
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
        section('<b>Govee Scene Management</b>') {
            href 'sceneManagement', title: 'Scene Management', description: 'Click to setup extraction credential, extract scenes, and manage device association.'
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
    if (state.goveeAppAPI != null) {
    logger('deviceSelect() DEVICE INFORMATION', 'debug')
                state.goveeAppAPI.each {
                    deviceName = it.deviceName
                    logger("deviceSelect() $deviceName found", 'debug')
                    options["${deviceName}"] = deviceName
                } 
                logger(" deviceSelect() $options", 'debug')
    }
    dynamicPage(name: 'deviceSelect', title: 'Add Devices page', uninstall: false, install: false, nextPage: "mainPage")
    {
        
        section('Device list refresh') {
            paragraph "Click button below to refresh device list from Govee API"
            input "deviceListRefresh" , "button",  title: "Refresh Device List "
        }
        
        section('<b>Device Add</b>')
        {
            paragraph 'Please select the devices you wish to integrate.'
            input(name: 'goveeDev', type: 'enum', required:false, description: 'Please select the devices you wish to integrate.', multiple:true,
                options: options, width: 8, height: 1)
        }
    }
}

def deviceLanManual() {
    dynamicPage(name: 'deviceLanManual', title: 'Manual Add for LAN API Enabled Devices', uninstall: false, install: false, nextPage: "deviceLanManual2" )
    {
        section('Please enter the parms for the LAN API enabled device you want to integrate.')
        {
            paragraph 'Please enter the below needed values to create your device '
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
            paragraph "Click button below to refresh scenes to all children device"
            input "pushScenesUpdate" , "button",  title: "Refresh Device Scene Awareness"
            paragraph "Click button below to reload preload scenes"
            input "sceneInitialize" , "button",  title: "Reload Preloaded Scene Data"
            paragraph "Click button below to clear DIY scenes"
            input "sceneDIYInitialize" , "button",  title: "Clear/Initialize DIY Scene Information"
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
//    def sceneData = [:]

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
//                            logger("sceneExtract(): Scene Name is ${sceneName}: command is ${command.inspect().replaceAll("\'", "\"")}", 'debug')
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
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    state.isInstalled = true
    state.diyEffects = [:]
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 3
    if (settings.APIKey != state.APIKey ) {
        child.each {
            logger('initialize() API key has been updated. Calling child devices to udpate', 'debug')
            it.apiKeyUpdate()
        }
        state?.APIKey = settings.APIKey
    }
    if (goveeDev) {
        def goveeAdd = settings.goveeDev - child.label
        logger("initialize() Found child devices ${child}", 'debug')
        logger("initialize() Govee Light/Switch/Plugs to add ${goveeAdd}.", 'info')
        if (goveeAdd) {
            goveeDevAdd(goveeAdd)
                }
        }
          else {
        logger('initialize() No devices to add', 'info')
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
        diyAddNum = 1001
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
}

/**
 *  diyAddManual()
 *
 *  Method to manually add shared Scenes to Hubitat.
 **/

def diyAddManual(devSKU, diyName, command) {
    logger("diyAdd(): Attempting add DIY Scene ${devSKU}:${diyName}:${command}", 'trace')
    diyEntry = [:]
    diyEntry.put("name", diyName)
    diyEntry.put("cmd", command)
    logger("diyAdd(): Trying to add ${diyEntry}", 'debug')
    logger("diyAdd(): keys are  ${state.diyEffects.keySet()}", 'debug')
    if (state.diyEffects.containsKey(devSKU) == false) {
        logger("diyAdd(): Device ${devSKU} not found", 'debug')
        logger("diyAdd(): New Device. Starting at 1001", 'debug')
        diyAddNum = 1001
        diyEntry2 = [:]
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
}

/**
 *  diyUpdateManual()
 *
 *  Method to manually add shared Scenes to Hubitat.
 **/

def diyUpdateManual(devSKU, diyAddNum, diyName, command) {
    logger("diyUpdateManual(): Attempting add DIY Scene ${devSKU}:${diyAddNum}:${diyName}:${command}", 'trace')
    diyEntry = [:]
    diyEntry.put("name", diyName)
    diyEntry.put("cmd", command)
    logger("diyUpdateManual(): Trying to add ${diyEntry}", 'debug')
    logger("diyUpdateManual(): keys are  ${state.diyEffects.keySet()}", 'debug')
    if (state.diyEffects.containsKey(devSKU) == false) {
        logger("diyUpdateManual(): Device ${devSKU} not found.", 'debug')
        diyEntry2 = [:]
        diyEntry2.put(diyAddNum,diyEntry)
        state.diyEffects.put(devSKU,diyEntry2)
    } else {
            logger("diyUpdateManual(): Device ${devSKU} was found. Updating scene", 'debug')
            state.diyEffects."${devSKU}".put(diyAddNum,diyEntry)
    }
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
private goveeDevAdd(goveeAdd) {
    def devices = goveeAdd
    drivers = getDriverList()
    logger("goveeDevAdd() drivers detected are ${drivers}", 'debug')
    logger("goveeDevAdd() $devices are selcted to be integrated", 'info')
    logger('goveeDevAdd() DEVICE INFORMATION', 'info')
    state.goveeAppAPI.each {
        dniCompare = "Govee_"+it.device
        deviceName = it.deviceName        
        if (childDNI.contains(dniCompare) == false) {
            logger("goveeDevAdd(): ${deviceName} is a new DNI. Passing to driver setup if selected.", 'debug')
            if (devices.contains(deviceName) == true) {
                deviceID = it.device
                deviceModel = it.sku
                devType = it.type
                commands = []
                capType = []
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
                if (devType == "devices.types.light") {
                    if (commands.contains("colorRgb") && commands.contains("colorTemperatureK") && commands.contains("segmentedBrightness") && commands.contains("segmentedColorRgb") && commands.contains("dreamViewToggle")) {
                        String driver = "Govee v2 Color Lights Dreamview Sync"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                        }
                    } else if (commands.contains("colorRgb") && commands.contains("colorTemperatureK") && commands.contains("segmentedBrightness") && commands.contains("segmentedColorRgb")) {
                        String driver = "Govee v2 Color Lights 3 Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                        }    
                    } else if (commands.contains("colorRgb") && commands.contains("colorTemperatureK")  && commands.contains("segmentedBrightness")) {
                        String driver = "Govee v2 Color Lights 2 Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                        }
                    } else if (commands.contains("colorRgb") && commands.contains("colorTemperatureK")  && commands.contains("segmentedColorRgb") && commands.contains("dreamViewToggle")) {
                        String driver = "Govee v2 Color Lights 4 Dreamview Sync"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                        }    
                    } else if (commands.contains("colorRgb") && commands.contains("colorTemperatureK")  && commands.contains("segmentedColorRgb")) {
                        String driver = "Govee v2 Color Lights 4 Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                        }    
                    } else if (commands.contains("colorRgb") == true && commands.contains("colorTemperatureK")) {
                        String driver = "Govee v2 Color Lights Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                        } 
                    } else if (commands.contains("colorTemperatureK")) {
                        String driver = "Govee v2 White Lights with CT Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                        }     
                    } else if (deviceModel == "H6091" || deviceModel == "H6092") {
                        String driver = "Govee v2 Galaxy Projector"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            addDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                        }    
                    } else {
                        String driver = "Govee v2 White Light Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            addDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)              
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                        }                    
                    }    
                } else if (devType == "devices.types.air_purifier") {
                    if (commands.contains("colorRgb") == true) {
                        String driver = "Govee v2 Air Purifier with RGB Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            addDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                        }
                    } else {
                        String driver = "Govee v2 Air Purifier Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            addDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                        }
                    }    
                } else if (devType == "devices.types.heater") {
                    if (commands.contains("colorRgb")) {
                        String driver = "Govee v2 Heating Appliance with RGB Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            addDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                        }    
                    } else {
                        String driver = "Govee v2 Heating Appliance Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            addDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                        }
                    }
                } else if (devType == "devices.types.humidifier") {
                    if (commands.contains("colorRgb")) {
                        String driver = "Govee v2 Humidifier with RGB Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            addDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                        }
                    } else {
                        String driver = "Govee v2 Humidifier Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            addDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                        }    
                    }
                } else if (devType == "devices.types.fan") {
                    if (commands.contains("colorRgb")) {
                        String driver = "Govee v2 Fan with RGB Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            addDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                        }
                    } else {
                        String driver = "Govee v2 Fan Driver"
                        if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            addDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                        } else {
                            logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                        }     
                    }
                } else if (devType == "devices.types.socket") {
                    String driver = "Govee v2 Sockets Driver"
                    if (drivers.contains(driver)) {
                            logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                            addDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                    } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                    }         
                } else if (devType == "devices.types.ice_maker") {
                    String driver = "Govee v2 Ice Maker"
                    if (drivers.contains(driver)) {
                        logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                        addDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                    } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                    }     
                } else if (devType == "devices.types.kettle") {
                    String driver = "Govee v2 Kettle Driver"
                    if (drivers.contains(driver)) {
                        logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                        addDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                    } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                    } 
                } else if (devType == "devices.types.thermometer") {
                    String driver = "Govee v2 Thermo/Hygrometer Driver"
                    if (drivers.contains(driver)) {
                        logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                        addDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                    } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                    }
                } else if (devType == "devices.types.sensor") {
                    String driver = "Govee v2 Presence Sensor"
                    if (drivers.contains(driver)) {
                        logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                        addDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                    } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                    }    
                } else if (devType == "devices.types.aroma_diffuser") {
                    String driver = "Govee v2 Aroma Diffuser Driver with Lights"
                    if (drivers.contains(driver)) {
                        logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                        addDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                    } else {
                        logger('goveeDevAdd(): You selected a device that needs driver "'+driver+'". Please load it', 'info')    
                    }     
                } else {
                    String driver = "Govee v2 Research Driver"
                    if (drivers.contains(driver)) {
                        logger("goveeDevAdd()  configuring ${deviceName}", 'info')
                        addDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType)
                    } else {
                    logger('goveeDevAdd(): The device does not have a driver and you do not have the '+driver+' loaded. Please load it and forward the device details to the developer', 'info')    
                    }
                }
            } else {
                logger("goveeDevAdd(): Device is not selected to be added. ${deviceName} not being installed", 'debug')
            }
        } else {
            logger("goveeDevAdd(): Device ID matches child DNI. ${deviceName} already installed", 'debug')    
        }                
    }
    state?.installDev = goveeDev
    logger('goveeDevAdd() Govee devices integrated', 'info')
}


/**
 *  goveeLightManAdd()
 *
 *  Wrapper function for all logging.
 **/
private goveeLightManAdd(model, ip, name) {
    def newDNI = "Govee_" + ip
    logger("goveeLightManAdd() Adding ${name} Model: ${model} at ${ip} with ${newDNI}", 'info')
    logger('goveeLightManAdd() DEVICE INFORMATION', 'info')
        String deviceIP = ip                            
        String deviceModel = model
        String deviceName = name
        String driver = "Govee Manual LAN API Device"
        if (childDNI.contains(newDNI) == false) {
            logger("goveeLightManAdd(): ${deviceName} is a new DNI. Passing to driver setup if selected.", 'debug') 
            String ctMin = 2000
            String ctMax = 9000
            logger("goveeLightManAdd():  configuring ${deviceName}", 'info')
            addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, "Lan Only")
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
    if (button == "sceneInitialize") {
        log.debug "appButtonHandler(): Initializing Scene data"
//        state?.lightEffects = [:]
        state.remove("lightEffect_Lyra_Lamp")
        state.remove("lightEffect_Table_Lamp")
        state.remove("lightEffect_Y_Light")
        state.remove("lightEffect_Hexa_Light")
        state.remove("lightEffect_Basic_Lamp")
        state.remove("lightEffect_Outdoor_String_Light")
        state.remove("lightEffect_Outdoor_Pod_Light")
        state.remove("lightEffect_Outdoor_Perm_Light")
        state.remove("lightEffect_Wall_Light_Bar")
        state.remove("lightEffect_Indoor_Pod_Lights")
        state.remove("lightEffect_XMAS_Light")  
        state.remove("lightEffect_RGBIC_Strip")
        state.remove("lightEffect_Curtain_Light")
        state.remove("lightEffect_Tri_Light")
        state.remove("lightEffect_Cylinder_Lamp")
        state.remove("lightEffect_TV_Light_Bar")
        state.remove("lightEffect_Outdoor_Flood_Light")
        state.remove("lightEffect_Galaxy_Projector")
        lightEffectSetup()
    } else if (button == "pushScenesUpdate") {
        child = getChildDevices()
        child.each {
        logger('appButtonHandler(): All Devices need to update scene data. Calling child devices to refresh scenes', 'debug')
        it.configure()
        }
    } else if (button == "sceneDIYInitialize") {
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
        def options = [:]
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
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        logger("appButtonHandler() Error: e.statusCode ${e.statusCode}", 'error')
        logger("appButtonHandler() ${e}", 'error')

        return 'unknown'
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

/**
 *  addDeviceHelper()
 *
 *  Handler for when adding Light devices in App. .
 **/

private def addLightDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, ctMin, ctMax, capType) {
                            addChildDevice('Mavrrick', driver, "Govee_${deviceID}" , location.hubs[0].id, [
                            'name': driver,
                            'label': deviceName,
                            'data': [
                                'deviceID': deviceID,
                                'deviceModel': deviceModel,
                                'apiKey': settings.APIKey,
                                'commands': commands,
                                'ctMin': ctMin,
                                'ctMax': ctMax,
                                'capTypes': capType
                                ],
                            'completedSetup': true,
                        ])
}

private def addDeviceHelper(driver, deviceID, deviceName, deviceModel, commands, capType) {
                        addChildDevice('Mavrrick', driver, "Govee_${deviceID}" , location.hubs[0].id, [
                            'name': driver,
                            'label': deviceName,
                            'data': [
                                'deviceID': deviceID,
                                'deviceModel': deviceModel,
                                'apiKey': settings.APIKey,
                                'commands': commands,
                                'capTypes': capType
                                ],
                            'completedSetup': true,
                        ])
}