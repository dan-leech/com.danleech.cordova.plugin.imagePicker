package com.danleech.cordova.plugin.imagePicker.features.camera;

import com.danleech.cordova.plugin.imagePicker.model.Image;

import java.util.List;

public interface OnImageReadyListener {
    void onImageReady(List<Image> image);
}
