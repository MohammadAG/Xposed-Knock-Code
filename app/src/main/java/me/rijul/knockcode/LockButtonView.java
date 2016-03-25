package me.rijul.knockcode;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;

public class LockButtonView extends LinearLayout implements View.OnClickListener, View.OnLongClickListener {
    public enum Mode {
        Ready, Correct, Incorrect, Disabled
    }

    public static int KNOCK_CODE_MAX_SIZE = 10, KNOCK_CODE_MIN_SIZE = 3;
    public static int GRID_MIN_SIZE = 2, GRID_MAX_SIZE = 5;

    private Grid mPatternSize = new Grid(2,2);
    private OnPositionTappedListener mListener;
    private OnLongClickListener mLongClickListener;

    private List<Button> mButtons = new ArrayList<Button>();
    private List<View> mHors = new ArrayList<View>();
    private List<View> mVers = new ArrayList<View>();

    private int readyColor;
    private int correctColor;
    private int errorColor;
    private int disabledColor = Color.DKGRAY;


    public interface OnPositionTappedListener {
        void onPositionTapped(Button button);
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
        setUpButtons();
    }

    private void setUpButtons() {
        mButtons.clear();
        mHors.clear();
        mVers.clear();
        this.removeAllViews();

        float mLineWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getContext().getResources().getDisplayMetrics());
        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);

        for(int currentRow=1; currentRow<=mPatternSize.numberOfRows; ++currentRow) {
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
            }

            for(int currentColumn=1; currentColumn<=mPatternSize.numberOfColumns; ++currentColumn) {
                Button button = new Button(getContext());
                LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
                button.setLayoutParams(buttonParams);
                button.setBackgroundResource(outValue.resourceId);
                button.setId(currentColumn + (currentRow - 1) * mPatternSize.numberOfColumns);
                button.setOnClickListener(this);
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

    public void showButtonTaps(boolean show) {
        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        for(Button btn : mButtons)
            btn.setBackgroundResource(show ? outValue.resourceId : 0);
        invalidate();
    }

    @Override
    public void onClick(View v) {
        if (mListener!=null)
            mListener.onPositionTapped((Button) v);
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
        if (settingsHelper.showBackground())
            setBackgroundColor(settingsHelper.getBackgroundColor());
        showLines(settingsHelper.showLines());
        showButtonTaps(settingsHelper.showButtonTaps());
        setPatternSize(settingsHelper.getPatternSize());
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
        //setMode(enable ? Mode.Ready : Mode.Disabled);
    }

    public Grid getPatternSize() {return mPatternSize;}
}