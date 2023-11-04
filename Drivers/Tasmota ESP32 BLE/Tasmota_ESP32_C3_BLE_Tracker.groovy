/**
 *  Tasmota ESP32-C3 BLE Tracker
 *  
 *  Using with blue charms, feasycom, and innway card beacons
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
 *  V1.0.0  10-23-2023        first run
 *  V1.1.0  10-24-2023        updated with berry script no more spamming messages
 *  V1.2.0  10-24-2023        expanded to support up to 6 beacons, clean up
 *  V1.3.0  10-26-2023        added refresh options and child devices toggle
 *  V1.4.0  10-30-2023        added beacon reset option and testing on ESP32-C3 w/ BLE 5.0 hardware
 *  V1.5.0  10-30-2023        changed method of upadting children for multiple child driver options (tasker)
 *  V2.0.0  11-02-2023        New Version for multiple trackers with beacon app to combine, removed child devices, expanded to 8 beacons
 */

def driverVer() { return "2.0" }

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name: "Tasmota ESP32-C3 BLE Tracker", namespace: "Gassgs", author: "Gary G") {
        capability "Switch"
        capability "Actuator"
        capability "Refresh"
        capability "Sensor"
        capability "Beacon"
        
        command "restartESP32"
        command "resetBeacons", [[name:"reset", type: "ENUM",description: "reset", constraints: ["No", "Are You Sure?",]]]
        
        attribute "beacon1","string"
        attribute "beacon2","string"
        attribute "beacon3","string"
        attribute "beacon4","string"
        attribute "beacon5","string"
        attribute "beacon6","string"
        attribute "beacon7","string"
        attribute "beacon8","string"
        attribute "wifi","string"
        
    }
}
    preferences {
        def refreshRate = [:]
        refreshRate << ["disabled" : "Disabled"]
        refreshRate << ["5 min" : "Ping every 5 minutes"]
        refreshRate << ["10 min" : "Ping every 10 minutes"]
	    refreshRate << ["15 min" : "Ping every 15 minutes"]
	    refreshRate << ["30 min" : "Ping every 30 minutes"]
        input name: "deviceIp",type: "string", title: "<b>Tasmota ESP32 IP Address</b>", required: true
        input name: "hubIp",type: "string", title: "<b>Hubitat Hub IP Address</b>", required: true
        input name: "refreshRate",type: "enum", title: "<b>Ping Option</b>",options: refreshRate, defaultValue: "15 min", required: true
        input name: "beacon1", type: "string", title: "<b>Beacon1 MAC Address</b>", required: false , defaultValue: "00:00:00:00:00:00"
        input name: "beacon2", type: "string", title: "<b>Beacon2 MAC Address</b>", required: false , defaultValue: "00:00:00:00:00:00"
        input name: "beacon3", type: "string", title: "<b>Beacon3 MAC Address</b>", required: false , defaultValue: "00:00:00:00:00:00"
        input name: "beacon4", type: "string", title: "<b>Beacon4 MAC Address</b>", required: false , defaultValue: "00:00:00:00:00:00"
        input name: "beacon5", type: "string", title: "<b>Beacon5 MAC Address</b>", required: false , defaultValue: "00:00:00:00:00:00"
        input name: "beacon6", type: "string", title: "<b>Beacon6 MAC Address</b>", required: false , defaultValue: "00:00:00:00:00:00"
        input name: "beacon7", type: "string", title: "<b>Beacon7 MAC Address</b>", required: false , defaultValue: "00:00:00:00:00:00"
        input name: "beacon8", type: "string", title: "<b>Beacon8 MAC Address</b>", required: false , defaultValue: "00:00:00:00:00:00"
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
    
     switch(refreshRate) {
        case "disabled" :
			unschedule(refresh)
            if (logEnable) log.debug "refresh schedule disabled"
            if (logInfo) log.info "$device.label refresh every 5 minutes schedule"
			break
		case "5 min" :
			runEvery5Minutes(refresh)
            if (logEnable) log.debug("refresh every 5 minutes schedule")
            if (logInfo) log.info "$device.label refresh every 5 minutes schedule"
			break
        case "10 min" :
			runEvery10Minutes(refresh)
            if (logEnable) log.debug ("refresh every 10 minutes schedule")
            if (logInfo) log.info "$device.label refresh every 10 minutes schedule"
			break
		case "15 min" :
			runEvery15Minutes(refresh)
            if (logEnable) log.debug ("refresh every 15 minutes schedule")
            if (logInfo) log.info "$device.label refresh every 15 minutes schedule"
			break
		case "30 min" :
			runEvery30Minutes(refresh)
            if (logEnable) log.debug ("refresh every 30 minutes schedule")
            if (logInfo) log.info "$device.label refresh every 30 minutes schedule"
            break
	}
    deviceSetup()
    syncSetupRule1()
    if (settings.beacon1 == "00:00:00:00:00:00"){device.deleteCurrentState('beacon1')}
    if (settings.beacon2 == "00:00:00:00:00:00"){device.deleteCurrentState('beacon2')}
    if (settings.beacon3 == "00:00:00:00:00:00"){device.deleteCurrentState('beacon3')}
    if (settings.beacon4 == "00:00:00:00:00:00"){device.deleteCurrentState('beacon4')}
    if (settings.beacon5 == "00:00:00:00:00:00"){device.deleteCurrentState('beacon5')}
    if (settings.beacon6 == "00:00:00:00:00:00"){device.deleteCurrentState('beacon6')}
    if (settings.beacon7 == "00:00:00:00:00:00"){device.deleteCurrentState('beacon7')}
    if (settings.beacon8 == "00:00:00:00:00:00"){device.deleteCurrentState('beacon8')}
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

def syncSetupRule1(){
        if (hubIp){
            def beacon1mac = settings.beacon1.replace(":","")
            def beacon2mac = settings.beacon2.replace(":","")
            def beacon3mac = settings.beacon3.replace(":","")
            def beacon4mac = settings.beacon4.replace(":","")
            def beacon5mac = settings.beacon5.replace(":","")
            def beacon6mac = settings.beacon6.replace(":","")
            def beacon7mac = settings.beacon7.replace(":","")
            def beacon8mac = settings.beacon8.replace(":","")

            rule = "ON System#Boot DO backlog IBEACONperiod 10; BLEAddrFilter 3;  BLEAlias $beacon1mac=beacon1 $beacon2mac=beacon2 $beacon3mac=beacon3 $beacon4mac=beacon4 $beacon5mac=beacon5 $beacon6mac=beacon6 $beacon7mac=beacon7 $beacon8mac=beacon8; iBeaconOnlyAliased 1; iBeacon 1 endon "
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
    runIn(2,turnOnRule1)
}

def turnOnRule1(){
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
    syncSetupRule3()
}

def syncSetupRule3(){
        if (hubIp){
            
            rule = "ON Power1#state DO webquery http://"+ hubIp + ":39501/ POST BEACONONE %value% ENDON " +
                "ON Power2#state DO webquery http://"+ hubIp + ":39501/ POST BEACONTWO %value% ENDON " +
                "ON Power3#state DO webquery http://"+ hubIp + ":39501/ POST BEACONTHREE %value% ENDON " +
                "ON Power4#state DO webquery http://"+ hubIp + ":39501/ POST BEACONFOUR %value% ENDON " +
                "ON Power5#state DO webquery http://"+ hubIp + ":39501/ POST BEACONFIVE %value% ENDON " +
                "ON Power6#state DO webquery http://"+ hubIp + ":39501/ POST BEACONSIX %value% ENDON " +
                "ON Power7#state DO webquery http://"+ hubIp + ":39501/ POST BEACONSEVEN %value% ENDON " +
                "ON Power8#state DO webquery http://"+ hubIp + ":39501/ POST BEACONEIGHT %value% ENDON "
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
    runIn(2,turnOnRule3)
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
    runIn(2,refresh)
    runIn(4,restartESP32)
}

def parse(LanMessage){
    if (logEnable) log.debug "data is ${LanMessage}"
    def msg = parseLanMessage(LanMessage)
    def json = msg.body
    if (logEnable) log.debug "${json}"
    iBeaconStatus = device.currentValue("switch")
    if (iBeaconStatus == null || iBeaconStatus == "off"){
        sendEvent(name:"switch",value:"on")
    }
    if (json.contains("BEACONONE")){   
        if (json.contains("1")){
            if (logInfo) log.info "$device.label Beacon 1 is <b>detected</b>"
            sendEvent(name:"beacon1",value:"detected")
        }
        else if (json.contains("0")){
            if (logInfo) log.info "$device.label Beacon 1 is <b>not detected</b>"
            sendEvent(name:"beacon1",value:"not detected")
        }
    }
    else if (json.contains("BEACONTWO")){
        if (json.contains("1")){
            if (logInfo) log.info "$device.label Beacon 2 is <b>detected</b>"
            sendEvent(name:"beacon2",value:"detected")
        }
        else if (json.contains("0")){
            if (logInfo) log.info "$device.label Beacon 2 is <b>not detected</b>"
            sendEvent(name:"beacon2",value:"not detected")
        }
    }
    else if (json.contains("BEACONTHREE")){
        if (json.contains("1")){
            if (logInfo) log.info "$device.label Beacon 3 is <b>detected</b>"
            sendEvent(name:"beacon3",value:"detected")
        }
        else if (json.contains("0")){
            if (logInfo) log.info "$device.label Beacon 3 is <b>not detected</b>"
            sendEvent(name:"beacon3",value:"not detected")
        }
    }
    else if (json.contains("BEACONFOUR")){ 
        if (json.contains("1")){
            if (logInfo) log.info "$device.label Beacon 4 is <b>detected</b>"
            sendEvent(name:"beacon4",value:"detected")
        }
        else if (json.contains("0")){
            if (logInfo) log.info "$device.label Beacon 4 is <b>not detected</b>"
            sendEvent(name:"beacon4",value:"not detected")
        }
    }
    else if (json.contains("BEACONFIVE")){ 
        if (json.contains("1")){
            if (logInfo) log.info "$device.label Beacon 5 is <b>detected</b>"
            sendEvent(name:"beacon5",value:"detected")
        }
        else if (json.contains("0")){
            if (logInfo) log.info "$device.label Beacon 5 is <b>not detected</b>"
            sendEvent(name:"beacon5",value:"not detected")
        }
    }
    else if (json.contains("BEACONSIX")){
        if (json.contains("1")){
            if (logInfo) log.info "$device.label Beacon 6 is <b>detected</b>"
            sendEvent(name:"beacon6",value:"detected")
        }
        else if (json.contains("0")){
            if (logInfo) log.info "$device.label Beacon 6 is <b>not detected</b>"
            sendEvent(name:"beacon6",value:"not detected")
        }
    }
    else if (json.contains("BEACONSEVEN")){
        if (json.contains("1")){
            if (logInfo) log.info "$device.label Beacon 7 is <b>detected</b>"
            sendEvent(name:"beacon7",value:"detected")
        }
        else if (json.contains("0")){
            if (logInfo) log.info "$device.label Beacon 7 is <b>not detected</b>"
            sendEvent(name:"beacon7",value:"not detected")
        }
    }
    else if (json.contains("BEACONEIGHT")){
        if (json.contains("1")){
            if (logInfo) log.info "$device.label Beacon 8 is <b>detected</b>"
            sendEvent(name:"beacon8",value:"detected")
        }
        else if (json.contains("0")){
            if (logInfo) log.info "$device.label Beacon 8 is <b>not detected</b>"
            sendEvent(name:"beacon8",value:"not detected")
        }
    }
}
    
def on(){
    try {
        httpGet("http://" + deviceIp + "/cm?cmnd=IBEACON%20ON") { resp ->
             def json = (resp.data) 
             if (json){
                 if (logInfo) log.info "$device.label - iBeacon Scanning Off"
                 if (logEnable) log.debug "Command Success response from Device -IBEACON ON"
                 sendEvent(name:"switch",value:"on")
             }else{
                 if (logEnable) log.debug "Command -ERROR- response from Device- $json"
             }
         }
     } catch (Exception e) {
         log.warn "Call to on failed: ${e.message}"
    }
}

def off(){
    try {
        httpGet("http://" + deviceIp + "/cm?cmnd=IBEACON%20OFF") { resp ->
             def json = (resp.data) 
             if (json){
                 if (logInfo) log.info "$device.label - iBeacon Scanning Off"
                 if (logEnable) log.debug "Command Success response from Device -IBEACON OFF"
                 sendEvent(name:"switch",value:"off")
             }else{
                 if (logEnable) log.debug "Command -ERROR- response from Device- $json"
             }
         }
     } catch (Exception e) {
         log.warn "Call to on failed: ${e.message}"
    }
}

def resetBeacons(value){
    if (value == "Are You Sure?"){
        try {
            httpGet("http://" + deviceIp + "/cm?cmnd=POWER0%20OFF") { resp ->
                def json = (resp.data) 
                if (json){
                    if (logInfo) log.info "$device.label Resetting All Beacons"
                    if (logEnable) log.debug "Command Success response from Device - Resetting All Beacons"
                    sendEvent(name:"switch",value:"off")
                }else{
                    if (logEnable) log.debug "Command -ERROR- response from Device- $json"
                }
            }
        } catch (Exception e) {
            log.warn "Call to on failed: ${e.message}"
        }
    }else{
        if (logInfo) log.info "$device.label Not Resetting All Beacon"
        if (logEnable) log.debug "$device.label Not Resetting All Beacons"
    }
}

def restartESP32(){
     try {
         httpGet("http://" + deviceIp + "/cm?cmnd=restart%20ON") { resp ->
             def json = (resp.data) 
             if (json){
                 if (logInfo) log.info "$device.label - Restarting"
                 if (logEnable) log.debug "Command Success response from Device - Restarting"
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
                   signal = json.StatusSTS.Wifi.Signal as String
                   if (logInfo) log.info "$device.label Wifi signal strength $signal db"
                   if (logEnable) log.debug "$device.label Wifi signal strength $signal db"
                   sendEvent(name:"wifi",value:"${signal}db")
                   status1 = json.StatusSTS.POWER1 as String
                   status2 = json.StatusSTS.POWER2 as String
                   status3 = json.StatusSTS.POWER3 as String
                   status4 = json.StatusSTS.POWER4 as String
                   status5 = json.StatusSTS.POWER5 as String
                   status6 = json.StatusSTS.POWER6 as String
                   status7 = json.StatusSTS.POWER7 as String
                   status8 = json.StatusSTS.POWER8 as String
                   if (settings.beacon1 != "00:00:00:00:00:00"){beacon1Active = true}else{beacon1Active = false}
                   if (settings.beacon2 != "00:00:00:00:00:00"){beacon2Active = true}else{beacon2Active = false}
                   if (settings.beacon3 != "00:00:00:00:00:00"){beacon3Active = true}else{beacon3Active = false}
                   if (settings.beacon4 != "00:00:00:00:00:00"){beacon4Active = true}else{beacon4Active = false}
                   if (settings.beacon5 != "00:00:00:00:00:00"){beacon5Active = true}else{beacon5Active = false}
                   if (settings.beacon6 != "00:00:00:00:00:00"){beacon6Active = true}else{beacon6Active = false}
                   if (settings.beacon7 != "00:00:00:00:00:00"){beacon7Active = true}else{beacon7Active = false}
                   if (settings.beacon8 != "00:00:00:00:00:00"){beacon8Active = true}else{beacon8Active = false}
                   if (status1 == "ON" && beacon1Active){
                       if (logInfo) log.info "$device.label Beacon 1 is <b>detected</b>"
                       sendEvent(name:"beacon1",value:"detected")
                   }
                   else if (status1 == "OFF" && beacon1Active){
                       if (logInfo) log.info "$device.label Beacon 1 is <b>not detected</b>"
                       sendEvent(name:"beacon1",value:"not detected")
                   } 
                   if (status2 == "ON" && beacon2Active){
                       if (logInfo) log.info "$device.label Beacon 2 is <b>detected</b>"
                       sendEvent(name:"beacon2",value:"detected")
                   }
                   else if (status2 == "OFF" && beacon2Active){
                       if (logInfo) log.info "$device.label Beacon 2 is <b>not detected</b>"
                       sendEvent(name:"beacon2",value:"not detected")
                   } 
                   if (status3 == "ON" && beacon3Active){
                       if (logInfo) log.info "$device.label Beacon 3 is <b>detected</b>"
                       sendEvent(name:"beacon3",value:"detected")
                   }
                   else if (status3 == "OFF" && beacon3Active){
                       if (logInfo) log.info "$device.label Beacon 3 is <b>not detected</b>"
                       sendEvent(name:"beacon3",value:"not detected")
                   }
                   if (status4 == "ON" && beacon4Active){
                       if (logInfo) log.info "$device.label Beacon 4 is <b>detected</b>"
                       sendEvent(name:"beacon4",value:"detected")
                   }
                   else if (status4 == "OFF" && beacon4Active){
                       if (logInfo) log.info "$device.label Beacon 4 is <b>not detected</b>"
                       sendEvent(name:"beacon4",value:"not detected")
                   }
                   if (status5 == "ON" && beacon5Active){
                       if (logInfo) log.info "$device.label Beacon 5 is <b>detected</b>"
                       sendEvent(name:"beacon5",value:"detected")
                   }
                   else if (status5 == "OFF" && beacon5Active){
                       if (logInfo) log.info "$device.label Beacon 5 is <b>not detected</b>"
                       sendEvent(name:"beacon5",value:"not detected")
                   }
                   if (status6 == "ON" && beacon6Active){
                       if (logInfo) log.info "$device.label Beacon 6 is <b>detected</b>"
                       sendEvent(name:"beacon6",value:"detected")
                   }
                   else if (status6 == "OFF" && beacon6Active){
                       if (logInfo) log.info "$device.label Beacon 6 is <b>not detected</b>"
                       sendEvent(name:"beacon6",value:"not detected")
                   }
                   if (status7 == "ON" && beacon7Active){
                       if (logInfo) log.info "$device.label Beacon 7 is <b>detected</b>"
                       sendEvent(name:"beacon7",value:"detected")
                   }
                   else if (status7 == "OFF" && beacon7Active){
                       if (logInfo) log.info "$device.label Beacon 7 is <b>not detected</b>"
                       sendEvent(name:"beacon7",value:"not detected")
                   }
                   if (status8 == "ON" && beacon8Active){
                       if (logInfo) log.info "$device.label Beacon 8 is <b>detected</b>"
                       sendEvent(name:"beacon8",value:"detected")
                   }
                   else if (status8 == "OFF" && beacon8Active){
                       if (logInfo) log.info "$device.label Beacon 8 is <b>not detected</b>"
                       sendEvent(name:"beacon8",value:"not detected")
                   }
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
