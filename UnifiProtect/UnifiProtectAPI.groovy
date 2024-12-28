/*
* UnifiProtectAPI
*
* Description:
* This Hubitat driver allows polling of the Unifi Protect API, initially geared around a Unifi Dream Machine Pro.
*
* Overall Setup:
* 1) Add both the UnifiProtectAPI.groovy and the related child drivers as new user drivers
*   NOTE: Protect uses Access Points as bridge devices by default so in most cases you will definitely need the bridge child.
* 2) Add a Virtual Device for the parent
* 3) Enter the Unifi's IP/Hostname, Username, Password, and select the Controller Type in the Preference fields and Save Preferences
* REQUIRED for "Other Unifi Controllers" Controller Type: Set the Controller Port #. This Preference will appear after you Save Preferences.
*   Set it, then Save Preferences again. This defaults to 7443 but newer version software may be using 443.
* OPTIONAL: Refresh Rate, Logging, and whether WebSockets are enabled
*
* Features List:
* WebSocket notifications allow immediate notification of motion and other events
* Shows NVR CPU and memory/storage usage as well as uptime and general status
* Shows Bridge uptime and general status
* Shows Light data and status such as whether it is on or current has motion, although this is impacted by the refresh rate
* Allow basic control of a Light-based device
* Checks drdsnell.com for an updated driver on a daily basis
*
* Licensing:
* Copyright 2024 David Snell
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
* in compliance with the License. You may obtain a copy of the License at: http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
* for the specific language governing permissions and limitations under the License.
*
* Known Issue(s):
* 
* Version Control:
* 0.2.49 - Change to smartDetectTypes data handling, additional data handling, and a new WebSocket type
* 0.2.48 - Change to Fingerprint data point due to Ubiquiti change
* 0.2.47 - Additional data handling
* 0.2.46 - Changed fingerprint data to be a forced event, so it will always be published
* 0.2.45 - Put fingerprint data to null if no userId provided
* 0.2.44 - Initial inclusion of fingerprint data for Doorbell
* 0.2.43 - Slight changes to WebSocket code for readability
* 0.2.42 - Rework of WebSocket code per @tomw's newer basis
* 0.2.41 - Adding support for 3rd-party camera attributes
* 0.2.40 - Handling for more data returned from the API, correction for turning off WebSockets, and correction to CPU Load %
* 0.2.39 - Added ability for PTZ cameras to go to preset positions, change to StopMoving command, and some additional data handling
* 0.2.38 - Switch PTZ cameras to new PTZCamera child driver, initial support for Pan/Tilt/Zoom/Home/Stop functions with a G5 PTZ as testbed
* 0.2.37 - Handling for changed authorization Token name in newer versions of Unifi OS
* 0.2.36 - Correction of isStateChange
* 0.2.35 - Additional handling for webSocket types
* 0.2.34 - Change to remove old driver-specific variables and events plus some data handling
* 0.2.33 - Added PowerDown command
* 0.2.32 - Added webSocket handling to try to deal with doorbell "ring" type and changed driver-specific attribute names to remove spaces
* 0.2.31 - Added device recognition for UVC G4 Doorbell Pro PoE
* 0.2.30 - Handling for additional data points returned by the API
* 0.2.29 - Change to receiving CSRF to account for changes in Unifi OS 3.2.8 and additional data handling
* 0.2.28 - Changes to parse to handle smart events better, using code from @blocklanders
* 0.2.27 - Additional null data handling in WebSocket code
* 0.2.26 - Additional handling for contact and motion from Sensors
* 0.2.25 - Added Sensor settings to accomodate API changes
* 0.2.24 - Handling for null epoch values
* 0.2.23 - Added recognition of additional data, particularly around sensors
* 0.2.22 - Adds ability to force child events to be isChanged
* 0.2.21 - Including CSRF in Login even if null
* 0.2.20 - Changes to accomodate UP-Sense sensor
* 0.2.19 - Correction to smartDetect indices per @dcaton1220 and attempt at handling multiple types
* 0.2.18 - Changes to Login error handling (trying to handle MFA better) and change to Uptime data handling for when devices add 000
* 0.2.17 - Revisions to WebSocket handling including input from @dcaton1220
* 0.2.16 - Removed calls for Login and refresh from within the WebSocket activities
* 0.2.15 - Reduced events triggered by WebSocket status activity, overhaul of WebSocket returned events handling, and correction to parent GetSnapshot command
* 0.2.14 - Adding smartDetectType into WebSocket data handling also changed how WebSocket Failures and LoginRetries are handled/tracked
* 0.2.13 - Added some Trace logging to the WebSocket data portion
* 0.2.12 - Changes to WebSocket code to try to more closely match @tomw's
* 0.2.11 - Cookie expiration may have changed so Login now happens every 10 minutes to account for it and now ignoring liveviews data (not useful at this time)
* 0.2.10 - Added Controller Port # preference for Other Unifi Controllers, removed PollingOK, site attribute, and excessive WebSocket logging,
*  added GetMotionEvents command however it just dumps data to Trace logging at this time
* 0.2.9 - Handle null uptime data
* 0.2.8 - Ability to get a snapshot image from a camera and better logging for adding child driver error
* 0.2.7 - Corrections to WebSocket handling (due to Ubiquiti changes) as well as additional data points handled
* 0.2.6 - Additional data points handled
* 0.2.5 - Added ability to save settings for camera devices
* 0.2.4 - Addition of initial websocket monitoring
* 0.2.3 - Changes to the driver version checking, moved child device data processing to parent, and added support for Doorbell child
* 0.2.2 - Initial addition of support for Camera devices
* 0.2.1 - Additional commands for light devices
* 0.2.0 - Setting up for different child devices with different drivers and attempting to have relevant child commands
* 0.1.3 - Correction to driver update checking code
* 0.1.2 - Added read-only support for recognize lights
* 0.1.1 - Some cleanup from Network leftovers
* 0.1.0 - Initial version
* 
* Thank you(s):
* @tomw for the WebSocket data parsing code
* @Cobra for original inspiration on driver version checking
* @user2371 for letting me know about Hubitat adding the uploadHubFile command so images can now be captured
*/

// Returns the driver name
def DriverName(){
    return "UnifiProtectAPI"
}

// Returns the driver version
def DriverVersion(){
    return "0.2.49"
}

// Driver Metadata
metadata{
	definition( name: "UnifiProtectAPI", namespace: "Snell", author: "David Snell", importUrl: "https://www.drdsnell.com/projects/hubitat/drivers/UnifiProtectAPI.groovy" ) {
		// Indicate what capabilities the device should be capable of
		capability "Sensor"
		capability "Refresh"
        capability "Actuator"
        
        // Commands
        //command "DoSomething" // Does something for development/testing purposes, should be commented before publishing
        //command "ConnectWebSocket" // Connect to the controller for WebSocket notices
        //command "CloseWebSocket" // Connect to the controller for WebSocket notices
        command "Login" // Logs in to the controller to get a cookie for the session
        command "GetSnapshot", [ [ name: "CameraID*", type: "STRING", description: "REQUIRED: Camera ID to get snapshot from" ] ]

        // Unifi Protect related commands
        command "GetProtectInfo" // Checks for general data on the Protect controller, same as refresh
        
        // System level commands
        command "PowerDown", [
            [ name: "Confirmation", type: "STRING", description: "Type the word PowerDown to confirm intent to power down the controller." ]
        ] // Submits a reboot command to the controller.
        
        // API Methods Not Implemented
        // "api/events?end=EPOCH&start=EPOCH" // Appears to be able to show events between a range of epoch-based times
        // "api/backups" // 
        // "api/cameras" // May show camera data
        command "GetMotionEvents", [ [ name: "Number*", type: "ENUM", description: "REQUIRED: Number of events to get", defaultValue: 10, constraints: [ 1, 5, 10, 25, 50, 100 ] ] ]

        // Generic WebHook command Mavrrick
        command "webhook", [[name: "DeviceID*", type: "STRING", description: "Camera ID to apply Webhook event to"],
                          [name: "Attribute", type: "STRING", description: "Type of attribute to be applied to child device"],
                          [name: "Value", type: "STRING", description: "Attribute value"]]
        
        
		// Attributes for the driver itself
		attribute "DriverName", "string" // Identifies the driver being used for update purposes
		attribute "DriverVersion", "string" // Handles version for driver
        attribute "DriverStatus", "string" // Handles version notices for driver
        
        // Attributes for the device
        attribute "Status", "string" // Show success/failure of commands performed
        attribute "Last Login", "string" // Shows when the last login was performed
        attribute "Last Refresh", "string" // Shows when the last refresh was performed
        attribute "Last Updated ID", "string" // Shows the ID for the last update from the bootstrap
        attribute "WebSocket Delay", "number"
        attribute "WebSocket Status", "enum", [ "OK", "Unknown", "Error" ]
        attribute "WebSocket Open", "enum", [ "open", "closed" ]
    }
	preferences{
		section{
            if( ShowAllPreferences || ShowAllPreferences == null ){ // Show the preferences options
                input( type: "enum", name: "RefreshRate", title: "<b>Stats Refresh Rate</b>", required: true, multiple: false, options: [ "5 minutes", "10 minutes", "15 minutes", "30 minutes", "1 hour", "3 hours", "Manual" ], defaultValue: "Manual" )
                input( type: "bool", name: "EnableWebSocket", title: "<b>Enable WebSocket Monitoring</b></br><font size='2'>Required for notification rather than polling of device events</font>", required: false, defaultValue: true )
    			input( type: "enum", name: "LogType", title: "<b>Enable Logging?</b>", required: false, multiple: false, options: [ "None", "Info", "Debug", "Trace" ], defaultValue: "Info" )
                input( type: "enum", name: "Controller", title: "<font color='FF0000'><b>Unifi Controller Type</b></font>", required: true, multiple: false, options: [ "Unifi Dream Machine (inc Pro)", "Other Unifi Controllers" ], defaultValue: "Unifi Dream Machine (inc Pro)" )
                if( Controller == "Other Unifi Controllers" ){
				    input( type: "string", name: "ControllerPort", title: "<font color='FF0000'><b>Controller Port #</b></font>", defaultValue: "7443", required: true )
                }
                input( type: "string", name: "UnifiURL", title: "<font color='FF0000'><b>Unifi Controller IP/Hostname</b></font>", required: true )
				input( type: "string", name: "Username", title: "<font color='FF0000'><b>Username</b></font>", required: true )
				input( type: "password", name: "Password", title: "<font color='FF0000'><b>Password</b></font>", required: true )
                input( type: "bool", name: "ShowAllPreferences", title: "<b>Show All Preferences?</b>", defaultValue: true )
            } else {
                input( type: "bool", name: "ShowAllPreferences", title: "<b>Show All Preferences?</b>", defaultValue: true )
            }
        }
	}
}

// Command to test fixes or other oddities during development
def DoSomething(){

}

// Create a WebSocket connection to monitor for events
def ConnectWebSocket(){
    try{
        if( Controller == "Unifi Dream Machine (inc Pro)" ){
            interfaces.webSocket.connect( "wss://${ UnifiURL }/proxy/protect/ws/updates?lastUpdateId=${ state.'Last Updated ID' }", ignoreSSLIssues: true, headers: [ Host: "${ UnifiURL }", Accept: "*/*", Cookie: "${ state.Cookie }", 'X-CSRF-Token': "${ state.CSRF }", 'accessKey': "${ state.'Auth Key' }" ] )
	    } else {
            interfaces.webSocket.connect( "wss://${ UnifiURL }:${ ControllerPort }/ws/updates?lastUpdateId=${ state.'Last Updated ID' }", ignoreSSLIssues: true, headers: [ Host: "${ UnifiURL }", Accept: "*/*", Cookie: "${ state.Cookie }", 'X-CSRF-Token': "${ state.CSRF }", 'accessKey': "${ state.'Auth Key' }" ] )
	    }
    }
    catch( Exception e ){
        Logging( "Exception when opening WebSocket: ${ e }", 5 )
        ProcessState( "WebSocket Status", "Error" )
    }
}

// Close the WebSocket connection
def CloseWebSocket(){
    try{
        ProcessState( "WebSocket Open", "closed" )
        pauseExecution( 500 )
        interfaces.webSocket.close()
    }
    catch( Exception e ){
        Logging( "Exception when closing WebSocket: ${ e }", 5 )
        ProcessState( "WebSocket Status", "WebSocket close error" )
    }
}

// Send a WebSocket message, if needed at some point
def SendWebSocketMessage( String Message ){
    try{
        interfaces.webSocket.sendMessage( Message )
    }
    catch( Exception e ){
        Logging( "Exception when sending WebSocket message: ${ e }", 5 )
        ProcessState( "WebSocket Status", "WebSocket message error" )
    }
}

// parse appears to be one of those "special" methods for when data is returned, for this driver the WebSocket data
// Changes to beginning regarding modelKey and events from @blocklanders
def parse( String description ){
    //def Data = DecodeWebSocket( description )
    //Logging( "WS Raw = ${ description }", 3 )
    def Data = packetValidateAndDecode( description )
    if( Data != null ){
        Logging( "WebSocket Data = ${ Data }", 4 )
        def modelKey = Data.actionPacket.actionPayload.modelKey
        def EventID
        def TempID
        switch( modelKey ){
            case "event":
                TempID = Data.actionPacket.actionPayload.recordId
                if( TempID.indexOf( "-" ) != -1 ){
                    TempID = Data.actionPacket.actionPayload.recordId.split( "-" )
                    EventID = TempID[ 0 ]
                    TempID = TempID[ 1 ]
                } else {
                    //TempID = Data.actionPacket.actionPayload.recordId
                    EventID = TempID
                }
                break
            default:
                TempID = Data.actionPacket.actionPayload.id    
                if( TempID.indexOf( "-" ) != -1 ){
                    TempID = Data.actionPacket.actionPayload.id.split( "-" )
                    EventID = TempID[ 0 ]
                    TempID = TempID[ 1 ]
                } else {
                    //TempID = Data.actionPacket.actionPayload.id
                    EventID = TempID
                }
                break
        }
        def Device
        def LastAction
        def LastActionID
		getChildDevices().each{
            if( TempID == getChildDevice( it.deviceNetworkId ).ReturnState( "ID" ) ){
                Device = it.deviceNetworkId
                LastAction = getChildDevice( it.deviceNetworkId ).ReturnState( "LastWSSAction" )
                LastActionID = getChildDevice( it.deviceNetworkId ).ReturnState( "LastWSSID" )
            }
		}
        if( ( Device == null ) && ( Data.dataPacket?.dataPayload != null ) ){
            if( Data.dataPacket?.dataPayload.metadata!= null ){
                if( Data.dataPacket?.dataPayload.metadata.sensorId != null ){
                    if( Data.dataPacket?.dataPayload.metadata.sensorId.text != null ){
                        TempID = Data.dataPacket?.dataPayload.metadata.sensorId.text
                        getChildDevices().each{
                            if( TempID == getChildDevice( it.deviceNetworkId ).ReturnState( "ID" ) ){
                                Device = it.deviceNetworkId
                                LastAction = getChildDevice( it.deviceNetworkId ).ReturnState( "LastWSSAction" )
                                LastActionID = getChildDevice( it.deviceNetworkId ).ReturnState( "LastWSSID" )
                            }
                        }
                    }
                }
            }
		}
		if( Device != null ){
            if( Data.dataPacket.dataPayload != null ){
                Data.dataPacket.dataPayload.each(){
                    switch( it.key ){
                        case "lastMotion":
                            if( it.value != null ){
                                PostEventToChild( "${ Device }", "Last Motion", ConvertEpochToDate( "${ it.value }" ) )
                            }
                            break
                        case "lastRing":
                            if( it.value != null ){
                                getChildDevice( "${ Device }" ).push( 1 )
                            }
                            break
                        case "isDark":
                            if( it.value ){
                                PostEventToChild( "${ Device }", "Dark", "true", null, true )
                            } else {
								PostEventToChild( "${ Device }", "Dark", "false", null, true )
							}
                            if( Data.actionPacket?.actionPayload?.action?.toString() == "add" ){
							    ProcessState( "LastWSSAction", "Dark" )
                            }
                            PostStateToChild( "${ Device }", "LastWSSAction", "Dark" )
							break
						case "isMotionDetected":
							if( it.value ){
								PostEventToChild( "${ Device }", "motion", "active", null, true )
							} else {
								PostEventToChild( "${ Device }", "motion", "inactive", null, true )
							}
                            if( Data.actionPacket?.actionPayload?.action?.toString() == "add" ){
							    ProcessState( "LastWSSAction", "motionDetected" )
                            }
                            PostStateToChild( "${ Device }", "LastWSSAction", "motionDetected" )
							break
						case "isPirMotionDetected":
							if( it.value ){
								PostEventToChild( "${ Device }", "motion", "active", null, true )
							} else {
								PostEventToChild( "${ Device }", "motion", "inactive", null, true )
							}
                            if( Data.actionPacket?.actionPayload?.action?.toString() == "add" ){
							    ProcessState( "LastWSSAction", "motionDetected" )
                            }
                            PostStateToChild( "${ Device }", "LastWSSAction", "motionDetected" )
							break
						case "isSmartDetected":
							if( it.value ){
								PostEventToChild( "${ Device }", "motion", "active", null, true )
							} else {
                                PostEventToChild( "${ Device }", "motion", "inactive", null, true )
                                //PostEventToChild( "${ Device }", "smartDetectType", "none" )
							}
                            if( Data.actionPacket?.actionPayload?.action?.toString() == "add" ){
							    ProcessState( "LastWSSAction", "motionDetected" )
                            }
                            PostStateToChild( "${ Device }", "LastWSSAction", "motionDetected" )
							break
						case "smartDetectTypes":
                            if( it.value.size() > 1 ){
                                def TempString = ""
                                def Count = 0
    							def Size = it.value.size()
                                it.value.each{
                                    TempString = TempString + "${ it }"
                                    Count = ( Count + 1 )
                                    if( Count < Size ){
                                        TempString = TempString + ", "
                                    } else {
                                        TempString = TempString + "."
                                    }
                                }
                                if( ( TempString != null ) && ( TempString != "null" ) ){
                                    PostEventToChild( "${ Device }", "smartDetectType", TempString, null, true )
                                }
                                if( Data.actionPacket?.actionPayload?.action?.toString() == "add" ){
							        ProcessState( "LastWSSAction", "smartDetectType" )
                                }
                                PostStateToChild( "${ Device }", "LastWSSAction", "smartDetectType" )
                            } else if( it.value.size() == 1 ){
                                if( ( "${ it.value[ 0 ] }" != null ) && ( "${ it.value[ 0 ] }" != "null" ) ){
                                    PostEventToChild( "${ Device }", "smartDetectType", "${ it.value[ 0 ] }", null, true )
                                }
                                if( Data.actionPacket?.actionPayload?.action?.toString() == "add" ){
							        ProcessState( "LastWSSAction", "smartDetectType" )
                                }
                                PostStateToChild( "${ Device }", "LastWSSAction", "smartDetectType" )
                            } else {
                                PostEventToChild( "${ Device }", "smartDetectType", "none" )
                            }
							break
						case "isLightOn":
							if( it.value ){
								PostEventToChild( "${ Device }", "switch", "on", null, true )
							} else {
								PostEventToChild( "${ Device }", "switch", "off", null, true )
							}
                            if( Data.actionPacket?.actionPayload?.action?.toString() == "add" ){
							    ProcessState( "LastWSSAction", "Light" )
                            }
                            PostStateToChild( "${ Device }", "LastWSSAction", "Light" )
							break
						case "isRecording":
							if( it.value ){
								PostEventToChild( "${ Device }", "Recording Now", "true", null, true )
							} else {
								PostEventToChild( "${ Device }", "Recording Now", "false", null, true )
							}
                            if( Data.actionPacket?.actionPayload?.action?.toString() == "add" ){
							    ProcessState( "LastWSSAction", "Recording Now" )
                            }
                            PostStateToChild( "${ Device }", "LastWSSAction", "Recording Now" )
							break
						case "eventStats":
							if( it.value.motion.today != null ){
								PostEventToChild( "${ Device }", "Motion Events Today", it.value.motion.today )
							}
							break
                        case "type":
                            switch( it.value ){
                                case "motion":
                                    PostEventToChild( "${ Device }", "motion", "active" )
                                    if( Data.actionPacket?.actionPayload?.action?.toString() == "add" ){
							            ProcessState( "LastWSSAction", "motion" )
                                    }
                                    PostStateToChild( "${ Device }", "LastWSSAction", "motion" )
                                    //pauseExecution( 1000 )
                                    //PostEventToChild( "${ Device }", "motion", "inactive" )
                                    break
                                case "sensorMotion":
                                    PostEventToChild( "${ Device }", "motion", "active" )
                                    if( Data.actionPacket?.actionPayload?.action?.toString() == "add" ){
							            ProcessState( "LastWSSAction", "sensorMotion" )
                                    }
                                    PostStateToChild( "${ Device }", "LastWSSAction", "sensorMotion" )
                                    //pauseExecution( 1000 )
                                    //PostEventToChild( "${ Device }", "motion", "inactive" )
                                    break
                                case "sensorOpened":
                                    PostEventToChild( "${ Device }", "contact", "open" )
                                    if( Data.actionPacket?.actionPayload?.action?.toString() == "add" ){
							            ProcessState( "LastWSSAction", "sensorOpened" )
                                    }
                                    PostStateToChild( "${ Device }", "LastWSSAction", "sensorOpened" )
                                    break
                                case "sensorClosed":
                                    PostEventToChild( "${ Device }", "contact", "closed" )
                                    if( Data.actionPacket?.actionPayload?.action?.toString() == "add" ){
							            ProcessState( "LastWSSAction", "sensorClosed" )
                                    }
                                    PostStateToChild( "${ Device }", "LastWSSAction", "sensorClosed" )
                                    break
                                case "ring":
                                    getChildDevice( "${ Device }" ).push( 1 )
                                    if( Data.actionPacket?.actionPayload?.action?.toString() == "add" ){
							            ProcessState( "LastWSSAction", "ring" )
                                    }
                                    PostStateToChild( "${ Device }", "LastWSSAction", "ring" )
                                    //PostEventToChild( "${ Device }", "pushed", 1, null, true )
                                    break
                                case "smartDetectZone":
                                    PostEventToChild( "${ Device }", "smartDetectZone", "active" )
                                    break
                                case "access":
                                    PostEventToChild( "${ Device }", "access", "active" )
                                    break
                                case "deviceDisconnected":
                                    PostStateToChild( "${ Device }", "DeviceDisconnected", new Date() )
                                    break
                                case "fingerprintIdentified":
                                    if( Data.dataPacket.dataPayload.metadata != null ){
                                        if( Data.dataPacket.dataPayload.metadata.fingerprint != null ){
                                            if( Data.dataPacket.dataPayload.metadata.fingerprint.ulpId != null ){
                                                PostEventToChild( "${ Device }", "FingerprintUserID", "${ Data.dataPacket.dataPayload.metadata.fingerprint.ulpId }", null, true )
                                            } else {
                                                PostEventToChild( "${ Device }", "FingerprintUserID", null, null, true )
                                                Logging( "No userID found: ${ Data.dataPacket.dataPayload }", 3 )
                                            }
                                        } else {
                                            Logging( "No fingerprint metadata found: ${ Data.dataPacket.dataPayload }", 3 )
                                        }
                                    } else {
                                        Logging( "No metadata for fingerprint found: ${ Data.dataPacket.dataPayload }", 3 )
                                    }
                                    break
                                case "sensorExtremeValues":
                                	PostStateToChild( "${ Device }", "SensorExtremeValues", "${ it }" )
                                    break
                                default:
                                    Logging( "Unhandled WebSocket Type for ${ Device }: ${ it }", 3 )
							        break
                            }
                            break
						// Things to ignore
						case "uptime":
						case "upSince":
						case "lastSeen":
						case "recordingSchedules":
						case "wifiConnectionState":
							break
						default:
							Logging( "${ Device } Update: ${ it.key } = ${ it.value }", 4 )
							break
					}
                    ProcessState( "LastWSSID", EventID )
                    PostStateToChild( Device, "LastWSSID", EventID )
                }
            } else if( ( Data.actionPacket?.actionPayload?.action?.toString() == "update" ) && ( ( Data.dataPacket?.dataPayload == null ) || ( Data.dataPacket?.dataPayload == "null" ) ) ){
				if( LastActionID == EventID ){
					switch( LastAction ){
						case "Dark":
							PostEventToChild( "${ Device }", "Dark", "true" )
							break
						case "motionDetected":
							PostEventToChild( "${ Device }", "motion", "inactive" )
							break
						case "smartDetectType":
                            def PreviousType = getChildDevice( "${ Device }" ).ReturnState( "smartDetectType" )
                            if( ( PreviousType != null ) && ( PreviousType != "null" ) ){
							    PostEventToChild( "${ Device }", "smartDetectType", null )
                            }
							break
						case "Light":
							PostEventToChild( "${ Device }", "switch", "off" )
							break
						case "Recording Now":
							PostEventToChild( "${ Device }", "Recording Now", "false" )
							break
                        case "motion":
                            PostEventToChild( "${ Device }", "motion", "inactive" )
							break
                        case "sensorMotion":
                            PostEventToChild( "${ Device }", "motion", "inactive" )
							break
                        case "smartDetectZone":
                            PostEventToChild( "${ Device }", "smartDetectZone", "active" )
                            break
                        case "access":
                            PostEventToChild( "${ Device }", "access", "active" )
                            break
                        case "ring":
                            getChildDevice( "${ Device }" ).release( 1 )
							break
					}
				}
			}
        }
    }
}

// Beggining of @tomw's code (with minor changes)
import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

// Receive status from the WebSocket, kindof like the WebSocket version of parse(?)
def webSocketStatus( String Response ){
    Logging( "WebSocket Status = ${ Response }", 4 )
    
    // @tomw: thanks for the idea: https://community.hubitat.com/t/websocket-client/11843/15
    if( Response.startsWith( "status: open" ) ){        
        ProcessState( "WebSocket Delay", 1 )
        ProcessState( "WebSocket Open", "open" )
        ProcessState( "WebSocket Status", "OK" )
        return
    } else if( Response.startsWith( "status: closing" ) ){
        if( state.'WebSocket Open' != "open" ){
            ProcessState( "WebSocket Open", "closed" )
            return
        }
        reinitialize()
        return
    }
    else if( Response.startsWith( "failure:" ) ){
        ProcessState( "WebSocket Failures", ( state.'WebSocket Failures' + 1 ) )
        if( state.'WebSocket Failures' > 5 ){
            ProcessState( "WebSocket Status", "Failed - Requires a manual login to reset" )
        } else {
            ProcessState( "WebSocket Status", "Error" )
            reinitialize()
        }
        return
    }
}

def reinitialize(){
    // thanks @ogiewon for the example
    
    // first delay is 2 seconds, doubles every time
    def delayCalc = ( state.'WebSocket Delay' ?: 1 ) * 2    
    // upper limit is 600s
    def reconnectDelay  = delayCalc <= 600 ? delayCalc : 600
    
    ProcessState( "WebSocket Delay", reconnectDelay  )
    runIn( reconnectDelay, initialize )
}

def initialize(){
    if( EnableWebSocket && ( state.'WebSocket Failures' < 5 ) ){
        //ProcessEvent( "WebSocket Status", "Unknown" )
        try {
            unschedule( "initialize" )
            CloseWebSocket()

            runIn( 5, ConnectWebSocket )
        
            ProcessState( "WebSocket Status", "OK" )
        }
        catch( Exception e ){
            Logging( "WebSocket initialization failed: ${ e.message }", 5 )
            ProcessState( "WebSocket Status", "Error" )
            ProcessState( "WebSocket Failures", ( state.'WebSocket Failures' + 1 ) )
            reinitialize()
        }
    }
}

private decompress( s ){
    // based on this example: https://dzone.com/articles/how-compress-and-uncompress    
    def sBytes = hubitat.helper.HexUtils.hexStringToByteArray( s )
    
    Inflater inflater = new Inflater() 
    inflater.setInput( sBytes )
    
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream( sBytes.length )
    
    byte[] buffer = new byte[ 1024 ]
    
    while( !inflater.finished() ){  
        int count = inflater.inflate( buffer )
        outputStream.write( buffer, 0, count )
    }
    
    outputStream.close()
    
    def resp = new String( outputStream.toByteArray() )    
    
    return resp
}

private subBytes( arr, start, length ){
    return arr.toList().subList( start, start + length ) as byte[]
}

private repackHeaderAsMap( header ){
    def headerMap = [
            packetType: subBytes( header, 0, 1 ),
            payloadFormat: subBytes( header, 1, 1 ),
            deflated: subBytes( header, 2, 1 ),
            payloadSize: hubitat.helper.HexUtils.hexStringToInt( hubitat.helper.HexUtils.byteArrayToHexString( subBytes( header, 4, 4 ) ) )
        ]
}

import groovy.transform.Field

def encHexMacro( string ) { return string.getBytes()?.encodeHex()?.toString() }

@Field String actionAdd        = encHexMacro( '"action":"add"' )
@Field String actionUpdate     = encHexMacro( '"action":"update"' )
@Field String modelKeyCamera   = encHexMacro( '"modelKey":"camera"' )
@Field String modelKeyChime    = encHexMacro( '"modelKey":"chime"' )
@Field String modelKeyEvent    = encHexMacro( '"modelKey":"event"' )
@Field String modelKeyLight    = encHexMacro( '"modelKey":"light"' )
@Field String modelKeyViewer   = encHexMacro( '"modelKey":"viewer"' )

@Field String eventValueRing            = encHexMacro( '"ring"' )
@Field String isDarkKey                 = encHexMacro( '"isDark"' )
@Field String isLightOnKey              = encHexMacro( '"isLightOn"' )
@Field String isMotionDetectedKey       = encHexMacro( '"isMotionDetected"' )
@Field String isPirMotionDetectedKey    = encHexMacro( '"isPirMotionDetected"' )
@Field String isSmartDetectedKey        = encHexMacro( '"isSmartDetected"' )
@Field String lcdMessageKey             = encHexMacro( '"lcdMessage"' )
@Field String ledSettingsKey            = encHexMacro( '"ledSettings"' )
@Field String lightDeviceSettingsKey    = encHexMacro( '"lightDeviceSettings"' )
@Field String liveviewKey               = encHexMacro( '"liveview"' )
@Field String smartDetectTypesKey       = encHexMacro( '"smartDetectTypes"' )
@Field String volumeKey                 = encHexMacro( '"volume"' )

def coarsePacketValidate( hexString ){
    // Beware: this is coarse and potentially brittle.  Check here first if you are not seeing packets that you think you should!
    
    // Before doing any other processing, try to determine if this packet contains useful updates.
    // This is to limit the processing utilization on Hubitat since the Protect controller is so chatty.    
    
    def localStr = hexString?.toLowerCase()
    if( !localStr ){ return null }
    
    def searchList
    
    if( localStr.contains( modelKeyEvent ) ){
        // "event"
        searchList = [ smartDetectTypesKey, eventValueRing ]
    }
    if( localStr.contains( modelKeyCamera ) && localStr.contains( actionUpdate ) ){
        // "camera" and "update"
        searchList = [ isDarkKey, isMotionDetectedKey, isSmartDetectedKey, lcdMessageKey, ledSettingsKey ]
    }
    if( localStr.contains( modelKeyChime ) && localStr.contains( actionUpdate ) ){
        // "chime" and "update"
        searchList = [ volumeKey ]
    }
    if( localStr.contains( modelKeyLight ) && localStr.contains( actionUpdate ) ){
        // "light" and "update"
        searchList = [ isDarkKey, isLightOnKey, isMotionDetectedKey, isPirMotionDetectedKey, lightDeviceSettingsKey ]
    }
    if( localStr.contains( modelKeyViewer ) && localStr.contains( actionUpdate) ){
        // "liveview" and "update"
        searchList = [ liveviewKey ]
    }
    
    return searchList?.any { localStr.contains( it ) }
}

private packetValidateAndDecode( hexString ){
    if( !coarsePacketValidate( hexString ) ){
        //logDebug("dropped packet: ${new String(hubitat.helper.HexUtils.hexStringToByteArray(hexString))}")
        return
    }
    
    // all of this is based on the packet formats described here:  https://github.com/hjdhjd/unifi-protect/blob/main/src/protect-api-updates.ts
    def actionHeader
    def actionLength
    def dataHeader
    def dataLength
    
    def bytes
    
    //
    // first, basic packet validation
    //
    
    try{
        //logDebug("incoming message = ${hexString}")
        bytes = hubitat.helper.HexUtils.hexStringToByteArray( hexString )
        
        actionHeader = subBytes( bytes, 0, 8 )
        actionLength = hubitat.helper.HexUtils.hexStringToInt(hubitat.helper.HexUtils.byteArrayToHexString( subBytes( actionHeader, 4, 4 ) ) )
        dataHeader = subBytes( bytes, actionHeader.size() + actionLength, 8 )
        dataLength = hubitat.helper.HexUtils.hexStringToInt(hubitat.helper.HexUtils.byteArrayToHexString( subBytes( dataHeader, 4, 4 ) ) )
        
        def totalLength = actionHeader.size() + actionLength + dataHeader.size() + dataLength
        //logDebug("totalLength = ${totalLength}")
        //logDebug("bytes.size() = ${bytes.size()}")
        
        if( totalLength != bytes.size() ){
            throw new Exception("Header/Packet mismatch.")
        }
    }
    catch( Exception e ){
        Logging( "Packet validation failed: ${ e.message }", 5 )
        // any error interpreted as fail
        return null
    }
    
    //
    // then, decode and re-pack data
    //
    
    try{
        def actionHeaderMap = repackHeaderAsMap( actionHeader )
        def dataHeaderMap = repackHeaderAsMap( dataHeader )
        
        def actionPacket = hubitat.helper.HexUtils.byteArrayToHexString( subBytes( bytes, actionHeader.size(), actionHeaderMap.payloadSize ) )  
        def dataPacket = hubitat.helper.HexUtils.byteArrayToHexString( subBytes( bytes, actionHeader.size() + actionLength + dataHeader.size(), dataHeaderMap.payloadSize ) )
        
        def slurper = new groovy.json.JsonSlurper()
        
        //logDebug("actionHeaderMap = ${actionHeaderMap}")
        //logDebug("actionPacket = ${actionPacket}")

        def actionJson = actionHeaderMap.deflated?.getAt( 0 ) ? decompress( actionPacket ) : makeString( actionPacket )
        def actionJsonMap = slurper.parseText( actionJson?.toString() )
        
        def dataJson = dataHeaderMap.deflated?.getAt( 0 ) ? decompress( dataPacket ) : makeString( dataPacket )
        def dataJsonMap = slurper.parseText( dataJson?.toString() )
        
        def decodedPacket = [
                actionPacket: [ actionHeader: actionHeaderMap, actionPayload: actionJsonMap ],
                dataPacket: [ dataHeader: dataHeaderMap, dataPayload: dataJsonMap ]
        ]
        
        //logDebug("decodedPacket = ${decodedPacket}")        
        return decodedPacket
    }
    catch (Exception e)
    {
        Logging( "Packet decoding failed: ${ e.message }", 5 )
        // any error interpreted as fail
        return null
    }
}

private makeString(s){
    return new String(hubitat.helper.HexUtils.hexStringToByteArray(s))
}
// End of @tomw's code

// updated is called whenever device parameters are saved
def updated(){
    Logging( "Updating...", 2 )
    
    if( state."Driver Status" != null ){
        state.remove( "Driver Name" )
        state.remove( "Driver Version" )
        state.remove( "Driver Status" )
        device.deleteCurrentState( "Driver Status" )
        device.deleteCurrentState( "Driver Name" )
        device.deleteCurrentState( "Driver Version" )
    }
    ProcessEvent( "DriverName", DriverName(), null, true )
    ProcessEvent( "DriverVersion", DriverVersion(), null, true )
    ProcessEvent( "DriverStatus", null, null, true )
    
    if( LogType == null ){
        LogType = "Info"
    }
    if( Controller == null ){
        Controller = "Unifi Dream Machine (inc Pro)"
    }
    
    if( ControllerPort == null ){
        if( Controller == "Other Unifi Controllers" ){
            ControllerPort = "7443"
        }
    }
    
    // Reset LoginRetries counter
    state.LoginRetries = 0
    pauseExecution( 1000 )
    
    SetScheduledTasks()
    
    // WebSocket Activity
    ProcessState( "WebSocket Failures", 0 )
    if( EnableWebSocket && ( state.'WebSocket Failures' < 5 ) ){
        ConnectWebSocket()
    } else {
        CloseWebSocket()
    }
    
    Logging( "Updated", 2 )
}

// refresh performs a poll of data
def refresh(){   
    if( EnableWebSocket ){
        CloseWebSocket()
    }
    
    GetProtectInfo()
    
    if( EnableWebSocket && ( state.'WebSocket Failures' < 5 ) ){
        ConnectWebSocket()
    }
    ProcessState( "Last Refresh", new Date() )
}

// Set scheduled tasks
def SetScheduledTasks(){
    unschedule()
    // Schedule a login every 10 minutes
    def Hour = ( new Date().format( "h" ) as int )
    def Minute = ( new Date().format( "m" ) as int )
    def Second = ( new Date().format( "s" ) as int )
    Second = ( (Second + 5) % 60 )
    schedule( "${ Second } 0/10 * ? * *", "Login" )
        
    // Check what the refresh rate is set for then run it
    switch( RefreshRate ){
        case "1 minute": // Schedule the refresh check for every minute
            schedule( "${ Second } * * ? * *", "refresh" )
            break
        case "5 minutes": // Schedule the refresh check for every 5 minutes
            schedule( "${ Second } 0/5 * ? * *", "refresh" )
            break
        case "10 minutes": // Schedule the refresh check for every 10 minutes
            schedule( "${ Second } 0/10 * ? * *", "refresh" )
            break
        case "15 minutes": // Schedule the refresh check for every 15 minutes
            schedule( "${ Second } 0/15 * ? * *", "refresh" )
            break
        case "30 minutes": // Schedule the refresh check for every 30 minutes
            schedule( "${ Second } 0/30 * ? * *", "refresh" )
            break
        case "1 hour": // Schedule the refresh check for every hour
            schedule( "${ Second } ${ Minute } * ? * *", "refresh" )
            break
        case "3 hours": // Schedule the refresh check for every 3 hours
            schedule( "${ Second } ${ Minute } 0/3 ? * *", "refresh" )
            break
        default:
            RefreshRate = "Manual"
            break
    }
    Logging( "Refresh rate: ${ RefreshRate }", 4 )
    
    // Set the driver name and version before update checking is scheduled
    if( state."Driver Name" != null ){
        state.remove( "Driver Name" )
        state.remove( "Driver Version" )
        device.deleteCurrentState( "Driver Name" )
        device.deleteCurrentState( "Driver Version" )
    }
    ProcessEvent( "DriverName", DriverName() )
    ProcessEvent( "DriverVersion", DriverVersion() )
    // Schedule checks that are only performed once a day
    schedule( "${ Second } ${ Minute } ${ Hour } ? * *", "CheckForUpdate" )
}

//Log in to Unifi
def Login( Manual = true ){
    def Params
    if( Manual || ( state.LoginRetries < 5 ) ){
        if( Controller == "Unifi Dream Machine (inc Pro)" ){
            Params = [ uri: "https://${ UnifiURL }/api/auth/login", ignoreSSLIssues: true, requestContentType: "application/json", contentType: "application/json", body: "{\"username\":\"${ Username }\",\"password\":\"${ Password }\",\"remember\":\"true\"}", headers: [ 'X-CSRF-Token': state.CSRF ] ]
        } else {
            Params = [ uri: "https://${ UnifiURL }:${ ControllerPort }/api/login", ignoreSSLIssues: true, requestContentType: "application/json", contentType: "application/json", body: "{\"username\":\"${ Username }\",\"password\":\"${ Password }\",\"remember\":\"true\"}", headers: [ 'X-CSRF-Token': state.CSRF ] ]
        }
        //Logging( "Login Parameters = ${ Params }", 4 )
        try{
            httpPost( Params ){ resp ->
                //Logging( "Login response = ${ resp.data }", 4 )
	            switch( resp.getStatus() ){
		            case 200:
                        if( state.LoginRetries > 0 ){
                            ProcessState( "LoginRetries", 0 )
                            SetScheduledTasks()
                        }
                        if( Manual && EnableWebSocket ){
                            ProcessState( "WebSocket Failures", 0 )
                        }
                        ProcessState( "Status", "Login successful." )
                        ProcessState( "Last Login", new Date() )
                        def Cookie
                        resp.getHeaders().each{
                        if( ( it.value.split( '=' )[ 0 ].toString() == "unifises" ) || ( it.value.split( '=' )[ 0 ].toString() == "TOKEN" ) || ( it.value.split( '=' )[ 0 ].toString() == "UOS_TOKEN" ) ){
                                Cookie = resp.getHeaders().'Set-Cookie'
                                if( Controller == "Unifi Dream Machine (inc Pro)" ){
                                    Cookie = Cookie.split( ";" )[ 0 ] + ";"
                                } else {
                                    Cookie = Cookie.split( ";" )[ 0 ]
                                }
                                ProcessState( "Cookie", Cookie )
                            } else {
                                def CSRF
                                if( Controller == "Unifi Dream Machine (inc Pro)" ){
                                    CSRF = it as String
                                    if( CSRF.split( ':' )[ 0 ].toUpperCase() == "X-CSRF-TOKEN" ){
                                       ProcessState( "CSRF", it.value )
                                   }
                                } else {
                                    if( it.value.split( '=' )[ 0 ].toString() == "csrf_token" ){
                                        CSRF = it.value.split( ';' )[ 0 ].split( '=' )[ 1 ]
                                        ProcessState( "CSRF", CSRF )
                                    }
                                }
                            }
                        }
                        asynchttpPost( "ReceiveData", GenerateProtectParams( "api/auth/access-key" ), [ Method: "Auth Key" ] )
			            break
                    case 403:
                        Logging( "Forbidden", 3 )
                        ProcessState( "Status", "Login failure" )
                        ProcessState( "LoginRetries", ( state.LoginRetries + 1 ) )
			            break
                    case 404:
                        Logging( "Not found", 3 )
                        ProcessState( "Status", "Login failure" )
                        ProcessState( "LoginRetries", ( state.LoginRetries + 1 ) )
			            break
                    case 408:
                        Logging( "Request Timeout", 3 )
                        ProcessState( "Status", "Login failure" )
                        ProcessState( "LoginRetries", ( state.LoginRetries + 1 ) )
			            break
		            default:
			            Logging( "Error logging in to controller: ${ resp.status }", 4 )
                        ProcessState( "Status", "Login failure" )
                        ProcessState( "LoginRetries", ( state.LoginRetries + 1 ) )
			            break
	            }
            }
        } catch( Exception e ){
            if( state.LoginRetries > 5 ){
                Logging( "Too many login failures. Please confirm the local username & password and perform a manual login.", 5 )
                ProcessState( "Status", "Login failure, check account information and perform manual login." )
                unschedule( "Login" )
                unschedule( "refresh" )
            } else {
                def Temp = "${ e }"
                Logging( "Temp Login Status = ${ Temp }", 3 )
                def StatusCode = Temp.split( "status code: " )[ 1 ].split( "," )[ 0 ]
                switch( StatusCode as int ){
                    case 403:
                        Logging( "Forbidden", 5 )
                        ProcessState( "Status", "Login failure" )
                        ProcessState( "LoginRetries", ( state.LoginRetries + 1 ) )
			            break
                    case 404:
                        Logging( "Not found", 5 )
                        ProcessState( "Status", "Login failure" )
                        ProcessState( "LoginRetries", ( state.LoginRetries + 1 ) )
			            break
                    case 408:
                        Logging( "Request Timeout", 5 )
                        ProcessState( "Status", "Login failure" )
                        ProcessState( "LoginRetries", ( state.LoginRetries + 1 ) )
			            break
                    case 499:
                        Logging( "Connection Closed - Likely due to MFA, please confirm Login using MFA method", 3 )
                        ProcessState( "Status", "MFA Login Required" )
                        //ProcessState( "LoginRetries", ( state.LoginRetries + 1 ) )
			            break
		            default:
			            Logging( "Error logging in to controller: ${ e }", 5 )
                        ProcessState( "Status", "Login failure" )
                        ProcessState( "LoginRetries", ( state.LoginRetries + 1 ) )
			            break
                }
            }
        }
    } else {
        Logging( "Too many login failures. Please confirm the local username & password and perform a manual login.", 5 )
        ProcessState( "Status", "Login failure, check account information and perform manual login." )
    }
}

// Command to attempt to get general Protect info
def GetProtectInfo(){
    def Attempt = "api/bootstrap"
    asynchttpGet( "ReceiveData", GenerateProtectParams( "${ Attempt }" ), [ Method: "Bootstrap" ] )
}

// Attempts to get a list of motion-based events
def GetMotionEvents( Number ){
    def Attempt = "api/events?allCameras=true&end&limit=${ Number }&orderDirection=DESC&start&types=motion&types=smartDetectZone&types=smartDetectLine&types=ring"
    Logging( "Attempting to get ${ Number } motion events", 4 )
    asynchttpGet( "ReceiveData", GenerateProtectParams( "${ Attempt }" ), [ Method: "GetMotionEvents", Number: "${ Number }" ] ) 
}

// Command to attempt to get a camera's snapshot
def GetSnapshot( String CameraID, String DNI = null ){
    if( DNI == null ){
        getChildDevices().each{
			if( CameraID == getChildDevice( it.deviceNetworkId ).ReturnState( "ID" ) ){
				DNI = it.deviceNetworkId
			}
		}
    }
    def Attempt = "api/cameras/${ CameraID }/snapshot"
    Logging( "Attempting to GetSnapshot for ${ CameraID }", 4 )
    asynchttpGet( "ReceiveImageData", GenerateProtectImageParams( "${ Attempt }" ), [ Method: "GetSnapshot", DNI: "${ DNI }", Camera: "${ CameraID }" ] )
}

// Receive image data from the controller
def ReceiveImageData( resp, data ){
    switch( resp.getStatus() ){
		case 200:
            ProcessState( "Status", "${ data.Method } successful." )
            switch( data.Method ){
                case "GetSnapshot":
                    PostEventToChild( "${ data.DNI }", "image", "<img width=\"25%\" height=\"25%\" src=\"data:image/jpeg;base64,${ resp.data }\">" )
                    //FileData = resp.data.getBytes()
                    //uploadHubFile( "Camera_${ data.Camera }_Snapshot.jpg", FileData )
                    //PostEventToChild( "${ data.DNI }", "Snapshot", "<img src=\"http://${ location.hub.localIP }/local/Camera_${ data.Camera }_Snapshot.jpg\">" )
                    break
            }
            break
        case 400: // Bad request
            ProcessState( "Status", "${ data.Method } Bad Request" )
            Logging( "Bad Request for ${ data.Method }", 5 )
            break
        case 401:
            ProcessState( "Status", "${ data.Method } Unauthorized, please Login again" )
            Logging( "Unauthorized for ${ data.Method } please Login again", 5 )
			break
        case 404:
            ProcessState( "Status", "${ data.Method } Page not found error" )
            Logging( "Page not found for ${ data.Method }", 5 )
			break
        case 408:
            ProcessState( "Status", "Request timeout for ${ data.Method }" )
            Logging( "Timeout for ${ data.Method } headers = ${ resp.getHeaders() }", 4 )
			break
		default:
            ProcessState( "Status", "Error ${ resp.status } connecting for ${ data.Method }" )
			Logging( "Error connecting to Unifi Controller: ${ resp.status } for ${ data.Method }", 5 )
			break
    }
}
                
// Receive general data from the controller
def ReceiveData( resp, data ){
    Logging( "${ resp.getStatus() } = Received ${ data }", 4 )
    switch( resp.getStatus() ){
		case 200:
            def Json = parseJson( resp.data )
            ProcessState( "Status", "${ data.Method } successful." )
            switch( data.Method ){
                case "Auth Key":
                    ProcessState( "Auth Key", "${ Json.accessKey }" )
                    break
                case "Bootstrap":
                    Logging( "Bootstrap = ${ resp.data }", 4 )
                    ProcessState( "Last Updated ID", Json.lastUpdateId )
                    Json.cameras.each(){
                        def DeviceType
                        switch( it.type ){
                            case "UVC G4 Doorbell Pro PoE": // G4 Doorbell Pro PoE
                            case "UVC G4 Doorbell Pro": // G4 Doorbell Pro
                            case "UVC G4 Doorbell": // G4 Doorbell
                                DeviceType = "Doorbell"
                                break
                            case "UVC G4 PTZ": // Camera G4 PTZ
                            case "UVC G5 PTZ": // Camera G5 PTZ
                                DeviceType = "PTZCamera"
                                break
                            case "UVC G4 Instant": // Camera G4 Instant
                            case "UVC G4 Flex": // Camera G4 Flex
                            case "UVC G3 Instant": // Camera G3 Instant
                            case "UVC G3 Flex": // Camera G3 Flex
                            case "UVC G4 PRO": // Camera G4 Pro
                            case "UVC AI Bullet": // Camera AI Bullet
                            case "UVC AI 360": // Camera AI 360
                            case "UVC G3 PRO": // Camera G3 Pro
                            case "UVC G4 BULLET": // Camera G4 Bullet
                            case "UVC G4 DOME": // Camera G4 Dome
                            case "UVC G3 BULLET": // Camera G3 Bullet
                            default:
                                DeviceType = "Camera"
                                break
                        }
                        ProcessData( "${ DeviceType } ${ it.mac }", it )
                    }
                    Json.liveviews.each(){ // Ignoring liveviews at this time because there is not much of anything that can be done with them
                        //Logging( "liveviews Unhandled: ${ it }", 4 )
                    }
                    if( Json.nvr.size() >= 1 ){
                        def DeviceType
                        switch( Json.nvr.type ){
                            case "UDM-PRO": // Unifi Dream Machine Pro
                            default:
                                DeviceType = "NVR"
                                break
                        }
                        ProcessData( "${ DeviceType } ${ Json.nvr.mac }", Json.nvr )
                    }
                    Json.viewers.each(){
                        def DeviceType
                        switch( it.type ){
                            default:
                                DeviceType = "Viewer"
                                break
                        }
                        ProcessData( "${ DeviceType } ${ it.mac }", it )
                    }
                    Json.displays.each(){
                        def DeviceType
                        switch( it.type ){
                            default:
                                DeviceType = "Display"
                                break
                        }
                        ProcessData( "${ DeviceType } ${ it.mac }", it )
                    }
                    Json.lights.each(){
                        def DeviceType
                        switch( it.type ){
                            case "UP FloodLight": // Floodlight
                            default:
                                DeviceType = "Light"
                                break
                        }
                        ProcessData( "${ DeviceType } ${ it.mac }", it )
                    }
                    Json.bridges.each(){
                        def DeviceType
                        switch( it.type ){
                            case "UFP-UAP-B": // Unifi Access Point - Covers multiple actual models apparently
                            default:
                                DeviceType = "Bridge"
                                break
                        }
                        ProcessData( "${ DeviceType } ${ it.mac }", it )
                    }
                    Json.sensors.each(){
                        def DeviceType
                        switch( it.type ){
                            default:
                                DeviceType = "Sensor"
                                break
                        }
                        ProcessData( "${ DeviceType } ${ it.mac }", it )
                    }
                    Json.doorlocks.each(){
                        def DeviceType
                        switch( it.type ){
                            default:
                                DeviceType = "Doorlock"
                                break
                        }
                        ProcessData( "${ DeviceType } ${ it.mac }", it )
                    }
                    break
                case "GetChildStatus":
                    Logging( "GetChildStatus for ${ data.DNI }: ${ resp.data }", 4 )
                    ProcessData( "${ data.DNI  }", Json )
                    break
                case "GetBridgeStatus":
                    Logging( "GetBridgeStatus for ${ data.DNI }: ${ resp.data }", 4 )
                    ProcessData( "${ data.DNI  }", Json )
                    break
                case "GetCameraStatus":
                    Logging( "GetCameraStatus for ${ data.DNI }: ${ resp.data }", 4 )
                    ProcessData( "${ data.DNI  }", Json )
                    break
                case "GetDoorbellStatus":
                    Logging( "GetDoorbellStatus for ${ data.DNI }: ${ resp.data }", 4 )
                    ProcessData( "${ data.DNI  }", Json )
                    break
                case "GetLightStatus":
                    Logging( "GetLightStatus for ${ data.DNI }: ${ resp.data }", 4 )
                    ProcessData( "${ data.DNI  }", Json )
                    break
                case "GetSensorStatus":
                    Logging( "GetSensorStatus for ${ data.DNI }: ${ resp.data }", 4 )
                    ProcessData( "${ data.DNI  }", Json )
                    break
                case "GetMotionEvents":
                    Logging( "GetMotionEvents: ${ resp.data }", 4 )
                    break
                case "SendBridgeSettings":
                    Logging( "SendBridgeSettings for ${ data.DNI }: ${ resp.data }", 4 )
                    ProcessData( "${ data.DNI  }", Json )
                    break
                case "SendCameraSettings":
                    Logging( "SendCameraSettings for ${ data.DNI }: ${ resp.data }", 4 )
                    ProcessData( "${ data.DNI  }", Json )
                    break
                case "SendDoorbellSettings":
                    Logging( "SendDoorbellSettings for ${ data.DNI }: ${ resp.data }", 4 )
                    ProcessData( "${ data.DNI  }", Json )
                    break
                case "SendLightSettings":
                    Logging( "SendLightSettings for ${ data.DNI }: ${ resp.data }", 4 )
                    ProcessData( "${ data.DNI  }", Json )
                    break
                case "SendSensorSettings":
                    Logging( "SendSensorSettings for ${ data.DNI }: ${ resp.data }", 4 )
                    ProcessData( "${ data.DNI  }", Json )
                    break
                case "SendCameraSettings":
                    Logging( "SendLightSettings for ${ data.DNI }: ${ resp.data }", 4 )
                    ProcessData( "${ data.DNI  }", Json )
                    break
                case "SwitchLight":
                    Logging( "SwitchLight for ${ data.DNI }: ${ resp.data }", 4 )
                    ProcessData( "${ data.DNI  }", Json )
                    break
                case "LocateBridge":
                    Logging( "LocateBridge for ${ data.DNI }: ${ resp.data }", 4 )
                    ProcessData( "${ data.DNI  }", Json )
                    break
                case "LocateCamera":
                    Logging( "LocateCamera for ${ data.DNI }: ${ resp.data }", 4 )
                    ProcessData( "${ data.DNI  }", Json )
                    break
                case "LocateDoorbell":
                    Logging( "LocateDoorbell for ${ data.DNI }: ${ resp.data }", 4 )
                    ProcessData( "${ data.DNI  }", Json )
                    break
                case "LocateLight":
                    Logging( "LocateLight for ${ data.DNI }: ${ resp.data }", 4 )
                    ProcessData( "${ data.DNI  }", Json )
                    break
                case "LocateSensor":
                    Logging( "LocateSensor for ${ data.DNI }: ${ resp.data }", 4 )
                    ProcessData( "${ data.DNI  }", Json )
                    break
                case "LightBrightness":
                    Logging( "LightBrightness for ${ data.DNI }: ${ resp.data }", 4 )
                    ProcessData( "${ data.DNI  }", Json )
                    break
                case "MoveCameraXY":
                    Logging( "MoveCameraXY for ${ data.DNI }: ${ resp.data }", 4 )
                    break
                case "ZoomCamera":
                    Logging( "ZoomCamera for ${ data.DNI }: ${ resp.data }", 4 )
                    break
                case "StopCamera":
                    Logging( "StopCamera for ${ data.DNI }: ${ resp.data }", 4 )
                    break
                case "GoToPreset":
                    Logging( "GoToPreset for ${ data.DNI }: ${ resp.data }", 4 )
                    break
                default:
                    Logging( "${ data.Method } Unhandled: ${ resp.data }", 3 )
                    break
            }
            break
        case 400: // Bad request
            ProcessState( "Status", "${ data.Method } Bad Request, maybe Login again or method is wrong..." )
            Logging( "Bad Request for ${ data.Method } maybe Login again or method is wrong...", 5 )
            break
        case 401: // Unauthorized
            if( state.LoginRetries < 5 ){
                ProcessState( "Status", "${ data.Method } Unauthorized, attempting login and retry..." )
                Logging( "Unauthorized for ${ data.Method }, attempting login and retry...", 5 )
                ProcessState( "LoginRetries", ( state.LoginRetries + 1 ) )
                Login( false )
                switch( data.Method ){
                    case "Auth Key":
                        
                        break
                    case "Bootstrap":
                        GetProtectInfo()
                        break
                    case "GetChildStatus":

                        break
                    case "GetBridgeStatus":

                        break
                    case "GetCameraStatus":

                        break
                    case "GetDoorbellStatus":

                        break
                    case "GetLightStatus":

                        break
                    case "GetSensorStatus":

                        break
                    case "GetMotionEvents":
                        GetMotionEvents()
                        break
                    case "SendBridgeSettings":
                        
                        break
                    case "SendCameraSettings":
                        
                        break
                    case "SendDoorbellSettings":
                        
                        break
                    case "SendLightSettings":
                        
                        break
                    case "SendSensorSettings":
                        
                        break
                    case "SendCameraSettings":
                        
                        break
                    case "SwitchLight":

                        break
                    case "LocateBridge":

                        break
                    case "LocateCamera":

                        break
                    case "LocateDoorbell":

                        break
                    case "LocateLight":

                        break
                    case "LocateSensor":

                        break
                    case "LightBrightness":

                        break
                    default:
                        Logging( "${ data.Method } Unhandled: ${ resp.data }", 3 )
                        break
                }
            } else {
                ProcessState( "Status", "${ data.Method } Unauthorized, too many retry failures, try again manually." )
                Logging( "Unauthorized for ${ data.Method }, too many retry failures, try again manually.", 5 )
            }
			break
        case 404:
            ProcessState( "Status", "${ data.Method } Page not found error" )
            Logging( "Page not found for ${ data.Method }", 5 )
			break
        case 408:
            ProcessState( "Status", "Request timeout for ${ data.Method }" )
            Logging( "Timeout for ${ data.Method } headers = ${ resp.getHeaders() }", 4 )
			break
		default:
            ProcessState( "Status", "Error ${ resp.status } connecting for ${ data.Method }" )
			Logging( "Error connecting to Unifi Controller: ${ resp.status } for ${ data.Method }", 5 )
			break
	}
}

// Attempt to identify/locate a device
def LocateLight( String DNI, String ChildID ){
    def Attempt = "api/lights/${ ChildID }/locate"
    asynchttpPost( "ReceiveData", GenerateProtectParams( "${ Attempt }" ), [ Method: "LocateLight", DNI: "${ DNI }", ChildID: "${ ChildID }" ] )
}

// Attempt to identify/locate a device
def LocateCamera( String DNI, String ChildID ){
    def Attempt = "api/cameras/${ ChildID }/locate"
    asynchttpPost( "ReceiveData", GenerateProtectParams( "${ Attempt }" ), [ Method: "LocateCamera", DNI: "${ DNI }", ChildID: "${ ChildID }" ] )
}

// Attempt to identify/locate a device
def LocateDoorbell( String DNI, String ChildID ){
    def Attempt = "api/doorbells/${ ChildID }/locate"
    asynchttpPost( "ReceiveData", GenerateProtectParams( "${ Attempt }" ), [ Method: "LocateDoorbell", DNI: "${ DNI }", ChildID: "${ ChildID }" ] )
}

// Attempt to identify/locate a device
def LocateBridge( String DNI, String ChildID ){
    def Attempt = "api/bridges/${ ChildID }/locate"
    asynchttpPost( "ReceiveData", GenerateProtectParams( "${ Attempt }" ), [ Method: "LocateBridge", DNI: "${ DNI }", ChildID: "${ ChildID }" ] )
}

// Attempt to identify/locate a device
def LocateSensor( String DNI, String ChildID ){
    def Attempt = "api/sensors/${ ChildID }/locate"
    asynchttpPost( "ReceiveData", GenerateProtectParams( "${ Attempt }" ), [ Method: "LocateSensor", DNI: "${ DNI }", ChildID: "${ ChildID }" ] )
}

// Attempt to stop a PTZ camera from moving
def StopCamera( String DNI, String ChildID ){
    def Attempt = "api/cameras/${ ChildID }/move"
    def StopString = "{\"type\":\"continuous\",\"payload\":{\"x\":0,\"y\":0,\"z\":0}}"
    asynchttpPost( "ReceiveData", GenerateProtectPTZCameraParams( "${ Attempt }", "${ StopString }" ), [ Method: "StopCamera", DNI: "${ DNI }", ChildID: "${ ChildID }" ] )
}

// Attempt to move a PTZ camera
def MoveCameraXY( String DNI, String ChildID, X = 0, Y = 0 ){
    def Attempt = "api/cameras/${ ChildID }/move"
    def TempX = 0
    def TempY = 0
    def Delay = 0
    if( X > 0 ){
        TempX = 750
        Delay = X
    } else if( X < 0 ){
        TempX = -750
        Delay = ( X * -1 )
    }
    if( Y > 0 ){
        TempY = 750
        Delay = Y
    } else if( Y < 0 ){
        TempY = -750
        Delay = ( Y * -1 )
    }
    def MoveString = "{\"type\":\"continuous\",\"payload\":{\"x\":${ TempX as int },\"y\":${ TempY as int }}}"
    Logging( "MoveCameraXY ${ MoveString }", 4 )
    asynchttpPost( "ReceiveData", GenerateProtectPTZCameraParams( "${ Attempt }", "${ MoveString }" ), [ Method: "MoveCameraXY", DNI: "${ DNI }", ChildID: "${ ChildID }", X: ( TempX as int ), Y: ( TempY as int ) ] )
    runInMillis( Delay as int, "StopCamera", [ data: [ DNI: "${ DNI }", ChildID: "${ ChildID }" ] ] )
}

// Attempt to zoom in/out a PTZ camera
def ZoomCamera( String DNI, String ChildID, Z = 0 ){
    def Attempt = "api/cameras/${ ChildID }/move"
    def TempZ = 0
    def Delay = 0
    if( Z > 0 ){
        TempZ = 750
        Delay = Z
    } else if( Z < 0 ){
        TempZ = -750
        Delay = ( Z * -1 )
    }
    def ZoomString = "{\"type\":\"continuous\",\"payload\":{\"x\":0,\"y\":0,\"z\":${ TempZ as int }}}"
    Logging( "ZoomCamera ${ ZoomString }", 4 )
    asynchttpPost( "ReceiveData", GenerateProtectPTZCameraParams( "${ Attempt }", "${ ZoomString }" ), [ Method: "ZoomCamera", DNI: "${ DNI }", ChildID: "${ ChildID }", Z: ( TempZ as int ) ] )
    runInMillis( Delay as int, "StopCamera", [ data: [ DNI: "${ DNI }", ChildID: "${ ChildID }" ] ] )
}

// Attempt to send camera to it's home position
def HomeCamera( String DNI, String ChildID ){
    GoToPreset( DNI, ChildID, "0" )
}

// Attempt to send camera to a preset position, defaults to Home position
def GoToPreset( String DNI, String ChildID, String PresetInput = "0" ){
    def Preset = ( ( PresetInput as int ) - 1 ) // Presets are handled in typical array fashion starting at 0 plus home position is at -1
    def Attempt = "api/cameras/${ ChildID }/ptz/goto/${ Preset }"
    asynchttpPost( "ReceiveData", GenerateProtectPTZCameraParams( "${ Attempt }" ), [ Method: "GoToPreset", DNI: "${ DNI }", ChildID: "${ ChildID }", Preset: "${ Preset }" ] )
}

// Configure device settings based on Preferences
def SendBridgeSettings( String DNI, String ChildID, String Value ){
    def Attempt = "api/bridges/"
    asynchttpPatch( "ReceiveData", GenerateProtectManageParams( "${ Attempt }", "${ ChildID }", "${ Value }" ), [ Method: "SendBridgeSettings", DNI: "${ DNI }", ChildID: "${ ChildID }", Value: "${ Value }" ] )
}

// Configure device settings based on Preferences
def SendLightSettings( String DNI, String ChildID, String Value ){
    def Attempt = "api/lights/"
    asynchttpPatch( "ReceiveData", GenerateProtectManageParams( "${ Attempt }", "${ ChildID }", "${ Value }" ), [ Method: "SendLightSettings", DNI: "${ DNI }", ChildID: "${ ChildID }", Value: "${ Value }" ] )
}

// Configure device settings based on Preferences
def SendSensorSettings( String DNI, String ChildID, String Value ){
    def Attempt = "api/sensors/"
    asynchttpPatch( "ReceiveData", GenerateProtectManageParams( "${ Attempt }", "${ ChildID }", "${ Value }" ), [ Method: "SendSensorSettings", DNI: "${ DNI }", ChildID: "${ ChildID }", Value: "${ Value }" ] )
}

// Configure device settings based on Preferences
def SendCameraSettings( String DNI, String ChildID, String Value ){
    def Attempt = "api/cameras/"
    asynchttpPatch( "ReceiveData", GenerateProtectManageParams( "${ Attempt }", "${ ChildID }", "${ Value }" ), [ Method: "SendCameraSettings", DNI: "${ DNI }", ChildID: "${ ChildID }", Value: "${ Value }" ] )
}

// Configure device settings based on Preferences
def SendDoorbellSettings( String DNI, String ChildID, String Value ){
    def Attempt = "api/doorbells/"
    asynchttpPatch( "ReceiveData", GenerateProtectManageParams( "${ Attempt }", "${ ChildID }", "${ Value }" ), [ Method: "SendDoorbellSettings", DNI: "${ DNI }", ChildID: "${ ChildID }", Value: "${ Value }" ] )
}

// Handle switching light on or off
def SwitchLight( String DNI, String ChildID, String Value ){
    def Attempt = "api/lights/"
    if( Value == "on" ){
        asynchttpPatch( "ReceiveData", GenerateProtectCommandParams( "${ Attempt }", "${ ChildID }", "{\"lightOnSettings\":{\"isLedForceOn\":true}}" ), [ Method: "SwitchLight", DNI: "${ DNI }", ChildID: "${ ChildID }", Value: "${ Value }" ] )
    } else {
        asynchttpPatch( "ReceiveData", GenerateProtectCommandParams( "${ Attempt }", "${ ChildID }", "{\"lightOnSettings\":{\"isLedForceOn\":false}}" ), [ Method: "SwitchLight", DNI: "${ DNI }", ChildID: "${ ChildID }", Value: "${ Value }" ] )
    }
}

// Handle switching light level
def LightBrightness( String DNI, String ChildID, Value ){
    def Attempt = "api/lights/"
    asynchttpPatch( "ReceiveData", GenerateProtectCommandParams( "${ Attempt }", "${ ChildID }", "{\"lightDeviceSettings\":{\"ledLevel\":${ Value }}}" ), [ Method: "LightBrightness", DNI: "${ DNI }", ChildID: "${ ChildID }", Value: "${ Value }" ] )
}

// Get a specific child's status
def GetBridgeStatus( String DNI, String ChildID ){
    def Attempt = "api/bridges/${ ChildID }"
    asynchttpGet( "ReceiveData", GenerateProtectParams( "${ Attempt }" ), [ Method: "GetBridgeStatus", DNI: "${ DNI }", ChildID: "${ ChildID }" ] )
}

// Get a specific child's status
def GetCameraStatus( String DNI, String ChildID ){
    def Attempt = "api/cameras/${ ChildID }"
    asynchttpGet( "ReceiveData", GenerateProtectParams( "${ Attempt }" ), [ Method: "GetCameraStatus", DNI: "${ DNI }", ChildID: "${ ChildID }" ] )
}

// Get a specific child's status
def GetDoorbellStatus( String DNI, String ChildID ){
    def Attempt = "api/doorbells/${ ChildID }"
    asynchttpGet( "ReceiveData", GenerateProtectParams( "${ Attempt }" ), [ Method: "GetDoorbellStatus", DNI: "${ DNI }", ChildID: "${ ChildID }" ] )
}

// Get a specific child's status
def GetLightStatus( String DNI, String ChildID ){
    def Attempt = "api/lights/${ ChildID }"
    asynchttpGet( "ReceiveData", GenerateProtectParams( "${ Attempt }" ), [ Method: "GetLightStatus", DNI: "${ DNI }", ChildID: "${ ChildID }" ] )
}

// Get a specific child's status
def GetSensorStatus( String DNI, String ChildID ){
    def Attempt = "api/sensors/${ ChildID }"
    asynchttpGet( "ReceiveData", GenerateProtectParams( "${ Attempt }" ), [ Method: "GetSensorStatus", DNI: "${ DNI }", ChildID: "${ ChildID }" ] )
}

// Power down the controller
def PowerDown( String Confirmation ){
    if( Confirmation == "PowerDown" ){
        def Params
        if( Controller == "Unifi Dream Machine (inc Pro)" ){
            Params = [ uri: "https://${ UnifiURL }:443/api/system/poweroff", ignoreSSLIssues: true, headers: [ Referer: "https://${ UnifiURL }/settings/advanced", Host: "${ UnifiURL }", Origin: "https://${ UnifiURL }", Accept: "*/*", Cookie: "${ state.Cookie }", "X-CSRF-Token": "${ state.CSRF }" ] ]
        } else {
            Params = [ uri: "https://${ UnifiURL }:${ ControllerPort }/api/system/poweroff", ignoreSSLIssues: true, headers: [ Referer: "https://${ UnifiURL }/settings/advanced", Host: "${ UnifiURL }", Origin: "https://${ UnifiURL }", Accept: "*/*", Cookie: "${ state.Cookie }", "X-CSRF-Token": "${ state.CSRF }" ] ]
        }
        Logging( "PowerDown Params = ${ Params }", 4 )
        try{
            httpPost( Params ){ resp ->
                switch( resp.getStatus() ){
                    case 200:
                    case 204:
                        ProcessEvent( "Status", "PowerDown command sent" )
                        Logging( "PowerDown command sent = ${ resp.data }", 4 )
                        break
                    default:
                        Logging( "PowerDown command error ${ resp.getStatus() }", 3 )
                        break
                }
            }
        } catch( Exception e ){
            Logging( "PowerDown failed due to ${ e }", 5 )
        }
    } else {
        Logging( "PowerDown confirmation incorrect. PowerDown command ignored.", 5 )
        ProcessEvent( "Status", "PowerDown confirmation incorrect. PowerDown command ignored." )
    }
}

// Generate Protect Params assembles the parameters to be sent to the controller rather than repeat so much of it
def GenerateProtectParams( String Path, String Data = null ){
	def Params
	if( Controller == "Unifi Dream Machine (inc Pro)" ){
        if( Data != null ){
            Params = [ uri: "https://${ UnifiURL }/proxy/protect/${ Path }", ignoreSSLIssues: true, requestContentType: "application/json", contentType: "application/json", headers: [ Host: "${ UnifiURL }", Accept: "*/*", Cookie: "${ state.Cookie }", 'X-CSRF-Token': "${ state.CSRF }", 'accessKey': "${ state.'Auth Key' }" ], data:"${ Data }" ]
        } else {
            Params = [ uri: "https://${ UnifiURL }/proxy/protect/${ Path }", ignoreSSLIssues: true, requestContentType: "application/json", contentType: "application/json", headers: [ Host: "${ UnifiURL }", Accept: "*/*", Cookie: "${ state.Cookie }", 'X-CSRF-Token': "${ state.CSRF }", 'accessKey': "${ state.'Auth Key' }" ] ]
        }
	} else {
        if( Data != null ){
            Params = [ uri: "https://${ UnifiURL }:${ ControllerPort }/${ Path }", ignoreSSLIssues: true, requestContentType: "application/json", contentType: "application/json", headers: [ Host: "${ UnifiURL }", Accept: "*/*", Cookie: "${ state.Cookie }", 'X-CSRF-Token': "${ state.CSRF }", 'accessKey': "${ state.'Auth Key' }" ], data:"${ Data }" ]
        } else {
            Params = [ uri: "https://${ UnifiURL }:${ ControllerPort }/${ Path }", ignoreSSLIssues: true, requestContentType: "application/json", contentType: "application/json", headers: [ Host: "${ UnifiURL }", Accept: "*/*", Cookie: "${ state.Cookie }", 'X-CSRF-Token': "${ state.CSRF }", 'accessKey': "${ state.'Auth Key' }" ] ]
        }
	}
    Logging( "Parameters = ${ Params }", 4 )
	return Params
}

// Generate Protect PTZ Camera Params assembles the parameters to be sent to the controller rather than repeat so much of it
def GenerateProtectPTZCameraParams( String Path, String Data = null ){
	def Params
	if( Controller == "Unifi Dream Machine (inc Pro)" ){
        if( Data != null ){
            Params = [ uri: "https://${ UnifiURL }/proxy/protect/${ Path }", ignoreSSLIssues: true, requestContentType: "application/json", contentType: "application/json", headers: [ Host: "${ UnifiURL }", Accept: "*/*", Cookie: "${ state.Cookie }", Origin: "https://${ UnifiURL }", 'X-Csrf-Token': "${ state.CSRF }" ], body:"${ Data }" ]
        } else {
            Params = [ uri: "https://${ UnifiURL }/proxy/protect/${ Path }", ignoreSSLIssues: true, requestContentType: "application/json", contentType: "application/json", headers: [ Host: "${ UnifiURL }", Accept: "*/*", Cookie: "${ state.Cookie }", Origin: "https://${ UnifiURL }", 'X-Csrf-Token': "${ state.CSRF }" ] ]
        }
	} else {
        if( Data != null ){
            Params = [ uri: "https://${ UnifiURL }:${ ControllerPort }/${ Path }", ignoreSSLIssues: true, requestContentType: "application/json", contentType: "application/json", headers: [ Host: "${ UnifiURL }", Accept: "*/*", Cookie: "${ state.Cookie }", Origin: "https://${ UnifiURL }", 'X-Csrf-Token': "${ state.CSRF }" ], body:"${ Data }" ]
        } else {
            Params = [ uri: "https://${ UnifiURL }:${ ControllerPort }/${ Path }", ignoreSSLIssues: true, requestContentType: "application/json", contentType: "application/json", headers: [ Host: "${ UnifiURL }", Accept: "*/*", Cookie: "${ state.Cookie }", Origin: "https://${ UnifiURL }", 'X-Csrf-Token': "${ state.CSRF }" ] ]
        }
	}
    Logging( "Parameters = ${ Params }", 4 )
	return Params
}

// Generate Protect Image Params assembles the parameters to be sent to the controller rather than repeat so much of it
def GenerateProtectImageParams( String Path, String Data = null ){
	def Params
	if( Controller == "Unifi Dream Machine (inc Pro)" ){
        if( Data != null ){
            Params = [ uri: "https://${ UnifiURL }/proxy/protect/${ Path }", ignoreSSLIssues: true, headers: [ Host: "${ UnifiURL }", Accept: "*/*", Cookie: "${ state.Cookie }", 'X-CSRF-Token': "${ state.CSRF }", 'accessKey': "${ state.'Auth Key' }" ], data:"${ Data }" ]
        } else {
            Params = [ uri: "https://${ UnifiURL }/proxy/protect/${ Path }", ignoreSSLIssues: true, headers: [ Host: "${ UnifiURL }", Accept: "*/*", Cookie: "${ state.Cookie }", 'X-CSRF-Token': "${ state.CSRF }", 'accessKey': "${ state.'Auth Key' }" ] ]
        }
	} else {
        if( Data != null ){
            Params = [ uri: "https://${ UnifiURL }:${ ControllerPort }/${ Path }", ignoreSSLIssues: true, headers: [ Host: "${ UnifiURL }", Accept: "*/*", Cookie: "${ state.Cookie }", 'X-CSRF-Token': "${ state.CSRF }", 'accessKey': "${ state.'Auth Key' }" ], data:"${ Data }" ]
        } else {
            Params = [ uri: "https://${ UnifiURL }:${ ControllerPort }/${ Path }", ignoreSSLIssues: true, headers: [ Host: "${ UnifiURL }", Accept: "*/*", Cookie: "${ state.Cookie }", 'X-CSRF-Token': "${ state.CSRF }", 'accessKey': "${ state.'Auth Key' }" ] ]
        }
	}
    Logging( "Parameters = ${ Params }", 4 )
	return Params
}

// Generate Protect Params assembles the parameters to be sent to the controller rather than repeat so much of it
def GenerateProtectCommandParams( String Path, String ChildID, String Data = null ){
	def Params
	if( Controller == "Unifi Dream Machine (inc Pro)" ){
        if( Data != null ){
            Params = [ uri: "https://${ UnifiURL }/proxy/protect/${ Path }/${ ChildID }", ignoreSSLIssues: true, requestContentType: "application/json", contentType: "application/json", headers: [ Referer: "https://${ UnifiURL }/protect/devices/${ ChildID }/general", Origin: "https://${ UnifiURL }", Cookie: "${ state.Cookie }", 'X-CSRF-Token': "${ state.CSRF }" ], body:"${ Data }" ]
        } else {
            Params = [ uri: "https://${ UnifiURL }/proxy/protect/${ Path }/${ ChildID }", ignoreSSLIssues: true, requestContentType: "application/json", contentType: "application/json", headers: [ Referer: "https://${ UnifiURL }/protect/devices/${ ChildID }/general", Origin: "https://${ UnifiURL }", Cookie: "${ state.Cookie }", 'X-CSRF-Token': "${ state.CSRF }" ] ]
        }
	} else {
        if( Data != null ){
            Params = [ uri: "https://${ UnifiURL }:${ ControllerPort }/${ Path }/${ ChildID }", ignoreSSLIssues: true, requestContentType: "application/json", contentType: "application/json", headers: [ Host: "${ UnifiURL }:${ ControllerPort }", Accept: "*/*", Cookie: "${ state.Cookie }", 'X-CSRF-Token': "${ state.CSRF }", 'accessKey': "${ state.'Auth Key' }" ], body:"${ Data }" ]
        } else {
            Params = [ uri: "https://${ UnifiURL }:${ ControllerPort }/${ Path }/${ ChildID }", ignoreSSLIssues: true, requestContentType: "application/json", contentType: "application/json", headers: [ Host: "${ UnifiURL }:${ ControllerPort }", Accept: "*/*", Cookie: "${ state.Cookie }", 'X-CSRF-Token': "${ state.CSRF }", 'accessKey': "${ state.'Auth Key' }" ] ]
        }
	}
    Logging( "Parameters = ${ Params }", 4 )
	return Params
}

// Generate Protect Params assembles the parameters to be sent to the controller rather than repeat so much of it
def GenerateProtectManageParams( String Path, String ChildID, String Data ){
	def Params
	if( Controller == "Unifi Dream Machine (inc Pro)" ){
        if( Data != null ){
            Params = [ uri: "https://${ UnifiURL }/proxy/protect/${ Path }/${ ChildID }", ignoreSSLIssues: true, requestContentType: "application/json", contentType: "application/json", headers: [ Referer: "https://${ UnifiURL }/protect/devices/${ ChildID }/manage", Origin: "https://${ UnifiURL }", Cookie: "${ state.Cookie }", 'X-CSRF-Token': "${ state.CSRF }" ], body:"${ Data }" ]
        } else {
            Params = [ uri: "https://${ UnifiURL }/proxy/protect/${ Path }/${ ChildID }", ignoreSSLIssues: true, requestContentType: "application/json", contentType: "application/json", headers: [ Referer: "https://${ UnifiURL }/protect/devices/${ ChildID }/manage", Origin: "https://${ UnifiURL }", Cookie: "${ state.Cookie }", 'X-CSRF-Token': "${ state.CSRF }" ] ]
        }
	}
    Logging( "Parameters = ${ Params }", 4 )
	return Params
}

// Process data coming in for a device
def ProcessData( String Device, data ){
    //Logging( "${ Device } Data: ${ data }", 4 )
    data.each(){
        if( it.key != null ){
            switch( it.key ){
                case "mac":
					PostStateToChild( "${ Device }", "MAC", it.value )
					break
                case "type":
					PostStateToChild( "${ Device }", "Type", it.value )
					break
                case "id":
                    PostStateToChild( "${ Device }", "ID", it.value )
                    break
                case "version":
                    PostStateToChild( "${ Device }", "Protect Version", it.value )
                    break
                case "name":
                    if( it.value != null ){
                        if( getChildDevice( "${ Device }" ).label == null ){
                            getChildDevice( "${ Device }" ).label = it.value
                        }
                        PostStateToChild( "${ Device }", "DeviceName", it.value )
                    }
                    break
                case "model":
                    PostStateToChild( "${ Device }", "Model", it.value )
                    break
                case "isMotionDetected":
                case "isPirMotionDetected":
                    if( it.value ){
                        PostEventToChild( "${ Device }", "motion", "active" )
                    } else {
                        PostEventToChild( "${ Device }", "motion", "inactive" )
                    }
                    break
                case "isLightOn":
                    if( it.value ){
                        PostEventToChild( "${ Device }", "switch", "on" )
                    } else {
                        PostEventToChild( "${ Device }", "switch", "off" )
                    }
                    break
                case "isDark":
					if( it.value ){
						PostEventToChild( "${ Device }", "Dark", "true" )
					} else {
						PostEventToChild( "${ Device }", "Dark", "false" )
					}
					break
				case "camera":
					if( it.value != null ){
						PostStateToChild( "${ Device }", "Camera Paired", it.value )
					} else {
						PostStateToChild( "${ Device }", "Camera Paired", "None" )
					}
					break
				case "lightDeviceSettings":
					it.value.each(){
						switch( it.key ){
							case "ledLevel":
								PostEventToChild( "${ Device }", "Brightness", it.value )
								switch( it.value ){
									case 1:
										PostEventToChild( "${ Device }", "level", 10, "%" )
										break
									case 2:
										PostEventToChild( "${ Device }", "level", 20, "%" )
										break
									case 3:
										PostEventToChild( "${ Device }", "level", 40, "%" )
										break
									case 4:
										PostEventToChild( "${ Device }", "level", 60, "%" )
										break
									case 5:
										PostEventToChild( "${ Device }", "level", 80, "%" )
										break
									case 6:
										PostEventToChild( "${ Device }", "level", 100, "%" )
										break
								}
								break
							case "pirSensitivity":
								PostStateToChild( "${ Device }", "Motion Sensitivity", it.value )
								break
							case "pirDuration":
								PostStateToChild( "${ Device }", "Motion Duration", ( it.value / 1000 ) )
								break
							case "luxSensitivity":
								PostStateToChild( "${ Device }", "Lux Sensitivity", it.value )
								break
							case "isIndicatorEnabled":
								PostStateToChild( "${ Device }", "Indicator Enabled", it.value )
								break
							default:
								Logging( "Unhandled lightDeviceSettings for light ${ it.key } = ${ it.value }", 3 )
								break
						}
					}
					break
				case "lightModeSettings":
					it.value.each(){
						switch( it.key ){
							case "mode":
								PostStateToChild( "${ Device }", "Light Trigger", it.value )
								break
							case "enableAt":
								PostStateToChild( "${ Device }", "Trigger At", it.value )
								break
							default:
								Logging( "Unhandled lightModeSetting for light ${ it.key } = ${ it.value }", 3 )
								break
						}
					}
					break
				case "state":
					PostEventToChild( "${ Device }", "Device Status", it.value )
					break
				case "isConnected":
					if( it.value ){
						PostStateToChild( "${ Device }", "presence", "present" )
					} else {
						PostStateToChild( "${ Device }", "presence", "not present" )
					}
					break
				case "latestFirmwareVersion":
					PostStateToChild( "${ Device }", "Latest Firmware Version", it.value )
					break
                case "firmwareVersion":
					PostStateToChild( "${ Device }", "Firmware Version", it.value )
					break
                case "hardwareRevision":
					PostStateToChild( "${ Device }", "Hardware Revision", it.value )
					break
				case "uptime":
                    if( it.value != null ){
					    def TempUptime = it.value as int
                        if( TempUptime >= 99999999 ){
                            TempUptime = ( ( TempUptime / 1000 ) as int )
                        }
					    def TempUptimeDays = Math.round( TempUptime / 86400 )
					    def TempUptimeHours = Math.round( ( TempUptime % 86400 ) / 3600 )
					    def TempUptimeMinutes = Math.round( ( TempUptime % 3600 ) / 60 )
					    def TempUptimeString = "${ TempUptimeDays } Day"
					    if( TempUptimeDays != 1 ){
					    	TempUptimeString += "s"
					    }
					    TempUptimeString += " ${ TempUptimeHours } Hour"
					    if( TempUptimeHours != 1 ){
					    	TempUptimeString += "s"
					    }
					    TempUptimeString += " ${ TempUptimeMinutes } Minute"
					    if( TempUptimeMinutes != 1 ){
					    	TempUptimeString += "s"
					    }
                        PostStateToChild( "${ Device }", "Uptime", TempUptimeString )
                    }
					break
				case "platform":
					PostStateToChild( "${ Device }", "Platform", it.value )
					break
                case "systemInfo":
                    it.value.each(){
						switch( it.key ){
							case "cpu":
								it.value.each(){
						            switch( it.key ){
                                        case "temperature":
                                            PostEventToChild( "${ Device }", "CPU Temp", ConvertTemperature( "C", it.value ), "°${ location.getTemperatureScale() }" )
                                            break
                                        case "averageLoad":
                                            PostEventToChild( "${ Device }", "CPU Average Load", it.value, "%" )
                                            break
                                    }
                                }
                                break
							case "memory":
                                PostEventToChild( "${ Device }", "Memory Available", ( Math.round( ( it.value.available / it.value.total ) * 10000 ) / 100 ), "%" )
                                break
                            case "storage":
                                PostEventToChild( "${ Device }", "Storage Available", ( Math.round( ( it.value.available / it.value.size ) * 10000 ) / 100 ), "%" )
                                break
                        }
                    }
                    break
                case "batteryStatus":
					PostEventToChild( "${ Device }", "battery", it.value.percentage, "%" )
					break
                case "stats":
                    it.value.each{
                        switch( it.key ){
                            case "light":
                                PostEventToChild( "${ Device }", "illuminance", it.value.value )
                                PostEventToChild( "${ Device }", "LightAlert", it.value.status )
                                break
                            case "temperature":
                                PostEventToChild( "${ Device }", "temperature", ConvertTemperature( "C", it.value.value ), "°${ location.getTemperatureScale() }" )
                                PostEventToChild( "${ Device }", "TemperatureAlert", it.value.status )
                                break
                            case "humidity":
                                PostEventToChild( "${ Device }", "humidity", it.value.value )
                                PostEventToChild( "${ Device }", "HumidityAlert", it.value.status )
                                break
                        }
                    }
                    break
                case "bluetoothConnectionState":
                    it.value.each{
                        switch( it.key ){
                            case "signalQuality":
                                PostEventToChild( "${ Device }", "lqi", it.value )
                                break
                            case "signalStrength":
                                PostEventToChild( "${ Device }", "rssi", it.value )
                                break
                        }
                    }
                    break
                case "alarmTriggeredAt":
                    PostStateToChild( "${ Device }", "Alarm_Triggered", ConvertEpochToDate( "${ it.value }" ) )
                    break
                case "openStatusChangedAt":
                    PostStateToChild( "${ Device }", "Open_Status_Changed", ConvertEpochToDate( "${ it.value }" ) )
                    break
                case "tamperingDetectedAt":
                    PostStateToChild( "${ Device }", "Tampering_Detected", ConvertEpochToDate( "${ it.value }" ) )
                    break
                case "leakDetectedAt":
                    PostStateToChild( "${ Device }", "Leak_Detected", ConvertEpochToDate( "${ it.value }" ) )
                    break
                case "motionDetectedAt":
                    PostStateToChild( "${ Device }", "Motion_Detected", ConvertEpochToDate( "${ it.value }" ) )
                    break
                case "lastDisconnect":
                    PostStateToChild( "${ Device }", "Last_Disconnect", ConvertEpochToDate( "${ it.value }" ) )
                    break
                case "fwUpdateState":
                    switch( it.value ){
                        case "upToDate":
                            PostStateToChild( "${ Device }", "FirmwareUpdateState", "Up To Date" )
                            break
                        default:
                            PostStateToChild( "${ Device }", "FirmwareUpdateState", it.value )
                            break
                    }
                    break
                case "alarmSettings":
                    it.value.each{
                        switch( it.key ){
                            case "isEnabled":
                                if( it.value ){
                                    PostEventToChild( "${ Device }", "Alarm_Enabled", "true" )
                                } else {
                                    PostEventToChild( "${ Device }", "Alarm_Enabled", "false" )
                                }
                                break
                            default:
                                PostStateToChild( "${ Device }", it.key, it.value )
                                break
                        }
                    }
                    break
                case "motionSettings":
                    it.value.each{
                        switch( it.key ){
                            case "isEnabled":
                                if( it.value ){
                                    PostEventToChild( "${ Device }", "Motion_Enabled", "true" )
                                } else {
                                    PostEventToChild( "${ Device }", "Motion_Enabled", "false" )
                                }
                                break
                            case "sensitivity":
                                PostEventToChild( "${ Device }", "Motion_Sensitivity", it.value )
                                break
                            default:
                                PostStateToChild( "${ Device }", it.key, it.value )
                                break
                        }
                    }
                    break
                case "temperatureSettings":
                    it.value.each{
                        switch( it.key ){
                            case "isEnabled":
                                if( it.value ){
                                    PostEventToChild( "${ Device }", "Temperature_Enabled", "true" )
                                } else {
                                    PostEventToChild( "${ Device }", "Temperature_Enabled", "false" )
                                }
                                break
                            case "margin":
                                PostEventToChild( "${ Device }", "Temperature_Margin", it.value )
                                break
                            case "lowThreshold":
                                PostEventToChild( "${ Device }", "Temperature_Low_Threshold", it.value )
                                break
                            case "highThreshold":
                                PostEventToChild( "${ Device }", "Temperature_High_Threshold", it.value )
                                break
                            default:
                                PostStateToChild( "${ Device }", it.key, it.value )
                                break
                        }
                    }
                    break
                case "humiditySettings":
                    it.value.each{
                        switch( it.key ){
                            case "isEnabled":
                                if( it.value ){
                                    PostEventToChild( "${ Device }", "Humidity_Enabled", "true" )
                                } else {
                                    PostEventToChild( "${ Device }", "Humidity_Enabled", "false" )
                                }
                                break
                            case "margin":
                                PostEventToChild( "${ Device }", "Humidity_Margin", it.value )
                                break
                            case "lowThreshold":
                                PostEventToChild( "${ Device }", "Humidity_Low_Threshold", it.value )
                                break
                            case "highThreshold":
                                PostEventToChild( "${ Device }", "Humidity_High_Threshold", it.value )
                                break
                            default:
                                PostStateToChild( "${ Device }", it.key, it.value )
                                break
                        }
                    }
                    break
                case "lightSettings":
                    it.value.each{
                        switch( it.key ){
                            case "isEnabled":
                                if( it.value ){
                                    PostEventToChild( "${ Device }", "Light_Enabled", "true" )
                                } else {
                                    PostEventToChild( "${ Device }", "Light_Enabled", "false" )
                                }
                                break
                            case "margin":
                                PostEventToChild( "${ Device }", "Light_Margin", it.value )
                                break
                            case "lowThreshold":
                                PostEventToChild( "${ Device }", "Light_Low_Threshold", it.value )
                                break
                            case "highThreshold":
                                PostEventToChild( "${ Device }", "Light_High_Threshold", it.value )
                                break
                            default:
                                PostStateToChild( "${ Device }", it.key, it.value )
                                break
                        }
                    }
                    break
                case "homekitSettings":
                    PostStateToChild( "${ Device }", "Homekit_Settings", it.value )
                    break
                case "isOpened":
                    if( it.value ){
                        PostEventToChild( "${ Device }", "contact", "open" )
                    } else {
                        PostEventToChild( "${ Device }", "contact", "closed" )
                    }
                    break
                case "bridge":
                    PostStateToChild( "${ Device }", "BridgeID", it.value )
                    break
                case "nvrMac":
                    PostStateToChild( "${ Device }", "NVR_MAC", it.value )
                    break
                case "publicIp":
                case "wanIp":
                    PostStateToChild( "${ Device }", "Public_IP", it.value )
                    break
                case "countryCode":
                    PostStateToChild( "${ Device }", "Country_Code", it.value )
                    break
                case "corruptionState":
                    switch( it.value ){
                        case "healthy":
                            PostStateToChild( "${ Device }", "Health_Status", "Healthy" )
                            break
                        default:
                            PostStateToChild( "${ Device }", "Health_Status", it.value )
                            break
                    }
                    break
                case "isWaterproofCaseAttached":
                    if( it.value ){
                        PostStateToChild( "${ Device }", "Waterproof_Case", "true" )
                    } else {
                        PostStateToChild( "${ Device }", "Waterproof_Case", "false" )
                    }
                    break
                case "is4K":
                    if( it.value ){
                        PostStateToChild( "${ Device }", "4K", "true" )
                    } else {
                        PostStateToChild( "${ Device }", "4K", "false" )
                    }
                    break
                case "is2K":
                    if( it.value ){
                        PostStateToChild( "${ Device }", "2K", "true" )
                    } else {
                        PostStateToChild( "${ Device }", "2K", "false" )
                    }
                    break
                case "mountType":
                    switch( it.value ){
                        case "door":
                            PostEventToChild( "${ Device }", "MountType", "Door" )
                            break
                        case "window":
                            PostEventToChild( "${ Device }", "MountType", "Window" )
                            break
                        case "garage":
                            PostEventToChild( "${ Device }", "MountType", "Garage" )
                            break
                        case "leak":
                            PostEventToChild( "${ Device }", "MountType", "Leak" )
                            break
                        case "none":
                        case "null":
                        default:
                            PostEventToChild( "${ Device }", "MountType", "None" )
                            break
                    }
                    break
                case "recordingSchedulesV2":
                    PostStateToChild( "${ Device }", "RecordingSchedules", it.value )
                    break
                case "hasRecordings":
                    PostStateToChild( "${ Device }", "HasRecordings", it.value )
                    break
                case "uplinkDevice":
                    PostStateToChild( "${ Device }", "UplinkDevice", it.value )
                    break
                case "deviceFirmwareSettings":
                    PostStateToChild( "${ Device }", "FirmwareSettings", it.value )
                    break
                case "cameraCapacity":
                    PostStateToChild( "${ Device }", "CameraCapacity", it.value )
                    break
                case "isNetworkInstalled":
                    PostStateToChild( "${ Device }", "IsNetworkInstalled", it.value )
                    break
                case "isPtz":
                    PostStateToChild( "${ Device }", "PanTiltZoom", it.value )
                    break
                case "displayName":
                    PostStateToChild( "${ Device }", "DisplayName", it.value )
                    break
                case "smartDetection":
                    PostStateToChild( "${ Device }", "SmartDetection", it.value )
                    break
                case "currentResolution":
                    PostStateToChild( "${ Device }", "CurrentResolution", it.value )
                    break
                case "alarms":
                    PostEventToChild( "${ Device }", "Alarms", it.value )
                    break
                case "shortcuts":
                    PostStateToChild( "${ Device }", "Shortcuts", it.value )
                    break
                case "tiltLimitsOfPrivacyZones":
                    PostStateToChild( "${ Device }", "TiltLimitsOfPrivacyZones", it.value )
                    break
                case "videoCodec":
                    PostStateToChild( "${ Device }", "VideoCodec", it.value )
                    break
                case "supportedScalingResolutions":
                    PostStateToChild( "${ Device }", "SupportedScalingResolutions", it.value )
                    break
                case "clients":
                    PostStateToChild( "${ Device }", "Clients", it.value )
                    break
                case "maxClients":
                    PostStateToChild( "${ Device }", "MaxClients", it.value )
                    break
                case "isAccessInstalled":
                    PostStateToChild( "${ Device }", "MaxClients", "${ it.value }" )
                    break
                // 3rd-party data
                case "isThirdPartyCamera":
                    PostEventToChild( "${ Device }", "ThirdPartyCamera", "${ it.value }" )
                    break
                case "thirdPartyCameraInfo":
                    PostEventToChild( "${ Device }", "ThirdPartyCameraInfo", "${ it.value }" )
                    break
                case "enableThirdPartyCamerasDiscovery":
                    PostStateToChild( "${ Device }", "ThirdPartyCamerasEnabled", "${ it.value }" )
                    break
                case "extendedAiFeatures":
                    PostStateToChild( "${ Device }", "ExtendedAIFeatures", "${ it.value }" )
                    break
                case "streamingChannels":
                    PostStateToChild( "${ Device }", "StreamingChannels", "${ it.value }" )
                    break
                case "isAiReportingEnabled":
                    PostStateToChild( "${ Device }", "AiReportingEnabled", "${ it.value }" )
                    break
                case "fingerprintSettings":
                    PostStateToChild( "${ Device }", "FingerprintSettings", "${ it.value }" )
                    break
                case "nfcSettings":
                    PostStateToChild( "${ Device }", "NFCSettings", "${ it.value }" )
                    break
                case "fingerprintState":
                    PostStateToChild( "${ Device }", "FingerprintState", "${ it.value }" )
                    break
                case "nfcState":
                    PostStateToChild( "${ Device }", "NFCState", "${ it.value }" )
                    break
                case "isPairedWithAiPort":
                    PostStateToChild( "${ Device }", "PairedWithAiPort", "${ it.value }" )
                    break
                case "smartDetectLoiterZones":
                    PostStateToChild( "${ Device }", "SmartDetectLoiterZones", "${ it.value }" )
                    break
                case "ptzControlEnabled":
                    PostStateToChild( "${ Device }", "PTZControlEnabled", "${ it.value }" )
                    break
                // Not sure the specific value but providing them as a state
                case "ulpVersion":
                case "dbRecoveryOptions":
                    PostStateToChild( "${ Device }", it.key, it.value )
                    break
                // Not doing anything with this data at this moment. Not sure of value.
                case "portStatus":
                case "globalCameraSettings":
                    break
				// Ignored data - limited use at this time or redundant
                case "isDbAvailable":
                case "streamSharingAvailable":
                case "streamSharing":
				case "lightOnSettings":
				case "upSince":
				case "lastSeen":
				case "isCameraPaired":
				case "canAdopt":
				case "isSshEnabled":
				case "wiredConnectionState":
				case "connectedSince":
				case "connectionHost":
				case "host":
				case "isUpdating":
				case "isLocating":
				case "isAdopted":
				case "isAttemptingToConnect":
				case "isAdoptedByOther":
				case "modelKey":
				case "lastMotion":
				case "firmwareBuild":
				case "isAdopting":
				case "isProvisioned":
				case "isRebooting":
                case "phyRate":
                case "recordingSettings":
                case "smartDetectSettings":
                case "isRecording":
                case "isSmartDetected":
                case "isProbingForWifi":
                case "featureFlags":
                case "lenses":
                case "eventStats":
                case "privacyZones":
                case "apMac":
                case "hdrMode":
                case "isManaged":
                case "channels":
                case "videoReconfigurationInProgress":
                case "isLiveHeatmapEnabled":
                case "talkbackSettings":
                case "ledSettings":
                case "speakerSettings":
                case "isPoorNetwork":
                case "hasWifi":
                case "videoMode":
                case "recordingSchedules":
                case "ispSettings":
                case "smartDetectZones":
                case "smartDetectLines":
                case "micVolume":
                case "pirSettings":
                case "isMicEnabled":
                case "isWirelessUplinkEnabled":
                case "isDeleting":
                case "lastRing":
                case "anonymousDeviceId":
                case "hasSpeaker":
                case "wifiConnectionState":
                case "elementInfo":
                case "lastPrivacyZonePositionId":
                case "audioBitrate":
                case "voltage":
                case "canManage":
                case "apRssi":
                case "lcdMessage":
                case "marketName":
                case "motionZones":
                case "chimeDuration":
                case "osdSettings":
                case "analyticsData":
                case "isRecordingDisabled":
                case "skipFirmwareUpdate":
                case "errorCode":
                case "enableCrashReporting":
                case "isRecordingMotionOnly":
                case "hostType":
                case "isStatsGatheringEnabled":
                case "recordingRetentionDurationMs":
                case "isAway":
                case "isHardware":
                case "enableAutomaticBackups":
                case "isSetup":
                case "enableStatsReporting":
                case "hardwarePlatform":
                case "canAutoUpdate":
                case "hosts":
                case "disableAutoLink":
                case "isRecycling":
                case "storageStats":
                case "releaseChannel":
                case "hardwareId":
                case "cameraUtilization":
                case "timezone":
                case "wifiSettings":
                case "smartDetectAgreement":
                case "ports":
                case "network":
                case "ucoreVersion":
                case "temperatureUnit":
                case "ssoChannel":
                case "disableAudio":
                case "lastUpdateAt":
                case "hostShortname":
                case "doorbellSettings":
                case "uiVersion":
                case "enableBridgeAutoAdoption":
                case "maxCameraCapacity":
                case "isStation":
                case "timeFormat":
                case "locationSettings":
                case "avgMotions":
                case "lastDriveSlowEvent":
                case "isStacked":
                case "isPrimary":
                case "isUCoreSetup":
                case "isDownloadingFW":
                case "isInsightsEnabled":
                case "guid":
                case "bridgeCandidates":
                case "vaultCameras":
                case "hasGateway":
                case "isVaultRegistered":
                case "stopStreamLevel":
                case "userConfiguredAp":
                case "useGlobal":
                case "apMgmtIp":
                case "isRestoring":
                case "isProtectUpdatable":
                case "hardDriveState":
                case "isUcoreUpdatable":
                case "lastDeviceFWUpdatesCheckedAt":
                case "audioSettings":
                case "isUCoreStacked":
                case "timelapseEnabled":
                case "consoleEnv":
                case "agreements":
                case "isExtenderInstalledEver":
                case "videoCodecSwitchingSince":
                case "downScaleMode":
                case "hubMac":
                case "activePatrolSlot":
                case "videoCodecState":
                case "sysid":
                case "chosenBridge":
                case "enableNfc":
                case "isRemoteAccessEnabled":
                case "isMissingRecordingDetected":
					break
                default:
                    Logging( "Unhandled data for ${ Device } ${ it.key } = ${ it.value }", 3 )
                    break
            }
        }
    }
}

// Handles data sent from a child to the parent for processing
def ReceiveFromChild( String Type, String Child, Map Data ){
    Logging( "Received ${ Type } from ${ Child } = ${ Data }", 4 )
    switch( Type ){
        case "State":
            ProcessState( "${ Data.Name }", Data.Value )
            break
        case "Event":
            ProcessEvent( "${ Data.Name }", Data.Value )
            break
        case "Logging":
            Logging( "Log from ${ Child }: ${  Data.Value }", Data.Level )
            break
        case "Map":
            Data.each(){
                switch( it.value.Type ){
                    case "State":
                        ProcessState( "${ it.value.Name }", it.value.Value )
                        break
                    case "Event":
                        ProcessEvent( "${ it.value.Name }", it.value.Value )
                        break
                    case "Logging":
                        Logging( "Log from ${ Child }: ${  it.value.Value }", it.value.Level )
                        break
                    default:
                        Logging( "Test of ReceiveFromChild = Map = HUH?! ${ Child }, Map Data = ${ it }", 4 )
                        break
                }
            }
            break
        default:
            Logging( "Test of ReceiveFromChild = HUH?! ${ Child }, Data = ${ Data }", 3 )
            break
    }
}

// installed is called when the device is installed
def installed(){
	Logging( "Installed", 2 )
}

// uninstalling device so make sure to clean up children
void uninstalled() {
    // Delete all children
    getChildDevices().each{
        deleteChildDevice( it.deviceNetworkId )
    }
    unschedule()
    Logging( "Uninstalled", 2 )
}

// Used to convert epoch values to text dates
def String ConvertEpochToDate( String Epoch ){
    def date
    if( ( Epoch != null ) && ( Epoch != "" ) && ( Epoch != "null" ) ){
        Long Temp = Epoch.toLong()
        if( Temp <= 9999999999 ){
            date = new Date( ( Temp * 1000 ) ).toString()
        } else {
            date = new Date( Temp ).toString()
        }
    } else {
        date = "Null value provided"
    }
    return date
}

// Checks the location.getTemperatureScale() to convert temperature values
def ConvertTemperature( String Scale, Number Value ){
    if( Value != null ){
        def ReturnValue = Value as double
        if( location.getTemperatureScale() == "C" && Scale.toUpperCase() == "F" ){
            ReturnValue = ( ( ( Value - 32 ) * 5 ) / 9 )
            Logging( "Temperature Conversion ${ Value }°F to ${ ReturnValue }°C", 4 )
        } else if( location.getTemperatureScale() == "F" && Scale.toUpperCase() == "C" ) {
            ReturnValue = ( ( ( Value * 9 ) / 5 ) + 32 )
            Logging( "Temperature Conversion ${ Value }°C to ${ ReturnValue }°F", 4 )
        } else if( ( location.getTemperatureScale() == "C" && Scale.toUpperCase() == "C" ) || ( location.getTemperatureScale() == "F" && Scale.toUpperCase() == "F" ) ){
            ReturnValue = Value
        }
        def TempInt = ( ReturnValue * 100 ) as int
        ReturnValue = ( TempInt / 100 )
        return ReturnValue
    }
}

// Process data to check against current state value and then send an event if it has changed
def ProcessEvent( Variable, Value, Unit = null, ForceEvent = false ){
    if( ( state."${ Variable }" != Value ) || ( ForceEvent == true ) ){
        state."${ Variable }" = Value
        if( Unit != null ){
            Logging( "Event: ${ Variable } = ${ Value }${ Unit }", 4 )
            sendEvent( name: "${ Variable }", value: Value, unit: Unit, isStateChange: true )
        } else {
            Logging( "Event: ${ Variable } = ${ Value }", 4 )
            sendEvent( name: "${ Variable }", value: Value, isStateChange: true )
        }
       //UpdateTile( "${ Value }" )
    }
}

// Process data to check against current state value
def ProcessState( Variable, Value ){
    if( state."${ Variable }" != Value ){
        Logging( "State: ${ Variable } = ${ Value }", 4 )
        state."${ Variable }" = Value
        //UpdateTile( "${ Value }" )
    }
}

// Post data to child device
def PostEventToChild( Child, Variable, Value, Unit = null, ForceEvent = null ){
    if( "${ Child }" != null ){
        if( getChildDevice( "${ Child }" ) == null ){
            TempChild = Child.split( " " )
            def ChildType = ""
            switch( TempChild[ 0 ] ){
                case "Light":
                    ChildType = "Light"
                    break
                case "Doorbell":
                    ChildType = "Doorbell"
                    break
                case "Camera":
                    ChildType = "Camera"
                    break
                case "PTZCamera":
                    ChildType = "PTZCamera"
                    break
                case "Bridge":
                    ChildType = "Bridge"
                    break
                case "Sensor":
                    ChildType = "Sensor"
                    break
                default:
                    ChildType = "Generic"
                    break
            }
            addChild( "${ Child }", ChildType )
        }
        if( getChildDevice( "${ Child }" ) != null ){
            if( Unit != null ){
                if( ForceEvent != null ){
                    getChildDevice( "${ Child }" ).ProcessEvent( "${ Variable }", Value, "${ Unit }", ForceEvent )
                    Logging( "Child Event: ${ Variable } = ${ Value }${ Unit }", 4 )
                } else {
                    getChildDevice( "${ Child }" ).ProcessEvent( "${ Variable }", Value, "${ Unit }" )
                    Logging( "Child Event: ${ Variable } = ${ Value }", 4 )
                }
            } else {
                if( ForceEvent != null ){
                    getChildDevice( "${ Child }" ).ProcessEvent( "${ Variable }", Value, null, ForceEvent )
                    Logging( "Child Event: ${ Variable } = ${ Value }${ Unit }", 4 )
                } else {
                    getChildDevice( "${ Child }" ).ProcessEvent( "${ Variable }", Value )
                    Logging( "Child Event: ${ Variable } = ${ Value }", 4 )
                }
            }
        } else {
            if( Unit != null ){
                Logging( "Failure to add ${ Child } and post ${ Variable }=${ Value }${ Unit }", 5 )
            } else {
                Logging( "Failure to add ${ Child } and post ${ Variable }=${ Value }", 5 )
            }
        }
    } else {
        Logging( "Failure to add child because child name was null", 5 )
    }
}

// Post data to child device
def PostStateToChild( Child, Variable, Value ){
    if( "${ Child }" != null ){
        if( getChildDevice( "${ Child }" ) == null ){
            TempChild = Child.split( " " )
            def ChildType = ""
            switch( TempChild[ 0 ] ){
                case "Light":
                    ChildType = "Light"
                    break
                case "Doorbell":
                    ChildType = "Doorbell"
                    break
                case "Camera":
                    ChildType = "Camera"
                    break
                case "PTZCamera":
                    ChildType = "PTZCamera"
                    break
                case "Bridge":
                    ChildType = "Bridge"
                    break
                case "Sensor":
                    ChildType = "Sensor"
                    break
                default:
                    ChildType = "Generic"
                    break
            }
            addChild( "${ Child }", ChildType )
        }
        if( getChildDevice( "${ Child }" ) != null ){
            Logging( "${ Child } State: ${ Variable } = ${ Value }", 4 )
            getChildDevice( "${ Child }" ).ProcessState( "${ Variable }", Value )
        } else {
            Logging( "Failure to add ${ Child } and post ${ Variable }=${ Value }", 5 )
        }
    } else {
        Logging( "Failure to add child because child name was null", 5 )
    }
}

// Adds a UnifiProtectChild child device
// Based on @mircolino's method for child sensors
def addChild( String DNI, String ChildType ){
    try{
        Logging( "addChild(${ DNI })", 4 )
        switch( ChildType ){
            case "Light":
                addChildDevice( "UnifiProtectChild-Light", DNI, [ name: "${ DNI }" ] )
                break
            case "Camera":
                addChildDevice( "UnifiProtectChild-Camera", DNI, [ name: "${ DNI }" ] )
                break
            case "PTZCamera":
                addChildDevice( "UnifiProtectChild-PTZCamera", DNI, [ name: "${ DNI }" ] )
                break
            case "Doorbell":
                addChildDevice( "UnifiProtectChild-Doorbell", DNI, [ name: "${ DNI }" ] )
                break
            case "Bridge":
                addChildDevice( "UnifiProtectChild-Bridge", DNI, [ name: "${ DNI }" ] )
                break
            case "Sensor":
                addChildDevice( "UnifiProtectChild-Sensor", DNI, [ name: "${ DNI }" ] )
                break
            default:
                addChildDevice( "UnifiProtectChild", DNI, [ name: "${ DNI }" ] )
                break
        }
    }
    catch( Exception e ){
        def Temp = e as String
        if( Temp.contains( "not found" ) ){
            if( ChildType != null ){
                Logging( "UnifiProtectChild-${ ChildType } driver is not loaded, this is required for the child device.", 5 )
            } else {
                Logging( "UnifiProtectChild driver is not loaded, this is required for the child device.", 5 )
            }
        } else {
            Logging( "addChild Error, likely child already exists: ${ Temp }", 5 )
        }
    }
}

// Handles whether logging is enabled and thus what to put there.
def Logging( LogMessage, LogLevel ){
	// Add all messages as info logging
    if( ( LogLevel == 2 ) && ( LogType != "None" ) ){
        log.info( "${ device.displayName } - ${ LogMessage }" )
    } else if( ( LogLevel == 3 ) && ( ( LogType == "Debug" ) || ( LogType == "Trace" ) ) ){
        log.debug( "${ device.displayName } - ${ LogMessage }" )
    } else if( ( LogLevel == 4 ) && ( LogType == "Trace" ) ){
        log.trace( "${ device.displayName } - ${ LogMessage }" )
    } else if( LogLevel == 5 ){
        log.error( "${ device.displayName } - ${ LogMessage }" )
    }
}

// Checks drdsnell.com for the latest version of the driver
// Original inspiration from @cobra's version checking
def CheckForUpdate(){
    ProcessEvent( "DriverName", DriverName() )
    ProcessEvent( "DriverVersion", DriverVersion() )
	httpGet( uri: "https://www.drdsnell.com/projects/hubitat/drivers/versions.json", contentType: "application/json" ){ resp ->
        switch( resp.status ){
            case 200:
                if( resp.data."${ DriverName() }" ){
                    CurrentVersion = DriverVersion().split( /\./ )
                    if( resp.data."${ DriverName() }".version == "REPLACED" ){
                       ProcessEvent( "DriverStatus", "Driver replaced, please use ${ resp.data."${ state.DriverName }".file }" )
                    } else if( resp.data."${ DriverName() }".version == "REMOVED" ){
                       ProcessEvent( "DriverStatus", "Driver removed and no longer supported." )
                    } else {
                        SiteVersion = resp.data."${ DriverName() }".version.split( /\./ )
                        if( CurrentVersion == SiteVersion ){
                            Logging( "Driver version up to date", 2 )
				            ProcessEvent( "DriverStatus", "Up to date" )
                        } else if( ( CurrentVersion[ 0 ] as int ) > ( SiteVersion [ 0 ] as int ) ){
                            Logging( "Major development ${ CurrentVersion[ 0 ] }.${ CurrentVersion[ 1 ] }.${ CurrentVersion[ 2 ] } version", 4 )
				            ProcessEvent( "DriverStatus", "Major development ${ CurrentVersion[ 0 ] }.${ CurrentVersion[ 1 ] }.${ CurrentVersion[ 2 ] } version" )
                        } else if( ( CurrentVersion[ 1 ] as int ) > ( SiteVersion [ 1 ] as int ) ){
                            Logging( "Minor development ${ CurrentVersion[ 0 ] }.${ CurrentVersion[ 1 ] }.${ CurrentVersion[ 2 ] } version", 4 )
				            ProcessEvent( "DriverStatus", "Minor development ${ CurrentVersion[ 0 ] }.${ CurrentVersion[ 1 ] }.${ CurrentVersion[ 2 ] } version" )
                        } else if( ( CurrentVersion[ 2 ] as int ) > ( SiteVersion [ 2 ] as int ) ){
                            Logging( "Patch development ${ CurrentVersion[ 0 ] }.${ CurrentVersion[ 1 ] }.${ CurrentVersion[ 2 ] } version", 4 )
				            ProcessEvent( "DriverStatus", "Patch development ${ CurrentVersion[ 0 ] }.${ CurrentVersion[ 1 ] }.${ CurrentVersion[ 2 ] } version" )
                        } else if( ( SiteVersion[ 0 ] as int ) > ( CurrentVersion[ 0 ] as int ) ){
                            Logging( "New major release ${ SiteVersion[ 0 ] }.${ SiteVersion[ 1 ] }.${ SiteVersion[ 2 ] } available", 2 )
				            ProcessEvent( "DriverStatus", "New major release ${ SiteVersion[ 0 ] }.${ SiteVersion[ 1 ] }.${ SiteVersion[ 2 ] } available" )
                        } else if( ( SiteVersion[ 1 ] as int ) > ( CurrentVersion[ 1 ] as int ) ){
                            Logging( "New minor release ${ SiteVersion[ 0 ] }.${ SiteVersion[ 1 ] }.${ SiteVersion[ 2 ] } available", 2 )
				            ProcessEvent( "DriverStatus", "New minor release ${ SiteVersion[ 0 ] }.${ SiteVersion[ 1 ] }.${ SiteVersion[ 2 ] } available" )
                        } else if( ( SiteVersion[ 2 ] as int ) > ( CurrentVersion[ 2 ] as int ) ){
                            Logging( "New patch ${ SiteVersion[ 0 ] }.${ SiteVersion[ 1 ] }.${ SiteVersion[ 2 ] } available", 2 )
				            ProcessEvent( "DriverStatus", "New patch ${ SiteVersion[ 0 ] }.${ SiteVersion[ 1 ] }.${ SiteVersion[ 2 ] } available" )
                        }
                    }
                } else {
                    Logging( "${ DriverName() } is not published on drdsnell.com", 2 )
                    ProcessEvent( "DriverStatus", "${ DriverName() } is not published on drdsnell.com" )
                }
                break
            default:
                Logging( "Unable to check drdsnell.com for ${ DriverName() } driver updates.", 2 )
                break
        }
    }
}

// Mavrrick code for Webhook processing 
// Webhook Helper processing

def webhook (DeviceID, Attribute, Value ) {
    log.debug "webhook: Device ${DeviceID} Type of alert ${Attribute} value ${Value}"
    PostEventToChild( "${ DeviceID }", "${Attribute}" , "${Value}" , null, true )
    
    
}

