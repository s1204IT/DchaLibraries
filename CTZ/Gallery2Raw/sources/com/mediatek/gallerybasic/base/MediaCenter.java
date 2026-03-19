package com.mediatek.gallerybasic.base;

import android.content.Context;
import android.util.SparseIntArray;
import com.mediatek.gallerybasic.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class MediaCenter {
    private static final String TAG = "MtkGallery2/MediaCenter";
    private int mHighestPriority;
    private LinkedHashMap<Integer, MediaMember> mCreatorMap = new LinkedHashMap<>();
    private LinkedHashMap<Integer, Layer> mLayerMap = new LinkedHashMap<>();

    public synchronized void registerMedias(ArrayList<MediaMember> arrayList) {
        Log.d(TAG, "<registerMedias> clear all at first");
        this.mCreatorMap.clear();
        this.mHighestPriority = Integer.MIN_VALUE;
        int size = arrayList.size();
        for (MediaMember mediaMember : arrayList) {
            Log.d(TAG, "<registerMedias> put member = " + mediaMember + ", type = " + size + ", priority = " + mediaMember.getPriority());
            int i = size + (-1);
            mediaMember.setType(size);
            this.mCreatorMap.put(Integer.valueOf(mediaMember.getType()), mediaMember);
            if (mediaMember.getPriority() > this.mHighestPriority) {
                this.mHighestPriority = mediaMember.getPriority();
            }
            size = i;
        }
    }

    public synchronized int getMemberCount() {
        return this.mCreatorMap.size();
    }

    public ExtItem getItem(MediaData mediaData) {
        MediaMember mainMember = getMainMember(mediaData);
        if (mainMember == null) {
            return null;
        }
        return mainMember.getItem(mediaData);
    }

    public ExtItem getRealItem(MediaData mediaData, int i) {
        MediaMember nextMember = getNextMember(mediaData, i);
        if (nextMember == null) {
            return null;
        }
        return nextMember.getItem(mediaData);
    }

    public Player getPlayer(MediaData mediaData, ThumbType thumbType) {
        MediaMember mainMember = getMainMember(mediaData);
        if (mainMember == null) {
            return null;
        }
        return mainMember.getPlayer(mediaData, thumbType);
    }

    public Player getRealPlayer(MediaData mediaData, ThumbType thumbType, int i) {
        MediaMember nextMember = getNextMember(mediaData, i);
        if (nextMember == null) {
            return null;
        }
        return nextMember.getPlayer(mediaData, thumbType);
    }

    public Generator getGenerator(MediaData mediaData) {
        MediaMember mainMember = getMainMember(mediaData);
        if (mainMember == null) {
            return null;
        }
        return mainMember.getGenerator();
    }

    public Generator getRealGenerator(MediaData mediaData, int i) {
        MediaMember nextMember = getNextMember(mediaData, i);
        if (nextMember == null) {
            return null;
        }
        return nextMember.getGenerator();
    }

    public Layer getLayer(Context context, MediaData mediaData) {
        MediaMember mainMember = getMainMember(mediaData);
        if (mainMember == null) {
            return null;
        }
        if (mainMember.isShelled()) {
            return this.mLayerMap.get(Integer.valueOf(mainMember.getType()));
        }
        return getLayerForUnshelledMembers(context, mediaData);
    }

    public Layer getRealLayer(Context context, MediaData mediaData, int i) {
        MediaMember nextMember = getNextMember(mediaData, i);
        if (nextMember == null) {
            return null;
        }
        if (nextMember.isShelled()) {
            return this.mLayerMap.get(Integer.valueOf(nextMember.getType()));
        }
        return getLayerForUnshelledMembers(context, mediaData);
    }

    public final synchronized LinkedHashMap<Integer, Layer> getAllLayer() {
        this.mLayerMap = new LinkedHashMap<>();
        Iterator<Map.Entry<Integer, MediaMember>> it = this.mCreatorMap.entrySet().iterator();
        while (it.hasNext()) {
            MediaMember value = it.next().getValue();
            this.mLayerMap.put(Integer.valueOf(value.getType()), value.getLayer());
        }
        return this.mLayerMap;
    }

    private synchronized MediaMember getMainMember(MediaData mediaData) {
        if (!mediaData.mediaType.isValid()) {
            initMediaType(mediaData);
        }
        return this.mCreatorMap.get(Integer.valueOf(mediaData.mediaType.getMainType()));
    }

    private synchronized MediaMember getNextMember(MediaData mediaData, int i) {
        if (!mediaData.mediaType.isValid()) {
            initMediaType(mediaData);
        }
        int[] allTypes = mediaData.mediaType.getAllTypes();
        for (int i2 = 0; i2 < allTypes.length; i2++) {
            if (allTypes[i2] == i && i2 < allTypes.length - 1) {
                return this.mCreatorMap.get(Integer.valueOf(allTypes[i2 + 1]));
            }
        }
        return null;
    }

    private void initMediaType(MediaData mediaData) {
        SparseIntArray sparseIntArray = new SparseIntArray();
        SparseIntArray sparseIntArray2 = new SparseIntArray();
        for (Map.Entry<Integer, MediaMember> entry : this.mCreatorMap.entrySet()) {
            if (entry.getValue().isMatching(mediaData)) {
                if (entry.getValue().isShelled()) {
                    sparseIntArray.put(entry.getValue().getPriority(), entry.getKey().intValue());
                } else {
                    sparseIntArray2.put(entry.getValue().getPriority(), entry.getKey().intValue());
                }
            }
        }
        for (int i = 0; i < sparseIntArray.size(); i++) {
            mediaData.mediaType.addType(sparseIntArray.keyAt(i) + this.mHighestPriority, sparseIntArray.valueAt(i));
        }
        for (int i2 = 0; i2 < sparseIntArray2.size(); i2++) {
            mediaData.mediaType.addType(sparseIntArray2.keyAt(i2), sparseIntArray2.valueAt(i2));
        }
    }

    private Layer getLayerForUnshelledMembers(Context context, MediaData mediaData) {
        ArrayList<MediaMember> arrayList = new ArrayList();
        synchronized (this) {
            if (!mediaData.mediaType.isValid()) {
                initMediaType(mediaData);
            }
            for (int i : mediaData.mediaType.getAllTypes()) {
                if (!this.mCreatorMap.get(Integer.valueOf(i)).isShelled()) {
                    arrayList.add(this.mCreatorMap.get(Integer.valueOf(i)));
                }
            }
        }
        if (arrayList.size() == 0) {
            return null;
        }
        ArrayList arrayList2 = new ArrayList();
        for (MediaMember mediaMember : arrayList) {
            if (this.mLayerMap.get(Integer.valueOf(mediaMember.getType())) != null) {
                arrayList2.add(this.mLayerMap.get(Integer.valueOf(mediaMember.getType())));
            }
        }
        if (arrayList2.size() == 1) {
            return (Layer) arrayList2.get(0);
        }
        if (arrayList2.size() > 1) {
            return new ComboLayer(context, arrayList2);
        }
        return null;
    }
}
