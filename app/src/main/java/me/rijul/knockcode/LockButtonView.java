package me.rijul.knockcode;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LockButtonView extends LinearLayout implements View.OnLongClickListener, View.OnTouchListener {
    private ArrayList<Integer> mEnteredCode = new ArrayList<>();

    final Handler mHandler = new Handler();
    Runnable mLongPressed = new Runnable() {
        @Override
        public void run() {
            mLongPress = true;
            if (mLongClickListener!=null)
                mLongClickListener.onLongClick(LockButtonView.this);
        }
    };
    private boolean mLongPress = false;

    public interface OnLongPressCompletedListener {
        void onLongPressCompleted();
    }
    private OnLongPressCompletedListener onLongPressCompletedListener;
    public void setOnLongPressCompletedListener(OnLongPressCompletedListener listener) {
        onLongPressCompletedListener = listener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN)
            mHandler.postDelayed(mLongPressed, ViewConfiguration.getLongPressTimeout());
        else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            if (mLongPress) {
                if (onLongPressCompletedListener != null)
                    onLongPressCompletedListener.onLongPressCompleted();
                mLongPress = false;
                return false;
            }
            mHandler.removeCallbacks(mLongPressed);
            mEnteredCode.add(v.getId());
            if (mListener != null)
                mListener.onPositionTapped((Button) v, mEnteredCode);
        }
        return false;
    }

    public enum Mode {
        Ready, Correct, Incorrect, Disabled
    }

    public static int KNOCK_CODE_MAX_SIZE = 12+1, KNOCK_CODE_MIN_SIZE = 3;
    public static int GRID_MIN_SIZE = 2, GRID_MAX_SIZE = 5;

    private Grid mPatternSize = new Grid(2,2);
    private OnPositionTappedListener mListener;
    private OnLongClickListener mLongClickListener;

    private List<Button> mButtons = new ArrayList<Button>();
    private List<View> mHors = new ArrayList<View>();
    private List<View> mVers = new ArrayList<View>();

    public ArrayList<ArrayList<View>> mOutputArray = new ArrayList<>();

    private int readyColor;
    private int correctColor;
    private int errorColor;
    private int disabledColor = Color.DKGRAY;


    public interface OnPositionTappedListener {
        void onPositionTapped(Button button, ArrayList<Integer> position);
    }


    public LockButtonView(Context context) {
        super(context);
        init();
    }

    public LockButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LockButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        readyColor = Utils.getOwnResources(getContext()).getColor(R.color.textColorPrimary);
        correctColor = Utils.getOwnResources(getContext()).getColor(R.color.colorCorrect);
        errorColor = Utils.getOwnResources(getContext()).getColor(R.color.colorWrong);
    }

    private void setUpButtons() {
        mButtons.clear();
        mHors.clear();
        mVers.clear();
        mOutputArray.clear();
        mEnteredCode.clear();
        this.removeAllViews();

        float mLineWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getContext().getResources().getDisplayMetrics());
        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);

        for(int currentRow=1; currentRow<=mPatternSize.numberOfRows; ++currentRow) {
            ArrayList<View> row = new ArrayList<>();

            LinearLayout horizontalLayout = new LinearLayout(getContext());
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
            horizontalLayout.setLayoutParams(layoutParams);
            horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);

            if (currentRow!=1) {
                View horizontalLine = new View(getContext());
                LinearLayout.LayoutParams horizontalLineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) mLineWidth);
                horizontalLine.setLayoutParams(horizontalLineParams);
                horizontalLine.setBackgroundColor(readyColor);
                mHors.add(horizontalLine);
                this.addView(horizontalLine);
                row.add(horizontalLine);
            }

            for(int currentColumn=1; currentColumn<=mPatternSize.numberOfColumns; ++currentColumn) {
                Button button = new Button(getContext());
                LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
                button.setLayoutParams(buttonParams);
                button.setBackgroundResource(outValue.resourceId);
                button.setId(currentColumn + (currentRow - 1) * mPatternSize.numberOfColumns);
                button.setOnTouchListener(this);
                button.setOnLongClickListener(this);
                mButtons.add(button);
                horizontalLayout.addView(button);

                if (currentColumn!=mPatternSize.numberOfColumns) {
                    View verticalLine = new View(getContext());
                    LinearLayout.LayoutParams verticalLineParams = new LinearLayout.LayoutParams((int) mLineWidth, ViewGroup.LayoutParams.MATCH_PARENT);
                    verticalLine.setLayoutParams(verticalLineParams);
                    verticalLine.setBackgroundColor(readyColor);
                    mVers.add(verticalLine);
                    horizontalLayout.addView(verticalLine);
                }
            }

            this.addView(horizontalLayout);
            row.add(horizontalLayout);
            mOutputArray.add(row);
        }
        invalidate();
    }

    private void recolorLines(int color) {
        for(View v : mHors)
            v.setBackgroundColor(color);
        for(View v : mVers)
            v.setBackgroundColor(color);
        invalidate();
    }

    public void showLines(boolean show) {
        for(View v : mHors)
            v.setVisibility(show ? VISIBLE : GONE);
        for(View v : mVers)
            v.setVisibility(show ? VISIBLE : GONE);
        invalidate();
    }

    public void showButtonTaps(boolean show, boolean borderless) {
        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(borderless ? android.R.attr.selectableItemBackgroundBorderless :
                android.R.attr.selectableItemBackground, outValue, true);
        for(Button btn : mButtons)
            btn.setBackgroundResource(show ? outValue.resourceId : 0);
        invalidate();
    }

    /*
    @Override
    public void onClick(View v) {
        mEnteredCode.add(v.getId());
        if (mListener!=null)
            mListener.onPositionTapped((Button) v, mEnteredCode);
    }*/

    public void clearCode() {
        mEnteredCode.clear();
    }

    @Override
    public boolean onLongClick(View v) {
        return mLongClickListener != null && mLongClickListener.onLongClick(this);
    }

    public void setOnPositionTappedListener(OnPositionTappedListener listener) {
        mListener = listener;
    }

    public void setOnLongClickListener(OnLongClickListener listener) {
        mLongClickListener = listener;
    }

    public void updateViewState(SettingsHelper settingsHelper) {
        readyColor = settingsHelper.getLinesReadyColor();
        errorColor = settingsHelper.getLinesErrorColor();
        correctColor = settingsHelper.getLinesCorrectColor();
        disabledColor = settingsHelper.getLinesDisabledColor();
        setPatternSize(settingsHelper.getPatternSize());
        if (settingsHelper.showBackground())
            setBackgroundColor(settingsHelper.getBackgroundColor());
        showLines(settingsHelper.showLines());
        showButtonTaps(settingsHelper.showButtonTaps(), settingsHelper.borderlessTaps());
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setPatternSize(Grid g) {
        mPatternSize = g;
        setUpButtons();
    }

    public void setMode(Mode mode) {
        switch (mode) {
            case Ready:
                recolorLines(readyColor);
                break;
            case Correct:
                recolorLines(correctColor);
                break;
            case Incorrect:
                recolorLines(errorColor);
                break;
            case Disabled:
                recolorLines(disabledColor);
                break;
        }
        invalidate();
    }

    public void enableButtons(boolean enable) {
        for(Button btn : mButtons)
            btn.setEnabled(enable);
    }

    public Grid getPatternSize() {return mPatternSize;}
}
