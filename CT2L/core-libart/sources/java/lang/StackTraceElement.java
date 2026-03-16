package java.lang;

import java.io.Serializable;

public final class StackTraceElement implements Serializable {
    private static final int NATIVE_LINE_NUMBER = -2;
    private static final long serialVersionUID = 6992337162326171013L;
    String declaringClass;
    String fileName;
    int lineNumber;
    String methodName;

    public StackTraceElement(String cls, String method, String file, int line) {
        if (cls == null) {
            throw new NullPointerException("cls == null");
        }
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        this.declaringClass = cls;
        this.methodName = method;
        this.fileName = file;
        this.lineNumber = line;
    }

    private StackTraceElement() {
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof StackTraceElement)) {
            return false;
        }
        StackTraceElement castObj = (StackTraceElement) obj;
        if (this.methodName == null || castObj.methodName == null || !getMethodName().equals(castObj.getMethodName()) || !getClassName().equals(castObj.getClassName())) {
            return false;
        }
        String localFileName = getFileName();
        if (localFileName == null) {
            if (castObj.getFileName() != null) {
                return false;
            }
        } else if (!localFileName.equals(castObj.getFileName())) {
            return false;
        }
        return getLineNumber() == castObj.getLineNumber();
    }

    public String getClassName() {
        return this.declaringClass == null ? "<unknown class>" : this.declaringClass;
    }

    public String getFileName() {
        return this.fileName;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    public String getMethodName() {
        return this.methodName == null ? "<unknown method>" : this.methodName;
    }

    public int hashCode() {
        if (this.methodName == null) {
            return 0;
        }
        return this.methodName.hashCode() ^ this.declaringClass.hashCode();
    }

    public boolean isNativeMethod() {
        return this.lineNumber == -2;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder(80);
        buf.append(getClassName());
        buf.append('.');
        buf.append(getMethodName());
        if (isNativeMethod()) {
            buf.append("(Native Method)");
        } else {
            String fName = getFileName();
            if (fName == null) {
                buf.append("(Unknown Source)");
            } else {
                int lineNum = getLineNumber();
                buf.append('(');
                buf.append(fName);
                if (lineNum >= 0) {
                    buf.append(':');
                    buf.append(lineNum);
                }
                buf.append(')');
            }
        }
        return buf.toString();
    }
}
