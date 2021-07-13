/**
 *  Neato Botvac Connected Series
 *
 *  Copyright 2017,2018,2019,2020 Alex Lee Yuk Cheung
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  VERSION HISTORY
 *
 *  V1.0 Hubitat
 *  V1.1 Hubitat   event update improvemnts
 *  V1.2 Hubitat   added stop command
 *  V1.3 Hubitat   fixes and improvements
 *  V1.4 Hubitat   minor fixes
 *
 */
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;

preferences
{
    input( "prefCleaningMode", "enum", options: ["turbo", "eco"], title: "Cleaning Mode", description: "Only supported on certain models", required: true, defaultValue: "turbo" )
    input( "prefNavigationMode", "enum", options: ["standard", "extraCare", "deep"], title: "Navigation Mode", description: "Only supported on certain models", required: true, defaultValue: "standard" )
    input( "prefPersistentMapMode", "enum", options: ["on", "off"], title: "Use Persistent Map, No-Go-Lines", description: "Only supported on certain models", required: false, defaultValue: on )
    input("dockRefresh", "number", title: "How often to 'Refresh' while docked, in Minutes", defaultValue: 15, required: true )
    input("runRefresh", "number", title: "How often to 'Refresh' while running, in Seconds", defaultValue: 60, required: true )
    input(name: "offEnable", type: "bool", title: "Off = Paused by default, Enable for, Off = Return to Dock", defaultValue: false)
    input(name: "debugEnable", type: "bool", title: "Enable Debug Logging", defaultValue: true)
    input(name:"logInfo",type:"bool",title: "Enable Info logging",required: true,defaultValue: true)
}

metadata {
	definition (name: "Neato Botvac Connected Series", namespace: "alyc100", author: "Alex Lee Yuk Cheung", ocfDeviceType: "oic.d.robotcleaner", mnmn: "SmartThingsCommunity", vid: "1b47ad78-269e-3c5c-a1a9-8c84d2a2ef05")	{
    	capability "Battery"
	capability "Refresh"
	capability "Switch"
        capability "Actuator"

	command "refresh"
        command "returnToDock"
        command "findMe"  //(Not working on my D4)
        command "start"
        command "stop"
        command "pause"

        attribute "status","string"
        attribute "network","string"
        attribute "charging","string"
        attribute "error","string"
	}
}

def installed() {
	logDebug ("Installed with settings: ${settings}")
	initialize()
    sendEvent(name: "checkInterval", value: 10 * 60 + 2 * 60, data: [protocol: "cloud"], displayed: false)
}

def updated() {
	logDebug ("Updated with settings: ${settings}")
	initialize()
    sendEvent(name: "checkInterval", value: 10 * 60 + 2 * 60, data: [protocol: "cloud"], displayed: false)
}

def initialize() {
	poll()
    if(debugEnable){
        runIn(1800, logsOff)
    }
}

def refreshSch(){
    def currentState = device.currentValue("status")
    if (currentState == "paused"){
        state.paused = true
        }else{
            state.paused = false
        }
    if (state.isDocked){
        logDebug ("$dockRefresh min refresh active")
        runIn(dockRefresh*60,refresh)
    }else{
        logDebug ("$runRefresh second refresh active")
        runIn(runRefresh,refresh)
    }
}

def on() {
	logDebug ("Executing 'on'")
    if (state.paused){
    	nucleoPOST("/messages", '{"reqId":"1", "cmd":"resumeCleaning"}')
    }
    else{
    	def modeParam = 1
        def navParam = 1
        def catParam = 2
        if (isTurboCleanMode()) modeParam = 2
        if (isExtraCareNavigationMode()) navParam = 2
        if (isDeepNavigationMode()) {
        	modeParam = 2
            navParam = 3
        }
        if (isPersistentMapMode()) catParam = 4
        switch (state.houseCleaning) {
            case "basic-1":
               	nucleoPOST("/messages", '{"reqId":"1", "cmd":"startCleaning", "params":{"category": 2, "mode": ' + modeParam + ', "modifier": 1}}')
			break;
			case "minimal-2":
				nucleoPOST("/messages", '{"reqId":"1", "cmd":"startCleaning", "params":{"category": 2, "navigationMode": ' + navParam + '}}')
			break;
            default:
            	nucleoPOST("/messages", '{"reqId":"1", "cmd":"startCleaning", "params":{"category": ' + catParam + ', "mode": ' + modeParam + ', "navigationMode": ' + navParam + '}}')
            break;
        }
    }
    runIn(2, refresh)
}

def start() {
    on()
}

def pause() {
	logDebug ("Executing Pause")
    nucleoPOST("/messages", '{"reqId":"1", "cmd":"pauseCleaning"}')
    runIn(2, refresh)
}

def stop() {
	logDebug ("Executing Stop")
    nucleoPOST("/messages", '{"reqId":"1", "cmd":"stopCleaning"}')
    runIn(2, refresh)
}

def off() {
    if (offEnable) {
    returnToDock()
    }else{
        pause()
    }
}

def returnToDock() {
	logDebug ("Executing 'return to dock'")
    nucleoPOST("/messages", '{"reqId":"1", "cmd":"sendToBase"}')
    sendEvent(name:"status",value:"returning to dock")
    runIn(25, refresh)
}

def findMe() {
    //not working on D4 model
	logDebug ("Executing 'findMe'")
    nucleoPOST("/messages", '{"reqId": "1","cmd": "findMe"}')
}

def setCleaningMode(mode) {
	if ( mode == "eco" || mode == "turbo" ) {
    	state.startCleaningMode = mode
    } else {
    	log.error("Unsupported cleaning mode: [${mode}]")
    }
}

def setNavigationMode(mode) {
	if ( mode == "deep" || mode == "extraCare" || mode == "standard") {
    	state.startNavigationMode = mode
	} else {
    	log.error("Unsupported navigation mode: [${mode}]")
    }
}

def setPersistentMapMode(mode) {
	if ( mode == "on" || mode == "off" ) {
    	state.startPersistentMapMode = mode
	} else {
    	log.error("Unsupported persistent map mode: [${mode}]")
    }
}

def poll() {
	logDebug ("Executing 'poll'")
    resp = nucleoPOST("/messages", '{"reqId":"1", "cmd":"getRobotState"}')
}

def refresh() {
	logDebug ("Executing 'refresh'")
    if (parent.getSecretKey(device.deviceNetworkId) == null) {
    }
	poll()
    refreshSch()
}

private def isTurboCleanMode() {
	def result = true
    if ((state.startCleaningMode == "unsupported") || (state.startCleaningMode != null && state.startCleaningMode == "eco" && settings.prefCleaningMode == "webcore") || (settings.prefCleaningMode == "eco")) {
    	result = false
    }
    result
}

private def isExtraCareNavigationMode() {
	def result = false
    if ((state.startNavigationMode == "unsupported") || (state.startNavigationMode != null && state.startNavigationMode == "extraCare" && settings.prefCleaningMode == "webcore") || (settings.prefNavigationMode == "extraCare")) {
    	result = true
    }
    result
}

private def isDeepNavigationMode() {
	def result = false
    if ((state.startNavigationMode == "unsupported") || (state.startNavigationMode != null && state.startNavigationMode == "deep" && settings.prefCleaningMode == "webcore") || (settings.prefNavigationMode == "deep"))  {
    	result = true
    }
    result
}

private def isPersistentMapMode() {
	def result = false
    if ((state.startPersistentMapMode == "unsupported") || (settings.prefPersistentMapMode == "on") || (state.startPersistentMapMode != null && state.startPersistentMapMode == "on")) {
    	result = true
    }
    result
}

def nucleoPOST(path, body) {
	try {
		if (debugEnable) log.debug("Beginning API POST: ${nucleoURL(path)}, ${body}")
		def date = new Date().format("EEE, dd MMM yyyy HH:mm:ss z", TimeZone.getTimeZone('GMT'))
		httpPostJson(uri: nucleoURL(path), body: body, headers: nucleoRequestHeaders(date, getHMACSignature(date, body)) ) {response ->
			parent.logResponse(response)
            def resp = (response.data)
            def status = (response.status)
            def result = resp
            if (status != 200) {
                if (result.find{ it.key == "message" }){
                    switch (result.message) {
                        case "Could not find robot_serial for specified vendor_name":
                        statusMsg += 'Robot serial and/or secret is not correct.\n'
                        break;
                    }
                }
                log.error("Unexpected result in poll(): [${resp}] ${status}")
                sendEvent(name:"status",value:"error")
                sendEvent(name:"network",value:"not connected")
                logDebug ("Not Connected To Neato")
            }
            else if (result.find{ it.key == "cleaning" }){
                batteryLevel = result.details.charge as String
                batteryPercent = result.details.charge as Integer
                logDebug ("Battery level ${batteryLevel}")
                if (logInfo) log.info "$device.label Battery level ${batteryLevel}"
                sendEvent(name:"battery",value: batteryLevel) 
                if (batteryPercent >= 95){
                    state.batteryFull = true
                }else{
                    state.batteryFull = false
                }
            }
            if (result.find{ it.key == "action" }){
                if (result.action == 4) {
                    state.returningToDock = true
                    logDebug ("returningToDock = true" )
                }else{
                    state.returningToDock = false
                    logDebug ("returningToDock = false" )
                }
            }
            if (result.find{ it.key == "state" }){
                sendEvent(name:"network",value:"connected")
                //state 1 - Ready to clean,state 2 - Cleaning, state 3 - Paused, state 4 - Error
                switch (result.state) {
                    case "1":
                    state.noError = true
                    sendEvent(name:"switch",value:"off")
                    if (! state.isDocked) {
                    sendEvent(name:"status",value:"stopped")
                    logDebug ("switch status should be off - Stopped")
                    if (logInfo) log.info "Botvac Stopped"
                    }
                    break;
                    case "2":
                    state.noError = true
                    if (state.returningToDock){
                        sendEvent(name:"status",value:"returning to dock")
                        sendEvent(name:"switch",value:"on")
                        logDebug ("switch should be on - returning to dock")
                        if (logInfo) log.info "$device.label Returning to Dock"
                    }else{
                        sendEvent(name:"status",value:"running")
                        sendEvent(name:"switch",value:"on")
                        logDebug ("switch should be on - running")
                        if (logInfo) log.info "$device.label Running"
                    }
                    break;
                    case "3":
                    state.noError = true
                        sendEvent(name:"status",value:"paused")
                        sendEvent(name:"switch",value:"on")
                        logDebug ("Vacuum should be paused")
                        if (logInfo) log.info "$device.label Paused"
                    break;
            	    case "4":
                    state.noError = false
                        sendEvent(name:"status",value:"error")
                        logDebug ("Vacuum Error??")
                        if (logInfo) log.info "$device.label error"
				    break;
            	        default:
                        sendEvent(name:"status",value:"unknown")
				    break;
                }
            }
            if (result.find{ it.key == "error" }){
                errorCode = result.error as String
                if (errorCode == null){
                    logDebug ("No errors")
                    sendEvent(name:"error",value:"clear")
                }else{
                    logDebug ("Error is -  $errorCode")
                    if (logInfo) log.info "$device.label error - $errorCode"
                    sendEvent(name:"error",value:errorCode)
                }
            }
            if (result.find{ it.key == "details" }){
                docked = result.details.isDocked as String
                if (docked == "true") {
                    logDebug ("Vacuum now Docked")
                    if (logInfo) log.info "$device.label Docked"
                    state.isDocked = true
                    if (state.noError){
                        sendEvent(name:"status",value:"docked")
                    }
                }else{
                    logDebug ("Botvac Not Docked")
                    state.isDocked = false
                }
                charge = result.details.isCharging as String
                logDebug ("charge status $charge")
                if (logInfo) log.info "$device.label charging $charge"
                if (charge == "false"){
                    state.notCharging = true
                }else{
                    state.notCharging = false
                }
                if (state.notCharging && state.batteryFull){
                    sendEvent(name:"charging",value:"fully charged") 
                }else{
                    sendEvent(name:"charging",value:result.details.isCharging as String)
                }
            }
            return response
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		parent.logResponse(e.response)
		return e.response
	}
}

def getHMACSignature(date, body) {
	//request params
	def robot_serial = device.deviceNetworkId
    //Format date should be "Fri, 03 Apr 2015 09:12:31 GMT"
	
    def robot_secret_key = parent.getSecretKey(device.deviceNetworkId)
	// build string to be signed
	def string_to_sign = "${robot_serial.toLowerCase()}\n${date}\n${body}"

	// create signature with SHA256
	//signature = OpenSSL::HMAC.hexdigest('sha256', robot_secret_key, string_to_sign)
    try {
    	Mac mac = Mac.getInstance("HmacSHA256")
    	SecretKeySpec secretKeySpec = new SecretKeySpec(robot_secret_key.getBytes(), "HmacSHA256")
    	mac.init(secretKeySpec)
    	byte[] digest = mac.doFinal(string_to_sign.getBytes())
    	return digest.encodeHex()
   	} catch (InvalidKeyException e) {
    	throw new RuntimeException("Invalid key exception while converting to HMac SHA256")
  	}
}

Map nucleoRequestHeaders(date, HMACsignature) {
	return [
        'X-Date': "${date}",
        'Accept': 'application/vnd.neato.nucleo.v1',
        'Content-Type': 'application/*+json',
        'X-Agent': '0.11.3-142',
        'Authorization': "NEATOAPP ${HMACsignature}"
    ]
}

def nucleoURL(path = '/') 			 { return "https://nucleo.neatocloud.com:4443/vendors/neato/robots/${device.deviceNetworkId.tokenize("|")[0]}${path}" }

void logDebug(String msg){
	if (settings?.debugEnable != false){
		log.debug "$msg"
	}
}

def logsOff(){
    log.warn "debug logging disabled..."
	device.updateSetting("debugEnable", [value:"false",type:"bool"])
}
