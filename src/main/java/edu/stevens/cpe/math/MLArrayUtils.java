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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MLArrayUtils {
	
	/**
	 * Print out the spike trains so it can be analyzed by plotting software
	 * @param file
	 * @param array
	 */
	public static void print(File file , double [] array){
		FileWriter fstream = null;
		BufferedWriter out = null;
		
		try {
			fstream = new FileWriter(file);
			out = new BufferedWriter(fstream);
			for (int i=0; i<array.length; i++){
				out.write(array[i] + "\t");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {

			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fstream != null) {
				try {
					fstream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}
	}
	/**
	 * 
	 * @param start Inclusive
	 * @param finish Exclusive
	 * @return
	 */
	public static int [] viewableRows(int start, int finish){
		
		//Retrieve the states after stimulating
		int [] viewable = new int [finish-start];
		int k=0;
		for (int i=start; i<finish; i++){
			viewable[k++] =i;
		}
		return viewable;
	}
	/**
	 * Print out the spike trains so it can be analyzed by plotting software
	 * @param file
	 * @param array
	 */
	public static void printArray(File file , double [] x, double [] y){
		FileWriter fstream = null;
		BufferedWriter out = null;
		
		try {
			fstream = new FileWriter(file);
			out = new BufferedWriter(fstream);
			for (int i=0; i<x.length; i++){
				out.write(x[i] + "\t");
			}
			out.write("\r\n");
			for (int i=0; i<y.length; i++){
				out.write(y[i] + "\t");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {

			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fstream != null) {
				try {
					fstream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}
	}
	public static double [] pad(double [] array, int size, boolean inFront){
		double [] padded = new double [array.length + size -1];
		
		for (int i=0; i<array.length + size -1; i++){
			if (i < size-1){
				padded[i] = 0;
			} else {
				padded[i] = array[i-(size-1)];
			}
		}
		
		return padded;
	}
	
}
