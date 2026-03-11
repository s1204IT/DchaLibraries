package android.support.v4.media;

import android.annotation.TargetApi;
import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.media.BaseMediaPlayer;
import android.support.v4.media.MediaSession2;
import android.support.v4.util.ArrayMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@TargetApi(19)
class SessionPlaylistAgentImplBase extends MediaPlaylistAgent {

    @VisibleForTesting
    static final int END_OF_PLAYLIST = -1;

    @VisibleForTesting
    static final int NO_VALID_ITEMS = -2;

    @GuardedBy("mLock")
    private PlayItem mCurrent;

    @GuardedBy("mLock")
    private MediaSession2.OnDataSourceMissingHelper mDsmHelper;

    @GuardedBy("mLock")
    private MediaMetadata2 mMetadata;

    @GuardedBy("mLock")
    private BaseMediaPlayer mPlayer;
    private final MyPlayerEventCallback mPlayerCallback;

    @GuardedBy("mLock")
    private int mRepeatMode;
    private final MediaSession2ImplBase mSession;

    @GuardedBy("mLock")
    private int mShuffleMode;
    private final PlayItem mEopPlayItem = new PlayItem(-1, null);
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private ArrayList<MediaItem2> mPlaylist = new ArrayList<>();

    @GuardedBy("mLock")
    private ArrayList<MediaItem2> mShuffledList = new ArrayList<>();

    @GuardedBy("mLock")
    private Map<MediaItem2, DataSourceDesc> mItemDsdMap = new ArrayMap();

    private class MyPlayerEventCallback extends BaseMediaPlayer.PlayerEventCallback {
        private MyPlayerEventCallback() {
        }

        @Override
        public void onCurrentDataSourceChanged(@NonNull BaseMediaPlayer mpb, @Nullable DataSourceDesc dsd) {
            synchronized (SessionPlaylistAgentImplBase.this.mLock) {
                if (SessionPlaylistAgentImplBase.this.mPlayer != mpb) {
                    return;
                }
                if (dsd == null && SessionPlaylistAgentImplBase.this.mCurrent != null) {
                    SessionPlaylistAgentImplBase.this.mCurrent = SessionPlaylistAgentImplBase.this.getNextValidPlayItemLocked(SessionPlaylistAgentImplBase.this.mCurrent.shuffledIdx, 1);
                    SessionPlaylistAgentImplBase.this.updateCurrentIfNeededLocked();
                }
            }
        }
    }

    private class PlayItem {
        public DataSourceDesc dsd;
        public MediaItem2 mediaItem;
        public int shuffledIdx;

        PlayItem(SessionPlaylistAgentImplBase sessionPlaylistAgentImplBase, int shuffledIdx) {
            this(shuffledIdx, null);
        }

        PlayItem(int shuffledIdx, DataSourceDesc dsd) {
            this.shuffledIdx = shuffledIdx;
            if (shuffledIdx >= 0) {
                this.mediaItem = (MediaItem2) SessionPlaylistAgentImplBase.this.mShuffledList.get(shuffledIdx);
                if (dsd == null) {
                    synchronized (SessionPlaylistAgentImplBase.this.mLock) {
                        this.dsd = SessionPlaylistAgentImplBase.this.retrieveDataSourceDescLocked(this.mediaItem);
                    }
                    return;
                }
                this.dsd = dsd;
            }
        }

        boolean isValid() {
            if (this == SessionPlaylistAgentImplBase.this.mEopPlayItem) {
                return true;
            }
            if (this.mediaItem == null || this.dsd == null) {
                return false;
            }
            if (this.mediaItem.getDataSourceDesc() != null && !this.mediaItem.getDataSourceDesc().equals(this.dsd)) {
                return false;
            }
            synchronized (SessionPlaylistAgentImplBase.this.mLock) {
                if (this.shuffledIdx >= SessionPlaylistAgentImplBase.this.mShuffledList.size()) {
                    return false;
                }
                return this.mediaItem == SessionPlaylistAgentImplBase.this.mShuffledList.get(this.shuffledIdx);
            }
        }
    }

    SessionPlaylistAgentImplBase(@NonNull MediaSession2ImplBase session, @NonNull BaseMediaPlayer player) {
        if (session == null) {
            throw new IllegalArgumentException("sessionImpl shouldn't be null");
        }
        if (player == null) {
            throw new IllegalArgumentException("player shouldn't be null");
        }
        this.mSession = session;
        this.mPlayer = player;
        this.mPlayerCallback = new MyPlayerEventCallback();
        this.mPlayer.registerPlayerEventCallback(this.mSession.getCallbackExecutor(), this.mPlayerCallback);
    }

    public void setPlayer(@NonNull BaseMediaPlayer player) {
        if (player == null) {
            throw new IllegalArgumentException("player shouldn't be null");
        }
        synchronized (this.mLock) {
            if (player == this.mPlayer) {
                return;
            }
            this.mPlayer.unregisterPlayerEventCallback(this.mPlayerCallback);
            this.mPlayer = player;
            this.mPlayer.registerPlayerEventCallback(this.mSession.getCallbackExecutor(), this.mPlayerCallback);
            updatePlayerDataSourceLocked();
        }
    }

    public void setOnDataSourceMissingHelper(MediaSession2.OnDataSourceMissingHelper helper) {
        synchronized (this.mLock) {
            this.mDsmHelper = helper;
        }
    }

    public void clearOnDataSourceMissingHelper() {
        synchronized (this.mLock) {
            this.mDsmHelper = null;
        }
    }

    @Override
    @Nullable
    public List<MediaItem2> getPlaylist() {
        List<MediaItem2> listUnmodifiableList;
        synchronized (this.mLock) {
            listUnmodifiableList = Collections.unmodifiableList(this.mPlaylist);
        }
        return listUnmodifiableList;
    }

    @Override
    public void setPlaylist(@NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) {
        if (list == null) {
            throw new IllegalArgumentException("list shouldn't be null");
        }
        synchronized (this.mLock) {
            this.mItemDsdMap.clear();
            this.mPlaylist.clear();
            this.mPlaylist.addAll(list);
            applyShuffleModeLocked();
            this.mMetadata = metadata;
            this.mCurrent = getNextValidPlayItemLocked(-1, 1);
            updatePlayerDataSourceLocked();
        }
        notifyPlaylistChanged();
    }

    @Override
    @Nullable
    public MediaMetadata2 getPlaylistMetadata() {
        MediaMetadata2 mediaMetadata2;
        synchronized (this.mLock) {
            mediaMetadata2 = this.mMetadata;
        }
        return mediaMetadata2;
    }

    @Override
    public void updatePlaylistMetadata(@Nullable MediaMetadata2 metadata) {
        synchronized (this.mLock) {
            if (metadata == this.mMetadata) {
                return;
            }
            this.mMetadata = metadata;
            notifyPlaylistMetadataChanged();
        }
    }

    @Override
    public MediaItem2 getCurrentMediaItem() {
        MediaItem2 mediaItem2;
        synchronized (this.mLock) {
            mediaItem2 = this.mCurrent == null ? null : this.mCurrent.mediaItem;
        }
        return mediaItem2;
    }

    @Override
    public void addPlaylistItem(int index, @NonNull MediaItem2 item) {
        if (item == null) {
            throw new IllegalArgumentException("item shouldn't be null");
        }
        synchronized (this.mLock) {
            int index2 = clamp(index, this.mPlaylist.size());
            this.mPlaylist.add(index2, item);
            if (this.mShuffleMode == 0) {
                this.mShuffledList.add(index2, item);
            } else {
                int shuffledIdx = (int) (Math.random() * ((double) (this.mShuffledList.size() + 1)));
                this.mShuffledList.add(shuffledIdx, item);
            }
            if (!hasValidItem()) {
                this.mCurrent = getNextValidPlayItemLocked(-1, 1);
                updatePlayerDataSourceLocked();
            } else {
                updateCurrentIfNeededLocked();
            }
        }
        notifyPlaylistChanged();
    }

    @Override
    public void removePlaylistItem(@NonNull MediaItem2 item) {
        if (item == null) {
            throw new IllegalArgumentException("item shouldn't be null");
        }
        synchronized (this.mLock) {
            if (this.mPlaylist.remove(item)) {
                this.mShuffledList.remove(item);
                this.mItemDsdMap.remove(item);
                updateCurrentIfNeededLocked();
                notifyPlaylistChanged();
            }
        }
    }

    @Override
    public void replacePlaylistItem(int index, @NonNull MediaItem2 item) {
        if (item == null) {
            throw new IllegalArgumentException("item shouldn't be null");
        }
        synchronized (this.mLock) {
            if (this.mPlaylist.size() <= 0) {
                return;
            }
            int index2 = clamp(index, this.mPlaylist.size() - 1);
            int shuffledIdx = this.mShuffledList.indexOf(this.mPlaylist.get(index2));
            this.mItemDsdMap.remove(this.mShuffledList.get(shuffledIdx));
            this.mShuffledList.set(shuffledIdx, item);
            this.mPlaylist.set(index2, item);
            if (!hasValidItem()) {
                this.mCurrent = getNextValidPlayItemLocked(-1, 1);
                updatePlayerDataSourceLocked();
            } else {
                updateCurrentIfNeededLocked();
            }
            notifyPlaylistChanged();
        }
    }

    @Override
    public void skipToPlaylistItem(@NonNull MediaItem2 item) {
        if (item == null) {
            throw new IllegalArgumentException("item shouldn't be null");
        }
        synchronized (this.mLock) {
            if (hasValidItem() && !item.equals(this.mCurrent.mediaItem)) {
                int shuffledIdx = this.mShuffledList.indexOf(item);
                if (shuffledIdx < 0) {
                    return;
                }
                this.mCurrent = new PlayItem(this, shuffledIdx);
                updateCurrentIfNeededLocked();
            }
        }
    }

    @Override
    public void skipToPreviousItem() {
        synchronized (this.mLock) {
            if (hasValidItem()) {
                PlayItem prev = getNextValidPlayItemLocked(this.mCurrent.shuffledIdx, -1);
                if (prev != this.mEopPlayItem) {
                    this.mCurrent = prev;
                }
                updateCurrentIfNeededLocked();
            }
        }
    }

    @Override
    public void skipToNextItem() {
        synchronized (this.mLock) {
            if (hasValidItem() && this.mCurrent != this.mEopPlayItem) {
                PlayItem next = getNextValidPlayItemLocked(this.mCurrent.shuffledIdx, 1);
                if (next != this.mEopPlayItem) {
                    this.mCurrent = next;
                }
                updateCurrentIfNeededLocked();
            }
        }
    }

    @Override
    public int getRepeatMode() {
        int i;
        synchronized (this.mLock) {
            i = this.mRepeatMode;
        }
        return i;
    }

    @Override
    public void setRepeatMode(int repeatMode) {
        if (repeatMode < 0 || repeatMode > 3) {
            return;
        }
        synchronized (this.mLock) {
            if (this.mRepeatMode == repeatMode) {
                return;
            }
            this.mRepeatMode = repeatMode;
            switch (repeatMode) {
                case 0:
                    this.mPlayer.loopCurrent(false);
                    break;
                case 1:
                    if (this.mCurrent != null && this.mCurrent != this.mEopPlayItem) {
                        this.mPlayer.loopCurrent(true);
                    }
                    break;
                case 2:
                case 3:
                    if (this.mCurrent == this.mEopPlayItem) {
                        this.mCurrent = getNextValidPlayItemLocked(-1, 1);
                        updatePlayerDataSourceLocked();
                    }
                    this.mPlayer.loopCurrent(false);
                    break;
            }
            notifyRepeatModeChanged();
        }
    }

    @Override
    public int getShuffleMode() {
        int i;
        synchronized (this.mLock) {
            i = this.mShuffleMode;
        }
        return i;
    }

    @Override
    public void setShuffleMode(int shuffleMode) {
        if (shuffleMode < 0 || shuffleMode > 2) {
            return;
        }
        synchronized (this.mLock) {
            if (this.mShuffleMode == shuffleMode) {
                return;
            }
            this.mShuffleMode = shuffleMode;
            applyShuffleModeLocked();
            updateCurrentIfNeededLocked();
            notifyShuffleModeChanged();
        }
    }

    @Override
    public MediaItem2 getMediaItem(DataSourceDesc dsd) {
        return null;
    }

    @VisibleForTesting
    int getCurShuffledIndex() {
        int i;
        synchronized (this.mLock) {
            i = hasValidItem() ? this.mCurrent.shuffledIdx : -2;
        }
        return i;
    }

    private boolean hasValidItem() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mCurrent != null;
        }
        return z;
    }

    public DataSourceDesc retrieveDataSourceDescLocked(MediaItem2 item) {
        DataSourceDesc dsd = item.getDataSourceDesc();
        if (dsd != null) {
            this.mItemDsdMap.put(item, dsd);
            return dsd;
        }
        DataSourceDesc dsd2 = this.mItemDsdMap.get(item);
        if (dsd2 != null) {
            return dsd2;
        }
        MediaSession2.OnDataSourceMissingHelper helper = this.mDsmHelper;
        if (helper != null && (dsd2 = helper.onDataSourceMissing(this.mSession.getInstance(), item)) != null) {
            this.mItemDsdMap.put(item, dsd2);
        }
        return dsd2;
    }

    public PlayItem getNextValidPlayItemLocked(int curShuffledIdx, int direction) {
        int size = this.mPlaylist.size();
        if (curShuffledIdx == -1) {
            curShuffledIdx = direction <= 0 ? size : -1;
        }
        int curShuffledIdx2 = curShuffledIdx;
        for (int curShuffledIdx3 = 0; curShuffledIdx3 < size; curShuffledIdx3++) {
            curShuffledIdx2 += direction;
            if (curShuffledIdx2 < 0 || curShuffledIdx2 >= this.mPlaylist.size()) {
                if (this.mRepeatMode == 0) {
                    if (curShuffledIdx3 == size - 1) {
                        return null;
                    }
                    return this.mEopPlayItem;
                }
                curShuffledIdx2 = curShuffledIdx2 < 0 ? this.mPlaylist.size() - 1 : 0;
            }
            DataSourceDesc dsd = retrieveDataSourceDescLocked(this.mShuffledList.get(curShuffledIdx2));
            if (dsd != null) {
                return new PlayItem(curShuffledIdx2, dsd);
            }
        }
        return null;
    }

    public void updateCurrentIfNeededLocked() {
        if (!hasValidItem() || this.mCurrent.isValid()) {
            return;
        }
        int shuffledIdx = this.mShuffledList.indexOf(this.mCurrent.mediaItem);
        if (shuffledIdx >= 0) {
            this.mCurrent.shuffledIdx = shuffledIdx;
            return;
        }
        if (this.mCurrent.shuffledIdx >= this.mShuffledList.size()) {
            this.mCurrent = getNextValidPlayItemLocked(this.mShuffledList.size() - 1, 1);
        } else {
            this.mCurrent.mediaItem = this.mShuffledList.get(this.mCurrent.shuffledIdx);
            if (retrieveDataSourceDescLocked(this.mCurrent.mediaItem) == null) {
                this.mCurrent = getNextValidPlayItemLocked(this.mCurrent.shuffledIdx, 1);
            }
        }
        updatePlayerDataSourceLocked();
    }

    private void updatePlayerDataSourceLocked() {
        if (this.mCurrent != null && this.mCurrent != this.mEopPlayItem && this.mPlayer.getCurrentDataSource() != this.mCurrent.dsd) {
            this.mPlayer.setDataSource(this.mCurrent.dsd);
            this.mPlayer.loopCurrent(this.mRepeatMode == 1);
        }
    }

    private void applyShuffleModeLocked() {
        this.mShuffledList.clear();
        this.mShuffledList.addAll(this.mPlaylist);
        if (this.mShuffleMode == 1 || this.mShuffleMode == 2) {
            Collections.shuffle(this.mShuffledList);
        }
    }

    private static int clamp(int value, int size) {
        if (value < 0) {
            return 0;
        }
        return value > size ? size : value;
    }
}
