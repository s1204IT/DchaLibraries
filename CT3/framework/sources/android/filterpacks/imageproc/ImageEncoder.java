package android.filterpacks.imageproc;

import android.app.Instrumentation;
import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
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

    public ImageEncoder(String name) {
        super(name);
        this.mQuality = 80;
    }

    @Override
    public void setupPorts() {
        addMaskedInputPort("image", ImageFormat.create(3, 0));
    }

    @Override
    public void process(FilterContext env) {
        Frame input = pullInput("image");
        Bitmap bitmap = input.getBitmap();
        bitmap.compress(Bitmap.CompressFormat.JPEG, this.mQuality, this.mOutputStream);
    }
}
