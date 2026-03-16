package android.support.v4.media.session;

import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.support.v4.media.MediaDescriptionCompat;

public class MediaSessionCompat {

    public static final class Token implements Parcelable {
        public static final Parcelable.Creator<Token> CREATOR = new Parcelable.Creator<Token>() {
            @Override
            public Token createFromParcel(Parcel in) {
                Object strongBinder;
                if (Build.VERSION.SDK_INT >= 21) {
                    strongBinder = in.readParcelable(null);
                } else {
                    strongBinder = in.readStrongBinder();
                }
                return new Token(strongBinder);
            }

            @Override
            public Token[] newArray(int size) {
                return new Token[size];
            }
        };
        private final Object mInner;

        Token(Object inner) {
            this.mInner = inner;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            if (Build.VERSION.SDK_INT >= 21) {
                dest.writeParcelable((Parcelable) this.mInner, flags);
            } else {
                dest.writeStrongBinder((IBinder) this.mInner);
            }
        }
    }

    public static final class QueueItem implements Parcelable {
        public static final Parcelable.Creator<QueueItem> CREATOR = new Parcelable.Creator<QueueItem>() {
            @Override
            public QueueItem createFromParcel(Parcel p) {
                return new QueueItem(p);
            }

            @Override
            public QueueItem[] newArray(int size) {
                return new QueueItem[size];
            }
        };
        private final MediaDescriptionCompat mDescription;
        private final long mId;

        private QueueItem(Parcel in) {
            this.mDescription = MediaDescriptionCompat.CREATOR.createFromParcel(in);
            this.mId = in.readLong();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            this.mDescription.writeToParcel(dest, flags);
            dest.writeLong(this.mId);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "MediaSession.QueueItem {Description=" + this.mDescription + ", Id=" + this.mId + " }";
        }
    }

    static final class ResultReceiverWrapper implements Parcelable {
        public static final Parcelable.Creator<ResultReceiverWrapper> CREATOR = new Parcelable.Creator<ResultReceiverWrapper>() {
            @Override
            public ResultReceiverWrapper createFromParcel(Parcel p) {
                return new ResultReceiverWrapper(p);
            }

            @Override
            public ResultReceiverWrapper[] newArray(int size) {
                return new ResultReceiverWrapper[size];
            }
        };
        private ResultReceiver mResultReceiver;

        ResultReceiverWrapper(Parcel in) {
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
}
