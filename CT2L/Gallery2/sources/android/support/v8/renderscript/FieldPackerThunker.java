package android.support.v8.renderscript;

public class FieldPackerThunker {
    private android.renderscript.FieldPacker mN;
    private int mPos = 0;

    public FieldPackerThunker(int len) {
        this.mN = new android.renderscript.FieldPacker(len);
    }

    void align(int v) {
        this.mN.align(v);
        while ((this.mPos & (v - 1)) != 0) {
            this.mPos++;
        }
    }

    void reset() {
        this.mN.reset();
        this.mPos = 0;
    }

    void reset(int i) {
        this.mN.reset(i);
        this.mPos = i;
    }

    public void skip(int i) {
        this.mN.skip(i);
        this.mPos += i;
    }

    public void addI8(byte v) {
        this.mN.addI8(v);
        this.mPos++;
    }

    public void addI16(short v) {
        this.mN.addI16(v);
        this.mPos += 2;
    }

    public void addI32(int v) {
        this.mN.addI32(v);
        this.mPos += 4;
    }

    public void addI64(long v) {
        this.mN.addI64(v);
        this.mPos += 8;
    }

    public void addU8(short v) {
        this.mN.addU8(v);
        this.mPos++;
    }

    public void addU16(int v) {
        this.mN.addU16(v);
        this.mPos += 2;
    }

    public void addU32(long v) {
        this.mN.addU32(v);
        this.mPos += 4;
    }

    public void addU64(long v) {
        this.mN.addU64(v);
        this.mPos += 8;
    }

    public void addF32(float v) {
        this.mN.addF32(v);
        this.mPos += 4;
    }

    public void addF64(double v) {
        this.mN.addF64(v);
        this.mPos += 8;
    }

    public void addObj(BaseObj obj) {
        if (obj != null) {
            this.mN.addObj(obj.getNObj());
        } else {
            this.mN.addObj(null);
        }
        this.mPos += 4;
    }

    public void addF32(Float2 v) {
        this.mN.addF32(new android.renderscript.Float2(v.x, v.y));
        this.mPos += 8;
    }

    public void addF32(Float3 v) {
        this.mN.addF32(new android.renderscript.Float3(v.x, v.y, v.z));
        this.mPos += 12;
    }

    public void addF32(Float4 v) {
        this.mN.addF32(new android.renderscript.Float4(v.x, v.y, v.z, v.w));
        this.mPos += 16;
    }

    public void addF64(Double2 v) {
        this.mN.addF64(new android.renderscript.Double2(v.x, v.y));
        this.mPos += 16;
    }

    public void addF64(Double3 v) {
        this.mN.addF64(new android.renderscript.Double3(v.x, v.y, v.z));
        this.mPos += 24;
    }

    public void addF64(Double4 v) {
        this.mN.addF64(new android.renderscript.Double4(v.x, v.y, v.z, v.w));
        this.mPos += 32;
    }

    public void addI8(Byte2 v) {
        this.mN.addI8(new android.renderscript.Byte2(v.x, v.y));
        this.mPos += 2;
    }

    public void addI8(Byte3 v) {
        this.mN.addI8(new android.renderscript.Byte3(v.x, v.y, v.z));
        this.mPos += 3;
    }

    public void addI8(Byte4 v) {
        this.mN.addI8(new android.renderscript.Byte4(v.x, v.y, v.z, v.w));
        this.mPos += 4;
    }

    public void addU8(Short2 v) {
        this.mN.addU8(new android.renderscript.Short2(v.x, v.y));
        this.mPos += 2;
    }

    public void addU8(Short3 v) {
        this.mN.addU8(new android.renderscript.Short3(v.x, v.y, v.z));
        this.mPos += 3;
    }

    public void addU8(Short4 v) {
        this.mN.addU8(new android.renderscript.Short4(v.x, v.y, v.z, v.w));
        this.mPos += 4;
    }

    public void addI16(Short2 v) {
        this.mN.addI16(new android.renderscript.Short2(v.x, v.y));
        this.mPos += 4;
    }

    public void addI16(Short3 v) {
        this.mN.addI16(new android.renderscript.Short3(v.x, v.y, v.z));
        this.mPos += 6;
    }

    public void addI16(Short4 v) {
        this.mN.addI16(new android.renderscript.Short4(v.x, v.y, v.z, v.w));
        this.mPos += 8;
    }

    public void addU16(Int2 v) {
        this.mN.addU16(new android.renderscript.Int2(v.x, v.y));
        this.mPos += 4;
    }

    public void addU16(Int3 v) {
        this.mN.addU16(new android.renderscript.Int3(v.x, v.y, v.z));
        this.mPos += 6;
    }

    public void addU16(Int4 v) {
        this.mN.addU16(new android.renderscript.Int4(v.x, v.y, v.z, v.w));
        this.mPos += 8;
    }

    public void addI32(Int2 v) {
        this.mN.addI32(new android.renderscript.Int2(v.x, v.y));
        this.mPos += 8;
    }

    public void addI32(Int3 v) {
        this.mN.addI32(new android.renderscript.Int3(v.x, v.y, v.z));
        this.mPos += 12;
    }

    public void addI32(Int4 v) {
        this.mN.addI32(new android.renderscript.Int4(v.x, v.y, v.z, v.w));
        this.mPos += 16;
    }

    public void addU32(Long2 v) {
        this.mN.addU32(new android.renderscript.Long2(v.x, v.y));
        this.mPos += 8;
    }

    public void addU32(Long3 v) {
        this.mN.addU32(new android.renderscript.Long3(v.x, v.y, v.z));
        this.mPos += 12;
    }

    public void addU32(Long4 v) {
        this.mN.addU32(new android.renderscript.Long4(v.x, v.y, v.z, v.w));
        this.mPos += 16;
    }

    public void addI64(Long2 v) {
        this.mN.addI64(new android.renderscript.Long2(v.x, v.y));
        this.mPos += 16;
    }

    public void addI64(Long3 v) {
        this.mN.addI64(new android.renderscript.Long3(v.x, v.y, v.z));
        this.mPos += 24;
    }

    public void addI64(Long4 v) {
        this.mN.addI64(new android.renderscript.Long4(v.x, v.y, v.z, v.w));
        this.mPos += 32;
    }

    public void addU64(Long2 v) {
        this.mN.addU64(new android.renderscript.Long2(v.x, v.y));
        this.mPos += 16;
    }

    public void addU64(Long3 v) {
        this.mN.addU64(new android.renderscript.Long3(v.x, v.y, v.z));
        this.mPos += 24;
    }

    public void addU64(Long4 v) {
        this.mN.addU64(new android.renderscript.Long4(v.x, v.y, v.z, v.w));
        this.mPos += 32;
    }

    public void addMatrix(Matrix4f v) {
        this.mN.addMatrix(new android.renderscript.Matrix4f(v.getArray()));
        this.mPos += 64;
    }

    public void addMatrix(Matrix3f v) {
        this.mN.addMatrix(new android.renderscript.Matrix3f(v.getArray()));
        this.mPos += 36;
    }

    public void addMatrix(Matrix2f v) {
        this.mN.addMatrix(new android.renderscript.Matrix2f(v.getArray()));
        this.mPos += 16;
    }

    public void addBoolean(boolean v) {
        this.mN.addBoolean(v);
        this.mPos++;
    }

    public final byte[] getData() {
        return this.mN.getData();
    }

    public int getPos() {
        return this.mPos;
    }
}
