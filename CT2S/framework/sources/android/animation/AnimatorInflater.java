package android.animation;

import android.content.Context;
import android.content.res.ConfigurationBoundResourceCache;
import android.content.res.ConstantState;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.PathParser;
import android.util.StateSet;
import android.util.TypedValue;
import android.util.Xml;
import android.view.InflateException;
import android.view.animation.AnimationUtils;
import android.view.animation.BaseInterpolator;
import android.view.animation.Interpolator;
import com.android.internal.R;
import java.io.IOException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AnimatorInflater {
    private static final boolean DBG_ANIMATOR_INFLATER = false;
    private static final int SEQUENTIALLY = 1;
    private static final String TAG = "AnimatorInflater";
    private static final int TOGETHER = 0;
    private static final int VALUE_TYPE_COLOR = 4;
    private static final int VALUE_TYPE_CUSTOM = 5;
    private static final int VALUE_TYPE_FLOAT = 0;
    private static final int VALUE_TYPE_INT = 1;
    private static final int VALUE_TYPE_PATH = 2;
    private static final TypedValue sTmpTypedValue = new TypedValue();

    public static Animator loadAnimator(Context context, int id) throws Resources.NotFoundException {
        return loadAnimator(context.getResources(), context.getTheme(), id);
    }

    public static Animator loadAnimator(Resources resources, Resources.Theme theme, int id) throws Resources.NotFoundException {
        return loadAnimator(resources, theme, id, 1.0f);
    }

    public static Animator loadAnimator(Resources resources, Resources.Theme theme, int id, float pathErrorScale) throws Resources.NotFoundException {
        ConfigurationBoundResourceCache<Animator> animatorCache = resources.getAnimatorCache();
        Animator animator = animatorCache.get(id, theme);
        if (animator != null) {
            return animator;
        }
        XmlResourceParser parser = null;
        try {
            try {
                parser = resources.getAnimation(id);
                Animator animator2 = createAnimatorFromXml(resources, theme, parser, pathErrorScale);
                if (animator2 != null) {
                    animator2.appendChangingConfigurations(getChangingConfigs(resources, id));
                    ConstantState<Animator> constantState = animator2.createConstantState();
                    if (constantState != null) {
                        animatorCache.put(id, theme, constantState);
                        animator2 = constantState.newInstance(resources, theme);
                    }
                }
                return animator2;
            } catch (IOException ex) {
                Resources.NotFoundException rnf = new Resources.NotFoundException("Can't load animation resource ID #0x" + Integer.toHexString(id));
                rnf.initCause(ex);
                throw rnf;
            } catch (XmlPullParserException ex2) {
                Resources.NotFoundException rnf2 = new Resources.NotFoundException("Can't load animation resource ID #0x" + Integer.toHexString(id));
                rnf2.initCause(ex2);
                throw rnf2;
            }
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    public static StateListAnimator loadStateListAnimator(Context context, int id) throws Resources.NotFoundException {
        Resources resources = context.getResources();
        ConfigurationBoundResourceCache<StateListAnimator> cache = resources.getStateListAnimatorCache();
        Resources.Theme theme = context.getTheme();
        StateListAnimator animator = cache.get(id, theme);
        if (animator != null) {
            return animator;
        }
        XmlResourceParser parser = null;
        try {
            try {
                try {
                    parser = resources.getAnimation(id);
                    StateListAnimator animator2 = createStateListAnimatorFromXml(context, parser, Xml.asAttributeSet(parser));
                    if (animator2 != null) {
                        animator2.appendChangingConfigurations(getChangingConfigs(resources, id));
                        ConstantState<StateListAnimator> constantState = animator2.createConstantState();
                        if (constantState != null) {
                            cache.put(id, theme, constantState);
                            animator2 = constantState.newInstance(resources, theme);
                        }
                    }
                    return animator2;
                } catch (XmlPullParserException ex) {
                    Resources.NotFoundException rnf = new Resources.NotFoundException("Can't load state list animator resource ID #0x" + Integer.toHexString(id));
                    rnf.initCause(ex);
                    throw rnf;
                }
            } catch (IOException ex2) {
                Resources.NotFoundException rnf2 = new Resources.NotFoundException("Can't load state list animator resource ID #0x" + Integer.toHexString(id));
                rnf2.initCause(ex2);
                throw rnf2;
            }
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    private static StateListAnimator createStateListAnimatorFromXml(Context context, XmlPullParser parser, AttributeSet attributeSet) throws XmlPullParserException, IOException {
        int stateIndex;
        StateListAnimator stateListAnimator = new StateListAnimator();
        while (true) {
            int type = parser.next();
            switch (type) {
                case 1:
                case 3:
                    return stateListAnimator;
                case 2:
                    Animator animator = null;
                    if (!"item".equals(parser.getName())) {
                        continue;
                    } else {
                        int attributeCount = parser.getAttributeCount();
                        int[] states = new int[attributeCount];
                        int i = 0;
                        int stateIndex2 = 0;
                        while (i < attributeCount) {
                            int attrName = attributeSet.getAttributeNameResource(i);
                            if (attrName == 16843213) {
                                int animId = attributeSet.getAttributeResourceValue(i, 0);
                                animator = loadAnimator(context, animId);
                                stateIndex = stateIndex2;
                            } else {
                                stateIndex = stateIndex2 + 1;
                                if (!attributeSet.getAttributeBooleanValue(i, false)) {
                                    attrName = -attrName;
                                }
                                states[stateIndex2] = attrName;
                            }
                            i++;
                            stateIndex2 = stateIndex;
                        }
                        if (animator == null) {
                            animator = createAnimatorFromXml(context.getResources(), context.getTheme(), parser, 1.0f);
                        }
                        if (animator == null) {
                            throw new Resources.NotFoundException("animation state item must have a valid animation");
                        }
                        stateListAnimator.addState(StateSet.trimStateSet(states, stateIndex2), animator);
                    }
                    break;
            }
        }
    }

    private static class PathDataEvaluator implements TypeEvaluator<PathParser.PathDataNode[]> {
        private PathParser.PathDataNode[] mNodeArray;

        private PathDataEvaluator() {
        }

        public PathDataEvaluator(PathParser.PathDataNode[] nodeArray) {
            this.mNodeArray = nodeArray;
        }

        @Override
        public PathParser.PathDataNode[] evaluate(float fraction, PathParser.PathDataNode[] startPathData, PathParser.PathDataNode[] endPathData) {
            if (!PathParser.canMorph(startPathData, endPathData)) {
                throw new IllegalArgumentException("Can't interpolate between two incompatible pathData");
            }
            if (this.mNodeArray == null || !PathParser.canMorph(this.mNodeArray, startPathData)) {
                this.mNodeArray = PathParser.deepCopyNodes(startPathData);
            }
            for (int i = 0; i < startPathData.length; i++) {
                this.mNodeArray[i].interpolatePathDataNode(startPathData[i], endPathData[i], fraction);
            }
            return this.mNodeArray;
        }
    }

    private static void parseAnimatorFromTypeArray(ValueAnimator anim, TypedArray arrayAnimator, TypedArray arrayObjectAnimator, float pixelSize) {
        long duration = arrayAnimator.getInt(1, 300);
        long startDelay = arrayAnimator.getInt(2, 0);
        int valueType = arrayAnimator.getInt(7, 0);
        TypeEvaluator evaluator = null;
        boolean getFloats = valueType == 0;
        TypedValue tvFrom = arrayAnimator.peekValue(5);
        boolean hasFrom = tvFrom != null;
        int fromType = hasFrom ? tvFrom.type : 0;
        TypedValue tvTo = arrayAnimator.peekValue(6);
        boolean hasTo = tvTo != null;
        int toType = hasTo ? tvTo.type : 0;
        if (valueType == 2) {
            evaluator = setupAnimatorForPath(anim, arrayAnimator);
        } else {
            if ((hasFrom && fromType >= 28 && fromType <= 31) || (hasTo && toType >= 28 && toType <= 31)) {
                getFloats = false;
                evaluator = ArgbEvaluator.getInstance();
            }
            setupValues(anim, arrayAnimator, getFloats, hasFrom, fromType, hasTo, toType);
        }
        anim.setDuration(duration);
        anim.setStartDelay(startDelay);
        if (arrayAnimator.hasValue(3)) {
            anim.setRepeatCount(arrayAnimator.getInt(3, 0));
        }
        if (arrayAnimator.hasValue(4)) {
            anim.setRepeatMode(arrayAnimator.getInt(4, 1));
        }
        if (evaluator != null) {
            anim.setEvaluator(evaluator);
        }
        if (arrayObjectAnimator != null) {
            setupObjectAnimator(anim, arrayObjectAnimator, getFloats, pixelSize);
        }
    }

    private static TypeEvaluator setupAnimatorForPath(ValueAnimator anim, TypedArray arrayAnimator) {
        String fromString = arrayAnimator.getString(5);
        String toString = arrayAnimator.getString(6);
        PathParser.PathDataNode[] nodesFrom = PathParser.createNodesFromPathData(fromString);
        PathParser.PathDataNode[] nodesTo = PathParser.createNodesFromPathData(toString);
        if (nodesFrom != null) {
            if (nodesTo != null) {
                anim.setObjectValues(nodesFrom, nodesTo);
                if (!PathParser.canMorph(nodesFrom, nodesTo)) {
                    throw new InflateException(arrayAnimator.getPositionDescription() + " Can't morph from " + fromString + " to " + toString);
                }
            } else {
                anim.setObjectValues(nodesFrom);
            }
            TypeEvaluator evaluator = new PathDataEvaluator(PathParser.deepCopyNodes(nodesFrom));
            return evaluator;
        }
        if (nodesTo == null) {
            return null;
        }
        anim.setObjectValues(nodesTo);
        TypeEvaluator evaluator2 = new PathDataEvaluator(PathParser.deepCopyNodes(nodesTo));
        return evaluator2;
    }

    private static void setupObjectAnimator(ValueAnimator anim, TypedArray arrayObjectAnimator, boolean getFloats, float pixelSize) {
        Keyframes xKeyframes;
        Keyframes yKeyframes;
        ObjectAnimator oa = (ObjectAnimator) anim;
        String pathData = arrayObjectAnimator.getString(1);
        if (pathData != null) {
            String propertyXName = arrayObjectAnimator.getString(2);
            String propertyYName = arrayObjectAnimator.getString(3);
            if (propertyXName == null && propertyYName == null) {
                throw new InflateException(arrayObjectAnimator.getPositionDescription() + " propertyXName or propertyYName is needed for PathData");
            }
            Path path = PathParser.createPathFromPathData(pathData);
            float error = 0.5f * pixelSize;
            PathKeyframes keyframeSet = KeyframeSet.ofPath(path, error);
            if (getFloats) {
                xKeyframes = keyframeSet.createXFloatKeyframes();
                yKeyframes = keyframeSet.createYFloatKeyframes();
            } else {
                xKeyframes = keyframeSet.createXIntKeyframes();
                yKeyframes = keyframeSet.createYIntKeyframes();
            }
            PropertyValuesHolder x = null;
            PropertyValuesHolder y = null;
            if (propertyXName != null) {
                x = PropertyValuesHolder.ofKeyframes(propertyXName, xKeyframes);
            }
            if (propertyYName != null) {
                y = PropertyValuesHolder.ofKeyframes(propertyYName, yKeyframes);
            }
            if (x == null) {
                oa.setValues(y);
                return;
            } else if (y == null) {
                oa.setValues(x);
                return;
            } else {
                oa.setValues(x, y);
                return;
            }
        }
        String propertyName = arrayObjectAnimator.getString(0);
        oa.setPropertyName(propertyName);
    }

    private static void setupValues(ValueAnimator anim, TypedArray arrayAnimator, boolean getFloats, boolean hasFrom, int fromType, boolean hasTo, int toType) {
        int valueTo;
        int valueFrom;
        int valueTo2;
        if (getFloats) {
            if (hasFrom) {
                float valueFrom2 = fromType == 5 ? arrayAnimator.getDimension(5, 0.0f) : arrayAnimator.getFloat(5, 0.0f);
                if (hasTo) {
                    float valueTo3 = toType == 5 ? arrayAnimator.getDimension(6, 0.0f) : arrayAnimator.getFloat(6, 0.0f);
                    anim.setFloatValues(valueFrom2, valueTo3);
                    return;
                } else {
                    anim.setFloatValues(valueFrom2);
                    return;
                }
            }
            float valueTo4 = toType == 5 ? arrayAnimator.getDimension(6, 0.0f) : arrayAnimator.getFloat(6, 0.0f);
            anim.setFloatValues(valueTo4);
            return;
        }
        if (hasFrom) {
            if (fromType == 5) {
                valueFrom = (int) arrayAnimator.getDimension(5, 0.0f);
            } else {
                valueFrom = (fromType < 28 || fromType > 31) ? arrayAnimator.getInt(5, 0) : arrayAnimator.getColor(5, 0);
            }
            if (hasTo) {
                if (toType == 5) {
                    valueTo2 = (int) arrayAnimator.getDimension(6, 0.0f);
                } else {
                    valueTo2 = (toType < 28 || toType > 31) ? arrayAnimator.getInt(6, 0) : arrayAnimator.getColor(6, 0);
                }
                anim.setIntValues(valueFrom, valueTo2);
                return;
            }
            anim.setIntValues(valueFrom);
            return;
        }
        if (hasTo) {
            if (toType == 5) {
                valueTo = (int) arrayAnimator.getDimension(6, 0.0f);
            } else {
                valueTo = (toType < 28 || toType > 31) ? arrayAnimator.getInt(6, 0) : arrayAnimator.getColor(6, 0);
            }
            anim.setIntValues(valueTo);
        }
    }

    private static Animator createAnimatorFromXml(Resources res, Resources.Theme theme, XmlPullParser parser, float pixelSize) throws XmlPullParserException, IOException {
        return createAnimatorFromXml(res, theme, parser, Xml.asAttributeSet(parser), null, 0, pixelSize);
    }

    private static Animator createAnimatorFromXml(Resources res, Resources.Theme theme, XmlPullParser parser, AttributeSet attrs, AnimatorSet parent, int sequenceOrdering, float pixelSize) throws XmlPullParserException, IOException {
        TypedArray a;
        Animator anim = null;
        ArrayList<Animator> childAnims = null;
        int depth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if ((type == 3 && parser.getDepth() <= depth) || type == 1) {
                break;
            }
            if (type == 2) {
                String name = parser.getName();
                if (name.equals("objectAnimator")) {
                    anim = loadObjectAnimator(res, theme, attrs, pixelSize);
                } else if (name.equals("animator")) {
                    anim = loadAnimator(res, theme, attrs, null, pixelSize);
                } else if (name.equals("set")) {
                    anim = new AnimatorSet();
                    if (theme != null) {
                        a = theme.obtainStyledAttributes(attrs, R.styleable.AnimatorSet, 0, 0);
                    } else {
                        a = res.obtainAttributes(attrs, R.styleable.AnimatorSet);
                    }
                    anim.appendChangingConfigurations(a.getChangingConfigurations());
                    int ordering = a.getInt(0, 0);
                    createAnimatorFromXml(res, theme, parser, attrs, (AnimatorSet) anim, ordering, pixelSize);
                    a.recycle();
                } else {
                    throw new RuntimeException("Unknown animator name: " + parser.getName());
                }
                if (parent != null) {
                    if (childAnims == null) {
                        childAnims = new ArrayList<>();
                    }
                    childAnims.add(anim);
                }
            }
        }
    }

    private static ObjectAnimator loadObjectAnimator(Resources res, Resources.Theme theme, AttributeSet attrs, float pathErrorScale) throws Resources.NotFoundException {
        ObjectAnimator anim = new ObjectAnimator();
        loadAnimator(res, theme, attrs, anim, pathErrorScale);
        return anim;
    }

    private static ValueAnimator loadAnimator(Resources res, Resources.Theme theme, AttributeSet attrs, ValueAnimator anim, float pathErrorScale) throws Resources.NotFoundException {
        TypedArray arrayAnimator;
        TypedArray arrayObjectAnimator = null;
        if (theme != null) {
            arrayAnimator = theme.obtainStyledAttributes(attrs, R.styleable.Animator, 0, 0);
        } else {
            arrayAnimator = res.obtainAttributes(attrs, R.styleable.Animator);
        }
        if (anim != null) {
            if (theme != null) {
                arrayObjectAnimator = theme.obtainStyledAttributes(attrs, R.styleable.PropertyAnimator, 0, 0);
            } else {
                arrayObjectAnimator = res.obtainAttributes(attrs, R.styleable.PropertyAnimator);
            }
            anim.appendChangingConfigurations(arrayObjectAnimator.getChangingConfigurations());
        }
        if (anim == null) {
            anim = new ValueAnimator();
        }
        anim.appendChangingConfigurations(arrayAnimator.getChangingConfigurations());
        parseAnimatorFromTypeArray(anim, arrayAnimator, arrayObjectAnimator, pathErrorScale);
        int resID = arrayAnimator.getResourceId(0, 0);
        if (resID > 0) {
            Interpolator interpolator = AnimationUtils.loadInterpolator(res, theme, resID);
            if (interpolator instanceof BaseInterpolator) {
                anim.appendChangingConfigurations(((BaseInterpolator) interpolator).getChangingConfiguration());
            }
            anim.setInterpolator(interpolator);
        }
        arrayAnimator.recycle();
        if (arrayObjectAnimator != null) {
            arrayObjectAnimator.recycle();
        }
        return anim;
    }

    private static int getChangingConfigs(Resources resources, int id) {
        int i;
        synchronized (sTmpTypedValue) {
            resources.getValue(id, sTmpTypedValue, true);
            i = sTmpTypedValue.changingConfigurations;
        }
        return i;
    }
}
