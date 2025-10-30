/**
 *  Govee Integrataion Tools
 *
 *  Copyright 2015 Bryan Greffin
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
 */
 /**
 * 10/31/2018
 * Updated required fields so the application can be used simply to humidify or dehumidify and not require both functions
 * Corrected value used for humidification low value to turn on assigned switch
 */
definition(
    name: "Govee Integrataion Tools",
    namespace: "Mavrrick",
    author: "Craig King",
    description: "Manage Tools Apps for the Govee Integration Devices",
    category: "Convenience",
    installOnOpen: "true",
    singleInstance: "true",
    iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather2-icn",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather2-icn?displaySize=2x"
    importUrl:   "https://raw.githubusercontent.com/Mavrrick/Hubitat-by-Mavrrick/refs/heads/main/Govee/show/GoveeIntegrationTools.groovy",
)

preferences {
    page(name: "setupMain")
}

def setupMain() {
    dynamicPage(name: "setupMain", title: " ", install: true, uninstall: true) {
        section("Govee Integration Light Effect App") {
            app(name: "Govee Integrataion Tools", appName: "Govee LightEffects Show", namespace: "Mavrrick", title: "Govee LightEffects Show", multiple: true)
        } 
    }
}
        
def installed() {

}

def updated() {

}
     
