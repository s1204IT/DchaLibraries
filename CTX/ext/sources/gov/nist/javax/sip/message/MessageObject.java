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
        Class<?> cls = getClass();
        sprint(cls.getName());
        sprint("{");
        for (Field field : cls.getDeclaredFields()) {
            if (field.getModifiers() != 2) {
                Class<?> type = field.getType();
                String name = field.getName();
                if (name.compareTo("stringRepresentation") != 0 && name.compareTo("indentation") != 0) {
                    sprint(name + Separators.COLON);
                    try {
                        if (type.isPrimitive()) {
                            String string = type.toString();
                            sprint(string + Separators.COLON);
                            if (string.compareTo("int") == 0) {
                                sprint(field.getInt(this));
                            } else if (string.compareTo("short") == 0) {
                                sprint(field.getShort(this));
                            } else if (string.compareTo("char") == 0) {
                                sprint(field.getChar(this));
                            } else if (string.compareTo("long") == 0) {
                                sprint(field.getLong(this));
                            } else if (string.compareTo("boolean") == 0) {
                                sprint(field.getBoolean(this));
                            } else if (string.compareTo("double") == 0) {
                                sprint(field.getDouble(this));
                            } else if (string.compareTo("float") == 0) {
                                sprint(field.getFloat(this));
                            }
                        } else if (GenericObject.class.isAssignableFrom(type)) {
                            if (field.get(this) != null) {
                                sprint(((GenericObject) field.get(this)).debugDump(this.indentation + 1));
                            } else {
                                sprint("<null>");
                            }
                        } else if (GenericObjectList.class.isAssignableFrom(type)) {
                            if (field.get(this) != null) {
                                sprint(((GenericObjectList) field.get(this)).debugDump(this.indentation + 1));
                            } else {
                                sprint("<null>");
                            }
                        } else {
                            if (field.get(this) != null) {
                                sprint(field.get(this).getClass().getName() + Separators.COLON);
                            } else {
                                sprint(type.getName() + Separators.COLON);
                            }
                            sprint("{");
                            if (field.get(this) != null) {
                                sprint(field.get(this).toString());
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

    public String dbgPrint(int i) {
        int i2 = this.indentation;
        this.indentation = i;
        String string = toString();
        this.indentation = i2;
        return string;
    }
}
