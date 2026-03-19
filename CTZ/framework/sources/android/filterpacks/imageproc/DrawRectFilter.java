package android.filterpacks.imageproc;

import android.app.slice.SliceItem;
import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.GLFrame;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.core.ShaderProgram;
import android.filterfw.format.ImageFormat;
import android.filterfw.format.ObjectFormat;
import android.filterfw.geometry.Quad;
import android.opengl.GLES20;

public class DrawRectFilter extends Filter {

    @GenerateFieldPort(hasDefault = true, name = "colorBlue")
    private float mColorBlue;

    @GenerateFieldPort(hasDefault = true, name = "colorGreen")
    private float mColorGreen;

    @GenerateFieldPort(hasDefault = true, name = "colorRed")
    private float mColorRed;
    private final String mFixedColorFragmentShader;
    private ShaderProgram mProgram;
    private final String mVertexShader;

    public DrawRectFilter(String str) {
        super(str);
        this.mColorRed = 0.8f;
        this.mColorGreen = 0.8f;
        this.mColorBlue = 0.0f;
        this.mVertexShader = "attribute vec4 aPosition;\nvoid main() {\n  gl_Position = aPosition;\n}\n";
        this.mFixedColorFragmentShader = "precision mediump float;\nuniform vec4 color;\nvoid main() {\n  gl_FragColor = color;\n}\n";
    }

    @Override
    public void setupPorts() {
        addMaskedInputPort(SliceItem.FORMAT_IMAGE, ImageFormat.create(3, 3));
        addMaskedInputPort("box", ObjectFormat.fromClass(Quad.class, 1));
        addOutputBasedOnInput(SliceItem.FORMAT_IMAGE, SliceItem.FORMAT_IMAGE);
    }

    @Override
    public FrameFormat getOutputFormat(String str, FrameFormat frameFormat) {
        return frameFormat;
    }

    @Override
    public void prepare(FilterContext filterContext) {
        this.mProgram = new ShaderProgram(filterContext, "attribute vec4 aPosition;\nvoid main() {\n  gl_Position = aPosition;\n}\n", "precision mediump float;\nuniform vec4 color;\nvoid main() {\n  gl_FragColor = color;\n}\n");
    }

    @Override
    public void process(FilterContext filterContext) {
        Frame framePullInput = pullInput(SliceItem.FORMAT_IMAGE);
        Quad quadTranslated = ((Quad) pullInput("box").getObjectValue()).scaled(2.0f).translated(-1.0f, -1.0f);
        GLFrame gLFrame = (GLFrame) filterContext.getFrameManager().duplicateFrame(framePullInput);
        gLFrame.focus();
        renderBox(quadTranslated);
        pushOutput(SliceItem.FORMAT_IMAGE, gLFrame);
        gLFrame.release();
    }

    private void renderBox(Quad quad) {
        float[] fArr = {this.mColorRed, this.mColorGreen, this.mColorBlue, 1.0f};
        float[] fArr2 = {quad.p0.x, quad.p0.y, quad.p1.x, quad.p1.y, quad.p3.x, quad.p3.y, quad.p2.x, quad.p2.y};
        this.mProgram.setHostValue("color", fArr);
        this.mProgram.setAttributeValues("aPosition", fArr2, 2);
        this.mProgram.setVertexCount(4);
        this.mProgram.beginDrawing();
        GLES20.glLineWidth(1.0f);
        GLES20.glDrawArrays(2, 0, 4);
    }
}
