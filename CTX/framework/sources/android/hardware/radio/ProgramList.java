package android.hardware.radio;

import android.annotation.SystemApi;
import android.hardware.radio.ProgramList;
import android.hardware.radio.ProgramSelector;
import android.hardware.radio.RadioManager;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SystemApi
public final class ProgramList implements AutoCloseable {
    private OnCloseListener mOnCloseListener;
    private final Object mLock = new Object();
    private final Map<ProgramSelector.Identifier, RadioManager.ProgramInfo> mPrograms = new HashMap();
    private final List<ListCallback> mListCallbacks = new ArrayList();
    private final List<OnCompleteListener> mOnCompleteListeners = new ArrayList();
    private boolean mIsClosed = false;
    private boolean mIsComplete = false;

    interface OnCloseListener {
        void onClose();
    }

    public interface OnCompleteListener {
        void onComplete();
    }

    ProgramList() {
    }

    public static abstract class ListCallback {
        public void onItemChanged(ProgramSelector.Identifier identifier) {
        }

        public void onItemRemoved(ProgramSelector.Identifier identifier) {
        }
    }

    class AnonymousClass1 extends ListCallback {
        final ListCallback val$callback;
        final Executor val$executor;

        AnonymousClass1(Executor executor, ListCallback listCallback) {
            this.val$executor = executor;
            this.val$callback = listCallback;
        }

        @Override
        public void onItemChanged(final ProgramSelector.Identifier identifier) {
            Executor executor = this.val$executor;
            final ListCallback listCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    listCallback.onItemChanged(identifier);
                }
            });
        }

        @Override
        public void onItemRemoved(final ProgramSelector.Identifier identifier) {
            Executor executor = this.val$executor;
            final ListCallback listCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    listCallback.onItemRemoved(identifier);
                }
            });
        }
    }

    public void registerListCallback(Executor executor, ListCallback listCallback) {
        registerListCallback(new AnonymousClass1(executor, listCallback));
    }

    public void registerListCallback(ListCallback listCallback) {
        synchronized (this.mLock) {
            if (this.mIsClosed) {
                return;
            }
            this.mListCallbacks.add((ListCallback) Objects.requireNonNull(listCallback));
        }
    }

    public void unregisterListCallback(ListCallback listCallback) {
        synchronized (this.mLock) {
            if (this.mIsClosed) {
                return;
            }
            this.mListCallbacks.remove(Objects.requireNonNull(listCallback));
        }
    }

    static void lambda$addOnCompleteListener$0(Executor executor, final OnCompleteListener onCompleteListener) {
        Objects.requireNonNull(onCompleteListener);
        executor.execute(new Runnable() {
            @Override
            public final void run() {
                onCompleteListener.onComplete();
            }
        });
    }

    public void addOnCompleteListener(final Executor executor, final OnCompleteListener onCompleteListener) {
        addOnCompleteListener(new OnCompleteListener() {
            @Override
            public final void onComplete() {
                ProgramList.lambda$addOnCompleteListener$0(executor, onCompleteListener);
            }
        });
    }

    public void addOnCompleteListener(OnCompleteListener onCompleteListener) {
        synchronized (this.mLock) {
            if (this.mIsClosed) {
                return;
            }
            this.mOnCompleteListeners.add((OnCompleteListener) Objects.requireNonNull(onCompleteListener));
            if (this.mIsComplete) {
                onCompleteListener.onComplete();
            }
        }
    }

    public void removeOnCompleteListener(OnCompleteListener onCompleteListener) {
        synchronized (this.mLock) {
            if (this.mIsClosed) {
                return;
            }
            this.mOnCompleteListeners.remove(Objects.requireNonNull(onCompleteListener));
        }
    }

    void setOnCloseListener(OnCloseListener onCloseListener) {
        synchronized (this.mLock) {
            if (this.mOnCloseListener != null) {
                throw new IllegalStateException("Close callback is already set");
            }
            this.mOnCloseListener = onCloseListener;
        }
    }

    @Override
    public void close() {
        synchronized (this.mLock) {
            if (this.mIsClosed) {
                return;
            }
            this.mIsClosed = true;
            this.mPrograms.clear();
            this.mListCallbacks.clear();
            this.mOnCompleteListeners.clear();
            if (this.mOnCloseListener != null) {
                this.mOnCloseListener.onClose();
                this.mOnCloseListener = null;
            }
        }
    }

    void apply(Chunk chunk) {
        synchronized (this.mLock) {
            if (this.mIsClosed) {
                return;
            }
            this.mIsComplete = false;
            if (chunk.isPurge()) {
                new HashSet(this.mPrograms.keySet()).stream().forEach(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        this.f$0.removeLocked((ProgramSelector.Identifier) obj);
                    }
                });
            }
            chunk.getRemoved().stream().forEach(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    this.f$0.removeLocked((ProgramSelector.Identifier) obj);
                }
            });
            chunk.getModified().stream().forEach(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    this.f$0.putLocked((RadioManager.ProgramInfo) obj);
                }
            });
            if (chunk.isComplete()) {
                this.mIsComplete = true;
                this.mOnCompleteListeners.forEach(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        ((ProgramList.OnCompleteListener) obj).onComplete();
                    }
                });
            }
        }
    }

    private void putLocked(RadioManager.ProgramInfo programInfo) {
        this.mPrograms.put((ProgramSelector.Identifier) Objects.requireNonNull(programInfo.getSelector().getPrimaryId()), programInfo);
        final ProgramSelector.Identifier primaryId = programInfo.getSelector().getPrimaryId();
        this.mListCallbacks.forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((ProgramList.ListCallback) obj).onItemChanged(primaryId);
            }
        });
    }

    private void removeLocked(ProgramSelector.Identifier identifier) {
        RadioManager.ProgramInfo programInfoRemove = this.mPrograms.remove(Objects.requireNonNull(identifier));
        if (programInfoRemove == null) {
            return;
        }
        final ProgramSelector.Identifier primaryId = programInfoRemove.getSelector().getPrimaryId();
        this.mListCallbacks.forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((ProgramList.ListCallback) obj).onItemRemoved(primaryId);
            }
        });
    }

    public List<RadioManager.ProgramInfo> toList() {
        List<RadioManager.ProgramInfo> list;
        synchronized (this.mLock) {
            list = (List) this.mPrograms.values().stream().collect(Collectors.toList());
        }
        return list;
    }

    public RadioManager.ProgramInfo get(ProgramSelector.Identifier identifier) {
        RadioManager.ProgramInfo programInfo;
        synchronized (this.mLock) {
            programInfo = this.mPrograms.get(Objects.requireNonNull(identifier));
        }
        return programInfo;
    }

    public static final class Filter implements Parcelable {
        public static final Parcelable.Creator<Filter> CREATOR = new Parcelable.Creator<Filter>() {
            @Override
            public Filter createFromParcel(Parcel parcel) {
                return new Filter(parcel, null);
            }

            @Override
            public Filter[] newArray(int i) {
                return new Filter[i];
            }
        };
        private final boolean mExcludeModifications;
        private final Set<Integer> mIdentifierTypes;
        private final Set<ProgramSelector.Identifier> mIdentifiers;
        private final boolean mIncludeCategories;
        private final Map<String, String> mVendorFilter;

        Filter(Parcel parcel, AnonymousClass1 anonymousClass1) {
            this(parcel);
        }

        public Filter(Set<Integer> set, Set<ProgramSelector.Identifier> set2, boolean z, boolean z2) {
            this.mIdentifierTypes = (Set) Objects.requireNonNull(set);
            this.mIdentifiers = (Set) Objects.requireNonNull(set2);
            this.mIncludeCategories = z;
            this.mExcludeModifications = z2;
            this.mVendorFilter = null;
        }

        public Filter() {
            this.mIdentifierTypes = Collections.emptySet();
            this.mIdentifiers = Collections.emptySet();
            this.mIncludeCategories = false;
            this.mExcludeModifications = false;
            this.mVendorFilter = null;
        }

        public Filter(Map<String, String> map) {
            this.mIdentifierTypes = Collections.emptySet();
            this.mIdentifiers = Collections.emptySet();
            this.mIncludeCategories = false;
            this.mExcludeModifications = false;
            this.mVendorFilter = map;
        }

        private Filter(Parcel parcel) {
            this.mIdentifierTypes = Utils.createIntSet(parcel);
            this.mIdentifiers = Utils.createSet(parcel, ProgramSelector.Identifier.CREATOR);
            this.mIncludeCategories = parcel.readByte() != 0;
            this.mExcludeModifications = parcel.readByte() != 0;
            this.mVendorFilter = Utils.readStringMap(parcel);
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            Utils.writeIntSet(parcel, this.mIdentifierTypes);
            Utils.writeSet(parcel, this.mIdentifiers);
            parcel.writeByte(this.mIncludeCategories ? (byte) 1 : (byte) 0);
            parcel.writeByte(this.mExcludeModifications ? (byte) 1 : (byte) 0);
            Utils.writeStringMap(parcel, this.mVendorFilter);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public Map<String, String> getVendorFilter() {
            return this.mVendorFilter;
        }

        public Set<Integer> getIdentifierTypes() {
            return this.mIdentifierTypes;
        }

        public Set<ProgramSelector.Identifier> getIdentifiers() {
            return this.mIdentifiers;
        }

        public boolean areCategoriesIncluded() {
            return this.mIncludeCategories;
        }

        public boolean areModificationsExcluded() {
            return this.mExcludeModifications;
        }
    }

    public static final class Chunk implements Parcelable {
        public static final Parcelable.Creator<Chunk> CREATOR = new Parcelable.Creator<Chunk>() {
            @Override
            public Chunk createFromParcel(Parcel parcel) {
                return new Chunk(parcel, null);
            }

            @Override
            public Chunk[] newArray(int i) {
                return new Chunk[i];
            }
        };
        private final boolean mComplete;
        private final Set<RadioManager.ProgramInfo> mModified;
        private final boolean mPurge;
        private final Set<ProgramSelector.Identifier> mRemoved;

        Chunk(Parcel parcel, AnonymousClass1 anonymousClass1) {
            this(parcel);
        }

        public Chunk(boolean z, boolean z2, Set<RadioManager.ProgramInfo> set, Set<ProgramSelector.Identifier> set2) {
            this.mPurge = z;
            this.mComplete = z2;
            this.mModified = set == null ? Collections.emptySet() : set;
            this.mRemoved = set2 == null ? Collections.emptySet() : set2;
        }

        private Chunk(Parcel parcel) {
            this.mPurge = parcel.readByte() != 0;
            this.mComplete = parcel.readByte() != 0;
            this.mModified = Utils.createSet(parcel, RadioManager.ProgramInfo.CREATOR);
            this.mRemoved = Utils.createSet(parcel, ProgramSelector.Identifier.CREATOR);
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeByte(this.mPurge ? (byte) 1 : (byte) 0);
            parcel.writeByte(this.mComplete ? (byte) 1 : (byte) 0);
            Utils.writeSet(parcel, this.mModified);
            Utils.writeSet(parcel, this.mRemoved);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public boolean isPurge() {
            return this.mPurge;
        }

        public boolean isComplete() {
            return this.mComplete;
        }

        public Set<RadioManager.ProgramInfo> getModified() {
            return this.mModified;
        }

        public Set<ProgramSelector.Identifier> getRemoved() {
            return this.mRemoved;
        }
    }
}
