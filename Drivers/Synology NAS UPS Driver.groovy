/**
 *  Synology NAS Driver
 *
 *  Used for battery backup status and messages. Parse webhook messages from DSM
 *
 *  Copyright 2022 Gassgs/ Gary Gassmann
 *
 *  Must setup http webhooks notifications in DSM
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
 *  V1.0.0  06-06-2022       first run   
 */

def driverVer() { return "1.0" }

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name: "Synology NAS Driver", namespace: "Gassgs", author: "Gary G") {
        capability "Actuator"
        capability "Sensor"
        capability "PowerSource"
        
        command "restoreHealth"
        
        attribute "lastMessage","string"
        attribute "status","string" 
    }
}
    preferences {
        input name: "deviceMac",type: "string", title: "Synology MAC address", required: true
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
    setDeviceNetworkId()
    def power = device.currentValue("powerSource")
    if (power == null){
        sendEvent(name:"powerSource",value:"mains")
        sendEvent(name:"status",value:"ok")
    if (logEnable) runIn(1800, logsOff)
    }
}

void setDeviceNetworkId(){
    if (deviceMac != null && device.deviceNetworkId != state.dni as String) {
        def macAddress = (deviceMac as String)
        def mac = macAddress.replace(":","").replace("-","")
        state.dni = mac as String
        device.deviceNetworkId = state.dni
        if (logEnable) log.debug "${state.dni as String} set as Device Network ID"
    }
}

def parse(LanMessage){
    if (logEnable) log.debug "data is ${LanMessage}"
    def msg = parseLanMessage(LanMessage)
    def json = msg.body.replace("{","").replace("}","").replace("message content","").replace('"',"").replace(":","")
    if (logEnable) log.debug "${json}"
    if (logInfo) log.info "${json}"
    sendEvent(name:"lastMessage",value:"${json}")
    if (json.contains("UPS")){
        if (logEnable) log.debug "Found the word UPS"
        if (json.contains("AC mode")){
            if (logEnable) log.debug "Found the value AC mode"
            if (logInfo) log.info "$device.label - Power source is Mains Power"
            sendEvent(name:"powerSource",value:"mains")
            sendEvent(name:"status",value:"ok")
        }
        else if (json.contains("battery mode")){
            if (logEnable) log.debug "Found the value battery mode"
            if (logInfo) log.info "$device.label - Power Source is Battery"
            sendEvent(name:"powerSource",value:"battery")
        }
        else if (json.contains("low battery")){
            if (logEnable) log.debug "Found the value low battery"
            if (logInfo) log.info "$device.label Battery is Low"
            sendEvent(name:"status",value:"low battery")
        }
        else if (json.contains("lost")){
            if (logEnable) log.debug "Found the value lost"
            if (logInfo) log.info "$device.label - UPS is Disconnected from Synology NAS"
            sendEvent(name:"status",value:"disconnected")
        }
        else if (json.contains("connected")){
            if (logEnable) log.debug "Found the value connected"
            if (logInfo) log.info "$device.label - UPS is Connected to Synology NAS"
            sendEvent(name:"powerSource",value:"mains")
            sendEvent(name:"status",value:"ok")
        }
    }
}

def restoreHealth(){
    if (logInfo) log.info "$device.label - Restore pressed, setting status back to Mains Power and OK"
    sendEvent(name:"powerSource",value:"mains")
    sendEvent(name:"status",value:"ok")
}

def installed() {
    log.info "installed..."
    log.warn "debug logging is: ${logEnable == true}"
    state.DriverVersion=driverVer()
    if (logEnable) runIn(1800, logsOff)
}
