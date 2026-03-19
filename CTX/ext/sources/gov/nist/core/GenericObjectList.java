package gov.nist.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public abstract class GenericObjectList extends LinkedList<GenericObject> implements Serializable, Cloneable {
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
    protected int indentation;
    protected String listName;
    protected Class<?> myClass;
    private ListIterator<? extends GenericObject> myListIterator;
    protected String separator;
    private String stringRep;

    protected String getIndentation() {
        char[] cArr = new char[this.indentation];
        Arrays.fill(cArr, ' ');
        return new String(cArr);
    }

    protected static boolean isCloneable(Object obj) {
        return obj instanceof Cloneable;
    }

    public static boolean isMySubclass(Class<?> cls) {
        return GenericObjectList.class.isAssignableFrom(cls);
    }

    @Override
    public Object clone() {
        GenericObjectList genericObjectList = (GenericObjectList) super.clone();
        ListIterator listIterator = genericObjectList.listIterator();
        while (listIterator.hasNext()) {
            listIterator.set((GenericObject) ((GenericObject) listIterator.next()).clone());
        }
        return genericObjectList;
    }

    public void setMyClass(Class cls) {
        this.myClass = cls;
    }

    protected GenericObjectList() {
        this.listName = null;
        this.stringRep = "";
        this.separator = ";";
    }

    protected GenericObjectList(String str) {
        this();
        this.listName = str;
    }

    protected GenericObjectList(String str, String str2) {
        this(str);
        try {
            this.myClass = Class.forName(str2);
        } catch (ClassNotFoundException e) {
            InternalErrorHandler.handleException(e);
        }
    }

    protected GenericObjectList(String str, Class cls) {
        this(str);
        this.myClass = cls;
    }

    protected GenericObject next(ListIterator listIterator) {
        try {
            return (GenericObject) listIterator.next();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    protected GenericObject first() {
        this.myListIterator = listIterator(0);
        try {
            return this.myListIterator.next();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    protected GenericObject next() {
        if (this.myListIterator == null) {
            this.myListIterator = listIterator(0);
        }
        try {
            return this.myListIterator.next();
        } catch (NoSuchElementException e) {
            this.myListIterator = null;
            return null;
        }
    }

    protected void concatenate(GenericObjectList genericObjectList) {
        concatenate(genericObjectList, false);
    }

    protected void concatenate(GenericObjectList genericObjectList, boolean z) {
        if (!z) {
            addAll(genericObjectList);
        } else {
            addAll(0, genericObjectList);
        }
    }

    private void sprint(String str) {
        if (str == null) {
            this.stringRep += getIndentation();
            this.stringRep += "<null>\n";
            return;
        }
        if (str.compareTo("}") == 0 || str.compareTo("]") == 0) {
            this.indentation--;
        }
        this.stringRep += getIndentation();
        this.stringRep += str;
        this.stringRep += "\n";
        if (str.compareTo("{") == 0 || str.compareTo("[") == 0) {
            this.indentation++;
        }
    }

    public String debugDump() {
        this.stringRep = "";
        GenericObject genericObjectFirst = first();
        if (genericObjectFirst == null) {
            return "<null>";
        }
        sprint("listName:");
        sprint(this.listName);
        sprint("{");
        while (genericObjectFirst != null) {
            sprint("[");
            sprint(genericObjectFirst.debugDump(this.indentation));
            genericObjectFirst = next();
            sprint("]");
        }
        sprint("}");
        return this.stringRep;
    }

    public String debugDump(int i) {
        int i2 = this.indentation;
        this.indentation = i;
        String strDebugDump = debugDump();
        this.indentation = i2;
        return strDebugDump;
    }

    @Override
    public void addFirst(GenericObject genericObject) {
        if (this.myClass == null) {
            this.myClass = genericObject.getClass();
        } else {
            super.addFirst(genericObject);
        }
    }

    public void mergeObjects(GenericObjectList genericObjectList) {
        if (genericObjectList == null) {
            return;
        }
        ListIterator listIterator = listIterator();
        ListIterator listIterator2 = genericObjectList.listIterator();
        while (listIterator.hasNext()) {
            GenericObject genericObject = (GenericObject) listIterator.next();
            while (listIterator2.hasNext()) {
                genericObject.merge(listIterator2.next());
            }
        }
    }

    public String encode() {
        if (isEmpty()) {
            return "";
        }
        StringBuffer stringBuffer = new StringBuffer();
        ListIterator listIterator = listIterator();
        if (listIterator.hasNext()) {
            while (true) {
                Object next = listIterator.next();
                if (next instanceof GenericObject) {
                    stringBuffer.append(((GenericObject) next).encode());
                } else {
                    stringBuffer.append(next.toString());
                }
                if (!listIterator.hasNext()) {
                    break;
                }
                stringBuffer.append(this.separator);
            }
        }
        return stringBuffer.toString();
    }

    @Override
    public String toString() {
        return encode();
    }

    public void setSeparator(String str) {
        this.separator = str;
    }

    @Override
    public int hashCode() {
        return 42;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        GenericObjectList genericObjectList = (GenericObjectList) obj;
        if (size() != genericObjectList.size()) {
            return false;
        }
        ListIterator listIterator = listIterator();
        while (listIterator.hasNext()) {
            do {
                try {
                } catch (NoSuchElementException e) {
                    return false;
                }
            } while (!listIterator.next().equals(genericObjectList.listIterator().next()));
        }
        ListIterator listIterator2 = genericObjectList.listIterator();
        while (listIterator2.hasNext()) {
            do {
                try {
                } catch (NoSuchElementException e2) {
                    return false;
                }
            } while (!listIterator2.next().equals(listIterator().next()));
        }
        return true;
    }

    public boolean match(Object obj) {
        if (!getClass().equals(obj.getClass())) {
            return false;
        }
        ListIterator listIterator = ((GenericObjectList) obj).listIterator();
        if (listIterator.hasNext()) {
            Object next = listIterator.next();
            ListIterator listIterator2 = listIterator();
            while (listIterator2.hasNext()) {
                Object next2 = listIterator2.next();
                if (next2 instanceof GenericObject) {
                    System.out.println("Trying to match  = " + ((GenericObject) next2).encode());
                }
                if (!GenericObject.isMySubclass(next2.getClass()) || !((GenericObject) next2).match(next)) {
                    if (isMySubclass(next2.getClass()) && ((GenericObjectList) next2).match(next)) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
            System.out.println(((GenericObject) next).encode());
            return false;
        }
        return true;
    }
}
