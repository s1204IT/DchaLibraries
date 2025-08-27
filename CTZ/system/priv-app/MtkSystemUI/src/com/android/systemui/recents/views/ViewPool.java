package com.android.systemui.recents.views;

import android.content.Context;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/* loaded from: classes.dex */
public class ViewPool<V, T> {
    Context mContext;
    LinkedList<V> mPool = new LinkedList<>();
    ViewPoolConsumer<V, T> mViewCreator;

    public interface ViewPoolConsumer<V, T> {
        V createView(Context context);

        boolean hasPreferredData(V v, T t);

        void onPickUpViewFromPool(V v, T t, boolean z);

        void onReturnViewToPool(V v);
    }

    public ViewPool(Context context, ViewPoolConsumer<V, T> viewPoolConsumer) {
        this.mContext = context;
        this.mViewCreator = viewPoolConsumer;
    }

    void returnViewToPool(V v) {
        this.mViewCreator.onReturnViewToPool(v);
        this.mPool.push(v);
    }

    V pickUpViewFromPool(T t, T t2) {
        V vPop;
        boolean z = false;
        if (this.mPool.isEmpty()) {
            vPop = this.mViewCreator.createView(this.mContext);
            z = true;
        } else {
            Iterator<V> it = this.mPool.iterator();
            while (true) {
                if (it.hasNext()) {
                    V next = it.next();
                    if (this.mViewCreator.hasPreferredData(next, t)) {
                        it.remove();
                        vPop = next;
                        break;
                    }
                } else {
                    vPop = null;
                    break;
                }
            }
            if (vPop == null) {
                vPop = this.mPool.pop();
            }
        }
        this.mViewCreator.onPickUpViewFromPool(vPop, t2, z);
        return vPop;
    }

    List<V> getViews() {
        return this.mPool;
    }
}
