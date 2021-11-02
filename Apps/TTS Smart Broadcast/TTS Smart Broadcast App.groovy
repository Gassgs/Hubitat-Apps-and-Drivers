/**
 *  ****************  TTS Smart Broadcast App ****************
 *
 *  Smart TTS anouncements and Push notifications
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
 *  Last Update: 10/24/2021
 *
 *  Changes:
 *
 *  V1.0.0      -       10-24-2021      First run
 *  V1.1.0      -       10-26-2021      Added default Volume and Voice options
 *
 *
 */

import groovy.transform.Field

definition(
    name: "TTS Smart Broadcast",
    namespace: "Gassgs",
    author: "Gary G",
    description: "TTS Smart Broadcast App",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences{
    
    section{
        
        paragraph(
        title: "TTS Smart Broadcast App",
        required: true,
    	"<div style='text-align:center'><b><big>--TTS Smart Broadcast App--</big></b></div>"
     	)
        paragraph(
        title: "TTS Critical Brodcast Device",
        required: true,
    	"<div style='text-align:center'><b>Critical Broadcast Device options</b></div>"
     	)
        input(
            name:"ttsCriticalDevice",
            type:"capability.speechSynthesis",
            title: "<b>TTS Critical Message Device</b> input for high priority messages",
            multiple: false,
            required: true,
            submitOnChange: true
            )
    }
    section{
        input(
            name:"ttsCriticalDevices",
            type:"capability.speechSynthesis",
            title: "<b>TTS Devices</b> for message output",
            multiple: true,
            required: true,
            submitOnChange: true
              )
        input(
            name:"pushCriticalDevices",
            type:"capability.notification",
            title: "<b>Push Devices</b> for message output",
            multiple: true,
            required: true,
            submitOnChange: true
            )
        input(
            name:"criticalVolume",
            type:"number",
            title: "<b>Volume Level</b> for critical messages",
            multiple: false,
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
                name:"defaultCriticalVoice",
                type:"enum", 
                title: "<div style='text-align:center'><b>Default Voice For Critical Announcements</b></div>",
                options: voiceOptions,
                defaultValue: "Amy"
            )
    }
    section{
        
        paragraph(
        title: "TTS Smart Brodcast Device",
        required: true,
    	"<div style='text-align:center'><b><big>Smart Broadcast Device options</big></b></div>"
     	)
        input(
            name:"ttsSmartDevice",
            type:"capability.speechSynthesis",
            title: "<b>TTS Smart Message Device</b> input for everyday messages",
            multiple: false,
            required: true,
            submitOnChange: true
              )
    }
    section{
        
        paragraph(
        title: "TTS Devices by room",
        required: true,
    	"<div style='text-align:center'><b>TTS Devices by Room for message output</b></div>"
     	)
        paragraph(
        title: "Living Room",
        required: true,
    	"<div style='text-align:center'>Living Room</div>"
     	)
        input(
            name:"livingRoomTts",
            type:"capability.speechSynthesis",
            title: "<b>Living Room TTS Device</b> For all messages",
            multiple: false,
            required: true,
            submitOnChange: true
              )
    }
    section{
        paragraph(
        title: "Kitchen",
        required: true,
    	"<div style='text-align:center'>Kitchen</div>"
     	)
            
        input(
            name:"kitchenTts",
            type:"capability.speechSynthesis",
            title: "<b>Kitchen TTS Device</b> For messages while occupied",
            multiple: false,
            required: true,
            submitOnChange: true
              )
        if (kitchenTts){
        input(
            name:"kitchenSensor",
            type:"capability.motionSensor",
            title: "<b>Kitchen Motion Sensor</b>",
            multiple: false,
            required: true,
            submitOnChange: true
              )
            }        
    }
    section{
        paragraph(
        title: "Basement",
        required: true,
    	"<div style='text-align:center'>Basement</div>"
     	)
            
        input(
            name:"basementTts",
            type:"capability.speechSynthesis",
            title: "<b>Basement TTS Device</b> For messages while occupied",
            multiple: false,
            required: true,
            submitOnChange: true
              )
        if (basementTts){
        input(
            name:"basementSensor",
            type:"capability.motionSensor",
            title: "<b>Basement Motion Sensor</b>",
            multiple: false,
            required: true,
            submitOnChange: true
              )
            }        
    }
    section{
        paragraph(
        title: "Bedroom",
        required: true,
    	"<div style='text-align:center'>Bedroom</div>"
     	)
            
        input(
            name:"bedroomTts",
            type:"capability.speechSynthesis",
            title: "<b>Bedroom TTS Device</b> For messages while occupied",
            multiple: false,
            required: true,
            submitOnChange: true
              )
        if (bedroomTts){
        input(
            name:"bedroomSensor",
            type:"capability.motionSensor",
            title: "<b>Bedroom Motion Sensor</b>",
            multiple: false,
            required: true,
            submitOnChange: true
              )
            }        
    }
    section{
        paragraph(
        title: "Spare Room",
        required: true,
    	"<div style='text-align:center'>Spare Room</div>"
     	)
            
        input(
            name:"spareRoomTts",
            type:"capability.speechSynthesis",
            title: "<b>Spare Room TTS Device</b> For messages while occupied",
            multiple: false,
            required: true,
            submitOnChange: true
              )
        if (bedroomTts){
        input(
            name:"spareRoomSensor",
            type:"capability.motionSensor",
            title: "<b>Spare Room Motion Sensor</b>",
            multiple: false,
            required: true,
            submitOnChange: true
              )
            }        
    }
    section{
        
        paragraph(
        title: "Volume per mode",
        required: true,
    	"<div style='text-align:center'><b>Volume Levels Per Mode</b></div>"
     	)
        input(
            name:"earlyMorningVolume",
            type:"number",
            title: "<b>Early Morning</b> Volume Level",
            defaultValue: 50,
            required: true,
            submitOnChange: true
              )
        input(
            name:"dayVolume",
            type:"number",
            title: "<b>Day</b> Volume Level",
            defaultValue: 70,
            required: true,
            submitOnChange: true
              )
        input(
            name:"afternoonVolume",
            type:"number",
            title: "<b>Afternoon</b> Volume Level",
            defaultValue: 80,
            required: true,
            submitOnChange: true
              )
        input(
            name:"dinnerVolume",
            type:"number",
            title: "<b>Dinner</b> Volume Level",
            defaultValue: 80,
            required: true,
            submitOnChange: true
              )
        input(
            name:"eveningVolume",
            type:"number",
            title: "<b>Evening</b> Volume Level",
            defaultValue: 80,
            required: true,
            submitOnChange: true
              )
        input(
            name:"lateEveningVolume",
            type:"number",
            title: "<b>Late Evening</b> Volume Level",
            defaultValue: 70,
            required: true,
            submitOnChange: true
              )
        input(
            name:"nightVolume",
            type:"number",
            title: "<b>Night</b> Volume Level",
            defaultValue: 50,
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
        input(
            name:"defaultSmartVoice",
            type:"enum", 
            title: "<div style='text-align:center'><b>Default Voice For Smart Announcements</b></div>",
            options: voiceOptions,
            defaultValue: "Amy"
            )
    }
    section{
        
        paragraph(
        title: "Push Devices -HOME-",
        required: true,
    	"<div style='text-align:center'><b>Push Devices -HOME-</b></div>"
     	)
        input(
            name:"smartPushHome",
            type:"capability.notification",
            title: "<b>Push Home Devices</b> For all messages",
            multiple: true,
            required: true,
            submitOnChange: true
              )
    }
    section{
        
        paragraph(
        title: "Push Devices -Mobile-",
        required: true,
    	"<div style='text-align:center'><b>Push Devices -MOBILE-</b></div>"
     	)
        input(
            name:"garyPush",
            type:"capability.notification",
            title: "<b>Gary's Mobile Device</b> For messages when away from home",
            multiple: false,
            required: true,
            submitOnChange: true
              )
        if(garyPush){
            input(
            name:"garyPresence",
            type:"capability.presenceSensor",
            title: "<b>Gary presence Device</b>",
            multiple: false,
            required: true,
            submitOnChange: true
              )
        }
        input(
            name:"lynettePush",
            type:"capability.notification",
            title: "<b>Lynette's Mobile Device</b> For messages when away from home",
            multiple: false,
            required: true,
            submitOnChange: true
              )
        if(lynettePush){
        input(
            name:"lynettePresence",
            type:"capability.presenceSensor",
            title: "<b>Lynette presence Device</b>",
            multiple: false,
            required: true,
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
    subscribe(settings.ttsCriticalDevice, "message", criticalMsgHandler)
	subscribe(settings.ttsSmartDevice, "message", smartMsgHandler)
	subscribe(settings.ttsSmartDevice, "volume", smartVolHandler)
    subscribe(settings.ttsCriticalDevice, "voice", criticalVoiceHandler)
	subscribe(settings.ttsSmartDevice, "voice", smartVoiceHandler)
    subscribe(settings.garyPresence, "presence", garyPresenceHandler)
    subscribe(settings.lynettePresence, "presence", lynettePresenceHandler)
    subscribe(settings.kitchenSensor, "motion", kitchenMotionHandler)
    subscribe(settings.basementSensor, "motion", basementMotionHandler)
    subscribe(settings.bedroomSensor, "motion", bedroomMotionHandler)
    subscribe(settings.spareRoomSensor, "motion", spareRoomMotionHandler)
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
    logInfo ("$app.label mode status $mode")
}

def criticalMsgHandler(evt){
    if (evt.value == "clear"){
        //do nothing
    }else{
        logInfo ("Critical Broadcast - $evt.value")
        state.criticalMsg = evt.value as String
        runIn(1,sendCriticalMsg)
    }
}

def criticalVoiceHandler(evt){
    if (evt.value == "clear"){
        //do nothing
    }
    else if (evt.value == "null"){
        state.criticalVoice = settings.defaultCriticalVoice
        logInfo ("Critical Broadcast Voice - Default voice, $defaultCriticalVoice")
    }else{
        logInfo ("Critical Broadcast Voice - $evt.value")
        state.criticalVoice = evt.value as String
    }
}

def sendCriticalMsg(){
    msg = state.criticalMsg as String
    voice = state.criticalVoice as String
    logInfo ("sending tts message $msg at volume $criticalVolume % with Voice $voice")
    settings.ttsCriticalDevices.speak(msg,criticalVolume as Integer,voice)
    settings.ttsCriticalDevices.setVolume(settings.criticalVolume as Integer)
    settings.pushCriticalDevices.deviceNotification(msg)
    runIn(7,resetCriticalDevices)
}

def resetCriticalDevices(){
    settings.ttsCriticalDevices.setVolume(60)
    logInfo ("Critical Devices Volume reset to 60%")
}

def garyPresenceHandler(evt){
    if (evt.value == "not present"){
        state.garyAway = true
        logInfo ("$app.label Gary Presence Away")
    }else{
        state.garyAway = false
        logInfo ("$app.label Gary Presence Home")
    }
}

def lynettePresenceHandler(evt){
    if (evt.value == "not present"){
        state.lynetteAway = true
        logInfo ("$app.label Lynette Presence Away")
    }else{
        state.lynetteAway = false
        logInfo ("$app.label Lynette Presence Home")
    }
}
        
def kitchenMotionHandler(evt){
    if (evt.value == "active"){
        state.kitchenActive = true
    }else{
        state.kitchenActive = false
    }
}

def basementMotionHandler(evt){
    if (evt.value == "active"){
        state.basementActive = true
    }else{
        state.basementActive = false
    }
}

def bedroomMotionHandler(evt){
    if (evt.value == "active"){
        state.bedroomActive = true
    }else{
        state.bedroomActive = false
    }
}

def spareRoomMotionHandler(evt){
    if (evt.value == "active"){
        state.spareRoomActive = true
    }else{
        state.spareRoomActive = false
    }
}

def smartMsgHandler(evt){
    if (evt.value == "clear"){
        //do nothing
    }else{
        logInfo ("Smart Broadcast $evt.value")
        state.smartMsg = evt.value as String
        runIn(1,sendMsg)
    }
}

def smartVoiceHandler(evt){
    if (evt.value == "clear"){
        //do nothing
    }
    else if (evt.value == "null"){
        state.smartVoice = settings.defaultSmartVoice
        logInfo ("Smart Broadcast Voice - Default voice, $defaultSmartVoice")    
    }else{
        logInfo ("Smart Broadcast Voice - $evt.value")
        state.smartVoice = evt.value as String
    }
}

def smartVolHandler(evt){
    if (evt.value == "clear"){
        // do nothing
    }
    else if (evt.value == "null"){
        state.volumeRequested = false
        logInfo ("Smart Volume Level Not Requested level")
    }else{
        logInfo ("Smart Volume requested level $evt.value %")
        state.volumeRequested = true
        state.smartVolume = evt.value as Integer
    }
}

def sendMsg(){
    msg = state.smartMsg as String
    voice = state.smartVoice as String
    if (state.away){
        sendAwayMessage()
        return
    }
    else if (state.volumeRequested){
        modeVolume = state.smartVolume as Integer
    }
    else if (state.earlyMorning){
        modeVolume = settings.earlyMorningVolume
    }
    else if (state.day){
        modeVolume = settings.dayVolume
    }
    else if (state.afternoon){
        modeVolume = settings.afternoonVolume
    }
    else if (state.dinner){
        modeVolume = settings.dinnerVolume
    }
    else if (state.evening){
        modeVolume = settings.eveningVolume
    }
    else if (state.lateEvening){
        modeVolume = settings.lateEveningVolume
    }
    else if (state.night){
        modeVolume = settings.nightVolume
    }
    logInfo ("sending TTS message $msg at volume $modeVolume % with Voice $voice")
    if (state.kitchenActive){
        logInfo ("Kitchen Active - sending TTS message")
        settings.kitchenTts.speak(msg,modeVolume as Integer,voice)
        settings.kitchenTts.setVolume(modeVolume as Integer)
        runIn(7,resetKitchen)
    }
    if (state.basementActive){
        logInfo ("Basement Active - sending TTS message")
        settings.basementTts.speak(msg,modeVolume as Integer,voice)
        settings.basementTts.setVolume(modeVolume as Integer)
        runIn(7,resetBasement)
    }
    if (state.bedroomActive){
        logInfo ("Bedroom Active - sending TTS message")
        settings.bedroomTts.speak(msg,modeVolume as Integer,voice)
        settings.bedroomTts.setVolume(modeVolume as Integer)
        runIn(7,resetBedroom)
    }
    if (state.spareRoomActive){
        logInfo ("Spare Room Active - sending TTS message")
        settings.spareRoomTts.speak(msg,modeVolume as Integer,voice)
        settings.spareRoomTts.setVolume(modeVolume as Integer)
        runIn(7,resetSpareRoom)
    }
    logInfo ("Living Room always - sending TTS message")
    settings.livingRoomTts.speak(msg,modeVolume as Integer,voice)
    settings.livingRoomTts.setVolume(modeVolume as Integer)
    runIn(7,resetLivingRoom)
    
    settings.smartPushHome.deviceNotification(msg)
    logInfo ("Sending Push message to home devices")
    if (state.garyAway){
        settings.garyPush.deviceNotification(msg)
        logInfo ("Sending Push message to Gary's Phone")
    }
    if (state.lynetteAway){
        settings.lynettePush.deviceNotification(msg)
        logInfo ("Sending Push message to Lynette's Phone")
    }
}

def resetKitchen(){
    logInfo ("Kitchen Volume reset to 60%")
    settings.kitchenTts.setVolume(60)
}

def resetBasement(){
    logInfo ("Basement Volume reset to 60%")
    settings.basementTts.setVolume(60)
}

def resetBedroom(){
    logInfo ("Bedroom Volume reset to 60%")
    settings.bedroomTts.setVolume(60)
}

def resetSpareRoom(){
    logInfo ("Spare Room Volume reset to 60%")
    settings.spareRoomTts.setVolume(60)
}

def resetLivingRoom(){
    logInfo ("Living Room Volume reset to 60%")
    settings.livingRoomTts.setVolume(60)
}
    
def sendAwayMessage(){
    logInfo ("Everyone Away, sending messages to Phones instead")
    msg = state.smartMsg as String
    settings.garyPush.deviceNotification(msg)
    settings.lynettePush.deviceNotification(msg)
}

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}
