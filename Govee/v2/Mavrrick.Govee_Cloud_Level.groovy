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
    switch(true) {
        case intv > 100:
        	if (debugLog) {log.debug ("cloudSetLevel(): Value of ${v} is over 100. Setting to 100")};
            intv = 100;
        break;
        case intv < 0:
        	if (debugLog) {log.debug ("cloudSetLevel(): Value of ${v} is below 0. Setting to 0")};
            intv = 0;
        break;
        default: 
            if (debugLog) {log.debug ("cloudSetLevel(): Setting Level to ${v}")};
        break;
    }
    if (device.currentValue("cloudAPI") == "Retry") {
        log.error "cloudSetLevel(): CloudAPI already in retry state. Aborting call." 
    } else {
        cloudsetLevel2(intv)
    }
}

def cloudsetLevel2(int v){
        sendEvent(name: "cloudAPI", value: "Pending")
		if  (aRngBright) {v=incBrightnessRange(v)}
        if (debugLog) { log.debug "cloudsetLevel2(): Sent Brightness = ${v}"}
		sendCommand("brightness", v,"devices.capabilities.range")    
}

