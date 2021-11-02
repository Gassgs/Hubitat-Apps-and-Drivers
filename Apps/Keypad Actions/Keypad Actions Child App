/**
 *  ****************  Keypad Actions Child App ****************
 *
 *  Use 4 digit codes to run actions
 *  Select multiple Keypads
 *  Select multiple devices and custom commands
 *  optional tts anouncements
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
 *  V1.0.0      -       3-04-2021       First run
 *  V1.1.0      -       10-21-2021      added notifications volume set "75"
 *  V1.2.0      -       10-26-2021      added Smart TTS Device option, With Voice and Volume options
 *
 *
 */

import groovy.transform.Field

definition(
    name: "Keypad Actions Child",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Run actions triggered by 4 digit codes",
    parent: "Gassgs:Keypad Actions",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences{
    
	section{
        paragraph(
        title: "Keypad Actions Child",
        required: true,
    	"<div style='text-align:center'><b><big>Keypad Actions</big></b></div>"
     	)
     paragraph(
        title: "Keypad Actions Options",
        required: true,
    	"<div style='text-align:center'><b>Keypad Actions Options</b></div>"
     	)
        input(
            name:"keypads",
            type:"capability.securityKeypad",
            title: "Keypad or multiple Keypads used to trigger",
            multiple: true,
            required: true,
            submitOnChange: true
              )
        input(
            name:"keyCode",
            type:"STRING",
            title: "4 digit code used to trigger action",
            multiple: false,
            required: true,
            submitOnChange: true
            )
    }
    section{
        paragraph(
            title: "Action Options",
            required: true,
            "<div style='text-align:center'><b>Action Options (choose one)</b></div>"
            )
        input(
            name:"toggleEnable",
            type:"bool",
            title: "Enable to toggle a switch On/Off",
            required: true,
            defaultValue: false,
            submitOnChange: true
            )
        if(toggleEnable){
        input(
            name:"toggleDevice",
            type:"capability.switch",
            title:"Switch to toggle On-Off",
            required: true,
            multiple: false,
            submitOnChange: true
            )
        }
        input(
            name:"onOffEnable",
            type:"bool",
            title:"Enable to turn a switch On or Off",
            required: true,
            defaultValue: false,
            submitOnChange: true
        )
        if(onOffEnable){
        input(
            name:"onOffDevice",
            type:"capability.switch",
            title:"Switch to turn On or Off",
            multiple: true,
            required: true,
            defaultValue: false,
            submitOnChange: true
            )
        if(onOffEnable){
        input(
            name:"offEnable",
            type:"bool",
            title:"Default is On, enable to turn device Off",
            required: true,
            defaultValue: false,
            submitOnChange: true
            )
        }
        }
        input(
            name:"lockEnable",
            type:"bool",
            title:"Enable to toggle a Lock",
            required: true,
            defaultValue: false,
            submitOnChange: true
        )
        if(lockEnable){
        input(
            name:"lockDevice",
            type:"capability.lock",
            title:"Lock to toggle Lock-Unlock",
            multiple: true,
            required: true,
            submitOnChange: true
            )
        }
        input(
            name:"ttsEnable",
            type:"bool",
            title:"Enable to send a TTS message Only",
            required: true,
            defaultValue: false,
            submitOnChange: true
        )
    }
    section{
        paragraph(
            title:"TTS Options",
            required: true,
            "<div style='text-align:center'><b>TTS Device Options</b></div>"
            )
        input(
            name:"ttsDevice",
            type:"capability.speechSynthesis",
            title:"TTS device for confirmation announcements (optional)",
            multiple: true,
            required: false,
            submitOnChange: true
        )
        if(ttsDevice){
            input(
                name:"msg",
                type:"STRING",
                title:"Message to speak on TTS devices",
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
	subscribe(settings.keypads, "lastCodeName", keypadHandler)
    if (toggleEnable){
        subscribe(settings.toggleDevice, "switch", switchHandler)
    }
    if (lockEnable){
        subscribe(settings.lockDevice, "lock", lockHandler)
    }
    logInfo ("subscribed to sensor events")
}

def switchHandler(evt){
    switchStatus = evt.value
    logInfo ("Switch $evt.value")
    state.switchOn = (evt.value == "on")
}

def lockHandler(evt){
    lockStatus = evt.value
    logInfo ("Lock $evt.value")
    state.lockLocked = (evt.value == "locked")
}

def keypadHandler(evt){
    code = evt.value
    logInfo ("** $evt.value **")
    state.codeOk = (code == keyCode)
    if (state.codeOk&&toggleEnable){
        logInfo ("toggle code accepted, triggering action")
        toggleAction()
    }
    if (state.codeOk&&onOffEnable){
        logInfo ("On or Off code accepted, triggering action")
        switchAction()
    }
    if (state.codeOk&&lockEnable){
        logInfo ("lock code accepted, triggering lock action")
        lockAction()
    }
    if (state.codeOk&&ttsEnable){
        logInfo ("TTS action code accepted, sending custom msg")
        sendMsg()
    }
}

def toggleAction(){
    if (state.switchOn){
        settings.toggleDevice.off()
        if (ttsDevice){
        sendMsg()
        }
    }
    else{
        settings.toggleDevice.on()
        if (ttsDevice){
        sendMsg()
        }
    }
}

def switchAction(){
    if (offEnable){
        settings.onOffDevice.off()
        if (ttsDevice){
        sendMsg()
        }
    }
    else{
        settings.onOffDevice.on()
        if (ttsDevice){
        sendMsg()
        }
    }
}

def lockAction(){
    if (state.lockLocked){
        settings.lockDevice.unlock()
        if (ttsDevice){
        sendMsg()
        }
    }
    else{
        settings.lockDevice.lock()
        if (ttsDevice){
        sendMsg()
        }
    }
}

def sendMsg(){
    logInfo ("sending tts message $msg at volume $volume % with Voice, $voice")
    settings.ttsDevice.speak(settings.msg,settings.volume as Integer,settings.voice as String)
}   

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}
