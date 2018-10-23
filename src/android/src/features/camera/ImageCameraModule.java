package com.danleech.cordova.plugin.imagePicker.features.camera;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;

import com.danleech.cordova.plugin.imagePicker.features.ImagePickerConfigFactory;
import com.danleech.cordova.plugin.imagePicker.features.common.BaseConfig;
import com.danleech.cordova.plugin.imagePicker.helper.ImagePickerUtils;
import com.danleech.cordova.plugin.imagePicker.helper.IpLogger;
import com.danleech.cordova.plugin.imagePicker.model.ImageFactory;

import java.io.File;
import java.io.Serializable;
import java.util.Locale;

public class ImageCameraModule implements CameraModule, Serializable {

    private String currentImagePath;

    public Intent getCameraIntent(Context context) {
        return getCameraIntent(context, ImagePickerConfigFactory.createDefault());
    }

    @Override
    public Intent getCameraIntent(Context context, BaseConfig config) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File imageFile = ImagePickerUtils.createImageFile(config.getImageDirectory());
        if (imageFile != null) {
            Context appContext = context.getApplicationContext();
            String providerName = String.format(Locale.ENGLISH, "%s%s", appContext.getPackageName(), ".imagepicker.provider");
            Uri uri = FileProvider.getUriForFile(appContext, providerName, imageFile);
            currentImagePath = "file:" + imageFile.getAbsolutePath();
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);

            ImagePickerUtils.grantAppPermission(context, intent, uri);

            return intent;
        }
        return null;
    }

    @Override
    public void getData(final Context context, Intent intent, final OnImageReadyListener imageReadyListener) {
        if (imageReadyListener == null) {
            throw new IllegalStateException("OnImageReadyListener must not be null");
        }

        if (currentImagePath == null) {
            IpLogger.getInstance().w("currentImagePath null. " +
                    "This happen if you haven't call #getCameraIntent() or the activity is being recreated");
            imageReadyListener.onImageReady(null);
            return;
        }

        final Uri imageUri = Uri.parse(currentImagePath);
        if (imageUri != null) {
            MediaScannerConnection.scanFile(context.getApplicationContext(),
                    new String[]{imageUri.getPath()}, null, (path, uri) -> {

                        IpLogger.getInstance().d("File " + path + " was scanned successfully: " + uri);

                        if (path == null) {
                            IpLogger.getInstance().d("This should not happen, go back to Immediate implemenation");
                            path = currentImagePath;
                        }

                        imageReadyListener.onImageReady(ImageFactory.singleListFromPath(path));
                        ImagePickerUtils.revokeAppPermission(context, imageUri);
                    });
        }
    }

    @Override
    public void removeData() {
        if (currentImagePath != null) {
            File file = new File(currentImagePath);
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
