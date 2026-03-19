package android.view;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.Region;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ViewTreeObserver {
    private static boolean sIllegalOnDrawModificationIsFatal;
    private boolean mAlive = true;
    private boolean mInDispatchOnDraw;
    private CopyOnWriteArray<OnComputeInternalInsetsListener> mOnComputeInternalInsetsListeners;
    private ArrayList<OnDrawListener> mOnDrawListeners;
    private CopyOnWriteArrayList<OnEnterAnimationCompleteListener> mOnEnterAnimationCompleteListeners;
    private CopyOnWriteArrayList<OnGlobalFocusChangeListener> mOnGlobalFocusListeners;
    private CopyOnWriteArray<OnGlobalLayoutListener> mOnGlobalLayoutListeners;
    private CopyOnWriteArray<OnPreDrawListener> mOnPreDrawListeners;
    private CopyOnWriteArray<OnScrollChangedListener> mOnScrollChangedListeners;
    private CopyOnWriteArrayList<OnTouchModeChangeListener> mOnTouchModeChangeListeners;
    private CopyOnWriteArrayList<OnWindowAttachListener> mOnWindowAttachListeners;
    private CopyOnWriteArrayList<OnWindowFocusChangeListener> mOnWindowFocusListeners;
    private CopyOnWriteArray<OnWindowShownListener> mOnWindowShownListeners;
    private boolean mWindowShown;

    public interface OnComputeInternalInsetsListener {
        void onComputeInternalInsets(InternalInsetsInfo internalInsetsInfo);
    }

    public interface OnDrawListener {
        void onDraw();
    }

    public interface OnEnterAnimationCompleteListener {
        void onEnterAnimationComplete();
    }

    public interface OnGlobalFocusChangeListener {
        void onGlobalFocusChanged(View view, View view2);
    }

    public interface OnGlobalLayoutListener {
        void onGlobalLayout();
    }

    public interface OnPreDrawListener {
        boolean onPreDraw();
    }

    public interface OnScrollChangedListener {
        void onScrollChanged();
    }

    public interface OnTouchModeChangeListener {
        void onTouchModeChanged(boolean z);
    }

    public interface OnWindowAttachListener {
        void onWindowAttached();

        void onWindowDetached();
    }

    public interface OnWindowFocusChangeListener {
        void onWindowFocusChanged(boolean z);
    }

    public interface OnWindowShownListener {
        void onWindowShown();
    }

    public static final class InternalInsetsInfo {
        public static final int TOUCHABLE_INSETS_CONTENT = 1;
        public static final int TOUCHABLE_INSETS_FRAME = 0;
        public static final int TOUCHABLE_INSETS_REGION = 3;
        public static final int TOUCHABLE_INSETS_VISIBLE = 2;
        int mTouchableInsets;
        public final Rect contentInsets = new Rect();
        public final Rect visibleInsets = new Rect();
        public final Region touchableRegion = new Region();

        public void setTouchableInsets(int i) {
            this.mTouchableInsets = i;
        }

        void reset() {
            this.contentInsets.setEmpty();
            this.visibleInsets.setEmpty();
            this.touchableRegion.setEmpty();
            this.mTouchableInsets = 0;
        }

        boolean isEmpty() {
            return this.contentInsets.isEmpty() && this.visibleInsets.isEmpty() && this.touchableRegion.isEmpty() && this.mTouchableInsets == 0;
        }

        public int hashCode() {
            return (31 * ((((this.contentInsets.hashCode() * 31) + this.visibleInsets.hashCode()) * 31) + this.touchableRegion.hashCode())) + this.mTouchableInsets;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            InternalInsetsInfo internalInsetsInfo = (InternalInsetsInfo) obj;
            if (this.mTouchableInsets == internalInsetsInfo.mTouchableInsets && this.contentInsets.equals(internalInsetsInfo.contentInsets) && this.visibleInsets.equals(internalInsetsInfo.visibleInsets) && this.touchableRegion.equals(internalInsetsInfo.touchableRegion)) {
                return true;
            }
            return false;
        }

        void set(InternalInsetsInfo internalInsetsInfo) {
            this.contentInsets.set(internalInsetsInfo.contentInsets);
            this.visibleInsets.set(internalInsetsInfo.visibleInsets);
            this.touchableRegion.set(internalInsetsInfo.touchableRegion);
            this.mTouchableInsets = internalInsetsInfo.mTouchableInsets;
        }
    }

    ViewTreeObserver(Context context) {
        sIllegalOnDrawModificationIsFatal = context.getApplicationInfo().targetSdkVersion >= 26;
    }

    void merge(ViewTreeObserver viewTreeObserver) {
        if (viewTreeObserver.mOnWindowAttachListeners != null) {
            if (this.mOnWindowAttachListeners != null) {
                this.mOnWindowAttachListeners.addAll(viewTreeObserver.mOnWindowAttachListeners);
            } else {
                this.mOnWindowAttachListeners = viewTreeObserver.mOnWindowAttachListeners;
            }
        }
        if (viewTreeObserver.mOnWindowFocusListeners != null) {
            if (this.mOnWindowFocusListeners != null) {
                this.mOnWindowFocusListeners.addAll(viewTreeObserver.mOnWindowFocusListeners);
            } else {
                this.mOnWindowFocusListeners = viewTreeObserver.mOnWindowFocusListeners;
            }
        }
        if (viewTreeObserver.mOnGlobalFocusListeners != null) {
            if (this.mOnGlobalFocusListeners != null) {
                this.mOnGlobalFocusListeners.addAll(viewTreeObserver.mOnGlobalFocusListeners);
            } else {
                this.mOnGlobalFocusListeners = viewTreeObserver.mOnGlobalFocusListeners;
            }
        }
        if (viewTreeObserver.mOnGlobalLayoutListeners != null) {
            if (this.mOnGlobalLayoutListeners != null) {
                this.mOnGlobalLayoutListeners.addAll(viewTreeObserver.mOnGlobalLayoutListeners);
            } else {
                this.mOnGlobalLayoutListeners = viewTreeObserver.mOnGlobalLayoutListeners;
            }
        }
        if (viewTreeObserver.mOnPreDrawListeners != null) {
            if (this.mOnPreDrawListeners != null) {
                this.mOnPreDrawListeners.addAll(viewTreeObserver.mOnPreDrawListeners);
            } else {
                this.mOnPreDrawListeners = viewTreeObserver.mOnPreDrawListeners;
            }
        }
        if (viewTreeObserver.mOnDrawListeners != null) {
            if (this.mOnDrawListeners != null) {
                this.mOnDrawListeners.addAll(viewTreeObserver.mOnDrawListeners);
            } else {
                this.mOnDrawListeners = viewTreeObserver.mOnDrawListeners;
            }
        }
        if (viewTreeObserver.mOnTouchModeChangeListeners != null) {
            if (this.mOnTouchModeChangeListeners != null) {
                this.mOnTouchModeChangeListeners.addAll(viewTreeObserver.mOnTouchModeChangeListeners);
            } else {
                this.mOnTouchModeChangeListeners = viewTreeObserver.mOnTouchModeChangeListeners;
            }
        }
        if (viewTreeObserver.mOnComputeInternalInsetsListeners != null) {
            if (this.mOnComputeInternalInsetsListeners != null) {
                this.mOnComputeInternalInsetsListeners.addAll(viewTreeObserver.mOnComputeInternalInsetsListeners);
            } else {
                this.mOnComputeInternalInsetsListeners = viewTreeObserver.mOnComputeInternalInsetsListeners;
            }
        }
        if (viewTreeObserver.mOnScrollChangedListeners != null) {
            if (this.mOnScrollChangedListeners != null) {
                this.mOnScrollChangedListeners.addAll(viewTreeObserver.mOnScrollChangedListeners);
            } else {
                this.mOnScrollChangedListeners = viewTreeObserver.mOnScrollChangedListeners;
            }
        }
        if (viewTreeObserver.mOnWindowShownListeners != null) {
            if (this.mOnWindowShownListeners != null) {
                this.mOnWindowShownListeners.addAll(viewTreeObserver.mOnWindowShownListeners);
            } else {
                this.mOnWindowShownListeners = viewTreeObserver.mOnWindowShownListeners;
            }
        }
        viewTreeObserver.kill();
    }

    public void addOnWindowAttachListener(OnWindowAttachListener onWindowAttachListener) {
        checkIsAlive();
        if (this.mOnWindowAttachListeners == null) {
            this.mOnWindowAttachListeners = new CopyOnWriteArrayList<>();
        }
        this.mOnWindowAttachListeners.add(onWindowAttachListener);
    }

    public void removeOnWindowAttachListener(OnWindowAttachListener onWindowAttachListener) {
        checkIsAlive();
        if (this.mOnWindowAttachListeners == null) {
            return;
        }
        this.mOnWindowAttachListeners.remove(onWindowAttachListener);
    }

    public void addOnWindowFocusChangeListener(OnWindowFocusChangeListener onWindowFocusChangeListener) {
        checkIsAlive();
        if (this.mOnWindowFocusListeners == null) {
            this.mOnWindowFocusListeners = new CopyOnWriteArrayList<>();
        }
        this.mOnWindowFocusListeners.add(onWindowFocusChangeListener);
    }

    public void removeOnWindowFocusChangeListener(OnWindowFocusChangeListener onWindowFocusChangeListener) {
        checkIsAlive();
        if (this.mOnWindowFocusListeners == null) {
            return;
        }
        this.mOnWindowFocusListeners.remove(onWindowFocusChangeListener);
    }

    public void addOnGlobalFocusChangeListener(OnGlobalFocusChangeListener onGlobalFocusChangeListener) {
        checkIsAlive();
        if (this.mOnGlobalFocusListeners == null) {
            this.mOnGlobalFocusListeners = new CopyOnWriteArrayList<>();
        }
        this.mOnGlobalFocusListeners.add(onGlobalFocusChangeListener);
    }

    public void removeOnGlobalFocusChangeListener(OnGlobalFocusChangeListener onGlobalFocusChangeListener) {
        checkIsAlive();
        if (this.mOnGlobalFocusListeners == null) {
            return;
        }
        this.mOnGlobalFocusListeners.remove(onGlobalFocusChangeListener);
    }

    public void addOnGlobalLayoutListener(OnGlobalLayoutListener onGlobalLayoutListener) {
        checkIsAlive();
        if (this.mOnGlobalLayoutListeners == null) {
            this.mOnGlobalLayoutListeners = new CopyOnWriteArray<>();
        }
        this.mOnGlobalLayoutListeners.add(onGlobalLayoutListener);
    }

    @Deprecated
    public void removeGlobalOnLayoutListener(OnGlobalLayoutListener onGlobalLayoutListener) {
        removeOnGlobalLayoutListener(onGlobalLayoutListener);
    }

    public void removeOnGlobalLayoutListener(OnGlobalLayoutListener onGlobalLayoutListener) {
        checkIsAlive();
        if (this.mOnGlobalLayoutListeners == null) {
            return;
        }
        this.mOnGlobalLayoutListeners.remove(onGlobalLayoutListener);
    }

    public void addOnPreDrawListener(OnPreDrawListener onPreDrawListener) {
        checkIsAlive();
        if (this.mOnPreDrawListeners == null) {
            this.mOnPreDrawListeners = new CopyOnWriteArray<>();
        }
        this.mOnPreDrawListeners.add(onPreDrawListener);
    }

    public void removeOnPreDrawListener(OnPreDrawListener onPreDrawListener) {
        checkIsAlive();
        if (this.mOnPreDrawListeners == null) {
            return;
        }
        this.mOnPreDrawListeners.remove(onPreDrawListener);
    }

    public void addOnWindowShownListener(OnWindowShownListener onWindowShownListener) {
        checkIsAlive();
        if (this.mOnWindowShownListeners == null) {
            this.mOnWindowShownListeners = new CopyOnWriteArray<>();
        }
        this.mOnWindowShownListeners.add(onWindowShownListener);
        if (this.mWindowShown) {
            onWindowShownListener.onWindowShown();
        }
    }

    public void removeOnWindowShownListener(OnWindowShownListener onWindowShownListener) {
        checkIsAlive();
        if (this.mOnWindowShownListeners == null) {
            return;
        }
        this.mOnWindowShownListeners.remove(onWindowShownListener);
    }

    public void addOnDrawListener(OnDrawListener onDrawListener) {
        checkIsAlive();
        if (this.mOnDrawListeners == null) {
            this.mOnDrawListeners = new ArrayList<>();
        }
        if (this.mInDispatchOnDraw) {
            IllegalStateException illegalStateException = new IllegalStateException("Cannot call addOnDrawListener inside of onDraw");
            if (sIllegalOnDrawModificationIsFatal) {
                throw illegalStateException;
            }
            Log.e("ViewTreeObserver", illegalStateException.getMessage(), illegalStateException);
        }
        this.mOnDrawListeners.add(onDrawListener);
    }

    public void removeOnDrawListener(OnDrawListener onDrawListener) {
        checkIsAlive();
        if (this.mOnDrawListeners == null) {
            return;
        }
        if (this.mInDispatchOnDraw) {
            IllegalStateException illegalStateException = new IllegalStateException("Cannot call removeOnDrawListener inside of onDraw");
            if (sIllegalOnDrawModificationIsFatal) {
                throw illegalStateException;
            }
            Log.e("ViewTreeObserver", illegalStateException.getMessage(), illegalStateException);
        }
        this.mOnDrawListeners.remove(onDrawListener);
    }

    public void addOnScrollChangedListener(OnScrollChangedListener onScrollChangedListener) {
        checkIsAlive();
        if (this.mOnScrollChangedListeners == null) {
            this.mOnScrollChangedListeners = new CopyOnWriteArray<>();
        }
        this.mOnScrollChangedListeners.add(onScrollChangedListener);
    }

    public void removeOnScrollChangedListener(OnScrollChangedListener onScrollChangedListener) {
        checkIsAlive();
        if (this.mOnScrollChangedListeners == null) {
            return;
        }
        this.mOnScrollChangedListeners.remove(onScrollChangedListener);
    }

    public void addOnTouchModeChangeListener(OnTouchModeChangeListener onTouchModeChangeListener) {
        checkIsAlive();
        if (this.mOnTouchModeChangeListeners == null) {
            this.mOnTouchModeChangeListeners = new CopyOnWriteArrayList<>();
        }
        this.mOnTouchModeChangeListeners.add(onTouchModeChangeListener);
    }

    public void removeOnTouchModeChangeListener(OnTouchModeChangeListener onTouchModeChangeListener) {
        checkIsAlive();
        if (this.mOnTouchModeChangeListeners == null) {
            return;
        }
        this.mOnTouchModeChangeListeners.remove(onTouchModeChangeListener);
    }

    public void addOnComputeInternalInsetsListener(OnComputeInternalInsetsListener onComputeInternalInsetsListener) {
        checkIsAlive();
        if (this.mOnComputeInternalInsetsListeners == null) {
            this.mOnComputeInternalInsetsListeners = new CopyOnWriteArray<>();
        }
        this.mOnComputeInternalInsetsListeners.add(onComputeInternalInsetsListener);
    }

    public void removeOnComputeInternalInsetsListener(OnComputeInternalInsetsListener onComputeInternalInsetsListener) {
        checkIsAlive();
        if (this.mOnComputeInternalInsetsListeners == null) {
            return;
        }
        this.mOnComputeInternalInsetsListeners.remove(onComputeInternalInsetsListener);
    }

    public void addOnEnterAnimationCompleteListener(OnEnterAnimationCompleteListener onEnterAnimationCompleteListener) {
        checkIsAlive();
        if (this.mOnEnterAnimationCompleteListeners == null) {
            this.mOnEnterAnimationCompleteListeners = new CopyOnWriteArrayList<>();
        }
        this.mOnEnterAnimationCompleteListeners.add(onEnterAnimationCompleteListener);
    }

    public void removeOnEnterAnimationCompleteListener(OnEnterAnimationCompleteListener onEnterAnimationCompleteListener) {
        checkIsAlive();
        if (this.mOnEnterAnimationCompleteListeners == null) {
            return;
        }
        this.mOnEnterAnimationCompleteListeners.remove(onEnterAnimationCompleteListener);
    }

    private void checkIsAlive() {
        if (!this.mAlive) {
            throw new IllegalStateException("This ViewTreeObserver is not alive, call getViewTreeObserver() again");
        }
    }

    public boolean isAlive() {
        return this.mAlive;
    }

    private void kill() {
        this.mAlive = false;
    }

    final void dispatchOnWindowAttachedChange(boolean z) {
        CopyOnWriteArrayList<OnWindowAttachListener> copyOnWriteArrayList = this.mOnWindowAttachListeners;
        if (copyOnWriteArrayList != null && copyOnWriteArrayList.size() > 0) {
            for (OnWindowAttachListener onWindowAttachListener : copyOnWriteArrayList) {
                if (z) {
                    onWindowAttachListener.onWindowAttached();
                } else {
                    onWindowAttachListener.onWindowDetached();
                }
            }
        }
    }

    final void dispatchOnWindowFocusChange(boolean z) {
        CopyOnWriteArrayList<OnWindowFocusChangeListener> copyOnWriteArrayList = this.mOnWindowFocusListeners;
        if (copyOnWriteArrayList != null && copyOnWriteArrayList.size() > 0) {
            Iterator<OnWindowFocusChangeListener> it = copyOnWriteArrayList.iterator();
            while (it.hasNext()) {
                it.next().onWindowFocusChanged(z);
            }
        }
    }

    final void dispatchOnGlobalFocusChange(View view, View view2) {
        CopyOnWriteArrayList<OnGlobalFocusChangeListener> copyOnWriteArrayList = this.mOnGlobalFocusListeners;
        if (copyOnWriteArrayList != null && copyOnWriteArrayList.size() > 0) {
            Iterator<OnGlobalFocusChangeListener> it = copyOnWriteArrayList.iterator();
            while (it.hasNext()) {
                it.next().onGlobalFocusChanged(view, view2);
            }
        }
    }

    public final void dispatchOnGlobalLayout() {
        CopyOnWriteArray<OnGlobalLayoutListener> copyOnWriteArray = this.mOnGlobalLayoutListeners;
        if (copyOnWriteArray != null && copyOnWriteArray.size() > 0) {
            CopyOnWriteArray.Access<OnGlobalLayoutListener> accessStart = copyOnWriteArray.start();
            try {
                int size = accessStart.size();
                for (int i = 0; i < size; i++) {
                    accessStart.get(i).onGlobalLayout();
                }
            } finally {
                copyOnWriteArray.end();
            }
        }
    }

    final boolean hasOnPreDrawListeners() {
        return this.mOnPreDrawListeners != null && this.mOnPreDrawListeners.size() > 0;
    }

    public final boolean dispatchOnPreDraw() {
        CopyOnWriteArray<OnPreDrawListener> copyOnWriteArray = this.mOnPreDrawListeners;
        if (copyOnWriteArray == null || copyOnWriteArray.size() <= 0) {
            return false;
        }
        try {
            int size = copyOnWriteArray.start().size();
            boolean z = false;
            for (int i = 0; i < size; i++) {
                z |= !r2.get(i).onPreDraw();
            }
            copyOnWriteArray.end();
            return z;
        } catch (Throwable th) {
            copyOnWriteArray.end();
            throw th;
        }
    }

    public final void dispatchOnWindowShown() {
        this.mWindowShown = true;
        CopyOnWriteArray<OnWindowShownListener> copyOnWriteArray = this.mOnWindowShownListeners;
        if (copyOnWriteArray != null && copyOnWriteArray.size() > 0) {
            CopyOnWriteArray.Access<OnWindowShownListener> accessStart = copyOnWriteArray.start();
            try {
                int size = accessStart.size();
                for (int i = 0; i < size; i++) {
                    accessStart.get(i).onWindowShown();
                }
            } finally {
                copyOnWriteArray.end();
            }
        }
    }

    public final void dispatchOnDraw() {
        if (this.mOnDrawListeners != null) {
            this.mInDispatchOnDraw = true;
            ArrayList<OnDrawListener> arrayList = this.mOnDrawListeners;
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                arrayList.get(i).onDraw();
            }
            this.mInDispatchOnDraw = false;
        }
    }

    final void dispatchOnTouchModeChanged(boolean z) {
        CopyOnWriteArrayList<OnTouchModeChangeListener> copyOnWriteArrayList = this.mOnTouchModeChangeListeners;
        if (copyOnWriteArrayList != null && copyOnWriteArrayList.size() > 0) {
            Iterator<OnTouchModeChangeListener> it = copyOnWriteArrayList.iterator();
            while (it.hasNext()) {
                it.next().onTouchModeChanged(z);
            }
        }
    }

    final void dispatchOnScrollChanged() {
        CopyOnWriteArray<OnScrollChangedListener> copyOnWriteArray = this.mOnScrollChangedListeners;
        if (copyOnWriteArray != null && copyOnWriteArray.size() > 0) {
            CopyOnWriteArray.Access<OnScrollChangedListener> accessStart = copyOnWriteArray.start();
            try {
                int size = accessStart.size();
                for (int i = 0; i < size; i++) {
                    accessStart.get(i).onScrollChanged();
                }
            } finally {
                copyOnWriteArray.end();
            }
        }
    }

    final boolean hasComputeInternalInsetsListeners() {
        CopyOnWriteArray<OnComputeInternalInsetsListener> copyOnWriteArray = this.mOnComputeInternalInsetsListeners;
        return copyOnWriteArray != null && copyOnWriteArray.size() > 0;
    }

    final void dispatchOnComputeInternalInsets(InternalInsetsInfo internalInsetsInfo) {
        CopyOnWriteArray<OnComputeInternalInsetsListener> copyOnWriteArray = this.mOnComputeInternalInsetsListeners;
        if (copyOnWriteArray != null && copyOnWriteArray.size() > 0) {
            CopyOnWriteArray.Access<OnComputeInternalInsetsListener> accessStart = copyOnWriteArray.start();
            try {
                int size = accessStart.size();
                for (int i = 0; i < size; i++) {
                    accessStart.get(i).onComputeInternalInsets(internalInsetsInfo);
                }
            } finally {
                copyOnWriteArray.end();
            }
        }
    }

    public final void dispatchOnEnterAnimationComplete() {
        CopyOnWriteArrayList<OnEnterAnimationCompleteListener> copyOnWriteArrayList = this.mOnEnterAnimationCompleteListeners;
        if (copyOnWriteArrayList != null && !copyOnWriteArrayList.isEmpty()) {
            Iterator<OnEnterAnimationCompleteListener> it = copyOnWriteArrayList.iterator();
            while (it.hasNext()) {
                it.next().onEnterAnimationComplete();
            }
        }
    }

    static class CopyOnWriteArray<T> {
        private ArrayList<T> mDataCopy;
        private boolean mStart;
        private ArrayList<T> mData = new ArrayList<>();
        private final Access<T> mAccess = new Access<>();

        static class Access<T> {
            private ArrayList<T> mData;
            private int mSize;

            Access() {
            }

            T get(int i) {
                return this.mData.get(i);
            }

            int size() {
                return this.mSize;
            }
        }

        CopyOnWriteArray() {
        }

        private ArrayList<T> getArray() {
            if (this.mStart) {
                if (this.mDataCopy == null) {
                    this.mDataCopy = new ArrayList<>(this.mData);
                }
                return this.mDataCopy;
            }
            return this.mData;
        }

        Access<T> start() {
            if (this.mStart) {
                throw new IllegalStateException("Iteration already started");
            }
            this.mStart = true;
            this.mDataCopy = null;
            ((Access) this.mAccess).mData = this.mData;
            ((Access) this.mAccess).mSize = this.mData.size();
            return this.mAccess;
        }

        void end() {
            if (!this.mStart) {
                throw new IllegalStateException("Iteration not started");
            }
            this.mStart = false;
            if (this.mDataCopy != null) {
                this.mData = this.mDataCopy;
                ((Access) this.mAccess).mData.clear();
                ((Access) this.mAccess).mSize = 0;
            }
            this.mDataCopy = null;
        }

        int size() {
            return getArray().size();
        }

        void add(T t) {
            getArray().add(t);
        }

        void addAll(CopyOnWriteArray<T> copyOnWriteArray) {
            getArray().addAll(copyOnWriteArray.mData);
        }

        void remove(T t) {
            getArray().remove(t);
        }

        void clear() {
            getArray().clear();
        }
    }
}
