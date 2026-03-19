package android.filterpacks.base;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
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

    public GLTextureSource(String name) {
        super(name);
        this.mRepeatFrame = false;
        this.mTimestamp = -1L;
    }

    @Override
    public void setupPorts() {
        addOutputPort("frame", ImageFormat.create(3, 3));
    }

    @Override
    public void fieldPortValueUpdated(String name, FilterContext context) {
        if (this.mFrame == null) {
            return;
        }
        this.mFrame.release();
        this.mFrame = null;
    }

    @Override
    public void process(FilterContext context) throws Throwable {
        if (this.mFrame == null) {
            FrameFormat outputFormat = ImageFormat.create(this.mWidth, this.mHeight, 3, 3);
            this.mFrame = context.getFrameManager().newBoundFrame(outputFormat, 100, this.mTexId);
            this.mFrame.setTimestamp(this.mTimestamp);
        }
        pushOutput("frame", this.mFrame);
        if (this.mRepeatFrame) {
            return;
        }
        closeOutputPort("frame");
    }

    @Override
    public void tearDown(FilterContext context) {
        if (this.mFrame == null) {
            return;
        }
        this.mFrame.release();
    }
}
