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

        public final int getItemAlignmentFocusViewId() {
            return this.mFocusViewId != -1 ? this.mFocusViewId : this.mViewId;
        }

        public boolean isAlignedToTextViewBaseLine() {
            return this.mAlignToBaseline;
        }
    }

    public ItemAlignmentDef[] getAlignmentDefs() {
        return this.mAlignmentDefs;
    }
}
