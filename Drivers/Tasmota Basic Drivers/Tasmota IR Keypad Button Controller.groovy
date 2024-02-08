/**
 *  Tasmota IR Keypad Button Controller
 *  For use with Sofabaton U1 & U2 remotes // digit keys as keypad input.
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
 *  V1.2.0  01-29-2024       New use case(security keypad for sofabaton U2)
 */

def driverVer() { return "1.2" }

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name: "Tasmota IR Keypad Button Controller", namespace: "Gassgs", author: "Gary G") {
        capability "PushableButton"
        capability "Actuator"
        capability "Refresh"
        capability "Sensor"
        capability "ShockSensor"
        
        command "push",[[name:"Push Value",type:"STRING"]]
        
        attribute "code","string"
        attribute "status","string"
        attribute "wifi","string"
    }
}
    preferences {
        input name: "deviceIp",type: "string", title: "<b>Tasmota IR Device IP Address</b>", required: true
        input name: "hubIp",type: "string", title: "<b>Hubitat Hub IP Address</b>", required: true
        input name: "refreshEnable",type: "bool", title: "<b>Enable to Refresh every 30mins</b>", defaultValue: true
        input name: "button0",type: "string", title: "<b>Button 0 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button1",type: "string", title: "<b>Button 1 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button2",type: "string", title: "<b>Button 2 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button3",type: "string", title: "<b>Button 3 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button4",type: "string", title: "<b>Button 4 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button5",type: "string", title: "<b>Button 5 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button6",type: "string", title: "<b>Button 6 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button7",type: "string", title: "<b>Button 7 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button8",type: "string", title: "<b>Button 8 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button9",type: "string", title: "<b>Button 9 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button11",type: "string", title: "<b>armHome IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button12",type: "string", title: "<b>armNight IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button13",type: "string", title: "<b>Button 13 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button14",type: "string", title: "<b>Button 14 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button15",type: "string", title: "<b>Button 15 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button16",type: "string", title: "<b>Button 16 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button17",type: "string", title: "<b>Button 17 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "button18",type: "string", title: "<b>Button 18 IR Data Value</b>", required: false, defaultValue: "0"
        input name: "securityCode", type: "STRING", title: "<b>4 digit Security Code</b>", defaultValue: "0000"
        input name: "panicCode", type: "STRING", title: "<b>4 digit Panic Code</b>", defaultValue: "0911"
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
    sendEvent(name:"numberOfButtons",value:18)
    sendEvent(name:"status",value:"clear",isStateChange: true)
    sendEvent(name:"code",value:"clear",isStateChange: true)
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
            rule = "ON IrReceived#Data=" + button0 +" DO webquery http://"+ hubIp + ":39501/ POST 0 ENDON " +
                "ON IrReceived#Data=" + button1 +" DO webquery http://"+ hubIp + ":39501/ POST 1 ENDON " +
                "ON IrReceived#Data=" + button2 +" DO webquery http://"+ hubIp + ":39501/ POST 2 ENDON " +
                "ON IrReceived#Data=" + button3 +" DO webquery http://"+ hubIp + ":39501/ POST 3 ENDON " +
                "ON IrReceived#Data=" + button4 +" DO webquery http://"+ hubIp + ":39501/ POST 4 ENDON " +
                "ON IrReceived#Data=" + button5 +" DO webquery http://"+ hubIp + ":39501/ POST 5 ENDON " 
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
    runIn(1,syncSetup2)
}

def syncSetup2(){
        if (hubIp){
            rule = "ON IrReceived#Data=" + button6 +" DO webquery http://"+ hubIp + ":39501/ POST 6 ENDON " +
                "ON IrReceived#Data=" + button7 +" DO webquery http://"+ hubIp + ":39501/ POST 7 ENDON " +
                "ON IrReceived#Data=" + button8 +" DO webquery http://"+ hubIp + ":39501/ POST 8 ENDON " +
                "ON IrReceived#Data=" + button9 +" DO webquery http://"+ hubIp + ":39501/ POST 9 ENDON " +
                "ON IrReceived#Data=" + button11 +" DO webquery http://"+ hubIp + ":39501/ POST armHome ENDON " +
                "ON IrReceived#Data=" + button12 +" DO webquery http://"+ hubIp + ":39501/ POST armNight ENDON "
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
    runIn(1,syncSetup3)
}

def syncSetup3(){
        if (hubIp){
            rule = "ON IrReceived#Data=" + button13 +" DO webquery http://"+ hubIp + ":39501/ POST 13 ENDON " +
                "ON IrReceived#Data=" + button14 +" DO webquery http://"+ hubIp + ":39501/ POST 14 ENDON " +
                "ON IrReceived#Data=" + button15 +" DO webquery http://"+ hubIp + ":39501/ POST 15 ENDON " +
                "ON IrReceived#Data=" + button16 +" DO webquery http://"+ hubIp + ":39501/ POST 16 ENDON " +
                "ON IrReceived#Data=" + button17 +" DO webquery http://"+ hubIp + ":39501/ POST 17 ENDON " +
                "ON IrReceived#Data=" + button18 +" DO webquery http://"+ hubIp + ":39501/ POST 18 ENDON "
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
    runIn(1,turnOnRule3)
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
       if (json == "armHome"){
        sendEvent(name:"status",value:"armHome",isStateChange: true)
        runIn(6,clearStatus)
    }
    else if (json == "armNight"){
        sendEvent(name:"status",value:"armNight",isStateChange: true)
        runIn(6,clearStatus)
    }
    else{
        sendEvent(name:"pushed",value:"$json",isStateChange: true)
        setCode("$json")
    }   
}

def push(value){
    if (logEnable) log.debug "data is ${value}"
    if (logInfo) log.info "$device.label - Button $value Pushed"
    if (value == "armHome"){
        sendEvent(name:"status",value:"armHome",isStateChange: true)
        runIn(6,clearStatus)
    }
    else if (value == "armNight"){
        sendEvent(name:"status",value:"armNight",isStateChange: true)
        runIn(6,clearStatus)
    }
    else{
        sendEvent(name:"pushed",value:"$value",isStateChange: true)
        setCode("$value")
    }   
}

def setCode(value){
    if (value == "0"||value == "1"||value == "2"||value == "3"||value == "4"||value == "5"||value == "6"||value == "7"||value == "8"||value == "9"){
        unschedule(clearCode)
        if (state.code == null){
            state.code = value
        }else{
            state.code = state.code+""+value
        }
        sendEvent(name:"code",value:"$state.code",isStateChange: true)
        if (state.code == settings.securityCode){
            checkArm()
        }
        if (state.code == settings.panicCode){
            shock()
        }
        runIn(3,clearCode)
    }
}
    
def clearStatus(){
    sendEvent(name:"status",value:"clear",isStateChange: true)
}

def clearCode(){
    sendEvent(name:"code",value:"clear",isStateChange: true)
    state.code = null
}

def checkArm(){
    status = device.currentValue("status")
    if (status == "armHome"){
        if (logInfo) log.info "$device.label sending Arm Home command"
        sendLocationEvent(name: "hsmSetArm", value: "armHome")
    }
    else if (status == "armNight"){
        if (logInfo) log.info "$device.label sending Arm Night command"
        sendLocationEvent(name: "hsmSetArm", value: "armNight")
    }
    else{
        if (logInfo) log.info "$device.label sending Disarm/clearAlerts commands"
        sendLocationEvent(name: "hsmSetArm", value: "disarm")
        sendLocationEvent(name: "hsmSetArm", value: "cancelAlerts")
    }
}

def shock(){
    if (logInfo) log.info "$device.label Panic Alert triggered"
    sendEvent(name:"shock",value:"detected")
    runIn(2,clearPanic)
}

def clearPanic(){
    sendEvent(name:"shock",value:"clear")
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
    sendEvent(name:"status",value:"clear",isStateChange: true)
    sendEvent(name:"code",value:"clear",isStateChange: true)
    if (logEnable) runIn(1800, logsOff)
}
