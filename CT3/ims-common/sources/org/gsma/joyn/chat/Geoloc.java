package org.gsma.joyn.chat;

import android.os.Parcel;
import android.os.Parcelable;
import java.io.Serializable;
import org.gsma.joyn.Logger;

public class Geoloc implements Parcelable, Serializable {
    public static final Parcelable.Creator<Geoloc> CREATOR = new Parcelable.Creator<Geoloc>() {
        @Override
        public Geoloc createFromParcel(Parcel source) {
            return new Geoloc(source);
        }

        @Override
        public Geoloc[] newArray(int size) {
            return new Geoloc[size];
        }
    };
    public static final String TAG = "TAPI-Geoloc";
    private static final long serialVersionUID = 0;
    private float accuracy;
    private long expiration;
    private String label;
    private double latitude;
    private double longitude;

    public Geoloc(String label, double latitude, double longitude, long expiration) {
        this.expiration = 0L;
        this.accuracy = 0.0f;
        Logger.i(TAG, "Geoloc entrylabel=" + label + " latitude=" + latitude + " longitude=" + longitude + " expiration=" + expiration);
        this.label = label;
        this.latitude = latitude;
        this.longitude = longitude;
        this.expiration = expiration;
    }

    public Geoloc(String label, double latitude, double longitude, long expiration, float accuracy) {
        this(label, latitude, longitude, expiration);
        Logger.i(TAG, "accuracy=" + accuracy);
        this.accuracy = accuracy;
    }

    public Geoloc(Parcel source) {
        this.expiration = 0L;
        this.accuracy = 0.0f;
        this.label = source.readString();
        this.latitude = source.readDouble();
        this.longitude = source.readDouble();
        this.expiration = source.readLong();
        this.accuracy = source.readFloat();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.label);
        dest.writeDouble(this.latitude);
        dest.writeDouble(this.longitude);
        dest.writeLong(this.expiration);
        dest.writeFloat(this.accuracy);
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getExpiration() {
        return this.expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public float getAccuracy() {
        return this.accuracy;
    }

    public void setAcuracy(float accuracy) {
        this.accuracy = accuracy;
    }
}
