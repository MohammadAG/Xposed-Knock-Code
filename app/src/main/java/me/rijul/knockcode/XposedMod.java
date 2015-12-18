package me.rijul.knockcode;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import android.content.Context;
import android.os.Build;
import android.provider.Settings.Secure;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage {

	private XC_MethodHook mUpdateSecurityViewHook;
	private XC_MethodHook mShowSecurityScreenHook;
	private XC_MethodHook mKeyguardHostViewInitHook;
	private XC_MethodHook mStartAppearAnimHook;
    private XC_MethodHook mOnPauseHook;
    protected static KnockCodeUnlockView mKnockCodeView;
	private static SettingsHelper mSettingsHelper;

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {

		if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) && (
				lpparam.packageName.contains("android.keyguard") || lpparam.packageName.contains("com.android.systemui"))) {
            createHooksIfNeeded("com.android.keyguard");
            Class<?> KeyguardHostView = XposedHelpers.findClass("com.android.keyguard.KeyguardSecurityContainer",
                    lpparam.classLoader);
            XposedBridge.hookAllConstructors(KeyguardHostView, mKeyguardHostViewInitHook);
            findAndHookMethod(KeyguardHostView, "showSecurityScreen", "com.android.keyguard.KeyguardSecurityModel$SecurityMode", mShowSecurityScreenHook);
            try {
                findAndHookMethod(KeyguardHostView, "updateSecurityView", View.class, boolean.class, mUpdateSecurityViewHook);
            }
            catch (NoSuchMethodError e) {
                findAndHookMethod(KeyguardHostView, "updateSecurityView", View.class, mUpdateSecurityViewHook);
            }
            findAndHookMethod(KeyguardHostView, "startAppearAnimation", mStartAppearAnimHook);
            findAndHookMethod(KeyguardHostView, "onPause", mOnPauseHook);
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

		mUpdateSecurityViewHook = new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
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
				Object securityMode = param.args[0];
				Class<?> SecurityMode = XposedHelpers.findClass(keyguardPackageName + ".KeyguardSecurityModel$SecurityMode",
						param.thisObject.getClass().getClassLoader());
				Object patternMode = XposedHelpers.getStaticObjectField(SecurityMode, "Pattern");
				if (!patternMode.equals(securityMode))
					return;
				Object mCurrentSecuritySelection = XposedHelpers.getObjectField(param.thisObject, "mCurrentSecuritySelection");
				if (securityMode == mCurrentSecuritySelection) return;

				Context mContext = ((FrameLayout) param.thisObject).getContext();
				View oldView = (View) callMethod(param.thisObject, "getSecurityView", mCurrentSecuritySelection);
				//LinearLayout eca = (LinearLayout) XposedHelpers.
				//	newInstance(XposedHelpers.findClass(keyguardPackageName + ".EmergencyCarrierArea", param.thisObject.getClass().getClassLoader()), mContext);
				//eca.addView((Button) XposedHelpers.
				//	newInstance(XposedHelpers.findClass(keyguardPackageName + ".EmergencyButton",param.thisObject.getClass().getClassLoader()), mContext));
				//eca.addView((TextView) XposedHelpers.
				//		newInstance(XposedHelpers.findClass(keyguardPackageName + ".CarrierText", param.thisObject.getClass().getClassLoader()), mContext));
				mKnockCodeView = new KnockCodeUnlockView(mContext,param);
				mKnockCodeView.setSettingsHelper(mSettingsHelper);
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
				if (isPattern(keyguardPackageName,param))
					if (mKnockCodeView!=null) {
						mKnockCodeView.startAppearAnimation();
                        param.setResult(null);
					}
			}
		};

        mOnPauseHook= new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
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
