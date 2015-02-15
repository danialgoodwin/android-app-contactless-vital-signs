package net.simplyadvanced.vitalsigns.util;

import android.os.Environment;

/** Static helper methods related to device storage. */
public class StorageUtils {

    /** No need to instantiate this class. */
    private StorageUtils() {}

    /** Returns true if external storage (like SD card or emulated SD card) is available
     * and able to write to it, otherwise returns false. */
    public static boolean isExternalStorageAvailableAndWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
//        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
//            // We can only read the media
//            mExternalStorageAvailable = true;
//            mExternalStorageWritable = false;
//        } else {
//            // Something else is wrong. It may be one of many other states, but all we need to know is we can neither read nor write
//            mExternalStorageAvailable = mExternalStorageWritable = false;
//        }
    }

}
