package me.rijul.knockcode;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.provider.Settings.Secure;

import de.robv.android.xposed.XSharedPreferences;

public class SettingsHelper {
    //these are for xposed
    private XSharedPreferences mXPreferences = null;
    //this is to be stored for when xposed asks to reload
    private String mUuid;
    //these are xposed versions of secure preferences
    private SecurePreferences mXSecurePreferences = null;

    //these are normal preferences, to be accessed via our app, they are automatically secure
    private SecurePreferences mSecurePreferences = null;

    //Xposed Constructor
    public SettingsHelper(String uuid) {
        mUuid = uuid;

        mXPreferences = new XSharedPreferences(BuildConfig.APPLICATION_ID);
        mXPreferences.makeWorldReadable();

        mXSecurePreferences = new SecurePreferences(mXPreferences, uuid);
    }

    public SettingsHelper(Context context) {
        mSecurePreferences = getWritablePreferences(context, Utils.PREFERENCES_FILE);
    }

    //Preferences related stuff
    public void reloadSettings() {
        mXPreferences.reload();

        mXSecurePreferences = new SecurePreferences(mXPreferences, mUuid);
    }

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    public static SecurePreferences getWritablePreferences(Context context, String fileName) {
        return new SecurePreferences(context.getSharedPreferences(fileName, Context.MODE_WORLD_READABLE),
                Secure.getString(context.getContentResolver(), Secure.ANDROID_ID));
    }

    public static SecurePreferences getWritablePreferences(Context context) {
        return getWritablePreferences(context, Utils.PREFERENCES_FILE);
    }

    public Editor edit() {
        return mSecurePreferences.edit();
    }

    public boolean contains(String key) {
        if (mSecurePreferences != null)
            return mSecurePreferences.contains(key);
        else if (mXPreferences != null)
            return mXPreferences.contains(key);
        return false;
    }

    public Map<String, ?> getAll() {
        if (mSecurePreferences != null) {
            return mSecurePreferences.getAll();
        } else if (mXSecurePreferences != null) {
            return mXSecurePreferences.getAll();
        }
        return null;
    }

    public boolean hasShortcut(String passcode) {
        return contains(Utils.PREFIX_SHORTCUT + passcode);
    }

    public boolean hasShortcut(ArrayList<Integer> passcode) {
        return hasShortcut(Utils.passcodeToString(passcode));
    }

    //Preferences getters - generic
    public String getString(String key, String defaultValue) {
        String returnResult = defaultValue;
        if (mSecurePreferences != null) {
            returnResult = mSecurePreferences.getString(key, defaultValue);
        } else if (mXSecurePreferences != null) {
            returnResult = mXSecurePreferences.getString(key, defaultValue);
        }
        return returnResult;
    }

    public int getInt(String key, int defaultValue) {
        int returnResult = defaultValue;
        if (mSecurePreferences != null) {
            returnResult = mSecurePreferences.getInt(key, defaultValue);
        } else if (mXSecurePreferences != null) {
            returnResult = mXSecurePreferences.getInt(key, defaultValue);
        }
        return returnResult;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        boolean returnResult = defaultValue;
        if (mSecurePreferences != null) {
            returnResult = mSecurePreferences.getBoolean(key, defaultValue);
        } else if (mXSecurePreferences != null) {
            returnResult = mXSecurePreferences.getBoolean(key, defaultValue);
        }
        return returnResult;
    }

    public float getFloat(String key, float defaultValue) {
        float returnResult = defaultValue;
        if (mSecurePreferences != null) {
            returnResult = mSecurePreferences.getFloat(key, defaultValue);
        } else if (mXSecurePreferences != null) {
            returnResult = mXSecurePreferences.getFloat(key, defaultValue);
        }
        return returnResult;
    }

    public String getShortcut(String key) {
        return getString(Utils.PREFIX_SHORTCUT + key, null);
    }

    public String getShortcut(ArrayList<Integer> passcode) {
        return getShortcut(Utils.passcodeToString(passcode));
    }

    public Set<String> getStringSet(String key, Set<String> defaultValue) {
        Set<String> returnResult = defaultValue;
        if (mSecurePreferences != null) {
            returnResult = mSecurePreferences.getStringSet(key, defaultValue);
        } else if (mXSecurePreferences != null) {
            returnResult = mXSecurePreferences.getStringSet(key, defaultValue);
        }
        return returnResult;
    }

    //Preferences getters - specific
    public ArrayList<Integer> getPasscodeOrNull() {
        String string = getString(Utils.SETTINGS_PASSCODE, null);
        if (string == null)
            return null;
        return Utils.stringToPasscode(string);
    }

    public int lastInstalledVersion() {return getInt(Utils.LAST_VERSION, 0);}
    public void saveThisVersion() {edit().putInt(Utils.LAST_VERSION, BuildConfig.VERSION_CODE).apply();}
    public boolean shouldShowCustomShortcutInstructions()
        {return getBoolean(Utils.SHOW_CUSTOM_SHORTCUT_INSTRUCTIONS, true);}

    public boolean isVirgin() {return  getPasscodeOrNull()==null;}
    public boolean isSwitchOff() {return !getBoolean(Utils.SETTINGS_SWITCH, false);}
    public boolean isDisabled() {return isVirgin() || isSwitchOff();}
    public boolean failSafe() {return getBoolean(Utils.SETTINGS_FAILSAFE, true);}

    public boolean customShortcutsDontUnlock() {return getBoolean(Utils.SETTINGS_CUSTOM_SHORTCUTS_DONT_UNLOCK, false);}

    //these are done via hooks, so they will check if enabled

    //fullscreen is stored unencrypted
    public boolean fullScreen() {return getBoolean(Utils.SETTINGS_CODE_FULLSCREEN, false);}
    public boolean showDialog() {return getBoolean(Utils.SETTINGS_CODE_DIALOG, true) && !isDisabled();}
    public XposedMod.UnlockPolicy getPolicy() {
        return XposedMod.UnlockPolicy.valueOf(getString(Utils.SETTINGS_CODE_DIRECT_ENTRY_POLICY, "NEVER"));
    }
    public boolean forceNone() {return getBoolean(Utils.SETTINGS_CODE_FORCE_NONE, false) && !isDisabled();}

    //these are done after making sure that enabled is on, so don't give a shit
    public boolean showBackground() {return getBoolean(Utils.SETTINGS_CODE_BACKGROUND, false);}
    public int getBackgroundColor() {return getInt(Utils.SETTINGS_CODE_BACKGROUND_COLOR, 0x4C000000);}
    public boolean showButtonTaps() {return getBoolean(Utils.SETTINGS_CODE_TAPS_VISIBLE, true);}
    public boolean borderlessTaps() {return getBoolean(Utils.SETTINGS_CODE_TAPS_VISIBLE_BORDERLESS, false);}

    public int getTextColor() {return getInt(Utils.SETTINGS_CODE_TEXT_COLOR, 0xFFFAFAFA);}
    public boolean showReadyText() {return getBoolean(Utils.SETTINGS_CODE_TEXT_READY, true);}
    public String getReadyText() {return getString(Utils.SETTINGS_CODE_TEXT_READY_VALUE, "");}
    public boolean showCorrectText() {return getBoolean(Utils.SETTINGS_CODE_TEXT_CORRECT, true);}
    public String getCorrectText() {return getString(Utils.SETTINGS_CODE_TEXT_CORRECT_VALUE, "");}
    public boolean showErrorText() {return getBoolean(Utils.SETTINGS_CODE_TEXT_ERROR, true);}
    public String getErrorText() {return getString(Utils.SETTINGS_CODE_TEXT_ERROR_VALUE, "");}
    public boolean showResetText() {return getBoolean(Utils.SETTINGS_CODE_TEXT_RESET, true);}
    public String getResetText() {return getString(Utils.SETTINGS_CODE_TEXT_RESET_VALUE, "");}
    public boolean showDisabledText() {return getBoolean(Utils.SETTINGS_CODE_TEXT_DISABLED, true);}
    public String getDisabledText() {return getString(Utils.SETTINGS_CODE_TEXT_DISABLED_VALUE, "");}

    public boolean showLines() {return getBoolean(Utils.SETTINGS_CODE_LINES_VISIBLE, true);}
    public int getLinesReadyColor() {return getInt(Utils.SETTINGS_CODE_LINES_COLOR_READY, 0xFFFAFAFA);}
    public boolean showLinesCorrect() {return getBoolean(Utils.SETTINGS_CODE_LINES_CORRECT, true);}
    public int getLinesCorrectColor() {return getInt(Utils.SETTINGS_CODE_LINES_COLOR_CORRECT, 0xFF4CAF50);}
    public boolean showLinesError() {return getBoolean(Utils.SETTINGS_CODE_LINES_ERROR, true);}
    public int getLinesErrorColor() {return getInt(Utils.SETTINGS_CODE_LINES_COLOR_ERROR, 0xFff44336);}
    public boolean showLinesDisabled() {return getBoolean(Utils.SETTINGS_CODE_LINES_DISABLED, true);}
    public int getLinesDisabledColor() {return getInt(Utils.SETTINGS_CODE_LINES_COLOR_DISABLED, Color.DKGRAY);}

    public boolean showDots() {return getBoolean(Utils.SETTINGS_CODE_DOTS_VISIBLE, true);}
    public int getDotsReadyColor() {return getInt(Utils.SETTINGS_CODE_DOTS_COLOR_READY, 0xFFFAFAFA);}
    public boolean showDotsCorrect() {return getBoolean(Utils.SETTINGS_CODE_DOTS_CORRECT, false);}
    public int getDotsCorrectColor() {return getInt(Utils.SETTINGS_CODE_DOTS_COLOR_CORRECT, 0xFF4CAF50);}
    public boolean showDotsError() {return getBoolean(Utils.SETTINGS_CODE_DOTS_ERROR, false);}

    public boolean showEmergencyButton() {return getBoolean(Utils.SETTINGS_EMERGENCY_BUTTON, true);}
    public boolean showEmergencyText() {return getBoolean(Utils.SETTINGS_EMERGENCY_TEXT, true);}
    public boolean showEmergencyBackground() {return getBoolean(Utils.SETTINGS_EMERGENCY_BACKGROUND, true);}

    public boolean vibrateOnTap() {return getBoolean(Utils.SETTINGS_VIBRATE_TAP, false);}
    public boolean vibrateOnLongPress() {return getBoolean(Utils.SETTINGS_VIBRATE_LONG_PRESS, true);}
    public boolean vibrateOnError() {return getBoolean(Utils.SETTINGS_VIBRATE_ERROR, false);}

    public int correctDuration() {return getInt(Utils.SETTINGS_CORRECT_DURATION, 50);}
    public int errorDuration() {return getInt(Utils.SETTINGS_ERROR_DURATION, 400);}
    public boolean waitForLastDot() {return getBoolean(Utils.SETTINGS_CODE_WAIT_LAST_DOT, true);}
    public int appearDuration() {return getInt(Utils.SETTINGS_APPEAR_DURATION, 500);}
    public int disappearDuration() {return getInt(Utils.SETTINGS_DISAPPEAR_DURATION, 300);}

    public Grid getPatternSize() {
        int columns,rows;
        columns = Integer.parseInt(getString(Utils.SETTINGS_CODE_SIZE_COLUMNS, "2"));
        rows = Integer.parseInt(getString(Utils.SETTINGS_CODE_SIZE_ROWS, "2"));
        return new Grid(columns,rows);
    }

    //preferences putters - generic
    public void putString(String key, String value) {edit().putString(key, value).apply();}
    public void putBoolean(String key, boolean value) {edit().putBoolean(key, value).apply();}

    //preference putters - specific
    public void setPasscode(ArrayList<Integer> passcode) {
        putString(Utils.SETTINGS_PASSCODE, Utils.passcodeToString(passcode));
    }
    public void storePatternSize(Grid g) {
        putString(Utils.SETTINGS_CODE_SIZE_COLUMNS, String.valueOf(g.numberOfColumns));
        putString(Utils.SETTINGS_CODE_SIZE_ROWS, String.valueOf(g.numberOfRows));
    }
    public void putShortcut(String passcode, String target) {
        putString(Utils.PREFIX_SHORTCUT + passcode, target);
    }
    public void putShortcut(ArrayList<Integer> passcode, String uri, String name, boolean unlock) {
        putShortcut(Utils.passcodeToString(passcode), uri + "|" + name + "|" + String.valueOf(unlock));
    }

    public void remove(String key) {edit().remove(key).apply();}
    public void removeShortcut(String passcode) {remove(Utils.PREFIX_SHORTCUT + passcode);}
    public void removeShortcut(ArrayList<Integer> passcode) {removeShortcut(Utils.passcodeToString(passcode));}
}
