package android.content;

import android.os.Parcel;
import android.os.Parcelable;
import java.io.PrintWriter;

public final class ComponentName implements Parcelable, Cloneable, Comparable<ComponentName> {
    public static final Parcelable.Creator<ComponentName> CREATOR = new Parcelable.Creator<ComponentName>() {
        @Override
        public ComponentName createFromParcel(Parcel in) {
            return new ComponentName(in);
        }

        @Override
        public ComponentName[] newArray(int size) {
            return new ComponentName[size];
        }
    };
    private final String mClass;
    private final String mPackage;

    public ComponentName(String pkg, String cls) {
        if (pkg == null) {
            throw new NullPointerException("package name is null");
        }
        if (cls == null) {
            throw new NullPointerException("class name is null");
        }
        this.mPackage = pkg;
        this.mClass = cls;
    }

    public ComponentName(Context pkg, String cls) {
        if (cls == null) {
            throw new NullPointerException("class name is null");
        }
        this.mPackage = pkg.getPackageName();
        this.mClass = cls;
    }

    public ComponentName(Context pkg, Class<?> cls) {
        this.mPackage = pkg.getPackageName();
        this.mClass = cls.getName();
    }

    public ComponentName m14clone() {
        return new ComponentName(this.mPackage, this.mClass);
    }

    public String getPackageName() {
        return this.mPackage;
    }

    public String getClassName() {
        return this.mClass;
    }

    public String getShortClassName() {
        int PN;
        int CN;
        return (this.mClass.startsWith(this.mPackage) && (CN = this.mClass.length()) > PN && this.mClass.charAt(PN) == '.') ? this.mClass.substring(PN, CN) : this.mClass;
    }

    private static void appendShortClassName(StringBuilder sb, String packageName, String className) {
        int PN;
        int CN;
        if (className.startsWith(packageName) && (CN = className.length()) > (PN = packageName.length()) && className.charAt(PN) == '.') {
            sb.append((CharSequence) className, PN, CN);
        } else {
            sb.append(className);
        }
    }

    private static void printShortClassName(PrintWriter pw, String packageName, String className) {
        int PN;
        int CN;
        if (className.startsWith(packageName) && (CN = className.length()) > (PN = packageName.length()) && className.charAt(PN) == '.') {
            pw.write(className, PN, CN - PN);
        } else {
            pw.print(className);
        }
    }

    public String flattenToString() {
        return this.mPackage + "/" + this.mClass;
    }

    public String flattenToShortString() {
        StringBuilder sb = new StringBuilder(this.mPackage.length() + this.mClass.length());
        appendShortString(sb, this.mPackage, this.mClass);
        return sb.toString();
    }

    public void appendShortString(StringBuilder sb) {
        appendShortString(sb, this.mPackage, this.mClass);
    }

    public static void appendShortString(StringBuilder sb, String packageName, String className) {
        sb.append(packageName).append('/');
        appendShortClassName(sb, packageName, className);
    }

    public static void printShortString(PrintWriter pw, String packageName, String className) {
        pw.print(packageName);
        pw.print('/');
        printShortClassName(pw, packageName, className);
    }

    public static ComponentName unflattenFromString(String str) {
        int sep = str.indexOf(47);
        if (sep < 0 || sep + 1 >= str.length()) {
            return null;
        }
        String pkg = str.substring(0, sep);
        String cls = str.substring(sep + 1);
        if (cls.length() > 0 && cls.charAt(0) == '.') {
            cls = pkg + cls;
        }
        return new ComponentName(pkg, cls);
    }

    public String toShortString() {
        return "{" + this.mPackage + "/" + this.mClass + "}";
    }

    public String toString() {
        return "ComponentInfo{" + this.mPackage + "/" + this.mClass + "}";
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        try {
            ComponentName other = (ComponentName) obj;
            if (this.mPackage.equals(other.mPackage)) {
                return this.mClass.equals(other.mClass);
            }
            return false;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public int hashCode() {
        return this.mPackage.hashCode() + this.mClass.hashCode();
    }

    @Override
    public int compareTo(ComponentName that) {
        int v = this.mPackage.compareTo(that.mPackage);
        return v != 0 ? v : this.mClass.compareTo(that.mClass);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.mPackage);
        out.writeString(this.mClass);
    }

    public static void writeToParcel(ComponentName c, Parcel out) {
        if (c != null) {
            c.writeToParcel(out, 0);
        } else {
            out.writeString(null);
        }
    }

    public static ComponentName readFromParcel(Parcel in) {
        String pkg = in.readString();
        if (pkg != null) {
            return new ComponentName(pkg, in);
        }
        return null;
    }

    public ComponentName(Parcel in) {
        this.mPackage = in.readString();
        if (this.mPackage == null) {
            throw new NullPointerException("package name is null");
        }
        this.mClass = in.readString();
        if (this.mClass == null) {
            throw new NullPointerException("class name is null");
        }
    }

    private ComponentName(String pkg, Parcel in) {
        this.mPackage = pkg;
        this.mClass = in.readString();
    }
}
