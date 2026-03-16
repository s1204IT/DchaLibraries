package com.android.systemui.recents.views;

import android.content.Context;
import java.util.Iterator;
import java.util.LinkedList;

public class ViewPool<V, T> {
    Context mContext;
    LinkedList<V> mPool = new LinkedList<>();
    ViewPoolConsumer<V, T> mViewCreator;

    public interface ViewPoolConsumer<V, T> {
        V createView(Context context);

        boolean hasPreferredData(V v, T t);

        void prepareViewToEnterPool(V v);

        void prepareViewToLeavePool(V v, T t, boolean z);
    }

    public ViewPool(Context context, ViewPoolConsumer<V, T> viewCreator) {
        this.mContext = context;
        this.mViewCreator = viewCreator;
    }

    void returnViewToPool(V v) {
        this.mViewCreator.prepareViewToEnterPool(v);
        this.mPool.push(v);
    }

    V pickUpViewFromPool(T t, T t2) {
        V vPop = null;
        boolean z = false;
        if (this.mPool.isEmpty()) {
            vPop = this.mViewCreator.createView(this.mContext);
            z = true;
        } else {
            Iterator<V> it = this.mPool.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                V next = it.next();
                if (this.mViewCreator.hasPreferredData(next, t)) {
                    vPop = next;
                    it.remove();
                    break;
                }
            }
            if (vPop == null) {
                vPop = this.mPool.pop();
            }
        }
        this.mViewCreator.prepareViewToLeavePool(vPop, t2, z);
        return vPop;
    }

    Iterator<V> poolViewIterator() {
        if (this.mPool != null) {
            return this.mPool.iterator();
        }
        return null;
    }
}
