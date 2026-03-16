package gov.nist.javax.sip.message;

import gov.nist.core.GenericObject;
import gov.nist.core.GenericObjectList;
import gov.nist.core.Separators;
import java.lang.reflect.Field;

public abstract class MessageObject extends GenericObject {
    @Override
    public abstract String encode();

    @Override
    public void dbgPrint() {
        super.dbgPrint();
    }

    @Override
    public String debugDump() {
        this.stringRepresentation = "";
        Class<?> myclass = getClass();
        sprint(myclass.getName());
        sprint("{");
        Field[] fields = myclass.getDeclaredFields();
        for (Field f : fields) {
            int modifier = f.getModifiers();
            if (modifier != 2) {
                Class<?> fieldType = f.getType();
                String fieldName = f.getName();
                if (fieldName.compareTo("stringRepresentation") != 0 && fieldName.compareTo("indentation") != 0) {
                    sprint(fieldName + Separators.COLON);
                    try {
                        if (fieldType.isPrimitive()) {
                            String fname = fieldType.toString();
                            sprint(fname + Separators.COLON);
                            if (fname.compareTo("int") == 0) {
                                int intfield = f.getInt(this);
                                sprint(intfield);
                            } else if (fname.compareTo("short") == 0) {
                                short shortField = f.getShort(this);
                                sprint(shortField);
                            } else if (fname.compareTo("char") == 0) {
                                char charField = f.getChar(this);
                                sprint(charField);
                            } else if (fname.compareTo("long") == 0) {
                                long longField = f.getLong(this);
                                sprint(longField);
                            } else if (fname.compareTo("boolean") == 0) {
                                boolean booleanField = f.getBoolean(this);
                                sprint(booleanField);
                            } else if (fname.compareTo("double") == 0) {
                                double doubleField = f.getDouble(this);
                                sprint(doubleField);
                            } else if (fname.compareTo("float") == 0) {
                                float floatField = f.getFloat(this);
                                sprint(floatField);
                            }
                        } else if (GenericObject.class.isAssignableFrom(fieldType)) {
                            if (f.get(this) != null) {
                                sprint(((GenericObject) f.get(this)).debugDump(this.indentation + 1));
                            } else {
                                sprint("<null>");
                            }
                        } else if (GenericObjectList.class.isAssignableFrom(fieldType)) {
                            if (f.get(this) != null) {
                                sprint(((GenericObjectList) f.get(this)).debugDump(this.indentation + 1));
                            } else {
                                sprint("<null>");
                            }
                        } else {
                            if (f.get(this) != null) {
                                sprint(f.get(this).getClass().getName() + Separators.COLON);
                            } else {
                                sprint(fieldType.getName() + Separators.COLON);
                            }
                            sprint("{");
                            if (f.get(this) != null) {
                                sprint(f.get(this).toString());
                            } else {
                                sprint("<null>");
                            }
                            sprint("}");
                        }
                    } catch (IllegalAccessException e) {
                    }
                }
            }
        }
        sprint("}");
        return this.stringRepresentation;
    }

    protected MessageObject() {
    }

    public String dbgPrint(int indent) {
        int save = this.indentation;
        this.indentation = indent;
        String retval = toString();
        this.indentation = save;
        return retval;
    }
}
