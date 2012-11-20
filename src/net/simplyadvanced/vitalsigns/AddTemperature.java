package net.simplyadvanced.vitalsigns;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class AddTemperature extends Activity {
	AddTemperature _activity;
    public static final String PREFS_NAME = "MyPrefsFile";
	EditText mEditTextSkinTemperature, mEditTextIndoorTemperature;
	TextView mTextViewSkinTemperature, mTextViewIndoorTemperature, mTextViewOutdoorTemperature;
	SharedPreferences settings;
	int outdoorTemperature = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	_activity = this;
        setContentView(R.layout.activity_add_temperature);

        mEditTextSkinTemperature = (EditText) findViewById(R.id.editTextSkinTemperature);
        mEditTextIndoorTemperature = (EditText) findViewById(R.id.editTextIndoorTemperature);
        mTextViewSkinTemperature = (TextView) findViewById(R.id.textViewSkinTemperature);
        mTextViewIndoorTemperature = (TextView) findViewById(R.id.textViewIndoorTemperature);
        mTextViewOutdoorTemperature = (TextView) findViewById(R.id.textViewOutdoorTemperature);

    	settings = getSharedPreferences(PREFS_NAME, 0);

        //mEditTextInputTemperature.setText("" + settings.getFloat("skinTemperature", 0));
        mEditTextSkinTemperature.setText( String.format("%.2f",settings.getFloat("skinTemperature", (float) 88.666)) );
        
        //mTextViewOutdoorTemperature.setText("Outdoor Temperature: " + getOutdoorTemperature("12772416")); // For Tampa
        getOutdoorTemperature("12772416"); // Eventually, add ability to insert location instead of woeid: http://code.google.com/p/yahooweatherandroid/source/browse/trunk/WeatherYahoo/src/com/exoplatform/weather/model/WeatherDataModel.java
    }
    
    public void getOutdoorTemperature(String woeid) { // TODO: Return a String eventually
    	if (!checkInternetConnection(_activity)) {
    		mTextViewOutdoorTemperature.setText("Outdoor Temperature: No Internet!");
    		return; // "No Internet!";
    	} else {
    		URL url;
			try {
				url = new URL("http://weather.yahooapis.com/forecastrss?w=" + woeid);
	    		new DownloadUrlStream().execute(url);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
    	}
    }

    public void Save(View v) {
    	SharedPreferences.Editor editor = settings.edit(); // Needed to make changes
    	editor.putFloat("skinTemperature", Float.parseFloat(mEditTextSkinTemperature.getText().toString()));
    	editor.putInt("outdoorTemperature", outdoorTemperature);
    	editor.commit(); // This line saves the edits

    	float ambientTemp;
    	if (mEditTextIndoorTemperature.getText().toString().equals("")) {
    		ambientTemp = outdoorTemperature;
    	} else {
    		ambientTemp = Float.parseFloat(mEditTextIndoorTemperature.getText().toString()); // TODO: Actually calculate indoor temperature on smartphones that have that ability 
    	}
    	
    	float measurement = settings.getFloat("skinTemperature", 88);
    	float modifiedTemp = (float) convertTemp((double)measurement, (double)ambientTemp);

    	editor.putFloat("internalTemperature", modifiedTemp);
    	editor.commit();

    	finish(); // Navigate back on stack
	}
    public void Cancel(View v) {
    	finish(); // Navigate back on stack
	}

	public double convertTemp(double measuredSkinTemperature, double atmosphericTemperature) {
		double factor = 3; //approximate constant factor regardless of C/F
		double itemp = (factor*measuredSkinTemperature - atmosphericTemperature)/(factor-1);
		return itemp;
	}
	
    public static boolean checkInternetConnection(Activity _activity) {
        ConnectivityManager conMgr = (ConnectivityManager) _activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (conMgr.getActiveNetworkInfo() != null
                && conMgr.getActiveNetworkInfo().isAvailable()
                && conMgr.getActiveNetworkInfo().isConnected()) 
            return true;
        else
            return false;
    }
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_add_temperature, menu);
        return true;
    }

    private class DownloadUrlStream extends AsyncTask<URL, Integer, String> { // Getting outdoorTemperature in a different thread than the main thread // Needed for APIs 3.0+(?)
        protected String doInBackground(URL... urls) { // Do the long-running work in here
        	try {
	            SAXParserFactory factory = SAXParserFactory.newInstance(); // create the factory
	            SAXParser parser = factory.newSAXParser();                 // create a parser
	            XMLReader xmlreader = parser.getXMLReader();               // create the reader (scanner)
	            RSSHandler theRssHandler = new RSSHandler();               // instantiate our handler
	            xmlreader.setContentHandler(theRssHandler);                // assign our handler
	            InputSource is = new InputSource(urls[0].openStream());    // get our data via the url class
	            xmlreader.parse(is);                                       // perform the synchronous parse
	            return theRssHandler.getFeed(); // get the results - should be a fully populated RSSFeed instance, or null on error
	        } catch (Exception e) {
	    		return "Not available";
	    	}
        }
    
//        protected void onProgressUpdate(Integer... progress) { // This is called each time you call publishProgress()
//            setProgressPercent(progress[0]);
//        }

        protected void onPostExecute(String result) { // This is called when doInBackground() is finished
            //showNotification("Downloaded " + result + " bytes");
        	outdoorTemperature = Integer.parseInt(result);
        	mTextViewOutdoorTemperature.setText("Outdoor Temperature: " + outdoorTemperature);
        }
    }


}
