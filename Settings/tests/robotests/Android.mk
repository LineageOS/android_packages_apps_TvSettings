#############################################
# TvSettings Robolectric test target. #
#############################################
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

# Include the testing libraries (JUnit4 + Robolectric libs).
LOCAL_STATIC_JAVA_LIBRARIES := \
    mockito-robolectric-prebuilt \
    platform-robolectric-android-all-stubs \
    truth-prebuilt

LOCAL_JAVA_LIBRARIES := \
    junit \
    platform-robolectric-3.5.1-prebuilt

LOCAL_INSTRUMENTATION_FOR := TvSettings
LOCAL_MODULE := TvSettingsRoboTests

LOCAL_MODULE_TAGS := optional

include $(BUILD_STATIC_JAVA_LIBRARY)

#############################################################
# TvSettings runner target to run the previous target. #
#############################################################
include $(CLEAR_VARS)

LOCAL_MODULE := RunTvSettingsRoboTests

LOCAL_SDK_VERSION := current

LOCAL_STATIC_JAVA_LIBRARIES := \
    TvSettingsRoboTests

LOCAL_TEST_PACKAGE := TvSettings

LOCAL_INSTRUMENT_SOURCE_DIRS := $(dir $(LOCAL_PATH))../src

LOCAL_ROBOTEST_TIMEOUT := 36000

include prebuilts/misc/common/robolectric/3.5.1/run_robotests.mk
