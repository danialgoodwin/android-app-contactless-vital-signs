//http://code.google.com/p/jstk/source/browse/trunk/jstk/src/de/fau/cs/jstk/sampled/filters/IIRFilter.java?r=176

package net.simplyadvanced.vitalsigns;

public class AudioFilter {
	static double []a = new double [3];
	static double []b = new double [3];
	static double []x = new double [3];
	static double []y = new double [3];
	
	public static double[] calculate(double[] source, int count, int samplerate) {
	   
		//**************apply low pass filter ***********************
	   iirFilter(samplerate, 1500, true); //true for low pass filter
	   for (int i=0;i<count;i++) {
	       y[2] = y[1]; y[1] = y[0];
	       y[0] = source[i];
	       x[2] = x[1]; x[1] = x[0];

	       x[0] =   (a[0] * y[0] + a[1] * y[1] + a[2] * y[2]
	                    - b[1] * x[0]
	                    - b[2] * x[1]);

	       source[i] = x[0];
	   }
	   //*******************apply high pass filter******************
	   iirFilter(samplerate, 1000, false); //true for low pass filter
	   for (int i=0;i<count;i++)
	   {
	       y[2] = y[1]; y[1] = y[0];
	       y[0] = source[i];
	       x[2] = x[1]; x[1] = x[0];

	       x[0] =   (a[0] * y[0] + a[1] * y[1] + a[2] * y[2]
	                    - b[1] * x[0]
	                    - b[2] * x[1]);

	       source[i] = x[0];
	   }
	   
	   return source;
	}
	
	public static void iirFilter(int samplerate, double freq, boolean lowp) {
        double ff = 2. * freq / samplerate;
       
        double scale = computeScale(2, ff, lowp);
       
        b = computeB(2, lowp);
        for (int i = 0; i < b.length; ++i)
                b[i] *= scale;
       
        a = computeA(2, ff);
           
	}
	
	

	private static double computeScale(int n, double f, boolean lowp) {
        double omega = Math.PI * f;
	    double fomega = Math.sin(omega);
	    double parg0 = Math.PI / (double)(2*n);
	   
	    double sf = 1.;
	    for (int k = 0; k < n/2; ++k )
	        sf *= 1.0 + fomega * Math.sin((double)(2*k+1)*parg0);
	
	    fomega = Math.sin(omega / 2.0);
	
	    if (n % 2 == 1)
	        sf *= fomega + (lowp ? Math.cos(omega / 2.0) : Math.sin(omega / 2.));
	    sf = Math.pow( fomega, n ) / sf;
	
	    return sf;
	}

	private static double [] computeB(int n, boolean lowp) {
        double [] ccof = new double [n + 1];
       
        ccof[0] = 1;
        ccof[1] = n;
       
        for (int i = 2; i < n/2 + 1; ++i) {
                ccof[i] = (n - i + 1) * ccof[i - 1] / i;
                ccof[n - i] = ccof[i];
        }
       
        ccof[n - 1] = n;
        ccof[n] = 1;

        if (!lowp) {
                for (int i = 1; i < n + 1; i += 2)
                        ccof[i] = -ccof[i];
        }
       
        return ccof;

	}

	private static double [] computeA(int n, double f) {
        double parg;    // pole angle
        double sparg;   // sine of the pole angle
        double cparg;   // cosine of the pole angle
        double a;               // workspace variable
        double [] rcof = new double [2 * n]; // binomial coefficients

        double theta = Math.PI * f;
        double st = Math.sin(theta);
        double ct = Math.cos(theta);

        for (int k = 0; k < n; ++k) {
                parg = Math.PI * (double) (2*k + 1) / (double) (2*n);
                sparg = Math.sin(parg);
                cparg = Math.cos(parg);
                a = 1. + st * sparg;
                rcof[2 * k] = -ct / a;
                rcof[2 * k + 1] = -st * cparg / a;
        }

        // compute the binomial
        double [] temp = binomialMult(rcof);
       
        // we only need the n+1 coefficients
        double [] dcof = new double [n + 1];
        dcof[0] = 1.0;
        dcof[1] = temp[0];
        dcof[2] = temp[2];
        for (int k = 3; k < n + 1; ++k)
                dcof[k] = temp[2*k - 2];

        return dcof;
	}

	private static double [] binomialMult(double [] p) {
        int n = p.length / 2;
        double [] a = new double [2 * n];

        for (int i = 0; i < n; ++i) {
                for (int j = i; j > 0; --j) {
                        a[2 * j] += p[2 * i] * a[2 * (j - 1)] - p[2 * i + 1]
                                        * a[2 * (j - 1) + 1];
                        a[2 * j + 1] += p[2 * i] * a[2 * (j - 1) + 1] + p[2 * i + 1]
                                        * a[2 * (j - 1)];
                }

                a[0] += p[2 * i];
                a[1] += p[2 * i + 1];
        }

        return a;
	}
	
}
