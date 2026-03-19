package android.filterpacks.base;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.format.ImageFormat;

public class GLTextureSource extends Filter {
    private Frame mFrame;

    @GenerateFieldPort(name = "height")
    private int mHeight;

    @GenerateFieldPort(hasDefault = true, name = "repeatFrame")
    private boolean mRepeatFrame;

    @GenerateFieldPort(name = "texId")
    private int mTexId;

    @GenerateFieldPort(hasDefault = true, name = "timestamp")
    private long mTimestamp;

    @GenerateFieldPort(name = "width")
    private int mWidth;

    public GLTextureSource(String str) {
        super(str);
        this.mRepeatFrame = false;
        this.mTimestamp = -1L;
    }

    @Override
    public void setupPorts() {
        addOutputPort("frame", ImageFormat.create(3, 3));
    }

    @Override
    public void fieldPortValueUpdated(String str, FilterContext filterContext) {
        if (this.mFrame != null) {
            this.mFrame.release();
            this.mFrame = null;
        }
    }

    @Override
    public void process(FilterContext filterContext) {
        if (this.mFrame == null) {
            this.mFrame = filterContext.getFrameManager().newBoundFrame(ImageFormat.create(this.mWidth, this.mHeight, 3, 3), 100, this.mTexId);
            this.mFrame.setTimestamp(this.mTimestamp);
        }
        pushOutput("frame", this.mFrame);
        if (!this.mRepeatFrame) {
            closeOutputPort("frame");
        }
    }

    @Override
    public void tearDown(FilterContext filterContext) {
        if (this.mFrame != null) {
            this.mFrame.release();
        }
    }
}
