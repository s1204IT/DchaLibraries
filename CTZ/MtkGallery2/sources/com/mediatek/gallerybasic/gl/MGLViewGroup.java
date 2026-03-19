package com.mediatek.gallerybasic.gl;

import java.util.ArrayList;
import java.util.Iterator;

public class MGLViewGroup implements MGLView {
    static final boolean $assertionsDisabled = false;
    private static final String TAG = "MtkGallery2/MGLViewGroup";
    private ArrayList<MGLView> mViews;

    public MGLViewGroup(ArrayList<MGLView> arrayList) {
        this.mViews = arrayList;
    }

    @Override
    public void doDraw(MGLCanvas mGLCanvas, int i, int i2) {
        if (this.mViews == null) {
            return;
        }
        Iterator<MGLView> it = this.mViews.iterator();
        while (it.hasNext()) {
            it.next().doDraw(mGLCanvas, i, i2);
        }
    }

    @Override
    public void doLayout(boolean z, int i, int i2, int i3, int i4) {
        if (this.mViews == null) {
            return;
        }
        Iterator<MGLView> it = this.mViews.iterator();
        while (it.hasNext()) {
            it.next().doLayout(z, i, i2, i3, i4);
        }
    }

    @Override
    public ArrayList<Object> getComponent() {
        ArrayList<Object> arrayList = new ArrayList<>();
        Iterator<MGLView> it = this.mViews.iterator();
        while (it.hasNext()) {
            Object component = it.next().getComponent();
            if (component != null) {
                arrayList.add(component);
            }
        }
        return arrayList;
    }

    @Override
    public void addComponent(Object obj) {
    }

    @Override
    public void removeComponent(Object obj) {
    }
}
