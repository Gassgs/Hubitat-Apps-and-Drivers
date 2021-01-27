/**
 *  Virtual Multi Sensor Plus
 *
 *  Average: Temperature, Humidity, and Illuminance   -  Group:  Locks, Contact, Motion, Water, Presence, and Sound Sensors  - Plus  a Virtual Switch  -  All  In One Device
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
 *  V1.0.0  1-09-2021       first run - Gassgs  
 *  V1.1.0  1-12-2021       improved layout and standardized Hum,Temp,Lux events  Gassgs
 *  V2.0.0  1-12-2021       improved presence sensor options to include nest cams and normal presence devices  Gassgs
 *  V2.1.0  1-26-2021       code cleanup
 * 
 */

metadata {
    definition (name: "Virtual Multi Sensor Plus", namespace: "Gassgs", author: "Gary G"){
        capability"Actuator"
        capability "Switch"
        capability "Sensor"
        capability "ContactSensor"
        capability "Motion Sensor"
        capability "WaterSensor"
        capability "PresenceSensor"
        capability "SoundSensor"
        capability "TemperatureMeasurement"
        capability "RelativeHumidityMeasurement"
        capability "IlluminanceMeasurement"

        command "setTemperature",["number"]
        command "setHumidity",["number"]
        command "setIlluminance",["number"]
        command "statusUpdate",[[name:"status",type:"STRING"],[name:"text",type:"STRING"]]

        attribute"Contacts","string"
        attribute"Motion_Sensors","string"
        attribute"Water_Sensors","string"
        attribute"Person_Detected","string"
        attribute"Home","string"
        attribute"Sound_Heard","string"
        attribute"lock","string"
        attribute"Locks","string"
    }
}

def on(){
    log.info "Switch_On"
    sendEvent(name: "switch", value: "on")
}

def off(){
    log.info "Switch_Off"
    sendEvent(name: "switch", value: "off")
}

def statusUpdate(String status,String value){
    textValue=value
    statusValue=status
    sendEvent(name:statusValue, value: textValue)
}

def setTemperature(avg){
    sendEvent(name: "temperature", value: avg)
}

def setHumidity(avg){
    sendEvent(name: "humidity", value: avg)
}

def setIlluminance(avg){
    sendEvent(name: "illuminance", value: avg)
}

def installed(){
}
