package net.simplyadvanced.vitalsigns;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.*;

import android.util.Log;

public class RSSHandler extends DefaultHandler {
	private String _feed; // For outdoorTemperature
	int currentstate = 0;

	RSSHandler() {} // Empty constructor
	
	/* This returns our feed when all of the parsing is complete */
	String getFeed() {
		return _feed;
	}

	public void startDocument() throws SAXException { // I don't think I need anything inside this method
//		// initialize our RSSFeed object - this will hold our parsed contents
//		_feed = new RSSFeed();
//		// initialize the RSSItem object - we will use this as a crutch to grab the info from the channel
//		// because the channel and items have very similar entries..
//		_item = new RSSItem();
	}
	public void endDocument() throws SAXException {}
	public void startElement(String namespaceURI, String localName,String qName, Attributes atts) throws SAXException {
//		depth++;
		
		/** Use this set for the wunderground api */
//		if (localName.equals("temperature_string")) {
//			Log.d("DEBUG", "Found temperature_string tag from xml parsing");
//			//_feed = atts.getValue("temp");
//			currentstate = 1;
//			return;
//		}

		/** Use this set for the weather.com api */
//		if (localName.equals("span")) {
//			Log.d("DEBUG", "Found span tag from xml parsing");
//			//if (atts.getLocalName(1).equals("itemprop")) {
//				if (atts.getValue("itemprop").equals("temperature-fahrenheit")) {
//					//_feed = atts.getValue("itemprop");
//					currentstate = 1;
//				}
//			//}
//		return;
		
		/** Use this set for the accu-weather.com api */
		if (localName.equals("temperature")) { // could also use "realfeel" for what temperature it feels like
		    Log.d("DEBUG", "Found temperature tag from xml parsing");
			currentstate = 1;
			return;
		}
		
		
//		if (localName.equals("id")) {
//			currentstate = RSS_ID;
//			return;
//		}
//		if (localName.equals("text")) {
//			currentstate = RSS_TWEET;
//			return;
//		}
//		// if we don't explicitly handle the element, make sure we don't wind up erroneously 
//		// storing a newline or other bogus data into one of our existing elements
		currentstate = 0;
	}
	
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
//		depth--;
//		if (localName.equals("status")) {
//			_feed.addItem(_item); // add our item to the list!
//			return;
//		}
	}
	 
	public void characters(char ch[], int start, int length) {
//		int realLength = 0;
//		for(int i=0;i<ch.length;i++) {
//			if((ch[i] == '<') && (ch[i+1] == '/')) {
//				realLength = i;
//				break;
//			}
//		}
		
		String theString = new String(ch,start,length);
//		Log.i("RSSReader","characters[" + theString + "]");
		
		switch (currentstate) {
			case 1: // "temperature_string"
				_feed = theString;
				Log.d("DEBUG", "ch.toString() = " + _feed);
				currentstate = 2;
				break;
//			case RSS_ID: // 1
//				_item.setId(theString);
//				currentstate = 0;
//				break;
//			case RSS_TWEET: // 2
//				_item.setTweet(theString);
//				currentstate = 0;
//				break;
			default:
				return;
		}
	}

}
