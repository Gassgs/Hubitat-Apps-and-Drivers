/**
 *  TTS Broadcast and Critical Relay Device
 *
 *  Virtual Driver for TTS Broadcst App
 *  
 *
 *  Copyright 2021 Gassgs/ Gary Gassmann
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
 *  Change History:
 *
 *    V1.0  10-24-2021   -       first run - Gassgs
 *    V1.2  10-28-2021   -       Added last message attibute for dashboards - Gassgs
 *    V1.3  11-14-2021   -       Added date info to last message attribute - Gassgs
 *  
 */

metadata {
    definition (name:"TTS Relay Device", namespace: "Gassgs", author: "Gary G") {
        capability "Actuator"
        capability "SpeechSynthesis"


        attribute"message","string"
        attribute"lastMessage","string"
        attribute"volume","string"
        attribute"voice","string"
    }
   
}
    preferences {
        input name: "logInfo", type: "bool", title: "<b>Enable info logging</b>", defaultValue: true
}
        
def speak(string,volume = "null",voice = "null"){
    now = new Date()
    dateFormat = new java.text.SimpleDateFormat("EE MMM d")
    timeFormat = new java.text.SimpleDateFormat("h:mm a")

    newDate = dateFormat.format(now)
    newTime = timeFormat.format(now)
    
    timeStamp = newDate + " " + newTime as String
    
    if (logInfo) log.info "$device.label Text - $string - Volume $volume - Voice $voice"
    sendEvent(name:"message",value:string)
    sendEvent(name:"volume",value:volume)
    sendEvent(name:"voice",value:voice)
    sendEvent(name:"lastMessage",value: timeStamp + " -- " + string)
    runIn(2,clearMsg)
}

def clearMsg(){
    if (logInfo) log.info "$device.label clearing"
    sendEvent(name:"message",value:"clear")
    sendEvent(name:"volume",value:"clear")
    sendEvent(name:"voice",value:"clear")
}
