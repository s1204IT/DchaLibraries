package android.support.v17.leanback.widget;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v4.util.LruCache;
import android.util.SparseArray;
import android.view.View;
import java.util.Map;

class ViewsStateBundle {
    private LruCache<String, SparseArray<Parcelable>> mChildStates;
    private int mSavePolicy = 0;
    private int mLimitNumber = 100;

    public void clear() {
        if (this.mChildStates != null) {
            this.mChildStates.evictAll();
        }
    }

    public void remove(int id) {
        if (this.mChildStates != null && this.mChildStates.size() != 0) {
            this.mChildStates.remove(getSaveStatesKey(id));
        }
    }

    public final Bundle saveAsBundle() {
        if (this.mChildStates == null || this.mChildStates.size() == 0) {
            return null;
        }
        Map<String, SparseArray<Parcelable>> snapshot = this.mChildStates.snapshot();
        Bundle bundle = new Bundle();
        for (Map.Entry<String, SparseArray<Parcelable>> e : snapshot.entrySet()) {
            bundle.putSparseParcelableArray(e.getKey(), e.getValue());
        }
        return bundle;
    }

    public final void loadFromBundle(Bundle savedBundle) {
        if (this.mChildStates != null && savedBundle != null) {
            this.mChildStates.evictAll();
            for (String key : savedBundle.keySet()) {
                this.mChildStates.put(key, savedBundle.getSparseParcelableArray(key));
            }
        }
    }

    public final void loadView(View view, int id) {
        if (this.mChildStates != null) {
            String key = getSaveStatesKey(id);
            SparseArray<Parcelable> container = this.mChildStates.remove(key);
            if (container != null) {
                view.restoreHierarchyState(container);
            }
        }
    }

    protected final void saveViewUnchecked(View view, int id) {
        if (this.mChildStates != null) {
            String key = getSaveStatesKey(id);
            SparseArray<Parcelable> container = new SparseArray<>();
            view.saveHierarchyState(container);
            this.mChildStates.put(key, container);
        }
    }

    public final Bundle saveOnScreenView(Bundle bundle, View view, int id) {
        if (this.mSavePolicy != 0) {
            String key = getSaveStatesKey(id);
            SparseArray<Parcelable> container = new SparseArray<>();
            view.saveHierarchyState(container);
            if (bundle == null) {
                bundle = new Bundle();
            }
            bundle.putSparseParcelableArray(key, container);
        }
        return bundle;
    }

    public final void saveOffscreenView(View view, int id) {
        switch (this.mSavePolicy) {
            case DialogFragment.STYLE_NO_TITLE:
                remove(id);
                break;
            case DialogFragment.STYLE_NO_FRAME:
            case DialogFragment.STYLE_NO_INPUT:
                saveViewUnchecked(view, id);
                break;
        }
    }

    static String getSaveStatesKey(int id) {
        return Integer.toString(id);
    }
}
