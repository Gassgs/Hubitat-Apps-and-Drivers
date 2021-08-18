/**
 *  Tasker Mobile Presence
 *
 *  Use Tasker with Maker API for presence
 *  GPS + Wifi for presence -  Power source and battery level also supported.
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
 */
 
metadata {
	definition (name: "Tasker Mobile Presence", namespace: "Gassgs", author: "Gary G") {
	    capability "Presence Sensor"
        capability "Sensor"
        capability "Power Source"
        capability "Actuator"
        capability "Battery"
        capability "Switch"
        
        attribute "wifiLocation", "String"
        attribute "gpsLocation", "STRING"
        
        command "gps", [[name:"Set GPS", type: "ENUM",description: "Set GPS", constraints: ["arrived", "departed"]]]
        command "wifi", [[name:"Set Wifi", type: "ENUM",description: "Set Wifi", constraints: ["arrived", "departed"]]]
        command "power", [[name:"Set power", type: "ENUM",description: "Set power", constraints: ["dc", "battery"]]]
        command "battery", [[name:"Set battery", type: "NUMBER",description: "Set battery"]]
	}
    
    preferences {
        input name: "infoEnable", type: "bool", title: "Enable Info Text logging", defaultValue: true
    }
}


def installed() {
	configure()
}

def updated() {
	configure()
}

def configure() {
	if (infoEnable) log.info "Running config with settings: ${settings}"
}

def gps(value){
    if (value == "arrived"){
        if (device.currentValue("gpsLocation") == "away" || device.currentValue("gpsLocation") == null){
            if (infoEnable) log.info "$device.label Gps location Present"
            sendEvent(name:"gpsLocation",value:"home")
            sendEvent(name:"presence",value:"present")
            sendEvent(name:"switch",value:"on")
        }
    }
    else if (value == "departed"){
        if (device.currentValue("gpsLocation") == "home" || device.currentValue("gpsLocation") == null){
            if (infoEnable) log.info "$device.label GPS location Not Present"
            sendEvent(name:"gpsLocation",value:"away")
            if (device.currentValue("wifiLocation") == "away"){
                if (infoEnable) log.info "$device.label Wifi and Gps location Not Present"
                sendEvent(name:"presence",value:"not present")
                sendEvent(name:"switch",value:"off")
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
            sendEvent(name:"switch",value:"on")
        }
    }
    else if (value == "departed"){
        if (device.currentValue("wifiLocation") == "home" || device.currentValue("wifiLocation") == null){
            if (infoEnable) log.info "$device.label Wifi location Not Present"
            sendEvent(name:"wifiLocation",value:"away")
            if (device.currentValue("gpsLocation") == "away"){
                if (infoEnable) log.info "$device.label Wifi and Gps location Not Present"
                sendEvent(name:"presence",value:"not present")
                sendEvent(name:"switch",value:"off")
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

def on(){
    if (infoEnable) log.info "$device.label ON pushed - PRESENT"
    sendEvent(name:"switch",value:"on")
    sendEvent(name:"presence",value:"present")
}

def off(){
    if (infoEnable) log.info "$device.label OFF pushed - NOT PRESENT"
    sendEvent(name:"switch",value:"off")
    sendEvent(name:"presence",value:"not present")
}
