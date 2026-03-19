package android.view.animation;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Xml;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AnimationUtils {
    private static final int SEQUENTIALLY = 1;
    private static final int TOGETHER = 0;
    private static ThreadLocal<AnimationState> sAnimationState = new ThreadLocal<AnimationState>() {
        @Override
        protected AnimationState initialValue() {
            return new AnimationState();
        }
    };

    private static class AnimationState {
        boolean animationClockLocked;
        long currentVsyncTimeMillis;
        long lastReportedTimeMillis;

        private AnimationState() {
        }
    }

    public static void lockAnimationClock(long j) {
        AnimationState animationState = sAnimationState.get();
        animationState.animationClockLocked = true;
        animationState.currentVsyncTimeMillis = j;
    }

    public static void unlockAnimationClock() {
        sAnimationState.get().animationClockLocked = false;
    }

    public static long currentAnimationTimeMillis() {
        AnimationState animationState = sAnimationState.get();
        if (animationState.animationClockLocked) {
            return Math.max(animationState.currentVsyncTimeMillis, animationState.lastReportedTimeMillis);
        }
        animationState.lastReportedTimeMillis = SystemClock.uptimeMillis();
        return animationState.lastReportedTimeMillis;
    }

    public static Animation loadAnimation(Context context, int i) throws Throwable {
        XmlResourceParser animation;
        XmlResourceParser xmlResourceParser = null;
        try {
            try {
                animation = context.getResources().getAnimation(i);
            } catch (Throwable th) {
                th = th;
            }
            try {
                Animation animationCreateAnimationFromXml = createAnimationFromXml(context, animation);
                if (animation != null) {
                    animation.close();
                }
                return animationCreateAnimationFromXml;
            } catch (IOException e) {
                e = e;
                Resources.NotFoundException notFoundException = new Resources.NotFoundException("Can't load animation resource ID #0x" + Integer.toHexString(i));
                notFoundException.initCause(e);
                throw notFoundException;
            } catch (XmlPullParserException e2) {
                e = e2;
                Resources.NotFoundException notFoundException2 = new Resources.NotFoundException("Can't load animation resource ID #0x" + Integer.toHexString(i));
                notFoundException2.initCause(e);
                throw notFoundException2;
            } catch (Throwable th2) {
                th = th2;
                xmlResourceParser = animation;
                if (xmlResourceParser != null) {
                    xmlResourceParser.close();
                }
                throw th;
            }
        } catch (IOException e3) {
            e = e3;
        } catch (XmlPullParserException e4) {
            e = e4;
        }
    }

    private static Animation createAnimationFromXml(Context context, XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        return createAnimationFromXml(context, xmlPullParser, null, Xml.asAttributeSet(xmlPullParser));
    }

    private static Animation createAnimationFromXml(Context context, XmlPullParser xmlPullParser, AnimationSet animationSet, AttributeSet attributeSet) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        Animation clipRectAnimation = null;
        while (true) {
            int next = xmlPullParser.next();
            if ((next == 3 && xmlPullParser.getDepth() <= depth) || next == 1) {
                break;
            }
            if (next == 2) {
                String name = xmlPullParser.getName();
                if (name.equals("set")) {
                    clipRectAnimation = new AnimationSet(context, attributeSet);
                    createAnimationFromXml(context, xmlPullParser, (AnimationSet) clipRectAnimation, attributeSet);
                } else if (name.equals("alpha")) {
                    clipRectAnimation = new AlphaAnimation(context, attributeSet);
                } else if (name.equals(BatteryManager.EXTRA_SCALE)) {
                    clipRectAnimation = new ScaleAnimation(context, attributeSet);
                } else if (name.equals("rotate")) {
                    clipRectAnimation = new RotateAnimation(context, attributeSet);
                } else if (name.equals("translate")) {
                    clipRectAnimation = new TranslateAnimation(context, attributeSet);
                } else if (name.equals("cliprect")) {
                    clipRectAnimation = new ClipRectAnimation(context, attributeSet);
                } else {
                    throw new RuntimeException("Unknown animation name: " + xmlPullParser.getName());
                }
                if (animationSet != null) {
                    animationSet.addAnimation(clipRectAnimation);
                }
            }
        }
    }

    public static LayoutAnimationController loadLayoutAnimation(Context context, int i) throws Throwable {
        XmlResourceParser animation;
        XmlResourceParser xmlResourceParser = null;
        try {
            try {
                animation = context.getResources().getAnimation(i);
            } catch (Throwable th) {
                th = th;
            }
            try {
                LayoutAnimationController layoutAnimationControllerCreateLayoutAnimationFromXml = createLayoutAnimationFromXml(context, animation);
                if (animation != null) {
                    animation.close();
                }
                return layoutAnimationControllerCreateLayoutAnimationFromXml;
            } catch (IOException e) {
                e = e;
                Resources.NotFoundException notFoundException = new Resources.NotFoundException("Can't load animation resource ID #0x" + Integer.toHexString(i));
                notFoundException.initCause(e);
                throw notFoundException;
            } catch (XmlPullParserException e2) {
                e = e2;
                Resources.NotFoundException notFoundException2 = new Resources.NotFoundException("Can't load animation resource ID #0x" + Integer.toHexString(i));
                notFoundException2.initCause(e);
                throw notFoundException2;
            } catch (Throwable th2) {
                th = th2;
                xmlResourceParser = animation;
                if (xmlResourceParser != null) {
                    xmlResourceParser.close();
                }
                throw th;
            }
        } catch (IOException e3) {
            e = e3;
        } catch (XmlPullParserException e4) {
            e = e4;
        }
    }

    private static LayoutAnimationController createLayoutAnimationFromXml(Context context, XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        return createLayoutAnimationFromXml(context, xmlPullParser, Xml.asAttributeSet(xmlPullParser));
    }

    private static LayoutAnimationController createLayoutAnimationFromXml(Context context, XmlPullParser xmlPullParser, AttributeSet attributeSet) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        LayoutAnimationController layoutAnimationController = null;
        while (true) {
            int next = xmlPullParser.next();
            if ((next == 3 && xmlPullParser.getDepth() <= depth) || next == 1) {
                break;
            }
            if (next == 2) {
                String name = xmlPullParser.getName();
                if ("layoutAnimation".equals(name)) {
                    layoutAnimationController = new LayoutAnimationController(context, attributeSet);
                } else if ("gridLayoutAnimation".equals(name)) {
                    layoutAnimationController = new GridLayoutAnimationController(context, attributeSet);
                } else {
                    throw new RuntimeException("Unknown layout animation name: " + name);
                }
            }
        }
    }

    public static Animation makeInAnimation(Context context, boolean z) throws Throwable {
        Animation animationLoadAnimation;
        if (z) {
            animationLoadAnimation = loadAnimation(context, 17432578);
        } else {
            animationLoadAnimation = loadAnimation(context, R.anim.slide_in_right);
        }
        animationLoadAnimation.setInterpolator(new DecelerateInterpolator());
        animationLoadAnimation.setStartTime(currentAnimationTimeMillis());
        return animationLoadAnimation;
    }

    public static Animation makeOutAnimation(Context context, boolean z) throws Throwable {
        Animation animationLoadAnimation;
        if (z) {
            animationLoadAnimation = loadAnimation(context, 17432579);
        } else {
            animationLoadAnimation = loadAnimation(context, R.anim.slide_out_left);
        }
        animationLoadAnimation.setInterpolator(new AccelerateInterpolator());
        animationLoadAnimation.setStartTime(currentAnimationTimeMillis());
        return animationLoadAnimation;
    }

    public static Animation makeInChildBottomAnimation(Context context) throws Throwable {
        Animation animationLoadAnimation = loadAnimation(context, R.anim.slide_in_child_bottom);
        animationLoadAnimation.setInterpolator(new AccelerateInterpolator());
        animationLoadAnimation.setStartTime(currentAnimationTimeMillis());
        return animationLoadAnimation;
    }

    public static Interpolator loadInterpolator(Context context, int i) throws Throwable {
        XmlResourceParser animation;
        XmlResourceParser xmlResourceParser = null;
        try {
            try {
                animation = context.getResources().getAnimation(i);
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e) {
            e = e;
        } catch (XmlPullParserException e2) {
            e = e2;
        }
        try {
            Interpolator interpolatorCreateInterpolatorFromXml = createInterpolatorFromXml(context.getResources(), context.getTheme(), animation);
            if (animation != null) {
                animation.close();
            }
            return interpolatorCreateInterpolatorFromXml;
        } catch (IOException e3) {
            e = e3;
            Resources.NotFoundException notFoundException = new Resources.NotFoundException("Can't load animation resource ID #0x" + Integer.toHexString(i));
            notFoundException.initCause(e);
            throw notFoundException;
        } catch (XmlPullParserException e4) {
            e = e4;
            Resources.NotFoundException notFoundException2 = new Resources.NotFoundException("Can't load animation resource ID #0x" + Integer.toHexString(i));
            notFoundException2.initCause(e);
            throw notFoundException2;
        } catch (Throwable th2) {
            th = th2;
            xmlResourceParser = animation;
            if (xmlResourceParser != null) {
                xmlResourceParser.close();
            }
            throw th;
        }
    }

    public static Interpolator loadInterpolator(Resources resources, Resources.Theme theme, int i) throws Throwable {
        XmlResourceParser animation;
        XmlResourceParser xmlResourceParser = null;
        try {
            try {
                animation = resources.getAnimation(i);
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e) {
            e = e;
        } catch (XmlPullParserException e2) {
            e = e2;
        }
        try {
            Interpolator interpolatorCreateInterpolatorFromXml = createInterpolatorFromXml(resources, theme, animation);
            if (animation != null) {
                animation.close();
            }
            return interpolatorCreateInterpolatorFromXml;
        } catch (IOException e3) {
            e = e3;
            Resources.NotFoundException notFoundException = new Resources.NotFoundException("Can't load animation resource ID #0x" + Integer.toHexString(i));
            notFoundException.initCause(e);
            throw notFoundException;
        } catch (XmlPullParserException e4) {
            e = e4;
            Resources.NotFoundException notFoundException2 = new Resources.NotFoundException("Can't load animation resource ID #0x" + Integer.toHexString(i));
            notFoundException2.initCause(e);
            throw notFoundException2;
        } catch (Throwable th2) {
            th = th2;
            xmlResourceParser = animation;
            if (xmlResourceParser != null) {
                xmlResourceParser.close();
            }
            throw th;
        }
    }

    private static Interpolator createInterpolatorFromXml(Resources resources, Resources.Theme theme, XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        Interpolator accelerateInterpolator;
        int depth = xmlPullParser.getDepth();
        Interpolator linearInterpolator = null;
        while (true) {
            int next = xmlPullParser.next();
            if ((next == 3 && xmlPullParser.getDepth() <= depth) || next == 1) {
                break;
            }
            if (next == 2) {
                AttributeSet attributeSetAsAttributeSet = Xml.asAttributeSet(xmlPullParser);
                String name = xmlPullParser.getName();
                if (name.equals("linearInterpolator")) {
                    linearInterpolator = new LinearInterpolator();
                } else {
                    if (name.equals("accelerateInterpolator")) {
                        accelerateInterpolator = new AccelerateInterpolator(resources, theme, attributeSetAsAttributeSet);
                    } else if (name.equals("decelerateInterpolator")) {
                        accelerateInterpolator = new DecelerateInterpolator(resources, theme, attributeSetAsAttributeSet);
                    } else if (name.equals("accelerateDecelerateInterpolator")) {
                        linearInterpolator = new AccelerateDecelerateInterpolator();
                    } else if (name.equals("cycleInterpolator")) {
                        accelerateInterpolator = new CycleInterpolator(resources, theme, attributeSetAsAttributeSet);
                    } else if (name.equals("anticipateInterpolator")) {
                        accelerateInterpolator = new AnticipateInterpolator(resources, theme, attributeSetAsAttributeSet);
                    } else if (name.equals("overshootInterpolator")) {
                        accelerateInterpolator = new OvershootInterpolator(resources, theme, attributeSetAsAttributeSet);
                    } else if (name.equals("anticipateOvershootInterpolator")) {
                        accelerateInterpolator = new AnticipateOvershootInterpolator(resources, theme, attributeSetAsAttributeSet);
                    } else if (name.equals("bounceInterpolator")) {
                        linearInterpolator = new BounceInterpolator();
                    } else if (name.equals("pathInterpolator")) {
                        accelerateInterpolator = new PathInterpolator(resources, theme, attributeSetAsAttributeSet);
                    } else {
                        throw new RuntimeException("Unknown interpolator name: " + xmlPullParser.getName());
                    }
                    linearInterpolator = accelerateInterpolator;
                }
            }
        }
    }
}
