/**
 *  Virtual Laundry Machine 2.0 
 *
 *  Virtual Driver for Washing Maching or Dryer Status
 *
 *  Copyright 2021 Gassgs/ Gary Gassmann
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
 *    V1.0  1-20-2021   -       first run - Gassgs  
 *    V1.1  2-13-2021   -       removed acceleration cap, not needed
 *    V1.2  7-1-2021    -       improved update method
 *    V1.3  10-8-2021   -       cleanup
 *    V1.4  12-19-2021  -       Added "door" and "machine" attributes
 *    V1.5  06-17-2022  -       Complete rewrite around a sonoff S31 for power monitor
 */
def driverVer() { return "1.5" }

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name: "Virtual Laundry Machine 2.0", namespace: "Gassgs", author: "Gary G") {
        capability "Actuator"
        capability "Switch"
        capability "Sensor"
        capability "Refresh"

        command"start"
        command"stop"
        command "relay", [[name:"Relay", type: "ENUM",description: "relay", constraints: ["On", "Off",]]]

        attribute"status","string"
        attribute"door","string"
        attribute"machine","string"
        attribute "outlet","string"
        attribute "wifi","string"
    }
}

    preferences {
        input name: "deviceIp",type: "string", title: "Tasmota Device IP Address", required: true
        input name: "hubIp",type: "string", title: "Hubitat Device IP Address", required: true
        input name: "refreshEnable",type: "bool", title: "Enable to Refresh every 30mins", defaultValue: true
        input name: "lowValue",type: "number", title: "Low Power threshold 'OFF'", defaultValue: 0 , required: true
        input name: "highValue",type: "number", title: "High Power threshold 'ON'", defaultValue: 0 , required: true
        input name: "timeout",type: "number", title: "Low Power Timeout - in minutes", defaultValue: 3 , required: true
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
    if (refreshEnable){
        runEvery30Minutes(refresh)
        if (logEnable) log.debug "refresh every 30 minutes scheduled"
    }else{
        unschedule(refresh)
        if (logEnable) log.debug "refresh schedule canceled"
	}
    deviceSetup()
    syncSetup()
    if (logEnable) runIn(1800, logsOff)
}

def deviceSetup(){
    if (deviceIp){
        try {
            httpGet("http://" + deviceIp + "/cm?cmnd=STATUS%200") { resp ->
                def json = (resp.data)
                if (json){
                    if (logEnable) log.debug "${json}"
                    def macAddress = (json.StatusNET.Mac)
                    def mac = macAddress.replace(":","")
                    state.dni = mac as String
                    if (logEnable) log.debug "Command Success response from Device"
                    if (logEnable) log.debug "Mac Address $macAddress  to DNI $mac"
                    setDeviceNetworkId()
                    def name = (json.Status.DeviceName)
                    if (logEnable) log.debug "Device Name set to $name"
                    device.name = "$name"
                }else{
                    if (logEnable) log.debug "Command -ERROR- response from Device- $json"
                }
            }
        } catch (Exception e) {
            log.warn "Call to on failed: ${e.message}"
        }
    }
}

void setDeviceNetworkId(){
    if (state.dni != null && state.dni != device.deviceNetworkId) {
       device.deviceNetworkId = state.dni
       if (logEnable) log.debug "${state.dni as String} set as Device Network ID"
    }
}

def syncSetup(){
        if (hubIp){
            rule = "ON Power1#state DO webquery http://"+ hubIp + ":39501/ POST Switch%value% ENDON " +
            "ON Margins#PowerLow=ON DO webquery http://"+ hubIp + ":39501/ POST PowerLow%value% ENDON " +
            "ON Margins#PowerHigh=ON DO webquery http://"+ hubIp + ":39501/ POST PowerHigh%value% ENDON " 
                    
            ruleNow = rule.replaceAll("%","%25").replaceAll("#","%23").replaceAll("/","%2F").replaceAll(":","%3A").replaceAll(";","%3B").replaceAll(" ","%20")
            if (logEnable) log.debug "$ruleNow"              
        try {
            httpGet("http://" + deviceIp + "/cm?cmnd=RULE3%20${ruleNow}") { resp ->
                def json = (resp.data) 
                if (json){
                    if (logEnable) log.debug "Command Success response from Device - Setup Rule 3"
                }else{
                    if (logEnable) log.debug "Command -ERROR- response from Device- $json"
                }
            }
        } catch (Exception e) {
            log.warn "Call to on failed: ${e.message}"
        }
    }
    runIn(2,turnOnRule)
}

def turnOnRule(){
    
    cmnd = "Rule3 ON; powerLow "+lowValue+"; powerHigh "+highValue+"; LedState 2"
    cmnds = cmnd.replaceAll("%","%25").replaceAll("#","%23").replaceAll("/","%2F").replaceAll(":","%3A").replaceAll(";","%3B").replaceAll(" ","%20")
            if (logEnable) log.debug "$cmnds" 
        
     try {
         httpGet("http://" + deviceIp + "/cm?cmnd=Backlog%20${cmnds}") { resp ->
             if (logEnable) log.debug "Command Success response from Device - Rule 3 And Power options activated"
         }
     } catch (Exception e) {
         log.warn "Call to on failed: ${e.message}"
    }
    refresh()
}

def parse(LanMessage){
    if (logEnable) log.debug "data is ${LanMessage}"
    def msg = parseLanMessage(LanMessage)
    def json = msg.body
    if (logEnable) log.debug "${json}"
    if (json.contains("Switch")){
        if (logEnable) log.debug "Found the word Switch"
        if (json.contains("1")){
            if (logEnable) log.debug "Found the value 1"
            if (logInfo) log.info "$device.label - Plug is On"
            sendEvent(name:"outlet",value:"on")
        }
        else if (json.contains("0")){
            if (logEnable) log.debug "Found the value 0"
            if (logInfo) log.info "$device.label - Plug is Off"
            sendEvent(name:"outlet",value:"off")
        }
    }
    if (json.contains("PowerLowON")){
        if (logEnable) log.debug "Found the word PowerLowON"
        if (logInfo) log.info "$device.label - Power is below Low threshold"
        runIn(timeout * 60,setDone)
    }
    if (json.contains("PowerHighON")){
        unschedule(setDone)
        if (logEnable) log.debug "Found the word PowerHighON"
        if (logInfo) log.info "$device.label - Power is above High threshold"
        sendEvent(name:"status",value:"running")
        sendEvent(name:"switch",value:"on")
    }
}

def setDone(){
    if (logInfo) log.info "$device.label - status is now Done"
    def door = device.currentValue("door")
    if (door == "closed"){
        sendEvent(name:"status",value:"done")
    }else{
        if (logInfo) log.info "$device.label - status is now Done, Door has already been opened"
    }
        
}

def relay(value){
    if (value == "On"){
        relayOn()
    }else{
        relayOff()
    }
}

def relayOn() {
    if (logEnable) log.debug "Sending On Command to [${settings.deviceIp}]"
    try {
        httpGet("http://" + deviceIp + "/cm?cmnd=POWER%20On") { resp ->
            def json = (resp.data)
            if (logEnable) log.debug "${json}"
            if (json.POWER == "ON"){
                if (logEnable) log.debug "Command Success response from Device"
            }else{
                if (logEnable) log.debug "Command -ERROR- response from Device- $json"
            }
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def relayOff() {
    if (logEnable) log.debug "Sending Off Command to [${settings.deviceIp}]"
    try {
        httpGet("http://" + deviceIp + "/cm?cmnd=POWER%20Off") { resp ->
            def json = (resp.data)
            if (logEnable) log.debug "${json}"
            if (json.POWER == "OFF"){
                if (logEnable) log.debug "Command Success response from Device"
            }else{
                if (logEnable) log.debug "Command -ERROR- response from Device- $json"
            }
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def refresh() {
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
                   if (logEnable) log.debug "$device.label $deviceIp - $status"
                   if (logInfo) log.info "$device.label Plug is - $status"
                   }   
                   if (status == "ON"){
                       sendEvent(name:"outlet",value:"on")
                   }else{
                       sendEvent(name:"outlet",value:"off")
                   }
           }
        }catch (Exception e) {
            sendEvent(name:"wifi",value:"offline")
            log.warn "Call to on failed: ${e.message}"
        }
    }
}

//doesn't function, device is for monitoring only
def on(){
    //sendEvent(name:"switch",value:"on")
    log.info "on pushed, doesn't do anything"
}
//used to turn off notifications and reset to "off"
def off(){
    sendEvent(name:"switch",value:"off")
    log.info "off pushed, setting to off"
}
//no function. device is for monitoring only
def start(){
    //sendEvent(name:"status",value:"running")
    log.info "start pushed, doesn't do anything"
}
//use only to reset the machine to idle if needed
def stop(){
    sendEvent(name:"status",value:"idle")
    log.info "stop pushed, status changed to idle"
}

def installed() {
    log.info "installed..."
    log.warn "debug logging is: ${logEnable == true}"
    state.DriverVersion=driverVer()
    if (logEnable) runIn(1800, logsOff)
}
