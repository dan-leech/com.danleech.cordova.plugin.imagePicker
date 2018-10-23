package com.danleech.cordova.plugin.imagePicker.features.imageloader;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.danleech.cordova.plugin.imagePicker.FakeR;

public class DefaultImageLoader implements ImageLoader {

    @Override
    public void loadImage(String path, ImageView imageView, ImageType imageType) {
        Glide.with(imageView.getContext())
                .load(path)
                .apply(new RequestOptions()
                        .placeholder(imageType == ImageType.FOLDER
                                ? FakeR.staticGetId("drawable", "ip_folder_placeholder")
                                : FakeR.staticGetId("drawable", "ip_image_placeholder"))
                        .error(imageType == ImageType.FOLDER
                                ? FakeR.staticGetId("drawable", "ip_folder_placeholder")
                                : FakeR.staticGetId("drawable", "ip_image_placeholder"))
                )
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView);
    }
}
