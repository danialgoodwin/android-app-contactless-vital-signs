package net.simplyadvanced.vitalsigns.util;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;

/** Static helper methods related to device Internet connectivity. */
public class ConnectivityUtils {

    /** No need to instantiate this class. */
    private ConnectivityUtils() {}

    /** Returns true if Internet access is currently available, otherwise false. */
    public static boolean hasInternet(Activity activity) {
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

}
