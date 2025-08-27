package androidx.versionedparcelable;

import java.lang.reflect.InvocationTargetException;

/* loaded from: classes.dex */
public abstract class VersionedParcel {
    protected abstract void closeField();

    protected abstract VersionedParcel createSubParcel();

    protected abstract String readString();

    protected abstract void writeString(String str);

    protected void writeVersionedParcelable(VersionedParcelable p) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (p == null) {
            writeString(null);
            return;
        }
        writeVersionedParcelableCreator(p);
        VersionedParcel subParcel = createSubParcel();
        writeToParcel(p, subParcel);
        subParcel.closeField();
    }

    private void writeVersionedParcelableCreator(VersionedParcelable p) {
        try {
            Class name = findParcelClass((Class<? extends VersionedParcelable>) p.getClass());
            writeString(name.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(p.getClass().getSimpleName() + " does not have a Parcelizer", e);
        }
    }

    protected <T extends VersionedParcelable> T readVersionedParcelable() {
        String string = readString();
        if (string == null) {
            return null;
        }
        return (T) readFromParcel(string, createSubParcel());
    }

    protected static <T extends VersionedParcelable> T readFromParcel(String parcelCls, VersionedParcel versionedParcel) throws ClassNotFoundException {
        try {
            Class<?> cls = Class.forName(parcelCls, true, VersionedParcel.class.getClassLoader());
            return (T) cls.getDeclaredMethod("read", VersionedParcel.class).invoke(null, versionedParcel);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("VersionedParcel encountered ClassNotFoundException", e);
        } catch (IllegalAccessException e2) {
            throw new RuntimeException("VersionedParcel encountered IllegalAccessException", e2);
        } catch (NoSuchMethodException e3) {
            throw new RuntimeException("VersionedParcel encountered NoSuchMethodException", e3);
        } catch (InvocationTargetException e4) {
            if (e4.getCause() instanceof RuntimeException) {
                throw ((RuntimeException) e4.getCause());
            }
            throw new RuntimeException("VersionedParcel encountered InvocationTargetException", e4);
        }
    }

    protected static <T extends VersionedParcelable> void writeToParcel(T val, VersionedParcel versionedParcel) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        try {
            Class cls = findParcelClass(val);
            cls.getDeclaredMethod("write", val.getClass(), VersionedParcel.class).invoke(null, val, versionedParcel);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("VersionedParcel encountered ClassNotFoundException", e);
        } catch (IllegalAccessException e2) {
            throw new RuntimeException("VersionedParcel encountered IllegalAccessException", e2);
        } catch (NoSuchMethodException e3) {
            throw new RuntimeException("VersionedParcel encountered NoSuchMethodException", e3);
        } catch (InvocationTargetException e4) {
            if (e4.getCause() instanceof RuntimeException) {
                throw ((RuntimeException) e4.getCause());
            }
            throw new RuntimeException("VersionedParcel encountered InvocationTargetException", e4);
        }
    }

    private static <T extends VersionedParcelable> Class findParcelClass(T val) throws ClassNotFoundException {
        return findParcelClass((Class<? extends VersionedParcelable>) val.getClass());
    }

    private static Class findParcelClass(Class<? extends VersionedParcelable> cls) throws ClassNotFoundException {
        String pkg = cls.getPackage().getName();
        String c = String.format("%s.%sParcelizer", pkg, cls.getSimpleName());
        return Class.forName(c, false, cls.getClassLoader());
    }
}
