package me.rijul.knockcode;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;

import java.util.Map;

/**
 * Created by rijul on 29/2/16.
 */
public class CheckPasswordValid extends AsyncTask<String, Void, Void> {
    public interface AsyncResponse {
        void isPasswordValid(String result);
    }

    String mResult = null;
    AsyncResponse mDelegate;

    CheckPasswordValid(AsyncResponse delegate) {
        mDelegate = delegate;
    }

    @Override
    protected Void doInBackground(String... params) {
        String newPassword = params[0];
        SettingsHelper settingsHelper = new SettingsHelper((Context) mDelegate);
        Map<String, ?> allEntries = settingsHelper.getAll();
        for(Map.Entry<String, ?> entry : allEntries.entrySet())
            //package_1,2,3,4 : com.whatsapp
            try {
                if ((entry.getKey() != null) && (entry.getKey().startsWith("package_"))) {
                    String oldPassword = ((String) entry.getKey()).substring(8);
                    if (oldPassword.equals(newPassword))
                        mResult = (String) entry.getValue();
                    else if (newPassword.startsWith(oldPassword))
                        mResult = (String) entry.getValue();
                    else if (oldPassword.startsWith(newPassword))
                        mResult = (String) entry.getValue();
                }
            } catch (NullPointerException e) {}
        return null;
    }

    @Override
    protected void onPostExecute(Void result2) {
        if (mResult==null)
            mDelegate.isPasswordValid(null);
        else {
            PackageManager packageManager = ((Context) mDelegate).getPackageManager();
            ApplicationInfo applicationInfo;
            try {
                applicationInfo = packageManager.getApplicationInfo(mResult, 0);
            } catch (PackageManager.NameNotFoundException e) {
                applicationInfo = null;
            }
            String applicationLabel = (String) (applicationInfo == null ? null : packageManager.getApplicationLabel(applicationInfo));
            mDelegate.isPasswordValid(applicationLabel);
        }
    }
}
