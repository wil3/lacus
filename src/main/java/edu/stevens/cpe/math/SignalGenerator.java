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
package edu.stevens.cpe.math;

import java.io.File;

public class SignalGenerator {

	/**
	 * 
	 * @param start		The start frequency
	 * @param end		The end frequency
	 * @param interval	Time interval
	 * @param steps		Number of samples to take in interval
	 * @return
	 */
	public static  double [][] linearChirp(double start, double end, double interval, double steps){

		 double [] samples = new double [(int)steps];
		 double [] times = new double [(int)steps];
		 double [] phases = new double [(int)steps];

		 for (int i = 0; i < steps; ++i) {
		        double delta = ((double)i) / steps;
		        double t = interval * delta;
		        double phase = 2 * Math.PI * t * (start + (end - start) * delta / 2);
		      // while (phase > 2 * Math.PI) phase -= 2 * Math.PI; // optional
		       samples[i] =  .5*Math.sin(phase) + .5;
		       times[i] = t;
		       phases[i] = phase;
		    }
			//MLArrayUtils.printArray(new File("/home/wil/Documents/Research/Projects/chirp.dat"), times, samples);
		//	MLArrayUtils.printArray(new File("/home/wil/Documents/Research/Projects/phase.dat"), times, phases);
		 return new double[][]{samples,phases};
	}
	
	public static double [] line(int interval, int samples, double slope, double intercept){
		double [] line= new double [samples];
		for (int i=0; i<samples; i++){
			double delta = ((double) i) / samples;
			double t = interval * delta;
			line[i] = slope * t + intercept;
		}
		return line;
	}
	/**
	 * y = a + bsin(k(t-c))
	 * a = veritcal
	 * b = amplitude
	 * k = period
	 * c = phase
	 * @param interval
	 * @param samples
	 * @param amplitude
	 * @param period
	 * @param phase
	 * @param vertical shift
	 * @return
	 */
	public static double [] sine(int interval, int samples, double amplitude, double period, double phase, double vertical){

		//Create input samples
		double [] sine= new double [samples];
		for (int i=0; i<samples; i++){
			double delta = ((double) i) / samples;
			double t = interval * delta;
			// sine_4[i] = .5*Math.sin(((double)i)/4.0)+0.5;
			 sine[i] = amplitude * Math.sin(period * (t-phase)) + vertical;
		}
		return sine;
	}
	public static void main(String[] args){
		MLArrayUtils.print(new File("/home/wil/Documents/Research/Projects/line.dat"), line(5,1000,(1.0/5.0),0));

	}
}
