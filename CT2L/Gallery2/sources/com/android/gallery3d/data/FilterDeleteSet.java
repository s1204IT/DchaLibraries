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

        public Request(int type, Path path, int indexHint) {
            this.type = type;
            this.path = path;
            this.indexHint = indexHint;
        }
    }

    private static class Deletion {
        int index;
        Path path;

        public Deletion(Path path, int index) {
            this.path = path;
            this.index = index;
        }
    }

    public FilterDeleteSet(Path path, MediaSet baseSet) {
        super(path, -1L);
        this.mRequests = new ArrayList<>();
        this.mCurrent = new ArrayList<>();
        this.mBaseSet = baseSet;
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
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        if (count <= 0) {
            return new ArrayList<>();
        }
        int end = (start + count) - 1;
        int n = this.mCurrent.size();
        int i = 0;
        while (i < n) {
            Deletion d = this.mCurrent.get(i);
            if (d.index - i > start) {
                break;
            }
            i++;
        }
        int j = i;
        while (j < n) {
            Deletion d2 = this.mCurrent.get(j);
            if (d2.index - j > end) {
                break;
            }
            j++;
        }
        ArrayList<MediaItem> base = this.mBaseSet.getMediaItem(start + i, (j - i) + count);
        for (int m = j - 1; m >= i; m--) {
            Deletion d3 = this.mCurrent.get(m);
            int k = d3.index - (start + i);
            base.remove(k);
        }
        return base;
    }

    @Override
    public long reload() {
        boolean newData = this.mBaseSet.reload() > this.mDataVersion;
        synchronized (this.mRequests) {
            if (!newData) {
                if (this.mRequests.isEmpty()) {
                    return this.mDataVersion;
                }
            }
            for (int i = 0; i < this.mRequests.size(); i++) {
                Request r = this.mRequests.get(i);
                switch (r.type) {
                    case 1:
                        int n = this.mCurrent.size();
                        int j = 0;
                        while (j < n && this.mCurrent.get(j).path != r.path) {
                            j++;
                        }
                        if (j == n) {
                            this.mCurrent.add(new Deletion(r.path, r.indexHint));
                        }
                        break;
                    case 2:
                        int n2 = this.mCurrent.size();
                        int j2 = 0;
                        while (true) {
                            if (j2 >= n2) {
                            }
                            if (this.mCurrent.get(j2).path != r.path) {
                                j2++;
                            } else {
                                this.mCurrent.remove(j2);
                            }
                            break;
                            break;
                        }
                        break;
                    case 3:
                        this.mCurrent.clear();
                        break;
                }
            }
            this.mRequests.clear();
            if (!this.mCurrent.isEmpty()) {
                int minIndex = this.mCurrent.get(0).index;
                int maxIndex = minIndex;
                for (int i2 = 1; i2 < this.mCurrent.size(); i2++) {
                    Deletion d = this.mCurrent.get(i2);
                    minIndex = Math.min(d.index, minIndex);
                    maxIndex = Math.max(d.index, maxIndex);
                }
                int n3 = this.mBaseSet.getMediaItemCount();
                int from = Math.max(minIndex - 5, 0);
                int to = Math.min(maxIndex + 5, n3);
                ArrayList<MediaItem> items = this.mBaseSet.getMediaItem(from, to - from);
                ArrayList<Deletion> result = new ArrayList<>();
                for (int i3 = 0; i3 < items.size(); i3++) {
                    MediaItem item = items.get(i3);
                    if (item != null) {
                        Path p = item.getPath();
                        int j3 = 0;
                        while (true) {
                            if (j3 < this.mCurrent.size()) {
                                Deletion d2 = this.mCurrent.get(j3);
                                if (d2.path != p) {
                                    j3++;
                                } else {
                                    d2.index = from + i3;
                                    result.add(d2);
                                    this.mCurrent.remove(j3);
                                }
                            }
                        }
                    }
                }
                this.mCurrent = result;
            }
            this.mDataVersion = nextVersionNumber();
            return this.mDataVersion;
        }
    }

    private void sendRequest(int type, Path path, int indexHint) {
        Request r = new Request(type, path, indexHint);
        synchronized (this.mRequests) {
            this.mRequests.add(r);
        }
        notifyContentChanged();
    }

    @Override
    public void onContentDirty() {
        notifyContentChanged();
    }

    public void addDeletion(Path path, int indexHint) {
        sendRequest(1, path, indexHint);
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
}
