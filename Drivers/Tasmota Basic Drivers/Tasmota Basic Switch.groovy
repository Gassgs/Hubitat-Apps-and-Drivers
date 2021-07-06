/**
 *  Tasmota Basic Switch
 *  
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
    definition (name: "Tasmota Basic Switch", namespace: "Gassgs", author: "Gary G") {
        capability "Switch"
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
        input name: "plugNum",type: "enum",title: "Plug Number", options:["0","1","2","3","4"], defaultValue: 0
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
    if (settings.plugNum == "0"){
        try {
            httpGet("http://" + deviceIp + "/cm?cmnd=POWER%20On") { resp ->
                def json = (resp.data)
                if (logEnable) log.debug "${json}"
                if (json.POWER == "ON"){
                    if (logEnable) log.debug "Command Success response from Device"
                    sendEvent(name: "switch", value: "on")
                    runIn(2,refresh)
                }else{
                    if (logEnable) log.debug "Command -ERROR- response from Device- $json"
                }
            }
        } catch (Exception e) {
            log.warn "Call to on failed: ${e.message}"
        }
    }else{
        try {
            httpGet("http://" + deviceIp + "/cm?cmnd=POWER" + plugNum + "%20On") { resp ->
                def json = (resp.data)
                if (logEnable) log.debug "${json}"
                if (json.POWER1 == "ON" || json.POWER2 == "ON" || json.POWER3 == "ON" || json.POWER4 == "ON" ) {
                    if (logEnable) log.debug "Command Success response from Device"
                    sendEvent(name: "switch", value: "on")
                    runIn(2,refresh)
                }else{
                    if (logEnable) log.debug "Command -ERROR- response from Device- $json"
                }
            }
        } catch (Exception e) {
            log.warn "Call to on failed: ${e.message}"
        }  
    }
}

def off() {
    if (logEnable) log.debug "Sending Off Command to [${settings.deviceIp}]"
        if (settings.plugNum == "0"){
        try {
            httpGet("http://" + deviceIp + "/cm?cmnd=POWER%20Off") { resp ->
                def json = (resp.data)
                if (logEnable) log.debug "${json}"
                if (json.POWER == "OFF"){
                    if (logEnable) log.debug "Command Success response from Device"
                    sendEvent(name: "switch", value: "off")
                    runIn(2,refresh)
                }else{
                    if (logEnable) log.debug "Command -ERROR- response from Device- $json"
                }
            }
        } catch (Exception e) {
            log.warn "Call to on failed: ${e.message}"
        }
    }else{
        try {
            httpGet("http://" + deviceIp + "/cm?cmnd=POWER" + plugNum + "%20Off") { resp ->
                def json = (resp.data)
                if (logEnable) log.debug "${json}"
                if (json.POWER1 == "OFF" || json.POWER2 == "OFF" || json.POWER3 == "OFF" || json.POWER4 == "OFF" ) {
                    if (logEnable) log.debug "Command Success response from Device"
                    sendEvent(name: "switch", value: "off")
                    runIn(2,refresh)
                }else{
                    if (logEnable) log.debug "Command -ERROR- response from Device- $json"
                }
            }
        } catch (Exception e) {
            log.warn "Call to on failed: ${e.message}"
        }  
    }
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
                   if (settings.plugNum == "0"){
                       status = json.StatusSTS.POWER as String
                       if (logEnable) log.debug "detected single plug"
                   }
                   if (settings.plugNum == "1"){
                       status = json.StatusSTS.POWER1 as String
                       if (logEnable) log.debug "detected plug 1"
                   }
                   if (settings.plugNum == "2"){
                       status = json.StatusSTS.POWER2 as String
                       if (logEnable) log.debug "detected plug 2"
                   }
                   if (settings.plugNum == "3"){
                       status = json.StatusSTS.POWER3 as String
                       if (logEnable) log.debug "detected plug 3"
                   }
                   if (settings.plugNum == "4"){
                       status = json.StatusSTS.POWER4 as String
                       if (logEnable) log.debug "detected plug 4"
                   }
                   signal = json.StatusSTS.Wifi.Signal as String
                   if (logEnable) log.debug "Wifi signal strength $signal db"
                   sendEvent(name:"wifi",value:"${signal}db")
                   if (settings.plugNum == "0"){
                       if (logEnable) log.debug "$device.label $deviceIp - $status"
                       if (logInfo) log.info "$device.label is - $status"
                   }else{
                       if (logEnable) log.debug "$device.label $deviceIp plug# $plugNum - $status"
                       if (logInfo) log.info "$device.label plug# $plugNum is - $status"
                   }   
                   if (status == "ON"){
                       sendEvent(name:"switch",value:"on")
                   }else{
                       sendEvent(name:"switch",value:"off")
               }
           }
        }
    }catch (Exception e) {
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
