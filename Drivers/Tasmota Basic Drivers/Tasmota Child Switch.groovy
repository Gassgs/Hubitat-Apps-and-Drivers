/**
 *  Tasmota Child Switch
 *
 *
 *  Copyright 2022 Gassgs
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
 * 
 *  V1.0.0  06-03-2022       first run
 *  V1.1.0  06-22-2022       added toogle command
 *  
 * 
 */

metadata {
    definition (name: "Tasmota Child Switch", namespace: "Gassgs", author: "Gary G"){
        capability "Actuator"
        capability "Sensor"
        capability "Motion Sensor"
        capability "Switch"
        
        command "toggle"
    }   
    preferences {
        input name: "logInfoEnable", type: "bool", title: "Enable text info logging", defaultValue: true
    }
}

def on() {
     if(logInfoEnable)log.info "$device.label On - Active"
    parent.childOn("$device.deviceNetworkId")
}

def off() {
     if(logInfoEnable)log.info "$device.label Off - Inactive"
    parent.childOff("$device.deviceNetworkId")
}

def toggle(){
    status = device.currentValue("switch")
    if (status == "on"){
        off()
    }else{
        on()
    }
}

def installed() {
}
