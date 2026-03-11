package android.support.v7.widget;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;

public class StaggeredGridLayoutManager$SavedState implements Parcelable {
    public static final Parcelable.Creator<StaggeredGridLayoutManager$SavedState> CREATOR = new Parcelable.Creator<StaggeredGridLayoutManager$SavedState>() {
        @Override
        public StaggeredGridLayoutManager$SavedState createFromParcel(Parcel in) {
            return new StaggeredGridLayoutManager$SavedState(in);
        }

        @Override
        public StaggeredGridLayoutManager$SavedState[] newArray(int size) {
            return new StaggeredGridLayoutManager$SavedState[size];
        }
    };
    boolean mAnchorLayoutFromEnd;
    int mAnchorPosition;
    List<StaggeredGridLayoutManager$LazySpanLookup$FullSpanItem> mFullSpanItems;
    boolean mLastLayoutRTL;
    boolean mReverseLayout;
    int[] mSpanLookup;
    int mSpanLookupSize;
    int[] mSpanOffsets;
    int mSpanOffsetsSize;
    int mVisibleAnchorPosition;

    public StaggeredGridLayoutManager$SavedState() {
    }

    StaggeredGridLayoutManager$SavedState(Parcel in) {
        this.mAnchorPosition = in.readInt();
        this.mVisibleAnchorPosition = in.readInt();
        this.mSpanOffsetsSize = in.readInt();
        if (this.mSpanOffsetsSize > 0) {
            this.mSpanOffsets = new int[this.mSpanOffsetsSize];
            in.readIntArray(this.mSpanOffsets);
        }
        this.mSpanLookupSize = in.readInt();
        if (this.mSpanLookupSize > 0) {
            this.mSpanLookup = new int[this.mSpanLookupSize];
            in.readIntArray(this.mSpanLookup);
        }
        this.mReverseLayout = in.readInt() == 1;
        this.mAnchorLayoutFromEnd = in.readInt() == 1;
        this.mLastLayoutRTL = in.readInt() == 1;
        this.mFullSpanItems = in.readArrayList(StaggeredGridLayoutManager$LazySpanLookup$FullSpanItem.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mAnchorPosition);
        dest.writeInt(this.mVisibleAnchorPosition);
        dest.writeInt(this.mSpanOffsetsSize);
        if (this.mSpanOffsetsSize > 0) {
            dest.writeIntArray(this.mSpanOffsets);
        }
        dest.writeInt(this.mSpanLookupSize);
        if (this.mSpanLookupSize > 0) {
            dest.writeIntArray(this.mSpanLookup);
        }
        dest.writeInt(this.mReverseLayout ? 1 : 0);
        dest.writeInt(this.mAnchorLayoutFromEnd ? 1 : 0);
        dest.writeInt(this.mLastLayoutRTL ? 1 : 0);
        dest.writeList(this.mFullSpanItems);
    }
}
