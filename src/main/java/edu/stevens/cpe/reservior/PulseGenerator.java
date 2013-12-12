/*******************************************************************************
 *  Copyright 2013 William Koch
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/
package edu.stevens.cpe.reservior;

import org.bushe.swing.event.DiscreteTimeThreadSafeEventService;
import org.bushe.swing.event.EventBus;


public class PulseGenerator {

	/**
	 * 
	 * @param srcId
	 * @param value
	 */
	public static void pulse(String srcId, double value){
		long bits = Double.doubleToLongBits(1.0);
		String binString = Long.toBinaryString(bits);
		pulseSpikeTrain(srcId, binString);
	}
	public static void pulseSpikeTrain(String srcId, String binString){
		for (int i=0; i<binString.length(); i++){
			double current = (binString.charAt(i) == '1') ? SpikeEvent.DEFAULT_SPIKE_HEIGHT : 0;
			SpikeEvent ev = new SpikeEvent(current, SpikeEvent.DEFAULT_PULSE_WIDTH);
			ev.setTime(i);
			EventBus.publish(srcId,  ev);
		}
	}
}
