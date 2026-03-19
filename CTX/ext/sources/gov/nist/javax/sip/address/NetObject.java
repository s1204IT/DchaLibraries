package gov.nist.javax.sip.address;

import gov.nist.core.GenericObject;
import gov.nist.core.GenericObjectList;
import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import java.lang.reflect.Field;

public abstract class NetObject extends GenericObject {
    protected static final String CORE_PACKAGE = "gov.nist.core";
    protected static final String GRUU = "gr";
    protected static final String LR = "lr";
    protected static final String MADDR = "maddr";
    protected static final String METHOD = "method";
    protected static final String NET_PACKAGE = "gov.nist.javax.sip.address";
    protected static final String PARSER_PACKAGE = "gov.nist.javax.sip.parser";
    protected static final String PHONE = "phone";
    protected static final String SIP = "sip";
    protected static final String SIPS = "sips";
    protected static final String TCP = "tcp";
    protected static final String TLS = "tls";
    protected static final String TRANSPORT = "transport";
    protected static final String TTL = "ttl";
    protected static final String UDP = "udp";
    protected static final String USER = "user";
    protected static final long serialVersionUID = 6149926203633320729L;

    @Override
    public boolean equals(Object obj) {
        if (!getClass().equals(obj.getClass())) {
            return false;
        }
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
                                    if (field.getInt(this) != field2.getInt(obj)) {
                                        return false;
                                    }
                                } else if (string.compareTo("short") == 0) {
                                    if (field.getShort(this) != field2.getShort(obj)) {
                                        return false;
                                    }
                                } else if (string.compareTo("char") == 0) {
                                    if (field.getChar(this) != field2.getChar(obj)) {
                                        return false;
                                    }
                                } else if (string.compareTo("long") == 0) {
                                    if (field.getLong(this) != field2.getLong(obj)) {
                                        return false;
                                    }
                                } else if (string.compareTo("boolean") == 0) {
                                    if (field.getBoolean(this) != field2.getBoolean(obj)) {
                                        return false;
                                    }
                                } else if (string.compareTo("double") == 0) {
                                    if (field.getDouble(this) != field2.getDouble(obj)) {
                                        return false;
                                    }
                                } else if (string.compareTo("float") == 0 && field.getFloat(this) != field2.getFloat(obj)) {
                                    return false;
                                }
                            } else if (field2.get(obj) != field.get(this)) {
                                if (field.get(this) == null && field2.get(obj) != null) {
                                    return false;
                                }
                                if ((field2.get(obj) == null && field.get(obj) != null) || !field.get(this).equals(field2.get(obj))) {
                                    return false;
                                }
                            }
                        } catch (IllegalAccessException e) {
                            InternalErrorHandler.handleException(e);
                        }
                    }
                }
            }
            if (!superclass.equals(NetObject.class)) {
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
        Class<?> superclass = obj.getClass();
        Class<?> superclass2 = getClass();
        while (true) {
            Field[] declaredFields = superclass2.getDeclaredFields();
            Field[] declaredFields2 = superclass.getDeclaredFields();
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
                                } else if (string.compareTo("float") == 0 && field.getFloat(this) != field2.getFloat(genericObject)) {
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
                                        if (!((String) obj3).equals("") && ((String) obj2).compareToIgnoreCase((String) obj3) != 0) {
                                            return false;
                                        }
                                    } else if (GenericObject.isMySubclass(obj2.getClass()) && GenericObject.isMySubclass(obj3.getClass()) && obj2.getClass().equals(obj3.getClass()) && ((GenericObject) obj3).getMatcher() != null) {
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
            if (superclass2.equals(NetObject.class)) {
                return true;
            }
            superclass2 = superclass2.getSuperclass();
            superclass = superclass.getSuperclass();
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
