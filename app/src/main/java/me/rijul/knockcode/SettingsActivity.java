package me.rijul.knockcode;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Map;

/**
 * Created by rijul on 24/3/16.
 */
public class SettingsActivity extends Activity {
    private static SettingsHelper mSettingsHelper;
    private static boolean MODULE_INACTIVE = true;
    ProgressDialog mProgressDialog;

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return SettingsHelper.getWritablePreferences(getApplicationContext());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        mSettingsHelper = new SettingsHelper(this);
        //lastInstalledVersion was introduced in v36, with default 0
        if (mSettingsHelper.lastInstalledVersion() == 0) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
            convertShortcuts();
        } else if (mSettingsHelper.lastInstalledVersion() < BuildConfig.VERSION_CODE)
            mSettingsHelper.saveThisVersion();
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
        } else {
            if (getXposedVersionCode() < BuildConfig.VERSION_CODE) {
                View moduleActive = findViewById(R.id.module_inactive);
                ((TextView) moduleActive).setText(R.string.settings_reboot_upgrade);
                moduleActive.setVisibility(View.VISIBLE);
            }
        }
        CustomLogger.log(this, "MainActivity", "App", "Opened settings", null, -1);
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

    private int getXposedVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    private void convertShortcuts() {
        new ConvertShortcutsTask().execute();
    }

    private class ConvertShortcutsTask extends AsyncTask<Void, Void, Void> {
        boolean toPut = mSettingsHelper.customShortcutsDontUnlock();

        protected void onPostExecute(Void result) {
            mProgressDialog.dismiss();
            mSettingsHelper.saveThisVersion();
        }

        @Override
        protected Void doInBackground(Void... params) {
            SharedPreferences.Editor editor = mSettingsHelper.edit();
            Map<String, ?> allEntries = mSettingsHelper.getAll();
            for(Map.Entry<String, ?> entry : allEntries.entrySet())
                try {
                    if ((entry.getKey()!=null) && (entry.getKey().startsWith(Utils.PREFIX_SHORTCUT))) {
                        String value = (String) entry.getValue();
                        value = value + "|" + String.valueOf(toPut);
                        editor.putString(entry.getKey(), value);
                    }
                } catch (NullPointerException ignored) {
                }
            editor.apply();
            return null;
        }
    }


    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
            if (!mSettingsHelper.isVirgin())
                findPreference(Utils.SETTINGS_CHANGE_CODE).setTitle(R.string.settings_change_code);
            else
                findPreference(Utils.SETTINGS_CUSTOM_SHORTCUTS).setEnabled(false);
            String[] editTextPreferences = {"settings_code_text_ready_value", "settings_code_text_correct_value",
                                            "settings_code_text_error_value", "settings_code_text_disabled_value",
                                            "settings_code_text_reset_value"};
            for(String key : editTextPreferences) {
                EditTextPreference etp = (EditTextPreference) findPreference(key);
                etp.setSummary(etp.getText());
            }
        }



        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            String key = preference.getKey();
            if (key==null)
                return false;
            else if (key.equals(Utils.SETTINGS_CHANGE_CODE)) {
                startActivityForResult(new Intent(getActivity(), MainActivity.class), MainActivity.CHANGE_CODE);
                return true;
            } else if (key.equals(Utils.SETTINGS_HIDE_LAUNCHER)) {
                if (((SwitchPreference) preference).isChecked()) {
                    ComponentName componentName = new ComponentName(getActivity(), BuildConfig.APPLICATION_ID + ".MainActivity-Alias");
                    getActivity().getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                } else {
                    ComponentName componentName = new ComponentName(getActivity(), BuildConfig.APPLICATION_ID + ".MainActivity-Alias");
                    getActivity().getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                }
                return true;
            }
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
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            key = SecurePreferences.decrypt(key);
            Preference preference = findPreference(key);
            if (preference instanceof EditTextPreference) {
                EditTextPreference editTextPreference = (EditTextPreference) preference;
                preference.setSummary(editTextPreference.getText());
            }
        }
    }
}
