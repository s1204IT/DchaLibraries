package org.xmlpull.v1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class XmlPullParserFactory {
    public static final String PROPERTY_NAME = "org.xmlpull.v1.XmlPullParserFactory";
    protected String classNamesLocation = null;
    protected HashMap<String, Boolean> features = new HashMap<>();
    protected ArrayList parserClasses = new ArrayList();
    protected ArrayList serializerClasses = new ArrayList();

    protected XmlPullParserFactory() {
        try {
            this.parserClasses.add(Class.forName("org.kxml2.io.KXmlParser"));
            this.serializerClasses.add(Class.forName("org.kxml2.io.KXmlSerializer"));
        } catch (ClassNotFoundException e) {
            throw new AssertionError();
        }
    }

    public void setFeature(String str, boolean z) throws XmlPullParserException {
        this.features.put(str, Boolean.valueOf(z));
    }

    public boolean getFeature(String str) {
        Boolean bool = this.features.get(str);
        if (bool != null) {
            return bool.booleanValue();
        }
        return false;
    }

    public void setNamespaceAware(boolean z) {
        this.features.put(XmlPullParser.FEATURE_PROCESS_NAMESPACES, Boolean.valueOf(z));
    }

    public boolean isNamespaceAware() {
        return getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES);
    }

    public void setValidating(boolean z) {
        this.features.put(XmlPullParser.FEATURE_VALIDATION, Boolean.valueOf(z));
    }

    public boolean isValidating() {
        return getFeature(XmlPullParser.FEATURE_VALIDATION);
    }

    public XmlPullParser newPullParser() throws XmlPullParserException {
        XmlPullParser parserInstance = getParserInstance();
        for (Map.Entry<String, Boolean> entry : this.features.entrySet()) {
            if (entry.getValue().booleanValue()) {
                parserInstance.setFeature(entry.getKey(), entry.getValue().booleanValue());
            }
        }
        return parserInstance;
    }

    private XmlPullParser getParserInstance() throws XmlPullParserException {
        ArrayList arrayList;
        if (this.parserClasses != null && !this.parserClasses.isEmpty()) {
            arrayList = new ArrayList();
            for (Object obj : this.parserClasses) {
                if (obj != null) {
                    try {
                        return (XmlPullParser) ((Class) obj).newInstance();
                    } catch (ClassCastException e) {
                        arrayList.add(e);
                    } catch (IllegalAccessException e2) {
                        arrayList.add(e2);
                    } catch (InstantiationException e3) {
                        arrayList.add(e3);
                    }
                }
            }
        } else {
            arrayList = null;
        }
        throw newInstantiationException("Invalid parser class list", arrayList);
    }

    private XmlSerializer getSerializerInstance() throws XmlPullParserException {
        ArrayList arrayList;
        if (this.serializerClasses != null && !this.serializerClasses.isEmpty()) {
            arrayList = new ArrayList();
            for (Object obj : this.serializerClasses) {
                if (obj != null) {
                    try {
                        return (XmlSerializer) ((Class) obj).newInstance();
                    } catch (ClassCastException e) {
                        arrayList.add(e);
                    } catch (IllegalAccessException e2) {
                        arrayList.add(e2);
                    } catch (InstantiationException e3) {
                        arrayList.add(e3);
                    }
                }
            }
        } else {
            arrayList = null;
        }
        throw newInstantiationException("Invalid serializer class list", arrayList);
    }

    private static XmlPullParserException newInstantiationException(String str, ArrayList<Exception> arrayList) {
        if (arrayList == null || arrayList.isEmpty()) {
            return new XmlPullParserException(str);
        }
        XmlPullParserException xmlPullParserException = new XmlPullParserException(str);
        Iterator<Exception> it = arrayList.iterator();
        while (it.hasNext()) {
            xmlPullParserException.addSuppressed(it.next());
        }
        return xmlPullParserException;
    }

    public XmlSerializer newSerializer() throws XmlPullParserException {
        return getSerializerInstance();
    }

    public static XmlPullParserFactory newInstance() throws XmlPullParserException {
        return new XmlPullParserFactory();
    }

    public static XmlPullParserFactory newInstance(String str, Class cls) throws XmlPullParserException {
        return newInstance();
    }
}
