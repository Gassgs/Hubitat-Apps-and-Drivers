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
 *  V1.2.0  02-10-2023       added flash command
 *  
 * 
 */

def driverVer() { return "1.2" }

metadata {
    definition (name: "Tasmota Child Switch", namespace: "Gassgs", author: "Gary G"){
        capability "Actuator"
        capability "Sensor"
        capability "Motion Sensor"
        capability "Switch"
        
        command "flash"
        command "toggle"
    }   
    preferences {
        input (name: "flashRate", type: "enum", title: "<b>Flash Rate</b>", defaultValue: 750, options: [750:"750ms", 1000:"1s", 2000:"2s", 5000:"5s" ])
        input name: "logInfoEnable", type: "bool", title: "<b>Enable text info logging</b>", defaultValue: true
    }
}

def on() {
    if (state.flashing){
        stopFlash()
    }
    if(logInfoEnable)log.info "$device.label On - Active"
    parent.childOn("$device.deviceNetworkId")
}

def off() {
    if (state.flashing){
        stopFlash()
    }
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

def flash(){
    if (state.flashing){
        stopFlash()
        if (state.restore){
            on()
        }
        else{
            off()
        }
    }
    else{
        if (logInfoEnable) log.info "$device.label Flashing Started"
        state.flashing = true
        currentStatus = device.currentValue("switch")
        if (currentStatus == "on"){
            state.restore = true
            flashOff()
        }
        else{
            state.restore = false
            flashOn()
        }
    }  
}

def flashOn(){
    parent.childOn("$device.deviceNetworkId")
    runInMillis(flashRate as Integer,flashOff)
}

def flashOff(){
    parent.childOff("$device.deviceNetworkId")
    runInMillis(flashRate as Integer,flashOn)  
}

def stopFlash(){
    if (logInfoEnable) log.info "$device.label Flashing Ended"
    unschedule(flashOn)
    unschedule(flashOff)
    state.flashing = false
}

def updated() {
    log.info "$device.label updated..."
    state.DriverVersion=driverVer()
}

def installed() {
}
