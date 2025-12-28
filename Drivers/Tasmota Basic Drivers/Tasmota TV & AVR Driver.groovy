/**
 *  Tasmota IR and S31 Power monitoring for TV and AVR Control and Power Status
 *
 *  Virtual Driver for Television and AVR
 *  Commands sent from tasmota IR & on /off based on Tasmota power reporting
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
 *    V1.0  08-10-2021   -       first run - Gassgs
 *    V1.1  11-01-2021   -       consolidated attribute for all commands
 *    V1.2  05-18-2022   -       Added motion capability
 *    V1.3  06-15-2022   -       Renamed, added tasmota power monitoring w/ Sonoff S31 into driver
 *    V1.4  06-25-2022   -       added tasmota IR into driver, no need for an app anymore
 *    V1.5  06-26-2022   -       Added version for AVR and TV control in one driver
 *    V1.6  06-28-2022   -       Removed "offline, status" moved to wifi atribute and general cleanup and improvments
 *    V1.7  03-09-2023   -       Improved info logging on refresh
 *    V1.8.0  05-29-2025         Added networkStatus attibute and changed "wifi" to "rssi" w/ capability 'Signal Strength'
 *  
 */

def driverVer() { return "1.8" }

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name:"Tasmota TV & AVR", namespace: "Gassgs", author: "Gary G") {
        capability "Actuator"
        capability "AudioVolume"
        capability "Switch"
        capability "Sensor"
        capability "MotionSensor"
        capability "Refresh"
        capability "Signal Strength"

        command"hdmi1"
        command"hdmi2"
        command "relay", [[name:"Relay", type: "ENUM",description: "relay", constraints: ["On", "Off",]]]
        command "toggle"
        
        attribute"outlet","string"
        attribute "networkStatus","string"
    }
   
}
    preferences {
        input name: "deviceIp",type: "string", title: "<b>Tasmota S31 Device IP Address</b>", required: true
        input name: "hubIp",type: "string", title: "<b>Hubitat Hub IP Address</b>", required: true
        input name: "irDeviceIp",type: "string", title: "<b>Tasmota IR Device IP Address</b>", required: true
        input( "deviceBrand","enum", options:["JVC","Samsung","Toshiba"], title: "<b>Device Brand</b>", defaultValue: "JVC")
        input name: "lowValue",type: "number", title: "<b>Low Power threshold 'OFF'</b>", defaultValue: 0 , required: true
        input name: "highValue",type: "number", title: "<b>High Power threshold 'ON'</b>", defaultValue: 0 , required: true
        input name: "refreshEnable",type: "bool", title: "<b>Enable to Refresh every 30mins</b>", defaultValue: true
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
    sendEvent(name:"volume",value:"50")
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
        state.powerOn = false
        sendEvent(name:"switch",value:"off")
        sendEvent(name:"motion",value:"inactive")
    }
    if (json.contains("PowerHighON")){
        if (logEnable) log.debug "Found the word PowerHighON"
        if (logInfo) log.info "$device.label - Power is above High threshold"
        state.powerOn = true
        sendEvent(name:"switch",value:"on")
        sendEvent(name:"motion",value:"active")
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
                   sendEvent(name:"rssi",value:"${signal}")
                   sendEvent(name:"networkStatus",value:"online")
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
            sendEvent(name:"networkStatus",value:"offline")
            if (logInfo) log.error "$device.label Unable to connect, device is <b>OFFLINE</b>"
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
       
def on(){
    if (logInfo) log.info "$device.label - Power on"
    if (!state.powerOn){
        if (deviceBrand == "JVC"){
        cmd = '{"Protocol":"JVC","Bits":16,"Data":"0xC5BC","DataLSB":"0xA33D","Repeat":1}'
        }
        if (deviceBrand == "Samsung"){
        cmd = '{"Protocol":"SAMSUNG","Bits":32,"Data":"0xE0E040BF","DataLSB":"0x70702FD","Repeat":0}'
        }
        if (deviceBrand == "Toshiba"){
        cmd = '{"Protocol":"NEC","Bits":32,"Data":"0x2FD48B7","DataLSB":"0x40BF12ED","Repeat":0}'
        }
        sendIr(cmd)
    }else{
        if (logInfo) log.info "$device.label - Power is already on"
    }
}

def off(){
    if (logInfo) log.info "$device.label - Power off"
    if (state.powerOn){
        if (deviceBrand == "JVC"){
        cmd = '{"Protocol":"JVC","Bits":16,"Data":"0xC5E8","DataLSB":"0xA317","Repeat":1}'
        }
        if (deviceBrand == "Samsung"){
        cmd = '{"Protocol":"SAMSUNG","Bits":32,"Data":"0xE0E040BF","DataLSB":"0x70702FD","Repeat":0}'
        }
        if (deviceBrand == "Toshiba"){
        cmd = '{"Protocol":"NEC","Bits":32,"Data":"0x2FD48B7","DataLSB":"0x40BF12ED","Repeat":0}'
        }
        sendIr(cmd)
    }else{
        if (logInfo) log.info "$device.label - Power is already off"
    }
}

def volumeDown(){
    if (logInfo) log.info "$device.label - Volume down"
    if (state.powerOn){
        if (deviceBrand == "JVC"){
            cmd = '{"Protocol":"JVC","Bits":16,"Data":"0xC5F8","DataLSB":"0xA31F","Repeat":1}'
        }
        if (deviceBrand == "Samsung"){
            cmd = '{"Protocol":"SAMSUNG","Bits":32,"Data":"0xE0E0D02F","DataLSB":"0x7070BF4","Repeat":0}'
        }
        if (deviceBrand == "Toshiba"){
            cmd = '{"Protocol":"SAMSUNG","Bits":32,"Data":"0x34346897","DataLSB":"0x2C2C16E9","Repeat":0}'
        }
        sendIr(cmd)
    }else{
        if (logInfo) log.info "$device.label - Power is off, command not sent"
    }
}

def volumeUp(){
    if (state.powerOn){
        if (logInfo) log.info "$device.label - Volume up"
        if (deviceBrand == "JVC"){
            cmd = '{"Protocol":"JVC","Bits":16,"Data":"0xC578","DataLSB":"0xA31E","Repeat":1}'
        }
        if (deviceBrand == "Samsung"){
            cmd = '{"Protocol":"SAMSUNG","Bits":32,"Data":"0xE0E0E01F","DataLSB":"0x70707F8","Repeat":0}'
        }
        if (deviceBrand == "Toshiba"){
            cmd = '{"Protocol":"SAMSUNG","Bits":32,"Data":"0x3434E817","DataLSB":"0x2C2C17E8","Repeat":0}'
        }
        sendIr(cmd)
    }else{
        if (logInfo) log.info "$device.label - Power is off, command not sent"
    }
}

def hdmi1(){
    if (state.powerOn){
        if (deviceBrand == "Samsung"){
            if (logInfo) log.info "$device.label - HDMI 1"
            cmd = '{"Protocol":"SAMSUNG","Bits":32,"Data":"0xE0E09768","DataLSB":"0x707E916","Repeat":0}'
        }else{
            if (logInfo) log.info "$device.label - command not supported"
        }
        sendIr(cmd)
    }else{
        if (logInfo) log.info "$device.label - Power is off, command not sent"
    }
}

def hdmi2(){
    if (state.powerOn){
        if (deviceBrand == "Samsung"){
            if (logInfo) log.info "$device.label - HDMI 2"
            cmd = '{"Protocol":"SAMSUNG","Bits":32,"Data":"0xE0E07D82","DataLSB":"0x707BE41","Repeat":0}'
        }else{
            if (logInfo) log.info "$device.label - command not supported"
        }
        sendIr(cmd)
    }else{
        if (logInfo) log.info "$device.label - Power is off, command not sent"
    }
}

def mute(){
    if (state.powerOn){
        if (logInfo) log.info "$device.label - mute"
        if (deviceBrand == "JVC"){
            cmd = '{"Protocol":"JVC","Bits":16,"Data":"0xC538","DataLSB":"0xA31C","Repeat":1}'
        }
        if (deviceBrand == "Samsung"){
            cmd = '{"Protocol":"SAMSUNG","Bits":32,"Data":"0xE0E0F00F","DataLSB":"0x7070FF0","Repeat":0}'
        }
        if (deviceBrand == "Toshiba"){
            cmd = '{"Protocol":"SAMSUNG","Bits":32,"Data":"0x3434F807","DataLSB":"0x2C2C1FE0","Repeat":0}'
        }
        sendIr(cmd)
    }else{
        if (logInfo) log.info "$device.label - Power is off, command not sent"
    }
}

def unmute(){
    if (state.powerOn){
        if (logInfo) log.info "$device.label - unmute"
        if (deviceBrand == "JVC"){
            cmd = '{"Protocol":"JVC","Bits":16,"Data":"0xC538","DataLSB":"0xA31C","Repeat":1}'
        }
        if (deviceBrand == "Samsung"){
            cmd = '{"Protocol":"SAMSUNG","Bits":32,"Data":"0xE0E0F00F","DataLSB":"0x7070FF0","Repeat":0}'
        }
        if (deviceBrand == "Toshiba"){
            cmd = '{"Protocol":"SAMSUNG","Bits":32,"Data":"0x3434F807","DataLSB":"0x2C2C1FE0","Repeat":0}'
        }
        sendIr(cmd)
    }else{
        if (logInfo) log.info "$device.label - Power is off, command not sent"
    }
}

def setVolume(value){
    if (value > 50){
        volumeUp()
    }else{
        volumeDown()
    }
}

def sendIr(cmd){
        if(settings.irDeviceIp){
        if (logEnable) log.debug "Sending IR command Device Status - $cmd"

        try {
           httpGet("http://" + irDeviceIp + "/cm?cmnd=irsend%20"+ URLEncoder.encode(cmd, "UTF-8").replaceAll(/\+/,'%20')) { resp ->
           def json = (resp.data)
               if (logEnable) log.debug "${json}"
               if (json.IRSend == "Done"){
                   sendEvent(name:"networkStatus",value:"online")
                   if (logEnable) log.debug "Command Success response from Device"
               }else{
                   if (logEnable) log.debug "Command -ERROR- response from Device- $json"
               }
           }
        }catch (Exception e) {
            sendEvent(name:"networkStatus",value:"offline")
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
