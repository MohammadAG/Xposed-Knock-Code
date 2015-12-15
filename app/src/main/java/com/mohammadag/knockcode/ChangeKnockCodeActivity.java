package com.mohammadag.knockcode;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.mohammadag.knockcode.KnockCodeView.Mode;
import com.mohammadag.knockcode.KnockCodeView.OnPositionTappedListener;

public class ChangeKnockCodeActivity extends Activity implements OnPositionTappedListener, OnClickListener {
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
		}
	}

	@Override
	public void onPositionTapped(int pos) {
		mKnockCodeView.setMode(Mode.READY);
		findViewById(R.id.next_button).setEnabled(true);
		findViewById(R.id.retry_button).setEnabled(true);

		if (!mIsConfirmationMode) {
			if (mFirstTappedPositions.size() < 8)
				mFirstTappedPositions.add(pos);

			if (mFirstTappedPositions.size() == 8 && !mIsOldCode) {
				mKnockCodeView.setMode(Mode.CORRECT);
				mIsConfirmationMode = true;
			}
			mPasscodeDotView.setCount(mFirstTappedPositions.size());
		} else {
			if (mSecondTappedPositions.size() < 8)
				mSecondTappedPositions.add(pos);

			if (mSecondTappedPositions.size() == 8) {
				mKnockCodeView.setMode(Mode.DISABLED);
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
}
