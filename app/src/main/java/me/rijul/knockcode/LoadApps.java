package me.rijul.knockcode;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by rijul on 29/2/16.
 */
public class LoadApps extends AsyncTask<Void, Void, Void> {
    public interface AsyncResponse {
        //passcode, package name
        void appsFinish(ArrayList<String> result);
    }

    AsyncResponse mDelegate;
    ArrayList<String> mResult = new ArrayList<>();

    LoadApps(AsyncResponse delegate) {
        mDelegate = delegate;
    }

    @Override
    protected Void doInBackground(Void... params) {
        PackageManager pm = ((Context) mDelegate).getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for(ApplicationInfo packageInfo : packages)
            if(pm.getLaunchIntentForPackage(packageInfo.packageName)!= null &&
                    !pm.getLaunchIntentForPackage(packageInfo.packageName).equals("")) {
                mResult.add((String) pm.getApplicationLabel(packageInfo));
            }
        Collections.sort(mResult, String.CASE_INSENSITIVE_ORDER);
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        mDelegate.appsFinish(mResult);
    }
}
