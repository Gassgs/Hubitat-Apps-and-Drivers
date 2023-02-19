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
 *  Last Update: 12/11/2021
 *
 *  Changes:
 *
 *  V1.0.0      -       10-24-2021      First run
 *  V1.1.0      -       10-26-2021      Added default Volume and Voice options
 *  V1.2.0      -       11-07-2021      Added [HTML] pushover bold and big settings for text
 *  V1.3.0      -       11-23-2021      Added Stop command removed volume options (not working) (coded in per device)
 *  V1.4.0      -       12-11-2021      Improvements and fixes. added send to "home" pushover devices only when ON or Online
 *  V1.5.0      -       12-14-2021      temp fix for issue with 1st gen nest hubs cutting off begining. (removed 12/16)
 *  V1.6.0      -       12-31-2021      Added garage smart area and send TTS to glasses through tasker
 *  V1.7.0      -       03-06-2021      Adjusted Gary Push rule to car stereo when in car or text when away. Not through glasses when home.
 *  V1.8.0      -       03-09-2021      Added Gary personal notification rule to car stereo when in car, text when away,  and glasses when home.
 *  V1.9.0      -       02-01-2023      Added message default to Living Room only when no motion in Kitchen.
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
            name:"pushCriticalMobileDevices",
            type:"capability.notification",
            title: "<b>Pushover Mobile Devices</b> for message output",
            multiple: true,
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
        if (spareRoomTts){
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
        title: "Garage",
        required: true,
    	"<div style='text-align:center'>Garage</div>"
     	)
            
        input(
            name:"garageTts",
            type:"capability.speechSynthesis",
            title: "<b>Garage TTS Device</b> For messages while occupied",
            multiple: false,
            required: true,
            submitOnChange: true
              )
        if (garageTts){
        input(
            name:"garageSensor",
            type:"capability.motionSensor",
            title: "<b>Garage Motion Sensor</b>",
            multiple: false,
            required: true,
            submitOnChange: true
              )
            }
    }
    section{
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
        title: "Pushover Devices -HOME-",
        required: true,
    	"<div style='text-align:center'><b>Pushover Devices -HOME-</b></div>"
     	)
        input(
            name:"livingroomTv",
            type:"capability.notification",
            title: "<b>Living Room TV</b> For push messages",
            multiple: true,
            required: true,
            submitOnChange: true
              )
        if(livingroomTv){
            input(
            name:"livingroomTvSwitch",
            type:"capability.switch",
            title: "<b>Living Room TV On/Off Switch</b>",
            multiple: false,
            required: true,
            submitOnChange: true
              )
        }
        input(
            name:"basementTv",
            type:"capability.notification",
            title: "<b>Basement TV</b> For push messages",
            multiple: true,
            required: true,
            submitOnChange: true
              )
        if(basementTv){
            input(
            name:"basementTvSwitch",
            type:"capability.switch",
            title: "<b>Basement TV On/Off Switch</b>",
            multiple: false,
            required: true,
            submitOnChange: true
              )
        }
        input(
            name:"bedroomTv",
            type:"capability.notification",
            title: "<b>Bedroom TV</b> For push messages",
            multiple: true,
            required: true,
            submitOnChange: true
              )
        if(bedroomTv){
            input(
            name:"bedroomTvSwitch",
            type:"capability.switch",
            title: "<b>Bedroom TV On/Off Switch</b>",
            multiple: false,
            required: true,
            submitOnChange: true
              )
        }
        input(
            name:"ethanRoomTv",
            type:"capability.notification",
            title: "<b>Ethan's Room TV</b> For push messages",
            multiple: true,
            required: true,
            submitOnChange: true
              )
        if(ethanRoomTv){
            input(
            name:"ethanRoomTvSwitch",
            type:"capability.switch",
            title: "<b>Ethan's Room TV On/Off Switch</b>",
            multiple: false,
            required: true,
            submitOnChange: true
              )
        }
    }
    section{
        
        paragraph(
        title: "Pushover Devices -Mobile-",
        required: true,
    	"<div style='text-align:center'><b>Pushover Devices -MOBILE-</b></div>"
     	)
        input(
            name:"garyPush",
            type:"capability.notification",
            title: "<b>Gary's Mobile Device</b> For messages when away from home",
            multiple: false,
            required: true,
            submitOnChange: true
              )
        input(
            name:"garyPushHome",
            type:"capability.notification",
            title: "<b>Gary's Mobile Device</b> For messages when driving",
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
        
        paragraph(
        title: "Gary Smart Notification Device",
        required: true,
    	"<div style='text-align:center'><b><big>Gary Smart Notification Device</big></b></div>"
     	)
        input(
            name:"garySmartDevice",
            type:"capability.speechSynthesis",
            title: "<b>Gary Smart Notification Device</b> input for Gary's messages",
            multiple: false,
            required: true,
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
    subscribe(settings.ttsCriticalDevice, "message", criticalMsgHandler)
	subscribe(settings.ttsSmartDevice, "message", smartMsgHandler)
    subscribe(settings.garySmartDevice, "message", garyMsgHandler)
	subscribe(settings.ttsSmartDevice, "volume", smartVolHandler)
    subscribe(settings.ttsCriticalDevice, "voice", criticalVoiceHandler)
	subscribe(settings.ttsSmartDevice, "voice", smartVoiceHandler)
    subscribe(settings.garyPresence, "presence", garyPresenceHandler)
    subscribe(settings.garyPresence, "inCar", garyCarHandler)
    subscribe(settings.lynettePresence, "presence", lynettePresenceHandler)
    subscribe(settings.kitchenSensor, "motion", kitchenMotionHandler)
    subscribe(settings.basementSensor, "motion", basementMotionHandler)
    subscribe(settings.bedroomSensor, "motion", bedroomMotionHandler)
    subscribe(settings.spareRoomSensor, "motion", spareRoomMotionHandler)
    subscribe(settings.garageSensor, "motion", garageMotionHandler)
    subscribe(settings.livingroomTvSwitch, "switch", livingroomTvSwitchHandler)
    subscribe(settings.basementTvSwitch, "switch", basementTvSwitchHandler)
    subscribe(settings.bedroomTvSwitch, "switch", bedroomTvSwitchHandler)
    subscribe(settings.ethanRoomTvSwitch, "switch", ethanRoomTvSwitchHandler)
    subscribe(location, "mode", modeEventHandler)
    state.away = false
    logInfo ("subscribed to sensor events")
}

def modeEventHandler(evt){
    mode = evt.value
    state.away = (mode == "Away")
    logInfo ("$app.label mode status $mode")
}

def livingroomTvSwitchHandler(evt){
    if (evt.value == "on"){
        state.livingroomTvOn = true
        logInfo ("$app.label Living Room TV On")
    }else{
        state.livingroomTvOn = false
        logInfo ("$app.label Living Room TV Off")
    }
}

def basementTvSwitchHandler(evt){
    if (evt.value == "on"){
        state.basementTvOn = true
        logInfo ("$app.label Basement TV On")
    }else{
        state.basementTvOn = false
        logInfo ("$app.label Basement TV Off")
    }
}

def bedroomTvSwitchHandler(evt){
    if (evt.value == "on"){
        state.bedroomTvOn = true
        logInfo ("$app.label Bedroom TV On")
    }else{
        state.bedroomTvOn = false
        logInfo ("$app.label Bedroom TV Off")
    }
}

def ethanRoomTvSwitchHandler(evt){
    if (evt.value == "on"){
        state.ethansTvOn = true
        logInfo ("$app.label Ethan's TV On")
    }else{
        state.ethansTvOn = false
        logInfo ("$app.label Ethan's TV Off")
    }
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
    settings.ttsCriticalDevices.speak(msg,90,voice)
    settings.ttsCriticalDevices.setVolume(90)
    settings.pushCriticalMobileDevices.deviceNotification(msg as String)
    if (state.livingroomTvOn){
        settings.livingroomTv.deviceNotification("[HTML]<b><big>" + msg as String)
        logInfo ("$app.label Sending Push message to Living Room TV")
    }
    if (state.basementTvOn){
        settings.basementTv.deviceNotification("[HTML]<b><big>" + msg as String)
        logInfo ("$app.label Sending Push message to Basement TV")
    }
    if (state.bedroomTvOn){
        settings.bedroomTv.deviceNotification("[HTML]<b><big>" + msg as String)
        logInfo ("$app.label Sending Push message to  Bedroom TV")
    }
    if (state.ethansTvOn){
        settings.ethanRoomTv.deviceNotification("[HTML]<b><big>" + msg as String)
        logInfo ("$app.label Sending Push message to Ethan's Room TV")
    }
    runIn(15,resetCriticalDevices)
}

def resetCriticalDevices(){
    settings.ttsCriticalDevices.setVolume(80)
    logInfo ("Critical Devices reset")
    settings.ttsCriticalDevices.stop()
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

def garyCarHandler(evt){
    if (evt.value == "true"){
        state.garyInCar = true
        logInfo ("$app.label Gary in Car True")
    }else{
        state.garyInCar = false
        logInfo ("$app.label Gary in Car False")
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

def garageMotionHandler(evt){
    if (evt.value == "active"){
        state.garageActive = true
    }else{
        state.garageActive = false
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
    logInfo ("sending TTS message $msg at volume $modeVolume % with Voice $voice")
    if (state.kitchenActive){
        logInfo ("Kitchen Active - sending TTS message")
        settings.kitchenTts.speak(msg,70,voice)
        settings.kitchenTts.setVolume(70)
        runIn(15,resetKitchen)
    }
    if (state.basementActive){
        logInfo ("Basement Active - sending TTS message")
        settings.basementTts.speak(msg,90,voice)
        settings.basementTts.setVolume(90)
        runIn(15,resetBasement)
    }
    if (state.bedroomActive){
        logInfo ("Bedroom Active - sending TTS message")
        settings.bedroomTts.speak(msg,90,voice)
        settings.bedroomTts.setVolume(90)
        runIn(15,resetBedroom)
    }
    if (state.spareRoomActive){
        logInfo ("Spare Room Active - sending TTS message")
        settings.spareRoomTts.speak(msg,80,voice)
        settings.spareRoomTts.setVolume(80)
        runIn(15,resetSpareRoom)
    }
    if (state.garageActive){
        logInfo ("Garage Active - sending TTS message")
        settings.garageTts.speak(msg,80,voice)
        settings.garageTts.setVolume(80)
        runIn(15,resetGarage)
    }
    if (!state.kitchenActive){
        logInfo ("Living Room always - sending TTS message")
        settings.livingRoomTts.speak(msg,90,voice)
        settings.livingRoomTts.setVolume(90)
        runIn(15,resetLivingRoom)
    }

    logInfo ("Sending Push message to home devices")
    if (state.livingroomTvOn){
        settings.livingroomTv.deviceNotification("[HTML]<b><big>" + msg as String)
        logInfo ("$app.label Sending Push message to Living Room TV")
    }
    if (state.basementOn){
        settings.basementTv.deviceNotification("[HTML]<b><big>" + msg as String)
        logInfo ("$app.label Sending Push message to Basement TV")
    }
    if (state.bedroomTvOn){
        settings.bedroomTv.deviceNotification("[HTML]<b><big>" + msg as String)
        logInfo ("$app.label Sending Push message to Bedroom TV")
    }
    if (state.ethansTvOn){
        settings.ethanRoomTv.deviceNotification("[HTML]<b><big>" + msg as String)
        logInfo ("$app.label Sending Push message to Ethan's Room TV")
    }
    if (state.garyAway && !state.garyInCar){
        settings.garyPush.deviceNotification("[HTML]<b><big>" + msg as String)
        logInfo ("$app.label Sending Push message to Gary's Phone")
    }
    if (state.garyInCar){
        settings.garyPushHome.deviceNotification(msg as String)
        logInfo ("$app.label Sending Push message to Gary's Phone For TTS to Car Stereo")
    }
    if (state.lynetteAway){
        settings.lynettePush.deviceNotification("[HTML]<b><big>" + msg as String)
        logInfo ("$app.label Sending Push message to Lynette's Phone")
    }
}

def resetKitchen(){
    logInfo ("Kitchen Volume reset")
    settings.kitchenTts.setVolume(60)
    settings.kitchenTts.stop()
}

def resetBasement(){
    logInfo ("Basement Volume reset")
    settings.basementTts.setVolume(80)
    settings.basementTts.stop()
}

def resetBedroom(){
    logInfo ("Bedroom Volume reset")
    settings.bedroomTts.setVolume(90)
    settings.bedroomTts.stop()
}

def resetSpareRoom(){
    logInfo ("Spare Room Volume reset")
    settings.spareRoomTts.setVolume(80)
    settings.spareRoomTts.stop()
}

def resetGarage(){
    logInfo ("Garage Volume reset")
    settings.garageTts.setVolume(80)
    settings.garageTts.stop()
}

def resetLivingRoom(){
    logInfo ("Living Room Volume reset")
    settings.livingRoomTts.setVolume(90)
    settings.livingRoomTts.stop()
}
    
def sendAwayMessage(){
    logInfo ("Everyone Away, sending messages to Phones instead")
    msg = state.smartMsg as String
    settings.garyPush.deviceNotification("[HTML]<b><big>" + msg as String)
    settings.lynettePush.deviceNotification("[HTML]<b><big>" + msg as String)
}

def garyMsgHandler(evt){
    if (evt.value == "clear"){
        //do nothing
    }else{
        logInfo ("Gary Notification - $evt.value")
        state.garyMsg = evt.value as String
        runIn(1,sendGaryMsg)
    }
}

def sendGaryMsg(){
    msg = state.garyMsg as String
    if (state.garyAway && !state.garyInCar){
        settings.garyPush.deviceNotification("[HTML]<b><big>" + msg as String)
        logInfo ("$app.label Sending Push message to Gary's Phone")
    }
    if (state.garyInCar || !state.garyAway){
        settings.garyPushHome.deviceNotification(msg as String)
        logInfo ("$app.label Sending Push message to Gary's Phone For TTS to Car Stereo or Glasses")
    }
}

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}
