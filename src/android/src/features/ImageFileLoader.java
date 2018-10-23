package com.danleech.cordova.plugin.imagePicker.features;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.danleech.cordova.plugin.imagePicker.features.common.ImageLoaderListener;
import com.danleech.cordova.plugin.imagePicker.helper.ImagePickerUtils;
import com.danleech.cordova.plugin.imagePicker.model.Folder;
import com.danleech.cordova.plugin.imagePicker.model.Image;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageFileLoader {

    private Context context;
    private ExecutorService executorService;

    public ImageFileLoader(Context context) {
        this.context = context;
    }

    private final String[] imageProjection = new String[]{
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.SIZE
    };

    private final String[] videoProjection = new String[]{
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Video.VideoColumns.DURATION,
    };

    public void loadDeviceImages(final boolean onlyVideo, final ArrayList<File> excludedImages, final ImageLoaderListener listener) {
        getExecutorService().execute(new ImageLoadRunnable(onlyVideo, excludedImages, listener));
    }

    public void abortLoadImages() {
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
    }

    private ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
        }
        return executorService;
    }

    private class ImageLoadRunnable implements Runnable {

        private boolean onlyVideo;
        private ArrayList<File> exlucedImages;
        private ImageLoaderListener listener;

        public ImageLoadRunnable(boolean onlyVideo, ArrayList<File> excludedImages, ImageLoaderListener listener) {
            this.onlyVideo = onlyVideo;
            this.exlucedImages = excludedImages;
            this.listener = listener;
        }

        @Override
        public void run() {
            Cursor cursor;
            if (onlyVideo) {
                String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                        + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

                cursor = context.getContentResolver().query(MediaStore.Files.getContentUri("external"), videoProjection,
                        selection, null, MediaStore.Images.Media.DATE_ADDED);
            } else {
                cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageProjection,
                        null, null, MediaStore.Images.Media.DATE_ADDED);
            }

            if (cursor == null) {
                listener.onFailed(new NullPointerException());
                return;
            }

            List<Image> temp = new ArrayList<>();
            Map<String, Folder> folderMap = new HashMap<>();

            if (cursor.moveToLast()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndex(imageProjection[0]));
                    String name = cursor.getString(cursor.getColumnIndex(imageProjection[1]));
                    String path = cursor.getString(cursor.getColumnIndex(imageProjection[2]));
                    String bucket = cursor.getString(cursor.getColumnIndex(imageProjection[3]));
                    long size = cursor.getLong(cursor.getColumnIndex(imageProjection[4]));

                    File file = makeSafeFile(path);
                    if (file != null && file.exists()) {
                        if (exlucedImages != null && exlucedImages.contains(file))
                            continue;

                        Image image;
                        if(ImagePickerUtils.isVideoFormat(path)) {
                            String duration = cursor.getString(cursor.getColumnIndex(videoProjection[5]));
                            image = new Image(id, name, path, duration, true);
                        } else
                            image = new Image(id, name, path, "", false);


                        image.setSize(size);
                        temp.add(image);

                        if (folderMap != null) {
                            Folder folder = folderMap.get(bucket);
                            if (folder == null) {
                                folder = new Folder(bucket);
                                folderMap.put(bucket, folder);
                            }
                            folder.getImages().add(image);
                        }
                    }

                } while (cursor.moveToPrevious());
            }
            cursor.close();

            /* Convert HashMap to ArrayList if not null */
            List<Folder> folders = null;
            if (folderMap != null) {
                folders = new ArrayList<>();

                Folder defaultFolder = new Folder("Gallery");
                defaultFolder.setImages((ArrayList<Image>) temp);
                folders.add(defaultFolder);

                folders.addAll(folderMap.values());
            }

            listener.onImageLoaded(temp, folders);
        }
    }

    private static File makeSafeFile(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        try {
            return new File(path);
        } catch (Exception ignored) {
            return null;
        }
    }

}
