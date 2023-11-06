/**
 *  BLE Tasker Beacon   **previosly named Tasker Mobile Presence
 *  *  Tasmota BLE Beacon - Child Device w/ Tasker extras  *
 *
 *  Use Tasker with Maker API for for extra features
 *
 *  Power source, battery level, and in vehicle extras.
 *
 *  Copyright 2023 Gassgs  GaryG
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
 *  V1.4.0  08-2-2022       No GPS, wifi near only removed ping
 *  V1.5.0  09-7-2022       wifi near and wifi ping/ ping added back in
 *  V2.0.0  10-23-2023      rebuild as child driver for Tasmota ESP 32 BLE Tracking solution
 */

def driverVer() { return "2.0" }

 
metadata {
	definition (name: "BLE Tasker Beacon", namespace: "Gassgs", author: "Gary G") {
	capability "Presence Sensor"
        capability "Beacon"
        capability "Sensor"
        capability "Power Source"
        capability "Actuator"
        capability "Battery"
        
        attribute "wifi", "String"
        attribute "beacon", "String"
        attribute "inCar", "String"
        
        command "beacon", [[name:"Set beacon", type: "ENUM",description: "Set beacon", constraints: ["detected", "not detected"]]]
        command "power", [[name:"Set power", type: "ENUM",description: "Set power", constraints: ["dc", "battery"]]]
        command "inCar", [[name:"In Car", type: "ENUM",description: "In Car", constraints: ["true", "false"]]]
        command "battery", [[name:"Set battery", type: "NUMBER",description: "Set battery"]]
	}
    
    preferences {
		input name: "ipAddress",type: "string",title: "<b>Phone IP Address</b>",required: true	
        input name: "timeoutMinutes",type: "number",title: "<b>Timeout in Minutes</b>",required: true,defaultValue: 3
        input name: "enableDevice",type: "bool",title: "<b>Enable Device Wifi Ping?</b>",required: true,defaultValue: true
        input name: "infoEnable", type: "bool", title: "<b>Enable Info Text logging</b>", defaultValue: true
        input name: "debugEnable", type: "bool", title: "<b>Enable debug logging</b>", defaultValue: true
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
        runIn(2, refresh)
    }
    else{
        wifi("notConnected")
    }
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
    
    if ((state.tryCount / state.triesPerMinute) > (timeoutMinutes < 1 ? 1 : timeoutMinutes) && device.currentValue('wifi') != "not conected") {
        def descriptionText = "${device.displayName} is OFFLINE";
        if (debugEnable) log.debug "descriptionText"
        wifi("notConnected")
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
		
		if (device.currentValue('wifi') != "connected") {
			def descriptionText = "${device.displayName} is ONLINE";
			if (debugEnable) log.debug "descriptionText"
			wifi("connected")
		}
	}
    else {
        if (debugEnable) log.debug  "${device.displayName}: httpGetCallback(The following result means that the hub was UNSUCCESSFUL in discovering the phone on the network: ${groovy.json.JsonOutput.toJson(response)}, data)"

    }
}

def beacon(value){
    if (value == "detected"){
        if (device.currentValue("beacon") == "not detected" || device.currentValue("beacon") == null){
            if (infoEnable) log.info "$device.label Beacon Detected -  Present"
            sendEvent(name:"beacon",value:"detected")
            sendEvent(name:"presence",value:"present")
        }
    }
    else if (value == "not detected"){
        if (device.currentValue("beacon") == "detected" || device.currentValue("beacon") == null){
            if (infoEnable) log.info "$device.label Beacon Not Detected"
            sendEvent(name:"beacon",value:"not detected")
            if (device.currentValue("wifi") == "not connected"){
                if (infoEnable) log.info "$device.label Wifi Not Connected and Beacon Not Detected - Not Present"
                sendEvent(name:"presence",value:"not present")
            }
        }
    }
}
 

def wifi(value){
    if (value == "connected"){
        if (device.currentValue("wifi") == "not connected" || device.currentValue("wifi") == null){
            if (infoEnable) log.info "$device.label wifi Connected - Present"
            sendEvent(name:"wifi",value:"connected")
            sendEvent(name:"presence",value:"present")
        }
    }
    else if (value == "notConnected"){
        if (device.currentValue("wifi") == "connected" || device.currentValue("wifi") == null){
            if (infoEnable) log.info "$device.label Wifi Not Connected"
            sendEvent(name:"wifi",value:"not connected")
            if (device.currentValue("beacon") == "not detected"){
                if (infoEnable) log.info "$device.label Wifi Not Connected and Beacon Not Detected - Not Present"
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
