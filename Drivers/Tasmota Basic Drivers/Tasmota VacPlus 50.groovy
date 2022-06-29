/**
 *  Tasmota VacPlus 50 Pint Dehumidifier
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
 * 
 *
 *  Change History:
 *
 *  V1.0.0  5-26-2022       first run
 *  V1.1.0  5-28-2022       added timer and Ionizer option
 *  V1.2.0  5-29-2022       added Maker API integration for automatic updates
 *  V1.3.0  5-30-2022       Sending post LAN messages to parse instead. No maker API needed
 *  V1.4.0  5-31-2022       Improved "refresh"
 *  V1.5.0  6-03-2022       Automated rule setup process
 *  V1.6.0  6-28-2022       Removed "offline, status" moved to wifi atribute and general cleanup and improvments
 * 
 */

def driverVer() { return "1.6" }

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name: "Tasmota VacPlus Dehumidifier", namespace: "Gassgs", author: "Gary G") {
        capability "Switch"
        capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
        capability "Actuator"
        capability "Sensor"
        capability "Refresh"
        
        command "ionizerOn"
        command "ionizerOff"
        command "setMode", [[name:"Set Mode", type: "ENUM",description: "Set Mode", constraints: ["standard","raining","drying","sleep"]]]
        command "setHumidity",[[name: "humidity",type: "NUMBER",description:"Desired Humidity Level"]]
        
        attribute "ionizer","string"
        attribute "mode","string"
        attribute "humiditySetpoint","number"
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
            rule = "ON Power1#state DO webquery http://" +hubIp +":39501/ POST SwitchOne%value% ENDON " +
                "ON Power2#state DO webquery http://" + hubIp + ":39501/ POST SwitchTwo%value% ENDON " +
                "ON Tele-TuyaSNS#Humidity DO webquery http://" + hubIp + ":39501/ POST Humidity%value% ENDON " +
                "ON Tele-TuyaSNS#Temperature DO webquery http://" + hubIp + ":39501/ POST Temperature%value% ENDON "

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
    runIn(1,syncSetup2)
}

def syncSetup2(){
        if (hubIp){
            rule = "ON TuyaEnum1#data DO webquery http://" + hubIp + ":39501/ POST Mode%value% ENDON " +
                "ON TuyaSNS#HumSet DO webquery http://" + hubIp + ":39501/ POST HumSet%value% ENDON "

            ruleNow = rule.replaceAll("%","%25").replaceAll("#","%23").replaceAll("/","%2F").replaceAll(":","%3A").replaceAll(" ","%20")
            if (logEnable) log.debug "$ruleNow"              
        try {
            httpGet("http://" + deviceIp + "/cm?cmnd=RULE2%20${ruleNow}") { resp ->
                def json = (resp.data) 
                if (json){
                    if (logEnable) log.debug "Command Success response from Device - Setup Rule 2"
                }else{
                    if (logEnable) log.debug "Command -ERROR- response from Device- $json"
                }
            }
        } catch (Exception e) {
            log.warn "Call to on failed: ${e.message}"
        }
    }
    runIn(1,turnOnRule)
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
    runIn(1,turnOnRule2)
}

def turnOnRule2(){
     try {
         httpGet("http://" + deviceIp + "/cm?cmnd=RULE2%20ON") { resp ->
             def json = (resp.data) 
             if (json){
                 if (logEnable) log.debug "Command Success response from Device - Rule 2 activated"
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
    if (json.contains("SwitchOne")){
        if (logEnable) log.debug "Found the word SwitchOne"
        if (json.contains("1")){
            if (logEnable) log.debug "Found the value 1"
            if (logInfo) log.info "$device.label - is On"
            sendEvent(name:"switch",value:"on")
        }
        else if (json.contains("0")){
            if (logEnable) log.debug "Found the value 0"
            if (logInfo) log.info "$device.label - is Off"
            sendEvent(name:"switch",value:"off")
        }
    }
    else if (json.contains("SwitchTwo")){
        if (logEnable) log.debug"Found the word SwitchTwo"
        if (json.contains("1")){
            if (logEnable) log.debug "Found the value 1"
            if (logInfo) log.info "$device.label - ionizer is On"
            sendEvent(name:"ionizer",value:"on")
        }
        else if (json.contains("0")){
            if (logEnable) log.debug "Found the value 0"
            if (logInfo) log.info "$device.label - ionizer is Off"
            sendEvent(name:"ionizer",value:"off")
        }
    }
    else if (json.contains("Mode")){
        if (logEnable) log.debug "Found the word Mode"
        if (json.contains("0")){
            if (logEnable) log.debug "Found the value 0"
            if (logInfo) log.info "$device.label - Standard Mode"
            sendEvent(name:"mode",value:"standard")
        }
        else if (json.contains("1")){
            if (logEnable) log.debug "Found the value 1"
            if (logInfo) log.info "$device.label - Raining Mode"
            sendEvent(name:"mode",value:"raining")
        }
        else if (json.contains("2")){
            if (logEnable) log.debug "Found the value 2"
            if (logInfo) log.info "$device.label - Drying Mode"
            sendEvent(name:"mode",value:"drying")
        }
        else if (json.contains("3")){
            if (logEnable) log.debug "Found the value 3"
            if (logInfo) log.info "$device.label - Sleep Mode"
            sendEvent(name:"mode",value:"sleep")
        }
    }
    else if (json.contains("Humidity")){
        json = json?.replace("Humidity","") 
        if (logEnable) log.debug "Found the word Humidity"
        if (logInfo) log.info "$device.label - Humidity is $json"
        sendEvent(name:"humidity",value:"$json")
        }
    else if (json.contains("Temperature")){
        json = json?.replace("Temperature","") 
        if (logEnable) log.debug "Found the word Temperature"
        if (logInfo) log.info "$device.label - Temperature is $json"
        sendEvent(name:"temperature",value:"$json")
        }
    else if (json.contains("HumSet")){
        json = json?.replace("HumSet","") 
        if (logEnable) log.debug "Found the word HumSet"
        if (logInfo) log.info "$device.label - Humidity Set Point is $json"
        sendEvent(name:"humiditySetpoint",value:"$json")
        }
}

def on() {
    if (logEnable) log.debug "Sending On Command to [${settings.deviceIp}]"
    try {
       httpGet("http://" + deviceIp + "/cm?cmnd=TuyaSend1%201,1") { resp ->
           def json = (resp.data)
           if (logEnable) log.debug "${json}"
           if (json.TuyaSend != "Done") {
               if (logEnable) log.debug "Command -ERROR- response from Device- $json"
           }
           if (json.TuyaSend == "Done") {
               if (logEnable) log.debug "Command Success response from Device"        
            }
       }   
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def off() {
    if (logEnable) log.debug "Sending Off Command to [${settings.deviceIp}]"
    try {
       httpGet("http://" + deviceIp + "/cm?cmnd=TuyaSend1%201,0") { resp ->
           def json = (resp.data)
           if (logEnable) log.debug "${json}"
           if (json.TuyaSend != "Done") {
               if (logEnable) log.debug "Command -ERROR- response from Device- $json"
           }
           if (json.TuyaSend == "Done") {
               if (logEnable) log.debug "Command Success response from Device"         
            }
       }   
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}  

def setMode(mode) {
    if (logEnable) log.debug "Sending Set Mode, ${mode} Command to [${settings.deviceIp}]"
    if (mode == "standard"){
        modeNum = 0
    }
    else if (mode == "raining"){
        modeNum = 1
    }
    else if (mode == "drying"){
        modeNum = 2
    }
    else if (mode == "sleep"){
        modeNum = 3
    }
    try {
       httpGet("http://" + deviceIp + "/cm?cmnd=TuyaSend4%205,"+ modeNum ) { resp ->
           def json = (resp.data)
           if (logEnable) log.debug "${json}"
           if (json.TuyaSend != "Done") {
               if (logEnable) log.debug "Command -ERROR- response from Device- $json"
           }
           if (json.TuyaSend == "Done") {
               if (logEnable) log.debug "Command Success response from Device"        
            }
       }   
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def setHumidity(value) {
    if (logEnable) log.debug "Sending Set Humidity, ${value} Command to [${settings.deviceIp}]"
    try {
       httpGet("http://" + deviceIp + "/cm?cmnd=TuyaSend2%202,"+ value) { resp ->
           def json = (resp.data)
           if (logEnable) log.debug "${json}"
           if (json.TuyaSend != "Done") {
               if (logEnable) log.debug "Command -ERROR- response from Device- $json"
           }
           if (json.TuyaSend == "Done") {
               if (logEnable) log.debug "Command Success response from Device"         
            }
       }   
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def ionizerOn() {
    if (logEnable) log.debug "Sending Anion On Command to [${settings.deviceIp}]"
    try {
       httpGet("http://" + deviceIp + "/cm?cmnd=TuyaSend1%2012,1") { resp ->
           def json = (resp.data)
           if (logEnable) log.debug "${json}"
           if (json.TuyaSend != "Done") {
               if (logEnable) log.debug "Command -ERROR- response from Device- $json"
           }
           if (json.TuyaSend == "Done") {
               if (logEnable) log.debug "Command Success response from Device"       
            }
       }   
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def ionizerOff() {
    if (logEnable) log.debug "Sending Anion Off Command to [${settings.deviceIp}]"
    try {
       httpGet("http://" + deviceIp + "/cm?cmnd=TuyaSend1%2012,0") { resp ->
           def json = (resp.data)
           if (logEnable) log.debug "${json}"
           if (json.TuyaSend != "Done") {
               if (logEnable) log.debug "Command -ERROR- response from Device- $json"
           }
           if (json.TuyaSend == "Done") {
               if (logEnable) log.debug "Command Success response from Device"          
            }
       }   
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def refresh() {
    if(settings.deviceIp){
        if (logEnable) log.debug "Refreshing Device Status - [${settings.deviceIp}] Cheking if device is Online"
        try {
            httpGet("http://" + deviceIp + "/cm?cmnd=status%200") { resp ->
                def json = (resp.data)
                if (json) {
                    if (logEnable) log.debug "${json}"
                }
                if (json.containsKey("StatusSTS")){
                    signal = json.StatusSTS.Wifi.Signal
                    if (logEnable) log.debug "$device.label Wifi signal strength $signal db"
                    sendEvent(name:"wifi",value:"${signal}db")
                    status1 = json.StatusSTS.POWER1 as String
                    status2 = json.StatusSTS.POWER2 as String
                    if (logEnable) log.debug "$device.label $deviceIp - $status1"
                    if (logInfo) log.info "$device.label - is $status1"
                    if (status1 == "ON"){
                       sendEvent(name:"switch",value:"on")
                    }else{
                       sendEvent(name:"switch",value:"off")
                    }
                    if (status2 == "ON"){
                       sendEvent(name:"ionizer",value:"on")
                    }else{
                       sendEvent(name:"ionizer",value:"off")
                    }
                }
                if (json.containsKey("StatusSNS")){
                    temp = json.StatusSNS.TuyaSNS.Temperature
                    if (logEnable) log.debug "$device.label temperature refresh $temp"
                    sendEvent(name:"temperature",value:"$temp")
                    hum = json.StatusSNS.TuyaSNS.Humidity
                    if (logEnable) log.debug "$device.label humidity refresh $hum"
                    sendEvent(name:"humidity",value:"$hum")
                    humSet = json.StatusSNS.TuyaSNS.HumSet
                    if (logEnable) log.debug "$device.label humidity setpoint refresh $humSet"
                    sendEvent(name:"humiditySetpoint",value:"$humSet")
               }
           }
    } catch (Exception e) {
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
