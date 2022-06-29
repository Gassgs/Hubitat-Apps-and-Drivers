/**
 *  Tasmota Sonoff TH16
 *  
 *
 *  Copyright 2022 Gassgs/ Gary Gassmann
 *
 *
 *  Based on the Hubitat community driver httpGetSwitch 
 *  https://raw.githubusercontent.com/hubitat/HubitatPublic/master/examples/drivers/httpGetSwitch.groovy
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
 *  V1.0.0  07-02-2021       first run   
 *  V1.1.0  07-17-2021       Refresh schedule improvements
 *  V1.2.0  08-15-2021       Added option to pause refreshing
 *  V1.3.0  10-09-2021       Fixed pause refreshing option
 *  V1.4.0  05-18-2022       Added motion capability
 *  V1.5.0  06-01-2022       Adding rule integration for syned updates, Many changes and improvments
 *  V1.6.0  06-02-2022       Adding temperature to the switch driver for TH16
 *  V1.7.0  06-28-2022       Removed "offline, status" moved to wifi atribute and general cleanup and improvments
 */

def driverVer() { return "1.7" }

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name: "Tasmota Sonoff TH16", namespace: "Gassgs", author: "Gary G") {
        capability "Switch"
        capability "Actuator"
        capability "Refresh"
        capability "Sensor"
        capability "TemperatureMeasurement"
        capability "MotionSensor"
        
        command "toggle"
        
        attribute "wifi","string"
        
    }
}
    preferences {
        input name: "deviceIp",type: "string", title: "Tasmota Device IP Address", required: true
        input name: "hubIp",type: "string", title: "Hubitat Device IP Address", required: true
        input name: "refreshEnable",type: "bool", title: "Enable to Refresh every 30mins", defaultValue: true
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
                "ON Tele-DS18B20#Temperature DO webquery http://" + hubIp + ":39501/ POST Temperature%value% ENDON "
                
            ruleNow = rule.replaceAll("%","%25").replaceAll("#","%23").replaceAll("/","%2F").replaceAll(":","%3A").replaceAll(" ","%20")
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
     try {
         httpGet("http://" + deviceIp + "/cm?cmnd=RULE3%20ON") { resp ->
             def json = (resp.data) 
             if (json){
                 if (logEnable) log.debug "Command Success response from Device - Rule 3 activated"
             }else{
                 if (logEnable) log.debug "Command -ERROR- response from Device- $json"
             }
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
            if (logInfo) log.info "$device.label - is On"
            sendEvent(name:"switch",value:"on")
            sendEvent(name:"motion",value:"active")
        }
        else if (json.contains("0")){
            if (logEnable) log.debug "Found the value 0"
            if (logInfo) log.info "$device.label - is Off"
            sendEvent(name:"switch",value:"off")
            sendEvent(name:"motion",value:"inactive")
        }
    }
    if (json.contains("Temperature")){
        json = json?.replace("Temperature","") 
        if (logEnable) log.debug "Found the word Temperature"
        if (logInfo) log.info "$device.label - Temperature is $json"
        sendEvent(name:"temperature",value:"$json")
    }
}
    
def on() {
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

def off() {
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

def toggle(){
    status = device.currentValue("switch")
    if (status == "on"){
        off()
    }else{
        on()
    }
}  

def refresh() {
    if(settings.deviceIp){
        if (logEnable) log.debug "Refreshing Device Status - [${settings.deviceIp}]"
        try {
           httpGet("http://" + deviceIp + "/cm?cmnd=status%200") { resp ->
           def json = (resp.data)
            if (logEnable) log.debug "${json}"
               if (json.containsKey("StatusSNS")){
                   if (logEnable) log.debug "DS18B20 temperature found"
                   temp = json.StatusSNS.DS18B20.Temperature
                   sendEvent(name:"temperature",value:"${temp}")
                   if (logEnable) log.debug "Temperature of $device.label $deviceIp is ${temp}"
                   if (logInfo) log.info "Temperature of $device.label - is ${temp}"
               }
               if (json.containsKey("StatusSTS")){
                   if (logEnable) log.debug "PWR status found"
                   status = json.StatusSTS.POWER as String
                   signal = json.StatusSTS.Wifi.Signal as String
                   if (logEnable) log.debug "Wifi signal strength $signal db"
                   sendEvent(name:"wifi",value:"${signal}db")
                   if (logEnable) log.debug "$device.label $deviceIp - $status"
                   if (logInfo) log.info "$device.label is - $status"
                   }   
                   if (status == "ON"){
                       sendEvent(name:"switch",value:"on")
                       sendEvent(name:"motion",value:"active")
                   }else{
                       sendEvent(name:"switch",value:"off")
                       sendEvent(name:"motion",value:"inactive")
                   }
           }
        }catch (Exception e) {
            sendEvent(name:"wifi",value:"offline")
            log.warn "Call to on failed: ${e.message}"
        }
    }
} 

def installed() {
    log.info "installed..."
    log.warn "debug logging is: ${logEnable == true}"
    state.DriverVersion=driverVer()
    if (logEnable) runIn(1800, logsOff)
}
