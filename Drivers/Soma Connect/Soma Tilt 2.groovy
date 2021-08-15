/**
 *  Soma Connect Driver for - Tilt 2
 *
 * To get the mac address for your devices type this command into a browser
 *      replace the IP address with the IP address of your Soma Connect
 *               ---  http://192.168.1.?:3000/list_devices  ---
 *
 *  Copyright 2021 Gassgs/ Gary Gassmann
 *
 *
 *  Based on the Hubitat community driver httpGetSwitch 
 * https://raw.githubusercontent.com/hubitat/HubitatPublic/master/examples/drivers/httpGetSwitch.groovy
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
 *  Change History:
 *
 *  V1.0.0  6-09-2021       first run   
 *  V1.1.0  6-14-2021       improvments & added morning position option
 *  V1.2.0  6-17-2021       Seperate Tilt and Shade drivers
 *  V1.3.0  8-14-2021       Fixed opening/closing bug with no position change.
 */

def driverVer() { return "1.2" }



metadata {
    definition (name: "Soma Connect Tilt 2", namespace: "Gassgs", author: "Gary G", importUrl: "https://raw.githubusercontent.com/Gassgs/Hubitat-Apps-and-Drivers/master/Drivers/Soma%20Connect/Soma%20Tilt%202.groovy") {
        capability "WindowShade"
        capability "Switch"
        capability "Switch Level"
        capability "Change Level"
        capability "Actuator"
        capability "Refresh"
        capability "Sensor"
        capability "Battery"

        command "morningPosition"

    }
}
    preferences {
        input name: "connectIp",type: "text", title: "Soma Connect IP Address", required: true
        input name: "mac", type: "text", title: "Mac address of Tilt 2 device", required: true
        input name: "timeout", type: "number", title: "Time it takes to open or close", required: true, defaultValue: 5
        input name: "openPos", type: "number", title: "Default Open Postion", required: true, defaultValue: 50
        input name: "morningPos",type: "number", title: "Postion to set for -Morning Position-", required: true, defaultValue: 30
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    state.DriverVersion=driverVer()
    refresh()
    if (logEnable) runIn(1800, logsOff)
}

def open(){
    if (logEnable) log.debug "Sending Open Command to [${settings.mac}]"
    setPosition(openPos)
}

def close() {
    if (logEnable) log.debug "Sending Close Command to [${settings.mac}]"
    try {
       httpGet("http://" + connectIp + ":3000/close_shade/"  + mac) { resp ->
           def json = (resp.data)
           if (logEnable) log.debug "${json}"
           if (json.result == "error") {
               if (logEnable) log.debug "Command -ERROR- from SOMA Connect- $json.msg"
           }
           if (json.result == "success") {
               if (logEnable) log.debug "Command Success Response from SOMA Connect"
               sendEvent(name: "windowShade", value: "closing", isStateChange: true)
               sendEvent(name: "position", value: 0, isStateChange: true)
               sendEvent(name: "level", value: 0, isStateChange: true)
               sendEvent(name: "switch", value: "off", isStateChange: true)
               runIn(timeout,refresh)          
            }
       }   
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def on() {
    open()
}

def off() {
    close()
}

def stopPositionChange() {
    if (logEnable) log.debug "Sending Stop Command to [${settings.mac}]"
    try {
       httpGet("http://" + connectIp + ":3000/stop_shade/"  + mac) { resp ->
           def json = (resp.data)
           if (logEnable) log.debug "${json}"
           if (json.result == "error") {
               if (logEnable) log.debug "Command -ERROR- from SOMA Connect- $json.msg"
           }
           if (json.result == "success") {
               if (logEnable) log.debug "Command Success Response from SOMA Connect"
               sendEvent(name: "windowShade", value: "stopped", isStateChange: true)
               runIn(2,refresh)          
            }
       }   
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def stopLevelChange() {
    stopPositionChange()
}

def setPosition(value) {
    if (logEnable) log.debug "Sending Set Position Command to [${settings.mac}]"

    value = value.toInteger()
    if (value <= 50){
        position = 100 - (value *2)
        direction = 0
    }
    else if (value > 50){
        position = (value - 50) *2
        direction = 1
    }
        
    try {
       httpGet("http://" + connectIp + ":3000/set_shade_position/"  + mac + "/" + position + "?close_upwards=" + direction) { resp ->
            def json = (resp.data)
            if (logEnable) log.debug "${json}"
            if (json.result == "error") {
               if (logEnable) log.debug "Command -ERROR- from SOMA Connect- $json.msg"
            }
            if (json.result == "success") {
                if (logEnable) log.debug "Command Success Response from SOMA Connect"
                sendEvent(name: "position", value: value, isStateChange: true)
                sendEvent(name: "level", value: value, isStateChange: true)
                if (value == device.currentValue("level")){
                    runIn(timeout,refresh)
                }
                else if (value > device.currentValue("level")){
                    sendEvent(name: "windowShade", value: "opening", isStateChange: true)
                    runIn(timeout,refresh) 
                }
                else{
                    sendEvent(name: "windowShade", value: "closing", isStateChange: true)
                    runIn(timeout,refresh) 
                }
            }
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def setLevel(value) {
    setPosition(value)
}

def startPositionChange(direction) {
    if (direction == "open") {
        open()
    } else {
       close()
    }
}

def startLevelChange(direction) {
    if (direction == "up") {
        open()
    } else {
       close()
    }
}

def morningPosition() {
    if (logEnable) log.debug "Sending Set Moring Position Command to [${settings.mac}]"
    
    def newPosition = 100 - morningPos
    
    try {
       httpGet("http://" + connectIp + ":3000/set_shade_position/"  + mac + "/"+ newPosition + "?morning_mode=1") { resp ->
            def json = (resp.data)
            if (logEnable) log.debug "${json}"
            if (json.result == "error") {
               if (logEnable) log.debug "Command -ERROR- from SOMA Connect- $json.msg"
            }
            if (json.result == "success") {
                if (logEnable) log.debug "Command Success Response from SOMA Connect"
                sendEvent(name: "position", value:morningPos, isStateChange: true)
                sendEvent(name: "level", value:morningPos, isStateChange: true)
                sendEvent(name: "windowShade", value: "opening",isStateChange: true)
                sendEvent(name: "switch", value: "on", isStateChange: true)
                runIn(timeout,refresh) 
            }
       }   
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def getPosition() {
    if (logEnable) log.debug "Checking Shade Position"
    try {
    httpGet("http://" + connectIp + ":3000/get_shade_state/"  + mac) { resp ->
        def json = (resp.data)
        if (logEnable) log.debug "${json}"
        if (json.find{ it.key == "closed_upwards" }){
            shadePos = 50 + (json.position / 2)
        }
        else if (json.position == 0){
            shadePos = 50
        }
        else if (json.position == 100){
            shadePos = 0
        
        }else{
            shadePos = (100 - json.position) /2
        }
        sendEvent(name: "position", value: shadePos)
        sendEvent(name: "level", value: shadePos)
        if (logEnable) log.debug  "Shade Position set to ${shadePos}"
        if (shadePos == openPos){
            sendEvent(name: "windowShade", value: "open",isStateChange: true)
            sendEvent(name: "switch", value: "on", isStateChange: true)
            } else if (shadePos == 0) {
                sendEvent(name: "windowShade", value: "closed",isStateChange: true)
                sendEvent(name: "switch", value: "off", isStateChange: true)
			} else {
                sendEvent(name: "windowShade", value: "partially open",isStateChange: true)
                sendEvent(name: "switch", value: "on", isStateChange: true)
			}
		}
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}


def getBattery() {
    if (logEnable) log.debug "Checking Battery Level"
    try {
    httpGet("http://" + connectIp + ":3000/get_battery_level/"  + mac) { resp ->
        def json = (resp.data)
        if (logEnable) log.debug  "${json}"
        def batteryPercent = json.battery_percentage
        sendEvent(name: "battery", value: batteryPercent)
        if (logEnable) log.debug  "Battery level set to ${batteryPercent}"
    }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def refresh() {
    getBattery()
    getPosition()
}

def installed() {
    log.info "installed..."
    log.warn "debug logging is: ${logEnable == true}"
    state.DriverVersion=driverVer()
    refresh()
    if (logEnable) runIn(1800, logsOff)
}
