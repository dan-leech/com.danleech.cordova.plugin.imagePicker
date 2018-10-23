package com.danleech.cordova.plugin.imagePicker.helper;

import android.content.Context;

import com.danleech.cordova.plugin.imagePicker.FakeR;
import com.danleech.cordova.plugin.imagePicker.features.ImagePickerConfig;
import com.danleech.cordova.plugin.imagePicker.features.IpCons;
import com.danleech.cordova.plugin.imagePicker.features.ReturnMode;
import com.danleech.cordova.plugin.imagePicker.features.common.BaseConfig;

import java.io.Serializable;

public class ConfigUtils {

    public static ImagePickerConfig checkConfig(ImagePickerConfig config) {
        if (config == null) {
            throw new IllegalStateException("ImagePickerConfig cannot be null");
        }
        if (config.getMode() != IpCons.MODE_SINGLE
                && (config.getReturnMode() == ReturnMode.GALLERY_ONLY
                || config.getReturnMode() == ReturnMode.ALL)) {
            throw new IllegalStateException("ReturnMode.GALLERY_ONLY and ReturnMode.ALL is only applicable in Single Mode!");
        }
        if (config.getImageLoader() != null && !(config.getImageLoader() instanceof Serializable)) {
            throw new IllegalStateException("Custom image loader must be a class that implement ImageLoader." +
                    " This limitation due to Serializeable");
        }
        return config;
    }

    public static boolean shouldReturn(BaseConfig config, boolean isCamera) {
        ReturnMode mode = config.getReturnMode();
        if (isCamera) {
            return mode == ReturnMode.ALL || mode == ReturnMode.CAMERA_ONLY;
        } else {
            return mode == ReturnMode.ALL || mode == ReturnMode.GALLERY_ONLY;
        }
    }

    public static String getFolderTitle(Context context, ImagePickerConfig config) {
        final String folderTitle = config.getFolderTitle();
        return ImagePickerUtils.isStringEmpty(folderTitle)
                ? context.getString(FakeR.staticGetId("string", "ef_title_folder"))
                : folderTitle;
    }

    public static String getImageTitle(Context context, ImagePickerConfig config) {
        final String configImageTitle = config.getImageTitle();
        return ImagePickerUtils.isStringEmpty(configImageTitle)
                ? context.getString(FakeR.staticGetId("string", "ef_title_select_image"))
                : configImageTitle;
    }
}
