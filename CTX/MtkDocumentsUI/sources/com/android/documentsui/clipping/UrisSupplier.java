package com.android.documentsui.clipping;

import android.content.ClipData;
import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.util.Log;
import com.android.documentsui.DocumentsApplication;
import com.android.documentsui.selection.Selection;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public abstract class UrisSupplier implements Parcelable {
    public abstract int getItemCount();

    abstract Iterable<Uri> getUris(ClipStore clipStore) throws IOException;

    public Iterable<Uri> getUris(Context context) throws IOException {
        return getUris(DocumentsApplication.getClipStore(context));
    }

    public void dispose() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static UrisSupplier create(ClipData clipData, ClipStore clipStore) throws IOException {
        if (clipData.getDescription().getExtras().containsKey("jumboSelection-tag")) {
            return new JumboUrisSupplier(clipData, clipStore);
        }
        return new StandardUrisSupplier(clipData);
    }

    public static UrisSupplier create(Selection selection, Function<String, Uri> function, ClipStore clipStore) throws IOException {
        ArrayList arrayList = new ArrayList(selection.size());
        Iterator<String> it = selection.iterator();
        while (it.hasNext()) {
            arrayList.add(function.apply(it.next()));
        }
        return create(arrayList, clipStore);
    }

    static UrisSupplier create(List<Uri> list, ClipStore clipStore) throws IOException {
        if (list.size() > 500) {
            return new JumboUrisSupplier(list, clipStore);
        }
        return new StandardUrisSupplier(list);
    }

    private static class JumboUrisSupplier extends UrisSupplier {
        static final boolean $assertionsDisabled = false;
        public static final Parcelable.Creator<JumboUrisSupplier> CREATOR = new Parcelable.Creator<JumboUrisSupplier>() {
            @Override
            public JumboUrisSupplier createFromParcel(Parcel parcel) {
                return new JumboUrisSupplier(parcel);
            }

            @Override
            public JumboUrisSupplier[] newArray(int i) {
                return new JumboUrisSupplier[i];
            }
        };
        private final File mFile;
        private final List<ClipStorageReader> mReaders;
        private final int mSelectionSize;

        private JumboUrisSupplier(ClipData clipData, ClipStore clipStore) throws IOException {
            this.mReaders = new ArrayList();
            PersistableBundle extras = clipData.getDescription().getExtras();
            this.mFile = clipStore.getFile(extras.getInt("jumboSelection-tag", -1));
            this.mSelectionSize = extras.getInt("jumboSelection-size");
        }

        private JumboUrisSupplier(Collection<Uri> collection, ClipStore clipStore) throws IOException {
            this.mReaders = new ArrayList();
            this.mFile = clipStore.getFile(clipStore.persistUris(collection));
            this.mSelectionSize = collection.size();
        }

        @Override
        public int getItemCount() {
            return this.mSelectionSize;
        }

        @Override
        Iterable<Uri> getUris(ClipStore clipStore) throws IOException {
            ClipStorageReader clipStorageReaderCreateReader = clipStore.createReader(this.mFile);
            synchronized (this.mReaders) {
                this.mReaders.add(clipStorageReaderCreateReader);
            }
            return clipStorageReaderCreateReader;
        }

        @Override
        public void dispose() {
            synchronized (this.mReaders) {
                Iterator<ClipStorageReader> it = this.mReaders.iterator();
                while (it.hasNext()) {
                    try {
                        it.next().close();
                    } catch (IOException e) {
                        Log.w("JumboUrisSupplier", "Failed to close a reader.", e);
                    }
                }
            }
            this.mFile.delete();
        }

        public String toString() {
            return "JumboUrisSupplier{file=" + this.mFile.getAbsolutePath() + ", selectionSize=" + this.mSelectionSize + "}";
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(this.mFile.getAbsolutePath());
            parcel.writeInt(this.mSelectionSize);
        }

        private JumboUrisSupplier(Parcel parcel) {
            this.mReaders = new ArrayList();
            this.mFile = new File(parcel.readString());
            this.mSelectionSize = parcel.readInt();
        }
    }

    public static class StandardUrisSupplier extends UrisSupplier {
        static final boolean $assertionsDisabled = false;
        public static final Parcelable.Creator<StandardUrisSupplier> CREATOR = new Parcelable.Creator<StandardUrisSupplier>() {
            @Override
            public StandardUrisSupplier createFromParcel(Parcel parcel) {
                return new StandardUrisSupplier(parcel);
            }

            @Override
            public StandardUrisSupplier[] newArray(int i) {
                return new StandardUrisSupplier[i];
            }
        };
        private final List<Uri> mDocs;

        private StandardUrisSupplier(ClipData clipData) {
            this.mDocs = listDocs(clipData);
        }

        public StandardUrisSupplier(List<Uri> list) {
            this.mDocs = list;
        }

        private List<Uri> listDocs(ClipData clipData) {
            ArrayList arrayList = new ArrayList(clipData.getItemCount());
            for (int i = 0; i < clipData.getItemCount(); i++) {
                arrayList.add(clipData.getItemAt(i).getUri());
            }
            return arrayList;
        }

        @Override
        public int getItemCount() {
            return this.mDocs.size();
        }

        @Override
        Iterable<Uri> getUris(ClipStore clipStore) {
            return this.mDocs;
        }

        public String toString() {
            return "StandardUrisSupplier{docs=" + this.mDocs.toString() + "}";
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeTypedList(this.mDocs);
        }

        private StandardUrisSupplier(Parcel parcel) {
            this.mDocs = parcel.createTypedArrayList(Uri.CREATOR);
        }
    }
}
