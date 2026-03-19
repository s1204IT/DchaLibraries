package com.android.location.provider;

public class ActivityRecognitionEvent {
    private final String mActivity;
    private final int mEventType;
    private final long mTimestampNs;

    public ActivityRecognitionEvent(String str, int i, long j) {
        this.mActivity = str;
        this.mEventType = i;
        this.mTimestampNs = j;
    }

    public String getActivity() {
        return this.mActivity;
    }

    public int getEventType() {
        return this.mEventType;
    }

    public long getTimestampNs() {
        return this.mTimestampNs;
    }

    public String toString() {
        String str;
        switch (this.mEventType) {
            case 0:
                str = "FlushComplete";
                break;
            case ActivityRecognitionProvider.EVENT_TYPE_ENTER:
                str = "Enter";
                break;
            case ActivityRecognitionProvider.EVENT_TYPE_EXIT:
                str = "Exit";
                break;
            default:
                str = "<Invalid>";
                break;
        }
        return String.format("Activity='%s', EventType=%s(%s), TimestampNs=%s", this.mActivity, str, Integer.valueOf(this.mEventType), Long.valueOf(this.mTimestampNs));
    }
}
