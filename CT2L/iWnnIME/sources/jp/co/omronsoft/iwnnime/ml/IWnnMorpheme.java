package jp.co.omronsoft.iwnnime.ml;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import jp.co.omronsoft.iwnnime.ml.IMorphemeService;

public class IWnnMorpheme {
    private static final String IWNNMORPHEMESERVICE_CLASSNAME = "jp.co.omronsoft.iwnnime.ml.IWnnMorphemeService";
    private static final String IWNNMORPHEMESERVICE_PACKAGENAME = "jp.co.omronsoft.iwnnime.ml";
    private boolean mIsBind;
    private IMorphemeService mServiceIf = null;
    private Context mContext = null;
    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IWnnMorpheme.this.mServiceIf = IMorphemeService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            IWnnMorpheme.this.mServiceIf = null;
        }
    };

    public boolean connect(Context context) {
        if (context == null) {
            return false;
        }
        Intent intent = new Intent();
        intent.setClassName(IWNNMORPHEMESERVICE_PACKAGENAME, IWNNMORPHEMESERVICE_CLASSNAME);
        boolean success = context.bindService(intent, this.mServiceConn, 1);
        if (success) {
            this.mIsBind = true;
            this.mContext = context;
            return success;
        }
        return success;
    }

    protected void finalize() throws Throwable {
        disconnect();
        super.finalize();
    }

    public Bundle splitWord(String input, int readingsMax) {
        try {
            if (this.mServiceIf != null) {
                return this.mServiceIf.splitWord(input, readingsMax);
            }
        } catch (Exception e) {
            Log.e("IWnnMorpheme", "splitWord", e);
        }
        return null;
    }

    public void disconnect() {
        if (this.mIsBind && this.mContext != null && this.mServiceConn != null) {
            this.mContext.unbindService(this.mServiceConn);
            this.mIsBind = false;
        }
    }
}
