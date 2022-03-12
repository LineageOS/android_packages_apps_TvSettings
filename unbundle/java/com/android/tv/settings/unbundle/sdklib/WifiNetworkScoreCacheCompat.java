/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.content.Context;
import android.net.ScoredNetwork;
import android.net.wifi.WifiNetworkScoreCache;
import android.os.Handler;

import java.util.List;

public class WifiNetworkScoreCacheCompat {
    private final WifiNetworkScoreCache mWifiNetworkScoreCache;

    // TODO: refactor later
    public interface CacheListener {
        void networkCacheUpdated(List<ScoredNetwork> updatedNetworks);
    }

    public WifiNetworkScoreCacheCompat(
            Context context, CacheListener cacheListener, Handler handler) {
        mWifiNetworkScoreCache = new WifiNetworkScoreCache(context,
                new WifiNetworkScoreCache.CacheListener(handler) {
                    @Override
                    public void networkCacheUpdated(List<ScoredNetwork> updatedNetworks) {
                        cacheListener.networkCacheUpdated(updatedNetworks);
                    }
                });
    }
}
