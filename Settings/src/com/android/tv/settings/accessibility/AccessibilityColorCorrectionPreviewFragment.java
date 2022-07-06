/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.tv.settings.accessibility;

import static android.graphics.drawable.GradientDrawable.Orientation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;

import com.android.tv.twopanelsettings.R;
import com.android.tv.twopanelsettings.slices.InfoFragment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link InfoFragment} for the preview pane of accessibility color correction
 */
@Keep
public class AccessibilityColorCorrectionPreviewFragment extends InfoFragment {

    @Override
    public View onCreateView(
        LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =
            inflater.inflate(R.xml.accessibility_color_correction_preview, container, false);

        ViewGroup paletteView = view.findViewById(R.id.palette_view);

        int[] paletteColors = getPaletteColors();
        int[] paletteItemIds = getPaletteItemIds();

        for (int i = 0; i < paletteItemIds.length; i++) {
            TextView textView = (TextView) view.findViewById(paletteItemIds[i]);
            textView.setBackground(createGradientDrawable(paletteView, paletteColors[i]));
        }

        return view;
    }

    private int[] getPaletteColors() {
        return getResources().getIntArray(R.array.color_correction_palette_colors);
    }

    private int[] getPaletteItemIds() {
        TypedArray typedArray =
            getResources().obtainTypedArray(R.array.color_correction_palette_item_ids);
        int[] ids = new int[typedArray.length()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = typedArray.getResourceId(i, 0);
        }
        typedArray.recycle();

        return ids;
    }

    private GradientDrawable createGradientDrawable(ViewGroup rootView, @ColorInt int color) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        Orientation orientation =
            rootView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
                ? Orientation.RIGHT_LEFT
                : Orientation.LEFT_RIGHT;
        gradientDrawable.setOrientation(orientation);

        int defaultColor =
            getContext().getColor(R.color.color_correction_palette_gradient_background);
        int[] gradientColors = new int[] {defaultColor, color};
        float[] gradientOffsets = new float[] {0.2f, 0.5f};
        gradientDrawable.setColors(gradientColors, gradientOffsets);
        return gradientDrawable;
    }

    @Override
    public void updateInfoFragment() {
      // No-op as this is hosting a static info preview panel.
    }
}