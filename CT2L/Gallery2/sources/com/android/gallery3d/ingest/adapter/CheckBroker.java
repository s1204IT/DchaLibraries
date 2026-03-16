package com.android.gallery3d.ingest.adapter;

import android.annotation.TargetApi;
import java.util.ArrayList;
import java.util.Collection;

@TargetApi(12)
public abstract class CheckBroker {
    private Collection<OnCheckedChangedListener> mListeners = new ArrayList();

    public interface OnCheckedChangedListener {
        void onBulkCheckedChanged();

        void onCheckedChanged(int i, boolean z);
    }

    public abstract boolean isItemChecked(int i);

    public abstract void setItemChecked(int i, boolean z);

    public void onCheckedChange(int position, boolean checked) {
        if (isItemChecked(position) != checked) {
            for (OnCheckedChangedListener l : this.mListeners) {
                l.onCheckedChanged(position, checked);
            }
        }
    }

    public void onBulkCheckedChange() {
        for (OnCheckedChangedListener l : this.mListeners) {
            l.onBulkCheckedChanged();
        }
    }

    public void registerOnCheckedChangeListener(OnCheckedChangedListener l) {
        this.mListeners.add(l);
    }

    public void unregisterOnCheckedChangeListener(OnCheckedChangedListener l) {
        this.mListeners.remove(l);
    }
}
