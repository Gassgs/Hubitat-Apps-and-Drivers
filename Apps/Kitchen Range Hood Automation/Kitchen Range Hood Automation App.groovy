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
 *  Last Update: 05/21/2022
 *
 *  Changes:
 *
 *  V1.0.0      -       2-15-2021       First run
 *  V1.1.0      -       10-21-2021      Improvements and fixes
 *  V1.2.0      -       11-14-2021      Improvements and fixes
 *  V1.3.0      -       12-05-2021      fixes - "unschedule auto off"
 *  V1.4.0      -       05-21-2022      Added wait time for turning fan off, to avoid mis-turn off's
 *  V1.5.0      -       12-16-2022      Made adjustments for new device driver that combines the light and fan in one.
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
        input(
            name:"hood",
            type:"capability.switchLevel",
            title: "Range Hood to control",
            multiple: false,
            required: true,
            submitOnChange: true
              )
     paragraph(
        title: "Kitchen Range Hood Automation",
        required: true,
    	"<b><div style='text-align:center'>Kitchen Range Hood Light Options</div></b>"
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
            name:"hoodSensor",
            type:"capability.temperatureMeasurement",
            title: "Range Hood temperature sensor",
            multiple: true,
            required: true,
            submitOnChange: true
            )
            if(hoodSensor){
            paragraph "<b>Current hood sensor temperature reading is ${hoodTemperature()}</b>"
        }
        input(
            name:"baselineSensors",
            type:"capability.temperatureMeasurement",
            title: "Baseline temperature sensor(s)",
            required: true,
            multiple: true,
            submitOnChange: true
            )
            if(baselineSensors){
            paragraph "<b>Current baseline temperature average is ${baselineTemperature()}</b>"
        }
        input(
            name:"tempOnThreshold",
            type:"number",
            title:"The Temperature threshold above baseline to trigger ON",
            defaultValue: 8,
            submitOnChange: true
        )
        input(
            name:"tempOffThreshold",
            type:"number",
            title:"The Temperature threshold above baseline to trigger OFF",
            defaultValue: 7,
            submitOnChange: true
        )
        input(
            name:"offTimeout",
            type:"number",
            title:"How long the tempurature must remain below threshold before turning OFF",
            defaultValue: 60,
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
    getValues()
}

def initialize(){
    subscribe(settings.hoodSensor, "temperature", hoodSensorTemperatureHandler)
    subscribe(settings.baselineSensors, "temperature", baselineTemperatureHandler)
    subscribe(settings.hood, "speed",  fanSwitchHandler)
    subscribe(settings.hood, "switch",  lightSwitchHandler)
    subscribe(settings.switch, "switch",  switchHandler)
    subscribe(settings.activeMotionSensors, "motion.active",  activeMotionHandler)
    subscribe(settings.inactiveMotionSensors, "motion",  inactiveMotionHandler)
    subscribe(location, "mode", modeEventHandler)
    logInfo ("subscribed to sensor events")
}

def getValues(){
    state.timerFan = false
    mode = location.currentMode as String
    state.earlyMorning = (mode == "Early_morning")
    state.day = (mode == "Day")
    state.afternoon = (mode == "Afternoon")
    state.dinner = (mode == "Dinner")
    state.evening = (mode == "Evening")
    state.lateEvening = (mode == "Late_evening")
    state.night = (mode == "Night")
    state.away = (mode == "Away")
    if (settings.switch){
        status = settings.switch.currentValue("switch")
        state.switchOk = (status == "on")
    }
    if (settings.hood){
        status = settings.hood.currentValue("speed")
        state.fanSwitchOn = (status == "on")
        state.fanSwitchOff = (status == "off")
    }
    tempFanHandler()
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
        settings.hood.setLevel(earlyMorningOn)
    }
    if (state.day){
        logInfo ("setting On Level for day mode")
        settings.hood.setLevel(dayOn)
    }
    if (state.afternoon){
        logInfo ("setting On Level for afternoon mode")
        settings.hood.setLevel(afternoonOn)
    }
    if (state.dinner){
        logInfo ("setting On Level for dinner mode")
        settings.hood.setLevel(dinnerOn)
    }
    if (state.evening){
        logInfo ("setting On Level for evening mode")
        settings.hood.setLevel(eveningOn)
    }
    if (state.lateEvening){
        logInfo ("setting On Level for late evening mode")
        settings.hood.setLevel(lateEveningOn)
    }
    if (state.night){
        logInfo ("setting On Level for night mode")
        settings.hood.setLevel(nightOn)
    }
    if (state.away){
        logInfo ("setting On Level for away mode")
        settings.hood.setLevel(awayOn)
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
        settings.hood.setLevel(0,duration)
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
    tempFanHandler()
}

def baselineTemperatureHandler(evt){
    getBaselineTemperature()
}
def baselineTemperature(){
    return roundedAverage(settings.baselineSensors, {it.currentTemperature}, 1)
}
def getBaselineTemperature(){
	logInfo ("Current baseline temperature is ${baselineTemperature()}")
    tempFanHandler()
}

def fanSwitchHandler(evt){
    switchStatus = evt.value
    logInfo ("Fan Switch $evt.value")
    state.fanSwitchOn = (evt.value == "on")
    state.fanSwitchOff = (evt.value == "off")
    if (state.fanSwitchOn && state.goodTemperature){
        state.timerFan = true
        logInfo ("Auto Fan on, turning off in $autoOff minutes")
        runIn(autoOff*60,autoFanOff)
    }
    else if (state.fanSwitchOff){
        unschedule(autoFanOff)
        state.timerFan = false
    }
}

def tempFanHandler(){
    hoodTemperature = hoodTemperature()
    baselineTemperature = baselineTemperature()
    fanTemperatureOnThreshold = (baselineTemperature + tempOnThreshold)
    fanTemperatureOffThreshold = (baselineTemperature + tempOffThreshold)
    state.highTemperature = (hoodTemperature > fanTemperatureOnThreshold)
    state.goodTemperature = (hoodTemperature <= fanTemperatureOffThreshold)
    if (state.highTemperature){
        unschedule(turnFanOff)
        logInfo ("temperature above threshold")
        turnFanOn()
    }
    if (state.goodTemperature){
        logInfo ("temperature below threshold")
        if (! state.timerFan){
            logInfo ("If temperature remains below threshold for $offTimeout seconds,fan will turn Off")
            runIn(offTimeout,turnFanOff)
        }
    }
}

def turnFanOn(){
    if (state.fanSwitchOff){
        logInfo ("turning fan ON")
        settings.hood.setSpeed("on")
    }
    else{
        logInfo ("fan already On, not turning On")
        state.timerFan = false
    }
}

def turnFanOff(){
    if (state.fanSwitchOn){
        logInfo ("turning fan Off")
        settings.hood.setSpeed("off")
    }
    else{
        logInfo ("fan already Off, not turning Off")
    }
}

def autoFanOff(){
    logInfo ("auto off timer expired, turning fan Off")
    settings.hood.setSpeed("off")
    state.timerFan = false
}

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}
