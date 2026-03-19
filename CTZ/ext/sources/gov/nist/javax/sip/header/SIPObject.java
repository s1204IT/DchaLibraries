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
    public StringBuffer encode(StringBuffer stringBuffer) {
        stringBuffer.append(encode());
        return stringBuffer;
    }

    @Override
    public boolean equals(Object obj) {
        if (!getClass().equals(obj.getClass())) {
            return false;
        }
        SIPObject sIPObject = (SIPObject) obj;
        Class<?> superclass = getClass();
        Class<?> superclass2 = obj.getClass();
        while (true) {
            Field[] declaredFields = superclass.getDeclaredFields();
            if (!superclass2.equals(superclass)) {
                return false;
            }
            Field[] declaredFields2 = superclass2.getDeclaredFields();
            for (int i = 0; i < declaredFields.length; i++) {
                Field field = declaredFields[i];
                Field field2 = declaredFields2[i];
                int modifiers = field.getModifiers();
                if ((modifiers & 2) != 2) {
                    Class<?> type = field.getType();
                    String name = field.getName();
                    if (name.compareTo("stringRepresentation") != 0 && name.compareTo("indentation") != 0) {
                        try {
                            if (type.isPrimitive()) {
                                String string = type.toString();
                                if (string.compareTo("int") == 0) {
                                    if (field.getInt(this) != field2.getInt(sIPObject)) {
                                        return false;
                                    }
                                } else if (string.compareTo("short") == 0) {
                                    if (field.getShort(this) != field2.getShort(sIPObject)) {
                                        return false;
                                    }
                                } else if (string.compareTo("char") == 0) {
                                    if (field.getChar(this) != field2.getChar(sIPObject)) {
                                        return false;
                                    }
                                } else if (string.compareTo("long") == 0) {
                                    if (field.getLong(this) != field2.getLong(sIPObject)) {
                                        return false;
                                    }
                                } else if (string.compareTo("boolean") == 0) {
                                    if (field.getBoolean(this) != field2.getBoolean(sIPObject)) {
                                        return false;
                                    }
                                } else if (string.compareTo("double") == 0) {
                                    if (field.getDouble(this) != field2.getDouble(sIPObject)) {
                                        return false;
                                    }
                                } else if (string.compareTo("float") == 0 && field.getFloat(this) != field2.getFloat(sIPObject)) {
                                    return false;
                                }
                            } else if (field2.get(sIPObject) != field.get(this)) {
                                if (field.get(this) == null && field2.get(sIPObject) != null) {
                                    return false;
                                }
                                if ((field2.get(sIPObject) == null && field.get(this) != null) || !field.get(this).equals(field2.get(sIPObject))) {
                                    return false;
                                }
                            }
                        } catch (IllegalAccessException e) {
                            System.out.println("accessed field " + name);
                            System.out.println("modifier  " + modifiers);
                            System.out.println("modifier.private  2");
                            InternalErrorHandler.handleException(e);
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
    public boolean match(Object obj) {
        if (obj == null) {
            return true;
        }
        if (!getClass().equals(obj.getClass())) {
            return false;
        }
        GenericObject genericObject = (GenericObject) obj;
        Class<?> superclass = getClass();
        Class<?> superclass2 = obj.getClass();
        while (true) {
            Field[] declaredFields = superclass.getDeclaredFields();
            Field[] declaredFields2 = superclass2.getDeclaredFields();
            for (int i = 0; i < declaredFields.length; i++) {
                Field field = declaredFields[i];
                Field field2 = declaredFields2[i];
                if ((field.getModifiers() & 2) != 2) {
                    Class<?> type = field.getType();
                    String name = field.getName();
                    if (name.compareTo("stringRepresentation") != 0 && name.compareTo("indentation") != 0) {
                        try {
                            if (type.isPrimitive()) {
                                String string = type.toString();
                                if (string.compareTo("int") == 0) {
                                    if (field.getInt(this) != field2.getInt(genericObject)) {
                                        return false;
                                    }
                                } else if (string.compareTo("short") == 0) {
                                    if (field.getShort(this) != field2.getShort(genericObject)) {
                                        return false;
                                    }
                                } else if (string.compareTo("char") == 0) {
                                    if (field.getChar(this) != field2.getChar(genericObject)) {
                                        return false;
                                    }
                                } else if (string.compareTo("long") == 0) {
                                    if (field.getLong(this) != field2.getLong(genericObject)) {
                                        return false;
                                    }
                                } else if (string.compareTo("boolean") == 0) {
                                    if (field.getBoolean(this) != field2.getBoolean(genericObject)) {
                                        return false;
                                    }
                                } else if (string.compareTo("double") == 0) {
                                    if (field.getDouble(this) != field2.getDouble(genericObject)) {
                                        return false;
                                    }
                                } else if (string.compareTo("float") != 0) {
                                    InternalErrorHandler.handleException("unknown type");
                                } else if (field.getFloat(this) != field2.getFloat(genericObject)) {
                                    return false;
                                }
                            } else {
                                Object obj2 = field.get(this);
                                Object obj3 = field2.get(genericObject);
                                if (obj3 != null && obj2 == null) {
                                    return false;
                                }
                                if ((obj3 != null || obj2 == null) && (obj3 != null || obj2 != null)) {
                                    if ((obj3 instanceof String) && (obj2 instanceof String)) {
                                        if (!((String) obj3).trim().equals("") && ((String) obj2).compareToIgnoreCase((String) obj3) != 0) {
                                            return false;
                                        }
                                    } else if (obj3 != null && GenericObject.isMySubclass(obj2.getClass()) && GenericObject.isMySubclass(obj3.getClass()) && obj2.getClass().equals(obj3.getClass()) && ((GenericObject) obj3).getMatcher() != null) {
                                        if (!((GenericObject) obj3).getMatcher().match(((GenericObject) obj2).encode())) {
                                            return false;
                                        }
                                    } else {
                                        if (GenericObject.isMySubclass(obj2.getClass()) && !((GenericObject) obj2).match(obj3)) {
                                            return false;
                                        }
                                        if (GenericObjectList.isMySubclass(obj2.getClass()) && !((GenericObjectList) obj2).match(obj3)) {
                                            return false;
                                        }
                                    }
                                }
                            }
                        } catch (IllegalAccessException e) {
                            InternalErrorHandler.handleException(e);
                        }
                    }
                }
            }
            if (superclass.equals(SIPObject.class)) {
                return true;
            }
            superclass = superclass.getSuperclass();
            superclass2 = superclass2.getSuperclass();
        }
    }

    @Override
    public String debugDump() {
        this.stringRepresentation = "";
        Class<?> cls = getClass();
        sprint(cls.getName());
        sprint("{");
        for (Field field : cls.getDeclaredFields()) {
            if ((field.getModifiers() & 2) != 2) {
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

    @Override
    public String debugDump(int i) {
        int i2 = this.indentation;
        this.indentation = i;
        String strDebugDump = debugDump();
        this.indentation = i2;
        return strDebugDump;
    }

    public String toString() {
        return encode();
    }
}
