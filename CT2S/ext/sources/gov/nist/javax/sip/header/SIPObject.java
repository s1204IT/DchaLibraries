package gov.nist.javax.sip.header;

import gov.nist.core.GenericObject;
import gov.nist.core.GenericObjectList;
import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import java.lang.reflect.Field;

public abstract class SIPObject extends GenericObject {
    @Override
    public abstract String encode();

    protected SIPObject() {
    }

    @Override
    public void dbgPrint() {
        super.dbgPrint();
    }

    @Override
    public StringBuffer encode(StringBuffer buffer) {
        return buffer.append(encode());
    }

    @Override
    public boolean equals(Object other) {
        if (!getClass().equals(other.getClass())) {
            return false;
        }
        SIPObject that = (SIPObject) other;
        Class<?> superclass = getClass();
        Class<?> superclass2 = other.getClass();
        while (true) {
            Field[] fields = superclass.getDeclaredFields();
            if (!superclass2.equals(superclass)) {
                return false;
            }
            Field[] hisfields = superclass2.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field f = fields[i];
                Field g = hisfields[i];
                int modifier = f.getModifiers();
                if ((modifier & 2) != 2) {
                    Class<?> type = f.getType();
                    String fieldName = f.getName();
                    if (fieldName.compareTo("stringRepresentation") != 0 && fieldName.compareTo("indentation") != 0) {
                        try {
                            if (type.isPrimitive()) {
                                String fname = type.toString();
                                if (fname.compareTo("int") == 0) {
                                    if (f.getInt(this) != g.getInt(that)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("short") == 0) {
                                    if (f.getShort(this) != g.getShort(that)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("char") == 0) {
                                    if (f.getChar(this) != g.getChar(that)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("long") == 0) {
                                    if (f.getLong(this) != g.getLong(that)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("boolean") == 0) {
                                    if (f.getBoolean(this) != g.getBoolean(that)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("double") == 0) {
                                    if (f.getDouble(this) != g.getDouble(that)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("float") == 0 && f.getFloat(this) != g.getFloat(that)) {
                                    return false;
                                }
                            } else if (g.get(that) == f.get(this)) {
                                continue;
                            } else {
                                if (f.get(this) == null && g.get(that) != null) {
                                    return false;
                                }
                                if ((g.get(that) == null && f.get(this) != null) || !f.get(this).equals(g.get(that))) {
                                    return false;
                                }
                            }
                        } catch (IllegalAccessException ex1) {
                            System.out.println("accessed field " + fieldName);
                            System.out.println("modifier  " + modifier);
                            System.out.println("modifier.private  2");
                            InternalErrorHandler.handleException(ex1);
                        }
                    }
                }
            }
            if (!superclass.equals(SIPObject.class)) {
                superclass = superclass.getSuperclass();
                superclass2 = superclass2.getSuperclass();
            } else {
                return true;
            }
        }
    }

    @Override
    public boolean match(Object other) {
        if (other == null) {
            return true;
        }
        if (!getClass().equals(other.getClass())) {
            return false;
        }
        GenericObject that = (GenericObject) other;
        Class<?> superclass = getClass();
        Class<?> superclass2 = other.getClass();
        while (true) {
            Field[] fields = superclass.getDeclaredFields();
            Field[] hisfields = superclass2.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field f = fields[i];
                Field g = hisfields[i];
                int modifier = f.getModifiers();
                if ((modifier & 2) != 2) {
                    Class<?> type = f.getType();
                    String fieldName = f.getName();
                    if (fieldName.compareTo("stringRepresentation") != 0 && fieldName.compareTo("indentation") != 0) {
                        try {
                            if (type.isPrimitive()) {
                                String fname = type.toString();
                                if (fname.compareTo("int") == 0) {
                                    if (f.getInt(this) != g.getInt(that)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("short") == 0) {
                                    if (f.getShort(this) != g.getShort(that)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("char") == 0) {
                                    if (f.getChar(this) != g.getChar(that)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("long") == 0) {
                                    if (f.getLong(this) != g.getLong(that)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("boolean") == 0) {
                                    if (f.getBoolean(this) != g.getBoolean(that)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("double") == 0) {
                                    if (f.getDouble(this) != g.getDouble(that)) {
                                        return false;
                                    }
                                } else if (fname.compareTo("float") != 0) {
                                    InternalErrorHandler.handleException("unknown type");
                                } else if (f.getFloat(this) != g.getFloat(that)) {
                                    return false;
                                }
                            } else {
                                Object myObj = f.get(this);
                                Object hisObj = g.get(that);
                                if (hisObj != null && myObj == null) {
                                    return false;
                                }
                                if ((hisObj != null || myObj == null) && (hisObj != null || myObj != null)) {
                                    if ((hisObj instanceof String) && (myObj instanceof String)) {
                                        if (!((String) hisObj).trim().equals("") && ((String) myObj).compareToIgnoreCase((String) hisObj) != 0) {
                                            return false;
                                        }
                                    } else if (hisObj != null && GenericObject.isMySubclass(myObj.getClass()) && GenericObject.isMySubclass(hisObj.getClass()) && myObj.getClass().equals(hisObj.getClass()) && ((GenericObject) hisObj).getMatcher() != null) {
                                        String myObjEncoded = ((GenericObject) myObj).encode();
                                        boolean retval = ((GenericObject) hisObj).getMatcher().match(myObjEncoded);
                                        if (!retval) {
                                            return false;
                                        }
                                    } else {
                                        if (GenericObject.isMySubclass(myObj.getClass()) && !((GenericObject) myObj).match(hisObj)) {
                                            return false;
                                        }
                                        if (GenericObjectList.isMySubclass(myObj.getClass()) && !((GenericObjectList) myObj).match(hisObj)) {
                                            return false;
                                        }
                                    }
                                }
                            }
                        } catch (IllegalAccessException ex1) {
                            InternalErrorHandler.handleException(ex1);
                        }
                    }
                }
            }
            if (!superclass.equals(SIPObject.class)) {
                superclass = superclass.getSuperclass();
                superclass2 = superclass2.getSuperclass();
            } else {
                return true;
            }
        }
    }

    @Override
    public String debugDump() {
        this.stringRepresentation = "";
        Class<?> cls = getClass();
        sprint(cls.getName());
        sprint("{");
        Field[] fields = cls.getDeclaredFields();
        for (Field f : fields) {
            int modifier = f.getModifiers();
            if ((modifier & 2) != 2) {
                Class<?> type = f.getType();
                String fieldName = f.getName();
                if (fieldName.compareTo("stringRepresentation") != 0 && fieldName.compareTo("indentation") != 0) {
                    sprint(fieldName + Separators.COLON);
                    try {
                        if (type.isPrimitive()) {
                            String fname = type.toString();
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
                        } else if (GenericObject.class.isAssignableFrom(type)) {
                            if (f.get(this) != null) {
                                sprint(((GenericObject) f.get(this)).debugDump(this.indentation + 1));
                            } else {
                                sprint("<null>");
                            }
                        } else if (GenericObjectList.class.isAssignableFrom(type)) {
                            if (f.get(this) != null) {
                                sprint(((GenericObjectList) f.get(this)).debugDump(this.indentation + 1));
                            } else {
                                sprint("<null>");
                            }
                        } else {
                            if (f.get(this) != null) {
                                sprint(f.get(this).getClass().getName() + Separators.COLON);
                            } else {
                                sprint(type.getName() + Separators.COLON);
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

    @Override
    public String debugDump(int indent) {
        int save = this.indentation;
        this.indentation = indent;
        String retval = debugDump();
        this.indentation = save;
        return retval;
    }

    public String toString() {
        return encode();
    }
}
