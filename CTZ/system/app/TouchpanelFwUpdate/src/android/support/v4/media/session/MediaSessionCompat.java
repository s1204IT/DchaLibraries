package android.support.v4.media.session;

import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.support.v4.app.BundleCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.SessionToken2;
import android.support.v4.media.session.IMediaSession;
import android.support.v4.media.session.MediaSessionCompatApi21;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class MediaSessionCompat {

    public static final class Token implements Parcelable {
        public static final Parcelable.Creator<Token> CREATOR = new Parcelable.Creator<Token>() { // from class: android.support.v4.media.session.MediaSessionCompat.Token.1
            /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public Token createFromParcel(Parcel in) {
                Object inner;
                if (Build.VERSION.SDK_INT >= 21) {
                    inner = in.readParcelable(null);
                } else {
                    inner = in.readStrongBinder();
                }
                return new Token(inner);
            }

            /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public Token[] newArray(int size) {
                return new Token[size];
            }
        };
        private IMediaSession mExtraBinder;
        private final Object mInner;
        private SessionToken2 mSessionToken2;

        Token(Object inner) {
            this(inner, null, null);
        }

        Token(Object inner, IMediaSession extraBinder, SessionToken2 token2) {
            this.mInner = inner;
            this.mExtraBinder = extraBinder;
            this.mSessionToken2 = token2;
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            if (Build.VERSION.SDK_INT >= 21) {
                dest.writeParcelable((Parcelable) this.mInner, flags);
            } else {
                dest.writeStrongBinder((IBinder) this.mInner);
            }
        }

        public int hashCode() {
            if (this.mInner == null) {
                return 0;
            }
            return this.mInner.hashCode();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Token)) {
                return false;
            }
            Token other = (Token) obj;
            if (this.mInner == null) {
                return other.mInner == null;
            }
            if (other.mInner == null) {
                return false;
            }
            return this.mInner.equals(other.mInner);
        }

        public IMediaSession getExtraBinder() {
            return this.mExtraBinder;
        }

        public void setExtraBinder(IMediaSession extraBinder) {
            this.mExtraBinder = extraBinder;
        }

        public void setSessionToken2(SessionToken2 token2) {
            this.mSessionToken2 = token2;
        }

        public static Token fromBundle(Bundle tokenBundle) {
            if (tokenBundle == null) {
                return null;
            }
            IMediaSession extraSession = IMediaSession.Stub.asInterface(BundleCompat.getBinder(tokenBundle, "android.support.v4.media.session.EXTRA_BINDER"));
            SessionToken2 token2 = SessionToken2.fromBundle(tokenBundle.getBundle("android.support.v4.media.session.SESSION_TOKEN2"));
            Token token = (Token) tokenBundle.getParcelable("android.support.v4.media.session.TOKEN");
            if (token == null) {
                return null;
            }
            return new Token(token.mInner, extraSession, token2);
        }
    }

    public static final class QueueItem implements Parcelable {
        public static final Parcelable.Creator<QueueItem> CREATOR = new Parcelable.Creator<QueueItem>() { // from class: android.support.v4.media.session.MediaSessionCompat.QueueItem.1
            /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public QueueItem createFromParcel(Parcel p) {
                return new QueueItem(p);
            }

            /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public QueueItem[] newArray(int size) {
                return new QueueItem[size];
            }
        };
        private final MediaDescriptionCompat mDescription;
        private final long mId;
        private Object mItem;

        private QueueItem(Object queueItem, MediaDescriptionCompat description, long id) {
            if (description == null) {
                throw new IllegalArgumentException("Description cannot be null.");
            }
            if (id == -1) {
                throw new IllegalArgumentException("Id cannot be QueueItem.UNKNOWN_ID");
            }
            this.mDescription = description;
            this.mId = id;
            this.mItem = queueItem;
        }

        QueueItem(Parcel in) {
            this.mDescription = MediaDescriptionCompat.CREATOR.createFromParcel(in);
            this.mId = in.readLong();
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            this.mDescription.writeToParcel(dest, flags);
            dest.writeLong(this.mId);
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        public static QueueItem fromQueueItem(Object queueItem) {
            if (queueItem == null || Build.VERSION.SDK_INT < 21) {
                return null;
            }
            Object descriptionObj = MediaSessionCompatApi21.QueueItem.getDescription(queueItem);
            MediaDescriptionCompat description = MediaDescriptionCompat.fromMediaDescription(descriptionObj);
            long id = MediaSessionCompatApi21.QueueItem.getQueueId(queueItem);
            return new QueueItem(queueItem, description, id);
        }

        public static List<QueueItem> fromQueueItemList(List<?> itemList) {
            if (itemList == null || Build.VERSION.SDK_INT < 21) {
                return null;
            }
            List<QueueItem> items = new ArrayList<>();
            for (Object itemObj : itemList) {
                items.add(fromQueueItem(itemObj));
            }
            return items;
        }

        public String toString() {
            return "MediaSession.QueueItem {Description=" + this.mDescription + ", Id=" + this.mId + " }";
        }
    }

    public static final class ResultReceiverWrapper implements Parcelable {
        public static final Parcelable.Creator<ResultReceiverWrapper> CREATOR = new Parcelable.Creator<ResultReceiverWrapper>() { // from class: android.support.v4.media.session.MediaSessionCompat.ResultReceiverWrapper.1
            /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public ResultReceiverWrapper createFromParcel(Parcel p) {
                return new ResultReceiverWrapper(p);
            }

            /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public ResultReceiverWrapper[] newArray(int size) {
                return new ResultReceiverWrapper[size];
            }
        };
        private ResultReceiver mResultReceiver;

        ResultReceiverWrapper(Parcel in) {
            this.mResultReceiver = (ResultReceiver) ResultReceiver.CREATOR.createFromParcel(in);
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            this.mResultReceiver.writeToParcel(dest, flags);
        }
    }
}
