package net.simplyadvanced.vitalsigns.heartrate;

import net.simplyadvanced.vitalsigns.heartrate.FastICA;

public class FastICA_RGB {
	public static void preICA(double [] inred, double [] ingreen, double [] inblue, 
			int frame, double [] outred, double [] outgreen, double [] outblue) {
		
		double [][] input = new double [3][frame];
		double [][] output = new double [3][frame];
		int iteration=1000;
		int nocomponent = 3;
		double epsilon = .001;
		
		for ( int x = 0; x<frame; x++) {
			input[0][x]=inred[x];
			input[1][x]=ingreen[x];
			input[2][x]=inblue[x];
		}
		
		output = FastICA.fastICA(input, iteration, epsilon, nocomponent);
		
		for (int y = 0; y < frame; y++) {
			outred[y]=output[0][y];
			outgreen[y]=output[1][y];
			outblue[y]=output[2][y];
		}
	}
}
