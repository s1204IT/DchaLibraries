package android.transition;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.transition.TransitionUtils;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.util.Map;

public class ChangeImageTransform extends Transition {
    private static final String TAG = "ChangeImageTransform";
    private static final String PROPNAME_MATRIX = "android:changeImageTransform:matrix";
    private static final String PROPNAME_BOUNDS = "android:changeImageTransform:bounds";
    private static final String[] sTransitionProperties = {PROPNAME_MATRIX, PROPNAME_BOUNDS};
    private static TypeEvaluator<Matrix> NULL_MATRIX_EVALUATOR = new TypeEvaluator<Matrix>() {
        @Override
        public Matrix evaluate(float fraction, Matrix startValue, Matrix endValue) {
            return null;
        }
    };
    private static Property<ImageView, Matrix> ANIMATED_TRANSFORM_PROPERTY = new Property<ImageView, Matrix>(Matrix.class, "animatedTransform") {
        @Override
        public void set(ImageView object, Matrix value) {
            object.animateTransform(value);
        }

        @Override
        public Matrix get(ImageView object) {
            return null;
        }
    };

    public ChangeImageTransform() {
    }

    public ChangeImageTransform(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void captureValues(TransitionValues transitionValues) {
        ImageView imageView;
        Drawable drawable;
        Matrix matrix;
        View view = transitionValues.view;
        if ((view instanceof ImageView) && view.getVisibility() == 0 && (drawable = (imageView = (ImageView) view).getDrawable()) != null) {
            Map<String, Object> values = transitionValues.values;
            int left = view.getLeft();
            int top = view.getTop();
            int right = view.getRight();
            int bottom = view.getBottom();
            Rect bounds = new Rect(left, top, right, bottom);
            values.put(PROPNAME_BOUNDS, bounds);
            ImageView.ScaleType scaleType = imageView.getScaleType();
            if (scaleType == ImageView.ScaleType.FIT_XY) {
                Matrix matrix2 = imageView.getImageMatrix();
                if (!matrix2.isIdentity()) {
                    matrix = new Matrix(matrix2);
                } else {
                    int drawableWidth = drawable.getIntrinsicWidth();
                    int drawableHeight = drawable.getIntrinsicHeight();
                    if (drawableWidth > 0 && drawableHeight > 0) {
                        float scaleX = bounds.width() / drawableWidth;
                        float scaleY = bounds.height() / drawableHeight;
                        matrix = new Matrix();
                        matrix.setScale(scaleX, scaleY);
                    } else {
                        matrix = null;
                    }
                }
            } else {
                matrix = new Matrix(imageView.getImageMatrix());
            }
            values.put(PROPNAME_MATRIX, matrix);
        }
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
        if (startValues == null || endValues == null) {
            return null;
        }
        Rect startBounds = (Rect) startValues.values.get(PROPNAME_BOUNDS);
        Rect endBounds = (Rect) endValues.values.get(PROPNAME_BOUNDS);
        if (startBounds == null || endBounds == null) {
            return null;
        }
        Matrix startMatrix = (Matrix) startValues.values.get(PROPNAME_MATRIX);
        Matrix endMatrix = (Matrix) endValues.values.get(PROPNAME_MATRIX);
        boolean matricesEqual = (startMatrix == null && endMatrix == null) || (startMatrix != null && startMatrix.equals(endMatrix));
        if (startBounds.equals(endBounds) && matricesEqual) {
            return null;
        }
        ImageView imageView = (ImageView) endValues.view;
        Drawable drawable = imageView.getDrawable();
        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();
        if (drawableWidth == 0 || drawableHeight == 0) {
            return createNullAnimator(imageView);
        }
        if (startMatrix == null) {
            startMatrix = Matrix.IDENTITY_MATRIX;
        }
        if (endMatrix == null) {
            endMatrix = Matrix.IDENTITY_MATRIX;
        }
        ANIMATED_TRANSFORM_PROPERTY.set(imageView, startMatrix);
        return createMatrixAnimator(imageView, startMatrix, endMatrix);
    }

    private ObjectAnimator createNullAnimator(ImageView imageView) {
        return ObjectAnimator.ofObject(imageView, (Property<ImageView, V>) ANIMATED_TRANSFORM_PROPERTY, (TypeEvaluator) NULL_MATRIX_EVALUATOR, (Object[]) new Matrix[]{null, null});
    }

    private ObjectAnimator createMatrixAnimator(ImageView imageView, Matrix startMatrix, Matrix endMatrix) {
        return ObjectAnimator.ofObject(imageView, (Property<ImageView, V>) ANIMATED_TRANSFORM_PROPERTY, (TypeEvaluator) new TransitionUtils.MatrixEvaluator(), (Object[]) new Matrix[]{startMatrix, endMatrix});
    }
}
