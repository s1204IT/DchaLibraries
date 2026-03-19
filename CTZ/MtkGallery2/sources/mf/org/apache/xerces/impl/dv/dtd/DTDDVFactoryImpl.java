package mf.org.apache.xerces.impl.dv.dtd;

import java.util.Hashtable;
import mf.org.apache.xerces.impl.dv.DTDDVFactory;
import mf.org.apache.xerces.impl.dv.DatatypeValidator;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class DTDDVFactoryImpl extends DTDDVFactory {
    static final Hashtable fBuiltInTypes = new Hashtable();

    static {
        createBuiltInTypes();
    }

    @Override
    public DatatypeValidator getBuiltInDV(String name) {
        return (DatatypeValidator) fBuiltInTypes.get(name);
    }

    @Override
    public Hashtable getBuiltInTypes() {
        return (Hashtable) fBuiltInTypes.clone();
    }

    static void createBuiltInTypes() {
        fBuiltInTypes.put(SchemaSymbols.ATTVAL_STRING, new StringDatatypeValidator());
        fBuiltInTypes.put(SchemaSymbols.ATTVAL_ID, new IDDatatypeValidator());
        IDREFDatatypeValidator iDREFDatatypeValidator = new IDREFDatatypeValidator();
        fBuiltInTypes.put(SchemaSymbols.ATTVAL_IDREF, iDREFDatatypeValidator);
        fBuiltInTypes.put(SchemaSymbols.ATTVAL_IDREFS, new ListDatatypeValidator(iDREFDatatypeValidator));
        DatatypeValidator dvTemp = new ENTITYDatatypeValidator();
        fBuiltInTypes.put(SchemaSymbols.ATTVAL_ENTITY, new ENTITYDatatypeValidator());
        fBuiltInTypes.put(SchemaSymbols.ATTVAL_ENTITIES, new ListDatatypeValidator(dvTemp));
        fBuiltInTypes.put(SchemaSymbols.ATTVAL_NOTATION, new NOTATIONDatatypeValidator());
        NMTOKENDatatypeValidator nMTOKENDatatypeValidator = new NMTOKENDatatypeValidator();
        fBuiltInTypes.put(SchemaSymbols.ATTVAL_NMTOKEN, nMTOKENDatatypeValidator);
        fBuiltInTypes.put(SchemaSymbols.ATTVAL_NMTOKENS, new ListDatatypeValidator(nMTOKENDatatypeValidator));
    }
}
