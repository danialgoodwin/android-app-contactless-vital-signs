package net.simplyadvanced.vitalsigns.heartrate;
import android.util.Log;
import net.simplyadvanced.vitalsigns.heartrate.DoubleFFT_1D;

public class fft {
	
	public static double FFT( double [] in, int size, double samplingFrequency/*, double[] output*/) {
		double temp = 0;
		double maxPosition = 0;
		double frequency=0;
		double[] output = new double[2*size];
		
		for(int i=0;i<output.length;i++)
			output[i] = 0;
		
		for(int x=0;x<size;x++){
			output[x]=in[x];
		}
		DoubleFFT_1D fft = new DoubleFFT_1D(size);
		fft.realForward(output);
		for(int x=0;x<2*size;x++){
			output[x]=Math.abs(output[x]);
        	Log.d("DEBUG Freq", "DEBUG - outputFFT[" + x + "]: " + output[x]);
		}
		
		for(int z=12; z<size; z++) { // TODO change 12 to a function 30-180 bpm
        	if(temp < output[z]) {
        		temp = output[z];
        		maxPosition = z;
	        	Log.d("DEBUG Freq", "DEBUG - maxPosition: " + maxPosition);
        	}
        }
		
        frequency = maxPosition*samplingFrequency/(2*size);
        //System.out.println(maxPosition+" "+samplingFrequency+" "+frequency);
        return frequency;
	}
}
