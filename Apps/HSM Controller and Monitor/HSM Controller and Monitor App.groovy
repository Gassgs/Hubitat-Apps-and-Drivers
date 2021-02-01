/**
 *  ****************  HSM Controller and Monitor App ****************
 *
 * HSM Controller and Monitor 
 * Monitor the status of HSM with a device in Hubitat and Google Home.
 * Automatically arm correct arm rule depending on mode
 * Arm and disarm Away mode with presence
 * Custom Water leak handler to shut off valve after water timeout
 * and close valve only in night and away modes
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
 *  Last Update: 1/27/2021
 *
 *  Changes:
 *
 *  V1.0.0  -       1-27-2021       First run
 *  V2.0.0  -       1-28-2021       Major Improvements and added presence
 *  V2.1.0  -       1-29-2021       Added custom Water Leak Handler
 *  V2.2.0  -       1-31-2021       Added additional Chime device options and improvements       
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
    	"<div style='text-align:center'><b>: HSM Controller and Monitor :</b></div>"
    	)
        paragraph( "<div style='text-align:center'><b>HSM Controller and Monitor options</b></div>"
        )
        input(
            name:"hsmDevice",
            type:"capability.switch",
            title: "  <b>HSM Controller and Monitoring device </b>",
            required: true
            )
    }
    section{
        input(
            name:"lock",
            type:"capability.lock",
            title: "<b>Lock</b> to lock when arming",
            multiple: true,
            required: false,
            submitOnChange: true
            )
    }
    section{
        paragraph( "<div style='text-align:center'><b>Chime device 1 options</b></div>"
                  )
        input(
            name:"chimeDevice1",
            type:"capability.chime",
            title: "Chime device 1 for notifications",
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
            title:"Sound number to play for armed and disarmed",
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
        paragraph( "<div style='text-align:center'><b>Chime device 2 options</b></div>"
            )
        input(
            name:"chimeDevice2",
            type:"capability.chime",
            title: "Chime device 2 for notifications",
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
            title:"Sound number to play for armed and disarmed",
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
            paragraph( "<div style='text-align:center'><b>Chime delay options</b></div>"
                  )
            input(
                name:"chimeTimer",
                type:"number",
                title: "Chime arming timer, should match time to arm",
                required: true,
                submitOnChange: true
                )
            input(
                name:"delayChime",
                type:"number",
                title: "Chime home and night intrusion delay timer, should match intrusion delay",
                required: true,
                submitOnChange: true
                )
            input(
                name:"delayAwayChime",
                type:"number",
                title: "Chime away intrusion delay timer, should match intrusion delay",
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
            title: "Lights to flash for delayed intrusion status",
            multiple: true,
            required: false,
            submitOnChange: true
            )
        input(
            name:"lights",
            type:"capability.colorTemperature",
            title: "Lights change color for armed and back for disarmed status",
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
            input(
                name:"presenceSensors",
                type:"capability.presenceSensor",
                title: "Presence Sensors for Home & Away",
                multiple: true,
                required: true,
                submitOnChange: true
            )
        }
    section{
        input(
            name:"waterSensors",
            type:"capability.waterSensor",
            title:"Water leak sensors to monitor",
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
                title:"Water valve to close if water detected -Night and Away Modes only",
                required: false,
                submitOnChange: true
                )
        }
    }
        section{
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
    subscribe(location, "hsmStatus", statusHandler)
    subscribe(location, "hsmAlert", alertHandler)
    subscribe(location, "mode", modeEventHandler)
    subscribe(settings.presenceSensors, "presence", presenceHandler)
    subscribe(settings.waterSensors, "water", waterHandler)
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

def hsmSwitchOnHandler(evt){
    logInfo ("HSM Status Arming")
    if (state.earlyMorning){
        sendLocationEvent(name: "hsmSetArm", value: "armHome")
    }
    if (state.day){
        sendLocationEvent(name: "hsmSetArm", value: "armHome")
    }
    if (state.afternoon){
        sendLocationEvent(name: "hsmSetArm", value: "armHome")
    }
    if (state.dinner){
        sendLocationEvent(name: "hsmSetArm", value: "armHome")
    }
    if (state.evening){
        sendLocationEvent(name: "hsmSetArm", value: "armHome")
    }
    if (state.lateEvening){
        sendLocationEvent(name: "hsmSetArm", value: "armNight")
    }
    if (state.night){
        sendLocationEvent(name: "hsmSetArm", value: "armNight")
    }
    if (state.away){
        sendLocationEvent(name: "hsmSetArm", value: "armAway")
    }
}

def hsmSwitchOffHandler(evt){
    logInfo ("HSM Status Disarming")
    sendLocationEvent(name: "hsmSetArm", value: "disarm")
    sendLocationEvent(name: "hsmSetArm", value: "cancelAlerts")
    if (state.homeDelay){
        logInfo ("Disarming while armed Home delayed intrusion")
        settings.hsmDevice.hsmUpdate("currentAlert","cancel")
        settings.hsmDevice.hsmUpdate("alert","ok")
    }
    if (state.nightDelay){
        logInfo ("Disarming while armed Night delayed intrusion")
        settings.hsmDevice.hsmUpdate("currentAlert","cancel")
        settings.hsmDevice.hsmUpdate("alert","ok")
    }
    if (state.awayDelay){
        logInfo ("Disarming while armed Away delayed intrusion")
        settings.hsmDevice.hsmUpdate("currentAlert","cancel")
        settings.hsmDevice.hsmUpdate("alert","ok")
    }
}

def hsmClearHandler(evt){
    logInfo ("HSM Status Clearing")
    sendLocationEvent(name: "hsmSetArm", value: "cancelAlerts")
    if (state.homeDelay){
        logInfo ("Clearing while armed Home delayed intrusion")
        settings.hsmDevice.hsmUpdate("currentAlert","cancel")
        settings.hsmDevice.off()
    }
    if (state.nightDelay){
        logInfo ("Clearing while armed Night delayed intrusion")
        settings.hsmDevice.hsmUpdate("currentAlert","cancel")
        settings.hsmDevice.off()
    }
    if (state.awayDelay){
        logInfo ("Clearing while armed Away delayed intrusion")
        settings.hsmDevice.hsmUpdate("currentAlert","cancel")
        settings.hsmDevice.off()
    }
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
    }
    if (state.armedAway){
        settings.hsmDevice.hsmUpdate("switch","on")
        settings.lights.setColor(hue: settings.hue,saturation: settings.sat)
        settings.chimeDevice1.playSound(armSound1)
        settings.chimeDevice2.playSound(armSound2)
    }
    if (state.armedHome){
        settings.hsmDevice.hsmUpdate("switch","on")
        settings.lights.setColor(hue: settings.hue,saturation: settings.sat)
        settings.chimeDevice1.playSound(armSound1)
        settings.chimeDevice2.playSound(armSound2)
    }
    if (state.disarmed){
        settings.hsmDevice.hsmUpdate("switch","off")
        settings.lights.setColorTemperature("2702")
        settings.chimeDevice1.playSound(armSound1)
        settings.chimeDevice2.playSound(armSound2)
    }
    if (state.armingNight){
        settings.hsmDevice.hsmUpdate("switch","on")
        settings.lock.lock()
        settings.chimeDevice1.playSound(delaySound1)
        settings.chimeDevice2.playSound(delaySound2)
        runIn(chimeTimer-1,stopChime)
    }
    if (state.armingAway){
        settings.hsmDevice.hsmUpdate("switch","on")
        settings.lock.lock()
        settings.chimeDevice1.playSound(delaySound1)
        settings.chimeDevice2.playSound(delaySound2)
        runIn(chimeTimer-1,stopChime)
    }
    if (state.armingHome){
        settings.hsmDevice.hsmUpdate("switch","on")
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
        settings.chimeDevice1.playSound(armSound1)
        settings.chimeDevice2.playSound(armSound2)
        settings.hsmDevice.hsmUpdate("alert","ok")
    }
    if (state.failedToArm){
        logInfo ("Failed to Arm System")
        runIn(5,resetDisarmed)
    }
    if (state.homeDelay){
        settings.chimeDevice1.playSound(delaySound1)
        settings.chimeDevice2.playSound(delaySound2)
        settings.lightsFlash.flash()
        runIn(delayChime-1,stopChime)
        runIn(delayChime,stopFlash)
    }
    if (state.nightDelay){
        settings.chimeDevice1.playSound(delaySound1)
        settings.chimeDevice2.playSound(delaySound2)
        settings.lightsFlash.flash()
        runIn(delayChime-1,stopChime)
        runIn(delayChime,stopFlash)
    }
    if (state.awayDelay){
        settings.chimeDevice1.playSound(delaySound1)
        settings.chimeDevice2.playSound(delaySound2)
        settings.lightsFlash.flash()
        runIn(delayAwayChime-2,stopChime)
        runIn(delayChime,stopFlash)
    }
}
def resetDisarmed(){
    settings.hsmDevice.off()
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
    settings.hsmDevice.on()
    logInfo ("Everyone Away Arming System")
}

def disarmReturn(){
    if (state.armedAway){
        settings.hsmDevice.clearAlert()
        settings.hsmDevice.off()
        logInfo (" Arrived Disarming System")
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
