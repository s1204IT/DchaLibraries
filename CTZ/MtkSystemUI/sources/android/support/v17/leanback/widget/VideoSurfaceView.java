package android.support.v17.leanback.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

public class VideoSurfaceView extends SurfaceView {
    public VideoSurfaceView(Context context) {
        super(context);
    }

    public VideoSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setTransitionVisibility(int visibility) {
    }
}
