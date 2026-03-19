package android.preference;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.preference.GenericInflater.Parent;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.InflateException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

abstract class GenericInflater<T, P extends Parent> {
    private static final Class[] mConstructorSignature = {Context.class, AttributeSet.class};
    private static final HashMap sConstructorMap = new HashMap();
    private final boolean DEBUG;
    private final Object[] mConstructorArgs;
    protected final Context mContext;
    private String mDefaultPackage;
    private Factory<T> mFactory;
    private boolean mFactorySet;

    public interface Factory<T> {
        T onCreateItem(String str, Context context, AttributeSet attributeSet);
    }

    public interface Parent<T> {
        void addItemFromInflater(T t);
    }

    public abstract GenericInflater cloneInContext(Context context);

    private static class FactoryMerger<T> implements Factory<T> {
        private final Factory<T> mF1;
        private final Factory<T> mF2;

        FactoryMerger(Factory<T> factory, Factory<T> factory2) {
            this.mF1 = factory;
            this.mF2 = factory2;
        }

        @Override
        public T onCreateItem(String str, Context context, AttributeSet attributeSet) {
            T tOnCreateItem = this.mF1.onCreateItem(str, context, attributeSet);
            return tOnCreateItem != null ? tOnCreateItem : this.mF2.onCreateItem(str, context, attributeSet);
        }
    }

    protected GenericInflater(Context context) {
        this.DEBUG = false;
        this.mConstructorArgs = new Object[2];
        this.mContext = context;
    }

    protected GenericInflater(GenericInflater<T, P> genericInflater, Context context) {
        this.DEBUG = false;
        this.mConstructorArgs = new Object[2];
        this.mContext = context;
        this.mFactory = genericInflater.mFactory;
    }

    public void setDefaultPackage(String str) {
        this.mDefaultPackage = str;
    }

    public String getDefaultPackage() {
        return this.mDefaultPackage;
    }

    public Context getContext() {
        return this.mContext;
    }

    public final Factory<T> getFactory() {
        return this.mFactory;
    }

    public void setFactory(Factory<T> factory) {
        if (this.mFactorySet) {
            throw new IllegalStateException("A factory has already been set on this inflater");
        }
        if (factory == null) {
            throw new NullPointerException("Given factory can not be null");
        }
        this.mFactorySet = true;
        if (this.mFactory == null) {
            this.mFactory = factory;
        } else {
            this.mFactory = new FactoryMerger(factory, this.mFactory);
        }
    }

    public T inflate(int i, P p) {
        return inflate(i, p, p != null);
    }

    public T inflate(XmlPullParser xmlPullParser, P p) {
        return inflate(xmlPullParser, p, p != null);
    }

    public T inflate(int i, P p, boolean z) {
        XmlResourceParser xml = getContext().getResources().getXml(i);
        try {
            return inflate(xml, p, z);
        } finally {
            xml.close();
        }
    }

    public T inflate(XmlPullParser xmlPullParser, P p, boolean z) {
        int next;
        T t;
        synchronized (this.mConstructorArgs) {
            AttributeSet attributeSetAsAttributeSet = Xml.asAttributeSet(xmlPullParser);
            this.mConstructorArgs[0] = this.mContext;
            do {
                try {
                    try {
                        try {
                            next = xmlPullParser.next();
                            if (next == 2) {
                                break;
                            }
                        } catch (InflateException e) {
                            throw e;
                        }
                    } catch (XmlPullParserException e2) {
                        InflateException inflateException = new InflateException(e2.getMessage());
                        inflateException.initCause(e2);
                        throw inflateException;
                    }
                } catch (IOException e3) {
                    InflateException inflateException2 = new InflateException(xmlPullParser.getPositionDescription() + ": " + e3.getMessage());
                    inflateException2.initCause(e3);
                    throw inflateException2;
                }
            } while (next != 1);
            if (next != 2) {
                throw new InflateException(xmlPullParser.getPositionDescription() + ": No start tag found!");
            }
            t = (T) onMergeRoots(p, z, (Parent) createItemFromTag(xmlPullParser, xmlPullParser.getName(), attributeSetAsAttributeSet));
            rInflate(xmlPullParser, t, attributeSetAsAttributeSet);
        }
        return t;
    }

    public final T createItem(String str, String str2, AttributeSet attributeSet) throws InflateException, ClassNotFoundException {
        String str3;
        Constructor<?> constructor;
        Constructor<?> constructor2 = (Constructor) sConstructorMap.get(str);
        if (constructor2 == null) {
            try {
                try {
                    ClassLoader classLoader = this.mContext.getClassLoader();
                    if (str2 != null) {
                        str3 = str2 + str;
                    } else {
                        str3 = str;
                    }
                    constructor = classLoader.loadClass(str3).getConstructor(mConstructorSignature);
                } catch (Exception e) {
                    e = e;
                }
                try {
                    constructor.setAccessible(true);
                    sConstructorMap.put(str, constructor);
                    constructor2 = constructor;
                } catch (Exception e2) {
                    e = e2;
                    constructor2 = constructor;
                    InflateException inflateException = new InflateException(attributeSet.getPositionDescription() + ": Error inflating class " + constructor2.getClass().getName());
                    inflateException.initCause(e);
                    throw inflateException;
                }
            } catch (ClassNotFoundException e3) {
                throw e3;
            } catch (NoSuchMethodException e4) {
                StringBuilder sb = new StringBuilder();
                sb.append(attributeSet.getPositionDescription());
                sb.append(": Error inflating class ");
                if (str2 != null) {
                    str = str2 + str;
                }
                sb.append(str);
                InflateException inflateException2 = new InflateException(sb.toString());
                inflateException2.initCause(e4);
                throw inflateException2;
            }
        }
        Object[] objArr = this.mConstructorArgs;
        objArr[1] = attributeSet;
        return (T) constructor2.newInstance(objArr);
    }

    protected T onCreateItem(String str, AttributeSet attributeSet) throws ClassNotFoundException {
        return createItem(str, this.mDefaultPackage, attributeSet);
    }

    private final T createItemFromTag(XmlPullParser xmlPullParser, String str, AttributeSet attributeSet) {
        T tOnCreateItem;
        try {
            if (this.mFactory != null) {
                tOnCreateItem = this.mFactory.onCreateItem(str, this.mContext, attributeSet);
            } else {
                tOnCreateItem = null;
            }
            if (tOnCreateItem == null) {
                if (-1 == str.indexOf(46)) {
                    return onCreateItem(str, attributeSet);
                }
                return createItem(str, null, attributeSet);
            }
            return tOnCreateItem;
        } catch (InflateException e) {
            throw e;
        } catch (ClassNotFoundException e2) {
            InflateException inflateException = new InflateException(attributeSet.getPositionDescription() + ": Error inflating class " + str);
            inflateException.initCause(e2);
            throw inflateException;
        } catch (Exception e3) {
            InflateException inflateException2 = new InflateException(attributeSet.getPositionDescription() + ": Error inflating class " + str);
            inflateException2.initCause(e3);
            throw inflateException2;
        }
    }

    private void rInflate(XmlPullParser xmlPullParser, T t, AttributeSet attributeSet) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if ((next != 3 || xmlPullParser.getDepth() > depth) && next != 1) {
                if (next == 2 && !onCreateCustomFromTag(xmlPullParser, t, attributeSet)) {
                    T tCreateItemFromTag = createItemFromTag(xmlPullParser, xmlPullParser.getName(), attributeSet);
                    ((Parent) t).addItemFromInflater(tCreateItemFromTag);
                    rInflate(xmlPullParser, tCreateItemFromTag, attributeSet);
                }
            } else {
                return;
            }
        }
    }

    protected boolean onCreateCustomFromTag(XmlPullParser xmlPullParser, T t, AttributeSet attributeSet) throws XmlPullParserException {
        return false;
    }

    protected P onMergeRoots(P p, boolean z, P p2) {
        return p2;
    }
}
