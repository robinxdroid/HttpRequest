package net.robinx.lib.http.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Provide some help about the network function
 * @author Robin
 * @since 2015/5/28 11:23.
 */
public class NetworkUtils {

    public static boolean checkNet(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null;
    }
    
}
