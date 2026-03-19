package android.app.usage;

import android.annotation.SystemApi;
import android.content.res.Configuration;
import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

public final class UsageEvents implements Parcelable {
    public static final Parcelable.Creator<UsageEvents> CREATOR = new Parcelable.Creator<UsageEvents>() {
        @Override
        public UsageEvents createFromParcel(Parcel parcel) {
            return new UsageEvents(parcel);
        }

        @Override
        public UsageEvents[] newArray(int i) {
            return new UsageEvents[i];
        }
    };
    public static final String INSTANT_APP_CLASS_NAME = "android.instant_class";
    public static final String INSTANT_APP_PACKAGE_NAME = "android.instant_app";
    private final int mEventCount;
    private List<Event> mEventsToWrite;
    private int mIndex;
    private Parcel mParcel;
    private String[] mStringPool;

    public static final class Event {
        public static final int CHOOSER_ACTION = 9;
        public static final int CONFIGURATION_CHANGE = 5;
        public static final int CONTINUE_PREVIOUS_DAY = 4;
        public static final int END_OF_DAY = 3;
        public static final int FLAG_IS_PACKAGE_INSTANT_APP = 1;
        public static final int KEYGUARD_HIDDEN = 18;
        public static final int KEYGUARD_SHOWN = 17;
        public static final int MOVE_TO_BACKGROUND = 2;
        public static final int MOVE_TO_FOREGROUND = 1;
        public static final int NONE = 0;

        @SystemApi
        public static final int NOTIFICATION_INTERRUPTION = 12;

        @SystemApi
        public static final int NOTIFICATION_SEEN = 10;
        public static final int SCREEN_INTERACTIVE = 15;
        public static final int SCREEN_NON_INTERACTIVE = 16;
        public static final int SHORTCUT_INVOCATION = 8;

        @SystemApi
        public static final int SLICE_PINNED = 14;

        @SystemApi
        public static final int SLICE_PINNED_PRIV = 13;
        public static final int STANDBY_BUCKET_CHANGED = 11;

        @SystemApi
        public static final int SYSTEM_INTERACTION = 6;
        public static final int USER_INTERACTION = 7;
        public String mAction;
        public int mBucketAndReason;
        public String mClass;
        public Configuration mConfiguration;
        public String[] mContentAnnotations;
        public String mContentType;
        public int mEventType;
        public int mFlags;
        public String mNotificationChannelId;
        public String mPackage;
        public String mShortcutId;
        public long mTimeStamp;

        @Retention(RetentionPolicy.SOURCE)
        public @interface EventFlags {
        }

        public Event() {
        }

        public Event(Event event) {
            this.mPackage = event.mPackage;
            this.mClass = event.mClass;
            this.mTimeStamp = event.mTimeStamp;
            this.mEventType = event.mEventType;
            this.mConfiguration = event.mConfiguration;
            this.mShortcutId = event.mShortcutId;
            this.mAction = event.mAction;
            this.mContentType = event.mContentType;
            this.mContentAnnotations = event.mContentAnnotations;
            this.mFlags = event.mFlags;
            this.mBucketAndReason = event.mBucketAndReason;
            this.mNotificationChannelId = event.mNotificationChannelId;
        }

        public String getPackageName() {
            return this.mPackage;
        }

        public String getClassName() {
            return this.mClass;
        }

        public long getTimeStamp() {
            return this.mTimeStamp;
        }

        public int getEventType() {
            return this.mEventType;
        }

        public Configuration getConfiguration() {
            return this.mConfiguration;
        }

        public String getShortcutId() {
            return this.mShortcutId;
        }

        public int getStandbyBucket() {
            return (this.mBucketAndReason & (-65536)) >>> 16;
        }

        public int getAppStandbyBucket() {
            return (this.mBucketAndReason & (-65536)) >>> 16;
        }

        public int getStandbyReason() {
            return this.mBucketAndReason & 65535;
        }

        @SystemApi
        public String getNotificationChannelId() {
            return this.mNotificationChannelId;
        }

        public Event getObfuscatedIfInstantApp() {
            if ((this.mFlags & 1) == 0) {
                return this;
            }
            Event event = new Event(this);
            event.mPackage = UsageEvents.INSTANT_APP_PACKAGE_NAME;
            event.mClass = UsageEvents.INSTANT_APP_CLASS_NAME;
            return event;
        }
    }

    public UsageEvents(Parcel parcel) {
        this.mEventsToWrite = null;
        this.mParcel = null;
        this.mIndex = 0;
        byte[] blob = parcel.readBlob();
        Parcel parcelObtain = Parcel.obtain();
        parcelObtain.unmarshall(blob, 0, blob.length);
        parcelObtain.setDataPosition(0);
        this.mEventCount = parcelObtain.readInt();
        this.mIndex = parcelObtain.readInt();
        if (this.mEventCount > 0) {
            this.mStringPool = parcelObtain.createStringArray();
            int i = parcelObtain.readInt();
            int i2 = parcelObtain.readInt();
            this.mParcel = Parcel.obtain();
            this.mParcel.setDataPosition(0);
            this.mParcel.appendFrom(parcelObtain, parcelObtain.dataPosition(), i);
            this.mParcel.setDataSize(this.mParcel.dataPosition());
            this.mParcel.setDataPosition(i2);
        }
    }

    UsageEvents() {
        this.mEventsToWrite = null;
        this.mParcel = null;
        this.mIndex = 0;
        this.mEventCount = 0;
    }

    public UsageEvents(List<Event> list, String[] strArr) {
        this.mEventsToWrite = null;
        this.mParcel = null;
        this.mIndex = 0;
        this.mStringPool = strArr;
        this.mEventCount = list.size();
        this.mEventsToWrite = list;
    }

    public boolean hasNextEvent() {
        return this.mIndex < this.mEventCount;
    }

    public boolean getNextEvent(Event event) {
        if (this.mIndex >= this.mEventCount) {
            return false;
        }
        readEventFromParcel(this.mParcel, event);
        this.mIndex++;
        if (this.mIndex >= this.mEventCount) {
            this.mParcel.recycle();
            this.mParcel = null;
        }
        return true;
    }

    public void resetToStart() {
        this.mIndex = 0;
        if (this.mParcel != null) {
            this.mParcel.setDataPosition(0);
        }
    }

    private int findStringIndex(String str) {
        int iBinarySearch = Arrays.binarySearch(this.mStringPool, str);
        if (iBinarySearch < 0) {
            throw new IllegalStateException("String '" + str + "' is not in the string pool");
        }
        return iBinarySearch;
    }

    private void writeEventToParcel(Event event, Parcel parcel, int i) {
        int iFindStringIndex;
        if (event.mPackage != null) {
            iFindStringIndex = findStringIndex(event.mPackage);
        } else {
            iFindStringIndex = -1;
        }
        int iFindStringIndex2 = event.mClass != null ? findStringIndex(event.mClass) : -1;
        parcel.writeInt(iFindStringIndex);
        parcel.writeInt(iFindStringIndex2);
        parcel.writeInt(event.mEventType);
        parcel.writeLong(event.mTimeStamp);
        switch (event.mEventType) {
            case 5:
                event.mConfiguration.writeToParcel(parcel, i);
                break;
            case 8:
                parcel.writeString(event.mShortcutId);
                break;
            case 9:
                parcel.writeString(event.mAction);
                parcel.writeString(event.mContentType);
                parcel.writeStringArray(event.mContentAnnotations);
                break;
            case 11:
                parcel.writeInt(event.mBucketAndReason);
                break;
            case 12:
                parcel.writeString(event.mNotificationChannelId);
                break;
        }
    }

    private void readEventFromParcel(Parcel parcel, Event event) {
        int i = parcel.readInt();
        if (i >= 0) {
            event.mPackage = this.mStringPool[i];
        } else {
            event.mPackage = null;
        }
        int i2 = parcel.readInt();
        if (i2 >= 0) {
            event.mClass = this.mStringPool[i2];
        } else {
            event.mClass = null;
        }
        event.mEventType = parcel.readInt();
        event.mTimeStamp = parcel.readLong();
        event.mConfiguration = null;
        event.mShortcutId = null;
        event.mAction = null;
        event.mContentType = null;
        event.mContentAnnotations = null;
        event.mNotificationChannelId = null;
        switch (event.mEventType) {
            case 5:
                event.mConfiguration = Configuration.CREATOR.createFromParcel(parcel);
                break;
            case 8:
                event.mShortcutId = parcel.readString();
                break;
            case 9:
                event.mAction = parcel.readString();
                event.mContentType = parcel.readString();
                event.mContentAnnotations = parcel.createStringArray();
                break;
            case 11:
                event.mBucketAndReason = parcel.readInt();
                break;
            case 12:
                event.mNotificationChannelId = parcel.readString();
                break;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        Parcel parcelObtain = Parcel.obtain();
        parcelObtain.writeInt(this.mEventCount);
        parcelObtain.writeInt(this.mIndex);
        if (this.mEventCount > 0) {
            parcelObtain.writeStringArray(this.mStringPool);
            if (this.mEventsToWrite != null) {
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain2.setDataPosition(0);
                    for (int i2 = 0; i2 < this.mEventCount; i2++) {
                        writeEventToParcel(this.mEventsToWrite.get(i2), parcelObtain2, i);
                    }
                    int iDataPosition = parcelObtain2.dataPosition();
                    parcelObtain.writeInt(iDataPosition);
                    parcelObtain.writeInt(0);
                    parcelObtain.appendFrom(parcelObtain2, 0, iDataPosition);
                } finally {
                    parcelObtain2.recycle();
                }
            } else if (this.mParcel != null) {
                parcelObtain.writeInt(this.mParcel.dataSize());
                parcelObtain.writeInt(this.mParcel.dataPosition());
                parcelObtain.appendFrom(this.mParcel, 0, this.mParcel.dataSize());
            } else {
                throw new IllegalStateException("Either mParcel or mEventsToWrite must not be null");
            }
        }
        parcel.writeBlob(parcelObtain.marshall());
    }
}
