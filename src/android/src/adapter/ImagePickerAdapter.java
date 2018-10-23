package com.danleech.cordova.plugin.imagePicker.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.danleech.cordova.plugin.imagePicker.FakeR;
import com.danleech.cordova.plugin.imagePicker.features.imageloader.ImageLoader;
import com.danleech.cordova.plugin.imagePicker.features.imageloader.ImageType;
import com.danleech.cordova.plugin.imagePicker.helper.ImagePickerUtils;
import com.danleech.cordova.plugin.imagePicker.listeners.OnImageClickListener;
import com.danleech.cordova.plugin.imagePicker.listeners.OnImagePositionSelectedListener;
import com.danleech.cordova.plugin.imagePicker.listeners.OnImageSelectedListener;
import com.danleech.cordova.plugin.imagePicker.model.Image;

import java.util.ArrayList;
import java.util.List;

public class ImagePickerAdapter extends BaseListAdapter<ImagePickerAdapter.ImageViewHolder> {

    private List<Image> images = new ArrayList<>();
    private List<Image> selectedImages = new ArrayList<>();

    private OnImageClickListener itemClickListener;
    private OnImageSelectedListener imageSelectedListener;
    private OnImagePositionSelectedListener imagePositionSelectedListener;

    public ImagePickerAdapter(Context context, ImageLoader imageLoader,
                              List<Image> selectedImages, OnImageClickListener itemClickListener) {
        super(context, imageLoader);
        this.itemClickListener = itemClickListener;

        if (selectedImages != null && !selectedImages.isEmpty()) {
            this.selectedImages.addAll(selectedImages);
        }
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ImageViewHolder(
                getInflater().inflate(FakeR.staticGetId("layout", "ip_item_image"), parent, false)
        );
    }

    @Override
    public void onBindViewHolder(ImageViewHolder viewHolder, int position) {

        final Image image = images.get(position);
        final boolean isSelected = isSelected(image);

        getImageLoader().loadImage(
                image.getPath(),
                viewHolder.imageView,
                ImageType.GALLERY
        );

        boolean showFileTypeIndicator = false;
        boolean showDurationIndicator = false;
        String fileTypeLabel = "";
        String durationLabel = "";
        if(ImagePickerUtils.isGifFormat(image)) {
            fileTypeLabel = getContext().getResources().getString(FakeR.staticGetId("string", "ip_gif"));
            showFileTypeIndicator = true;
        }
        if(image.isVideo()) {
            fileTypeLabel = getContext().getResources().getString(FakeR.staticGetId("string", "ip_video"));
            durationLabel = image.getDuration();
            showDurationIndicator = true;
        }

        viewHolder.fileTypeIndicator.setText(fileTypeLabel);
        viewHolder.fileTypeIndicator.setVisibility(showFileTypeIndicator
                ? View.VISIBLE
                : View.GONE);

        viewHolder.durationIndicator.setText(durationLabel);
        viewHolder.durationIndicator.setVisibility(showDurationIndicator
                ? View.VISIBLE
                : View.GONE);

        viewHolder.alphaView.setAlpha(isSelected
                ? 0.5f
                : 0f);

        viewHolder.itemView.setOnClickListener(v -> {
            // recheck on click
            final boolean isSelectedOnClick = isSelected(image);

            boolean shouldSelect = itemClickListener.onImageClick(
                    isSelectedOnClick
            );

            if (isSelectedOnClick) {
                removeSelectedImage(image, position);
            } else if (shouldSelect) {
                addSelected(image, position);
            }
        });

        viewHolder.container.setForeground(isSelected
                ? ContextCompat.getDrawable(getContext(), FakeR.staticGetId("drawable", "ip_ic_check_white"))
                : null);
    }

    private boolean isSelected(Image image) {
        for (Image selectedImage : selectedImages) {
            if (selectedImage.getPath().equals(image.getPath())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getItemCount() {
        return images.size();
    }


    public void setData(List<Image> images) {
        this.images.clear();
        this.images.addAll(images);
    }

    private void addSelected(final Image image, final int position) {
        mutateSelection(() -> {
            selectedImages.add(image);
            imagePositionSelectedListener.onSelected(position);
            notifyItemChanged(position);
        });
    }

    private void removeSelectedImage(final Image image, final int position) {
        mutateSelection(() -> {
            selectedImages.remove(image);
            notifyItemChanged(position);
        });
    }

    public void removeAllSelectedSingleClick() {
        mutateSelection(() -> {
            selectedImages.clear();
            notifyDataSetChanged();
        });
    }

    private void mutateSelection(Runnable runnable) {
        runnable.run();
        if (imageSelectedListener != null) {
            imageSelectedListener.onSelectionUpdate(selectedImages);
        }
    }

    public void setImageSelectedListener(OnImageSelectedListener imageSelectedListener) {
        this.imageSelectedListener = imageSelectedListener;
    }

    public void setImagePositionSelectedListener(OnImagePositionSelectedListener imagePositionSelectedListener) {
        this.imagePositionSelectedListener = imagePositionSelectedListener;
    }

    public Image getItem(int position) {
        return images.get(position);
    }

    public List<Image> getSelectedImages() {
        return selectedImages;
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;
        private View alphaView;
        private TextView fileTypeIndicator;
        private TextView durationIndicator;
        private FrameLayout container;

        ImageViewHolder(View itemView) {
            super(itemView);

            container = (FrameLayout) itemView;
            imageView = itemView.findViewById(FakeR.staticGetId("id", "image_view"));
            alphaView = itemView.findViewById(FakeR.staticGetId("id", "view_alpha"));
            fileTypeIndicator = itemView.findViewById(FakeR.staticGetId("id", "ip_item_file_type_indicator"));
            durationIndicator = itemView.findViewById(FakeR.staticGetId("id", "ip_item_duration_indicator"));
        }
    }

    public Image getPreviewImage() {
        if(!selectedImages.isEmpty())
            return selectedImages.get(selectedImages.size() - 1);
        if(!images.isEmpty())
            return images.get(0);

        return null;
    }

}
