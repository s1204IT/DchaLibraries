package android.support.v7.preference;

import android.content.Context;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.support.v14.preference.SwitchPreference;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.InflateException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class PreferenceInflater {
    private static final String EXTRA_TAG_NAME = "extra";
    private static final String INTENT_TAG_NAME = "intent";
    private static final String TAG = "PreferenceInflater";
    private final Object[] mConstructorArgs = new Object[2];
    private final Context mContext;
    private String[] mDefaultPackages;
    private PreferenceManager mPreferenceManager;
    private static final Class<?>[] CONSTRUCTOR_SIGNATURE = {Context.class, AttributeSet.class};
    private static final HashMap<String, Constructor> CONSTRUCTOR_MAP = new HashMap<>();

    public PreferenceInflater(Context context, PreferenceManager preferenceManager) {
        this.mContext = context;
        init(preferenceManager);
    }

    private void init(PreferenceManager preferenceManager) {
        this.mPreferenceManager = preferenceManager;
        setDefaultPackages(new String[]{Preference.class.getPackage().getName() + ".", SwitchPreference.class.getPackage().getName() + "."});
    }

    public void setDefaultPackages(String[] defaultPackage) {
        this.mDefaultPackages = defaultPackage;
    }

    public String[] getDefaultPackages() {
        return this.mDefaultPackages;
    }

    public Context getContext() {
        return this.mContext;
    }

    public Preference inflate(int resource, PreferenceGroup root) {
        XmlResourceParser parser = getContext().getResources().getXml(resource);
        try {
            return inflate(parser, root);
        } finally {
            parser.close();
        }
    }

    public Preference inflate(XmlPullParser parser, PreferenceGroup root) {
        int type;
        Preference result;
        synchronized (this.mConstructorArgs) {
            AttributeSet attrs = Xml.asAttributeSet(parser);
            this.mConstructorArgs[0] = this.mContext;
            do {
                try {
                    try {
                        try {
                            type = parser.next();
                            if (type == 2) {
                                break;
                            }
                        } catch (InflateException e) {
                            throw e;
                        }
                    } catch (XmlPullParserException e2) {
                        InflateException ex = new InflateException(e2.getMessage());
                        ex.initCause(e2);
                        throw ex;
                    }
                } catch (IOException e3) {
                    InflateException ex2 = new InflateException(parser.getPositionDescription() + ": " + e3.getMessage());
                    ex2.initCause(e3);
                    throw ex2;
                }
            } while (type != 1);
            if (type != 2) {
                throw new InflateException(parser.getPositionDescription() + ": No start tag found!");
            }
            Preference xmlRoot = createItemFromTag(parser.getName(), attrs);
            result = onMergeRoots(root, (PreferenceGroup) xmlRoot);
            rInflate(parser, result, attrs);
        }
        return result;
    }

    private PreferenceGroup onMergeRoots(PreferenceGroup givenRoot, PreferenceGroup xmlRoot) {
        if (givenRoot == null) {
            xmlRoot.onAttachedToHierarchy(this.mPreferenceManager);
            return xmlRoot;
        }
        return givenRoot;
    }

    private Preference createItem(String name, String[] prefixes, AttributeSet attrs) throws InflateException, ClassNotFoundException {
        Constructor<?> constructor = CONSTRUCTOR_MAP.get(name);
        if (constructor == null) {
            try {
                try {
                    ClassLoader classLoader = this.mContext.getClassLoader();
                    Class<?> clazz = null;
                    if (prefixes == null || prefixes.length == 0) {
                        clazz = classLoader.loadClass(name);
                    } else {
                        ClassNotFoundException notFoundException = null;
                        int length = prefixes.length;
                        int i = 0;
                        while (true) {
                            if (i >= length) {
                                break;
                            }
                            String prefix = prefixes[i];
                            try {
                                clazz = classLoader.loadClass(prefix + name);
                                break;
                            } catch (ClassNotFoundException e) {
                                notFoundException = e;
                                i++;
                            }
                        }
                        if (clazz == null) {
                            if (notFoundException == null) {
                                throw new InflateException(attrs.getPositionDescription() + ": Error inflating class " + name);
                            }
                            throw notFoundException;
                        }
                    }
                    constructor = clazz.getConstructor(CONSTRUCTOR_SIGNATURE);
                    constructor.setAccessible(true);
                    CONSTRUCTOR_MAP.put(name, constructor);
                } catch (ClassNotFoundException e2) {
                    throw e2;
                }
            } catch (Exception e3) {
                InflateException ie = new InflateException(attrs.getPositionDescription() + ": Error inflating class " + name);
                ie.initCause(e3);
                throw ie;
            }
        }
        Object[] args = this.mConstructorArgs;
        args[1] = attrs;
        return (Preference) constructor.newInstance(args);
    }

    protected Preference onCreateItem(String name, AttributeSet attrs) throws ClassNotFoundException {
        return createItem(name, this.mDefaultPackages, attrs);
    }

    private Preference createItemFromTag(String name, AttributeSet attrs) {
        try {
            if (-1 == name.indexOf(46)) {
                Preference item = onCreateItem(name, attrs);
                return item;
            }
            Preference item2 = createItem(name, null, attrs);
            return item2;
        } catch (InflateException e) {
            throw e;
        } catch (ClassNotFoundException e2) {
            InflateException ie = new InflateException(attrs.getPositionDescription() + ": Error inflating class (not found)" + name);
            ie.initCause(e2);
            throw ie;
        } catch (Exception e3) {
            InflateException ie2 = new InflateException(attrs.getPositionDescription() + ": Error inflating class " + name);
            ie2.initCause(e3);
            throw ie2;
        }
    }

    private void rInflate(XmlPullParser parser, Preference parent, AttributeSet attrs) throws XmlPullParserException, IOException {
        int depth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if ((type != 3 || parser.getDepth() > depth) && type != 1) {
                if (type == 2) {
                    String name = parser.getName();
                    if (INTENT_TAG_NAME.equals(name)) {
                        try {
                            Intent intent = Intent.parseIntent(getContext().getResources(), parser, attrs);
                            parent.setIntent(intent);
                        } catch (IOException e) {
                            XmlPullParserException ex = new XmlPullParserException("Error parsing preference");
                            ex.initCause(e);
                            throw ex;
                        }
                    } else if (EXTRA_TAG_NAME.equals(name)) {
                        getContext().getResources().parseBundleExtra(EXTRA_TAG_NAME, attrs, parent.getExtras());
                        try {
                            skipCurrentTag(parser);
                        } catch (IOException e2) {
                            XmlPullParserException ex2 = new XmlPullParserException("Error parsing preference");
                            ex2.initCause(e2);
                            throw ex2;
                        }
                    } else {
                        Preference item = createItemFromTag(name, attrs);
                        ((PreferenceGroup) parent).addItemFromInflater(item);
                        rInflate(parser, item, attrs);
                    }
                }
            } else {
                return;
            }
        }
    }

    private static void skipCurrentTag(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type == 3 && parser.getDepth() <= outerDepth) {
                    return;
                }
            } else {
                return;
            }
        }
    }
}
