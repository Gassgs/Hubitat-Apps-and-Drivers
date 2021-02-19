/**
 *  ****************  Mode Blinds-Shades-Curtains Child App ****************
 *
 *  Mode blinds shades curtains app. Use this app to set postion based on mode.
 *  motion option to activate plus Lux level, outdoor temp, contact sensors
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
 *  Last Update: 2/11/2021
 *
 *  Changes:
 *
 *  V1.0.0  -       2-12-2021       First run
 *  V1.1.0  -       2-13-2021       Logic improvements
 *            
 */

import groovy.transform.Field

definition(
    name: "Mode Blinds-Shades-Curtains Child",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Mode Blinds-Shades-Curtains Child",
    parent: "Gassgs:Mode Blinds-Shades-Curtains",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences{
    
	section{
        paragraph(
        title: "Mode Blinds-Shades-Curtains Child",
        required: true,
    	"<div style='text-align:center'><b>Mode Blinds-Shades-Curtains</b></div>"
     	)
     paragraph(
        title: "Mode Blinds-Shades-Curtains Child",
        required: true,
    	"<div style='text-align:center'>Mode Blinds-Shades-Curtains Options</div>"
     	)
        input(
            name:"shade",
            type:"capability.switchLevel",
            title: "Blinds, Shades or Curtains to control",
            multiple: true,
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
            title:"<b>Evening</b> open position (set to -0- for close)",
            defaultValue:"0",
            required: true,
            submitOnChange: true
        )
        input(
            name:"lateEveningPos",
            type:"number",
            title:"<b>Late Evening</b> open position (set to -0- for close)",
            defaultValue:"0",
            required: true,
            submitOnChange: true
        )
        input(
            name:"nightPos",
            type:"number",
            title:"<b>Night</b> open position (set to -0- for close)",
            defaultValue:"0",
            required: true,
            submitOnChange: true
        )
    }
    section{
        input(
            name:"timeout",
            type:"number",
            title:"<b>Close Delay</b> delay in seconds before closing",
            defaultValue:"30",
            required: true,
            submitOnChange: true
        )
    }
    section{
        input(
            name:"duration",
            type:"number",
            title:"<b>Set Level duration</b> if supported by device",
            defaultValue:"1",
            required: true,
            submitOnChange: true
        )
    }
    section{
        input(
            name:"motionSensor",
            type:"capability.motionSensor",
            title: "Motion sensor will activate early morning position",
            multiple: false,
            required: false,
            submitOnChange: true
              )
    }
    section{
        input(
            name:"contact",
            type:"capability.contactSensor",
            title:"Contact sensor to prevent blinds, shades or Curtains from closing if open",
            multiple: true,
            required: false,
            submitOnChange: true
        )
    }
    section{
        input(
            name:"luxSensors",
            type:"capability.illuminanceMeasurement",
            title:"Lux sensor(s) to disable automatic setting position",
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
            title:"Lux needs to be above this to open",
            required: false,
            defaultValue:"500",
            submitOnChange: true
        )
        }
        if (luxSensors){ 
        input(
            name:"luxLowThreshold",
            type:"number",
            title:"Lux needs to be below this level to close",
            required: false,
            defaultValue:"20",
            submitOnChange: true
        )
        }
    }
    section{
        input(
            name:"temperatureSensors",
            type:"capability.temperatureMeasurement",
            title:"Outdoor temperature sensors to disable automatic closing",
            multiple: true,
            required: false,
            submitOnChange: true
        )
        if(temperatureSensors){
            paragraph "Current temperature average is ${averageTemperature()}"
        }
        if (temperatureSensors){ 
        input(
            name:"tempThreshold",
            type:"number",
            title:"If temperature is below this level Blinds, Shades or Curtains stay open",
            required: false,
            defaultValue:"10",
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
    subscribe(settings.motionSensor, "motion.active",  activeMotionHandler)
    subscribe(settings.contact, "contact",  contactHandler)
    subscribe(settings.shade, "switch",  shadeHandler)
    subscribe(settings.luxSensors, "illuminance", illuminanceSensorsHandler)
    subscribe(settings.temperatureSensors, "temperature", temperatureSensorsHandler)
    subscribe(location, "mode", modeEventHandler)
    if (luxSensors){
        getLux()
    }
    if (temperatureSensors){
        getTemperature()
    }
    if (contact){
        getContacts()
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
    setModePosition()
    logInfo ("mode status $mode")
}

def shadeHandler(evt){
    shadeStatus = evt.value
    logInfo ("window shade $shadeStatus")
    state.shadeOpen = (shadeStatus == "on")
    state.shadeClosed = (shadeStatus == "off")
}

def contactHandler(evt){
    getContacts()
}

def getContacts(){
    def open = settings.contact.findAll { it?.latestValue("contact") == 'open' }
		if (open){
            state.windowOpen = true
            contactList = "${open}"
            logInfo("contactOpen"+contactList)
        }
    else{
        state.windowOpen = false
        setModePosition()
        logInfo("All Closed")
        }
}

def activeMotionHandler(evt){
    logInfo ("motion active")
    if (state.shadeClosed){
        motionActive()
    }
}
def motionActive(){
    if (state.luxOk&&state.earlyMorning){
        logInfo ("setting position")
        settings.shade.setLevel(earlyMorningPos,duration)
    }
}

def setModePosition(){
    if (state.earlyMorning){
        if (motionSensor){
            logInfo ("waiting for motion to open shade")
        }
        else if (state.luxOk){
        logInfo ("setting position for early morning mode")
        settings.shade.setLevel(earlyMorningPos,duration)
        }
    }
    if (state.day){
        logInfo ("setting position for day mode")
        settings.shade.setLevel(dayPos,duration)
    }
    if (state.afternoon){
        logInfo ("setting position for afternoon mode")
        settings.shade.setLevel(afternoonPos,duration)
    }
    if (state.dinner){
        logInfo ("setting position for dinner mode")
        settings.shade.setLevel(dinnerPos,duration)
    }
    if (state.evening){
        state.closeEvening = (eveningPos == 0)
        if (state.closeEvening){
            logInfo ("Pos is set to 0 checking to close")
            shadesCloseCheck()
        }
        else{
            logInfo ("setting position for evening mode")
            settings.shade.setLevel(eveningPos,duration)
        }
    }
    if (state.lateEvening){
        state.closeLateEvening = (lateEveningPos == 0)
        if (state.closeLateEvening){
            logInfo ("Pos is set to 0 checking to close")
            shadesCloseCheck()
        }
        else{
            logInfo ("setting position for late evening mode")
            settings.shade.setLevel(lateEveningPos,duration)
        }
    }
    if (state.night){
        state.closeNight = (nightPos == 0)
        if (state.closeNight){
            logInfo ("Pos is set to 0 checking to close")
            shadesCloseCheck()
        }
        else{
            logInfo ("setting position for night mode")
            settings.shade.setLevel(nightPos,duration)
        }
    }
    if (state.away){
        if (state.luxOk){
            logInfo ("setting position for day mode")
            settings.shade.setlevel(dayPos,duration)
        }
        if (state.luxLow){
            shadesCloseCheck()
        }
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
    state.luxLow = avg < luxLowThreshold
    if (state.luxOk&&state.shadeClosed){
        logInfo (" lux above threshold setting mode position")
        setModePosition()
    }
    if (state.luxLow&&state.shadeOpen){
        logInfo (" lux below threshold setting mode position")
        setModePosition()
    }
}

def temperatureSensorsHandler(evt){
    getTemperature()
}

def averageTemperature(){
	def total = 0
    def n = settings.temperatureSensors.size()
	settings.temperatureSensors.each {total += it.currentTemperature}
	return (total /n).toDouble().round(1)
}

def getTemperature(){
	def avg = averageTemperature()
	logInfo ("Current temperature average is ${averageTemperature()}")
    state.temperatureNeg = avg <= tempThreshold
}

def shadesCloseCheck(){
    if (settings.contact&&settings.temperatureSensors){
        if (state.windowOpen||state.temperatureNeg){
        logInfo ("window open or temperature below threshold, not closing")
        }
        else if (state.luxLow){
        logInfo ("closing shades after timeout")
        runIn(timeout,shadesClose)
        }
    }
    else if (settings.contact){
        if (state.windowOpen){
        logInfo ("window open, not closing")
        }
        else if (state.luxLow){
        logInfo ("closing shades after timeout")
        runIn(timeout,shadesClose)
        }
    }
    else if (settings.temperatureSensors){
        if (state.temperatureNeg){
        logInfo ("Temperature below threshold, not closing")
        }
        else if (state.luxLow){
        logInfo ("closing shades after timeout")
        runIn(timeout,shadesClose)
        }
    }
}

def shadesClose(){
    logInfo ("shade closed")
    settings.shade.setLevel(0,duration)
}

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}
