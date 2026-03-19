package android.telephony.ims.stub;

import android.annotation.SystemApi;
import android.os.Bundle;
import android.os.RemoteException;
import android.telephony.ims.ImsUtListener;
import com.android.ims.internal.IImsUt;
import com.android.ims.internal.IImsUtListener;

@SystemApi
public class ImsUtImplBase {
    private IImsUt.Stub mServiceImpl = new IImsUt.Stub() {
        @Override
        public void close() throws RemoteException {
            ImsUtImplBase.this.close();
        }

        @Override
        public int queryCallBarring(int i) throws RemoteException {
            return ImsUtImplBase.this.queryCallBarring(i);
        }

        @Override
        public int queryCallForward(int i, String str) throws RemoteException {
            return ImsUtImplBase.this.queryCallForward(i, str);
        }

        @Override
        public int queryCallWaiting() throws RemoteException {
            return ImsUtImplBase.this.queryCallWaiting();
        }

        @Override
        public int queryCLIR() throws RemoteException {
            return ImsUtImplBase.this.queryCLIR();
        }

        @Override
        public int queryCLIP() throws RemoteException {
            return ImsUtImplBase.this.queryCLIP();
        }

        @Override
        public int queryCOLR() throws RemoteException {
            return ImsUtImplBase.this.queryCOLR();
        }

        @Override
        public int queryCOLP() throws RemoteException {
            return ImsUtImplBase.this.queryCOLP();
        }

        @Override
        public int transact(Bundle bundle) throws RemoteException {
            return ImsUtImplBase.this.transact(bundle);
        }

        @Override
        public int updateCallBarring(int i, int i2, String[] strArr) throws RemoteException {
            return ImsUtImplBase.this.updateCallBarring(i, i2, strArr);
        }

        @Override
        public int updateCallForward(int i, int i2, String str, int i3, int i4) throws RemoteException {
            return ImsUtImplBase.this.updateCallForward(i, i2, str, i3, i4);
        }

        @Override
        public int updateCallWaiting(boolean z, int i) throws RemoteException {
            return ImsUtImplBase.this.updateCallWaiting(z, i);
        }

        @Override
        public int updateCLIR(int i) throws RemoteException {
            return ImsUtImplBase.this.updateCLIR(i);
        }

        @Override
        public int updateCLIP(boolean z) throws RemoteException {
            return ImsUtImplBase.this.updateCLIP(z);
        }

        @Override
        public int updateCOLR(int i) throws RemoteException {
            return ImsUtImplBase.this.updateCOLR(i);
        }

        @Override
        public int updateCOLP(boolean z) throws RemoteException {
            return ImsUtImplBase.this.updateCOLP(z);
        }

        @Override
        public void setListener(IImsUtListener iImsUtListener) throws RemoteException {
            ImsUtImplBase.this.setListener(new ImsUtListener(iImsUtListener));
        }

        @Override
        public int queryCallBarringForServiceClass(int i, int i2) throws RemoteException {
            return ImsUtImplBase.this.queryCallBarringForServiceClass(i, i2);
        }

        @Override
        public int updateCallBarringForServiceClass(int i, int i2, String[] strArr, int i3) throws RemoteException {
            return ImsUtImplBase.this.updateCallBarringForServiceClass(i, i2, strArr, i3);
        }
    };

    public void close() {
    }

    public int queryCallBarring(int i) {
        return -1;
    }

    public int queryCallBarringForServiceClass(int i, int i2) {
        return -1;
    }

    public int queryCallForward(int i, String str) {
        return -1;
    }

    public int queryCallWaiting() {
        return -1;
    }

    public int queryCLIR() {
        return queryClir();
    }

    public int queryCLIP() {
        return queryClip();
    }

    public int queryCOLR() {
        return queryColr();
    }

    public int queryCOLP() {
        return queryColp();
    }

    public int queryClir() {
        return -1;
    }

    public int queryClip() {
        return -1;
    }

    public int queryColr() {
        return -1;
    }

    public int queryColp() {
        return -1;
    }

    public int transact(Bundle bundle) {
        return -1;
    }

    public int updateCallBarring(int i, int i2, String[] strArr) {
        return -1;
    }

    public int updateCallBarringForServiceClass(int i, int i2, String[] strArr, int i3) {
        return -1;
    }

    public int updateCallForward(int i, int i2, String str, int i3, int i4) {
        return 0;
    }

    public int updateCallWaiting(boolean z, int i) {
        return -1;
    }

    public int updateCLIR(int i) {
        return updateClir(i);
    }

    public int updateCLIP(boolean z) {
        return updateClip(z);
    }

    public int updateCOLR(int i) {
        return updateColr(i);
    }

    public int updateCOLP(boolean z) {
        return updateColp(z);
    }

    public int updateClir(int i) {
        return -1;
    }

    public int updateClip(boolean z) {
        return -1;
    }

    public int updateColr(int i) {
        return -1;
    }

    public int updateColp(boolean z) {
        return -1;
    }

    public void setListener(ImsUtListener imsUtListener) {
    }

    public IImsUt getInterface() {
        return this.mServiceImpl;
    }
}
