package android.support.v4.media.session;

import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.media.session.PlaybackStateCompatApi21;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class PlaybackStateCompat implements Parcelable {
    public static final Parcelable.Creator<PlaybackStateCompat> CREATOR = new Parcelable.Creator<PlaybackStateCompat>() {
        @Override
        public PlaybackStateCompat createFromParcel(Parcel parcel) {
            return new PlaybackStateCompat(parcel);
        }

        @Override
        public PlaybackStateCompat[] newArray(int i) {
            return new PlaybackStateCompat[i];
        }
    };
    final long mActions;
    final long mActiveItemId;
    final long mBufferedPosition;
    List<CustomAction> mCustomActions;
    final int mErrorCode;
    final CharSequence mErrorMessage;
    final Bundle mExtras;
    final long mPosition;
    final float mSpeed;
    final int mState;
    private Object mStateObj;
    final long mUpdateTime;

    public static final class CustomAction implements Parcelable {
        public static final Parcelable.Creator<CustomAction> CREATOR = new Parcelable.Creator<CustomAction>() {
            @Override
            public CustomAction createFromParcel(Parcel parcel) {
                return new CustomAction(parcel);
            }

            @Override
            public CustomAction[] newArray(int i) {
                return new CustomAction[i];
            }
        };
        private final String mAction;
        private Object mCustomActionObj;
        private final Bundle mExtras;
        private final int mIcon;
        private final CharSequence mName;

        CustomAction(Parcel parcel) {
            this.mAction = parcel.readString();
            this.mName = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
            this.mIcon = parcel.readInt();
            this.mExtras = parcel.readBundle();
        }

        CustomAction(String str, CharSequence charSequence, int i, Bundle bundle) {
            this.mAction = str;
            this.mName = charSequence;
            this.mIcon = i;
            this.mExtras = bundle;
        }

        public static CustomAction fromCustomAction(Object obj) {
            if (obj == null || Build.VERSION.SDK_INT < 21) {
                return null;
            }
            CustomAction customAction = new CustomAction(PlaybackStateCompatApi21.CustomAction.getAction(obj), PlaybackStateCompatApi21.CustomAction.getName(obj), PlaybackStateCompatApi21.CustomAction.getIcon(obj), PlaybackStateCompatApi21.CustomAction.getExtras(obj));
            customAction.mCustomActionObj = obj;
            return customAction;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public String toString() {
            return "Action:mName='" + ((Object) this.mName) + ", mIcon=" + this.mIcon + ", mExtras=" + this.mExtras;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(this.mAction);
            TextUtils.writeToParcel(this.mName, parcel, i);
            parcel.writeInt(this.mIcon);
            parcel.writeBundle(this.mExtras);
        }
    }

    PlaybackStateCompat(int i, long j, long j2, float f, long j3, int i2, CharSequence charSequence, long j4, List<CustomAction> list, long j5, Bundle bundle) {
        this.mState = i;
        this.mPosition = j;
        this.mBufferedPosition = j2;
        this.mSpeed = f;
        this.mActions = j3;
        this.mErrorCode = i2;
        this.mErrorMessage = charSequence;
        this.mUpdateTime = j4;
        this.mCustomActions = new ArrayList(list);
        this.mActiveItemId = j5;
        this.mExtras = bundle;
    }

    PlaybackStateCompat(Parcel parcel) {
        this.mState = parcel.readInt();
        this.mPosition = parcel.readLong();
        this.mSpeed = parcel.readFloat();
        this.mUpdateTime = parcel.readLong();
        this.mBufferedPosition = parcel.readLong();
        this.mActions = parcel.readLong();
        this.mErrorMessage = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
        this.mCustomActions = parcel.createTypedArrayList(CustomAction.CREATOR);
        this.mActiveItemId = parcel.readLong();
        this.mExtras = parcel.readBundle();
        this.mErrorCode = parcel.readInt();
    }

    public static PlaybackStateCompat fromPlaybackState(Object obj) {
        if (obj == null || Build.VERSION.SDK_INT < 21) {
            return null;
        }
        List<Object> customActions = PlaybackStateCompatApi21.getCustomActions(obj);
        ArrayList arrayList = null;
        if (customActions != null) {
            arrayList = new ArrayList(customActions.size());
            Iterator<Object> it = customActions.iterator();
            while (it.hasNext()) {
                arrayList.add(CustomAction.fromCustomAction(it.next()));
            }
        }
        PlaybackStateCompat playbackStateCompat = new PlaybackStateCompat(PlaybackStateCompatApi21.getState(obj), PlaybackStateCompatApi21.getPosition(obj), PlaybackStateCompatApi21.getBufferedPosition(obj), PlaybackStateCompatApi21.getPlaybackSpeed(obj), PlaybackStateCompatApi21.getActions(obj), 0, PlaybackStateCompatApi21.getErrorMessage(obj), PlaybackStateCompatApi21.getLastPositionUpdateTime(obj), arrayList, PlaybackStateCompatApi21.getActiveQueueItemId(obj), Build.VERSION.SDK_INT >= 22 ? PlaybackStateCompatApi22.getExtras(obj) : null);
        playbackStateCompat.mStateObj = obj;
        return playbackStateCompat;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public float getPlaybackSpeed() {
        return this.mSpeed;
    }

    public String toString() {
        return "PlaybackState {state=" + this.mState + ", position=" + this.mPosition + ", buffered position=" + this.mBufferedPosition + ", speed=" + this.mSpeed + ", updated=" + this.mUpdateTime + ", actions=" + this.mActions + ", error code=" + this.mErrorCode + ", error message=" + this.mErrorMessage + ", custom actions=" + this.mCustomActions + ", active item id=" + this.mActiveItemId + "}";
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mState);
        parcel.writeLong(this.mPosition);
        parcel.writeFloat(this.mSpeed);
        parcel.writeLong(this.mUpdateTime);
        parcel.writeLong(this.mBufferedPosition);
        parcel.writeLong(this.mActions);
        TextUtils.writeToParcel(this.mErrorMessage, parcel, i);
        parcel.writeTypedList(this.mCustomActions);
        parcel.writeLong(this.mActiveItemId);
        parcel.writeBundle(this.mExtras);
        parcel.writeInt(this.mErrorCode);
    }
}
