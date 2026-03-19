package com.googlecode.mp4parser.authoring;

import java.util.Date;

public class TrackMetaData implements Cloneable {
    private double height;
    private String language;
    int layer;
    private long timescale;
    private float volume;
    private double width;
    private Date modificationTime = new Date();
    private Date creationTime = new Date();
    private long trackId = 1;
    private int group = 0;
    private long[] matrix = {65536, 0, 0, 0, 65536, 0, 0, 0, 1073741824};

    public String getLanguage() {
        return this.language;
    }

    public void setLanguage(String str) {
        this.language = str;
    }

    public long getTimescale() {
        return this.timescale;
    }

    public void setTimescale(long j) {
        this.timescale = j;
    }

    public void setModificationTime(Date date) {
        this.modificationTime = date;
    }

    public Date getCreationTime() {
        return this.creationTime;
    }

    public void setCreationTime(Date date) {
        this.creationTime = date;
    }

    public double getWidth() {
        return this.width;
    }

    public void setWidth(double d) {
        this.width = d;
    }

    public long[] getMatrix() {
        return this.matrix;
    }

    public void setMatrix(long[] jArr) {
        this.matrix = jArr;
    }

    public double getHeight() {
        return this.height;
    }

    public void setHeight(double d) {
        this.height = d;
    }

    public long getTrackId() {
        return this.trackId;
    }

    public void setTrackId(long j) {
        this.trackId = j;
    }

    public int getLayer() {
        return this.layer;
    }

    public void setLayer(int i) {
        this.layer = i;
    }

    public float getVolume() {
        return this.volume;
    }

    public int getGroup() {
        return this.group;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
