package android.renderscript;

import android.renderscript.Type;

public class AllocationAdapter extends Allocation {
    AllocationAdapter(long id, RenderScript rs, Allocation alloc) {
        super(id, rs, alloc.mType, alloc.mUsage);
        this.mAdaptedAllocation = alloc;
    }

    @Override
    long getID(RenderScript rs) {
        throw new RSInvalidStateException("This operation is not supported with adapters at this time.");
    }

    public void subData(int xoff, FieldPacker fp) {
        super.setFromFieldPacker(xoff, fp);
    }

    public void subElementData(int xoff, int component_number, FieldPacker fp) {
        super.setFromFieldPacker(xoff, component_number, fp);
    }

    public void subData1D(int off, int count, int[] d) {
        super.copy1DRangeFrom(off, count, d);
    }

    public void subData1D(int off, int count, short[] d) {
        super.copy1DRangeFrom(off, count, d);
    }

    public void subData1D(int off, int count, byte[] d) {
        super.copy1DRangeFrom(off, count, d);
    }

    public void subData1D(int off, int count, float[] d) {
        super.copy1DRangeFrom(off, count, d);
    }

    public void subData2D(int xoff, int yoff, int w, int h, int[] d) {
        super.copy2DRangeFrom(xoff, yoff, w, h, d);
    }

    public void subData2D(int xoff, int yoff, int w, int h, float[] d) {
        super.copy2DRangeFrom(xoff, yoff, w, h, d);
    }

    public void readData(int[] d) {
        super.copyTo(d);
    }

    public void readData(float[] d) {
        super.copyTo(d);
    }

    void initLOD(int lod) {
        if (lod < 0) {
            throw new RSIllegalArgumentException("Attempting to set negative lod (" + lod + ").");
        }
        int tx = this.mAdaptedAllocation.mType.getX();
        int ty = this.mAdaptedAllocation.mType.getY();
        int tz = this.mAdaptedAllocation.mType.getZ();
        for (int ct = 0; ct < lod; ct++) {
            if (tx == 1 && ty == 1 && tz == 1) {
                throw new RSIllegalArgumentException("Attempting to set lod (" + lod + ") out of range.");
            }
            if (tx > 1) {
                tx >>= 1;
            }
            if (ty > 1) {
                ty >>= 1;
            }
            if (tz > 1) {
                tz >>= 1;
            }
        }
        this.mCurrentDimX = tx;
        this.mCurrentDimY = ty;
        this.mCurrentDimZ = tz;
        this.mCurrentCount = this.mCurrentDimX;
        if (this.mCurrentDimY > 1) {
            this.mCurrentCount *= this.mCurrentDimY;
        }
        if (this.mCurrentDimZ > 1) {
            this.mCurrentCount *= this.mCurrentDimZ;
        }
        this.mSelectedY = 0;
        this.mSelectedZ = 0;
    }

    public void setLOD(int lod) {
        if (!this.mAdaptedAllocation.getType().hasMipmaps()) {
            throw new RSInvalidStateException("Cannot set LOD when the allocation type does not include mipmaps.");
        }
        if (!this.mConstrainedLOD) {
            throw new RSInvalidStateException("Cannot set LOD when the adapter includes mipmaps.");
        }
        initLOD(lod);
    }

    public void setFace(Type.CubemapFace cf) {
        if (!this.mAdaptedAllocation.getType().hasFaces()) {
            throw new RSInvalidStateException("Cannot set Face when the allocation type does not include faces.");
        }
        if (!this.mConstrainedFace) {
            throw new RSInvalidStateException("Cannot set LOD when the adapter includes mipmaps.");
        }
        if (cf == null) {
            throw new RSIllegalArgumentException("Cannot set null face.");
        }
        this.mSelectedFace = cf;
    }

    public void setY(int y) {
        if (this.mAdaptedAllocation.getType().getY() == 0) {
            throw new RSInvalidStateException("Cannot set Y when the allocation type does not include Y dim.");
        }
        if (this.mAdaptedAllocation.getType().getY() <= y) {
            throw new RSInvalidStateException("Cannot set Y greater than dimension of allocation.");
        }
        if (!this.mConstrainedY) {
            throw new RSInvalidStateException("Cannot set Y when the adapter includes Y.");
        }
        this.mSelectedY = y;
    }

    public void setZ(int z) {
        if (this.mAdaptedAllocation.getType().getZ() == 0) {
            throw new RSInvalidStateException("Cannot set Z when the allocation type does not include Z dim.");
        }
        if (this.mAdaptedAllocation.getType().getZ() <= z) {
            throw new RSInvalidStateException("Cannot set Z greater than dimension of allocation.");
        }
        if (!this.mConstrainedZ) {
            throw new RSInvalidStateException("Cannot set Z when the adapter includes Z.");
        }
        this.mSelectedZ = z;
    }

    public static AllocationAdapter create1D(RenderScript rs, Allocation a) {
        rs.validate();
        AllocationAdapter aa = new AllocationAdapter(0L, rs, a);
        aa.mConstrainedLOD = true;
        aa.mConstrainedFace = true;
        aa.mConstrainedY = true;
        aa.mConstrainedZ = true;
        aa.initLOD(0);
        return aa;
    }

    public static AllocationAdapter create2D(RenderScript rs, Allocation a) {
        rs.validate();
        AllocationAdapter aa = new AllocationAdapter(0L, rs, a);
        aa.mConstrainedLOD = true;
        aa.mConstrainedFace = true;
        aa.mConstrainedY = false;
        aa.mConstrainedZ = true;
        aa.initLOD(0);
        return aa;
    }

    @Override
    public synchronized void resize(int dimX) {
        throw new RSInvalidStateException("Resize not allowed for Adapters.");
    }
}
