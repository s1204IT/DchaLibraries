package android.content;

import android.content.res.AssetFileDescriptor;
import android.database.BulkCursorDescriptor;
import android.database.BulkCursorToCursorAdaptor;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ICancellationSignal;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;

final class ContentProviderProxy implements IContentProvider {
    private IBinder mRemote;

    public ContentProviderProxy(IBinder iBinder) {
        this.mRemote = iBinder;
    }

    @Override
    public IBinder asBinder() {
        return this.mRemote;
    }

    @Override
    public Cursor query(String str, Uri uri, String[] strArr, Bundle bundle, ICancellationSignal iCancellationSignal) throws RemoteException {
        int length;
        BulkCursorToCursorAdaptor bulkCursorToCursorAdaptor = new BulkCursorToCursorAdaptor();
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            try {
                try {
                    parcelObtain.writeInterfaceToken(IContentProvider.descriptor);
                    parcelObtain.writeString(str);
                    uri.writeToParcel(parcelObtain, 0);
                    if (strArr != null) {
                        length = strArr.length;
                    } else {
                        length = 0;
                    }
                    parcelObtain.writeInt(length);
                    for (int i = 0; i < length; i++) {
                        parcelObtain.writeString(strArr[i]);
                    }
                    parcelObtain.writeBundle(bundle);
                    parcelObtain.writeStrongBinder(bulkCursorToCursorAdaptor.getObserver().asBinder());
                    BulkCursorToCursorAdaptor bulkCursorToCursorAdaptor2 = null;
                    parcelObtain.writeStrongBinder(iCancellationSignal != null ? iCancellationSignal.asBinder() : null);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    DatabaseUtils.readExceptionFromParcel(parcelObtain2);
                    if (parcelObtain2.readInt() != 0) {
                        BulkCursorDescriptor bulkCursorDescriptorCreateFromParcel = BulkCursorDescriptor.CREATOR.createFromParcel(parcelObtain2);
                        Binder.copyAllowBlocking(this.mRemote, bulkCursorDescriptorCreateFromParcel.cursor != null ? bulkCursorDescriptorCreateFromParcel.cursor.asBinder() : null);
                        bulkCursorToCursorAdaptor.initialize(bulkCursorDescriptorCreateFromParcel);
                        bulkCursorToCursorAdaptor2 = bulkCursorToCursorAdaptor;
                    } else {
                        bulkCursorToCursorAdaptor.close();
                    }
                    return bulkCursorToCursorAdaptor2;
                } catch (RuntimeException e) {
                    bulkCursorToCursorAdaptor.close();
                    throw e;
                }
            } catch (RemoteException e2) {
                bulkCursorToCursorAdaptor.close();
                throw e2;
            }
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    @Override
    public String getType(Uri uri) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IContentProvider.descriptor);
            uri.writeToParcel(parcelObtain, 0);
            this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
            DatabaseUtils.readExceptionFromParcel(parcelObtain2);
            return parcelObtain2.readString();
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    @Override
    public Uri insert(String str, Uri uri, ContentValues contentValues) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IContentProvider.descriptor);
            parcelObtain.writeString(str);
            uri.writeToParcel(parcelObtain, 0);
            contentValues.writeToParcel(parcelObtain, 0);
            this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
            DatabaseUtils.readExceptionFromParcel(parcelObtain2);
            return Uri.CREATOR.createFromParcel(parcelObtain2);
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    @Override
    public int bulkInsert(String str, Uri uri, ContentValues[] contentValuesArr) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IContentProvider.descriptor);
            parcelObtain.writeString(str);
            uri.writeToParcel(parcelObtain, 0);
            parcelObtain.writeTypedArray(contentValuesArr, 0);
            this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
            DatabaseUtils.readExceptionFromParcel(parcelObtain2);
            return parcelObtain2.readInt();
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    @Override
    public ContentProviderResult[] applyBatch(String str, ArrayList<ContentProviderOperation> arrayList) throws RemoteException, OperationApplicationException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IContentProvider.descriptor);
            parcelObtain.writeString(str);
            parcelObtain.writeInt(arrayList.size());
            Iterator<ContentProviderOperation> it = arrayList.iterator();
            while (it.hasNext()) {
                it.next().writeToParcel(parcelObtain, 0);
            }
            this.mRemote.transact(20, parcelObtain, parcelObtain2, 0);
            DatabaseUtils.readExceptionWithOperationApplicationExceptionFromParcel(parcelObtain2);
            return (ContentProviderResult[]) parcelObtain2.createTypedArray(ContentProviderResult.CREATOR);
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    @Override
    public int delete(String str, Uri uri, String str2, String[] strArr) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IContentProvider.descriptor);
            parcelObtain.writeString(str);
            uri.writeToParcel(parcelObtain, 0);
            parcelObtain.writeString(str2);
            parcelObtain.writeStringArray(strArr);
            this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
            DatabaseUtils.readExceptionFromParcel(parcelObtain2);
            return parcelObtain2.readInt();
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    @Override
    public int update(String str, Uri uri, ContentValues contentValues, String str2, String[] strArr) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IContentProvider.descriptor);
            parcelObtain.writeString(str);
            uri.writeToParcel(parcelObtain, 0);
            contentValues.writeToParcel(parcelObtain, 0);
            parcelObtain.writeString(str2);
            parcelObtain.writeStringArray(strArr);
            this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
            DatabaseUtils.readExceptionFromParcel(parcelObtain2);
            return parcelObtain2.readInt();
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    @Override
    public ParcelFileDescriptor openFile(String str, Uri uri, String str2, ICancellationSignal iCancellationSignal, IBinder iBinder) throws RemoteException, FileNotFoundException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IContentProvider.descriptor);
            parcelObtain.writeString(str);
            uri.writeToParcel(parcelObtain, 0);
            parcelObtain.writeString(str2);
            ParcelFileDescriptor parcelFileDescriptorCreateFromParcel = null;
            parcelObtain.writeStrongBinder(iCancellationSignal != null ? iCancellationSignal.asBinder() : null);
            parcelObtain.writeStrongBinder(iBinder);
            this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
            DatabaseUtils.readExceptionWithFileNotFoundExceptionFromParcel(parcelObtain2);
            if (parcelObtain2.readInt() != 0) {
                parcelFileDescriptorCreateFromParcel = ParcelFileDescriptor.CREATOR.createFromParcel(parcelObtain2);
            }
            return parcelFileDescriptorCreateFromParcel;
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    @Override
    public AssetFileDescriptor openAssetFile(String str, Uri uri, String str2, ICancellationSignal iCancellationSignal) throws RemoteException, FileNotFoundException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IContentProvider.descriptor);
            parcelObtain.writeString(str);
            uri.writeToParcel(parcelObtain, 0);
            parcelObtain.writeString(str2);
            AssetFileDescriptor assetFileDescriptorCreateFromParcel = null;
            parcelObtain.writeStrongBinder(iCancellationSignal != null ? iCancellationSignal.asBinder() : null);
            this.mRemote.transact(15, parcelObtain, parcelObtain2, 0);
            DatabaseUtils.readExceptionWithFileNotFoundExceptionFromParcel(parcelObtain2);
            if (parcelObtain2.readInt() != 0) {
                assetFileDescriptorCreateFromParcel = AssetFileDescriptor.CREATOR.createFromParcel(parcelObtain2);
            }
            return assetFileDescriptorCreateFromParcel;
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    @Override
    public Bundle call(String str, String str2, String str3, Bundle bundle) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IContentProvider.descriptor);
            parcelObtain.writeString(str);
            parcelObtain.writeString(str2);
            parcelObtain.writeString(str3);
            parcelObtain.writeBundle(bundle);
            this.mRemote.transact(21, parcelObtain, parcelObtain2, 0);
            DatabaseUtils.readExceptionFromParcel(parcelObtain2);
            return parcelObtain2.readBundle();
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    @Override
    public String[] getStreamTypes(Uri uri, String str) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IContentProvider.descriptor);
            uri.writeToParcel(parcelObtain, 0);
            parcelObtain.writeString(str);
            this.mRemote.transact(22, parcelObtain, parcelObtain2, 0);
            DatabaseUtils.readExceptionFromParcel(parcelObtain2);
            return parcelObtain2.createStringArray();
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    @Override
    public AssetFileDescriptor openTypedAssetFile(String str, Uri uri, String str2, Bundle bundle, ICancellationSignal iCancellationSignal) throws RemoteException, FileNotFoundException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IContentProvider.descriptor);
            parcelObtain.writeString(str);
            uri.writeToParcel(parcelObtain, 0);
            parcelObtain.writeString(str2);
            parcelObtain.writeBundle(bundle);
            AssetFileDescriptor assetFileDescriptorCreateFromParcel = null;
            parcelObtain.writeStrongBinder(iCancellationSignal != null ? iCancellationSignal.asBinder() : null);
            this.mRemote.transact(23, parcelObtain, parcelObtain2, 0);
            DatabaseUtils.readExceptionWithFileNotFoundExceptionFromParcel(parcelObtain2);
            if (parcelObtain2.readInt() != 0) {
                assetFileDescriptorCreateFromParcel = AssetFileDescriptor.CREATOR.createFromParcel(parcelObtain2);
            }
            return assetFileDescriptorCreateFromParcel;
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    @Override
    public ICancellationSignal createCancellationSignal() throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IContentProvider.descriptor);
            this.mRemote.transact(24, parcelObtain, parcelObtain2, 0);
            DatabaseUtils.readExceptionFromParcel(parcelObtain2);
            return ICancellationSignal.Stub.asInterface(parcelObtain2.readStrongBinder());
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    @Override
    public Uri canonicalize(String str, Uri uri) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IContentProvider.descriptor);
            parcelObtain.writeString(str);
            uri.writeToParcel(parcelObtain, 0);
            this.mRemote.transact(25, parcelObtain, parcelObtain2, 0);
            DatabaseUtils.readExceptionFromParcel(parcelObtain2);
            return Uri.CREATOR.createFromParcel(parcelObtain2);
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    @Override
    public Uri uncanonicalize(String str, Uri uri) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IContentProvider.descriptor);
            parcelObtain.writeString(str);
            uri.writeToParcel(parcelObtain, 0);
            this.mRemote.transact(26, parcelObtain, parcelObtain2, 0);
            DatabaseUtils.readExceptionFromParcel(parcelObtain2);
            return Uri.CREATOR.createFromParcel(parcelObtain2);
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    @Override
    public boolean refresh(String str, Uri uri, Bundle bundle, ICancellationSignal iCancellationSignal) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IContentProvider.descriptor);
            parcelObtain.writeString(str);
            uri.writeToParcel(parcelObtain, 0);
            parcelObtain.writeBundle(bundle);
            parcelObtain.writeStrongBinder(iCancellationSignal != null ? iCancellationSignal.asBinder() : null);
            this.mRemote.transact(27, parcelObtain, parcelObtain2, 0);
            DatabaseUtils.readExceptionFromParcel(parcelObtain2);
            return parcelObtain2.readInt() == 0;
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }
}
