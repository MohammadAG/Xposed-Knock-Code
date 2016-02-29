package me.rijul.knockcode;

import android.content.Context;
import android.os.AsyncTask;
import java.util.Map;

/**
 * Created by rijul on 29/2/16.
 */
public class GetKeyForValue extends AsyncTask<String, Void, Void> {
    public interface AsyncResponse {
        void foundKey(String key);
    }

    AsyncResponse mDelegate;
    String result = null;

    GetKeyForValue(AsyncResponse delegate) {
        mDelegate = delegate;
    }

    @Override
    protected Void doInBackground(String... params) {
        String value = params[0];
        SettingsHelper settingsHelper = new SettingsHelper((Context) mDelegate);
        Map<String, ?> allEntries = settingsHelper.getAll();
        for(Map.Entry<String, ?> entry : allEntries.entrySet())
            if ((entry.getValue()!=null) && (entry.getValue().equals(value)))
                result = entry.getKey();
        return null;
    }

    @Override
    protected void onPostExecute(Void result2) {
        mDelegate.foundKey(result);
    }
}
