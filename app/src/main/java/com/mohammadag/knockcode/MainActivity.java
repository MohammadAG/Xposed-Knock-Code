package com.mohammadag.knockcode;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener, OnPreferenceChangeListener  {
	private static final String URL_MY_MODULES = "http://repo.xposed.info/users/mohammadag";
	private static final String URL_MY_APPS = "market://search?q=pub:Mohammad Abu-Garbeyyeh";

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

	// We need to find a way to simulate pattern entries, will look into that later.
	// Simulation
	//		ArrayList<Cell> array = new ArrayList<Cell>();
	//		array.add(new Cell(1, 0));
	//		array.add(new Cell(1, 1));
	//		array.add(new Cell(2, 0));
	//		array.add(new Cell(2, 1));
	//		Toast.makeText(getApplicationContext(), PatternUtils.patternToString(array),
	//				Toast.LENGTH_SHORT).show();

	@SuppressWarnings("deprecation")
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);
		findPreference("change_knock_code").setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				startActivity(new Intent(MainActivity.this, ChangeKnockCodeActivity.class));
				return false;
			}
		});
		initCopyright();

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

	@SuppressWarnings("deprecation")
	private void initCopyright() {
		Preference copyrightPreference = findPreference("copyright_key");
		copyrightPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setTitle("")
				.setItems(R.array.my_apps, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Uri uri = null;
						Intent intent = new Intent(Intent.ACTION_VIEW);
						switch (which) {
						case 0:
							uri = Uri.parse(URL_MY_APPS);
							intent.setPackage("com.android.vending");
							break;
						case 1:
							uri = Uri.parse(URL_MY_MODULES);
							break;
						}
						try {
							startActivity(intent.setData(uri));
						} catch (ActivityNotFoundException e) {
							Toast.makeText(MainActivity.this, "Play Store not found", Toast.LENGTH_SHORT).show();
						}
					}
				});
				builder.create().show();
				return false;
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
