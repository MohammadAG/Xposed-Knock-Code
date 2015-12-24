package me.rijul.knockcode;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

public class MainActivity extends AppCompatPreferenceActivity implements OnSharedPreferenceChangeListener, OnPreferenceChangeListener  {

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		SettingsHelper.emitSettingsChanged(getApplicationContext());
		return true;
	}

	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
		return SettingsHelper.getWritablePreferences(getApplicationContext());
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		SettingsHelper.emitSettingsChanged(getApplicationContext());
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		addPreferencesFromResource(R.xml.preferences);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		if (isModuleDisabled()) {
			View moduleActive = findViewById(R.id.xposed_active);
			moduleActive.setVisibility(View.VISIBLE);
			moduleActive.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					new AlertDialog.Builder(getActivity())
							.setTitle(R.string.module_inactive)
							.setMessage(R.string.dialog_message_not_active)
							.create()
							.show();
				}
			});
		}

		findPreference("hide_app_from_launcher").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Toast.makeText(getActivity(), R.string.reboot_required, Toast.LENGTH_SHORT).show();
				if (((CheckBoxPreference) preference).isChecked()) {
					ComponentName componentName = new ComponentName(getActivity(), "me.rijul.knockcode.MainActivity");
					getActivity().getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
				} else {
					ComponentName componentName = new ComponentName(getActivity(), "me.rijul.knockcode.MainActivity");
					getActivity().getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
				}
				return true;
			}
		});
	}

	public static boolean isModuleDisabled() {
		return true;
	}

	public MainActivity getActivity() {
		return this;
	}

	@Override
	protected void onPause() {
		getSharedPreferences("", 0).unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		getSharedPreferences("", 0).registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
		SwitchCompat master_switch = (SwitchCompat) MenuItemCompat.getActionView(menu.findItem(R.id.toolbar_master_switch));
		master_switch.setChecked(!(new SettingsHelper(getApplicationContext()).isDisabled()));
		master_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton button, boolean b) {
				new SettingsHelper(getApplicationContext()).edit().putBoolean("switch", b).commit();
			}
		});
        return true;
	}



	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_about) {
            startActivity(new Intent(MainActivity.this, AboutActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}