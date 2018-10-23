package com.danleech.cordova.plugin.imagePicker.features;

import com.danleech.cordova.plugin.imagePicker.features.common.MvpView;
import com.danleech.cordova.plugin.imagePicker.model.Folder;
import com.danleech.cordova.plugin.imagePicker.model.Image;

import java.util.List;

public interface ImagePickerView extends MvpView {
    void showLoading(boolean isLoading);
    void showFetchCompleted(List<Image> images, List<Folder> folders);
    void showError(Throwable throwable);
    void showEmpty();
    void showCapturedImage();
    void finishPickImages(List<Image> images);
}
