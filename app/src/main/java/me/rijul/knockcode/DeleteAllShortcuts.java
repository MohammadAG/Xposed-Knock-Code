package me.rijul.knockcode;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import java.util.Map;

/**
 * Created by rijul on 29/2/16.
 */
public class DeleteAllShortcuts extends AsyncTask<Void, Void, Void> {
    public interface AsyncResponse {
        //passcode, package name
        void deleteShortcutsFinish();
    }

    AsyncResponse mDelegate;

    DeleteAllShortcuts(AsyncResponse delegate) {
        mDelegate = delegate;
    }

    @Override
    protected Void doInBackground(Void... params) {
        SettingsHelper settingsHelper = new SettingsHelper((Context) mDelegate);
        SharedPreferences.Editor editor = settingsHelper.edit();
        Map<String, ?> allEntries = settingsHelper.getAll();
        for(Map.Entry<String, ?> entry : allEntries.entrySet())
            //package_1,2,3,4 : com.whatsapp
            try {
                if ((entry.getKey()!=null) && (entry.getKey().startsWith("uri_"))) {
                    editor.remove(entry.getKey());
                }
            } catch (NullPointerException e) {
            }
        editor.apply();
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        mDelegate.deleteShortcutsFinish();
    }
}
