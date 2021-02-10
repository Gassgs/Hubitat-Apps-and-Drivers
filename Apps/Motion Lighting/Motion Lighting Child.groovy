/**
 *  ****************  Motion Lighting Child App ****************
 *
 *  Simple motion lighting app. Allows dim level instead of "off"
 *  disable app from running with switch,lux and mode options.
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
 *  Last Update: 2/08/2021
 *
 *  Changes:
 *
 *  V1.0.0  -       2-08-2021       First run
 *  V1.1.0  -       2-09-2021       added all modes GG             
 */

import groovy.transform.Field

definition(
    name: "Motion Lighting Child",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Motion Lighting Child",
    parent: "Gassgs:Motion Lighting",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences{
    
	section{
        paragraph(
        title: "Motion Lighting Child",
        required: true,
    	"<div style='text-align:center'><b>Motion Lighting</b></div>"
     	)
     paragraph(
        title: "Motion Lighting Child",
        required: true,
    	"<div style='text-align:center'>Motion Lighting Options</div>"
     	)
        input(
            name:"light",
            type:"capability.switchLevel",
            title: "Light(s) to control",
            multiple: true,
            required: true,
            submitOnChange: true
              )
        input(
            name:"activeMotionSensors",
            type:"capability.motionSensor",
            title: "Motion sensors for active On level",
            multiple: true,
            required: true,
            submitOnChange: true
              )
        input(
            name:"inactiveMotionSensors",
            type:"capability.motionSensor",
            title: "Motion sensors or zone for inactive Off level",
            multiple: true,
            required: true,
            submitOnChange: true
              )
        input(
            name:"timeout",
            type:"number",
            title: "Motion timeout in seconds before inactive action",
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
            name:"earlyMorningOff",
            type:"number",
            title:"<b>Early Morning</b> Off Level",
            defaultValue:"0",
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
            name:"dayOff",
            type:"number",
            title:"<b>Day</b> Off Level",
            defaultValue:"0",
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
            name:"afternoonOff",
            type:"number",
            title:"<b>Afternoon</b> Off Level",
            defaultValue:"0",
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
            name:"dinnerOff",
            type:"number",
            title:"<b>Dinner</b> Off Level",
            defaultValue:"0",
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
            name:"eveningOff",
            type:"number",
            title:"<b>Evening</b> Off Level",
            defaultValue:"0",
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
            name:"lateEveningOff",
            type:"number",
            title:"<b>Late Evening</b> Off Level",
            defaultValue:"0",
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
            name:"nightOff",
            type:"number",
            title:"<b>Night</b> Off Level",
            defaultValue:"0",
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
        input(
            name:"awayOff",
            type:"number",
            title:"<b>Away</b> Off Level",
            defaultValue:"0",
            required: true,
            submitOnChange: true
        )
        input(
            name:"duration",
            type:"number",
            title:"<b>Fade duration</b> for Off Level",
            defaultValue:"1",
            required: true,
            submitOnChange: true
        )
    }
    section{
        input(
            name:"switch",
            type:"capability.switch",
            title:"Switch to disable automatic lighting (optional)",
            multiple: false,
            required: false,
            submitOnChange: true
        )
    }
    section{
        input(
            name:"luxSensors",
            type:"capability.illuminanceMeasurement",
            title:"Lux sensor(s) to disable automatic lighting (optional)",
            multiple: true,
            required: false,
            submitOnChange: true
        )
        if(luxSensors){
            paragraph "Current Lux average is ${averageIlluminance()}"
        }
        if (luxSensors){ 
        input(
            name:"luxThreshold",
            type:"number",
            title:"Lux needs to be below this level for actions to run",
            required: false,
            defaultValue:"200",
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
    subscribe(settings.activeMotionSensors, "motion",  activeMotionHandler)
    subscribe(settings.inactiveMotionSensors, "motion",  inactiveMotionHandler)
    subscribe(settings.switch, "switch",  switchHandler)
    subscribe(settings.luxSensors, "illuminance", illuminanceSensorsHandler)
    subscribe(location, "mode", modeEventHandler)
    if (luxSensors){
        getLux()
    }
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
    runIn(2,getInactiveMotion)
}

def activeMotionHandler(evt){
    motionStatus = evt.value
    logInfo ("motion status $motionStatus")
    state.motionActive = (motionStatus == "active")
    if (state.motionActive){
        motionActive()
    }
}
def motionActive(){
    if (settings.switch&&settings.luxSensors){
        logInfo ("checking switch and lux values")
        checkSwitchLuxOnLevel()
    }
    else if (settings.switch){
        logInfo ("checking switch value")
        checkSwitchOnLevel()
    }
    else if (settings.luxSensors){
        logInfo ("checking lux value")
        checkLuxOnLevel()
    }
    else{
        lightsOnLevel()
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
    if (settings.switch&&settings.luxSensors){
        logInfo ("checking switch and lux values")
        checkSwitchLuxOffLevel()
    }
    else if (settings.switch){
        logInfo ("checking switch value")
        checkSwitchOffLevel()
    }
    else if (settings.luxSensors){
        logInfo ("checking lux value")
        checkLuxOffLevel()
    }
    else{
        lightsOffLevel()
    }
}


def lightsOffLevel(){
    if (state.earlyMorning){
        logInfo ("setting Off Level for early morning mode")
        settings.light.setLevel(earlyMorningOff,duration)
    }
    if (state.day){
        logInfo ("setting Off Level for day mode")
        settings.light.setLevel(dayOff,duration)
    }
    if (state.afternoon){
        logInfo ("setting Off Level for afternoon mode")
        settings.light.setLevel(afternoonOff,duration)
    }
    if (state.dinner){
        logInfo ("setting Off Level for dinner mode")
        settings.light.setLevel(dinnerOff,duration)
    }
    if (state.evening){
        logInfo ("setting Off Level for evening mode")
        settings.light.setLevel(eveningOff,duration)
    }
    if (state.lateEvening){
        logInfo ("setting Off Level for late evening mode")
        settings.light.setLevel(lateEveningOff,duration)
    }
    if (state.night){
        logInfo ("setting Off Level for night mode")
        settings.light.setLevel(nightOff,duration)
    }
    if (state.away){
        logInfo ("setting Off Level for away mode")
        settings.light.setLevel(awayOff,duration)
    }
}


def switchHandler(evt){
    switchStatus = evt.value
    logInfo ("motion lighting switch $switchStatus")
    state.switchOk = (switchStatus == "on")
}

def illuminanceSensorsHandler(evt){
    getLux()
}

def averageIlluminance(){
	def total = 0
    def n = settings.luxSensors.size()
	settings.luxSensors.each {total += it.currentIlluminance}
	return (total /n).toDouble().round()
}

def getLux(){
	def avg = averageIlluminance()
	logInfo ("Current lux average is ${averageIlluminance()}")
    state.luxOk = avg < luxThreshold
}

def checkSwitchLuxOnLevel(){
    if(state.switchOk&&state.luxOk){
        logInfo ("switch and lux values OK")
        lightsOnLevel()
    }
    else{
        logInfo ("switch or lux value false stopping")
    }
}
def checkSwitchLuxOffLevel(){
    if(state.switchOk&&state.luxOk){
        logInfo ("switch and lux values OK")
        lightsOffLevel()
    }
    else{
        logInfo ("switch or lux value false stopping")
    }
}

def checkSwitchOnLevel(){
    if(state.switchOk){
        logInfo ("switch value OK")
        lightsOnLevel()
    }
    else{
        logInfo ("switch value false stopping")
    }
}
def checkSwitchOffLevel(){
    if(state.switchOk){
        logInfo ("switch value OK")
        lightsOffLevel()
    }
    else{
        logInfo ("switch value false stopping")
    }
}

def checkLuxOnLevel(){
    if(state.luxOk){
        logInfo ("lux value OK")
        lightsOnLevel()
    }
    else{
        logInfo ("lux value false stopping")
    }
}
def checkLuxOffLevel(){
    if(state.luxOk){
        logInfo ("lux value OK")
        lightsOffLevel()
    }
    else{
        logInfo ("lux value false stopping")
    }
}

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}
