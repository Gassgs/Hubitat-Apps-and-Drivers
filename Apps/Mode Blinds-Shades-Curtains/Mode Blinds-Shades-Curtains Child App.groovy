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
 *  V1.2.0  -       2-18-2021       Handler improvements
 *  V1.3.0  -       2-19-2021       Logic redo           
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
    	"<div style='text-align:center'><b><big>Mode Blinds-Shades-Curtains</big></b></div>"
     	)
     paragraph(
        title: "Mode Blinds-Shades-Curtains Child",
        required: true,
    	"<div style='text-align:center'><b>Mode Blinds-Shades-Curtains Options</b></div>"
     	)
        input(
            name:"shade",
            type:"capability.switchLevel",
            title: "<b>Blinds, Shades or Curtains to control</b>",
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
            title:"<b>Early Morning</b> position",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
        input(
            name:"dayPos",
            type:"number",
            title:"<b>Day</b> position",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
        paragraph("<b>Afternoon Mode</b>")
        input(
            name:"afternoonChange",
            type:"bool",
            title: "Enable to change position at afternoon mode",
            required: true,
            defaultValue: false,
            submitOnChange: true
            )
        if (afternoonChange){
        input(
            name:"afternoonPos",
            type:"number",
            title:"<b>Afternoon</b> position",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
        }
        paragraph("<b>Dinner Mode</b>")
        input(
            name:"dinnerChange",
            type:"bool",
            title: "Enable to change position at dinner mode",
            required: true,
            defaultValue: false,
            submitOnChange: true
            )
        if (dinnerChange){
        input(
            name:"dinnerPos",
            type:"number",
            title:"<b>Dinner</b> position",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
        }
        paragraph("<b>Evening Mode</b>")
        input(
            name:"eveningChange",
            type:"bool",
            title: "Enable to change position at evening mode",
            required: true,
            defaultValue: false,
            submitOnChange: true
            )
        if (eveningChange){
        input(
            name:"eveningPos",
            type:"number",
            title:"<b>Evening</b> position",
            defaultValue:"40",
            required: true,
            submitOnChange: true
        )
        }else{
        input(
            name:"eveningOff",
            type:"bool",
            title: "Enable closing at evening mode when Lux below threshold",
            required: true,
            defaultValue: false,
            submitOnChange: true
            )
        }
        paragraph("<b>Late Evening Mode</b>")
        input(
            name:"lateEveningChange",
            type:"bool",
            title: "Enable to change position at late evening mode",
            required: true,
            defaultValue: false,
            submitOnChange: true
            )
        if (lateEveningChange){
        input(
            name:"lateEveningPos",
            type:"number",
            title:"<b>Late Evening</b> position",
            defaultValue:"30",
            required: true,
            submitOnChange: true
        )
        }else{
        input(
            name:"lateEveningOff",
            type:"bool",
            title: "Enable closing at late evening mode when Lux below threshold",
            required: true,
            defaultValue: false,
            submitOnChange: true
            )
        }
        paragraph("<b>Night Mode</b>")
        input(
            name:"nightChange",
            type:"bool",
            title: "Enable to change position at night mode",
            required: true,
            defaultValue: false,
            submitOnChange: true
            )
        if (nightChange){
        input(
            name:"nightPos",
            type:"number",
            title:"<b>Night</b> position",
            defaultValue:"20",
            required: true,
            submitOnChange: true
        )
        }else{
        input(
            name:"nightOff",
            type:"bool",
            title: "Enable closing at night mode when Lux below threshold",
            required: true,
            defaultValue: false,
            submitOnChange: true
            )
        }
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
            title: "<b>Motion sensor</b> will activate early morning position",
            multiple: false,
            required: false,
            submitOnChange: true
              )
    }
    section{
        input(
            name:"luxSensors",
            type:"capability.illuminanceMeasurement",
            title:"<b>Lux sensors</b>",
            multiple: true,
            required: false,
            submitOnChange: true
        )
        if(luxSensors){
            paragraph "<b>Current Lux average is ${averageIlluminance()}</b>"
        }
        if (luxSensors){ 
        input(
            name:"luxThreshold",
            type:"number",
            title:"Lux needs to be above this level to open",
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
            name:"contact",
            type:"capability.contactSensor",
            title:"<b>Contact sensor</b> to prevent Blinds, Shades or Curtains from closing if open",
            multiple: true,
            required: false,
            submitOnChange: true
        )
    }
    section{
        input(
            name:"temperatureSensors",
            type:"capability.temperatureMeasurement",
            title:"<b>Outdoor temperature sensors</b> to disable closing when cold outside",
            multiple: true,
            required: false,
            submitOnChange: true
        )
        if(temperatureSensors){
            paragraph "<b>Current temperature average is ${averageTemperature()}</b>"
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
            defaultValue: false,
            submitOnChange: true
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
        logInfo ("motion active setting position $earlyMorningPos for early morning")
        settings.shade.setLevel(earlyMorningPos,duration)
    }
}

def setModePosition(){
    if (state.earlyMorning){
        if (motionSensor){
            logInfo ("waiting for motion to open shade")
        }
        else if (state.luxOk){
        logInfo ("setting position $earlyMorningPos for early morning mode")
        settings.shade.setLevel(earlyMorningPos,duration)
        }
    }
    if (state.day){
        logInfo ("setting position $dayPos for day mode")
        settings.shade.setLevel(dayPos,duration)
    }
    if (state.afternoon){
        if (settings.afternoonChange){
            logInfo ("setting position $afternoonPos for afternoon mode")
            settings.shade.setLevel(afternoonPos,duration)
        }
        else{
            logInfo ("no position change set for afternoon mode")
        }
    }
    if (state.dinner){
        if (settings.dinnerChange){
            logInfo ("setting position $dinnerPos for dinner mode")
            settings.shade.setLevel(dinnerPos,duration)
        }
        else{
            logInfo ("no position change set for dinner mode")
        }
    }
    if (state.evening){
        if (settings.eveningOff){
            logInfo ("Set to close at evening mode, checking")
            shadesCloseCheck()
        }
        if (settings.eveningChange){
            logInfo ("setting position $eveningPos for evening mode")
            settings.shade.setLevel(eveningPos,duration)
        }
        else{
            logInfo ("no position change set for evening mode")
        }
    }
    if (state.lateEvening){
        if (settings.lateEveningOff){
            logInfo ("Set to close at late evening mode, checking")
            shadesCloseCheck()
        }
        if (settings.lateEveningChange){
            logInfo ("setting position $lateEveningPos for late evening mode")
            settings.shade.setLevel(lateEveningPos,duration)
        }
        else{
            logInfo ("no position change set for late evening mode")
        }
    }
    if (state.night){
        if (settings.nightOff){
            logInfo ("Set to close at night mode, checking")
            shadesCloseCheck()
        }
        if (settings.nightChange){
            logInfo ("setting position $nightPos for night mode")
            settings.shade.setLevel(nightPos,duration)
        }
        else{
            logInfo ("no position change set for night mode")
        }
    }
    if (state.away){
        if (state.luxOk){
            logInfo ("setting position $dayPos for day mode- Away")
            settings.shade.setlevel(dayPos,duration)
        }
        if (state.luxLow){
            logInfo ("Lux below threshold checking to close for day Away mode")
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
        if (state.earlyMoring){
            logInfo (" lux above threshold setting mode position only early morning")
            setModePosition()
        }
        else{
        logInfo (" lux above threshold mode position should already be set")
        }
    }
    if (state.luxLow&&state.shadeOpen){
        logInfo (" lux below threshold and shade is still open ")
        if (state.evening&&settings.eveningOff){
            logInfo (" lux below threshold and set to close at evening, checking ")
            shadesCloseCheck()
        }
        if (state.lateEvening&&settings.lateEveningOff){
            logInfo (" lux below threshold and set to close at late evening, checking ")
            shadesCloseCheck()
        }
        if (state.night&&settings.nightOff){
            logInfo (" lux below threshold and set to close at night, checking ")
            shadesCloseCheck()
        }  
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
