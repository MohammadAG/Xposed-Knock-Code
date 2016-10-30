package me.rijul.knockcode;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XModuleResources;
import android.graphics.Point;
import android.os.Build;
import android.provider.Settings.Secure;
import android.util.TypedValue;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ViewFlipper;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage, IXposedHookZygoteInit, IXposedHookInitPackageResources {

    private XC_MethodHook mUpdateSecurityViewHook;
    private XC_MethodHook mShowSecurityScreenHook;
    private XC_MethodHook mKeyguardHostViewInitHook;
    private XC_MethodHook mStartAppearAnimHook;
    private XC_MethodHook mStartDisAppearAnimHook;
    private XC_MethodHook mOnPauseHook;
    private XC_MethodHook mOnResumeHook;
    private XC_MethodHook mOnSimStateChangedHook;
    private XC_MethodHook mOnPhoneStateChangedHook;
    private XC_MethodHook mShowTimeoutDialogHook;
    private XC_MethodHook mOnScreenTurnedOnHook;
    private XC_MethodHook mOnScreenTurnedOffHook;
    private XC_MethodHook mShowNextSecurityScreenOrFinishHook;
    protected static KeyguardKnockView mKnockCodeView;
    private static SettingsHelper mSettingsHelper;
    private String modulePath;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mSettingsHelper!=null)
                mSettingsHelper.reloadSettings();
        }
    };
    private XC_MethodHook mShowPrimarySecurityScreenHook;

    public enum UnlockPolicy { NEVER, ALWAYS, NO_CLEARABLE_NOTIF, NO_NOTIF };

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        modulePath = startupParam.modulePath;
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resParam) throws Throwable {
        if ((resParam.packageName.contains("android.keyguard")) || (resParam.packageName.contains("com.android.systemui"))) {
            XModuleResources modRes = XModuleResources.createInstance(modulePath, resParam.res);
            Utils.XposedLog("Setting resources to fullscreen!");
            resParam.res.setReplacement(resParam.packageName, "dimen", "keyguard_security_view_margin", modRes.fwd(R.dimen.replace_keyguard_security_view_margin));
            resParam.res.setReplacement(resParam.packageName, "dimen", "keyguard_security_width", modRes.fwd(R.dimen.replace_keyguard_security_max_height));
            resParam.res.setReplacement(resParam.packageName, "dimen", "keyguard_security_max_height", modRes.fwd(R.dimen.replace_keyguard_security_max_height));
            //resParam.res.setReplacement(resParam.packageName, "integer", "keyguard_max_notification_count", 0);
        }
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return;
        if (lpparam.packageName.equals("me.rijul.knockcode")) {
            Class<?> SettingsActivityClazz = XposedHelpers.findClass("me.rijul.knockcode.SettingsActivity", lpparam.classLoader);
            XposedHelpers.setStaticBooleanField(SettingsActivityClazz, "MODULE_INACTIVE", false);
            XposedHelpers.findAndHookMethod(SettingsActivityClazz, "getXposedVersionCode",
                    XC_MethodReplacement.returnConstant(BuildConfig.VERSION_CODE));
        } else if (lpparam.packageName.equals("com.htc.lockscreen")) {
            Utils.XposedLog("HTC Device");
            createHooksIfNeeded("com.htc.lockscreen.keyguard");
            hookMethods("com.htc.lockscreen.keyguard", lpparam);
        } else if ((lpparam.packageName.contains("android.keyguard")) || (lpparam.packageName.contains("com.android.systemui"))) {
            Utils.XposedLog("AOSPish Device");
            createHooksIfNeeded("com.android.keyguard");
            hookMethods("com.android.keyguard", lpparam);
        }
    }

    private void hookMethods(String packageName, LoadPackageParam lpparam) {
        Class <?> KeyguardHostView = XposedHelpers.findClass(packageName + ".KeyguardSecurityContainer", lpparam.classLoader);
        XposedBridge.hookAllConstructors(KeyguardHostView, mKeyguardHostViewInitHook);
        findAndHookMethod(KeyguardHostView, "startAppearAnimation", mStartAppearAnimHook);
        findAndHookMethod(KeyguardHostView, "startDisappearAnimation", Runnable.class, mStartDisAppearAnimHook);
        findAndHookMethod(KeyguardHostView, "onPause", mOnPauseHook);
        findAndHookMethod(KeyguardHostView, "onResume", int.class, mOnResumeHook);
        Class<?> KeyguardUpdateMonitorCallback = XposedHelpers.findClass(packageName + ".KeyguardUpdateMonitorCallback",
                lpparam.classLoader);
        findAndHookMethod(KeyguardHostView, "showSecurityScreen", packageName + ".KeyguardSecurityModel$SecurityMode",
                    mShowSecurityScreenHook);
        try {
            XposedBridge.hookAllMethods(KeyguardUpdateMonitorCallback, "onSimStateChanged", mOnSimStateChangedHook);
        } catch (NoSuchMethodError ignored) {}
        findAndHookMethod(KeyguardUpdateMonitorCallback, "onPhoneStateChanged", int.class, mOnPhoneStateChangedHook);

        //marshmallow vs lollipop
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
            findAndHookMethod(KeyguardHostView, "showTimeoutDialog", int.class, mShowTimeoutDialogHook);
            findAndHookMethod(KeyguardHostView, "updateSecurityView", View.class, mUpdateSecurityViewHook);
        } else {
            findAndHookMethod(KeyguardHostView, "showTimeoutDialog", mShowTimeoutDialogHook);
            findAndHookMethod(KeyguardHostView, "updateSecurityView", View.class, boolean.class, mUpdateSecurityViewHook);
        }
        try {
            Class<?> keyguardViewManager = XposedHelpers.findClass("com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager",
                    lpparam.classLoader);
            XposedBridge.hookAllMethods(keyguardViewManager, "onScreenTurnedOn", mOnScreenTurnedOnHook);
            XposedBridge.hookAllMethods(keyguardViewManager, "onScreenTurnedOff", mOnScreenTurnedOffHook);
        } catch (NoSuchMethodError ignored) {}

        /*
        XposedHelpers.findAndHookMethod("com.android.keyguard.KeyguardStatusView", lpparam.classLoader, "onFinishInflate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                GridLayout self = (GridLayout) param.thisObject;
                int children = self.getChildCount();
                for(int i=0; i<children; ++i)
                    self.getChildAt(i).setVisibility(View.GONE);
                Utils.XposedLog("Removed all views!");
            }
        });
        */
        //findAndHookMethod(KeyguardHostView, "showPrimarySecurityScreen", boolean.class, mShowPrimarySecurityScreenHook);
        //findAndHookMethod(KeyguardHostView, "showNextSecurityScreenOrFinish", boolean.class, mShowNextSecurityScreenOrFinishHook);
    }

    private void createHooksIfNeeded(final String keyguardPackageName) {
        mKeyguardHostViewInitHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mSettingsHelper == null) {
                    Context context = (Context) param.args[0];
                    String uuid = Secure.getString(context.getContentResolver(),
                            Secure.ANDROID_ID);
                    mSettingsHelper = new SettingsHelper(uuid);
                }
            }
        };

        mOnSimStateChangedHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if ((mSettingsHelper==null) || (mSettingsHelper.isDisabled()))
                    return;
                if (mKnockCodeView != null) {
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
                        mKnockCodeView.updateEmergencyCallButton();
                    }
                    else {
                        mKnockCodeView.updateEmergencyCallButton(0);
                    }
                    param.setResult(null);
                }
            }
        };

        mShowTimeoutDialogHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mSettingsHelper==null|| mSettingsHelper.showDialog())
                    return;
                if (mKnockCodeView != null)
                    param.setResult(null);
            }
        };

        mOnPhoneStateChangedHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if ((mSettingsHelper==null) || (mSettingsHelper.isDisabled()))
                    return;
                if (mKnockCodeView!=null) {
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
                        mKnockCodeView.updateEmergencyCallButton();
                    } else {
                        mKnockCodeView.updateEmergencyCallButton((int) param.args[0]);
                        param.setResult(null);
                    }
                }
            }
        };

        mUpdateSecurityViewHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if ((mSettingsHelper==null) || (mSettingsHelper.isDisabled()))
                    return;
                View view = (View) param.args[0];
                if (view instanceof KeyguardKnockView) {
                    KeyguardKnockView unlockView = (KeyguardKnockView) view;
                    unlockView.setKeyguardCallback(XposedHelpers.getObjectField(param.thisObject, "mCallback"));
                    unlockView.setLockPatternUtils(XposedHelpers.getObjectField(param.thisObject, "mLockPatternUtils"));
                    try {
                        Boolean isBouncing = (Boolean) param.args[1];
                        if (isBouncing)
                            unlockView.showBouncer(0);
                        else
                            unlockView.hideBouncer(0);
                        XposedHelpers.setObjectField(param.thisObject, "mIsBouncing", isBouncing);
                    }
                    catch (Exception ignored) {}
                    param.setResult(null);
                }
            }
        };

        mShowSecurityScreenHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Context mContext = ((FrameLayout) param.thisObject).getContext();
                mContext.registerReceiver(broadcastReceiver, new IntentFilter(Utils.SETTINGS_CHANGED));
                if ((mSettingsHelper==null) || (mSettingsHelper.isDisabled())) {
                    //find whichever view is enabled
                    View pinView = (View) callMethod(param.thisObject, "getSecurityView", param.args[0]);
                    ViewGroup.LayoutParams layoutParams = pinView.getLayoutParams();
                    //set width and height
                    layoutParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 400, mContext.getResources().getDisplayMetrics());
                    layoutParams.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 320, mContext.getResources().getDisplayMetrics());
                    pinView.setLayoutParams(layoutParams);
                    mKnockCodeView = null;
                    return;
                }
                Object securityMode = param.args[0];
                Class<?> SecurityMode = XposedHelpers.findClass(keyguardPackageName + ".KeyguardSecurityModel$SecurityMode",
                        param.thisObject.getClass().getClassLoader());
                Object pinMode = XposedHelpers.getStaticObjectField(SecurityMode, "PIN");
                if (!pinMode.equals(securityMode)) {
                    //find whichever view is enabled
                    View pinView = (View) callMethod(param.thisObject, "getSecurityView", param.args[0]);
                    ViewGroup.LayoutParams layoutParams = pinView.getLayoutParams();
                    //set width and height
                    layoutParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 400, mContext.getResources().getDisplayMetrics());
                    layoutParams.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 320, mContext.getResources().getDisplayMetrics());
                    pinView.setLayoutParams(layoutParams);
                    mKnockCodeView = null;
                    return;
                }
                Object mCurrentSecuritySelection = XposedHelpers.getObjectField(param.thisObject, "mCurrentSecuritySelection");
                if (securityMode == mCurrentSecuritySelection) {
                    param.setResult(null);
                    return;
                }
                View oldView = (View) callMethod(param.thisObject, "getSecurityView", mCurrentSecuritySelection);
                mKnockCodeView = new KeyguardKnockView(mContext,param,mSettingsHelper,keyguardPackageName);
                View newView = mKnockCodeView;

                // pause old view, and ignore requests from it
                if (oldView != null) {
                    Object mNullCallback = getObjectField(param.thisObject, "mNullCallback");
                    callMethod(oldView, "onPause");
                    callMethod(oldView, "setKeyguardCallback", mNullCallback);
                }

                View pinView = (View) callMethod(param.thisObject, "getSecurityView", pinMode);
                ViewGroup.LayoutParams layoutParams = pinView.getLayoutParams();
                if (mSettingsHelper.fullScreen())  {
                    Utils.XposedLog("Setting view to fullscreen!");
                    Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    int rotation = display.getRotation();
                    if (rotation==Surface.ROTATION_0 || rotation==Surface.ROTATION_180) {
                        layoutParams.height = size.y;
                        layoutParams.width = size.x;
                    } else {
                        layoutParams.height = size.x;
                        layoutParams.width = size.y;
                    }
                } else {
                    layoutParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 400, mContext.getResources().getDisplayMetrics());
                    layoutParams.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 320, mContext.getResources().getDisplayMetrics());
                }
                newView.setLayoutParams(layoutParams);


                //show new view, and set a callback for it
                Object mCallback = getObjectField(param.thisObject, "mCallback");
                callMethod(newView, "onResume", KeyguardKnockView.VIEW_REVEALED);
                callMethod(newView, "setKeyguardCallback", mCallback);

                // add the view to the viewflipper and show it
                ViewFlipper mSecurityViewContainer = (ViewFlipper) getObjectField(param.thisObject, "mSecurityViewFlipper");
                mSecurityViewContainer.addView(newView);
                final int childCount = mSecurityViewContainer.getChildCount();

                for (int i = 0; i < childCount; i++) {
                    if (mSecurityViewContainer.getChildAt(i) instanceof KeyguardKnockView) {
                        mSecurityViewContainer.setDisplayedChild(i);
                        mSecurityViewContainer.getChildAt(i).requestFocus();
                        break;
                    }
                }

                // set that knock code is currently selected
                XposedHelpers.setObjectField(param.thisObject, "mCurrentSecuritySelection", securityMode);
                param.setResult(null);
            }
        };

        mStartAppearAnimHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if ((mSettingsHelper == null) || (mSettingsHelper.isDisabled()))
                    return;
                if (mKnockCodeView != null) {
                    mKnockCodeView.startAppearAnimation();
                    param.setResult(null);
                }
            }
        };

        mStartDisAppearAnimHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if ((mSettingsHelper==null) || (mSettingsHelper.isDisabled()))
                    return;
                if (mKnockCodeView!=null) {
                    param.setResult(mKnockCodeView.startDisappearAnimation((Runnable) param.args[0]));
                }
            }
        };

        mOnPauseHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if ((mSettingsHelper==null) || (mSettingsHelper.isDisabled()))
                    return;
                    if (mKnockCodeView!=null) {
                        mKnockCodeView.onPause();
                        param.setResult(null);
                    }
            }
        };

        mOnResumeHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if ((mSettingsHelper==null) || (mSettingsHelper.isDisabled()))
                    return;
                if (mKnockCodeView!=null) {
                    mKnockCodeView.onResume((int) param.args[0]);
                    param.setResult(null);
                }
            }
        };

        mOnScreenTurnedOnHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (mSettingsHelper==null || mKnockCodeView==null)
                    return;
                CustomLogger.log(((Context) XposedHelpers.getObjectField(param.thisObject, "mContext")), "Lockscreen", "Device", "Screen turned on", null, -1);
                if (mKnockCodeView.isInCall())
                    return;
                if (shouldUnlock(param))
                    XposedHelpers.callMethod(param.thisObject, "showBouncer");
            }
        };

        mOnScreenTurnedOffHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (mSettingsHelper==null || mKnockCodeView==null)
                    return;
                CustomLogger.log(((Context) XposedHelpers.getObjectField(param.thisObject, "mContext")), "Lockscreen", "Device", "Screen turned off", null, -1);
            }
        };

        mShowNextSecurityScreenOrFinishHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if (mSettingsHelper==null || mKnockCodeView==null)
                    return;
                if (!mSettingsHelper.forceNone())
                    return;
                if (param.args[0].equals(false)) {
                    Object mSecurityModel = XposedHelpers.getObjectField(param.thisObject, "mSecurityModel");
                    Object securityMode = XposedHelpers.callMethod(mSecurityModel, "getSecurityMode");
                    //Class<?> SecurityMode = XposedHelpers.findClass(keyguardPackageName + ".KeyguardSecurityModel$SecurityMode",
                    //        param.thisObject.getClass().getClassLoader());
                    //Object SecurityNone = XposedHelpers.getStaticObjectField(SecurityMode, "None");
                    //if (!securityMode.equals(SecurityNone)) {
                        XposedHelpers.callMethod(param.thisObject, "showSecurityScreen", securityMode);
                        param.setResult(false);
                    //}
                }
            }
        };

        mShowPrimarySecurityScreenHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if (mSettingsHelper==null || mKnockCodeView==null)
                    return;
                if (!mSettingsHelper.forceNone())
                    return;
                param.setResult(null);
                Class<?> SecurityMode = XposedHelpers.findClass(keyguardPackageName + ".KeyguardSecurityModel$SecurityMode",
                        param.thisObject.getClass().getClassLoader());
                Object SecurityNone = XposedHelpers.getStaticObjectField(SecurityMode, "None");
                XposedHelpers.callMethod(param.thisObject, "showSecurityScreen", SecurityNone);
            }
        };
    }

    private boolean shouldUnlock(XC_MethodHook.MethodHookParam param) {
        UnlockPolicy currentPolicy = mSettingsHelper.getPolicy();
        if (currentPolicy.equals(UnlockPolicy.NEVER))
            return false;
        if (currentPolicy.equals(UnlockPolicy.ALWAYS))
            return true;
        else {
            Object mPhoneStatusBar = XposedHelpers.getObjectField(param.thisObject, "mPhoneStatusBar");
            ViewGroup stack = (ViewGroup) XposedHelpers.getObjectField(mPhoneStatusBar, "mStackScroller");
            int childCount = stack.getChildCount();
            int notifCount = 0;
            int notifClearableCount = 0;
            for (int i = 0; i < childCount; i++) {
                View v = stack.getChildAt(i);
                if (v.getVisibility() != View.VISIBLE ||
                        !v.getClass().getName().equals("com.android.systemui.statusbar.ExpandableNotificationRow")) {
                    continue;
                }
                notifCount++;
                if ((boolean) XposedHelpers.callMethod(v, "isClearable")) {
                    notifClearableCount++;
                }
            }
            return (currentPolicy == UnlockPolicy.NO_CLEARABLE_NOTIF ? notifClearableCount == 0 : notifCount == 0);
        }
    }
}
