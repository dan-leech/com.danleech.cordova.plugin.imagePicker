package com.danleech.cordova.plugin.imagePicker.features.recyclers;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Parcelable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.danleech.cordova.plugin.imagePicker.FakeR;
import com.danleech.cordova.plugin.imagePicker.adapter.FolderPickerAdapter;
import com.danleech.cordova.plugin.imagePicker.adapter.ImagePickerAdapter;
import com.danleech.cordova.plugin.imagePicker.features.ImagePickerConfig;
import com.danleech.cordova.plugin.imagePicker.features.ReturnMode;
import com.danleech.cordova.plugin.imagePicker.features.imageloader.ImageLoader;
import com.danleech.cordova.plugin.imagePicker.helper.ConfigUtils;
import com.danleech.cordova.plugin.imagePicker.helper.ImagePickerUtils;
import com.danleech.cordova.plugin.imagePicker.listeners.OnFolderClickListener;
import com.danleech.cordova.plugin.imagePicker.listeners.OnImageClickListener;
import com.danleech.cordova.plugin.imagePicker.listeners.OnImagePositionSelectedListener;
import com.danleech.cordova.plugin.imagePicker.listeners.OnImageSelectedListener;
import com.danleech.cordova.plugin.imagePicker.model.Folder;
import com.danleech.cordova.plugin.imagePicker.model.Image;
import com.danleech.cordova.plugin.imagePicker.view.AppBarPreviewLayout;
import com.danleech.cordova.plugin.imagePicker.view.GridSpacingItemDecoration;

import java.util.ArrayList;
import java.util.List;

import static com.danleech.cordova.plugin.imagePicker.features.IpCons.MAX_LIMIT;
import static com.danleech.cordova.plugin.imagePicker.features.IpCons.MODE_MULTIPLE;
import static com.danleech.cordova.plugin.imagePicker.features.IpCons.MODE_SINGLE;

public class RecyclerViewManager {

    private final Context context;
    private final RecyclerView recyclerView;
    private final ImagePickerConfig config;

    private RecyclerLayoutManager layoutManager;
    private GridSpacingItemDecoration itemOffsetDecoration;

    private ImagePickerAdapter imageAdapter;

    private int imageColumns;

    public RecyclerViewManager(RecyclerView recyclerView, ImagePickerConfig config, int orientation) {
        this.recyclerView = recyclerView;
        this.config = config;
        this.context = recyclerView.getContext();
        changeOrientation(orientation);
    }

    /**
     * Set item size, column size base on the screen orientation
     */
    public void changeOrientation(int orientation) {
        imageColumns = orientation == Configuration.ORIENTATION_PORTRAIT ? 3 : 5;

        layoutManager = new RecyclerLayoutManager(context, imageColumns);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        setItemDecoration(imageColumns);
    }

    public void setupAdapters(OnImageClickListener onImageClickListener) {
        ArrayList<Image> selectedImages = null;
        if (config.getMode() == MODE_MULTIPLE && !config.getSelectedImages().isEmpty()) {
            selectedImages = config.getSelectedImages();
        }

        final ImageLoader imageLoader = config.getImageLoader();
        imageAdapter = new ImagePickerAdapter(context, imageLoader, selectedImages, onImageClickListener);

        imageAdapter.setImagePositionSelectedListener(new OnImagePositionSelectedListener() {
            @Override
            public void onSelected(int position) {
                // scroll
                recyclerView.smoothScrollToPosition(position);
            }
        });
    }

    private void setItemDecoration(int columns) {
        if (itemOffsetDecoration != null) {
            recyclerView.removeItemDecoration(itemOffsetDecoration);
        }
        itemOffsetDecoration = new GridSpacingItemDecoration(
                columns,
                context.getResources().getDimensionPixelSize(FakeR.staticGetId("dimen", "ip_item_padding")),
                false
        );
        recyclerView.addItemDecoration(itemOffsetDecoration);

        layoutManager.setSpanCount(columns);
    }

    public void handleBack(OnBackAction action) {
        action.onFinishImagePicker();
    }

    private boolean isDisplayingFolderView() {
        return recyclerView.getAdapter() == null || recyclerView.getAdapter() instanceof FolderPickerAdapter;
    }

    public String getTitle() {
        if (isDisplayingFolderView()) {
            return ConfigUtils.getFolderTitle(context, config);
        }

        if (config.getMode() == MODE_SINGLE) {
            return ConfigUtils.getImageTitle(context, config);
        }

        final int imageSize = imageAdapter.getSelectedImages().size();
        final boolean useDefaultTitle = !ImagePickerUtils.isStringEmpty(config.getImageTitle()) && imageSize == 0;

        if (useDefaultTitle) {
            return ConfigUtils.getImageTitle(context, config);
        }
        return config.getLimit() == MAX_LIMIT
                ? String.format(context.getString(FakeR.staticGetId("string", "ip_selected")), imageSize)
                : String.format(context.getString(FakeR.staticGetId("string", "ip_selected_with_limit")), imageSize, config.getLimit());
    }

    public void setImageAdapter(List<Image> images) {
        imageAdapter.setData(images);
        setItemDecoration(imageColumns);
        recyclerView.setAdapter(imageAdapter);
    }

    /* --------------------------------------------------- */
    /* > Images */
    /* --------------------------------------------------- */

    private void checkAdapterIsInitialized() {
        if (imageAdapter == null) {
            throw new IllegalStateException("Must call setupAdapters first!");
        }
    }

    public List<Image> getSelectedImages() {
        checkAdapterIsInitialized();
        return imageAdapter.getSelectedImages();
    }

    public void setImageSelectedListener(OnImageSelectedListener listener) {
        checkAdapterIsInitialized();
        imageAdapter.setImageSelectedListener(listener);
    }

    public boolean selectImage(boolean isSelected) {
        if (config.getMode() == MODE_MULTIPLE) {
            if (imageAdapter.getSelectedImages().size() >= config.getLimit() && !isSelected) {
                Toast.makeText(context, FakeR.staticGetId("string", "ip_msg_limit_images"), Toast.LENGTH_SHORT).show();
                return false;
            }
        } else if (config.getMode() == MODE_SINGLE) {
            if (imageAdapter.getSelectedImages().size() > 0) {
                imageAdapter.removeAllSelectedSingleClick();
            }
        }
        return true;
    }

    public boolean isShowDoneButton() {
        return !isDisplayingFolderView()
                && !imageAdapter.getSelectedImages().isEmpty()
                && (config.getReturnMode() != ReturnMode.ALL && config.getReturnMode() != ReturnMode.GALLERY_ONLY);
    }

    public Image getPreviewImage() {
        return imageAdapter.getPreviewImage();
    }

    public void setAppBarManager(AppBarPreviewLayout appBarLayout) {
        this.layoutManager.setAppBarManager(appBarLayout);
    }
}
