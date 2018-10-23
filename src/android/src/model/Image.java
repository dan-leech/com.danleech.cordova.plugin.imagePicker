package com.danleech.cordova.plugin.imagePicker.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Image implements Parcelable {

    private long id;
    private String name;
    private String path;
    private String duration;
    private int rawDuration = 0;
    private boolean isVideo;
    private long size;

    public Image(long id, String name, String path, String duration, boolean isVideo) {
        this.id = id;
        this.name = name;
        this.path = path;
        if (isVideo) {
            this.rawDuration = Integer.parseInt(duration);
            Date date = new Date(this.rawDuration);
            DateFormat formatter = new SimpleDateFormat("mm:ss");
            this.duration = formatter.format(date);
        }
        this.isVideo = isVideo;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDuration() {
        return duration;
    }

    public int getRawDuration() {
        return rawDuration;
    }

    public void setSize(long size) { this.size = size; }

    public long getSize() { return size; }

    public boolean isVideo() { return isVideo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Image image = (Image) o;
        return image.getPath().equalsIgnoreCase(getPath());
    }

    /* --------------------------------------------------- */
    /* > Parcelable */
    /* --------------------------------------------------- */

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeLong(this.size);
        dest.writeString(this.name);
        dest.writeString(this.path);
        dest.writeString(this.duration);
        dest.writeInt(this.rawDuration);
        dest.writeInt(this.isVideo ? 1 : 0);
    }

    protected Image(Parcel in) {
        this.id = in.readLong();
        this.size = in.readLong();
        this.name = in.readString();
        this.path = in.readString();
        this.duration = in.readString();
        this.rawDuration = in.readInt();
        this.isVideo = in.readInt() == 1;
    }

    public static final Creator<Image> CREATOR = new Creator<Image>() {
        @Override
        public Image createFromParcel(Parcel source) {
            return new Image(source);
        }

        @Override
        public Image[] newArray(int size) {
            return new Image[size];
        }
    };
}
