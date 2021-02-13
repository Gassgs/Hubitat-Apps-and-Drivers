/**
 *  ****************  Motion Automation ****************
 *
 *  Motion automation parent app. Parent for Advanced lighting,Simple Lighting and
 *  Blinds + Shades.
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
 *  V1.0.0  -       2-08-2021       First attempt 
 *  V1.1.0  -       2-09-2021       visual improvements, changed name
 *  V1.2.0  -       2-11-2021       Added support for 3 child apps   
 */

import groovy.transform.Field

definition(
    name: "Motion Automation",
    namespace: "Gassgs",
    author: "Gary G",
    description: "Motion Automation Parent App",
    category: "",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences{
    page(
        name: "mainPreferences",
        title: "<div style='text-align:center'><b>Motion Automation</b></div>",
        install: true,
        uninstall: true)
        {
        section{
            app(
                name: "childApps",
                appName: "Motion Lighting Advanced Child",
                namespace: "Gassgs",
                title: "<b>Add New Motion Lighting Advanced Rule</b>",
                multiple: true
                )
        }
        section{
            app(
                name: "childApps",
                appName: "Motion Lighting Simple Child",
                namespace: "Gassgs",
                title: "<b>Add New Motion Lighting Simple Rule</b>",
                multiple: true
                )
        }
        section{
            app(
                name: "childApps",
                appName: "Motion Blinds and Shades Child",
                namespace: "Gassgs",
                title: "<b>Add New Motion Blinds and Shades Rule</b>",
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
