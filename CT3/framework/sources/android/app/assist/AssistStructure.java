package android.app.assist;

import android.app.Activity;
import android.content.ComponentName;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.BadParcelableException;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PooledStringReader;
import android.os.PooledStringWriter;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewRootImpl;
import android.view.ViewStructure;
import android.view.WindowManagerGlobal;
import java.util.ArrayList;

public class AssistStructure implements Parcelable {
    public static final Parcelable.Creator<AssistStructure> CREATOR = new Parcelable.Creator<AssistStructure>() {
        @Override
        public AssistStructure createFromParcel(Parcel in) {
            return new AssistStructure(in);
        }

        @Override
        public AssistStructure[] newArray(int size) {
            return new AssistStructure[size];
        }
    };
    static final boolean DEBUG_PARCEL = false;
    static final boolean DEBUG_PARCEL_CHILDREN = false;
    static final boolean DEBUG_PARCEL_TREE = false;
    static final String DESCRIPTOR = "android.app.AssistStructure";
    static final String TAG = "AssistStructure";
    static final int TRANSACTION_XFER = 2;
    static final int VALIDATE_VIEW_TOKEN = 572662306;
    static final int VALIDATE_WINDOW_TOKEN = 286331153;
    ComponentName mActivityComponent;
    boolean mHaveData;
    final ArrayList<ViewNodeBuilder> mPendingAsyncChildren;
    IBinder mReceiveChannel;
    SendChannel mSendChannel;
    Rect mTmpRect;
    final ArrayList<WindowNode> mWindowNodes;

    static final class SendChannel extends Binder {
        volatile AssistStructure mAssistStructure;

        SendChannel(AssistStructure as) {
            this.mAssistStructure = as;
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == 2) {
                AssistStructure as = this.mAssistStructure;
                if (as == null) {
                    return true;
                }
                data.enforceInterface(AssistStructure.DESCRIPTOR);
                ?? strongBinder = data.readStrongBinder();
                if (strongBinder != 0) {
                    if (strongBinder instanceof ParcelTransferWriter) {
                        strongBinder.writeToParcel(as, reply);
                        return true;
                    }
                    Log.w(AssistStructure.TAG, "Caller supplied bad token type: " + ((Object) strongBinder));
                    return true;
                }
                ParcelTransferWriter xfer = new ParcelTransferWriter(as, reply);
                xfer.writeToParcel(as, reply);
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }
    }

    static final class ViewStackEntry {
        int curChild;
        ViewNode node;
        int numChildren;

        ViewStackEntry() {
        }
    }

    static final class ParcelTransferWriter extends Binder {
        ViewStackEntry mCurViewStackEntry;
        int mCurViewStackPos;
        int mCurWindow;
        int mNumWindows;
        int mNumWrittenViews;
        int mNumWrittenWindows;
        final boolean mWriteStructure;
        final ArrayList<ViewStackEntry> mViewStack = new ArrayList<>();
        final float[] mTmpMatrix = new float[9];

        ParcelTransferWriter(AssistStructure as, Parcel out) {
            this.mWriteStructure = as.waitForReady();
            ComponentName.writeToParcel(as.mActivityComponent, out);
            this.mNumWindows = as.mWindowNodes.size();
            if (this.mWriteStructure && this.mNumWindows > 0) {
                out.writeInt(this.mNumWindows);
            } else {
                out.writeInt(0);
            }
        }

        void writeToParcel(AssistStructure as, Parcel out) {
            int start = out.dataPosition();
            this.mNumWrittenWindows = 0;
            this.mNumWrittenViews = 0;
            boolean more = writeToParcelInner(as, out);
            Log.i(AssistStructure.TAG, "Flattened " + (more ? "partial" : "final") + " assist data: " + (out.dataPosition() - start) + " bytes, containing " + this.mNumWrittenWindows + " windows, " + this.mNumWrittenViews + " views");
        }

        boolean writeToParcelInner(AssistStructure as, Parcel out) {
            if (this.mNumWindows == 0) {
                return false;
            }
            PooledStringWriter pwriter = new PooledStringWriter(out);
            while (writeNextEntryToParcel(as, out, pwriter)) {
                if (out.dataSize() > 65536) {
                    out.writeInt(0);
                    out.writeStrongBinder(this);
                    pwriter.finish();
                    return true;
                }
            }
            pwriter.finish();
            this.mViewStack.clear();
            return false;
        }

        void pushViewStackEntry(ViewNode node, int pos) {
            ViewStackEntry entry;
            if (pos >= this.mViewStack.size()) {
                entry = new ViewStackEntry();
                this.mViewStack.add(entry);
            } else {
                entry = this.mViewStack.get(pos);
            }
            entry.node = node;
            entry.numChildren = node.getChildCount();
            entry.curChild = 0;
            this.mCurViewStackEntry = entry;
        }

        void writeView(ViewNode child, Parcel out, PooledStringWriter pwriter, int levelAdj) {
            out.writeInt(AssistStructure.VALIDATE_VIEW_TOKEN);
            int flags = child.writeSelfToParcel(out, pwriter, this.mTmpMatrix);
            this.mNumWrittenViews++;
            if ((1048576 & flags) == 0) {
                return;
            }
            out.writeInt(child.mChildren.length);
            int pos = this.mCurViewStackPos + 1;
            this.mCurViewStackPos = pos;
            pushViewStackEntry(child, pos);
        }

        boolean writeNextEntryToParcel(AssistStructure as, Parcel out, PooledStringWriter pwriter) {
            if (this.mCurViewStackEntry != null) {
                if (this.mCurViewStackEntry.curChild < this.mCurViewStackEntry.numChildren) {
                    ViewNode child = this.mCurViewStackEntry.node.mChildren[this.mCurViewStackEntry.curChild];
                    this.mCurViewStackEntry.curChild++;
                    writeView(child, out, pwriter, 1);
                    return true;
                }
                while (true) {
                    int pos = this.mCurViewStackPos - 1;
                    this.mCurViewStackPos = pos;
                    if (pos < 0) {
                        this.mCurViewStackEntry = null;
                        break;
                    }
                    this.mCurViewStackEntry = this.mViewStack.get(pos);
                    if (this.mCurViewStackEntry.curChild < this.mCurViewStackEntry.numChildren) {
                        break;
                    }
                }
                return true;
            }
            int pos2 = this.mCurWindow;
            if (pos2 >= this.mNumWindows) {
                return false;
            }
            WindowNode win = as.mWindowNodes.get(pos2);
            this.mCurWindow++;
            out.writeInt(AssistStructure.VALIDATE_WINDOW_TOKEN);
            win.writeSelfToParcel(out, pwriter, this.mTmpMatrix);
            this.mNumWrittenWindows++;
            ViewNode root = win.mRoot;
            this.mCurViewStackPos = 0;
            writeView(root, out, pwriter, 0);
            return true;
        }
    }

    final class ParcelTransferReader {
        private final IBinder mChannel;
        private Parcel mCurParcel;
        int mNumReadViews;
        int mNumReadWindows;
        PooledStringReader mStringReader;
        final float[] mTmpMatrix = new float[9];
        private IBinder mTransferToken;

        ParcelTransferReader(IBinder channel) {
            this.mChannel = channel;
        }

        void go() {
            fetchData();
            AssistStructure.this.mActivityComponent = ComponentName.readFromParcel(this.mCurParcel);
            int N = this.mCurParcel.readInt();
            if (N <= 0) {
                return;
            }
            this.mStringReader = new PooledStringReader(this.mCurParcel);
            for (int i = 0; i < N; i++) {
                AssistStructure.this.mWindowNodes.add(new WindowNode(this));
            }
        }

        Parcel readParcel(int validateToken, int level) {
            int token = this.mCurParcel.readInt();
            if (token != 0) {
                if (token != validateToken) {
                    throw new BadParcelableException("Got token " + Integer.toHexString(token) + ", expected token " + Integer.toHexString(validateToken));
                }
                return this.mCurParcel;
            }
            this.mTransferToken = this.mCurParcel.readStrongBinder();
            if (this.mTransferToken == null) {
                throw new IllegalStateException("Reached end of partial data without transfer token");
            }
            fetchData();
            this.mStringReader = new PooledStringReader(this.mCurParcel);
            this.mCurParcel.readInt();
            return this.mCurParcel;
        }

        private void fetchData() {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken(AssistStructure.DESCRIPTOR);
            data.writeStrongBinder(this.mTransferToken);
            if (this.mCurParcel != null) {
                this.mCurParcel.recycle();
            }
            this.mCurParcel = Parcel.obtain();
            try {
                this.mChannel.transact(2, data, this.mCurParcel, 0);
                data.recycle();
                this.mNumReadViews = 0;
                this.mNumReadWindows = 0;
            } catch (RemoteException e) {
                Log.w(AssistStructure.TAG, "Failure reading AssistStructure data", e);
                throw new IllegalStateException("Failure reading AssistStructure data: " + e);
            }
        }
    }

    static final class ViewNodeText {
        String mHint;
        int[] mLineBaselines;
        int[] mLineCharOffsets;
        CharSequence mText;
        int mTextBackgroundColor;
        int mTextColor;
        int mTextSelectionEnd;
        int mTextSelectionStart;
        float mTextSize;
        int mTextStyle;

        ViewNodeText() {
            this.mTextColor = 1;
            this.mTextBackgroundColor = 1;
        }

        boolean isSimple() {
            if (this.mTextBackgroundColor == 1 && this.mTextSelectionStart == 0 && this.mTextSelectionEnd == 0 && this.mLineCharOffsets == null && this.mLineBaselines == null) {
                return this.mHint == null;
            }
            return false;
        }

        ViewNodeText(Parcel in, boolean simple) {
            this.mTextColor = 1;
            this.mTextBackgroundColor = 1;
            this.mText = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            this.mTextSize = in.readFloat();
            this.mTextStyle = in.readInt();
            this.mTextColor = in.readInt();
            if (simple) {
                return;
            }
            this.mTextBackgroundColor = in.readInt();
            this.mTextSelectionStart = in.readInt();
            this.mTextSelectionEnd = in.readInt();
            this.mLineCharOffsets = in.createIntArray();
            this.mLineBaselines = in.createIntArray();
            this.mHint = in.readString();
        }

        void writeToParcel(Parcel out, boolean simple) {
            TextUtils.writeToParcel(this.mText, out, 0);
            out.writeFloat(this.mTextSize);
            out.writeInt(this.mTextStyle);
            out.writeInt(this.mTextColor);
            if (simple) {
                return;
            }
            out.writeInt(this.mTextBackgroundColor);
            out.writeInt(this.mTextSelectionStart);
            out.writeInt(this.mTextSelectionEnd);
            out.writeIntArray(this.mLineCharOffsets);
            out.writeIntArray(this.mLineBaselines);
            out.writeString(this.mHint);
        }
    }

    public static class WindowNode {
        final int mDisplayId;
        final int mHeight;
        final ViewNode mRoot;
        final CharSequence mTitle;
        final int mWidth;
        final int mX;
        final int mY;

        WindowNode(AssistStructure assist, ViewRootImpl root) {
            View view = root.getView();
            Rect rect = new Rect();
            view.getBoundsOnScreen(rect);
            this.mX = rect.left - view.getLeft();
            this.mY = rect.top - view.getTop();
            this.mWidth = rect.width();
            this.mHeight = rect.height();
            this.mTitle = root.getTitle();
            this.mDisplayId = root.getDisplayId();
            this.mRoot = new ViewNode();
            ViewNodeBuilder builder = new ViewNodeBuilder(assist, this.mRoot, false);
            if ((root.getWindowFlags() & 8192) != 0) {
                view.onProvideStructure(builder);
                builder.setAssistBlocked(true);
            } else {
                view.dispatchProvideStructure(builder);
            }
        }

        WindowNode(ParcelTransferReader reader) {
            Parcel in = reader.readParcel(AssistStructure.VALIDATE_WINDOW_TOKEN, 0);
            reader.mNumReadWindows++;
            this.mX = in.readInt();
            this.mY = in.readInt();
            this.mWidth = in.readInt();
            this.mHeight = in.readInt();
            this.mTitle = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            this.mDisplayId = in.readInt();
            this.mRoot = new ViewNode(reader, 0);
        }

        void writeSelfToParcel(Parcel out, PooledStringWriter pwriter, float[] tmpMatrix) {
            out.writeInt(this.mX);
            out.writeInt(this.mY);
            out.writeInt(this.mWidth);
            out.writeInt(this.mHeight);
            TextUtils.writeToParcel(this.mTitle, out, 0);
            out.writeInt(this.mDisplayId);
        }

        public int getLeft() {
            return this.mX;
        }

        public int getTop() {
            return this.mY;
        }

        public int getWidth() {
            return this.mWidth;
        }

        public int getHeight() {
            return this.mHeight;
        }

        public CharSequence getTitle() {
            return this.mTitle;
        }

        public int getDisplayId() {
            return this.mDisplayId;
        }

        public ViewNode getRootViewNode() {
            return this.mRoot;
        }
    }

    public static class ViewNode {
        static final int FLAGS_ACCESSIBILITY_FOCUSED = 4096;
        static final int FLAGS_ACTIVATED = 8192;
        static final int FLAGS_ALL_CONTROL = -1048576;
        static final int FLAGS_ASSIST_BLOCKED = 128;
        static final int FLAGS_CHECKABLE = 256;
        static final int FLAGS_CHECKED = 512;
        static final int FLAGS_CLICKABLE = 1024;
        static final int FLAGS_CONTEXT_CLICKABLE = 16384;
        static final int FLAGS_DISABLED = 1;
        static final int FLAGS_FOCUSABLE = 16;
        static final int FLAGS_FOCUSED = 32;
        static final int FLAGS_HAS_ALPHA = 536870912;
        static final int FLAGS_HAS_CHILDREN = 1048576;
        static final int FLAGS_HAS_COMPLEX_TEXT = 8388608;
        static final int FLAGS_HAS_CONTENT_DESCRIPTION = 33554432;
        static final int FLAGS_HAS_ELEVATION = 268435456;
        static final int FLAGS_HAS_EXTRAS = 4194304;
        static final int FLAGS_HAS_ID = 2097152;
        static final int FLAGS_HAS_LARGE_COORDS = 67108864;
        static final int FLAGS_HAS_MATRIX = 1073741824;
        static final int FLAGS_HAS_SCROLL = 134217728;
        static final int FLAGS_HAS_TEXT = 16777216;
        static final int FLAGS_LONG_CLICKABLE = 2048;
        static final int FLAGS_SELECTED = 64;
        static final int FLAGS_VISIBILITY_MASK = 12;
        public static final int TEXT_COLOR_UNDEFINED = 1;
        public static final int TEXT_STYLE_BOLD = 1;
        public static final int TEXT_STYLE_ITALIC = 2;
        public static final int TEXT_STYLE_STRIKE_THRU = 8;
        public static final int TEXT_STYLE_UNDERLINE = 4;
        float mAlpha;
        ViewNode[] mChildren;
        String mClassName;
        CharSequence mContentDescription;
        float mElevation;
        Bundle mExtras;
        int mFlags;
        int mHeight;
        int mId;
        String mIdEntry;
        String mIdPackage;
        String mIdType;
        Matrix mMatrix;
        int mScrollX;
        int mScrollY;
        ViewNodeText mText;
        int mWidth;
        int mX;
        int mY;

        ViewNode() {
            this.mId = -1;
            this.mAlpha = 1.0f;
        }

        ViewNode(ParcelTransferReader reader, int nestingLevel) {
            this.mId = -1;
            this.mAlpha = 1.0f;
            Parcel in = reader.readParcel(AssistStructure.VALIDATE_VIEW_TOKEN, nestingLevel);
            reader.mNumReadViews++;
            PooledStringReader preader = reader.mStringReader;
            this.mClassName = preader.readString();
            this.mFlags = in.readInt();
            int flags = this.mFlags;
            if ((2097152 & flags) != 0) {
                this.mId = in.readInt();
                if (this.mId != 0) {
                    this.mIdEntry = preader.readString();
                    if (this.mIdEntry != null) {
                        this.mIdType = preader.readString();
                        this.mIdPackage = preader.readString();
                    }
                }
            }
            if ((67108864 & flags) != 0) {
                this.mX = in.readInt();
                this.mY = in.readInt();
                this.mWidth = in.readInt();
                this.mHeight = in.readInt();
            } else {
                int val = in.readInt();
                this.mX = val & 32767;
                this.mY = (val >> 16) & 32767;
                int val2 = in.readInt();
                this.mWidth = val2 & 32767;
                this.mHeight = (val2 >> 16) & 32767;
            }
            if ((134217728 & flags) != 0) {
                this.mScrollX = in.readInt();
                this.mScrollY = in.readInt();
            }
            if ((1073741824 & flags) != 0) {
                this.mMatrix = new Matrix();
                in.readFloatArray(reader.mTmpMatrix);
                this.mMatrix.setValues(reader.mTmpMatrix);
            }
            if ((268435456 & flags) != 0) {
                this.mElevation = in.readFloat();
            }
            if ((536870912 & flags) != 0) {
                this.mAlpha = in.readFloat();
            }
            if ((33554432 & flags) != 0) {
                this.mContentDescription = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            }
            if ((16777216 & flags) != 0) {
                this.mText = new ViewNodeText(in, (8388608 & flags) == 0);
            }
            if ((4194304 & flags) != 0) {
                this.mExtras = in.readBundle();
            }
            if ((1048576 & flags) == 0) {
                return;
            }
            int NCHILDREN = in.readInt();
            this.mChildren = new ViewNode[NCHILDREN];
            for (int i = 0; i < NCHILDREN; i++) {
                this.mChildren[i] = new ViewNode(reader, nestingLevel + 1);
            }
        }

        int writeSelfToParcel(Parcel out, PooledStringWriter pwriter, float[] tmpMatrix) {
            int flags = this.mFlags & 1048575;
            if (this.mId != -1) {
                flags |= 2097152;
            }
            if ((this.mX & (-32768)) == 0 && (this.mY & (-32768)) == 0) {
                if (((this.mWidth & (-32768)) != 0) | ((this.mHeight & (-32768)) != 0)) {
                }
            } else {
                flags |= 67108864;
            }
            if (this.mScrollX != 0 || this.mScrollY != 0) {
                flags |= 134217728;
            }
            if (this.mMatrix != null) {
                flags |= 1073741824;
            }
            if (this.mElevation != 0.0f) {
                flags |= 268435456;
            }
            if (this.mAlpha != 1.0f) {
                flags |= 536870912;
            }
            if (this.mContentDescription != null) {
                flags |= 33554432;
            }
            if (this.mText != null) {
                flags |= 16777216;
                if (!this.mText.isSimple()) {
                    flags |= 8388608;
                }
            }
            if (this.mExtras != null) {
                flags |= 4194304;
            }
            if (this.mChildren != null) {
                flags |= 1048576;
            }
            pwriter.writeString(this.mClassName);
            out.writeInt(flags);
            if ((flags & 2097152) != 0) {
                out.writeInt(this.mId);
                if (this.mId != 0) {
                    pwriter.writeString(this.mIdEntry);
                    if (this.mIdEntry != null) {
                        pwriter.writeString(this.mIdType);
                        pwriter.writeString(this.mIdPackage);
                    }
                }
            }
            if ((67108864 & flags) != 0) {
                out.writeInt(this.mX);
                out.writeInt(this.mY);
                out.writeInt(this.mWidth);
                out.writeInt(this.mHeight);
            } else {
                out.writeInt((this.mY << 16) | this.mX);
                out.writeInt((this.mHeight << 16) | this.mWidth);
            }
            if ((134217728 & flags) != 0) {
                out.writeInt(this.mScrollX);
                out.writeInt(this.mScrollY);
            }
            if ((1073741824 & flags) != 0) {
                this.mMatrix.getValues(tmpMatrix);
                out.writeFloatArray(tmpMatrix);
            }
            if ((268435456 & flags) != 0) {
                out.writeFloat(this.mElevation);
            }
            if ((536870912 & flags) != 0) {
                out.writeFloat(this.mAlpha);
            }
            if ((33554432 & flags) != 0) {
                TextUtils.writeToParcel(this.mContentDescription, out, 0);
            }
            if ((16777216 & flags) != 0) {
                this.mText.writeToParcel(out, (8388608 & flags) == 0);
            }
            if ((flags & 4194304) != 0) {
                out.writeBundle(this.mExtras);
            }
            return flags;
        }

        public int getId() {
            return this.mId;
        }

        public String getIdPackage() {
            return this.mIdPackage;
        }

        public String getIdType() {
            return this.mIdType;
        }

        public String getIdEntry() {
            return this.mIdEntry;
        }

        public int getLeft() {
            return this.mX;
        }

        public int getTop() {
            return this.mY;
        }

        public int getScrollX() {
            return this.mScrollX;
        }

        public int getScrollY() {
            return this.mScrollY;
        }

        public int getWidth() {
            return this.mWidth;
        }

        public int getHeight() {
            return this.mHeight;
        }

        public Matrix getTransformation() {
            return this.mMatrix;
        }

        public float getElevation() {
            return this.mElevation;
        }

        public float getAlpha() {
            return this.mAlpha;
        }

        public int getVisibility() {
            return this.mFlags & 12;
        }

        public boolean isAssistBlocked() {
            return (this.mFlags & 128) != 0;
        }

        public boolean isEnabled() {
            return (this.mFlags & 1) == 0;
        }

        public boolean isClickable() {
            return (this.mFlags & 1024) != 0;
        }

        public boolean isFocusable() {
            return (this.mFlags & 16) != 0;
        }

        public boolean isFocused() {
            return (this.mFlags & 32) != 0;
        }

        public boolean isAccessibilityFocused() {
            return (this.mFlags & 4096) != 0;
        }

        public boolean isCheckable() {
            return (this.mFlags & 256) != 0;
        }

        public boolean isChecked() {
            return (this.mFlags & 512) != 0;
        }

        public boolean isSelected() {
            return (this.mFlags & 64) != 0;
        }

        public boolean isActivated() {
            return (this.mFlags & 8192) != 0;
        }

        public boolean isLongClickable() {
            return (this.mFlags & 2048) != 0;
        }

        public boolean isContextClickable() {
            return (this.mFlags & 16384) != 0;
        }

        public String getClassName() {
            return this.mClassName;
        }

        public CharSequence getContentDescription() {
            return this.mContentDescription;
        }

        public CharSequence getText() {
            if (this.mText != null) {
                return this.mText.mText;
            }
            return null;
        }

        public int getTextSelectionStart() {
            if (this.mText != null) {
                return this.mText.mTextSelectionStart;
            }
            return -1;
        }

        public int getTextSelectionEnd() {
            if (this.mText != null) {
                return this.mText.mTextSelectionEnd;
            }
            return -1;
        }

        public int getTextColor() {
            if (this.mText != null) {
                return this.mText.mTextColor;
            }
            return 1;
        }

        public int getTextBackgroundColor() {
            if (this.mText != null) {
                return this.mText.mTextBackgroundColor;
            }
            return 1;
        }

        public float getTextSize() {
            if (this.mText != null) {
                return this.mText.mTextSize;
            }
            return 0.0f;
        }

        public int getTextStyle() {
            if (this.mText != null) {
                return this.mText.mTextStyle;
            }
            return 0;
        }

        public int[] getTextLineCharOffsets() {
            if (this.mText != null) {
                return this.mText.mLineCharOffsets;
            }
            return null;
        }

        public int[] getTextLineBaselines() {
            if (this.mText != null) {
                return this.mText.mLineBaselines;
            }
            return null;
        }

        public String getHint() {
            if (this.mText != null) {
                return this.mText.mHint;
            }
            return null;
        }

        public Bundle getExtras() {
            return this.mExtras;
        }

        public int getChildCount() {
            if (this.mChildren != null) {
                return this.mChildren.length;
            }
            return 0;
        }

        public ViewNode getChildAt(int index) {
            return this.mChildren[index];
        }
    }

    static class ViewNodeBuilder extends ViewStructure {
        final AssistStructure mAssist;
        final boolean mAsync;
        final ViewNode mNode;

        ViewNodeBuilder(AssistStructure assist, ViewNode node, boolean async) {
            this.mAssist = assist;
            this.mNode = node;
            this.mAsync = async;
        }

        @Override
        public void setId(int id, String packageName, String typeName, String entryName) {
            this.mNode.mId = id;
            this.mNode.mIdPackage = packageName;
            this.mNode.mIdType = typeName;
            this.mNode.mIdEntry = entryName;
        }

        @Override
        public void setDimens(int left, int top, int scrollX, int scrollY, int width, int height) {
            this.mNode.mX = left;
            this.mNode.mY = top;
            this.mNode.mScrollX = scrollX;
            this.mNode.mScrollY = scrollY;
            this.mNode.mWidth = width;
            this.mNode.mHeight = height;
        }

        @Override
        public void setTransformation(Matrix matrix) {
            if (matrix == null) {
                this.mNode.mMatrix = null;
            } else {
                this.mNode.mMatrix = new Matrix(matrix);
            }
        }

        @Override
        public void setElevation(float elevation) {
            this.mNode.mElevation = elevation;
        }

        @Override
        public void setAlpha(float alpha) {
            this.mNode.mAlpha = alpha;
        }

        @Override
        public void setVisibility(int visibility) {
            this.mNode.mFlags = (this.mNode.mFlags & (-13)) | visibility;
        }

        public void setAssistBlocked(boolean state) {
            this.mNode.mFlags = (state ? 128 : 0) | (this.mNode.mFlags & (-129));
        }

        @Override
        public void setEnabled(boolean state) {
            this.mNode.mFlags = (state ? 0 : 1) | (this.mNode.mFlags & (-2));
        }

        @Override
        public void setClickable(boolean state) {
            this.mNode.mFlags = (state ? 1024 : 0) | (this.mNode.mFlags & (-1025));
        }

        @Override
        public void setLongClickable(boolean state) {
            this.mNode.mFlags = (state ? 2048 : 0) | (this.mNode.mFlags & (-2049));
        }

        @Override
        public void setContextClickable(boolean state) {
            this.mNode.mFlags = (state ? 16384 : 0) | (this.mNode.mFlags & (-16385));
        }

        @Override
        public void setFocusable(boolean state) {
            this.mNode.mFlags = (state ? 16 : 0) | (this.mNode.mFlags & (-17));
        }

        @Override
        public void setFocused(boolean state) {
            this.mNode.mFlags = (state ? 32 : 0) | (this.mNode.mFlags & (-33));
        }

        @Override
        public void setAccessibilityFocused(boolean state) {
            this.mNode.mFlags = (state ? 4096 : 0) | (this.mNode.mFlags & (-4097));
        }

        @Override
        public void setCheckable(boolean state) {
            this.mNode.mFlags = (state ? 256 : 0) | (this.mNode.mFlags & (-257));
        }

        @Override
        public void setChecked(boolean state) {
            this.mNode.mFlags = (state ? 512 : 0) | (this.mNode.mFlags & (-513));
        }

        @Override
        public void setSelected(boolean state) {
            this.mNode.mFlags = (state ? 64 : 0) | (this.mNode.mFlags & (-65));
        }

        @Override
        public void setActivated(boolean state) {
            this.mNode.mFlags = (state ? 8192 : 0) | (this.mNode.mFlags & (-8193));
        }

        @Override
        public void setClassName(String className) {
            this.mNode.mClassName = className;
        }

        @Override
        public void setContentDescription(CharSequence contentDescription) {
            this.mNode.mContentDescription = contentDescription;
        }

        private final ViewNodeText getNodeText() {
            if (this.mNode.mText != null) {
                return this.mNode.mText;
            }
            this.mNode.mText = new ViewNodeText();
            return this.mNode.mText;
        }

        @Override
        public void setText(CharSequence text) {
            ViewNodeText t = getNodeText();
            t.mText = text;
            t.mTextSelectionEnd = -1;
            t.mTextSelectionStart = -1;
        }

        @Override
        public void setText(CharSequence text, int selectionStart, int selectionEnd) {
            ViewNodeText t = getNodeText();
            t.mText = text;
            t.mTextSelectionStart = selectionStart;
            t.mTextSelectionEnd = selectionEnd;
        }

        @Override
        public void setTextStyle(float size, int fgColor, int bgColor, int style) {
            ViewNodeText t = getNodeText();
            t.mTextColor = fgColor;
            t.mTextBackgroundColor = bgColor;
            t.mTextSize = size;
            t.mTextStyle = style;
        }

        @Override
        public void setTextLines(int[] charOffsets, int[] baselines) {
            ViewNodeText t = getNodeText();
            t.mLineCharOffsets = charOffsets;
            t.mLineBaselines = baselines;
        }

        @Override
        public void setHint(CharSequence hint) {
            getNodeText().mHint = hint != null ? hint.toString() : null;
        }

        @Override
        public CharSequence getText() {
            if (this.mNode.mText != null) {
                return this.mNode.mText.mText;
            }
            return null;
        }

        @Override
        public int getTextSelectionStart() {
            if (this.mNode.mText != null) {
                return this.mNode.mText.mTextSelectionStart;
            }
            return -1;
        }

        @Override
        public int getTextSelectionEnd() {
            if (this.mNode.mText != null) {
                return this.mNode.mText.mTextSelectionEnd;
            }
            return -1;
        }

        @Override
        public CharSequence getHint() {
            if (this.mNode.mText != null) {
                return this.mNode.mText.mHint;
            }
            return null;
        }

        @Override
        public Bundle getExtras() {
            if (this.mNode.mExtras != null) {
                return this.mNode.mExtras;
            }
            this.mNode.mExtras = new Bundle();
            return this.mNode.mExtras;
        }

        @Override
        public boolean hasExtras() {
            return this.mNode.mExtras != null;
        }

        @Override
        public void setChildCount(int num) {
            this.mNode.mChildren = new ViewNode[num];
        }

        @Override
        public int addChildCount(int num) {
            if (this.mNode.mChildren == null) {
                setChildCount(num);
                return 0;
            }
            int start = this.mNode.mChildren.length;
            ViewNode[] newArray = new ViewNode[start + num];
            System.arraycopy(this.mNode.mChildren, 0, newArray, 0, start);
            this.mNode.mChildren = newArray;
            return start;
        }

        @Override
        public int getChildCount() {
            if (this.mNode.mChildren != null) {
                return this.mNode.mChildren.length;
            }
            return 0;
        }

        @Override
        public ViewStructure newChild(int index) {
            ViewNode node = new ViewNode();
            this.mNode.mChildren[index] = node;
            return new ViewNodeBuilder(this.mAssist, node, false);
        }

        @Override
        public ViewStructure asyncNewChild(int index) {
            ViewNodeBuilder builder;
            synchronized (this.mAssist) {
                ViewNode node = new ViewNode();
                this.mNode.mChildren[index] = node;
                builder = new ViewNodeBuilder(this.mAssist, node, true);
                this.mAssist.mPendingAsyncChildren.add(builder);
            }
            return builder;
        }

        @Override
        public void asyncCommit() {
            synchronized (this.mAssist) {
                if (!this.mAsync) {
                    throw new IllegalStateException("Child " + this + " was not created with ViewStructure.asyncNewChild");
                }
                if (!this.mAssist.mPendingAsyncChildren.remove(this)) {
                    throw new IllegalStateException("Child " + this + " already committed");
                }
                this.mAssist.notifyAll();
            }
        }

        public Rect getTempRect() {
            return this.mAssist.mTmpRect;
        }
    }

    public AssistStructure(Activity activity) {
        this.mWindowNodes = new ArrayList<>();
        this.mPendingAsyncChildren = new ArrayList<>();
        this.mTmpRect = new Rect();
        this.mHaveData = true;
        this.mActivityComponent = activity.getComponentName();
        ArrayList<ViewRootImpl> views = WindowManagerGlobal.getInstance().getRootViews(activity.getActivityToken());
        for (int i = 0; i < views.size(); i++) {
            ViewRootImpl root = views.get(i);
            this.mWindowNodes.add(new WindowNode(this, root));
        }
    }

    public AssistStructure() {
        this.mWindowNodes = new ArrayList<>();
        this.mPendingAsyncChildren = new ArrayList<>();
        this.mTmpRect = new Rect();
        this.mHaveData = true;
        this.mActivityComponent = null;
    }

    public AssistStructure(Parcel in) {
        this.mWindowNodes = new ArrayList<>();
        this.mPendingAsyncChildren = new ArrayList<>();
        this.mTmpRect = new Rect();
        this.mReceiveChannel = in.readStrongBinder();
    }

    public void dump() {
        Log.i(TAG, "Activity: " + this.mActivityComponent.flattenToShortString());
        int N = getWindowNodeCount();
        for (int i = 0; i < N; i++) {
            WindowNode node = getWindowNodeAt(i);
            Log.i(TAG, "Window #" + i + " [" + node.getLeft() + "," + node.getTop() + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + node.getWidth() + "x" + node.getHeight() + "]" + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + node.getTitle());
            dump("  ", node.getRootViewNode());
        }
    }

    void dump(String prefix, ViewNode node) {
        Log.i(TAG, prefix + "View [" + node.getLeft() + "," + node.getTop() + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + node.getWidth() + "x" + node.getHeight() + "]" + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + node.getClassName());
        int id = node.getId();
        if (id != 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(prefix);
            sb.append("  ID: #");
            sb.append(Integer.toHexString(id));
            String entry = node.getIdEntry();
            if (entry != null) {
                String type = node.getIdType();
                String pkg = node.getIdPackage();
                sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                sb.append(pkg);
                sb.append(":");
                sb.append(type);
                sb.append("/");
                sb.append(entry);
            }
            Log.i(TAG, sb.toString());
        }
        int scrollX = node.getScrollX();
        int scrollY = node.getScrollY();
        if (scrollX != 0 || scrollY != 0) {
            Log.i(TAG, prefix + "  Scroll: " + scrollX + "," + scrollY);
        }
        Matrix matrix = node.getTransformation();
        if (matrix != null) {
            Log.i(TAG, prefix + "  Transformation: " + matrix);
        }
        float elevation = node.getElevation();
        if (elevation != 0.0f) {
            Log.i(TAG, prefix + "  Elevation: " + elevation);
        }
        float alpha = node.getAlpha();
        if (alpha != 0.0f) {
            Log.i(TAG, prefix + "  Alpha: " + elevation);
        }
        CharSequence contentDescription = node.getContentDescription();
        if (contentDescription != null) {
            Log.i(TAG, prefix + "  Content description: " + contentDescription);
        }
        CharSequence text = node.getText();
        if (text != null) {
            Log.i(TAG, prefix + "  Text (sel " + node.getTextSelectionStart() + ContactsContract.Aas.ENCODE_SYMBOL + node.getTextSelectionEnd() + "): " + text);
            Log.i(TAG, prefix + "  Text size: " + node.getTextSize() + " , style: #" + node.getTextStyle());
            Log.i(TAG, prefix + "  Text color fg: #" + Integer.toHexString(node.getTextColor()) + ", bg: #" + Integer.toHexString(node.getTextBackgroundColor()));
        }
        String hint = node.getHint();
        if (hint != null) {
            Log.i(TAG, prefix + "  Hint: " + hint);
        }
        Bundle extras = node.getExtras();
        if (extras != null) {
            Log.i(TAG, prefix + "  Extras: " + extras);
        }
        if (node.isAssistBlocked()) {
            Log.i(TAG, prefix + "  BLOCKED");
        }
        int NCHILDREN = node.getChildCount();
        if (NCHILDREN > 0) {
            Log.i(TAG, prefix + "  Children:");
            String cprefix = prefix + "    ";
            for (int i = 0; i < NCHILDREN; i++) {
                ViewNode cnode = node.getChildAt(i);
                dump(cprefix, cnode);
            }
        }
    }

    public ComponentName getActivityComponent() {
        ensureData();
        return this.mActivityComponent;
    }

    public int getWindowNodeCount() {
        ensureData();
        return this.mWindowNodes.size();
    }

    public WindowNode getWindowNodeAt(int index) {
        ensureData();
        return this.mWindowNodes.get(index);
    }

    public void ensureData() {
        if (this.mHaveData) {
            return;
        }
        this.mHaveData = true;
        ParcelTransferReader reader = new ParcelTransferReader(this.mReceiveChannel);
        reader.go();
    }

    boolean waitForReady() {
        boolean skipStructure = false;
        synchronized (this) {
            long endTime = SystemClock.uptimeMillis() + 5000;
            while (this.mPendingAsyncChildren.size() > 0) {
                long now = SystemClock.uptimeMillis();
                if (now >= endTime) {
                    break;
                }
                try {
                    wait(endTime - now);
                } catch (InterruptedException e) {
                }
            }
            if (this.mPendingAsyncChildren.size() > 0) {
                Log.w(TAG, "Skipping assist structure, waiting too long for async children (have " + this.mPendingAsyncChildren.size() + " remaining");
                skipStructure = true;
            }
        }
        return !skipStructure;
    }

    public void clearSendChannel() {
        if (this.mSendChannel == null) {
            return;
        }
        this.mSendChannel.mAssistStructure = null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        if (this.mHaveData) {
            if (this.mSendChannel == null) {
                this.mSendChannel = new SendChannel(this);
            }
            out.writeStrongBinder(this.mSendChannel);
            return;
        }
        out.writeStrongBinder(this.mReceiveChannel);
    }
}
