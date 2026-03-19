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

    public static IContentProvider asInterface(IBinder obj) {
        if (obj == null) {
            return null;
        }
        IContentProvider in = (IContentProvider) obj.queryLocalInterface(IContentProvider.descriptor);
        if (in != null) {
            return in;
        }
        return new ContentProviderProxy(obj);
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws Exception {
        CursorToBulkCursorAdaptor adaptor;
        try {
            switch (code) {
                case 1:
                    data.enforceInterface(IContentProvider.descriptor);
                    String callingPkg = data.readString();
                    Uri url = Uri.CREATOR.createFromParcel(data);
                    int num = data.readInt();
                    String[] projection = null;
                    if (num > 0) {
                        projection = new String[num];
                        for (int i = 0; i < num; i++) {
                            projection[i] = data.readString();
                        }
                    }
                    String selection = data.readString();
                    int num2 = data.readInt();
                    String[] selectionArgs = null;
                    if (num2 > 0) {
                        selectionArgs = new String[num2];
                        for (int i2 = 0; i2 < num2; i2++) {
                            selectionArgs[i2] = data.readString();
                        }
                    }
                    String sortOrder = data.readString();
                    IContentObserver observer = IContentObserver.Stub.asInterface(data.readStrongBinder());
                    ICancellationSignal cancellationSignal = ICancellationSignal.Stub.asInterface(data.readStrongBinder());
                    Cursor cursor = query(callingPkg, url, projection, selection, selectionArgs, sortOrder, cancellationSignal);
                    if (cursor != null) {
                        CursorToBulkCursorAdaptor adaptor2 = null;
                        try {
                            adaptor = new CursorToBulkCursorAdaptor(cursor, observer, getProviderName());
                            cursor = null;
                        } catch (Throwable th) {
                            th = th;
                        }
                        try {
                            BulkCursorDescriptor d = adaptor.getBulkCursorDescriptor();
                            adaptor2 = null;
                            reply.writeNoException();
                            reply.writeInt(1);
                            d.writeToParcel(reply, 1);
                            return true;
                        } catch (Throwable th2) {
                            th = th2;
                            adaptor2 = adaptor;
                            if (adaptor2 != null) {
                                adaptor2.close();
                            }
                            if (cursor != null) {
                                cursor.close();
                            }
                            throw th;
                        }
                    }
                    reply.writeNoException();
                    reply.writeInt(0);
                    return true;
                case 2:
                    data.enforceInterface(IContentProvider.descriptor);
                    Uri url2 = Uri.CREATOR.createFromParcel(data);
                    String type = getType(url2);
                    reply.writeNoException();
                    reply.writeString(type);
                    return true;
                case 3:
                    data.enforceInterface(IContentProvider.descriptor);
                    String callingPkg2 = data.readString();
                    Uri url3 = Uri.CREATOR.createFromParcel(data);
                    ContentValues values = ContentValues.CREATOR.createFromParcel(data);
                    Uri out = insert(callingPkg2, url3, values);
                    reply.writeNoException();
                    Uri.writeToParcel(reply, out);
                    return true;
                case 4:
                    data.enforceInterface(IContentProvider.descriptor);
                    String callingPkg3 = data.readString();
                    Uri url4 = Uri.CREATOR.createFromParcel(data);
                    String selection2 = data.readString();
                    String[] selectionArgs2 = data.readStringArray();
                    int count = delete(callingPkg3, url4, selection2, selectionArgs2);
                    reply.writeNoException();
                    reply.writeInt(count);
                    return true;
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 11:
                case 12:
                case 16:
                case 17:
                case 18:
                case 19:
                default:
                    return super.onTransact(code, data, reply, flags);
                case 10:
                    data.enforceInterface(IContentProvider.descriptor);
                    String callingPkg4 = data.readString();
                    Uri url5 = Uri.CREATOR.createFromParcel(data);
                    ContentValues values2 = ContentValues.CREATOR.createFromParcel(data);
                    String selection3 = data.readString();
                    String[] selectionArgs3 = data.readStringArray();
                    int count2 = update(callingPkg4, url5, values2, selection3, selectionArgs3);
                    reply.writeNoException();
                    reply.writeInt(count2);
                    return true;
                case 13:
                    data.enforceInterface(IContentProvider.descriptor);
                    String callingPkg5 = data.readString();
                    Uri url6 = Uri.CREATOR.createFromParcel(data);
                    ContentValues[] values3 = (ContentValues[]) data.createTypedArray(ContentValues.CREATOR);
                    int count3 = bulkInsert(callingPkg5, url6, values3);
                    reply.writeNoException();
                    reply.writeInt(count3);
                    return true;
                case 14:
                    data.enforceInterface(IContentProvider.descriptor);
                    String callingPkg6 = data.readString();
                    Uri url7 = Uri.CREATOR.createFromParcel(data);
                    String mode = data.readString();
                    ICancellationSignal signal = ICancellationSignal.Stub.asInterface(data.readStrongBinder());
                    IBinder callerToken = data.readStrongBinder();
                    ParcelFileDescriptor fd = openFile(callingPkg6, url7, mode, signal, callerToken);
                    reply.writeNoException();
                    if (fd != null) {
                        reply.writeInt(1);
                        fd.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 15:
                    data.enforceInterface(IContentProvider.descriptor);
                    String callingPkg7 = data.readString();
                    Uri url8 = Uri.CREATOR.createFromParcel(data);
                    String mode2 = data.readString();
                    ICancellationSignal signal2 = ICancellationSignal.Stub.asInterface(data.readStrongBinder());
                    AssetFileDescriptor fd2 = openAssetFile(callingPkg7, url8, mode2, signal2);
                    reply.writeNoException();
                    if (fd2 != null) {
                        reply.writeInt(1);
                        fd2.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 20:
                    data.enforceInterface(IContentProvider.descriptor);
                    String callingPkg8 = data.readString();
                    int numOperations = data.readInt();
                    ArrayList<ContentProviderOperation> operations = new ArrayList<>(numOperations);
                    for (int i3 = 0; i3 < numOperations; i3++) {
                        operations.add(i3, ContentProviderOperation.CREATOR.createFromParcel(data));
                    }
                    ContentProviderResult[] results = applyBatch(callingPkg8, operations);
                    reply.writeNoException();
                    reply.writeTypedArray(results, 0);
                    return true;
                case 21:
                    data.enforceInterface(IContentProvider.descriptor);
                    String callingPkg9 = data.readString();
                    String method = data.readString();
                    String stringArg = data.readString();
                    Bundle args = data.readBundle();
                    Bundle responseBundle = call(callingPkg9, method, stringArg, args);
                    reply.writeNoException();
                    reply.writeBundle(responseBundle);
                    return true;
                case 22:
                    data.enforceInterface(IContentProvider.descriptor);
                    Uri url9 = Uri.CREATOR.createFromParcel(data);
                    String mimeTypeFilter = data.readString();
                    String[] types = getStreamTypes(url9, mimeTypeFilter);
                    reply.writeNoException();
                    reply.writeStringArray(types);
                    return true;
                case 23:
                    data.enforceInterface(IContentProvider.descriptor);
                    String callingPkg10 = data.readString();
                    Uri url10 = Uri.CREATOR.createFromParcel(data);
                    String mimeType = data.readString();
                    Bundle opts = data.readBundle();
                    ICancellationSignal signal3 = ICancellationSignal.Stub.asInterface(data.readStrongBinder());
                    AssetFileDescriptor fd3 = openTypedAssetFile(callingPkg10, url10, mimeType, opts, signal3);
                    reply.writeNoException();
                    if (fd3 != null) {
                        reply.writeInt(1);
                        fd3.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 24:
                    data.enforceInterface(IContentProvider.descriptor);
                    ICancellationSignal cancellationSignal2 = createCancellationSignal();
                    reply.writeNoException();
                    reply.writeStrongBinder(cancellationSignal2.asBinder());
                    return true;
                case 25:
                    data.enforceInterface(IContentProvider.descriptor);
                    String callingPkg11 = data.readString();
                    Uri url11 = Uri.CREATOR.createFromParcel(data);
                    Uri out2 = canonicalize(callingPkg11, url11);
                    reply.writeNoException();
                    Uri.writeToParcel(reply, out2);
                    return true;
                case 26:
                    data.enforceInterface(IContentProvider.descriptor);
                    String callingPkg12 = data.readString();
                    Uri url12 = Uri.CREATOR.createFromParcel(data);
                    Uri out3 = uncanonicalize(callingPkg12, url12);
                    reply.writeNoException();
                    Uri.writeToParcel(reply, out3);
                    return true;
            }
        } catch (Exception e) {
            DatabaseUtils.writeExceptionToParcel(reply, e);
            return true;
        }
    }

    @Override
    public IBinder asBinder() {
        return this;
    }
}
