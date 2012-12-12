package net.simplyadvanced.vitalsigns;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class AddTemperature extends Activity {
	AddTemperature _activity;
    public static final String PREFS_NAME = "MyPrefsFile";
	EditText mEditTextSkinTemperature, mEditTextIndoorTemperature;
	TextView mTextViewSkinTemperature, mTextViewIndoorTemperature, mTextViewOutdoorTemperature;
	SharedPreferences settings;
	int outdoorTemperature = 75;

	boolean displayEnglishUnits = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	_activity = this;
        setContentView(R.layout.activity_add_temperature);

    	settings = getSharedPreferences(PREFS_NAME, 0);

        mEditTextSkinTemperature = (EditText) findViewById(R.id.editTextSkinTemperature);
        mEditTextIndoorTemperature = (EditText) findViewById(R.id.editTextIndoorTemperature);
        mTextViewSkinTemperature = (TextView) findViewById(R.id.textViewSkinTemperature);
        mTextViewIndoorTemperature = (TextView) findViewById(R.id.textViewIndoorTemperature);
        mTextViewOutdoorTemperature = (TextView) findViewById(R.id.textViewOutdoorTemperature);
		displayEnglishUnits = settings.getBoolean("displayEnglishUnits", true); // TODO: Use: (displayEnglishUnits==true)?" F":" C"


        //mEditTextInputTemperature.setText("" + settings.getFloat("skinTemperature", 0));
        mEditTextSkinTemperature.setText( String.format("%.2f",settings.getFloat("skinTemperature", (float) 88.666)) );
       
        // DEBUG
    	int lat = settings.getInt("currentLatitude", 0/*28058987*/); // Gets coordinates that were saved from TestBloodPressure.java
    	int lon = settings.getInt("currentLongitude", 0/*-82415560*/); // Tampa coordinates
    	Toast.makeText(_activity, "lat,long = " + lat + "," + lon, Toast.LENGTH_LONG).show(); // DEBUG
        
        getOutdoorTemperature();
    }
    
    public void getOutdoorTemperature() {
    	if (!checkInternetConnection(_activity)) {
    		mTextViewOutdoorTemperature.setText("Outdoor Temperature: No Internet!");
    		return;
    	} else {
	    	new DownloadUrlStream().execute();
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

    private class DownloadUrlStream extends AsyncTask<Void, Integer, String> { // Gets outdoorTemperature in a different thread than the main thread // Needed for APIs 3.0+(?)
        protected String doInBackground(Void... params) { // Do the long-running work in here
        	int lat = settings.getInt("currentLatitude", 0); // Gets coordinates that were saved from TestBloodPressure.java
        	int lon = settings.getInt("currentLongitude", 0);
        	
        	//int lat = 28058987;
        	//int lon = -82415560;
        	
        	if (lat == 0 || lon == 0) {
        		return "Not available";
        	}
        	
        	String zipCode = convertCoordinatesToZipCode(lat/(double)1000000,lon/(double)1000000);
        	//String zipCode = "33620";
        	Log.d("DEBUG", "zipCode: " + zipCode); // Works
    		//Toast.makeText(_activity, "zip code = " + zipCode, Toast.LENGTH_SHORT).show(); // DEBUG // Can't call in AsyncTask (because it is not on the UI thread?)
            
        	URL url;        	
        	try {
				// url = new URL("http://api.wunderground.com/api/9db19bdec18308cd/conditions/q/CA/" + zipCode + ".xml"); // Doesn't work right now. But, code is fine with this url
        		// url = new URL("http://www.weather.com/weather/right-now/" + zipCode); // There is a IOException with this // Could be parsing error because it isn't xml
        		url = new URL("http://thale.accu-weather.com/widget/thale/weather-data.asp?location=" + zipCode);
				
	            SAXParserFactory factory = SAXParserFactory.newInstance(); // creates the factory
	            SAXParser parser = factory.newSAXParser();                 // creates a parser
	            XMLReader xmlreader = parser.getXMLReader();               // creates the reader (scanner)
	            RSSHandler theRssHandler = new RSSHandler();               // creates and instantiates our handler
	            xmlreader.setContentHandler(theRssHandler);                // assigns our handler to the reader (scanner)
	            InputSource is = new InputSource(url.openStream());        // gets data via the url class
	            xmlreader.parse(is);                                       // performs the synchronous parse
	            return theRssHandler.getFeed(); // get the results - should be a fully populated RSSFeed instance, or null on error
	        } catch (MalformedURLException e) { // from URL()
	    		return "Not available - bad url";
	        } catch (IOException e1) { // from openStream() and parse()
	    		return "Not available - bad io";
	        } catch (SAXException e2) { // from parse()
	    		return "Not available - bad sax";
			} catch (Exception e3) {
	    		return "Not available - other error";
	    	}
        }
    
//        protected void onProgressUpdate(Integer... progress) { // This is called each time you call publishProgress()
//            setProgressPercent(progress[0]);
//        }

        protected void onPostExecute(String result) { // This is called when doInBackground() is finished
            //showNotification("Downloaded " + result + " bytes");
        	try {
        		mTextViewOutdoorTemperature.setText("Outdoor Temperature: " + result); // WAS: parseInt(), but I don't know why. It is not needed
			} catch (NumberFormatException e) {
	        	mTextViewOutdoorTemperature.setText("Outdoor Temperature: " + result);
			}
        }
    } // END DownloadUrlStream()
    
	public String convertCoordinatesToZipCode(double lat, double lon) {
		//Toast.makeText(_activity, "lat,lon = " + lat + "," + lon, Toast.LENGTH_SHORT).show(); // DEBUG
		Geocoder geocoder = new Geocoder(this, Locale.ENGLISH);

		List<Address> addresses = null;

		try {
		    addresses = geocoder.getFromLocation(lat, lon, 3);
		} catch (IOException ex) { }

		String zipCode = null;
		for (int i = 0; i < addresses.size(); i++) {
		    Address address = addresses.get(i);
		    if (address.getPostalCode() != null) {
		        zipCode = address.getPostalCode();
		        return zipCode;
		    }
		}
		return null;
	} // END convertCoordinatesToZipCode()

} // END AddTemperature.java
