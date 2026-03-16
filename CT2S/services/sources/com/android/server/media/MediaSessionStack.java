package com.android.server.media;

import android.media.session.MediaSession;
import java.io.PrintWriter;
import java.util.ArrayList;

public class MediaSessionStack {
    private static final int[] ALWAYS_PRIORITY_STATES = {4, 5, 9, 10};
    private static final int[] TRANSITION_PRIORITY_STATES = {6, 8, 3};
    private ArrayList<MediaSessionRecord> mCachedActiveList;
    private MediaSessionRecord mCachedButtonReceiver;
    private MediaSessionRecord mCachedDefault;
    private ArrayList<MediaSessionRecord> mCachedTransportControlList;
    private MediaSessionRecord mCachedVolumeDefault;
    private MediaSessionRecord mGlobalPrioritySession;
    private MediaSessionRecord mLastInterestingRecord;
    private final ArrayList<MediaSessionRecord> mSessions = new ArrayList<>();

    public void addSession(MediaSessionRecord record) {
        this.mSessions.add(record);
        clearCache();
        this.mLastInterestingRecord = record;
    }

    public void removeSession(MediaSessionRecord record) {
        this.mSessions.remove(record);
        if (record == this.mGlobalPrioritySession) {
            this.mGlobalPrioritySession = null;
        }
        clearCache();
    }

    public boolean onPlaystateChange(MediaSessionRecord record, int oldState, int newState) {
        if (shouldUpdatePriority(oldState, newState)) {
            this.mSessions.remove(record);
            this.mSessions.add(0, record);
            clearCache();
            this.mLastInterestingRecord = record;
            return true;
        }
        if (MediaSession.isActiveState(newState)) {
            return false;
        }
        this.mCachedVolumeDefault = null;
        return false;
    }

    public void onSessionStateChange(MediaSessionRecord record) {
        if ((record.getFlags() & 65536) != 0) {
            this.mGlobalPrioritySession = record;
        }
        clearCache();
    }

    public ArrayList<MediaSessionRecord> getActiveSessions(int userId) {
        if (this.mCachedActiveList == null) {
            this.mCachedActiveList = getPriorityListLocked(true, 0, userId);
        }
        return this.mCachedActiveList;
    }

    public ArrayList<MediaSessionRecord> getTransportControlSessions(int userId) {
        if (this.mCachedTransportControlList == null) {
            this.mCachedTransportControlList = getPriorityListLocked(true, 2, userId);
        }
        return this.mCachedTransportControlList;
    }

    public MediaSessionRecord getDefaultSession(int userId) {
        if (this.mCachedDefault != null) {
            return this.mCachedDefault;
        }
        ArrayList<MediaSessionRecord> records = getPriorityListLocked(true, 0, userId);
        if (records.size() > 0) {
            return records.get(0);
        }
        return null;
    }

    public MediaSessionRecord getDefaultMediaButtonSession(int userId, boolean includeNotPlaying) {
        if (this.mGlobalPrioritySession != null && this.mGlobalPrioritySession.isActive()) {
            return this.mGlobalPrioritySession;
        }
        if (this.mCachedButtonReceiver != null) {
            return this.mCachedButtonReceiver;
        }
        ArrayList<MediaSessionRecord> records = getPriorityListLocked(true, 1, userId);
        if (records.size() > 0) {
            MediaSessionRecord record = records.get(0);
            if (record.isPlaybackActive(false)) {
                this.mLastInterestingRecord = record;
                this.mCachedButtonReceiver = record;
            } else if (this.mLastInterestingRecord != null) {
                if (records.contains(this.mLastInterestingRecord)) {
                    this.mCachedButtonReceiver = this.mLastInterestingRecord;
                } else {
                    this.mLastInterestingRecord = null;
                }
            }
            if (includeNotPlaying && this.mCachedButtonReceiver == null) {
                this.mCachedButtonReceiver = record;
            }
        }
        return this.mCachedButtonReceiver;
    }

    public MediaSessionRecord getDefaultVolumeSession(int userId) {
        if (this.mGlobalPrioritySession != null && this.mGlobalPrioritySession.isActive()) {
            return this.mGlobalPrioritySession;
        }
        if (this.mCachedVolumeDefault != null) {
            return this.mCachedVolumeDefault;
        }
        ArrayList<MediaSessionRecord> records = getPriorityListLocked(true, 0, userId);
        int size = records.size();
        for (int i = 0; i < size; i++) {
            MediaSessionRecord record = records.get(i);
            if (record.isPlaybackActive(false)) {
                this.mCachedVolumeDefault = record;
                return record;
            }
        }
        return null;
    }

    public MediaSessionRecord getDefaultRemoteSession(int userId) {
        ArrayList<MediaSessionRecord> records = getPriorityListLocked(true, 0, userId);
        int size = records.size();
        for (int i = 0; i < size; i++) {
            MediaSessionRecord record = records.get(i);
            if (record.getPlaybackType() == 2) {
                return record;
            }
        }
        return null;
    }

    public boolean isGlobalPriorityActive() {
        if (this.mGlobalPrioritySession == null) {
            return false;
        }
        return this.mGlobalPrioritySession.isActive();
    }

    public void dump(PrintWriter pw, String prefix) {
        ArrayList<MediaSessionRecord> sortedSessions = getPriorityListLocked(false, 0, -1);
        int count = sortedSessions.size();
        pw.println(prefix + "Global priority session is " + this.mGlobalPrioritySession);
        pw.println(prefix + "Sessions Stack - have " + count + " sessions:");
        String indent = prefix + "  ";
        for (int i = 0; i < count; i++) {
            MediaSessionRecord record = sortedSessions.get(i);
            record.dump(pw, indent);
            pw.println();
        }
    }

    private ArrayList<MediaSessionRecord> getPriorityListLocked(boolean activeOnly, int withFlags, int userId) {
        ArrayList<MediaSessionRecord> result = new ArrayList<>();
        int lastLocalIndex = 0;
        int lastActiveIndex = 0;
        int lastPublishedIndex = 0;
        int size = this.mSessions.size();
        for (int i = 0; i < size; i++) {
            MediaSessionRecord session = this.mSessions.get(i);
            if ((userId == -1 || userId == session.getUserId()) && (session.getFlags() & ((long) withFlags)) == withFlags) {
                if (!session.isActive()) {
                    if (!activeOnly) {
                        result.add(session);
                    }
                } else if (session.isSystemPriority()) {
                    result.add(0, session);
                    lastLocalIndex++;
                    lastActiveIndex++;
                    lastPublishedIndex++;
                } else if (session.isPlaybackActive(true)) {
                    result.add(lastLocalIndex, session);
                    lastLocalIndex++;
                    lastActiveIndex++;
                    lastPublishedIndex++;
                } else {
                    result.add(lastPublishedIndex, session);
                    lastPublishedIndex++;
                }
            }
        }
        return result;
    }

    private boolean shouldUpdatePriority(int oldState, int newState) {
        if (containsState(newState, ALWAYS_PRIORITY_STATES)) {
            return true;
        }
        return !containsState(oldState, TRANSITION_PRIORITY_STATES) && containsState(newState, TRANSITION_PRIORITY_STATES);
    }

    private boolean containsState(int state, int[] states) {
        for (int i : states) {
            if (i == state) {
                return true;
            }
        }
        return false;
    }

    private void clearCache() {
        this.mCachedDefault = null;
        this.mCachedVolumeDefault = null;
        this.mCachedButtonReceiver = null;
        this.mCachedActiveList = null;
        this.mCachedTransportControlList = null;
    }
}
