/*
 * Copyright (c) 2012, David Erosa
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following  conditions are met:
 *
 *   Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *   Redistributions in binary form must reproduce the above copyright notice,
 *      this list of conditions and the following  disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,  BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT  SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR  BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDIN G NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH  DAMAGE
 *
 * Code modified by Andrew Stephan for Sync OnSet
 *
 */

package com.danleech.cordova.plugin.imagePicker.features;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.danleech.cordova.plugin.imagePicker.FakeR;
import com.danleech.cordova.plugin.imagePicker.features.camera.CameraHelper;
import com.danleech.cordova.plugin.imagePicker.features.camera.CameraModule;
import com.danleech.cordova.plugin.imagePicker.features.imageloader.ImageType;
import com.danleech.cordova.plugin.imagePicker.features.recyclers.OnBackAction;
import com.danleech.cordova.plugin.imagePicker.features.recyclers.RecyclerViewManager;
import com.danleech.cordova.plugin.imagePicker.helper.ConfigUtils;
import com.danleech.cordova.plugin.imagePicker.helper.ImagePickerPreferences;
import com.danleech.cordova.plugin.imagePicker.helper.IpLogger;
import com.danleech.cordova.plugin.imagePicker.helper.LocaleManager;
import com.danleech.cordova.plugin.imagePicker.model.Folder;
import com.danleech.cordova.plugin.imagePicker.model.Image;
import com.danleech.cordova.plugin.imagePicker.view.AppBarPreviewLayout;
import com.danleech.cordova.plugin.imagePicker.view.SnackBarView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import static com.danleech.cordova.plugin.imagePicker.helper.ImagePickerPreferences.PREF_WRITE_EXTERNAL_STORAGE_REQUESTED;

public class ImagePickerActivity extends AppCompatActivity implements ImagePickerView {
    private static final String TAG = "ImagePicker";

    private static final String STATE_KEY_CAMERA_MODULE = "Key.CameraModule";

    private static final int RC_CAPTURE_IMAGE = 2000;
    private static final int RC_CAPTURE_VIDEO = 2001;

    private static final int RC_PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 23;
    private static final int RC_PERMISSION_REQUEST_CAMERA = 24;

    private IpLogger logger = IpLogger.getInstance();

    private ActionBar actionBar;
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private RecyclerView recyclerView;
    private SnackBarView snackBarView;
    private AppCompatButton backBtn;
    private AppCompatButton doneBtn;
    private AppCompatButton cameraBtn;
    private Spinner folderSpinner;
    private ImageView imagePreviewView;
    private VideoView videoPreviewView;
    private AppBarPreviewLayout appBarPreviewLayout;

    private RecyclerViewManager recyclerViewManager;

    private ImagePickerPresenter presenter;
    private ImagePickerPreferences preferences;
    private ImagePickerConfig config;

    private Handler handler;
    private ContentObserver observer;

    private boolean isCameraOnly;

    private FakeR fakeR;

    private ProgressDialog progress;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.updateResources(newBase));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

        /* This should not happen */
        Intent intent = getIntent();
        if (intent == null || intent.getExtras() == null) {
            IpLogger.getInstance().e("This should not happen. Please open an issue!");
            finish();
            return;
        }

        setupComponents();

        ImagePickerConfig config = getImagePickerConfig();
        setTheme(config.getTheme());
        fakeR = new FakeR(this);
        setContentView(fakeR.getId("layout", "ip_activity_image_picker"));
        setupView(config);
        setupRecyclerView(config);
        setupFolderView();
    }

    private ImagePickerConfig getImagePickerConfig() {
        if (config == null) {
            Intent intent = getIntent();
            Bundle bundle = intent.getExtras();
            if (bundle == null) {
                throw new IllegalStateException("This should not happen. Please open an issue!");
            }
            config = bundle.getParcelable(ImagePickerConfig.class.getSimpleName());
        }
        return config;
    }

    private void setupView(ImagePickerConfig config) {
        backBtn = findViewById(FakeR.staticGetId("id", "ip_main_btn_back"));
        doneBtn = findViewById(FakeR.staticGetId("id", "ip_main_btn_done"));
        cameraBtn = findViewById(FakeR.staticGetId("id", "ip_main_btn_camera"));
//        progressBar = findViewById(fakeR.getId("ip_activity_image_picker", "progress_bar"));
//        emptyTextView = findViewById(FakeR.staticGetId("id", "tv_empty_images"));
//        coordinatorLayout = findViewById(FakeR.staticGetId("id", "ip_main_coordinator_layout"));
        recyclerView = findViewById(FakeR.staticGetId("id", "ip_main_rv_gallery"));
        snackBarView = findViewById(FakeR.staticGetId("id", "ip_main_snackbar"));
        imagePreviewView = findViewById(FakeR.staticGetId("id", "ip_main_preview_image"));
        videoPreviewView = findViewById(FakeR.staticGetId("id", "ip_main_preview_video"));
        appBarPreviewLayout = findViewById(FakeR.staticGetId("id", "ip_main_preview_appbar"));

        final float scale = getApplicationContext().getResources().getDisplayMetrics().density;
//        coordinatorLayout.setTopViewParam((int) (scale * 400 + 0.5f), (int) (scale * 60 + 0.5f));
//        recyclerView.getLayoutParams().height = ViewUtils.getScreenHeight() - topBarHeight;
//        parentLayout.getLayoutParams().height = topViewHeight + ViewUtils.getScreenHeight() - topBarHeight;
//        recyclerView.setCoordinatorListener(coordinatorLayout);


        Toolbar toolbar = findViewById(fakeR.getId("id", "ip_main_toolbar"));
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        if (config.getSelectedImages().isEmpty())
            doneBtn.setEnabled(false);

        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDone();
            }
        });

        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImageWithPermission();
            }
        });
    }

    private void setupRecyclerView(ImagePickerConfig config) {
        recyclerViewManager = new RecyclerViewManager(
                recyclerView,
                config,
                getResources().getConfiguration().orientation
        );

        recyclerViewManager.setupAdapters((isSelected) -> recyclerViewManager.selectImage(isSelected));
        recyclerViewManager.setAppBarManager(appBarPreviewLayout);

        recyclerViewManager.setImageSelectedListener(selectedImage -> {
//            invalidateTitle();
            updateImagePreview();

            if(!selectedImage.isEmpty()) {
                appBarPreviewLayout.expandAppBar();
            }

            if (ConfigUtils.shouldReturn(config, false) && !selectedImage.isEmpty()) {
                onDone();
            } else {
                doneBtn.setEnabled(recyclerViewManager.isShowDoneButton());
            }
        });

    }

    private void setupFolderView() {
        folderSpinner = findViewById(FakeR.staticGetId("id", "ip_main_folder_spinner"));

        folderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Folder folder = (Folder) folderSpinner.getAdapter().getItem(position);
                setImageAdapter(folder.getImages());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void setupComponents() {
        preferences = new ImagePickerPreferences(this);
        presenter = new ImagePickerPresenter(new ImageFileLoader(this));
        presenter.attachView(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getDataWithPermission();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (config.isOnlyVideo())
            outState.putSerializable(STATE_KEY_CAMERA_MODULE, (Serializable) presenter.getCameraModule(true));
        else
            outState.putSerializable(STATE_KEY_CAMERA_MODULE, (Serializable) presenter.getCameraModule(false));
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        presenter.setCameraModule((CameraModule) savedInstanceState.getSerializable(STATE_KEY_CAMERA_MODULE));
    }

    /**
     * Set image adapter
     * 1. Set new data
     * 2. Update item decoration
     * 3. Update image preview
     */
    private void setImageAdapter(List<Image> images) {
        recyclerViewManager.setImageAdapter(images);
        //invalidateTitle();
    }

    private void setFolderAdapter(List<Folder> folders) {
        ArrayAdapter<Folder> dataAdapter = new ArrayAdapter<Folder>(this,
                FakeR.staticGetId("layout", "ip_folder_spinner_item"), folders);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        folderSpinner.setAdapter(dataAdapter);
    }

    private void updateImagePreview() {
        Image image = recyclerViewManager.getPreviewImage();

        if(image != null)
            if(image.isVideo()) {
                imagePreviewView.setVisibility(View.GONE);
                videoPreviewView.setVisibility(View.GONE);
                videoPreviewView.setVideoPath(image.getPath());
                videoPreviewView.start();
                videoPreviewView.setVisibility(View.VISIBLE);
            } else {
                videoPreviewView.setVisibility(View.GONE);
                videoPreviewView.stopPlayback();
                imagePreviewView.setVisibility(View.VISIBLE);
                config.getImageLoader().loadImage(
                        image.getPath(),
                        imagePreviewView,
                        ImageType.GALLERY
                );
            }
    }

//    private void invalidateTitle() {
//        supportInvalidateOptionsMenu();
//        actionBar.setTitle(recyclerViewManager.getTitle());
//    }

    /**
     * On finish selected image
     * Get all selected images then return image to caller activity
     */
    private void onDone() {
        presenter.onDoneSelectImages(recyclerViewManager.getSelectedImages());
    }

    /**
     * Config recyclerView when configuration changed
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (recyclerViewManager != null) {
            // recyclerViewManager can be null here if we use cameraOnly mode
            recyclerViewManager.changeOrientation(newConfig.orientation);
        }
    }

    /**
     * Check permission
     */
    private void getDataWithPermission() {
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            getData();
        } else {
            requestWriteExternalPermission();
        }
    }

    private void getData() {
        ImagePickerConfig config = getImagePickerConfig();
        presenter.abortLoad();
        presenter.loadImages(config.isOnlyVideo(), config.getExcludedImages());
    }

    /**
     * Request for permission
     * If permission denied or app is first launched, request for permission
     * If permission denied and user choose 'Never Ask Again', show snackbar with an action that navigate to app settings
     */
    private void requestWriteExternalPermission() {
        logger.w("Write External permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, permissions, RC_PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            final String permission = PREF_WRITE_EXTERNAL_STORAGE_REQUESTED;
            if (!preferences.isPermissionRequested(permission)) {
                preferences.setPermissionRequested(permission);
                ActivityCompat.requestPermissions(this, permissions, RC_PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
            } else {
                snackBarView.show(fakeR.getId("string", "ip_msg_no_write_external_permission"), v -> openAppSettings());
            }
        }
    }

    private void requestCameraPermissions() {
        logger.w("Write External permission is not granted. Requesting permission");

        ArrayList<String> permissions = new ArrayList<>(2);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (checkForRationale(permissions)) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[permissions.size()]), RC_PERMISSION_REQUEST_CAMERA);
        } else {
            final String permission = ImagePickerPreferences.PREF_CAMERA_REQUESTED;
            if (!preferences.isPermissionRequested(permission)) {
                preferences.setPermissionRequested(permission);
                ActivityCompat.requestPermissions(this, permissions.toArray(new String[permissions.size()]), RC_PERMISSION_REQUEST_CAMERA);
            } else {
                if (isCameraOnly) {
                    Toast.makeText(getApplicationContext(),
                            getString(fakeR.getId("string", "ip_msg_no_camera_permission")), Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    snackBarView.show(fakeR.getId("string", "ip_msg_no_camera_permission"), v -> openAppSettings());
                }
            }
        }
    }

    private boolean checkForRationale(List<String> permissions) {
        for (int i = 0, size = permissions.size(); i < size; i++) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions.get(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handle permission results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RC_PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    logger.d("Write External permission granted");
                    getData();
                    return;
                }
                logger.e("Permission not granted: results len = " + grantResults.length +
                        " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));
                finish();
            }
            break;
            case RC_PERMISSION_REQUEST_CAMERA: {
                if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    logger.d("Camera permission granted");
                    captureImage();
                    return;
                }
                logger.e("Permission not granted: results len = " + grantResults.length +
                        " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));
                finish();
                break;
            }
            default: {
                logger.d("Got unexpected permission result: " + requestCode);
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
            }
        }
    }

    /**
     * Open app settings screen to change permissions manualy
     */
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Check if the captured image/video is stored successfully
     * Then reload data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_CAPTURE_IMAGE:
                if (resultCode == RESULT_OK) {
                    presenter.finishCaptureImage(this, data, getImagePickerConfig());
                } else if (resultCode == RESULT_CANCELED && isCameraOnly) {
                    presenter.abortCaptureImage();
                    finish();
                }
                break;
            case RC_CAPTURE_VIDEO:
                if (resultCode == RESULT_OK) {
                    presenter.finishCaptureVideo(this, data, getImagePickerConfig());
                } else if (resultCode == RESULT_CANCELED && isCameraOnly) {
                    presenter.abortCaptureVideo();
                    finish();
                }
                break;
        }
    }

    /**
     * Request for camera permission
     */
    private void captureImageWithPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                captureImage();
            } else {
                logger.w("Camera permission is not granted. Requesting permission");
                requestCameraPermissions();
            }
        } else {
            captureImage();
        }
    }

    /**
     * Start camera intent
     * Create a temporary file and pass file Uri to camera intent
     */
    private void captureImage() {
        if (!CameraHelper.checkCameraAvailability(this)) {
            return;
        }
        if(config.isOnlyVideo())
            presenter.captureVideo(this, getImagePickerConfig(), RC_CAPTURE_VIDEO);
        else
            presenter.captureImage(this, getImagePickerConfig(), RC_CAPTURE_IMAGE);
    }


    @Override
    protected void onStart() {
        super.onStart();

        if (isCameraOnly) {
            return;
        }

        if (handler == null) {
            handler = new Handler();
        }
        observer = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                getData();
            }
        };
        getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, observer);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (presenter != null) {
            presenter.abortLoad();
            presenter.detachView();
        }

        if (observer != null) {
            getContentResolver().unregisterContentObserver(observer);
            observer = null;
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (isCameraOnly) {
            super.onBackPressed();
            return;
        }

        recyclerViewManager.handleBack(new OnBackAction() {
            @Override
            public void onBackToFolder() {
//                invalidateTitle();
            }

            @Override
            public void onFinishImagePicker() {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    /* --------------------------------------------------- */
    /* > View Methods */
    /* --------------------------------------------------- */

    @Override
    public void finishPickImages(List<Image> images) {
        Intent data = new Intent();
        data.putParcelableArrayListExtra(IpCons.EXTRA_SELECTED_IMAGES, (ArrayList<? extends Parcelable>) images);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void showCapturedImage() {
        getDataWithPermission();
    }

    @Override
    public void showFetchCompleted(List<Image> images, List<Folder> folders) {
        setImageAdapter(images);
        setFolderAdapter(folders);
        updateImagePreview();
    }

    @Override
    public void showError(Throwable throwable) {
        String message = "Unknown Error";
        if (throwable != null && throwable instanceof NullPointerException) {
            message = "Images not exist";
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showLoading(boolean isLoading) {
//        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
//        recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
//        emptyTextView.setVisibility(View.GONE);
    }

    @Override
    public void showEmpty() {
//        progressBar.setVisibility(View.GONE);
//        recyclerView.setVisibility(View.GONE);
//        emptyTextView.setVisibility(View.VISIBLE);
    }


    /**
     * =============================================================================================
     * Old code
     * =============================================================================================
     */
    private void setupHeader() {
        // From Roman Nkk's code
        // https://plus.google.com/113735310430199015092/posts/R49wVvcDoEW
        // Inflate a "Done/Discard" custom action bar view
        /*
         * Copyright 2013 The Android Open Source Project
         *
         * Licensed under the Apache License, Version 2.0 (the "License");
         * you may not use this file except in compliance with the License.
         * You may obtain a copy of the License at
         *
         *     http://www.apache.org/licenses/LICENSE-2.0
         *
         * Unless required by applicable law or agreed to in writing, software
         * distributed under the License is distributed on an "AS IS" BASIS,
         * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
         * See the License for the specific language governing permissions and
         * limitations under the License.
         */
        LayoutInflater inflater = (LayoutInflater) getActionBar().getThemedContext().getSystemService(
                LAYOUT_INFLATER_SERVICE);
        final View customActionBarView = inflater.inflate(fakeR.getId("layout", "actionbar_custom_view_done_discard"), null);
        customActionBarView.findViewById(fakeR.getId("id", "actionbar_done")).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // "Done"
//                selectClicked(null);
            }
        });
        customActionBarView.findViewById(fakeR.getId("id", "actionbar_discard")).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Show the custom action bar view and hide the normal Home icon and title.
//        final ActionBar actionBar = getActionBar();
//        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM
//                | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
//        actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private int calculateNextSampleSize(int sampleSize) {
        double logBaseTwo = (int) (Math.log(sampleSize) / Math.log(2));
        return (int) Math.pow(logBaseTwo + 1, 2);
    }

    private float calculateScale(int width, int height) {
        float widthScale = 1.0f;
        float heightScale = 1.0f;
        float scale = 1.0f;
//        if (desiredWidth > 0 || desiredHeight > 0) {
//            if (desiredHeight == 0 && desiredWidth < width) {
//                scale = (float)desiredWidth/width;
//            } else if (desiredWidth == 0 && desiredHeight < height) {
//                scale = (float)desiredHeight/height;
//            } else {
//                if (desiredWidth > 0 && desiredWidth < width) {
//                    widthScale = (float)desiredWidth/width;
//                }
//                if (desiredHeight > 0 && desiredHeight < height) {
//                    heightScale = (float)desiredHeight/height;
//                }
//                if (widthScale < heightScale) {
//                    scale = widthScale;
//                } else {
//                    scale = heightScale;
//                }
//            }
//        }

        return scale;
    }

    private class ResizeImagesTask extends AsyncTask<Set<Entry<String, Integer>>, Void, ArrayList<String>> {
        private Exception asyncTaskError = null;

        @Override
        protected ArrayList<String> doInBackground(Set<Entry<String, Integer>>... fileSets) {
            Set<Entry<String, Integer>> fileNames = fileSets[0];
            ArrayList<String> al = new ArrayList<String>();
            try {
                Iterator<Entry<String, Integer>> i = fileNames.iterator();
                Bitmap bmp;
                while (i.hasNext()) {
                    Entry<String, Integer> imageInfo = i.next();
                    File file = new File(imageInfo.getKey());
                    int rotate = imageInfo.getValue().intValue();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 1;
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                    int width = options.outWidth;
                    int height = options.outHeight;
                    float scale = calculateScale(width, height);
                    if (scale < 1) {
                        int finalWidth = (int) (width * scale);
                        int finalHeight = (int) (height * scale);
                        int inSampleSize = calculateInSampleSize(options, finalWidth, finalHeight);
                        options = new BitmapFactory.Options();
                        options.inSampleSize = inSampleSize;
                        try {
                            bmp = this.tryToGetBitmap(file, options, rotate, true);
                        } catch (OutOfMemoryError e) {
                            options.inSampleSize = calculateNextSampleSize(options.inSampleSize);
                            try {
                                bmp = this.tryToGetBitmap(file, options, rotate, false);
                            } catch (OutOfMemoryError e2) {
                                throw new IOException("Unable to load image into memory.");
                            }
                        }
                    } else {
                        try {
                            bmp = this.tryToGetBitmap(file, null, rotate, false);
                        } catch (OutOfMemoryError e) {
                            options = new BitmapFactory.Options();
                            options.inSampleSize = 2;
                            try {
                                bmp = this.tryToGetBitmap(file, options, rotate, false);
                            } catch (OutOfMemoryError e2) {
                                options = new BitmapFactory.Options();
                                options.inSampleSize = 4;
                                try {
                                    bmp = this.tryToGetBitmap(file, options, rotate, false);
                                } catch (OutOfMemoryError e3) {
                                    throw new IOException("Unable to load image into memory.");
                                }
                            }
                        }
                    }

                    file = this.storeImage(bmp, file.getName());
                    al.add(Uri.fromFile(file).toString());
                }
                return al;
            } catch (IOException e) {
                try {
                    asyncTaskError = e;
                    for (int i = 0; i < al.size(); i++) {
                        URI uri = new URI(al.get(i));
                        File file = new File(uri);
                        file.delete();
                    }
                } catch (Exception exception) {
                    // the finally does what we want to do
                } finally {
                    return new ArrayList<String>();
                }
            }
        }

        @Override
        protected void onPostExecute(ArrayList<String> al) {
            Intent data = new Intent();

            if (asyncTaskError != null) {
                Bundle res = new Bundle();
                res.putString("ERRORMESSAGE", asyncTaskError.getMessage());
                data.putExtras(res);
                setResult(RESULT_CANCELED, data);
            } else if (al.size() > 0) {
                Bundle res = new Bundle();
                res.putStringArrayList("MULTIPLEFILENAMES", al);
//                if (imagecursor != null) {
//                    res.putInt("TOTALFILES", imagecursor.getCount());
//                }
                data.putExtras(res);
                setResult(RESULT_OK, data);
            } else {
                setResult(RESULT_CANCELED, data);
            }

            progress.dismiss();
            finish();
        }

        private Bitmap tryToGetBitmap(File file, BitmapFactory.Options options, int rotate, boolean shouldScale) throws IOException, OutOfMemoryError {
            Bitmap bmp;
            if (options == null) {
                bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
            } else {
                bmp = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            }
            if (bmp == null) {
                throw new IOException("The image file could not be opened.");
            }
            if (options != null && shouldScale) {
                float scale = calculateScale(options.outWidth, options.outHeight);
                bmp = this.getResizedBitmap(bmp, scale);
            }
            if (rotate != 0) {
                Matrix matrix = new Matrix();
                matrix.setRotate(rotate);
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
            }
            return bmp;
        }

        /*
         * The following functions are originally from
         * https://github.com/raananw/PhoneGap-Image-Resizer
         *
         * They have been modified by Andrew Stephan for Sync OnSet
         *
         * The software is open source, MIT Licensed.
         * Copyright (C) 2012, webXells GmbH All Rights Reserved.
         */
        private File storeImage(Bitmap bmp, String fileName) throws IOException {
            int index = fileName.lastIndexOf('.');
            String name = fileName.substring(0, index);
            String ext = fileName.substring(index);
            File file = File.createTempFile("tmp_" + name, ext);
            OutputStream outStream = new FileOutputStream(file);
//            if (ext.compareToIgnoreCase(".png") == 0) {
//                bmp.compress(Bitmap.CompressFormat.PNG, quality, outStream);
//            } else {
//                bmp.compress(Bitmap.CompressFormat.JPEG, quality, outStream);
//            }
            outStream.flush();
            outStream.close();
            return file;
        }

        private Bitmap getResizedBitmap(Bitmap bm, float factor) {
            int width = bm.getWidth();
            int height = bm.getHeight();
            // create a matrix for the manipulation
            Matrix matrix = new Matrix();
            // resize the bit map
            matrix.postScale(factor, factor);
            // recreate the new Bitmap
            Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
            return resizedBitmap;
        }
    }
}
