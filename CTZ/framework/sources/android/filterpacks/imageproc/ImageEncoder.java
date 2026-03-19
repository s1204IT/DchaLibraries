package android.filterpacks.imageproc;

import android.app.Instrumentation;
import android.app.slice.SliceItem;
import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.format.ImageFormat;
import android.graphics.Bitmap;
import android.media.MediaFormat;
import java.io.OutputStream;

public class ImageEncoder extends Filter {

    @GenerateFieldPort(name = Instrumentation.REPORT_KEY_STREAMRESULT)
    private OutputStream mOutputStream;

    @GenerateFieldPort(hasDefault = true, name = MediaFormat.KEY_QUALITY)
    private int mQuality;

    public ImageEncoder(String str) {
        super(str);
        this.mQuality = 80;
    }

    @Override
    public void setupPorts() {
        addMaskedInputPort(SliceItem.FORMAT_IMAGE, ImageFormat.create(3, 0));
    }

    @Override
    public void process(FilterContext filterContext) {
        pullInput(SliceItem.FORMAT_IMAGE).getBitmap().compress(Bitmap.CompressFormat.JPEG, this.mQuality, this.mOutputStream);
    }
}
