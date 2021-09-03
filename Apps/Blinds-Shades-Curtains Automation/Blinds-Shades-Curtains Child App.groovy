/**
 *  ****************  Blinds-Shades-Curtains Child App ****************
 *
 *  Mode blinds shades curtains app. Use this app to Set Postion based on Mode.
 *  Motion and Switch options, Close shade when door closed for privacy, plus Lux level, 
 *  Outdoor temp and Window contact sensor options
 *  
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
 *  V1.4.0  -       7-22-2021       Adding features and Improvements
 *  V1.5.0  -       7-27-2021       Bug fixes and Improvements
 *  V1.6.0  -       8-15-2021       Added no change options for afternoon and dinner modes
 *  V1.7.0  -       8-27-2021       Fixed morning retriggering from motion and switches
 *  V1.8.0  -       8-30-2021       Fixed unwanted extra changes triggered by Lux and contact changes.
 */

import groovy.transform.Field

definition(
    name: "Blinds-Shades-Curtains Child",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Blinds-Shades-Curtains Child",
    parent: "Gassgs:Blinds-Shades-Curtains Automation",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences{
    
	section{
        paragraph(
        title: "Blinds-Shades-Curtains Child",
        required: true,
    	"<div style='text-align:center'><b><big>Blinds-Shades-Curtains</big></b></div>"
     	)
     paragraph(
        title: "Blinds-Shades-Curtains Child",
        required: true,
    	"<div style='text-align:center'><b>Blinds-Shades-Curtains Options</b></div>"
     	)
        input(
            name:"shade",
            type:"capability.windowShade",
            title: "<b>Blind, Shade, or Curtain to control</b>",
            multiple: false,
            required: true,
            submitOnChange: true
              )
    }
    section{
        paragraph(
        title: "Privacy Options",
        required: true,
    	"<div style='text-align:center'><b>Privacy Options</b> *optional* </div>"
     	)
        input(
            name:"privacyOption",
            type:"bool",
            title: "Enable to close shade when door closes",
            required: true,
            defaultValue: false,
            submitOnChange: true
        )
        if (privacyOption){
        input(
            name:"door",
            type:"capability.contactSensor",
            title:"<b>Door Sensor</b> When this door closes, close shade for privacy",
            multiple: false,
            required: true,
            submitOnChange: true
        )
        input(
            name:"delay",
            type:"number",
            title:"<b>Delay</b> delay opening shade once door opens, in seconds",
            defaultValue:"5",
            required: true,
            submitOnChange: true
        )
        }
    }
    section{
        paragraph(
        title: "Motion Sensor Option",
        required: true,
    	"<div style='text-align:center'><b>Motion Sensor Option</b> *optional* </div>"
     	)
        input(
            name:"motionOption",
            type:"bool",
            title: "Enable to set early morning position from active motion sensor",
            required: true,
            defaultValue: false,
            submitOnChange: true
        )
        if (motionOption){
        input(
            name:"motionSensor",
            type:"capability.motionSensor",
            title: "<b>Motion sensor</b> When active will enable early morning position",
            multiple: true,
            required: true,
            submitOnChange: true
        )
        }
    }
    section{
        paragraph(
        title: "Switch Opening Option",
        required: true,
    	"<div style='text-align:center'><b>Switch Option</b> *optional* </div>"
     	)
        input(
            name:"switchOpenOption",
            type:"bool",
            title: "Enable to set early morning or day position from Switch",
            required: true,
            defaultValue: false,
            submitOnChange: true
        )
        if (switchOpenOption){
        input(
            name:"openSwitch",
            type:"capability.switch",
            title: "<b>Switch</b> to set early morning or day position",
            multiple: false,
            required: true,
            submitOnChange: true
        )
        input(
            name:"openSwitchOff",
            type:"bool",
            title:"Open shade when switch turns On?  enable for Off",
            defaultValue: false,
            required: true,
            submitOnChange: true
        )
        }
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
            defaultValue:"40",
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
        input(
            name:"afternoonPos",
            type:"number",
            title:"<b>Afternoon</b> position - Enter 0 for no change",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
        input(
            name:"dinnerPos",
            type:"number",
            title:"<b>Dinner</b> position - Enter 0 for no change",
            defaultValue:"50",
            required: true,
            submitOnChange: true
        )
        input(
            name:"eveningPos",
            type:"number",
            title:"<b>Evening</b> position - Enter 0 to enable closing",
            defaultValue:"40",
            required: true,
            submitOnChange: true
        )
        input(
            name:"lateEveningPos",
            type:"number",
            title:"<b>Late Evening</b> position - Enter 0 to enable closing",
            defaultValue:"30",
            required: true,
            submitOnChange: true
        )
        input(
            name:"nightPos",
            type:"number",
            title:"<b>Night</b> position - Enter 0 to enable closing",
            defaultValue:"20",
            required: true,
            submitOnChange: true
        )
    }
    section{
        paragraph(
        title: "Away Mode",
        required: true,
    	"<div style='text-align:center'><b>Away Mode sets day position when lux above threshold and closes when lux below threshold</b></div>"
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
        paragraph(
        title: "Lux Sensor options",
        required: true,
    	"<div style='text-align:center'><b>Lux Sensor Options</b></div>"
     	)
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
        paragraph(
        title: "Additional Closing Options",
        required: true,
    	"<div style='text-align:center'><b>Additional Closing Options</b></div>"
     	)
        input(
            name:"switchCloseOption",
            type:"bool",
            title: "Enable to set close position from Switch -(evening, late evening or night modes)",
            required: true,
            defaultValue: false,
            submitOnChange: true
        )
        if (switchCloseOption){
        input(
            name:"closeSwitch",
            type:"capability.switch",
            title:"<b>Switch</b> to set closed position",
            multiple: false,
            required: true,
            submitOnChange: true
        )
        input(
            name:"closeSwitchOff",
            type:"bool",
            title:"Close shade when switch turns On?  enable for Off",
            defaultValue: false,
            required: true,
            submitOnChange: true
        )
        }
    }
    section{
        input(
            name:"contact",
            type:"capability.contactSensor",
            title:"<b>Window sensor(s)</b> to prevent Shades from closing if window open",
            multiple: true,
            required: false,
            submitOnChange: true
        )
        if (contact){
            input(
            name:"timeout",
            type:"number",
            title:"<b>Close Delay</b> delay in seconds before closing once window is closed",
            defaultValue:"30",
            required: true,
            submitOnChange: true
        )
        }
    }
    section{
        input(
            name:"temperatureSensors",
            type:"capability.temperatureMeasurement",
            title:"<b>Outdoor temperature</b> to disable closing if cold outside to avoid condensation",
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
            title: "Enable Debug logging",
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
	logDebug ("uninstalling app")
}

def updated(){
    logDebug ("Updated with settings: ${settings}")
	unschedule()
    unsubscribe()
	initialize()
}

def initialize(){
    if (settings.door){
    subscribe(settings.door,"contact",doorHandler)
    }
    if (settings.motionSensor){
    subscribe(settings.motionSensor,"motion.active",activeMotionHandler)
    }
    if (settings.openSwitch){
    subscribe(settings.openSwitch,"switch",openSwitchHandler)
    }
    if (settings.closeSwitch){
    subscribe(settings.closeSwitch,"switch",closeSwitchHandler)
    }
    if (settings.contact){
    subscribe(settings.contact,"contact",contactHandler)
    }
    if (settings.temperatureSensors){
    subscribe(settings.temperatureSensors,"temperature",temperatureSensorsHandler)
    }
    if (settings.luxSensors){
    subscribe(settings.luxSensors,"illuminance",illuminanceSensorsHandler)
    }
    subscribe(settings.shade,"switch",shadeHandler)
    subscribe(location,"mode",modeEventHandler)
    if (luxSensors){
        getLux()
    }
    if (temperatureSensors){
        getTemperature()
    }
    if (contact){
        getContacts()
    }
    getValues()
    logDebug ("subscribed to sensor events")
    
}

def getValues(){
    state.shadeActiveMode = true
    state.changeOnLux = true
    state.waitingToClose = false
    mode = location.currentMode as String
    state.earlyMorning = (mode == "Early_morning")
    state.day = (mode == "Day")
    state.afternoon = (mode == "Afternoon")
    state.dinner = (mode == "Dinner")
    state.evening = (mode == "Evening")
    state.lateEvening = (mode == "Late_evening")
    state.night = (mode == "Night")
    state.away = (mode == "Away")
    if (settings.door){
        doorStatus = settings.door.currentValue("contact")
        state.doorOpen = (doorStatus == "open")
        state.doorClosed = (doorStatus == "closed")
    }
    if (settings.shade){
        shadeStatus = settings.shade.currentValue("switch")
        state.shadeOpen = (shadeStatus == "on")
        state.shadeClosed = (shadeStatus == "off")
    }
    if (settings.openSwitch){
        status = settings.openSwitch.currentValue("switch")
        if (openSwitchOff){
            state.openOn = (status == "off")
        }else{
            state.openOn = (status == "on")
        }
    }
    if (settings.closeSwitch){
        value = settings.closeSwitch.currentValue("switch")
        if (closeSwitchOff){
            state.closeOn = (value == "off")
        }else{
            state.closeOn = (value == "on")
        }
    }
    runIn(5,setModePosition)
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
    logDebug ("$app.label mode status now $mode")
}

def shadeHandler(evt){
    shadeStatus = evt.value
    logDebug ("$app.label window shade $shadeStatus")
    state.shadeOpen = (shadeStatus == "on")
    state.shadeClosed = (shadeStatus == "off")
}

def doorHandler(evt){
    doorStatus = evt.value
    logDebug ("$app.label Door $doorStatus")
    state.doorOpen = (doorStatus == "open")
    state.doorClosed = (doorStatus == "closed")
    if (state.doorClosed && state.shadeOpen){
        logDebug ("Door closed, closing Shade")
        settings.shade.close()
    }
    if (state.doorOpen && state.shadeActiveMode){
        runIn(delay,resumeOpen)
    }
}

def resumeOpen(){
    logDebug ("Door opened, resuming Shade Position")
    if (state.earlyMorning){
        settings.shade.setPosition(earlyMorningPos)
    }
    if (state.day){
        settings.shade.setPosition(dayPos)
    }
    if (state.afternoon){
        settings.shade.setPosition(afternoonPos)
    }
    if (state.dinner){
        settings.shade.setPosition(dinnerPos)
    }
    if (state.evening){
        if (eveningPos == 0){
            settings.shade.setPosition(dinnerPos)
        }else{
            settings.shade.setPosition(eveningPos)
        }
    }
    if (state.lateEvening){
        if (lateEveningPos == 0){
            settings.shade.setPosition(dinnerPos)
        }else{
            settings.shade.setPosition(lateEveningPos)
        }
    }
    if (state.night){
        if (nightPos == 0){
            settings.shade.setPosition(dinnerPos)
        }else{
            settings.shade.setPosition(nightPos)
        }
    }
    if (state.away){
        log.warn "why is the door opening when everyone is away"
        settings.shade.setPosition(nightPos)
    }
}    

def activeMotionHandler(evt){
    logDebug ("$app.label motion sensor active")
    if (state.shadeClosed){
        motionActive()
    }
}

def motionActive(){
    if (state.luxOk && state.earlyMorning && !state.shadeActiveMode) {
        logDebug ("motion active setting position $earlyMorningPos for early morning")
        state.shadeActiveMode = true
         if (settings.duration == 1 || settings.duration == 0){
                settings.shade.setPosition(earlyMorningPos)
            }else{
                settings.shade.setLevel(earlyMorningPos,duration)
            }
    }
}

def openSwitchHandler(evt){
    status = evt.value
    if (openSwitchOff){
        state.openOn = (status == "off")
    }else{
        state.openOn = (status == "on")
    }
    if (state.openOn && state.earlyMorning && !state.shadeActiveMode){
        if (state.shadeClosed){
            logDebug ("Open Switch activated, setting early morning position $earlyMorningPos")
            state.shadeActiveMode =true
            if (settings.duration == 1 || settings.duration == 0){
                settings.shade.setPosition(earlyMorningPos)
            }else{
                settings.shade.setLevel(earlyMorningPos,duration)
            }
        }   
    }
    else if (state.openOn && state.day && !state.shadeActiveMode){
        if (state.shadeClosed){
            logDebug ("Open Switch activated, day position $dayPos")
            state.shadeActiveMode =true
            if (settings.duration == 1 || settings.duration == 0){
                settings.shade.setPosition(dayPos)
            }else{
                settings.shade.setLevel(dayPos,duration)
            }
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
	logDebug ("Current lux average is ${averageIlluminance()}")
    state.luxOk = avg >= luxThreshold
    state.luxLow = avg < luxLowThreshold
    if (state.luxOk && !state.shadeActiveMode){
        state.changeOnLux = true
        logDebug (" lux above threshold setting mode position")
        setModePosition()
    }
    if (state.luxLow && state.shadeActiveMode && state.changeOnLux){
        logDebug (" lux below threshold and shade is still open ")
        setModePosition()
        state.changeOnLux = false
    }
}

def closeSwitchHandler(evt){
    status = evt.value
    if (closeSwitchOff){
        state.closeOn = (status == "off")
    }else{
        state.closeOn = (status == "on")
    }
    if (state.closeOn && state.evening || state.closeOn && state.lateEvening || state.closeOn && state.night) {
        logDebug ("Close Switch activated, setting close position")
        shadeClose()
    }       
}

def contactHandler(evt){
    getContacts()
}

def getContacts(){
    def open = settings.contact.findAll { it?.latestValue("contact") == 'open' }
		if (open){
            state.windowOpen = true
            contactList = "${open}"
            logDebug ("contactOpen"+contactList)
        }
    else{
        if (!state.windowOpen && state.waitingToClose){
            runIn(timeout,setModePosition)
            logDebug ("Windows all Closed")
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
	logDebug ("Current temperature average is ${averageTemperature()}")
    state.temperatureNeg = avg <= tempThreshold
}


def setModePosition(){
    logDebug ("Setting Mode Position command called")
    if (state.earlyMorning){
        if (settings.motionSensor || settings.openSwitch){
            logDebug ("waiting for switch or motion to set early morning position")
        }
        else if (state.luxOk){
            logDebug ("Setting early morning position $earlyMorningPos")
            state.shadeActiveMode =true
            if (settings.duration == 1 || settings.duration == 0){
                settings.shade.setPosition(earlyMorningPos)
            }else{
                settings.shade.setLevel(earlyMorningPos,duration)
            }
        }else{
            logDebug ("Lux still to Low, not settting early morning position")
        }
    }
    if (state.day){
        if (settings.openSwitch){
            logDebug ("waiting for switch to set day level")
        }
        else{
            logDebug ("Setting day position $dayPos")
            state.shadeActiveMode = true
            if (settings.duration == 1 || settings.duration == 0){
                settings.shade.setPosition(dayPos)
            }else{
                settings.shade.setLevel(dayPos,duration)
            }
        }
    }
    if (state.afternoon){
        state.shadeActiveMode = true
        currentPos = shade.currentValue("position") as Integer
        logDebug ("current position is $currentPos, afternoon position is $afternoonPos")
        if (currentPos == afternoonPos){
            logDebug ("Currrent Position is already at afternoon position, $afternoonPos no change needed")
        }
        else if (afternoonPos == 0){
            logDebug ("Position Set, No Change Requested")
        }
        else{
            logDebug ("Setting afternoon position $afternoonPos")
            if (settings.duration == 1 || settings.duration == 0){
                settings.shade.setPosition(afternoonPos)
            }else{
                settings.shade.setLevel(afternoonPos,duration)
            }
        }
    }
    if (state.dinner){
        currentPos = shade.currentValue("position") as Integer
        logDebug ("current position is $currentPos, dinner position is $dinnerPos")
        if (currentPos == dinnerPos){
            logDebug ("Currrent Position is already at dinner position, $dinnerPos no change needed")
        }
        else if (dinnerPos == 0){
            logDebug ("Position Set, No Change Requested")
        }
        else{
            logDebug ("Setting dinner position $dinnerPos")
            if (settings.duration == 1 || settings.duration == 0){
                settings.shade.setPosition(dinnerPos)
            }else{
                settings.shade.setLevel(dinnerPos,duration)
            }
        }
    }
    if (state.evening){
        currentPos = shade.currentValue("position") as Integer
        logDebug ("current position is $currentPos, evening position is $eveningPos")
        if (currentPos == eveningPos){
            logDebug ("Currrent Position is already at evening position, $eveningPos no change needed")
        }else{
            if (eveningPos == 0){
                shadesCloseCheck()
            }else if (state.shadeActiveMode) {
                logDebug ("Setting evening position $eveningPos")
                if (settings.duration == 1 || settings.duration == 0){
                    settings.shade.setPosition(eveningPos)
                }else{
                    settings.shade.setLevel(eveningPos,duration)
                }
            }
        }   
    }
    if (state.lateEvening){
        currentPos = shade.currentValue("position") as Integer
        logDebug ("current position is $currentPos, late evening position is $lateEveningPos")
        if (currentPos == lateEveningPos){
            logDebug ("Currrent Position is already at late evening position, $lateEveningPos no change needed")
        }else{
            if (lateEveningPos == 0){
                shadesCloseCheck()
            }else if (state.shadeActiveMode) {
                logDebug ("Setting late evening position $lateEveningPos")
                if (settings.duration == 1 || settings.duration == 0){
                    settings.shade.setPosition(lateEveningPos)
                }else{
                    settings.shade.setLevel(lateEveningPos,duration)
                }
            }
        }   
    }
    if (state.night){
        currentPos = shade.currentValue("position") as Integer
        logDebug ("current position is $currentPos, night position is $nightPos")
        if (currentPos == nightPos){
            logDebug ("Currrent Position is already at night position, $nightPos no change needed")
        }else{
            if (nightPos == 0){
                shadesCloseCheck()
            }else if (state.shadeActiveMode) {
                logDebug ("Setting night position $nightPos")
                if (settings.duration == 1 || settings.duration == 0){
                    settings.shade.setPosition(nightPos)
                }else{
                    settings.shade.setLevel(nightPos,duration)
                }
            }
        }   
    }
    if (state.away){ 
        if (state.luxOk){
            logDebug ("setting position $dayPos for mode- Away")
            state.shadeActiveMode = true
            if (settings.duration == 1 || settings.duration == 0){
                settings.shade.setPosition(dayPos)
            }else{
                settings.shade.setLevel(dayPos,duration)
            }
        }
        if (state.luxLow){
            logDebug ("Lux below threshold checking to close for day Away mode")
            state.shadeActiveMode = false
            if (settings.duration == 1 || settings.duration == 0){
                settings.shade.setPosition(0)
            }else{
                settings.shade.setLevel(0,duration)
            }
        }
    }
}

def shadesCloseCheck(){
    logDebug ("Shade Close Check")
    if (settings.contact && settings.temperatureSensors){
        if (state.windowOpen || state.temperatureNeg){
        state.waitingToClose = true
        logDebug ("window open or temperature below threshold, not closing")
        }
        else if (state.luxLow){
        logDebug ("closing shades")
        shadeClose()
        }
        else{
            logDebug ("waiting for lux to be below threshold to close shades")
        }
    }
    else if (settings.contact){
        if (state.windowOpen){
        state.waitingToClose = true
        logDebug ("window open, not closing")
        }
        else if (state.luxLow){
        logDebug ("closing shades")
        shadeClose()
        }
        else{
            logDebug ("waiting for lux to be below threshold to close shades")
        }
    }
    else if (settings.temperatureSensors){
        if (state.temperatureNeg){
        logDebug ("Temperature below threshold, not closing")
        }
        else if (state.luxLow){
        logDebug ("closing shades")
        shadeClose()
        }
        else{
            logDebug ("waiting for lux to be below threshold to close shades")
        }
    }
    else{
        if (state.luxLow){
        logDebug ("closing shades")
        shadeClose()
        }
        else{
            logDebug ("waiting for lux to be below threshold to close shades")
        }
    }
}

def shadeClose(){
    logDebug ("shade closed")
    state.shadeActiveMode = false
    state.waitingToClose = false
    if (settings.duration == 1 || settings.duration == 0){
        settings.shade.setPosition(0)
    }else{
        settings.shade.setLevel(0,duration)
    }
}

void logDebug(String msg){
	if (settings?.logEnable != false){
		log.debug "$msg"
	}
}
