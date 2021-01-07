
// Hubitat driver for Govee RGB Strips using Cloud API - test
// Version 1.0.0

metadata {
	definition(name: "Govee Immersion LED Strip", namespace: "Obi2000", author: "Obi2000") {
		capability "Switch"         
		capability "ColorControl"
        capability "ColorTemperature"
		capability "Light"
		capability "SwitchLevel"
        capability "ColorMode"        
        capability "Refresh"
		
		attribute "colorName", "string"
        
//        command "white"
//        command "ModeMusic"
//        command "ModeVideo"
//          command "DeviceInfo"
        
    }

	preferences {		
		section("Device Info") {
        input(name: "Model", type: "enum", title: "Please select the Govee Model Number", options: ["H6160", "H6163", "H6104", "H6109", "H6110", "H6117",
            "H6159", "H7021", "H7022", "H6086", "H6089", "H6182", "H6085",
            "H7014", "H5081", "H6188", "H6135", "H6137", "H6141", "H6142",
            "H6195", "H7005", "H6083", "H6002", "H6003",
            "H6148","H6052","H6143","H6144","H6050","H6199","H6054","H5001"], defaultValue: "POST", required: true, displayDuringSetup: true)
            input(name: "APIKey", type: "string", title: "User API Key", displayDuringSetup: true, required: true)
			input(name: "MACAddr", type: "string", title: "Device Mac address", displayDuringSetup: true, required: true)
            input(name: "UpdateState", type: "bool", title: "Enabled/Disable Status Update", displayDuringSetup: true, default: true ,required: true)
		}
		
	}
}

def parse(String description) {

}

def on() {
    sendEvent(name: "switch", value: "on")
	sendCommand("turn", "on")
}

def off() {
    sendEvent(name: "switch", value: "off")
	sendCommand("turn", "off")
}

def setColorTemperature(value)
{
	sendEvent(name: "colorMode", value: "CT")
	log.debug "ColorTemp = " + value
	def intvalue = value.toInteger()
	
    sendEvent(name: "colorTemperature", value: intvalue)
    
		sendCommand("colorTem", intvalue)
		setCTColorName(intvalue)
}   



def setCTColorName(value)
{
	    if (value < 2600) {
            sendEvent(name: "colorName", value: "Warm White")
        }
        else if (value < 3500) {
            sendEvent(name: "colorName", value: "Incandescent")
        }
        else if (value < 4500) {
            sendEvent(name: "colorName", value: "White")
        }
        else if (value < 5500) {
            sendEvent(name: "colorName", value: "Daylight")
        }
        else if (value >=  5500) {
            sendEvent(name: "colorName", value: "Cool White")
        }
	
}





    
def setColor(value) {
	log.debug "HSBColor = "+ value
	   if (value instanceof Map) {
        def h = value.containsKey("hue") ? value.hue : null
        def s = value.containsKey("saturation") ? value.saturation : null
        def b = value.containsKey("level") ? value.level : null
    	setHsb(h, s, b)
    } else {
        log.warn "Invalid argument for setColor: ${value}"
    }
}

def setHsb(h,s,b)
{
	log.debug("setHSB - ${h},${s},${b}")

	hsbcmd = [h,s,b]
	log.debug "Cmd = ${hsbcmd}"

    sendEvent(name: "hue", value: "${h}")
    sendEvent(name: "saturation", value: "${s}")
	if(b!= device.currentValue("level").toInteger()){
		sendEvent(name: "level", value: "${b}")
		setLevel(b)
	}
	rgb = hubitat.helper.ColorUtils.hsvToRGB(hsbcmd)
    def rgbmap = [:]
    rgbmap.r = rgb[0]
    rgbmap.g = rgb[1]
    rgbmap.b = rgb[2]   
    
 
        sendEvent(name: "colorMode", value: "RGB")
        sendCommand("color", rgbmap)
    
}

def setHue(h)
{
    setHsb(h,device.currentValue( "saturation" ),device.currentValue("level"))
}

def setSaturation(s)
{
	setHsb(device.currentValue("hue"),s,device.currentValue("level"))
}

def setLevel(v)
{
	    sendEvent(name: "level", value: v)
        sendCommand("brightness", v)
}


def white() {

}


 def DeviceInfo(){
	     def params = [
            uri   : "https://developer-api.govee.com",
            path  : '/v1/devices',
			headers: ["Govee-API-Key": settings.APIKey, "Content-Type": "application/json"],
        ]
    


try {

			httpGet(params) { resp ->

				log.debug resp.data
				return resp.data
			}
			
	} catch (groovyx.net.http.HttpResponseException e) {
		log.error "callURL() >>>>>>>>>>>>>>>> ERROR >>>>>>>>>>>>>>>>"
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}"
		log.error "callURL() <<<<<<<<<<<<<<<< ERROR <<<<<<<<<<<<<<<<"
		
		return 'unknown'
	}    
} 



private def sendCommand(String command, payload) {


     def params = [
            uri   : "https://developer-api.govee.com",
            path  : '/v1/devices/control',
			headers: ["Govee-API-Key": settings.APIKey, "Content-Type": "application/json"],
            contentType: "application/json",      
			body: [device: settings.MACAddr, model: settings.Model, cmd: ["name": command, "value": payload]],
        ]
    

try {

			httpPutJson(params) { resp ->
				
				//log.debug "response.data="+resp.data
				
				return resp.data
		
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		log.error "callURL() >>>>>>>>>>>>>>>> ERROR >>>>>>>>>>>>>>>>"
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}"
		log.error "callURL() <<<<<<<<<<<<<<<< ERROR <<<<<<<<<<<<<<<<"
		
		return 'unknown'
	}
}


def getDeviceState(){
    if (UpdateState) {
	
	     def params = [
            uri   : "https://developer-api.govee.com",
            path  : '/v1/devices/state',
			headers: ["Govee-API-Key": settings.APIKey, "Content-Type": "application/json"],
			query: [device: settings.MACAddr, model: settings.Model],
        ]
    


try {

			httpGet(params) { resp ->

			
				log.debug resp.data.data.properties
                
                sendEvent(name: "switch", value: resp.data.data.properties[1].powerState)
                sendEvent(name: "level", value: resp.data.data.properties[2].brightness)

                if(resp.data.data.properties[3].containsKey("colorTemInKelvin")){
					ct = resp.data.data.properties[3].colorTemInKelvin
					sendEvent(name: "colorTemperature", value: ct)
					setCTColorName(ct)					
                }
                
				if(resp.data.data.properties[3].containsKey("color")){
                    r=resp.data.data.properties[3].color.r
                    g=resp.data.data.properties[3].color.g
                    b=resp.data.data.properties[3].color.b
                    HSVlst=hubitat.helper.ColorUtils.rgbToHSV([r,g,b])
                    hue=HSVlst[0].toInteger()
                    sat=HSVlst[1].toInteger()
                    sendEvent(name: "hue", value: hue)
                    sendEvent(name: "saturation", value: sat)
				
                }
				
				return resp.data
			}
			
	} catch (groovyx.net.http.HttpResponseException e) {
		log.error "callURL() >>>>>>>>>>>>>>>> ERROR >>>>>>>>>>>>>>>>"
		log.error "Error: e.statusCode ${e.statusCode}"
		log.error "${e}"
		log.error "callURL() <<<<<<<<<<<<<<<< ERROR <<<<<<<<<<<<<<<<"
		
		return 'unknown'
	    }
    }
}    

def poll() {
	refresh()
}

def refresh() {
    getDeviceState()
}

def updated() {
    DeviceInfo()
    refresh()
}
