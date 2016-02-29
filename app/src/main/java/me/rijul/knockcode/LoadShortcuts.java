package me.rijul.knockcode;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by rijul on 29/2/16.
 */
public class LoadShortcuts extends AsyncTask<Void, Void, Void> {
    public interface AsyncResponse {
        //passcode, package name
        void shortcutsFinish(ArrayList<String> result);
    }

    AsyncResponse mDelegate;
    ArrayList<String> mResult = new ArrayList<>();

    LoadShortcuts(AsyncResponse delegate) {
        mDelegate = delegate;
    }

    @Override
    protected Void doInBackground(Void... params) {
        PackageManager pm = ((Context) mDelegate).getPackageManager();
        ApplicationInfo ai;
        SettingsHelper settingsHelper = new SettingsHelper((Context) mDelegate);
        Map<String, ?> allEntries = settingsHelper.getAll();
        for(Map.Entry<String, ?> entry : allEntries.entrySet())
            //package_1,2,3,4 : com.whatsapp
            try {
                if ((entry.getKey()!=null) && (entry.getKey().startsWith("package_"))) {
                    ai = pm.getApplicationInfo( (String) entry.getValue(), 0);
                    mResult.add((String) pm.getApplicationLabel(ai) );
                }
            } catch (NullPointerException e) {
            } catch (PackageManager.NameNotFoundException e) {
                settingsHelper.edit().remove(entry.getKey()).commit();
            }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        mDelegate.shortcutsFinish(mResult);
    }
}
