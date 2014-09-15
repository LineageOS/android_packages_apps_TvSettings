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
import com.android.tv.settings.dialog.old.Action;
import com.android.tv.settings.widget.ScrollAdapter;
import com.android.tv.settings.widget.ScrollAdapterBase;
import com.android.tv.settings.widget.ScrollAdapterView.OnItemChangeListener;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

class NotificationAdapter extends ArrayAdapter<NotificationListItem> implements ScrollAdapter,
        View.OnFocusChangeListener, View.OnKeyListener, OnItemChangeListener {

    private static final String TAG = "NotificationAdapter";
    private static final boolean DEBUG = true;

    interface NotificationOnClickListener {
        void onClick(NotificationListItem item);
    }

    // TODO: this constant is only in KLP: update when KLP has a more standard
    // SDK.
    private static final int FX_KEYPRESS_INVALID = 9; // AudioManager.KEYPRESS_INVALID;

    private final NotificationOnClickListener mListener;
    private final int mAnimationDuration;
    private final float mFocusedMainAlpha;
    private final float mUnfocusedAlpha;
    private boolean mKeyPressed;

    public NotificationAdapter(Context context, NotificationOnClickListener listener) {
        super(context, R.layout.notification_row);
        mListener = listener;

        final Resources res = context.getResources();
        mAnimationDuration = res.getInteger(R.integer.dialog_animation_duration);

        mFocusedMainAlpha = getFloatResource(R.dimen.list_item_selected_title_text_alpha);
        mUnfocusedAlpha = getFloatResource(R.dimen.list_item_unselected_text_alpha);

        mKeyPressed = false;
    }

    @Override
    public void viewRemoved(View view) {
    }

    @Override
    public View getScrapView(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        return inflater.inflate(R.layout.notification_row, parent, false);
    }

    @Override
    public ScrollAdapterBase getExpandAdapter() {
        return null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = getScrapView(parent);

        NotificationListItem item = getItem(position);

        ViewGroup mainStyle = (ViewGroup) convertView.findViewById(R.id.main_style);
        ViewGroup customStyle = (ViewGroup) convertView.findViewById(R.id.custom_style);
        TextView titleView = (TextView) convertView.findViewById(R.id.title);
        TextView summaryView = (TextView) convertView.findViewById(R.id.description);
        TextView subtextView = (TextView) convertView.findViewById(R.id.subtext);
        NotificationDateTimeView timestampView = (NotificationDateTimeView) convertView
                .findViewById(R.id.timestamp);
        TextView badgeTextView = (TextView) convertView.findViewById(R.id.badge_text);
        ImageView iconBig = (ImageView) convertView.findViewById(R.id.icon_big);
        ImageView iconSmall = (ImageView) convertView.findViewById(R.id.icon_small);
        ImageView badgeIcon = (ImageView) convertView.findViewById(R.id.badge_icon);

        RemoteViews customView = item.getCustomView();

        View remoteView = null;
        if (customView != null) {
            try {
                remoteView = customView.apply(getContext(), customStyle);
            } catch (Exception e) {
                Log.e(TAG, "Couldn't inflate custom view, defaulting to standard view: " + e);
            }
        }

        if (remoteView != null) {
            mainStyle.setVisibility(View.GONE);
            setItemInView(item, customStyle);
            customStyle.addView(remoteView);
            customStyle.setVisibility(View.VISIBLE);
            makeUnfocusable(customStyle);
        } else {
            setTextView(titleView, item.getTitle());
            setTextView(summaryView, item.getText());
            setTextView(subtextView, item.getSubText());
            setNotificationDateTimeView(timestampView, item.getTimestamp());
            setTextView(badgeTextView, item.getBadgeText());

            if (item.showLeftIconSpace()) {
                Bitmap largeIcon = item.getLargeIcon();
                String smallIconUri = item.getSmallIconUri();
                String badgeIconUri = item.getBadgeIconUri();

                if (largeIcon != null) {
                    iconBig.setImageBitmap(largeIcon);
                } else {
                    ViewUtil.hideView(iconBig);
                }
                ViewUtil.setImageViewUri(getContext(), iconSmall, smallIconUri);
                ViewUtil.setImageViewUri(getContext(), badgeIcon, badgeIconUri);
            } else {
                ViewUtil.hideView(convertView.findViewById(R.id.icon));
            }
        }

        setItemInView(item, convertView);
        convertView.setTag(R.id.main_style, mainStyle);
        convertView.setTag(R.id.custom_style, customStyle);
        convertView.setTag(R.id.title, titleView);
        convertView.setTag(R.id.description, summaryView);
        convertView.setTag(R.id.subtext, subtextView);
        convertView.setTag(R.id.timestamp, timestampView);
        convertView.setTag(R.id.icon_big, iconBig);
        convertView.setTag(R.id.icon_small, iconSmall);
        convertView.setTag(R.id.badge_icon, badgeIcon);
        convertView.setTag(R.id.badge_text, badgeTextView);

        convertView.setOnKeyListener(this);
        convertView.setOnFocusChangeListener(this);

        onFocusChange(convertView, false);

        return convertView;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if ((v != null) && (!hasFocus || !getItemFromView(v).isEnabled())) {
            v.setAlpha(mUnfocusedAlpha);
        }
    }

    @Override
    public void onItemSelected(View v, int position, int targetCenter) {
        if (v != null && getItemFromView(v).isEnabled()) {
            v.setAlpha(mFocusedMainAlpha);
        }
    }

    /**
     * Now only handles KEYCODE_ENTER and KEYCODE_NUMPAD_ENTER key event.
     */
    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (v == null) {
            return false;
        }
        final NotificationListItem item = getItemFromView(v);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BUTTON_X:
            case KeyEvent.KEYCODE_BUTTON_Y:
            case KeyEvent.KEYCODE_ENTER:
                if (!item.isEnabled()) {
                    if (v.isSoundEffectsEnabled() && event.getAction() == KeyEvent.ACTION_DOWN) {
                        AudioManager manager = (AudioManager) v.getContext()
                                .getSystemService(Context.AUDIO_SERVICE);
                        manager.playSoundEffect(FX_KEYPRESS_INVALID);
                    }
                    return false;
                }

                switch (event.getAction()) {
                    case KeyEvent.ACTION_DOWN:
                        if (!mKeyPressed) {
                            mKeyPressed = true;

                            if (DEBUG) {
                                Log.d(TAG, "Enter Key down");
                            }

                            prepareAndAnimateView(v, mFocusedMainAlpha,
                                    mUnfocusedAlpha, mAnimationDuration,
                                    0, null, mKeyPressed);
                        }
                        break;
                    case KeyEvent.ACTION_UP:
                        if (mKeyPressed) {
                            mKeyPressed = false;

                            if (DEBUG) {
                                Log.d(TAG, "Enter Key up");
                            }

                            prepareAndAnimateView(v, mUnfocusedAlpha,
                                    mFocusedMainAlpha, mAnimationDuration,
                                    0, null, mKeyPressed);
                        }
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
        return false;
    }

    private NotificationListItem getItemFromView(View v) {
        return (NotificationListItem) v.getTag(R.layout.notification_row);
    }

    private void setItemInView(NotificationListItem item, View v) {
        v.setTag(R.layout.notification_row, item);
    }

    private void prepareAndAnimateView(final View v, float initAlpha, float destAlpha, int duration,
            int delay, Interpolator interpolator, final boolean pressed) {
        if (v != null && v.getWindowToken() != null) {
            final NotificationListItem item = getItemFromView(v);

            v.setAlpha(initAlpha);
            v.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            v.buildLayer();
            v.animate().alpha(destAlpha).setDuration(duration).setStartDelay(delay);
            if (interpolator != null) {
                v.animate().setInterpolator(interpolator);
            }
            v.animate().setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (v == null) {
                        return;
                    }

                    v.setLayerType(View.LAYER_TYPE_NONE, null);
                    if (!pressed) {
                        if (item.isEnabled()) {
                            mListener.onClick(item);
                        }
                    }
                }
            });
            v.animate().start();
        }
    }

    private void makeUnfocusable(View v) {
        v.setFocusable(false);
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            final int children = vg.getChildCount();
            for (int i = 0; i < children; i++) {
                makeUnfocusable(vg.getChildAt(i));
            }
        }
    }

    private void setTextView(TextView textView, String value) {
        if (value != null) {
            textView.setText(value);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }
    }

    private void setNotificationDateTimeView(NotificationDateTimeView timestampView, long value) {
        if (value != -1) {
            timestampView.setTime(value);
            timestampView.setVisibility(View.VISIBLE);
        } else {
            timestampView.setVisibility(View.GONE);
        }
    }

    private float getFloatResource(int resourceId) {
        TypedValue holder = new TypedValue();
        getContext().getResources().getValue(resourceId, holder, true);
        return holder.getFloat();
    }
}
