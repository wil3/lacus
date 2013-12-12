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

import org.apache.commons.math3.linear.Array2DRowFieldMatrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;

import cern.colt.matrix.DoubleMatrix2D;

import edu.stevens.cpe.reservior.ReserviorException;
import edu.stevens.cpe.reservior.Reservoir;
import edu.stevens.cpe.reservior.ReservoirNetwork;
import edu.stevens.cpe.reservior.layers.Input;
import edu.stevens.cpe.reservior.layers.ReadoutFunction;

public class GradientDecent {
	private ReservoirNetwork reservoir;
	private MLDataSet trainingSet;
	private  double learningRate;
	
	DoubleMatrix2D X;
	DoubleMatrix2D Y;
	
	/**
	 * The number of weights = outputs * reservoir nodes
	 */
	private double [] deltaWeights;
	public GradientDecent(ReservoirNetwork reservoir, MLDataSet trainingSet, double learningRate){
		this.reservoir = reservoir;
		this.trainingSet = trainingSet;
		this.learningRate = learningRate;
		//this.deltaWeights = new double [reservoir.getReservior().getNeuronCount() * reservoir.getReadout().getNumberOutputs()];
		Arrays.fill(deltaWeights, 0.0);
	}
	
	public GradientDecent(DoubleMatrix2D X, DoubleMatrix2D Y, double learningRate){
		this.learningRate = learningRate;
		this.X = X;
		this.Y = Y;
	}
	
	public void iteration(){
		for (int i=0; i<X.rows(); i++){
			/*
			try {
				
				//Output
				double output = X.viewRow(i).zDotProduct(W.viewDice().viewRow(0));
				double target = Y.get(i, 0);
				double error += ErrorUtility.computeLinearRegressionError(new double [] {target}, new double [] { output});

				input.scalarMultiply(learningRate*error);
				
			//	deltaWeights = deltaWeights + 
			} catch (ReserviorException e) {
				e.printStackTrace();
			}
			
			
				*/
			
		}
	}
	

	
}
