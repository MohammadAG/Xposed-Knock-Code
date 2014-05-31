package com.mohammadag.knockcode;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import com.mohammadag.knockcode.SettingsHelper.OnSettingsReloadedListener;

public class KnockCodeView extends View implements OnSettingsReloadedListener {
	public enum Mode {
		READY, CORRECT, INCORRECT, DISABLED
	}

	private Paint mLinePaint;
	private Paint mInnerPaint;

	private int mPosition = -1;

	private OnPositionTappedListener mListener;

	private float mLineWidth;
	private Mode mMode = Mode.READY;
	private SettingsHelper mSettingsHelper;
	protected boolean mLongClick;
	private OnLongClickListener mLongClickListener;

	public interface OnPositionTappedListener {
		void onPositionTapped(int pos);
	}

	public KnockCodeView(Context context) {
		super(context);
		init(context);
	}

	private void init(Context context) {
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		mLineWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, dm);
		super.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if (mLongClickListener != null) {
					v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
					mLongClick = true;
					return mLongClickListener.onLongClick(KnockCodeView.this);
				}
				return false;
			}
		});
	}

	public KnockCodeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public KnockCodeView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	public void setOnPositionTappedListener(OnPositionTappedListener listener) {
		mListener = listener;
	}

	public void setSettingsHelper(SettingsHelper settingsHelper) {
		mSettingsHelper = settingsHelper;
		mSettingsHelper.addOnReloadListener(this);
	}

	private boolean shouldDrawLines() {
		if (mSettingsHelper != null) {
			return mSettingsHelper.shouldDrawLines();
		} else {
			return true;
		}
	}

	private boolean shouldDrawFill() {
		if (mSettingsHelper != null) {
			return mSettingsHelper.shouldDrawFill();
		} else {
			return true;
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mLinePaint == null) {
			mLinePaint = new Paint();
			mLinePaint.setColor(Color.GRAY);
			mLinePaint.setStrokeWidth(mLineWidth);
			mLinePaint.setAntiAlias(true);

			mInnerPaint = new Paint();
			mInnerPaint.setColor(Color.WHITE);
			mInnerPaint.setAntiAlias(true);
			mInnerPaint.setStyle(Paint.Style.FILL);
		}

		switch (mMode) {
		case READY:
			mLinePaint.setColor(Color.GRAY);
			break;
		case CORRECT:
			mLinePaint.setColor(Color.GREEN);
			break;
		case INCORRECT:
			mLinePaint.setColor(Color.RED);
			break;
		case DISABLED:
			mLinePaint.setColor(Color.BLACK);
			break;
		}

		if (!isEnabled()) {
			mLinePaint.setColor(Color.BLACK);
		}

		if (shouldDrawLines()) {
			canvas.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight(), mLinePaint);
			canvas.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2, mLinePaint);
		}

		if (mPosition != -1 && shouldDrawFill()) {
			canvas.drawRect(getRectForPosition(mPosition), mInnerPaint);
		}
	}

	@Override
	public void setOnClickListener(OnClickListener l) {
		throw new RuntimeException("Unsupported");
	}

	@Override
	public void setOnLongClickListener(OnLongClickListener l) {
		mLongClickListener = l;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled())
			return true;

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mPosition = getPositionOfClick(event.getX(), event.getY());
			invalidate();
			break;
		case MotionEvent.ACTION_UP:
			if (mLongClick) {
				mPosition = -1;
				invalidate();
				mLongClick = false;
				return super.onTouchEvent(event);
			} else {
				int position = getPositionOfClick(event.getX(), event.getY());
				if (mPosition != position) {
					mPosition = -1;
					break;
				}

				if (mListener != null)
					mListener.onPositionTapped(position);
			}

			mPosition = -1;
			invalidate();
			return super.onTouchEvent(event);
		}
		invalidate();
		return super.onTouchEvent(event);
	}

	private int getPositionOfClick(float x, float y) {
		if (y < getHeight() / 2) {
			if (x > getWidth() / 2) {
				return 2;
			} else {
				return 1;
			}
		} else if (y > getHeight() / 2) {
			if (x > getWidth() / 2) {
				return 4;
			} else {
				return 3;
			}
		}
		return -1;
	}

	private Rect getRectForPosition(int pos) {
		return normalizeRect(getRectForPositionImpl(pos));
	}

	private Rect getRectForPositionImpl(int pos) {
		switch (pos) {
		case 1:
			return new Rect(0, 0, getWidth() / 2, getHeight() / 2);
		case 2:
			return new Rect(getWidth() / 2, 0, getWidth(), getHeight() / 2);
		case 3:
			return new Rect(0, getHeight() / 2, getWidth() / 2, getHeight());
		case 4:
			return new Rect(getWidth() / 2, getHeight() / 2, getWidth(), getHeight());
		default:
			throw new IllegalArgumentException("Only position 1-4 supported");
		}
	}

	private Rect normalizeRect(Rect rect) {
		Rect newRect = rect;
		if (rect.top > 0) {
			newRect.top = rect.top + (int) mLineWidth;
		}
		if (rect.left > 0) {
			newRect.left = rect.left + (int) mLineWidth;
		}
		if (rect.right > 0) {
			newRect.right = rect.right - (int) mLineWidth;
		}
		if (rect.bottom > 0) {
			newRect.bottom = rect.bottom - (int) mLineWidth;
		}

		return newRect;
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (!enabled) {
			mMode = Mode.DISABLED;
		}
		super.setEnabled(enabled);
		invalidate();
	}

	public void setMode(Mode mode) {
		if (mMode == mode)
			return;

		mMode = mode;
		if (mode == Mode.DISABLED && isEnabled()) {
			setEnabled(false);
		}

		invalidate();
	}

	@Override
	public void onSettingsReloaded() {
		invalidate();
	}
}
