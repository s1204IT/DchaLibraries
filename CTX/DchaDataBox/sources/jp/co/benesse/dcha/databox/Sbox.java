package jp.co.benesse.dcha.databox;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Base64;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import jp.co.benesse.dcha.databox.ISbox;
import jp.co.benesse.dcha.util.Logger;

public class Sbox extends Service {
    protected SboxProviderAdapter mSboxAdapter;
    private ISbox.Stub mStub = new ISbox.Stub() {
        @Override
        public String getStringValue(String str) throws Throwable {
            Logger.d(Sbox.TAG, "getStringValue key:", str);
            String value = Sbox.this.mSboxAdapter.getValue(Sbox.this.getContentResolver(), str);
            Logger.d(Sbox.TAG, "getStringValue value:", value);
            return value;
        }

        @Override
        public void setStringValue(String str, String str2) {
            Logger.d(Sbox.TAG, "setStringValue key:", str, "value:", str2);
            if (str == null || str2 == null) {
                throw new IllegalArgumentException();
            }
            Sbox.this.mSboxAdapter.setValue(Sbox.this.getContentResolver(), str, str2);
        }

        @Override
        public String getArrayValues(String str) throws Throwable {
            Logger.d(Sbox.TAG, "getArrayValues key:", str);
            String value = Sbox.this.mSboxAdapter.getValue(Sbox.this.getContentResolver(), str);
            Logger.d(Sbox.TAG, "getArrayValues value:", value);
            return value;
        }

        @Override
        public void setArrayValues(String str, String str2) {
            Logger.v(Sbox.TAG, "setArrayValues key:", str, "values:", str2);
            if (str == null || str2 == null) {
                throw new IllegalArgumentException();
            }
            Sbox.this.mSboxAdapter.setValue(Sbox.this.getContentResolver(), str, str2);
        }

        @Override
        public String getAppIdentifier(int i) throws RemoteException {
            try {
                return Sbox.this.cipher(Sbox.APP_IDENTIFIER[i]);
            } catch (IndexOutOfBoundsException e) {
                Logger.e(Sbox.TAG, "getAppIdentifier IndexOutOfBoundsException", e);
                throw new RemoteException();
            }
        }

        @Override
        public String getAuthUrl(int i) throws RemoteException {
            try {
                return Sbox.this.cipher(Sbox.AUTH_URL[i]);
            } catch (IndexOutOfBoundsException e) {
                Logger.e(Sbox.TAG, "getAuthUrl IndexOutOfBoundsException", e);
                throw new RemoteException();
            }
        }
    };
    private static final String TAG = Sbox.class.getSimpleName();
    private static final String[] APP_IDENTIFIER = {"4zgMD9wfE+SgNw0c9UUinhAcetm+F0zplycg/5eGCQSSGWDCUAxShnxRu8wpfKSN", "UMDSIs4KifjCaQV2WBCzlFmefCGePr8YrPb2a2fUM8uSGWDCUAxShnxRu8wpfKSN"};
    private static final String[] AUTH_URL = {"51IcXZDWzq3rX/FMI7vdvPhf7nDsvH6lkGEzLpfLSo8kXH9q4HtJdjFo3/EQLS8U", "EcAMOmmDvWdNMj40+2EnOFYnYar+QfSzeN3/XwXOPCpT4IXeeIXfV15CNciqyUZ4khlgwlAMUoZ8UbvMKXykjQ=="};

    @Override
    public IBinder onBind(Intent intent) {
        return this.mStub;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mSboxAdapter = new SboxProviderAdapter();
    }

    protected byte[] getSignatures() throws RemoteException {
        try {
            byte[] byteArray = getPackageManager().getPackageInfo(getPackageName(), 64).signatures[0].toByteArray();
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(byteArray);
            return messageDigest.digest();
        } catch (Exception e) {
            Logger.e(TAG, "getSignatures Exception", e);
            throw new RemoteException();
        }
    }

    public String cipher(String str) throws RemoteException {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(getSignatures(), "AES");
            byte[] bArrDecode = Base64.decode(str.getBytes(), 0);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(2, secretKeySpec);
            return new String(cipher.doFinal(bArrDecode));
        } catch (Exception e) {
            Logger.e(TAG, "cipher Exception", e);
            throw new RemoteException();
        }
    }
}
