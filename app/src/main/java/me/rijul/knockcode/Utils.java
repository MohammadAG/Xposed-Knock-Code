package me.rijul.knockcode;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by rijul on 2/3/16.
 */
public class Utils {
    public static final String PREFERENCES_FILE = BuildConfig.APPLICATION_ID + "_preferences";
    public static final String PREFIX_SHORTCUT = "shortcut_";

    public static final String SETTINGS_PASSCODE = "settings_passcode";
    public static final String SETTINGS_SWITCH = "settings_switch";

    public static final String SETTINGS_CHANGE_CODE = "settings_change_code";
    public static final String SETTINGS_CUSTOM_SHORTCUTS = "settings_custom_shortcuts";
    public static final String SETTINGS_CUSTOM_SHORTCUTS_DONT_UNLOCK = "settings_custom_shortcuts_dont_unlock";
    public static final String SETTINGS_RESTART_KEYGUARD = "settings_restart_keyguard";
    public static final String SETTINGS_HIDE_LAUNCHER = "settings_hide_launcher";
    public static final String SETTINGS_FAILSAFE = "settings_failsafe";

    public static final String SETTINGS_CODE_FULLSCREEN = "settings_code_fullscreen";
    public static final String SETTINGS_CODE_DIALOG = "settings_code_dialog";
    public static final String SETTINGS_CODE_DIRECT_ENTRY = "settings_code_direct_entry";
    public static final String SETTINGS_CODE_TEXT = "settings_code_text";
    public static final String SETTINGS_CODE_BACKGROUND = "settings_code_background";
    public static final String SETTINGS_CODE_BACKGROUND_COLOR = "settings_code_background_color";
    public static final String SETTINGS_CODE_TAPS_VISIBLE = "settings_code_taps_visible";

    public static final String SETTINGS_CODE_LINES_VISIBLE = "settings_code_lines_visible";
    public static final String SETTINGS_CODE_LINES_COLOR_READY = "settings_code_lines_color_ready";
    public static final String SETTINGS_CODE_LINES_CORRECT = "settings_code_lines_correct";
    public static final String SETTINGS_CODE_LINES_COLOR_CORRECT = "settings_code_lines_color_correct";
    public static final String SETTINGS_CODE_LINES_ERROR = "settings_code_lines_error";
    public static final String SETTINGS_CODE_LINES_COLOR_ERROR = "settings_code_lines_color_error";
    public static final String SETTINGS_CODE_LINES_DISABLED = "settings_code_lines_disabled";
    public static final String SETTINGS_CODE_LINES_COLOR_DISABLED = "settings_code_lines_color_disabled";

    public static final String SETTINGS_CODE_DOTS_VISIBLE = "settings_code_dots_visible";
    public static final String SETTINGS_CODE_DOTS_COLOR_READY = "settings_code_dots_color_ready";
    public static final String SETTINGS_CODE_DOTS_CORRECT = "settings_code_dots_correct";
    public static final String SETTINGS_CODE_DOTS_COLOR_CORRECT = "settings_code_dots_color_correct";
    public static final String SETTINGS_CODE_DOTS_ERROR = "settings_code_dots_error";
    public static final String SETTINGS_CODE_DOTS_COLOR_ERROR = "settings_code_dots_color_error";

    public static final String SETTINGS_CODE_SIZE_COLUMNS = "settings_code_size_columns";
    public static final String SETTINGS_CODE_SIZE_ROWS = "settings_code_size_rows";

    public static final String SETTINGS_CHANGED = BuildConfig.APPLICATION_ID + "_SETTINGS_CHANGED";

    public static final String SETTINGS_EMERGENCY_BUTTON = "settings_emergency_button";
    public static final String SETTINGS_EMERGENCY_TEXT = "settings_emergency_button";

    public static final String SETTINGS_VIBRATE_TAP = "settings_vibrate_tap";
    public static final String SETTINGS_VIBRATE_LONG_PRESS = "settings_vibrate_long_press";

    public static final String ABOUT_DISCLAIMER = "about_disclaimer";
    public static final String ABOUT_RIJUL = "about_rijul";

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

    public static void restartKeyguard(Context ctx) {
        PackageManager packageManager = ctx.getPackageManager();
        String packageToKill;
        try {
            packageManager.getApplicationInfo("com.htc.lockscreen", 0);
            packageToKill = "com.htc.lockscreen";
        } catch (PackageManager.NameNotFoundException e) {
            packageToKill = "com.android.systemui";
        }
        (new killPackage()).execute(packageToKill);
    }

    public static Resources getResourcesForPackage(Context context, String packageName) {
        try {
            context = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
            return context.getResources();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(Utils.class.getName(), "", e);
        }
        return null;
    }

    public static String getString(Context context, int id) {
        return getOwnResources(context).getString(id);
    }

    public static String getString(Context context, int id, Object... args) {
        return getOwnResources(context).getString(id, args);
    }

    public static Resources getOwnResources(Context context) {
        return getResourcesForPackage(context, BuildConfig.APPLICATION_ID);
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