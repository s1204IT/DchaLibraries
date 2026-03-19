package com.mediatek.gallerybasic.gl;

public interface MGLView {
    void addComponent(Object obj);

    void doDraw(MGLCanvas mGLCanvas, int i, int i2);

    void doLayout(boolean z, int i, int i2, int i3, int i4);

    Object getComponent();

    void removeComponent(Object obj);
}
