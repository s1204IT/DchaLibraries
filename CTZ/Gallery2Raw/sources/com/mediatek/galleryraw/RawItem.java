package com.mediatek.galleryraw;

import android.content.Context;
import com.mediatek.gallerybasic.base.ExtItem;
import com.mediatek.gallerybasic.base.MediaData;
import java.util.ArrayList;

class RawItem extends ExtItem {
    public RawItem(Context context, MediaData mediaData) {
        super(context, mediaData);
    }

    public RawItem(MediaData mediaData) {
        super(mediaData);
    }

    @Override
    public ArrayList<ExtItem.SupportOperation> getNotSupportedOperations() {
        ArrayList<ExtItem.SupportOperation> arrayList = new ArrayList<>();
        arrayList.add(ExtItem.SupportOperation.EDIT);
        return arrayList;
    }
}
