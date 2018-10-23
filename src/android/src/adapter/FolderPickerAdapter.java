package com.danleech.cordova.plugin.imagePicker.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.danleech.cordova.plugin.imagePicker.FakeR;
import com.danleech.cordova.plugin.imagePicker.features.imageloader.ImageLoader;
import com.danleech.cordova.plugin.imagePicker.features.imageloader.ImageType;
import com.danleech.cordova.plugin.imagePicker.listeners.OnFolderClickListener;
import com.danleech.cordova.plugin.imagePicker.model.Folder;

import java.util.ArrayList;
import java.util.List;

public class FolderPickerAdapter extends BaseListAdapter<FolderPickerAdapter.FolderViewHolder> {

    private final OnFolderClickListener folderClickListener;

    private List<Folder> folders = new ArrayList<>();

    public FolderPickerAdapter(Context context, ImageLoader imageLoader, OnFolderClickListener folderClickListener) {
        super(context, imageLoader);
        this.folderClickListener = folderClickListener;
    }

    @Override
    public FolderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FolderViewHolder(
                getInflater().inflate(FakeR.staticGetId("layout", "ef_imagepicker_item_folder"), parent, false));
    }

    @Override
    public void onBindViewHolder(final FolderViewHolder holder, int position) {
        final Folder folder = folders.get(position);

        getImageLoader().loadImage(
                folder.getImages().get(0).getPath(),
                holder.image,
                ImageType.FOLDER
        );

        holder.name.setText(folders.get(position).getFolderName());
        holder.number.setText(String.valueOf(folders.get(position).getImages().size()));

        holder.itemView.setOnClickListener(v -> {
            if (folderClickListener != null)
                folderClickListener.onFolderClick(folder);
        });
    }

    public void setData(List<Folder> folders) {
        if (folders != null) {
            this.folders.clear();
            this.folders.addAll(folders);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {

        private ImageView image;
        private TextView name;
        private TextView number;

        FolderViewHolder(View itemView) {
            super(itemView);

            image = (ImageView) itemView.findViewById(FakeR.staticGetId("id", "image"));
            name = (TextView) itemView.findViewById(FakeR.staticGetId("id", "tv_name"));
            number = (TextView) itemView.findViewById(FakeR.staticGetId("id", "tv_number"));
        }
    }
}
