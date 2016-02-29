package me.rijul.knockcode;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

public class SettingsActivity extends AppCompatPreferenceActivity implements OnSharedPreferenceChangeListener  {
	private static boolean MODULE_INACTIVE = true;
	private Menu mMenu;
	private BroadcastReceiver deadReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateSwitchState(false);
		}
	};

	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
		return SettingsHelper.getWritablePreferences(getApplicationContext());
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Toast.makeText(SettingsActivity.this, R.string.reboot_required, Toast.LENGTH_LONG).show();
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		addPreferencesFromResource(R.xml.preferences);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		if (MODULE_INACTIVE) {
			View moduleActive = findViewById(R.id.xposed_active);
			moduleActive.setVisibility(View.VISIBLE);
			moduleActive.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					new AlertDialog.Builder(SettingsActivity.this)
							.setTitle(R.string.module_inactive_dialog)
							.setMessage(R.string.dialog_message_not_active)
							.create()
							.show();
				}
			});
		}

		findPreference("hide_app_from_launcher").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (((CheckBoxPreference) preference).isChecked()) {
					ComponentName componentName = new ComponentName(SettingsActivity.this, "me.rijul.knockcode.MainActivity");
					SettingsActivity.this.getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
				} else {
					ComponentName componentName = new ComponentName(SettingsActivity.this, "me.rijul.knockcode.MainActivity");
					SettingsActivity.this.getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
				}
				return true;
			}
		});

		findPreference("change_knock_code").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivityForResult(new Intent(SettingsActivity.this, MainActivity.class), MainActivity.SET_NEW_PASSCODE);
				return true;
			}
		});

		boolean oldCode = new SettingsHelper(SettingsActivity.this).getPasscodeOrNull() != null;
		findPreference("key_custom_shortcuts").setEnabled(oldCode);

		if (!oldCode) {
			Preference pref = findPreference("change_knock_code");
			pref.setTitle(R.string.set_knock_code_title);
			pref.setSummary(R.string.set_knock_code_summary);
		}
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		intent.putExtra("requestCode", Integer.toString(requestCode));
		super.startActivityForResult(intent, requestCode);
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode==RESULT_OK) {
			findPreference("key_custom_shortcuts").setEnabled(true);
			Preference pref = findPreference("change_knock_code");
			pref.setTitle(R.string.pref_title_change_code);
			pref.setSummary(R.string.pref_description_change_code);
			if (new SettingsHelper(SettingsActivity.this).isSwitchOff())
				Toast.makeText(SettingsActivity.this, R.string.reboot_required, Toast.LENGTH_SHORT).show();
		}
	}


	@Override
	protected void onPause() {
		getSharedPreferences("", 0).unregisterOnSharedPreferenceChangeListener(this);
		unregisterReceiver(deadReceiver);
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		getSharedPreferences("", 0).registerOnSharedPreferenceChangeListener(this);
		registerReceiver(deadReceiver, new IntentFilter("me.rijul.knockcode.DEAD"));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mMenu = menu;
        getMenuInflater().inflate(R.menu.main, menu);
		updateSwitchState(true);
        return true;
	}

	private void updateSwitchState(final boolean showToast) {
		SwitchCompat master_switch = (SwitchCompat) MenuItemCompat.getActionView(mMenu.findItem(R.id.toolbar_master_switch));
		master_switch.setChecked(!(new SettingsHelper(getApplicationContext()).isSwitchOff()));
		if (!showToast)
			return;
		master_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton button, boolean b) {
				new SettingsHelper(getApplicationContext()).edit().putBoolean("switch", b).commit();
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_about) {
            startActivity(new Intent(SettingsActivity.this, AboutActivity.class));
			return true;
		}
		else if (id == R.id.restart_systemui) {
			SettingsHelper.killPackage();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}