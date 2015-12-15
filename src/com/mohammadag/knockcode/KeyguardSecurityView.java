package com.mohammadag.knockcode;

public abstract interface KeyguardSecurityView {
	public static final int SCREEN_ON = 1;
	public static final int VIEW_REVEALED = 2;

	public abstract Object getCallback();

	public abstract boolean needsInput();

	public abstract void onPause();

	public abstract void onResume(int paramInt);

	public abstract void reset();

	public abstract void setKeyguardCallback(Object paramKeyguardSecurityCallback);

	public abstract void setLockPatternUtils(Object paramLockPatternUtils);

	public abstract void showUsabilityHint();

	public abstract void startAppearAnimation();

	public abstract boolean startDisappearAnimation(Runnable finishRunnable);

	public abstract void showBouncer(int duration);

	public abstract void hideBouncer(int duration);
}
