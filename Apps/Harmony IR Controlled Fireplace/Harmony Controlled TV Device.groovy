/**
 *  Harmony Controlled TV
 *
 *  Virtual Driver for Television
 *  Commands sent from Harmony hub & on /off based on power reporting
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
 *    V1.0  8-10-2021   -       first run - Gassgs
 *    V1.1  11-1-2021   -       consolidated attribute for all commands
 *  
 */

metadata {
    definition (name:"Harmony Controlled TV", namespace: "Gassgs", author: "Gary G") {
        capability "Actuator"
        capability "Switch"
        capability "Sensor"

        command"volumeUp"
        command"volumeDown"
        command"hdmi1"
        command"hdmi2"
        
        attribute"command","string"
    }
   
}
    preferences {
        input name: "logInfo", type: "bool", title: "Enable info logging", defaultValue: true
}
        
def on(){
    if (logInfo) log.info "$device.label - Power on"
    sendEvent(name:"command",value:"on")
    runInMillis(200,clear)
}

def off(){
    if (logInfo) log.info "$device.label - Power off"
    sendEvent(name:"command",value:"off")
    runInMillis(200,clear)
}

def volumeDown(){
    if (logInfo) log.info "$device.label - Volume down"
    sendEvent(name:"command",value:"down")
    runInMillis(200,clear)
}

def volumeUp(){
    if (logInfo) log.info "$device.label - Volume up"
    sendEvent(name:"command",value:"up")
    runInMillis(200,clear)
}

def hdmi1(){
    if (logInfo) log.info "$device.label - HDMI 1"
    sendEvent(name:"command",value:"hdmi1")
    runInMillis(200,clear)
}

def hdmi2(){
    if (logInfo) log.info "$device.label - HDMI 2"
    sendEvent(name:"command",value:"hdmi2")
    runInMillis(200,clear)
}

def clear(){
    sendEvent(name:"command",value:"set")
}
