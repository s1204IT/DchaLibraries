package android.content;

public class UndoOwner {
    Object mData;
    UndoManager mManager;
    int mOpCount;
    int mSavedIdx;
    int mStateSeq;
    final String mTag;

    UndoOwner(String tag) {
        this.mTag = tag;
    }

    public String getTag() {
        return this.mTag;
    }

    public Object getData() {
        return this.mData;
    }
}
