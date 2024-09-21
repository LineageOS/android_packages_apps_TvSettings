/*
 * Copyright 2017 The Android Open Source Project
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

package com.android.tv.twopanelsettings.slices.compat;

import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_REMOTE_INPUT;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.IconCompat;
import androidx.versionedparcelable.ParcelUtils;

import com.android.tv.twopanelsettings.slices.compat.core.SliceActionImpl;
import com.android.tv.twopanelsettings.slices.compat.core.SliceHints;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.nio.charset.Charset;

/**
 * Utilities for dealing with slices.
 *
 * Slice framework has been deprecated, it will not receive any updates moving
 * forward. If you are looking for a framework that handles communication across apps,
 * consider using {@link android.app.appsearch.AppSearchManager}.
 */
// @Deprecated // Supported for TV
public class SliceUtils {

    private SliceUtils() {
    }

    /**
     * Parse a slice that has been previously serialized.
     * <p>
     * Parses a slice that was serialized with {@link #serializeSlice}.
     * <p>
     * Note: Slices returned by this cannot be passed to {@link SliceConvert#unwrap(Slice)}.
     *
     * @param input    The input stream to read from.
     * @param encoding The encoding to read as.
     * @param listener Listener used to handle actions when reconstructing the slice.
     * @throws SliceParseException if the InputStream cannot be parsed.
     */
    public static @NonNull Slice parseSlice(@NonNull final Context context,
            @NonNull InputStream input, @NonNull String encoding,
            @NonNull final SliceActionListener listener) throws IOException, SliceParseException {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(input);
        String parcelName = Slice.class.getName();

        bufferedInputStream.mark(parcelName.length() + 4);
        boolean usesParcel = doesStreamStartWith(parcelName, bufferedInputStream);
        bufferedInputStream.reset();
        if (usesParcel) {
            Slice slice;
            final SliceItem.ActionHandler handler = new SliceItem.ActionHandler() {
                @Override
                public void onAction(@NonNull SliceItem item, @Nullable Context context,
                        Intent intent) {
                    listener.onSliceAction(item.getSlice().getUri(), context, intent);
                }
            };
            synchronized (SliceItemHolder.sSerializeLock) {
                SliceItemHolder.sHandler = new SliceItemHolder.HolderHandler() {
                    @Override
                    public void handle(@NonNull SliceItemHolder holder, @NonNull String format) {
                        setActionsAndUpdateIcons(holder, handler, context, format);
                    }
                };
                slice = ParcelUtils.fromInputStream(bufferedInputStream);
                slice.mHints = ArrayUtils.appendElement(String.class, slice.mHints,
                        SliceHints.HINT_CACHED);
                SliceItemHolder.sHandler = null;
            }
            return slice;
        }
        Slice s = SliceXml.parseSlice(context, bufferedInputStream, encoding, listener);
        s.mHints = ArrayUtils.appendElement(String.class, s.mHints, SliceHints.HINT_CACHED);
        return s;
    }

    static void setActionsAndUpdateIcons(SliceItemHolder holder,
            SliceItem.ActionHandler listener,
            Context context, String format) {
        switch (format) {
            case FORMAT_IMAGE:
                if (holder.mVersionedParcelable instanceof IconCompat) {
                    ((IconCompat) holder.mVersionedParcelable).checkResource(context);
                }
                break;
            case FORMAT_ACTION:
                holder.mCallback = listener;
                break;
        }
    }

    /**
     */
    // @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static int parseImageMode(@NonNull SliceItem iconItem) {
        return SliceActionImpl.parseImageMode(iconItem);
    }

    private static boolean doesStreamStartWith(String parcelName, BufferedInputStream inputStream) {
        byte[] data = parcelName.getBytes(Charset.forName("UTF-16"));
        byte[] buf = new byte[data.length];
        try {
            // Read out the int size of the string.
            if (inputStream.read(buf, 0, 4) < 0) {
                return false;
            }
            if (inputStream.read(buf, 0, buf.length) < 0) {
                return false;
            }
            return parcelName.equals(new String(buf, "UTF-16"));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Holds options for how to handle SliceItems that cannot be serialized.
     */
    public static class SerializeOptions {
        /**
         * Constant indicating that the an {@link IllegalArgumentException} should be thrown
         * when this format is encountered.
         */
        public static final int MODE_THROW = 0;
        /**
         * Constant indicating that the SliceItem should be removed when this format is encountered.
         */
        public static final int MODE_REMOVE = 1;
        /**
         * Constant indicating that the SliceItem should be serialized as much as possible.
         * <p>
         * For images this means they will be attempted to be serialized. For actions, the
         * action will be removed but the content of the action will be serialized. The action
         * may be triggered later on a de-serialized slice by binding the slice again and activating
         * a pending-intent at the same location as the serialized action.
         */
        public static final int MODE_CONVERT = 2;

        @IntDef({MODE_THROW, MODE_REMOVE, MODE_CONVERT})
        @Retention(SOURCE)
        @interface FormatMode {
        }

        private int mActionMode = MODE_THROW;
        private int mImageMode = MODE_THROW;
        private int mMaxWidth = 1000;
        private int mMaxHeight = 1000;

        private Bitmap.CompressFormat mFormat = Bitmap.CompressFormat.PNG;
        private int mQuality = 100;

        /**
         */
        // @RestrictTo(RestrictTo.Scope.LIBRARY)
        public void checkThrow(String format) {
            switch (format) {
                case FORMAT_ACTION:
                case FORMAT_REMOTE_INPUT:
                    if (mActionMode != MODE_THROW) return;
                    break;
                case FORMAT_IMAGE:
                    if (mImageMode != MODE_THROW) return;
                    break;
                default:
                    return;
            }
            throw new IllegalArgumentException(format + " cannot be serialized");
        }

        /**
         */
        // @RestrictTo(RestrictTo.Scope.LIBRARY)
        public @FormatMode int getActionMode() {
            return mActionMode;
        }

        /**
         */
        // @RestrictTo(RestrictTo.Scope.LIBRARY)
        public @FormatMode int getImageMode() {
            return mImageMode;
        }

        /**
         */
        // @RestrictTo(RestrictTo.Scope.LIBRARY)
        public int getMaxWidth() {
            return mMaxWidth;
        }

        /**
         */
        // @RestrictTo(RestrictTo.Scope.LIBRARY)
        public int getMaxHeight() {
            return mMaxHeight;
        }

        /**
         */
        // @RestrictTo(RestrictTo.Scope.LIBRARY)
        public Bitmap.CompressFormat getFormat() {
            return mFormat;
        }

        /**
         */
        // @RestrictTo(RestrictTo.Scope.LIBRARY)
        public int getQuality() {
            return mQuality;
        }

        /**
         * Sets how {@link android.app.slice.SliceItem#FORMAT_ACTION} items should be handled.
         *
         * The default mode is {@link #MODE_THROW}.
         *
         * @param mode The desired mode.
         */
        public SerializeOptions setActionMode(@FormatMode int mode) {
            mActionMode = mode;
            return this;
        }

        /**
         * Sets how {@link android.app.slice.SliceItem#FORMAT_IMAGE} items should be handled.
         *
         * The default mode is {@link #MODE_THROW}.
         *
         * @param mode The desired mode.
         */
        public SerializeOptions setImageMode(@FormatMode int mode) {
            mImageMode = mode;
            return this;
        }

        /**
         * Set the maximum width of an image to use when serializing.
         * <p>
         * Will only be used if the {@link #setImageMode(int)} is set to {@link #MODE_CONVERT}.
         * Any images larger than the maximum size will be scaled down to fit within that size.
         * The default value is 1000.
         */
        public SerializeOptions setMaxImageWidth(int width) {
            mMaxWidth = width;
            return this;
        }

        /**
         * Set the maximum height of an image to use when serializing.
         * <p>
         * Will only be used if the {@link #setImageMode(int)} is set to {@link #MODE_CONVERT}.
         * Any images larger than the maximum size will be scaled down to fit within that size.
         * The default value is 1000.
         */
        public SerializeOptions setMaxImageHeight(int height) {
            mMaxHeight = height;
            return this;
        }

        /**
         * Sets the options to use when converting icons to be serialized. Only used if
         * the image mode is set to {@link #MODE_CONVERT}.
         *
         * @param format  The format to encode images with, default is
         *                {@link android.graphics.Bitmap.CompressFormat#PNG}.
         * @param quality The quality to use when encoding images.
         */
        public SerializeOptions setImageConversionFormat(Bitmap.CompressFormat format,
                int quality) {
            mFormat = format;
            mQuality = quality;
            return this;
        }
    }

    /**
     * A listener used to receive events on slices parsed with
     * {@link #parseSlice(Context, InputStream, String, SliceActionListener)}.
     */
    public interface SliceActionListener {
        /**
         * Called when an action is triggered on a slice parsed with
         * {@link #parseSlice(Context, InputStream, String, SliceActionListener)}.
         *
         * @param actionUri The uri of the action selected.
         * @param context   The context passed to {@link SliceItem#fireAction(Context, Intent)}
         * @param intent    The intent passed to {@link SliceItem#fireAction(Context, Intent)}
         */
        void onSliceAction(Uri actionUri, Context context, Intent intent);
    }

    /**
     * Exception thrown during
     * {@link #parseSlice(Context, InputStream, String, SliceActionListener)}.
     */
    public static class SliceParseException extends Exception {
        /**
         */
        // @RestrictTo(RestrictTo.Scope.LIBRARY)
        public SliceParseException(String s, Throwable e) {
            super(s, e);
        }

        /**
         */
        // @RestrictTo(RestrictTo.Scope.LIBRARY)
        public SliceParseException(String s) {
            super(s);
        }
    }
}
