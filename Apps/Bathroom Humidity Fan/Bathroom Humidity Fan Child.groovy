/**
 *  ****************  Bathroom Humidity Fan Child App ****************
 *
 *  Turn exhaust fans on or off based on humidity comparison
 *   
 *
 *
 *  Copyright 2021 Gassgs / Gary Gassmann
 *
 *
 *-------------------------------------------------------------------------------------------------------------------
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *-------------------------------------------------------------------------------------------------------------------
 *
 *
 *-------------------------------------------------------------------------------------------------------------------
 *
 *  Last Update: 1/24/2021
 *
 *  Changes:
 *
 *  V1.0.0      -       2-01-2021       First run
 *  V1.2.0      -       2-08-2021       Fixed errors improvements
 *  V1.3.0      -       2-15-2021       Added auto off
 *
 */

import groovy.transform.Field

definition(
    name: "Bathroom Humidity Fan Child",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Turn exhaust fans on or off based on humidity comparison",
    parent: "Gassgs:Bathroom Humidity Fan",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences{
    
	section{
        paragraph(
        title: "Bathroom Humidity Fan Child",
        required: true,
    	"<div style='text-align:center'><b>Bathroom Humidity Fan App</b></div>"
     	)
     paragraph(
        title: "Bathroom Humidity Fan Child",
        required: true,
    	"<div style='text-align:center'>Bathroom Fan Control Options</div>"
     	)
        input(
            name:"fanSwitch",
            type:"capability.switch",
            title: "Fan to control",
            multiple: false,
            required: true,
            submitOnChange: true
              )
        input(
            name:"bathroomSensors",
            type:"capability.relativeHumidityMeasurement",
            title: "Bathroom humidity sensor(s)",
            multiple: true,
            required: true,
            submitOnChange: true
            )
            if(bathroomSensors){
            paragraph "<b>Current bathroom humidity average is ${averageBathroomHumidity()}%</b>"
        }
        input(
            name:"baselineSensors",
            type:"capability.relativeHumidityMeasurement",
            title: "Humidity sensor(s) to use as baseline",
            required: true,
            multiple: true,
            submitOnChange: true
            )
            if(baselineSensors){
            paragraph "<b>Current baseline humidity average is ${averageBaselineHumidity()}%</b>"
        }
        input(
            name:"threshold",
            type:"number",
            title:"The threshold above baseline to trigger on-off",
            defaultValue: 15,
            submitOnChange: true
        )
    }
    section{
        input(
            name:"autoOff",
            type:"number",
            title:"Number of minutes to run fan if turned on manually",
            multiple: false,
            required: true,
            defaultValue: 5,
            submitOnChange: true
        )
    }
    section{
        input(
            name:"motionSensor",
            type:"capability.motionSensor",
            title:"Motion sensor to activate fan when humidity above threshold (optional)",
            multiple: false,
            required: false,
            submitOnChange: true
        )
         if(motionSensor){
             input(
                name:"motionDelay",
                type:"number",
                title:"Delay fan for this many seconds after motion detected",
                multiple: false,
                required: false,
                defaultValue:"30",
                submitOnChange: true
            )
         }
    }
    section{
        input(
            name:"logEnable",
            type:"bool",
            title: "Enable Info logging",
            required: true,
            defaultValue: false
            )
    }
}


def installed(){
	initialize()
}

def uninstalled(){
	logInfo ("uninstalling app")
}

def updated(){
    logInfo ("Updated with settings: ${settings}")
	unschedule()
    unsubscribe()
	initialize()
}

def initialize(){
	subscribe(settings.bathroomSensors, "humidity", bathroomSensorsHandler)
    subscribe(settings.baselineSensors, "humidity", baselineSensorsHandler)
    subscribe(settings.fanSwitch, "switch",  fanSwitchHandler)
    subscribe(settings.motionSensor, "motion",  motionHandler)
    humidityFanHandler()
    logInfo ("subscribed to sensor events")
}


def bathroomSensorsHandler(evt){
    getBathroom()
}

def averageBathroomHumidity(){
	def total = 0
    def n = settings.bathroomSensors.size()
	settings.bathroomSensors.each {total += it.currentHumidity}
	return (total /n).toDouble().round(1)
}

def getBathroom(){
	def bathroomHumidity = averageBathroomHumidity()
	logInfo ("Current bathroom humidity average is ${averageBathroomHumidity()}")
    humidityFanHandler()
}

def baselineSensorsHandler(evt){
    getBaseline()
}

def averageBaselineHumidity(){
	def total = 0
    def n = settings.baselineSensors.size()
	settings.baselineSensors.each {total += it.currentHumidity}
	return (total /n).toDouble().round(1)
}

def getBaseline(){
	def baselineHumidity = averageBaselineHumidity()
	logInfo ("Current baseline humidity average is ${averageBaselineHumidity()}")
    humidityFanHandler()
}

def fanSwitchHandler(evt){
    switchStatus = evt.value
    logInfo ("Fan Switch $evt.value")
    state.fanSwitchOn = (evt.value == "on")
    state.fanSwitchOff = (evt.value == "off")
    if (state.fanSwitchOn&&state.goodHumidity){
        runIn(autoOff*60,autoFanOff)
    }
}

def motionHandler(evt){
    motionStatus = evt.value
    state.motionActive = (motionStatus == "active")
    state.motionInactive = (motionStatus == "inactive")
    if (state.motionActive){
        logInfo ("motion active checking humdity")
        humidityFanHandler()
    }
}

def humidityFanHandler(){
    bathroomHumidity = averageBathroomHumidity()
    baselineHumidity = averageBaselineHumidity()
    fanThreshold = (baselineHumidity + threshold)
    state.highHumidity = (bathroomHumidity > fanThreshold)
    state.goodHumidity = (bathroomHumidity <= fanThreshold)
    if (settings.motionSensor){
        humidityPlusMotion()
    }
    else{
        if (state.highHumidity&&state.fanSwitchOff){
        logInfo ("Humidity is higher than baseline plus threshold - turning ON")
        settings.fanSwitch.on()
        state.humidityActivatedFan = true
        }
        if (state.goodHumidity&&state.humidityActivatedFan){
        logInfo ("Humidity is OK - turning OFF")
        settings.fanSwitch.off()
        state.humidityActivatedFan = false
        }
    }
}
def humidityPlusMotion(){
    state.humidityActive = (state.highHumidity&&state.motionActive)
        if (state.humidityActive&&state.fanSwitchOff){
        logInfo ("Humidity is higher than baseline plus threshold and motion active - turning ON delayed")
        runIn(motionDelay,delayedFanOn)
        state.humidityActivatedFan = true
        }
        if (state.goodHumidity&&state.humidityActivatedFan){
        logInfo ("Humidity is OK - turning OFF")
        settings.fanSwitch.off()
        state.humidityActivatedFan = false
    }
}
def delayedFanOn(){
    logInfo ("Motion active delay over - turning ON")
    settings.fanSwitch.on()
}

def autoFanOff(){
    logInfo ("auto off timer expired, turning fan Off")
    settings.fanSwitch.off()
}
    

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}
