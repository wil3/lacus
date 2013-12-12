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
package edu.stevens.cpe.reservior.readout;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.optimization.direct.CMAESOptimizer;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.log4j.Logger;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;

import edu.stevens.cpe.reservior.ReserviorException;
import edu.stevens.cpe.reservior.ReservoirNetwork;
import edu.stevens.cpe.reservior.layers.ReadoutFunction;
import edu.stevens.cpe.reservior.layers.SpikingOutput;
import edu.stevens.cpe.reservior.neuron.IFSpikingNeuron;

public class CMAES {
	public static Logger logger = Logger.getLogger(CMAES.class);
	private ReservoirNetwork reservoir;
	private MLDataSet trainingSet;
	private double startValue = 0;
	private double lowerBound = 0;
	private double upperBound = 50;
	private double insigma = .1;
	private int epoch = 0;
	private ReadoutFunction readout;
	
	double minError = Double.POSITIVE_INFINITY; 
	double [] bestWeights;
	public CMAES(ReservoirNetwork reservoir, MLDataSet trainingSet,ReadoutFunction readout ){
		this.reservoir = reservoir;
		this.trainingSet = trainingSet;
		this.readout = readout;
	}
	
	private final MultivariateFunction fitnessFuntion = new MultivariateFunction(){

		@Override
		public double value(double[] weights) {
			double error  = 0;
			updateReadoutWeights(weights);
			for(MLDataPair pair: trainingSet ) {
				try {
					
					reservoir.input(pair.getInput());
					double [] output = readout.getOutput().getData();
					double [] target = pair.getIdeal().getData();
					//error += compareSpikeTrains(target, output);
					error += ErrorUtility.computeLinearRegressionError(target, output);
					//logger.info("error");
				} catch (ReserviorException e) {
					//Because we are in an implemented class kill this way for now.
					logger.error(e);
					System.exit(0);
				}
				//After each time the reservoir sees a training entry reset the reservoir
				reservoir.getReservior().reset();
				readout.reset();

			}
			//error *= error;
			CMAES.this.epoch++;
			//ByteArrayOutputStream baos = new ByteArrayOutputStream();
		//	PrintStream ps = new PrintStream(baos);
		//	ps.printf("%2d \t %5.5f",epoch, error);
		//	logger.info(baos.toString());
			logger.info(epoch + "\t" + error + "\t*" + minError);
			if (error < minError){
				minError = error;
				bestWeights = weights;
			}
			return error;
		}
		
	};

	
		
	public void updateReadoutWeights(double [] newWeights){
		//TODO remove neuron hardcoding type
		int index = 0;
		for (int i=0; i<((SpikingOutput)readout).getNeurons().length; i++){
			HashMap<String, Double> weights = ((SpikingOutput)readout).getNeurons()[i].getWeights();
			Iterator<String> it = weights.keySet().iterator();
			while (it.hasNext()){
				String id = it.next();
				weights.put(id, newWeights[index]);
				index++;
			}
		}
	}

	public double [] train(int numberWeightsToTrain){
		this.epoch = 0;
		
		//Number of weights to evolve
		final double[] start = new double [numberWeightsToTrain];
		Arrays.fill(start, startValue);
		final double[] lower = new double [numberWeightsToTrain];
		Arrays.fill(lower, lowerBound);
		final double[] upper = new double [numberWeightsToTrain];
		Arrays.fill(upper, upperBound);
		
		//Set to 1/3 the initial search volume
		final double[] sigma = new double [numberWeightsToTrain];
		Arrays.fill(sigma, insigma);
		
		int lambda =  4 + (int)(3.*Math.log(numberWeightsToTrain));
		int maxEvals = CMAESOptimizer.DEFAULT_MAXITERATIONS;
		double stopValue = .01;
		boolean isActive = true; //Chooses the covariance matrix update method.
		int diagonalOnly = 0;
		int checkFeasable = 0;
		final CMAESOptimizer optimizer = new CMAESOptimizer( lambda, sigma, CMAESOptimizer.DEFAULT_MAXITERATIONS,
                stopValue, isActive, diagonalOnly,
                checkFeasable,  new MersenneTwister(), false);
		
		
		final PointValuePair result = optimizer.optimize(maxEvals, fitnessFuntion, GoalType.MINIMIZE, start, lower, upper);
		logger.info(Arrays.toString(result.getPoint()));
		logger.info("Best weights: " + Arrays.toString(bestWeights));

		updateReadoutWeights(result.getPoint());
		logger.info("done training.");
		return bestWeights;
	}
}
