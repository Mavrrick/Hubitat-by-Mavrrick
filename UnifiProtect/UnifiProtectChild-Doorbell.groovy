/*
* UnifiProtectChild-Doorbell
*
* Description:
* This Hubitat driver provides a spot to put data from Unifi Protect Doorbell-related devices. It does not belong on it's own and requires
* the UnifiProtectAPI driver as a parent device.
*
* Instructions for using Tile Template method (originally based on @mircolino's HTML Templates):
* 1) In "Hubitat -> Devices" select the child/sensor (not the parent) you would like to "templetize"
* 2) In "Preferences -> Tile Template" enter your template (example below) and click "Save Preferences"
*   Ex: "[font size='2'][b]Temperature:[/b] ${ temperature }°${ location.getTemperatureScale() }[/br][/font]"
* 3) In a Hubitat dashboard, add a new tile, and select the child/sensor, in the center select "Attribute", and on the right select the "Tile" attribute
* 4) Select the Add Tile button and the tile should appear
* NOTE: Should accept most HTML formatting commands with [] instead of <>
* 
* Features List:
* Ability to control general device settings
* Ability to trigger device's locate function
* Ability to check a website (mine) to notify user if there is a newer version of the driver available
* 
* Licensing:
* Copyright 2024 David Snell
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
* in compliance with the License. You may obtain a copy of the License at: http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
* for the specific language governing permissions and limitations under the License.
*
* Version Control:
* 0.1.13 - Inclusion of an attribute to store the userId associated with a fingerprint
* 0.1.12 - Changing refresh to try Camera status because Doorbell no longer works.
* 0.1.11 - Correction of isStateChange
* 0.1.10 - Change to remove old driver-specific variables and events
* 0.1.9 - Correct to button handling and removed space from Button Presses attribute name
* 0.1.8 - Added ReleaseableButton capability and changed driver-specific attribute names to remove spaces
* 0.1.7 - Update to refresh command method
* 0.1.6 - More button-related functionality, updated ProcessEvent, added info logging for push command
* 0.1.5 - Added attribute for Last Motion
* 0.1.4 - Added attribute for smartDetectType
* 0.1.3 - Command to get a snapshot image from the camera
* 0.1.2 - Removed capabilities not applicable to doorbells and added preferences for settings
* 0.1.1 - Added pushable button capability and additional attributes
* 0.1.0 - Initial version
* 
* Thank you(s):
* @Cobra for inspiration of how I perform driver version checking
* @mircolino for HTML Template method for dashboard use
*/

// Returns the driver name
def DriverName(){
    return "UnifiProtectChild-Doorbell"
}

// Returns the driver version
def DriverVersion(){
    return "0.1.13"
}

// Driver Metadata
metadata{
	definition( name: "UnifiProtectChild-Doorbell", namespace: "Snell", author: "David Snell", importUrl: "https://www.drdsnell.com/projects/hubitat/drivers/UnifiProtectChild-Doorbell.groovy" ) {
        capability "Sensor"
        capability "Actuator"
        capability "MotionSensor"
        capability "ImageCapture"
        capability "PushableButton"
        capability "ReleasableButton"
        capability "Refresh"
        
        // Commands
        //command "DoSomething" // For testing and development purposes only, it should not be uncommented for normal use
        command "push"
        command "release"
        //command "Locate" // Meant to help identify/locate the particular device by flashing the light
        
        // Attributes for the driver itself
		attribute "DriverName", "string" // Identifies the driver being used for update purposes
		attribute "DriverVersion", "string" // Handles version for driver
        attribute "DriverStatus", "string" // Handles version notices for driver
        
        // General Device Attributes
        attribute "Type", "string" // The type of device, per Ubiquiti data
        
        // Attributes - Device Related
        attribute "Status", "string" // Show success/failure of commands performed
        attribute "Device Status", "string" // Show the current state of the device as reported by the controller
        attribute "Device Type", "string" // What type of device the child is respresenting

        // Device Attributes
        attribute "Dark", "enum", [ "true", "false" ] // Whether the light level is dark
        attribute "Indicator Enabled", "enum", [ "true", "false" ] // Whether the camera's indicator is enabled
        attribute "Recording Now", "enum", [ "true", "false" ] // Whether the camera is recording this moment
        attribute "Motion Events Today", "number" // Number of motion events today
        attribute "ButtonPresses", "number" // ButtonPresses is used to show the number of button presses
        attribute "SnapshotURL", "string"
        attribute "SnapshotImage", "string"
        attribute "smartDetectType", "string"
        attribute "Last Motion", "string"
        attribute "FingerprintUserID", "string" // Stores the userID received when a fingerprint is read
        
        // Tile Template attribute
        attribute "Tile", "string" // Ex: "[font size='2'][b]Temperature:[/b] ${ temperature }°${ location.getTemperatureScale() }[/br][/font]"
        
        // AI related Attributes
        attribute "AIRecognition", "string" // Stores the ID associated with a persons face as recognized by AI
        
    }
	preferences{
        if( ShowAllPreferences ){
            input( type: "string", name: "DeviceName", title: "<b>Device Name</b>", description: "<font size='2'>If set it will change the device's name on the controller.</font>", defaultValue: "${ device.label }")
            input( type: "number", name: "MicVolume", title: "<b>Microphone Volume</b>", description: "<font size='2'>0 to 100</font>", required: true, defaultValue: 100, range: "0..100" )
            input( type: "bool", name: "StatusLED", title: "<b>Status LED On/Off</b>", defaultValue: false)
            input( type: "bool", name: "ExternalIR", title: "<b>External IR Lights On/Off</b>", defaultValue: false)
            input( type: "bool", name: "SystemSounds", title: "<b>System Status Sounds On/Off</b>", defaultValue: false)
            input( type: "int", name: "MaxPresses", title: "<b>Max button presses before rolling over</b>", required: false, defaultValue: 4 )
            input( name: "TileTemplate", type: "string", title: "<b>Tile Template</b>", description: "<font size='2'>Ex: [b]Temperature:[/b] \${ state.temperature }&deg;${ location.getTemperatureScale() }[/br]</font>", defaultValue: "");
            input( type: "enum", name: "LogType", title: "<b>Enable Logging?</b>", required: false, multiple: false, options: [ "None", "Info", "Debug", "Trace" ], defaultValue: "Info" )
            input( type: "bool", name: "ShowAllPreferences", title: "<b>Show All Preferences?</b>", defaultValue: true )
        } else {
            input( type: "bool", name: "ShowAllPreferences", title: "<b>Show All Preferences?</b>", defaultValue: true )
        }
	}
}


// DoSomething is for testing and development purposes. It should not be uncommented for normal usage.
def DoSomething(){

}

// updated
def updated( boolean NewDevice = false ){
    if( LogType == null ){
        LogType = "Info"
    }
    
    if( NewDevice != true ){
        SendSettings()
    }
    
    // Set basic button info
    if( MaxPresses == null ){
        MaxPresses = 4
    }
    ProcessState( "MaxPresses", MaxPresses )
    
    ProcessEvent( "numberOfButtons", 1 )
    ProcessEvent( "ButtonPresses", 0 )
    
    // Schedule daily check for driver updates to notify user
    def Hour = ( new Date().format( "h" ) as int )
    def Minute = ( new Date().format( "m" ) as int )
    def Second = ( new Date().format( "s" ) as int )

    // Set the driver name and version before update checking is scheduled
    if( state."Driver Status" != null ){
        state.remove( "Driver Name" )
        state.remove( "Driver Version" )
        state.remove( "Driver Status" )
        device.deleteCurrentState( "Driver Status" )
        device.deleteCurrentState( "Driver Name" )
        device.deleteCurrentState( "Driver Version" )
    }
    ProcessEvent( "DriverName", DriverName() )
    ProcessEvent( "DriverVersion", DriverVersion() )
    ProcessEvent( "DriverStatus", null )
    // Schedule checks that are only performed once a day
    schedule( "${ Second } ${ Minute } ${ Hour } ? * *", "CheckForUpdate" )
    
    if (EnableAIChildDevice == true && getChildDevices().size() == 0) {
        runIn(10, addAIDeviceHelper)
    }
    
    Logging( "Updated", 2 )
}

// Handle any button push activities
def push( Number Button = 1 ){
    ProcessEvent( "pushed", 1, null, true )
    state.pushed = 1
    //ProcessEvent( "pushed", Button, null, true )
    def Presses
    if( state.ButtonPresses != null ){
        Presses = ( state.ButtonPresses + 1 ) as int
    } else {
        Presses = 1
    }
    def Max
    if( MaxPresses != null ){
        Max = ( MaxPresses as int )
    } else {
        Max = 4
    }
    if( Presses > Max ){
        Presses = 1
    }
    ProcessEvent( "ButtonPresses", Presses, null, true )
    Logging( "Button ${ Button } pressed & number of presses = ${ Presses }", 2 )
}

// Handle any button release activities
def release( Number Button = 1 ){
    ProcessEvent( "released", Button, null, true )
    Logging( "Button ${ Button } released", 2 )
}

// Attempts to trigger a snapshot image from the doorbell
def take(){
    if( state.ID != null ){
        Logging( "Attempting to take a snapshot image", 2 )
        parent.GetSnapshot( state.ID, device.getDeviceNetworkId() )
    } else {
        Logging( "No ID for ${ device.getDeviceNetworkId() }, cannot get snapshot", 5 )
    } 
}

// refresh information on the specific child
def refresh(){
    if( state.ID != null ){
        parent.GetCameraStatus( device.getDeviceNetworkId(), state.ID )
    } else {
        Logging( "No ID for ${ device.getDeviceNetworkId() }, cannot refresh", 5 )
    }
}

// Turn on the device's locate function to help locate/identify it
def Locate(){
    if( state.ID != null ){
        parent.LocateDoorbell( device.getDeviceNetworkId(), state.ID )
    } else {
        Logging( "No ID for ${ device.getDeviceNetworkId() }, cannot activate identify function", 5 )
    }
}

// Configure device settings based on Preferences
def SendSettings(){
    if( state.ID != null ){
        if( DeviceName != null && DeviceName != device.label ){
            parent.SendCameraSettings( device.getDeviceNetworkId(), state.ID, "{\"micVolume\":${ MicVolume },\"name\":\"${ DeviceName }\",\"ledSettings\":{\"isEnabled\":${ StatusLED }},\"ispSettings\":{\"isExternalIrEnabled\":${ ExternalIR }},\"speakerSettings\":{\"areSystemSoundsEnabled\":${ SystemSounds }}}" )
        } else {
            parent.SendCameraSettings( device.getDeviceNetworkId(), state.ID, "{\"micVolume\":${ MicVolume },\"name\":\"${ device.label }\",\"ledSettings\":{\"isEnabled\":${ StatusLED }},\"ispSettings\":{\"isExternalIrEnabled\":${ ExternalIR }},\"speakerSettings\":{\"areSystemSoundsEnabled\":${ SystemSounds }}}" )
        }
    } else {
        Logging( "No ID for ${ device.getDeviceNetworkId() }, cannot send settings", 5 )
    }
}

// installed is called when the device is installed, all it really does is run updated
def installed(){
	Logging( "Installed", 2 )
	updated( true )
}

// initialize is called when the device is initialized, all it really does is run updated
def initialize(){
	Logging( "Initialized", 2 )
	updated( true )
}

// Return a state value
def ReturnState( Variable ){
    return state."${ Variable }"
}

// Tile Template method based on @mircolino's HTML Template method
private void UpdateTile( String val ){
    if( settings.TileTemplate ){
        // Create special compound/html tile
        val = settings.TileTemplate.toString().replaceAll( "\\[", "<" )
        val = val.replaceAll( "\\]", ">" )
        val = val.replaceAll( ~/\$\{\s*([A-Za-z][A-Za-z0-9_]*)\s*\}/ ) { java.util.ArrayList m -> device.currentValue("${ m [ 1 ] }").toString() }
        if( device.currentValue( "Tile" ).toString() != val ){
            sendEvent( name: "Tile", value: val )
        }
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
       UpdateTile( "${ Value }" )
    }
}

// Process data to check against current state value
def ProcessState( Variable, Value ){
    if( state."${ Variable }" != Value ){
        Logging( "State: ${ Variable } = ${ Value }", 4 )
        state."${ Variable }" = Value
        UpdateTile( "${ Value }" )
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

