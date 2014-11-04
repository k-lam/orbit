# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
#OPENCV_INSTALL_MODULES:=on
OPENCV_CAMERA_MODULES:=off
OPENCV_LIB_TYPE:=STATIC
include  D:\currentworkspace\android-ndk-r9d-windows-x86\android-ndk-r9d\sources\OpenCV-2.4.9-android-sdk\OpenCV-2.4.9-android-sdk\sdk\native\jni\OpenCV.mk
LOCAL_LDLIBS := -llog -ljnigraphics -lz -landroid
LOCAL_MODULE    := orbit
LOCAL_SRC_FILES := orbit_jni.c \
decoder.c \
utils.cpp


LOCAL_STATIC_LIBRARIES += libswscale libavformat libavcodec libavutil
				
include $(BUILD_SHARED_LIBRARY)

$(call import-module,ffmpeg/android/arm)
#$(call import-module,kltemp)

