package android.filterpacks.imageproc;

import android.app.slice.Slice;
import android.app.slice.SliceItem;
import android.content.Context;
import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.MutableFrameFormat;
import android.filterfw.core.ShaderProgram;
import android.filterfw.format.ImageFormat;
import android.filterfw.format.ObjectFormat;
import android.filterfw.geometry.Quad;

public class DrawOverlayFilter extends Filter {
    private ShaderProgram mProgram;

    public DrawOverlayFilter(String str) {
        super(str);
    }

    @Override
    public void setupPorts() {
        MutableFrameFormat mutableFrameFormatCreate = ImageFormat.create(3, 3);
        addMaskedInputPort(Slice.SUBTYPE_SOURCE, mutableFrameFormatCreate);
        addMaskedInputPort(Context.OVERLAY_SERVICE, mutableFrameFormatCreate);
        addMaskedInputPort("box", ObjectFormat.fromClass(Quad.class, 1));
        addOutputBasedOnInput(SliceItem.FORMAT_IMAGE, Slice.SUBTYPE_SOURCE);
    }

    @Override
    public FrameFormat getOutputFormat(String str, FrameFormat frameFormat) {
        return frameFormat;
    }

    @Override
    public void prepare(FilterContext filterContext) {
        this.mProgram = ShaderProgram.createIdentity(filterContext);
    }

    @Override
    public void process(FilterContext filterContext) {
        Frame framePullInput = pullInput(Slice.SUBTYPE_SOURCE);
        Frame framePullInput2 = pullInput(Context.OVERLAY_SERVICE);
        this.mProgram.setTargetRegion(((Quad) pullInput("box").getObjectValue()).translated(1.0f, 1.0f).scaled(2.0f));
        Frame frameNewFrame = filterContext.getFrameManager().newFrame(framePullInput.getFormat());
        frameNewFrame.setDataFromFrame(framePullInput);
        this.mProgram.process(framePullInput2, frameNewFrame);
        pushOutput(SliceItem.FORMAT_IMAGE, frameNewFrame);
        frameNewFrame.release();
    }
}
