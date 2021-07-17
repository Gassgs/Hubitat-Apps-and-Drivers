/*
 *  Tasmota Aquarium w/ Tasmota Sonoff RF Bridge & Tasmota Auto Fish Feeder
 *
 * RF Controlled Light, Tasmota Auto Fish Feeder with added Temperature Probe
 *
 * Calls URIs with HTTP GET forSonOff RF Bridge  Aquarium Light Control
 * Tasmota Automatic Fish Feeder with Counter and Temperature Sensor
 *
 *
 * Based on the Hubitat community driver httpGetSwitch 
 * https://raw.githubusercontent.com/hubitat/HubitatPublic/master/examples/drivers/httpGetSwitch.groovy
 *
 * Raw Codes -----
 *
 * 0%   AAB04D0408022603CA0654172A3818080818081818180808180808081818080808080808081808080808081808080808080808081818080808080808080808180808081818080818181818081808082955
 * 10%  AAB04D0408021203CA064A177A3818080818081818180808180808081818180808080808081808080808081808080808080808081818080808080808080808180808081818081808081808181808082955
 * 20%  AAB04D0408021C03D4065417843818080818081818180808180808081818180808080808180808080808081808080808080808181818080808080808080808180808081818080818180808080818082955
 * 30%  AAB04D0408021203CA064017703818080818081818180808180808081818180808080808181808080808081808080808080808180818080808181808080808180808081818081818080808181818082955
 * 40%  AAB04D0408021C03CA067C17203818080818081818180808180808081818180808080818080808080808081808080808080808180818080808181808080808180808081818080818081808081818082955
 * 50%  AAB04D0408021C03D4062C177A3818080818081818180808180808081818180808080818081808080808081808080808080808180818080808181808080808180808081818080808080808180808082955
 * 60%  AAB04D0408021C03D4064A177A3818080818081818180808180808081818180808080818180808080808081808080808080808180818080808181808080808180808081818081818181818180818082955
 * 70%  AAB04D0408021C03C0064A177A3818080818081818180808180808081818180808080818181808080808081808080808080808180818080808181808080808180808081818081808180818081808082955
 * 80%  AAB04D0408021C03CA064017703818080818081818180808180808081818180808081808080808080808081808080808080808180818080808181808080808180808081818081818180808180808082955
 * 90%  AAB04D0408021C03D4064017703818080818081818180808180808081818180808081808081808080808081808080808080808180818080808181808080808180808081818081808181808081818082955
 * 100% AAB04D0408021C03CA068617703818080818081818180808180808081818180808081808180808080808081808080808080808180818080808181808080808180808081818080818080818081808082955
 * BluLow  AAB04D0408021C03C0064A17203818080818081818180808180808081818080808081808180818080808080818080808080808180818080808181808080808180808081818081808081818180818082955
 * BluHigh AAB04D0408021C03CA064017663818080818081818180808180808081818080808081808180818080808081818180808080808180818080808181808080808180808081818080818081808080818082955
 */
def driverVer() { return "1.2" }

metadata {
    definition(name: "Tasmota Aquarium", namespace: "Gassgs ", author: "Gary G") {
        capability "Actuator"
        capability "Switch"
        capability "Switch Level"
        capability "Sensor"
        capability "Refresh"
        capability "Temperature Measurement"
        
        command  "blueLightLow"
        command  "blueLightHigh"
        command  "feedFish"
        command  "resetCounter"
        
        attribute "blueLight","string"
        attribute "lastFeeding","string"
        attribute "feedCounter","string"
        attribute "wifi","string"
    }
}

preferences {
    section("URIs") {
        def refreshRate = [:]
		refreshRate << ["1 min" : "Refresh every minute"]
		refreshRate << ["5 min" : "Refresh every 5 minutes"]
        refreshRate << ["10 min" : "Refresh every 10 minutes"]
		refreshRate << ["15 min" : "Refresh every 15 minutes"]
		refreshRate << ["30 min" : "Refresh every 30 minutes"]
		input "sonoffIP", "text", title: "Sonoff RF Bridge IP Address", required: true
        input name: "rfoffenabled", type: "bool" , title: "Send RF Raw Off", defaultValue: true
        input "feederIP", "text", title: "Tasmota Feeder IP Address", required: false // true
        input ("refresh_Rate", "enum", title: "Device Refresh Rate", options: refreshRate, defaultValue: "15 min")
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "infoEnable", type: "bool", title: "Enable info logging", defaultValue: true
    }
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def rfoffdisabled() {
    log.warn "rf raw off disabled..."
    device.updateSetting("rfoffenabled", [value: "false", type: "bool"])
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    state.DriverVersion=driverVer()
    
     switch(refresh_Rate) {
		case "1 min" :
			runEvery1Minute(refresh)
            if (logEnable) log.debug "refresh every minute schedule"
			break
		case "5 min" :
			runEvery5Minutes(refresh)
            if (logEnable) log.debug "refresh every 5 minutes schedule"
			break
        case "10 min" :
			runEvery10Minutes(refresh)
            if (logEnable) log.debug "refresh every 10 minutes schedule"
			break
		case "15 min" :
			runEvery15Minutes(refresh)
            if (logEnable) log.debug "refresh every 15 minutes schedule"
			break
		case "30 min" :
			runEvery30Minutes(refresh)
            if (logEnable) log.debug "refresh every 30 minutes schedule"
     }
    if (logEnable) runIn(1800, logsOff)
}

def parse(String description) {
    if (logEnable) log.debug(description)
}

def feedFish(){
    date = new Date()
    if (logEnable) log.debug "Sending Feed Command to $device.label"
    try {
       httpGet("http://" + feederIP + "/cm?cmnd=Power%20On") { resp ->
           def json = (resp.data)
           if (logEnable) log.debug "${json}"
           if (json.POWER != "ON") {
               if (logEnable) log.debug "Command -ERROR- response from Device- $json"
           }
           if (json.POWER == "ON") {
               if (logEnable) log.debug "Command Success response from Device"
               sendEvent(name: "lastFeeding", value: "$date")
               if (infoEnable) log.info "$device.label Fish feed $date"
               runIn(10,refresh)          
            }
       }   
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def resetCounter(){
    if (logEnable) log.debug "Sending Reset Counter Command to $device.label"
    try {
       httpGet("http://" + feederIP + "/cm?cmnd=Counter%200") { resp ->
           def json = (resp.data)
           if (logEnable) log.debug "${json}"
           if (json.Counter1 != 0) {
               if (logEnable) log.debug "Command -ERROR- response from Device- $json"
           }
           if (json.Counter1 == 0) {
               if (logEnable) log.debug "Command Success response from Device"
               if (infoEnable) log.info "Counter Reset"
               runIn(2,refresh)          
            }
       }   
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def refresh() {
    if(settings.feederIP){
        if (logEnable) log.debug "Refreshing Device Status - [${settings.feederIP}]"
        try {
           httpGet("http://" + feederIP + "/cm?cmnd=status%200") { resp ->
           def json = (resp.data)
            if (logEnable) log.debug "${json}"
               if (json.containsKey("StatusSNS")){
                   if (logEnable) log.debug "DS18B20 temperature found"
                   temp = json.StatusSNS.DS18B20.Temperature
                   count = json.StatusSNS.COUNTER.C1
                   sendEvent(name:"temperature",value:"${temp}")
                   sendEvent(name:"feedCounter",value:"${count}")
                   if (logEnable) log.debug "Temperature of $device.label $feederIP is ${temp} Feed Count is ${count}"
                   if (infoEnable) log.info "Temperature of $device.label - is ${temp} feed counter is $count"
            }
               if (json.containsKey("StatusSTS")){
                   status = json.StatusSTS.POWER as String
                   signal = json.StatusSTS.Wifi.Signal as String
                   if (logEnable) log.debug "Wifi signal strength $signal db"
                   sendEvent(name:"wifi",value:"${signal}db")
                   if (logEnable) log.debug "$device.label $feederIP - $status Wifi signal strength $signal db"
                   if (infoEnable) log.info "$device.label Wifi signal strength $signal db"
               }
           }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
    }
}

def on() {
    def data = "$state.lastLevel"
    if (logEnable) log.debug "on at $data last level"
    setLevel("$data")
}

def off() {
    state.lastLevel = device.currentValue("level")
    if (logEnable) log.debug "last level is ${state.lastLevel}"
    setLevel(0)
} 

def setLevel(data){
    value = data as Integer
    if (value == 0){
        cmd = "AAB04D0408022603CA0654172A3818080818081818180808180808081818080808080808081808080808081808080808080808081818080808080808080808180808081818080818181818081808082955"
        if (logEnable) log.debug "Set value 0 - Off"
    }
    if (value > 0 && value <= 10){
        cmd = "AAB04D0408021203CA064A177A3818080818081818180808180808081818180808080808081808080808081808080808080808081818080808080808080808180808081818081808081808181808082955"
        if (logEnable) log.debug "Set value 1 - 10 cmd $cmd"
    }
    if (value > 10 && value <= 20){
        cmd = "AAB04D0408021C03D4065417843818080818081818180808180808081818180808080808180808080808081808080808080808181818080808080808080808180808081818080818180808080818082955"
        if (logEnable) log.debug "Set value 11 - 20 cmd $cmd"
    }
    if (value > 20 && value <= 30){
        cmd = "AAB04D0408021203CA064017703818080818081818180808180808081818180808080808181808080808081808080808080808180818080808181808080808180808081818081818080808181818082955"
        if (logEnable) log.debug "Set value 21 - 30 cmd $cmd"
    }
    if (value > 30 && value <= 40){
        cmd = "AAB04D0408021C03CA067C17203818080818081818180808180808081818180808080818080808080808081808080808080808180818080808181808080808180808081818080818081808081818082955"
        if (logEnable) log.debug "Set value 31 - 40 cmd $cmd"
    }
    if (value > 40 && value <= 50){
        cmd = "AAB04D0408021C03D4062C177A3818080818081818180808180808081818180808080818081808080808081808080808080808180818080808181808080808180808081818080808080808180808082955"
        if (logEnable) log.debug "Set value 41 - 50 cmd $cmd"
    }
    if (value > 50 && value <= 60){
        cmd = "AAB04D0408021C03D4064A177A3818080818081818180808180808081818180808080818180808080808081808080808080808180818080808181808080808180808081818081818181818180818082955"
        if (logEnable) log.debug "Set value 51 - 60 cmd $cmd"
    }
    if (value > 60 && value <= 70){
        cmd = "AAB04D0408021C03C0064A177A3818080818081818180808180808081818180808080818181808080808081808080808080808180818080808181808080808180808081818081808180818081808082955"
        if (logEnable) log.debug "Set value 61 - 70 cmd $cmd"
    }
    if (value > 70 && value <= 80){
        cmd = "AAB04D0408021C03CA064017703818080818081818180808180808081818180808081808080808080808081808080808080808180818080808181808080808180808081818081818180808180808082955"
        if (logEnable) log.debug "Set value 71 - 80 cmd $cmd"
    }
    if (value > 80 && value <= 90){
        cmd = "AAB04D0408021C03D4064017703818080818081818180808180808081818180808081808081808080808081808080808080808180818080808181808080808180808081818081808181808081818082955"
        if (logEnable) log.debug "Set value 81 - 90 cmd $cmd"
    }
    if (value > 90){
        cmd = "AAB04D0408021C03CA068617703818080818081818180808180808081818180808081808180808080808081808080808080808180818080808181808080808180808081818080818080818081808082955"
        if (logEnable) log.debug "Set value 91 -100 cmd $cmd"
    }
    if (logEnable) log.debug "Sending Set Level GET request to [${settings.sonoffIP}]"

    try {
       httpGet( "http://" + sonoffIP + "/cm?cmnd=RfRaw%20" + cmd){ resp ->
            if (resp.success) {
                if (value > 0){
                    sendEvent(name:"switch",value:"on")
                    sendEvent(name: "blueLight", value: "off")
                }else{
                    sendEvent(name:"switch",value:"off")
                    sendEvent(name: "blueLight", value: "off")
                }
                sendEvent(name:"level",value:"$value")
                if (logEnable) log.debug "Set level $value"
                if (infoEnable) log.info "$device.label Light $value %"
                if (rfoffenabled)runInMillis(500,"rawOff")
            }
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    
    }
    
}

def blueLightLow(){
    state.lastLevel = device.currentValue("level")
    cmd = "AAB04D0408021203CA064017703818080818081818180808180808081818080808081808180818080808080818080808080808180818080808181808080808180808081818081808081818180818082955"
    if (logEnable) log.debug "Sending Blue Light Low GET request to [${settings.sonoffIP}]"

    try {
       httpGet( "http://" + sonoffIP + "/cm?cmnd=RfRaw%20" + cmd){ resp ->
            if (resp.success) {
                sendEvent(name: "blueLight", value: "low")
                sendEvent(name:"switch",value:"off")
                if (infoEnable) log.info "$device.label Blue Light On Low"
                if (rfoffenabled)runInMillis(500,"rawOff")
            }
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    
    }
    
}

def blueLightHigh(){
    state.lastLevel = device.currentValue("level")
    cmd = "AAB04D0408021C03CA064017663818080818081818180808180808081818080808081808180818080808081818180808080808180818080808181808080808180808081818080818081808080818082955"
    if (logEnable) log.debug "Sending Blue Liht High GET request to [${settings.sonoffIP}]"

    try {
       httpGet( "http://" + sonoffIP + "/cm?cmnd=RfRaw%20" + cmd){ resp ->
            if (resp.success) {
                sendEvent(name: "blueLight", value: "high")
                sendEvent(name:"switch",value:"off")
                if (infoEnable) log.info "$device.label Blue Light On High"
                if (rfoffenabled)runInMillis(500,"rawOff")
            }
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    
    }
    
}

def rawOff() {
    if (logEnable) log.debug "Sending off GET request to [${settings.sonoffIP}]"

    try {
        httpGet("http://" + sonoffIP + "/cm?cmnd=RfRaw%20" + 0){ resp -> 
            if (resp.success) {
            }
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to off failed: ${e.message}"
    }
}

def installed() {
    log.info "installed..."
    log.warn "debug logging is: ${logEnable == true}"
    state.DriverVersion=driverVer()
    if (logEnable) runIn(1800, logsOff)
}
