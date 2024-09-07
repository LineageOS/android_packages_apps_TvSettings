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

package com.android.tv.settings.accessibility.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class KeyRepeatViewModel extends ViewModel {
    private MutableLiveData<String> keyRepeatRateSummary =
            new MutableLiveData<String>(""); // Initially empty
    private MutableLiveData<String> delayBeforeKeyRepeatSummary =
            new MutableLiveData<String>(""); // Initially empty


    public LiveData<String> getKeyRepeatRateSummary() {
        return keyRepeatRateSummary;
    }

    public void setKeyRepeatRateSummary(String summary) {
        this.keyRepeatRateSummary.setValue(summary);
    }

    public LiveData<String> getDelayBeforeKeyRepeatSummary(){
        return delayBeforeKeyRepeatSummary;
    }

    public void setDelayBeforeKeyRepeatSummary(String summary){
        this.delayBeforeKeyRepeatSummary.setValue(summary);
    }
}
