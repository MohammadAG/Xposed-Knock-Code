package com.mohammadag.knockcode;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class LineView extends View {
	private Paint mLinePaint;

	public LineView(Context context) {
		super(context);
	}

	public LineView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public LineView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mLinePaint == null) {
			mLinePaint = new Paint();
			mLinePaint.setColor(Color.GRAY);
		}

		canvas.drawLine(getPaddingLeft(), getHeight() / 2, getWidth()-getPaddingRight(), getHeight() / 2, mLinePaint);
	}
}
