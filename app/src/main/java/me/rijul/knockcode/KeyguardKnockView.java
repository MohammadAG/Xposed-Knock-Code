package me.rijul.knockcode;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.Vibrator;
import android.telecom.TelecomManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by rijul on 2/3/16.
 */
@SuppressLint("ViewConstructor")
public class KeyguardKnockView extends LinearLayout implements LockButtonView.OnPositionTappedListener, View.OnLongClickListener, LockButtonView.OnLongPressCompletedListener {
    //constants
    public static final int SCREEN_ON = 1;
    public static final int VIEW_REVEALED = 2;
    private int mMaxTries;
    //views
    private TextView mTextView;
    private DotsView mDotsView;
    private LockButtonView mLockButtonView;
    private Button mEmergencyButton;
    //variables detecting lock screen state, from AOSP
    private int mTotalFailedPatternAttempts = 0;
    private int mFailedPatternAttemptsSinceLastTimeout = 0;
    private int mLongPress = 0;
    //variables in the class
    private Object mCallback;
    public Object mKeyguardUpdateMonitor;
    private Object mLockPatternUtils;
    private AppearAnimationUtils mAppearAnimationUtils;
    private DisappearAnimationUtils mDisappearAnimationUtils;
    private int mDisappearYTranslation;
    //variables made by us
    private SettingsHelper mSettingsHelper;
    private XC_MethodHook.MethodHookParam mParam;

    private Runnable mCancelRunnable = new Runnable() {
        public void run() {
            if (mSettingsHelper.showReadyText())
                mTextView.setText(mSettingsHelper.getReadyText());
            else
                mTextView.setText("");
            mDotsView.setPaintColor(mSettingsHelper.showDots() ? mSettingsHelper.getDotsReadyColor() : Color.TRANSPARENT);
            mDotsView.reset(true);
            mLockButtonView.showLines(mSettingsHelper.showLines());
            mLockButtonView.setMode(LockButtonView.Mode.Ready);
            mLockButtonView.enableButtons(true);
            mTappedPositions.clear();
            mLockButtonView.clearCode();
        }
    };

    private Runnable mUnlockRunnable = new Runnable() {
        @Override
        public void run() {
                unlock();
        }
    };
    //settings changed receiver
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onSettingsReloaded();
        }
    };
    //actual passcode and tapped positions
    private ArrayList<Integer> mPasscode, mTappedPositions = new ArrayList<Integer>();
    private Vibrator mVibrator;
    private Runnable mVibrateRunnable = new Runnable() {
        @Override
        public void run() {
            if (mVibrator.hasVibrator())
                mVibrator.vibrate((long) mSettingsHelper.errorDuration());
        }
    };

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public KeyguardKnockView(Context context, XC_MethodHook.MethodHookParam param,
                               SettingsHelper settingsHelper, String keyguardPackageName) {
        super(context);
        setOrientation(VERTICAL);
        mParam = param;
        mKeyguardUpdateMonitor = XposedHelpers.callStaticMethod(XposedHelpers.findClass(keyguardPackageName +
                ".KeyguardUpdateMonitor", mParam.thisObject.getClass().getClassLoader()), "getInstance", getContext());
        try {
            mLockPatternUtils = XposedHelpers.getObjectField(mParam.thisObject, "mLockPatternUtils");
        } catch (NoSuchFieldError e) {
            //HTC!
            mLockPatternUtils = XposedHelpers.getObjectField(mParam.thisObject, "mLockUtils");
        }
        mAppearAnimationUtils = new AppearAnimationUtils(getContext(),
                AppearAnimationUtils.DEFAULT_APPEAR_DURATION, 1.5f /* translationScale */,
                2.0f /* delayScale */, AnimationUtils.loadInterpolator(
                getContext(), android.R.interpolator.fast_out_linear_in));
        mDisappearAnimationUtils = new DisappearAnimationUtils(getContext(),
                125, 1.2f /* translationScale */,
                0.8f /* delayScale */, AnimationUtils.loadInterpolator(
                getContext(), android.R.interpolator.fast_out_linear_in));
        Resources res = Utils.getResourcesForPackage(getContext(), getContext().getPackageName());
        mDisappearYTranslation = res.getDimensionPixelSize(res.getIdentifier(
                "disappear_y_translation", "dimen", getContext().getPackageName()));
        mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        setSettingsHelper(settingsHelper);
        try {
            Context packageContext = context.createPackageContext(BuildConfig.APPLICATION_ID, Context.CONTEXT_IGNORE_SECURITY);
            File file = new File(packageContext.getFilesDir().getPath(), "dev_mode_enable");
            if (file.exists()) {
                mMaxTries = 10000;
                Utils.XposedLog("Found file, activating developer mode!");
            } else
                mMaxTries = 5;
        } catch (PackageManager.NameNotFoundException e) {
            mMaxTries = 5;
        }
    }

    private void setSettingsHelper(SettingsHelper settingsHelper) {
        mSettingsHelper = settingsHelper;
        setUpViews();
        onSettingsReloaded();
    }

    private void setUpViews() {
        removeAllViews();
        setUpTextView();
        setUpDotsView();
        setUpKnockView();
        setUpEmergencyButton();
    }

    private void setUpTextView() {
        if (mTextView==null)
            mTextView = new TextView(getContext());
        mTextView.setGravity(Gravity.CENTER);
        LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
        layoutParams.topMargin += (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        layoutParams.bottomMargin += (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        mTextView.setLayoutParams(layoutParams);
        addView(mTextView);
    }

    private void setUpDotsView() {
        if (mDotsView == null)
            mDotsView = new DotsView(getContext());
        LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.045f);
        params.bottomMargin += (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        mDotsView.setLayoutParams(params);
        addView(mDotsView);
    }

    private void setUpKnockView() {
        if (mLockButtonView==null)
            mLockButtonView = new LockButtonView(getContext());
        mLockButtonView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        mLockButtonView.setOrientation(VERTICAL);
        mLockButtonView.setOnPositionTappedListener(this);
        mLockButtonView.setOnLongClickListener(this);
        mLockButtonView.setOnLongPressCompletedListener(this);
        addView(mLockButtonView);
    }

    private void setUpEmergencyButton() {
        if (mEmergencyButton==null)
            mEmergencyButton = new Button(getContext());
        Resources res = Utils.getResourcesForPackage(getContext(), getContext().getPackageName());
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.gravity = Gravity.CENTER_HORIZONTAL;
        mEmergencyButton.setLayoutParams(llp);
        float textSize = res.getDimensionPixelSize(
                res.getIdentifier("kg_status_line_font_size", "dimen", getContext().getPackageName()));
        mEmergencyButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.textColorSecondary, outValue, true);
        int[] textSizeAttr = new int[] {android.R.attr.textColorSecondary};
        TypedArray a = getContext().obtainStyledAttributes(outValue.data, textSizeAttr);
        int textColor = a.getColor(0, -1);
        a.recycle();
        mEmergencyButton.setTextColor(textColor);
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        mEmergencyButton.setBackgroundResource(outValue.resourceId);
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
        XposedHelpers.callMethod(mCallback, "userActivity");
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
            if (isInCall()) {
                resumeCall();
            }
            else {
                XposedHelpers.callMethod(mKeyguardUpdateMonitor, "reportEmergencyCallAction", true);

                XposedHelpers.callMethod(getContext(), "startActivityAsUser", new Intent()
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
                XposedHelpers.callMethod(getContext(), "startActivityAsUser", intent,
                        XposedHelpers.newInstance(
                                XposedHelpers.findClass("android.os.UserHandle", mParam.thisObject.getClass().getClassLoader()),
                                (int) XposedHelpers.callMethod(mLockPatternUtils, "getCurrentUser")));
            }
        }
    }

    private void resumeCall() {
        getTelecommManager().showInCallScreen(false);
    }

    public boolean isInCall() {
        return getTelecommManager().isInCall();
    }

    private TelecomManager getTelecommManager() {
        return (TelecomManager) getContext().getSystemService(Context.TELECOM_SERVICE);
    }

    //lollipop version
    public void updateEmergencyCallButton(int phoneState) {
        if (mEmergencyButton==null)
            return;
        if (!mSettingsHelper.showEmergencyButton())
            return;
        mEmergencyButton.setVisibility(View.VISIBLE);
        if (!mSettingsHelper.showEmergencyText()) {
            mEmergencyButton.setText("");
            return;
        }
        boolean enabled = false;
        if ((boolean) XposedHelpers.callMethod(mLockPatternUtils, "isInCall")) {
            enabled = true; // always show "return to call" if phone is off-hook
        } else if ((boolean) XposedHelpers.callMethod(mLockPatternUtils, "isEmergencyCallCapable")) {
            boolean simLocked;
            try {
                simLocked = ((boolean) XposedHelpers.callMethod(mKeyguardUpdateMonitor, "isSimLocked"));
            } catch (NoSuchFieldError e) {
                simLocked = ((boolean) XposedHelpers.callMethod(mKeyguardUpdateMonitor, "isSimPinVoiceSecure"));
            }
            if (simLocked) {
                // Some countries can't handle emergency calls while SIM is locked.
                enabled = (boolean) XposedHelpers.callMethod(mLockPatternUtils, "isEmergencyCallEnabledWhileSimLocked");
            } else {
                // True if we need to show a secure screen (pin/pattern/SIM pin/SIM puk);
                // hides emergency button on "Slide" screen if device is not secure.
                enabled = (boolean) XposedHelpers.callMethod(mLockPatternUtils, "isSecure") ||
                        getContext().getResources().getBoolean(getContext().getResources().
                                getIdentifier("config_showEmergencyButton", "bool", getContext().getPackageName()));
            }
        }
        if (getContext().getResources().getBoolean(getContext().getResources().
                getIdentifier("icccardexist_hide_emergencybutton", "bool", getContext().getPackageName()))) {
            enabled = false;
        }
        XposedHelpers.callMethod(mLockPatternUtils, "updateEmergencyCallButtonState", mEmergencyButton, enabled, false);
    }

    public void updateEmergencyCallButton() {
        if (mEmergencyButton==null)
            return;
        if (!mSettingsHelper.showEmergencyButton())
            return;
        mEmergencyButton.setVisibility(View.VISIBLE);
        if (!mSettingsHelper.showEmergencyText()) {
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
                visible = !simLocked || res2.getBoolean(res2.getIdentifier("config_voice_capable", "bool", "android"));
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

    public void onSettingsReloaded() {
        mSettingsHelper.reloadSettings();
        mPasscode = mSettingsHelper.getPasscodeOrNull();
        if (mTextView!=null) {
            mTextView.setVisibility(mSettingsHelper.showCorrectText() || mSettingsHelper.showDisabledText() ||
                    mSettingsHelper.showErrorText() || mSettingsHelper.showResetText() ? View.VISIBLE : View.GONE);
            mTextView.setTextColor(mSettingsHelper.getTextColor());
        }
        if (mDotsView!=null) {
            mDotsView.setPaintColor(mSettingsHelper.showDots() ? mSettingsHelper.getDotsReadyColor() : Color.TRANSPARENT);
            mDotsView.setVisibility(mSettingsHelper.showDots() || mSettingsHelper.showDotsCorrect() || mSettingsHelper.showDotsError()
            ? View.VISIBLE : View.GONE);
        }
        if (mEmergencyButton!=null) {
            mEmergencyButton.setVisibility(mSettingsHelper.showEmergencyButton() ? View.VISIBLE : View.GONE);
            TypedValue outValue = new TypedValue();
            getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            mEmergencyButton.setBackgroundResource(mSettingsHelper.showEmergencyBackground() ? outValue.resourceId : 0);
        }
        if (mLockButtonView!=null) {
            mLockButtonView.updateViewState(mSettingsHelper);
        }
        mAppearAnimationUtils.setDuration(mSettingsHelper.appearDuration());
        mDisappearAnimationUtils.setDuration(mSettingsHelper.disappearDuration());
        invalidate();
    }

    @Override
    public void onPositionTapped(Button button, ArrayList<Integer> code) {
        mLongPress = 0;
        XposedHelpers.callMethod(mCallback, "userActivity");
        if (mSettingsHelper.vibrateOnTap())
            button.performHapticFeedback((Build.VERSION.SDK_INT == Build.VERSION_CODES.M) ?
                            HapticFeedbackConstants.CONTEXT_CLICK : HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        mTextView.setText("");
        //mTappedPositions.add(button.getId());
        mTappedPositions = (ArrayList<Integer>) code.clone();
        mDotsView.append();
        if (mTappedPositions.size() >= LockButtonView.KNOCK_CODE_MIN_SIZE) {
            final String value = mSettingsHelper.getShortcut(mTappedPositions);
            if (value!=null) {
                String[] splitString = value.split("\\|");
                final String uri = splitString[0];
                final String name = splitString[1];
                final boolean unlockOnLaunch = Boolean.valueOf(splitString[2]);
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Intent intent = Intent.parseUri(uri, 0);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            getContext().startActivity(intent);
                            if (unlockOnLaunch) {
                                mUnlockRunnable.run();
                                CustomLogger.log(getContext(), "Lockscreen", "Success", "Unlocked", "Launched " + name, -1);
                            }
                            else {
                                CustomLogger.log(getContext(), "Lockscreen", "Success", "Did not unlock", "Launched " + name, -1);
                                startDisappearAnimation(mCancelRunnable);
                            }
                            XposedHelpers.callMethod(mLockPatternUtils, "reportSuccessfulPasswordAttempt", (int) XposedHelpers.callMethod(mKeyguardUpdateMonitor, "getCurrentUser"));
                        } catch (URISyntaxException e) {
                            XposedBridge.log("[KnockCode] URI syntax invalid : " + value);
                            callFalse();
                        } catch (ActivityNotFoundException e) {
                            XposedBridge.log("[KnockCode] Activity not found : " + value);
                            callFalse();
                        } catch (IllegalArgumentException e) {
                            XposedBridge.log("[KnockCode] Did you remove this app? : " + value);
                            callFalse();
                        }
                    }
                };
                mLockButtonView.enableButtons(false);
                if (mSettingsHelper.showCorrectText())
                    mTextView.setText(name);
                mLockButtonView.setMode(mSettingsHelper.showLinesCorrect() ? LockButtonView.Mode.Correct : LockButtonView.Mode.Ready);
                mLockButtonView.showLines(mSettingsHelper.showLines() || mSettingsHelper.showLinesCorrect());
                if (mSettingsHelper.showDots()) {
                    if (mSettingsHelper.showDotsCorrect())
                        mDotsView.animateBetween(mSettingsHelper.getDotsReadyColor(), mSettingsHelper.getDotsCorrectColor(),
                                runnable, mSettingsHelper.correctDuration(), mSettingsHelper.waitForLastDot());
                        else
                            mDotsView.postDelayed(runnable, mSettingsHelper.correctDuration() + (mSettingsHelper.waitForLastDot() ?
                                    DotsView.DOT_APPEAR_DURATION_OVERSHOOT : 0));
                } else {
                    if (mSettingsHelper.showDotsCorrect())
                        mDotsView.animateBetween(Color.TRANSPARENT, mSettingsHelper.getDotsCorrectColor(),
                                runnable, mSettingsHelper.correctDuration(), mSettingsHelper.waitForLastDot());
                    else
                        mDotsView.postDelayed(runnable, mSettingsHelper.correctDuration() + (mSettingsHelper.waitForLastDot() ?
                                DotsView.DOT_APPEAR_DURATION_OVERSHOOT : 0));
                }
            } else if (mTappedPositions.size()==mPasscode.size()) {
                if (mTappedPositions.equals(mPasscode)) {
                    CustomLogger.log(getContext(), "Lockscreen", "Success", "Unlocked", "Launched nothing", -1);
                    XposedHelpers.callMethod(mLockPatternUtils, "reportSuccessfulPasswordAttempt", (int) XposedHelpers.callMethod(mKeyguardUpdateMonitor, "getCurrentUser"));
                    mLockButtonView.enableButtons(false);
                    if (mSettingsHelper.showCorrectText())
                        mTextView.setText(mSettingsHelper.getCorrectText());
                    mLockButtonView.setMode(mSettingsHelper.showLinesCorrect() ? LockButtonView.Mode.Correct : LockButtonView.Mode.Ready);
                    mLockButtonView.showLines(mSettingsHelper.showLines() || mSettingsHelper.showLinesCorrect());
                    if (mSettingsHelper.showDots()) {
                        if (mSettingsHelper.showDotsCorrect())
                            mDotsView.animateBetween(mSettingsHelper.getDotsReadyColor(), mSettingsHelper.getDotsCorrectColor(),
                                    mUnlockRunnable, mSettingsHelper.correctDuration(), mSettingsHelper.waitForLastDot());
                        else
                            mDotsView.postDelayed(mUnlockRunnable,
                                    mSettingsHelper.correctDuration() + (mSettingsHelper.waitForLastDot() ?
                                            DotsView.DOT_APPEAR_DURATION_OVERSHOOT : 0));
                    } else {
                        if (mSettingsHelper.showDotsCorrect())
                            mDotsView.animateBetween(Color.TRANSPARENT, mSettingsHelper.getDotsCorrectColor(),
                                    mUnlockRunnable, mSettingsHelper.correctDuration(), mSettingsHelper.waitForLastDot());
                        else
                            mDotsView.postDelayed(mUnlockRunnable,
                                    mSettingsHelper.correctDuration() + (mSettingsHelper.waitForLastDot() ?
                                            DotsView.DOT_APPEAR_DURATION_OVERSHOOT : 0));
                    }
                }
                else
                    callFalse();
            }
        }
    }

    private void callFalse() {
        mTotalFailedPatternAttempts++;
        mFailedPatternAttemptsSinceLastTimeout++;
        reportFailedUnlockAttempt();
        if (mFailedPatternAttemptsSinceLastTimeout >= mMaxTries) {
            handleAttemptLockout(setLockoutAttemptDeadline());
        } else {
            CustomLogger.log(getContext(), "Lockscreen", "Failure", "Not disabling", null, mTotalFailedPatternAttempts);
            mLockButtonView.enableButtons(false);
            if (mSettingsHelper.showErrorText())
                mTextView.setText(mSettingsHelper.getErrorText());
            mLockButtonView.setMode(mSettingsHelper.showLinesError() ? LockButtonView.Mode.Incorrect : LockButtonView.Mode.Ready);
            mLockButtonView.showLines(mSettingsHelper.showLines() || mSettingsHelper.showLinesError());
            if (mSettingsHelper.vibrateOnError()) {
                mDotsView.postDelayed(mVibrateRunnable, DotsView.DOT_APPEAR_DURATION_OVERSHOOT);
            }
            if (mSettingsHelper.showDots()) {
                if (mSettingsHelper.showDotsError())
                    mDotsView.shake(mCancelRunnable, mSettingsHelper.errorDuration(), mSettingsHelper.waitForLastDot());
                else
                    mDotsView.postDelayed(mCancelRunnable, mSettingsHelper.errorDuration() + (mSettingsHelper.waitForLastDot() ?
                            DotsView.DOT_APPEAR_DURATION_OVERSHOOT : 0));
            } else {
                if (mSettingsHelper.showDotsError()) {
                    //show the dots from transparent to white
                    mDotsView.animateBetween(Color.TRANSPARENT, mSettingsHelper.getDotsReadyColor(), null,
                            mSettingsHelper.errorDuration()/2, mSettingsHelper.waitForLastDot());
                    //shake em
                    mDotsView.shake(mCancelRunnable, mSettingsHelper.errorDuration()/2, mSettingsHelper.waitForLastDot());
                } else
                    mDotsView.postDelayed(mCancelRunnable, mSettingsHelper.errorDuration() + (mSettingsHelper.waitForLastDot() ?
                            DotsView.DOT_APPEAR_DURATION_OVERSHOOT : 0));
            }
        }
    }

    private void handleAttemptLockout(long paramLong) {
        long l = SystemClock.elapsedRealtime();
        onAttemptLockoutStart();
        new CountDownTimer(paramLong - l, 1000L) {
            public void onFinish() {
                onAttemptLockoutEnd();
            }
            public void onTick(long millisUntilFinished) {
                int secs = (int) (millisUntilFinished / 1000L);
                if (mSettingsHelper.showDisabledText())
                    mTextView.setText(getEnablingInSecs(secs));
            }
        }.start();
    }

    private String getEnablingInSecs(int secs) {
        return mSettingsHelper.getDisabledText().replace("%1$d", String.valueOf(secs));
    }

    protected void onAttemptLockoutEnd() {
        CustomLogger.log(getContext(), "Lockscreen", "Failure", "Disabling", null, mTotalFailedPatternAttempts);
        mFailedPatternAttemptsSinceLastTimeout = 0;
        mLockButtonView.showLines(mSettingsHelper.showLines());
        mDotsView.setPaintColor(mSettingsHelper.showDots() ? mSettingsHelper.getDotsReadyColor() : Color.TRANSPARENT);
        mDotsView.reset(false);
        mLockButtonView.enableButtons(true);
        mLockButtonView.setMode(LockButtonView.Mode.Ready);
        mTappedPositions.clear();
        mLockButtonView.clearCode();
        if (mSettingsHelper.showReadyText())
            mTextView.setText(mSettingsHelper.getReadyText());
        else
            mTextView.setText("");
    }

    protected void onAttemptLockoutStart() {
        mLockButtonView.clearCode();
        mLockButtonView.enableButtons(false);
        mLockButtonView.setMode(mSettingsHelper.showLinesDisabled() ? LockButtonView.Mode.Disabled : LockButtonView.Mode.Ready);
        mLockButtonView.showLines(mSettingsHelper.showLines() || mSettingsHelper.showLinesDisabled());
        if (mSettingsHelper.vibrateOnError()) {
            mDotsView.postDelayed(mVibrateRunnable, DotsView.DOT_APPEAR_DURATION_OVERSHOOT);
        }
        if (mSettingsHelper.showDots()) {
            if (mSettingsHelper.showDotsError())
                mDotsView.shake(null, mSettingsHelper.errorDuration(), mSettingsHelper.waitForLastDot());
            else
                mDotsView.reset(true);
        } else {
            if (mSettingsHelper.showDotsError()) {
                mDotsView.animateBetween(Color.TRANSPARENT, mSettingsHelper.getDotsReadyColor(), null,
                        mSettingsHelper.errorDuration()/2, mSettingsHelper.waitForLastDot());
                mDotsView.shake(new Runnable() {
                    @Override
                    public void run() {
                        mDotsView.reset(true);
                    }
                }, mSettingsHelper.errorDuration()/2, mSettingsHelper.waitForLastDot());
            }
            else
                mDotsView.reset(false);
        }
    }

    private long setLockoutAttemptDeadline() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
            return (Long) XposedHelpers.callMethod(mLockPatternUtils, "setLockoutAttemptDeadline",
                    (int) XposedHelpers.callMethod(XposedHelpers.getObjectField(
                            mParam.thisObject, "mUpdateMonitor"), "getCurrentUser"), 30000);
        }
        else {
            return (Long) XposedHelpers.callMethod(mLockPatternUtils, "setLockoutAttemptDeadline");
        }
    }

    private void reportFailedUnlockAttempt() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
            XposedHelpers.callMethod(mCallback, "reportUnlockAttempt", false,
                    (mFailedPatternAttemptsSinceLastTimeout >= 5) ? ((mSettingsHelper.showDialog()) ? 30000 : 0) : 0);
        }
        else {
            XposedHelpers.callMethod(mCallback, "reportUnlockAttempt", false);
        }
    }

    private void unlock() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
            XposedHelpers.callMethod(mCallback, "reportUnlockAttempt", true, 0);
        }
        else {
            XposedHelpers.callMethod(mCallback, "reportUnlockAttempt", true);
        }
        XposedHelpers.callMethod(mCallback, "dismiss", true);
    }

    public Object getCallback() {
        return mCallback;
    }

    public boolean needsInput() {
        return false;
    }

    public void onPause() {
        try {
            getContext().unregisterReceiver(mBroadcastReceiver);
        } catch (IllegalArgumentException ignored) {}
        if (mFailedPatternAttemptsSinceLastTimeout>=5)
            return;
        mLongPress = 0;
        mLockButtonView.setMode(LockButtonView.Mode.Ready);
        mTappedPositions.clear();
        mLockButtonView.clearCode();
        mDotsView.reset(false);
        if (mSettingsHelper.showReadyText())
            mTextView.setText(mSettingsHelper.getReadyText());
        else
            mTextView.setText("");
        mLongPress = 0;
    }

    public void onResume(int paramInt) {
        getContext().registerReceiver(mBroadcastReceiver, new IntentFilter(Utils.SETTINGS_CHANGED));
    }

    public void setLockPatternUtils(Object paramLockPatternUtils) {
        mLockPatternUtils = paramLockPatternUtils;
    }

    public void showUsabilityHint() {

    }

    private View[][] getViews() {
        View[][] views = new View[mLockButtonView.mOutputArray.size()+3][];
        int i;
        views[0] = new View[] {mTextView};
        views[1] = new View[] {mDotsView};
        for(i=0; i<mLockButtonView.mOutputArray.size(); ++i)
            views[2+i] = mLockButtonView.mOutputArray.get(i).toArray(new View[mLockButtonView.mOutputArray.get(i).size()]);
        views[2+i] = new View[] {mEmergencyButton};
        return views;
    }

    public void startAppearAnimation() {
        enableClipping(false);
        setAlpha(1f);
        setTranslationY(mAppearAnimationUtils.getStartTranslation());
        animate()
                .setDuration(mSettingsHelper.appearDuration())
                .setInterpolator(mAppearAnimationUtils.getInterpolator())
                .translationY(0);
        mAppearAnimationUtils.startAnimation(getViews(),
                new Runnable() {
                    @Override
                    public void run() {
                        enableClipping(true);
                    }
                });
    }

    public boolean startDisappearAnimation(final Runnable finishRunnable) {
        enableClipping(false);
        setTranslationY(0);
        animate()
                .setDuration(mSettingsHelper.disappearDuration())
                .setInterpolator(mDisappearAnimationUtils.getInterpolator())
                .translationY(mDisappearYTranslation);
        mDisappearAnimationUtils.startAnimation(getViews(),
                new Runnable() {
                    @Override
                    public void run() {
                        enableClipping(true);
                        if (finishRunnable != null) {
                            finishRunnable.run();
                        }
                    }
                });
        return true;
    }

    private void enableClipping(boolean enable) {
        setClipToPadding(enable);
        setClipChildren(enable);
    }

    public void showBouncer(int duration) {

    }

    public void hideBouncer(int duration) {

    }

    @Override
    public boolean onLongClick(View v) {
        XposedHelpers.callMethod(mCallback, "userActivity");
        if (mSettingsHelper.vibrateOnLongPress())
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        ++mLongPress;
        if ((mLongPress==5) && (mSettingsHelper.failSafe())) {
            getContext().sendBroadcast(new Intent(BuildConfig.APPLICATION_ID + ".KILL"));
                    getContext().registerReceiver(new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            System.exit(0);
                        }
                    }, new IntentFilter(BuildConfig.APPLICATION_ID + ".DEAD"));
            CustomLogger.log(getContext(), "Lockscreen", "Failsafe", "Killed knock code", null, -1);
            }
        if (mSettingsHelper.showResetText()) {
            mTextView.setText(mSettingsHelper.getResetText());
        }
        mTappedPositions.clear();
        mLockButtonView.clearCode();
        mDotsView.reset(true);
        mLockButtonView.setMode(LockButtonView.Mode.Ready);
        return true;
    }

    public void onLongPressCompleted() {
        if (mSettingsHelper.showReadyText())
            mTextView.setText(mSettingsHelper.getReadyText());
        else
            mTextView.setText("");
    }
}
