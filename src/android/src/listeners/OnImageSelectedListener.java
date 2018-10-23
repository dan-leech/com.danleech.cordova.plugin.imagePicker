package com.danleech.cordova.plugin.imagePicker.listeners;

import com.danleech.cordova.plugin.imagePicker.model.Image;

import java.util.List;

public interface OnImageSelectedListener {
    void onSelectionUpdate(List<Image> selectedImages);
}
