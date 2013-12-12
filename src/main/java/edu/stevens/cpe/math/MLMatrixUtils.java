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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

import com.google.common.base.Function;

import cern.colt.function.DoubleFunction;
import cern.colt.function.IntIntDoubleFunction;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.matrix.linalg.EigenvalueDecomposition;
import cern.jet.math.Functions;

public class MLMatrixUtils {
	 private static Random random = new Random(System.nanoTime());

	public static String prettyPrint(DoubleMatrix2D matrix){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		
		double [][] arr = matrix.toArray();
		DecimalFormat df = new DecimalFormat("##.###");
		for (int i=0; i<arr.length; i++){
			for (int j=0; j<arr[i].length; j++){
				ps.printf(df.format(arr[i][j])+"\t");
			}
			ps.print("\r\n");
		}
		return baos.toString();
	}
	private static final DoubleFunction noise = new DoubleFunction(){

		@Override
		public double apply(double argument) {
			double noise = (random.nextDouble() > .5) ? 0.0001 : -0.0001;
			return argument + noise;
		}
	};
	public static void addNoise(DoubleMatrix2D matrix){
		matrix.assign(noise);
	}
	private static final DoubleFunction customRandomizer = new DoubleFunction(){
		
		@Override
		public double apply(double argument) {
			// TODO Auto-generated method stub
			 Random random = new Random(System.nanoTime());

			return random.nextDouble();
		}
	};

	public static DoubleMatrix2D sprand(int m, int n, double density){
		int nnzwanted = (int)Math.round(m * n * Math.min(density, 1));
		DoubleFactory2D factory = DoubleFactory2D.sparse;
		DoubleMatrix2D i = factory.random(nnzwanted, 1).assign(customRandomizer).assign(Functions.mult(m)).assign(Functions.floor);//.assign(Functions.plus(1));
		DoubleMatrix2D j = factory.random(nnzwanted, 1).assign(customRandomizer).assign(Functions.mult(n)).assign(Functions.floor);//.assign(Functions.plus(1));

		DoubleMatrix2D rows=null;
		DoubleMatrix2D cols=null;
		//Combine i j
		DoubleMatrix2D ij = unique(factory.appendColumns(i, j));
		if (ij.cardinality() != 0){
			//separate into i and j again
			rows = ij.viewSelection(null, new int[]{0});
			cols= ij.viewSelection(null, new int[]{1});
		}
		
		
		DoubleMatrix2D rands = factory.random(rows.rows(), 1);
		
		double [][] R = new double [m][n];
		for (int x=0; x<rows.rows(); x++ ){
		//	for (int y=0; y<n; y++){
				R[(int)rows.get(x, 0)][(int)cols.get(x, 0)] = rands.get(x, 0);
		//	}
		}
		
		DoubleMatrix2D y = new SparseDoubleMatrix2D(R);
		return y;
	}
	
	/**
	 * Matlab unique(A,'rows')
	 * 
	 * Get rid of duplicate rows
	 */
	public static DoubleMatrix2D unique(DoubleMatrix2D A){
		A = A.viewSorted(0);

		//Keep track of row indices that are duplicates
		final ArrayList<Integer> dups = new ArrayList<Integer>();

		for (int x=0; x<A.rows(); x++){
			if (dups.contains(x)){
				continue;
			}
			for (int y=x+1;y<A.rows(); y++ ){
				if (!dups.contains(y)){
					if (A.viewRow(x).equals(A.viewRow(y))){
						dups.add(y);
					}
				}
			}
		}
		
		//Get unique values in row, return in sorted order
		final ArrayList<Integer> alKeepRows = new ArrayList<Integer>();
		for (int i = 0; i< A.rows(); i++){
			if (!dups.contains(i)){
				alKeepRows.add(i);
			}
		}
		
		//Ugh conver to primative int array
		int [] keepRows = new int[alKeepRows.size()];
		for (int x=0; x<alKeepRows.size(); x++){
			keepRows[x] = alKeepRows.get(x);
		}
		
		return A.viewSelection(keepRows, null);
	}

	
	public static double getSpectralRadius(DoubleMatrix2D A){
		
		EigenvalueDecomposition eigen = new EigenvalueDecomposition(A);
		DoubleMatrix1D eigenValues = eigen.getRealEigenvalues();
		return eigenValues.assign(Functions.abs).viewSorted().get(eigenValues.size()-1);
	}
	
	
	public static DoubleMatrix2D convertArrayTo2DMatrix(long [] array){
		double [] preciseSpikes = new double [ array.length];
		for (int i=0; i<preciseSpikes.length; i++){
			preciseSpikes[i] = array[i];
		}	
		DoubleFactory2D factory = DoubleFactory2D.dense;
		return factory.make(new double [][]{preciseSpikes});
	}
	public static DoubleMatrix2D convertArrayTo2DMatrix(double [] array){
		double [] preciseSpikes = new double [ array.length];
		for (int i=0; i<preciseSpikes.length; i++){
			preciseSpikes[i] = array[i];
		}	
		DoubleFactory2D factory = DoubleFactory2D.dense;
		return factory.make(new double [][]{preciseSpikes});
	}
	/**
	 * 
	 * @param n => (n_max x N) Matrix where each row is state of reservoir for each time step
	 * @param y => (n_max x L) Matrix where each row is state of output for each time step
	 * @param alpha The idenity coefficient. This can be found using cross-validation
	 * @return
	 */
	public static DoubleMatrix2D ridge(DoubleMatrix2D n, DoubleMatrix2D y, double alpha){
		DoubleFactory2D factory = DoubleFactory2D.dense;

		//force same type, this is giving a cast error. Bug on their part?
		DoubleMatrix2D _n = factory.make(n.toArray());
		DoubleMatrix2D _y = factory.make(y.toArray());

		
		//aI
		DoubleMatrix2D scaledIdentity = factory.identity(_n.rows()).assign(Functions.mult(alpha));
		
		
		Algebra alg = new Algebra();
		// ((n x n^T) + aI)^-1
		// (n_max x N) x (N x n_max)
		DoubleMatrix2D inverse = alg.inverse(n.zMult(_n.viewDice(), null).assign(scaledIdentity, Functions.plus));
		//System.out.println("-1=" + inverse);

		//X = ((n x n^T) + aI)^-1 x n
		// (n_max x n_max) x 
		DoubleMatrix2D X = inverse.zMult(_n, null);
		//System.out.println("X=" + X);
		//((n x n^T) + aI)^-1 x n x y
		DoubleMatrix2D weights = X.viewDice().zMult(_y, null);
		//X.viewDice()
		return weights;	
	}
	
	/**
	 * (n^-1 x y)'
	 */
	public static DoubleMatrix2D WienerHopf1(DoubleMatrix2D n, DoubleMatrix2D y) {
		//N = number of reservoir units
		//K = number of inputs
		//S = n_max x (N + K)
		//output matrix D = n_max x L = 200 x 1
		
		Algebra alg = new Algebra();
		//DoubleMatrix2D W = alg.inverse(X_state).zMult(Y_state.viewDice(), null);
		//System.out.println(W);
		
		DoubleMatrix2D S_inv = alg.inverse(n);
		
		DoubleFactory2D factory = DoubleFactory2D.dense;
		y = factory.make(y.toArray());
		
		//S^-1 = 10 x 200
		return S_inv.zMult(y, null);
	}
	/**
	 * ( (n'n)^-1(n'y) )
	 */
	public static DoubleMatrix2D WienerHopf2(DoubleMatrix2D n, DoubleMatrix2D y) {
		//P = S'D = (N x n_max) x (n_max x L) = (N x L)
		//R = S'S = (N x n_max) x (n_max x N) = (N x N)
		//W = (R^-1P)' = (R^-1)'(P)' = (N x N) x (N x L)
		return null;
	}
	
	public static DoubleMatrix2D psuedoInverse(DoubleMatrix2D n, DoubleMatrix2D y){
		Algebra alg = new Algebra();
		DoubleMatrix2D S_inv = alg.inverse(n.viewDice());
		return S_inv.zMult(y, null);
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		double [][] R = new double [] [] { {5, 6},{ 1, 2}};//, {1,2},  {3,4}, {5,6}};
		
		DoubleFactory2D sparse = DoubleFactory2D.sparse;
		DoubleMatrix2D R2 = sparse.make(R);
		double radius = getSpectralRadius(R2);
		System.out.println(radius);
		
		//DoubleFactory2D factory = DoubleFactory2D.dense;
		//DoubleMatrix2D R1 = factory.make(R);
		//System.out.println(R1);
		

		//DoubleMatrix2D m = sprand(10,10,.2);
		//System.out.println(unique(R1));
	}

}
