package android.view.accessibility;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Pools;
import com.android.internal.util.BitUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.function.IntFunction;

public final class AccessibilityEvent extends AccessibilityRecord implements Parcelable {
    public static final int CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION = 4;
    public static final int CONTENT_CHANGE_TYPE_PANE_APPEARED = 16;
    public static final int CONTENT_CHANGE_TYPE_PANE_DISAPPEARED = 32;
    public static final int CONTENT_CHANGE_TYPE_PANE_TITLE = 8;
    public static final int CONTENT_CHANGE_TYPE_SUBTREE = 1;
    public static final int CONTENT_CHANGE_TYPE_TEXT = 2;
    public static final int CONTENT_CHANGE_TYPE_UNDEFINED = 0;
    private static final boolean DEBUG = false;
    public static final boolean DEBUG_ORIGIN = false;
    public static final int INVALID_POSITION = -1;
    private static final int MAX_POOL_SIZE = 10;

    @Deprecated
    public static final int MAX_TEXT_LENGTH = 500;
    public static final int TYPES_ALL_MASK = -1;
    public static final int TYPE_ANNOUNCEMENT = 16384;
    public static final int TYPE_ASSIST_READING_CONTEXT = 16777216;
    public static final int TYPE_GESTURE_DETECTION_END = 524288;
    public static final int TYPE_GESTURE_DETECTION_START = 262144;
    public static final int TYPE_NOTIFICATION_STATE_CHANGED = 64;
    public static final int TYPE_TOUCH_EXPLORATION_GESTURE_END = 1024;
    public static final int TYPE_TOUCH_EXPLORATION_GESTURE_START = 512;
    public static final int TYPE_TOUCH_INTERACTION_END = 2097152;
    public static final int TYPE_TOUCH_INTERACTION_START = 1048576;
    public static final int TYPE_VIEW_ACCESSIBILITY_FOCUSED = 32768;
    public static final int TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED = 65536;
    public static final int TYPE_VIEW_CLICKED = 1;
    public static final int TYPE_VIEW_CONTEXT_CLICKED = 8388608;
    public static final int TYPE_VIEW_FOCUSED = 8;
    public static final int TYPE_VIEW_HOVER_ENTER = 128;
    public static final int TYPE_VIEW_HOVER_EXIT = 256;
    public static final int TYPE_VIEW_LONG_CLICKED = 2;
    public static final int TYPE_VIEW_SCROLLED = 4096;
    public static final int TYPE_VIEW_SELECTED = 4;
    public static final int TYPE_VIEW_TEXT_CHANGED = 16;
    public static final int TYPE_VIEW_TEXT_SELECTION_CHANGED = 8192;
    public static final int TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY = 131072;
    public static final int TYPE_WINDOWS_CHANGED = 4194304;
    public static final int TYPE_WINDOW_CONTENT_CHANGED = 2048;
    public static final int TYPE_WINDOW_STATE_CHANGED = 32;
    public static final int WINDOWS_CHANGE_ACCESSIBILITY_FOCUSED = 128;
    public static final int WINDOWS_CHANGE_ACTIVE = 32;
    public static final int WINDOWS_CHANGE_ADDED = 1;
    public static final int WINDOWS_CHANGE_BOUNDS = 8;
    public static final int WINDOWS_CHANGE_CHILDREN = 512;
    public static final int WINDOWS_CHANGE_FOCUSED = 64;
    public static final int WINDOWS_CHANGE_LAYER = 16;
    public static final int WINDOWS_CHANGE_PARENT = 256;
    public static final int WINDOWS_CHANGE_PIP = 1024;
    public static final int WINDOWS_CHANGE_REMOVED = 2;
    public static final int WINDOWS_CHANGE_TITLE = 4;
    int mAction;
    int mContentChangeTypes;
    private long mEventTime;
    private int mEventType;
    int mMovementGranularity;
    private CharSequence mPackageName;
    private ArrayList<AccessibilityRecord> mRecords;
    int mWindowChangeTypes;
    public StackTraceElement[] originStackTrace = null;
    private static final Pools.SynchronizedPool<AccessibilityEvent> sPool = new Pools.SynchronizedPool<>(10);
    public static final Parcelable.Creator<AccessibilityEvent> CREATOR = new Parcelable.Creator<AccessibilityEvent>() {
        @Override
        public AccessibilityEvent createFromParcel(Parcel parcel) {
            AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain();
            accessibilityEventObtain.initFromParcel(parcel);
            return accessibilityEventObtain;
        }

        @Override
        public AccessibilityEvent[] newArray(int i) {
            return new AccessibilityEvent[i];
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    public @interface ContentChangeTypes {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface EventType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface WindowsChangeTypes {
    }

    private AccessibilityEvent() {
    }

    void init(AccessibilityEvent accessibilityEvent) {
        super.init((AccessibilityRecord) accessibilityEvent);
        this.mEventType = accessibilityEvent.mEventType;
        this.mMovementGranularity = accessibilityEvent.mMovementGranularity;
        this.mAction = accessibilityEvent.mAction;
        this.mContentChangeTypes = accessibilityEvent.mContentChangeTypes;
        this.mWindowChangeTypes = accessibilityEvent.mWindowChangeTypes;
        this.mEventTime = accessibilityEvent.mEventTime;
        this.mPackageName = accessibilityEvent.mPackageName;
    }

    @Override
    public void setSealed(boolean z) {
        super.setSealed(z);
        ArrayList<AccessibilityRecord> arrayList = this.mRecords;
        if (arrayList != null) {
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                arrayList.get(i).setSealed(z);
            }
        }
    }

    public int getRecordCount() {
        if (this.mRecords == null) {
            return 0;
        }
        return this.mRecords.size();
    }

    public void appendRecord(AccessibilityRecord accessibilityRecord) {
        enforceNotSealed();
        if (this.mRecords == null) {
            this.mRecords = new ArrayList<>();
        }
        this.mRecords.add(accessibilityRecord);
    }

    public AccessibilityRecord getRecord(int i) {
        if (this.mRecords == null) {
            throw new IndexOutOfBoundsException("Invalid index " + i + ", size is 0");
        }
        return this.mRecords.get(i);
    }

    public int getEventType() {
        return this.mEventType;
    }

    public int getContentChangeTypes() {
        return this.mContentChangeTypes;
    }

    private static String contentChangeTypesToString(int i) {
        return BitUtils.flagsToString(i, new IntFunction() {
            @Override
            public final Object apply(int i2) {
                return AccessibilityEvent.singleContentChangeTypeToString(i2);
            }
        });
    }

    private static String singleContentChangeTypeToString(int i) {
        if (i == 4) {
            return "CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION";
        }
        if (i == 8) {
            return "CONTENT_CHANGE_TYPE_PANE_TITLE";
        }
        switch (i) {
            case 0:
                return "CONTENT_CHANGE_TYPE_UNDEFINED";
            case 1:
                return "CONTENT_CHANGE_TYPE_SUBTREE";
            case 2:
                return "CONTENT_CHANGE_TYPE_TEXT";
            default:
                return Integer.toHexString(i);
        }
    }

    public void setContentChangeTypes(int i) {
        enforceNotSealed();
        this.mContentChangeTypes = i;
    }

    public int getWindowChanges() {
        return this.mWindowChangeTypes;
    }

    public void setWindowChanges(int i) {
        this.mWindowChangeTypes = i;
    }

    private static String windowChangeTypesToString(int i) {
        return BitUtils.flagsToString(i, new IntFunction() {
            @Override
            public final Object apply(int i2) {
                return AccessibilityEvent.singleWindowChangeTypeToString(i2);
            }
        });
    }

    private static String singleWindowChangeTypeToString(int i) {
        if (i == 4) {
            return "WINDOWS_CHANGE_TITLE";
        }
        if (i == 8) {
            return "WINDOWS_CHANGE_BOUNDS";
        }
        if (i == 16) {
            return "WINDOWS_CHANGE_LAYER";
        }
        if (i == 32) {
            return "WINDOWS_CHANGE_ACTIVE";
        }
        if (i == 64) {
            return "WINDOWS_CHANGE_FOCUSED";
        }
        if (i == 128) {
            return "WINDOWS_CHANGE_ACCESSIBILITY_FOCUSED";
        }
        if (i == 256) {
            return "WINDOWS_CHANGE_PARENT";
        }
        if (i != 512) {
            switch (i) {
                case 1:
                    return "WINDOWS_CHANGE_ADDED";
                case 2:
                    return "WINDOWS_CHANGE_REMOVED";
                default:
                    return Integer.toHexString(i);
            }
        }
        return "WINDOWS_CHANGE_CHILDREN";
    }

    public void setEventType(int i) {
        enforceNotSealed();
        this.mEventType = i;
    }

    public long getEventTime() {
        return this.mEventTime;
    }

    public void setEventTime(long j) {
        enforceNotSealed();
        this.mEventTime = j;
    }

    public CharSequence getPackageName() {
        return this.mPackageName;
    }

    public void setPackageName(CharSequence charSequence) {
        enforceNotSealed();
        this.mPackageName = charSequence;
    }

    public void setMovementGranularity(int i) {
        enforceNotSealed();
        this.mMovementGranularity = i;
    }

    public int getMovementGranularity() {
        return this.mMovementGranularity;
    }

    public void setAction(int i) {
        enforceNotSealed();
        this.mAction = i;
    }

    public int getAction() {
        return this.mAction;
    }

    public static AccessibilityEvent obtainWindowsChangedEvent(int i, int i2) {
        AccessibilityEvent accessibilityEventObtain = obtain(4194304);
        accessibilityEventObtain.setWindowId(i);
        accessibilityEventObtain.setWindowChanges(i2);
        accessibilityEventObtain.setImportantForAccessibility(true);
        return accessibilityEventObtain;
    }

    public static AccessibilityEvent obtain(int i) {
        AccessibilityEvent accessibilityEventObtain = obtain();
        accessibilityEventObtain.setEventType(i);
        return accessibilityEventObtain;
    }

    public static AccessibilityEvent obtain(AccessibilityEvent accessibilityEvent) {
        AccessibilityEvent accessibilityEventObtain = obtain();
        accessibilityEventObtain.init(accessibilityEvent);
        if (accessibilityEvent.mRecords != null) {
            int size = accessibilityEvent.mRecords.size();
            accessibilityEventObtain.mRecords = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                accessibilityEventObtain.mRecords.add(AccessibilityRecord.obtain(accessibilityEvent.mRecords.get(i)));
            }
        }
        return accessibilityEventObtain;
    }

    public static AccessibilityEvent obtain() {
        AccessibilityEvent accessibilityEventAcquire = sPool.acquire();
        return accessibilityEventAcquire == null ? new AccessibilityEvent() : accessibilityEventAcquire;
    }

    @Override
    public void recycle() {
        clear();
        sPool.release(this);
    }

    @Override
    protected void clear() {
        super.clear();
        this.mEventType = 0;
        this.mMovementGranularity = 0;
        this.mAction = 0;
        this.mContentChangeTypes = 0;
        this.mWindowChangeTypes = 0;
        this.mPackageName = null;
        this.mEventTime = 0L;
        if (this.mRecords != null) {
            while (!this.mRecords.isEmpty()) {
                this.mRecords.remove(0).recycle();
            }
        }
    }

    public void initFromParcel(Parcel parcel) {
        this.mSealed = parcel.readInt() == 1;
        this.mEventType = parcel.readInt();
        this.mMovementGranularity = parcel.readInt();
        this.mAction = parcel.readInt();
        this.mContentChangeTypes = parcel.readInt();
        this.mWindowChangeTypes = parcel.readInt();
        this.mPackageName = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
        this.mEventTime = parcel.readLong();
        this.mConnectionId = parcel.readInt();
        readAccessibilityRecordFromParcel(this, parcel);
        int i = parcel.readInt();
        if (i > 0) {
            this.mRecords = new ArrayList<>(i);
            for (int i2 = 0; i2 < i; i2++) {
                AccessibilityRecord accessibilityRecordObtain = AccessibilityRecord.obtain();
                readAccessibilityRecordFromParcel(accessibilityRecordObtain, parcel);
                accessibilityRecordObtain.mConnectionId = this.mConnectionId;
                this.mRecords.add(accessibilityRecordObtain);
            }
        }
    }

    private void readAccessibilityRecordFromParcel(AccessibilityRecord accessibilityRecord, Parcel parcel) {
        accessibilityRecord.mBooleanProperties = parcel.readInt();
        accessibilityRecord.mCurrentItemIndex = parcel.readInt();
        accessibilityRecord.mItemCount = parcel.readInt();
        accessibilityRecord.mFromIndex = parcel.readInt();
        accessibilityRecord.mToIndex = parcel.readInt();
        accessibilityRecord.mScrollX = parcel.readInt();
        accessibilityRecord.mScrollY = parcel.readInt();
        accessibilityRecord.mScrollDeltaX = parcel.readInt();
        accessibilityRecord.mScrollDeltaY = parcel.readInt();
        accessibilityRecord.mMaxScrollX = parcel.readInt();
        accessibilityRecord.mMaxScrollY = parcel.readInt();
        accessibilityRecord.mAddedCount = parcel.readInt();
        accessibilityRecord.mRemovedCount = parcel.readInt();
        accessibilityRecord.mClassName = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
        accessibilityRecord.mContentDescription = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
        accessibilityRecord.mBeforeText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
        accessibilityRecord.mParcelableData = parcel.readParcelable(null);
        parcel.readList(accessibilityRecord.mText, null);
        accessibilityRecord.mSourceWindowId = parcel.readInt();
        accessibilityRecord.mSourceNodeId = parcel.readLong();
        accessibilityRecord.mSealed = parcel.readInt() == 1;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(isSealed() ? 1 : 0);
        parcel.writeInt(this.mEventType);
        parcel.writeInt(this.mMovementGranularity);
        parcel.writeInt(this.mAction);
        parcel.writeInt(this.mContentChangeTypes);
        parcel.writeInt(this.mWindowChangeTypes);
        TextUtils.writeToParcel(this.mPackageName, parcel, 0);
        parcel.writeLong(this.mEventTime);
        parcel.writeInt(this.mConnectionId);
        writeAccessibilityRecordToParcel(this, parcel, i);
        int recordCount = getRecordCount();
        parcel.writeInt(recordCount);
        for (int i2 = 0; i2 < recordCount; i2++) {
            writeAccessibilityRecordToParcel(this.mRecords.get(i2), parcel, i);
        }
    }

    private void writeAccessibilityRecordToParcel(AccessibilityRecord accessibilityRecord, Parcel parcel, int i) {
        parcel.writeInt(accessibilityRecord.mBooleanProperties);
        parcel.writeInt(accessibilityRecord.mCurrentItemIndex);
        parcel.writeInt(accessibilityRecord.mItemCount);
        parcel.writeInt(accessibilityRecord.mFromIndex);
        parcel.writeInt(accessibilityRecord.mToIndex);
        parcel.writeInt(accessibilityRecord.mScrollX);
        parcel.writeInt(accessibilityRecord.mScrollY);
        parcel.writeInt(accessibilityRecord.mScrollDeltaX);
        parcel.writeInt(accessibilityRecord.mScrollDeltaY);
        parcel.writeInt(accessibilityRecord.mMaxScrollX);
        parcel.writeInt(accessibilityRecord.mMaxScrollY);
        parcel.writeInt(accessibilityRecord.mAddedCount);
        parcel.writeInt(accessibilityRecord.mRemovedCount);
        TextUtils.writeToParcel(accessibilityRecord.mClassName, parcel, i);
        TextUtils.writeToParcel(accessibilityRecord.mContentDescription, parcel, i);
        TextUtils.writeToParcel(accessibilityRecord.mBeforeText, parcel, i);
        parcel.writeParcelable(accessibilityRecord.mParcelableData, i);
        parcel.writeList(accessibilityRecord.mText);
        parcel.writeInt(accessibilityRecord.mSourceWindowId);
        parcel.writeLong(accessibilityRecord.mSourceNodeId);
        parcel.writeInt(accessibilityRecord.mSealed ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("EventType: ");
        sb.append(eventTypeToString(this.mEventType));
        sb.append("; EventTime: ");
        sb.append(this.mEventTime);
        sb.append("; PackageName: ");
        sb.append(this.mPackageName);
        sb.append("; MovementGranularity: ");
        sb.append(this.mMovementGranularity);
        sb.append("; Action: ");
        sb.append(this.mAction);
        sb.append("; ContentChangeTypes: ");
        sb.append(contentChangeTypesToString(this.mContentChangeTypes));
        sb.append("; WindowChangeTypes: ");
        sb.append(windowChangeTypesToString(this.mWindowChangeTypes));
        super.appendTo(sb);
        sb.append("; recordCount: ");
        sb.append(getRecordCount());
        return sb.toString();
    }

    public static String eventTypeToString(int i) {
        if (i == -1) {
            return "TYPES_ALL_MASK";
        }
        StringBuilder sb = new StringBuilder();
        int i2 = 0;
        while (i != 0) {
            int iNumberOfTrailingZeros = 1 << Integer.numberOfTrailingZeros(i);
            i &= ~iNumberOfTrailingZeros;
            if (i2 > 0) {
                sb.append(", ");
            }
            sb.append(singleEventTypeToString(iNumberOfTrailingZeros));
            i2++;
        }
        if (i2 > 1) {
            sb.insert(0, '[');
            sb.append(']');
        }
        return sb.toString();
    }

    private static String singleEventTypeToString(int i) {
        switch (i) {
            case 1:
                return "TYPE_VIEW_CLICKED";
            case 2:
                return "TYPE_VIEW_LONG_CLICKED";
            case 4:
                return "TYPE_VIEW_SELECTED";
            case 8:
                return "TYPE_VIEW_FOCUSED";
            case 16:
                return "TYPE_VIEW_TEXT_CHANGED";
            case 32:
                return "TYPE_WINDOW_STATE_CHANGED";
            case 64:
                return "TYPE_NOTIFICATION_STATE_CHANGED";
            case 128:
                return "TYPE_VIEW_HOVER_ENTER";
            case 256:
                return "TYPE_VIEW_HOVER_EXIT";
            case 512:
                return "TYPE_TOUCH_EXPLORATION_GESTURE_START";
            case 1024:
                return "TYPE_TOUCH_EXPLORATION_GESTURE_END";
            case 2048:
                return "TYPE_WINDOW_CONTENT_CHANGED";
            case 4096:
                return "TYPE_VIEW_SCROLLED";
            case 8192:
                return "TYPE_VIEW_TEXT_SELECTION_CHANGED";
            case 16384:
                return "TYPE_ANNOUNCEMENT";
            case 32768:
                return "TYPE_VIEW_ACCESSIBILITY_FOCUSED";
            case 65536:
                return "TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED";
            case 131072:
                return "TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY";
            case 262144:
                return "TYPE_GESTURE_DETECTION_START";
            case 524288:
                return "TYPE_GESTURE_DETECTION_END";
            case 1048576:
                return "TYPE_TOUCH_INTERACTION_START";
            case 2097152:
                return "TYPE_TOUCH_INTERACTION_END";
            case 4194304:
                return "TYPE_WINDOWS_CHANGED";
            case 8388608:
                return "TYPE_VIEW_CONTEXT_CLICKED";
            case 16777216:
                return "TYPE_ASSIST_READING_CONTEXT";
            default:
                return Integer.toHexString(i);
        }
    }
}
