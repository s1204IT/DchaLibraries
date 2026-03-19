package java.nio.file.attribute;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

public final class PosixFilePermissions {
    private PosixFilePermissions() {
    }

    private static void writeBits(StringBuilder sb, boolean z, boolean z2, boolean z3) {
        if (z) {
            sb.append('r');
        } else {
            sb.append('-');
        }
        if (z2) {
            sb.append('w');
        } else {
            sb.append('-');
        }
        if (z3) {
            sb.append(Locale.PRIVATE_USE_EXTENSION);
        } else {
            sb.append('-');
        }
    }

    public static String toString(Set<PosixFilePermission> set) {
        StringBuilder sb = new StringBuilder(9);
        writeBits(sb, set.contains(PosixFilePermission.OWNER_READ), set.contains(PosixFilePermission.OWNER_WRITE), set.contains(PosixFilePermission.OWNER_EXECUTE));
        writeBits(sb, set.contains(PosixFilePermission.GROUP_READ), set.contains(PosixFilePermission.GROUP_WRITE), set.contains(PosixFilePermission.GROUP_EXECUTE));
        writeBits(sb, set.contains(PosixFilePermission.OTHERS_READ), set.contains(PosixFilePermission.OTHERS_WRITE), set.contains(PosixFilePermission.OTHERS_EXECUTE));
        return sb.toString();
    }

    private static boolean isSet(char c, char c2) {
        if (c == c2) {
            return true;
        }
        if (c == '-') {
            return false;
        }
        throw new IllegalArgumentException("Invalid mode");
    }

    private static boolean isR(char c) {
        return isSet(c, 'r');
    }

    private static boolean isW(char c) {
        return isSet(c, 'w');
    }

    private static boolean isX(char c) {
        return isSet(c, Locale.PRIVATE_USE_EXTENSION);
    }

    public static Set<PosixFilePermission> fromString(String str) {
        if (str.length() != 9) {
            throw new IllegalArgumentException("Invalid mode");
        }
        EnumSet enumSetNoneOf = EnumSet.noneOf(PosixFilePermission.class);
        if (isR(str.charAt(0))) {
            enumSetNoneOf.add(PosixFilePermission.OWNER_READ);
        }
        if (isW(str.charAt(1))) {
            enumSetNoneOf.add(PosixFilePermission.OWNER_WRITE);
        }
        if (isX(str.charAt(2))) {
            enumSetNoneOf.add(PosixFilePermission.OWNER_EXECUTE);
        }
        if (isR(str.charAt(3))) {
            enumSetNoneOf.add(PosixFilePermission.GROUP_READ);
        }
        if (isW(str.charAt(4))) {
            enumSetNoneOf.add(PosixFilePermission.GROUP_WRITE);
        }
        if (isX(str.charAt(5))) {
            enumSetNoneOf.add(PosixFilePermission.GROUP_EXECUTE);
        }
        if (isR(str.charAt(6))) {
            enumSetNoneOf.add(PosixFilePermission.OTHERS_READ);
        }
        if (isW(str.charAt(7))) {
            enumSetNoneOf.add(PosixFilePermission.OTHERS_WRITE);
        }
        if (isX(str.charAt(8))) {
            enumSetNoneOf.add(PosixFilePermission.OTHERS_EXECUTE);
        }
        return enumSetNoneOf;
    }

    public static FileAttribute<Set<PosixFilePermission>> asFileAttribute(Set<PosixFilePermission> set) {
        final HashSet hashSet = new HashSet(set);
        Iterator<E> it = hashSet.iterator();
        while (it.hasNext()) {
            if (((PosixFilePermission) it.next()) == null) {
                throw new NullPointerException();
            }
        }
        return new FileAttribute<Set<PosixFilePermission>>() {
            @Override
            public String name() {
                return "posix:permissions";
            }

            @Override
            public Set<PosixFilePermission> value() {
                return Collections.unmodifiableSet(hashSet);
            }
        };
    }
}
