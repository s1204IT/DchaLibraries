package android.support.transition;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.transition.TransitionUtils;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.util.Map;
/* loaded from: classes.dex */
public class ChangeImageTransform extends Transition {
    private static final String[] sTransitionProperties = {"android:changeImageTransform:matrix", "android:changeImageTransform:bounds"};
    private static final TypeEvaluator<Matrix> NULL_MATRIX_EVALUATOR = new TypeEvaluator<Matrix>() { // from class: android.support.transition.ChangeImageTransform.1
        @Override // android.animation.TypeEvaluator
        public Matrix evaluate(float fraction, Matrix startValue, Matrix endValue) {
            return null;
        }
    };
    private static final Property<ImageView, Matrix> ANIMATED_TRANSFORM_PROPERTY = new Property<ImageView, Matrix>(Matrix.class, "animatedTransform") { // from class: android.support.transition.ChangeImageTransform.2
        @Override // android.util.Property
        public void set(ImageView view, Matrix matrix) {
            ImageViewUtils.animateTransform(view, matrix);
        }

        @Override // android.util.Property
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
        View view = transitionValues.view;
        if (!(view instanceof ImageView) || view.getVisibility() != 0) {
            return;
        }
        ImageView imageView = (ImageView) view;
        Drawable drawable = imageView.getDrawable();
        if (drawable == null) {
            return;
        }
        Map<String, Object> values = transitionValues.values;
        int left = view.getLeft();
        int top = view.getTop();
        int right = view.getRight();
        int bottom = view.getBottom();
        Rect bounds = new Rect(left, top, right, bottom);
        values.put("android:changeImageTransform:bounds", bounds);
        values.put("android:changeImageTransform:matrix", copyImageMatrix(imageView));
    }

    @Override // android.support.transition.Transition
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override // android.support.transition.Transition
    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override // android.support.transition.Transition
    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    @Override // android.support.transition.Transition
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
        ObjectAnimator animator;
        if (startValues == null || endValues == null) {
            return null;
        }
        Rect startBounds = (Rect) startValues.values.get("android:changeImageTransform:bounds");
        Rect endBounds = (Rect) endValues.values.get("android:changeImageTransform:bounds");
        if (startBounds == null || endBounds == null) {
            return null;
        }
        Matrix startMatrix = (Matrix) startValues.values.get("android:changeImageTransform:matrix");
        Matrix endMatrix = (Matrix) endValues.values.get("android:changeImageTransform:matrix");
        boolean matricesEqual = (startMatrix == null && endMatrix == null) || (startMatrix != null && startMatrix.equals(endMatrix));
        if (startBounds.equals(endBounds) && matricesEqual) {
            return null;
        }
        ImageView imageView = (ImageView) endValues.view;
        Drawable drawable = imageView.getDrawable();
        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();
        ImageViewUtils.startAnimateTransform(imageView);
        if (drawableWidth == 0 || drawableHeight == 0) {
            animator = createNullAnimator(imageView);
        } else {
            if (startMatrix == null) {
                startMatrix = MatrixUtils.IDENTITY_MATRIX;
            }
            if (endMatrix == null) {
                endMatrix = MatrixUtils.IDENTITY_MATRIX;
            }
            ANIMATED_TRANSFORM_PROPERTY.set(imageView, startMatrix);
            animator = createMatrixAnimator(imageView, startMatrix, endMatrix);
        }
        ImageViewUtils.reserveEndAnimateTransform(imageView, animator);
        return animator;
    }

    private ObjectAnimator createNullAnimator(ImageView imageView) {
        return ObjectAnimator.ofObject(imageView, (Property<ImageView, V>) ANIMATED_TRANSFORM_PROPERTY, (TypeEvaluator) NULL_MATRIX_EVALUATOR, (Object[]) new Matrix[]{null, null});
    }

    private ObjectAnimator createMatrixAnimator(ImageView imageView, Matrix startMatrix, Matrix endMatrix) {
        return ObjectAnimator.ofObject(imageView, (Property<ImageView, V>) ANIMATED_TRANSFORM_PROPERTY, (TypeEvaluator) new TransitionUtils.MatrixEvaluator(), (Object[]) new Matrix[]{startMatrix, endMatrix});
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: android.support.transition.ChangeImageTransform$3  reason: invalid class name */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass3 {
        static final /* synthetic */ int[] $SwitchMap$android$widget$ImageView$ScaleType = new int[ImageView.ScaleType.values().length];

        static {
            try {
                $SwitchMap$android$widget$ImageView$ScaleType[ImageView.ScaleType.FIT_XY.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$widget$ImageView$ScaleType[ImageView.ScaleType.CENTER_CROP.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    private static Matrix copyImageMatrix(ImageView view) {
        switch (AnonymousClass3.$SwitchMap$android$widget$ImageView$ScaleType[view.getScaleType().ordinal()]) {
            case 1:
                return fitXYMatrix(view);
            case 2:
                return centerCropMatrix(view);
            default:
                return new Matrix(view.getImageMatrix());
        }
    }

    private static Matrix fitXYMatrix(ImageView view) {
        Drawable image = view.getDrawable();
        Matrix matrix = new Matrix();
        matrix.postScale(view.getWidth() / image.getIntrinsicWidth(), view.getHeight() / image.getIntrinsicHeight());
        return matrix;
    }

    private static Matrix centerCropMatrix(ImageView view) {
        Drawable image = view.getDrawable();
        int imageWidth = image.getIntrinsicWidth();
        int imageViewWidth = view.getWidth();
        float scaleX = imageViewWidth / imageWidth;
        int imageHeight = image.getIntrinsicHeight();
        int imageViewHeight = view.getHeight();
        float scaleY = imageViewHeight / imageHeight;
        float maxScale = Math.max(scaleX, scaleY);
        float width = imageWidth * maxScale;
        float height = imageHeight * maxScale;
        int tx = Math.round((imageViewWidth - width) / 2.0f);
        int ty = Math.round((imageViewHeight - height) / 2.0f);
        Matrix matrix = new Matrix();
        matrix.postScale(maxScale, maxScale);
        matrix.postTranslate(tx, ty);
        return matrix;
    }
}
