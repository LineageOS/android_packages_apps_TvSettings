/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.tv.settings;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.res.Fs;
import org.robolectric.res.ResourcePath;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Custom test runner. This is needed because the
 * default behavior for robolectric is just to grab the resource directory in the target package.
 * We want to override this to add several spanning different projects.
 */
public class TvSettingsRobolectricTestRunner extends RobolectricTestRunner {
    public TvSettingsRobolectricTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected AndroidManifest getAppManifest(Config config) {
        try {
            final URL appRoot = new URL("file:packages/apps/TvSettings/Settings/");
            final URL manifestPath = new URL(appRoot, "AndroidManifest.xml");
            final URL resDir = new URL(appRoot, "tests/robotests/res");
            final URL assetsDir = new URL(appRoot, config.assetDir());

            return new AndroidManifest(Fs.fromURL(manifestPath),
                    Fs.fromURL(resDir), Fs.fromURL(assetsDir), "com.android.tv.settings") {
                @Override
                public List<ResourcePath> getIncludedResourcePaths() {
                    final List<ResourcePath> paths =
                            super.getIncludedResourcePaths();
                    getIncludedResourcePahts(paths);
                    return paths;
                }
            };
        } catch (MalformedURLException e) {
            throw new RuntimeException("TvSettingsRobolectricTestRunner failure", e);
        }
    }

    private static void getIncludedResourcePahts(List<ResourcePath> paths) {
        try {
            paths.add(new ResourcePath(null,
                    Fs.fromURL(new URL("file:packages/apps/TvSettings/Settings/res")), null));
            paths.add(new ResourcePath(null,
                    Fs.fromURL(new URL("file:frameworks/base/packages/SettingsLib/res")), null));
            paths.add(new ResourcePath(null,
                    Fs.fromURL(new URL("file:frameworks/base/core/res/res")), null));
            paths.add(new ResourcePath(null,
                    Fs.fromURL(new URL("file:frameworks/support/leanback/res")), null));
            paths.add(new ResourcePath(null,
                    Fs.fromURL(new URL("file:frameworks/support/v7/preference/res")), null));
        } catch (MalformedURLException e) {
            throw new RuntimeException("TvSettingsRobolectricTestRunner failure", e);
        }
    }
}
