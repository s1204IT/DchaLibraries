package android.view.textclassifier;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;
import java.util.Objects;

public final class SelectionEvent implements Parcelable {
    public static final int ACTION_ABANDON = 107;
    public static final int ACTION_COPY = 101;
    public static final int ACTION_CUT = 103;
    public static final int ACTION_DRAG = 106;
    public static final int ACTION_OTHER = 108;
    public static final int ACTION_OVERTYPE = 100;
    public static final int ACTION_PASTE = 102;
    public static final int ACTION_RESET = 201;
    public static final int ACTION_SELECT_ALL = 200;
    public static final int ACTION_SHARE = 104;
    public static final int ACTION_SMART_SHARE = 105;
    public static final Parcelable.Creator<SelectionEvent> CREATOR = new Parcelable.Creator<SelectionEvent>() {
        @Override
        public SelectionEvent createFromParcel(Parcel parcel) {
            return new SelectionEvent(parcel);
        }

        @Override
        public SelectionEvent[] newArray(int i) {
            return new SelectionEvent[i];
        }
    };
    public static final int EVENT_AUTO_SELECTION = 5;
    public static final int EVENT_SELECTION_MODIFIED = 2;
    public static final int EVENT_SELECTION_STARTED = 1;
    public static final int EVENT_SMART_SELECTION_MULTI = 4;
    public static final int EVENT_SMART_SELECTION_SINGLE = 3;
    public static final int INVOCATION_LINK = 2;
    public static final int INVOCATION_MANUAL = 1;
    public static final int INVOCATION_UNKNOWN = 0;
    private static final String NO_SIGNATURE = "";
    private final int mAbsoluteEnd;
    private final int mAbsoluteStart;
    private long mDurationSincePreviousEvent;
    private long mDurationSinceSessionStart;
    private int mEnd;
    private final String mEntityType;
    private int mEventIndex;
    private long mEventTime;
    private int mEventType;
    private int mInvocationMethod;
    private String mPackageName;
    private String mResultId;
    private TextClassificationSessionId mSessionId;
    private int mSmartEnd;
    private int mSmartStart;
    private int mStart;
    private String mWidgetType;
    private String mWidgetVersion;

    @Retention(RetentionPolicy.SOURCE)
    public @interface ActionType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface EventType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface InvocationMethod {
    }

    SelectionEvent(int i, int i2, int i3, String str, int i4, String str2) {
        this.mPackageName = "";
        this.mWidgetType = "unknown";
        Preconditions.checkArgument(i2 >= i, "end cannot be less than start");
        this.mAbsoluteStart = i;
        this.mAbsoluteEnd = i2;
        this.mEventType = i3;
        this.mEntityType = (String) Preconditions.checkNotNull(str);
        this.mResultId = str2;
        this.mInvocationMethod = i4;
    }

    private SelectionEvent(Parcel parcel) {
        this.mPackageName = "";
        this.mWidgetType = "unknown";
        this.mAbsoluteStart = parcel.readInt();
        this.mAbsoluteEnd = parcel.readInt();
        this.mEventType = parcel.readInt();
        this.mEntityType = parcel.readString();
        this.mWidgetVersion = parcel.readInt() > 0 ? parcel.readString() : null;
        this.mPackageName = parcel.readString();
        this.mWidgetType = parcel.readString();
        this.mInvocationMethod = parcel.readInt();
        this.mResultId = parcel.readString();
        this.mEventTime = parcel.readLong();
        this.mDurationSinceSessionStart = parcel.readLong();
        this.mDurationSincePreviousEvent = parcel.readLong();
        this.mEventIndex = parcel.readInt();
        this.mSessionId = parcel.readInt() > 0 ? TextClassificationSessionId.CREATOR.createFromParcel(parcel) : null;
        this.mStart = parcel.readInt();
        this.mEnd = parcel.readInt();
        this.mSmartStart = parcel.readInt();
        this.mSmartEnd = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mAbsoluteStart);
        parcel.writeInt(this.mAbsoluteEnd);
        parcel.writeInt(this.mEventType);
        parcel.writeString(this.mEntityType);
        parcel.writeInt(this.mWidgetVersion != null ? 1 : 0);
        if (this.mWidgetVersion != null) {
            parcel.writeString(this.mWidgetVersion);
        }
        parcel.writeString(this.mPackageName);
        parcel.writeString(this.mWidgetType);
        parcel.writeInt(this.mInvocationMethod);
        parcel.writeString(this.mResultId);
        parcel.writeLong(this.mEventTime);
        parcel.writeLong(this.mDurationSinceSessionStart);
        parcel.writeLong(this.mDurationSincePreviousEvent);
        parcel.writeInt(this.mEventIndex);
        parcel.writeInt(this.mSessionId != null ? 1 : 0);
        if (this.mSessionId != null) {
            this.mSessionId.writeToParcel(parcel, i);
        }
        parcel.writeInt(this.mStart);
        parcel.writeInt(this.mEnd);
        parcel.writeInt(this.mSmartStart);
        parcel.writeInt(this.mSmartEnd);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static SelectionEvent createSelectionStartedEvent(int i, int i2) {
        return new SelectionEvent(i2, i2 + 1, 1, "", i, "");
    }

    public static SelectionEvent createSelectionModifiedEvent(int i, int i2) {
        Preconditions.checkArgument(i2 >= i, "end cannot be less than start");
        return new SelectionEvent(i, i2, 2, "", 0, "");
    }

    public static SelectionEvent createSelectionModifiedEvent(int i, int i2, TextClassification textClassification) {
        String entity;
        Preconditions.checkArgument(i2 >= i, "end cannot be less than start");
        Preconditions.checkNotNull(textClassification);
        if (textClassification.getEntityCount() > 0) {
            entity = textClassification.getEntity(0);
        } else {
            entity = "";
        }
        return new SelectionEvent(i, i2, 2, entity, 0, textClassification.getId());
    }

    public static SelectionEvent createSelectionModifiedEvent(int i, int i2, TextSelection textSelection) {
        String entity;
        Preconditions.checkArgument(i2 >= i, "end cannot be less than start");
        Preconditions.checkNotNull(textSelection);
        if (textSelection.getEntityCount() > 0) {
            entity = textSelection.getEntity(0);
        } else {
            entity = "";
        }
        return new SelectionEvent(i, i2, 5, entity, 0, textSelection.getId());
    }

    public static SelectionEvent createSelectionActionEvent(int i, int i2, int i3) {
        Preconditions.checkArgument(i2 >= i, "end cannot be less than start");
        checkActionType(i3);
        return new SelectionEvent(i, i2, i3, "", 0, "");
    }

    public static SelectionEvent createSelectionActionEvent(int i, int i2, int i3, TextClassification textClassification) {
        String entity;
        Preconditions.checkArgument(i2 >= i, "end cannot be less than start");
        Preconditions.checkNotNull(textClassification);
        checkActionType(i3);
        if (textClassification.getEntityCount() > 0) {
            entity = textClassification.getEntity(0);
        } else {
            entity = "";
        }
        return new SelectionEvent(i, i2, i3, entity, 0, textClassification.getId());
    }

    private static void checkActionType(int i) throws IllegalArgumentException {
        switch (i) {
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
                return;
            default:
                switch (i) {
                    case 200:
                    case 201:
                        return;
                    default:
                        throw new IllegalArgumentException(String.format(Locale.US, "%d is not an eventType", Integer.valueOf(i)));
                }
        }
    }

    int getAbsoluteStart() {
        return this.mAbsoluteStart;
    }

    int getAbsoluteEnd() {
        return this.mAbsoluteEnd;
    }

    public int getEventType() {
        return this.mEventType;
    }

    void setEventType(int i) {
        this.mEventType = i;
    }

    public String getEntityType() {
        return this.mEntityType;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public String getWidgetType() {
        return this.mWidgetType;
    }

    public String getWidgetVersion() {
        return this.mWidgetVersion;
    }

    void setTextClassificationSessionContext(TextClassificationContext textClassificationContext) {
        this.mPackageName = textClassificationContext.getPackageName();
        this.mWidgetType = textClassificationContext.getWidgetType();
        this.mWidgetVersion = textClassificationContext.getWidgetVersion();
    }

    public int getInvocationMethod() {
        return this.mInvocationMethod;
    }

    void setInvocationMethod(int i) {
        this.mInvocationMethod = i;
    }

    public String getResultId() {
        return this.mResultId;
    }

    SelectionEvent setResultId(String str) {
        this.mResultId = str;
        return this;
    }

    public long getEventTime() {
        return this.mEventTime;
    }

    SelectionEvent setEventTime(long j) {
        this.mEventTime = j;
        return this;
    }

    public long getDurationSinceSessionStart() {
        return this.mDurationSinceSessionStart;
    }

    SelectionEvent setDurationSinceSessionStart(long j) {
        this.mDurationSinceSessionStart = j;
        return this;
    }

    public long getDurationSincePreviousEvent() {
        return this.mDurationSincePreviousEvent;
    }

    SelectionEvent setDurationSincePreviousEvent(long j) {
        this.mDurationSincePreviousEvent = j;
        return this;
    }

    public int getEventIndex() {
        return this.mEventIndex;
    }

    SelectionEvent setEventIndex(int i) {
        this.mEventIndex = i;
        return this;
    }

    public TextClassificationSessionId getSessionId() {
        return this.mSessionId;
    }

    SelectionEvent setSessionId(TextClassificationSessionId textClassificationSessionId) {
        this.mSessionId = textClassificationSessionId;
        return this;
    }

    public int getStart() {
        return this.mStart;
    }

    SelectionEvent setStart(int i) {
        this.mStart = i;
        return this;
    }

    public int getEnd() {
        return this.mEnd;
    }

    SelectionEvent setEnd(int i) {
        this.mEnd = i;
        return this;
    }

    public int getSmartStart() {
        return this.mSmartStart;
    }

    SelectionEvent setSmartStart(int i) {
        this.mSmartStart = i;
        return this;
    }

    public int getSmartEnd() {
        return this.mSmartEnd;
    }

    SelectionEvent setSmartEnd(int i) {
        this.mSmartEnd = i;
        return this;
    }

    boolean isTerminal() {
        return isTerminal(this.mEventType);
    }

    public static boolean isTerminal(int i) {
        switch (i) {
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
                return true;
            default:
                return false;
        }
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mAbsoluteStart), Integer.valueOf(this.mAbsoluteEnd), Integer.valueOf(this.mEventType), this.mEntityType, this.mWidgetVersion, this.mPackageName, this.mWidgetType, Integer.valueOf(this.mInvocationMethod), this.mResultId, Long.valueOf(this.mEventTime), Long.valueOf(this.mDurationSinceSessionStart), Long.valueOf(this.mDurationSincePreviousEvent), Integer.valueOf(this.mEventIndex), this.mSessionId, Integer.valueOf(this.mStart), Integer.valueOf(this.mEnd), Integer.valueOf(this.mSmartStart), Integer.valueOf(this.mSmartEnd));
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SelectionEvent)) {
            return false;
        }
        SelectionEvent selectionEvent = (SelectionEvent) obj;
        return this.mAbsoluteStart == selectionEvent.mAbsoluteStart && this.mAbsoluteEnd == selectionEvent.mAbsoluteEnd && this.mEventType == selectionEvent.mEventType && Objects.equals(this.mEntityType, selectionEvent.mEntityType) && Objects.equals(this.mWidgetVersion, selectionEvent.mWidgetVersion) && Objects.equals(this.mPackageName, selectionEvent.mPackageName) && Objects.equals(this.mWidgetType, selectionEvent.mWidgetType) && this.mInvocationMethod == selectionEvent.mInvocationMethod && Objects.equals(this.mResultId, selectionEvent.mResultId) && this.mEventTime == selectionEvent.mEventTime && this.mDurationSinceSessionStart == selectionEvent.mDurationSinceSessionStart && this.mDurationSincePreviousEvent == selectionEvent.mDurationSincePreviousEvent && this.mEventIndex == selectionEvent.mEventIndex && Objects.equals(this.mSessionId, selectionEvent.mSessionId) && this.mStart == selectionEvent.mStart && this.mEnd == selectionEvent.mEnd && this.mSmartStart == selectionEvent.mSmartStart && this.mSmartEnd == selectionEvent.mSmartEnd;
    }

    public String toString() {
        return String.format(Locale.US, "SelectionEvent {absoluteStart=%d, absoluteEnd=%d, eventType=%d, entityType=%s, widgetVersion=%s, packageName=%s, widgetType=%s, invocationMethod=%s, resultId=%s, eventTime=%d, durationSinceSessionStart=%d, durationSincePreviousEvent=%d, eventIndex=%d,sessionId=%s, start=%d, end=%d, smartStart=%d, smartEnd=%d}", Integer.valueOf(this.mAbsoluteStart), Integer.valueOf(this.mAbsoluteEnd), Integer.valueOf(this.mEventType), this.mEntityType, this.mWidgetVersion, this.mPackageName, this.mWidgetType, Integer.valueOf(this.mInvocationMethod), this.mResultId, Long.valueOf(this.mEventTime), Long.valueOf(this.mDurationSinceSessionStart), Long.valueOf(this.mDurationSincePreviousEvent), Integer.valueOf(this.mEventIndex), this.mSessionId, Integer.valueOf(this.mStart), Integer.valueOf(this.mEnd), Integer.valueOf(this.mSmartStart), Integer.valueOf(this.mSmartEnd));
    }
}
