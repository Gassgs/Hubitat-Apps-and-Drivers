/**
 *  Tasmota IR Button Controller
 *  For use with Sofabaton X1 and other Universal IR remotes
 *  
 *  Copyright 2024 Gassgs
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
 *  V1.0.0  01-17-2024       first run
 *  V1.1.0  01-27-2024       cleanup added push cmd method
 *  V1.2.0  02-05-2024       expanded to support up to 30 codes
 *  V1.3.0  02-07-2024       expanded to support up to 40 codes (needed more)
 */

def driverVer() { return "1.3" }

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name: "Tasmota IR Button Controller", namespace: "Gassgs", author: "Gary G") {
        capability "PushableButton"
        capability "Actuator"
        capability "Refresh"
        capability "Sensor"
        
        attribute "wifi","string"
    }
}
    preferences {
        input name: "deviceIp",type: "string", title: "<b>Tasmota IR Device IP Address</b>", required: true
        input name: "hubIp",type: "string", title: "<b>Hubitat Hub IP Address</b>", required: true
        input (name: "numberOfButtons", type: "enum", title: "<b>Number Of Buttons</b>", defaultValue: "10", options: ["10", "20", "30","40"])
        input name: "button1",type: "string", title: "<b>Button 1 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button2",type: "string", title: "<b>Button 2 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button3",type: "string", title: "<b>Button 3 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button4",type: "string", title: "<b>Button 4 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button5",type: "string", title: "<b>Button 5 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button6",type: "string", title: "<b>Button 6 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button7",type: "string", title: "<b>Button 7 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button8",type: "string", title: "<b>Button 8 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button9",type: "string", title: "<b>Button 9 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button10",type: "string", title: "<b>Button 10 IR Data Value</b>", required: false, defaultValue: "0"
        if (numberOfButtons == "20" || numberOfButtons == "30" || numberOfButtons == "40"){
            input name: "button11",type: "string", title: "<b>Button 11 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button12",type: "string", title: "<b>Button 12 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button13",type: "string", title: "<b>Button 13 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button14",type: "string", title: "<b>Button 14 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button15",type: "string", title: "<b>Button 15 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button16",type: "string", title: "<b>Button 16 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button17",type: "string", title: "<b>Button 17 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button18",type: "string", title: "<b>Button 18 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button19",type: "string", title: "<b>Button 19 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button20",type: "string", title: "<b>Button 20 IR Data Value</b>", required: false, defaultValue: "0"
        }
        if (numberOfButtons == "30" || numberOfButtons == "40"){
            input name: "button21",type: "string", title: "<b>Button 21 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button22",type: "string", title: "<b>Button 22 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button23",type: "string", title: "<b>Button 23 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button24",type: "string", title: "<b>Button 24 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button25",type: "string", title: "<b>Button 25 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button26",type: "string", title: "<b>Button 26 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button27",type: "string", title: "<b>Button 27 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button28",type: "string", title: "<b>Button 28 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button29",type: "string", title: "<b>Button 29 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button30",type: "string", title: "<b>Button 30 IR Data Value</b>", required: false, defaultValue: "0"
        }
        if (numberOfButtons == "40"){
            input name: "button31",type: "string", title: "<b>Button 31 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button32",type: "string", title: "<b>Button 32 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button33",type: "string", title: "<b>Button 33 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button34",type: "string", title: "<b>Button 34 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button35",type: "string", title: "<b>Button 35 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button36",type: "string", title: "<b>Button 36 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button37",type: "string", title: "<b>Button 37 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button38",type: "string", title: "<b>Button 38 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button39",type: "string", title: "<b>Button 39 IR Data Value</b>", required: false, defaultValue: "0"
            input name: "button40",type: "string", title: "<b>Button 40 IR Data Value</b>", required: false, defaultValue: "0"
        }
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
    if (logEnable) runIn(1800, logsOff)
    sendEvent(name:"numberOfButtons",value:"$numberOfButtons")
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
            rule = "ON IrReceived#Data=" + button1 +" DO webquery http://"+ hubIp + ":39501/ POST 1 ENDON " +
                "ON IrReceived#Data=" + button2 +" DO webquery http://"+ hubIp + ":39501/ POST 2 ENDON " +
                "ON IrReceived#Data=" + button3 +" DO webquery http://"+ hubIp + ":39501/ POST 3 ENDON " +
                "ON IrReceived#Data=" + button4 +" DO webquery http://"+ hubIp + ":39501/ POST 4 ENDON " +
                "ON IrReceived#Data=" + button5 +" DO webquery http://"+ hubIp + ":39501/ POST 5 ENDON " +
                "ON IrReceived#Data=" + button6 +" DO webquery http://"+ hubIp + ":39501/ POST 6 ENDON " +
                "ON IrReceived#Data=" + button7 +" DO webquery http://"+ hubIp + ":39501/ POST 7 ENDON " +
                "ON IrReceived#Data=" + button8 +" DO webquery http://"+ hubIp + ":39501/ POST 8 ENDON " +
                "ON IrReceived#Data=" + button9 +" DO webquery http://"+ hubIp + ":39501/ POST 9 ENDON " +
                "ON IrReceived#Data=" + button10 +" DO webquery http://"+ hubIp + ":39501/ POST 10 ENDON " +
                "ON IrReceived#Data=" + button11 +" DO webquery http://"+ hubIp + ":39501/ POST 11 ENDON " +
                "ON IrReceived#Data=" + button12 +" DO webquery http://"+ hubIp + ":39501/ POST 12 ENDON " +
                "ON IrReceived#Data=" + button13 +" DO webquery http://"+ hubIp + ":39501/ POST 13 ENDON " 
            ruleNow = rule.replaceAll("%","%25").replaceAll("#","%23").replaceAll("/","%2F").replaceAll(":","%3A").replaceAll(" ","%20")
            if (logEnable) log.debug "$ruleNow"              
        try {
            httpGet("http://" + deviceIp + "/cm?cmnd=RULE1%20${ruleNow}") { resp ->
                def json = (resp.data) 
                if (json){
                    if (logEnable) log.debug "Command Success response from Device - Setup Rule 1"
                }else{
                    if (logEnable) log.debug "Command -ERROR- response from Device- $json"
                }
            }
        } catch (Exception e) {
            log.warn "Call to on failed: ${e.message}"
        }
    }
    if (numberOfButtons == "20" || numberOfButtons == "30"|| numberOfButtons == "40"){
        runIn(1,syncSetup2)
    }
    else{
        runIn(2,turnOnRule)
    }
}

def syncSetup2(){
        if (hubIp){
            rule = "ON IrReceived#Data=" + button14 +" DO webquery http://"+ hubIp + ":39501/ POST 14 ENDON " +
                "ON IrReceived#Data=" + button15 +" DO webquery http://"+ hubIp + ":39501/ POST 15 ENDON " +
                "ON IrReceived#Data=" + button16 +" DO webquery http://"+ hubIp + ":39501/ POST 16 ENDON " +
                "ON IrReceived#Data=" + button17 +" DO webquery http://"+ hubIp + ":39501/ POST 17 ENDON " +
                "ON IrReceived#Data=" + button18 +" DO webquery http://"+ hubIp + ":39501/ POST 18 ENDON " +
                "ON IrReceived#Data=" + button19 +" DO webquery http://"+ hubIp + ":39501/ POST 19 ENDON " +
                "ON IrReceived#Data=" + button20 +" DO webquery http://"+ hubIp + ":39501/ POST 20 ENDON " +
                "ON IrReceived#Data=" + button21 +" DO webquery http://"+ hubIp + ":39501/ POST 21 ENDON " +
                "ON IrReceived#Data=" + button22 +" DO webquery http://"+ hubIp + ":39501/ POST 22 ENDON " +
                "ON IrReceived#Data=" + button23 +" DO webquery http://"+ hubIp + ":39501/ POST 23 ENDON " +
                "ON IrReceived#Data=" + button24 +" DO webquery http://"+ hubIp + ":39501/ POST 24 ENDON " +
                "ON IrReceived#Data=" + button25 +" DO webquery http://"+ hubIp + ":39501/ POST 25 ENDON " +
                "ON IrReceived#Data=" + button26 +" DO webquery http://"+ hubIp + ":39501/ POST 26 ENDON "
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
    if (numberOfButtons == "30" || numberOfButtons == "40"){
        runIn(1,syncSetup3)
    }
    else{
        runIn(2,turnOnRule)
    }
}

def syncSetup3(){
        if (hubIp){
            rule = "ON IrReceived#Data=" + button27 +" DO webquery http://"+ hubIp + ":39501/ POST 27 ENDON " +
                "ON IrReceived#Data=" + button28 +" DO webquery http://"+ hubIp + ":39501/ POST 28 ENDON " +
                "ON IrReceived#Data=" + button29 +" DO webquery http://"+ hubIp + ":39501/ POST 29 ENDON " +
                "ON IrReceived#Data=" + button30 +" DO webquery http://"+ hubIp + ":39501/ POST 30 ENDON " +
                "ON IrReceived#Data=" + button31 +" DO webquery http://"+ hubIp + ":39501/ POST 31 ENDON " +
                "ON IrReceived#Data=" + button32 +" DO webquery http://"+ hubIp + ":39501/ POST 32 ENDON " +
                "ON IrReceived#Data=" + button33 +" DO webquery http://"+ hubIp + ":39501/ POST 33 ENDON " +
                "ON IrReceived#Data=" + button34 +" DO webquery http://"+ hubIp + ":39501/ POST 34 ENDON " +
                "ON IrReceived#Data=" + button35 +" DO webquery http://"+ hubIp + ":39501/ POST 35 ENDON " +
                "ON IrReceived#Data=" + button36 +" DO webquery http://"+ hubIp + ":39501/ POST 36 ENDON " +
                "ON IrReceived#Data=" + button37 +" DO webquery http://"+ hubIp + ":39501/ POST 37 ENDON " +
                "ON IrReceived#Data=" + button38 +" DO webquery http://"+ hubIp + ":39501/ POST 38 ENDON " +
                "ON IrReceived#Data=" + button39 +" DO webquery http://"+ hubIp + ":39501/ POST 39 ENDON " +
                "ON IrReceived#Data=" + button40 +" DO webquery http://"+ hubIp + ":39501/ POST 40 ENDON "
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
         httpGet("http://" + deviceIp + "/cm?cmnd=RULE1%20ON") { resp ->
             def json = (resp.data) 
             if (json){
                 if (logEnable) log.debug "Command Success response from Device - Rule 1 activated"
             }else{
                 if (logEnable) log.debug "Command -ERROR- response from Device- $json"
             }
         }
     } catch (Exception e) {
         log.warn "Call to on failed: ${e.message}"
    }
    if (numberOfButtons == "20" || numberOfButtons == "30" || numberOfButtons == "40"){
        runIn(1,turnOnRule2)
    }
    else{
        refresh()
    }
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
    if (numberOfButtons == "30" || numberOfButtons == "40"){
        runIn(1,turnOnRule3)
    }
    else{
        refresh()
    }
}

def turnOnRule3(){
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
    if (logEnable) log.debug "Value == ${json}"
    if (logInfo) log.info "$device.label - Button $json Pushed"
    sendEvent(name:"pushed",value:"${json}",isStateChange: true)
}

def push(value){
    if (logEnable) log.debug "data is ${value}"
    if (logInfo) log.info "$device.label - Button $value Pushed"
    sendEvent(name:"pushed",value:"$value",isStateChange: true)
}

def refresh() {
    if(settings.deviceIp){
        if (logEnable) log.debug "Refreshing Device Status - [${settings.deviceIp}]"
        try {
           httpGet("http://" + deviceIp + "/cm?cmnd=status%2011") { resp ->
           def json = (resp.data)
            if (logEnable) log.debug "${json}"
               if (json.containsKey("StatusSTS")){
                   signal = json.StatusSTS.Wifi.Signal as String
                   if (logEnable) log.debug "Wifi signal strength $signal db"
                   sendEvent(name:"wifi",value:"${signal}db")
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
