package com.android.gallery3d.data;

import java.util.ArrayList;

public class FilterDeleteSet extends MediaSet implements ContentListener {
    private final MediaSet mBaseSet;
    private ArrayList<Deletion> mCurrent;
    private ArrayList<Request> mRequests;

    private static class Request {
        int indexHint;
        Path path;
        int type;

        public Request(int i, Path path, int i2) {
            this.type = i;
            this.path = path;
            this.indexHint = i2;
        }
    }

    private static class Deletion {
        int index;
        Path path;

        public Deletion(Path path, int i) {
            this.path = path;
            this.index = i;
        }
    }

    public FilterDeleteSet(Path path, MediaSet mediaSet) {
        super(path, -1L);
        this.mRequests = new ArrayList<>();
        this.mCurrent = new ArrayList<>();
        this.mBaseSet = mediaSet;
        this.mBaseSet.addContentListener(this);
    }

    @Override
    public boolean isCameraRoll() {
        return this.mBaseSet.isCameraRoll();
    }

    @Override
    public String getName() {
        return this.mBaseSet.getName();
    }

    @Override
    public int getMediaItemCount() {
        return this.mBaseSet.getMediaItemCount() - this.mCurrent.size();
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int i, int i2) {
        if (i2 <= 0) {
            return new ArrayList<>();
        }
        int i3 = (i + i2) - 1;
        int size = this.mCurrent.size();
        int i4 = 0;
        while (i4 < size && this.mCurrent.get(i4).index - i4 <= i) {
            i4++;
        }
        int i5 = i4;
        while (i5 < size && this.mCurrent.get(i5).index - i5 <= i3) {
            i5++;
        }
        int i6 = i + i4;
        ArrayList<MediaItem> mediaItem = this.mBaseSet.getMediaItem(i6, i2 + (i5 - i4));
        for (int i7 = i5 - 1; i7 >= i4; i7--) {
            if (i7 >= this.mCurrent.size()) {
                Log.d("Gallery2/FilterDeleteSet", "<getMediaItem> error! IndexOutOfBoundsException,m:" + i7 + " size:" + this.mCurrent.size());
            } else {
                int i8 = this.mCurrent.get(i7).index - i6;
                if (i8 < mediaItem.size()) {
                    mediaItem.remove(i8);
                }
            }
        }
        return mediaItem;
    }

    @Override
    public long reload() {
        boolean z = this.mBaseSet.reload() > this.mDataVersion;
        synchronized (this.mRequests) {
            if (!z) {
                try {
                    if (this.mRequests.isEmpty()) {
                        return this.mDataVersion;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            int iMin = Math.min(this.mRequests.size(), 1);
            for (int i = 0; i < iMin; i++) {
                Request requestRemove = this.mRequests.remove(i);
                switch (requestRemove.type) {
                    case 1:
                        int size = this.mCurrent.size();
                        int i2 = 0;
                        while (i2 < size && this.mCurrent.get(i2).path != requestRemove.path) {
                            i2++;
                        }
                        if (i2 == size) {
                            this.mCurrent.add(new Deletion(requestRemove.path, requestRemove.indexHint));
                        }
                        break;
                    case 2:
                        int size2 = this.mCurrent.size();
                        int i3 = 0;
                        while (true) {
                            if (i3 < size2) {
                                if (this.mCurrent.get(i3).path != requestRemove.path) {
                                    i3++;
                                } else {
                                    this.mCurrent.remove(i3);
                                }
                            }
                        }
                        break;
                    case 3:
                        this.mCurrent.clear();
                        break;
                }
            }
            if (!this.mRequests.isEmpty()) {
                Log.d("Gallery2/FilterDeleteSet", "<reload> mRequests size = " + this.mRequests.size() + ", notifyContentChanged");
                notifyContentChanged();
            }
            if (!this.mCurrent.isEmpty()) {
                int iMin2 = this.mCurrent.get(0).index;
                int iMax = iMin2;
                for (int i4 = 1; i4 < this.mCurrent.size(); i4++) {
                    Deletion deletion = this.mCurrent.get(i4);
                    iMin2 = Math.min(deletion.index, iMin2);
                    iMax = Math.max(deletion.index, iMax);
                }
                int mediaItemCount = this.mBaseSet.getMediaItemCount();
                int iMax2 = Math.max(iMin2 - 5, 0);
                ArrayList<MediaItem> mediaItem = this.mBaseSet.getMediaItem(iMax2, Math.min(iMax + 5, mediaItemCount) - iMax2);
                ArrayList<Deletion> arrayList = new ArrayList<>();
                for (int i5 = 0; i5 < mediaItem.size(); i5++) {
                    MediaItem mediaItem2 = mediaItem.get(i5);
                    if (mediaItem2 != null) {
                        Path path = mediaItem2.getPath();
                        int i6 = 0;
                        while (true) {
                            if (i6 < this.mCurrent.size()) {
                                Deletion deletion2 = this.mCurrent.get(i6);
                                if (deletion2.path != path) {
                                    i6++;
                                } else {
                                    deletion2.index = iMax2 + i5;
                                    arrayList.add(deletion2);
                                    this.mCurrent.remove(i6);
                                }
                            }
                        }
                    }
                }
                this.mCurrent = arrayList;
            }
            this.mDataVersion = nextVersionNumber();
            return this.mDataVersion;
        }
    }

    private void sendRequest(int i, Path path, int i2) {
        Request request = new Request(i, path, i2);
        synchronized (this.mRequests) {
            this.mRequests.add(request);
        }
        notifyContentChanged();
    }

    @Override
    public void onContentDirty() {
        notifyContentChanged();
    }

    public void addDeletion(Path path, int i) {
        sendRequest(1, path, i);
    }

    public void removeDeletion(Path path) {
        sendRequest(2, path, 0);
    }

    public void clearDeletion() {
        sendRequest(3, null, 0);
    }

    public int getNumberOfDeletions() {
        return this.mCurrent.size();
    }

    public void resetDeletion() {
        if (this.mCurrent != null) {
            this.mCurrent.clear();
        }
    }

    @Override
    public void stopReload() {
        Log.d("Gallery2/FilterDeleteSet", "<stopReload> ......");
        if (this.mBaseSet != null) {
            this.mBaseSet.stopReload();
        }
    }
}
