package me.rijul.knockcode;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

import java.util.List;

/**
 * Created by rijul on 29/2/16.
 */
public class GetPackageNameForApp extends AsyncTask<String, Void, Void> {
    public interface AsyncResponse {
        void foundPackageName(String packageName, int requestCode);
    }

    public static final int APP_LISTENER = 1;
    public static final int SHORTCUT_LISTENER = 2;

    AsyncResponse mDelegate;
    int mRequestCode;
    String result = null;

    GetPackageNameForApp(AsyncResponse delegate, int requestCode) {
        mDelegate = delegate;
        mRequestCode = requestCode;
    }

    @Override
    protected Void doInBackground(String... params) {
        String appName = params[0];
        PackageManager pm = ((Context) mDelegate).getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for(ApplicationInfo packageInfo : packages)
            if (((String) pm.getApplicationLabel(packageInfo)).equals(appName))
                result =  packageInfo.packageName;
        return null;
    }

    @Override
    protected void onPostExecute(Void result2) {
        mDelegate.foundPackageName(result, mRequestCode);
    }
}
