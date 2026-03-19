package com.android.documentsui.base;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class DocumentStack implements Parcelable, Durable {
    public static final Parcelable.Creator<DocumentStack> CREATOR = new Parcelable.Creator<DocumentStack>() {
        @Override
        public DocumentStack createFromParcel(Parcel parcel) {
            DocumentStack documentStack = new DocumentStack();
            DurableUtils.readFromParcel(parcel, documentStack);
            return documentStack;
        }

        @Override
        public DocumentStack[] newArray(int i) {
            return new DocumentStack[i];
        }
    };
    private LinkedList<DocumentInfo> mList;
    private RootInfo mRoot;
    private boolean mStackTouched;

    public DocumentStack() {
        this.mList = new LinkedList<>();
    }

    public DocumentStack(RootInfo rootInfo, DocumentInfo... documentInfoArr) {
        this.mList = new LinkedList<>();
        for (DocumentInfo documentInfo : documentInfoArr) {
            this.mList.add(documentInfo);
        }
        this.mRoot = rootInfo;
    }

    public DocumentStack(RootInfo rootInfo, List<DocumentInfo> list) {
        this.mList = new LinkedList<>(list);
        this.mRoot = rootInfo;
    }

    public DocumentStack(DocumentStack documentStack, DocumentInfo... documentInfoArr) {
        this.mList = new LinkedList<>(documentStack.mList);
        for (DocumentInfo documentInfo : documentInfoArr) {
            push(documentInfo);
        }
        this.mStackTouched = false;
        this.mRoot = documentStack.mRoot;
    }

    public boolean isInitialized() {
        return this.mRoot != null;
    }

    public RootInfo getRoot() {
        return this.mRoot;
    }

    public boolean isEmpty() {
        return this.mList.isEmpty();
    }

    public int size() {
        return this.mList.size();
    }

    public DocumentInfo peek() {
        return this.mList.peekLast();
    }

    public DocumentInfo get(int i) {
        return this.mList.get(i);
    }

    public void push(DocumentInfo documentInfo) {
        Preconditions.checkArgument(!this.mList.contains(documentInfo));
        if (SharedMinimal.DEBUG) {
            Log.d("DocumentStack", "Adding doc to stack: " + documentInfo);
        }
        this.mList.addLast(documentInfo);
        this.mStackTouched = true;
    }

    public boolean isPresent(DocumentInfo documentInfo) {
        return this.mList.contains(documentInfo);
    }

    public DocumentInfo pop() {
        if (SharedMinimal.DEBUG) {
            Log.d("DocumentStack", "Popping doc off stack.");
        }
        DocumentInfo documentInfoRemoveLast = this.mList.removeLast();
        this.mStackTouched = true;
        return documentInfoRemoveLast;
    }

    public void popToRootDocument() {
        if (SharedMinimal.DEBUG) {
            Log.d("DocumentStack", "Popping docs to root folder.");
        }
        while (this.mList.size() > 1) {
            this.mList.removeLast();
        }
        this.mStackTouched = true;
    }

    public void changeRoot(RootInfo rootInfo) {
        if (SharedMinimal.DEBUG) {
            Log.d("DocumentStack", "Root changed to: " + rootInfo);
        }
        reset();
        this.mRoot = rootInfo;
    }

    public boolean hasLocationChanged() {
        return this.mStackTouched;
    }

    public String getTitle() {
        if (this.mList.size() == 1 && this.mRoot != null) {
            return this.mRoot.title;
        }
        if (this.mList.size() > 1) {
            return peek().displayName;
        }
        return null;
    }

    public boolean isRecents() {
        return this.mRoot != null && this.mRoot.isRecents();
    }

    public void reset(DocumentStack documentStack) {
        if (SharedMinimal.DEBUG) {
            Log.d("DocumentStack", "Resetting the whole darn stack to: " + documentStack);
        }
        this.mList = documentStack.mList;
        this.mRoot = documentStack.mRoot;
        this.mStackTouched = true;
    }

    public String toString() {
        return "DocumentStack{root=" + this.mRoot + ", docStack=" + this.mList + ", stackTouched=" + this.mStackTouched + "}";
    }

    @Override
    public void reset() {
        this.mList.clear();
        this.mRoot = null;
    }

    private void updateRoot(Collection<RootInfo> collection) throws FileNotFoundException {
        for (RootInfo rootInfo : collection) {
            if (rootInfo.equals(this.mRoot)) {
                this.mRoot = rootInfo;
                return;
            }
        }
        throw new FileNotFoundException("Failed to find matching mRoot for " + this.mRoot);
    }

    private synchronized void updateDocuments(ContentResolver contentResolver) throws FileNotFoundException {
        Iterator<DocumentInfo> it = this.mList.iterator();
        while (it.hasNext()) {
            it.next().updateSelf(contentResolver);
        }
    }

    public static DocumentStack fromLastAccessedCursor(Cursor cursor, Collection<RootInfo> collection, ContentResolver contentResolver) throws IOException {
        if (cursor.moveToFirst()) {
            DocumentStack documentStack = new DocumentStack();
            DurableUtils.readFromArray(cursor.getBlob(cursor.getColumnIndex("stack")), documentStack);
            documentStack.updateRoot(collection);
            documentStack.updateDocuments(contentResolver);
            return documentStack;
        }
        return null;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DocumentStack)) {
            return false;
        }
        DocumentStack documentStack = (DocumentStack) obj;
        return Objects.equals(this.mRoot, documentStack.mRoot) && this.mList.equals(documentStack.mList);
    }

    public int hashCode() {
        return Objects.hash(this.mRoot, this.mList);
    }

    @Override
    public void read(DataInputStream dataInputStream) throws IOException {
        int i = dataInputStream.readInt();
        switch (i) {
            case 1:
                throw new ProtocolException("Ignored upgrade");
            case 2:
                if (dataInputStream.readBoolean()) {
                    this.mRoot = new RootInfo();
                    this.mRoot.read(dataInputStream);
                }
                int i2 = dataInputStream.readInt();
                for (int i3 = 0; i3 < i2; i3++) {
                    DocumentInfo documentInfo = new DocumentInfo();
                    documentInfo.read(dataInputStream);
                    this.mList.add(documentInfo);
                }
                this.mStackTouched = dataInputStream.readInt() != 0;
                return;
            default:
                throw new ProtocolException("Unknown version " + i);
        }
    }

    @Override
    public void write(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(2);
        if (this.mRoot != null) {
            dataOutputStream.writeBoolean(true);
            this.mRoot.write(dataOutputStream);
        } else {
            dataOutputStream.writeBoolean(false);
        }
        int size = this.mList.size();
        dataOutputStream.writeInt(size);
        for (int i = 0; i < size; i++) {
            this.mList.get(i).write(dataOutputStream);
        }
        dataOutputStream.writeInt(this.mStackTouched ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        DurableUtils.writeToParcel(parcel, this);
    }
}
