package android.support.v8.renderscript;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.renderscript.Allocation;
import android.support.v8.renderscript.Allocation;

class AllocationThunker extends Allocation {
    static BitmapFactory.Options mBitmapOptions = new BitmapFactory.Options();
    android.renderscript.Allocation mN;

    @Override
    android.renderscript.Allocation getNObj() {
        return this.mN;
    }

    static Allocation.MipmapControl convertMipmapControl(Allocation.MipmapControl mc) {
        switch (mc) {
            case MIPMAP_NONE:
                return Allocation.MipmapControl.MIPMAP_NONE;
            case MIPMAP_FULL:
                return Allocation.MipmapControl.MIPMAP_FULL;
            case MIPMAP_ON_SYNC_TO_TEXTURE:
                return Allocation.MipmapControl.MIPMAP_ON_SYNC_TO_TEXTURE;
            default:
                return null;
        }
    }

    @Override
    public Type getType() {
        return TypeThunker.find(this.mN.getType());
    }

    @Override
    public Element getElement() {
        return getType().getElement();
    }

    @Override
    public int getUsage() {
        try {
            return this.mN.getUsage();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public int getBytesSize() {
        try {
            return this.mN.getBytesSize();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    AllocationThunker(RenderScript rs, Type t, int usage, android.renderscript.Allocation na) {
        super(0, rs, t, usage);
        this.mType = t;
        this.mUsage = usage;
        this.mN = na;
    }

    @Override
    public void syncAll(int srcLocation) {
        try {
            this.mN.syncAll(srcLocation);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void ioSend() {
        try {
            this.mN.ioSend();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void ioReceive() {
        try {
            this.mN.ioReceive();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copyFrom(BaseObj[] d) {
        if (d != null) {
            android.renderscript.BaseObj[] dN = new android.renderscript.BaseObj[d.length];
            for (int i = 0; i < d.length; i++) {
                dN[i] = d[i].getNObj();
            }
            try {
                this.mN.copyFrom(dN);
            } catch (android.renderscript.RSRuntimeException e) {
                throw ExceptionThunker.convertException(e);
            }
        }
    }

    @Override
    public void copyFromUnchecked(int[] d) {
        try {
            this.mN.copyFromUnchecked(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copyFromUnchecked(short[] d) {
        try {
            this.mN.copyFromUnchecked(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copyFromUnchecked(byte[] d) {
        try {
            this.mN.copyFromUnchecked(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copyFromUnchecked(float[] d) {
        try {
            this.mN.copyFromUnchecked(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copyFrom(int[] d) {
        try {
            this.mN.copyFrom(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copyFrom(short[] d) {
        try {
            this.mN.copyFrom(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copyFrom(byte[] d) {
        try {
            this.mN.copyFrom(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copyFrom(float[] d) {
        try {
            this.mN.copyFrom(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copyFrom(Bitmap b) {
        try {
            this.mN.copyFrom(b);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copyFrom(Allocation a) {
        AllocationThunker at = (AllocationThunker) a;
        try {
            this.mN.copyFrom(at.mN);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void setFromFieldPacker(int xoff, FieldPacker fp) {
        try {
            byte[] data = fp.getData();
            int fp_length = fp.getPos();
            android.renderscript.FieldPacker nfp = new android.renderscript.FieldPacker(fp_length);
            for (int i = 0; i < fp_length; i++) {
                nfp.addI8(data[i]);
            }
            this.mN.setFromFieldPacker(xoff, nfp);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void setFromFieldPacker(int xoff, int component_number, FieldPacker fp) {
        try {
            byte[] data = fp.getData();
            int fp_length = fp.getPos();
            android.renderscript.FieldPacker nfp = new android.renderscript.FieldPacker(fp_length);
            for (int i = 0; i < fp_length; i++) {
                nfp.addI8(data[i]);
            }
            this.mN.setFromFieldPacker(xoff, component_number, nfp);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void generateMipmaps() {
        try {
            this.mN.generateMipmaps();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copy1DRangeFromUnchecked(int off, int count, int[] d) {
        try {
            this.mN.copy1DRangeFromUnchecked(off, count, d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copy1DRangeFromUnchecked(int off, int count, short[] d) {
        try {
            this.mN.copy1DRangeFromUnchecked(off, count, d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copy1DRangeFromUnchecked(int off, int count, byte[] d) {
        try {
            this.mN.copy1DRangeFromUnchecked(off, count, d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copy1DRangeFromUnchecked(int off, int count, float[] d) {
        try {
            this.mN.copy1DRangeFromUnchecked(off, count, d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copy1DRangeFrom(int off, int count, int[] d) {
        try {
            this.mN.copy1DRangeFrom(off, count, d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copy1DRangeFrom(int off, int count, short[] d) {
        try {
            this.mN.copy1DRangeFrom(off, count, d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copy1DRangeFrom(int off, int count, byte[] d) {
        try {
            this.mN.copy1DRangeFrom(off, count, d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copy1DRangeFrom(int off, int count, float[] d) {
        try {
            this.mN.copy1DRangeFrom(off, count, d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copy1DRangeFrom(int off, int count, Allocation data, int dataOff) {
        try {
            AllocationThunker at = (AllocationThunker) data;
            this.mN.copy1DRangeFrom(off, count, at.mN, dataOff);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copy2DRangeFrom(int xoff, int yoff, int w, int h, byte[] data) {
        try {
            this.mN.copy2DRangeFrom(xoff, yoff, w, h, data);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copy2DRangeFrom(int xoff, int yoff, int w, int h, short[] data) {
        try {
            this.mN.copy2DRangeFrom(xoff, yoff, w, h, data);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copy2DRangeFrom(int xoff, int yoff, int w, int h, int[] data) {
        try {
            this.mN.copy2DRangeFrom(xoff, yoff, w, h, data);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copy2DRangeFrom(int xoff, int yoff, int w, int h, float[] data) {
        try {
            this.mN.copy2DRangeFrom(xoff, yoff, w, h, data);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copy2DRangeFrom(int xoff, int yoff, int w, int h, Allocation data, int dataXoff, int dataYoff) {
        try {
            AllocationThunker at = (AllocationThunker) data;
            this.mN.copy2DRangeFrom(xoff, yoff, w, h, at.mN, dataXoff, dataYoff);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copy2DRangeFrom(int xoff, int yoff, Bitmap data) {
        try {
            this.mN.copy2DRangeFrom(xoff, yoff, data);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copyTo(Bitmap b) {
        try {
            this.mN.copyTo(b);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copyTo(byte[] d) {
        try {
            this.mN.copyTo(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copyTo(short[] d) {
        try {
            this.mN.copyTo(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copyTo(int[] d) {
        try {
            this.mN.copyTo(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    @Override
    public void copyTo(float[] d) {
        try {
            this.mN.copyTo(d);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    static {
        mBitmapOptions.inScaled = false;
    }

    public static Allocation createTyped(RenderScript rs, Type type, Allocation.MipmapControl mips, int usage) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        TypeThunker tt = (TypeThunker) type;
        try {
            android.renderscript.Allocation a = android.renderscript.Allocation.createTyped(rst.mN, tt.mN, convertMipmapControl(mips), usage);
            return new AllocationThunker(rs, type, usage, a);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public static Allocation createFromBitmap(RenderScript rs, Bitmap b, Allocation.MipmapControl mips, int usage) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        try {
            android.renderscript.Allocation a = android.renderscript.Allocation.createFromBitmap(rst.mN, b, convertMipmapControl(mips), usage);
            TypeThunker tt = new TypeThunker(rs, a.getType());
            return new AllocationThunker(rs, tt, usage, a);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public static Allocation createCubemapFromBitmap(RenderScript rs, Bitmap b, Allocation.MipmapControl mips, int usage) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        try {
            android.renderscript.Allocation a = android.renderscript.Allocation.createCubemapFromBitmap(rst.mN, b, convertMipmapControl(mips), usage);
            TypeThunker tt = new TypeThunker(rs, a.getType());
            return new AllocationThunker(rs, tt, usage, a);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public static Allocation createCubemapFromCubeFaces(RenderScript rs, Bitmap xpos, Bitmap xneg, Bitmap ypos, Bitmap yneg, Bitmap zpos, Bitmap zneg, Allocation.MipmapControl mips, int usage) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        try {
            android.renderscript.Allocation a = android.renderscript.Allocation.createCubemapFromCubeFaces(rst.mN, xpos, xneg, ypos, yneg, zpos, zneg, convertMipmapControl(mips), usage);
            TypeThunker tt = new TypeThunker(rs, a.getType());
            return new AllocationThunker(rs, tt, usage, a);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public static Allocation createFromBitmapResource(RenderScript rs, Resources res, int id, Allocation.MipmapControl mips, int usage) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        try {
            android.renderscript.Allocation a = android.renderscript.Allocation.createFromBitmapResource(rst.mN, res, id, convertMipmapControl(mips), usage);
            TypeThunker tt = new TypeThunker(rs, a.getType());
            return new AllocationThunker(rs, tt, usage, a);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public static Allocation createFromString(RenderScript rs, String str, int usage) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        try {
            android.renderscript.Allocation a = android.renderscript.Allocation.createFromString(rst.mN, str, usage);
            TypeThunker tt = new TypeThunker(rs, a.getType());
            return new AllocationThunker(rs, tt, usage, a);
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public static Allocation createSized(RenderScript rs, Element e, int count, int usage) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        try {
            android.renderscript.Allocation a = android.renderscript.Allocation.createSized(rst.mN, (android.renderscript.Element) e.getNObj(), count, usage);
            TypeThunker tt = new TypeThunker(rs, a.getType());
            return new AllocationThunker(rs, tt, usage, a);
        } catch (android.renderscript.RSRuntimeException exc) {
            throw ExceptionThunker.convertException(exc);
        }
    }
}
