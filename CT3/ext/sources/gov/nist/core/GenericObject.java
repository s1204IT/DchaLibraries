package gov.nist.core;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class GenericObject implements Serializable, Cloneable {
    protected static final String AND = "&";
    protected static final String AT = "@";
    protected static final String COLON = ":";
    protected static final String COMMA = ",";
    protected static final String DOT = ".";
    protected static final String DOUBLE_QUOTE = "\"";
    protected static final String EQUALS = "=";
    protected static final String GREATER_THAN = ">";
    protected static final String HT = "\t";
    protected static final String LESS_THAN = "<";
    protected static final String LPAREN = "(";
    protected static final String NEWLINE = "\r\n";
    protected static final String PERCENT = "%";
    protected static final String POUND = "#";
    protected static final String QUESTION = "?";
    protected static final String QUOTE = "'";
    protected static final String RETURN = "\n";
    protected static final String RPAREN = ")";
    protected static final String SEMICOLON = ";";
    protected static final String SLASH = "/";
    protected static final String SP = " ";
    protected static final String STAR = "*";
    protected Match matchExpression;
    protected static final Set<Class<?>> immutableClasses = new HashSet(10);
    static final String[] immutableClassNames = {"String", "Character", "Boolean", "Byte", "Short", "Integer", "Long", "Float", "Double"};
    protected int indentation = 0;
    protected String stringRepresentation = "";

    public abstract String encode();

    static {
        for (int i = 0; i < immutableClassNames.length; i++) {
            try {
                immutableClasses.add(Class.forName("java.lang." + immutableClassNames[i]));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Internal error", e);
            }
        }
    }

    public void setMatcher(Match matchExpression) {
        if (matchExpression == null) {
            throw new IllegalArgumentException("null arg!");
        }
        this.matchExpression = matchExpression;
    }

    public Match getMatcher() {
        return this.matchExpression;
    }

    public static Class<?> getClassFromName(String className) {
        try {
            return Class.forName(className);
        } catch (Exception ex) {
            InternalErrorHandler.handleException(ex);
            return null;
        }
    }

    public static boolean isMySubclass(Class<?> other) {
        return GenericObject.class.isAssignableFrom(other);
    }

    public static Object makeClone(Object obj) throws CloneNotSupportedException {
        if (obj == null) {
            throw new NullPointerException("null obj!");
        }
        Class<?> c = obj.getClass();
        Object clone_obj = obj;
        if (immutableClasses.contains(c)) {
            return obj;
        }
        if (c.isArray()) {
            Class<?> ec = c.getComponentType();
            if (!ec.isPrimitive()) {
                return ((Object[]) obj).clone();
            }
            if (ec == Character.TYPE) {
                clone_obj = ((char[]) obj).clone();
            } else if (ec == Boolean.TYPE) {
                clone_obj = ((boolean[]) obj).clone();
            }
            if (ec == Byte.TYPE) {
                return ((byte[]) obj).clone();
            }
            if (ec == Short.TYPE) {
                return ((short[]) obj).clone();
            }
            if (ec == Integer.TYPE) {
                return ((int[]) obj).clone();
            }
            if (ec == Long.TYPE) {
                return ((long[]) obj).clone();
            }
            if (ec == Float.TYPE) {
                return ((float[]) obj).clone();
            }
            if (ec == Double.TYPE) {
                return ((double[]) obj).clone();
            }
            return clone_obj;
        }
        if (GenericObject.class.isAssignableFrom(c)) {
            return ((GenericObject) obj).clone();
        }
        if (GenericObjectList.class.isAssignableFrom(c)) {
            return ((GenericObjectList) obj).clone();
        }
        if (Cloneable.class.isAssignableFrom(c)) {
            try {
                Method meth = c.getMethod("clone", null);
                return meth.invoke(obj, null);
            } catch (IllegalAccessException e) {
                return clone_obj;
            } catch (IllegalArgumentException ex) {
                InternalErrorHandler.handleException(ex);
                return clone_obj;
            } catch (NoSuchMethodException e2) {
                return clone_obj;
            } catch (SecurityException e3) {
                return clone_obj;
            } catch (InvocationTargetException e4) {
                return clone_obj;
            }
        }
        return clone_obj;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Internal error");
        }
    }

    public void merge(Object mergeObject) {
        if (mergeObject == null) {
            return;
        }
        if (!mergeObject.getClass().equals(getClass())) {
            throw new IllegalArgumentException("Bad override object");
        }
        Class<?> myclass = getClass();
        do {
            Field[] fields = myclass.getDeclaredFields();
            for (Field f : fields) {
                int modifier = f.getModifiers();
                if (!Modifier.isPrivate(modifier) && !Modifier.isStatic(modifier) && !Modifier.isInterface(modifier)) {
                    Class<?> fieldType = f.getType();
                    String fname = fieldType.toString();
                    try {
                        if (fieldType.isPrimitive()) {
                            if (fname.compareTo("int") == 0) {
                                int intfield = f.getInt(mergeObject);
                                f.setInt(this, intfield);
                            } else if (fname.compareTo("short") == 0) {
                                short shortField = f.getShort(mergeObject);
                                f.setShort(this, shortField);
                            } else if (fname.compareTo("char") == 0) {
                                char charField = f.getChar(mergeObject);
                                f.setChar(this, charField);
                            } else if (fname.compareTo("long") == 0) {
                                long longField = f.getLong(mergeObject);
                                f.setLong(this, longField);
                            } else if (fname.compareTo("boolean") == 0) {
                                boolean booleanField = f.getBoolean(mergeObject);
                                f.setBoolean(this, booleanField);
                            } else if (fname.compareTo("double") == 0) {
                                double doubleField = f.getDouble(mergeObject);
                                f.setDouble(this, doubleField);
                            } else if (fname.compareTo("float") == 0) {
                                float floatField = f.getFloat(mergeObject);
                                f.setFloat(this, floatField);
                            }
                        } else {
                            Object obj = f.get(this);
                            Object mobj = f.get(mergeObject);
                            if (mobj != null) {
                                if (obj == null) {
                                    f.set(this, mobj);
                                } else if (obj instanceof GenericObject) {
                                    GenericObject gobj = (GenericObject) obj;
                                    gobj.merge(mobj);
                                } else {
                                    f.set(this, mobj);
                                }
                            }
                        }
                    } catch (IllegalAccessException ex1) {
                        ex1.printStackTrace();
                    }
                }
            }
            myclass = myclass.getSuperclass();
        } while (!myclass.equals(GenericObject.class));
    }

    protected GenericObject() {
    }

    protected String getIndentation() {
        char[] chars = new char[this.indentation];
        Arrays.fill(chars, ' ');
        return new String(chars);
    }

    protected void sprint(String a) {
        if (a == null) {
            this.stringRepresentation += getIndentation();
            this.stringRepresentation += "<null>\n";
            return;
        }
        if (a.compareTo("}") == 0 || a.compareTo("]") == 0) {
            this.indentation--;
        }
        this.stringRepresentation += getIndentation();
        this.stringRepresentation += a;
        this.stringRepresentation += "\n";
        if (a.compareTo("{") != 0 && a.compareTo("[") != 0) {
            return;
        }
        this.indentation++;
    }

    protected void sprint(Object o) {
        sprint(o.toString());
    }

    protected void sprint(int intField) {
        sprint(String.valueOf(intField));
    }

    protected void sprint(short shortField) {
        sprint(String.valueOf((int) shortField));
    }

    protected void sprint(char charField) {
        sprint(String.valueOf(charField));
    }

    protected void sprint(long longField) {
        sprint(String.valueOf(longField));
    }

    protected void sprint(boolean booleanField) {
        sprint(String.valueOf(booleanField));
    }

    protected void sprint(double doubleField) {
        sprint(String.valueOf(doubleField));
    }

    protected void sprint(float floatField) {
        sprint(String.valueOf(floatField));
    }

    protected void dbgPrint() {
        Debug.println(debugDump());
    }

    protected void dbgPrint(String s) {
        Debug.println(s);
    }

    public boolean equals(Object that) {
        if (that == null || !getClass().equals(that.getClass())) {
            return false;
        }
        Class<?> myclass = getClass();
        Class<?> hisclass = that.getClass();
        while (true) {
            Field[] fields = myclass.getDeclaredFields();
            Field[] hisfields = hisclass.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field f = fields[i];
                Field g = hisfields[i];
                int modifier = f.getModifiers();
                if ((modifier & 2) != 2) {
                    Class<?> fieldType = f.getType();
                    String fieldName = f.getName();
                    if (fieldName.compareTo("stringRepresentation") != 0 && fieldName.compareTo("indentation") != 0) {
                        try {
                            if (fieldType.isPrimitive()) {
                                String fname = fieldType.toString();
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
                            } else {
                                if (g.get(that) == f.get(this)) {
                                    return true;
                                }
                                if (f.get(this) == null || g.get(that) == null) {
                                    return false;
                                }
                                if ((g.get(that) == null && f.get(this) != null) || !f.get(this).equals(g.get(that))) {
                                    return false;
                                }
                            }
                        } catch (IllegalAccessException ex1) {
                            InternalErrorHandler.handleException(ex1);
                        }
                    }
                }
            }
            if (!myclass.equals(GenericObject.class)) {
                myclass = myclass.getSuperclass();
                hisclass = hisclass.getSuperclass();
            } else {
                return true;
            }
        }
    }

    public boolean match(Object other) {
        if (other == null) {
            return true;
        }
        if (!getClass().equals(other.getClass())) {
            return false;
        }
        GenericObject that = (GenericObject) other;
        Class<?> myclass = getClass();
        Field[] fields = myclass.getDeclaredFields();
        Class<?> hisclass = other.getClass();
        Field[] hisfields = hisclass.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            Field g = hisfields[i];
            int modifier = f.getModifiers();
            if ((modifier & 2) != 2) {
                Class<?> fieldType = f.getType();
                String fieldName = f.getName();
                if (fieldName.compareTo("stringRepresentation") != 0 && fieldName.compareTo("indentation") != 0) {
                    try {
                        if (fieldType.isPrimitive()) {
                            String fname = fieldType.toString();
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
                                } else {
                                    if (isMySubclass(myObj.getClass()) && !((GenericObject) myObj).match(hisObj)) {
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
        return true;
    }

    public String debugDump() {
        this.stringRepresentation = "";
        Class<?> myclass = getClass();
        sprint(myclass.getName());
        sprint("{");
        Field[] fields = myclass.getDeclaredFields();
        for (Field f : fields) {
            int modifier = f.getModifiers();
            if ((modifier & 2) != 2) {
                Class<?> fieldType = f.getType();
                String fieldName = f.getName();
                if (fieldName.compareTo("stringRepresentation") != 0 && fieldName.compareTo("indentation") != 0) {
                    sprint(fieldName + ":");
                    try {
                        if (fieldType.isPrimitive()) {
                            String fname = fieldType.toString();
                            sprint(fname + ":");
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
                                sprint(f.get(this).getClass().getName() + ":");
                            } else {
                                sprint(fieldType.getName() + ":");
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
                    } catch (Exception ex) {
                        InternalErrorHandler.handleException(ex);
                    }
                }
            }
        }
        sprint("}");
        return this.stringRepresentation;
    }

    public String debugDump(int indent) {
        this.indentation = indent;
        String retval = debugDump();
        this.indentation = 0;
        return retval;
    }

    public StringBuffer encode(StringBuffer buffer) {
        return buffer.append(encode());
    }
}
