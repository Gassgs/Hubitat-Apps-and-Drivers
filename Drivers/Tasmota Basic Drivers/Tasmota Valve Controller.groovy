/**
 *  Tasmota Valve Controller
 *
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
 *  V1.0.0  7-2-2021       first run   
 * 
 */

def driverVer() { return "1.0" }

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name: "Tasmota Valve Controller", namespace: "Gassgs", author: "Gary G") {
        capability "Switch"
        capability "Valve"
        capability "Actuator"
        capability "Refresh"
        capability "Sensor"
        
        command "toggle"
        
        attribute "wifi","string"
        
    }
}
    preferences {
        input name: "deviceIp",type: "string", title: "Tasmota Device IP Address", required: true
        input name: "refreshInt",type: "number", title: "How often to refresh, in Minutes", required: true, defaultValue:5
        input name: "logInfo", type: "bool", title: "Enable info logging", defaultValue: true
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

def on() {
    if (logEnable) log.debug "Sending On Command to [${settings.deviceIp}]"
    try {
       httpGet("http://" + deviceIp + "/cm?cmnd=Power%20On") { resp ->
           def json = (resp.data)
           if (logEnable) log.debug "${json}"
           if (json.POWER != "ON") {
               if (logEnable) log.debug "Command -ERROR- response from Device- $json"
           }
           if (json.POWER == "ON") {
               if (logEnable) log.debug "Command Success response from Device"
               sendEvent(name: "switch", value: "on")
               sendEvent(name: "valve", value: "open")
               runIn(2,refresh)          
            }
       }   
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def open(){
    on()
}

def off() {
    if (logEnable) log.debug "Sending Off Command to [${settings.deviceIp}]"
    try {
       httpGet("http://" + deviceIp + "/cm?cmnd=Power%20Off") { resp ->
           def json = (resp.data)
           if (logEnable) log.debug "${json}"
           if (json.POWER != "OFF") {
               if (logEnable) log.debug "Command -ERROR- response from Device- $json"
           }
           if (json.POWER == "OFF") {
               if (logEnable) log.debug "Command Success response from Device"
               sendEvent(name: "switch", value: "off")
               sendEvent(name: "valve", value: "closed")
               runIn(2,refresh)          
            }
       }   
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def close(){
    off()
}

def toggle(){
    status = device.currentValue("switch")
    if (status == "on"){
        off()
    }else{
        on()
    }
}  

def refresh() {
    unschedule(refresh)
    if(settings.deviceIp){
        if (logEnable) log.debug "Refreshing Device Status - [${settings.deviceIp}]"
        try {
           httpGet("http://" + deviceIp + "/cm?cmnd=status%2011") { resp ->
           def json = (resp.data)
            if (logEnable) log.debug "${json}"
               if (json.containsKey("StatusSTS")){
                   if (logEnable) log.debug "PWR status found"
                   status = json.StatusSTS.POWER as String
                   signal = json.StatusSTS.Wifi.Signal as String
                   if (logEnable) log.debug "Wifi signal strength $signal db"
                   sendEvent(name:"wifi",value:"${signal}db")
                   if (logEnable) log.debug "$deviceIp - $status"
                   if (logInfo) log.info "$deviceIp - is $status"
                   if (status == "ON"){
                       sendEvent(name:"switch",value:"on")
                       sendEvent(name: "valve", value: "open")
               }else{
                       sendEvent(name:"switch",value:"off")
                       sendEvent(name: "valve", value: "closed")
                   }  
               }
           }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
    runIn(refreshInt*60,refresh)
    }
}

def installed() {
    log.info "installed..."
    log.warn "debug logging is: ${logEnable == true}"
    state.DriverVersion=driverVer()
    refresh()
    if (logEnable) runIn(1800, logsOff)
}
