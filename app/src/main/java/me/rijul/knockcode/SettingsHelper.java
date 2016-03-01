package me.rijul.knockcode;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.provider.Settings.Secure;
import de.robv.android.xposed.XSharedPreferences;

public class SettingsHelper {
	public static final String PACKAGE_NAME = BuildConfig.APPLICATION_ID;
	private static final String PREFS_NAME = PACKAGE_NAME + "_preferences";
	private XSharedPreferences mXPreferencesImpl = null;
	private SecurePreferences mXPreferences = null;
	private static SecurePreferences mPreferences = null;
	private Context mContext;
	private String mUuid;

	//Class Related stuff
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

	//Preferences related stuff
	public void reloadSettings() {
		mXPreferencesImpl.reload();
		if (mUuid!=null)
			mXPreferences = new SecurePreferences(mXPreferencesImpl, mUuid);
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

	public Editor edit() {
		return mPreferences.edit();
	}

	public Map<String, ?> getAll() {
		if (mPreferences != null) {
			return mPreferences.getAll();
		} else if (mXPreferences != null) {
			return mXPreferences.getAll();
		}
		return null;
	}

	public boolean contains(String key) {
		if (mPreferences != null)
			return mPreferences.contains(key);
		else if (mXPreferences != null)
			return mXPreferences.contains(key);

		return false;
	}

	//Preferences getters - generic
	public String getString(String key, String defaultValue) {
		String returnResult = defaultValue;
		if (mPreferences != null) {
			returnResult = mPreferences.getString(key, defaultValue);
		} else if (mXPreferences != null) {
			returnResult = mXPreferences.getString(key, defaultValue);
		}
		return returnResult;
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

	//Preferences getters - specific
	public ArrayList<Integer> getPasscodeOrNull() {
		String string = getString("passcode", null);
		if (string == null)
			return null;
		return Utils.stringToPasscode(string);
	}

	public ArrayList<Integer> getPasscode() {
		String string = getString("passcode", "1,2,3,4");
		return Utils.stringToPasscode(string);
	}

	public boolean shouldDrawLines() {return getBoolean("should_draw_lines", true);}
	public boolean shouldShowText() {return getBoolean("should_show_text", true);}
	public boolean shouldDrawFill() {return getBoolean("should_draw_fill", true);}
	public boolean shouldDisableDialog() {return !getBoolean("should_show_dialog", true);}
	public boolean hideEmergencyButton() {return !getBoolean("show_emergency_button", true);}
	public boolean hideEmergencyText() {return !getBoolean("show_emergency_text", true);}
	public boolean vibrateOnLongPress() {return getBoolean("vibrate_long_press", true);}
	public boolean vibrateOnTap() {return getBoolean("vibrate_tap", false);}
	public boolean failSafe() {return getBoolean("fail_safe", true);}
	public boolean isDisabled() {return getPasscodeOrNull() == null || isSwitchOff();}
	public boolean isSwitchOff() {return !getBoolean("switch", false);}
	public boolean showDots() {return getBoolean("show_dots", true); }

	public Grid getPatternSize() {
		int columns,rows;
		columns = Integer.parseInt(getString("pattern_size_columns", "2"));
		rows = Integer.parseInt(getString("pattern_size_rows", "2"));
		return new Grid(columns,rows);
	}

	//preferences putters - generic
	public void putInt(String key, int value) {edit().putInt(key, value).apply();}
	public void putString(String key, String value) {edit().putString(key, value).apply();}
	public void putFloat(String key, float value) {edit().putFloat(key, value).apply();}
	public void putBoolean(String key, boolean value) {edit().putBoolean(key, value).apply();}
	public void putStringSet(String key, Set<String> value) {edit().putStringSet(key, value).apply();}

	//preference putters - specific
	public void setPasscode(ArrayList<Integer> passcode) {putString("passcode", Utils.passcodeToString(passcode));}
	public void storePatternSize(Grid g) {
		putString("pattern_size_columns", "" + g.numberOfColumns);
		putString("pattern_size_rows", "" + g.numberOfRows);
	}
	public void storeShortcut(Shortcut shortcut) {
		putString("uri_" + shortcut.passCode, shortcut.uri + "|" + shortcut.friendlyName);
	}

	public void remove(String key) {edit().remove(key).apply();}

}
