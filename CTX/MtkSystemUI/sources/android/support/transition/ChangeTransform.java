package android.support.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Build;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import org.xmlpull.v1.XmlPullParser;

public class ChangeTransform extends Transition {
    private static final boolean SUPPORTS_VIEW_REMOVAL_SUPPRESSION;
    private boolean mReparent;
    private Matrix mTempMatrix;
    private boolean mUseOverlay;
    private static final String[] sTransitionProperties = {"android:changeTransform:matrix", "android:changeTransform:transforms", "android:changeTransform:parentMatrix"};
    private static final Property<PathAnimatorMatrix, float[]> NON_TRANSLATIONS_PROPERTY = new Property<PathAnimatorMatrix, float[]>(float[].class, "nonTranslations") {
        @Override
        public float[] get(PathAnimatorMatrix object) {
            return null;
        }

        @Override
        public void set(PathAnimatorMatrix object, float[] value) {
            object.setValues(value);
        }
    };
    private static final Property<PathAnimatorMatrix, PointF> TRANSLATIONS_PROPERTY = new Property<PathAnimatorMatrix, PointF>(PointF.class, "translations") {
        @Override
        public PointF get(PathAnimatorMatrix object) {
            return null;
        }

        @Override
        public void set(PathAnimatorMatrix object, PointF value) {
            object.setTranslation(value);
        }
    };

    static {
        SUPPORTS_VIEW_REMOVAL_SUPPRESSION = Build.VERSION.SDK_INT >= 21;
    }

    public ChangeTransform() {
        this.mUseOverlay = true;
        this.mReparent = true;
        this.mTempMatrix = new Matrix();
    }

    public ChangeTransform(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mUseOverlay = true;
        this.mReparent = true;
        this.mTempMatrix = new Matrix();
        TypedArray a = context.obtainStyledAttributes(attrs, Styleable.CHANGE_TRANSFORM);
        this.mUseOverlay = TypedArrayUtils.getNamedBoolean(a, (XmlPullParser) attrs, "reparentWithOverlay", 1, true);
        this.mReparent = TypedArrayUtils.getNamedBoolean(a, (XmlPullParser) attrs, "reparent", 0, true);
        a.recycle();
    }

    @Override
    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    private void captureValues(TransitionValues transitionValues) {
        Matrix matrix;
        View view = transitionValues.view;
        if (view.getVisibility() == 8) {
            return;
        }
        transitionValues.values.put("android:changeTransform:parent", view.getParent());
        Transforms transforms = new Transforms(view);
        transitionValues.values.put("android:changeTransform:transforms", transforms);
        Matrix matrix2 = view.getMatrix();
        if (matrix2 == null || matrix2.isIdentity()) {
            matrix = null;
        } else {
            matrix = new Matrix(matrix2);
        }
        transitionValues.values.put("android:changeTransform:matrix", matrix);
        if (this.mReparent) {
            Matrix parentMatrix = new Matrix();
            ViewGroup parent = (ViewGroup) view.getParent();
            ViewUtils.transformMatrixToGlobal(parent, parentMatrix);
            parentMatrix.preTranslate(-parent.getScrollX(), -parent.getScrollY());
            transitionValues.values.put("android:changeTransform:parentMatrix", parentMatrix);
            transitionValues.values.put("android:changeTransform:intermediateMatrix", view.getTag(R.id.transition_transform));
            transitionValues.values.put("android:changeTransform:intermediateParentMatrix", view.getTag(R.id.parent_matrix));
        }
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
        if (!SUPPORTS_VIEW_REMOVAL_SUPPRESSION) {
            ((ViewGroup) transitionValues.view.getParent()).startViewTransition(transitionValues.view);
        }
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
        if (startValues == null || endValues == null || !startValues.values.containsKey("android:changeTransform:parent") || !endValues.values.containsKey("android:changeTransform:parent")) {
            return null;
        }
        ViewGroup startParent = (ViewGroup) startValues.values.get("android:changeTransform:parent");
        ViewGroup endParent = (ViewGroup) endValues.values.get("android:changeTransform:parent");
        boolean handleParentChange = this.mReparent && !parentsMatch(startParent, endParent);
        Matrix startMatrix = (Matrix) startValues.values.get("android:changeTransform:intermediateMatrix");
        if (startMatrix != null) {
            startValues.values.put("android:changeTransform:matrix", startMatrix);
        }
        Matrix startParentMatrix = (Matrix) startValues.values.get("android:changeTransform:intermediateParentMatrix");
        if (startParentMatrix != null) {
            startValues.values.put("android:changeTransform:parentMatrix", startParentMatrix);
        }
        if (handleParentChange) {
            setMatricesForParent(startValues, endValues);
        }
        ObjectAnimator transformAnimator = createTransformAnimator(startValues, endValues, handleParentChange);
        if (handleParentChange && transformAnimator != null && this.mUseOverlay) {
            createGhostView(sceneRoot, startValues, endValues);
        } else if (!SUPPORTS_VIEW_REMOVAL_SUPPRESSION) {
            startParent.endViewTransition(startValues.view);
        }
        return transformAnimator;
    }

    private ObjectAnimator createTransformAnimator(TransitionValues startValues, TransitionValues endValues, final boolean handleParentChange) {
        Matrix startMatrix = (Matrix) startValues.values.get("android:changeTransform:matrix");
        Matrix endMatrix = (Matrix) endValues.values.get("android:changeTransform:matrix");
        if (startMatrix == null) {
            startMatrix = MatrixUtils.IDENTITY_MATRIX;
        }
        if (endMatrix == null) {
            endMatrix = MatrixUtils.IDENTITY_MATRIX;
        }
        if (startMatrix.equals(endMatrix)) {
            return null;
        }
        final Transforms transforms = (Transforms) endValues.values.get("android:changeTransform:transforms");
        final View view = endValues.view;
        setIdentityTransforms(view);
        float[] startMatrixValues = new float[9];
        startMatrix.getValues(startMatrixValues);
        float[] endMatrixValues = new float[9];
        endMatrix.getValues(endMatrixValues);
        final PathAnimatorMatrix pathAnimatorMatrix = new PathAnimatorMatrix(view, startMatrixValues);
        PropertyValuesHolder valuesProperty = PropertyValuesHolder.ofObject(NON_TRANSLATIONS_PROPERTY, new FloatArrayEvaluator(new float[9]), startMatrixValues, endMatrixValues);
        Path path = getPathMotion().getPath(startMatrixValues[2], startMatrixValues[5], endMatrixValues[2], endMatrixValues[5]);
        PropertyValuesHolder translationProperty = PropertyValuesHolderUtils.ofPointF(TRANSLATIONS_PROPERTY, path);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(pathAnimatorMatrix, valuesProperty, translationProperty);
        final Matrix finalEndMatrix = endMatrix;
        AnimatorListenerAdapter listener = new AnimatorListenerAdapter() {
            private boolean mIsCanceled;
            private Matrix mTempMatrix = new Matrix();

            @Override
            public void onAnimationCancel(Animator animation) {
                this.mIsCanceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!this.mIsCanceled) {
                    if (handleParentChange && ChangeTransform.this.mUseOverlay) {
                        setCurrentMatrix(finalEndMatrix);
                    } else {
                        view.setTag(R.id.transition_transform, null);
                        view.setTag(R.id.parent_matrix, null);
                    }
                }
                ViewUtils.setAnimationMatrix(view, null);
                transforms.restore(view);
            }

            @Override
            public void onAnimationPause(Animator animation) {
                Matrix currentMatrix = pathAnimatorMatrix.getMatrix();
                setCurrentMatrix(currentMatrix);
            }

            @Override
            public void onAnimationResume(Animator animation) {
                ChangeTransform.setIdentityTransforms(view);
            }

            private void setCurrentMatrix(Matrix currentMatrix) {
                this.mTempMatrix.set(currentMatrix);
                view.setTag(R.id.transition_transform, this.mTempMatrix);
                transforms.restore(view);
            }
        };
        animator.addListener(listener);
        AnimatorUtils.addPauseListener(animator, listener);
        return animator;
    }

    private boolean parentsMatch(ViewGroup startParent, ViewGroup endParent) {
        if (!isValidTarget(startParent) || !isValidTarget(endParent)) {
            boolean parentsMatch = startParent == endParent;
            return parentsMatch;
        }
        TransitionValues endValues = getMatchedTransitionValues(startParent, true);
        if (endValues == null) {
            return false;
        }
        boolean parentsMatch2 = endParent == endValues.view;
        return parentsMatch2;
    }

    private void createGhostView(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues) {
        View view = endValues.view;
        Matrix endMatrix = (Matrix) endValues.values.get("android:changeTransform:parentMatrix");
        Matrix localEndMatrix = new Matrix(endMatrix);
        ViewUtils.transformMatrixToLocal(sceneRoot, localEndMatrix);
        GhostViewImpl ghostView = GhostViewUtils.addGhost(view, sceneRoot, localEndMatrix);
        if (ghostView == null) {
            return;
        }
        ghostView.reserveEndViewTransition((ViewGroup) startValues.values.get("android:changeTransform:parent"), startValues.view);
        Transition outerTransition = this;
        while (outerTransition.mParent != null) {
            outerTransition = outerTransition.mParent;
        }
        GhostListener listener = new GhostListener(view, ghostView);
        outerTransition.addListener(listener);
        if (SUPPORTS_VIEW_REMOVAL_SUPPRESSION) {
            if (startValues.view != endValues.view) {
                ViewUtils.setTransitionAlpha(startValues.view, 0.0f);
            }
            ViewUtils.setTransitionAlpha(view, 1.0f);
        }
    }

    private void setMatricesForParent(TransitionValues startValues, TransitionValues endValues) {
        Matrix endParentMatrix = (Matrix) endValues.values.get("android:changeTransform:parentMatrix");
        endValues.view.setTag(R.id.parent_matrix, endParentMatrix);
        Matrix toLocal = this.mTempMatrix;
        toLocal.reset();
        endParentMatrix.invert(toLocal);
        Matrix startLocal = (Matrix) startValues.values.get("android:changeTransform:matrix");
        if (startLocal == null) {
            startLocal = new Matrix();
            startValues.values.put("android:changeTransform:matrix", startLocal);
        }
        Matrix startParentMatrix = (Matrix) startValues.values.get("android:changeTransform:parentMatrix");
        startLocal.postConcat(startParentMatrix);
        startLocal.postConcat(toLocal);
    }

    public static void setIdentityTransforms(View view) {
        setTransforms(view, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f);
    }

    public static void setTransforms(View view, float translationX, float translationY, float translationZ, float scaleX, float scaleY, float rotationX, float rotationY, float rotationZ) {
        view.setTranslationX(translationX);
        view.setTranslationY(translationY);
        ViewCompat.setTranslationZ(view, translationZ);
        view.setScaleX(scaleX);
        view.setScaleY(scaleY);
        view.setRotationX(rotationX);
        view.setRotationY(rotationY);
        view.setRotation(rotationZ);
    }

    private static class Transforms {
        final float mRotationX;
        final float mRotationY;
        final float mRotationZ;
        final float mScaleX;
        final float mScaleY;
        final float mTranslationX;
        final float mTranslationY;
        final float mTranslationZ;

        Transforms(View view) {
            this.mTranslationX = view.getTranslationX();
            this.mTranslationY = view.getTranslationY();
            this.mTranslationZ = ViewCompat.getTranslationZ(view);
            this.mScaleX = view.getScaleX();
            this.mScaleY = view.getScaleY();
            this.mRotationX = view.getRotationX();
            this.mRotationY = view.getRotationY();
            this.mRotationZ = view.getRotation();
        }

        public void restore(View view) {
            ChangeTransform.setTransforms(view, this.mTranslationX, this.mTranslationY, this.mTranslationZ, this.mScaleX, this.mScaleY, this.mRotationX, this.mRotationY, this.mRotationZ);
        }

        public boolean equals(Object that) {
            if (!(that instanceof Transforms)) {
                return false;
            }
            Transforms thatTransform = (Transforms) that;
            return thatTransform.mTranslationX == this.mTranslationX && thatTransform.mTranslationY == this.mTranslationY && thatTransform.mTranslationZ == this.mTranslationZ && thatTransform.mScaleX == this.mScaleX && thatTransform.mScaleY == this.mScaleY && thatTransform.mRotationX == this.mRotationX && thatTransform.mRotationY == this.mRotationY && thatTransform.mRotationZ == this.mRotationZ;
        }

        public int hashCode() {
            int code = this.mTranslationX != 0.0f ? Float.floatToIntBits(this.mTranslationX) : 0;
            return (31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * code) + (this.mTranslationY != 0.0f ? Float.floatToIntBits(this.mTranslationY) : 0))) + (this.mTranslationZ != 0.0f ? Float.floatToIntBits(this.mTranslationZ) : 0))) + (this.mScaleX != 0.0f ? Float.floatToIntBits(this.mScaleX) : 0))) + (this.mScaleY != 0.0f ? Float.floatToIntBits(this.mScaleY) : 0))) + (this.mRotationX != 0.0f ? Float.floatToIntBits(this.mRotationX) : 0))) + (this.mRotationY != 0.0f ? Float.floatToIntBits(this.mRotationY) : 0))) + (this.mRotationZ != 0.0f ? Float.floatToIntBits(this.mRotationZ) : 0);
        }
    }

    private static class GhostListener extends TransitionListenerAdapter {
        private GhostViewImpl mGhostView;
        private View mView;

        GhostListener(View view, GhostViewImpl ghostView) {
            this.mView = view;
            this.mGhostView = ghostView;
        }

        @Override
        public void onTransitionEnd(Transition transition) {
            transition.removeListener(this);
            GhostViewUtils.removeGhost(this.mView);
            this.mView.setTag(R.id.transition_transform, null);
            this.mView.setTag(R.id.parent_matrix, null);
        }

        @Override
        public void onTransitionPause(Transition transition) {
            this.mGhostView.setVisibility(4);
        }

        @Override
        public void onTransitionResume(Transition transition) {
            this.mGhostView.setVisibility(0);
        }
    }

    private static class PathAnimatorMatrix {
        private final Matrix mMatrix = new Matrix();
        private float mTranslationX;
        private float mTranslationY;
        private final float[] mValues;
        private final View mView;

        PathAnimatorMatrix(View view, float[] values) {
            this.mView = view;
            this.mValues = (float[]) values.clone();
            this.mTranslationX = this.mValues[2];
            this.mTranslationY = this.mValues[5];
            setAnimationMatrix();
        }

        void setValues(float[] values) {
            System.arraycopy(values, 0, this.mValues, 0, values.length);
            setAnimationMatrix();
        }

        void setTranslation(PointF translation) {
            this.mTranslationX = translation.x;
            this.mTranslationY = translation.y;
            setAnimationMatrix();
        }

        private void setAnimationMatrix() {
            this.mValues[2] = this.mTranslationX;
            this.mValues[5] = this.mTranslationY;
            this.mMatrix.setValues(this.mValues);
            ViewUtils.setAnimationMatrix(this.mView, this.mMatrix);
        }

        Matrix getMatrix() {
            return this.mMatrix;
        }
    }
}
