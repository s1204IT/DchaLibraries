package org.ksoap2.serialization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.SoapFault12;
import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class SoapSerializationEnvelope extends SoapEnvelope {
    private static final String ANY_TYPE_LABEL = "anyType";
    private static final String ARRAY_MAPPING_NAME = "Array";
    private static final String ARRAY_TYPE_LABEL = "arrayType";
    static final Marshal DEFAULT_MARSHAL = new DM();
    private static final String HREF_LABEL = "href";
    private static final String ID_LABEL = "id";
    private static final String ITEM_LABEL = "item";
    private static final String NIL_LABEL = "nil";
    private static final String NULL_LABEL = "null";
    protected static final int QNAME_MARSHAL = 3;
    protected static final int QNAME_NAMESPACE = 0;
    protected static final int QNAME_TYPE = 1;
    private static final String ROOT_LABEL = "root";
    private static final String TYPE_LABEL = "type";
    protected boolean addAdornments;
    public boolean avoidExceptionForUnknownProperty;
    protected Hashtable classToQName;
    public boolean dotNet;
    Hashtable idMap;
    public boolean implicitTypes;
    Vector multiRef;
    public Hashtable properties;
    protected Hashtable qNameToClass;

    public SoapSerializationEnvelope(int version) {
        super(version);
        this.properties = new Hashtable();
        this.idMap = new Hashtable();
        this.qNameToClass = new Hashtable();
        this.classToQName = new Hashtable();
        this.addAdornments = true;
        addMapping(this.enc, ARRAY_MAPPING_NAME, PropertyInfo.VECTOR_CLASS);
        DEFAULT_MARSHAL.register(this);
    }

    public boolean isAddAdornments() {
        return this.addAdornments;
    }

    public void setAddAdornments(boolean addAdornments) {
        this.addAdornments = addAdornments;
    }

    public void setBodyOutEmpty(boolean emptyBody) {
        if (emptyBody) {
            this.bodyOut = null;
        }
    }

    @Override
    public void parseBody(XmlPullParser parser) throws XmlPullParserException, IOException {
        SoapFault fault;
        this.bodyIn = null;
        parser.nextTag();
        if (parser.getEventType() == 2 && parser.getNamespace().equals(this.env) && parser.getName().equals("Fault")) {
            if (this.version < 120) {
                fault = new SoapFault(this.version);
            } else {
                fault = new SoapFault12(this.version);
            }
            fault.parse(parser);
            this.bodyIn = fault;
            return;
        }
        while (parser.getEventType() == 2) {
            String rootAttr = parser.getAttributeValue(this.enc, ROOT_LABEL);
            Object o = read(parser, null, -1, parser.getNamespace(), parser.getName(), PropertyInfo.OBJECT_TYPE);
            if ("1".equals(rootAttr) || this.bodyIn == null) {
                this.bodyIn = o;
            }
            parser.nextTag();
        }
    }

    protected void readSerializable(XmlPullParser parser, SoapObject obj) throws XmlPullParserException, IOException {
        for (int counter = 0; counter < parser.getAttributeCount(); counter++) {
            String attributeName = parser.getAttributeName(counter);
            String value = parser.getAttributeValue(counter);
            obj.addAttribute(attributeName, value);
        }
        readSerializable(parser, (KvmSerializable) obj);
    }

    protected void readSerializable(XmlPullParser parser, KvmSerializable obj) throws XmlPullParserException, IOException {
        while (parser.nextTag() != 3) {
            String name = parser.getName();
            if (!this.implicitTypes || !(obj instanceof SoapObject)) {
                PropertyInfo info = new PropertyInfo();
                int propertyCount = obj.getPropertyCount();
                boolean propertyFound = false;
                for (int i = 0; i < propertyCount && !propertyFound; i++) {
                    info.clear();
                    obj.getPropertyInfo(i, this.properties, info);
                    if ((name.equals(info.name) && info.namespace == null) || (name.equals(info.name) && parser.getNamespace().equals(info.namespace))) {
                        propertyFound = true;
                        obj.setProperty(i, read(parser, obj, i, null, null, info));
                    }
                }
                if (propertyFound) {
                    continue;
                } else {
                    if (!this.avoidExceptionForUnknownProperty) {
                        throw new RuntimeException("Unknown Property: " + name);
                    }
                    while (true) {
                        if (parser.next() != 3 || !name.equals(parser.getName())) {
                        }
                    }
                }
            } else {
                ((SoapObject) obj).addProperty(parser.getName(), read(parser, obj, obj.getPropertyCount(), ((SoapObject) obj).getNamespace(), name, PropertyInfo.OBJECT_TYPE));
            }
        }
        parser.require(3, null, null);
    }

    protected Object readUnknown(XmlPullParser parser, String typeNamespace, String typeName) throws XmlPullParserException, IOException {
        String name = parser.getName();
        String namespace = parser.getNamespace();
        Vector attributeInfoVector = new Vector();
        for (int attributeCount = 0; attributeCount < parser.getAttributeCount(); attributeCount++) {
            AttributeInfo attributeInfo = new AttributeInfo();
            attributeInfo.setName(parser.getAttributeName(attributeCount));
            attributeInfo.setValue(parser.getAttributeValue(attributeCount));
            attributeInfo.setNamespace(parser.getAttributeNamespace(attributeCount));
            attributeInfo.setType(parser.getAttributeType(attributeCount));
            attributeInfoVector.addElement(attributeInfo);
        }
        parser.next();
        Object result = null;
        String text = null;
        if (parser.getEventType() == 4) {
            text = parser.getText();
            SoapPrimitive sp = new SoapPrimitive(typeNamespace, typeName, text);
            result = sp;
            for (int i = 0; i < attributeInfoVector.size(); i++) {
                sp.addAttribute((AttributeInfo) attributeInfoVector.elementAt(i));
            }
            parser.next();
        } else if (parser.getEventType() == 3) {
            SoapObject so = new SoapObject(typeNamespace, typeName);
            for (int i2 = 0; i2 < attributeInfoVector.size(); i2++) {
                so.addAttribute((AttributeInfo) attributeInfoVector.elementAt(i2));
            }
            result = so;
        }
        if (parser.getEventType() == 2) {
            if (text != null && text.trim().length() != 0) {
                throw new RuntimeException("Malformed input: Mixed content");
            }
            SoapObject so2 = new SoapObject(typeNamespace, typeName);
            for (int i3 = 0; i3 < attributeInfoVector.size(); i3++) {
                so2.addAttribute((AttributeInfo) attributeInfoVector.elementAt(i3));
            }
            while (parser.getEventType() != 3) {
                so2.addProperty(parser.getName(), read(parser, so2, so2.getPropertyCount(), null, null, PropertyInfo.OBJECT_TYPE));
                parser.nextTag();
            }
            result = so2;
        }
        parser.require(3, namespace, name);
        return result;
    }

    private int getIndex(String value, int start, int dflt) {
        return (value != null && value.length() - start >= 3) ? Integer.parseInt(value.substring(start + 1, value.length() - 1)) : dflt;
    }

    protected void readVector(XmlPullParser parser, Vector v, PropertyInfo elementType) throws XmlPullParserException, IOException {
        String namespace = null;
        String name = null;
        int size = v.size();
        boolean dynamic = true;
        String type = parser.getAttributeValue(this.enc, ARRAY_TYPE_LABEL);
        if (type != null) {
            int cut0 = type.indexOf(58);
            int cut1 = type.indexOf("[", cut0);
            name = type.substring(cut0 + 1, cut1);
            String prefix = cut0 == -1 ? "" : type.substring(0, cut0);
            namespace = parser.getNamespace(prefix);
            size = getIndex(type, cut1, -1);
            if (size != -1) {
                v.setSize(size);
                dynamic = false;
            }
        }
        if (elementType == null) {
            elementType = PropertyInfo.OBJECT_TYPE;
        }
        parser.nextTag();
        int position = getIndex(parser.getAttributeValue(this.enc, "offset"), 0, 0);
        while (parser.getEventType() != 3) {
            int position2 = getIndex(parser.getAttributeValue(this.enc, "position"), 0, position);
            if (dynamic && position2 >= size) {
                size = position2 + 1;
                v.setSize(size);
            }
            v.setElementAt(read(parser, v, position2, namespace, name, elementType), position2);
            position = position2 + 1;
            parser.nextTag();
        }
        parser.require(3, null, null);
    }

    public Object read(XmlPullParser parser, Object owner, int index, String namespace, String name, PropertyInfo expected) throws XmlPullParserException, IOException {
        Object obj;
        String elementName = parser.getName();
        String href = parser.getAttributeValue(null, HREF_LABEL);
        if (href != null) {
            if (owner == null) {
                throw new RuntimeException("href at root level?!?");
            }
            String href2 = href.substring(1);
            obj = this.idMap.get(href2);
            if (obj == null || (obj instanceof FwdRef)) {
                FwdRef f = new FwdRef();
                f.next = (FwdRef) obj;
                f.obj = owner;
                f.index = index;
                this.idMap.put(href2, f);
                obj = null;
            }
            parser.nextTag();
            parser.require(3, null, elementName);
        } else {
            String nullAttr = parser.getAttributeValue(this.xsi, NIL_LABEL);
            String id = parser.getAttributeValue(null, ID_LABEL);
            if (nullAttr == null) {
                nullAttr = parser.getAttributeValue(this.xsi, NULL_LABEL);
            }
            if (nullAttr != null && SoapEnvelope.stringToBoolean(nullAttr)) {
                obj = null;
                parser.nextTag();
                parser.require(3, null, elementName);
            } else {
                String type = parser.getAttributeValue(this.xsi, TYPE_LABEL);
                if (type != null) {
                    int cut = type.indexOf(58);
                    name = type.substring(cut + 1);
                    String prefix = cut == -1 ? "" : type.substring(0, cut);
                    namespace = parser.getNamespace(prefix);
                } else if (name == null && namespace == null) {
                    if (parser.getAttributeValue(this.enc, ARRAY_TYPE_LABEL) != null) {
                        namespace = this.enc;
                        name = ARRAY_MAPPING_NAME;
                    } else {
                        Object[] names = getInfo(expected.type, null);
                        namespace = (String) names[0];
                        name = (String) names[1];
                    }
                }
                if (type == null) {
                    this.implicitTypes = true;
                }
                obj = readInstance(parser, namespace, name, expected);
                if (obj == null) {
                    obj = readUnknown(parser, namespace, name);
                }
            }
            if (id != null) {
                Object hlp = this.idMap.get(id);
                if (hlp instanceof FwdRef) {
                    FwdRef f2 = (FwdRef) hlp;
                    do {
                        if (f2.obj instanceof KvmSerializable) {
                            ((KvmSerializable) f2.obj).setProperty(f2.index, obj);
                        } else {
                            ((Vector) f2.obj).setElementAt(obj, f2.index);
                        }
                        f2 = f2.next;
                    } while (f2 != null);
                } else if (hlp != null) {
                    throw new RuntimeException("double ID");
                }
                this.idMap.put(id, obj);
            }
        }
        parser.require(3, null, elementName);
        return obj;
    }

    public Object readInstance(XmlPullParser parser, String namespace, String name, PropertyInfo expected) throws XmlPullParserException, IOException {
        Object obj;
        Object obj2 = this.qNameToClass.get(new SoapPrimitive(namespace, name, null));
        if (obj2 == null) {
            return null;
        }
        if (obj2 instanceof Marshal) {
            return ((Marshal) obj2).readInstance(parser, namespace, name, expected);
        }
        if (obj2 instanceof SoapObject) {
            obj = ((SoapObject) obj2).newInstance();
        } else if (obj2 == SoapObject.class) {
            obj = new SoapObject(namespace, name);
        } else {
            try {
                obj = ((Class) obj2).newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e.toString());
            }
        }
        if (obj instanceof SoapObject) {
            readSerializable(parser, (SoapObject) obj);
            return obj;
        }
        if (obj instanceof KvmSerializable) {
            readSerializable(parser, (KvmSerializable) obj);
            return obj;
        }
        if (obj instanceof Vector) {
            readVector(parser, (Vector) obj, expected.elementType);
            return obj;
        }
        throw new RuntimeException("no deserializer for " + obj.getClass());
    }

    public Object[] getInfo(Object type, Object instance) {
        Object[] tmp;
        if (type == null) {
            if ((instance instanceof SoapObject) || (instance instanceof SoapPrimitive)) {
                type = instance;
            } else {
                type = instance.getClass();
            }
        }
        if (type instanceof SoapObject) {
            SoapObject so = (SoapObject) type;
            return new Object[]{so.getNamespace(), so.getName(), null, null};
        }
        if (!(type instanceof SoapPrimitive)) {
            return (!(type instanceof Class) || type == PropertyInfo.OBJECT_CLASS || (tmp = (Object[]) this.classToQName.get(((Class) type).getName())) == null) ? new Object[]{this.xsd, ANY_TYPE_LABEL, null, null} : tmp;
        }
        SoapPrimitive sp = (SoapPrimitive) type;
        return new Object[]{sp.getNamespace(), sp.getName(), null, DEFAULT_MARSHAL};
    }

    public void addMapping(String namespace, String name, Class clazz, Marshal marshal) {
        this.qNameToClass.put(new SoapPrimitive(namespace, name, null), marshal == null ? clazz : marshal);
        this.classToQName.put(clazz.getName(), new Object[]{namespace, name, null, marshal});
    }

    public void addMapping(String namespace, String name, Class clazz) {
        addMapping(namespace, name, clazz, null);
    }

    public void addTemplate(SoapObject so) {
        this.qNameToClass.put(new SoapPrimitive(so.namespace, so.name, null), so);
    }

    public Object getResponse() throws SoapFault {
        if (this.bodyIn instanceof SoapFault) {
            throw ((SoapFault) this.bodyIn);
        }
        KvmSerializable ks = (KvmSerializable) this.bodyIn;
        if (ks.getPropertyCount() == 0) {
            return null;
        }
        if (ks.getPropertyCount() == 1) {
            return ks.getProperty(0);
        }
        Vector ret = new Vector();
        for (int i = 0; i < ks.getPropertyCount(); i++) {
            ret.add(ks.getProperty(i));
        }
        return ret;
    }

    @Override
    public void writeBody(XmlSerializer writer) throws IOException {
        if (this.bodyOut != null) {
            this.multiRef = new Vector();
            this.multiRef.addElement(this.bodyOut);
            Object[] qName = getInfo(null, this.bodyOut);
            writer.startTag(this.dotNet ? "" : (String) qName[0], (String) qName[1]);
            if (this.dotNet) {
                writer.attribute(null, "xmlns", (String) qName[0]);
            }
            if (this.addAdornments) {
                writer.attribute(null, ID_LABEL, qName[2] == null ? "o0" : (String) qName[2]);
                writer.attribute(this.enc, ROOT_LABEL, "1");
            }
            writeElement(writer, this.bodyOut, null, qName[3]);
            writer.endTag(this.dotNet ? "" : (String) qName[0], (String) qName[1]);
        }
    }

    public void writeObjectBody(XmlSerializer writer, SoapObject obj) throws IOException {
        int cnt = obj.getAttributeCount();
        for (int counter = 0; counter < cnt; counter++) {
            AttributeInfo attributeInfo = new AttributeInfo();
            obj.getAttributeInfo(counter, attributeInfo);
            writer.attribute(attributeInfo.getNamespace(), attributeInfo.getName(), attributeInfo.getValue().toString());
        }
        writeObjectBody(writer, (KvmSerializable) obj);
    }

    public void writeObjectBody(XmlSerializer writer, KvmSerializable obj) throws IOException {
        String name;
        int cnt = obj.getPropertyCount();
        PropertyInfo propertyInfo = new PropertyInfo();
        for (int i = 0; i < cnt; i++) {
            Object prop = obj.getProperty(i);
            obj.getPropertyInfo(i, this.properties, propertyInfo);
            if (!(prop instanceof SoapObject)) {
                if ((propertyInfo.flags & 1) == 0) {
                    writer.startTag(propertyInfo.namespace, propertyInfo.name);
                    writeProperty(writer, obj.getProperty(i), propertyInfo);
                    writer.endTag(propertyInfo.namespace, propertyInfo.name);
                }
            } else {
                SoapObject nestedSoap = (SoapObject) prop;
                Object[] qName = getInfo(null, nestedSoap);
                String namespace = (String) qName[0];
                String type = (String) qName[1];
                if (propertyInfo.name != null && propertyInfo.name.length() > 0) {
                    name = propertyInfo.name;
                } else {
                    name = (String) qName[1];
                }
                if (name.equals("DevInfo") || name.equals("DevDetail") || name.equals("PerProviderSubscription") || name.equals("MgmtTree")) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    XmlSerializer kXmlSerializer = new KXmlSerializer();
                    kXmlSerializer.setOutput(bos, "UTF-8");
                    kXmlSerializer.startTag(this.dotNet ? "" : namespace, name);
                    if (!this.implicitTypes) {
                        String prefix = writer.getPrefix(namespace, true);
                        writer.attribute(this.xsi, TYPE_LABEL, prefix + ":" + type);
                    }
                    writeObjectBody(kXmlSerializer, nestedSoap);
                    if (this.dotNet) {
                        namespace = "";
                    }
                    kXmlSerializer.endTag(namespace, name);
                    kXmlSerializer.flush();
                    bos.flush();
                    writer.cdsect(bos.toString());
                } else {
                    writer.startTag(this.dotNet ? "" : namespace, name);
                    if (!this.implicitTypes) {
                        String prefix2 = writer.getPrefix(namespace, true);
                        writer.attribute(this.xsi, TYPE_LABEL, prefix2 + ":" + type);
                    }
                    writeObjectBody(writer, nestedSoap);
                    if (this.dotNet) {
                        namespace = "";
                    }
                    writer.endTag(namespace, name);
                }
            }
        }
    }

    protected void writeProperty(XmlSerializer writer, Object obj, PropertyInfo type) throws IOException {
        if (obj != null) {
            Object[] qName = getInfo(null, obj);
            if (type.multiRef || qName[2] != null) {
                int i = this.multiRef.indexOf(obj);
                if (i == -1) {
                    i = this.multiRef.size();
                    this.multiRef.addElement(obj);
                }
                writer.attribute(null, HREF_LABEL, qName[2] == null ? "#o" + i : "#" + qName[2]);
                return;
            }
            if (!this.implicitTypes || obj.getClass() != type.type) {
                String prefix = writer.getPrefix((String) qName[0], true);
                writer.attribute(this.xsi, TYPE_LABEL, prefix + ":" + qName[1]);
            }
            writeElement(writer, obj, type, qName[3]);
        }
    }

    private void writeElement(XmlSerializer writer, Object element, PropertyInfo type, Object marshal) throws IOException {
        if (marshal != null) {
            ((Marshal) marshal).writeInstance(writer, element);
            return;
        }
        if (element instanceof SoapObject) {
            writeObjectBody(writer, (SoapObject) element);
        } else if (element instanceof KvmSerializable) {
            writeObjectBody(writer, (KvmSerializable) element);
        } else {
            if (element instanceof Vector) {
                writeVectorBody(writer, (Vector) element, type.elementType);
                return;
            }
            throw new RuntimeException("Cannot serialize: " + element);
        }
    }

    protected void writeVectorBody(XmlSerializer writer, Vector vector, PropertyInfo elementType) throws IOException {
        String itemsTagName = ITEM_LABEL;
        String itemsNamespace = null;
        if (elementType == null) {
            elementType = PropertyInfo.OBJECT_TYPE;
        } else if ((elementType instanceof PropertyInfo) && elementType.name != null) {
            itemsTagName = elementType.name;
            itemsNamespace = elementType.namespace;
        }
        int cnt = vector.size();
        Object[] arrType = getInfo(elementType.type, null);
        if (!this.implicitTypes) {
            writer.attribute(this.enc, ARRAY_TYPE_LABEL, writer.getPrefix((String) arrType[0], false) + ":" + arrType[1] + "[" + cnt + "]");
        }
        boolean skipped = false;
        for (int i = 0; i < cnt; i++) {
            if (vector.elementAt(i) == null) {
                skipped = true;
            } else {
                writer.startTag(itemsNamespace, itemsTagName);
                if (skipped) {
                    writer.attribute(this.enc, "position", "[" + i + "]");
                    skipped = false;
                }
                writeProperty(writer, vector.elementAt(i), elementType);
                writer.endTag(itemsNamespace, itemsTagName);
            }
        }
    }
}
