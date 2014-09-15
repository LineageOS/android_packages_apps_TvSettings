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
 * limitations under the License.
 */

package com.android.tv.settings.notifications;

import com.android.tv.settings.R;
import com.android.tv.settings.widget.BitmapDownloader;
import com.android.tv.settings.widget.BitmapDownloader.BitmapCallback;
import com.android.tv.settings.widget.BitmapWorkerOptions;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

/**
 * View util functions for notifications.
 */
class ViewUtil {

    interface Canceller {
        void cancel();
    }

    /**
     * Hides the view. If the view is null, this function does nothing.
     *
     * @param v the view to hide.
     */
    static void hideView(View v) {
        if (v != null) {
            v.setVisibility(View.GONE);
        }
    }

    static Canceller setImageViewUri(Context context, final ImageView imageView, String imageUri) {

        if (TextUtils.isEmpty(imageUri) || imageView == null) {
            imageView.setVisibility(View.GONE);
            return new Canceller() {
                @Override
                public void cancel() {
                }
            };
        }

        imageView.setVisibility(View.INVISIBLE);
        final BitmapCallback bitmapCallBack = new BitmapCallback() {
            @Override
            public void onBitmapRetrieved(Bitmap bitmap) {
                imageView.setImageBitmap(bitmap);
                imageView.setVisibility(View.VISIBLE);
            }
        };
        final BitmapDownloader bitmapDownloader = BitmapDownloader.getInstance(context);
        BitmapWorkerOptions.Builder options = new BitmapWorkerOptions.Builder(context).resource(
                Uri.parse(imageUri));

        boolean limitSet = false;
        if (imageView.getLayoutParams().width > 0) {
            options.width(imageView.getLayoutParams().width);
            limitSet = true;
        }
        if (imageView.getLayoutParams().height > 0) {
            options.height(imageView.getLayoutParams().height);
            limitSet = true;
        }
        if (!limitSet) {
            int contentIconWidth = context.getResources()
                    .getDimensionPixelSize(R.dimen.content_fragment_icon_width);
            options.width(contentIconWidth).height(contentIconWidth);
        }

        bitmapDownloader.getBitmap(options.build(), bitmapCallBack);
        return new Canceller() {
            @Override
            public void cancel() {
                bitmapDownloader.cancelDownload(bitmapCallBack);
            }
        };
    }
}
