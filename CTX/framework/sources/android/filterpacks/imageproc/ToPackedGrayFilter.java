package android.filterpacks.imageproc;

import android.app.slice.SliceItem;
import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.core.MutableFrameFormat;
import android.filterfw.core.Program;
import android.filterfw.core.ShaderProgram;
import android.filterfw.format.ImageFormat;
import android.net.wifi.WifiEnterpriseConfig;

public class ToPackedGrayFilter extends Filter {
    private final String mColorToPackedGrayShader;

    @GenerateFieldPort(hasDefault = true, name = "keepAspectRatio")
    private boolean mKeepAspectRatio;

    @GenerateFieldPort(hasDefault = true, name = "oheight")
    private int mOHeight;

    @GenerateFieldPort(hasDefault = true, name = "owidth")
    private int mOWidth;
    private Program mProgram;

    public ToPackedGrayFilter(String str) {
        super(str);
        this.mOWidth = 0;
        this.mOHeight = 0;
        this.mKeepAspectRatio = false;
        this.mColorToPackedGrayShader = "precision mediump float;\nconst vec4 coeff_y = vec4(0.299, 0.587, 0.114, 0);\nuniform sampler2D tex_sampler_0;\nuniform float pix_stride;\nvarying vec2 v_texcoord;\nvoid main() {\n  for (int i = 0; i < 4; ++i) {\n    vec4 p = texture2D(tex_sampler_0,\n                       v_texcoord + vec2(pix_stride * float(i), 0.0));\n    gl_FragColor[i] = dot(p, coeff_y);\n  }\n}\n";
    }

    @Override
    public void setupPorts() {
        addMaskedInputPort(SliceItem.FORMAT_IMAGE, ImageFormat.create(3, 3));
        addOutputBasedOnInput(SliceItem.FORMAT_IMAGE, SliceItem.FORMAT_IMAGE);
    }

    @Override
    public FrameFormat getOutputFormat(String str, FrameFormat frameFormat) {
        return convertInputFormat(frameFormat);
    }

    private void checkOutputDimensions(int i, int i2) {
        if (i <= 0 || i2 <= 0) {
            throw new RuntimeException("Invalid output dimensions: " + i + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + i2);
        }
    }

    private FrameFormat convertInputFormat(FrameFormat frameFormat) {
        int iMax = this.mOWidth;
        int iMax2 = this.mOHeight;
        int width = frameFormat.getWidth();
        int height = frameFormat.getHeight();
        if (this.mOWidth == 0) {
            iMax = width;
        }
        if (this.mOHeight == 0) {
            iMax2 = height;
        }
        if (this.mKeepAspectRatio) {
            if (width > height) {
                iMax = Math.max(iMax, iMax2);
                iMax2 = (height * iMax) / width;
            } else {
                iMax2 = Math.max(iMax, iMax2);
                iMax = (width * iMax2) / height;
            }
        }
        return ImageFormat.create((iMax <= 0 || iMax >= 4) ? 4 * (iMax / 4) : 4, iMax2, 1, 2);
    }

    @Override
    public void prepare(FilterContext filterContext) {
        this.mProgram = new ShaderProgram(filterContext, "precision mediump float;\nconst vec4 coeff_y = vec4(0.299, 0.587, 0.114, 0);\nuniform sampler2D tex_sampler_0;\nuniform float pix_stride;\nvarying vec2 v_texcoord;\nvoid main() {\n  for (int i = 0; i < 4; ++i) {\n    vec4 p = texture2D(tex_sampler_0,\n                       v_texcoord + vec2(pix_stride * float(i), 0.0));\n    gl_FragColor[i] = dot(p, coeff_y);\n  }\n}\n");
    }

    @Override
    public void process(FilterContext filterContext) {
        Frame framePullInput = pullInput(SliceItem.FORMAT_IMAGE);
        FrameFormat format = framePullInput.getFormat();
        FrameFormat frameFormatConvertInputFormat = convertInputFormat(format);
        int width = frameFormatConvertInputFormat.getWidth();
        int height = frameFormatConvertInputFormat.getHeight();
        checkOutputDimensions(width, height);
        this.mProgram.setHostValue("pix_stride", Float.valueOf(1.0f / width));
        MutableFrameFormat mutableFrameFormatMutableCopy = format.mutableCopy();
        mutableFrameFormatMutableCopy.setDimensions(width / 4, height);
        Frame frameNewFrame = filterContext.getFrameManager().newFrame(mutableFrameFormatMutableCopy);
        this.mProgram.process(framePullInput, frameNewFrame);
        Frame frameNewFrame2 = filterContext.getFrameManager().newFrame(frameFormatConvertInputFormat);
        frameNewFrame2.setDataFromFrame(frameNewFrame);
        frameNewFrame.release();
        pushOutput(SliceItem.FORMAT_IMAGE, frameNewFrame2);
        frameNewFrame2.release();
    }
}
