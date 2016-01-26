package me.rijul.knockcode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.provider.Settings.Secure;
import android.util.Log;

import de.robv.android.xposed.XSharedPreferences;

public class SettingsHelper {
	public static final String PACKAGE_NAME = "me.rijul.knockcode";
	private static final String PREFS_NAME = PACKAGE_NAME + "_preferences";
	public static final String INTENT_SETTINGS_CHANGED = PACKAGE_NAME + ".SETTINGS_CHANGED";

	private XSharedPreferences mXPreferencesImpl = null;
	private SecurePreferences mXPreferences = null;
	private static SecurePreferences mPreferences = null;
	private Context mContext;

	public static HashSet<OnSettingsReloadedListener> mReloadListeners = new HashSet<SettingsHelper.OnSettingsReloadedListener>();
	private String mUuid;

	public interface OnSettingsReloadedListener {
		void onSettingsReloaded();
	}

	public SettingsHelper(String uuid) {
		mUuid = uuid;
		mXPreferencesImpl = new XSharedPreferences(PACKAGE_NAME);
		mXPreferencesImpl.makeWorldReadable();
		mXPreferences = new SecurePreferences(mXPreferencesImpl, uuid);

		reloadSettings();
	}

	public SettingsHelper(Context context) {
		mContext = context;
		mPreferences = getWritablePreferences(context);
	}

	public Context getContext() {
		return mContext;
	}

	public void reloadSettings() {
		mXPreferencesImpl.reload();
		if (mUuid!=null)
			mXPreferences = new SecurePreferences(mXPreferencesImpl, mUuid);
		announceToListeners();
	}

	public static void announceToListeners() {
		try {
			if (mReloadListeners != null) {
				for (SettingsHelper.OnSettingsReloadedListener listener : mReloadListeners)
					listener.onSettingsReloaded();
			}
		} catch (Throwable t) {}
	}

	public void addInProcessListener(Context context) {
		context.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				reloadSettings();
			}
		}, new IntentFilter(INTENT_SETTINGS_CHANGED));
	}

	public static void emitSettingsChanged(Context context) {
		context.sendBroadcast(new Intent(INTENT_SETTINGS_CHANGED));
	}

	public void addOnReloadListener(OnSettingsReloadedListener listener) {
		if (mReloadListeners == null)
			mReloadListeners = new HashSet<SettingsHelper.OnSettingsReloadedListener>();

		mReloadListeners.add(listener);
	}

	public Editor edit() {
		return mPreferences.edit();
	}

	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	public static SecurePreferences getWritablePreferences(Context context) {
		String uuid = Secure.getString(context.getContentResolver(),
				Secure.ANDROID_ID);
		if (mPreferences == null)
			mPreferences = new SecurePreferences(context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE), uuid);

		return mPreferences;
	}

	public String getString(String key, String defaultValue) {
		String returnResult = defaultValue;
		if (mPreferences != null) {
			returnResult = mPreferences.getString(key, defaultValue);
		} else if (mXPreferences != null) {
			returnResult = mXPreferences.getString(key, defaultValue);
		}
		return returnResult;
	}

	public Map<String, ?> getAll() {
		if (mPreferences != null) {
			return mPreferences.getAll();
		} else if (mXPreferences != null) {
			return mXPreferences.getAll();
		}

		return null;
	}

	public float getFloat(String key, float defaultValue) {
		float returnResult = defaultValue;
		if (mPreferences != null) {
			returnResult = mPreferences.getFloat(key, defaultValue);
		} else if (mXPreferences != null) {
			returnResult = mXPreferences.getFloat(key, defaultValue);
		}
		return returnResult;
	}

	public int getInt(String key, int defaultValue) {
		int returnResult = defaultValue;
		if (mPreferences != null) {
			returnResult = mPreferences.getInt(key, defaultValue);
		} else if (mXPreferences != null) {
			returnResult = mXPreferences.getInt(key, defaultValue);
		}
		return returnResult;
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		boolean returnResult = defaultValue;
		if (mPreferences != null) {
			returnResult = mPreferences.getBoolean(key, defaultValue);
		} else if (mXPreferences != null) {
			returnResult = mXPreferences.getBoolean(key, defaultValue);
		}
		return returnResult;
	}

	public Set<String> getStringSet(String key, Set<String> defaultValue) {
		Set<String> returnResult = defaultValue;
		if (mPreferences != null) {
			returnResult = mPreferences.getStringSet(key, defaultValue);
		} else if (mXPreferences != null) {
			returnResult = mXPreferences.getStringSet(key, defaultValue);
		}
		return returnResult;
	}

	public boolean contains(String key) {
		if (mPreferences != null)
			return mPreferences.contains(key);
		else if (mXPreferences != null)
			return mXPreferences.contains(key);

		return false;
	}

	public void setPasscode(ArrayList<Integer> passcode) {
		String string = "";
		for (int i = 0; i < passcode.size(); i++) {
			string += String.valueOf(passcode.get(i));
			if (i != passcode.size()-1) {
				string += ",";
			}
		}

		edit().putString("passcode", string).commit();
		emitSettingsChanged(mContext);
	}

	public ArrayList<Integer> getPasscodeOrNull() {
		ArrayList<Integer> passcode = new ArrayList<Integer>();
		String string = getString("passcode", null);
		if (string == null)
			return null;

		String[] integers = string.split(",");
		for (String digitString : integers) {
			passcode.add(Integer.parseInt(digitString));
		}
		return passcode;
	}

	public ArrayList<Integer> getPasscode() {
		ArrayList<Integer> passcode = new ArrayList<Integer>();
		String string = getString("passcode", "1,2,3,4");
		String[] integers = string.split(",");
		for (String digitString : integers) {
			passcode.add(Integer.parseInt(digitString));
		}
		return passcode;
	}

	public boolean shouldDrawLines() {
		return getBoolean("should_draw_lines", true);
	}

	public void setShouldDrawLines(boolean draw) {
		edit().putBoolean("should_draw_lines", draw).commit();
		emitSettingsChanged(mContext);
	}

	public boolean shouldShowText() {return getBoolean("should_show_text", true);}

	public boolean shouldDrawFill() {
		return getBoolean("should_draw_fill", true);
	}

	public boolean shouldDisableDialog() {return !getBoolean("should_show_dialog", true); }

	public boolean hideEmergencyButton() {return !getBoolean("show_emergency_button", true); }

	public boolean hideEmergencyText() {return !getBoolean("show_emergency_text", true); }

	public boolean vibrateOnLongPress() {return getBoolean("vibrate_long_press", true);}

	public boolean vibrateOnTap() {return getBoolean("vibrate_tap", false); }

	public boolean failSafe() {return getBoolean("fail_safe", true); }

	public boolean isDisabled() { return !getBoolean("switch", false);	}

	public boolean showDots() {return getBoolean("show_dots", true); }

	public void setShouldDrawFill(boolean draw) {
		edit().putBoolean("should_draw_fill", draw).commit();
		emitSettingsChanged(mContext);
	}

	public void putInt(String key, int value) {
		edit().putInt(key, value).commit();
		emitSettingsChanged(mContext);
	}

	public Grid getPatternSize() {
		int columns,rows;
		columns = Integer.parseInt(getString("pattern_size_columns", "2"));
		rows = Integer.parseInt(getString("pattern_size_rows", "2"));
		return new Grid(columns,rows);
	}

	public void storePatternSize(Grid g) {
		edit().putString("pattern_size_columns", "" + g.numberOfColumns).commit();
		edit().putString("pattern_size_rows", "" + g.numberOfRows).commit();
		emitSettingsChanged(mContext);
	}

	private static class killPackage extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
			// this method is executed in a background thread
			// no problem calling su here
			String packageToKill = params[0];
			Process su = null;
			// get superuser
			try {
				su = Runtime.getRuntime().exec("su");
			} catch (IOException e) {
				e.printStackTrace();
			}

			// kill given package
			if (su != null ){
				try {
					DataOutputStream os = new DataOutputStream(su.getOutputStream());
					os.writeBytes("pkill " + packageToKill + "\n");
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

	public static void killPackage(String packageToKill) {
		(new killPackage()).execute(packageToKill);
	}
}
