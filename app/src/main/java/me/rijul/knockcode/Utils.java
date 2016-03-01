package me.rijul.knockcode;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by rijul on 1/3/16.
 */
public class Utils {
    private static class killPackage extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            String packageToKill = params[0];
            Process su = null;
            try {
                su = Runtime.getRuntime().exec("su");
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (su!= null) {
                try {
                    DataOutputStream os = new DataOutputStream(su.getOutputStream());
                    os.writeBytes("pkill -f " + packageToKill + "\n");
                    os.flush();
                    os.writeBytes("exit\n");
                    os.flush();
                    su.waitFor();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    public static void killKeyguard(Context ctx) {
        PackageManager packageManager = ctx.getPackageManager();
        String packageToKill;
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo("com.htc.lockscreen", 0);
            packageToKill = "com.htc.lockscreen";
        } catch (PackageManager.NameNotFoundException e) {
            packageToKill = "com.android.systemui";
        }
        (new killPackage()).execute(packageToKill);
    }

    public static String passcodeToString(ArrayList<Integer> passcode) {
        String string = "";
        for (int i = 0; i < passcode.size(); i++) {
            string += String.valueOf(passcode.get(i));
            if (i != passcode.size()-1) {
                string += ",";
            }
        }
        return string;
    }

    public static ArrayList<Integer> stringToPasscode(String string) {
        ArrayList<Integer> passcode = new ArrayList<>();
        String[] integers = string.split(",");
        for (String digitString : integers) {
            passcode.add(Integer.parseInt(digitString));
        }
        return passcode;
    }
}
