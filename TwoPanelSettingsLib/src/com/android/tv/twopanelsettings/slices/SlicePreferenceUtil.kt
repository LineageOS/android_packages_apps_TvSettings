/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tv.twopanelsettings.slices

import android.app.slice.Slice
import android.content.Context
import android.graphics.drawable.Icon
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import android.util.Pair
import androidx.core.graphics.drawable.IconCompat
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.android.tv.twopanelsettings.IconUtil
import com.android.tv.twopanelsettings.R
import com.android.tv.twopanelsettings.slices.NonSlicePreferenceBuilder.Companion.forClassName
import com.android.tv.twopanelsettings.slices.compat.SliceItem
import com.android.tv.twopanelsettings.slices.compat.core.SliceActionImpl
import com.android.tv.twopanelsettings.slices.compat.core.SliceQuery
import com.android.tv.twopanelsettings.slices.compat.widget.SliceContent

/**
 * Generate corresponding preference based upon the slice data.
 */
object SlicePreferencesUtil {
    private const val TAG = "SlicePreferenceUtil"

    fun getPreference(
        item: SliceItem?, context: Context,
        className: String?, isTwoPanel: Boolean, parent: PreferenceGroup?
    ): Preference? {
        var preference: Preference? = null
        if (item == null) {
            return null
        }
        val data = extract(item)
        if (item.subType != null) {
            val subType = item.subType!!
            if (isPreferenceSubType(subType)) {
                // TODO: Figure out all the possible cases and reorganize the logic
                if (data.mClassNameItem != null) {
                    try {
                        preference = forClassName(
                            data.mClassNameItem!!.text.toString()
                        )
                            .create(
                                context,
                                if (data.mPropertiesItem != null)
                                    data.mPropertiesItem!!.bundle
                                else
                                    null
                            )
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to create preference", e)
                        return null
                    }
                } else if (data.mInfoItems.size > 0) {
                    preference = InfoPreference(
                        context, getInfoList(data.mInfoItems)
                    )
                } else if (data.mIntentItem != null) {
                    val action = SliceActionImpl(
                        data.mIntentItem!!
                    )
                    if (action != null) {
                        // Currently if we don't set icon for the SliceAction, slice lib will
                        // automatically treat it as a toggle. To distinguish preference action and
                        // toggle action, we need to add a subtype if this is a preference action.
                        preference = SlicePreference(context)
                        preference.sliceAction =
                            action
                        preference.actionId =
                            getActionId(item)
                        if (data.mFollowupIntentItem != null) {
                            val followUpAction =
                                SliceActionImpl(data.mFollowupIntentItem!!)
                            preference.followupSliceAction =
                                followUpAction
                        }
                    }
                } else if (data.mEndItems.size > 0 && data.mEndItems[0] != null) {
                    val action = SliceActionImpl(
                        data.mEndItems[0]!!
                    )
                    if (action != null) {
                        val buttonStyle = getButtonStyle(item)
                        when (buttonStyle) {
                            SlicesConstants.CHECKMARK -> preference = SliceCheckboxPreference(
                                context, action
                            )

                            SlicesConstants.SWITCH -> preference =
                                SliceSwitchPreference(context, action)

                            SlicesConstants.RADIO -> {
                                preference = SliceRadioPreference(context, action)
                                if (getRadioGroup(item) != null) {
                                    preference.radioGroup =
                                        getRadioGroup(item).toString()
                                }
                            }

                            SlicesConstants.SEEKBAR -> {
                                val min = getSeekbarMin(item)
                                val max = getSeekbarMax(item)
                                val value = getSeekbarValue(item)
                                preference = SliceSeekbarPreference(
                                    context, action, min, max, value
                                )
                            }
                        }
                        if (preference is HasSliceAction) {
                            (preference as HasSliceAction).actionId = getActionId(item)
                        }
                        if (data.mFollowupIntentItem != null) {
                            val followUpAction =
                                SliceActionImpl(data.mFollowupIntentItem!!)
                            (preference as HasSliceAction).followupSliceAction = followUpAction
                        }
                    }
                }

                val uri = getText(data.mTargetSliceItem)
                if (uri == null || TextUtils.isEmpty(uri)) {
                    if (preference == null) {
                        preference = CustomContentDescriptionPreference(context)
                    }
                } else {
                    if (preference == null) {
                        preference =
                            if (subType == SlicesConstants.TYPE_PREFERENCE_EMBEDDED_PLACEHOLDER) {
                                EmbeddedSlicePreference(
                                    context,
                                    uri.toString()
                                )
                            } else {
                                SlicePreference(context)
                            }
                        if (hasEndIcon(data.mHasEndIconItem)) {
                            preference.layoutResource = R.layout.preference_reversed_icon
                        }
                    }
                    (preference as HasSliceUri).uri = uri.toString()
                    if (preference is HasSliceAction) {
                        (preference as HasSliceAction).actionId = getActionId(item)
                    }
                    preference.fragment = className
                }
            } else if (item.subType == SlicesConstants.TYPE_PREFERENCE_CATEGORY) {
                preference = CustomContentDescriptionPreferenceCategory(context)
            }
        }

        if (preference != null) {
            if (preference is PreferenceGroup && !data.mChildPreferences.isEmpty()) {
                val group = preference
                parent?.addPreference(preference)
                for (child in data.mChildPreferences) {
                    val childPreference = getPreference(
                        child, context, className, isTwoPanel, group
                    )
                    if (childPreference != null) {
                        group.addPreference(childPreference)
                    }
                }
                parent?.removePreference(preference)
            }

            val isEnabled = enabled(item)
            // Set whether preference is enabled.
            if (preference is InfoPreference || !isEnabled) {
                preference.isEnabled = false
            }
            // Set whether preference is selectable
            if (!selectable(item) || !isEnabled) {
                preference.isSelectable = false
            }
            // Set the key for the preference
            val key = getKey(item)
            if (key != null) {
                preference.key = key.toString()
            }

            val icon = getIcon(data.mStartItem)
            if (icon != null) {
                val isIconNeedToBeProcessed =
                    isIconNeedsToBeProcessed(item)
                val iconDrawable = icon.loadDrawable(context)
                if (isIconNeedToBeProcessed && isTwoPanel) {
                    preference.icon =
                        IconUtil.getCompoundIcon(context, iconDrawable)
                } else {
                    preference.icon = iconDrawable
                }
            }

            if (data.mTitleItem != null) {
                preference.title = getText(data.mTitleItem)
            }

            //Set summary
            val subtitle =
                if (data.mSubtitleItem != null) data.mSubtitleItem!!.text else null
            val subtitleExists = !TextUtils.isEmpty(subtitle)
                    || (data.mSubtitleItem != null && data.mSubtitleItem!!.hasHint(Slice.HINT_PARTIAL))
            if (subtitleExists) {
                preference.summary = subtitle
            } else {
                if (data.mSummaryItem != null) {
                    preference.summary = getText(data.mSummaryItem)
                }
            }

            // Set preview info image and text
            val infoText = getInfoText(item)
            val infoSummary = getInfoSummary(item)
            val addInfoStatus = addInfoStatus(item)
            val infoImage = getInfoImage(item)
            val infoTitleIcon = getInfoTitleIcon(item)
            val b = preference.extras
            var fallbackInfoContentDescription = ""
            if (preference.title != null) {
                fallbackInfoContentDescription += preference.title.toString()
            }
            if (infoImage != null) {
                b.putParcelable(SlicesConstants.EXTRA_PREFERENCE_INFO_IMAGE, infoImage.toIcon())
            }
            if (infoTitleIcon != null) {
                b.putParcelable(
                    SlicesConstants.EXTRA_PREFERENCE_INFO_TITLE_ICON,
                    infoTitleIcon.toIcon()
                )
            }
            if (infoText != null) {
                if (preference is SliceSwitchPreference && addInfoStatus) {
                    b.putBoolean(InfoFragment.EXTRA_INFO_HAS_STATUS, true)
                    b.putBoolean(
                        SlicesConstants.EXTRA_PREFERENCE_INFO_STATUS,
                        preference.isChecked
                    )
                } else {
                    b.putBoolean(InfoFragment.EXTRA_INFO_HAS_STATUS, false)
                }
                b.putCharSequence(SlicesConstants.EXTRA_PREFERENCE_INFO_TEXT, infoText)
                if (preference.title != null
                    && preference.title != infoText.toString()
                ) {
                    fallbackInfoContentDescription +=
                        HasCustomContentDescription.CONTENT_DESCRIPTION_SEPARATOR + infoText.toString()
                }
            }
            if (infoSummary != null) {
                b.putCharSequence(SlicesConstants.EXTRA_PREFERENCE_INFO_SUMMARY, infoSummary)
                fallbackInfoContentDescription +=
                    HasCustomContentDescription.CONTENT_DESCRIPTION_SEPARATOR + infoSummary
            }
            val contentDescription = getInfoContentDescription(item)
            // Respect the content description values provided by slice.
            // If not provided, for SlicePreference, SliceSwitchPreference,
            // CustomContentDescriptionPreference, use the fallback value.
            // Otherwise, do not set the contentDescription for preference. Rely on the talkback
            // framework to generate the value itself.
            if (!TextUtils.isEmpty(contentDescription)) {
                if (preference is HasCustomContentDescription) {
                    (preference as HasCustomContentDescription).contentDescription =
                        contentDescription
                }
            } else {
                if ((preference is SlicePreference)
                    || (preference is SliceSwitchPreference)
                    || (preference is CustomContentDescriptionPreference)
                ) {
                    (preference as HasCustomContentDescription).contentDescription =
                        fallbackInfoContentDescription
                }
            }
            if (infoImage != null || infoText != null || infoSummary != null) {
                preference.fragment = InfoFragment::class.java.canonicalName
            }
        }

        return preference
    }

    fun extract(sliceItem: SliceItem): Data {
        val data = Data()
        val possibleStartItems =
            SliceQuery.findAll(sliceItem, null, Slice.HINT_TITLE, null)
        if (possibleStartItems.size > 0) {
            // The start item will be at position 0 if it exists
            val format = possibleStartItems[0].format
            if ((android.app.slice.SliceItem.FORMAT_ACTION == format
                        && SliceQuery.find(
                    possibleStartItems[0],
                    android.app.slice.SliceItem.FORMAT_IMAGE
                ) != null)
                || android.app.slice.SliceItem.FORMAT_SLICE == format
                || android.app.slice.SliceItem.FORMAT_LONG == format
                || android.app.slice.SliceItem.FORMAT_IMAGE == format
            ) {
                data.mStartItem = possibleStartItems[0]
            }
        }

        val items = sliceItem.slice!!
            .items
        for (i in items.indices) {
            val item = items[i]
            val subType = item.subType
            if (subType != null) {
                if (isPreferenceSubType(subType)) {
                    data.mChildPreferences.add(item)
                    continue
                }
                when (subType) {
                    SlicesConstants.SUBTYPE_INFO_PREFERENCE -> data.mInfoItems.add(item)
                    SlicesConstants.SUBTYPE_INTENT -> data.mIntentItem = item
                    SlicesConstants.SUBTYPE_FOLLOWUP_INTENT -> data.mFollowupIntentItem = item
                    SlicesConstants.TAG_TARGET_URI -> data.mTargetSliceItem = item
                    SlicesConstants.EXTRA_HAS_END_ICON -> data.mHasEndIconItem = item
                    SlicesConstants.SUBTYPE_CLASSNAME -> data.mClassNameItem = item
                    SlicesConstants.SUBTYPE_PROPERTIES -> data.mPropertiesItem = item
                }
            } else if (android.app.slice.SliceItem.FORMAT_TEXT == item.format && (item.subType == null)) {
                if ((data.mTitleItem == null || !data.mTitleItem!!.hasHint(Slice.HINT_TITLE))
                    && item.hasHint(Slice.HINT_TITLE) && !item.hasHint(Slice.HINT_SUMMARY)
                ) {
                    data.mTitleItem = item
                } else if (data.mSubtitleItem == null && !item.hasHint(Slice.HINT_SUMMARY)) {
                    data.mSubtitleItem = item
                } else if (data.mSummaryItem == null && item.hasHint(Slice.HINT_SUMMARY)) {
                    data.mSummaryItem = item
                }
            } else {
                data.mEndItems.add(item)
            }
        }
        data.mEndItems.remove(data.mStartItem)
        return data
    }

    private fun getInfoList(sliceItems: List<SliceItem>): List<Pair<CharSequence?, CharSequence?>> {
        val infoList: MutableList<Pair<CharSequence?, CharSequence?>> = ArrayList()
        for (item in sliceItems) {
            val itemSlice = item.slice
            if (itemSlice != null) {
                var title: CharSequence? = null
                var summary: CharSequence? = null
                for (element in itemSlice.items) {
                    if (element.hints.contains(Slice.HINT_TITLE)) {
                        title = element.text
                    } else if (element.hints.contains(Slice.HINT_SUMMARY)) {
                        summary = element.text
                    }
                }
                infoList.add(Pair(title, summary))
            }
        }
        return infoList
    }

    private fun getKey(item: SliceItem): CharSequence? {
        val target =
            SliceQuery.findSubtype(
                item,
                android.app.slice.SliceItem.FORMAT_TEXT,
                SlicesConstants.TAG_KEY
            )
        return target?.text
    }

    private fun getRadioGroup(item: SliceItem): CharSequence? {
        val target = SliceQuery.findSubtype(
            item, android.app.slice.SliceItem.FORMAT_TEXT, SlicesConstants.TAG_RADIO_GROUP
        )
        return target?.text
    }

    /**
     * Get the screen title item for the slice.
     * @param sliceItems list of SliceItem extracted from slice data.
     * @return screen title item.
     */
    fun getScreenTitleItem(sliceItems: List<SliceContent>): SliceItem? {
        for (contentItem in sliceItems) {
            val item = contentItem.sliceItem
            if (item!!.subType != null
                && item.subType == SlicesConstants.TYPE_PREFERENCE_SCREEN_TITLE
            ) {
                return item
            }
        }
        return null
    }

    fun getRedirectSlice(sliceItems: List<SliceContent>): SliceItem? {
        for (contentItem in sliceItems) {
            val item = contentItem.sliceItem
            if (item!!.subType != null
                && item.subType == SlicesConstants.TYPE_REDIRECTED_SLICE_URI
            ) {
                return item
            }
        }
        return null
    }

    fun getFocusedPreferenceItem(sliceItems: List<SliceContent>): SliceItem? {
        for (contentItem in sliceItems) {
            val item = contentItem.sliceItem
            if (item!!.subType != null
                && item.subType == SlicesConstants.TYPE_FOCUSED_PREFERENCE
            ) {
                return item
            }
        }
        return null
    }

    fun getEmbeddedItem(sliceItems: List<SliceContent>): SliceItem? {
        for (contentItem in sliceItems) {
            val item = contentItem.sliceItem
            if (item!!.subType != null
                && item.subType == SlicesConstants.TYPE_PREFERENCE_EMBEDDED
            ) {
                return item
            }
        }
        return null
    }

    private fun isIconNeedsToBeProcessed(sliceItem: SliceItem): Boolean {
        val items = sliceItem.slice!!
            .items
        for (item in items) {
            if (item.subType != null && item.subType == SlicesConstants.SUBTYPE_ICON_NEED_TO_BE_PROCESSED) {
                return item.int == 1
            }
        }
        return false
    }

    private fun getButtonStyle(sliceItem: SliceItem): Int {
        val items = sliceItem.slice!!
            .items
        for (item in items) {
            if (item.subType != null
                && item.subType == SlicesConstants.SUBTYPE_BUTTON_STYLE
            ) {
                return item.int
            }
        }
        return -1
    }

    private fun getSeekbarMin(sliceItem: SliceItem): Int {
        val items = sliceItem.slice!!
            .items
        for (item in items) {
            if (item.subType != null
                && item.subType == SlicesConstants.SUBTYPE_SEEKBAR_MIN
            ) {
                return item.int
            }
        }
        return -1
    }

    private fun getSeekbarMax(sliceItem: SliceItem): Int {
        val items = sliceItem.slice!!
            .items
        for (item in items) {
            if (item.subType != null
                && item.subType == SlicesConstants.SUBTYPE_SEEKBAR_MAX
            ) {
                return item.int
            }
        }
        return -1
    }

    private fun getSeekbarValue(sliceItem: SliceItem): Int {
        val items = sliceItem.slice!!
            .items
        for (item in items) {
            if (item.subType != null
                && item.subType == SlicesConstants.SUBTYPE_SEEKBAR_VALUE
            ) {
                return item.int
            }
        }
        return -1
    }

    private fun enabled(sliceItem: SliceItem): Boolean {
        val items = sliceItem.slice!!
            .items
        for (item in items) {
            if (item.subType != null
                && item.subType == SlicesConstants.SUBTYPE_IS_ENABLED
            ) {
                return item.int == 1
            }
        }
        return true
    }

    private fun selectable(sliceItem: SliceItem): Boolean {
        val items = sliceItem.slice!!
            .items
        for (item in items) {
            if (item.subType != null
                && item.subType == SlicesConstants.SUBTYPE_IS_SELECTABLE
            ) {
                return item.int == 1
            }
        }
        return true
    }

    private fun addInfoStatus(sliceItem: SliceItem): Boolean {
        val items = sliceItem.slice!!
            .items
        for (item in items) {
            if (item.subType != null
                && item.subType == SlicesConstants.EXTRA_ADD_INFO_STATUS
            ) {
                return item.int == 1
            }
        }
        return true
    }

    private fun hasEndIcon(item: SliceItem?): Boolean {
        return item != null && item.int > 0
    }

    /**
     * Checks if custom content description should be forced to be used if provided. This function
     * can be extended with more cases if needed.
     *
     * @param item The [SliceItem] containing the necessary information.
     * @return `true` if custom content description should be used.
     */
    private fun shouldForceContentDescription(sliceItem: SliceItem): Boolean {
        val items = sliceItem.slice!!
            .items
        for (item in items) {
            // Checks if an end icon has been set.
            if (item.subType != null
                && item.subType == SlicesConstants.EXTRA_HAS_END_ICON
            ) {
                return hasEndIcon(item)
            }
        }
        return false
    }

    /**
     * Get the text from the SliceItem.
     */
    fun getText(item: SliceItem?): CharSequence {
        if (item == null) {
            return ""
        }
        return item.text ?: ""
    }

    /** Get the icon from the SliceItem if available  */
    fun getIcon(startItem: SliceItem?): Icon? {
        if (startItem != null && startItem.slice != null && startItem.slice!!.items != null && startItem.slice!!
                .items.size > 0
        ) {
            val iconItem = startItem.slice!!
                .items[0]
            if (android.app.slice.SliceItem.FORMAT_IMAGE == iconItem.format) {
                val icon = iconItem.icon
                return icon!!.toIcon()
            }
        }
        return null
    }

    fun getStatusPath(uriString: String?): Uri {
        val statusUri = Uri.parse(uriString)
            .buildUpon().path("/" + SlicesConstants.PATH_STATUS).build()
        return statusUri
    }

    fun getPageId(item: SliceItem?): Int {
        val target =
            SliceQuery.findSubtype(
                item,
                android.app.slice.SliceItem.FORMAT_INT,
                SlicesConstants.EXTRA_PAGE_ID
            )
        return target?.int ?: 0
    }

    private fun getActionId(item: SliceItem): Int {
        val target =
            SliceQuery.findSubtype(
                item,
                android.app.slice.SliceItem.FORMAT_INT,
                SlicesConstants.EXTRA_ACTION_ID
            )
        return target?.int ?: 0
    }


    private fun getInfoText(item: SliceItem): CharSequence? {
        val target =
            SliceQuery.findSubtype(
                item,
                android.app.slice.SliceItem.FORMAT_TEXT,
                SlicesConstants.EXTRA_PREFERENCE_INFO_TEXT
            )
        return target?.text
    }

    private fun getInfoSummary(item: SliceItem): CharSequence? {
        val target =
            SliceQuery.findSubtype(
                item,
                android.app.slice.SliceItem.FORMAT_TEXT,
                SlicesConstants.EXTRA_PREFERENCE_INFO_SUMMARY
            )
        return target?.text
    }

    private fun getInfoImage(item: SliceItem): IconCompat? {
        val target =
            SliceQuery.findSubtype(
                item,
                android.app.slice.SliceItem.FORMAT_IMAGE,
                SlicesConstants.EXTRA_PREFERENCE_INFO_IMAGE
            )
        return target?.icon
    }

    private fun getInfoTitleIcon(item: SliceItem): IconCompat? {
        val target = SliceQuery.findSubtype(
            item,
            android.app.slice.SliceItem.FORMAT_IMAGE,
            SlicesConstants.EXTRA_PREFERENCE_INFO_TITLE_ICON
        )
        return target?.icon
    }

    /**
     * Get the content description from SliceItem if available
     */
    private fun getInfoContentDescription(
        sliceItem: SliceItem
    ): String? {
        val items = sliceItem.slice!!
            .items
        for (item in items) {
            if (item.subType != null
                && item.subType == Slice.SUBTYPE_CONTENT_DESCRIPTION
            ) {
                return item.text.toString()
            }
        }
        return null
    }

    private fun isPreferenceSubType(subType: String): Boolean {
        return subType == SlicesConstants.TYPE_PREFERENCE
                || subType == SlicesConstants.TYPE_PREFERENCE_EMBEDDED
                || subType == SlicesConstants.TYPE_PREFERENCE_EMBEDDED_PLACEHOLDER
    }

    class Data {
        var mStartItem: SliceItem? = null
        var mTitleItem: SliceItem? = null
        var mSubtitleItem: SliceItem? = null
        var mSummaryItem: SliceItem? = null
        var mTargetSliceItem: SliceItem? = null
        var mRadioGroupItem: SliceItem? = null
        var mIntentItem: SliceItem? = null
        var mFollowupIntentItem: SliceItem? = null
        var mHasEndIconItem: SliceItem? = null
        var mEndItems: MutableList<SliceItem?> = ArrayList()
        var mInfoItems: MutableList<SliceItem> = ArrayList()
        var mClassNameItem: SliceItem? = null
        var mPropertiesItem: SliceItem? = null
        var mChildPreferences: MutableList<SliceItem> = ArrayList()
    }
}