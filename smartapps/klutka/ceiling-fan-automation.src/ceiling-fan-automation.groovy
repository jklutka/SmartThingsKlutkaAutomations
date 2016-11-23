/**
 *  Copyright 2016 Justin Klutka
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
 *  Synchro Ceiling Fans
 *
 *  Author: Justin Klutka
 */
 
 /*
 	Version History:
    
    1.0 - 5/29/2016 - Basic version release.  You may specifiy a thermostat and a set of fans
    
    
    Future Plans:
    - Add conditional preferences. Ex: Mode of the house, time of day, day of week, if certain people are home
 */

definition(
    name: "Ceiling Fan Automation",
    namespace: "klutka",
    author: "Justin Klutka",
    description: "Designed to coordinate device operating state for a thermostat to a set of switches.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light24-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light24-icn@2x.png"
)

preferences() {
	section("Thermostat Setup") {
		input "thermostat", "capability.thermostat", required: true, title: "Select your thermostat"
        input "triggerStates", "enum", title: "Select your trigger operating states", multiple: true, required: false, options:
          [["heating": "heating"], ["cooling": "cooling"], ["fan only": "fan only"], ["idle": "idle"]]
	}
    section("Fan Setup") {
    	input "fans", "capability.switch", required: true, multiple: true, title: "Select your Fans or Fan Switches"
    }
    section("Snooze Events") {
    	input "motionSensors", "capability.motionSensor", required: false, title: "Snooze if motion is detected in one of these locations.", multiple: true
    }
    
}

def installed()
{
	subscribeToEvents()
}

def updated()
{
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents()
{
	
	if (thermostat) {
        subscribe(thermostat, "thermostatOperatingState", operatingStateHandler)
	}
	evaluate()
}

def operatingStateHandler(evt)
{
	evaluate()
}

private evaluate()
{
	
    if (thermostat) {
        def currentOperatingState = thermostat.currentState("thermostatOperatingState")?.value
        def fansShouldBeOn = false
        log.debug("The current thermostat operating state: ${currentOperatingState}.")
                        
        triggerStates.each { 
        	if (it == currentOperatingState) {
            	switchesOn()
                fansShouldBeOn = true
            }            
        }
        
        //handle off condition
        if (fansShouldBeOn == false)
        	switchesOff()
        
		//switch statement in case logical branching by operating state is needed in the future
        /*
		switch (currentOperatingState) {
        	case "heating":
            	switchesOn()
            	break
            
            case "pending heat":
            	switchesOn()
            	break
            
            case "cooling":
            	switchesOn()
            	break
            
            case "pending cool":
            	switchesOn()
            	break
            
            case "fan only":
            	switchesOn()
            	break
            
            default:
            	switchesOff()
                log.debug("evaluate:switch statement encountered an unsupported operating state during the event handler")
                break
        }
        */


    }
    else {
    	log.trace("A thermostat poll will be requested")
    	thermostat.poll()
    }
}

private switchesOn()
{	
	fans.each {
    	it.on()
    }
}

private switchesOff()
{
	fans.each {
    	it.off()
    }
}