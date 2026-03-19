package android.security;

import android.net.wifi.WifiEnterpriseConfig;
import android.os.Environment;
import android.os.FileUtils;
import com.android.internal.logging.nano.MetricsProto;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.KeyGenerator;
import libcore.io.IoUtils;

public class SystemKeyStore {
    private static final String KEY_FILE_EXTENSION = ".sks";
    private static final String SYSTEM_KEYSTORE_DIRECTORY = "misc/systemkeys";
    private static SystemKeyStore mInstance = new SystemKeyStore();

    private SystemKeyStore() {
    }

    public static SystemKeyStore getInstance() {
        return mInstance;
    }

    public static String toHexString(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        int length = bArr.length;
        StringBuilder sb = new StringBuilder(bArr.length * 2);
        for (byte b : bArr) {
            String string = Integer.toString(b & 255, 16);
            if (string.length() == 1) {
                string = WifiEnterpriseConfig.ENGINE_DISABLE + string;
            }
            sb.append(string);
        }
        return sb.toString();
    }

    public String generateNewKeyHexString(int i, String str, String str2) throws NoSuchAlgorithmException {
        return toHexString(generateNewKey(i, str, str2));
    }

    public byte[] generateNewKey(int i, String str, String str2) throws NoSuchAlgorithmException {
        File keyFile = getKeyFile(str2);
        if (keyFile.exists()) {
            throw new IllegalArgumentException();
        }
        KeyGenerator keyGenerator = KeyGenerator.getInstance(str);
        keyGenerator.init(i, SecureRandom.getInstance("SHA1PRNG"));
        byte[] encoded = keyGenerator.generateKey().getEncoded();
        try {
            if (!keyFile.createNewFile()) {
                throw new IllegalArgumentException();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(keyFile);
            fileOutputStream.write(encoded);
            fileOutputStream.flush();
            FileUtils.sync(fileOutputStream);
            fileOutputStream.close();
            FileUtils.setPermissions(keyFile.getName(), MetricsProto.MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION, -1, -1);
            return encoded;
        } catch (IOException e) {
            return null;
        }
    }

    private File getKeyFile(String str) {
        return new File(new File(Environment.getDataDirectory(), SYSTEM_KEYSTORE_DIRECTORY), str + KEY_FILE_EXTENSION);
    }

    public String retrieveKeyHexString(String str) throws IOException {
        return toHexString(retrieveKey(str));
    }

    public byte[] retrieveKey(String str) throws IOException {
        File keyFile = getKeyFile(str);
        if (!keyFile.exists()) {
            return null;
        }
        return IoUtils.readFileAsByteArray(keyFile.toString());
    }

    public void deleteKey(String str) {
        File keyFile = getKeyFile(str);
        if (!keyFile.exists()) {
            throw new IllegalArgumentException();
        }
        keyFile.delete();
    }
}
