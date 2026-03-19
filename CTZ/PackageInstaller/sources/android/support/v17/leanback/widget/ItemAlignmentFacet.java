package android.support.v17.leanback.widget;

public final class ItemAlignmentFacet {
    private ItemAlignmentDef[] mAlignmentDefs = {new ItemAlignmentDef()};

    public static class ItemAlignmentDef {
        private boolean mAlignToBaseline;
        int mViewId = -1;
        int mFocusViewId = -1;
        int mOffset = 0;
        float mOffsetPercent = 50.0f;
        boolean mOffsetWithPadding = false;

        public final void setItemAlignmentOffset(int offset) {
            this.mOffset = offset;
        }

        public final void setItemAlignmentOffsetWithPadding(boolean withPadding) {
            this.mOffsetWithPadding = withPadding;
        }

        public final void setItemAlignmentOffsetPercent(float percent) {
            if ((percent < 0.0f || percent > 100.0f) && percent != -1.0f) {
                throw new IllegalArgumentException();
            }
            this.mOffsetPercent = percent;
        }

        public final void setItemAlignmentViewId(int viewId) {
            this.mViewId = viewId;
        }

        public final int getItemAlignmentFocusViewId() {
            return this.mFocusViewId != -1 ? this.mFocusViewId : this.mViewId;
        }

        public final void setAlignedToTextViewBaseline(boolean alignToBaseline) {
            this.mAlignToBaseline = alignToBaseline;
        }

        public boolean isAlignedToTextViewBaseLine() {
            return this.mAlignToBaseline;
        }
    }

    public void setAlignmentDefs(ItemAlignmentDef[] defs) {
        if (defs == null || defs.length < 1) {
            throw new IllegalArgumentException();
        }
        this.mAlignmentDefs = defs;
    }

    public ItemAlignmentDef[] getAlignmentDefs() {
        return this.mAlignmentDefs;
    }
}
