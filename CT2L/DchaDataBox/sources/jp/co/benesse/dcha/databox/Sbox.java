package jp.co.benesse.dcha.databox;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Base64;
import java.io.File;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import jp.co.benesse.dcha.databox.ISbox;
import jp.co.benesse.dcha.util.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Sbox extends Service {
    public static final int COMMAND_WIPE_SBOX = 1;
    public static final String REQUEST_COMMAND_TYPE = "REQ_AUTH_COMMAND";
    public static final String SBOX_PATH = "data/data/jp.co.benesse.dcha.databox/.sbox";
    private static final String TAG = Sbox.class.getSimpleName();
    private ISbox.Stub mStub = new ISbox.Stub() {
        private final String[] APP_IDENTIFIER = {"4zgMD9wfE+SgNw0c9UUinhAcetm+F0zplycg/5eGCQSSGWDCUAxShnxRu8wpfKSN", "UMDSIs4KifjCaQV2WBCzlFmefCGePr8YrPb2a2fUM8uSGWDCUAxShnxRu8wpfKSN"};
        private final String[] AUTH_URL = {"xgJjoUvCuAJdOu9rWkNVZKir463KuvoNIrPt2KQ49ucvqJLSvIXMPnRHJdY5loSp", "fcVLyCXv8UXsdtWP71RhRcK9EF7WXqTK7RKU2wXDBKJTZN8Ofw1rtC002SFlP8s/"};

        @Override
        public String getStringValue(String key) throws RemoteException {
            Logger.d(Sbox.TAG, "getStringValue key:" + key);
            String ret = null;
            try {
                String jsonString = Sbox.getJSONString();
                if (jsonString != null) {
                    JSONObject rootJson = new JSONObject(jsonString);
                    String value = rootJson.getString(key);
                    if (value != null) {
                        ret = new String(value);
                    }
                }
            } catch (Exception e) {
                Logger.e(Sbox.TAG, "getStringValue Exception", e);
            }
            Logger.d(Sbox.TAG, "getStringValue value:" + ret);
            return ret;
        }

        @Override
        public void setStringValue(String key, String value) throws RemoteException {
            JSONObject rootJson;
            Logger.d(Sbox.TAG, "setStringValue key:" + key + " value:" + value);
            if (key == null && value == null) {
                throw new IllegalArgumentException();
            }
            try {
                String jsonString = Sbox.getJSONString();
                if (jsonString != null) {
                    try {
                        rootJson = new JSONObject(jsonString);
                    } catch (JSONException e) {
                        Logger.e(Sbox.TAG, "setStringValue JSONException", e);
                        rootJson = new JSONObject();
                    }
                } else {
                    rootJson = new JSONObject();
                }
                rootJson.putOpt(key, value);
                Sbox.setJSONString(rootJson.toString());
            } catch (Exception e2) {
                Logger.e(Sbox.TAG, "setStringValue Exception", e2);
            }
        }

        @Override
        public String getArrayValues(String key) throws RemoteException {
            Logger.d(Sbox.TAG, "getArrayValues key:" + key);
            String ret = null;
            try {
                String jsonString = Sbox.getJSONString();
                if (jsonString != null) {
                    JSONObject rootJson = new JSONObject(jsonString);
                    JSONArray values = rootJson.getJSONArray(key);
                    if (values != null) {
                        ret = values.toString();
                    }
                }
            } catch (Exception e) {
                Logger.e(Sbox.TAG, "getArrayValues Exception", e);
            }
            Logger.d(Sbox.TAG, "getArrayValues value:" + ret);
            return ret;
        }

        @Override
        public void setArrayValues(String key, String values) throws RemoteException {
            JSONObject rootJson;
            Logger.v(Sbox.TAG, "setArrayValues key:" + key + " values:" + values);
            if (key == null && values == null) {
                throw new IllegalArgumentException();
            }
            try {
                String jsonString = Sbox.getJSONString();
                if (jsonString != null) {
                    try {
                        rootJson = new JSONObject(jsonString);
                    } catch (JSONException e) {
                        Logger.e(Sbox.TAG, "setArrayValues JSONException", e);
                        rootJson = new JSONObject();
                    }
                } else {
                    rootJson = new JSONObject();
                }
                rootJson.putOpt(key, new JSONArray(values));
                Sbox.setJSONString(rootJson.toString());
            } catch (Exception e2) {
                Logger.e(Sbox.TAG, "setArrayValues Exception", e2);
            }
        }

        @Override
        public String getAppIdentifier(int serverType) throws RemoteException {
            try {
                String str = this.APP_IDENTIFIER[serverType];
                return Sbox.this.cipher(str);
            } catch (IndexOutOfBoundsException e) {
                Logger.e(Sbox.TAG, "getAppID IndexOutOfBoundsException", e);
                throw new RemoteException();
            }
        }

        @Override
        public String getAuthUrl(int serverType) throws RemoteException {
            try {
                String str = this.AUTH_URL[serverType];
                return Sbox.this.cipher(str);
            } catch (IndexOutOfBoundsException e) {
                Logger.e(Sbox.TAG, "getAppID IndexOutOfBoundsException", e);
                throw new RemoteException();
            }
        }
    };

    private static native String getJSONString();

    private static native int setJSONString(String str);

    static {
        System.loadLibrary("sbox");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mStub;
    }

    protected byte[] getSignatures() throws RemoteException {
        PackageManager pm = getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(getPackageName(), 64);
            byte[] byteArray = packageInfo.signatures[0].toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(byteArray);
            byte[] valueArray = md.digest();
            return valueArray;
        } catch (Exception e) {
            Logger.e(TAG, "getSignatures Exception", e);
            throw new RemoteException();
        }
    }

    private String cipher(String str) throws RemoteException {
        try {
            byte[] kagi = getSignatures();
            SecretKeySpec sks = new SecretKeySpec(kagi, "AES");
            byte[] input = Base64.decode(str.getBytes(), 0);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(2, sks);
            String ret = new String(cipher.doFinal(input));
            return ret;
        } catch (Exception e) {
            Logger.e(TAG, "getAppID Exception", e);
            throw new RemoteException();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int command = intent.getIntExtra(REQUEST_COMMAND_TYPE, 0);
            switch (command) {
                case COMMAND_WIPE_SBOX:
                    try {
                        File file = new File(SBOX_PATH);
                        if (file.exists()) {
                            file.delete();
                        }
                    } catch (Exception e) {
                        Logger.e(TAG, "onStartCommand Exception", e);
                    }
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
