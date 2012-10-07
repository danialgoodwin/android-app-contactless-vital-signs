package net.simplyadvanced.vitalsigns.heartrate;

public class FFT {
	static int n;
	static int m;
	static double [] cos;
	static double [] sin;
	static double [] window;
	public static void fft(double [] x, int size, double frequency) {
		int i,j,k,n1,n2,a;
		int Hz = 0;
		double c,s,e,t1,t2;
		double [] result = new double [size];
		
		for (int z = 0; z<size; z++) {
			result[z] = x[z];
		}
		
		checker(size);
		//Bit-reverse
		j = 0;
		n2 = n/2;
		for ( i =1;i<n-1;i++) {
			n1=n2;
			while(j>=n1) {
				j = j-n1;
				n1 =n1/2;
			}
			j = j+n1;
			if (i < j) {
				t1 = result[i];
				result[i] = result[j];
				result[j] = t1;
			}
		}
		
		//FFT
		n1 = 0;
		n2 = 1;
		
		for(i = 0; i< m; i++) {
			n1 = n2;
			n2 = n2 + n2;
			a = 0;
			for ( j = 0; j < n1; j++) {
				c = cos[a];
				s = sin[a];
				a += 1 << (m-i-1);
				for (k =j; k<n; k = k+n2) {
					t1 = c*result[k+n1];
					t2 = s*result[k+n1];
					result[k+n1] = result[k] -t1;
					result[k]= result[k] + t1;
					result[k] = Math.abs(result[k]);
				}
			}
		}
		
		double temp = 0;
        for(int z=0; z<32; z++) {
        	if(temp < result[z]) {
        		temp = result[z];
        		Hz = z;
        	}
        }
        frequency = result[Hz];
	}

	public static void checker(int size) {
		n = size;
		m = (int)(Math.log(n)/Math.log(2));
		cos = new double [n/2];
		sin = new double [n/2];
		window = new double [n];
		
		for(int i = 0; i<n/2; i++) {
			cos[i] = Math.cos(-2*Math.PI*i/n);
			sin[i] = Math.sin(-2*Math.PI*i/n);
		}
		for (int i = 0; i <window.length; i++){
			window[i] = .42-0.5*Math.cos(2*Math.PI*i/(n-1)) 
					+ .08 *Math.cos(4*Math.PI*i/(n-1));
		}
	}
}
