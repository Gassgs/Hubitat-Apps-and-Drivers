/**
 *  King Of Fans Zigbee Fan Controller - Light Child Device
 *
 *  
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
def version() {"v2.0"}

metadata {
	definition (name: "KOF Fan Light Child", namespace: "Gassgs", author: "GaryG") {
		capability "Actuator"
        capability "Switch"
        capability "Switch Level"
        capability "Change Level"
        capability "Light"
        capability "Sensor"
        
        command "flash"
   }
}

preferences {
    input( "lastOn","bool", title: "<b>Enable On = Last Level,</b> (Default = 100%)", defaultValue:true)
    input( "transRate","number", title: "<b>Change Level Speed in ms</b> (150 - 1000)",range: 150..1000, defaultValue:250)
    input( "fadeOff","bool", title: "<b>Enable for Instant Off </b>", defaultValue:false, submitOnChange: true)
    if (!fadeOff){
        input( "fadeRate","number", title: "<b>Fade Off Speed in ms</b> (50 - 1000)",range:50..1000, defaultValue:250)
    }
    input (name: "flashRate", type: "enum", title: "<b>Flash Rate</b>", defaultValue: 750, options: [750:"750ms", 1000:"1s", 2000:"2s", 5000:"5s" ])
    input( "logInfo","bool", title: "<b>Enable Info Text Loggging</b>", defaultValue:true)
}


def on() {
    if (state.flashing){
        stopFlash()
    }
    if (lastOn){
        level = state.lastLevel as Integer
    }else{
        level = 100
    }
    if (logInfo) log.info "$device.label Light On level $level"
    setLevel(level)
}

def off() {
    if (state.flashing){
        stopFlash()
    }
    currentLevel = device.currentValue("level") as Integer
    state.lastLevel = currentLevel
    if (logInfo) log.info "$device.label Light Off"
    if (fadeOff){
        parent.off()
    }
    else{
        state.levelChange = currentLevel
        fade()
    }
}

def fade(){
    currentLevel = state.levelChange as Integer
    if (currentLevel > 5 && currentLevel <= 30 ){
        newLevel = (currentLevel - 5)
        parent.setLevel(newLevel)
        state.levelChange = (currentLevel - 5) as Integer
        runInMillis(fadeRate,fade)
    }
    else if (currentLevel > 30 ){
        newLevel = (currentLevel - 10)
        parent.setLevel(newLevel)
        state.levelChange = (currentLevel - 10) as Integer
        runInMillis(fadeRate,fade)
    }else{
        unschedule(fade)
        parent.off()
    }  
}

def setLevel(val, duration = null) {
    if (logInfo) log.info "$device.label Set Level $val"
    if (val == 0) {
        off()
    }else{
        parent.setLevel(val)
    }
}

def startLevelChange(dir) {
    if (logInfo) log.info "$device.label Change Level $dir"
    currentLevel = device.currentValue("level") as Integer
    state.levelChange = currentLevel
    if (dir == "up"){
        levelUp()
    }else{
        levelDown()
    }
}

def levelUp(){
    currentStatus = device.currentValue("switch")
    currentLevel = state.levelChange as Integer
    if (currentStatus == "off"){
        parent.setLevel(5)
        state.levelChange = 5
        runInMillis(transRate,levelUp)
    }else if (currentLevel <= 30 ){
        newLevel = (currentLevel + 5)
        parent.setLevel(newLevel)
        state.levelChange = (currentLevel + 5) as Integer
        runInMillis(transRate,levelUp)
    }
    else if (currentLevel >30 && currentLevel < 100){
        newLevel = (currentLevel + 10)
        parent.setLevel(newLevel)
        state.levelChange = (currentLevel + 10) as Integer
        runInMillis(transRate,levelUp)
    }else{
        stopLevelChange()
    }  
}

def levelDown(){
    currentLevel = state.levelChange as Integer
    if (currentLevel > 5 && currentLevel <= 30 ){
        newLevel = (currentLevel - 5)
        parent.setLevel(newLevel)
        state.levelChange = (currentLevel - 5) as Integer
        runInMillis(transRate,levelDown)
    }
    else if (currentLevel > 30 ){
        newLevel = (currentLevel - 10)
        parent.setLevel(newLevel)
        state.levelChange = (currentLevel - 10) as Integer
        runInMillis(transRate,levelDown)
    }else{
        stopLevelChange()
    }  
}

def stopLevelChange() {
    if (logInfo) log.info "$device.label Stop Level Change"
	unschedule(levelUp)
    unschedule(levelDown)
}

def flash(){
    if (state.flashing){
        stopFlash()
        if (state.restore){
            on()
        }
        else{
            off()
        }
    }
    else{
        if (logInfo) log.info "$device.label Flashing Started"
        state.flashing = true
        currentStatus = device.currentValue("switch")
        if (currentStatus == "on"){
            state.restore = true
            flashOff()
        }
        else{
            state.restore = false
            level = state.lastLevel as Integer
            setLevel(level)
            runInMillis(flashRate as Integer,flashOff)
        }
    }  
}

def flashOn(){
    parent.on()
    runInMillis(flashRate as Integer,flashOff)
}

def flashOff(){
    parent.off()
    runInMillis(flashRate as Integer,flashOn)  
}

def stopFlash(){
    if (logInfo) log.info "$device.label Flashing Ended"
    unschedule(flashOn)
    unschedule(flashOff)
    state.flashing = false
}

def installed() {
 	initialize()
}

def updated() {
 	initialize()
}

def initialize() {
    state.version = version()
}
