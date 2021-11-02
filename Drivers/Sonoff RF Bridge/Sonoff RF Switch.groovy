/*
 * Sonoff RF  Switch
 *
 * Calls URIs with HTTP GET forSonOff RF Bridge  switch on on/off
*
*
* Based on the Hubitat community driver httpGetSwitch 
* https://raw.githubusercontent.com/hubitat/HubitatPublic/master/examples/drivers/httpGetSwitch.groovy
*
* Example "B0 raw code" 
    "AAB0210308017C04BA31922819081908190819090908190819081908190818181818181855"
 * 
 */
def driverVer() { return "2.2" }

metadata {
    definition(name: "Sonoff RF  Switch", namespace: "Gassgs ", author: "Gary G", importUrl: "https://raw.githubusercontent.com/Gassgs/SonOff-RF-Bridge-Drivers-for-Hubitat/master/Sonoff%20RF%20Switch") {
        capability "Actuator"
        capability "Switch"
        capability "Sensor"
    }
}

preferences {
    section("URIs") {
        input "onURI", "text", title: "On B0 Raw(no  spaces)", required: false
        input "offURI", "text", title: "Off B0 Raw(no spaces)", required: false
		input "sonoffIP", "text", title: "Sonoff RF Bridge IP Address", required: true
        input name: "rfoffenabled", type: "bool" , title: "Send RF Raw Off", defaultValue: true
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
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
    if (logEnable) runIn(1800, logsOff)
    state.DriverVersion=driverVer()
}

def parse(String description) {
    if (logEnable) log.debug(description)
}

def on() {
    if (logEnable) log.debug "Sending off GET request to [${settings.onURI}]"

    try {
       httpGet( "http://" + sonoffIP + "/cm?cmnd=RfRaw%20" + onURI){ resp ->
            if (resp.success) {
                sendEvent(name: "switch", value: "on", isStateChange: true)
                 if (rfoffenabled)runInMillis(500,"rawOff")
            }
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    
    } 
}

def off() {
    if (logEnable) log.debug "Sending off GET request to [${settings.offURI}]"

    try {
        httpGet("http://" + sonoffIP + "/cm?cmnd=RfRaw%20" + offURI){ resp -> 
            if (resp.success) {
                sendEvent(name: "switch", value: "off", isStateChange: true)
               if (rfoffenabled)runInMillis(500,"rawOff")
            }
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to off failed: ${e.message}"
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
    if (logEnable) runIn(1800, logsOff)
    state.DriverVersion=driverVer()
}
