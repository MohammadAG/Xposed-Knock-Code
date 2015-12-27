package me.rijul.knockcode;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import android.content.Context;
import android.os.Build;
import android.provider.Settings.Secure;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ViewFlipper;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage {

	private XC_MethodHook mUpdateSecurityViewHook;
	private XC_MethodHook mShowSecurityScreenHook;
	private XC_MethodHook mKeyguardHostViewInitHook;
	private XC_MethodHook mStartAppearAnimHook;
	private XC_MethodHook mStartDisAppearAnimHook;
    private XC_MethodHook mOnPauseHook;
	private XC_MethodHook mOnSimStateChangedHook;
	private XC_MethodHook mOnPhoneStateChangedHook;
	private XC_MethodHook mShowTimeoutDialogHook;
    protected static KnockCodeUnlockView mKnockCodeView;
	public static boolean isXperiaDevice=false;
	private static SettingsHelper mSettingsHelper;

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
			return;
		if (lpparam.packageName.equals("me.rijul.knockcode")) {
			XposedHelpers.setStaticBooleanField(XposedHelpers.findClass("me.rijul.knockcode.MainActivity", lpparam.classLoader),
					"MODULE_INACTIVE", false);
		}
		else if ((lpparam.packageName.contains("android.keyguard")) || (lpparam.packageName.contains("com.android.systemui"))) {
            createHooksIfNeeded("com.android.keyguard");
                    Class < ?> KeyguardHostView = XposedHelpers.findClass("com.android.keyguard.KeyguardSecurityContainer",
                    lpparam.classLoader);
            XposedBridge.hookAllConstructors(KeyguardHostView, mKeyguardHostViewInitHook);
			findAndHookMethod(KeyguardHostView, "startAppearAnimation", mStartAppearAnimHook);
			//findAndHookMethod(KeyguardHostView, "startDisappearAnimation", Runnable.class, mStartDisAppearAnimHook);
			findAndHookMethod(KeyguardHostView, "onPause", mOnPauseHook);
			Class<?> clazz = XposedHelpers.findClass("com.android.keyguard.KeyguardUpdateMonitorCallback",
					lpparam.classLoader);
			Class<?> state = XposedHelpers.findClass("com.android.internal.telephony.IccCardConstants.State",
					lpparam.classLoader);
			try {
				findAndHookMethod(clazz, "onSimStateChanged", int.class, int.class, state, mOnSimStateChangedHook);
				XposedBridge.log("[KnockCode] 5.1.x or 6.0.x device");
			}
			catch (NoSuchMethodError e)
			{
				try {
					findAndHookMethod(clazz, "onSimStateChanged", long.class, state, mOnSimStateChangedHook);
					XposedBridge.log("[KnockCode] 5.0.x device");
				}
				catch (NoSuchMethodError e2) {
					try {
						findAndHookMethod(clazz, "onSimStateChanged", int.class, state, mOnSimStateChangedHook);
						isXperiaDevice = true;
						XposedBridge.log("[KnockCode] Xperia device");
					}
					catch (NoSuchMethodError e3) {
						XposedBridge.log("[KnockCode] Unknown type of device, not hooking onSimStateChanged");
					}
				}
			}
			findAndHookMethod(clazz, "onPhoneStateChanged", int.class, mOnPhoneStateChangedHook);
            findAndHookMethod(KeyguardHostView, "showSecurityScreen", "com.android.keyguard.KeyguardSecurityModel$SecurityMode", mShowSecurityScreenHook);

			//marshmallow vs lollipop
			if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
				findAndHookMethod(KeyguardHostView, "showTimeoutDialog", int.class, mShowTimeoutDialogHook);
				findAndHookMethod(KeyguardHostView, "updateSecurityView", View.class, mUpdateSecurityViewHook);
			}
			else {
				findAndHookMethod(KeyguardHostView, "showTimeoutDialog", mShowTimeoutDialogHook);
				findAndHookMethod(KeyguardHostView, "updateSecurityView", View.class, boolean.class, mUpdateSecurityViewHook);
			}
        }
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
						int phoneState = (int) XposedHelpers.callMethod(mKnockCodeView.mKeyguardUpdateMonitor, "getPhoneState");
						mKnockCodeView.updateEmergencyCallButton(phoneState);
					}
					param.setResult(null);
				}
			}
		};

		mShowTimeoutDialogHook = new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if ((mSettingsHelper==null) || (mSettingsHelper.isDisabled()))
                    return;
				if ((mKnockCodeView != null) && (mSettingsHelper != null)) {
					if (mSettingsHelper.shouldDisableDialog())
						param.setResult(null);
				}
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
				if (view instanceof KnockCodeUnlockView) {
					KnockCodeUnlockView unlockView = (KnockCodeUnlockView) view;
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
					catch (Exception e) {
					}
					param.setResult(null);
				}
			}
		};

		mShowSecurityScreenHook = new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if ((mSettingsHelper==null) || (mSettingsHelper.isDisabled())) {
					mKnockCodeView = null;
					return;
				}
				Object securityMode = param.args[0];
				Class<?> SecurityMode = XposedHelpers.findClass(keyguardPackageName + ".KeyguardSecurityModel$SecurityMode",
						param.thisObject.getClass().getClassLoader());
				Object patternMode = XposedHelpers.getStaticObjectField(SecurityMode, "Pattern");
				if (!patternMode.equals(securityMode)) {
					mKnockCodeView = null;
					return;
				}
				Object mCurrentSecuritySelection = XposedHelpers.getObjectField(param.thisObject, "mCurrentSecuritySelection");
				if (securityMode == mCurrentSecuritySelection) {
					param.setResult(null);
					return;
				}

				Context mContext = ((FrameLayout) param.thisObject).getContext();
				View oldView = (View) callMethod(param.thisObject, "getSecurityView", mCurrentSecuritySelection);
				//LinearLayout eca = (LinearLayout) XposedHelpers.
				//	newInstance(XposedHelpers.findClass(keyguardPackageName + ".EmergencyCarrierArea", param.thisObject.getClass().getClassLoader()), mContext);
				//eca.addView((Button) XposedHelpers.
				//	newInstance(XposedHelpers.findClass(keyguardPackageName + ".EmergencyButton",param.thisObject.getClass().getClassLoader()), mContext));
				//eca.addView((TextView) XposedHelpers.
				//		newInstance(XposedHelpers.findClass(keyguardPackageName + ".CarrierText", param.thisObject.getClass().getClassLoader()), mContext));
				mKnockCodeView = new KnockCodeUnlockView(mContext,param,mSettingsHelper);
				View newView = mKnockCodeView;

                FrameLayout layout = (FrameLayout) param.thisObject;
				int disableSearch = XposedHelpers.getStaticIntField(View.class, "STATUS_BAR_DISABLE_SEARCH");
				layout.setSystemUiVisibility((layout.getSystemUiVisibility() & ~disableSearch));

				// pause old view, and ignore requests from it
				if (oldView != null) {
					Object mNullCallback = getObjectField(param.thisObject, "mNullCallback");
					callMethod(oldView, "onPause");
					callMethod(oldView, "setKeyguardCallback", mNullCallback);
				}

				//show new view, and set a callback for it
				Object mCallback = getObjectField(param.thisObject, "mCallback");
				callMethod(newView, "onResume", KeyguardSecurityView.VIEW_REVEALED);
				callMethod(newView, "setKeyguardCallback", mCallback);

				// add the view to the viewflipper and show it
				ViewFlipper mSecurityViewContainer = (ViewFlipper) getObjectField(param.thisObject, "mSecurityViewFlipper");
				mSecurityViewContainer.addView(newView);
				final int childCount = mSecurityViewContainer.getChildCount();

				for (int i = 0; i < childCount; i++) {
					if (mSecurityViewContainer.getChildAt(i) instanceof KnockCodeUnlockView) {
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
                if ((mSettingsHelper==null) || (mSettingsHelper.isDisabled()))
                    return;
				if (isPattern(keyguardPackageName,param))
					if (mKnockCodeView!=null) {
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
				if (isPattern(keyguardPackageName,param))
					if (mKnockCodeView!=null) {
						param.setResult(mKnockCodeView.startDisappearAnimation((Runnable) param.args[0]));
					}
			}
		};

        mOnPauseHook= new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if ((mSettingsHelper==null) || (mSettingsHelper.isDisabled()))
                    return;
				if (isPattern(keyguardPackageName,param))
                  if (mKnockCodeView!=null) {
                        mKnockCodeView.onPause();
                        param.setResult(null);
                    }
            }
        };
	}

	private boolean isPattern(String keyguardPackageName, XC_MethodHook.MethodHookParam param) {
		Class<?> SecurityMode = XposedHelpers.findClass(keyguardPackageName + ".KeyguardSecurityModel$SecurityMode",
				param.thisObject.getClass().getClassLoader());
		Object patternMode = XposedHelpers.getStaticObjectField(SecurityMode, "Pattern");
		return  (patternMode.equals(XposedHelpers.getObjectField(param.thisObject, "mCurrentSecuritySelection")));
	}
}
