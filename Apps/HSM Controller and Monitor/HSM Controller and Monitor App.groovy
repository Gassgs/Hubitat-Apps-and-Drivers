/**
 *  ****************  HSM Controller and Monitor App ****************
 *
 * HSM Controller and Monitor 
 * Monitor the status of HSM with a device in Hubitat and Google Home.
 * Automatically arm correct arm rule depending on mode
 * Arm and disarm Away mode with presence
 * Custom Water leak handler to shut off valve after water timeout
 * and close valve only in night and away modes
 * Plus Keypad integration
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
 *  Last Update: 1/27/2021
 *
 *  Changes:
 *
 *  V1.0.0  -       1-27-2021       First run
 *  V2.0.0  -       1-28-2021       Major Improvements and added presence
 *  V2.1.0  -       1-29-2021       Added custom Water Leak Handler
 *  V2.2.0  -       1-31-2021       Added additional Chime device options and improvements
 *  V2.3.0  -       2-17-2021       Google integration Improvements
 *  V2.4.0  -       2-20-2021       Improvements add keypad integration
 *  V2.5.0  -       2-23-2021       Added panic mode option     
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
            title: "<b>Keypads</b> to sync",
            multiple: true,
            required: false,
            submitOnChange: true
            )
        input(
            name:"panicEnable",
            type:"bool",
            title: "Enable PANIC Mode option - code 0911",
            defaultValue: false,
            required: true,
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
        paragraph( "<div style='text-align:center'><b>Aeotec tone device options</b></div>"
                  )
        input(
            name:"chimeDevice1",
            type:"capability.chime",
            title: "<b>Aeotec</b> tone devices for notifications",
            multiple: true,
            required: false,
            submitOnChange: true
            )
        if (chimeDevice1){
        input(
            name:"delaySound1",
            type:"number",
            title:"Sound number to play for delays",
            required: true,
            submitOnChange: true
            )
        input(
            name:"armSound1",
            type:"number",
            title:"Sound number to play for armed",
            required: true,
            submitOnChange: true
            )
         input(
            name:"disarmSound1",
            type:"number",
            title:"Sound number to play for disarmed",
            required: true,
            submitOnChange: true
            )
            input(
            name:"waterSound1",
            type:"number",
            title:"Sound number to play for water leak detection",
            required: true,
            submitOnChange: true
            )
        }
        paragraph( "<div style='text-align:center'><b>Ecolink tone device options</b></div>"
            )
        input(
            name:"chimeDevice2",
            type:"capability.chime",
            title: "<b>Ecolink</b> tone device for notifications",
            multiple: true,
            required: false,
            submitOnChange: true
            )
        if (chimeDevice2){
        input(
            name:"delaySound2",
            type:"number",
            title:"Sound number to play for delays",
            required: true,
            submitOnChange: true
            )
        input(
            name:"armSound2",
            type:"number",
            title:"Sound number to play for armed",
            required: true,
            submitOnChange: true
            )
          input(
            name:"disarmSound2",
            type:"number",
            title:"Sound number to play for disarmed",
            required: true,
            submitOnChange: true
            )
            input(
            name:"waterSound2",
            type:"number",
            title:"Sound number to play for water leak detection",
            required: true,
            submitOnChange: true
            )
        }
        if (chimeDevice1){
            paragraph( "<div style='text-align:center'><b>Delay options</b></div>"
                  )
            input(
                name:"chimeTimer",
                type:"number",
                title: "Arming timer for Home and Night, should match time to arm",
                required: true,
                submitOnChange: true
                )
            input(
                name:"chimeAwayTimer",
                type:"number",
                title: "Arming timer for Away, should match time to arm",
                required: true,
                submitOnChange: true
                )
            input(
                name:"delayChime",
                type:"number",
                title: "Home and Night intrusion delay timer, should match intrusion delay",
                required: true,
                submitOnChange: true
                )
            input(
                name:"delayAwayChime",
                type:"number",
                title: "Away intrusion delay timer, should match intrusion delay",
                required: true,
                submitOnChange: true
                )
        }
    }
    section{
           paragraph( "<div style='text-align:center'><b>Light Options</b></div>"
            )
           input(
            name:"lightsFlash",
            type:"capability.switchLevel",
            title: "<b>Lights</b> to flash for delayed intrusion status",
            multiple: true,
            required: false,
            submitOnChange: true
            )
        input(
            name:"lights",
            type:"capability.colorTemperature",
            title: "<b>Lights</b> to change color for armed and back for disarmed status",
            multiple: true,
            required: false,
            submitOnChange: true
            )
        if (lights){
            input(
                name:"hue",
                type:"number",
                title: "hue (1 -100)",
                required: true,
                submitOnChange: true
            )
            input(
                name:"sat",
                type:"number",
                title: "saturation  (1 -100)",
                required: true,
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
    subscribe(settings.hsmDevice, "alert.arm", hsmSwitchOnHandler)
    subscribe(settings.hsmDevice, "alert.disarm", hsmSwitchOffHandler)
    subscribe(settings.hsmDevice, "alert.clearing", hsmClearHandler)
    subscribe(settings.keypads, "securityKeypad", keypadHandler)
    subscribe(location, "hsmStatus", statusHandler)
    subscribe(location, "hsmAlert", alertHandler)
    subscribe(location, "mode", modeEventHandler)
    subscribe(settings.presenceSensors, "presence", presenceHandler)
    subscribe(settings.waterSensors, "water", waterHandler)
    if(panicEnable){
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
    settings.hsmDevice.hsmUpdate("currentMode","$evt.value")
}

def lastCodeHandler(evt){
    code = evt.value
    logInfo ("** $evt.value **")
    state.panicMode = (code == "pin entered 0911")
    state.panicClear = (code == "Keypads") // user name for code must be "Keypads" to clear
    if (state.panicMode){
        logInfo ("PANIC Mode started")
        settings.hsmDevice.hsmUpdate("alert","active")
        settings.keypads.panic() // a custom panic rule must be set up in HSM. trigger is shock sensor of keypads
    }
    else if (state.disarmed&&state.panicClear){
        logInfo ("PANIC Mode Cleared")
        settings.hsmDevice.clearAlert()
    }
}

def keypadHandler(evt){
    keypadStatus = evt.value
    logInfo ("Keypad Status: $evt.value")
    state.keypadArmedHome = (keypadStatus == "armed home")
    state.keypadArmedNight = (keypadStatus == "armed night")
    state.keypadArmedAway = (keypadStatus == "armed away")
    state.keypadDisarmed = (keypadStatus == "disarmed")
    if (state.keypadArmedHome){
        if (state.disarmed||state.armedAway||state.armedNight){
            sendLocationEvent(name: "hsmSetArm", value: "armHome")
        }
    }
    if (state.keypadArmedNight){
        if (state.disarmed||state.armedAway||state.armedHome){
            sendLocationEvent(name: "hsmSetArm", value: "armNight")
        }
    }
    if (state.keypadArmedAway){
        if (state.disarmed||state.armedNight||state.armedHome){
            sendLocationEvent(name: "hsmSetArm", value: "armAway")
        }
    }
    if (state.keypadDisarmed){
        if (state.armedAway||state.armedNight||state.armedHome){
            sendLocationEvent(name: "hsmSetArm", value: "disarm")
        }     
    }

}

def hsmSwitchOnHandler(evt){
    logInfo ("HSM Status Arming")
    if (state.lateEvening||state.night){
        sendLocationEvent(name: "hsmSetArm", value: "armNight")
    }
    if (state.away){
        sendLocationEvent(name: "hsmSetArm", value: "armAway")
    }
    else{
        sendLocationEvent(name: "hsmSetArm", value: "armHome")
    }
}

def hsmSwitchOffHandler(evt){
    logInfo ("HSM Status Disarming")
    sendLocationEvent(name: "hsmSetArm", value: "disarm")
}

def hsmClearHandler(evt){
    logInfo ("HSM Status Clearing")
    sendLocationEvent(name: "hsmSetArm", value: "cancelAlerts")
    settings.hsmDevice.hsmUpdate("currentAlert","cancel")
}

def statusHandler(evt){
    logInfo ("HSM Status: $evt.value")
    hsmStatus = evt.value
    settings.hsmDevice.hsmUpdate("status","$evt.value")
    state.armedNight = (hsmStatus == "armedNight")
    state.armedAway = (hsmStatus == "armedAway")
    state.armedHome = (hsmStatus == "armedHome")
    state.disarmed = (hsmStatus == "disarmed")
    state.armingNight = (hsmStatus == "armingNight")
    state.armingAway = (hsmStatus == "armingAway")
    state.armingHome = (hsmStatus == "armingHome")
    if (state.armedNight){
        settings.hsmDevice.hsmUpdate("switch","on")
        settings.lights.setColor(hue: settings.hue,saturation: settings.sat)
        settings.chimeDevice1.playSound(armSound1)
        settings.chimeDevice2.playSound(armSound2)
        settings.keypads.armNight()      
    }
    if (state.armedAway){
        settings.hsmDevice.hsmUpdate("switch","on")
        settings.lights.setColor(hue: settings.hue,saturation: settings.sat)
        settings.chimeDevice1.playSound(armSound1)
        settings.chimeDevice2.playSound(armSound2)
        settings.keypads.armAway() 
    }
    if (state.armedHome){
        settings.hsmDevice.hsmUpdate("switch","on")
        settings.lights.setColor(hue: settings.hue,saturation: settings.sat)
        settings.chimeDevice1.playSound(armSound1)
        settings.chimeDevice2.playSound(armSound2)
        settings.keypads.armHome() 
    }
    if (state.disarmed){
        settings.hsmDevice.hsmUpdate("switch","off")
        settings.hsmDevice.clearAlert()
        settings.lights.setColorTemperature("2702")
        settings.chimeDevice1.playSound(disarmSound1)
        settings.chimeDevice2.playSound(disarmSound2)
        settings.keypads.disarm()
        runIn(4,keypadBeepStop)// needed for the xfinity UE's in case they don't stop beeping

    }
    if (state.armingNight){
        logInfo ("arming security system, locking locks")
        settings.lock.lock()
        settings.chimeDevice1.playSound(delaySound1)
        settings.chimeDevice2.playSound(delaySound2)
        runIn(chimeTimer-1,stopChime)
    }
    if (state.armingAway){
        logInfo ("arming security system, locking locks")
        settings.lock.lock()
        settings.chimeDevice1.playSound(delaySound1)
        settings.chimeDevice2.playSound(delaySound2)
        runIn(chimeAwayTimer-1,stopChime)
    }
    if (state.armingHome){
        logInfo ("arming security system, locking locks")
        settings.lock.lock()
        settings.chimeDevice1.playSound(delaySound1)
        settings.chimeDevice2.playSound(delaySound2)
        runIn(chimeTimer-1,stopChime)
    }
}

def stopChime(){
    logInfo ("chime stopped")
    settings.chimeDevice1.stop()
    settings.chimeDevice2.stop()
}

def stopFlash(){
    logInfo ("flashing lights stopped")
    settings.lightsFlash.off()
}

def keypadBeepStop(){
    logInfo ("telling keypads to stop beeping, just in case")
    settings.keypads.stop()
}

def alertHandler(evt){
	logInfo ("HSM Alert: $evt.value" + (evt.value == "rule" ? ",  $evt.descriptionText" : ""))
    alertValue = evt.value
    state.cancelled = (alertValue == "cancel")
    state.failedToArm = (alertValue == "arming")
    state.homeDelay = (alertValue == "intrusion-home-delay")
    state.nightDelay = (alertValue == "intrusion-night-delay")
    state.awayDelay = (alertValue == "intrusion-delay")
    settings.hsmDevice.hsmUpdate("currentAlert","$evt.value" + (evt.value == "rule" ? ",  $evt.descriptionText" : ""))
    settings.hsmDevice.hsmUpdate("alert","active")
    if (state.cancelled){
        logInfo ("Canceling Alerts")
        settings.hsmDevice.hsmUpdate("alert","ok")
    }
    if (state.failedToArm){
        logInfo ("Failed to Arm System")
        runIn(4,resetDisarmed)
    }
    if (state.homeDelay){
        logInfo ("Home delayed intrusion alerts")
        settings.keypads.entry()
        settings.chimeDevice1.playSound(delaySound1)
        settings.chimeDevice2.playSound(delaySound2)
        settings.lightsFlash.flash()
        runIn(delayChime-1,stopChime)
        runIn(delayChime,stopFlash)
        runIn(delayChime,keypadBeepStop)
    }
    if (state.nightDelay){
        logInfo ("Night  delayed intrusion alerts")
        settings.keypads.entry()
        settings.chimeDevice1.playSound(delaySound1)
        settings.chimeDevice2.playSound(delaySound2)
        settings.lightsFlash.flash()
        runIn(delayChime-1,stopChime)
        runIn(delayChime,stopFlash)
        runIn(delayChime,keypadBeepStop)
    }
    if (state.awayDelay){
        logInfo ("Away delayed intrusion alerts")
        settings.keypads.entry()
        settings.chimeDevice1.playSound(delaySound1)
        settings.chimeDevice2.playSound(delaySound2)
        settings.lightsFlash.flash()
        runIn(delayAwayChime-1,stopChime)
        runIn(delayAwayChime,stopFlash)
        runIn(delayAwayChime,keypadBeepStop)
    }
}

def resetDisarmed(){
    logInfo ("resetting to disarmed after failed to arm")
    settings.hsmDevice.off()
    settings.chimeDevice1.playSound(disarmSound1)
    settings.chimeDevice2.playSound(disarmSound2)
    settings.keypads.disarm()
}

def presenceHandler(evt){
    def present = settings.presenceSensors.findAll { it?.latestValue("presence") == 'present' }
		if (present){
            presenceList = "${present}"
            hsmDevice.hsmUpdate("presence","present")
            hsmDevice.hsmUpdate("Home",presenceList)
            logInfo("Home"+presenceList)
            disarmReturn()
        }
    else{
        hsmDevice.hsmUpdate("presence","not present")
        hsmDevice.hsmUpdate("Home","Everyone is  Away")
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
        settings.hsmDevice.hsmUpdate("water","wet")
        settings.hsmDevice.hsmUpdate("Leak",waterList)
        logInfo("leakDetected"+waterList)
        runIn(waterTimeout,waterLeakDetected)
    }
    else{
        unschedule(waterLeakDetected)
        settings.hsmDevice.hsmUpdate("water","dry")
        settings.hsmDevice.hsmUpdate("Leak","All Dry")
    }
}
def waterLeakDetected(){
    if (state.earlyMorning){
        settings.chimeDevice1.playSound(waterSound1)
        settings.chimeDevice2.playSound(waterSound2)
        logInfo ("Leak Detected for longer than timeout limit")
    }
    if (state.day){
        settings.chimeDevice1.playSound(waterSound1)
        settings.chimeDevice2.playSound(waterSound2)
        logInfo ("Leak Detected for longer than timeout limit")
    }
    if (state.afternoon){
        settings.chimeDevice1.playSound(waterSound1)
        settings.chimeDevice2.playSound(waterSound2)
        logInfo ("Leak Detected for longer than timeout limit")
    }
    if (state.dinner){
        settings.chimeDevice1.playSound(waterSound1)
        settings.chimeDevice2.playSound(waterSound2)
        logInfo ("Leak Detected for longer than timeout limit")
    }
    if (state.evening){
        settings.chimeDevice1.playSound(waterSound1)
        settings.chimeDevice2.playSound(waterSound2)
        logInfo ("Leak Detected for longer than timeout limit")
    }
    if (state.lateEvening){
        settings.chimeDevice1.playSound(waterSound1)
        settings.chimeDevice2.playSound(waterSound2)
        logInfo ("Leak Detected for longer than timeout limit")
    }
    if (state.night){
        settings.chimeDevice1.playSound(waterSound1)
        settings.chimeDevice2.playSound(waterSound2)
        settings.valveDevice.close()
        logInfo ("Leak Detected for longer than timeout limit Mode is Night Closing water Valve")
    }
    if (state.away){
        settings.chimeDevice1.playSound(waterSound1)
        settings.chimeDevice2.playSound(waterSound2)
        settings.valveDevice.close()
        logInfo ("Leak Detected for longer than timeout limit Mode is Away Closing water Valve")
    }
}

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}
