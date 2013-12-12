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

import org.encog.engine.network.activation.ActivationLinear;
import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.ml.fitting.linear.LinearRegression;
import org.encog.ml.fitting.linear.TrainLinearRegression;
import org.encog.ml.train.MLTrain;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.neural.networks.training.simple.TrainAdaline;
import org.encog.neural.pattern.ADALINEPattern;

import cern.colt.matrix.DoubleMatrix2D;

public class Perceptron {
	private final BasicNetwork network;
	private int numberOutputs;
	private int numberInputs;
	
	public Perceptron(int in, int out){
		this.numberOutputs = out;
		this.numberInputs = in;
		//this.network = new BasicNetwork();
		//In
		/*network.addLayer(new BasicLayer(null,false,in));
		network.addLayer(new BasicLayer(new ActivationLinear(),false,out));
		network.getStructure().finalizeStructure();
		network.reset();
		*/
		ADALINEPattern pattern = new ADALINEPattern();
		pattern.setInputNeurons(in);
		pattern.setOutputNeurons(out);
		this.network = (BasicNetwork)pattern.generate();
		
		
	
	}
	
	public void train( DoubleMatrix2D X, DoubleMatrix2D Y){

		
		MLDataSet training= new BasicMLDataSet(X.toArray(), Y.toArray());

		
		// train the neural network
	//	final Backpropagation train = new Backpropagation(network, trainingSet);
		MLTrain train = new TrainAdaline(network,training,0.01);

		int epoch = 1;

		do {
			train.iteration();
			//System.out.println("Epoch #" + epoch + " Error:" + train.getError());
			epoch++;
		//} while(train.getError() > .1);
		} while (epoch < 200000);
	
	}
	
	public double [] compute(double []  input){
		double [] out = new double [numberOutputs];
		network.compute(input,out);
		return out;
	}
	
}
