package com.danleech.cordova.plugin.imagePicker.model;

import java.util.ArrayList;

public class Folder {

    private String folderName;
    private ArrayList<Image> images;

    public Folder(String bucket) {
        folderName = bucket;
        images = new ArrayList<>();
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public ArrayList<Image> getImages() {
        return images;
    }

    public void setImages(ArrayList<Image> images) {
        this.images = images;
    }

    public String toString() {
        return folderName;
    }
}
