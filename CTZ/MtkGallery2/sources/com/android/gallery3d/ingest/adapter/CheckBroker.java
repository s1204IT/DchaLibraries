package com.android.gallery3d.ingest.adapter;

import android.annotation.TargetApi;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

@TargetApi(12)
public abstract class CheckBroker {
    private Collection<OnCheckedChangedListener> mListeners = new ArrayList();

    public interface OnCheckedChangedListener {
        void onBulkCheckedChanged();

        void onCheckedChanged(int i, boolean z);
    }

    public abstract boolean isItemChecked(int i);

    public abstract void setItemChecked(int i, boolean z);

    public void onCheckedChange(int i, boolean z) {
        if (isItemChecked(i) != z) {
            Iterator<OnCheckedChangedListener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onCheckedChanged(i, z);
            }
        }
    }

    public void onBulkCheckedChange() {
        Iterator<OnCheckedChangedListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onBulkCheckedChanged();
        }
    }

    public void registerOnCheckedChangeListener(OnCheckedChangedListener onCheckedChangedListener) {
        this.mListeners.add(onCheckedChangedListener);
    }

    public void unregisterOnCheckedChangeListener(OnCheckedChangedListener onCheckedChangedListener) {
        this.mListeners.remove(onCheckedChangedListener);
    }
}
