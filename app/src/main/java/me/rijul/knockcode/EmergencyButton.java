/*
 * Copyright (c) 2013-2014, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 *
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.rijul.knockcode;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

//import com.android.internal.telephony.IccCardConstants.State;
//import com.android.internal.widget.LockPatternUtils;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * This class implements a smart emergency button that updates itself based
 * on telephony state.  When the phone is idle, it is an emergency call button.
 * When there's a call in progress, it presents an appropriate message and
 * allows the user to return to the call.
 */
public class EmergencyButton extends Button {

    private Object mLockPatternUtils;
    private PowerManager mPowerManager;
    private XC_MethodHook.MethodHookParam mParam;
    private Context mContext;
    private static final String ACTION_EMERGENCY_DIAL = "com.android.phone.EmergencyDialer.DIAL";

    KeyguardUpdateMonitorCallback mInfoCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onSimStateChanged(int subId, int slotId, Object simState) {
           // int phoneState = KeyguardUpdateMonitor.getInstance(mContext).getPhoneState();
            int phoneState = (int) XposedHelpers.callMethod(XposedHelpers.
                    callStaticMethod(XposedHelpers.
                                    findClass("com.android.keyguard.KeyguardUpdateMonitor", mParam.thisObject.getClass().getClassLoader()),
                            "getInstance", mContext), "getPhoneState");
            updateEmergencyCallButton(phoneState);
        }

        @Override
        public void onPhoneStateChanged(int phoneState) {
            updateEmergencyCallButton(phoneState);
        }
    };


   // public EmergencyButton(Context context, AttributeSet attrs) {
     //   super(context, attrs);
    //}

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        XposedHelpers.callMethod(XposedHelpers.
                callStaticMethod(XposedHelpers.
                                findClass("com.android.keyguard.KeyguardUpdateMonitor", mParam.thisObject.getClass().getClassLoader()),
                        "getInstance", mContext), "registerCallback", mInfoCallback);
        //KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mInfoCallback);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        XposedHelpers.callMethod(XposedHelpers.
                callStaticMethod(XposedHelpers.
                                findClass("com.android.keyguard.KeyguardUpdateMonitor", mParam.thisObject.getClass().getClassLoader()),
                        "getInstance", mContext), "removeCallback", mInfoCallback);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        //mLockPatternUtils = new LockPatternUtils(mContext);
        mLockPatternUtils = XposedHelpers.newInstance(XposedHelpers.findClass("com.android.internal.widget.LockPatternUtils",mParam.thisObject.getClass().getClassLoader()),mContext);
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                takeEmergencyCallAction();
            }
        });
        int phoneState = (int) XposedHelpers.callMethod(XposedHelpers.
                callStaticMethod(XposedHelpers.
                                findClass("com.android.keyguard.KeyguardUpdateMonitor", mParam.thisObject.getClass().getClassLoader()),
                        "getInstance", mContext), "getPhoneState");
        updateEmergencyCallButton(phoneState);
    }


    public EmergencyButton(Context ctx, XC_MethodHook.MethodHookParam param, AttributeSet attrs) {
        super(ctx,attrs);
        mContext = ctx;
        mParam = param;
    }

    public EmergencyButton(Context ctx, XC_MethodHook.MethodHookParam param) {
        this(ctx,param,null);
    }

    /**
     * Shows the emergency dialer or returns the user to the existing call.
     */
    public void takeEmergencyCallAction() {
        // TODO: implement a shorter timeout once new PowerManager API is ready.
        // should be the equivalent to the old userActivity(EMERGENCY_CALL_TIMEOUT)
        XposedHelpers.callMethod(mPowerManager, "userActivity", SystemClock.uptimeMillis(), true);
        //mPowerManager.userActivity(SystemClock.uptimeMillis(), true);
        if ((boolean) XposedHelpers.callMethod(mLockPatternUtils,"isInCall")) {
            XposedHelpers.callMethod(mLockPatternUtils,"resumeCall");
        } else {
            final boolean bypassHandler = true;
            XposedHelpers.callMethod(XposedHelpers.
                    callStaticMethod(XposedHelpers.
                                    findClass("com.android.keyguard.KeyguardUpdateMonitor", mParam.thisObject.getClass().getClassLoader()),
                            "getInstance", mContext), "reportEmergencyCallAction", bypassHandler);
                    //KeyguardUpdateMonitor.getInstance(mContext).reportEmergencyCallAction(bypassHandler);
            Intent intent = new Intent(ACTION_EMERGENCY_DIAL);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            XposedHelpers.callMethod(getContext(),"startActivityAsUser",intent,new UserHandle((android.os.Parcel)XposedHelpers.callMethod(mLockPatternUtils,"getCurrentUser")));
            //getContext().startActivityAsUser(intent,
              //      new UserHandle(mLockPatternUtils.getCurrentUser()));
        }
    }

    private void updateEmergencyCallButton(int phoneState) {
        boolean enabled = false;
        if ((boolean) XposedHelpers.callMethod(mLockPatternUtils,"isInCall")) {
            enabled = true; // always show "return to call" if phone is off-hook
        } else if ((boolean) XposedHelpers.callMethod(mLockPatternUtils,"isEmergencyCallCapable")) {
            //boolean simLocked = KeyguardUpdateMonitor.getInstance(mContext).isSimPinVoiceSecure();
            boolean simLocked = (boolean) XposedHelpers.callMethod(XposedHelpers.
                    callStaticMethod(XposedHelpers.
                                    findClass("com.android.keyguard.KeyguardUpdateMonitor", mParam.thisObject.getClass().getClassLoader()),
                            "getInstance", mContext), "isSimPinVoiceSecure");
            if (simLocked) {
                // Some countries can't handle emergency calls while SIM is locked.
                //enabled = mLockPatternUtils.isEmergencyCallEnabledWhileSimLocked();
                enabled = (boolean) XposedHelpers.callMethod(mLockPatternUtils, "isEmergencyCallEnabledWhileSimLocked");
            } else {
                // True if we need to show a secure screen (pin/pattern/SIM pin/SIM puk);
                // hides emergency button on "Slide" screen if device is not secure.
                int resId = mContext.getResources().getIdentifier("config_showEmergencyButton", "bool", mContext.getPackageName());
                enabled = (boolean) XposedHelpers.callMethod(mLockPatternUtils,"isSecure") || mContext.getResources().getBoolean(resId);
                //enabled = mLockPatternUtils.isSecure() ||
                  //      mContext.getResources().getBoolean(R.bool.config_showEmergencyButton);
            }
        }

        if (getContext().getResources().getBoolean(mContext.getResources().getIdentifier("icccardexist_hide_emergencybutton", "bool", mContext.getPackageName()))) {
            enabled = false;
        }
        //mLockPatternUtils.updateEmergencyCallButtonState(this, enabled, false);
        XposedHelpers.callMethod(mLockPatternUtils,"updateEmergencyCallButtonState",this,enabled,false);
    }

}
