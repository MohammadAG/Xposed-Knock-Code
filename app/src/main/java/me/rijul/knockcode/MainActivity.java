package me.rijul.knockcode;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

public class MainActivity extends AppCompatPreferenceActivity implements OnSharedPreferenceChangeListener  {
	private static boolean MODULE_INACTIVE = true;
	private Menu mMenu;

	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
		return SettingsHelper.getWritablePreferences(getApplicationContext());
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Toast.makeText(getActivity(), R.string.reboot_required, Toast.LENGTH_LONG).show();
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
					new AlertDialog.Builder(getActivity())
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
					ComponentName componentName = new ComponentName(getActivity(), "me.rijul.knockcode.MainActivity");
					getActivity().getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
				} else {
					ComponentName componentName = new ComponentName(getActivity(), "me.rijul.knockcode.MainActivity");
					getActivity().getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
				}
				return true;
			}
		});

		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				updateSwitchState(false);
			}
		}, new IntentFilter("me.rijul.knockcode.RELOAD_SWITCH_STATE"));
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
		mMenu = menu;
        getMenuInflater().inflate(R.menu.main, menu);
		updateSwitchState(true);
        return true;
	}

	private void updateSwitchState(final boolean showToast) {
		SwitchCompat master_switch = (SwitchCompat) MenuItemCompat.getActionView(mMenu.findItem(R.id.toolbar_master_switch));
		master_switch.setChecked(!(new SettingsHelper(getApplicationContext()).isDisabled()));
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
            startActivity(new Intent(MainActivity.this, AboutActivity.class));
			return true;
		}
		else if (id == R.id.restart_systemui) {
			SettingsHelper.killPackage("com.android.systemui");
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}