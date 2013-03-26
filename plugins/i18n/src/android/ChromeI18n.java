package org.apache.cordova;

import java.util.Locale;
import org.apache.cordova.api.CallbackContext;
import org.apache.cordova.api.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.util.Log;

public class ChromeI18n extends CordovaPlugin {

    private static final String LOG_TAG = "ChromeI18n";

    @Override
    public boolean execute(String action, CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        if ("getAcceptLanguages".equals(action)) {
            getAcceptLanguages(args, callbackContext);
            return true;
        }

        return false;
    }

    private void getAcceptLanguages(final CordovaArgs args, final CallbackContext callbackContext) {
        try {
            JSONArray ret = new JSONArray();
            Locale locale = Locale.getDefault();
            String localString = locale.toString().replace('_', '-'); 
            ret.put(localString);
            callbackContext.success(ret);
        } catch (Exception e) {
            callbackContext.error("Could not retrieve supported locales");
            Log.e(LOG_TAG, "Could not retrieve supported locales", e);
        }
    }
}
