package android.support.v4.media.session;

import android.media.session.MediaSession;

class MediaSessionCompatApi21 {

    static class QueueItem {
        public static Object getDescription(Object obj) {
            return ((MediaSession.QueueItem) obj).getDescription();
        }

        public static long getQueueId(Object obj) {
            return ((MediaSession.QueueItem) obj).getQueueId();
        }
    }
}
