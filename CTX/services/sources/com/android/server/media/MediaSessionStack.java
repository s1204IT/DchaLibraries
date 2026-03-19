package com.android.server.media;

import android.media.session.MediaSession;
import android.os.Debug;
import android.util.IntArray;
import android.util.Log;
import android.util.SparseArray;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

class MediaSessionStack {
    private static final String TAG = "MediaSessionStack";
    private final AudioPlayerStateMonitor mAudioPlayerStateMonitor;
    private MediaSessionRecord mCachedVolumeDefault;
    private MediaSessionRecord mMediaButtonSession;
    private final OnMediaButtonSessionChangedListener mOnMediaButtonSessionChangedListener;
    private static final boolean DEBUG = MediaSessionService.DEBUG;
    private static final int[] ALWAYS_PRIORITY_STATES = {4, 5, 9, 10};
    private static final int[] TRANSITION_PRIORITY_STATES = {6, 8, 3};
    private final List<MediaSessionRecord> mSessions = new ArrayList();
    private final SparseArray<ArrayList<MediaSessionRecord>> mCachedActiveLists = new SparseArray<>();

    interface OnMediaButtonSessionChangedListener {
        void onMediaButtonSessionChanged(MediaSessionRecord mediaSessionRecord, MediaSessionRecord mediaSessionRecord2);
    }

    MediaSessionStack(AudioPlayerStateMonitor audioPlayerStateMonitor, OnMediaButtonSessionChangedListener onMediaButtonSessionChangedListener) {
        this.mAudioPlayerStateMonitor = audioPlayerStateMonitor;
        this.mOnMediaButtonSessionChangedListener = onMediaButtonSessionChangedListener;
    }

    public void addSession(MediaSessionRecord mediaSessionRecord) {
        this.mSessions.add(mediaSessionRecord);
        clearCache(mediaSessionRecord.getUserId());
        updateMediaButtonSessionIfNeeded();
    }

    public void removeSession(MediaSessionRecord mediaSessionRecord) {
        this.mSessions.remove(mediaSessionRecord);
        if (this.mMediaButtonSession == mediaSessionRecord) {
            updateMediaButtonSession(null);
        }
        clearCache(mediaSessionRecord.getUserId());
    }

    public boolean contains(MediaSessionRecord mediaSessionRecord) {
        return this.mSessions.contains(mediaSessionRecord);
    }

    public void onPlaystateChanged(MediaSessionRecord mediaSessionRecord, int i, int i2) {
        MediaSessionRecord mediaSessionRecordFindMediaButtonSession;
        if (shouldUpdatePriority(i, i2)) {
            this.mSessions.remove(mediaSessionRecord);
            this.mSessions.add(0, mediaSessionRecord);
            clearCache(mediaSessionRecord.getUserId());
        } else if (!MediaSession.isActiveState(i2)) {
            this.mCachedVolumeDefault = null;
        }
        if (this.mMediaButtonSession != null && this.mMediaButtonSession.getUid() == mediaSessionRecord.getUid() && (mediaSessionRecordFindMediaButtonSession = findMediaButtonSession(this.mMediaButtonSession.getUid())) != this.mMediaButtonSession) {
            updateMediaButtonSession(mediaSessionRecordFindMediaButtonSession);
        }
    }

    public void onSessionStateChange(MediaSessionRecord mediaSessionRecord) {
        clearCache(mediaSessionRecord.getUserId());
    }

    public void updateMediaButtonSessionIfNeeded() {
        if (DEBUG) {
            Log.d(TAG, "updateMediaButtonSessionIfNeeded, callers=" + Debug.getCallers(2));
        }
        IntArray sortedAudioPlaybackClientUids = this.mAudioPlayerStateMonitor.getSortedAudioPlaybackClientUids();
        for (int i = 0; i < sortedAudioPlaybackClientUids.size(); i++) {
            MediaSessionRecord mediaSessionRecordFindMediaButtonSession = findMediaButtonSession(sortedAudioPlaybackClientUids.get(i));
            if (mediaSessionRecordFindMediaButtonSession != null) {
                this.mAudioPlayerStateMonitor.cleanUpAudioPlaybackUids(mediaSessionRecordFindMediaButtonSession.getUid());
                if (this.mMediaButtonSession != mediaSessionRecordFindMediaButtonSession) {
                    updateMediaButtonSession(mediaSessionRecordFindMediaButtonSession);
                    return;
                }
                return;
            }
        }
    }

    private MediaSessionRecord findMediaButtonSession(int i) {
        MediaSessionRecord mediaSessionRecord = null;
        for (MediaSessionRecord mediaSessionRecord2 : this.mSessions) {
            if (i == mediaSessionRecord2.getUid()) {
                if (mediaSessionRecord2.getPlaybackState() != null && mediaSessionRecord2.isPlaybackActive() == this.mAudioPlayerStateMonitor.isPlaybackActive(mediaSessionRecord2.getUid())) {
                    return mediaSessionRecord2;
                }
                if (mediaSessionRecord == null) {
                    mediaSessionRecord = mediaSessionRecord2;
                }
            }
        }
        return mediaSessionRecord;
    }

    public ArrayList<MediaSessionRecord> getActiveSessions(int i) {
        ArrayList<MediaSessionRecord> arrayList = this.mCachedActiveLists.get(i);
        if (arrayList == null) {
            ArrayList<MediaSessionRecord> priorityList = getPriorityList(true, i);
            this.mCachedActiveLists.put(i, priorityList);
            return priorityList;
        }
        return arrayList;
    }

    public MediaSessionRecord getMediaButtonSession() {
        return this.mMediaButtonSession;
    }

    private void updateMediaButtonSession(MediaSessionRecord mediaSessionRecord) {
        MediaSessionRecord mediaSessionRecord2 = this.mMediaButtonSession;
        this.mMediaButtonSession = mediaSessionRecord;
        this.mOnMediaButtonSessionChangedListener.onMediaButtonSessionChanged(mediaSessionRecord2, mediaSessionRecord);
    }

    public MediaSessionRecord getDefaultVolumeSession() {
        if (this.mCachedVolumeDefault != null) {
            return this.mCachedVolumeDefault;
        }
        ArrayList<MediaSessionRecord> priorityList = getPriorityList(true, -1);
        int size = priorityList.size();
        for (int i = 0; i < size; i++) {
            MediaSessionRecord mediaSessionRecord = priorityList.get(i);
            if (mediaSessionRecord.isPlaybackActive()) {
                this.mCachedVolumeDefault = mediaSessionRecord;
                return mediaSessionRecord;
            }
        }
        return null;
    }

    public MediaSessionRecord getDefaultRemoteSession(int i) {
        ArrayList<MediaSessionRecord> priorityList = getPriorityList(true, i);
        int size = priorityList.size();
        for (int i2 = 0; i2 < size; i2++) {
            MediaSessionRecord mediaSessionRecord = priorityList.get(i2);
            if (mediaSessionRecord.getPlaybackType() == 2) {
                return mediaSessionRecord;
            }
        }
        return null;
    }

    public void dump(PrintWriter printWriter, String str) {
        ArrayList<MediaSessionRecord> priorityList = getPriorityList(false, -1);
        int size = priorityList.size();
        printWriter.println(str + "Media button session is " + this.mMediaButtonSession);
        printWriter.println(str + "Sessions Stack - have " + size + " sessions:");
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append("  ");
        String string = sb.toString();
        for (int i = 0; i < size; i++) {
            priorityList.get(i).dump(printWriter, string);
            printWriter.println();
        }
    }

    public ArrayList<MediaSessionRecord> getPriorityList(boolean z, int i) {
        ArrayList<MediaSessionRecord> arrayList = new ArrayList<>();
        int size = this.mSessions.size();
        int i2 = 0;
        int i3 = 0;
        for (int i4 = 0; i4 < size; i4++) {
            MediaSessionRecord mediaSessionRecord = this.mSessions.get(i4);
            if (i == -1 || i == mediaSessionRecord.getUserId()) {
                if (!mediaSessionRecord.isActive()) {
                    if (!z) {
                        arrayList.add(mediaSessionRecord);
                    }
                } else if (mediaSessionRecord.isPlaybackActive()) {
                    arrayList.add(i2, mediaSessionRecord);
                    i3++;
                    i2++;
                } else {
                    arrayList.add(i3, mediaSessionRecord);
                    i3++;
                }
            }
        }
        return arrayList;
    }

    private boolean shouldUpdatePriority(int i, int i2) {
        if (containsState(i2, ALWAYS_PRIORITY_STATES)) {
            return true;
        }
        return !containsState(i, TRANSITION_PRIORITY_STATES) && containsState(i2, TRANSITION_PRIORITY_STATES);
    }

    private boolean containsState(int i, int[] iArr) {
        for (int i2 : iArr) {
            if (i2 == i) {
                return true;
            }
        }
        return false;
    }

    private void clearCache(int i) {
        this.mCachedVolumeDefault = null;
        this.mCachedActiveLists.remove(i);
        this.mCachedActiveLists.remove(-1);
    }
}
