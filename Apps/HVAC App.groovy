/**
 *  ****************  Smart Thermostat HVAC  ****************
 *
 *  Smart Thermostat HVAC App.
 *
 *
 *  Copyright 2023 Gassgs / Gary Gassmann

 *  App to offset thermostat setpoint by mode and presence, send notification about open windows and doors when HVAC is running, emergency off for smoke and CO detection,
 *  ,filter change reminder, and Average Humidity reading pushed to device page.
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
 *
 *  Changes:
 *
 *  V1.0.0 -        11-22-2021       First run
 *  V1.1.0 -        05-16-2022       Logic improvments
 *  V1.2.0 -        09-11-2022       Fixed unschedule MSG when AC or Heat is turned off
 *  V1.3.0 -        03-19-2023       New app for controling smart thermostat, Adding to HVAC notification app
 *  V1.4.0 -        03-23-2023       Added avg humidity and filter life reminder options
 *  V1.5.0 -        03-24-2023       Added Armed status setpoint offset & emergency options for smoke and CO
 *  V1.6.0 -        03-30-2023       Added humidistat options
 *  V1.7.0 -        04-22-2023       Updated filter handler to match updates to kono driver
 *  V1.8.0 -        07-06-2025       Bug fix for disabling open contact messages
 *  V1.9.0 -        11-08-2025       Added checks for Humidity changes and changes for Zen Thermostat
 *  V2.0.0 -        12-09-2025       changes for Honeywell T6 Pro Z-wave thermostat(remove humidity average use T6 reading)
 */

import groovy.transform.Field

definition(
    name: "Smart Thermostat HVAC",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Smart Thermostat HVAC",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences {

	section {

     paragraph(
         title: "Smart Thermostat HVAC",
         required: false,
    	"<div style='text-align:center'><b><big>: Smart Thermostat HVAC App :</big></b></div>"
    	)
        paragraph( "<div style='text-align:center'><b>Smart Thermostat HVAC Humidistat Options</b></div>"
        )
        input(
            name:"thermostat",
            type:"capability.thermostat",
            title: "<b>Smart Thermostat Device</b>",
            required: true,
            multiple: false,
            submitOnChange: true
            )
        if (thermostat){
            paragraph "Temperature - <b>${temperature()}F</b>     Humidity - <b>${humidity()}%</b>     Heating Setpoint - <b>${heatSet()}F</b>  </b>     Cooling Setpoint - <b>${coolSet()}F</b>  </b>     Mode - <b>${mode()}</b>     State - <b>${operatingState()} </b>"
        }
    }
    section{
        input(
            name:"temperatureSensors",
            type:"capability.temperatureMeasurement",
            title: "<b>Outdoor Temperature Sensor(s)</b> for Calculating The Target Humidity",
            multiple: true,
            required: false,
            submitOnChange: true
        )
        if(temperatureSensors){
            paragraph "Current Target Humidity - <b> ${calculateTarget()}% </b> Based on Outdoor Temperature of <b>${averageTemperature()}</b> Degrees"
        }
        input(
            name:"minRh",
            type:"decimal",
            title: "The minimum target relative humidity allowed",
            defaultValue: 20,
            submitOnChange: true
        )
        input(
            name:"maxRh",
            type:"decimal",
            title:"The maximum target relative humidity allowed",
            defaultValue: 50,
            submitOnChange: true
        )
        input(
            name:"frostCorrection",
            type:"decimal",
            title:"Target offset, reduce this value to avoid condensation",
            defaultValue: -3,
            submitOnChange: true
        )
    }
    section{
        
        paragraph( "<div style='text-align:center'><b>Night and Away Mode Temperature Offset Options</b></div>"
        )
        input(
            name:"security",
            type:"capability.switch",
            title:"<b>Security System Device for Armed Night and Armed Away Modes</b>",
            required: false,
            multiple: false,
            submitOnChange: true
            )
        if (security){
            paragraph "Current Status - <b>${armedStatus()}</b>"
        }
        input(
            name:"modeOffset",
            type:"number",
            title:"<b>Offset Amount for Armed Night and Armed Away Modes</b>",
            defaultValue: 2,
            submitOnChange: true
            )
    }
    section{
        
        paragraph( "<div style='text-align:center'><b>Smoke and CO Detection Options</b></div>"
        )
        input(
            name:"smokeDetector",
            type:"capability.smokeDetector",
            title:"<b>Smoke and CO detectors</b> for emergency shutdown",
            multiple: true,
            submitOnChange: true
        )
    }
    section{
        
        paragraph( "<div style='text-align:center'><b>Filter Reminder Options</b></div>"
        )
        input(
            name:"filterNotification",
            type:"bool",
            title:"<b>Enable Filter Change Reminders</b>",
            defaultValue: false,
            submitOnChange: true
            )
        if (filterNotification && thermostat){
            paragraph "Current filter life - <b> ${currentFilterLifeValue()} % </b> --  Status - <b>${currentFilterValue()}</b>"
        }
    }
    section{
        
        paragraph( "<div style='text-align:center'><b>Contact Notification Options</b></div>"
        )
        input(
            name:"contactEnable",
            type:"bool",
            title:"<b>Enable Open Contacts for Notifications while HVAC running</b>",
            defaultValue: false,
            submitOnChange: true
            )
        if (contactEnable && thermostat){
            input(
                name:"contacts",
                type:"capability.contactSensor",
                title:"<b>Contact Sensors</b> to monitor",
                multiple: true,
                submitOnChange: true
                )
            input(
                name:"timeout",
                type:"number",
                title: "<b>Timeout</b> in minutes when contact is open while furnace or A/C is running",
                multiple: false,
                defaultValue: 5,
                submitOnChange: true
                )
        }
    }
    section{
        
        paragraph( "<div style='text-align:center'><b>TTS Options</b></div>"
        )
        input(
            name:"ttsDevice",
            type:"capability.speechSynthesis",
            title:"<b>TTS Device</b> for messsage alert",
            multiple: true,
            submitOnChange: true
            )
        input(
            name:"volume",
            type:"number",
            title:"Volume level for message",
            defaultValue: 90,
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
    subscribe(settings.thermostat, "thermostatOperatingState", thermostatOperatingStateHandler)
    subscribe(settings.thermostat, "thermostatMode", thermostatModeHandler)
    subscribe(settings.temperatureSensors, "temperature", temperatureSensorsHandler)
    subscribe(settings.thermostat, "filterLife", filterHandler)
    subscribe(settings.contacts, "contact", contactHandler)
    subscribe(settings.security, "status", securityHandler)
    subscribe(settings.smokeDetector, "smoke", smokeHandler)
    subscribe(settings.smokeDetector, "carbonMonoxide", coHandler)
    subscribe(location, "mode", modeEventHandler)   //- Not currently being used
    loadValues()
    logInfo ("$app.label subscribed to Events")
}

def loadValues(){
    if (settings.temperatureSensors){
        getTemperature()
    }
    if (settings.contacts){
        getContacts()
    }
    modeValue = settings.thermostat.currentValue("thermostatMode")
    thermostatModeHandler(value:"$modeValue")
    armedValue = settings.security.currentValue("status")
    securityHandler(value:"$armedValue")
    mode = location.currentMode as String
    state.earlyMorning = (mode == "Early_morning")
    state.day = (mode == "Day")
    state.afternoon = (mode == "Afternoon")
    state.dinner = (mode == "Dinner")
    state.evening = (mode == "Evening")
    state.lateEvening = (mode == "Late_evening")
    state.night = (mode == "Night")
    state.away = (mode == "Away")
}

def temperature(){
    def value = settings.thermostat.currentValue("temperature")
    if (value == null){value = "N/A"}
    return value as String
}

def humidity(){
    def value = settings.thermostat.currentValue("humidity")
    if (value == null){value = "N/A"}
    return value as String
}

def heatSet(){
    def value = settings.thermostat.currentValue("heatingSetpoint")
    if (value == null){value = "N/A"}
    return value as String
}

def coolSet(){
    def value = settings.thermostat.currentValue("coolingSetpoint")
    if (value == null){value = "N/A"}
    return value as String
}

def mode(){
    def value = settings.thermostat.currentValue("thermostatMode")
    if (value == null){value = "N/A"}
    return value as String
}

def operatingState(){
    def value = settings.thermostat.currentValue("thermostatOperatingState")
    if (value == null){value = "N/A"}
    return value as String
}

def armedStatus(){
    def value = settings.security.currentValue("status")
    if (value == null){value = "N/A"}
    return value as String
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
	logInfo ("$app.label Current temperature average is ${averageTemperature()}")
    setThermostatHumiditySetpoint()
}

def calculateTarget() {
	def double outdoorTemp = averageTemperature()
	def target = Math.max(Math.min((double)frostCorrection+27.7+0.535*outdoorTemp - 0.00409 * Math.pow(outdoorTemp, 2), (double)maxRh), (double)minRh)
    logInfo ( "$app.label Calculated a target humidity of ${target.round(1)}% for the main floor with an outdoor temperature of ${outdoorTemp}")
    return Math.round(target*10)/10
}

def setThermostatHumiditySetpoint(){
    value = calculateTarget()
    settings.thermostat.setHumiditySetpoint(value)
    logInfo ("$app.label setting Thermostat humidity setpoint $value")
}

def currentFilterValue(){
    def value = settings.thermostat.currentValue("filter")
    if (value == null){value = "N/A"}
    return value as String
}

def currentFilterLifeValue(){
    def value = settings.thermostat.currentValue("filterLife")
    if (value == null){value = "N/A"}
    return value as String
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
    //logInfo ("$app.label mode status $mode") - Mode not currently being used
}

def securityHandler(evt){
    if (state.baseSet == null){
        state.baseSet= 68
    }
    armedValue = evt.value
    state.armedNight = (armedValue == "armed night")
    state.armedAway = (armedValue == "armed away")
    if (state.armedNight && state.heatMode || state.armedAway && state.heatMode){
        heatSet = settings.thermostat.currentValue("heatingSetpoint") as Integer
        state.baseSet = heatSet as Integer
        newSet = heatSet - modeOffset as Integer
        settings.thermostat.setHeatingSetpoint(newSet)
        logInfo ("$app.label Security System - $armedValue Thermostat set to $newSet")
    }
    else if (state.armedNight && state.coolMode || state.armedAway && state.coolMode){
        coolSet = settings.thermostat.currentValue("coolingSetpoint") as Integer
        state.baseSet = coolSet as Integer
        newSet = coolSet + modeOffset as Integer
        settings.thermostat.setCoolingSetpoint(newSet)
        logInfo ("$app.label Security System - $armedValue Thermostat set to $newSet")
    }
    else if (state.coolMode){
        newSet = state.baseSet as Integer
        settings.thermostat.setCoolingSetpoint(newSet)
        logInfo ("$app.label Security System - $armedValue Thermostat set to $newSet")
    }
    else if (state.heatMode){
        newSet = state.baseSet as Integer
        settings.thermostat.setHeatingSetpoint(newSet)
        logInfo ("$app.label Security System - $armedValue Thermostat set to $newSet")
    }
    else{
        logInfo ("$app.label Security System $armedValue Thermostat Mode is OFF")
    }
}

def filterHandler(evt){
    filterValue = evt.value as Integer
    log.info "$app.label Furnace filter status - $filterValue%"
    if (filterValue <= 0){
        log.info "$app.label The furnace filter is due to be replaced - Sending Reminder Notification"
        settings.ttsDevice.playTrack("FurnaceFilter")
        settings.ttsDevice.deviceNotification("Reminder! It is time to change the furnace filter. Please replace the furnace air filter as soon as possible")
    }     
}

def smokeHandler(evt){
    status = evt.value as String
    log.info "$app.label smoke $status"
    if (status == "detected"){
        log.info "$app.label smoke $status setting Thermostat Mode to OFF"
        settings.thermostat.setThermostatMode("off")
    }
    else{
        log.info "$app.label smoke $status"
    }   
}

def coHandler(evt){
    status = evt.value as String
    log.info "$app.label carbonMonoxide $status"
    if (status == "detected"){
        log.info "$app.label carbonMonoxide $status setting Thermostat Mode to OFF"
        settings.thermostat.setThermostatMode("off")
    }
    else{
        log.info "$app.label carbonMonoxide $status"
    }
}

def contactHandler(evt){
    getContacts()
}

def getContacts(){
	def open = settings.contacts.findAll { it?.latestValue("contact") == 'open' }
    if (open){
        contactList = "${open}"
        logInfo("$app.label Open Contacts" + contactList)
        state.contactMsg = contactList as String
        runIn(timeout*60,contactsOpen)
    }else{
        state.contactsOpen = false
        unschedule(contactsOpen)
        unschedule(repeatOpenHeatMsg)
        unschedule(repeatOpenAcMsg)
    }
}

def contactsOpen(){
    state.contactsOpen = true
    contactMsg = state.contactMsg as String
    logInfo("$app.label Contacts left open longer than timeout")
    if (state.heating){
        logInfo ("$app.label system is heating and $contactMsg is open, Sending Alert")
        contactsOpenHeatMsg()
    }
    else if (state.cooling){
        logInfo ("$app.label system is cooling and $contactMsg is open, Sending Alert")
        contactsOpenAcMsg()
    }else{
        logInfo ("$app.label $contactMsg is open, HVAC not currently running")
    }   
}

def thermostatOperatingStateHandler(evt){
    status = evt.value
    contactMsg = state.contactMsg as String
    logInfo ("$app.label thermostat operating state $status")
    if (status == "cooling"){
        state.cooling = true
    }
    else if (status == "heating"){
        state.heating = true
    }
    else if (status == "idle"){
        state.cooling = false
        state.heating = false
    }
    if (state.heating && state.contactsOpen){
        logInfo ("$app.label system is heating and $contactMsg is open, Sending Alert")
        contactsOpenHeatMsg()
    }
    else if (state.cooling && state.contactsOpen){
        logInfo ("$app.label system is cooling and $contactMsg is open, Sending Alert")
        contactsOpenAcMsg()
    }else{
        logInfo ("$app.label HVAC $status Contacts are Closed or State is currently Idle")
    } 
}

def thermostatModeHandler(evt){
    status = evt.value
    if (status == "off"){
        state.heatMode = false
        state.coolMode = false
        unschedule(repeatOpenHeatMsg)
        unschedule(repeatOpenAcMsg)
    }
    if (status == "heat"){
        state.heatMode = true
        state.coolMode = false
    }
    if (status == "cool"){
        state.heatMode = false
        state.coolMode = true
    }
    logInfo ("$app.label Thermostat Mode $status")
}

def contactsOpenHeatMsg(){
    if (contactEnable){
    contactMsg = state.contactMsg.replaceAll('Sensor',' ').replaceAll('🚪','').replaceAll('🪟','') as String //.replaceAll('🚪','') .replaceAll('Door','') 
    settings.ttsDevice.speak("Alert! The Furnace is running. Please close the $contactMsg",settings.volume as Integer,settings.voice as String)
    logInfo ("$app.label HVAC heating. $contactMsg open message sent")
    runIn(120,repeatOpenHeatMsg)
    }
}

def contactsOpenAcMsg(){
    if (contactEnable){
    contactMsg = state.contactMsg.replaceAll('Sensor',' ').replaceAll('🚪','').replaceAll('🪟','') as String //.replaceAll('Window','') .replaceAll('Door','')
    settings.ttsDevice.speak("Alert! The Air Conditioner is running. Please close the $contactMsg",settings.volume as Integer,settings.voice as String)
    logInfo ("$app.label HVAC cooling. $contactMsg open message sent")
    runIn(120,repeatOpenAcMsg)
    }
}

def repeatOpenHeatMsg(){
    if (contactEnable && contactsOpen && state.heating){
        contactsOpenHeatMsg()
        logInfo ("$app.label HVAC heating. $contactMsg open REPEAT message sent")
    }else{
        logInfo ("$app.label repeat msg not needed")
    }  
}

def repeatOpenAcMsg(){
    if (contactEnable && contactsOpen && state.cooling){
        contactsOpenAcMsg()
        logInfo ("$app.label HVAC cooling. $contactMsg open REPEAT message sent")
    }else{
        logInfo ("$app.label repeat msg not needed")
    }  
}

void logInfo(String Msg){
    if (settings?.logEnable !=false){
        log.info "$Msg"
    }
}
