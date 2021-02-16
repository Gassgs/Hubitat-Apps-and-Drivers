/**
 *  ****************  Kitchen Range Hood Automation App ****************
 *
 *  Motion Lighting and Hood fan automation based on humidity and temperature comparison
 *  to a baseline sensors reading.
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
 *  Last Update: 2/15/2021
 *
 *  Changes:
 *
 *  V1.0.0      -       2-15-2021       First run
 *
 */

import groovy.transform.Field

definition(
    name: "Kitchen Range Hood Automation",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Motion Lighting and Hood fan automation",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences{
    
	section{
        paragraph(
        title: "Kitchen Range Hood Automation",
        required: true,
    	"<div style='text-align:center'><big><b>Kitchen Range Hood Automation</b></div></big>"
     	)
     paragraph(
        title: "Kitchen Range Hood Automation",
        required: true,
    	"<b><div style='text-align:center'>Kitchen Range Hood Light Options</div></b>"
     	)
        input(
            name:"light",
            type:"capability.switchLevel",
            title: "Range Hood Light to control",
            multiple: true,
            required: true,
            submitOnChange: true
              )
        input(
            name:"activeMotionSensors",
            type:"capability.motionSensor",
            title: "Range Hood Motion Sensor for On",
            multiple: true,
            required: true,
            submitOnChange: true
              )
        input(
            name:"inactiveMotionSensors",
            type:"capability.motionSensor",
            title: "Motion sensors or zone to keep light On",
            multiple: true,
            required: true,
            submitOnChange: true
              )
        input(
            name:"timeout",
            type:"number",
            title: "Motion timeout in seconds before turn Off",
            defaultValue:"0",
            required: true,
            submitOnChange: true
              )
    }
    section{
        paragraph(
        title: "Mode Options",
        required: true,
    	"<div style='text-align:center'><b>Mode Options</b></div>"
     	)
        input(
            name:"earlyMorningOn",
            type:"number",
            title:"<b>Early Morning</b> On Level",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
        input(
            name:"dayOn",
            type:"number",
            title:"<b>Day</b> On Level",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
        input(
            name:"afternoonOn",
            type:"number",
            title:"<b>Afternoon</b> On Level",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
        input(
            name:"dinnerOn",
            type:"number",
            title:"<b>Dinner</b> On Level",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
        input(
            name:"eveningOn",
            type:"number",
            title:"<b>Evening</b> On Level",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
        input(
            name:"lateEveningOn",
            type:"number",
            title:"<b>Late Evening</b> On Level",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
        input(
            name:"nightOn",
            type:"number",
            title:"<b>Night</b> On Level",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
        input(
            name:"awayOn",
            type:"number",
            title:"<b>Away</b> On Level",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
    }
    section{
        input(
            name:"duration",
            type:"number",
            title:"<b>Fade duration</b> for Off",
            defaultValue:"1",
            required: true,
            submitOnChange: true
        )
    }
    section{
        input(
            name:"switch",
            type:"capability.switch",
            title:"Switch to disable automatic lighting",
            multiple: false,
            required: true,
            submitOnChange: true
        )
    }
    section{
        paragraph(
        title: "Kitchen Range Hood Automation",
        required: true,
    	"<b><div style='text-align:center'>Kitchen Range Hood Fan Options</div></b>"
     	)
        input(
            name:"fanSwitch",
            type:"capability.switch",
            title: "Range Hood Fan to control",
            multiple: false,
            required: true,
            submitOnChange: true
              )
        input(
            name:"hoodSensor",
            type:"capability.relativeHumidityMeasurement",
            title: "Range Hood temperature and humidity sensor",
            multiple: true,
            required: true,
            submitOnChange: true
            )
            if(hoodSensor){
            paragraph "<b>Current hood sensor humidity reading is ${hoodHumidity()}%</b>"
        }
            if(hoodSensor){
            paragraph "<b>Current hood sensor temperature reading is ${hoodTemperature()}</b>"
        }
        input(
            name:"baselineSensors",
            type:"capability.relativeHumidityMeasurement",
            title: "Baseline temperature and humidity sensor(s)",
            required: true,
            multiple: true,
            submitOnChange: true
            )
            if(baselineSensors){
            paragraph "<b>Current baseline humidity average is ${baselineHumidity()}%</b>"
        }
            if(baselineSensors){
            paragraph "<b>Current baseline temperature average is ${baselineTemperature()}</b>"
        }
        input(
            name:"tempThreshold",
            type:"number",
            title:"The Temperature threshold above baseline to trigger on-off",
            defaultValue: 8,
            submitOnChange: true
        )
        input(
            name:"humThreshold",
            type:"number",
            title:"The Humidity threshold above baseline to trigger on-off",
            defaultValue: 5,
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
	subscribe(settings.hoodSensor, "humidity", hoodSensorHumidityHandler)
    subscribe(settings.hoodSensor, "temperature", hoodSensorTemperatureHandler)
    subscribe(settings.baselineSensors, "humidity", baselineHumidityHandler)
    subscribe(settings.baselineSensors, "temperature", baselineTemperatureHandler)
    subscribe(settings.fanSwitch, "switch",  fanSwitchHandler)
    subscribe(settings.light, "switch",  lightSwitchHandler)
    subscribe(settings.switch, "switch",  switchHandler)
    subscribe(settings.activeMotionSensors, "motion.active",  activeMotionHandler)
    subscribe(settings.inactiveMotionSensors, "motion",  inactiveMotionHandler)
    subscribe(location, "mode", modeEventHandler)
    //run something  ---get humidity and temp
    logInfo ("subscribed to sensor events")
}

def modeEventHandler(evt){
    mode = evt.value
    state.earlyMorning = (mode == "Early_morning")
    state.day = (mode == "Day")
    state.afternoon = (mode == "Afternoon")
    state.dinner = (mode == "Dinner")
    state.evening = (mode == "Evening")
    state.lateEvening = (mode == "Late_evening")
    state.night = (mode == "Night")
    state.away = (mode == "Away")
    logInfo ("mode status $mode")
}

def lightSwitchHandler(evt){
    lightSwitch = evt.value
    logInfo ("Light Switch $lightSwitch")
    state.lightSwitchOn = (lightSwitch == "on")
}

def switchHandler(evt){
    switchStatus = evt.value
    logInfo ("motion lighting switch $switchStatus")
    state.switchOk = (switchStatus == "on")
}

def activeMotionHandler(evt){
    if (state.lightSwitchOn){
        logInfo ("not setting level already on")
    }
    else if (state.switchOk){
        logInfo ("Motion activated")
        lightsOnLevel()
    }
    else{
        logInfo ("motion lighting switch Off not turning on")
    }
}

def lightsOnLevel(){
    if (state.earlyMorning){
        logInfo ("setting On Level for early morning mode")
        settings.light.setLevel(earlyMorningOn)
    }
    if (state.day){
        logInfo ("setting On Level for day mode")
        settings.light.setLevel(dayOn)
    }
    if (state.afternoon){
        logInfo ("setting On Level for afternoon mode")
        settings.light.setLevel(afternoonOn)
    }
    if (state.dinner){
        logInfo ("setting On Level for dinner mode")
        settings.light.setLevel(dinnerOn)
    }
    if (state.evening){
        logInfo ("setting On Level for evening mode")
        settings.light.setLevel(eveningOn)
    }
    if (state.lateEvening){
        logInfo ("setting On Level for late evening mode")
        settings.light.setLevel(lateEveningOn)
    }
    if (state.night){
        logInfo ("setting On Level for night mode")
        settings.light.setLevel(nightOn)
    }
    if (state.away){
        logInfo ("setting On Level for away mode")
        settings.light.setLevel(awayOn)
    }
}

def inactiveMotionHandler(evt){
    getInactiveMotion()
}

def getInactiveMotion(){
	def active = settings.inactiveMotionSensors.findAll { it?.latestValue("motion") == 'active' }
		if (active){
		    unschedule(motionInactive)
            motionList = "${active}"
            logInfo("motionActive"+motionList)
        }
    else{
       runIn(timeout,motionInactive)
    }
}
def motionInactive(){
    logInfo("All Inactive")
    if (state.switchOk){
        logInfo ("Turning lights Off")
        settings.light.setLevel("0",duration)
    }
    else{
        logInfo ("motion lighting switch off, not turning light off")
    }
}

def roundedAverage(list, transform, precision){
    return (list.sum(transform).toDouble() / list.size()).round(precision)
}

def hoodSensorTemperatureHandler(evt){
    getHoodTemperature()
}
def hoodTemperature(){
    return roundedAverage(settings.hoodSensor, {it.currentTemperature}, 1)
}
def getHoodTemperature(){
    logInfo ("Current temperature average is ${hoodTemperature()}")
    tempHumidityFanHandler()
}

def baselineTemperatureHandler(evt){
    getBaselineTemperature()
}
def baselineTemperature(){
    return roundedAverage(settings.baselineSensors, {it.currentTemperature}, 1)
}
def getBaselineTemperature(){
    logInfo ("Current baseline temperature is ${baselineTemperature()}")
    tempHumidityFanHandler()
}

def hoodSensorHumidityHandler(evt){
    getHoodHumidity()
}
def hoodHumidity(){
    return roundedAverage(settings.hoodSensor, {it.currentHumidity}, 1)
}
def getHoodHumidity(){
    logInfo("Current hood humidity is ${hoodHumidity()}%")
    tempHumidityFanHandler()
}

def baselineHumidityHandler(evt){
    getBaselineHumidity()
}
def baselineHumidity(){
    return roundedAverage(settings.baselineSensors, {it.currentHumidity}, 1)
}
def getBaselineHumidity(){
    logInfo("Current baseline humidity  is ${baselineHumidity()}%")
    tempHumidityFanHandler()
}

def fanSwitchHandler(evt){
    switchStatus = evt.value
    logInfo ("Fan Switch $evt.value")
    state.fanSwitchOn = (evt.value == "on")
    state.fanSwitchOff = (evt.value == "off")
    if (state.fanSwitchOn&&state.tempHumidityFanOff){
        runIn(autoOff*60,autoFanOff)
    }
}

def tempHumidityFanHandler(){
    hoodHumidity = hoodHumidity()
    hoodTemperature = hoodTemperature()
    baselineHumidity = baselineHumidity()
    baselineTemperature = baselineTemperature()
    fanHumidityThreshold = (baselineHumidity + humThreshold)
    fanTemperatureThreshold = (baselineTemperature + tempThreshold)
    state.highHumidity = (hoodHumidity > fanHumidityThreshold)
    state.goodHumidity = (hoodHumidity <= fanHumidityThreshold)
    state.highTemperature = (hoodTemperature > fanTemperatureThreshold)
    state.goodTemperature = (hoodTemperature <= fanTemperatureThreshold)
    if (state.highHumidity||state.highTemperature){
        logInfo ("temp or humidity above threshold")
        state.tempHumidityFanOff = false
        turnFanOn()
    }
    if (state.goodHumidity&&state.goodTemperature){
        logInfo ("temp and humidity below threshold")
        state.tempHumidityFanOff = true
        turnFanOff()
    }
}

def turnFanOn(){
    if (state.fanSwitchOff){
        logInfo ("turning fan ON")
        settings.fanSwitch.on()
    }
    else{
        logInfo ("fan already On, not turning On")
    }
}

def turnFanOff(){
    if (state.fanSwitchOn){
        logInfo ("turning fan Off")
        settings.fanSwitch.off()
    }
    else{
        logInfo ("fan already Off, not turning Off")
    }
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
