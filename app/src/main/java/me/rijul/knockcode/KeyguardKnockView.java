package me.rijul.knockcode;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
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
import java.net.URISyntaxException;
import java.util.ArrayList;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by rijul on 2/3/16.
 */
@SuppressLint("ViewConstructor")
public class KeyguardKnockView extends LinearLayout implements LockButtonView.OnPositionTappedListener, View.OnLongClickListener {
    //constants
    public static final int SCREEN_ON = 1;
    public static final int VIEW_REVEALED = 2;
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
    //runnables to clear or unlock
    private Runnable mCancelRunnable = new Runnable() {
        public void run() {
            mLockButtonView.showLines(mSettingsHelper.showLines());
            mDotsView.setPaintColor(mSettingsHelper.showDots() ? mSettingsHelper.getDotsReadyColor() : Color.TRANSPARENT);
            mDotsView.reset(true);
            mLockButtonView.enableButtons(true);
            mLockButtonView.setMode(LockButtonView.Mode.Ready);
            mTappedPositions.clear();
            mTextView.setText("");
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
        mAppearAnimationUtils = new AppearAnimationUtils(getContext());
        mDisappearAnimationUtils = new DisappearAnimationUtils(getContext(),
                125, 1.2f /* translationScale */,
                0.8f /* delayScale */, AnimationUtils.loadInterpolator(
                getContext(), android.R.interpolator.fast_out_linear_in));
        Resources res = Utils.getResourcesForPackage(getContext(), getContext().getPackageName());
        mDisappearYTranslation = res.getDimensionPixelSize(res.getIdentifier(
                "disappear_y_translation", "dimen", getContext().getPackageName()));
        setSettingsHelper(settingsHelper);
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
        mLockButtonView.setPatternSize(mSettingsHelper.getPatternSize());
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
        if (mTextView!=null)
            mTextView.setVisibility(mSettingsHelper.showText() ? View.VISIBLE : View.GONE);
        if (mDotsView!=null)
            mDotsView.setPaintColor(mSettingsHelper.showDots() ? mSettingsHelper.getDotsReadyColor() : Color.TRANSPARENT);
        if (mEmergencyButton!=null)
            mEmergencyButton.setVisibility(mSettingsHelper.showEmergencyButton() ? View.VISIBLE : View.GONE);
        if (mLockButtonView!=null) {
            mLockButtonView.updateViewState(mSettingsHelper);
        }
        invalidate();
    }

    @Override
    public void onPositionTapped(Button button) {
        mLongPress = 0;
        XposedHelpers.callMethod(mCallback, "userActivity");
        if (mSettingsHelper.vibrateOnTap())
            button.performHapticFeedback((Build.VERSION.SDK_INT == Build.VERSION_CODES.M) ?
                            HapticFeedbackConstants.CONTEXT_CLICK : HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        mTextView.setText("");
        mTappedPositions.add(button.getId());
        mDotsView.append();
        if (mTappedPositions.size() >= LockButtonView.KNOCK_CODE_MIN_SIZE) {
            final String value = mSettingsHelper.getShortcut(mTappedPositions);
            if (value!=null) {
                final int separator = value.indexOf('|');
                final String uri = value.substring(0, separator);
                String name = value.substring(separator + 1);
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Intent intent = Intent.parseUri(uri, 0);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            getContext().startActivity(intent);
                            if (!mSettingsHelper.customShortcutsDontUnlock())
                                mUnlockRunnable.run();
                            else {
                                startDisappearAnimation(mCancelRunnable);
                            }
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
                mTextView.setText(name);
                mLockButtonView.setMode(mSettingsHelper.showLinesCorrect() ? LockButtonView.Mode.Correct : LockButtonView.Mode.Ready);
                mLockButtonView.showLines(mSettingsHelper.showLines() || mSettingsHelper.showLinesCorrect());
                if (mSettingsHelper.showDots()) {
                    if (mSettingsHelper.showDotsCorrect())
                        mDotsView.animateBetween(mSettingsHelper.getDotsReadyColor(), mSettingsHelper.getDotsCorrectColor(), runnable, mSettingsHelper.getDotsCorrectLag(), false);
                    else
                        runnable.run();
                }
                else {
                    if (mSettingsHelper.showDotsCorrect())
                        mDotsView.animateBetween(Color.TRANSPARENT, mSettingsHelper.getDotsCorrectColor(), runnable, mSettingsHelper.getDotsCorrectLag(), false);
                    else
                        runnable.run();
                    }
            } else if (mTappedPositions.size()==mPasscode.size()) {
                if (mTappedPositions.equals(mPasscode)) {
                    mTextView.setText(Utils.getString(getContext(), R.string.knock_code_correct));
                    mLockButtonView.setMode(mSettingsHelper.showLinesCorrect() ? LockButtonView.Mode.Correct : LockButtonView.Mode.Ready);
                    mLockButtonView.showLines(mSettingsHelper.showLines() || mSettingsHelper.showLinesCorrect());
                    if (mSettingsHelper.showDots()) {
                        if (mSettingsHelper.showDotsCorrect())
                            mDotsView.animateBetween(mSettingsHelper.getDotsReadyColor(), mSettingsHelper.getDotsCorrectColor(), mUnlockRunnable, mSettingsHelper.getDotsCorrectLag(), false);
                        else
                            mUnlockRunnable.run();
                    }
                    else {
                        if (mSettingsHelper.showDotsCorrect())
                            mDotsView.animateBetween(Color.TRANSPARENT, mSettingsHelper.getDotsCorrectColor(), mUnlockRunnable, mSettingsHelper.getDotsCorrectLag(), false);
                        else
                            mUnlockRunnable.run();
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
        if (mFailedPatternAttemptsSinceLastTimeout >= 5) {
            handleAttemptLockout(setLockoutAttemptDeadline());
        } else {
            mTextView.setText(Utils.getString(getContext(), R.string.knock_code_wrong));
            mLockButtonView.setMode(mSettingsHelper.showLinesError() ? LockButtonView.Mode.Incorrect : LockButtonView.Mode.Ready);
            mLockButtonView.showLines(mSettingsHelper.showLines() || mSettingsHelper.showLinesError());
            mLockButtonView.enableButtons(false);
            if (mSettingsHelper.showDots()) {
                if (mSettingsHelper.showDotsError())
                    mDotsView.animateBetween(mSettingsHelper.getDotsReadyColor(), mSettingsHelper.getDotsWrongColor(), mCancelRunnable, mSettingsHelper.getDotsErrorLag(), true);
                else
                    mDotsView.postDelayed(mCancelRunnable,
                            mSettingsHelper.showText() ?
                                    (mSettingsHelper.showLinesError() ? mSettingsHelper.getLinesErrorLag() :
                                            mSettingsHelper.getTextLag()) : 0);
            } else {
                if (mSettingsHelper.showDotsError())
                    mDotsView.animateBetween(Color.TRANSPARENT, mSettingsHelper.getDotsWrongColor(), mCancelRunnable, mSettingsHelper.getDotsErrorLag(), true);
                else
                    mDotsView.postDelayed(mCancelRunnable,
                            mSettingsHelper.showText() ?
                                    (mSettingsHelper.showLinesError() ? mSettingsHelper.getLinesErrorLag() :
                                            mSettingsHelper.getTextLag()) : 0);
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
                mTextView.setText(getEnablingInSecs(secs));
            }
        }.start();
    }

    private String getEnablingInSecs(int secs) {
        return Utils.getString(getContext(), R.string.knock_code_enabling, secs);
    }

    protected void onAttemptLockoutEnd() {
        mFailedPatternAttemptsSinceLastTimeout = 0;
        mLockButtonView.showLines(mSettingsHelper.showLines());
        mDotsView.setPaintColor(mSettingsHelper.showDots() ? mSettingsHelper.getDotsReadyColor() : Color.TRANSPARENT);
        mDotsView.reset(false);
        mLockButtonView.enableButtons(true);
        mLockButtonView.setMode(LockButtonView.Mode.Ready);
        mTappedPositions.clear();
        mTextView.setText("");
    }

    protected void onAttemptLockoutStart() {
        mLockButtonView.enableButtons(false);
        mLockButtonView.setMode(mSettingsHelper.showLinesDisabled() ? LockButtonView.Mode.Disabled : LockButtonView.Mode.Ready);
        mLockButtonView.showLines(mSettingsHelper.showLines() || mSettingsHelper.showLinesDisabled());
        if (mSettingsHelper.showDots()) {
            if (mSettingsHelper.showDotsError())
                mDotsView.animateBetween(mSettingsHelper.getDotsReadyColor(), mSettingsHelper.getDotsWrongColor(), null, mSettingsHelper.getDotsErrorLag(), true);
            else
                mDotsView.reset(true);
        } else {
            if (mSettingsHelper.showDotsError())
                mDotsView.animateBetween(Color.TRANSPARENT, mSettingsHelper.getDotsWrongColor(), null, mSettingsHelper.getDotsErrorLag(), true);
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
        if (mFailedPatternAttemptsSinceLastTimeout>=5)
            return;
        mLockButtonView.setMode(LockButtonView.Mode.Ready);
        mTextView.setText("");
        mLongPress = 0;
        try {
            getContext().unregisterReceiver(mBroadcastReceiver);
        } catch (IllegalArgumentException e) {}
    }

    public void onResume(int paramInt) {
        if (mFailedPatternAttemptsSinceLastTimeout>=5)
            return;
        mLockButtonView.setMode(LockButtonView.Mode.Ready);
        mTextView.setText("");
        mLongPress = 0;
        getContext().registerReceiver(mBroadcastReceiver, new IntentFilter(Utils.SETTINGS_CHANGED));
    }

    public void setLockPatternUtils(Object paramLockPatternUtils) {
        mLockPatternUtils = paramLockPatternUtils;
    }

    public void showUsabilityHint() {

    }

    public void startAppearAnimation() {
        enableClipping(false);
        setAlpha(1f);
        setTranslationY(mAppearAnimationUtils.getStartTranslation());
        animate()
                .setDuration(500)
                .setInterpolator(mAppearAnimationUtils.getInterpolator())
                .translationY(0);
        mAppearAnimationUtils.startAnimation(new View[]{mTextView, mDotsView, mLockButtonView, mEmergencyButton},
                new Runnable() {
                    @Override
                    public void run() {
                        enableClipping(true);
                    }
                });
    }

    public boolean startDisappearAnimation(Runnable finishRunnable) {
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
            }
        mTextView.setText(Utils.getString(getContext(), R.string.knock_code_longpress_cleared));
        mTappedPositions.clear();
        mDotsView.reset(true);
        mLockButtonView.setMode(LockButtonView.Mode.Ready);
        return true;
    }
}
