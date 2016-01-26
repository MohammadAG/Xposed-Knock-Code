package me.rijul.knockcode;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedBridge;

public class KnockCodeButtonView extends LinearLayout implements View.OnClickListener, View.OnLongClickListener {
    public enum Mode {
        READY, CORRECT, INCORRECT, DISABLED
    }
    private Context mContext;
    public Grid mPatternSize = new Grid(2,2);
    private OnPositionTappedListener mListener;
    private List<Button> mButtons = new ArrayList<Button>();
    private List<View> mHors = new ArrayList<View>();
    private List<View> mVers = new ArrayList<View>();
    private SettingsHelper mSettingsHelper;
    private boolean mLongClick;
    private OnLongClickListener mLongClickListener;
    private Mode mMode = Mode.READY;
    private boolean mEnabled = true;
    private float mLineWidth;


    public interface OnPositionTappedListener {
        void onPositionTapped(int pos);
    }


    public KnockCodeButtonView(Context context) {
        super(context);
        init(context);
    }

    public KnockCodeButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public KnockCodeButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mLineWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, mContext.getResources().getDisplayMetrics());
        setUpButtons();
    }

    private void setUpButtons() {
        mButtons.clear();
        mHors.clear();
        mVers.clear();
        this.removeAllViews();

        TypedValue outValue = new TypedValue();
        mContext.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);

        for(int currentRow=1; currentRow<=mPatternSize.numberOfRows; ++currentRow) {
            LinearLayout horizontalLayout = new LinearLayout(mContext);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
            horizontalLayout.setLayoutParams(layoutParams);
            horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);

            if (currentRow!=1) {
                View horizontalLine = new View(mContext);
                LinearLayout.LayoutParams horizontalLineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) mLineWidth);
                horizontalLine.setLayoutParams(horizontalLineParams);
                horizontalLine.setBackgroundColor(0xFF212121);
                mHors.add(horizontalLine);
                this.addView(horizontalLine);
            }

            for(int currentColumn=1; currentColumn<=mPatternSize.numberOfColumns; ++currentColumn) {
                Button button = new Button(mContext);
                LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
                button.setLayoutParams(buttonParams);
                button.setBackgroundResource(outValue.resourceId);
                button.setId(currentColumn + (currentRow - 1) * mPatternSize.numberOfColumns);
                button.setOnClickListener(this);
                button.setOnLongClickListener(this);
                mButtons.add(button);
                horizontalLayout.addView(button);

                if (currentColumn!=mPatternSize.numberOfColumns) {
                    View verticalLine = new View(mContext);
                    LinearLayout.LayoutParams verticalLineParams = new LinearLayout.LayoutParams((int) mLineWidth, ViewGroup.LayoutParams.MATCH_PARENT);
                    verticalLine.setLayoutParams(verticalLineParams);
                    verticalLine.setBackgroundColor(0xFF212121);
                    mVers.add(verticalLine);
                    horizontalLayout.addView(verticalLine);
                }
            }

            this.addView(horizontalLayout);
        }
        invalidate();
    }

    public void reColourLines(int color) {
        for(View v : mHors)
            v.setBackgroundColor(color);
        for(View v : mVers)
            v.setBackgroundColor(color);
        invalidate();
    }

    public void hideLines() {
        reColourLines(0);
    }

    public void hideButtonTaps() {
        for(Button btn : mButtons)
            btn.setBackgroundResource(0);
        invalidate();
    }

    @Override
    public void onClick(View v) {
        if (vibrateOnTap())
            v.performHapticFeedback((Build.VERSION.SDK_INT == Build.VERSION_CODES.M) ?
                            HapticFeedbackConstants.CONTEXT_CLICK : HapticFeedbackConstants.VIRTUAL_KEY ,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        if (mLongClick) {
            mLongClick = false;
            return;
        }
        if (mListener!=null)
            mListener.onPositionTapped(v.getId());
    }

    @Override
    public boolean onLongClick(View v) {
        if (vibrateOnLongPress())
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        if (mLongClickListener != null)
            return mLongClickListener.onLongClick(KnockCodeButtonView.this);
        else
            return false;
    }

    public void setOnPositionTappedListener(OnPositionTappedListener listener) {
        mListener = listener;
    }

    public void setOnLongClickListener(OnLongClickListener listener) {
        mLongClickListener = listener;
    }

    public void setSettingsHelper(SettingsHelper settingsHelper) {
        mSettingsHelper = settingsHelper;
    }

    private boolean vibrateOnLongPress() {
        if (mSettingsHelper!=null)
            return mSettingsHelper.vibrateOnLongPress();
        else
            return true;
    }

    private boolean vibrateOnTap() {
        if (mSettingsHelper!=null)
            return mSettingsHelper.vibrateOnTap();
        else
            return false;
    }

    private boolean shouldDrawLines() {
        if (mSettingsHelper != null) {
            return mSettingsHelper.shouldDrawLines();
        } else {
            return true;
        }
    }

    public void setPatternSize(Grid g) {
        mPatternSize = g;
        setUpButtons();
    }

    public void setMode(Mode mode) {
        mMode = mode;
        if (mMode==Mode.DISABLED)
            enableButtons(false);
        else
            enableButtons(true);
        if (shouldDrawLines())
            switch(mMode) {
                case READY:
                    if (mSettingsHelper!=null)
                        reColourLines(0xfffafafa);
                    else
                        reColourLines(0xff212121);
                    break;
                case CORRECT:
                    reColourLines(0xFF4CAF50);
                    break;
                case INCORRECT:
                    reColourLines(0xFFF44336);
                    break;
                case DISABLED:
                    reColourLines(0xFF9E9E9E);
                    break;
            }
    }

    public void enableButtons(boolean enable) {
        if (enable==mEnabled)
            return;
        mEnabled = enable;
        for(Button btn : mButtons)
            btn.setEnabled(mEnabled);
        invalidate();
    }
}