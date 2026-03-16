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
    private static final Interpolator ANIM_INTERPOLATE = new DecelerateInterpolator(1.5f);
    private PositionProvider mPositionProvider;
    private float mProgress;
    private RectF mSource = new RectF();
    private RectF mTarget = new RectF();
    private ArrayList<Entry> mList = new ArrayList<>();

    public static class Entry {
        public Rect dest;
        public int index;
        public Path path;
        public Rect source;
        public RawTexture texture;
    }

    public interface PositionProvider {
        int getItemIndex(Path path);

        Rect getPosition(int i);
    }

    public PhotoFallbackEffect() {
        setDuration(300);
        setInterpolator(ANIM_INTERPOLATE);
    }

    public boolean draw(GLCanvas canvas) {
        boolean more = calculate(AnimationTime.get());
        int n = this.mList.size();
        for (int i = 0; i < n; i++) {
            Entry entry = this.mList.get(i);
            if (entry.index >= 0) {
                entry.dest = this.mPositionProvider.getPosition(entry.index);
                drawEntry(canvas, entry);
            }
        }
        return more;
    }

    private void drawEntry(GLCanvas canvas, Entry entry) {
        if (entry.texture.isLoaded()) {
            int w = entry.texture.getWidth();
            int h = entry.texture.getHeight();
            Rect s = entry.source;
            Rect d = entry.dest;
            float p = this.mProgress;
            float fullScale = d.height() / Math.min(s.width(), s.height());
            float scale = (fullScale * p) + (1.0f * (1.0f - p));
            float cx = (d.centerX() * p) + (s.centerX() * (1.0f - p));
            float cy = (d.centerY() * p) + (s.centerY() * (1.0f - p));
            float ch = s.height() * scale;
            float cw = s.width() * scale;
            if (w > h) {
                this.mTarget.set(cx - (ch / 2.0f), cy - (ch / 2.0f), (ch / 2.0f) + cx, (ch / 2.0f) + cy);
                this.mSource.set((w - h) / 2, 0.0f, (w + h) / 2, h);
                canvas.drawTexture(entry.texture, this.mSource, this.mTarget);
                canvas.save(1);
                canvas.multiplyAlpha(1.0f - p);
                this.mTarget.set(cx - (cw / 2.0f), cy - (ch / 2.0f), cx - (ch / 2.0f), (ch / 2.0f) + cy);
                this.mSource.set(0.0f, 0.0f, (w - h) / 2, h);
                canvas.drawTexture(entry.texture, this.mSource, this.mTarget);
                this.mTarget.set((ch / 2.0f) + cx, cy - (ch / 2.0f), (cw / 2.0f) + cx, (ch / 2.0f) + cy);
                this.mSource.set((w + h) / 2, 0.0f, w, h);
                canvas.drawTexture(entry.texture, this.mSource, this.mTarget);
                canvas.restore();
                return;
            }
            this.mTarget.set(cx - (cw / 2.0f), cy - (cw / 2.0f), (cw / 2.0f) + cx, (cw / 2.0f) + cy);
            this.mSource.set(0.0f, (h - w) / 2, w, (h + w) / 2);
            canvas.drawTexture(entry.texture, this.mSource, this.mTarget);
            canvas.save(1);
            canvas.multiplyAlpha(1.0f - p);
            this.mTarget.set(cx - (cw / 2.0f), cy - (ch / 2.0f), (cw / 2.0f) + cx, cy - (cw / 2.0f));
            this.mSource.set(0.0f, 0.0f, w, (h - w) / 2);
            canvas.drawTexture(entry.texture, this.mSource, this.mTarget);
            this.mTarget.set(cx - (cw / 2.0f), (cw / 2.0f) + cy, (cw / 2.0f) + cx, (ch / 2.0f) + cy);
            this.mSource.set(0.0f, (w + h) / 2, w, h);
            canvas.drawTexture(entry.texture, this.mSource, this.mTarget);
            canvas.restore();
        }
    }

    @Override
    protected void onCalculate(float progress) {
        this.mProgress = progress;
    }

    public void setPositionProvider(PositionProvider provider) {
        this.mPositionProvider = provider;
        if (this.mPositionProvider != null) {
            int n = this.mList.size();
            for (int i = 0; i < n; i++) {
                Entry entry = this.mList.get(i);
                entry.index = this.mPositionProvider.getItemIndex(entry.path);
            }
        }
    }

    @Override
    public boolean acceptSlot(int index) {
        int n = this.mList.size();
        for (int i = 0; i < n; i++) {
            Entry entry = this.mList.get(i);
            if (entry.index == index) {
                return false;
            }
        }
        return true;
    }
}
