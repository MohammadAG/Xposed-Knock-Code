package me.rijul.knockcode;

import java.util.ArrayList;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.animation.AnimationUtils;
import java.lang.Runnable;
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
    protected View mEcaView;


	private static final ArrayList<Integer> mPasscode = new ArrayList<Integer>();

	static {
		mPasscode.add(1);
		mPasscode.add(2);
		mPasscode.add(3);
		mPasscode.add(4);
	}

	public KnockCodeUnlockView(Context context, XC_MethodHook.MethodHookParam param) {
		super(context);
		setOrientation(VERTICAL);
		mContext = context;
		mTextView = new TextView(context);
		mTextView.setGravity(Gravity.CENTER);
		mTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT, 0.1f));
		int spacing = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());
		mTextView.setPadding(mTextView.getPaddingLeft(), mTextView.getPaddingTop(),
				mTextView.getPaddingRight(), mTextView.getPaddingBottom() + spacing);
		addView(mTextView);

		mKnockCodeUnlockView = new KnockCodeView(mContext);
		mKnockCodeUnlockView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT, 0.9f));
		mKnockCodeUnlockView.setOnPositionTappedListener(this);
		mKnockCodeUnlockView.setOnLongClickListener(this);
		addView(mKnockCodeUnlockView);
        mAppearAnimationUtils = new AppearAnimationUtils(mContext,
                AppearAnimationUtils.DEFAULT_APPEAR_DURATION, 1.5f /* translationScale */,
                2.0f /* delayScale */, AnimationUtils.loadInterpolator(
                mContext, android.R.interpolator.linear_out_slow_in));
        mDisappearAnimationUtils = new DisappearAnimationUtils(context,
                125, 1.2f /* translationScale */,
                0.8f /* delayScale */, AnimationUtils.loadInterpolator(
                mContext, android.R.interpolator.fast_out_linear_in));
		mDisappearYTranslation = ResourceHelper.getResource(mContext, "com.android.systemui", "disappear_y_translation", "dimen");
		//mEcaView = (View) XposedHelpers.newInstance(XposedHelpers.findClass("com.android.keyguard.EmergencyCarrierArea", null), context);
		//addView(mEcaView);
		//addView(new EmergencyButton(context,param));
	}

	public void setKeyguardCallback(Object paramKeyguardSecurityCallback) {
		mCallback = paramKeyguardSecurityCallback;
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
		XposedHelpers.callMethod(mCallback, "reportUnlockAttempt", false);
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
	}

	protected void onAttemptLockoutStart() {
		mKnockCodeUnlockView.setEnabled(false);
	}

	private long setLockoutAttemptDeadline() {
		if (mLockPatternUtils==null)
			mLockPatternUtils = XposedHelpers.newInstance(XposedHelpers.findClass("com.android.internal.widget.LockPatternUtils",null),getContext());
		return (Long) XposedHelpers.callMethod(mLockPatternUtils, "setLockoutAttemptDeadline");
	}

	private void verifyPasscodeAndUnlock() {
		XposedHelpers.callMethod(mCallback, "reportUnlockAttempt", true);
		try {
			XposedHelpers.callMethod(mCallback, "dismiss", true);
		} catch (Throwable t) {

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
				reportFailedUnlockAttempt();
				mTotalFailedPatternAttempts++;
				mFailedPatternAttemptsSinceLastTimeout++;
				if (mFailedPatternAttemptsSinceLastTimeout >= 5) {
					handleAttemptLockout(setLockoutAttemptDeadline());
					mKnockCodeUnlockView.setMode(Mode.DISABLED);
				} else {
					mKnockCodeUnlockView.setEnabled(true);
				}
				mKnockCodeUnlockView.setMode(Mode.INCORRECT);

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
	}

	@Override
	public boolean onLongClick(View v) {
        mTextView.setText(ResourceHelper.getString(getContext(), R.string.long_pressed_hint));
        mTappedPositions.clear();
		mKnockCodeUnlockView.setMode(Mode.READY);
		return true;
	}

	public void startAppearAnimation() {
        XposedBridge.log("Animating");
        //enableClipping(false);
        setAlpha(1f);
        setTranslationY(mAppearAnimationUtils.getStartTranslation());
        animate()
                .setDuration(500)
                .setInterpolator(mAppearAnimationUtils.getInterpolator())
                .translationY(0);
        mAppearAnimationUtils.startAnimation(new View[]{mKnockCodeUnlockView, null}, new Runnable() {
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
        mDisappearAnimationUtils.startAnimation(new View[]{mKnockCodeUnlockView, null}, new Runnable() {
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
