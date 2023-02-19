/**
 *  Tasmota Powerstrip
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
 *  V1.6.0  06-03-2022       Addding child devices for Powerstrip
 *  V1.7.0  06-22-2022       Addding outlet controls from parent device
 *  V1.8.0  06-28-2022       Removed "offline, status" moved to wifi atribute and general cleanup and improvments
 */

def driverVer() { return "1.8" }

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name: "Tasmota Powerstrip", namespace: "Gassgs", author: "Gary G") {
        capability "Actuator"
        capability "Refresh"
        capability "Sensor"
        
        command "outlet1", [[name:"Relay", type: "ENUM",description: "relay", constraints: ["On", "Off",]]]
        command "outlet2", [[name:"Relay", type: "ENUM",description: "relay", constraints: ["On", "Off",]]]
        command "outlet3", [[name:"Relay", type: "ENUM",description: "relay", constraints: ["On", "Off",]]]
        command "outlet4", [[name:"Relay", type: "ENUM",description: "relay", constraints: ["On", "Off",]]]
        
        attribute "switch1","string"
        attribute "switch2","string"
        attribute "switch3","string"
        attribute "switch4","string"
        attribute "wifi","string"  
    }
}
    preferences {
        input name: "deviceIp",type: "string", title: "<b>Tasmota Device IP Address</b>", required: true
        input name: "hubIp",type: "string", title: "<b>Hubitat Device IP Address</b>", required: true
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
    runIn(1,createChild1)
}


def createChild1() {
    def childDevice = getChildDevices()?.find {it.data.componentLabel == "Switch"}
    if (!childDevice) {
        for (i in 1..4) {
            childDevice = addChildDevice ("Gassgs", "Tasmota Child Switch", "${device.deviceNetworkId}-${i}",[label: "${device.displayName} Relay ${i}", isComponent: false, componentLabel: "Switch"])
        }
        }else{
            if (logInfo) log.info "$device.label Switch Child already exists"   
	}
}

def deleteChildren() {
	if (logInfo) log.info "Deleting children"
	def children = getChildDevices()
    children.each {child->
  		deleteChildDevice(child.deviceNetworkId)
    }
}

def syncSetup(){
        if (hubIp){
            rule = "ON Power1#state DO webquery http://"+ hubIp + ":39501/ POST SwitchOne%value% ENDON " +
                "ON Power2#state DO webquery http://"+ hubIp + ":39501/ POST SwitchTwo%value% ENDON " +
                "ON Power3#state DO webquery http://"+ hubIp + ":39501/ POST SwitchThree%value% ENDON " +
                "ON Power4#state DO webquery http://"+ hubIp + ":39501/ POST SwitchFour%value% ENDON "
            
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
    if (json.contains("SwitchOne")){
        def childDevice1 = childDevices.find{it.deviceNetworkId == "$device.deviceNetworkId-1"}          
        if (logEnable) log.debug "Found the word SwitchOne"
        if (json.contains("1")){
            if (logEnable) log.debug "Found the value 1"
            if (logInfo) log.info "$device.label - Switch 1 is On"
            sendEvent(name:"switch1",value:"on")
            if (childDevice1) {
                childDevice1.sendEvent(name: "switch", value:"on")
                childDevice1.sendEvent(name: "motion", value:"active")
            }
        }
        else if (json.contains("0")){
            if (logEnable) log.debug "Found the value 0"
            if (logInfo) log.info "$device.label - Switch 1 is Off"
            sendEvent(name:"switch1",value:"off")
            if (childDevice1) {
                childDevice1.sendEvent(name: "switch", value:"off")
                childDevice1.sendEvent(name: "motion", value:"inactive")
            }
        }
    }
    if (json.contains("SwitchTwo")){
        def childDevice2 = childDevices.find{it.deviceNetworkId == "$device.deviceNetworkId-2"}
        if (logEnable) log.debug "Found the word SwitchTwo"
        if (json.contains("1")){
            if (logEnable) log.debug "Found the value 1"
            if (logInfo) log.info "$device.label - Switch 2 is On"
            sendEvent(name:"switch2",value:"on")
            if (childDevice2) {
                childDevice2.sendEvent(name: "switch", value:"on")
                childDevice2.sendEvent(name: "motion", value:"active")
            }
        }
        else if (json.contains("0")){
            if (logEnable) log.debug "Found the value 0"
            if (logInfo) log.info "$device.label - Switch 2 is Off"
            sendEvent(name:"switch2",value:"off")
            if (childDevice2) {
                childDevice2.sendEvent(name: "switch", value:"off")
                childDevice2.sendEvent(name: "motion", value:"inactive")
            }
        }
    }
    if (json.contains("SwitchThree")){
        def childDevice3 = childDevices.find{it.deviceNetworkId == "$device.deviceNetworkId-3"}          
        if (logEnable) log.debug "Found the word SwitchThree"
        if (json.contains("1")){
            if (logEnable) log.debug "Found the value 1"
            if (logInfo) log.info "$device.label - Switch 3 is On"
            sendEvent(name:"switch3",value:"on")
            if (childDevice3) {
                childDevice3.sendEvent(name: "switch", value:"on")
                childDevice3.sendEvent(name: "motion", value:"active")
            }
        }
        else if (json.contains("0")){
            if (logEnable) log.debug "Found the value 0"
            if (logInfo) log.info "$device.label - Switch 3 is Off"
            sendEvent(name:"switch3",value:"off")
            if (childDevice3) {
                childDevice3.sendEvent(name: "switch", value:"off")
                childDevice3.sendEvent(name: "motion", value:"inactive")
            }
        }
    }
    if (json.contains("SwitchFour")){
        def childDevice4 = childDevices.find{it.deviceNetworkId == "$device.deviceNetworkId-4"}          
        if (logEnable) log.debug "Found the word SwitchFour"
        if (json.contains("1")){
            if (logEnable) log.debug "Found the value 1"
            if (logInfo) log.info "$device.label - Switch 4 is On"
            sendEvent(name:"switch4",value:"on")
            if (childDevice4) {
                childDevice4.sendEvent(name: "switch", value:"on")
                childDevice4.sendEvent(name: "motion", value:"active")
            }
        }
        else if (json.contains("0")){
            if (logEnable) log.debug "Found the value 0"
            if (logInfo) log.info "$device.label - Switch 4 is Off"
            sendEvent(name:"switch4",value:"off")
            if (childDevice4) {
                childDevice4.sendEvent(name: "switch", value:"off")
                childDevice4.sendEvent(name: "motion", value:"inactive")
            }
        }
    }
}
    
def on(value) {
    if (logEnable) log.debug "Sending On Command to [${settings.deviceIp} Switch $value]"
    try {
        httpGet("http://" + deviceIp + "/cm?cmnd=POWER" + "$value" + "%20On") { resp ->
            def json = (resp.data)
            if (logEnable) log.debug "${json}"
            if (json.POWER1 == "ON" || json.POWER2 == "ON" || json.POWER3 == "ON" || json.POWER4 == "ON"){
                if (logEnable) log.debug "Command Success response from Device"
            }else{
                if (logEnable) log.debug "Command -ERROR- response from Device- $json"
            }
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def off(value) {
    if (logEnable) log.debug "Sending Off Command to [${settings.deviceIp} Switch $value]"
    try {
        httpGet("http://" + deviceIp + "/cm?cmnd=POWER" + "$value" + "%20Off") { resp ->
            def json = (resp.data)
            if (logEnable) log.debug "${json}"
            if (json.POWER1 == "OFF" || json.POWER2 == "OFF" || json.POWER3 == "OFF" || json.POWER4 == "OFF"){
                if (logEnable) log.debug "Command Success response from Device"
            }else{
                if (logEnable) log.debug "Command -ERROR- response from Device- $json"
            }
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def childOn(data){
    if (logEnable) log.debug  "Switch On $data"
    dataNew = ("$data"[-1]) as Integer
    if (logEnable) log.debug  "$dataNew"
    on("$dataNew")
}

def childOff(data){
    if (logEnable) log.debug  "Switch Off $data"
    dataNew = ("$data"[-1]) as Integer
    if (logEnable) log.debug  "$dataNew"
    off("$dataNew")
}

def outlet1(data){
    if (data == "On"){
        on(1)
    }else{
        off(1)
    }
}

def outlet2(data){
    if (data == "On"){
        on(2)
    }else{
        off(2)
    }
}

def outlet3(data){
    if (data == "On"){
        on(3)
    }else{
        off(3)
    }
}

def outlet4(data){
    if (data == "On"){
        on(4)
    }else{
        off(4)
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
                   signal = json.StatusSTS.Wifi.Signal as String
                   if (logEnable) log.debug "Wifi signal strength $signal db"
                   sendEvent(name:"wifi",value:"${signal}db")
                   status1 = json.StatusSTS.POWER1 as String
                   if (logEnable) log.debug "POWER1: $status1"
                   if (logInfo) log.info "$device.label - Switch 1 is $status1"
                   sendEvent(name: "switch1", value: "$status1".toLowerCase())
                   def childDevice1 = childDevices.find{it.deviceNetworkId == "$device.deviceNetworkId-1"}
                   if (childDevice1) {         
                       childDevice1.sendEvent(name: "switch", value:"$status1".toLowerCase())
                       if ("$status1" == "ON"){
                           childDevice1.sendEvent(name: "motion", value:"active")
                       }else{
                           childDevice1.sendEvent(name: "motion", value:"inactive")
                       }
                   }
                   status2 = json.StatusSTS.POWER2 as String
                   if (logEnable) log.debug "POWER2: $status2"
                   if (logInfo) log.info "$device.label - Switch 2 is $status2"
                   sendEvent(name: "switch2", value: "$status2".toLowerCase())
                   def childDevice2 = childDevices.find{it.deviceNetworkId == "$device.deviceNetworkId-2"}
                   if (childDevice2) {         
                       childDevice2.sendEvent(name: "switch", value: "$status2".toLowerCase())
                       if ("$status2" == "ON"){
                           childDevice2.sendEvent(name: "motion", value:"active")
                       }else{
                           childDevice2.sendEvent(name: "motion", value:"inactive")
                       }
                   }
                   status3 = json.StatusSTS.POWER3 as String
                   if (logEnable) log.debug "POWER3: $status3"
                   if (logInfo) log.info "$device.label - Switch 3 is $status3"
                   sendEvent(name: "switch3", value: "$status3".toLowerCase())
                   def childDevice3 = childDevices.find{it.deviceNetworkId == "$device.deviceNetworkId-3"}
                   if (childDevice3) {         
                       childDevice3.sendEvent(name: "switch", value: "$status3".toLowerCase())
                       if ("$status3" == "ON"){
                           childDevice3.sendEvent(name: "motion", value:"active")
                       }else{
                           childDevice3.sendEvent(name: "motion", value:"inactive")
                       }
                   }
                   status4 = json.StatusSTS.POWER4 as String
                   if (logEnable) log.debug "POWER4: $status4"
                   if (logInfo) log.info "$device.label - Switch 4 is $status4"
                   sendEvent(name: "switch4", value: "$status4".toLowerCase())
                   def childDevice4 = childDevices.find{it.deviceNetworkId == "$device.deviceNetworkId-4"}
                   if (childDevice4) {         
                       childDevice4.sendEvent(name: "switch", value: "$status4".toLowerCase())
                       if ("$status4" == "ON"){
                           childDevice4.sendEvent(name: "motion", value:"active")
                       }else{
                           childDevice4.sendEvent(name: "motion", value:"inactive")
                       }
                   }
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

def uninstalled() {
    deleteChildren()
}

