package com.danleech.cordova.plugin.imagePicker.features.recyclers;

public interface AppBarManager {

    void collapseAppBar();
    void expandAppBar();
    int getVisibleHeightForRecyclerViewInPx();

}