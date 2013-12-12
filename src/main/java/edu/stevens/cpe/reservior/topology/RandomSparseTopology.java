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
package edu.stevens.cpe.reservior.topology;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Random;

import org.apache.log4j.Logger;

import cern.colt.function.IntIntDoubleFunction;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.linalg.QRDecomposition;
import cern.jet.math.Functions;

import edu.stevens.cpe.math.MLMatrixUtils;
import edu.stevens.cpe.reservior.ReserviorException;
import edu.stevens.cpe.reservior.neuron.Neuron;

public class RandomSparseTopology  implements NetworkTopology {
	public static Logger logger = Logger.getLogger(RandomSparseTopology.class);
	private double density = 0;
	private int N;
	private DoubleMatrix2D weightMatrix;
	private double spectralRadius = 1;
	
	public RandomSparseTopology(int N, double denisty) throws ReserviorException{
		this.density = denisty;
		this.N = N;
		createWeightMatrix();
	}
	
	/**
	 * 
	 * @param N Number of neurons in reservoir
	 * @param denisty Density of the reservoir
	 * @throws ReserviorException 
	 */
	public RandomSparseTopology(int N, double denisty, double spectralRadius) throws ReserviorException{
		this.density = denisty;
		this.N = N;
		this.spectralRadius= spectralRadius;
		createWeightMatrix();
	}
	private static final IntIntDoubleFunction forEachNonZeroSubtract = new IntIntDoubleFunction(){
		@Override
		public double apply(int x, int y, double value) {
			return value - 0.5;
		}
	};
	/**
	 * Attempt to make a weight matrix with spectral radius
	 * @return
	 * @throws ReserviorException 
	 */
	private void createWeightMatrix() throws ReserviorException{
		double spectralR = Double.POSITIVE_INFINITY;
		DoubleMatrix2D sparse = null;
		int maxTries = 100;
		int iteration = 0;
		//while ((spectralR >= 1) && (iteration < maxTries)){
			sparse = MLMatrixUtils.sprand(N, N, density);
		
			//For all non-zero values subtract .5
			sparse = sparse.forEachNonZero(forEachNonZeroSubtract);
				
			sparse =sparse.assign(Functions.div(2));
			
			spectralR = MLMatrixUtils.getSpectralRadius(sparse);
			iteration++;
		//}
		
		//if (spectralR < 1){
			//Divide the matrix by the spectral radius

			this.weightMatrix=sparse.assign(Functions.div(spectralR));
		//} else {
		//	throw new ReserviorException("Could not form matrix with spectral radius less than 1");
		//}
		
		//weightMatrix.assign(Functions.abs);
		//This is where we scale the matrix by passed in spectral radius
		weightMatrix.assign(Functions.mult(spectralRadius));
		QRDecomposition qrd = new QRDecomposition(weightMatrix);
		logger.info("Full rank? " + qrd.hasFullRank());
		logger.info("Matrix created with spectral radius = " + spectralR);
		 
	}
	
	/**
	 * Mapping of the neuron indexs and weight which the target neurons
	 */
	@Override public HashMap<Integer,Double> getConnections(int sourceNeuronIndex)
			throws ReserviorException {
		HashMap<Integer,Double> connections = new HashMap<Integer,Double>();
		for (int i=0; i<N; i++){
			double weight = weightMatrix.get(sourceNeuronIndex, i);
			if (weight != 0){
				connections.put(i, weight);
			}
		}
		
		return connections;
	}
	/**
	 * @return the weightMatrix
	 */
	public DoubleMatrix2D getWeightMatrix() {
		return weightMatrix;
	}

	/**
	 * Print the weight matrix of the reseroir
	 */
	@Override public String toString(){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		
		
		//logger.info(baos.toString());
		return "";
	}

	/**
	 * @param weightMatrix the weightMatrix to set
	 */
	public void setWeightMatrix(DoubleMatrix2D weightMatrix) {
		this.weightMatrix = weightMatrix;
	}
}
