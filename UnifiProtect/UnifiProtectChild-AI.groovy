metadata {
	definition (name: "UnifiProtectChild-AI", namespace: "Mavrrick", author: "Mavrrick") {
		capability "Actuator"
//		capability "Motion Sensor"
//        capability "Switch"
        
        attribute "faceID", "string"
        attribute "licensePlate", "string"
        attribute "lastUpdate", "string"
        
        command "face", [[name: 'Face', type: 'STRING']]
        command "plate", [[name: 'License Plate', type: 'STRING']]
	}   
}

void face(v) {
    log.debug "face: ${v}"
    time = ConvertEpochToDate(now().toString())
    log.debug "face: ${time}"
    sendEvent(name: "faceID", value: "$v", isStateChange: true)
    sendEvent(name: "lastUpdate", value: "$time", isStateChange: true)
}

void plate(v) {
    log.debug "plate: ${v}"
    time = ConvertEpochToDate(now().toString())
    log.debug "plate: ${time}"
    sendEvent(name: "licensePlate", value: "$v", isStateChange: true)
    sendEvent(name: "lastUpdate", value: "$time", isStateChange: true)
}

def installed() {
    initialized()
}

def initialized() {
    time = ConvertEpochToDate(now().toString())
    sendEvent(name: "faceID", value: "none", isStateChange: true)
    sendEvent(name: "licensePlate", value: "none", isStateChange: true)
    sendEvent(name: "lastUpdate", value: "$time", isStateChange: true)
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
