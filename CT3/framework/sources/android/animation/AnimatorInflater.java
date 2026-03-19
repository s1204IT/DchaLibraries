package android.animation;

import android.content.Context;
import android.content.res.ConfigurationBoundResourceCache;
import android.content.res.ConstantState;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Path;
import android.net.ProxyInfo;
import android.util.AttributeSet;
import android.util.Log;
import android.util.PathParser;
import android.util.StateSet;
import android.util.TypedValue;
import android.util.Xml;
import android.view.InflateException;
import android.view.animation.AnimationUtils;
import android.view.animation.BaseInterpolator;
import android.view.animation.Interpolator;
import com.android.internal.R;
import dalvik.system.BlockGuard;
import java.io.IOException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AnimatorInflater {
    private static final boolean DBG_ANIMATOR_INFLATER = false;
    private static final int SEQUENTIALLY = 1;
    private static final String TAG = "AnimatorInflater";
    private static final int TOGETHER = 0;
    private static final int VALUE_TYPE_COLOR = 3;
    private static final int VALUE_TYPE_FLOAT = 0;
    private static final int VALUE_TYPE_INT = 1;
    private static final int VALUE_TYPE_PATH = 2;
    private static final int VALUE_TYPE_UNDEFINED = 4;
    private static final TypedValue sTmpTypedValue = new TypedValue();

    public static Animator loadAnimator(Context context, int id) throws Resources.NotFoundException {
        return loadAnimator(context.getResources(), context.getTheme(), id);
    }

    public static Animator loadAnimator(Resources resources, Resources.Theme theme, int id) throws Resources.NotFoundException {
        return loadAnimator(resources, theme, id, 1.0f);
    }

    public static Animator loadAnimator(Resources resources, Resources.Theme theme, int id, float pathErrorScale) throws Resources.NotFoundException {
        ConfigurationBoundResourceCache<Animator> animatorCache = resources.getAnimatorCache();
        Animator animator = animatorCache.getInstance(id, resources, theme);
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
                        animator2 = constantState.newInstance2(resources, theme);
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
        StateListAnimator animator = cache.getInstance(id, resources, theme);
        if (animator != null) {
            return animator;
        }
        XmlResourceParser parser = null;
        try {
            try {
                parser = resources.getAnimation(id);
                StateListAnimator animator2 = createStateListAnimatorFromXml(context, parser, Xml.asAttributeSet(parser));
                if (animator2 != null) {
                    animator2.appendChangingConfigurations(getChangingConfigs(resources, id));
                    ConstantState<StateListAnimator> constantState = animator2.createConstantState();
                    if (constantState != null) {
                        cache.put(id, theme, constantState);
                        animator2 = constantState.newInstance2(resources, theme);
                    }
                }
                return animator2;
            } catch (IOException ex) {
                Resources.NotFoundException rnf = new Resources.NotFoundException("Can't load state list animator resource ID #0x" + Integer.toHexString(id));
                rnf.initCause(ex);
                throw rnf;
            } catch (XmlPullParserException ex2) {
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

    private static class PathDataEvaluator implements TypeEvaluator<PathParser.PathData> {
        private final PathParser.PathData mPathData;

        PathDataEvaluator(PathDataEvaluator pathDataEvaluator) {
            this();
        }

        private PathDataEvaluator() {
            this.mPathData = new PathParser.PathData();
        }

        @Override
        public PathParser.PathData evaluate(float fraction, PathParser.PathData startPathData, PathParser.PathData endPathData) {
            if (!PathParser.interpolatePathData(this.mPathData, startPathData, endPathData, fraction)) {
                throw new IllegalArgumentException("Can't interpolate between two incompatible pathData");
            }
            return this.mPathData;
        }
    }

    private static PropertyValuesHolder getPVH(TypedArray styledAttributes, int valueType, int valueFromId, int valueToId, String propertyName) throws BlockGuard.BlockGuardPolicyException {
        int valueTo;
        int valueFrom;
        int valueTo2;
        float valueTo3;
        float valueFrom2;
        float valueTo4;
        TypedValue tvFrom = styledAttributes.peekValue(valueFromId);
        boolean hasFrom = tvFrom != null;
        int fromType = hasFrom ? tvFrom.type : 0;
        TypedValue tvTo = styledAttributes.peekValue(valueToId);
        boolean hasTo = tvTo != null;
        int toType = hasTo ? tvTo.type : 0;
        if (valueType == 4) {
            if ((hasFrom && isColorType(fromType)) || (hasTo && isColorType(toType))) {
                valueType = 3;
            } else {
                valueType = 0;
            }
        }
        boolean getFloats = valueType == 0;
        PropertyValuesHolder returnValue = null;
        if (valueType == 2) {
            String fromString = styledAttributes.getString(valueFromId);
            String toString = styledAttributes.getString(valueToId);
            PathParser.PathData pathData = fromString == null ? null : new PathParser.PathData(fromString);
            PathParser.PathData pathData2 = toString == null ? null : new PathParser.PathData(toString);
            if (pathData == null && pathData2 == null) {
                return null;
            }
            if (pathData != null) {
                TypeEvaluator evaluator = new PathDataEvaluator(null);
                if (pathData2 != null) {
                    if (!PathParser.canMorph(pathData, pathData2)) {
                        throw new InflateException(" Can't morph from " + fromString + " to " + toString);
                    }
                    PropertyValuesHolder returnValue2 = PropertyValuesHolder.ofObject(propertyName, evaluator, pathData, pathData2);
                    return returnValue2;
                }
                PropertyValuesHolder returnValue3 = PropertyValuesHolder.ofObject(propertyName, evaluator, pathData);
                return returnValue3;
            }
            if (pathData2 == null) {
                return null;
            }
            TypeEvaluator evaluator2 = new PathDataEvaluator(null);
            PropertyValuesHolder returnValue4 = PropertyValuesHolder.ofObject(propertyName, evaluator2, pathData2);
            return returnValue4;
        }
        TypeEvaluator evaluator3 = null;
        if (valueType == 3) {
            evaluator3 = ArgbEvaluator.getInstance();
        }
        if (getFloats) {
            if (hasFrom) {
                if (fromType == 5) {
                    valueFrom2 = styledAttributes.getDimension(valueFromId, 0.0f);
                } else {
                    valueFrom2 = styledAttributes.getFloat(valueFromId, 0.0f);
                }
                if (hasTo) {
                    if (toType == 5) {
                        valueTo4 = styledAttributes.getDimension(valueToId, 0.0f);
                    } else {
                        valueTo4 = styledAttributes.getFloat(valueToId, 0.0f);
                    }
                    returnValue = PropertyValuesHolder.ofFloat(propertyName, valueFrom2, valueTo4);
                } else {
                    returnValue = PropertyValuesHolder.ofFloat(propertyName, valueFrom2);
                }
            } else {
                if (toType == 5) {
                    valueTo3 = styledAttributes.getDimension(valueToId, 0.0f);
                } else {
                    valueTo3 = styledAttributes.getFloat(valueToId, 0.0f);
                }
                returnValue = PropertyValuesHolder.ofFloat(propertyName, valueTo3);
            }
        } else if (hasFrom) {
            if (fromType == 5) {
                valueFrom = (int) styledAttributes.getDimension(valueFromId, 0.0f);
            } else if (isColorType(fromType)) {
                valueFrom = styledAttributes.getColor(valueFromId, 0);
            } else {
                valueFrom = styledAttributes.getInt(valueFromId, 0);
            }
            if (hasTo) {
                if (toType == 5) {
                    valueTo2 = (int) styledAttributes.getDimension(valueToId, 0.0f);
                } else if (isColorType(toType)) {
                    valueTo2 = styledAttributes.getColor(valueToId, 0);
                } else {
                    valueTo2 = styledAttributes.getInt(valueToId, 0);
                }
                returnValue = PropertyValuesHolder.ofInt(propertyName, valueFrom, valueTo2);
            } else {
                returnValue = PropertyValuesHolder.ofInt(propertyName, valueFrom);
            }
        } else if (hasTo) {
            if (toType == 5) {
                valueTo = (int) styledAttributes.getDimension(valueToId, 0.0f);
            } else if (isColorType(toType)) {
                valueTo = styledAttributes.getColor(valueToId, 0);
            } else {
                valueTo = styledAttributes.getInt(valueToId, 0);
            }
            returnValue = PropertyValuesHolder.ofInt(propertyName, valueTo);
        }
        if (returnValue != null && evaluator3 != null) {
            returnValue.setEvaluator(evaluator3);
            return returnValue;
        }
        return returnValue;
    }

    private static void parseAnimatorFromTypeArray(ValueAnimator anim, TypedArray arrayAnimator, TypedArray arrayObjectAnimator, float pixelSize) throws BlockGuard.BlockGuardPolicyException {
        long duration = arrayAnimator.getInt(1, 300);
        long startDelay = arrayAnimator.getInt(2, 0);
        int valueType = arrayAnimator.getInt(7, 4);
        if (valueType == 4) {
            valueType = inferValueTypeFromValues(arrayAnimator, 5, 6);
        }
        PropertyValuesHolder pvh = getPVH(arrayAnimator, valueType, 5, 6, ProxyInfo.LOCAL_EXCL_LIST);
        if (pvh != null) {
            anim.setValues(pvh);
        }
        anim.setDuration(duration);
        anim.setStartDelay(startDelay);
        if (arrayAnimator.hasValue(3)) {
            anim.setRepeatCount(arrayAnimator.getInt(3, 0));
        }
        if (arrayAnimator.hasValue(4)) {
            anim.setRepeatMode(arrayAnimator.getInt(4, 1));
        }
        if (arrayObjectAnimator == null) {
            return;
        }
        setupObjectAnimator(anim, arrayObjectAnimator, valueType == 0, pixelSize);
    }

    private static TypeEvaluator setupAnimatorForPath(ValueAnimator anim, TypedArray arrayAnimator) {
        PathDataEvaluator pathDataEvaluator = null;
        String fromString = arrayAnimator.getString(5);
        String toString = arrayAnimator.getString(6);
        PathParser.PathData pathData = fromString == null ? null : new PathParser.PathData(fromString);
        PathParser.PathData pathData2 = toString == null ? null : new PathParser.PathData(toString);
        if (pathData != null) {
            if (pathData2 != null) {
                anim.setObjectValues(pathData, pathData2);
                if (!PathParser.canMorph(pathData, pathData2)) {
                    throw new InflateException(arrayAnimator.getPositionDescription() + " Can't morph from " + fromString + " to " + toString);
                }
            } else {
                anim.setObjectValues(pathData);
            }
            TypeEvaluator evaluator = new PathDataEvaluator(pathDataEvaluator);
            return evaluator;
        }
        if (pathData2 == null) {
            return null;
        }
        anim.setObjectValues(pathData2);
        TypeEvaluator evaluator2 = new PathDataEvaluator(pathDataEvaluator);
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
                valueFrom = isColorType(fromType) ? arrayAnimator.getColor(5, 0) : arrayAnimator.getInt(5, 0);
            }
            if (hasTo) {
                if (toType == 5) {
                    valueTo2 = (int) arrayAnimator.getDimension(6, 0.0f);
                } else {
                    valueTo2 = isColorType(toType) ? arrayAnimator.getColor(6, 0) : arrayAnimator.getInt(6, 0);
                }
                anim.setIntValues(valueFrom, valueTo2);
                return;
            }
            anim.setIntValues(valueFrom);
            return;
        }
        if (!hasTo) {
            return;
        }
        if (toType == 5) {
            valueTo = (int) arrayAnimator.getDimension(6, 0.0f);
        } else {
            valueTo = isColorType(toType) ? arrayAnimator.getColor(6, 0) : arrayAnimator.getInt(6, 0);
        }
        anim.setIntValues(valueTo);
    }

    private static Animator createAnimatorFromXml(Resources res, Resources.Theme theme, XmlPullParser parser, float pixelSize) throws XmlPullParserException, IOException {
        return createAnimatorFromXml(res, theme, parser, Xml.asAttributeSet(parser), null, 0, pixelSize);
    }

    private static Animator createAnimatorFromXml(Resources res, Resources.Theme theme, XmlPullParser parser, AttributeSet attrs, AnimatorSet parent, int sequenceOrdering, float pixelSize) throws XmlPullParserException, BlockGuard.BlockGuardPolicyException, IOException {
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
                boolean gotValues = false;
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
                } else if (name.equals("propertyValuesHolder")) {
                    PropertyValuesHolder[] values = loadValues(res, theme, parser, Xml.asAttributeSet(parser));
                    if (values != null && anim != null && (anim instanceof ValueAnimator)) {
                        ((ValueAnimator) anim).setValues(values);
                    }
                    gotValues = true;
                } else {
                    throw new RuntimeException("Unknown animator name: " + parser.getName());
                }
                if (parent != null && !gotValues) {
                    if (childAnims == null) {
                        childAnims = new ArrayList<>();
                    }
                    childAnims.add(anim);
                }
            }
        }
    }

    private static PropertyValuesHolder[] loadValues(Resources res, Resources.Theme theme, XmlPullParser parser, AttributeSet attrs) throws XmlPullParserException, BlockGuard.BlockGuardPolicyException, IOException {
        TypedArray a;
        ArrayList arrayList = null;
        while (true) {
            int type = parser.getEventType();
            if (type == 3 || type == 1) {
                break;
            }
            if (type != 2) {
                parser.next();
            } else {
                String name = parser.getName();
                if (name.equals("propertyValuesHolder")) {
                    if (theme != null) {
                        a = theme.obtainStyledAttributes(attrs, R.styleable.PropertyValuesHolder, 0, 0);
                    } else {
                        a = res.obtainAttributes(attrs, R.styleable.PropertyValuesHolder);
                    }
                    String propertyName = a.getString(3);
                    int valueType = a.getInt(2, 4);
                    PropertyValuesHolder pvh = loadPvh(res, theme, parser, propertyName, valueType);
                    if (pvh == null) {
                        pvh = getPVH(a, valueType, 0, 1, propertyName);
                    }
                    if (pvh != null) {
                        if (arrayList == null) {
                            arrayList = new ArrayList();
                        }
                        arrayList.add(pvh);
                    }
                    a.recycle();
                }
                parser.next();
            }
        }
        PropertyValuesHolder[] valuesArray = null;
        if (arrayList != null) {
            int count = arrayList.size();
            valuesArray = new PropertyValuesHolder[count];
            for (int i = 0; i < count; i++) {
                valuesArray[i] = (PropertyValuesHolder) arrayList.get(i);
            }
        }
        return valuesArray;
    }

    private static int inferValueTypeOfKeyframe(Resources res, Resources.Theme theme, AttributeSet attrs) {
        TypedArray a;
        int valueType;
        if (theme != null) {
            a = theme.obtainStyledAttributes(attrs, R.styleable.Keyframe, 0, 0);
        } else {
            a = res.obtainAttributes(attrs, R.styleable.Keyframe);
        }
        TypedValue keyframeValue = a.peekValue(0);
        boolean hasValue = keyframeValue != null;
        if (hasValue && isColorType(keyframeValue.type)) {
            valueType = 3;
        } else {
            valueType = 0;
        }
        a.recycle();
        return valueType;
    }

    private static int inferValueTypeFromValues(TypedArray styledAttributes, int valueFromId, int valueToId) {
        TypedValue tvFrom = styledAttributes.peekValue(valueFromId);
        boolean hasFrom = tvFrom != null;
        int fromType = hasFrom ? tvFrom.type : 0;
        TypedValue tvTo = styledAttributes.peekValue(valueToId);
        boolean hasTo = tvTo != null;
        int toType = hasTo ? tvTo.type : 0;
        if ((hasFrom && isColorType(fromType)) || (hasTo && isColorType(toType))) {
            return 3;
        }
        return 0;
    }

    private static void dumpKeyframes(Object[] keyframes, String header) {
        if (keyframes == null || keyframes.length == 0) {
            return;
        }
        Log.d(TAG, header);
        int count = keyframes.length;
        for (int i = 0; i < count; i++) {
            Keyframe keyframe = (Keyframe) keyframes[i];
            Log.d(TAG, "Keyframe " + i + ": fraction " + (keyframe.getFraction() < 0.0f ? "null" : Float.valueOf(keyframe.getFraction())) + ", , value : " + (keyframe.hasValue() ? keyframe.getValue() : "null"));
        }
    }

    private static PropertyValuesHolder loadPvh(Resources res, Resources.Theme theme, XmlPullParser parser, String propertyName, int valueType) throws XmlPullParserException, BlockGuard.BlockGuardPolicyException, IOException {
        int count;
        PropertyValuesHolder value = null;
        ArrayList arrayList = null;
        while (true) {
            int type = parser.next();
            if (type == 3 || type == 1) {
                break;
            }
            String name = parser.getName();
            if (name.equals("keyframe")) {
                if (valueType == 4) {
                    valueType = inferValueTypeOfKeyframe(res, theme, Xml.asAttributeSet(parser));
                }
                Keyframe keyframe = loadKeyframe(res, theme, Xml.asAttributeSet(parser), valueType);
                if (keyframe != null) {
                    if (arrayList == null) {
                        arrayList = new ArrayList();
                    }
                    arrayList.add(keyframe);
                }
                parser.next();
            }
        }
        if (arrayList != null && (count = arrayList.size()) > 0) {
            Keyframe firstKeyframe = (Keyframe) arrayList.get(0);
            Keyframe lastKeyframe = (Keyframe) arrayList.get(count - 1);
            float endFraction = lastKeyframe.getFraction();
            if (endFraction < 1.0f) {
                if (endFraction < 0.0f) {
                    lastKeyframe.setFraction(1.0f);
                } else {
                    arrayList.add(arrayList.size(), createNewKeyframe(lastKeyframe, 1.0f));
                    count++;
                }
            }
            float startFraction = firstKeyframe.getFraction();
            if (startFraction != 0.0f) {
                if (startFraction < 0.0f) {
                    firstKeyframe.setFraction(0.0f);
                } else {
                    arrayList.add(0, createNewKeyframe(firstKeyframe, 0.0f));
                    count++;
                }
            }
            Keyframe[] keyframeArray = new Keyframe[count];
            arrayList.toArray(keyframeArray);
            for (int i = 0; i < count; i++) {
                Keyframe keyframe2 = keyframeArray[i];
                if (keyframe2.getFraction() < 0.0f) {
                    if (i == 0) {
                        keyframe2.setFraction(0.0f);
                    } else if (i == count - 1) {
                        keyframe2.setFraction(1.0f);
                    } else {
                        int startIndex = i;
                        int endIndex = i;
                        for (int j = i + 1; j < count - 1 && keyframeArray[j].getFraction() < 0.0f; j++) {
                            endIndex = j;
                        }
                        float gap = keyframeArray[endIndex + 1].getFraction() - keyframeArray[startIndex - 1].getFraction();
                        distributeKeyframes(keyframeArray, gap, startIndex, endIndex);
                    }
                }
            }
            value = PropertyValuesHolder.ofKeyframe(propertyName, keyframeArray);
            if (valueType == 3) {
                value.setEvaluator(ArgbEvaluator.getInstance());
            }
        }
        return value;
    }

    private static Keyframe createNewKeyframe(Keyframe sampleKeyframe, float fraction) {
        if (sampleKeyframe.getType() == Float.TYPE) {
            return Keyframe.ofFloat(fraction);
        }
        if (sampleKeyframe.getType() == Integer.TYPE) {
            return Keyframe.ofInt(fraction);
        }
        return Keyframe.ofObject(fraction);
    }

    private static void distributeKeyframes(Keyframe[] keyframes, float gap, int startIndex, int endIndex) {
        int count = (endIndex - startIndex) + 2;
        float increment = gap / count;
        for (int i = startIndex; i <= endIndex; i++) {
            keyframes[i].setFraction(keyframes[i - 1].getFraction() + increment);
        }
    }

    private static Keyframe loadKeyframe(Resources res, Resources.Theme theme, AttributeSet attrs, int valueType) throws XmlPullParserException, BlockGuard.BlockGuardPolicyException, IOException {
        TypedArray a;
        if (theme != null) {
            a = theme.obtainStyledAttributes(attrs, R.styleable.Keyframe, 0, 0);
        } else {
            a = res.obtainAttributes(attrs, R.styleable.Keyframe);
        }
        Keyframe keyframe = null;
        float fraction = a.getFloat(3, -1.0f);
        TypedValue keyframeValue = a.peekValue(0);
        boolean hasValue = keyframeValue != null;
        if (valueType == 4) {
            if (hasValue && isColorType(keyframeValue.type)) {
                valueType = 3;
            } else {
                valueType = 0;
            }
        }
        if (hasValue) {
            switch (valueType) {
                case 0:
                    float value = a.getFloat(0, 0.0f);
                    keyframe = Keyframe.ofFloat(fraction, value);
                    break;
                case 1:
                case 3:
                    int intValue = a.getInt(0, 0);
                    keyframe = Keyframe.ofInt(fraction, intValue);
                    break;
            }
        } else {
            keyframe = valueType == 0 ? Keyframe.ofFloat(fraction) : Keyframe.ofInt(fraction);
        }
        int resID = a.getResourceId(1, 0);
        if (resID > 0) {
            Interpolator interpolator = AnimationUtils.loadInterpolator(res, theme, resID);
            keyframe.setInterpolator(interpolator);
        }
        a.recycle();
        return keyframe;
    }

    private static ObjectAnimator loadObjectAnimator(Resources res, Resources.Theme theme, AttributeSet attrs, float pathErrorScale) throws Resources.NotFoundException, BlockGuard.BlockGuardPolicyException {
        ObjectAnimator anim = new ObjectAnimator();
        loadAnimator(res, theme, attrs, anim, pathErrorScale);
        return anim;
    }

    private static ValueAnimator loadAnimator(Resources res, Resources.Theme theme, AttributeSet attrs, ValueAnimator anim, float pathErrorScale) throws Resources.NotFoundException, BlockGuard.BlockGuardPolicyException {
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

    private static boolean isColorType(int type) {
        return type >= 28 && type <= 31;
    }
}
