package java.lang.reflect;

import com.android.dex.Dex;

public final class ArtField {
    private int accessFlags;
    private Class<?> declaringClass;
    private int fieldDexIndex;
    private int offset;

    private ArtField() {
    }

    public int getAccessFlags() {
        return this.accessFlags;
    }

    int getDexFieldIndex() {
        return this.fieldDexIndex;
    }

    int getOffset() {
        return this.offset;
    }

    public String getName() {
        if (this.fieldDexIndex == -1) {
            if (!this.declaringClass.isProxy()) {
                throw new AssertionError();
            }
            return "throws";
        }
        Dex dex = this.declaringClass.getDex();
        int nameIndex = dex.nameIndexFromFieldIndex(this.fieldDexIndex);
        return this.declaringClass.getDexCacheString(dex, nameIndex);
    }

    Class<?> getDeclaringClass() {
        return this.declaringClass;
    }

    Class<?> getType() {
        if (this.fieldDexIndex == -1) {
            if (!this.declaringClass.isProxy()) {
                throw new AssertionError();
            }
            return Class[][].class;
        }
        Dex dex = this.declaringClass.getDex();
        int typeIndex = dex.typeIndexFromFieldIndex(this.fieldDexIndex);
        return this.declaringClass.getDexCacheType(dex, typeIndex);
    }
}
