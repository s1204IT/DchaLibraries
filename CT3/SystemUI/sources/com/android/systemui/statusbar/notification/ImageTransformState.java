package com.android.systemui.statusbar.notification;

import android.graphics.drawable.Icon;
import android.util.Pools;
import android.view.View;
import android.widget.ImageView;
import com.android.systemui.R;

public class ImageTransformState extends TransformState {
    private static Pools.SimplePool<ImageTransformState> sInstancePool = new Pools.SimplePool<>(40);
    private Icon mIcon;

    @Override
    public void initFrom(View view) {
        super.initFrom(view);
        if (!(view instanceof ImageView)) {
            return;
        }
        this.mIcon = (Icon) view.getTag(R.id.image_icon_tag);
    }

    @Override
    protected boolean sameAs(TransformState otherState) {
        if (otherState instanceof ImageTransformState) {
            if (this.mIcon != null) {
                return this.mIcon.sameAs(((ImageTransformState) otherState).getIcon());
            }
            return false;
        }
        return super.sameAs(otherState);
    }

    public Icon getIcon() {
        return this.mIcon;
    }

    public static ImageTransformState obtain() {
        ImageTransformState instance = (ImageTransformState) sInstancePool.acquire();
        if (instance != null) {
            return instance;
        }
        return new ImageTransformState();
    }

    @Override
    protected boolean transformScale() {
        return true;
    }

    @Override
    public void recycle() {
        super.recycle();
        sInstancePool.release(this);
    }

    @Override
    protected void reset() {
        super.reset();
        this.mIcon = null;
    }
}
