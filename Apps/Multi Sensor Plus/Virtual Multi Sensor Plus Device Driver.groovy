/**
 *  Virtual Multi Sensor Plus
 *
 *  Average: Temperature, Humidity,Illuminance, and Carbon Dioxide  -  Group:  Locks, Contact, Motion, Water, Presence, Sound, and Smoke Sensors  - Plus  a Virtual Switch  -  All  In One Device
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
 *  V2.2.0  7-05-2021       added auto off options, toggle, and status update method redo.
 *  V2.3.0  11-17-2023      added waterLeak attribute for google home app
 *  V2.4.0  11-17-2023      added CO and Smoke capabilities w/ non standard values for google home community app
 *  V2.5.0  02-25-2024      added capability Carbon Dioxide Measurement
 * 
 */

metadata {
    definition (name: "Virtual Multi Sensor Plus", namespace: "Gassgs", author: "Gary G", importUrl:"https://raw.githubusercontent.com/Gassgs/Hubitat-Apps-and-Drivers/master/Apps/Multi%20Sensor%20Plus/Virtual%20Multi%20Sensor%20Plus%20Device%20Driver.groovy"){
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
        capability "CarbonMonoxideDetector"
        capability "SmokeDetector"
        capability "CarbonDioxideMeasurement"
        
        command "toggle"

        attribute"Contacts","string"
        attribute"Motion_Sensors","string"
        attribute"Water_Sensors","string"
        attribute"Person_Detected","string"
        attribute"Home","string"
        attribute"Sound_Heard","string"
        attribute"lock","string"
        attribute"Locks","string"
        attribute"waterLeak","string"
        attribute"CarbonMonoxide_Sensors","string"
        attribute"Smoke_Sensors","string"
    }
}
    preferences {
        input name: "autoOffEnable", type: "bool", title: "<b>Enable for auto off</b>", required: true, defaultValue: false, submitOnChange: true
        if (autoOffEnable){
            input name: "autoOff", type: "number", title: "<b>Timer for auto off, in seconds</b>", required: true, defaultValue: 5
        }
        input name: "logEnable", type: "bool", title: "<b>Enable info logging</b>", defaultValue: true
    
}

def on(){
    if (logEnable) log.info "Switch_On"
    sendEvent(name: "switch", value: "on")
    if(autoOffEnable){
        if (logEnable) log.info "Turning Off in $autoOff seconds"
        runIn(autoOff,off)
    }
}

def off(){
    if (logEnable) log.info "Switch_Off"
    sendEvent(name: "switch", value: "off")
}

def toggle(){
    status = (device.currentValue("switch"))
    if (status == "on"){
        off()
    }else{
        on()
    }
}

def installed(){
}
