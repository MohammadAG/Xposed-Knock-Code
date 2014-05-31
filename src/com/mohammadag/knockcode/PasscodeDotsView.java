package com.mohammadag.knockcode;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class PasscodeDotsView extends View {
	private Paint mDotPaint;
	private int mDotRadius = 50;
	private int mDotCount = 8;

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
			mDotPaint.setColor(Color.WHITE);
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
