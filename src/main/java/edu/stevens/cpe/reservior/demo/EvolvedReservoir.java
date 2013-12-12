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
package edu.stevens.cpe.reservior.demo;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.optimization.direct.CMAESOptimizer;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.log4j.Logger;
import org.bushe.swing.event.DiscreteTimeThreadSafeEventService;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.EventServiceExistsException;
import org.bushe.swing.event.EventServiceLocator;
import org.encog.mathutil.error.ErrorCalculation;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;

import edu.stevens.cpe.math.MLArrayUtils;
import edu.stevens.cpe.math.MLMatrixUtils;
import edu.stevens.cpe.math.SignalGenerator;
import edu.stevens.cpe.reservior.ReserviorException;
import edu.stevens.cpe.reservior.ReservoirNetwork;
import edu.stevens.cpe.reservior.SpikeEvent;
import edu.stevens.cpe.reservior.layers.SpikingOutput;
import edu.stevens.cpe.reservior.neuron.IFSpikingNeuron;
import edu.stevens.cpe.reservior.readout.ErrorUtility;
import edu.stevens.cpe.reservior.topology.RandomSparseTopology;
import edu.stevens.cpe.reservoir.translate.HSA;

/**
 * memory = 2.53
 * rp = 0
 * alpha =0.84
 * @author wil
 *
 */
public class EvolvedReservoir {
	public static Logger logger = Logger.getLogger(EvolvedReservoir.class);

		private double insigma = .1;

		
		private double [] target;
		private int epoch = 0;
		private double minError = Double.POSITIVE_INFINITY; 
		private double [] bestCoeff;

		private double [] dataset;
		private double [] analog;
		private HSA hsa;
		
		SpikingOutput out;
		ReservoirNetwork<IFSpikingNeuron> network;
		
		public EvolvedReservoir(){
			
			int samples = 300;  
			//Create input samples
			this.analog = SignalGenerator.sine(300, samples, (1.0/2.0), (1.0/4.0), 0, (1.0/2.0));
			//Convert to spike trains
			this.hsa = new HSA(analog);
			hsa.optimize(10, .1, 0, 1, .2);
			this.dataset = hsa.encode();
			
		
				/*
				 * Lower number increase frequency..but not amplitude
				 */
				IFSpikingNeuron.MEMORY_CAPACITY_COEF = 3.2;
				/*
				 * Having a 0 refactory period causes an extremely slow system
				 */
				IFSpikingNeuron.REFACTORY_PEROID = 0;
				
			
				System.setProperty("org.bushe.swing.event.eventBusClass", "org.bushe.swing.event.DiscreteTimeThreadSafeEventService");
				try {
					EventServiceLocator.setEventService("DiscreteTimeThreadSafeEventService", new DiscreteTimeThreadSafeEventService());
				} catch (EventServiceExistsException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		
		}
		private void createNetwork(int size, double density){
			
			try {
				//Create reservoir
				int inputs = 1;
				int outputs = 1;
				boolean useBias = false;
				RandomSparseTopology topology = new RandomSparseTopology(size,density);
				this.network = new ReservoirNetwork<IFSpikingNeuron>(inputs, size, useBias, topology, IFSpikingNeuron.class);
				this.out = new SpikingOutput<IFSpikingNeuron>(network.getReservior(), outputs, IFSpikingNeuron.class);
			} catch (ReserviorException e) {
				e.printStackTrace();
			}
		}
		private final MultivariateFunction fitnessFuntion = new MultivariateFunction(){

			@Override
			public double value(double[] coeff) {
				
				IFSpikingNeuron.MEMORY_CAPACITY_COEF = coeff[0];
				/*
				 * Having a 0 refactory period causes an extremely slow system
				 */
				IFSpikingNeuron.REFACTORY_PEROID = (long)Math.floor(coeff[1]);
				
				double alpha = coeff[2];
				
				
				createNetwork((int)Math.floor(coeff[3]), coeff[4]);
				
				double [] dataset = EvolvedReservoir.this.dataset;
			
				//Stimulate the output node. Teacher forcing
				String PUBLISHER_ID = "U";
				out.subscribe(PUBLISHER_ID);
				for (int i=0; i<dataset.length; i++){
					SpikeEvent ev = new SpikeEvent(dataset[i]*SpikeEvent.DEFAULT_SPIKE_HEIGHT, SpikeEvent.DEFAULT_PULSE_WIDTH);
					ev.setTime(i);
					EventBus.publish(PUBLISHER_ID,  ev);
					//logger.info(i);
					ReservoirNetwork.incClock();
				}
				
				//Retrieve the states after stimulating
				DoubleFactory2D factory = DoubleFactory2D.dense;
				DoubleMatrix2D Y_state = factory.make(new double [][] {dataset}).viewDice();//.viewSelection(MLArrayUtils.viewableRows(100, samples), null);
				DoubleMatrix2D X_state = network.getReservior().getInternalFiringTimes().viewDice();//.viewSelection(MLArrayUtils.viewableRows(100, samples), null);
		
				//Compute the weights
				DoubleMatrix2D W = MLMatrixUtils.ridge(X_state, Y_state,alpha);
				//Take these weights and now set connections, first weight index goes to first created neuron
				out.setWeightsByType(IFSpikingNeuron.ID_PREFIX, W.viewDice().viewRow(0).toArray());
				network.getReservior().reset();
				out.reset();
					
				//Run the reservoir
				try {
					network.input(new double [][]{dataset});
				} catch (ReserviorException e) {
					e.printStackTrace();
				}
				double [] spikeOutput = out.getState().viewRow(0).toArray();
				double [] analogOutput = 	hsa.decode(spikeOutput);
				network.getReservior().reset();
				out.reset();
				//Calculate the error
				ErrorCalculation errorCalc = new ErrorCalculation();
				for (int i=0; i<analog.length; i++){
					errorCalc.updateError(analogOutput[i], analog[i]);
				}
				double error = errorCalc.calculateMSE();
				
network.shutdown();
				epoch++;
				if (epoch%100 == 0){
					logger.info(epoch + "\t" + error + "\t*" + minError);
				}
				if (error < minError){
					minError = error;
					bestCoeff = coeff;
				}
				return error;
			}
			
		};

		/**
		 * Optimize the convolution filter
		 */
		public void optimize(int size){
			this.epoch = 0;
			
			//Number of weights to evolve
			final double[] start = new double []{3, 0, 1, 20, .1};
			final double[] lower = new double []{1, 0, 0, 15, .08};
			final double[] upper = new double []{5, 5, 3, 120, .3};
			
			//Set to 1/3 the initial search volume
			final double[] sigma = new double [size];
			Arrays.fill(sigma, insigma);
			
			int lambda =  4 + (int)(3.*Math.log(size));
			int maxEvals = 100000;//CMAESOptimizer.DEFAULT_MAXITERATIONS;
			double stopValue = .01;
			boolean isActive = true; //Chooses the covariance matrix update method.
			int diagonalOnly = 0;
			int checkFeasable = 0;
						
			final CMAESOptimizer optimizer = new CMAESOptimizer( lambda, sigma, 100000,//CMAESOptimizer.DEFAULT_MAXITERATIONS,
	                stopValue, isActive, diagonalOnly,
	                checkFeasable,  new MersenneTwister(), false);
			
			
			final PointValuePair result = optimizer.optimize(maxEvals, fitnessFuntion, GoalType.MINIMIZE, start, lower, upper);
			logger.info(Arrays.toString(result.getPoint()));

		}
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		EvolvedReservoir evo = new EvolvedReservoir();
		evo.optimize(5);
	}

}
