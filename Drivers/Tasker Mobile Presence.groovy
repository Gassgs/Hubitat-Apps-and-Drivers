/**
 *  Tasker Mobile Presence
 *
 *  Use Tasker with Maker API for presence
 *  GPS + Wifi for presence - Tasker & Auto Location  
 *  Power source, battery level, and in vehicle also supported.
 *
 *  Copyright 2021 Gassgs  GaryG
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
 *  V1.0.0  8-17-2021       First Run
 *  V1.1.0  8-23-2021       Added Wifi ping for double check
 *  V1.2.0  8-27-2021       Added "In Car" - booleen
 *  V1.3.0  11-1-2021       removed On/Off, not needed. testing complete.
 */

def driverVer() { return "1.3" }

 
metadata {
	definition (name: "Tasker Mobile Presence", namespace: "Gassgs", author: "Gary G") {
	capability "Presence Sensor"
        capability "Sensor"
        capability "Power Source"
        capability "Actuator"
        capability "Battery"
        capability "Refresh"
        
        attribute "wifiLocation", "String"
        attribute "gpsLocation", "String"
        attribute "inCar", "String"
        
        command "gps", [[name:"Set GPS", type: "ENUM",description: "Set GPS", constraints: ["arrived", "departed"]]]
        command "wifi", [[name:"Set Wifi", type: "ENUM",description: "Set Wifi", constraints: ["arrived", "departed"]]]
        command "power", [[name:"Set power", type: "ENUM",description: "Set power", constraints: ["dc", "battery"]]]
        command "inCar", [[name:"In Car", type: "ENUM",description: "In Car", constraints: ["true", "false"]]]
        command "battery", [[name:"Set battery", type: "NUMBER",description: "Set battery"]]
	}
    
    preferences {
	input name: "ipAddress",type: "string",title: "Phone IP Address",required: true	
        input name: "timeoutMinutes",type: "number",title: "Timeout Minutes",required: true,defaultValue: 3
        input name: "enableDevice",type: "bool",title: "Enable Device?",required: true,defaultValue: true
        input name: "infoEnable", type: "bool", title: "Enable Info Text logging", defaultValue: true
        input name: "debugEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}


def installed() {
	configure()
}

def updated() {
    log.info "${device.displayName}.updated()"
    
    state.DriverVersion = driverVer()
    
    state.tryCount = 0
    
	unschedule()
    
    if (enableDevice) {
        runEvery1Minute(refresh)
        state.triesPerMinute = 1
    }
    runIn(2, refresh)
	configure()
}

def ensureStateVariables() {
    if (state.triesPerMinute == null) {
        state.triesPerMinute = 1
    }
}

def configure() {
	if (infoEnable) log.info "Running config with settings: ${settings}"
}

def refresh() {
	if (debugEnable) log.debug "${device.displayName}.refresh()"

	state.tryCount = state.tryCount + 1
    
    ensureStateVariables()
    
    if ((state.tryCount / state.triesPerMinute) > (timeoutMinutes < 1 ? 1 : timeoutMinutes) && device.currentValue('wifiLocation') != "away") {
        def descriptionText = "${device.displayName} is OFFLINE";
        if (debugEnable) log.debug "descriptionText"
        if (infoEnable) log.info "$device.label Wifi location Not Present - backup ping"
        wifi("departed")
    }
    
	if (ipAddress == null || ipAddress.size() == 0) {
		return
	}
	
	asynchttpGet("httpGetCallback", [
		uri: "http://${ipAddress}/",
        timeout: 10
	]);
}

def httpGetCallback(response, data) {
	if (debugEnable) log.debug "${device.displayName}: httpGetCallback(${groovy.json.JsonOutput.toJson(response)}, data)"
	
	if (response != null && response.status == 408 && response.errorMessage.contains("Connection refused")) {
        if (debugEnable) log.debug "${device.displayName}: httpGetCallback(The following 'connection refused' result means that the hub was SUCCESSFUL in discovering the phone on the network: ${groovy.json.JsonOutput.toJson(response)}, data)"
		state.tryCount = 0
		
		if (device.currentValue('wifiLocation') != "home") {
			def descriptionText = "${device.displayName} is ONLINE";
			if (debugEnable) log.debug "descriptionText"
			wifi("arrived")
		}
	}
    else {
        if (debugEnable) log.debug  "${device.displayName}: httpGetCallback(The following result means that the hub was UNSUCCESSFUL in discovering the phone on the network: ${groovy.json.JsonOutput.toJson(response)}, data)"

    }
}

def gps(value){
    if (value == "arrived"){
        if (device.currentValue("gpsLocation") == "away" || device.currentValue("gpsLocation") == null){
            if (infoEnable) log.info "$device.label Gps location Present"
            sendEvent(name:"gpsLocation",value:"home")
            sendEvent(name:"presence",value:"present")
        }
    }
    else if (value == "departed"){
        if (device.currentValue("gpsLocation") == "home" || device.currentValue("gpsLocation") == null){
            if (infoEnable) log.info "$device.label GPS location Not Present"
            sendEvent(name:"gpsLocation",value:"away")
            if (device.currentValue("wifiLocation") == "away"){
                if (infoEnable) log.info "$device.label Wifi and Gps location Not Present"
                sendEvent(name:"presence",value:"not present")
            }
        }
    }
}

def wifi(value){
    if (value == "arrived"){
        if (device.currentValue("wifiLocation") == "away" || device.currentValue("wifiLocation") == null){
            if (infoEnable) log.info "$device.label wifi location Present"
            sendEvent(name:"wifiLocation",value:"home")
            sendEvent(name:"presence",value:"present")
        }
    }
    else if (value == "departed"){
        if (device.currentValue("wifiLocation") == "home" || device.currentValue("wifiLocation") == null){
            if (infoEnable) log.info "$device.label Wifi location Not Present"
            sendEvent(name:"wifiLocation",value:"away")
            if (device.currentValue("gpsLocation") == "away"){
                if (infoEnable) log.info "$device.label Wifi and Gps location Not Present"
                sendEvent(name:"presence",value:"not present")
            }
        }
    }
}

def power(value){
    if (value == "dc"){
        if (infoEnable) log.info "$device.label power dc"
        sendEvent(name:"powerSource",value:"dc")
    }
    else if (value == "battery"){
        if (infoEnable) log.info "$device.label power battery"
        sendEvent(name:"powerSource",value:"battery")
    }
}

def battery(value){
    if (infoEnable) log.info "$device.label Battery Level $value %"
    sendEvent(name:"battery",value:"$value")
}

def inCar(value){
    if (value == "true"){
        if (infoEnable) log.info "$device.label Status In Car - True"
        sendEvent(name:"inCar",value:"true")
    }
    else if (value == "false"){
        if (infoEnable) log.info "$device.label Status In Car - False"
        sendEvent(name:"inCar",value:"false")
    }
}
