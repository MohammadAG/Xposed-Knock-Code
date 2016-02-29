package me.rijul.knockcode;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import me.rijul.knockcode.KnockCodeButtonView.OnPositionTappedListener;

/**
 * Created by rijul on 29/2/16.
 */
public class MainActivity extends AppCompatActivity implements OnPositionTappedListener, OnClickListener,
        DeleteAllShortcuts.AsyncResponse, View.OnLongClickListener, CheckPasswordValid.AsyncResponse {
    public static int KNOCK_CODE_MAX_SIZE = 20;
    public static final int KNOCK_CODE_MIN_SIZE = 3;
    public static final int GRID_MAX_SIZE = 5;
    public static final int GRID_MIN_SIZE = 2;
    public static final int SET_NEW_PASSCODE = 1;
    public static final int GET_A_CODE = 2;

    private KnockCodeButtonView mKnockCodeView;
    private PasswordTextView mPasscodeDotView;
    private TextView mHintTextView;

    private ArrayList<Integer> mFirstTappedPositions = new ArrayList<>();
    private ArrayList<Integer> mSecondTappedPositions = new ArrayList<>();

    private boolean mIsOldCode;
    private SettingsHelper mSettingsHelper;
    private int requestCode;
    private boolean mIsConfirmationMode = false;

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_passcode);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSettingsHelper = new SettingsHelper(MainActivity.this);
        mKnockCodeView = (KnockCodeButtonView) findViewById(R.id.knockCodeView1);
        mKnockCodeView.setOnLongClickListener(this);
        mHintTextView = (TextView) findViewById(android.R.id.hint);

        if (getCallingActivity()==null) {
            requestCode = -1;
            mIsOldCode = (mSettingsHelper.getPasscodeOrNull() != null);
            if (!mIsOldCode) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                finish();
            }
            mHintTextView.setText(R.string.enter_previous_knock_code);
        } else {
            mIsOldCode = false;
            Intent intent = getIntent();
            requestCode = Integer.parseInt(intent.getStringExtra("requestCode"));
            ActionBar actionBar = getSupportActionBar();
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            if (requestCode==SET_NEW_PASSCODE) {
                mHintTextView.setText(R.string.enter_new_knock_code);
                getNewPatternSize();
            } else if (requestCode==GET_A_CODE) {
                mHintTextView.setText(R.string.enter_new_knock_code);
                KNOCK_CODE_MAX_SIZE = mSettingsHelper.getPasscode().size()+1;
            } else throw new IllegalArgumentException("Invalid request code!");
        }

        mKnockCodeView.setOnPositionTappedListener(this);

        mPasscodeDotView = (PasswordTextView) findViewById(R.id.passcodeDotView1);
        mPasscodeDotView.setPaintColor(Color.BLACK);

        findViewById(R.id.retry_button).setOnClickListener(this);
        findViewById(R.id.next_button).setOnClickListener(this);
        findViewById(R.id.retry_button).setEnabled(false);

        mKnockCodeView.setPatternSize(mSettingsHelper.getPatternSize());
    }

    @Override
    public void onPositionTapped(int pos) {
        mKnockCodeView.setMode(KnockCodeButtonView.Mode.READY);
        findViewById(R.id.next_button).setEnabled(true);
        findViewById(R.id.retry_button).setEnabled(true);

        if (!mIsConfirmationMode) {
            if (mFirstTappedPositions.size() < KNOCK_CODE_MAX_SIZE) {
                mFirstTappedPositions.add(pos);
                mPasscodeDotView.append('R');
            }
            if (mFirstTappedPositions.size() == KNOCK_CODE_MAX_SIZE && !mIsOldCode) {
                Toast.makeText(MainActivity.this, R.string.size_exceeded, Toast.LENGTH_SHORT).show();
                reset();
            }
        } else {
            if (mSecondTappedPositions.size() < KNOCK_CODE_MAX_SIZE) {
                mSecondTappedPositions.add(pos);
                mPasscodeDotView.append('R');
            }
            if (mSecondTappedPositions.size() == KNOCK_CODE_MAX_SIZE) {
                Toast.makeText(MainActivity.this, R.string.size_exceeded, Toast.LENGTH_SHORT).show();
                reset();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.retry_button:
                mKnockCodeView.setMode(KnockCodeButtonView.Mode.READY);
                reset();
                break;
            case R.id.next_button:
                if (mIsOldCode) {
                    if (mFirstTappedPositions.equals(mSettingsHelper.getPasscode())) {
                        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                        finish();
                    }
                    else {
                        mKnockCodeView.setMode(KnockCodeButtonView.Mode.INCORRECT);
                        reset();
                    }
                    return;
                }
                if (!mIsConfirmationMode) {
                    if (mFirstTappedPositions.size()<KNOCK_CODE_MIN_SIZE) {
                        Toast.makeText(MainActivity.this, getString(R.string.minimum_size, KNOCK_CODE_MIN_SIZE), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (requestCode==GET_A_CODE) {
                        if (mFirstTappedPositions.equals(mSettingsHelper.getPasscode())) {
                            Toast.makeText(MainActivity.this, R.string.master_app_same, Toast.LENGTH_SHORT).show();
                            reset();
                            return;
                        }
                        if (needleStartsWithHaystack(mFirstTappedPositions, mSettingsHelper.getPasscode())) {
                            Toast.makeText(MainActivity.this, R.string.master_app_start, Toast.LENGTH_SHORT).show();
                            reset();
                            return;
                        }
                        checkPasswordValid();
                    } else
                        allowNext();

                } else {
                    if (mFirstTappedPositions.equals(mSecondTappedPositions)) {
                        knocksMatch();
                    } else {
                        knocksDoNotMatch();
                    }
                }
                break;
        }
    }

    private boolean needleStartsWithHaystack(ArrayList<Integer> needle, ArrayList<Integer> haystack) {
        for(int i=0; i<needle.size(); ++i)
            if (needle.get(i)!=haystack.get(i))
                return false;
        return true;
    }


    private void reset() {
        if (mIsConfirmationMode) {
            if (mSecondTappedPositions.size()!=0) {
                mSecondTappedPositions.clear();
                mHintTextView.setText(R.string.confirm_new_knock_code);
            }
            else {
                mIsConfirmationMode = false;
                mHintTextView.setText(mIsOldCode ? R.string.enter_previous_knock_code : R.string.enter_new_knock_code);
                mFirstTappedPositions.clear();
                findViewById(R.id.retry_button).setEnabled(false);
            }
        }
        else {
            mFirstTappedPositions.clear();
            mHintTextView.setText(mIsOldCode ? R.string.enter_previous_knock_code : R.string.enter_new_knock_code);
            findViewById(R.id.retry_button).setEnabled(false);
        }
        mPasscodeDotView.reset(true);
        findViewById(R.id.next_button).setEnabled(false);
    }

    private void knocksMatch() {
        mKnockCodeView.setMode(KnockCodeButtonView.Mode.CORRECT);
        if (requestCode==SET_NEW_PASSCODE) {
            mSettingsHelper.setPasscode(mSecondTappedPositions);
            mSettingsHelper.storePatternSize(mKnockCodeView.mPatternSize);
            Toast.makeText(this, R.string.successfully_changed_code, Toast.LENGTH_SHORT).show();
            Intent returnIntent = getIntent();
            setResult(this.RESULT_OK, returnIntent);
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.show();
            (new DeleteAllShortcuts(this)).execute();
        } else if (requestCode==GET_A_CODE) {
            Intent returnIntent = getIntent();
            returnIntent.putExtra("newCode", passcodeToString(mFirstTappedPositions));
            setResult(this.RESULT_OK, returnIntent);
            KNOCK_CODE_MAX_SIZE = 20;
            finish();
        }
    }

    private String passcodeToString(ArrayList<Integer> passcode) {
        String string = "";
        for (int i = 0; i < passcode.size(); i++) {
            string += String.valueOf(passcode.get(i));
            if (i != passcode.size()-1) {
                string += ",";
            }
        }
        return string;
    }

    private void knocksDoNotMatch() {
        reset();
        mKnockCodeView.setMode(KnockCodeButtonView.Mode.INCORRECT);
    }

    private void getNewPatternSize() {
        ViewGroup numberPickerView = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.grid_number_picker, null);
        final NumberPicker npr,npc;
        if (numberPickerView!=null) {
            npr = (NumberPicker) numberPickerView.findViewById(R.id.numberPickerX);
            if (npr!=null) {
                npr.setMinValue(GRID_MIN_SIZE);
                npr.setMaxValue(GRID_MAX_SIZE);
                npr.setValue(mKnockCodeView.mPatternSize.numberOfRows);
            }

            npc = (NumberPicker) numberPickerView.findViewById(R.id.numberPickerY);
            if (npc!=null) {
                npc.setMinValue(GRID_MIN_SIZE);
                npc.setMaxValue(GRID_MAX_SIZE);
                npc.setValue(mKnockCodeView.mPatternSize.numberOfColumns);
            }

            new AlertDialog.Builder(this)
                    .setTitle(R.string.pref_description_change_code_pick_size)
                    .setView(numberPickerView)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mKnockCodeView.setPatternSize(new Grid(npc.getValue(), npr.getValue()));
                            dialog.dismiss();
                        }
                    })
                    .create()
                    .show();
        }
        return;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (requestCode==-1)
                    onBackPressed();
                else {
                    Intent returnIntent = getIntent();
                    setResult(this.RESULT_CANCELED, returnIntent);
                    KNOCK_CODE_MAX_SIZE = 20;
                    finish();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void deleteShortcutsFinish() {
        mProgressDialog.dismiss();
        if (!mSettingsHelper.isSwitchOff())
            SettingsHelper.killPackage();
        finish();
    }

    @Override
    public boolean onLongClick(View v) {
        reset();
        return true;
    }

    private void checkPasswordValid() {
        if (mProgressDialog==null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage(getString(R.string.loading));
        }
        mProgressDialog.show();
        (new CheckPasswordValid(this)).execute(passcodeToString(mFirstTappedPositions));
    }

    @Override
    public void isPasswordValid(String result) {
        mProgressDialog.dismiss();
        if (result==null)
            allowNext();
        else {
            Toast.makeText(MainActivity.this, getString(R.string.shortcut_conflict, result), Toast.LENGTH_SHORT).show();
            reset();
        }
    }

    private void allowNext() {
        mIsConfirmationMode = true;
        if ((requestCode==SET_NEW_PASSCODE) && (!mSettingsHelper.isSwitchOff()))
            Toast.makeText(MainActivity.this, R.string.will_restart_systemui, Toast.LENGTH_SHORT).show();
        mHintTextView.setText(R.string.confirm_new_knock_code);
        findViewById(R.id.next_button).setEnabled(false);
        mPasscodeDotView.reset(true);
    }
}