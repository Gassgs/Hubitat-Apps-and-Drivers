/**
 *  ****************  Motion Lighting ****************
 *
 *  Simple motion lighting app. Allows dim level instead of "off"
 *  disable app from running with switch,lux and mode options.
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
 *  V1.0.0 -        2-08-2021       First attempt GG
 */

import groovy.transform.Field

definition(
    name: "Motion Lighting",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Motion Lighting Parent App",
    category: "",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences{
    page(
        name: "mainPreferences",
        title: "<div style='text-align:center'><b>Motion Lighting</b></div>",
        install: true,
        uninstall: true)
        {
        section{
            app(
                name: "childApps",
                appName: "Motion Lighting Child",
                namespace: "Gassgs",
                title: "Add Motion Lighting Rule",
                multiple: true
                )
        }
        section{
            app(
                name: "childApps",
                appName: "Motion Lighting Simple Child",
                namespace: "Gassgs",
                title: "Add Motion Lighting Simple Rule",
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
