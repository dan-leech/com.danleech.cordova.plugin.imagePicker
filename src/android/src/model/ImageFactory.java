package com.danleech.cordova.plugin.imagePicker.model;

import com.danleech.cordova.plugin.imagePicker.helper.ImagePickerUtils;

import java.util.ArrayList;
import java.util.List;

public class ImageFactory {

    public static List<Image> singleListFromPath(String path) {
        List<Image> images = new ArrayList<>();
        images.add(new Image(0, ImagePickerUtils.getNameFromFilePath(path), path, "0", ImagePickerUtils.isVideoFormat(path)));
        return images;
    }
}
