/*
 * Sonoff RF Bridge Curtain/Shades  Driver
 *
 *Calls URIs with HTTP GET for  open/close/stop -  w/ SonOff RF Bridge
 * 
 * 
 * To use: 
 * 1) Run a stopwatch to time how long it takes for the blinds to close (in
 * seconds) 
 * 
 * Input the value in the configuration, rounding down
 *
 * Example "B0 raw code" 
 *   "AAB0210308017204C431742819081908190819090908190819081908190818190908181855"
 *
 *  
 * Based on the Hubitat community driver httpGetSwitch 
 * https://raw.githubusercontent.com/hubitat/HubitatPublic/master/examples/drivers/httpGetSwitch.groovy
 *
 * And HUGE!!!!! thanks to code from "bdwilson" Brian Wilson's driver for Neo-smart-controller: https://raw.githubusercontent.com/bdwilson/hubitat/master/NeoSmart/NeoSmart.groovy
 * https://community.hubitat.com/t/neo-smart-controller-for-blinds/26391
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 * 
 *
 */

def driverVer() { return "3.2" }

metadata {
    definition(name: "Sonoff RF Curtain/Shades", namespace: "Gassgs", author: "GaryG,", importUrl: "https://raw.githubusercontent.com/Gassgs/SonOff-RF-Bridge-Drivers-for-Hubitat/master/Sonoff%20RF%20Bridge%20Curtain-shades%20Driver") {
        
        capability "WindowShade"
		    capability "Switch"
		    capability "Actuator"
		    capability "Switch Level"
        capability "ChangeLevel"   
	
    }
}
preferences {
    section("URIs") {
        input "openB0", "text", title: "Open B0 Raw(no spaces)", required: false
        input "closeB0", "text", title: "Close B0 Raw(no  spaces)", required: false
        input "stopB0", "text", title: "Stop B0 Raw(no spaces)", required: false
		    input "sonoffIP", "text", title: "Sonoff RF Bridge IP Address", required: true
        input "timeToClose", "number", title: "Time in seconds it takes to close", required: true
        input name: "rfoffenabled", type: "bool" , title: "Send RF Raw Off", defaultValue: true
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

def date() {
    def origdate = new Date().getTime().toString().drop(6)
    def random = Math.floor(Math.random() * 1000)
    random = random.toInteger().toString().take(4) 
    def date = origdate.toInteger() + random.toInteger() 
    if (logEnable) log.debug "Using ${date}"
	return date
}

def get(url,mystate) {
   if (logEnable) log.debug "Call to ${url}; setting ${mystate}"
   state.lastCmd = mystate
   try {
        httpGet([uri:url, timeout:5]) { resp ->
            if (resp.success) {
                sendEvent(name: "windowShade", value: "${mystate}", isStateChange: true)
           }
           if (logEnable)
                if (resp.data) log.debug "${resp.data}"
       }
    } catch (Exception e) {
        log.warn "Call to ${url} failed: ${e.message}"
    }
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def rfoffdisabled() {
    log.warn "rf raw off disabled..."
    device.updateSetting("rfoffenabled", [value: "false", type: "bool"])
}

def installed() {
    log.info "installed..."
	if (!sonoffIP ||  !timeToClose) {
		log.error "Please make sure sonoff IP address  and time to close are configured." 
	}
    log.warn "debug logging is: ${logEnable == true}"
    if (logEnable) runIn(3600, logsOff)
	state.DriverVersion=driverVer()
}

def updated() {
    log.info "updated..."
	if (!sonoffIP ||  !timeToClose) {
		log.error "Please make sure sonoff IP address  and time to close are configured." 
	}
    log.warn "debug logging is: ${logEnable == true}"
    if (logEnable) runIn(1800, logsOff)
	state.DriverVersion=driverVer()
}

def parse(String description) {
    if (logEnable) log.debug(description)
}

def on() {
	open() 
}

def off() {
    close()
}
    
def close() {
    url = "http://" + sonoffIP + "/cm?cmnd=RfRaw%20" + closeB0 +  date()
    
     if (state.lastCmd != "closing") {
        
        UpdateTimeRunning()
    
        def closingTime = timeToClose
   
        if (state.lastCmd == "opening") {  
            if (logEnable) log.debug "Status was opening, closing was hit. scheduling closingTime for ${state.secs}"
            unschedule(updateStatus)
            closingTime = state.secs
        } else if (state.lastCmd == "stopped") { 
            closingTime = timeToClose - state.secs
            if (logEnable) log.debug "Going from Stopped to Closing. closingTime will now be: ${closingTime}"
        } else if (state.lastCmd == "closed") {
            if (logEnable) log.debug "Status is closed, close was hit. unscheduling all and updating status immediately"
	        unschedule(updateStatus)
            closingTime=0
        } else {
            if (logEnable) log.debug "else called in close, lastCmd is ${state.lastCmd}, setting closingTime to timeToClose"
            closingTime = timeToClose
        }   
        if (logEnable) log.debug "Sending close GET request to ${url}"  
        get(url,"closing")
    
        if (logEnable) log.debug "Closing... timeToClose: ${closingTime}"
         
        closingTime = closingTime * 1000
        runInMillis(closingTime.toInteger(),updateStatus,[data: [status: "closed", level: 0,secs: timeToClose]])
        if (rfoffenabled)runInMillis(500,"rawOff")
    }
}

def open() {
    url = "http://" + sonoffIP + "/cm?cmnd=RfRaw%20" + openB0 +  date()
    
     if (state.lastCmd != "opening") {
        
        UpdateTimeRunning()    
    
        def closingTime = timeToClose
    
        if (state.lastCmd == "closing") {
            unschedule(updateStatus)
            if (logEnable) log.debug "Status was closing, open was hit - scheduling closingTime for ${state.secs}"
            closingTime = state.secs
        } else if (state.lastCmd == "stopped") { 
            closingTime = state.secs
            if (logEnable) log.debug "Going from Stopped to Opening. closingTime will now be: ${closingTime}"
        } else if (state.lastCmd == "open") {
            if (logEnable) log.debug "Status is open, open was hit. unscheduling all and updating status immediately"
            unschedule(updateStatus)
            closingTime=0
        } else {
            if (logEnable) log.debug "else called in open, lastCmd is ${state.lastCmd}, setting closingTime to timeToClose"
            closingTime = timeToClose
        }       
    
        if (logEnable) log.debug "Sending open GET request to ${url}"
	    get(url,"opening")
   
        if (logEnable) log.debug "Opening... timeToOpen: ${closingTime}"
  
        closingTime = closingTime * 1000
        runInMillis(closingTime.toInteger(),updateStatus,[data: [status: "open", level: 100, secs: 0]])
         sendEvent(name: "switch", value: "on")
         if (rfoffenabled)runInMillis(500,"rawOff")
    }
}

def updateStatus(data) {
    state.level = data.level
    state.lastCmd = data.status
    state.secs = data.secs
    if (logEnable) log.debug "Running updateStatus: status: ${state.lastCmd}, level: ${state.level}, secs: ${state.secs}"
    sendEvent(name: "level", value: "${state.level}", isStateChange: true)
    sendEvent(name: "windowShade", value: "${state.lastCmd}", isStateChange: true)
    if (state.level <= 0) {
         sendEvent(name: "switch", value: "off")
    } else {
        sendEvent(name: "switch", value: "on")
    }
}

def stop() {
     url = "http://" + sonoffIP + "/cm?cmnd=RfRaw%20" + stopB0 +  date()
    
    unschedule(updateStatus)
    
    UpdateTimeRunning()
	
    if (logEnable) log.debug "Sending stop GET request to ${url}"
    get(url,"partially open")
    state.level=100-((state.secs/timeToClose)*100).toInteger()
    state.data = [status: "partially open", level: state.level, secs: state.secs]
    updateStatus(state.data)
    state.lastCmd = "stopped"
    if (rfoffenabled)runInMillis(500,"rawOff")
}

def UpdateTimeRunning() {
    def now = new Date().getTime()
    def timeRunningSecs = null 
    def timeRunning = null 
    if (state.stateChangeTime) {
        timeRunning = now.toInteger()-state.stateChangeTime.toInteger()
        timeRunningSecs = timeRunning/1000
        if (logEnable) log.debug "Found that ${now} - ${state.stateChangeTime} = ${timeRunning} ($timeRunningSecs)"

    }
    if ((timeRunningSecs > timeToClose) || (state.lastCmd == "open") || (state.lastCmd == "closed") || (state.lastCmd == "stopped")) { 
        if (logEnable) log.debug "Found that ${timeRunningSecs} > ${timeToClose} OR lastCmd was open/closed/stopped"
        if (logEnable) log.debug "Resetting state.stateChangeTime = ${now}, lastCmd: ${state.lastCmd}"
        state.stateChangeTime = now
    } else if ((state.stateChangeTime < now) && (timeRunningSecs != null)) {
        if (logEnable) log.debug "UpdateTimeRunning- Time Running: ${timeRunningSecs} Old State.secs: ${state.secs} Last Cmd: ${state.lastCmd}"
        if (state.lastCmd == "closing") {
        	state.secs=timeRunningSecs + state.secs
        } else if (state.lastCmd == "opening") {
        	state.secs=state.secs - timeRunningSecs
        }
        if (logEnable) log.debug "UpdateTimeRunning- Time Running: ${timeRunningSecs} New State.secs: ${state.secs}"
    } else {
        if (logEnable) log.debug "state.stateChangeTime = ${state.stateChangeTime} timeRunningSecs = ${timeRunningSecs}"
        if (logEnable) log.debug "Setting state.stateChangeTime = ${now}"
        state.stateChangeTime = now
    }
}

def stopPosition(data) {
   stop()
    if (logEnable) log.debug "Sending stop GET request to ${url}"
	get(url,"partially open")
    state.data = [status: "partially open", level: data.newposition, secs: state.secs]
    updateStatus(state.data)
    if (logEnable) log.debug "Stopped ${data.newposition} ${data.difference}"
}

def runAndStop(data) {
    if (data.direction == "up") {
         open()
    } else {
       close()
    }
    if (logEnable) log.debug "Adjusting ${data.direction} to ${data.newposition} for ${data.difference} request to ${url}"
    get(url,"partially open")   
    runInMillis(data.difference.toInteger(),stopPosition,[data: [newposition: data.newposition, difference: data.difference]])
}

def setPosition(position) {
    secs = timeToClose-((position/100)*timeToClose)   
    if (logEnable) log.debug "Setting Position for ${blindCode} to ${position} (maps to travel time from open of ${secs} secs)"
    if (secs >= timeToClose) {
		secs = timeToClose
	}
    if (position != state.level)  {
        state.lastCmd="stopped"
        if (logEnable) log.debug "Position: ${position}  StateLevel: ${state.level}  StateSecs: ${state.secs} Secs: ${secs} Last Cmd: ${state.lastCmd}"
            if ((position > state.level) || (state.secs > secs)) { 
                if (position == 100) {
                    open()
                } else {
                    pos = ((state.secs - secs) * 1000)
                    if (pos < 1000) {
                         pos = 1000
                    }
                    state.secs = secs
                    if (logEnable) log.debug "Opening... Stopping at ${pos} secs"
                    runInMillis(10,runAndStop,[data: [direction: "up", difference: pos, newposition: position]])
                }
            } else {  
                if (position == 0) {
                    close()
                } else {
                    def pos = ((secs - state.secs)*1000)
                    if (pos < 1000) {
                         pos = 1000
                    }
                    state.secs = secs
                    if (logEnable) log.debug "Closing... Stopping at ${pos} secs"
                    runInMillis(10,runAndStop,[data: [direction: "down", difference: pos, newposition: position]])
                     
                }
            }
      }
}

def rawOff() {
    if (logEnable) log.debug "Sending off GET request to [${settings.sonoffIP}]"

    try {
        httpGet("http://" + sonoffIP + "/cm?cmnd=RfRaw%20" + 0){ resp -> 
            if (resp.success) {
            }
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to off failed: ${e.message}"
    }
}

def stopLevelChange(){
             stop()
}
def startLevelChange(direction) {
    if (direction == "up") {
        open()
    } else {
       close()
    }
}
def stopPositionChange(){
             stop()
}
def startPositionChange(direction) {
    if (direction == "open") {
        open()
    } else {
       close()
    }
}

def setLevel(level) {
    setPosition(level)
    
}
