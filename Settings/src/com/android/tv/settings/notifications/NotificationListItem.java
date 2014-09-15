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

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

/**
 * Used by the NotificationAdaptor and NotificationCOntentFragment to display
 * their text and image views.
 */
interface NotificationListItem {

    /**
     * @return the title of the item or null if the item has no title.
     */
    String getTitle();

    /**
     * @return the text of the item or null if the item has no text.
     */
    String getText();

    /**
     * @return the subtext of the item or null if the item has no subtext.
     */
    String getSubText();

    /**
     * @return the timestamp of the item or -1 if the item has no timestamp.
     */
    long getTimestamp();

    /**
     * @return the badge text of the item or null if the item has no badge text.
     */
    String getBadgeText();

    /**
     * @return the large icon of the item or null if the item has no large icon.
     */
    Bitmap getLargeIcon();

    /**
     * @return the uri to the small icon of the item or null if the item has no
     *         small icon.
     */
    String getSmallIconUri();

    /**
     * @return the uri to the badge icon of the item or null if the item has no
     *         badge icon.
     */
    String getBadgeIconUri();

    /**
     * @return true if the left icon space should be shown for this item or false
     *         if no left icon space should be shown for this item.
     */
    boolean showLeftIconSpace();

    /**
     * Performs any associated action the list item has.
     *
     * @param context the context to use to perform the action.
     */
    void launch(Context context);

    /**
     * @return the item's custom view or null if the item doesn't have a custom
     *         view.
     */
    RemoteViews getCustomView();

    /**
     * @return true if clicking the item should cause something to happen, false otherwise.
     */
    boolean isEnabled();
}
