package me.rijul.knockcode;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

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
		setContentView(R.layout.activity_about);
		addPreferencesFromResource(R.xml.preferences);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		findPreference("change_knock_code").setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                startActivity(new Intent(MainActivity.this, ChangeKnockCodeActivity.class));
                return false;
            }
        });
	findPreference("about_key").setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            startActivity(new Intent(MainActivity.this, AboutActivity.class));
            return false;
        }
    });
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
