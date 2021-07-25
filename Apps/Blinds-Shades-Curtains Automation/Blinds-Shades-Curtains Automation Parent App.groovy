/**
 *  ****************  Blinds-Shades-Curtains Automation Parent App ****************
 *
 *  Blinds-Shades-Curtains Automation Parent App for Blinds-Shades-Curtains Child
 *  Mode blinds shades curtains app. Use this app to set postion based on mode.
 *  motion option to activate plus Lux level, outdoor temp, contact sensors
 *  and mode options.
 *   
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
 *  Last Update: 2/08/2021
 *
 *  Changes:
 *
 *  V1.0.0  -       2-13-2021       First Run
 *  V1.1.0  -       7-23-2021       Child app redo and naming
 *    
 */

import groovy.transform.Field

definition(
    name: "Blinds-Shades-Curtains Automation",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Blinds-Shades-Curtains Automation Parent App",
    category: "",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences{
    page(
        name: "mainPreferences",
        title: "<div style='text-align:center'><b><big>Blinds-Shades-Curtains Automation</big></b></div>",
        install: true,
        uninstall: true)
        {
            section{
            app(
                name: "childApps",
                appName: "Blinds-Shades-Curtains Child",
                namespace: "Gassgs",
                title: "<b>Add New Blinds-Shades-Curtains Rule</b>",
                multiple: true
                )
            }
    }
}

def uninstalled(){
    getChildApps().each { childApp ->
        deleteChildApp(childApp.id)
    }
}
