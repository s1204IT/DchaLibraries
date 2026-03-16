package android.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Canvas;
import android.media.TtmlUtils;
import android.os.Handler;
import android.os.Message;
import android.os.Trace;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.internal.R;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public abstract class LayoutInflater {
    private static final boolean DEBUG = false;
    private static final String TAG_1995 = "blink";
    private static final String TAG_INCLUDE = "include";
    private static final String TAG_MERGE = "merge";
    private static final String TAG_REQUEST_FOCUS = "requestFocus";
    private static final String TAG_TAG = "tag";
    final Object[] mConstructorArgs;
    protected final Context mContext;
    private Factory mFactory;
    private Factory2 mFactory2;
    private boolean mFactorySet;
    private Filter mFilter;
    private HashMap<String, Boolean> mFilterMap;
    private Factory2 mPrivateFactory;
    private static final String TAG = LayoutInflater.class.getSimpleName();
    static final Class<?>[] mConstructorSignature = {Context.class, AttributeSet.class};
    private static final HashMap<CachedClassKey, Constructor<? extends View>> sConstructorMap = new HashMap<>();
    private static final int[] ATTRS_THEME = {16842752};

    public interface Factory {
        View onCreateView(String str, Context context, AttributeSet attributeSet);
    }

    public interface Factory2 extends Factory {
        View onCreateView(View view, String str, Context context, AttributeSet attributeSet);
    }

    public interface Filter {
        boolean onLoadClass(Class cls);
    }

    public abstract LayoutInflater cloneInContext(Context context);

    private static class CachedClassKey {
        private ClassLoader mLoader;
        private String mName;

        public CachedClassKey(String name, ClassLoader loader) {
            this.mName = name;
            this.mLoader = loader;
        }

        public boolean equals(Object o) {
            if (o == null || !(o instanceof CachedClassKey)) {
                return false;
            }
            CachedClassKey classKey = (CachedClassKey) o;
            return TextUtils.equals(this.mName, classKey.mName) && this.mLoader != null && this.mLoader.equals(classKey.mLoader);
        }

        public int hashCode() {
            return (this.mName + this.mLoader.toString()).hashCode();
        }
    }

    private static class FactoryMerger implements Factory2 {
        private final Factory mF1;
        private final Factory2 mF12;
        private final Factory mF2;
        private final Factory2 mF22;

        FactoryMerger(Factory f1, Factory2 f12, Factory f2, Factory2 f22) {
            this.mF1 = f1;
            this.mF2 = f2;
            this.mF12 = f12;
            this.mF22 = f22;
        }

        @Override
        public View onCreateView(String name, Context context, AttributeSet attrs) {
            View v = this.mF1.onCreateView(name, context, attrs);
            return v != null ? v : this.mF2.onCreateView(name, context, attrs);
        }

        @Override
        public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
            View v = this.mF12 != null ? this.mF12.onCreateView(parent, name, context, attrs) : this.mF1.onCreateView(name, context, attrs);
            if (v != null) {
                return v;
            }
            return this.mF22 != null ? this.mF22.onCreateView(parent, name, context, attrs) : this.mF2.onCreateView(name, context, attrs);
        }
    }

    protected LayoutInflater(Context context) {
        this.mConstructorArgs = new Object[2];
        this.mContext = context;
    }

    protected LayoutInflater(LayoutInflater original, Context newContext) {
        this.mConstructorArgs = new Object[2];
        this.mContext = newContext;
        this.mFactory = original.mFactory;
        this.mFactory2 = original.mFactory2;
        this.mPrivateFactory = original.mPrivateFactory;
        setFilter(original.mFilter);
    }

    public static LayoutInflater from(Context context) {
        LayoutInflater LayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (LayoutInflater == null) {
            throw new AssertionError("LayoutInflater not found.");
        }
        return LayoutInflater;
    }

    public Context getContext() {
        return this.mContext;
    }

    public final Factory getFactory() {
        return this.mFactory;
    }

    public final Factory2 getFactory2() {
        return this.mFactory2;
    }

    public void setFactory(Factory factory) {
        if (this.mFactorySet) {
            throw new IllegalStateException("A factory has already been set on this LayoutInflater");
        }
        if (factory == null) {
            throw new NullPointerException("Given factory can not be null");
        }
        this.mFactorySet = true;
        if (this.mFactory == null) {
            this.mFactory = factory;
        } else {
            this.mFactory = new FactoryMerger(factory, null, this.mFactory, this.mFactory2);
        }
    }

    public void setFactory2(Factory2 factory) {
        if (this.mFactorySet) {
            throw new IllegalStateException("A factory has already been set on this LayoutInflater");
        }
        if (factory == null) {
            throw new NullPointerException("Given factory can not be null");
        }
        this.mFactorySet = true;
        if (this.mFactory == null) {
            this.mFactory2 = factory;
            this.mFactory = factory;
        } else {
            FactoryMerger factoryMerger = new FactoryMerger(factory, factory, this.mFactory, this.mFactory2);
            this.mFactory2 = factoryMerger;
            this.mFactory = factoryMerger;
        }
    }

    public void setPrivateFactory(Factory2 factory) {
        if (this.mPrivateFactory == null) {
            this.mPrivateFactory = factory;
        } else {
            this.mPrivateFactory = new FactoryMerger(factory, factory, this.mPrivateFactory, this.mPrivateFactory);
        }
    }

    public Filter getFilter() {
        return this.mFilter;
    }

    public void setFilter(Filter filter) {
        this.mFilter = filter;
        if (filter != null) {
            this.mFilterMap = new HashMap<>();
        }
    }

    public View inflate(int resource, ViewGroup root) {
        return inflate(resource, root, root != null);
    }

    public View inflate(XmlPullParser parser, ViewGroup root) {
        return inflate(parser, root, root != null);
    }

    public View inflate(int resource, ViewGroup root, boolean attachToRoot) {
        Resources res = getContext().getResources();
        XmlResourceParser parser = res.getLayout(resource);
        try {
            return inflate(parser, root, attachToRoot);
        } finally {
            parser.close();
        }
    }

    public View inflate(XmlPullParser parser, ViewGroup root, boolean attachToRoot) {
        View result;
        int type;
        synchronized (this.mConstructorArgs) {
            Trace.traceBegin(8L, "inflate");
            AttributeSet attrs = Xml.asAttributeSet(parser);
            Context lastContext = (Context) this.mConstructorArgs[0];
            this.mConstructorArgs[0] = this.mContext;
            result = root;
            do {
                try {
                    try {
                        type = parser.next();
                        if (type == 2) {
                            break;
                        }
                    } catch (IOException e) {
                        InflateException ex = new InflateException(parser.getPositionDescription() + ": " + e.getMessage());
                        ex.initCause(e);
                        throw ex;
                    } catch (XmlPullParserException e2) {
                        InflateException ex2 = new InflateException(e2.getMessage());
                        ex2.initCause(e2);
                        throw ex2;
                    }
                } catch (Throwable th) {
                    this.mConstructorArgs[0] = lastContext;
                    this.mConstructorArgs[1] = null;
                    throw th;
                }
            } while (type != 1);
            if (type != 2) {
                throw new InflateException(parser.getPositionDescription() + ": No start tag found!");
            }
            String name = parser.getName();
            if (TAG_MERGE.equals(name)) {
                if (root == null || !attachToRoot) {
                    throw new InflateException("<merge /> can be used only with a valid ViewGroup root and attachToRoot=true");
                }
                rInflate(parser, root, attrs, false, false);
            } else {
                View temp = createViewFromTag(root, name, attrs, false);
                ViewGroup.LayoutParams params = null;
                if (root != null) {
                    params = root.generateLayoutParams(attrs);
                    if (!attachToRoot) {
                        temp.setLayoutParams(params);
                    }
                }
                rInflate(parser, temp, attrs, true, true);
                if (root != null && attachToRoot) {
                    root.addView(temp, params);
                }
                if (root == null || !attachToRoot) {
                    result = temp;
                }
            }
            this.mConstructorArgs[0] = lastContext;
            this.mConstructorArgs[1] = null;
            Trace.traceEnd(8L);
        }
        return result;
    }

    private Constructor<? extends View> findCachedConstructor(String name, ClassLoader classLoader) {
        Constructor<? extends View> constructor = null;
        for (ClassLoader loader = classLoader; loader != null; loader = loader.getParent()) {
            Constructor<? extends View> constructor2 = sConstructorMap.get(new CachedClassKey(name, loader));
            constructor = constructor2;
            if (constructor != null) {
                break;
            }
        }
        return constructor;
    }

    public final View createView(String name, String prefix, AttributeSet attrs) throws InflateException, ClassNotFoundException {
        Constructor<? extends View> constructor = findCachedConstructor(name, this.mContext.getClassLoader());
        Class clsAsSubclass = null;
        try {
            try {
                try {
                    Trace.traceBegin(8L, name);
                    if (constructor == null) {
                        clsAsSubclass = this.mContext.getClassLoader().loadClass(prefix != null ? prefix + name : name).asSubclass(View.class);
                        if (this.mFilter != null && clsAsSubclass != null) {
                            if (!this.mFilter.onLoadClass(clsAsSubclass)) {
                                failNotAllowed(name, prefix, attrs);
                            }
                        }
                        constructor = clsAsSubclass.getConstructor(mConstructorSignature);
                        sConstructorMap.put(new CachedClassKey(name, clsAsSubclass.getClassLoader()), constructor);
                    } else if (this.mFilter != null) {
                        Boolean allowedState = this.mFilterMap.get(name);
                        if (allowedState == null) {
                            clsAsSubclass = this.mContext.getClassLoader().loadClass(prefix != null ? prefix + name : name).asSubclass(View.class);
                            boolean allowed = clsAsSubclass != null && this.mFilter.onLoadClass(clsAsSubclass);
                            this.mFilterMap.put(name, Boolean.valueOf(allowed));
                            if (!allowed) {
                                failNotAllowed(name, prefix, attrs);
                            }
                        } else if (allowedState.equals(Boolean.FALSE)) {
                            failNotAllowed(name, prefix, attrs);
                        }
                    }
                    Object[] args = this.mConstructorArgs;
                    args[1] = attrs;
                    constructor.setAccessible(true);
                    View view = constructor.newInstance(args);
                    if (view instanceof ViewStub) {
                        ViewStub viewStub = (ViewStub) view;
                        viewStub.setLayoutInflater(cloneInContext((Context) args[0]));
                    }
                    return view;
                } catch (ClassCastException e) {
                    StringBuilder sbAppend = new StringBuilder().append(attrs.getPositionDescription()).append(": Class is not a View ");
                    if (prefix != null) {
                        name = prefix + name;
                    }
                    InflateException ie = new InflateException(sbAppend.append(name).toString());
                    ie.initCause(e);
                    throw ie;
                } catch (ClassNotFoundException e2) {
                    throw e2;
                }
            } catch (NoSuchMethodException e3) {
                StringBuilder sbAppend2 = new StringBuilder().append(attrs.getPositionDescription()).append(": Error inflating class ");
                if (prefix != null) {
                    name = prefix + name;
                }
                InflateException ie2 = new InflateException(sbAppend2.append(name).toString());
                ie2.initCause(e3);
                throw ie2;
            } catch (Exception e4) {
                InflateException ie3 = new InflateException(attrs.getPositionDescription() + ": Error inflating class " + (clsAsSubclass == null ? MediaStore.UNKNOWN_STRING : clsAsSubclass.getName()));
                ie3.initCause(e4);
                throw ie3;
            }
        } finally {
            Trace.traceEnd(8L);
        }
    }

    private void failNotAllowed(String name, String prefix, AttributeSet attrs) {
        StringBuilder sbAppend = new StringBuilder().append(attrs.getPositionDescription()).append(": Class not allowed to be inflated ");
        if (prefix != null) {
            name = prefix + name;
        }
        throw new InflateException(sbAppend.append(name).toString());
    }

    protected View onCreateView(String name, AttributeSet attrs) throws ClassNotFoundException {
        return createView(name, "android.view.", attrs);
    }

    protected View onCreateView(View parent, String name, AttributeSet attrs) throws ClassNotFoundException {
        return onCreateView(name, attrs);
    }

    View createViewFromTag(View parent, String name, AttributeSet attrs, boolean inheritContext) {
        Context viewContext;
        View view;
        View view2;
        if (name.equals("view")) {
            name = attrs.getAttributeValue(null, "class");
        }
        if (parent != null && inheritContext) {
            viewContext = parent.getContext();
        } else {
            viewContext = this.mContext;
        }
        TypedArray ta = viewContext.obtainStyledAttributes(attrs, ATTRS_THEME);
        int themeResId = ta.getResourceId(0, 0);
        if (themeResId != 0) {
            viewContext = new ContextThemeWrapper(viewContext, themeResId);
        }
        ta.recycle();
        if (name.equals(TAG_1995)) {
            return new BlinkLayout(viewContext, attrs);
        }
        try {
            if (this.mFactory2 != null) {
                view = this.mFactory2.onCreateView(parent, name, viewContext, attrs);
            } else if (this.mFactory != null) {
                view = this.mFactory.onCreateView(name, viewContext, attrs);
            } else {
                view = null;
            }
            if (view == null && this.mPrivateFactory != null) {
                view = this.mPrivateFactory.onCreateView(parent, name, viewContext, attrs);
            }
            if (view == null) {
                Object lastContext = this.mConstructorArgs[0];
                this.mConstructorArgs[0] = viewContext;
                try {
                    if (-1 == name.indexOf(46)) {
                        view2 = onCreateView(parent, name, attrs);
                    } else {
                        view2 = createView(name, null, attrs);
                    }
                    return view2;
                } finally {
                    this.mConstructorArgs[0] = lastContext;
                }
            }
            return view;
        } catch (InflateException e) {
            throw e;
        } catch (ClassNotFoundException e2) {
            InflateException ie = new InflateException(attrs.getPositionDescription() + ": Error inflating class " + name);
            ie.initCause(e2);
            throw ie;
        } catch (Exception e3) {
            InflateException ie2 = new InflateException(attrs.getPositionDescription() + ": Error inflating class " + name);
            ie2.initCause(e3);
            throw ie2;
        }
    }

    void rInflate(XmlPullParser parser, View parent, AttributeSet attrs, boolean finishInflate, boolean inheritContext) throws XmlPullParserException, IOException {
        int depth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if ((type == 3 && parser.getDepth() <= depth) || type == 1) {
                break;
            }
            if (type == 2) {
                String name = parser.getName();
                if (TAG_REQUEST_FOCUS.equals(name)) {
                    parseRequestFocus(parser, parent);
                } else if ("tag".equals(name)) {
                    parseViewTag(parser, parent, attrs);
                } else if (TAG_INCLUDE.equals(name)) {
                    if (parser.getDepth() == 0) {
                        throw new InflateException("<include /> cannot be the root element");
                    }
                    parseInclude(parser, parent, attrs, inheritContext);
                } else {
                    if (TAG_MERGE.equals(name)) {
                        throw new InflateException("<merge /> must be the root element");
                    }
                    View view = createViewFromTag(parent, name, attrs, inheritContext);
                    ViewGroup viewGroup = (ViewGroup) parent;
                    ViewGroup.LayoutParams params = viewGroup.generateLayoutParams(attrs);
                    rInflate(parser, view, attrs, true, true);
                    viewGroup.addView(view, params);
                }
            }
        }
    }

    private void parseRequestFocus(XmlPullParser parser, View view) throws XmlPullParserException, IOException {
        int type;
        view.requestFocus();
        int currentDepth = parser.getDepth();
        do {
            type = parser.next();
            if (type == 3 && parser.getDepth() <= currentDepth) {
                return;
            }
        } while (type != 1);
    }

    private void parseViewTag(XmlPullParser parser, View view, AttributeSet attrs) throws XmlPullParserException, IOException {
        int type;
        TypedArray ta = this.mContext.obtainStyledAttributes(attrs, R.styleable.ViewTag);
        int key = ta.getResourceId(1, 0);
        CharSequence value = ta.getText(0);
        view.setTag(key, value);
        ta.recycle();
        int currentDepth = parser.getDepth();
        do {
            type = parser.next();
            if (type == 3 && parser.getDepth() <= currentDepth) {
                return;
            }
        } while (type != 1);
    }

    private void parseInclude(XmlPullParser parser, View parent, AttributeSet attrs, boolean inheritContext) throws XmlPullParserException, IOException {
        int type;
        int type2;
        if (parent instanceof ViewGroup) {
            int layout = attrs.getAttributeResourceValue(null, TtmlUtils.TAG_LAYOUT, 0);
            if (layout == 0) {
                String value = attrs.getAttributeValue(null, TtmlUtils.TAG_LAYOUT);
                if (value == null) {
                    throw new InflateException("You must specifiy a layout in the include tag: <include layout=\"@layout/layoutID\" />");
                }
                throw new InflateException("You must specifiy a valid layout reference. The layout ID " + value + " is not valid.");
            }
            XmlResourceParser childParser = getContext().getResources().getLayout(layout);
            try {
                AttributeSet childAttrs = Xml.asAttributeSet(childParser);
                do {
                    type = childParser.next();
                    if (type == 2) {
                        break;
                    }
                } while (type != 1);
                if (type != 2) {
                    throw new InflateException(childParser.getPositionDescription() + ": No start tag found!");
                }
                String childName = childParser.getName();
                if (TAG_MERGE.equals(childName)) {
                    rInflate(childParser, parent, childAttrs, false, inheritContext);
                } else {
                    View view = createViewFromTag(parent, childName, childAttrs, inheritContext);
                    ViewGroup group = (ViewGroup) parent;
                    ViewGroup.LayoutParams params = null;
                    try {
                        try {
                            params = group.generateLayoutParams(attrs);
                        } catch (RuntimeException e) {
                            ViewGroup.LayoutParams params2 = group.generateLayoutParams(childAttrs);
                            if (params2 != null) {
                                view.setLayoutParams(params2);
                            }
                        }
                        rInflate(childParser, view, childAttrs, true, true);
                        TypedArray a = this.mContext.obtainStyledAttributes(attrs, R.styleable.View, 0, 0);
                        int id = a.getResourceId(9, -1);
                        int visibility = a.getInt(21, -1);
                        a.recycle();
                        if (id != -1) {
                            view.setId(id);
                        }
                        switch (visibility) {
                            case 0:
                                view.setVisibility(0);
                                break;
                            case 1:
                                view.setVisibility(4);
                                break;
                            case 2:
                                view.setVisibility(8);
                                break;
                        }
                        group.addView(view);
                    } finally {
                        if (params != null) {
                            view.setLayoutParams(params);
                        }
                    }
                }
                childParser.close();
                int currentDepth = parser.getDepth();
                do {
                    type2 = parser.next();
                    if (type2 == 3 && parser.getDepth() <= currentDepth) {
                        return;
                    }
                } while (type2 != 1);
                return;
            } catch (Throwable th) {
                childParser.close();
                throw th;
            }
        }
        throw new InflateException("<include /> can only be used inside of a ViewGroup");
    }

    private static class BlinkLayout extends FrameLayout {
        private static final int BLINK_DELAY = 500;
        private static final int MESSAGE_BLINK = 66;
        private boolean mBlink;
        private boolean mBlinkState;
        private final Handler mHandler;

        public BlinkLayout(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.mHandler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    if (msg.what != 66) {
                        return false;
                    }
                    if (BlinkLayout.this.mBlink) {
                        BlinkLayout.this.mBlinkState = BlinkLayout.this.mBlinkState ? false : true;
                        BlinkLayout.this.makeBlink();
                    }
                    BlinkLayout.this.invalidate();
                    return true;
                }
            });
        }

        private void makeBlink() {
            Message message = this.mHandler.obtainMessage(66);
            this.mHandler.sendMessageDelayed(message, 500L);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            this.mBlink = true;
            this.mBlinkState = true;
            makeBlink();
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            this.mBlink = false;
            this.mBlinkState = true;
            this.mHandler.removeMessages(66);
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            if (this.mBlinkState) {
                super.dispatchDraw(canvas);
            }
        }
    }
}
