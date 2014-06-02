package com.mohammadag.knockcode;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import android.content.Context;
import android.provider.Settings.Secure;
import android.view.View;
import android.view.ViewGroup;
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
	private XC_MethodReplacement mOnScreenTurnedOnHook;
	private XC_MethodReplacement mOnScreenTurnedOffHook;
	protected KnockCodeUnlockView mKnockCodeView;
	private static SettingsHelper mSettingsHelper;

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if ("com.htc.lockscreen".equals(lpparam.packageName)) {
			hookHtcLockscreen(lpparam);
		}

		if ("com.android.keyguard".equals(lpparam.packageName)) {
			hookAospLockscreen(lpparam);
		}

		if ("android".equals(lpparam.packageName)) {
			hookPrekitkatLockscreen(lpparam);
		}
	}

	private void createHooksIfNeeded(final String keyguardPackageName) {
		// We don't need this since we can't cast our widget to a KeyguardSecurityView, no matter
		// how hard we try. Although if possible, this is the cleaner way to do things: it gets rid
		// of all the hooks below.
		//
		//		mGetSecurityViewHook = new XC_MethodHook() {
		//			@Override
		//			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
		//				Object paramSecurityMode = param.args[0];
		//				Class<?> SecurityMode = XposedHelpers.findClass(securityModeClasssName,
		//						param.thisObject.getClass().getClassLoader());
		//				Object patternMode = XposedHelpers.getStaticObjectField(SecurityMode, "Pattern");
		//
		//				if (patternMode.equals(paramSecurityMode)) {
		//					Context mContext;
		//					try {
		//						mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mThemeContext");
		//					} catch (NoSuchFieldError e) {
		//						mContext = ((FrameLayout) param.thisObject).getContext();
		//					}
		//					KnockCodeUnlockView knockCodeView = new KnockCodeUnlockView(mContext);
		//					ViewFlipper mSecurityViewContainer = (ViewFlipper) XposedHelpers.getObjectField(param.thisObject, "mSecurityViewContainer");
		//					mSecurityViewContainer.addView(knockCodeView);
		//
		//					XposedHelpers.callMethod(param.thisObject, "updateSecurityView", knockCodeView);
		//					param.setResult(knockCodeView);
		//				}
		//			}
		//		};

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

		mUpdateSecurityViewHook = new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				View view = (View) param.args[0];
				if (view instanceof KnockCodeUnlockView) {
					KnockCodeUnlockView unlockView = (KnockCodeUnlockView) view;
					unlockView.setKeyguardCallback(XposedHelpers.getObjectField(param.thisObject, "mCallback"));
					unlockView.setLockPatternUtils(XposedHelpers.getObjectField(param.thisObject, "mLockPatternUtils"));
					param.setResult(null);
				}
			}
		};

		mShowSecurityScreenHook = new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				Object securityMode = param.args[0];
				Class<?> SecurityMode = XposedHelpers.findClass(keyguardPackageName + ".KeyguardSecurityModel$SecurityMode",
						param.thisObject.getClass().getClassLoader());
				Object patternMode = XposedHelpers.getStaticObjectField(SecurityMode, "Pattern");

				if (!patternMode.equals(securityMode))
					return;
				Object mCurrentSecuritySelection = XposedHelpers.getObjectField(param.thisObject, "mCurrentSecuritySelection");

				if (securityMode == mCurrentSecuritySelection) return;

				Context mContext;
				try {
					mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mThemeContext");
				} catch (NoSuchFieldError e) {
					mContext = ((FrameLayout) param.thisObject).getContext();
				}
				View oldView = (View) callMethod(param.thisObject, "getSecurityView", mCurrentSecuritySelection);

				mKnockCodeView = new KnockCodeUnlockView(mContext);
				mKnockCodeView.setSettingsHelper(mSettingsHelper);
				View newView = mKnockCodeView;

				ViewGroup mAppWidgetContainer = (ViewGroup) getObjectField(param.thisObject, "mAppWidgetContainer");
				mAppWidgetContainer.setVisibility(View.VISIBLE);
				FrameLayout layout = (FrameLayout) param.thisObject;
				// Don't show camera or search in navbar when SIM or Account screen is showing

				int disableSearch = XposedHelpers.getStaticIntField(View.class, "STATUS_BAR_DISABLE_SEARCH");
				layout.setSystemUiVisibility((layout.getSystemUiVisibility() & ~disableSearch));

				Object mSlidingChallengeLayout = getObjectField(param.thisObject, "mSlidingChallengeLayout");
				if (mSlidingChallengeLayout != null) {
					callMethod(mSlidingChallengeLayout, "setChallengeInteractive", true);
				}

				// Emulate Activity life cycle
				if (oldView != null) {
					Object mNullCallback = getObjectField(param.thisObject, "mNullCallback");
					callMethod(oldView, "onPause");
					callMethod(oldView, "setKeyguardCallback", mNullCallback); // ignore requests from old view
				}
				Object mCallback = getObjectField(param.thisObject, "mCallback");
				callMethod(newView, "onResume", KeyguardSecurityView.VIEW_REVEALED);
				callMethod(newView, "setKeyguardCallback", mCallback);

				final boolean needsInput = (Boolean) callMethod(newView, "needsInput");
				Object mViewMediatorCallback = getObjectField(param.thisObject, "mViewMediatorCallback");
				if (mViewMediatorCallback != null) {
					callMethod(mViewMediatorCallback, "setNeedsInput", needsInput);
				}

				// Find and show this child.
				ViewFlipper mSecurityViewContainer = (ViewFlipper) getObjectField(param.thisObject, "mSecurityViewContainer");
				mSecurityViewContainer.addView(newView);
				final int childCount = mSecurityViewContainer.getChildCount();

				for (int i = 0; i < childCount; i++) {
					if (mSecurityViewContainer.getChildAt(i) instanceof KnockCodeUnlockView) {
						mSecurityViewContainer.setDisplayedChild(i);
						mSecurityViewContainer.getChildAt(i).requestFocus();
						break;
					}
				}

				XposedHelpers.setObjectField(param.thisObject,
						"mCurrentSecuritySelection", securityMode);

				try {
					callMethod(param.thisObject, "notifyScreenChanged");
					callMethod(param.thisObject, "updateFooterPanelVisibility");
				} catch (Throwable t) {
					// Not an HTC
				}
				param.setResult(null);
			}
		};

		mOnScreenTurnedOnHook = new XC_MethodReplacement() {
			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				callMethod(param.thisObject, "showPrimarySecurityScreen", false);
				Object mCurrentSecuritySelection = getObjectField(param.thisObject, "mCurrentSecuritySelection");
				Object view = callMethod(param.thisObject, "getSecurityView", mCurrentSecuritySelection);
				if (view != null) {
					callMethod(view, "onResume", 1);
				}

				if (mKnockCodeView != null) {
					mKnockCodeView.onResume(1);
				}

				// This is a an attempt to fix bug 7137389 where the device comes back on but the entire
				// layout is blank but forcing a layout causes it to reappear (e.g. with with
				// hierarchyviewer).
				FrameLayout layout = (FrameLayout) param.thisObject;
				layout.requestLayout();

				try {
					Object mViewStateManager = getObjectField(param.thisObject, "mViewStateManager");
					if (mViewStateManager != null) {
						callMethod("mViewStateManager", "showUsabilityHints");
					}
				} catch (Throwable t) {

				}

				layout.requestFocus();
				return null;
			}
		};

		mOnScreenTurnedOffHook = new XC_MethodReplacement() {
			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				FrameLayout layout = (FrameLayout) param.thisObject;
				Context mContext = (Context) getObjectField(param.thisObject, "mContext");
				// Once the screen turns off, we no longer consider this to be first boot and we want the
				// biometric unlock to start next time keyguard is shown.
				Class<?> KeyguardUpdateMonitor = XposedHelpers.findClass(keyguardPackageName + ".KeyguardUpdateMonitor",
						param.thisObject.getClass().getClassLoader());
				Object KeyguardUpdateMonitorInstance = XposedHelpers.callStaticMethod(KeyguardUpdateMonitor, "getInstance", mContext);
				callMethod(KeyguardUpdateMonitorInstance, "setAlternateUnlockEnabled", true);
				// We use mAppWidgetToShow to show a particular widget after you add it-- once the screen
				// turns off we reset that behavior
				callMethod(param.thisObject, "clearAppWidgetToShow");
				if ((Boolean) callMethod(KeyguardUpdateMonitorInstance, "hasBootCompleted")) {
					callMethod(param.thisObject, "checkAppWidgetConsistency");
				}
				callMethod(param.thisObject, "showPrimarySecurityScreen", true);
				Object mCurrentSecuritySelection = getObjectField(param.thisObject, "mCurrentSecuritySelection");
				Object view = callMethod(param.thisObject, "getSecurityView", mCurrentSecuritySelection);
				if (view != null) {
					callMethod(view, "onPause");
				}

				if (mKnockCodeView != null) {
					mKnockCodeView.onPause();
				}
				try {
					Object cameraPage = callMethod(param.thisObject, "findCameraPage");
					if (cameraPage != null) {
						callMethod(cameraPage, "onScreenTurnedOff");
					}
				} catch (Throwable t) {

				}

				layout.clearFocus();
				return null;
			}
		};
	}

	private void hookHtcLockscreen(LoadPackageParam lpparam) {
		createHooksIfNeeded("com.htc.lockscreen.keyguard");
		Class<?> KeyguardHostView = XposedHelpers.findClass("com.htc.lockscreen.keyguard.KeyguardHostView",
				lpparam.classLoader);
		XposedBridge.hookAllConstructors(KeyguardHostView, mKeyguardHostViewInitHook);
		findAndHookMethod(KeyguardHostView, "showSecurityScreen", "com.htc.lockscreen.keyguard.KeyguardSecurityModel$SecurityMode", mShowSecurityScreenHook);
		findAndHookMethod(KeyguardHostView, "updateSecurityView", View.class, mUpdateSecurityViewHook);
		findAndHookMethod(KeyguardHostView, "onScreenTurnedOn", mOnScreenTurnedOnHook);
		findAndHookMethod(KeyguardHostView, "onScreenTurnedOff", mOnScreenTurnedOffHook);
	}

	private void hookAospLockscreen(LoadPackageParam lpparam) {
		createHooksIfNeeded("com.android.keyguard");
		Class<?> KeyguardHostView = XposedHelpers.findClass("com.android.keyguard.KeyguardHostView",
				lpparam.classLoader);
		XposedBridge.hookAllConstructors(KeyguardHostView, mKeyguardHostViewInitHook);
		findAndHookMethod(KeyguardHostView, "showSecurityScreen", "com.android.keyguard.KeyguardSecurityModel$SecurityMode", mShowSecurityScreenHook);
		findAndHookMethod(KeyguardHostView, "updateSecurityView", View.class, mUpdateSecurityViewHook);
		findAndHookMethod(KeyguardHostView, "onScreenTurnedOn", mOnScreenTurnedOnHook);
		findAndHookMethod(KeyguardHostView, "onScreenTurnedOff", mOnScreenTurnedOffHook);
	}

	private void hookPrekitkatLockscreen(LoadPackageParam lpparam) {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT)
			return;

		createHooksIfNeeded("com.android.internal.policy.impl.keyguard");
		Class<?> KeyguardHostView = XposedHelpers.findClass("com.android.internal.policy.impl.keyguard.KeyguardHostView",
				lpparam.classLoader);
		XposedBridge.hookAllConstructors(KeyguardHostView, mKeyguardHostViewInitHook);
		findAndHookMethod(KeyguardHostView, "showSecurityScreen", "com.android.internal.policy.impl.keyguard.KeyguardSecurityModel$SecurityMode", mShowSecurityScreenHook);
		findAndHookMethod(KeyguardHostView, "updateSecurityView", View.class, mUpdateSecurityViewHook);
		findAndHookMethod(KeyguardHostView, "onScreenTurnedOn", mOnScreenTurnedOnHook);
		findAndHookMethod(KeyguardHostView, "onScreenTurnedOff", mOnScreenTurnedOffHook);
	}
}
