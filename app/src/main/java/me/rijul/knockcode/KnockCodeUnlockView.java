package me.rijul.knockcode;

import java.util.ArrayList;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.telecom.TelecomManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import java.lang.Runnable;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import me.rijul.knockcode.KnockCodeButtonView.Mode;
import me.rijul.knockcode.KnockCodeButtonView.OnPositionTappedListener;
import me.rijul.knockcode.SettingsHelper.OnSettingsReloadedListener;

import de.robv.android.xposed.XposedHelpers;

public class KnockCodeUnlockView extends LinearLayout implements OnPositionTappedListener, KeyguardSecurityView, OnSettingsReloadedListener, OnLongClickListener {
	private KnockCodeButtonView mKnockCodeUnlockView;
	private Object mCallback;
	private Object mLockPatternUtils;
	@SuppressWarnings("unused")
	private CountDownTimer mCountdownTimer;
	protected Context mContext;
	@SuppressWarnings("unused")
	private int mTotalFailedPatternAttempts = 0;
	private int mFailedPatternAttemptsSinceLastTimeout = 0;
	private int mLongPress = 0;
	private ArrayList<Integer> mTappedPositions = new ArrayList<Integer>();
	private TextView mTextView;
	private PasswordTextView mDotsView;
	private SettingsHelper mSettingsHelper;
    private AppearAnimationUtils mAppearAnimationUtils;
	private DisappearAnimationUtils mDisappearAnimationUtils;
	private int mDisappearYTranslation;
	protected XC_MethodHook.MethodHookParam mParam;
	public Button mEmergencyButton;
	public Object mKeyguardUpdateMonitor;
	private String mKeyguardPackageName;

	private static final ArrayList<Integer> mPasscode = new ArrayList<Integer>();

	static {
		mPasscode.add(1);
		mPasscode.add(2);
		mPasscode.add(3);
		mPasscode.add(4);
	}

	public KnockCodeUnlockView(Context context, XC_MethodHook.MethodHookParam param, SettingsHelper settingsHelper, String keyguardPackageName) {
		super(context);
		setOrientation(VERTICAL);
		mContext = context;
		mParam = param;
		mKeyguardPackageName = keyguardPackageName;
		mKeyguardUpdateMonitor = XposedHelpers.callStaticMethod(XposedHelpers.
						findClass(mKeyguardPackageName + ".KeyguardUpdateMonitor", mParam.thisObject.getClass().getClassLoader()),
				"getInstance", mContext);
		try {
			mLockPatternUtils = XposedHelpers.getObjectField(mParam.thisObject, "mLockPatternUtils");
		} catch (NoSuchFieldError e) {
			mLockPatternUtils = XposedHelpers.getObjectField(mParam.thisObject, "mLockUtils");
		}
        mAppearAnimationUtils = new AppearAnimationUtils(mContext);
		mDisappearAnimationUtils = new DisappearAnimationUtils(mContext,
				125, 0.6f /* translationScale */,
				0.45f /* delayScale */, AnimationUtils.loadInterpolator(
				mContext, android.R.interpolator.fast_out_linear_in));
		Resources res = ResourceHelper.getResourcesForPackage(mContext, mContext.getPackageName());
		mDisappearYTranslation = res.getDimensionPixelSize(res.getIdentifier(
				"disappear_y_translation", "dimen", mContext.getPackageName()));
        setSettingsHelper(settingsHelper);
	}

	public void setKeyguardCallback(Object paramKeyguardSecurityCallback) {
		mCallback = paramKeyguardSecurityCallback;
	}

	public void takeEmergencyCallAction() {
		if (mLockPatternUtils==null)
			mLockPatternUtils = XposedHelpers.getObjectField(mParam.thisObject, "mLockPatternUtils");
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
        if (mEmergencyButton==null)
            return;
		if (mSettingsHelper.hideEmergencyButton())
			return;
		boolean enabled = false;
		if ((boolean) XposedHelpers.callMethod(mLockPatternUtils, "isInCall")) {
			enabled = true; // always show "return to call" if phone is off-hook
		} else if ((boolean) XposedHelpers.callMethod(mLockPatternUtils, "isEmergencyCallCapable")) {
			boolean simLocked = (XposedMod.isXperiaDevice) ?
					((boolean) XposedHelpers.callMethod(mKeyguardUpdateMonitor, "isSimLocked")) :
					((boolean) XposedHelpers.callMethod(mKeyguardUpdateMonitor, "isSimPinVoiceSecure"));
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
		XposedHelpers.callMethod(mLockPatternUtils, "updateEmergencyCallButtonState", mEmergencyButton, enabled, false);
		if (mSettingsHelper.hideEmergencyText()) {
			mEmergencyButton.setText("");
		}
	}

	public void updateEmergencyCallButton() {
        if (mEmergencyButton==null)
            return;
		if (mSettingsHelper.hideEmergencyButton())
			return;
		mEmergencyButton.setVisibility(View.VISIBLE);
		if (mSettingsHelper.hideEmergencyText()) {
			mEmergencyButton.setText("");
			return;
		}
		Resources res2 = Resources.getSystem();
		boolean visible = false;
		if (res2.getBoolean(res2.getIdentifier("config_voice_capable", "bool", "android"))) {
			if (isInCall())
				visible = true;
			else {
				final boolean simLocked = (boolean) XposedHelpers.callMethod(mKeyguardUpdateMonitor, "isSimPinVoiceSecure");
				visible = simLocked ? res2.getBoolean(res2.getIdentifier("config_voice_capable", "bool", "android")) : true;
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
					setText(mTextView, getEnablingInSecs(secs));
				}
			}
		}
		.start();
	}

	private String getEnablingInSecs(int secs) {
		return ResourceHelper.getString(getContext(), R.string.device_disabled, secs);
	}

	protected void onAttemptLockoutEnd() {
		mFailedPatternAttemptsSinceLastTimeout = 0;
		mKnockCodeUnlockView.enableButtons(true);
		mKnockCodeUnlockView.setMode(Mode.READY);
		setText(mTextView, "");
	}

	protected void onAttemptLockoutStart() {
		mKnockCodeUnlockView.setMode(Mode.DISABLED);;
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
        XposedBridge.log("[KnockCode] inside verifying");
		if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
            XposedBridge.log("[KnockCode] reporting unlock attempt");
			XposedHelpers.callMethod(mCallback, "reportUnlockAttempt", true, 0);
		}
		else {
			XposedHelpers.callMethod(mCallback, "reportUnlockAttempt", true);
		}
        XposedBridge.log("[KnockCode] dismissing");
        XposedHelpers.callMethod(mCallback, "dismiss", true);
		XposedBridge.log("[KnockCode] dismissed");
		mFailedPatternAttemptsSinceLastTimeout = 0;
		mKnockCodeUnlockView.setMode(Mode.READY);
	}

	@Override
	public void onPositionTapped(int pos) {
		mLongPress = 0;
		XposedHelpers.callMethod(mCallback, "userActivity");
		mTappedPositions.add(pos);
		setDotsCount(mTappedPositions.size());
		setText(mTextView, "");
		if (mTappedPositions.size() == mPasscode.size()) {
			mKnockCodeUnlockView.enableButtons(false);

			boolean correct = true;
			for (int i = 0; i < mPasscode.size(); i++) {
				if (mTappedPositions.get(i) != mPasscode.get(i)) {
					correct = false;
					break;
				}
			}

			mTappedPositions.clear();
			if (correct) {
                XposedBridge.log("[KnockCode] setting correct");
				mKnockCodeUnlockView.setMode(Mode.CORRECT);
                XposedBridge.log("[KnockCode] verifying");
				verifyPasscodeAndUnlock();
			} else {
				mTotalFailedPatternAttempts++;
				mFailedPatternAttemptsSinceLastTimeout++;
				setDotsColor(mKnockCodeUnlockView, !(mFailedPatternAttemptsSinceLastTimeout>=5));
				reportFailedUnlockAttempt();
				if (mFailedPatternAttemptsSinceLastTimeout >= 5) {
					handleAttemptLockout(setLockoutAttemptDeadline());
				}
				setText(mTextView, ResourceHelper.getString(getContext(), R.string.incorrect_pattern));
			}
		} else if (mTappedPositions.size() > mPasscode.size()) {
			mTappedPositions.clear();
			resetDots();
		}
	}

	public boolean needsInput() {
		return false;
	}

	public void reset() {
		mTappedPositions.clear();
		resetDots();
	}

	public void showUsabilityHint() {

	}

	public void onPause() {
		if (mFailedPatternAttemptsSinceLastTimeout>=5)
			return;
		mTappedPositions.clear();
		resetDots();
		mKnockCodeUnlockView.setMode(Mode.READY);
		setText(mTextView, "");
		mLongPress = 0;
	}

	/*   
	 * public static final int SCREEN_ON = 1;
	 * public static final int VIEW_REVEALED = 2;
	 */
	public void onResume(int type) {
		if (mFailedPatternAttemptsSinceLastTimeout>=5)
			return;
		mTappedPositions.clear();
		resetDots();
		mKnockCodeUnlockView.setMode(Mode.READY);
		setText(mTextView, "");
		mLongPress = 0;
	}

	public void setSettingsHelper(SettingsHelper settingsHelper) {
		mSettingsHelper = settingsHelper;
		setUpViews();
		onSettingsReloaded();
        mKnockCodeUnlockView.setSettingsHelper(settingsHelper);
        mSettingsHelper.addInProcessListener(getContext());
        mSettingsHelper.addOnReloadListener(this);
	}

    private void setUpTextView() {
        if (mTextView==null)
            mTextView = new TextView(mContext);
        mTextView.setGravity(Gravity.CENTER);
		mTextView.setLayoutParams(getParams(0.07f));
        setText(mTextView, "", true);
        addView(mTextView);
    }

    private void setUpDotsView() {
		if (mDotsView == null)
			mDotsView = new PasswordTextView(mContext);
		mDotsView.setLayoutParams(getParams(0.03f));
		addView(mDotsView);
    }

    private void setUpKnockView() {
        if (mKnockCodeUnlockView==null)
            mKnockCodeUnlockView = new KnockCodeButtonView(mContext);
        mKnockCodeUnlockView.setLayoutParams(getParams(0.77f));
		mKnockCodeUnlockView.setOrientation(VERTICAL);
        mKnockCodeUnlockView.setOnPositionTappedListener(this);
        mKnockCodeUnlockView.setOnLongClickListener(this);
        addView(mKnockCodeUnlockView);
		mKnockCodeUnlockView.setSettingsHelper(mSettingsHelper);
	}

    private void setUpEmergencyButton() {
        if (mEmergencyButton==null)
            mEmergencyButton = new Button(mContext);
        Resources res = ResourceHelper.getResourcesForPackage(mContext, mContext.getPackageName());
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0.1f);
        llp.gravity = Gravity.CENTER_HORIZONTAL;
        mEmergencyButton.setLayoutParams(llp);
        float textSize = res.getDimensionPixelSize(
                res.getIdentifier("kg_status_line_font_size", "dimen", mContext.getPackageName()));
        mEmergencyButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        TypedValue outValue = new TypedValue();
        mContext.getTheme().resolveAttribute(android.R.attr.textColorSecondary, outValue, true);
        int[] textSizeAttr = new int[] {android.R.attr.textColorSecondary};
        TypedArray a = mContext.obtainStyledAttributes(outValue.data, textSizeAttr);
        int textColor = a.getColor(0, -1);
        a.recycle();
        mEmergencyButton.setTextColor(textColor);
        mContext.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        mEmergencyButton.setBackgroundResource(outValue.resourceId);
        int spacing = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());
        mEmergencyButton.setPadding(mEmergencyButton.getPaddingLeft() + spacing, mEmergencyButton.getPaddingTop(),
                mEmergencyButton.getPaddingRight() + spacing, mEmergencyButton.getPaddingBottom());
        mEmergencyButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                takeEmergencyCallAction();
            }
        });
        addView(mEmergencyButton);
    }

    private LinearLayout.LayoutParams getParams(float weight) {
        return new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, weight);
    }

    private void setUpViews() {
        removeAllViews();
        setUpTextView();
        setUpDotsView();
        setUpKnockView();
        setUpEmergencyButton();
    }

	private void showOrHideViews() {
		if (mTextView!=null)
			mTextView.setVisibility(mSettingsHelper.shouldShowText() ? View.VISIBLE : View.GONE);
		if (mDotsView!=null)
			mDotsView.setVisibility(mSettingsHelper.showDots() ? View.VISIBLE : View.GONE);
		if (mEmergencyButton!=null)
			mEmergencyButton.setVisibility(mSettingsHelper.hideEmergencyButton() ? View.GONE : View.VISIBLE);
		if (mKnockCodeUnlockView!=null)
			mKnockCodeUnlockView.setPatternSize(mSettingsHelper.getPatternSize());
		if (!(mSettingsHelper.shouldDrawFill())) {
			mKnockCodeUnlockView.hideButtonTaps();
		}
		if (mSettingsHelper.shouldDrawLines())
			mKnockCodeUnlockView.reColourLines(0xfffafafa);
		else
			mKnockCodeUnlockView.hideLines();
		invalidate();
	}

	@Override
	public void onSettingsReloaded() {
		mPasscode.clear();
		mPasscode.addAll(mSettingsHelper.getPasscode());
		showOrHideViews();
	}

	@Override
	public boolean onLongClick(View v) {
		++mLongPress;
		if ((mLongPress==5) && (mSettingsHelper.failSafe())) {
			Intent intent = new Intent("me.rijul.knockcode.KILL_THE_CODE");
			mContext.sendBroadcast(intent);
			mContext.registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					XposedBridge.log("[KnockCode] Received news, taking revenge on SystemUI");
					System.exit(0);
				}
			}, new IntentFilter("me.rijul.knockcode.DEAD"));
		}
		XposedHelpers.callMethod(mCallback, "userActivity");
		setText(mTextView, ResourceHelper.getString(getContext(), R.string.long_pressed_hint));
		mTappedPositions.clear();
		resetDots();
		mKnockCodeUnlockView.setMode(Mode.READY);
		return true;
	}

	public void startAppearAnimation() {
		XposedBridge.log("[KnockCode] Appear Animation!");
		enableClipping(false);
		setAlpha(1f);
		setTranslationY(mAppearAnimationUtils.getStartTranslation());
		animate()
				.setDuration(500)
				.setInterpolator(mAppearAnimationUtils.getInterpolator())
				.translationY(0);
		mAppearAnimationUtils.startAnimation(new View[]{mTextView, mDotsView, mKnockCodeUnlockView, mEmergencyButton},
				new Runnable() {
					@Override
					public void run() {
						enableClipping(true);
					}
				});

	}

	public boolean startDisappearAnimation(final Runnable finishRunnable) {
		XposedBridge.log("[KnockCode] Disappear Animation!");
		enableClipping(false);
		animate()
				.alpha(0f)
				.translationY(mDisappearYTranslation)
				.setInterpolator(mDisappearAnimationUtils.getInterpolator())
				.setDuration(100)
				.withEndAction(finishRunnable);
		return true;
	}

    private void enableClipping(boolean enable) {
        setClipToPadding(enable);
        setClipChildren(enable);
    }

	public void showBouncer(int duration) {return;}

	public void hideBouncer(int duration) {return;}

	private void setText(final TextView tv, final String text, final boolean firstRun) {
        if (tv==null)
            return;
        if (!firstRun)
            if (tv.getText().equals(text))
				return;
		if ((firstRun)||(mSettingsHelper.shouldShowText())) {
			setTranslationY(0);
			tv
					.animate()
					.translationY(-getHeight())
					.alpha(0.0f).setDuration(100)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							super.onAnimationEnd(animation);
							tv.setText(text);
							tv.animate().translationY(0).alpha(1.1f).setDuration(200).start();
						}
					})
					.start();
		}
	}

    private void setText(final TextView tv, final String text) {
        setText(tv,text,false);
    }

    private void setDotsCount(int n) {
        if (mDotsView!=null)
            mDotsView.append('R');
    }

    private void resetDots() {
        if (mDotsView!=null)
            mDotsView.reset(true);
    }

	private void setDotsColor(KnockCodeButtonView kcv, boolean shouldEnable) {
		if (mDotsView!=null)
			mDotsView.setDotsColor(kcv, shouldEnable);
	}
}
