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

public class SpikeEvent {

	/**
	 * The amplitude of the current spike in volts?
	 */
	public static final double DEFAULT_SPIKE_HEIGHT = 1;// * Math.pow(10, -6); //1
	/**
	 * The time width of the pulse in nanoseconds
	 */
	public static final long DEFAULT_PULSE_WIDTH = 1 ;//* (long)Math.pow(10, 6); //1

	/**
	 * The strength of the pulse
	 */
	private double amplitude = 0;
	/**
	 * How long the pulse is applied for
	 */
	private long pulseWidth = 0;
	/**
	 * The time the spike occured
	 */
	private long time = 0;
	
	private int sourceID;
	
	public SpikeEvent(){
		this(DEFAULT_SPIKE_HEIGHT,DEFAULT_PULSE_WIDTH);
	}
	public SpikeEvent(double amplitude, long pulseWidth){
		this.amplitude = amplitude;
		this.pulseWidth = pulseWidth;
	}
	public double getAmplitude() {
		return amplitude;
	}

	public void setAmplitude(double amplitude) {
		this.amplitude = amplitude;
	}
	/**
	 * @return the executionTime
	 */
	public long getPulseWidth() {
		return pulseWidth;
	}
	/**
	 * @param pulseWidth the executionTime to set
	 */
	public void setPulseWidth(long pulseWidth) {
		this.pulseWidth = pulseWidth;
	}
	/**
	 * @return the time
	 */
	public long getTime() {
		return time;
	}
	/**
	 * @param time the time to set
	 */
	public void setTime(long time) {
		this.time = time;
	}
	/**
	 * @return the sourceID
	 */
	public int getSourceID() {
		return sourceID;
	}
	/**
	 * @param sourceID the sourceID to set
	 */
	public void setSourceID(int sourceID) {
		this.sourceID = sourceID;
	}
}
