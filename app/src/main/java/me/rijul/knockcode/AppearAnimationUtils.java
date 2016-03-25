/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License
 */

package me.rijul.knockcode;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

/**
 * A class to make nice appear transitions for views in a tabular layout.
 */
public class AppearAnimationUtils implements AppearAnimationCreator<View> {

    public static final long DEFAULT_APPEAR_DURATION = 220;

    private final Interpolator mInterpolator;
    private float mStartTranslation;
    private final AppearAnimationProperties mProperties = new AppearAnimationProperties();
    protected final float mDelayScale;
    private final long mDuration;
    protected boolean mScaleTranslationWithRow;
    protected boolean mAppearing;

    public AppearAnimationUtils(Context ctx) {
        this(ctx, DEFAULT_APPEAR_DURATION,
                1.0f, 1.0f,
                AnimationUtils.loadInterpolator(ctx, android.R.interpolator.linear_out_slow_in));
    }

    public AppearAnimationUtils(Context ctx, long duration, float translationScaleFactor,
                                float delayScaleFactor, Interpolator interpolator) {
        mInterpolator = interpolator;
        Resources res = Utils.getResourcesForPackage(ctx, ctx.getPackageName());
        mStartTranslation = res.getDimensionPixelOffset(res.
                getIdentifier("appear_y_translation_start", "dimen", ctx.getPackageName())) * translationScaleFactor;
        //mStartTranslation = 32 * translationScaleFactor;
        mDelayScale = delayScaleFactor;
        mDuration = duration;
        mScaleTranslationWithRow = false;
        mAppearing = true;
    }

    public void startAnimation(View[][] objects, final Runnable finishListener) {
        startAnimation(objects, finishListener, this);
    }

    public void startAnimation(View[] objects, final Runnable finishListener) {
        startAnimation(objects, finishListener, this);
    }

    public <T> void startAnimation(T[][] objects, final Runnable finishListener,
                                   AppearAnimationCreator<T> creator) {
        AppearAnimationProperties properties = getDelays(objects);
        startAnimations(properties, objects, finishListener, creator);
    }

    public <T> void startAnimation(T[] objects, final Runnable finishListener,
                                   AppearAnimationCreator<T> creator) {
        AppearAnimationProperties properties = getDelays(objects);
        startAnimations(properties, objects, finishListener, creator);
    }

    private <T> void startAnimations(AppearAnimationProperties properties, T[] objects,
                                     final Runnable finishListener, AppearAnimationCreator<T> creator) {
        if (properties.maxDelayRowIndex == -1 || properties.maxDelayColIndex == -1) {
            finishListener.run();
            return;
        }
        for (int row = 0; row < properties.delays.length; row++) {
            long[] columns = properties.delays[row];
            long delay = columns[0];
            Runnable endRunnable = null;
            if (properties.maxDelayRowIndex == row && properties.maxDelayColIndex == 0) {
                endRunnable = finishListener;
            }
            creator.createAnimation(objects[row], delay, mDuration,
                    mStartTranslation, true /* appearing */, mInterpolator, endRunnable);
        }
    }

    private <T> void startAnimations(AppearAnimationProperties properties, T[][] objects,
                                     final Runnable finishListener, AppearAnimationCreator<T> creator) {
        if (properties.maxDelayRowIndex == -1 || properties.maxDelayColIndex == -1) {
            finishListener.run();
            return;
        }
        for (int row = 0; row < properties.delays.length; row++) {
            long[] columns = properties.delays[row];
            float translation = mScaleTranslationWithRow
                    ? (float) (Math.pow((properties.delays.length - row), 2)
                    / properties.delays.length * mStartTranslation)
                    : mStartTranslation;
            for (int col = 0; col < columns.length; col++) {
                long delay = columns[col];
                Runnable endRunnable = null;
                if (properties.maxDelayRowIndex == row && properties.maxDelayColIndex == col) {
                    endRunnable = finishListener;
                }
                creator.createAnimation(objects[row][col], delay, mDuration,
                        mAppearing ? translation : -translation,
                        mAppearing, mInterpolator, endRunnable);
            }
        }
    }

    private <T> AppearAnimationProperties getDelays(T[] items) {
        long maxDelay = -1;
        mProperties.maxDelayColIndex = -1;
        mProperties.maxDelayRowIndex = -1;
        mProperties.delays = new long[items.length][];
        for (int row = 0; row < items.length; row++) {
            mProperties.delays[row] = new long[1];
            long delay = calculateDelay(row, 0);
            mProperties.delays[row][0] = delay;
            if (items[row] != null && delay > maxDelay) {
                maxDelay = delay;
                mProperties.maxDelayColIndex = 0;
                mProperties.maxDelayRowIndex = row;
            }
        }
        return mProperties;
    }

    private <T> AppearAnimationProperties getDelays(T[][] items) {
        long maxDelay = -1;
        mProperties.maxDelayColIndex = -1;
        mProperties.maxDelayRowIndex = -1;
        mProperties.delays = new long[items.length][];
        for (int row = 0; row < items.length; row++) {
            T[] columns = items[row];
            mProperties.delays[row] = new long[columns.length];
            for (int col = 0; col < columns.length; col++) {
                long delay = calculateDelay(row, col);
                mProperties.delays[row][col] = delay;
                if (items[row][col] != null && delay > maxDelay) {
                    maxDelay = delay;
                    mProperties.maxDelayColIndex = col;
                    mProperties.maxDelayRowIndex = row;
                }
            }
        }
        return mProperties;
    }

    protected long calculateDelay(int row, int col) {
        return (long) ((row * 40 + col * (Math.pow(row, 0.4) + 0.4) * 20) * mDelayScale);
    }

    public Interpolator getInterpolator() {
        return mInterpolator;
    }

    public float getStartTranslation() {
        return mStartTranslation;
    }

    @Override
    public void createAnimation(View view, long delay, long duration, float translationY,
                                boolean appearing, Interpolator interpolator, Runnable endRunnable) {
        if (view != null) {
            view.setAlpha(appearing ? 0f : 1.0f);
            view.setTranslationY(appearing ? translationY : 0);
            view.animate()
                    .alpha(appearing ? 1f : 0f)
                    .translationY(appearing ? 0 : translationY)
                    .setInterpolator(interpolator)
                    .setDuration(duration)
                    .setStartDelay(delay);
            if (view.hasOverlappingRendering()) {
                view.animate().withLayer();
            }
            if (endRunnable != null) {
                view.animate().withEndAction(endRunnable);
            }
        }
    }

    public class AppearAnimationProperties {
        public long[][] delays;
        public int maxDelayRowIndex;
        public int maxDelayColIndex;
    }
}
