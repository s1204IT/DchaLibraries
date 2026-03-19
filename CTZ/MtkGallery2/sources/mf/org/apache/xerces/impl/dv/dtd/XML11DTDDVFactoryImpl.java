package mf.org.apache.xerces.impl.dv.dtd;

import java.util.Hashtable;
import java.util.Map;
import mf.org.apache.xerces.impl.dv.DatatypeValidator;

public class XML11DTDDVFactoryImpl extends DTDDVFactoryImpl {
    static final Hashtable fXML11BuiltInTypes = new Hashtable();

    static {
        fXML11BuiltInTypes.put("XML11ID", new XML11IDDatatypeValidator());
        XML11IDREFDatatypeValidator xML11IDREFDatatypeValidator = new XML11IDREFDatatypeValidator();
        fXML11BuiltInTypes.put("XML11IDREF", xML11IDREFDatatypeValidator);
        fXML11BuiltInTypes.put("XML11IDREFS", new ListDatatypeValidator(xML11IDREFDatatypeValidator));
        XML11NMTOKENDatatypeValidator xML11NMTOKENDatatypeValidator = new XML11NMTOKENDatatypeValidator();
        fXML11BuiltInTypes.put("XML11NMTOKEN", xML11NMTOKENDatatypeValidator);
        fXML11BuiltInTypes.put("XML11NMTOKENS", new ListDatatypeValidator(xML11NMTOKENDatatypeValidator));
    }

    @Override
    public DatatypeValidator getBuiltInDV(String name) {
        if (fXML11BuiltInTypes.get(name) != null) {
            return (DatatypeValidator) fXML11BuiltInTypes.get(name);
        }
        return (DatatypeValidator) fBuiltInTypes.get(name);
    }

    @Override
    public Hashtable getBuiltInTypes() {
        Hashtable toReturn = (Hashtable) fBuiltInTypes.clone();
        for (Map.Entry entry : fXML11BuiltInTypes.entrySet()) {
            Object key = entry.getKey();
            Object dv = entry.getValue();
            toReturn.put(key, dv);
        }
        return toReturn;
    }
}
