package me.rijul.knockcode;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceFragment;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by rijul on 2/3/16.
 */
public class MainActivity extends Activity {
    public static final int CHANGE_CODE = 1, NEW_SHORTCUT = 2;
    private static SettingsHelper mSettingsHelper;
    private static ArrayList<Integer> mPasscode;
    private static ProgressDialog mProgressDialog;
    private static Vibrator mVibrator;

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return SettingsHelper.getWritablePreferences(getApplicationContext());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mSettingsHelper = new SettingsHelper(this);
        mPasscode = mSettingsHelper.getPasscodeOrNull();
        mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (getCallingActivity()==null) {
            if (mPasscode == null) {
                startActivity(new Intent(this, SettingsActivity.class));
                CustomLogger.log(this, "MainActivity", "App", "First run", null, -1);
                finish();
            } else {
                CustomLogger.log(this, "MainActivity", "App", "Default run", null, -1);
                getFragmentManager().beginTransaction().replace(R.id.fragment_container, new MainFragment()).commit();
            }
        } else {
            Bundle bundle = getIntent().getExtras();
            MainFragment fragment = new MainFragment();
            fragment.setArguments(bundle);
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
            CustomLogger.log(this, "MainActivity", "App", "Called for result", null, Integer.parseInt(bundle.getString("requestCode")));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (getCallingActivity()==null) {
                    finish();
                }
                else {
                    Intent returnIntent = getIntent();
                    setResult(Activity.RESULT_CANCELED, returnIntent);
                    finish();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class MainFragment extends PreferenceFragment implements LockButtonView.OnPositionTappedListener, View.OnClickListener,
            View.OnLongClickListener {
        private LockButtonView mLockButtonView;
        private DotsView mDotsView;
        private int mRequestCode;
        private String mUri, mName, mOriginalPasscode;
        private boolean mConfirmationMode = false;
        private boolean mUnlockOnLaunch;
        private ArrayList<Integer> mFirstTappedPositions = new ArrayList<>(), mSecondTappedPositions = new ArrayList<>();
        private Runnable mFinishRunnable = new Runnable() {
            @Override
            public void run() {
                if (mRequestCode==-1) {
                    startActivity(new Intent(getActivity(), SettingsActivity.class));
                } else {
                    getActivity().setResult(MainActivity.RESULT_OK, getActivity().getIntent());
                    getActivity().sendBroadcast(new Intent(Utils.SETTINGS_CHANGED));
                }
                getActivity().finish();
            }
        };
        private Runnable mFailRunnable = new Runnable() {
            @Override
            public void run() {
                mLockButtonView.enableButtons(true);
                mLockButtonView.setMode(LockButtonView.Mode.Ready);
                mDotsView.setPaintColor(Utils.getOwnResources(getActivity()).getColor(R.color.textColorPrimary));
                reset();
            }
        };
        private int maxSize = LockButtonView.KNOCK_CODE_MAX_SIZE;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_main, container, false);
            mLockButtonView = (LockButtonView) rootView.findViewById(R.id.lock_button_view);
            mLockButtonView.setPatternSize(mSettingsHelper.getPatternSize());
            mDotsView = (DotsView) rootView.findViewById(R.id.dots_view);
            mLockButtonView.setOnPositionTappedListener(this);
            mLockButtonView.setOnLongClickListener(this);
            rootView.findViewById(R.id.next_button).setOnClickListener(this);
            rootView.findViewById(R.id.next_button).setEnabled(false);
            rootView.findViewById(R.id.retry_button).setOnClickListener(this);
            rootView.findViewById(R.id.retry_button).setEnabled(false);
            try {
                mRequestCode = Integer.parseInt(getArguments().getString("requestCode"));
            } catch (NullPointerException e) {
                mRequestCode = -1;
            }
            //Activity or Fragment is started to login into the app
            if (mRequestCode==-1) {
                ((TextView) rootView.findViewById(android.R.id.hint)).setText(R.string.knock_enter_code);
                rootView.findViewById(R.id.bottom_buttons).setVisibility(View.GONE);
            } else {
                ActionBar actionBar = getActivity().getActionBar();
                actionBar.setDisplayHomeAsUpEnabled(true);
                if (mRequestCode==MainActivity.CHANGE_CODE) {
                    ((TextView) rootView.findViewById(android.R.id.hint)).setText(R.string.knock_enter_new_code);
                    getNewPatternSize();
                } else {
                    Bundle bundle = getArguments();
                    mUri = bundle.getString("uri");
                    mName = bundle.getString("name");
                    mOriginalPasscode = bundle.getString("passcode");
                    mUnlockOnLaunch = bundle.getBoolean("unlockOnLaunch", true);
                    ((TextView) rootView.findViewById(android.R.id.hint)).setText(mName);
                    maxSize = mPasscode.size() + 1;
                }
            }
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    getActivity().onBackPressed();
                }
            });
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage(getString(R.string.loading));
            return rootView;
        }

        private void getNewPatternSize() {
            ViewGroup numberPickerView = (ViewGroup) LayoutInflater.from(getActivity()).inflate(R.layout.grid_number_picker, null);
            final NumberPicker npr,npc;
            Grid patternSize = mLockButtonView.getPatternSize();
            if (numberPickerView!=null) {
                npr = (NumberPicker) numberPickerView.findViewById(R.id.numberPickerX);
                if (npr!=null) {
                    npr.setMinValue(LockButtonView.GRID_MIN_SIZE);
                    npr.setMaxValue(LockButtonView.GRID_MAX_SIZE);
                    npr.setValue(patternSize.numberOfRows);
                }

                npc = (NumberPicker) numberPickerView.findViewById(R.id.numberPickerY);
                if (npc!=null) {
                    npc.setMinValue(LockButtonView.GRID_MIN_SIZE);
                    npc.setMaxValue(LockButtonView.GRID_MAX_SIZE);
                    npc.setValue(patternSize.numberOfColumns);
                }

                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.main_select_grid_size)
                        .setView(numberPickerView)
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                getActivity().onBackPressed();
                            }
                        })
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mLockButtonView.setPatternSize(new Grid(npc.getValue(), npr.getValue()));
                                dialog.dismiss();
                            }
                        })
                        .create()
                        .show();
            }
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.retry_button:
                    reset();
                    break;
                case R.id.next_button:
                    if (!mConfirmationMode) {
                        if (mFirstTappedPositions.isEmpty())
                            return;
                        if (mRequestCode==MainActivity.NEW_SHORTCUT) {
                            (new CheckPasswordValid()).execute(Utils.passcodeToString(mFirstTappedPositions));
                        } else if (mRequestCode==MainActivity.CHANGE_CODE) {
                            mConfirmationMode = true;
                            getView().findViewById(R.id.retry_button).setEnabled(true);
                            getView().findViewById(R.id.next_button).setEnabled(false);
                            Toast.makeText(getActivity(), R.string.main_shortcuts_will_delete, Toast.LENGTH_SHORT).show();
                            ((TextView) getView().findViewById(android.R.id.hint)).setText(R.string.knock_confirm_code);
                            mDotsView.reset(true);
                            maxSize = mFirstTappedPositions.size() + 1;
                            mLockButtonView.clearCode();
                        }
                    } else {
                        if (mSecondTappedPositions.isEmpty())
                            return;
                        if (mRequestCode==MainActivity.CHANGE_CODE) {
                            if (mFirstTappedPositions.equals(mSecondTappedPositions)) {
                                ((TextView) getView().findViewById(android.R.id.hint)).setText(R.string.knock_match);
                                mLockButtonView.setMode(LockButtonView.Mode.Correct);
                                mLockButtonView.enableButtons(false);
                                mDotsView.animateBetween(Utils.getOwnResources(getActivity()).getColor(R.color.textColorPrimary),
                                        Utils.getOwnResources(getActivity()).getColor(R.color.colorCorrect), new Runnable() {
                                            @Override
                                            public void run() {
                                                deleteShortcuts();
                                            }
                                        }, 50, true, true);
                            } else {
                                ((TextView) getView().findViewById(android.R.id.hint)).setText(R.string.knock_no_match);
                                mLockButtonView.setMode(LockButtonView.Mode.Incorrect);
                                mLockButtonView.enableButtons(false);
                                mDotsView.shake(mFailRunnable, 400, true);
                                if (mVibrator.hasVibrator())
                                    mVibrator.vibrate(400L);
                            }
                        } else {
                            if (mFirstTappedPositions.equals(mSecondTappedPositions)) {
                                ((TextView) getView().findViewById(android.R.id.hint)).setText(R.string.knock_match);
                                mLockButtonView.setMode(LockButtonView.Mode.Correct);
                                mLockButtonView.enableButtons(false);
                                mDotsView.animateBetween(Utils.getOwnResources(getActivity()).getColor(R.color.textColorPrimary),
                                        Utils.getOwnResources(getActivity()).getColor(R.color.colorCorrect), mFinishRunnable,
                                        50, true, true);
                                mSettingsHelper.putShortcut(mFirstTappedPositions, mUri, mName, mUnlockOnLaunch);
                                if (mOriginalPasscode!=null)
                                    mSettingsHelper.removeShortcut(mOriginalPasscode);
                            } else {
                                mLockButtonView.setMode(LockButtonView.Mode.Incorrect);
                                ((TextView) getView().findViewById(android.R.id.hint)).setText(R.string.knock_no_match);
                                mLockButtonView.enableButtons(false);
                                if (mVibrator.hasVibrator())
                                    mVibrator.vibrate(400L);
                                mDotsView.shake(mFailRunnable, 400, true);
                            }
                        }
                    }
            }
        }

        private void deleteShortcuts() {
            new DeleteAllShortcuts().execute();
        }

        public class DeleteAllShortcuts extends AsyncTask<Void, Void, Void> {
            @Override
            protected void onPreExecute() {
                mProgressDialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                SharedPreferences.Editor editor = mSettingsHelper.edit();
                Map<String, ?> allEntries = mSettingsHelper.getAll();
                for(Map.Entry<String, ?> entry : allEntries.entrySet())
                    try {
                        if ((entry.getKey()!=null) && (entry.getKey().startsWith(Utils.PREFIX_SHORTCUT))) {
                            editor.remove(entry.getKey());
                        }
                    } catch (NullPointerException ignored) {
                    }
                editor.apply();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                mSettingsHelper.setPasscode(mFirstTappedPositions);
                mSettingsHelper.storePatternSize(mLockButtonView.getPatternSize());
                mProgressDialog.dismiss();
                mFinishRunnable.run();
            }
        }


        private void reset() {
            if (mConfirmationMode) {
                //Second gesture entry is not done, so reset to first
                if (mSecondTappedPositions.isEmpty()) {
                    mConfirmationMode = false;
                    resetToFirst();
                    return;
                }
                else if (mRequestCode==MainActivity.CHANGE_CODE)
                    ((TextView) getView().findViewById(android.R.id.hint)).setText(R.string.knock_confirm_code);
                else
                    ((TextView) getView().findViewById(android.R.id.hint)).setText(getString(R.string.knock_code_confirm_shortcut, mName));
                mSecondTappedPositions.clear();
                mLockButtonView.clearCode();
                mDotsView.reset(true);
            }
            else
                resetToFirst();
            getView().findViewById(R.id.next_button).setEnabled(false);
        }

        private void resetToFirst() {
            if (mRequestCode==MainActivity.CHANGE_CODE)
                ((TextView) getView().findViewById(android.R.id.hint)).setText(R.string.knock_enter_new_code);
            else if (mRequestCode==MainActivity.NEW_SHORTCUT)
                ((TextView) getView().findViewById(android.R.id.hint)).setText(mName);
            else if (mRequestCode==-1)
                ((TextView) getView().findViewById(android.R.id.hint)).setText(R.string.knock_enter_code);
            getView().findViewById(R.id.retry_button).setEnabled(false);
            mFirstTappedPositions.clear();
            mLockButtonView.clearCode();
            mDotsView.reset(true);
        }

        @Override
        public void onPositionTapped(Button button, ArrayList<Integer> code) {
            if (mRequestCode==-1) {
                mFirstTappedPositions = (ArrayList<Integer>) code.clone();
                mDotsView.append();
                if (mFirstTappedPositions.size()==mPasscode.size()) {
                    if (mFirstTappedPositions.equals(mPasscode)) {
                        mLockButtonView.enableButtons(false);
                        ((TextView) getView().findViewById(android.R.id.hint)).setText(R.string.knock_code_correct);
                        mDotsView.animateBetween(Utils.getOwnResources(getActivity()).getColor(R.color.textColorPrimary),
                                Utils.getOwnResources(getActivity()).getColor(R.color.colorCorrect), mFinishRunnable,
                                50, true, true);
                        mLockButtonView.setMode(LockButtonView.Mode.Correct);
                    } else {
                        mLockButtonView.setMode(LockButtonView.Mode.Incorrect);
                        mLockButtonView.enableButtons(false);
                        ((TextView) getView().findViewById(android.R.id.hint)).setText(R.string.knock_code_wrong);
                        if (mVibrator.hasVibrator())
                            mVibrator.vibrate(400L);
                        mDotsView.shake(mFailRunnable, 400, true);
                    }
                }
                return;
            }
            getView().findViewById(R.id.retry_button).setEnabled(true);
            if (!mConfirmationMode) {
                mFirstTappedPositions = (ArrayList<Integer>) code.clone();
                mDotsView.append();
                if (mFirstTappedPositions.size() == maxSize) {
                    Toast.makeText(getActivity(), R.string.main_too_many_taps, Toast.LENGTH_SHORT).show();
                    reset();
                    return;
                }
                if (mFirstTappedPositions.size() == LockButtonView.KNOCK_CODE_MIN_SIZE)
                    getView().findViewById(R.id.next_button).setEnabled(true);
            } else {
                mSecondTappedPositions = (ArrayList<Integer>) code.clone();
                mDotsView.append();
                if (mSecondTappedPositions.size() == maxSize) {
                    Toast.makeText(getActivity(), R.string.main_too_many_taps, Toast.LENGTH_SHORT).show();
                    reset();
                    return;
                }
                if (mSecondTappedPositions.size() == LockButtonView.KNOCK_CODE_MIN_SIZE)
                    getView().findViewById(R.id.next_button).setEnabled(true);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            reset();
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            return true;
        }

        public class CheckPasswordValid extends AsyncTask<String, Void, Void> {
            String mResult = null;

            @Override
            protected void onPreExecute() {
                mProgressDialog.show();
            }

            @Override
            protected Void doInBackground(String... params) {
                String newPassword = params[0];
                Map<String, ?> allEntries = mSettingsHelper.getAll();
                String passcode = Utils.passcodeToString(mPasscode);
                if (passcode.startsWith(newPassword)) {
                    mResult = "master password";
                    return null;
                }
                for(Map.Entry<String, ?> entry : allEntries.entrySet())
                    try {
                        String oldPassword = entry.getKey();
                        if (oldPassword.startsWith(Utils.PREFIX_SHORTCUT)) {
                            oldPassword = oldPassword.substring(9);
                            if (oldPassword.equals(newPassword))
                                mResult = ((String) entry.getValue()).split("\\|")[1];
                            else if (newPassword.startsWith(oldPassword))
                                mResult = ((String) entry.getValue()).split("\\|")[1];
                            else if (oldPassword.startsWith(newPassword))
                                mResult = ((String) entry.getValue()).split("\\|")[1];
                            if (mResult != null)
                                break;
                        }
                    }
                    catch (NullPointerException ignored) {}
                return null;
            }

            @Override
            protected void onPostExecute(Void result2) {
                allowNext(mResult);
            }
        }

        private void allowNext(String result) {
            mProgressDialog.dismiss();
            if (result==null) {
                mConfirmationMode = true;
                ((TextView) getView().findViewById(android.R.id.hint)).setText(getString(R.string.knock_code_confirm_shortcut, mName));
                getView().findViewById(R.id.next_button).setEnabled(false);
                getView().findViewById(R.id.retry_button).setEnabled(true);
                mDotsView.reset(true);
                mLockButtonView.clearCode();
            } else {
                Toast.makeText(getActivity(), getString(R.string.main_conflict_shortcut, result), Toast.LENGTH_SHORT).show();
                reset();
            }
        }
    }
}