package com.android.setupwizardlib.items;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.InflateException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public abstract class GenericInflater<T> {
    private static final Class[] mConstructorSignature = {Context.class, AttributeSet.class};
    private static final HashMap<String, Constructor<?>> sConstructorMap = new HashMap<>();
    private final Object[] mConstructorArgs = new Object[2];
    protected final Context mContext;
    private String mDefaultPackage;
    private Factory<T> mFactory;

    public interface Factory<T> {
        T onCreateItem(String str, Context context, AttributeSet attributeSet);
    }

    protected abstract void onAddChildItem(T t, T t2);

    protected GenericInflater(Context context) {
        this.mContext = context;
    }

    public void setDefaultPackage(String defaultPackage) {
        this.mDefaultPackage = defaultPackage;
    }

    public Context getContext() {
        return this.mContext;
    }

    public T inflate(int resource) {
        return inflate(resource, null);
    }

    public T inflate(int resource, T root) {
        return inflate(resource, root, root != null);
    }

    public T inflate(int resource, T root, boolean attachToRoot) {
        XmlResourceParser parser = getContext().getResources().getXml(resource);
        try {
            return inflate(parser, root, attachToRoot);
        } finally {
            parser.close();
        }
    }

    public T inflate(XmlPullParser parser, T root, boolean attachToRoot) {
        int type;
        T result;
        synchronized (this.mConstructorArgs) {
            AttributeSet attrs = Xml.asAttributeSet(parser);
            this.mConstructorArgs[0] = this.mContext;
            do {
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
            } while (type != 1);
            if (type != 2) {
                throw new InflateException(parser.getPositionDescription() + ": No start tag found!");
            }
            T xmlRoot = createItemFromTag(parser, parser.getName(), attrs);
            result = onMergeRoots(root, attachToRoot, xmlRoot);
            rInflate(parser, result, attrs);
        }
        return result;
    }

    public final T createItem(String str, String str2, AttributeSet attributeSet) throws InflateException, ClassNotFoundException {
        Constructor<?> constructor = sConstructorMap.get(str);
        if (constructor == null) {
            try {
                constructor = this.mContext.getClassLoader().loadClass(str2 != null ? str2 + str : str).getConstructor(mConstructorSignature);
                constructor.setAccessible(true);
                sConstructorMap.put(str, constructor);
            } catch (ClassNotFoundException e) {
                throw e;
            } catch (NoSuchMethodException e2) {
                StringBuilder sbAppend = new StringBuilder().append(attributeSet.getPositionDescription()).append(": Error inflating class ");
                if (str2 != null) {
                    str = str2 + str;
                }
                InflateException inflateException = new InflateException(sbAppend.append(str).toString());
                inflateException.initCause(e2);
                throw inflateException;
            } catch (Exception e3) {
                StringBuilder sbAppend2 = new StringBuilder().append(attributeSet.getPositionDescription()).append(": Error inflating class ");
                if (str2 != null) {
                    str = str2 + str;
                }
                InflateException inflateException2 = new InflateException(sbAppend2.append(str).toString());
                inflateException2.initCause(e3);
                throw inflateException2;
            }
        }
        Object[] objArr = this.mConstructorArgs;
        objArr[1] = attributeSet;
        return (T) constructor.newInstance(objArr);
    }

    protected T onCreateItem(String name, AttributeSet attrs) throws ClassNotFoundException {
        return createItem(name, this.mDefaultPackage, attrs);
    }

    private T createItemFromTag(XmlPullParser parser, String name, AttributeSet attrs) {
        try {
            T tOnCreateItem = this.mFactory == null ? null : this.mFactory.onCreateItem(name, this.mContext, attrs);
            if (tOnCreateItem == null) {
                if (-1 == name.indexOf(46)) {
                    return onCreateItem(name, attrs);
                }
                return createItem(name, null, attrs);
            }
            return tOnCreateItem;
        } catch (InflateException e) {
            throw e;
        } catch (Exception e2) {
            InflateException ie = new InflateException(attrs.getPositionDescription() + ": Error inflating class " + name);
            ie.initCause(e2);
            throw ie;
        }
    }

    private void rInflate(XmlPullParser parser, T node, AttributeSet attrs) throws XmlPullParserException, IOException {
        int depth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if ((type == 3 && parser.getDepth() <= depth) || type == 1) {
                return;
            }
            if (type == 2 && !onCreateCustomFromTag(parser, node, attrs)) {
                String name = parser.getName();
                T item = createItemFromTag(parser, name, attrs);
                onAddChildItem(node, item);
                rInflate(parser, item, attrs);
            }
        }
    }

    protected boolean onCreateCustomFromTag(XmlPullParser parser, T node, AttributeSet attrs) throws XmlPullParserException {
        return false;
    }

    protected T onMergeRoots(T givenRoot, boolean attachToGivenRoot, T xmlRoot) {
        return xmlRoot;
    }
}
