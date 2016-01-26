package me.rijul.knockcode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import de.robv.android.xposed.XposedBridge;

/**
 * Created by rijul on 26/1/16.
 */
public class KillReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        new SettingsHelper(context).edit().putBoolean("switch", false).commit();
        XposedBridge.log("[KnockCode] SystemUI is killing us");
        context.sendBroadcast(new Intent("me.rijul.knockcode.DEAD"));
        context.sendBroadcast(new Intent("me.rijul.knockcode.RELOAD_SWITCH_STATE"));
    }
}
