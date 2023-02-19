/**
 *  ****************  HSM Controller and Monitor App ****************
 *
 * HSM Controller and Monitor 
 * Monitor the status of HSM with a device in Hubitat and Google Home.
 * Automatically arm correct arm rule depending on mode
 * Arm and disarm Away mode with presence
 * Custom Water leak handler to shut off valve after water timeout
 * and close valve only in night and away modes
 * Plus Keypad integration with Panic option
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
 *  Last Update: 6/30/2021
 *
 *  Changes:
 *
 *  V1.0.0  -       1-27-2021       First run
 *  V2.0.0  -       1-28-2021       Major Improvements and added presence
 *  V2.1.0  -       1-29-2021       Added custom Water Leak Handler
 *  V2.2.0  -       1-31-2021       Added additional Chime device options and improvements
 *  V2.3.0  -       2-17-2021       Google integration Improvements
 *  V2.4.0  -       2-20-2021       Improvements add keypad integration
 *  V2.5.0  -       2-23-2021       Added panic option & cleanup
 *  V2.6.0  -       2-25-2021       Improved Keypad integration/HSM
 *  V2.7.0  -       6-30-2021       Improved update handling
 *  V2.8.0  -       10-8-2021       Removed ecolink siren options
 *  V2.9.0  -       12-24-2022      Removed on/off handlers, moved to driver
 *  V3.0.0  -       02-05-2023      Added options for Kauf RGB Wall switch as indicator
 */

import groovy.transform.Field

definition(
    name: "HSM Controller and Monitor",
    namespace: "Gassgs",
    author: "Gary G",
    description: "HSM Controller and Monitor",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences {

	section {

     paragraph(
         title: "HSM Controller and Monitor",
         required: false,
    	"<div style='text-align:center'><b><big>: HSM Controller and Monitor :</big></b></div>"
    	)
        paragraph( "<div style='text-align:center'><b>HSM Controller and Monitor options</b></div>"
        )
        input(
            name:"hsmDevice",
            type:"capability.switch",
            title: "<b>HSM Controller and Monitoring device </b>",
            required: true
            )
    }
    section{
         paragraph( "<div style='text-align:center'><b>Keypads Options</b></div>"
            )
        input(
            name:"keypads",
            type:"capability.securityKeypad",
            title: "<b>Keypads</b> to use for Panic Mode",
            multiple: true,
            required: false,
            submitOnChange: true
            )
        input(
            name:"panicCode",
            type:"string",
            title: "<b>PANIC Mode</b> code # default - 0911",
            defaultValue: "0911",
            required: false,
            submitOnChange: true
            )
        input(
            name:"userCode",
            type:"string",
            title: "<b>Clear Panic</b> - valid user code name",
            defaultValue: "Keypads",
            required: false,
            submitOnChange: true
            )
    }
    section{
         paragraph( "<div style='text-align:center'><b>Lock options</b></div>"
            )
        input(
            name:"lock",
            type:"capability.lock",
            title: "<b>Locks</b> to lock when arming",
            multiple: true,
            required: false,
            submitOnChange: true
            )
    }
    section{
        paragraph( "<div style='text-align:center'><b>Aeotec Siren 6 device options</b></div>"
                  )
        input(
            name:"chimeDevice",
            type:"capability.chime",
            title: "<b>Aeotec</b> Siren 6 devices for sound notifications",
            multiple: true,
            required: false,
            submitOnChange: true
            )
        if (chimeDevice1){
        input(
            name:"delaySound",
            type:"number",
            title:"Sound number to play for delays",
            required: true,
            defaultValue: 30,
            submitOnChange: true
            )
        input(
            name:"armSound",
            type:"number",
            title:"Sound number to play for armed",
            required: true,
            defaultValue: 29,
            submitOnChange: true
            )
         input(
            name:"disarmSound",
            type:"number",
            title:"Sound number to play for disarmed",
            required: true,
            defaultValue: 29,
            submitOnChange: true
            )
            input(
            name:"waterSound",
            type:"number",
            title:"Sound number to play for water leak detection",
            required: true,
            defaultValue: 14,
            submitOnChange: true
            )
        }
        if (chimeDevice){
            paragraph( "<div style='text-align:center'><b>Delay options</b></div>"
                  )
            input(
                name:"chimeTimer",
                type:"number",
                title: "Arming timer for Home and Night, should match time to arm",
                required: true,
                defaultValue: 5,
                submitOnChange: true
                )
            input(
                name:"chimeAwayTimer",
                type:"number",
                title: "Arming timer for Away, should match time to arm",
                required: true,
                defaultValue: 10,
                submitOnChange: true
                )
            input(
                name:"delayChime",
                type:"number",
                title: "Home and Night intrusion delay timer, should match intrusion delay",
                required: true,
                defaultValue: 10,
                submitOnChange: true
                )
            input(
                name:"delayAwayChime",
                type:"number",
                title: "Away intrusion delay timer, should match intrusion delay",
                required: true,
                defaultValue: 10,
                submitOnChange: true
                )
        }
    }
    section{
           paragraph( "<div style='text-align:center'><b>Light Options</b></div>"
            )
           input(
            name:"lightsFlash",
            type:"capability.switch",
            title: "<b>Lights</b> to flash for delayed intrusion status",
            multiple: true,
            required: false,
            submitOnChange: true
            )
        input(
            name:"lights",
            type:"capability.colorControl",
            title: "<b>Lights</b> to change color for armed and back for disarmed status",
            multiple: true,
            required: false,
            submitOnChange: true
            )
        input(
            name:"rgbSwitch",
            type:"capability.colorControl",
            title: "<b>RGB Switch</b> to change color for armed and water alerts",
            multiple: true,
            required: false,
            submitOnChange: true
            )
        if (lights || rgbSwitch){
            input(
                name:"hue",
                type:"number",
                title: "hue (1 -100)",
                required: true,
                defaultValue: 97,
                submitOnChange: true
            )
            input(
                name:"sat",
                type:"number",
                title: "saturation  (1 -100)",
                required: true,
                defaultValue: 90,
                submitOnChange: true
                )
        }
        }
        section{
             paragraph( "<div style='text-align:center'><b>Presence options</b></div>"
            )
            input(
                name:"presenceSensors",
                type:"capability.presenceSensor",
                title: "<b>Presence Sensors</b> Arm when all Away - Disarm when any arrive",
                multiple: true,
                required: true,
                submitOnChange: true
            )
        }
    section{
         paragraph( "<div style='text-align:center'><b>Leak sensor options</b></div>"
            )
        input(
            name:"waterSensors",
            type:"capability.waterSensor",
            title:"<b>Water leak sensors</b> to monitor",
            multiple: true,
            required: false,
            submitOnChange: true,
            )
        if(waterSensors){
            input(
                name:"waterTimeout",
                type:"number",
                title:"Number of seconds water  needs to report wet before action",
                required: true,
                submitOnChange: true
                )
            input(
                name:"valveDevice",
                type:"capability.valve",
                title:"<b>Water valve</b> to close if water detected -Night and Away Modes only",
                required: false,
                submitOnChange: true
                )
        }
    }
        section{
             paragraph( "<div style='text-align:center'><b>Logging</b></div>"
            )
            input(
                name:"logInfo",
                type:"bool",
                title: "Enable info logging",
                required: true,
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
    logInfo ("Settings: ${settings}")
    subscribe(settings.hsmDevice, "alert.clearing", hsmClearHandler)
    subscribe(location, "hsmStatus", statusHandler)
    subscribe(location, "hsmAlert", alertHandler)
    subscribe(location, "mode", modeEventHandler)
    subscribe(settings.presenceSensors, "presence", presenceHandler)
    subscribe(settings.waterSensors, "water", waterHandler)
    if(keypads){
        subscribe(settings.keypads, "lastCodeName", lastCodeHandler)
    }
    logInfo ("subscribed to Events")
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
    sendEvent(settings.hsmDevice,[name:"currentMode",value:"$evt.value"])
}

def lastCodeHandler(evt){
    code = evt.value
    logInfo ("** $evt.value **")
    state.panicMode = (code == panicCode)
    state.panicClear = (code == userCode)
    if (state.panicMode){
        logInfo ("PANIC Mode started")
        sendEvent(settings.hsmDevice,[name:"alert",value:"active"])
        settings.keypads.panic() // a custom panic rule must be set up in HSM. trigger is shock sensor of keypads
    }
    else if (state.disarmed&&state.panicClear){
        logInfo ("PANIC Mode Cleared")
        settings.hsmDevice.clearAlert()
    }
}

def hsmClearHandler(evt){
    logInfo ("HSM Status Clearing")
    sendLocationEvent(name: "hsmSetArm", value: "cancelAlerts")
    sendEvent(settings.hsmDevice,[name:"currentAlert",value:"cancel"])
    settings.rgbSwitch.rgbOff()
}

def statusHandler(evt){
    logInfo ("HSM Status: $evt.value")
    hsmStatus = evt.value
    state.armedNight = (hsmStatus == "armedNight")
    state.armedAway = (hsmStatus == "armedAway")
    state.armedHome = (hsmStatus == "armedHome")
    state.disarmed = (hsmStatus == "disarmed")
    state.armingNight = (hsmStatus == "armingNight")
    state.armingAway = (hsmStatus == "armingAway")
    state.armingHome = (hsmStatus == "armingHome")
    if (state.armedNight){
        sendEvent(settings.hsmDevice,[name:"status",value:"armed night"])
        sendEvent(settings.hsmDevice,[name:"switch",value:"on"])
        sendEvent(settings.hsmDevice,[name:"exitAllowance",value:0])
        settings.lights.setColor(hue: settings.hue,saturation: settings.sat)
        settings.rgbSwitch.setColor(hue:settings.hue,saturation:100,level:100)
        settings.chimeDevice.playSound(armSound)
        runIn(2,stopChime)
    }
    if (state.armedAway){
        sendEvent(settings.hsmDevice,[name:"status",value:"armed away"])
        sendEvent(settings.hsmDevice,[name:"switch",value:"on"])
        sendEvent(settings.hsmDevice,[name:"exitAllowance",value:0])
        settings.lights.setColor(hue: settings.hue,saturation: settings.sat)
        settings.rgbSwitch.setColor(hue:settings.hue,saturation:100,level:100)
        settings.chimeDevice.playSound(armSound)
        runIn(2,stopChime)
    }
    if (state.armedHome){
        sendEvent(settings.hsmDevice,[name:"status",value:"armed home"])
        sendEvent(settings.hsmDevice,[name:"switch",value:"on"])
        sendEvent(settings.hsmDevice,[name:"exitAllowance",value:0])
        settings.lights.setColor(hue: settings.hue,saturation: settings.sat)
        settings.rgbSwitch.setColor(hue:settings.hue,saturation:100,level:100)
        settings.chimeDevice.playSound(armSound)
        runIn(2,stopChime)
    }
    if (state.disarmed){
        sendEvent(settings.hsmDevice,[name:"status",value:"disarmed"])
        sendEvent(settings.hsmDevice,[name:"switch",value:"off"])
        sendEvent(settings.hsmDevice,[name:"exitAllowance",value:0])
        settings.hsmDevice.clearAlert()
        settings.lights.setColorTemperature(3000)
        settings.rgbSwitch.rgbOff()
        settings.chimeDevice.playSound(disarmSound)
        runIn(2,stopChime)
    }
    if (state.armingNight){
        logInfo ("arming security system, locking locks")
        settings.lock.lock()
        if (settings.chimeTimer != 0){
            logInfo ("playing arming delay sound")
            settings.chimeDevice.playSound(delaySound)
            runIn(chimeTimer-1,stopChime)
        }else{
            logInfo ("no arming delay for night mode set")
        }
    }
    if (state.armingAway){
        logInfo ("arming security system, locking locks")
        settings.lock.lock()
        sendEvent(settings.hsmDevice,[name:"exitAllowance",value:10])
        if (settings.chimeAwayTimer != 0){
            logInfo ("playing arming delay sound")
            settings.chimeDevice.playSound(delaySound)
            runIn(chimeAwayTimer-1,stopChime)
        }else{
            logInfo ("no arming delay for away mode set")
       }         
    }
    if (state.armingHome){
        logInfo ("arming security system, locking locks")
        settings.lock.lock()
        if (settings.chimeTimer != 0){
            logInfo ("playing arming delay sound")
            settings.chimeDevice.playSound(delaySound)
            runIn(chimeTimer-1,stopChime)
        }else{
            logInfo ("no arming delay for home mode set")
        }
    }
}

def stopChime(){
    logInfo ("chime stopped")
    settings.chimeDevice.stop()
    settings.keypads.stop()
}

def stopFlash(){
    logInfo ("flashing lights stopped")
    settings.lightsFlash.flash()
    settings.lightsFlash.off()
}

def alertHandler(evt){
	logInfo ("HSM Alert: $evt.value" + (evt.value == "rule" ? ",  $evt.descriptionText" : ""))
    alertValue = evt.value
    state.cancelled = (alertValue == "cancel")
    state.failedToArm = (alertValue == "arming")
    state.homeDelay = (alertValue == "intrusion-home-delay")
    state.nightDelay = (alertValue == "intrusion-night-delay")
    state.awayDelay = (alertValue == "intrusion-delay")
    sendEvent(settings.hsmDevice,[name:"currentAlert",value:"$evt.value" + (evt.value == "rule" ? ",  $evt.descriptionText" : "")])
    sendEvent(settings.hsmDevice,[name:"alert",value:"active"])
    if (state.cancelled){
        logInfo ("Canceling Alerts")
        sendEvent(settings.hsmDevice,[name:"alert",value:"ok"])
        stopChime()
        
    }
    if (state.failedToArm){
        logInfo ("Failed to Arm System")
        runIn(3,resetDisarmed)
    }
    if (state.homeDelay){
        logInfo ("Home delayed intrusion alerts")
        settings.chimeDevice.playSound(delaySound)
        settings.lightsFlash.flash()
        runIn(delayChime-1,stopChime)
        runIn(delayChime,stopFlash)
    }
    if (state.nightDelay){
        logInfo ("Night  delayed intrusion alerts")
        settings.chimeDevice.playSound(delaySound)
        settings.lightsFlash.flash()
        runIn(delayChime-1,stopChime)
        runIn(delayChime,stopFlash)
    }
    if (state.awayDelay){
        logInfo ("Away delayed intrusion alerts")
        settings.chimeDevice.playSound(delaySound)
        settings.lightsFlash.flash()
        runIn(delayAwayChime-1,stopChime)
        runIn(delayAwayChime,stopFlash)
    }
}

def resetDisarmed(){
    logInfo ("resetting to disarmed after failed to arm")
    settings.hsmDevice.off()
    settings.keypads.disarm()
    settings.chimeDevice.playSound(disarmSound)
}

def presenceHandler(evt){
    def present = settings.presenceSensors.findAll { it?.latestValue("presence") == 'present' }
		if (present){
            presenceList = "${present}"
            sendEvent(settings.hsmDevice,[name:"presence",value:"present"])
            sendEvent(settings.hsmDevice,[name:"Home",value:presenceList])
            logInfo("Home"+presenceList)
            disarmReturn()
        }
    else{
        sendEvent(settings.hsmDevice,[name:"presence",value:"not present"])
        sendEvent(settings.hsmDevice,[name:"Home",value:"Everyone is Away"])
        runIn(2,armAway)
        logInfo("Everyone is Away")
    }
}

def armAway(){
    logInfo ("Everyone Away Arming System")
    settings.hsmDevice.on() 
}

def disarmReturn(){
    if (state.armedAway){
        logInfo (" Arrived Disarming System")
        settings.hsmDevice.off()  
    }
}

def waterHandler(evt){
    wet = settings.waterSensors.findAll {it?.latestValue("water") == 'wet'}
    if (wet){
        waterList = "${wet}"
        sendEvent(settings.hsmDevice,[name:"water",value:"wet"])
        sendEvent(settings.hsmDevice,[name:"Leak",value:waterList])
        logInfo("leakDetected"+waterList)
        runIn(waterTimeout,waterLeakDetected)
    }
    else{
        unschedule(waterLeakDetected)
        sendEvent(settings.hsmDevice,[name:"water",value:"dry"])
        sendEvent(settings.hsmDevice,[name:"Leak",value:"All dry"])
    }
}
def waterLeakDetected(){
    if (state.night||state.away){
        settings.chimeDevice.playSound(waterSound)
        settings.rgbSwitch.setColor(hue:60,saturation:100,level:100)
        settings.valveDevice.close()
        logInfo ("Leak Detected for longer than timeout limit, Mode is Away or Night, Closing water Valve")
    }
    else{
        settings.chimeDevice.playSound(waterSound)
        settings.rgbSwitch.setColor(hue:60,saturation:100,level:100)
        logInfo ("Leak Detected for longer than timeout limit")
    }
}

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}
