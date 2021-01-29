/**
 *  ****************  HSM Controller and Monitor App ****************
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
 *  Last Update: 1/27/2021
 *
 *  Changes:
 *
 *  V1.0.0  -       1-27-2021       First run
 *  V2.0.0  -       1-28-2021       Major Improvements adding presence  
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
            title: "Lock to lock when arming",
            multiple: true,
            required: false,
            submitOnChange: true
            )
    }
    section{
        input(
            name:"chimeDevice",
            type:"capability.chime",
            title: "Chime device for  notification for arming",
            multiple: true,
            required: false,
            submitOnChange: true
            )
        if (chimeDevice){
             input(
                 name:"chimeTimer",
                 type:"number",
                 title: "Chime notification timer, should match time to arm",
                 required: true,
                 submitOnChange: true
             )
        }
    }
    section{
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
    subscribe(settings.hsmDevice, "switch.on", hsmSwitchOnHandler)
    subscribe(settings.hsmDevice, "switch.off", hsmSwitchOffHandler)
    subscribe(settings.hsmDevice, "alert.clearing", hsmClearHandler)
    subscribe(location, "hsmStatus", statusHandler)
    subscribe(location, "hsmAlert", alertHandler)
    subscribe(location, "mode", modeEventHandler)
    subscribe(settings.presenceSensors, "presence", presenceHandler)
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
}

def hsmClearHandler(evt){
    logInfo ("HSM Status Clearing")
    sendLocationEvent(name: "hsmSetArm", value: "cancelAlerts")
}

def statusHandler(evt){
    logInfo ("HSM Status: $evt.value")
    hsmStatus = evt.value
    settings.hsmDevice.hsmUpdate("hsmStatus","$evt.value")
    state.armedNight = (hsmStatus == "armedNight")
    state.armedAway = (hsmStatus == "armedAway")
    state.armedHome = (hsmStatus == "armedHome")
    state.disarmed = (hsmStatus == "disarmed")
    state.armingNight = (hsmStatus == "armingNight")
    state.armingAway = (hsmStatus == "armingAway")
    state.armingHome = (hsmStatus == "armingHome")
    if (state.armedNight){
        settings.hsmDevice.hsmUpdate("status","armed")
        settings.lights.setColor(hue: settings.hue,saturation: settings.sat)
    }
    if (state.armedAway){
        settings.hsmDevice.hsmUpdate("status","armed")
        settings.lights.setColor(hue: settings.hue,saturation: settings.sat)
    }
    if (state.armedHome){
        settings.hsmDevice.hsmUpdate("status","armed")
        settings.lights.setColor(hue: settings.hue,saturation: settings.sat)
    }
    if (state.disarmed){
        settings.hsmDevice.hsmUpdate("status","disarmed")
        settings.lights.setColorTemperature("2702")
        settings.chimeDevice.playSound("2")
    }
    if (state.armingNight){
        settings.lock.lock
        settings.chimeDevice.playSound("4")
        runIn(chimeTimer,stopChime)
    }
    if (state.armingAway){
        settings.lock.lock
        settings.chimeDevice.playSound("4")
        runIn(chimeTimer,stopChime)
    }
    if (state.armingHome){
        settings.lock.lock
        settings.chimeDevice.playSound("4")
        runIn(chimeTimer,stopChime)
    }
}

def stopChime(){
    settings.chimeDevice.stop()
}

def alertHandler(evt){
	logInfo ("HSM Alert: $evt.value")
    alertValue = evt.value
    state.cancelled = (alertValue == "cancel")
    settings.hsmDevice.hsmUpdate("currentAlert","$evt.value")
    settings.hsmDevice.hsmUpdate("alert","active")
    if (state.cancelled){
        settings.chimeDevice.playSound("2")
    }
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
    settings.hsmDevice.arm()
    logInfo ("Everyone Away Arming System")
}

def disarmReturn(){
    if (state.armedAway){
        settings.hsmDevice.disarm()
        logInfo (" Arrived Disarming System")
    }
}

void logInfo(String msg){
	if (settings?.logEnable != false){
		log.info "$msg"
	}
}
