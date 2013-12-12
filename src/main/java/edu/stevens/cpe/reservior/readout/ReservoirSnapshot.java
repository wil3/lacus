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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.optimization.direct.CMAESOptimizer;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.log4j.Logger;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;
import org.encog.util.arrayutil.NormalizationAction;
import org.encog.util.arrayutil.NormalizedField;

import cern.colt.function.IntIntDoubleFunction;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.math.Functions;

import edu.stevens.cpe.math.MLMatrixUtils;
import edu.stevens.cpe.reservior.ReserviorException;
import edu.stevens.cpe.reservior.ReservoirNetwork;
import edu.stevens.cpe.reservior.SpikeEvent;
import edu.stevens.cpe.reservior.layers.SpikingOutput;
import edu.stevens.cpe.reservior.neuron.IFSpikingNeuron;

public class ReservoirSnapshot {
	public static Logger logger = Logger.getLogger(ReservoirSnapshot.class);

	private ReservoirNetwork net;
	private MLDataSet dataset;
	private double alpha;
	
	public DoubleMatrix2D N_STATES;
	public DoubleMatrix2D Y;
	
	private static double spikeCols = 63;
	private static final double V_SPIKE = 1 / IFSpikingNeuron.MEMORY_CAPACITY_COEF;

	
	public ReservoirSnapshot(ReservoirNetwork reservoir, MLDataSet trainingSet,double alpha){
		this.net = reservoir;
		this.dataset = trainingSet;
		this.alpha = alpha;
				
		this.N_STATES = accumVoltages();
		logger.info("STATES: " + N_STATES);
		this.Y = target();
	}
	public ReservoirSnapshot(){};
	
	public void update(){
		this.N_STATES = accumVoltages();
		this.Y = target();
		logger.info("STATES: " + N_STATES);

	}
	
	
	public DoubleMatrix2D ridge(){
	//	DoubleMatrix2D H = accumVoltages();
	//	DoubleMatrix2D Y = target();
		
		DoubleFactory2D factory = DoubleFactory2D.dense;
		DoubleMatrix2D scaledIdentity = factory.identity((int)dataset.getRecordCount()).assign(Functions.mult(alpha));
		
		
		Algebra alg = new Algebra();
		
		DoubleMatrix2D inverse = alg.inverse(N_STATES.zMult(N_STATES.viewDice(), null).assign(scaledIdentity, Functions.plus));
		
		DoubleMatrix2D X = N_STATES.viewDice().zMult(inverse, null);
		
		DoubleMatrix2D weights = Y.viewDice().zMult(X.viewDice(), null);
				
		return weights;		
	}
	
	
	/**
	 * @return the dataset
	 */
	public MLDataSet getDataset() {
		return dataset;
	}
	private void test(DoubleMatrix2D V , DoubleMatrix2D W, DoubleMatrix2D Y){
		for (int i=0; i<V.rows(); i++){
			double out = V.viewRow(i).zDotProduct(W.viewDice().viewRow(0));
			logger.info("");
		}
	}
	
	private static final IntIntDoubleFunction forEachNonZeroDenormalize = new IntIntDoubleFunction(){
		@Override
		public double apply(int x, int y, double value) {
			NormalizedField norm = new NormalizedField(NormalizationAction.Normalize, 
					null,IFSpikingNeuron.THRESHOLD,0,1,0);
			
			return norm.deNormalize(value);
		}
	};
	private static final IntIntDoubleFunction forEachNonZeroNormalize = new IntIntDoubleFunction(){
		@Override
		public double apply(int x, int y, double value) {
			NormalizedField norm = new NormalizedField(NormalizationAction.Normalize, 
					null,V_SPIKE*spikeCols,0,1,0); // make voltage a neuron can supply, 64 time steps * spike
			
			return norm.normalize(value);
		}
	};
	
	/**
	 * Put the target values in a matrix
	 * @return
	 */
	private DoubleMatrix2D target(){
		DoubleFactory2D factory = DoubleFactory2D.dense;
		DoubleMatrix2D allTargets  = null;
		for(MLDataPair pair: dataset ) {
			DoubleMatrix2D record = new DenseDoubleMatrix2D(new double [][] {pair.getIdealArray()});
			if (allTargets == null){
				allTargets = new DenseDoubleMatrix2D(record.toArray());
			} else {
				allTargets = factory.appendRows(allTargets, record);
			}
		}
		
		//denormalize all records 
		//allTargets.forEachNonZero(forEachNonZeroDenormalize);
		
		return allTargets;
	}
	/**
	 * Create matrix taking internal snapshots at each training period
	 * @return
	 */
	private DoubleMatrix2D accumVoltages(){
		DoubleFactory2D factory = DoubleFactory2D.dense;
		DoubleMatrix2D allSpikes = null;
		//DoubleMatrix2D allSpikes 
		for(MLDataPair pair: dataset ) {
			try {
					
				net.input(pair.getInput());
				
				DoubleMatrix2D A= getSpikeTimesForAllNeurons();
				if (allSpikes == null){
					allSpikes = new DenseDoubleMatrix2D(A.toArray());
				} else {
					allSpikes = factory.appendRows(allSpikes, A);
				}
			} catch (ReserviorException e) {
				//Because we are in an implemented class kill this way for now.
				System.exit(0);
			}
			//After each time the reservoir sees a training entry reset the reservoir
			net.getReservior().reset();

		}
		DoubleMatrix2D sums = allSpikes.zMult(factory.make(allSpikes.columns(), 1).assign(1), null);
		
		
		sums.assign(Functions.mult(V_SPIKE));
		
		int N = net.getReservior().getNeuronCount();
		DoubleMatrix2D trainingStates = null;
		int [] rows = new int [N];
		
		for (int i=0; i<sums.rows(); i+=N){
			int rowIndex = 0;
			for (int j=i; j<i+N; j++, rowIndex++){
				rows[rowIndex] = j;
			}
			DoubleMatrix2D neurons = sums.viewSelection(rows, null).viewDice();
			if (trainingStates==null){
				trainingStates = new DenseDoubleMatrix2D(neurons.toArray());
			} else {
				trainingStates =	factory.appendRows(trainingStates, neurons);
			}
		}
		
		trainingStates.forEachNonZero(forEachNonZeroNormalize);
		return trainingStates;
	}
	/**
	 * At the current reservoir state get all of the spike times
	 * @return
	 */
	private DoubleMatrix2D getSpikeTimesForAllNeurons(){
		DoubleFactory2D factory = DoubleFactory2D.dense;
		IFSpikingNeuron n1 = (IFSpikingNeuron)net.getReservior().getNeurons()[0];
		DoubleMatrix2D allSpikes = MLMatrixUtils.convertArrayTo2DMatrix( n1.getFiringTimes());
		
		for (int i=1; i<net.getReservior().getNeurons().length; i++){
			IFSpikingNeuron n = (IFSpikingNeuron)net.getReservior().getNeurons()[i];
			allSpikes = factory.appendRows(allSpikes, MLMatrixUtils.convertArrayTo2DMatrix(n.getFiringTimes()));
		}
		return allSpikes;
	}
	

	
	
	/**
	 * @param dataset the dataset to set
	 */
	public void setDataset(MLDataSet dataset) {
		this.dataset = dataset;
	}


	
	public static void main(String [] args){
		ReservoirSnapshot r = new ReservoirSnapshot();
		r.N_STATES = new DenseDoubleMatrix2D(new double [][] {});
		r.Y = new DenseDoubleMatrix2D(new double [][] {});
		r.ridge();
	}
}
