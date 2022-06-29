/**
 *  ****************  Laundry Washer and Dryer Monitor App 2.0 ****************
 *
 * Virtual Washing Machine and Dryer
 * Monitor the status of the devices in Hubitat and Google Home.
 * Send notifications when cycles complete/ notifications turn off when
 * washer or dryer doors open or when turned off in google home..
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
 *  Last Update: 06/21/2022
 *
 *  Changes:
 *
 *  V1.0.0 -        1-20-2021       First run
 *  V1.1.0 -        1-23-2021       Rewrite
 *  V1.2.0 -        1-24-2021       rewrite ,again
 *  V1.3.0 -        1-25-2021       Complete logic redo improvements
 *  V1.4.0 -        2-11-2021       Improved door/lid event handlers
 *  V1.5.0 -        2-18-2021       Redo -removed vibration sensors not reliable
 *  V1.6.0 -        7-01-2021       Changed update method and device driver
 *  V1.7.0 -        10-23-2021      added notifications volume set "75"
 *  V1.8.0 -        10-26-2021      added Smart TTS Device option, With Voice and Volume options
 *  V1.9.0 -        12-19-2021      added "door" and "machine" attributes to virtual device
 *  V2.0.0 -        06-21-2022      Rewrite for Sonoff S31 power reporting built into virtual device, small tweeks
 */

import groovy.transform.Field

definition(
    name: "Laundry Washer and Dryer Monitor 2.0",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Laundry Washer and Dryer Monitor App 2.0",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences {

	section {

     paragraph(
         title: "Laundry Washer and Dryer Monitor 2.0",
         required: false,
    	"<div style='text-align:center'><b><big>: Laundry Washer and Dryer Monitor 2.0 :</big></b></div>"
    	)
        paragraph( "<div style='text-align:center'><b>Washing Machine options</b></div>"
        )
        input(
            name:"washer",
            type:"capability.switch",
            title: "Virtual Laundry Machine device <b>-Washer-</b>",
            required: true
            )
    }
    section{
        input(
            name:"washerLidSensor",
            type:"capability.switch",
            title:"Contact Sensor switch that monitors the washing machine lid",
            multiple: false,
            submitOnChange: true
            )
    }
    section{
        paragraph("<div style='text-align:center'><b>Dryer options</b></div>"
        )
        input(
            name:"dryer",
            type:"capability.switch",
            title:"Virtual Laundry Machine device <b>-Dryer-</b> ",
            required: true
            )
    }
    section{
        input(
            name:"dryerDoorSensor",
            type:"capability.contactSensor",
            title: "Contact sensor that monitors the dryer door",
            multiple: false,
            submitOnChange: true
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
            defaultValue: 80,
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
    logInfo ("Settings: ${settings}")
    subscribe(settings.washer, "switch", washerSwitchHandler)
    subscribe(settings.dryer, "switch", dryerSwitchHandler)
    subscribe(settings.washerLidSensor, "switch", washerLidHandler)
    subscribe(settings.dryerDoorSensor, "contact", dryerDoorHandler)
    subscribe(settings.washer, "status", washerStatusHandler)
    subscribe(settings.dryer, "status", dryerStatusHandler)
    logInfo ("subscribed to Events")
}

def washerSwitchHandler(evt){
    washerSwitch = evt.value
    logInfo ("Washer switch $washerSwitch")
    state.washerSwitchOn = (washerSwitch == "on")
    state.washerSwitchOff = (washerSwitch == "off")
    if (state.washerSwitchOff){
        unschedule(washerNotifications)
    }
}

def washerLidHandler(evt){
    washerLidStatus = evt.value
    logInfo ("Washer lid $washerLidStatus")
    state.washerLidClosed = (washerLidStatus == "on")
    state.washerLidOpen = (washerLidStatus == "off")
    if (state.washerLidOpen){
        settings.washer.off()
        sendEvent(settings.washer,[name:"status",value:"idle"])
        sendEvent(settings.washer,[name:"machine",value:"empty"])
        sendEvent(settings.washer,[name:"door",value:"open"])
    }else{
        sendEvent(settings.washer,[name:"door",value:"closed"])
    }
        
}

def washerStatusHandler(evt){
    washerStatus = evt.value
    logInfo ("Washer $washerStatus")
    state.washerRunning = (washerStatus == "running")
    state.washerDone = (washerStatus == "done")
    state.washerIdle = (washerStatus == "idle")
    if (state.washerRunning){
        logInfo ("Washer Status running, setting Machine to Full")
        sendEvent(settings.washer,[name:"machine",value:"full"])
    }
    else if (state.washerDone){
        logInfo ("Washer Status Done, sending notifications")
        washerNotifications()
    }    
}

def washerNotifications(){
    logInfo ("Sending washer done message in 10 seconds")
    runIn(10,sendWasherMsg)
}

def sendWasherMsg(){
    if (state.washerSwitchOn && state.washerDone){
        logInfo ("sending washer done message")
        settings.ttsDevice.speak("The Washer is done. Please move the wet laundry to the dryer to stop these messages",settings.volume as Integer,settings.voice as String)
        runIn(15*60,washerNotifications)
    }
    else{
        logInfo ("Washer load complete, message will not be sent")
        sendEvent(settings.washer,[name:"machine",value:"empty"])
    }
}

def dryerSwitchHandler(evt){
    dryerSwitch = evt.value
    logInfo ("Dryer switch $dryerSwitch")
    state.dryerSwitchOn = (dryerSwitch == "on")
    state.dryerSwitchOff = (dryerSwitch == "off")
    if (state.dryerSwitchOff){
        unschedule(sendDryerMsg)
        unschedule(sendDryerMsg2)
    }
}

def dryerDoorHandler(evt){
    dryerDoorStatus = evt.value
    logInfo ("Dryer door $dryerDoorStatus")
    state.dryerDoorClosed = (dryerDoorStatus == "closed")
    state.dryerDoorOpen = (dryerDoorStatus == "open")
    if (state.dryerDoorOpen){
        settings.dryer.off()
        sendEvent(settings.dryer,[name:"status",value:"idle"])
        sendEvent(settings.dryer,[name:"machine",value:"empty"])
        sendEvent(settings.dryer,[name:"door",value:"open"])
    }else{
        sendEvent(settings.dryer,[name:"door",value:"closed"])
    }
}

def dryerStatusHandler(evt){
    dryerStatus = evt.value
    logInfo ("Dryer $dryerStatus")
    state.dryerRunning = (dryerStatus == "running")
    state.dryerDone = (dryerStatus == "done")
    state.dryerIdle = (dryerStatus == "idle")
    if (state.dryerRunning){
        logInfo ("Dryer Running setting Machine to Full")
        sendEvent(settings.dryer,[name:"machine",value:"full"])
    }
    else if (state.dryerDone){
        logInfo ("Dryer Done sending Dryer notifications")
        dryerNotifications()
    }    
}

def dryerNotifications(){
    logInfo ("Sending dryer done message in 10 seconds")
    runIn(10,sendDryerMsg)
}

def sendDryerMsg(){
    if (state.dryerSwitchOn&&state.dryerDone){
        logInfo ("sending dryer notifications")
        settings.ttsDevice.speak("The laundry in the dryer is now dry",settings.volume as Integer,settings.voice as String)
        logInfo ("sending again in 30 minutes")
        runIn(30 *60,sendDryerMsg2)
    }
    else{
        logInfo ("Dryer load complete, message will not be sent")
        sendEvent(settings.dryer,[name:"machine",value:"empty"])
    }
}

def sendDryerMsg2(){
    if (state.dryerSwitchOn&&state.dryerDone){
        logInfo ("sending dryer notifications 2")
        settings.ttsDevice.speak("The laundry in the dryer is dry and can be put away",settings.volume as Integer,settings.voice as String)
        sendEvent(settings.dryer,[name:"switch",value:"off"])
        logInfo ("dryer cycle complete")
    }
    else{
        logInfo ("Dryer load complete, 2nd message will not be sent")
    }
}

void logInfo(String Msg){
    if (settings?.logEnable !=false){
        log.info "$Msg"
    }
}
