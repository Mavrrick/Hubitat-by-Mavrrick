/* groovylint-disable LineLength, DuplicateNumberLiteral, DuplicateStringLiteral, ImplementationAsType, ImplicitClosureParameter, InvertedCondition, LineLength, MethodReturnTypeRequired, MethodSize, NestedBlockDepth, NglParseError, NoDef, NoJavaUtilDate, NoWildcardImports, ParameterReassignment, UnnecessaryGString, UnnecessaryObjectReferences, UnnecessaryToString, UnusedImport, VariableTypeRequired */
/**
 *  Unifi Integration Manager
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
    name: 'Unifi Integration Manager',
    namespace: 'Mavrrick',
    author: 'CRAIG KING',
    description: "Unifi Integration Manager, helps manage devices and drivers based on @Snell's Unifi API projects.",
    category: 'Networking',
    importUrl: "https://raw.githubusercontent.com/Mavrrick/Hubitat-by-Mavrrick/refs/heads/main/UniFi/Mavrrick.UnifiIntegrationManager.groovy",
    iconUrl: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg',
    iconX2Url: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg',
//    iconX3Url: 'https://lh4.googleusercontent.com/-1dmLp--W0OE/AAAAAAAAAAI/AAAAAAAAEYU/BRuIXPPiOmI/s0-c-k-no-ns/photo.jpg',
    singleThreaded: true,
    singleInstance: true)

/*
* Initial release v1.0.0 
*/

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field
import hubitat.helper.HexUtils

@Field static List child = []
@Field static List childDNI = []
@Field static String statusMessage = ""
@Field static String BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"


preferences
{
    page(name: 'mainPage', title: 'Unifi Integration Manager')
    page(name: 'pageEnableAPI')
    page(name: "pageDisableAPI")
 //   page(name: 'about', title: 'About')
    mappings {
        path("/") {
            action: [
            GET: "webHook"
            ]
        }
    }
}

/*
    mainPage

    UI Page: Main menu for the app.
*/

def mainPage() {
    atomicState.backgroundActionInProgress = null
    statusMessage = ""

    def int childCount = child.size()
    dynamicPage(name: 'mainPage', title: 'Main menu', uninstall: true, install: true, submitOnChange: true)
    {
        section('<b>Integration Configuration</b>') {
            paragraph('When selecting the option for the type of setup to use, you can select the following options: Not Enabled, Managed, External.')
            paragraph('<ul><li>Not Enabled - Will not be used at all.</li><li>Managed - Integration will manage all aspects of the setup. Best for new setups.</li><li>External - Integration will allow additional features but not manage setup. Best for already configured setups.</li></ul>')
//                href 'setup', title: 'Unifi Environment Setup', description: 'Click to load values for Unifi Integrations.'
//            paragraph('<hr style="height:4px;border-width:0;color:gray;background-color:gray">')
            input 'unifiNetwork', 'enum', title: 'Unifi Network Integration', required: true, submitOnChange: true, options:[ "Not Enabled", "Managed", "External" ], defaultValue: "Not Enabled"
            if (unifiNetwork == "Managed"){
                input 'unifiNetControllerType', 'enum', title: 'Please select the controller type Protect', required: true, submitOnChange: true, options:[ "Unifi Dream Machine (inc Pro)", "Other Unifi Controllers" ], defaultValue: "Unifi Dream Machine (inc Pro)"
                if (unifiNetControllerType == "Other Unifi Controllers") {
                    input 'unifiNetControllerPort', 'string', title: 'Please enter the port of your Unifi Network controller', required: true, submitOnChange: false, defaultValue: "8443"
                }
                input 'unifiNetControllerIP', 'string', title: 'Please enter the IP of your Unifi Network controller', required: true, submitOnChange: false
                input 'unifiNetUserID', 'string', title: 'Please enter your controller User ID', required: true, submitOnChange: false
                input 'unifiNetPassword', 'password', title: 'Please enter your controller password', required: true, submitOnChange: false    
                input 'unifiNetChild', 'bool', title: 'Please activate to have child devices created for connect unifi devices', required: true, submitOnChange: false, defaultValue: false            
                input 'unifiNetRefreshRate', 'enum', title: 'Please select the controller type Protect', required: true, submitOnChange: true, options:[ "5 minutes", "10 minutes", "15 minutes", "30 minutes", "1 hour", "3 hours", "Manual" ], defaultValue: "15 minutes"        
            } else if (unifiNetwork == "External"){
                input name: "unifiNetDevice", type: "device.UnifiNetworkAPI", title: "Choose device"
            }
            paragraph('<hr style="height:2px;border-width:0;color:gray;background-color:gray">')
            input 'unifiProtect', 'enum', title: 'Unifi Protect Integration', required: true, submitOnChange: true, options:[ "Not Enabled", "Managed", "External" ], defaultValue: "Not Enabled"
            if (unifiProtect == "Managed"){
                input 'unifiProControllerType', 'enum', title: 'Please select the controller type for Portect', required: true, submitOnChange: true, options:[ "Unifi Dream Machine (inc Pro)", "Other Unifi Controllers" ], defaultValue: "Unifi Dream Machine (inc Pro)"
                if (unifiProControllerType == "Other Unifi Controllers") {
                    input 'unifiProControllerPort', 'string', title: 'Please enter the port of your Unifi Protect controller', required: true, submitOnChange: false, defaultValue: "7443"
                }
                input 'unifiProControllerIP', 'string', title: 'Please enter the IP of your Protect Controllercontroller', required: true, submitOnChange: false
                input 'unifiProUserID', 'string', title: 'Please enter your controller User ID', required: true, submitOnChange: false
                input 'unifiProPassword', 'password', title: 'Please enter your controller password', required: true, submitOnChange: false            
                input 'unifiProRefreshRate', 'enum', title: 'Please select the controller type Protect', required: true, submitOnChange: true, options:[ "5 minutes", "10 minutes", "15 minutes", "30 minutes", "1 hour", "3 hours", "Manual" ], defaultValue: "15 minutes"        
            } else if (unifiProtect == "External"){
                input name: "unifiProDevice", type: "device.UnifiProtectAPI", title: "Choose device"
            }
            paragraph('<hr style="height:2px;border-width:0;color:gray;background-color:gray">')
            input 'unifiConnect', 'enum', title: 'Unifi Connect Integration', required: true, submitOnChange: true, options:[ "Not Enabled", "Managed", "External" ], defaultValue: "Not Enabled"
            if (unifiConnect == "Managed"){
                input 'unifiConControllerType', 'enum', title: 'Please select the controller type Connect', required: true, submitOnChange: true, options:[ "Unifi Dream Machine (inc Pro)", "Other Unifi Controllers" ], defaultValue: "Unifi Dream Machine (inc Pro)"
                if (unifiConControllerType == "Other Unifi Controllers") {
                    input 'unifiConControllerPort', 'string', title: 'Please enter the port of your Unifi Connect controller', required: true, submitOnChange: falsed, defaultValue: "7443"
                }
                input 'unifiConControllerIP', 'string', title: 'Please enter the IP of your Connect controller', required: true, submitOnChange: false
                input 'unifiConUserID', 'string', title: 'Please enter your controller User ID', required: true, submitOnChange: false
                input 'unifiConPassword', 'password', title: 'Please enter your controller password', required: true, submitOnChange: false            
                input 'unifiConRefreshRate', 'enum', title: 'Please select the controller type Protect', required: true, submitOnChange: true, options:[ "5 minutes", "10 minutes", "15 minutes", "30 minutes", "1 hour", "3 hours", "Manual" ], defaultValue: "15 minutes"        
            } else if (unifiConnect == "External"){
                input name: "unifiConDevice", type: "device.UnifiConnectAPI", title: "Choose device"
            }
        paragraph('<hr style="height:4px;border-width:0;color:gray;background-color:gray">')
            }

        section('<b>Unifi Integration API Token</b>')
        {
        paragraph "API Token is only required for outbound webhook calls from Hubitat to Unifi Alarm Manager. You will not be able to enable those functions until this is entered. "
            input 'unifiApiToken', 'string', title: 'Please enter your Unifi Integration API Token here.', required: false, submitOnChange: true
//            paragraph('<hr style="height:4px;border-width:0;color:gray;background-color:gray">')
        }

        section("<b>Outbound Webhook Calls</b>") {
            paragraph "Outbound Webhook Trigger Child Apps"
            if (unifiApiToken){
                app(name: "Outbound Webhook App", appName: "Unifi Integration Manager Outbound-Webhook", namespace: "Mavrrick", title: "Add Alarm Manager Webhook app", multiple: true)
            } else {
                paragraph "<b>No API Token Configured for integration. Please setup API token for Outbound Webhook Child apps to be avaliable</b>"
            }
            paragraph('<hr style="height:4px;border-width:0;color:gray;background-color:gray">')
        }
        
        section("") {
         if (state.accessToken == null) {
             section("Inbound Webhook", hidden: true) {
                paragraph("API is not yet Initialized! Click below to complete setup")
                href(name: "hrefPageEnableAPI", title: "Enable API", description: "", page: "pageEnableAPI")
             }
         } else { 
		        section("Inbound Webhook: (Expand for directions on use)", hideable: true, hidden: true) {
                    paragraph """This url is to allow you to send information from Unifi Alarm manager to Hubitat based on known alarm manager events. You will use the below URLs with updated params for dni, type, and value to convey what the Alarm Manager event means. <br><br><ul><li>Replace %DEVICE_DNI% with the Hubitat Device DNI intended to recieve the event.</li> <li>Replace %DETECTION_TYPE% with the Detection type from Alarm Manager.</li> <li>Replace %Additional_PARM% with any additional relevant info for the Alarm Manager event like the person or license plate detected</li></ul>"""
                    paragraph """Valid Detection types are:<br><br><ul><li>Face</li><li>LicensePlate</li><li>NFCCardScan</li><li>FingerprintScan</li><li>Sound</li><li>PersonOfInterest</li><li>KnownFace</li><li>UnknownFace</li><li>VehicleOfInterest</li><li>KnownVehicle</li><li>UnknownVehicle</li><li>Person</li><li>Vehicle</li><li>Package</li><li>Animal</li><li>LineCrossing</li><li>Loitering</li><li>DoorbellRings</li><li>Motion</li></ul> Enter exactly as shown here with proper case"""
                } 
                
  		        section("Inbound Webook URLs") {
                    String localURL = "${state.localAPIEndpoint}/?access_token=${state.accessToken}&dni=%DEVICE_DNI%&type=%DETECTION_TYPE%&value=%Additional_PARM%"
//                    String remoteURL = "${state.remoteAPIEndpoint}/?access_token=${state.accessToken}&dni=%DEVICE_DNI%&type=%DETECTION_TYPE%&value=%Additional_PARM%"
                    paragraph("LOCAL API URL: <a href=\"$localURL\" target=\"_blank\">$localURL</a>")
//                    paragraph("REMOTE API: <a href=\"$remoteURL\" target=\"_blank\">$remoteURL</a>")
                }             
            }
            paragraph('<hr style="height:4px;border-width:0;color:gray;background-color:gray">')
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
    }
}


String initializeAPIEndpoint() {
    if(!state.accessToken) {
        if(createAccessToken() != null) {
            state.endpoint = getApiServerUrl()
            state.localAPIEndpoint = getFullLocalApiServerUrl()
            state.remoteAPIEndpoint = getFullApiServerUrl()
        }
    }
    return state.accessToken
}

/* Pages */
Map pageDisableAPI() {
    dynamicPage(name: "pageDisableAPI") {
        section() {
            if (state.accessToken != null) {
                state.accessToken = null
                state.endpoint = null
                paragraph("SUCCESS: API Access Token REVOKED! Tap Done to continue")
            }
        }
    }
}

Map pageEnableAPI() {
    dynamicPage(name: "pageEnableAPI", title: "", nextPage: "mainPage") {
        section() {
            if(state.accessToken == null) {
                initializeAPIEndpoint()
            }
            if (state.accessToken == null){
                paragraph("FAILURE: API NOT Initialized!")
            } else {
                paragraph("SUCCESS: API Initialized! Tap Done to continue")
            }
        }
    }
}

def installed() {
    if (unifiNetwork == "Managed"){
        unifiNetInstall()
    }
    if (unifiProtect == "Managed"){
        unifiProInstall()
    }
    if (unifiConnect == "Managed"){
        unifiConInstall()
    }
    state.isInstalled = true
}

def updated() {
    List childDNI = getChildDevices().deviceNetworkId
    if (childDNI.contains("UnifiNetworkAPI") == false) {
        if (unifiNetwork == "Managed"){
            unifiNetInstall()
        }
    } else {
        setNetPref()
    }
    if (childDNI.contains("UnifiProtectAPI") == false) {
        if (unifiProtect == "Managed"){
            unifiProInstall()
        }
    } else {
        setProPref()        
    }
    if (childDNI.contains("UnifiConnectAPI") == false) {
        if (unifiConnect == "Managed"){
            unifiConInstall()
        }
    } else {
        setNetPref()
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

/**
*
* Routine called by using inbound Webook with at 
*
*/

def webHook () {
    log.debug("Processing a webHook() $params")
    log.debug("Processing a webHook() $params.dni $params.type $params.value")
    String devicedni = params.dni.toString()
    String type = params.type.toString()
    String value = params.value.toString()
    if (unifiProtect == "Managed"){
        device = getChildDevice('UnifiProtectAPI')
        device.ApplyWebHook(devicedni, type, value)
    } else {
	    unifiProDevice.ApplyWebHook(devicedni, type, value)
    }
}

/**
 *  Device Install Wrapper functions  
 **/

void unifiNetInstall() {
    List childDNI = getChildDevices().deviceNetworkId
    if (childDNI.contains("UnifiNetworkAPI") == false) {
        logger("unifiNetInstall()  configuring Govee v2 Device Manager", 'info')
        addChildDevice('Snell', 'UnifiNetworkAPI', "UnifiNetworkAPI" , location.hubs[0].id, [
            'name': 'UnifiNetworkAPI',
            'label': 'UnifiNetworkAPI',
             'data': [
                'apiKey': settings.unifiApiToken
             ],
             'completedSetup': true,
         ])
    }
    setNetPref()
}

void unifiProInstall() {
    List childDNI = getChildDevices().deviceNetworkId
    if (childDNI.contains("UnifiProtectAPI") == false) {
        logger("unifiProInstall()  configuring Govee v2 Device Manager", 'info')
        addChildDevice('Snell', 'UnifiProtectAPI', "UnifiProtectAPI" , location.hubs[0].id, [
            'name': 'UnifiProtectAPI',
            'label': 'UnifiProtectAPI',
             'data': [
                'apiKey': settings.unifiApiToken
             ],
             'completedSetup': true,
         ])
    }
    setProPref()
}

void unifiConInstall() {
    List childDNI = getChildDevices().deviceNetworkId
    if (childDNI.contains("UnifiConnectAPI") == false) {
        logger("unifiConInstall()  configuring Govee v2 Device Manager", 'info')
        addChildDevice('Snell', 'UnifiConnectAPI', "UnifiConnectAPI" , location.hubs[0].id, [
            'name': 'UnifiConnectAPI',
            'label': 'UnifiConnectAPI',
             'data': [
                'apiKey': settings.unifiApiToken
             ],
             'completedSetup': true,
         ])
    } 
    setConPref()
}

/*
* Set preferences helper app for each integration
*/
void setNetPref(){
    log.debug "Setting values for Unifi Network App"
    device = getChildDevice('UnifiNetworkAPI')
    device.unschedule(login)
    device.updateDevSettings("Controller", "enum", unifiNetControllerType)
    if (unifiProControllerType == "Other Unifi Controllers") {
        device.updateDevSettings("ControllerPort", "string", unifiNetControllerPort)
    }  
    device.updateDevSettings("UnifiURL", "string", unifiNetControllerIP)
    device.updateDevSettings("Username", "string", unifiNetUserID)
    device.updateDevSettings("Password", "password", unifiNetPassword)
    device.updateDevSettings("UnifiChildren", "bool", unifiNetChild.toString())
    device.updateDevSettings("RefreshRate", "enum", unifiNetRefreshRate)
    device.Login()
    device.updated()
}

void setProPref(){
    device = getChildDevice('UnifiProtectAPI')
    device.unschedule(login)
    device.updateDevSettings("Controller", "enum", unifiProControllerType)
    if (unifiProControllerType == "Other Unifi Controllers") {
        device.updateDevSettings("ControllerPort", "string", unifiProControllerPort)
    }  
    device.updateDevSettings("UnifiURL", "string", unifiProControllerIP)
    device.updateDevSettings("Username", "string", unifiProUserID)
    device.updateDevSettings("Password", "password", unifiProPassword)
    device.updateDevSettings("RefreshRate", "enum", unifiProRefreshRate)
    device.Login()
    device.refresh()
    device.SetScheduledTasks()
}

void setConPref(){
    device = getChildDevice('UnifiConnectAPI')
    device.unschedule(login)
    device.updateDevSettings("Controller", "enum", unifiConControllerType)
    if (unifiProControllerType == "Other Unifi Controllers") {
        device.updateDevSettings("ControllerPort", "string", unifiConControllerPort)
    }  
    device.updateDevSettings("UnifiURL", "string", unifiConControllerIP)
    device.updateDevSettings("Username", "string", unifiConUserID)
    device.updateDevSettings("Password", "password", unifiConPassword)
    device.updateDevSettings("RefreshRate", "enum", unifiConRefreshRate)    
    device.Login()
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


