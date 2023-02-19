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
 *  V1.5 Hubitat   added ability to toggle schedules
 *  V1.6 Hubitat   improved refresh schedule method
 *  V1.7 Hubitat   Removed Schedule toggle, added Schedule On and Off Commands
 *  V1.8 Hubitat   Added Clear alert Command, fixes and cleanup
 *  V1.9 Hubitat   Added Commands to Set - Power and Navigation modes
 *  V2.0 Hubitat   Added rooms/zones child devices for D7 Vacuums
 *
 */

def driverVer() { return "2.0" }

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;

preferences{
    def refreshRate = [:]
	refreshRate << ["5 min" : "Refresh every 5 minutes"]
    refreshRate << ["10 min" : "Refresh every 10 minutes"]
	refreshRate << ["15 min" : "Refresh every 15 minutes"]
	refreshRate << ["30 min" : "Refresh every 30 minutes"]
    input("dockRefresh", "enum", title: "<b>Refresh Interval while resting</b>",options: refreshRate, defaultValue: "15 min", required: true )
    input("runRefresh", "number", title: "<b>Refresh Interval while running</b>", description: "*In Seconds*", defaultValue: 30, required: true )
    input( "prefPersistentMapMode", "enum", options: ["on", "off"], title: "<b>Use Persistent Map, No-Go-Lines</b>", description: "*Only supported on certain models*", required: false, defaultValue: on )
    input(name: "offEnable", type: "bool", title: "<b>Off = Paused by default, Enable for Return to Dock</b>", defaultValue: false)
    input(name:"clearEnable",type:"bool",title: "<b>Enable to automatically clear alerts</b>",required:false,defaultValue: false)
    input(name:"zoneEnable",type:"bool",title: "<b>Enable Zone Child Devices</b>", description: "*D7 model only*",required: true, defaultValue: false)
    input(name:"logInfo",type:"bool",title: "<b>Enable Info logging</b>",required: true,defaultValue: true)
    input(name: "debugEnable", type: "bool", title: "<b>Enable Debug Logging</b>", defaultValue: true)   
}

metadata {
	definition (name: "Neato Botvac Connected Series", namespace: "alyc100", author: "Alex Lee Yuk Cheung")	{
    	capability "Battery"
	    capability "Refresh"
	    capability "Switch"
        capability "Actuator"

	    command "refresh"
        command "clearAlert"
        command "returnToDock"
        command "findMe"  //(Only works on D7 model)
        command "start"
        command "stop"
        command "pause"
        command "scheduleOn"
        command "scheduleOff"
        command "setPowerMode", [[name:"Set Power Mode", type: "ENUM",description: "Set Power Mode", constraints: ["eco", "turbo"]]]
        command "setNavigationMode", [[name:"Set Navigation Mode", type: "ENUM",description: "Set Navigation Mode", constraints: ["standard", "extraCare","deep"]]]

        attribute "status","string"
        attribute "mode","string"
        attribute "navigation","string"
        attribute "zone","string"
        attribute "network","string"
        attribute "charging","string"
        attribute "error","string"
        attribute "alert","string"
        attribute "schedule","string"
	}
}

def installed() {
	logDebug ("Installed with settings: ${settings}")
	initialize()
}

def updated() {
	logDebug ("Updated with settings: ${settings}")
	state.DriverVersion=driverVer()
	if (state.pwrMode == null){
		state.pwrMode = "turbo"
	}
	if (state.navMode == null){
		state.navMode = "standard"
	}
    
        switch(dockRefresh) {
		case "5 min" :
			runEvery5Minutes(refresh)
            logDebug ("refresh every 5 minutes schedule")
            if (logInfo) log.info "$device.label refresh every 5 minutes schedule"
			break
        	case "10 min" :
			runEvery10Minutes(refresh)
            logDebug ("refresh every 10 minutes schedule")
            if (logInfo) log.info "$device.label refresh every 10 minutes schedule"
			break
		case "15 min" :
			runEvery15Minutes(refresh)
            logDebug ("refresh every 15 minutes schedule")
            if (logInfo) log.info "$device.label refresh every 15 minutes schedule"
			break
		case "30 min" :
			runEvery30Minutes(refresh)
            logDebug ("refresh every 30 minutes schedule")
            if (logInfo) log.info "$device.label refresh every 30 minutes schedule"
            break
	}
    if (state.model == "BotVacD7Connected"){
        if (zoneEnable){
            zoneAdd()
        }else{
            zoneRemove()
        }
    }
	initialize()
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
    if (!state.isDocked){
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
        if (prefPersistentMapMode == "off"){
            catParam = 2
        }else{
            catParam = 4
        }
        if (state.pwrMode == "eco"){
            modeParam = 1
        }else{
            modeParam = 2
        }
        if (state.navMode == "standard"){
            navParam = 1
        }
        else if (state.navMode == "extraCare"){
            navParam = 2
        }
        else if (state.navMode == "deep"){
            modeParam = 2
            navParam = 3
        }
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
    //Only works on D7 model
	logDebug ("Executing 'findMe'")
	nucleoPOST("/messages", '{"reqId": "1","cmd":"findMe"}')
}

def scheduleOn() {
	logDebug ("Executing Schedule Enable")
	nucleoPOST("/messages", '{"reqId":"1", "cmd":"enableSchedule"}')  
	runIn(2, refresh)
}

def scheduleOff() {
	logDebug ("Executing Schedule Disable")
    nucleoPOST("/messages", '{"reqId":"1", "cmd":"disableSchedule"}')
	runIn(2, refresh)
}

def clearAlert(){
    logDebug ("Clearing current alert")
    nucleoPOST("/messages", '{"reqId":"1", "cmd":"dismissCurrentAlert"}')
    runIn(2,refresh)
}

def setPowerMode(mode){
    if (mode == "turbo"){
        state.pwrMode = "turbo"
    }
    else if (mode == "eco" && state.navMode != "deep"){
        state.pwrMode = "eco"
    }else{
        logDebug "cannot set Eco mode when navigaion mode is set to Deep"
    }
}

def setNavigationMode(mode){
    if (mode == "standard"){
        state.navMode = "standard"
    }
    else if (mode == "extraCare"){
        state.navMode = "extraCare"
    }
    else if (mode == "deep"){
        state.navMode = "deep"
        state.pwrMode = "turbo"
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
                mode = result.cleaning.mode as Integer
                if (mode == 1){
                    logDebug ("Cleaning mode is eco")
                    sendEvent(name:"mode",value:"eco")
                }
                else if (mode == 2){
                    logDebug ("Cleaning mode is eco")
                    sendEvent(name:"mode",value:"turbo")
                }else{
                    logDebug ("Cleaning mode unknown")
                    if (logInfo) log.info "$device.label cleaning mode unknown"
                    sendEvent(name:"mode",value:"unknown")
                }
                navMode = result.cleaning.navigationMode as Integer
                if (navMode == 1){
                    logDebug ("Navigation mode is standard")
                    sendEvent(name:"navigation",value:"standard")
                }
                else if (navMode == 2){
                    logDebug ("Navigaton mode is extraCare")
                    sendEvent(name:"navigation",value:"extraCare")
                }
                else if (navMode == 3){
                    logDebug ("Navigation mode is Deep")
                    sendEvent(name:"navigation",value:"deep")
                }else{
                    logDebug ("Navigation mode unknown")
                    if (logInfo) log.info "$device.label navigation mode unknown"
                    sendEvent(name:"navigation",value:"unknown")
                }
                if (result.cleaning.boundary != null){
                    zone = result.cleaning.boundary.name as String
                    sendEvent(name:"zone",value:"$zone")
                }else{
                    sendEvent(name:"zone",value:"Home")
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
            if (result.find{ it.key == "availableServices" }){
                state.houseCleaning = result.availableServices.houseCleaning     
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
                    errorMsg = "Error. " + result.error.replaceAll('_',' ').replaceAll('batt','battery').replaceAll('gen','robot was').replaceAll('hw','hardware').replaceAll('maint',' ').replaceAll('nav','navigation error, ').capitalize()
                    if (logInfo) log.info "$device.label error - $errorMsg"
                    sendEvent(name:"error",value:errorMsg)
                }
            }
            if (result.find{ it.key == "alert" }){
                alertText = result.alert as String
                if (alertText == null){
                    logDebug ("No Alerts")
                    sendEvent(name:"alert",value:"clear")
                }else{
                    logDebug ("Alert is -  $alertText")
                    alertMsg = "Alert. " + result.alert.replaceAll('_',' ').replaceAll('maint','time for').replaceAll('nav',' ').replaceAll('sched','schedule').capitalize()
                    if (logInfo) log.info "$device.label error - $alertMsg"
                    sendEvent(name:"alert",value:alertMsg)
                    if (clearEnable){
                        runIn(5,clearAlert)
                    }
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
                //if (logInfo) log.info "$device.label charging $charge"
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
                scheduleStatus = result.details.isScheduleEnabled as String
                logDebug ("Schedule Enabled - $scheduleStatus")
                //if (logInfo) log.info "$device.label Schedule Enabled - $scheduleStatus"
                if (scheduleStatus == "true"){
                    sendEvent(name:"schedule",value:"enabled") 
                }else{
                    sendEvent(name:"schedule",value:"disabled")
                }
            }
            if (result.find{ it.key == "meta" }){
                model = result.meta.modelName as String
                state.model = "$model"
            }
               
            return response
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		parent.logResponse(e.response)
		return e.response
	}
}

/////////////////////////////Zone Devices D7 Only//////////////////////

def zoneAdd(){
    def childDevice = getChildDevices()?.find {it.data.componentLabel == "zone"}
    if (!childDevice) {
        def resp2 = parent.beehiveGET("/users/me/robots/${device.deviceNetworkId.tokenize("|")[0]}/persistent_maps")
        def mapId = resp2.data[0].id
        nucleoPOST2("/messages", '{"reqId": "1", "cmd": "getMapBoundaries", "params": {"mapId": "' + mapId + '"}}')
        if (debugEnable) log.debug("map ID = ${mapId}")
    }else{
        if (debugEnable) log.debug("Child zones already created- to add or update, remove child devices and try again")
    }      
}

def nucleoPOST2(path, body) {
	try {
		if (debugEnable) log.debug("Beginning API POST: ${nucleoURL(path)}, ${body}")
		def date = new Date().format("EEE, dd MMM yyyy HH:mm:ss z", TimeZone.getTimeZone('GMT'))
		httpPostJson(uri: nucleoURL(path), body: body, headers: nucleoRequestHeaders(date, getHMACSignature(date, body)) ) {response ->
			parent.logResponse(response)
            def resp = (response.data)
            def status = (response.status)
            def result = resp
            if (result.find{ it.key == "data" }){
                if (result.data.boundaries != null){
                   
                    def rooms = [:]
                    result.data.boundaries.findAll { it.name }.each { rooms[it.name] = it.id }
                    if (debugEnable) log.debug "$rooms"
                    
                    result.data.boundaries.findAll { it.name }.each {
                        
                        def zoneName = "$it.name"
                        def zoneId = "$it.id"
                        
                        log.trace "$zoneName  =  $zoneId"
                    
                        if (zoneName != null) { 
                            log.info("Adding Neato zone device ${zoneName}:${zoneId}")
                            
                            childDevice = addChildDevice("alyc100","Neato Botvac Zone Child","${zoneId}",[name:"Neato Botvac Zone - ${zoneName}",label: "Vacuum ${zoneName}", isComponent: true, componentLabel: "zone"])
                            childDevice.setId("$zoneId")
                            childDevice.off()
                            
                            if (debugEnable) "Created Zone Child - ${zoneName} with id: ${zoneId}"
                        }   
                    } 
                }
            }
            
            return response
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		parent.logResponse(e.response)
		return e.response
	}
}

def zoneRemove(){
    def childDevice = getChildDevices()?.find {it.data.componentLabel == "zone"}
    if (childDevice) {
        if (debugEnable) log.debug "Deleting Zone children"
        def children = getChildDevices()
        children.each {child->
            deleteChildDevice(child.deviceNetworkId)
        }
    }else{
        if (debugEnable) log.debug "No Zone children to delete"
    }
}

/////////////////////////////Zone Devices D7 Only//////////////////////

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
