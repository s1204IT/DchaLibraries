package com.android.gallery3d.ui;

import android.graphics.Rect;
import android.graphics.RectF;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import com.android.gallery3d.anim.Animation;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.RawTexture;
import com.android.gallery3d.ui.AlbumSlotRenderer;
import java.util.ArrayList;

public class PhotoFallbackEffect extends Animation implements AlbumSlotRenderer.SlotFilter {
    private static final int ANIM_DURATION = 300;
    private static final Interpolator ANIM_INTERPOLATE = new DecelerateInterpolator(1.5f);
    private PositionProvider mPositionProvider;
    private float mProgress;
    private RectF mSource = new RectF();
    private RectF mTarget = new RectF();
    private ArrayList<Entry> mList = new ArrayList<>();

    public interface PositionProvider {
        int getItemIndex(Path path);

        Rect getPosition(int i);
    }

    public static class Entry {
        public Rect dest;
        public int index;
        public Path path;
        public Rect source;
        public RawTexture texture;

        public Entry(Path path, Rect rect, RawTexture rawTexture) {
            this.path = path;
            this.source = rect;
            this.texture = rawTexture;
        }
    }

    public PhotoFallbackEffect() {
        setDuration(ANIM_DURATION);
        setInterpolator(ANIM_INTERPOLATE);
    }

    public void addEntry(Path path, Rect rect, RawTexture rawTexture) {
        this.mList.add(new Entry(path, rect, rawTexture));
    }

    public Entry getEntry(Path path) {
        int size = this.mList.size();
        for (int i = 0; i < size; i++) {
            Entry entry = this.mList.get(i);
            if (entry.path == path) {
                return entry;
            }
        }
        return null;
    }

    public boolean draw(GLCanvas gLCanvas) {
        boolean zCalculate = calculate(AnimationTime.get());
        int size = this.mList.size();
        for (int i = 0; i < size; i++) {
            Entry entry = this.mList.get(i);
            if (entry.index >= 0) {
                entry.dest = this.mPositionProvider.getPosition(entry.index);
                drawEntry(gLCanvas, entry);
            }
        }
        return zCalculate;
    }

    private void drawEntry(GLCanvas gLCanvas, Entry entry) {
        if (entry.texture.isLoaded()) {
            int width = entry.texture.getWidth();
            int height = entry.texture.getHeight();
            Rect rect = entry.source;
            Rect rect2 = entry.dest;
            float f = this.mProgress;
            float f2 = 1.0f - f;
            float fHeight = ((rect2.height() / Math.min(rect.width(), rect.height())) * f) + (1.0f * f2);
            float fCenterX = (rect2.centerX() * f) + (rect.centerX() * f2);
            float fCenterY = (rect2.centerY() * f) + (rect.centerY() * f2);
            float fHeight2 = rect.height() * fHeight;
            float fWidth = rect.width() * fHeight;
            if (width > height) {
                float f3 = fHeight2 / 2.0f;
                float f4 = fCenterX - f3;
                float f5 = fCenterY - f3;
                float f6 = fCenterX + f3;
                float f7 = fCenterY + f3;
                this.mTarget.set(f4, f5, f6, f7);
                float f8 = (width - height) / 2;
                float f9 = (width + height) / 2;
                float f10 = height;
                this.mSource.set(f8, 0.0f, f9, f10);
                gLCanvas.drawTexture(entry.texture, this.mSource, this.mTarget);
                gLCanvas.save(1);
                gLCanvas.multiplyAlpha(f2);
                float f11 = fWidth / 2.0f;
                this.mTarget.set(fCenterX - f11, f5, f4, f7);
                this.mSource.set(0.0f, 0.0f, f8, f10);
                gLCanvas.drawTexture(entry.texture, this.mSource, this.mTarget);
                this.mTarget.set(f6, f5, fCenterX + f11, f7);
                this.mSource.set(f9, 0.0f, width, f10);
                gLCanvas.drawTexture(entry.texture, this.mSource, this.mTarget);
                gLCanvas.restore();
                return;
            }
            float f12 = fWidth / 2.0f;
            float f13 = fCenterX - f12;
            float f14 = fCenterY - f12;
            float f15 = fCenterX + f12;
            float f16 = f12 + fCenterY;
            this.mTarget.set(f13, f14, f15, f16);
            float f17 = (height - width) / 2;
            float f18 = width;
            float f19 = (width + height) / 2;
            this.mSource.set(0.0f, f17, f18, f19);
            gLCanvas.drawTexture(entry.texture, this.mSource, this.mTarget);
            gLCanvas.save(1);
            gLCanvas.multiplyAlpha(f2);
            float f20 = fHeight2 / 2.0f;
            this.mTarget.set(f13, fCenterY - f20, f15, f14);
            this.mSource.set(0.0f, 0.0f, f18, f17);
            gLCanvas.drawTexture(entry.texture, this.mSource, this.mTarget);
            this.mTarget.set(f13, f16, f15, fCenterY + f20);
            this.mSource.set(0.0f, f19, f18, height);
            gLCanvas.drawTexture(entry.texture, this.mSource, this.mTarget);
            gLCanvas.restore();
        }
    }

    @Override
    protected void onCalculate(float f) {
        this.mProgress = f;
    }

    public void setPositionProvider(PositionProvider positionProvider) {
        this.mPositionProvider = positionProvider;
        if (this.mPositionProvider != null) {
            int size = this.mList.size();
            for (int i = 0; i < size; i++) {
                Entry entry = this.mList.get(i);
                entry.index = this.mPositionProvider.getItemIndex(entry.path);
            }
        }
    }

    @Override
    public boolean acceptSlot(int i) {
        int size = this.mList.size();
        for (int i2 = 0; i2 < size; i2++) {
            if (this.mList.get(i2).index == i) {
                return false;
            }
        }
        return true;
    }
}
