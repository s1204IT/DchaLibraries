package android.support.v4.media.session;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;

final class MediaSessionCompat$ResultReceiverWrapper implements Parcelable {
    public static final Parcelable.Creator<MediaSessionCompat$ResultReceiverWrapper> CREATOR = new Parcelable.Creator<MediaSessionCompat$ResultReceiverWrapper>() {
        @Override
        public MediaSessionCompat$ResultReceiverWrapper createFromParcel(Parcel p) {
            return new MediaSessionCompat$ResultReceiverWrapper(p);
        }

        @Override
        public MediaSessionCompat$ResultReceiverWrapper[] newArray(int size) {
            return new MediaSessionCompat$ResultReceiverWrapper[size];
        }
    };
    private ResultReceiver mResultReceiver;

    MediaSessionCompat$ResultReceiverWrapper(Parcel in) {
        this.mResultReceiver = (ResultReceiver) ResultReceiver.CREATOR.createFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        this.mResultReceiver.writeToParcel(dest, flags);
    }
}
