package org.gsma.joyn.chat;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Date;
import org.gsma.joyn.Logger;

public class GeolocMessage extends ChatMessage implements Parcelable {
    public static final Parcelable.Creator<GeolocMessage> CREATOR = new Parcelable.Creator<GeolocMessage>() {
        @Override
        public GeolocMessage createFromParcel(Parcel source) {
            return new GeolocMessage(source);
        }

        @Override
        public GeolocMessage[] newArray(int size) {
            return new GeolocMessage[size];
        }
    };
    public static final String MIME_TYPE = "application/geoloc";
    public static final String TAG = "TAPI-GeolocMessage";
    private Geoloc geoloc;

    public GeolocMessage(String messageId, String remote, Geoloc geoloc, Date receiptAt, boolean imdnDisplayedRequested) {
        super(messageId, remote, null, receiptAt, imdnDisplayedRequested, null);
        this.geoloc = null;
        Logger.i(TAG, "GeolocMessage entry geoloc =" + geoloc);
        this.geoloc = geoloc;
    }

    public GeolocMessage(Parcel source) {
        super(source);
        this.geoloc = null;
        this.geoloc = new Geoloc(source);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        this.geoloc.writeToParcel(dest, flags);
    }

    public Geoloc getGeoloc() {
        return this.geoloc;
    }
}
