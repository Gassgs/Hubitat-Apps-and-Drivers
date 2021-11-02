/**
 *  Sonoff RF Simple Shade CMD
 *
 *  Send RF comannds for button control on Zigbee/Z-wave shades with RF 433mhz
 *
 * Usage case - use for button control when you have a z wave or zigbee shade that tracks level changes  but does not have a "stop" command
 * 
 *      -examples-
 *
 *   push button - if shade is opening/closing" = "stop" , else = "open/close"
 *              or
 *   push and hold = "open" ,  release "stop"
 *
 *  Based on the Hubitat community driver httpGetSwitch 
 * https://raw.githubusercontent.com/hubitat/HubitatPublic/master/examples/drivers/httpGetSwitch.groovy
 *
 * Example "B0 raw code" 
    "AAB0210308017C04BA31922819081908190819090908190819081908190818181818181855"
 * 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  
 * 
 */
def driverVer() { return "1.4" }

metadata {
    definition (name: "Sonoff RF Simple shade CMD", namespace: "Gassgs", author: "Gary G", importUrl: "https://raw.githubusercontent.com/Gassgs/SonOff-RF-Bridge-Drivers-for-Hubitat/master/Sonoff%20RF%20Simple%20shade%20CMD") {
        capability "WindowShade"
        capability "Actuator"
        capability"Sensor"
        
        command"stop"
    }   
}    
    preferences {
    section("URIs") {
        input "openURI", "text", title: "Open B0 Raw(no spaces)", required: false
        input "closeURI", "text", title: "Close B0 Raw(no  spaces)", required: false
        input "stopURI", "text", title: "Stop B0 Raw(no spaces)", required: false
		input "sonoffIP", "text", title: "Sonoff RF Bridge IP Address", required: true
        input  "timeout",  "number" , title: "Timeout in seconds for status to change to stopped", defaultValue: 15, required: true
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

def open() {
    if (logEnable) log.debug "Sending off GET request to [${settings.onURI}]"

    try {
       httpGet("http://" + sonoffIP + "/cm?cmnd=RfRaw%20" + openURI) { resp ->
            if (resp.success) {
                sendEvent(name: "windowShade", value: "opening", isStateChange: true)
                  if (rfoffenabled)runInMillis(500,"rawOff")
                  runIn(timeout-1, auto)
            }
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def close() {
    if (logEnable) log.debug "Sending off GET request to [${settings.closeURI}]"

    try {
       httpGet("http://" + sonoffIP + "/cm?cmnd=RfRaw%20" + closeURI) { resp ->
            if (resp.success) {
                sendEvent(name: "windowShade", value: "closing", isStateChange: true)
                 if (rfoffenabled)runInMillis(500,"rawOff")
                  runIn(timeout-1, auto)
            }
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def stop() {
    if (logEnable) log.debug "Sending off GET request to [${settings.stopURI}]"

    try {
       httpGet("http://" + sonoffIP + "/cm?cmnd=RfRaw%20" + stopURI) { resp ->
            if (resp.success) {
                sendEvent(name: "windowShade", value: "stopped", isStateChange: true)
                 if (rfoffenabled)runInMillis(500,"rawOff")
            }
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def auto() {
    sendEvent(name: "windowShade", value: "stopped")
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
