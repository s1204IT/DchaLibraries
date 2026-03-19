package android.filterpacks.imageproc;

import android.app.slice.SliceItem;
import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.core.Program;
import android.filterfw.core.ShaderProgram;
import android.filterfw.format.ImageFormat;
import android.graphics.Bitmap;

public class BitmapOverlayFilter extends Filter {

    @GenerateFieldPort(name = "bitmap")
    private Bitmap mBitmap;
    private Frame mFrame;
    private final String mOverlayShader;
    private Program mProgram;
    private int mTarget;

    @GenerateFieldPort(hasDefault = true, name = "tile_size")
    private int mTileSize;

    public BitmapOverlayFilter(String str) {
        super(str);
        this.mTileSize = 640;
        this.mTarget = 0;
        this.mOverlayShader = "precision mediump float;\nuniform sampler2D tex_sampler_0;\nuniform sampler2D tex_sampler_1;\nvarying vec2 v_texcoord;\nvoid main() {\n  vec4 original = texture2D(tex_sampler_0, v_texcoord);\n  vec4 mask = texture2D(tex_sampler_1, v_texcoord);\n  gl_FragColor = vec4(original.rgb * (1.0 - mask.a) + mask.rgb, 1.0);\n}\n";
    }

    @Override
    public void setupPorts() {
        addMaskedInputPort(SliceItem.FORMAT_IMAGE, ImageFormat.create(3));
        addOutputBasedOnInput(SliceItem.FORMAT_IMAGE, SliceItem.FORMAT_IMAGE);
    }

    @Override
    public FrameFormat getOutputFormat(String str, FrameFormat frameFormat) {
        return frameFormat;
    }

    public void initProgram(FilterContext filterContext, int i) {
        if (i == 3) {
            ShaderProgram shaderProgram = new ShaderProgram(filterContext, "precision mediump float;\nuniform sampler2D tex_sampler_0;\nuniform sampler2D tex_sampler_1;\nvarying vec2 v_texcoord;\nvoid main() {\n  vec4 original = texture2D(tex_sampler_0, v_texcoord);\n  vec4 mask = texture2D(tex_sampler_1, v_texcoord);\n  gl_FragColor = vec4(original.rgb * (1.0 - mask.a) + mask.rgb, 1.0);\n}\n");
            shaderProgram.setMaximumTileSize(this.mTileSize);
            this.mProgram = shaderProgram;
            this.mTarget = i;
            return;
        }
        throw new RuntimeException("Filter FisheyeFilter does not support frames of target " + i + "!");
    }

    @Override
    public void tearDown(FilterContext filterContext) {
        if (this.mFrame != null) {
            this.mFrame.release();
            this.mFrame = null;
        }
    }

    @Override
    public void process(FilterContext filterContext) {
        Frame framePullInput = pullInput(SliceItem.FORMAT_IMAGE);
        FrameFormat format = framePullInput.getFormat();
        Frame frameNewFrame = filterContext.getFrameManager().newFrame(format);
        if (this.mProgram == null || format.getTarget() != this.mTarget) {
            initProgram(filterContext, format.getTarget());
        }
        if (this.mBitmap != null) {
            Frame frameCreateBitmapFrame = createBitmapFrame(filterContext);
            this.mProgram.process(new Frame[]{framePullInput, frameCreateBitmapFrame}, frameNewFrame);
            frameCreateBitmapFrame.release();
        } else {
            frameNewFrame.setDataFromFrame(framePullInput);
        }
        pushOutput(SliceItem.FORMAT_IMAGE, frameNewFrame);
        frameNewFrame.release();
    }

    private Frame createBitmapFrame(FilterContext filterContext) {
        Frame frameNewFrame = filterContext.getFrameManager().newFrame(ImageFormat.create(this.mBitmap.getWidth(), this.mBitmap.getHeight(), 3, 3));
        frameNewFrame.setBitmap(this.mBitmap);
        this.mBitmap.recycle();
        this.mBitmap = null;
        return frameNewFrame;
    }
}
