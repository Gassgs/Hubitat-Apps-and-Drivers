/**
 *  ****************  Auto Door Lock ****************
 *
 *  Timed locking of door by mode. Door locks only when closed and resets timer if reopened.
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
 *  Last Update: 10/26/2021
 *
 *  Changes:
 *
 *  V1.0.0      -       2-16-2021       First run
 *  V1.1.0      -       2-17-2021       Logic improvements
 *  V1.2.0      -       2-19-2021       added notifications
 *  V1.3.0      -       10-21-2021      added notifications volume set "75"
 *  V1.4.0      -       10-26-2021      added Smart TTS Device option, With Voice and Volume options
 */

import groovy.transform.Field

definition(
    name: "Auto Door Lock",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Auto Door Lock App",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences{
    
	section{
        paragraph(
        title: "Auto Door Lock",
        required: true,
    	"<div style='text-align:center'><big><b>Auto Door Lock</b></div></big>"
     	)
     paragraph(
        title: "Auto Door Lock Options",
        required: true,
    	"<b><div style='text-align:center'>Auto Door Lock Options</div></b>"
     	)
        input(
            name:"lock",
            type:"capability.lock",
            title: "Lock To Control",
            multiple: false,
            required: true,
            submitOnChange: true
              )
        input(
            name:"doorSensor",
            type:"capability.contactSensor",
            title: "Door Contact Sensor",
            multiple: false,
            required: true,
            submitOnChange: true
              )
    }
    section{
        paragraph(
        title: "Mode Lock Delay Options",
        required: true,
    	"<div style='text-align:center'><b>Mode Lock Delay Options</b></div>"
     	)
        input(
            name:"earlyMorningDelay",
            type:"number",
            title:"<b>Early Morning</b> Number of minutes to delay locking",
            defaultValue:"5",
            required: true,
            submitOnChange: true
        )
        input(
            name:"dayDelay",
            type:"number",
            title:"<b>Day</b> Number of minutes to delay locking",
            defaultValue:"5",
            required: true,
            submitOnChange: true
        )
        input(
            name:"afternoonDelay",
            type:"number",
            title:"<b>Afternoon</b> Number of minutes to delay locking",
            defaultValue:"5",
            required: true,
            submitOnChange: true
        )
        input(
            name:"dinnerDelay",
            type:"number",
            title:"<b>Dinner</b> Number of minutes to delay locking",
            defaultValue:"5",
            required: true,
            submitOnChange: true
        )
        input(
            name:"eveningDelay",
            type:"number",
            title:"<b>Evening</b> Number of minutes to delay locking",
            defaultValue:"5",
            required: true,
            submitOnChange: true
        )
        input(
            name:"lateEveningDelay",
            type:"number",
            title:"<b>Late Evening</b> Number of minutes to delay locking",
            defaultValue:"5",
            required: true,
            submitOnChange: true
        )
        input(
            name:"nightDelay",
            type:"number",
            title:"<b>Night</b> Number of seconds to delay locking",
            defaultValue:"10",
            required: true,
            submitOnChange: true
        )
        input(
            name:"awayDelay",
            type:"number",
            title:"<b>Away</b> Number of seconds to delay locking",
            defaultValue:"10",
            required: true,
            submitOnChange: true
        )
    }
    section{
        paragraph(
        title: "Notification Options",
        required: true,
    	"<b><div style='text-align:center'>Lock Notification Options</div></b>"
     	)
        input(
            name:"unlockedTimeout",
            type:"number",
            title:"<b>Lock unlocked</b> for how many minutes before notification",
            defaultValue:"45",
            required: true,
            submitOnChange: true
        )
        input(
            name:"doorOpenTimeout",
            type:"number",
            title:"<b>Door left open</b> for how many minutes before notification ",
            defaultValue:"10",
            required: true,
            submitOnChange: true
        )
        paragraph(
        title: "Notification information",
        required: true,
    	"<b><div style='text-align:center'>Lock notifications will also be sent for errors</div></b>"
     	)
    }
    section{
        paragraph(
            title:"TTS Options",
            required: true,
            "<div style='text-align:center'><b>TTS Smart Device Options</b></div>"
            )
        input(
            name:"ttsDevice",
            type:"capability.speechSynthesis",
            title: "TTS Smart notification device",
            multiple: false,
            required: true,
            submitOnChange: true
            )
        input(
            name:"volume",
            type:"number",
            title:"Volume level for message",
            defaultValue: 75,
            required: true,
            submitOnChange: true
            )
            def voiceOptions = [:]
            voiceOptions << ["Nicole" : "Nicole, Female, Australian English"]
            voiceOptions << ["Russell" : "Russell, Male, Australian English"]
		    voiceOptions << ["Amy" : "Amy, Female, British English"]
            voiceOptions << ["Emma" : "Emma, Female, British English"]
            voiceOptions << ["Brian" : "Brian, Male, British English"]
            voiceOptions << ["Aditi" : "Aditi, Female, Indian English"]
            voiceOptions << ["Raveena" : "Raveena, Female, Indian English"]
		    voiceOptions << ["Ivy" : "Ivy, Female, US English"]
            voiceOptions << ["Joanna" : "Joanna, Female, US English"]
            voiceOptions << ["Kendra" : "Kendra, Female, US English"]
            voiceOptions << ["Kimberly" : "Kimberly, Female, US English"]
            voiceOptions << ["Salli" : "Salli, Female, US English"]
            voiceOptions << ["Joey" : "Joey, Male, US English"]
            voiceOptions << ["Justin" : "Justin, Male, US English"]
            voiceOptions << ["Matthew" : "Matthew, Male, US English"]
            voiceOptions << ["Penelope" : "Penelope, Female, US Spanish"]
            voiceOptions << ["Miguel" : "Miguel, Male, US Spanish"]
            voiceOptions << ["Geraint" : "Geraint, Male, Welsh English"]
        input (
            name:"voice",
            type:"enum", 
            title: "Voice Options",
            options: voiceOptions,
            defaultValue: "Amy"
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
    subscribe(lock, "lock", lockHandler)
    subscribe(doorSensor, "contact", doorSensorHandler)
    subscribe(location, "mode", modeEventHandler)
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

def lockHandler(evt){
    lockStatus = evt.value
    logInfo ("Lock $lockStatus")
    state.locked = (lockStatus == "locked")
    state.unlocked = (lockStatus == "unlocked")
    if (state.locked&&state.doorOpen){
        logInfo ("ERROR check door, Lock is locked but the door is still open")
        runIn(5,lockError)
    }
    else if (state.locked){
        logInfo ("Door locked cancelling lock tasks")
        unschedule (lockDoor)
        unschedule (unlockedCheck)
    }
    else if (state.unlocked){
        unschedule (lockDoor)
        logInfo ("starting lock countdown")
        countdownTimer()
        runIn(unlockedTimeout*60,unlockedCheck)
    }
}

def doorSensorHandler(evt){
    contactStatus = evt.value
    logInfo ("Door $contactStatus")
    state.doorOpen = (contactStatus == "open")
    state.doorClosed = (contactStatus == "closed")
    if (state.doorClosed){
        logInfo ("Door Closed Locking after delay")
        unschedule (doorOpenCheck)
    }
    else if (state.doorOpen){
        unschedule (lockDoor)
        logInfo ("starting lock countdown")
        countdownTimer()  
        runIn(doorOpenTimeout*60,doorOpenCheck) 
    }
}

def countdownTimer(){
    if (state.earlyMorning){
        def delay = earlyMorningDelay
        logInfo ("locking in $earlyMorningDelay minutes")
        runIn(delay*60,lockDoor)
    }
    if (state.day){
        def delay = dayDelay
        logInfo ("locking in $dayDelay minutes")
        runIn(delay*60,lockDoor)
    }
    if (state.afternoon){
        def delay = afternoonDelay
        logInfo ("locking in $afternoonDelay minutes")
        runIn(delay*60,lockDoor)
    }
    if (state.dinner){
        def delay = dinnerDelay
        logInfo ("locking in $dinnerDelay minutes")
        runIn(delay*60,lockDoor)
    }
    if (state.evening){
        def delay = eveningDelay
        logInfo ("locking in $eveningDelay minutes")
        runIn(delay*60,lockDoor)
    }
    if (state.lateEvening){
        def delay = lateEveningDelay
        logInfo ("locking in $lateEveningDelay minutes")
        runIn(delay*60,lockDoor)
    }
    if (state.night){
        def delay = nightDelay
        logInfo ("locking in $nightDelay seconds")
        runIn(delay,lockDoor)
    }
    if (state.away){
        def delay = awayDelay
        logInfo ("locking in $awayDelay seconds")
        runIn(delay,lockDoor)
    }
}

def lockDoor(){
    logInfo ("locking door if closed")
    if (state.doorClosed){
        settings.lock.lock()
        runIn(8,checkLock)
    }
    else if (state.doorOpen){
        unschedule (lockDoor)
        logInfo ("starting lock countdown")
        countdownTimer()  
    }
}
def checkLock(){
    if (state.locked)
    logInfo ("door closed and locked")

    else if (state.unLocked){
        logInfo ("lock did not lock, sending error Msg")
        checkLockError()
    }
}

def unlockedCheck(){
    if (state.unlocked){
        logInfo ("Lock has been unlocked longer than timeout threshold sending Msg")
        settings.ttsDevice.speak("Front Door lock has been unlocked for $unlockedTimeout minutes please check front door",settings.volume as Integer,settings.voice as String)
    }
}

def doorOpenCheck(){
    if (state.doorOpen ){
        logInfo ("Door has been open longer than timeout threshold sending Msg")
        settings.ttsDevice.speak("Front Door has been open for $doorOpenTimeout minutes please check door",settings.volume as Integer,settings.voice as String)
    }
}

def lockError(){
    if (state.locked&&state.doorOpen){
        logInfo ("Lock is locked but the door is still open sending Msg")
        settings.ttsDevice.speak("Lock ERROR! the Lock is locked but the front door is still open, please check the front door",settings.volume as Integer,settings.voice as String)
    }
}

def checkLockError(){
    if (state.unlocked){
        logInfo ("Lock was not able to lock - the lock could be jammed-  sending Msg")
        settings.ttsDevice.speak("Lock ERROR! the Lock was not able to lock - the lock could be jammed-, please check the front door",settings.volume as Integer,settings.voice as String)
    }
}

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}
