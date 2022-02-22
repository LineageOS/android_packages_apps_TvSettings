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

import android.content.pm.UserInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

import java.util.ArrayList;
import java.util.List;

public class UserInfoCompat implements Parcelable {
    UserInfo mUserInfo;

    public int restrictedProfileParentId;
    public static final int NO_PROFILE_GROUP_ID = UserHandle.USER_NULL;

    protected UserInfoCompat(Parcel in) {
        mUserInfo = in.readParcelable(UserInfo.class.getClassLoader());
        restrictedProfileParentId = in.readInt();
        id = in.readInt();
    }

    public static final Creator<UserInfoCompat> CREATOR = new Creator<UserInfoCompat>() {
        @Override
        public UserInfoCompat createFromParcel(Parcel in) {
            return new UserInfoCompat(in);
        }

        @Override
        public UserInfoCompat[] newArray(int size) {
            return new UserInfoCompat[size];
        }
    };

    public UserHandle getUserHandle() {
        return mUserInfo.getUserHandle();
    }

    UserInfoCompat(UserInfo userInfo) {
        mUserInfo = userInfo;
        id = userInfo.id;
        restrictedProfileParentId = userInfo.restrictedProfileParentId;
    }

    public boolean isManagedProfile() {
        return mUserInfo.isManagedProfile();
    }

    public boolean isRestricted() {
        return mUserInfo.isRestricted();
    }

    public boolean isAdmin() {
        return mUserInfo.isAdmin();
    }

    public int id;

    static List<UserInfoCompat> convert(List<UserInfo> infos) {
        List<UserInfoCompat> list = new ArrayList<>();
        for (UserInfo info : infos) {
            list.add(new UserInfoCompat(info));
        }
        return list;
    }

    @Override
    public int describeContents() {
        return mUserInfo.describeContents();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        mUserInfo.writeToParcel(dest, flags);
    }
}
