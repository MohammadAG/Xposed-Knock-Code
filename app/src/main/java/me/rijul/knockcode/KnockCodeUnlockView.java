package me.rijul.knockcode;

import java.util.ArrayList;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.telecom.TelecomManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.animation.AnimationUtils;
import java.lang.Runnable;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import me.rijul.knockcode.KnockCodeView.Mode;
import me.rijul.knockcode.KnockCodeView.OnPositionTappedListener;
import me.rijul.knockcode.SettingsHelper.OnSettingsReloadedListener;

import de.robv.android.xposed.XposedHelpers;

public class KnockCodeUnlockView extends LinearLayout implements OnPositionTappedListener, KeyguardSecurityView, OnSettingsReloadedListener, OnLongClickListener {
	private KnockCodeView mKnockCodeUnlockView;
	private Object mCallback;
	private Object mLockPatternUtils;
	@SuppressWarnings("unused")
	private CountDownTimer mCountdownTimer;
	protected Context mContext;
	@SuppressWarnings("unused")
	private int mTotalFailedPatternAttempts = 0;
	private int mFailedPatternAttemptsSinceLastTimeout = 0;
	private ArrayList<Integer> mTappedPositions = new ArrayList<Integer>();
	private TextView mTextView;
	private SettingsHelper mSettingsHelper;
    private final AppearAnimationUtils mAppearAnimationUtils;
    private final DisappearAnimationUtils mDisappearAnimationUtils;
    private int mDisappearYTranslation;
    //protected View mEcaView;
	protected XC_MethodHook.MethodHookParam mParam;
	public Button mEmergencyButton;
	public Object mKeyguardUpdateMonitor;

	private static final ArrayList<Integer> mPasscode = new ArrayList<Integer>();

	static {
		mPasscode.add(1);
		mPasscode.add(2);
		mPasscode.add(3);
		mPasscode.add(4);
	}

	public KnockCodeUnlockView(Context context, XC_MethodHook.MethodHookParam param) {
		//initialisation
		super(context);
		setOrientation(VERTICAL);
		mContext = context;
		mParam = param;
		mKeyguardUpdateMonitor = XposedHelpers.callStaticMethod(XposedHelpers.
						findClass("com.android.keyguard.KeyguardUpdateMonitor", mParam.thisObject.getClass().getClassLoader()),
				"getInstance", mContext);
		mLockPatternUtils = XposedHelpers.getObjectField(mParam.thisObject, "mLockPatternUtils");

		//top text view
		mTextView = new TextView(mContext);
		mTextView.setGravity(Gravity.CENTER);
		mTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.1f));
		addView(mTextView);

		//knock code view
		mKnockCodeUnlockView = new KnockCodeView(mContext);
		mKnockCodeUnlockView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.8f));
		mKnockCodeUnlockView.setOnPositionTappedListener(this);
		mKnockCodeUnlockView.setOnLongClickListener(this);
		addView(mKnockCodeUnlockView);
		//appear utils for knockcode view
        mAppearAnimationUtils = new AppearAnimationUtils(mContext,
                AppearAnimationUtils.DEFAULT_APPEAR_DURATION, 1.5f /* translationScale */,
                2.0f /* delayScale */, AnimationUtils.loadInterpolator(
                mContext, android.R.interpolator.linear_out_slow_in));
        mDisappearAnimationUtils = new DisappearAnimationUtils(context,
                125, 1.2f /* translationScale */,
                0.8f /* delayScale */, AnimationUtils.loadInterpolator(
                mContext, android.R.interpolator.fast_out_linear_in));
		Resources res = ResourceHelper.getResourcesForPackage(mContext, mContext.getPackageName());
		mDisappearYTranslation = res.getDimensionPixelOffset(res.getIdentifier(
				"disappear_y_translation", "dimen", mContext.getPackageName()));

		//emergency button
		mEmergencyButton = new Button(mContext);
		LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, 0, 0.1f);
		llp.gravity = Gravity.CENTER_HORIZONTAL;
		mEmergencyButton.setLayoutParams(llp);
		float textSize = res.getDimensionPixelSize(
				res.getIdentifier("kg_status_line_font_size", "dimen", mContext.getPackageName()));
		mEmergencyButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
		if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
			Resources res2 = ResourceHelper.getOwnResources(mContext);
			mEmergencyButton.setText(res2.getString(res2.getIdentifier("lockscreen_return_to_call", "string", "me.rijul.knockcode")));
		}
		else
			mEmergencyButton.setText(res.getString(res.getIdentifier("kg_emergency_call_label", "string", mContext.getPackageName())));
		TypedValue outValue = new TypedValue();
		mContext.getTheme().resolveAttribute(android.R.attr.textColorSecondary, outValue, true);
		int[] textSizeAttr = new int[] {android.R.attr.textColorSecondary};
		TypedArray a = context.obtainStyledAttributes(outValue.data, textSizeAttr);
		int textColor = a.getColor(0, -1);
		a.recycle();
		mEmergencyButton.setTextColor(textColor);

		mContext.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
		mEmergencyButton.setBackgroundResource(outValue.resourceId);

		int spacing = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());
		mEmergencyButton.setPadding(mEmergencyButton.getPaddingLeft() + spacing, mEmergencyButton.getPaddingTop(),
				mEmergencyButton.getPaddingRight() + spacing, mEmergencyButton.getPaddingBottom());

		if (mEmergencyButton.getText()==null)
			mEmergencyButton.setText("Error");

		mEmergencyButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				takeEmergencyCallAction();
			}
		});
        addView(mEmergencyButton);
	}

	public void setKeyguardCallback(Object paramKeyguardSecurityCallback) {
		mCallback = paramKeyguardSecurityCallback;
	}

	public void takeEmergencyCallAction() {
		if (mLockPatternUtils==null)
			mLockPatternUtils = XposedHelpers.getObjectField(mParam.thisObject, "mLockPatternUtils");
		// should be the equivalent to the old userActivity(EMERGENCY_CALL_TIMEOUT)
		XposedHelpers.callMethod(mCallback, "userActivity");
		if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
			if (isInCall()) {
				resumeCall();
			}
			else {
				XposedHelpers.callMethod(mKeyguardUpdateMonitor, "reportEmergencyCallAction", true);

				XposedHelpers.callMethod(mContext, "startActivityAsUser", new Intent()
								.setAction("com.android.phone.EmergencyDialer.DIAL")
								.setPackage("com.android.phone")
								.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
										| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
										| Intent.FLAG_ACTIVITY_CLEAR_TOP),
						ActivityOptions.makeCustomAnimation(getContext(), 0, 0).toBundle(),
						XposedHelpers.newInstance(
								XposedHelpers.findClass("android.os.UserHandle", mParam.thisObject.getClass().getClassLoader()),
								(int) XposedHelpers.callMethod(mKeyguardUpdateMonitor, "getCurrentUser")));
			}
		}
		else {
			if ((boolean) XposedHelpers.callMethod(mLockPatternUtils, "isInCall")) {
				XposedHelpers.callMethod(mLockPatternUtils, "resumeCall");
			} else {
				XposedHelpers.callMethod(mKeyguardUpdateMonitor, "reportEmergencyCallAction", true);
				Intent intent = new Intent("com.android.phone.EmergencyDialer.DIAL");
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
						| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
				XposedHelpers.callMethod(mContext, "startActivityAsUser", intent,
						XposedHelpers.newInstance(
								XposedHelpers.findClass("android.os.UserHandle", mParam.thisObject.getClass().getClassLoader()),
								(int) XposedHelpers.callMethod(mLockPatternUtils, "getCurrentUser")));
				}
			}
		}

	private void resumeCall() {
		getTelecommManager().showInCallScreen(false);
	}

	private boolean isInCall() {
		return getTelecommManager().isInCall();
	}

	private TelecomManager getTelecommManager() {
		return (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
	}

	public void updateEmergencyCallButton(int phoneState) {
		if (mSettingsHelper.hideEmergencyButton())
			return;
		boolean enabled = false;
		if ((boolean) XposedHelpers.callMethod(mLockPatternUtils, "isInCall")) {
			enabled = true; // always show "return to call" if phone is off-hook
		} else if ((boolean) XposedHelpers.callMethod(mLockPatternUtils, "isEmergencyCallCapable")) {
			boolean simLocked = (boolean) XposedHelpers.callMethod(mKeyguardUpdateMonitor, "isSimPinVoiceSecure");
			if (simLocked) {
				// Some countries can't handle emergency calls while SIM is locked.
				enabled = (boolean) XposedHelpers.callMethod(mLockPatternUtils, "isEmergencyCallEnabledWhileSimLocked");
			} else {
				// True if we need to show a secure screen (pin/pattern/SIM pin/SIM puk);
				// hides emergency button on "Slide" screen if device is not secure.
				enabled = (boolean) XposedHelpers.callMethod(mLockPatternUtils, "isSecure") ||
						mContext.getResources().getBoolean(mContext.getResources().
								getIdentifier("config_showEmergencyButton", "bool", mContext.getPackageName()));
			}
		}
		if (mContext.getResources().getBoolean(mContext.getResources().
				getIdentifier("icccardexist_hide_emergencybutton", "bool", mContext.getPackageName()))) {
			enabled = false;
		}
		XposedHelpers.callMethod(mLockPatternUtils, "updateEmergencyCallButtonState", mEmergencyButton, enabled, XposedMod.isXperiaDevice);
		if (mSettingsHelper.hideEmergencyText()) {
			mEmergencyButton.setText("");
		}
	}

	public void updateEmergencyCallButton() {
		if (mSettingsHelper.hideEmergencyButton())
			return;
		mEmergencyButton.setVisibility(View.VISIBLE);
		if (mSettingsHelper.hideEmergencyText()) {
			mEmergencyButton.setText("");
			return;
		}
		//Resources res2 = ResourceHelper.getOwnResources(mContext);
		Resources res2 = Resources.getSystem();
		boolean visible = false;
		if (res2.getBoolean(res2.getIdentifier("config_voice_capable", "bool", "android"))) {
			if (isInCall())
				visible = true;
			else {
				final boolean simLocked = (boolean) XposedHelpers.callMethod(mKeyguardUpdateMonitor, "isSimPinVoiceSecure");
				if (simLocked)
					visible = res2.getBoolean(res2.getIdentifier("config_voice_capable", "bool", "android"));
				else
					visible = true;
			}
		}
		if (visible) {
			mEmergencyButton.setVisibility(View.VISIBLE);
			int id;
			if (isInCall())
				id = res2.getIdentifier("lockscreen_return_to_call", "string", "android");
			else
				id = res2.getIdentifier("lockscreen_emergency_call", "string", "android");
			mEmergencyButton.setText(res2.getString(id));
		}
		else
			mEmergencyButton.setVisibility(View.GONE);
	}

	public void setLockPatternUtils(Object paramLockPatternUtils) {
		mLockPatternUtils = paramLockPatternUtils;
	}

	public Object getCallback() {
		return mCallback;
	}

	public void extendTimeout() {
		XposedHelpers.callMethod(mCallback, "userActivity", 7000L);	
	}

	private void reportFailedUnlockAttempt() {
		if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
			XposedHelpers.callMethod(mCallback, "reportUnlockAttempt", false,
					(mFailedPatternAttemptsSinceLastTimeout>=5) ? ((mSettingsHelper.shouldDisableDialog()) ? 0 : 30000) : 0);
		}
		else {
			XposedHelpers.callMethod(mCallback, "reportUnlockAttempt", false);
		}
	}

	private void handleAttemptLockout(long paramLong) {
		long l = SystemClock.elapsedRealtime();
		onAttemptLockoutStart();
		mCountdownTimer = new CountDownTimer(paramLong - l, 1000L) {
			public void onFinish() {
				onAttemptLockoutEnd();
			}

			public void onTick(long millisUntilFinished) {
				int secs = (int) (millisUntilFinished / 1000L);
				if (mContext != null) {
					if (mSettingsHelper.shouldShowText())
					mTextView.setText(getEnablingInSecs(secs));
				}
			}
		}
		.start();
	}

	private String getEnablingInSecs(int secs) {
		return ResourceHelper.getString(getContext(), R.string.device_disabled, secs);
	}

	protected void onAttemptLockoutEnd() {
		mKnockCodeUnlockView.setEnabled(true);
		mFailedPatternAttemptsSinceLastTimeout = 0;
		mKnockCodeUnlockView.setMode(Mode.READY);
		mTextView.setText("");
	}

	protected void onAttemptLockoutStart() {
		mKnockCodeUnlockView.setEnabled(false);
	}

	private long setLockoutAttemptDeadline() {
		if (mLockPatternUtils==null)
			mLockPatternUtils = XposedHelpers.getObjectField(mParam.thisObject, "mLockPatternUtils");

		if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
			return (Long) XposedHelpers.callMethod(mLockPatternUtils, "setLockoutAttemptDeadline",
					(int) XposedHelpers.callMethod(XposedHelpers.getObjectField(
					mParam.thisObject, "mUpdateMonitor"), "getCurrentUser"), 30000);
		}
		else {
			return (Long) XposedHelpers.callMethod(mLockPatternUtils, "setLockoutAttemptDeadline");
		}
	}

	private void verifyPasscodeAndUnlock() {
		XposedHelpers.callMethod(mCallback, "dismiss", true);
		if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
			XposedHelpers.callMethod(mCallback, "reportUnlockAttempt", true, 0);
		}
		else {
			XposedHelpers.callMethod(mCallback, "reportUnlockAttempt", true);
		}
		mFailedPatternAttemptsSinceLastTimeout = 0;
		mKnockCodeUnlockView.setMode(Mode.READY);
	}

	@Override
	public void onPositionTapped(int pos) {
		XposedHelpers.callMethod(mCallback,"userActivity");
		mTappedPositions.add(pos);
		mKnockCodeUnlockView.setMode(Mode.READY);
		mTextView.setText("");
		if (mTappedPositions.size() == mPasscode.size()) {
			mKnockCodeUnlockView.setEnabled(false);

			boolean correct = true;
			for (int i = 0; i < mPasscode.size(); i++) {
				if (mTappedPositions.get(i) != mPasscode.get(i)) {
					correct = false;
					break;
				}
			}

			mTappedPositions.clear();

			if (correct) {
				mKnockCodeUnlockView.setMode(Mode.CORRECT);
				mKnockCodeUnlockView.setEnabled(true);
				verifyPasscodeAndUnlock();
			} else {
				mTotalFailedPatternAttempts++;
				mFailedPatternAttemptsSinceLastTimeout++;
				reportFailedUnlockAttempt();
				if (mFailedPatternAttemptsSinceLastTimeout >= 5) {
					handleAttemptLockout(setLockoutAttemptDeadline());
					mKnockCodeUnlockView.setMode(Mode.DISABLED);
				} else {
					mKnockCodeUnlockView.setEnabled(true);
				}
				mKnockCodeUnlockView.setMode(Mode.INCORRECT);
				if (mSettingsHelper.shouldShowText())
					mTextView.setText(ResourceHelper.getString(getContext(), R.string.incorrect_pattern));
			}
		} else if (mTappedPositions.size() > mPasscode.size()) {
			mTappedPositions.clear();
		}
	}

	public boolean needsInput() {
		return false;
	}

	public void reset() {
		mTappedPositions.clear();
	}

	public void showUsabilityHint() {

	}

	public void onPause() {
		mTappedPositions.clear();
		mKnockCodeUnlockView.setMode(Mode.READY);
		mTextView.setText("");
	}

	/*   
	 * public static final int SCREEN_ON = 1;
	 * public static final int VIEW_REVEALED = 2;
	 */
	public void onResume(int type) {
        mTappedPositions.clear();
		mKnockCodeUnlockView.setMode(Mode.READY);
		mTextView.setText("");
	}

	public void setSettingsHelper(SettingsHelper settingsHelper) {
		mSettingsHelper = settingsHelper;
		mKnockCodeUnlockView.setSettingsHelper(settingsHelper);
		mSettingsHelper.addInProcessListener(getContext());
		mSettingsHelper.addOnReloadListener(this);
		onSettingsReloaded();
	}

	@Override
	public void onSettingsReloaded() {
		mPasscode.clear();
        mPasscode.addAll(mSettingsHelper.getPasscode());
		if (mSettingsHelper.hideEmergencyButton())
			mEmergencyButton.setVisibility(GONE);
		if (mSettingsHelper.hideEmergencyText())
			mEmergencyButton.setText("");
	}

	@Override
	public boolean onLongClick(View v) {
		XposedHelpers.callMethod(mCallback, "userActivity");
		if (mSettingsHelper.shouldShowText())
        	mTextView.setText(ResourceHelper.getString(getContext(), R.string.long_pressed_hint));
        mTappedPositions.clear();
		mKnockCodeUnlockView.setMode(Mode.READY);
		return true;
	}

	public void startAppearAnimation() {
        //enableClipping(false);
        setAlpha(1f);
        setTranslationY(mAppearAnimationUtils.getStartTranslation());
        animate()
                .setDuration(500)
                .setInterpolator(mAppearAnimationUtils.getInterpolator())
                .translationY(0);
        mAppearAnimationUtils.startAnimation(new View[]{mTextView, mKnockCodeUnlockView, mEmergencyButton}, new Runnable() {
                    @Override
                    public void run() {
                        //enableClipping(true);
                    }
                });

	}

	public boolean startDisappearAnimation(final Runnable finishRunnable) {
        //enableClipping(false);
        setTranslationY(0);
        animate()
                .setDuration(500)
                .setInterpolator(mDisappearAnimationUtils.getInterpolator())
                .translationY(mDisappearYTranslation);
        mDisappearAnimationUtils.startAnimation(new View[]{mTextView, mKnockCodeUnlockView, mEmergencyButton}, new Runnable() {
                    @Override
                    public void run() {
                        //enableClipping(true);
                        if (finishRunnable != null) {
                            finishRunnable.run();
                        }
                    }
                });
		return true;
	}

	public void showBouncer(int duration) {return;}

	public void hideBouncer(int duration) {return;}
}
