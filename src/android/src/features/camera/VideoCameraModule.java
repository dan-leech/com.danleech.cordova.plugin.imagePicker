package com.danleech.cordova.plugin.imagePicker.features.camera;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;

import com.danleech.cordova.plugin.imagePicker.features.ImagePickerConfigFactory;
import com.danleech.cordova.plugin.imagePicker.features.common.BaseConfig;
import com.danleech.cordova.plugin.imagePicker.helper.ImagePickerUtils;
import com.danleech.cordova.plugin.imagePicker.helper.IpLogger;
import com.danleech.cordova.plugin.imagePicker.model.Image;
import com.danleech.cordova.plugin.imagePicker.model.ImageFactory;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VideoCameraModule implements CameraModule, Serializable {

    private String currentVideoPath;

    public Intent getCameraIntent(Context context) {
        return getCameraIntent(context, ImagePickerConfigFactory.createDefault());
    }

    @Override
    public Intent getCameraIntent(Context context, BaseConfig config) {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        File videoFile = ImagePickerUtils.createVideoFile(config.getImageDirectory());
        if (videoFile != null) {
            Context appContext = context.getApplicationContext();
            String providerName = String.format(Locale.ENGLISH, "%s%s", appContext.getPackageName(), ".imagepicker.provider");
            Uri uri = FileProvider.getUriForFile(appContext, providerName, videoFile);
            currentVideoPath = "file:" + videoFile.getAbsolutePath();
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);

            ImagePickerUtils.grantAppPermission(context, intent, uri);

            return intent;
        }
        return intent;
    }

    @Override
    public void getData(final Context context, Intent intent, final OnImageReadyListener imageReadyListener) {
        if (imageReadyListener == null) {
            throw new IllegalStateException("OnImageReadyListener must not be null");
        }

        if (currentVideoPath == null) {
            IpLogger.getInstance().w("currentVideoPath null. " +
                    "This happen if you haven't call #getCameraIntent() or the activity is being recreated");
            imageReadyListener.onImageReady(null);
            return;
        }

        final Uri videoUri = Uri.parse(currentVideoPath);
        if (videoUri != null) {
            MediaScannerConnection.scanFile(context.getApplicationContext(),
                    new String[]{videoUri.getPath()}, null, (path, uri) -> {

                        IpLogger.getInstance().d("File " + path + " was scanned successfully: " + uri);

                        if (path == null) {
                            IpLogger.getInstance().d("This should not happen, go back to Immediate implemenation");
                            path = currentVideoPath;
                        }

                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        //use one of overloaded setDataSource() functions to set your data source
                        retriever.setDataSource(context, videoUri);
                        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

                        File file = new File(path);
                        long size = file.length();

                        retriever.release();

                        List<Image> images = new ArrayList<>();
                        Image video = new Image(0, ImagePickerUtils.getNameFromFilePath(path), path, duration, true);
                        video.setSize(size);
                        images.add(video);

                        imageReadyListener.onImageReady(images);
                        ImagePickerUtils.revokeAppPermission(context, videoUri);
                    });
        }
    }

    @Override
    public void removeData() {
        if (currentVideoPath != null) {
            File file = new File(currentVideoPath);
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
