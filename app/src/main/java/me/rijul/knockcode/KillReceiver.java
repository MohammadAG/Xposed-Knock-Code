package me.rijul.knockcode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by rijul on 25/3/16.
 */
public class KillReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        new SettingsHelper(context).putBoolean(Utils.SETTINGS_SWITCH, false);
        context.sendBroadcast(new Intent(BuildConfig.APPLICATION_ID + ".DEAD"));
    }
}
