package android.media;

public class MediaSyncEvent {
    public static final int SYNC_EVENT_NONE = 0;
    public static final int SYNC_EVENT_PRESENTATION_COMPLETE = 1;
    private int mAudioSession = 0;
    private final int mType;

    public static MediaSyncEvent createEvent(int i) throws IllegalArgumentException {
        if (!isValidType(i)) {
            throw new IllegalArgumentException(i + "is not a valid MediaSyncEvent type.");
        }
        return new MediaSyncEvent(i);
    }

    private MediaSyncEvent(int i) {
        this.mType = i;
    }

    public MediaSyncEvent setAudioSessionId(int i) throws IllegalArgumentException {
        if (i > 0) {
            this.mAudioSession = i;
            return this;
        }
        throw new IllegalArgumentException(i + " is not a valid session ID.");
    }

    public int getType() {
        return this.mType;
    }

    public int getAudioSessionId() {
        return this.mAudioSession;
    }

    private static boolean isValidType(int i) {
        switch (i) {
            case 0:
            case 1:
                return true;
            default:
                return false;
        }
    }
}
