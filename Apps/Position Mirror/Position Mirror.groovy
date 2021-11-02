/**
 *  **************** Position Mirror ****************
 *
 * Used to group blinds or shades. Mirror position changes from one shade to one or more other shades
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
 *  Last Update: 10/17/2021
 *
 *  Changes:
 *
 *  V1.0.0 -        10-17-2021       First run
 *
 */

import groovy.transform.Field

definition(
    name: "Position Mirror",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Position Mirror App",
    category: "Utilities",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences {

	section {

     paragraph(
         title: "Position Mirror",
         required: false,
    	"<div style='text-align:center'><b><big>: Position Mirror :</big></b></div>"
    	)
        paragraph( "<div style='text-align:center'><b>Master Shade Device</b></div>"
        )
        input(
            name:"master",
            type:"capability.windowShade",
            title: "<b> - Master Shade Device - </b>",
            required: true
            )
    }
    section{
        
        paragraph(
         title: "Position Mirror",
         required: false,
    	"<div style='text-align:center'><b><big>: Position Mirror :</big></b></div>"
    	)
        paragraph( "<div style='text-align:center'><b>Mirror Shade Device(s)</b></div>"
        )
        input(
            name:"mirror",
            type:"capability.windowShade",
            title: "<b> - Mirror Shade Device - </b>",
            multiple: true,
            required: true,
            submitOnChange: true
            )
    }
    section{
        input(
            name:"logEnable",
            type:"bool",
            title: "<b> - Enable Info Logging - </b>",
            required: true,
            defaultValue: false,
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
    subscribe(settings.master, "position", positionHandler)
    logInfo ("subscribed to Events")
}

def positionHandler(evt){
    logInfo ("$app.label position $evt.value recieved")
    settings.mirror.setPosition(evt.value as Integer)
    logInfo ("$app.label position $evt.value sent")
}

void logInfo(String Msg){
    if (settings?.logEnable !=false){
        log.info "$Msg"
    }
}
