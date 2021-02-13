/**
 *  ****************  Motion Blinds and Shades Child App ****************
 *
 *  Motion blinds and shades app. Use this app to set postion when inactive
 *  Close when active. enable activity with Lux levels, window contact sensors
 *  and mode options.
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
 *  V1.2.0  -       2-11-2021       improved lux logic and set position GG           
 */

import groovy.transform.Field

definition(
    name: "Motion Blinds and Shades Child",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Motion Blinds and Shades Child",
    parent: "Gassgs:Motion Automation",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences{
    
	section{
        paragraph(
        title: "Motion Blinds and Shades Child",
        required: true,
    	"<div style='text-align:center'><b>Motion Blinds and Shades</b></div>"
     	)
     paragraph(
        title: "Motion Blinds and Shades Child",
        required: true,
    	"<div style='text-align:center'>Motion Blinds and Shades Options</div>"
     	)
        input(
            name:"shade",
            type:"capability.switchLevel",
            title: "Blinds or Shades to control",
            multiple: true,
            required: true,
            submitOnChange: true
              )
        input(
            name:"activeMotionSensors",
            type:"capability.motionSensor",
            title: "Motion sensor when active closes Blinds or Shades",
            multiple: true,
            required: true,
            submitOnChange: true
              )
        input(
            name:"inactiveMotionSensors",
            type:"capability.motionSensor",
            title: "Motion sensors or zone when inactive sets position",
            multiple: true,
            required: true,
            submitOnChange: true
              )
        input(
            name:"timeout",
            type:"number",
            title: "Motion timeout in seconds before inactive set position",
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
            name:"earlyMorningPos",
            type:"number",
            title:"<b>Early Morning</b> open position",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
        input(
            name:"dayPos",
            type:"number",
            title:"<b>Day</b> open position",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
        input(
            name:"afternoonPos",
            type:"number",
            title:"<b>Afternoon</b> open position",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
        input(
            name:"dinnerPos",
            type:"number",
            title:"<b>Dinner</b> open position",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
        input(
            name:"eveningPos",
            type:"number",
            title:"<b>Evening</b> open position",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
        input(
            name:"lateEveningPos",
            type:"number",
            title:"<b>Late Evening</b> open position",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
        input(
            name:"nightPos",
            type:"number",
            title:"<b>Night</b> open position",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
        input(
            name:"awayPos",
            type:"number",
            title:"<b>Away</b> open Position",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
    }
    section{
        input(
            name:"contact",
            type:"capability.contactSensor",
            title:"Contact sensor to prevent blinds or shades from closing if open",
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
            title:"Lux needs to be above this level for blinds or shades to open",
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
    subscribe(settings.contact, "contact",  contactHandler)
    subscribe(settings.shade, "switch",  shadeHandler)
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

def shadeHandler(evt){
    shadeStatus = evt.value
    logInfo ("window shade $shadeStatus")
    state.shadeOpen = (shadeStatus == "on")
    state.shadeClosed = (shadeStatus == "off")
}

def contactHandler(evt){
    contactStatus = evt.value
    logInfo ("window  $contactStatus")
    state.windowOpen = (contactStatus == "open")
    state.windowClosed = (contactStatus == "closed")
    if (state.windowClosed){
        runIn(10,getInactiveMotion)
    }
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
    if (state.shadeOpen){
        logInfo ("closing shade")
        settings.shade.close()
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
    if (state.luxOk){
        logInfo ("checking lux values")
        setModePosition()
    }
    else if (state.windowOpen){
        logInfo ("checking contact value")
        setModePosition()
    }
    else if (state.shadeOpen){
    logInfo ("Lux is too low and window is closed not setting position, closing")
    settings.shade.close()
    }
}


def setModePosition(){
    if (state.earlyMorning){
        logInfo ("setting position for early morning mode")
        settings.shade.setPosition(earlyMorningPos)
    }
    if (state.day){
        logInfo ("setting position for day mode")
        settings.shade.setPosition(dayPos)
    }
    if (state.afternoon){
        logInfo ("setting position for afternoon mode")
        settings.shade.setPosition(afternoonPos)
    }
    if (state.dinner){
        logInfo ("setting position for dinner mode")
        settings.shade.setPosition(dinnerPos)
    }
    if (state.evening){
        logInfo ("setting position for evening mode")
        settings.shade.setPosition(eveningPos)
    }
    if (state.lateEvening){
        logInfo ("setting position for late evening mode")
        settings.shade.setPosition(lateEveningPos)
    }
    if (state.night){
        logInfo ("setting position for night mode")
        settings.shade.setPosition(nightPos)
    }
    if (state.away){
        logInfo ("setting position for away mode")
        settings.shade.setPosition(awayPos)
    }
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
    state.luxOk = avg >= luxThreshold
    state.luxLow = avg < luxThreshold
    getInactiveMotion()
}

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}
