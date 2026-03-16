package jp.co.omronsoft.android.decoemojimanager.interfacedata;

import android.os.Parcel;
import android.os.Parcelable;

public class DecoEmojiUriInfo implements Parcelable {
    public static final Parcelable.Creator<DecoEmojiUriInfo> CREATOR = new Parcelable.Creator<DecoEmojiUriInfo>() {
        @Override
        public DecoEmojiUriInfo createFromParcel(Parcel in) {
            return new DecoEmojiUriInfo(in);
        }

        @Override
        public DecoEmojiUriInfo[] newArray(int size) {
            return new DecoEmojiUriInfo[size];
        }
    };
    private String mUri;

    public DecoEmojiUriInfo() {
    }

    public DecoEmojiUriInfo(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void readFromParcel(Parcel in) {
        this.mUri = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mUri);
    }

    public String getUri() {
        return this.mUri;
    }

    public void setUri(String uri) {
        this.mUri = uri;
    }
}
