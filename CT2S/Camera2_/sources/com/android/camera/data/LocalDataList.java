package com.android.camera.data;

import android.net.Uri;
import com.android.camera.debug.Log;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class LocalDataList {
    private static final Log.Tag TAG = new Log.Tag("LocalDataList");
    private final LinkedList<LocalData> mList = new LinkedList<>();
    private final HashMap<Uri, LocalData> mUriMap = new HashMap<>();

    private static class UriWrapper {
        private final Uri mUri;

        public UriWrapper(Uri uri) {
            this.mUri = uri;
        }

        public boolean equals(Object o) {
            if (o instanceof LocalData) {
                return this.mUri.equals(((LocalData) o).getUri());
            }
            return false;
        }
    }

    public LocalData get(int index) {
        return this.mList.get(index);
    }

    public synchronized LocalData remove(int index) {
        LocalData removedItem;
        try {
            removedItem = this.mList.remove(index);
            this.mUriMap.remove(removedItem);
        } catch (IndexOutOfBoundsException ex) {
            Log.w(TAG, "Could not remove item. Not found: " + index, ex);
            removedItem = null;
        }
        return removedItem;
    }

    public LocalData get(Uri uri) {
        return this.mUriMap.get(uri);
    }

    public void set(int pos, LocalData data) {
        this.mList.set(pos, data);
        this.mUriMap.put(data.getUri(), data);
    }

    public void add(LocalData data) {
        this.mList.add(data);
        this.mUriMap.put(data.getUri(), data);
    }

    public void add(int pos, LocalData data) {
        this.mList.add(pos, data);
        this.mUriMap.put(data.getUri(), data);
    }

    public void addAll(List<LocalData> localDataList) {
        for (LocalData localData : localDataList) {
            add(localData);
        }
    }

    public int size() {
        return this.mList.size();
    }

    public void sort(Comparator<LocalData> comparator) {
        Collections.sort(this.mList, comparator);
    }

    public int indexOf(Uri uri) {
        if (this.mUriMap.containsKey(uri)) {
            return this.mList.indexOf(new UriWrapper(uri));
        }
        return -1;
    }
}
