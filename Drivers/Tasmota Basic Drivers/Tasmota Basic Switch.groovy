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
 *  V1.0.0  7-02-2021       first run   
 *  V1.1.0  7-17-2021       Refresh schedule improvements
 *  V1.2.0  8-15-2021       Added option to pause refreshing
 */

def driverVer() { return "1.1" }

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
        def refreshRate = [:]
		refreshRate << ["1 min" : "Refresh every minute"]
		refreshRate << ["5 min" : "Refresh every 5 minutes"]
        refreshRate << ["10 min" : "Refresh every 10 minutes"]
		refreshRate << ["15 min" : "Refresh every 15 minutes"]
		refreshRate << ["30 min" : "Refresh every 30 minutes"]
        input name: "deviceIp",type: "string", title: "Tasmota Device IP Address", required: true
        input ("refresh_Rate", "enum", title: "Device Refresh Rate", options: refreshRate, defaultValue: "15 min")
        input name: "plugNum",type: "enum",title: "Plug Number", options:["0","1","2","3","4"], defaultValue: 0
        input name: "paused", type: "bool", title: "Disable Device Check", defaultValue: false
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
    
    switch(refresh_Rate) {
		case "1 min" :
        if(!paused){
			runEvery1Minute(refresh)
            if (logEnable) log.debug "refresh every minute schedule"
        }
			break
		case "5 min" :
        if(!paused){
			runEvery5Minutes(refresh)
            if (logEnable) log.debug "refresh every 5 minutes schedule"
        }
			break
        case "10 min" :
        if(!paused){
			runEvery10Minutes(refresh)
            if (logEnable) log.debug "refresh every 10 minutes schedule"
        }
			break
		case "15 min" :
        if(!paused){
			runEvery15Minutes(refresh)
            if (logEnable) log.debug "refresh every 15 minutes schedule"
        }
			break
		case "30 min" :
        if(!paused){
			runEvery30Minutes(refresh)
            if (logEnable) log.debug "refresh every 30 minutes schedule"
        }
	}
    if (paused){
        log.warn "device is paused and will not be refreshed"
    }
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
    }
} 


def installed() {
    log.info "installed..."
    log.warn "debug logging is: ${logEnable == true}"
    state.DriverVersion=driverVer()
    if (logEnable) runIn(1800, logsOff)
}
