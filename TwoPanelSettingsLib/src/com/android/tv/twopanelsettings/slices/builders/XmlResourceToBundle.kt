/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.tv.twopanelsettings.slices.builders

import android.content.Intent
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.AttributeSet
import androidx.annotation.XmlRes
import androidx.core.content.res.ResourcesCompat
import org.xmlpull.v1.XmlPullParser

/**
 * Converts R.xml. resources to structured bundles to assist generating TV Settings slices from
 * declarative resources.
 */
class XmlResourceToBundle(
  private val res: Resources,
  @XmlRes private val resourceId: Int,
  private val theme: Resources.Theme,
  private val packageName: String,
) {
  private val parser = res.getXml(resourceId)

  /**
   * Returns a nested bundle representation of specified R.xml resource. Each node has the following
   * extras:
   * - [NAME_KEY] Name of the XML tag (like "preference" for <preference>)
   * - [ATTRIBUTES_KEY] Bundle with extras for each XML attribute, like "alpha" -> 0.2f. Namespaces
   *   are not needed for current uses and may be added as another array later if needed.
   * - [CHILDREN_KEY] If children are present, bundle array in the same format as parent node
   * - [INTENT_KEY] If <intent> child is present, parsed out Intent it represents.
   *
   * @throws IllegalStateException if XML is malformed.
   */
  fun toBundle(): Bundle {
    var type: Int
    do {
      type = parser.next()
    } while (type != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT)

    if (type != XmlPullParser.START_TAG) {
      throw IllegalStateException("No start tag found: ${parser.positionDescription}")
    }

    return processElement(parser)
  }

  /** Extracts attributes for specified element and recursively traverses and adds children. */
  private fun processElement(parser: XmlResourceParser): Bundle {
    val bundle = Bundle()
    bundle.putSerializable(NAME_KEY, parser.name)

    val attrs = copyAttributesToBundle(parser)
    if (!attrs.isEmpty) {
      bundle.putParcelable(ATTRIBUTES_KEY, copyAttributesToBundle(parser))
    }

    val depth = parser.depth
    val children = ArrayList<Bundle>()

    do {
      val type = parser.next()

      if (
        (type == XmlPullParser.END_TAG && parser.depth == depth) ||
          type == XmlPullParser.END_DOCUMENT
      ) {
        break
      }

      if (type != XmlPullParser.START_TAG) {
        continue
      }

      if (parser.name == INTENT_KEY) {
        bundle.putParcelable(INTENT_KEY, Intent.parseIntent(res, parser, parser))
      } else {
        children.add(processElement(parser))
      }
    } while (true)

    if (children.isNotEmpty()) {
      bundle.putParcelableArrayList(CHILDREN_KEY, children)
    }

    return bundle
  }

  /** Extracts attribute values to bundle extras. */
  private fun copyAttributesToBundle(attributeSet: AttributeSet): Bundle {
    val bundle = Bundle()
    for (i in 0..<attributeSet.attributeCount) {
      val name = attributeSet.getAttributeName(i)
      val resourceId = attributeSet.getAttributeResourceValue(i, NO_RESOURCE)
      if (resourceId != NO_RESOURCE) {
        val resourceType = res.getResourceTypeName(resourceId)
        when (resourceType) {
          "array" -> bundle.putStringArray(name, res.getStringArray(resourceId))
          "bool" -> bundle.putBoolean(name, res.getBoolean(resourceId))
          "integer" -> bundle.putInt(name, res.getInteger(resourceId))
          "float" -> bundle.putFloat(name, ResourcesCompat.getFloat(res, resourceId))
          "drawable" -> bundle.putParcelable(name, Icon.createWithResource(packageName, resourceId))
          "color" -> bundle.putInt(name, res.getColor(resourceId, theme))
          "string" -> bundle.putString(name, res.getString(resourceId))
        }
      } else {
        // There is no direct way to get value type, so call getters for specific types and
        // check if they return default value.
        if (
          attributeSet.getAttributeBooleanValue(i, false) &&
            attributeSet.getAttributeValue(i) == "true"
        ) {
          bundle.putBoolean(name, true)
          continue
        }

        if (
          !attributeSet.getAttributeBooleanValue(i, true) &&
            attributeSet.getAttributeValue(i) == "false"
        ) {
          bundle.putBoolean(name, false)
          continue
        }

        val intValue = attributeSet.getAttributeIntValue(i, Int.MIN_VALUE)
        if (intValue != Int.MIN_VALUE || attributeSet.getAttributeIntValue(i, 0) == Int.MIN_VALUE) {
          bundle.putInt(name, intValue)
          continue
        }

        try {
          val floatValue = attributeSet.getAttributeFloatValue(i, Float.NaN)
          if (!floatValue.isNaN()) {
            bundle.putFloat(name, floatValue)
            continue
          }
        } catch (_: RuntimeException) {} // Not a float.

        bundle.putString(name, attributeSet.getAttributeValue(i))
      }
    }

    return bundle
  }

  companion object {
    private const val NO_RESOURCE = 0

    // Keys that store information from R.xml resource in a bundle.
    const val NAME_KEY = "name"
    const val ATTRIBUTES_KEY = "attributes"
    const val CHILDREN_KEY = "children"
    const val INTENT_KEY = "intent"
  }
}
