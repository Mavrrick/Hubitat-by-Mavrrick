library (
 author: "Mavrrick",
 category: "Govee",
 description: "GoveeCloudAPI",
 name: "Govee_Cloud_Level",
 namespace: "Mavrrick",
 documentationLink: "http://www.example.com/"
)

def cloudSetLevel(float v,duration = 0){
    int intv = v.toInteger()
    if (device.currentValue("cloudAPI") == "Retry") {
        log.error "setLevel(): CloudAPI already in retry state. Aborting call." 
    } else {
        cloudsetLevel2(intv)
    }
}

def cloudsetLevel2(int v){
        sendEvent(name: "cloudAPI", value: "Pending")
		if  (aRngBright) {v=incBrightnessRange(v)}
        if (debugLog) { log.debug "setLevel2(): Sent Brightness = ${v}"}
		sendCommand("brightness", v,"devices.capabilities.range")    
}

