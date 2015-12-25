package me.rijul.knockcode;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;

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
					setAlpha(1.0f);
					setTranslationY(0);
				}
			})
			.start();
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
