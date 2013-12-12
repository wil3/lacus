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
package edu.stevens.cpe.reservoir.translate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.optimization.direct.CMAESOptimizer;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.log4j.Logger;
import org.encog.ml.data.MLDataPair;

import edu.stevens.cpe.math.MLArrayUtils;
import edu.stevens.cpe.math.SignalGenerator;
import edu.stevens.cpe.reservior.ReserviorException;
import edu.stevens.cpe.reservior.readout.CMAES;
import edu.stevens.cpe.reservior.readout.ErrorUtility;
/**
 * Hough Spiker Algorithm
 * 
 * encoding scheme has only been used for encoding sound data. However, since EEG
 * signals also fall under the frequency domain,
 * 
 * Configuration:
 * 	For each new signal the convolution filter must be adjusted.
 * @author wil
 *
 */
public class HSA implements Translator {
	public static Logger logger = Logger.getLogger(HSA.class);
	/**
	 * Every filter has optimal threshold
	 */
	public static double THRESHOLD = 0.7;//0.955;
		
	private double [] analog;
	private double [] filter = new double [] {0.3, 0.3, 0.3, 0.3, 0.3,0.3,0.3};

	public HSA(){}
	/**
	 * 
	 * @param analog The original input analog signal which will be converted into spike trains.
	 */
	public HSA(double[] analog){
		this.analog = analog;
	}
	/**
	 * Fit the convolution filter to multiple signals
	 * @param analogs
	 */
	public HSA(double[][] analogs){
		//this.analog = analog;
	}
	/**
	 * 
	 * @param analog Load the signal from a file
	 */
	public HSA(File analog){
		
	}
	
	/**
	 * Decode the spike trains with the current filter.
	 * @param spiketrains
	 */
	@Override public double[] decode(double [] spiketrains) {
		int width =  spiketrains.length ;//+ filter.length -1;
		
		double [] output = new double [width];
		//Be non-invasive, make copy
		double [] spikes = Arrays.copyOf(spiketrains, spiketrains.length); //ArrayUtils.pad(spiketrains, filter.length, true);//new double [width];
		ArrayUtils.reverse(spikes);
		int shiftIndex = spikes.length-1;
		for (int k=0; k < width; k++){
			double sum= 0;

			for (int j=0; j< filter.length; j++){
				if (shiftIndex+j < spikes.length){// && shiftIndex + j >=0 ){
					sum += spikes[shiftIndex+j] * filter[j];
				} else {
					break;
				}
			}
			
			output[k] = sum;
			shiftIndex--;
		}
		
		return output;
	}


	//@Override 
	public double[] encode2() {
		//Make copy because we are going to modify it
		double [] _analog = Arrays.copyOf(analog, analog.length);

		double [] output = new double [_analog.length];
		
		for (int i=0; i< _analog.length; i++){
			
			//Compute errors
			double error1 = 0;
			double error2 = 0;
			for (int j=0; j< filter.length; j++){
				if (i+j < _analog.length){
					error1 += Math.abs(_analog[i+j] - filter[j]);
					error2 += Math.abs(_analog[i+j]);
				}
			}
			
	
			if (error1 <= (error2 - THRESHOLD)){
				output[i] = 1;
				for (int j=0; j < filter.length; j++){
					if (i+j < _analog.length){
						_analog[i+j] -= filter[j];
					}
				}
			} else {
				output[i] = 0;
			}
			
		}
		
		
		return output;
	}

	
	@Override public double[] encode() {
		//Make copy because we are going to modify it
		double [] input = Arrays.copyOf(analog, analog.length);

		double [] output = new double [input.length];
		
		for (int i=0; i< input.length; i++){
			
			//Compute errors
			double count = 0;
			for (int j=0; j< filter.length; j++){
				if (i+j < input.length){
					if (input[i+j] >= filter[j]) {
						count ++;
					}
				}
			}
			
	
			if (count == filter.length){
				output[i] = 1;
				for (int j=0; j < filter.length; j++){
					if (i+j < input.length){
						input[i+j] -= filter[j];
					}
				}
			} else {
				output[i] = 0;
			}
			
		}
		
		
		return output;
	}
	
	public void optimize(int size, double initValue, double lowerBound, double upperBound, double inSigma){
		FilterOptimization opt = 	new FilterOptimization();
		opt.setTrainingData(analog);
		opt.optimize(size,initValue,lowerBound,upperBound,inSigma);
	}
	/**
	 * 
	 * @param size Filter size
	 */
	public void optimize(int size){
		FilterOptimization opt = 	new FilterOptimization();
		opt.setTrainingData(analog);
		opt.optimize(size);
	}

	/**
	 * @return the filter
	 */
	public double[] getFilter() {
		return filter;
	}


	/**
	 * @param filter the filter to set
	 */
	public void setFilter(double[] filter) {
		this.filter = filter;
	}


	/**
	 * @return the analog
	 */
	public double[] getAnalog() {
		return analog;
	}


	/**
	 * @param analog the analog to set
	 */
	public void setAnalog(double[] analog) {
		this.analog = analog;
	}


	class FilterOptimization {
		
		private double startValue = .3;
		private double lowerBound = -1;
		private double upperBound = 1;
		private double insigma = .1;

		
		private double [] target;
		private int epoch = 0;
		private double minError = Double.POSITIVE_INFINITY; 
		private double [] bestCoeff;
		private HSA bsa;
		private final MultivariateFunction fitnessFuntion = new MultivariateFunction(){

			@Override
			public double value(double[] coeff) {
				double error  = 0;
				
				bsa.setFilter(coeff);
				double [] spikes = bsa.encode();
				double [] decoded = bsa.decode(spikes);
				error = ErrorUtility.computeLinearRegressionError2(target, decoded);
				

				epoch++;
			//	logger.info(epoch + "\t" + error + "\t*" + minError);
				if (error < minError){
					minError = error;
					bestCoeff = coeff;
				}
				return error;
			}
			
		};
		public void optimize(int size, double initValue, double lowerBound, double upperBound, double inSigma){
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
			this.insigma = inSigma;
			this.startValue = initValue;
			optimize(size);
		}
		/**
		 * Optimize the convolution filter
		 */
		public void optimize(int size){
			this.epoch = 0;
			
			//Number of weights to evolve
			final double[] start = new double [size];
			Arrays.fill(start, startValue);
			final double[] lower = new double [size];
			Arrays.fill(lower, lowerBound);
			final double[] upper = new double [size];
			Arrays.fill(upper, upperBound);
			
			//Set to 1/3 the initial search volume
			final double[] sigma = new double [size];
			Arrays.fill(sigma, insigma);
			
			int lambda =  4 + (int)(3.*Math.log(size));
			int maxEvals = 100000;//CMAESOptimizer.DEFAULT_MAXITERATIONS;
			double stopValue = .1;
			boolean isActive = true; //Chooses the covariance matrix update method.
			int diagonalOnly = 0;
			int checkFeasable = 0;
			
			this.bsa = new HSA(target);
			
			final CMAESOptimizer optimizer = new CMAESOptimizer( lambda, sigma, 100000,//CMAESOptimizer.DEFAULT_MAXITERATIONS,
	                stopValue, isActive, diagonalOnly,
	                checkFeasable,  new MersenneTwister(), false);
			
			
			final PointValuePair result = optimizer.optimize(maxEvals, fitnessFuntion, GoalType.MINIMIZE, start, lower, upper);
			logger.info(Arrays.toString(result.getPoint()));
			logger.info("done evolving after " + epoch + ". Error="+minError);
			HSA.this.setFilter(bestCoeff);
		}
		/**
		 * @return the trainingData
		 */
		public double[] getTrainingData() {
			return target;
		}
		/**
		 * @param trainingData the trainingData to set
		 */
		public void setTrainingData(double[] trainingData) {
			this.target = trainingData;
		}

	}
	
	
	public static void sineTest(){
		int samples = 300;  
		//Create input samples
		double [] analog = SignalGenerator.sine(300, samples, (1.0/2.0), (1.0/4.0), 0, (1.0/2.0));
	
		HSA bsa = new HSA(analog);
		bsa.optimize(10, .1, 0, 1, .2);
		
		double [] spikes = bsa.encode();
		double [] output = bsa.decode(spikes);
		
		MLArrayUtils.print(new File("/home/wil/Documents/Research/Projects/analog.dat"), analog);
		MLArrayUtils.print(new File("/home/wil/Documents/Research/Projects/spikes.dat"), spikes);
		MLArrayUtils.print(new File("/home/wil/Documents/Research/Projects/output.dat"), output);
		MLArrayUtils.print(new File("/home/wil/Documents/Research/Projects/filter.dat"), bsa.getFilter());

	}
	
	public static void encodeTest(){
		double [] analog = new double[] {1, 5, 13, 15, 7, 7, 6, 2, 9, 5, -2};
		double [] filter = new double[]{1,4,9,5,-2};
		HSA bsa = new HSA();
		bsa.setAnalog(analog);
		bsa.setFilter(filter);
		double [] spikes = bsa.encode2();
		System.out.println("Input: " + Arrays.toString(analog));
		System.out.println("Spikes: " + Arrays.toString(spikes));

		double [] decoded = bsa.decode(spikes);
		System.out.println("Decoded: " + Arrays.toString(decoded));
	}
	//TODO put this into unit test later
	public static void decodeTest(){
		HSA bsa = new HSA();
		System.out.println("Decode Test");
		bsa.setFilter(new double[] {1,4,9,5,-2});
		double [] analog = bsa.decode(new double [] {1, 0, 0, 1, 0, 1, 1});
		System.out.println(Arrays.toString(analog));
	}
	
	public static void expTest(){
		double [] analog = SignalGenerator.linearChirp(10,1,5,1000)[0];
		HSA bsa = new HSA(analog);
		bsa.optimize(10, .5, 0, 2, .1);
	
		double [] spikes = bsa.encode();
			MLArrayUtils.print(new File("/home/wil/Documents/Research/Projects/spikes.dat"), spikes);

		double [] decoded = bsa.decode(spikes);
		
		MLArrayUtils.print(new File("/home/wil/Documents/Research/Projects/analog.dat"), analog);
		MLArrayUtils.print(new File("/home/wil/Documents/Research/Projects/output.dat"), decoded);

		MLArrayUtils.print(new File("/home/wil/Documents/Research/Projects/filter.dat"), bsa.getFilter());

	}
	
	public static void lineTest(){
		double [] analog =SignalGenerator.line(5,1000,(1.0/5.0),0);
		HSA bsa = new HSA(analog);
		bsa.optimize(10, .1, 0, 1, .2);
		double [] spikes = bsa.encode();
		double [] decoded = bsa.decode(spikes);
		MLArrayUtils.print(new File("/home/wil/Documents/Research/Projects/spikes.dat"), spikes);
		MLArrayUtils.print(new File("/home/wil/Documents/Research/Projects/analog.dat"), analog);
		MLArrayUtils.print(new File("/home/wil/Documents/Research/Projects/output.dat"), decoded);
		MLArrayUtils.print(new File("/home/wil/Documents/Research/Projects/filter.dat"), bsa.getFilter());

	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//lineTest();
		//expTest();
		sineTest();
		//encodeTest();
		///decodeTest();
		
	}
}
