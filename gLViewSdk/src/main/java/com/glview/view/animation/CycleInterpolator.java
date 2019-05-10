/*
 * Copyright (C) 2007 The Android Open Source Project
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
 * limitations under the License.
 */

package com.glview.view.animation;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.Resources.Theme;
import android.util.AttributeSet;

/**
 * Repeats the animation for a specified number of cycles. The
 * rate of change follows a sinusoidal pattern.
 *
 */
public class CycleInterpolator implements Interpolator {
    public CycleInterpolator(float cycles) {
        mCycles = cycles;
    }

    public CycleInterpolator(Context context, AttributeSet attrs) {
        this(context.getResources(), context.getTheme(), attrs);
    }

    /** @hide */
    public CycleInterpolator(Resources resources, Theme theme, AttributeSet attrs) {
        TypedArray a;
        if (theme != null) {
            a = theme.obtainStyledAttributes(attrs, com.glview.R.styleable.CycleInterpolator, 0, 0);
        } else {
            a = resources.obtainAttributes(attrs, com.glview.R.styleable.CycleInterpolator);
        }

        mCycles = a.getFloat(com.glview.R.styleable.CycleInterpolator_cycles, 1.0f);

        a.recycle();
    }

    public float getInterpolation(float input) {
        return (float)(Math.sin(2 * mCycles * Math.PI * input));
    }

    private float mCycles;

}
