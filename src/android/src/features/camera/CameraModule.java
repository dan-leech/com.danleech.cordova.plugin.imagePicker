package com.danleech.cordova.plugin.imagePicker.features.camera;

import android.content.Context;
import android.content.Intent;

import com.danleech.cordova.plugin.imagePicker.features.common.BaseConfig;

public interface CameraModule {
    Intent getCameraIntent(Context context, BaseConfig config);
    void getData(Context context, Intent intent, OnImageReadyListener imageReadyListener);
    void removeData();
}
