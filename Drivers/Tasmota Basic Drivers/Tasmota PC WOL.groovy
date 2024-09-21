/**
 *  Tasmota PC w/ Wake on Lan
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
 *  V1.6.0  06-01-2022       Adding pwer monitoring high/low rules. Switch is on when over High threshold off when under Low etc...
 *  V1.7.0  06-28-2022       Removed "offline, status" moved to wifi atribute and general cleanup and improvments
 *  V1.8.0  03-09-2023       Improved info logging on refresh
 *  V1.9.0  08-17-2023       Added Wake On Lan for PC power monitor on/off
 */

def driverVer() { return "1.9" }

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name: "Tasmota PC WOL", namespace: "Gassgs", author: "Gary G") {
        capability "Switch"
        capability "Actuator"
        capability "Sensor"
        capability "Refresh"
        
        command "relay", [[name:"Relay", type: "ENUM",description: "relay", constraints: ["On", "Off",]]]
        command "wake"
        
        attribute "outlet","string"
        attribute "status","string"
        attribute "wifi","string"
        
    }
}
    preferences {
        input name:"myMac", type: "text", title: "<b>MAC address of PC</b>", required: true
        input name:"myIP", type: "text", title: "<b>PC IP Address</b>", required: true
        input name:"myPort", type: "number", title: "<b>Port - </b>Default port is 7",defaultValue :"7",required: false
        input name: "deviceIp",type: "string", title: "<b>Tasmota Device IP Address</b>", required: true
        input name: "hubIp",type: "string", title: "<b>Hubitat Device IP Address</b>", required: true
        input name: "refreshEnable",type: "bool", title: "<b>Enable to Refresh every 30mins</b>", defaultValue: true
        input name: "lowValue",type: "number", title: "<b>Low Power threshold 'OFF'</b>", defaultValue: 0 , required: true
        input name: "highValue",type: "number", title: "<b>High Power threshold 'ON'</b>", defaultValue: 0 , required: true
        input name: "logInfo", type: "bool", title: "<b>Enable info logging</b>", defaultValue: true
        input name: "logEnable", type: "bool", title: "<b>Enable debug logging</b>", defaultValue: true
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
        sendEvent(name:"switch",value:"off")
        sendEvent(name:"status",value:"asleep")
    }
    if (json.contains("PowerHighON")){
        if (logEnable) log.debug "Found the word PowerHighON"
        if (logInfo) log.info "$device.label - Power is above High threshold"
        sendEvent(name:"switch",value:"on")
        sendEvent(name:"status",value:"awake")
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

def on() {
    def secureOn = mySecureOn ?: "000000000000"
    def port = myPort ?: 7
    def ip = myIP ?: "255.255.255.255"
    def macHEX = myMac.replaceAll("-","").replaceAll(":","").replaceAll(" ","")
    def command = "FFFFFFFFFFFF$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$macHEX$secureOn"
    def myHubAction = new hubitat.device.HubAction(command, 
                           hubitat.device.Protocol.LAN, 
                           [type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT, 
                            destinationAddress: "$ip:$port",
                            encoding: hubitat.device.HubAction.Encoding.HEX_STRING])
    sendHubCommand(myHubAction)
    log.info "Sent WOL to $myMac"
}

def wake(){
    on()
}

def off(){
    sendEvent(name:"error",value:"PC will sleep on it's own")
}

def refresh() {
    deviceStatus = device.currentValue("switch").toUpperCase()
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
                   if (logInfo) log.info "Relay is $status - $device.label is $deviceStatus"
                   }   
                   if (status == "ON"){
                       sendEvent(name:"outlet",value:"on")
                   }else{
                       sendEvent(name:"outlet",value:"off")
                   }
           }
        }catch (Exception e) {
            sendEvent(name:"wifi",value:"offline")
            if (logInfo) log.error "$device.label Unable to connect, device is <b>OFFLINE</b>"
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
