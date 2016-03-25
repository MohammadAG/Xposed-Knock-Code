package me.rijul.knockcode;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

/**
 * Created by rijul on 24/3/16.
 */
public class SettingsActivity extends Activity {
    private static SettingsHelper mSettingsHelper;
    private static boolean MODULE_INACTIVE = true;
    BroadcastReceiver deadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            invalidateOptionsMenu();
        }
    };

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return SettingsHelper.getWritablePreferences(getApplicationContext());
    }

    public SharedPreferences getSharedPreferences(String name, int mode, boolean original) {
        return super.getSharedPreferences(name, mode);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        mSettingsHelper = new SettingsHelper(this);
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, new SettingsFragment()).commit();
        if (MODULE_INACTIVE) {
            View moduleActive = findViewById(R.id.module_inactive);
            moduleActive.setVisibility(View.VISIBLE);
            moduleActive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(SettingsActivity.this)
                            .setTitle(R.string.settings_module_inactive_title)
                            .setMessage(R.string.settings_module_inactive_message)
                            .create()
                            .show();
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        Switch masterSwitch = ((Switch) menu.findItem(R.id.toolbar_master_switch).getActionView());
        masterSwitch.setChecked(!mSettingsHelper.isSwitchOff());
        masterSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSettingsHelper.putBoolean(Utils.SETTINGS_SWITCH, isChecked);
                SettingsActivity.this.sendBroadcast(new Intent(Utils.SETTINGS_CHANGED));
            }
        });
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(deadReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(deadReceiver, new IntentFilter(BuildConfig.APPLICATION_ID + ".DEAD"));
        try {
            if (android.provider.Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCK_PATTERN_ENABLED) == 0) {
                View patternActive = findViewById(R.id.pattern_inactive);
                patternActive.setVisibility(View.VISIBLE);
                patternActive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent("android.app.action.SET_NEW_PASSWORD");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                });
            } else
                findViewById(R.id.pattern_inactive).setVisibility(View.GONE);
        } catch (Settings.SettingNotFoundException ignored) {}
    }


    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
            if (!mSettingsHelper.isVirgin())
                findPreference(Utils.SETTINGS_CHANGE_CODE).setTitle(R.string.settings_change_code);
            else
                findPreference(Utils.SETTINGS_CUSTOM_SHORTCUTS).setEnabled(false);
            findPreference(Utils.SETTINGS_CODE_FULLSCREEN).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                @SuppressLint("WorldReadableFiles")
                @SuppressWarnings("deprecation")
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    ((SettingsActivity) getActivity()).getSharedPreferences(Utils.PREFERENCES_FILE, Context.MODE_WORLD_READABLE, true).edit().putBoolean(
                            Utils.SETTINGS_CODE_FULLSCREEN, (boolean) newValue).apply();
                    return true;
                }
            });
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            String key = preference.getKey();
            if (key==null)
                return false;
            else if (key.equals(Utils.SETTINGS_CHANGE_CODE)) {
                startActivityForResult(new Intent(getActivity(), MainActivity.class), MainActivity.CHANGE_CODE);
                return true;
            } else if (key.equals(Utils.SETTINGS_RESTART_KEYGUARD)) {
                Utils.restartKeyguard(getActivity());
                return true;
            }
            else if (key.equals(Utils.SETTINGS_HIDE_LAUNCHER)) {
                if (((SwitchPreference) preference).isChecked()) {
                    ComponentName componentName = new ComponentName(getActivity(), BuildConfig.APPLICATION_ID + ".MainActivity-Alias");
                    getActivity().getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                } else {
                    ComponentName componentName = new ComponentName(getActivity(), BuildConfig.APPLICATION_ID + ".MainActivity-Alias");
                    getActivity().getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                }
                return true;
            } else if (key.equals(Utils.SETTINGS_CODE_FULLSCREEN))
                Toast.makeText(getActivity(), R.string.settings_reboot_required, Toast.LENGTH_SHORT).show();
            return false;
        }

        @Override
        public void startActivityForResult(Intent intent, int requestCode) {
            intent.putExtra("requestCode", Integer.toString(requestCode));
            super.startActivityForResult(intent, requestCode);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (resultCode==MainActivity.RESULT_OK) {
                findPreference(Utils.SETTINGS_CUSTOM_SHORTCUTS).setEnabled(true);
                findPreference(Utils.SETTINGS_CHANGE_CODE).setTitle(R.string.settings_change_code);
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            getActivity().sendBroadcast(new Intent(Utils.SETTINGS_CHANGED));
        }

        @Override
        public void onResume() {
            super.onResume();
        }
    }
}
