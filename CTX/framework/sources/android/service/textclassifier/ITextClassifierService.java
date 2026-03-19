package android.service.textclassifier;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.service.textclassifier.ITextClassificationCallback;
import android.service.textclassifier.ITextLinksCallback;
import android.service.textclassifier.ITextSelectionCallback;
import android.view.textclassifier.SelectionEvent;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassificationContext;
import android.view.textclassifier.TextClassificationSessionId;
import android.view.textclassifier.TextLinks;
import android.view.textclassifier.TextSelection;

public interface ITextClassifierService extends IInterface {
    void onClassifyText(TextClassificationSessionId textClassificationSessionId, TextClassification.Request request, ITextClassificationCallback iTextClassificationCallback) throws RemoteException;

    void onCreateTextClassificationSession(TextClassificationContext textClassificationContext, TextClassificationSessionId textClassificationSessionId) throws RemoteException;

    void onDestroyTextClassificationSession(TextClassificationSessionId textClassificationSessionId) throws RemoteException;

    void onGenerateLinks(TextClassificationSessionId textClassificationSessionId, TextLinks.Request request, ITextLinksCallback iTextLinksCallback) throws RemoteException;

    void onSelectionEvent(TextClassificationSessionId textClassificationSessionId, SelectionEvent selectionEvent) throws RemoteException;

    void onSuggestSelection(TextClassificationSessionId textClassificationSessionId, TextSelection.Request request, ITextSelectionCallback iTextSelectionCallback) throws RemoteException;

    public static abstract class Stub extends Binder implements ITextClassifierService {
        private static final String DESCRIPTOR = "android.service.textclassifier.ITextClassifierService";
        static final int TRANSACTION_onClassifyText = 2;
        static final int TRANSACTION_onCreateTextClassificationSession = 5;
        static final int TRANSACTION_onDestroyTextClassificationSession = 6;
        static final int TRANSACTION_onGenerateLinks = 3;
        static final int TRANSACTION_onSelectionEvent = 4;
        static final int TRANSACTION_onSuggestSelection = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ITextClassifierService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof ITextClassifierService)) {
                return (ITextClassifierService) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            TextClassificationSessionId textClassificationSessionIdCreateFromParcel;
            TextClassificationSessionId textClassificationSessionIdCreateFromParcel2;
            TextClassificationSessionId textClassificationSessionIdCreateFromParcel3;
            TextClassificationSessionId textClassificationSessionIdCreateFromParcel4;
            TextClassificationContext textClassificationContextCreateFromParcel;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        textClassificationSessionIdCreateFromParcel = TextClassificationSessionId.CREATOR.createFromParcel(parcel);
                    } else {
                        textClassificationSessionIdCreateFromParcel = null;
                    }
                    onSuggestSelection(textClassificationSessionIdCreateFromParcel, parcel.readInt() != 0 ? TextSelection.Request.CREATOR.createFromParcel(parcel) : null, ITextSelectionCallback.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        textClassificationSessionIdCreateFromParcel2 = TextClassificationSessionId.CREATOR.createFromParcel(parcel);
                    } else {
                        textClassificationSessionIdCreateFromParcel2 = null;
                    }
                    onClassifyText(textClassificationSessionIdCreateFromParcel2, parcel.readInt() != 0 ? TextClassification.Request.CREATOR.createFromParcel(parcel) : null, ITextClassificationCallback.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        textClassificationSessionIdCreateFromParcel3 = TextClassificationSessionId.CREATOR.createFromParcel(parcel);
                    } else {
                        textClassificationSessionIdCreateFromParcel3 = null;
                    }
                    onGenerateLinks(textClassificationSessionIdCreateFromParcel3, parcel.readInt() != 0 ? TextLinks.Request.CREATOR.createFromParcel(parcel) : null, ITextLinksCallback.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        textClassificationSessionIdCreateFromParcel4 = TextClassificationSessionId.CREATOR.createFromParcel(parcel);
                    } else {
                        textClassificationSessionIdCreateFromParcel4 = null;
                    }
                    onSelectionEvent(textClassificationSessionIdCreateFromParcel4, parcel.readInt() != 0 ? SelectionEvent.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        textClassificationContextCreateFromParcel = TextClassificationContext.CREATOR.createFromParcel(parcel);
                    } else {
                        textClassificationContextCreateFromParcel = null;
                    }
                    onCreateTextClassificationSession(textClassificationContextCreateFromParcel, parcel.readInt() != 0 ? TextClassificationSessionId.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    onDestroyTextClassificationSession(parcel.readInt() != 0 ? TextClassificationSessionId.CREATOR.createFromParcel(parcel) : null);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements ITextClassifierService {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public void onSuggestSelection(TextClassificationSessionId textClassificationSessionId, TextSelection.Request request, ITextSelectionCallback iTextSelectionCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (textClassificationSessionId != null) {
                        parcelObtain.writeInt(1);
                        textClassificationSessionId.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (request != null) {
                        parcelObtain.writeInt(1);
                        request.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iTextSelectionCallback != null ? iTextSelectionCallback.asBinder() : null);
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onClassifyText(TextClassificationSessionId textClassificationSessionId, TextClassification.Request request, ITextClassificationCallback iTextClassificationCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (textClassificationSessionId != null) {
                        parcelObtain.writeInt(1);
                        textClassificationSessionId.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (request != null) {
                        parcelObtain.writeInt(1);
                        request.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iTextClassificationCallback != null ? iTextClassificationCallback.asBinder() : null);
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onGenerateLinks(TextClassificationSessionId textClassificationSessionId, TextLinks.Request request, ITextLinksCallback iTextLinksCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (textClassificationSessionId != null) {
                        parcelObtain.writeInt(1);
                        textClassificationSessionId.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (request != null) {
                        parcelObtain.writeInt(1);
                        request.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iTextLinksCallback != null ? iTextLinksCallback.asBinder() : null);
                    this.mRemote.transact(3, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onSelectionEvent(TextClassificationSessionId textClassificationSessionId, SelectionEvent selectionEvent) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (textClassificationSessionId != null) {
                        parcelObtain.writeInt(1);
                        textClassificationSessionId.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (selectionEvent != null) {
                        parcelObtain.writeInt(1);
                        selectionEvent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(4, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onCreateTextClassificationSession(TextClassificationContext textClassificationContext, TextClassificationSessionId textClassificationSessionId) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (textClassificationContext != null) {
                        parcelObtain.writeInt(1);
                        textClassificationContext.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (textClassificationSessionId != null) {
                        parcelObtain.writeInt(1);
                        textClassificationSessionId.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(5, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onDestroyTextClassificationSession(TextClassificationSessionId textClassificationSessionId) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (textClassificationSessionId != null) {
                        parcelObtain.writeInt(1);
                        textClassificationSessionId.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(6, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
