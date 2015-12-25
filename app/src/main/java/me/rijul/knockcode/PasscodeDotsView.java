package me.rijul.knockcode;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;

public class PasscodeDotsView extends View {
	private Paint mDotPaint;
	public int mDotRadius = 50;
	public int mDotCount = 8;

	public PasscodeDotsView(Context context) {
		super(context);
	}

	public PasscodeDotsView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PasscodeDotsView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mDotPaint == null) {
			mDotPaint = new Paint();
			mDotPaint.setColor(Color.BLACK);
			mDotPaint.setAntiAlias(true);
			mDotPaint.setStyle(Paint.Style.FILL);
		}

		int y = getHeight() / 2;
		int x = getWidth() / 2;
		for (int i = 0; i < mDotCount; i++) {
			if (i == 0) {
				x = (x - (getDotWidthWithSpacing()*(mDotCount-1))/2);
			} else {
				x = x + getDotWidthWithSpacing();
			}

			canvas.drawCircle(x, y, mDotRadius, mDotPaint);
		}
	}

	public void setDotPaintColor(int color) {
		if (mDotPaint == null) {
			mDotPaint = new Paint();
			mDotPaint.setColor(color);
			mDotPaint.setAntiAlias(true);
			mDotPaint.setStyle(Paint.Style.FILL);
		}
		else {
			mDotPaint.setColor(color);
			invalidate();
		}
	}

	public void reset() {
		animate()
			.translationY(-getHeight())
			.alpha(0.0f).setDuration(200)
			.setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					super.onAnimationEnd(animation);
					setCount(0);
					setDotPaintColor(Color.WHITE);
					setAlpha(1.0f);
					setTranslationY(0);
				}
			})
			.start();
	}

	public void reset(final boolean b) {
		final float[] from = new float[3],
				to =   new float[3];

		Color.colorToHSV(Color.parseColor("#FFFFFFFF"), from);   // from white
		if (b)
			Color.colorToHSV(Color.parseColor("#FF4CAF50"), to);     // to green
		else
			Color.colorToHSV(Color.parseColor("#FFF44336"), to);     // to red


		ValueAnimator anim = ValueAnimator.ofFloat(0, 1);   // animate from 0 to 1
		anim.setDuration(100);                              // for 300 ms

		final float[] hsv  = new float[3];                  // transition color
		anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				// Transition along each axis of HSV (hue, saturation, value)
				hsv[0] = from[0] + (to[0] - from[0])*animation.getAnimatedFraction();
				hsv[1] = from[1] + (to[1] - from[1])*animation.getAnimatedFraction();
				hsv[2] = from[2] + (to[2] - from[2])*animation.getAnimatedFraction();

				setDotPaintColor(Color.HSVToColor(hsv));

				if (animation.getAnimatedFraction()==1) {
					//if (b)
						reset();
					//else {
					//	setCount(0);
					//	setDotPaintColor(Color.WHITE);
					//}
				}
			}
		});

		anim.start();
	}

	public int getDotWidthWithSpacing() {
		return mDotRadius * 3;
	}

	public void setCount(int count) {
		mDotCount = count;
		invalidate();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		mDotRadius = (h / 3);
	}
}
