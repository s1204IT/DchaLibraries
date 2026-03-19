package com.android.bluetooth.avrcp;

import android.media.session.MediaSession;
import android.os.SystemProperties;
import android.util.Log;
import java.util.Iterator;

class GPMWrapper extends MediaPlayerWrapper {
    private static final boolean DEBUG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    private static final String TAG = "NewAvrcpGPMWrapper";

    GPMWrapper() {
    }

    @Override
    boolean isMetadataSynced() {
        if (getQueue() == null) {
            return false;
        }
        MediaSession.QueueItem queueItem = null;
        Iterator<MediaSession.QueueItem> it = getQueue().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            MediaSession.QueueItem next = it.next();
            if (next.getQueueId() == getActiveQueueID()) {
                queueItem = next;
                break;
            }
        }
        Metadata metadata = Util.toMetadata(queueItem);
        Metadata metadata2 = Util.toMetadata(getMetadata());
        if (queueItem == null || !metadata.equals(metadata2)) {
            if (DEBUG) {
                Log.d(TAG, "Metadata currently out of sync for Google Play Music");
                Log.d(TAG, "  └ Current queueItem: " + metadata);
                Log.d(TAG, "  └ Current metadata : " + metadata2);
            }
            return false;
        }
        return true;
    }
}
