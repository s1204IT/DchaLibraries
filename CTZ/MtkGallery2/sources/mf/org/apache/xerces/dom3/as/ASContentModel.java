package mf.org.apache.xerces.dom3.as;

public interface ASContentModel extends ASObject {
    public static final short AS_ALL = 2;
    public static final short AS_CHOICE = 1;
    public static final short AS_NONE = 3;
    public static final short AS_SEQUENCE = 0;
    public static final int AS_UNBOUNDED = Integer.MAX_VALUE;

    int appendsubModel(ASObject aSObject) throws DOMASException;

    short getListOperator();

    int getMaxOccurs();

    int getMinOccurs();

    ASObjectList getSubModels();

    void insertsubModel(ASObject aSObject) throws DOMASException;

    void removesubModel(ASObject aSObject);

    void setListOperator(short s);

    void setMaxOccurs(int i);

    void setMinOccurs(int i);

    void setSubModels(ASObjectList aSObjectList);
}
