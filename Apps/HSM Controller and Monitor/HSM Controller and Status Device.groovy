/**
 *  HSM Controller and Status Device
 *
 *  Virtual Driver for Home  Security Control and Status
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
 *  V1.0.0  1-27-2021   -       First run - Gassgs
 *  V1.2.0  1-28-2021   -       Added arm and disarm away with presence       
 *  V1.3.0  1-29-2021   -       Added Water sensor custom Handler
 *  V2.0.0  1-31-2021   -       Cleanup and improvements
 *  V2.1.0  6-30-2021   -       Improvements changed update method
 *  V2.2.0  12-24-2022  -       Improvements simplified on/off
 * 
 */

metadata {
    definition (name: "HSM Controller and Status Device", namespace: "Gassgs", author: "Gary G") {
        capability"Actuator"
        capability "Switch"
        capability "Presence Sensor"
        capability "WaterSensor"
        capability "Refresh"
        
        command"clearAlert"
        command"armHome"
        command"armNight"
        command"armAway"
        command"disarm"
      
        attribute"status","string"
        attribute"alert","string"
        attribute"currentAlert","string"
        attribute"currentMode","string"
        attribute"Home","string"
        attribute"Leak","string"
        attribute"exitAllowance","number"
        
    }
}

def on(){
    mode = device.currentValue("currentMode")
    if (mode == "Away"){
        sendLocationEvent(name: "hsmSetArm", value: "armAway")
    }
    else if (mode == "Night" || mode == "Late_evening"){
        sendLocationEvent(name: "hsmSetArm", value: "armNight")
    }else{
        sendLocationEvent(name: "hsmSetArm", value: "armHome")
    }
}

def off(){
    sendLocationEvent(name: "hsmSetArm", value: "disarm")
}

//current event will send :"alert","active" / when cancelled will send: "alert", "ok"
def clearAlert(){
    sendEvent(name:"alert",value:"clearing")
    runIn(5,resetAlert)
}

def resetAlert(){
    sendEvent(name:"alert",value:"ok")
}

def armAway(){
    sendLocationEvent(name: "hsmSetArm", value: "armAway")
}

def armNight(){
    sendLocationEvent(name: "hsmSetArm", value: "armNight")
}

def armHome(){
    sendLocationEvent(name: "hsmSetArm", value: "armHome")
}

def disarm(){
    sendLocationEvent(name: "hsmSetArm", value: "disarm")
}

def refresh(){
    log.info "refresh called, does nothing.used for google home"
    //does nothing.used for google home
}
