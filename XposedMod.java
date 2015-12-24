package com.mohammadag.knockcode;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import android.content.Context;
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

	private XC_MethodHook mGetSecurityViewHook;
	protected KnockCodeUnlockView mKnockCodeView;

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if ("com.android.keyguard".equals(lpparam.packageName)) {
			hookAospLockscreen(lpparam);
		}
	}

	private void createHooksIfNeeded() {
				mGetSecurityViewHook = new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						Object paramSecurityMode = param.args[0];
						Class<?> SecurityMode = XposedHelpers.findClass(securityModeClasssName,
								param.thisObject.getClass().getClassLoader());
						Object patternMode = XposedHelpers.getStaticObjectField(SecurityMode, "Pattern");

						if (patternMode.equals(paramSecurityMode)) {
							Context mContext;
							try {
								mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mThemeContext");
							} catch (NoSuchFieldError e) {
								mContext = ((FrameLayout) param.thisObject).getContext();
							}
							KnockCodeUnlockView knockCodeView = new KnockCodeUnlockView(mContext);
							ViewFlipper mSecurityViewContainer = (ViewFlipper) XposedHelpers.getObjectField(param.thisObject, "mSecurityViewContainer");
							mSecurityViewContainer.addView(knockCodeView);

							XposedHelpers.callMethod(param.thisObject, "updateSecurityView", knockCodeView);
							param.setResult(knockCodeView);
						}
					}
				};

	private void hookAospLockscreen(LoadPackageParam lpparam) {
		createHooksIfNeeded();
		Class<?> KeyguardHostView = XposedHelpers.findClass("com.android.keyguard.KeyguardHostView",
				lpparam.classLoader);
		findAndHookMethod(KeyguardHostView, "getSecurityView", mGetSecurityViewHook);
	}
