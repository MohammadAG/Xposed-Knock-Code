package me.rijul.knockcode;

import java.util.ArrayList;

import android.content.DialogInterface;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import me.rijul.knockcode.KnockCodeView.Mode;
import me.rijul.knockcode.KnockCodeView.OnPositionTappedListener;

public class ChangeKnockCodeActivity extends AppCompatActivity implements OnPositionTappedListener, OnClickListener {
	public static final int KNOCK_CODE_MAX_SIZE = 20;
    public static final int MAX_SIZE = 5;
    public static final int MIN_SIZE = 2;

	private KnockCodeView mKnockCodeView;
	private PasscodeDotsView mPasscodeDotView;
	private TextView mHintTextView;

	private ArrayList<Integer> mFirstTappedPositions = new ArrayList<Integer>();
	private ArrayList<Integer> mSecondTappedPositions = new ArrayList<Integer>();

	private boolean mIsConfirmationMode = false;
	private boolean mIsOldCode = true;
	private SettingsHelper mSettingsHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_change_passcode);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_mtrl_am_alpha);

		mSettingsHelper = new SettingsHelper(getApplicationContext());

		mKnockCodeView = (KnockCodeView) findViewById(R.id.knockCodeView1);
		mKnockCodeView.setOnPositionTappedListener(this);

		mPasscodeDotView = (PasscodeDotsView) findViewById(R.id.passcodeDotView1);
		mPasscodeDotView.setCount(0);

		mHintTextView = (TextView) findViewById(android.R.id.hint);

		findViewById(R.id.retry_button).setOnClickListener(this);
		findViewById(R.id.next_button).setOnClickListener(this);
		findViewById(R.id.retry_button).setEnabled(false);

		mIsOldCode = (mSettingsHelper.getPasscodeOrNull() != null);

		if (mIsOldCode) {
			mHintTextView.setText(R.string.enter_previous_knock_code);
            mKnockCodeView.setPatternSize(mSettingsHelper.getPatternSize());
		}
        else
            getNewPatternSize(true);
	}

	@Override
	public void onPositionTapped(int pos) {
		mKnockCodeView.setMode(Mode.READY);
		findViewById(R.id.next_button).setEnabled(true);
		findViewById(R.id.retry_button).setEnabled(true);

		if (!mIsConfirmationMode) {
			if (mFirstTappedPositions.size() < KNOCK_CODE_MAX_SIZE)
				mFirstTappedPositions.add(pos);

			if (mFirstTappedPositions.size() == KNOCK_CODE_MAX_SIZE && !mIsOldCode) {
				Toast.makeText(ChangeKnockCodeActivity.this, R.string.size_exceeded, Toast.LENGTH_SHORT).show();
				reset();
			}
			mPasscodeDotView.setCount(mFirstTappedPositions.size());
		} else {
			if (mSecondTappedPositions.size() < KNOCK_CODE_MAX_SIZE)
				mSecondTappedPositions.add(pos);

			if (mSecondTappedPositions.size() == KNOCK_CODE_MAX_SIZE) {
				Toast.makeText(ChangeKnockCodeActivity.this, R.string.size_exceeded, Toast.LENGTH_SHORT).show();
				reset();
			}
			mPasscodeDotView.setCount(mSecondTappedPositions.size());
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.retry_button:
			mKnockCodeView.setMode(Mode.READY);
			reset();
			break;
		case R.id.next_button:
			if (mIsOldCode) {
				if (confirmPasscodePair(mFirstTappedPositions, mSettingsHelper.getPasscode())) {
					mIsOldCode = false;
                    getNewPatternSize(false);
					mHintTextView.setText(R.string.enter_new_knock_code);
					reset();
				} else {
					mKnockCodeView.setMode(Mode.INCORRECT);
					reset();
				}
				return;
			}
			if (!mIsConfirmationMode) {
				mIsConfirmationMode = true;
				mHintTextView.setText(R.string.confirm_new_knock_code);
				v.setEnabled(false);
				mPasscodeDotView.setCount(0);
			} else {
				if (confirmPasscodePair(mFirstTappedPositions, mSecondTappedPositions)) {
					knocksMatch();
				} else {
					knocksDoNotMatch();
				}
			}
			break;
		}
	}

	private void knocksMatch() {
		mKnockCodeView.setMode(Mode.CORRECT);
		mSettingsHelper.setPasscode(mSecondTappedPositions);
		Toast.makeText(this, R.string.successfully_changed_code, Toast.LENGTH_SHORT).show();
        mSettingsHelper.storePatternSize(mKnockCodeView.mPatternSize);
		finish();
	}

	private void reset() {
		mIsConfirmationMode = false;
		mFirstTappedPositions.clear();
		mSecondTappedPositions.clear();
		mPasscodeDotView.setCount(0);
		findViewById(R.id.retry_button).setEnabled(false);
		mHintTextView.setText(mIsOldCode ? R.string.enter_previous_knock_code : R.string.enter_new_knock_code);
		findViewById(R.id.next_button).setEnabled(false);
	}

	private void knocksDoNotMatch() {
		reset();
		mKnockCodeView.setMode(Mode.INCORRECT);
	}

	private boolean confirmPasscodePair(ArrayList<Integer> a1, ArrayList<Integer> a2) {
		if (a1.size() != a2.size()) {
			return false;
		}
		for (int i = 0; i < a1.size(); i++) {
			if (a1.get(i) != a2.get(i)) {
				return false;
			}
		}
		return true;
	}

    private void getNewPatternSize(boolean firstRun) {
        ViewGroup numberPickerView = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.grid_number_picker, null);
        final NumberPicker npr,npc;
        if (numberPickerView!=null) {
            npr = (NumberPicker) numberPickerView.findViewById(R.id.numberPickerX);
            if (npr!=null) {
                npr.setMinValue(MIN_SIZE);
                npr.setMaxValue(MAX_SIZE);
                npr.setValue((firstRun) ? 2 : mKnockCodeView.mPatternSize.numberOfRows);
            }

            npc = (NumberPicker) numberPickerView.findViewById(R.id.numberPickerY);
            if (npc!=null) {
                npc.setMinValue(MIN_SIZE);
                npc.setMaxValue(MAX_SIZE);
                npc.setValue((firstRun) ? 2 : mKnockCodeView.mPatternSize.numberOfColumns);
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
}
