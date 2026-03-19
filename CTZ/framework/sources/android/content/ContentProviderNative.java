package android.content;

import android.content.res.AssetFileDescriptor;
import android.database.BulkCursorDescriptor;
import android.database.Cursor;
import android.database.CursorToBulkCursorAdaptor;
import android.database.DatabaseUtils;
import android.database.IContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ICancellationSignal;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import java.util.ArrayList;

public abstract class ContentProviderNative extends Binder implements IContentProvider {
    public abstract String getProviderName();

    public ContentProviderNative() {
        attachInterface(this, IContentProvider.descriptor);
    }

    public static IContentProvider asInterface(IBinder iBinder) {
        if (iBinder == null) {
            return null;
        }
        IContentProvider iContentProvider = (IContentProvider) iBinder.queryLocalInterface(IContentProvider.descriptor);
        if (iContentProvider != null) {
            return iContentProvider;
        }
        return new ContentProviderProxy(iBinder);
    }

    @Override
    public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws Throwable {
        String[] strArr;
        try {
            if (i != 10) {
                int i3 = 0;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(IContentProvider.descriptor);
                        String string = parcel.readString();
                        Uri uriCreateFromParcel = Uri.CREATOR.createFromParcel(parcel);
                        int i4 = parcel.readInt();
                        CursorToBulkCursorAdaptor cursorToBulkCursorAdaptor = null;
                        if (i4 <= 0) {
                            strArr = null;
                        } else {
                            String[] strArr2 = new String[i4];
                            for (int i5 = 0; i5 < i4; i5++) {
                                strArr2[i5] = parcel.readString();
                            }
                            strArr = strArr2;
                        }
                        Bundle bundle = parcel.readBundle();
                        IContentObserver iContentObserverAsInterface = IContentObserver.Stub.asInterface(parcel.readStrongBinder());
                        Cursor cursorQuery = query(string, uriCreateFromParcel, strArr, bundle, ICancellationSignal.Stub.asInterface(parcel.readStrongBinder()));
                        if (cursorQuery != null) {
                            try {
                                CursorToBulkCursorAdaptor cursorToBulkCursorAdaptor2 = new CursorToBulkCursorAdaptor(cursorQuery, iContentObserverAsInterface, getProviderName());
                                try {
                                    BulkCursorDescriptor bulkCursorDescriptor = cursorToBulkCursorAdaptor2.getBulkCursorDescriptor();
                                    try {
                                        parcel2.writeNoException();
                                        parcel2.writeInt(1);
                                        bulkCursorDescriptor.writeToParcel(parcel2, 1);
                                    } catch (Throwable th) {
                                        th = th;
                                        cursorQuery = null;
                                        if (cursorToBulkCursorAdaptor != null) {
                                            cursorToBulkCursorAdaptor.close();
                                        }
                                        if (cursorQuery != null) {
                                            cursorQuery.close();
                                        }
                                        throw th;
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                    cursorQuery = null;
                                    cursorToBulkCursorAdaptor = cursorToBulkCursorAdaptor2;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                            }
                            break;
                        } else {
                            parcel2.writeNoException();
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 2:
                        parcel.enforceInterface(IContentProvider.descriptor);
                        String type = getType(Uri.CREATOR.createFromParcel(parcel));
                        parcel2.writeNoException();
                        parcel2.writeString(type);
                        return true;
                    case 3:
                        parcel.enforceInterface(IContentProvider.descriptor);
                        Uri uriInsert = insert(parcel.readString(), Uri.CREATOR.createFromParcel(parcel), ContentValues.CREATOR.createFromParcel(parcel));
                        parcel2.writeNoException();
                        Uri.writeToParcel(parcel2, uriInsert);
                        return true;
                    case 4:
                        parcel.enforceInterface(IContentProvider.descriptor);
                        int iDelete = delete(parcel.readString(), Uri.CREATOR.createFromParcel(parcel), parcel.readString(), parcel.readStringArray());
                        parcel2.writeNoException();
                        parcel2.writeInt(iDelete);
                        return true;
                    default:
                        switch (i) {
                            case 13:
                                parcel.enforceInterface(IContentProvider.descriptor);
                                int iBulkInsert = bulkInsert(parcel.readString(), Uri.CREATOR.createFromParcel(parcel), (ContentValues[]) parcel.createTypedArray(ContentValues.CREATOR));
                                parcel2.writeNoException();
                                parcel2.writeInt(iBulkInsert);
                                return true;
                            case 14:
                                parcel.enforceInterface(IContentProvider.descriptor);
                                ParcelFileDescriptor parcelFileDescriptorOpenFile = openFile(parcel.readString(), Uri.CREATOR.createFromParcel(parcel), parcel.readString(), ICancellationSignal.Stub.asInterface(parcel.readStrongBinder()), parcel.readStrongBinder());
                                parcel2.writeNoException();
                                if (parcelFileDescriptorOpenFile != null) {
                                    parcel2.writeInt(1);
                                    parcelFileDescriptorOpenFile.writeToParcel(parcel2, 1);
                                } else {
                                    parcel2.writeInt(0);
                                }
                                return true;
                            case 15:
                                parcel.enforceInterface(IContentProvider.descriptor);
                                AssetFileDescriptor assetFileDescriptorOpenAssetFile = openAssetFile(parcel.readString(), Uri.CREATOR.createFromParcel(parcel), parcel.readString(), ICancellationSignal.Stub.asInterface(parcel.readStrongBinder()));
                                parcel2.writeNoException();
                                if (assetFileDescriptorOpenAssetFile != null) {
                                    parcel2.writeInt(1);
                                    assetFileDescriptorOpenAssetFile.writeToParcel(parcel2, 1);
                                } else {
                                    parcel2.writeInt(0);
                                }
                                return true;
                            default:
                                switch (i) {
                                    case 20:
                                        parcel.enforceInterface(IContentProvider.descriptor);
                                        String string2 = parcel.readString();
                                        int i6 = parcel.readInt();
                                        ArrayList<ContentProviderOperation> arrayList = new ArrayList<>(i6);
                                        for (int i7 = 0; i7 < i6; i7++) {
                                            arrayList.add(i7, ContentProviderOperation.CREATOR.createFromParcel(parcel));
                                        }
                                        ContentProviderResult[] contentProviderResultArrApplyBatch = applyBatch(string2, arrayList);
                                        parcel2.writeNoException();
                                        parcel2.writeTypedArray(contentProviderResultArrApplyBatch, 0);
                                        return true;
                                    case 21:
                                        parcel.enforceInterface(IContentProvider.descriptor);
                                        Bundle bundleCall = call(parcel.readString(), parcel.readString(), parcel.readString(), parcel.readBundle());
                                        parcel2.writeNoException();
                                        parcel2.writeBundle(bundleCall);
                                        return true;
                                    case 22:
                                        parcel.enforceInterface(IContentProvider.descriptor);
                                        String[] streamTypes = getStreamTypes(Uri.CREATOR.createFromParcel(parcel), parcel.readString());
                                        parcel2.writeNoException();
                                        parcel2.writeStringArray(streamTypes);
                                        return true;
                                    case 23:
                                        parcel.enforceInterface(IContentProvider.descriptor);
                                        AssetFileDescriptor assetFileDescriptorOpenTypedAssetFile = openTypedAssetFile(parcel.readString(), Uri.CREATOR.createFromParcel(parcel), parcel.readString(), parcel.readBundle(), ICancellationSignal.Stub.asInterface(parcel.readStrongBinder()));
                                        parcel2.writeNoException();
                                        if (assetFileDescriptorOpenTypedAssetFile != null) {
                                            parcel2.writeInt(1);
                                            assetFileDescriptorOpenTypedAssetFile.writeToParcel(parcel2, 1);
                                        } else {
                                            parcel2.writeInt(0);
                                        }
                                        return true;
                                    case 24:
                                        parcel.enforceInterface(IContentProvider.descriptor);
                                        ICancellationSignal iCancellationSignalCreateCancellationSignal = createCancellationSignal();
                                        parcel2.writeNoException();
                                        parcel2.writeStrongBinder(iCancellationSignalCreateCancellationSignal.asBinder());
                                        return true;
                                    case 25:
                                        parcel.enforceInterface(IContentProvider.descriptor);
                                        Uri uriCanonicalize = canonicalize(parcel.readString(), Uri.CREATOR.createFromParcel(parcel));
                                        parcel2.writeNoException();
                                        Uri.writeToParcel(parcel2, uriCanonicalize);
                                        return true;
                                    case 26:
                                        parcel.enforceInterface(IContentProvider.descriptor);
                                        Uri uriUncanonicalize = uncanonicalize(parcel.readString(), Uri.CREATOR.createFromParcel(parcel));
                                        parcel2.writeNoException();
                                        Uri.writeToParcel(parcel2, uriUncanonicalize);
                                        return true;
                                    case 27:
                                        parcel.enforceInterface(IContentProvider.descriptor);
                                        boolean zRefresh = refresh(parcel.readString(), Uri.CREATOR.createFromParcel(parcel), parcel.readBundle(), ICancellationSignal.Stub.asInterface(parcel.readStrongBinder()));
                                        parcel2.writeNoException();
                                        if (!zRefresh) {
                                            i3 = -1;
                                        }
                                        parcel2.writeInt(i3);
                                        return true;
                                    default:
                                        return super.onTransact(i, parcel, parcel2, i2);
                                }
                        }
                }
            }
            parcel.enforceInterface(IContentProvider.descriptor);
            int iUpdate = update(parcel.readString(), Uri.CREATOR.createFromParcel(parcel), ContentValues.CREATOR.createFromParcel(parcel), parcel.readString(), parcel.readStringArray());
            parcel2.writeNoException();
            parcel2.writeInt(iUpdate);
            return true;
        } catch (Exception e) {
            DatabaseUtils.writeExceptionToParcel(parcel2, e);
            return true;
        }
    }

    @Override
    public IBinder asBinder() {
        return this;
    }
}
