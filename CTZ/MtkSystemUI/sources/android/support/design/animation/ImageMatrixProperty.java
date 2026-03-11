package android.support.design.animation;

import android.graphics.Matrix;
import android.util.Property;
import android.widget.ImageView;

public class ImageMatrixProperty extends Property<ImageView, Matrix> {
    private final Matrix matrix;

    public ImageMatrixProperty() {
        super(Matrix.class, "imageMatrixProperty");
        this.matrix = new Matrix();
    }

    @Override
    public void set(ImageView object, Matrix value) {
        object.setImageMatrix(value);
    }

    @Override
    public Matrix get(ImageView object) {
        this.matrix.set(object.getImageMatrix());
        return this.matrix;
    }
}
