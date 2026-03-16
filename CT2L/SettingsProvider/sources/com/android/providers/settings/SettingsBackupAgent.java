package com.android.providers.settings;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FullBackupDataOutput;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.FileUtils;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.CRC32;
import libcore.io.IoUtils;

public class SettingsBackupAgent extends BackupAgentHelper {
    private static String mWifiConfigFile;
    private SettingsHelper mSettingsHelper;
    private WifiManager mWfm;
    WifiRestoreRunnable mWifiRestore = null;
    private static final int[] STATE_SIZES = {0, 4, 5, 6};
    private static final byte[] EMPTY_DATA = new byte[0];
    private static final String[] PROJECTION = {"_id", "name", "value"};

    static class Network {
        String ssid = "";
        String key_mgmt = "";
        boolean certUsed = false;
        boolean hasWepKey = false;
        final ArrayList<String> rawLines = new ArrayList<>();

        Network() {
        }

        public static Network readFromStream(BufferedReader in) {
            String line;
            Network n = new Network();
            while (in.ready() && (line = in.readLine()) != null && !line.startsWith("}")) {
                try {
                    n.rememberLine(line);
                } catch (IOException e) {
                    return null;
                }
            }
            return n;
        }

        void rememberLine(String line) {
            String line2 = line.trim();
            if (!line2.isEmpty()) {
                this.rawLines.add(line2);
                if (line2.startsWith("ssid=")) {
                    this.ssid = line2;
                    return;
                }
                if (line2.startsWith("key_mgmt=")) {
                    this.key_mgmt = line2;
                    return;
                }
                if (line2.startsWith("client_cert=")) {
                    this.certUsed = true;
                    return;
                }
                if (line2.startsWith("ca_cert=")) {
                    this.certUsed = true;
                } else if (line2.startsWith("ca_path=")) {
                    this.certUsed = true;
                } else if (line2.startsWith("wep_")) {
                    this.hasWepKey = true;
                }
            }
        }

        public void write(Writer w) throws IOException {
            w.write("\nnetwork={\n");
            for (String line : this.rawLines) {
                w.write("\t" + line + "\n");
            }
            w.write("}\n");
        }

        public String configKey() {
            if (this.ssid == null) {
                return null;
            }
            String bareSsid = this.ssid.substring(this.ssid.indexOf(61) + 1);
            BitSet types = new BitSet();
            if (this.key_mgmt == null) {
                types.set(1);
                types.set(2);
            } else {
                String bareKeyMgmt = this.key_mgmt.substring(this.key_mgmt.indexOf(61) + 1);
                String[] typeStrings = bareKeyMgmt.split("\\s+");
                for (String ktype : typeStrings) {
                    if (ktype.equals("WPA-PSK")) {
                        Log.v("SettingsBackupAgent", "  + setting WPA_PSK bit");
                        types.set(1);
                    } else if (ktype.equals("WPA-EAP")) {
                        Log.v("SettingsBackupAgent", "  + setting WPA_EAP bit");
                        types.set(2);
                    } else if (ktype.equals("IEEE8021X")) {
                        Log.v("SettingsBackupAgent", "  + setting IEEE8021X bit");
                        types.set(3);
                    }
                }
            }
            if (types.get(1)) {
                return bareSsid + WifiConfiguration.KeyMgmt.strings[1];
            }
            if (types.get(2) || types.get(3)) {
                return bareSsid + WifiConfiguration.KeyMgmt.strings[2];
            }
            if (this.hasWepKey) {
                return bareSsid + "WEP";
            }
            return bareSsid + WifiConfiguration.KeyMgmt.strings[0];
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Network)) {
                return false;
            }
            try {
                Network other = (Network) o;
                return this.ssid.equals(other.ssid) && this.key_mgmt.equals(other.key_mgmt);
            } catch (ClassCastException e) {
                return false;
            }
        }

        public int hashCode() {
            int result = this.ssid.hashCode() + 527;
            return (result * 31) + this.key_mgmt.hashCode();
        }
    }

    boolean networkInWhitelist(Network net, List<WifiConfiguration> whitelist) {
        String netConfigKey = net.configKey();
        int N = whitelist.size();
        for (int i = 0; i < N; i++) {
            if (Objects.equals(netConfigKey, whitelist.get(i).configKey(true))) {
                return true;
            }
        }
        return false;
    }

    class WifiNetworkSettings {
        final HashSet<Network> mKnownNetworks = new HashSet<>();
        final ArrayList<Network> mNetworks = new ArrayList<>(8);

        WifiNetworkSettings() {
        }

        public void readNetworks(BufferedReader in, List<WifiConfiguration> whitelist) {
            while (in.ready()) {
                try {
                    String line = in.readLine();
                    if (line != null && line.startsWith("network")) {
                        Network net = Network.readFromStream(in);
                        if (whitelist == null || SettingsBackupAgent.this.networkInWhitelist(net, whitelist)) {
                            if (!this.mKnownNetworks.contains(net)) {
                                this.mKnownNetworks.add(net);
                                this.mNetworks.add(net);
                            }
                        }
                    }
                } catch (IOException e) {
                    return;
                }
            }
        }

        public void write(Writer w) throws IOException {
            for (Network net : this.mNetworks) {
                if (!net.certUsed) {
                    net.write(w);
                }
            }
        }
    }

    @Override
    public void onCreate() {
        this.mSettingsHelper = new SettingsHelper(this);
        super.onCreate();
        WifiManager mWfm = (WifiManager) getSystemService("wifi");
        if (mWfm != null) {
            mWifiConfigFile = mWfm.getConfigFile();
        }
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws Throwable {
        byte[] systemSettingsData = getSystemSettings();
        byte[] secureSettingsData = getSecureSettings();
        byte[] globalSettingsData = getGlobalSettings();
        byte[] locale = this.mSettingsHelper.getLocaleData();
        byte[] wifiSupplicantData = getWifiSupplicant("/data/misc/wifi/wpa_supplicant.conf");
        byte[] wifiConfigData = getFileData(mWifiConfigFile);
        long[] stateChecksums = readOldChecksums(oldState);
        stateChecksums[0] = writeIfChanged(stateChecksums[0], "system", systemSettingsData, data);
        stateChecksums[1] = writeIfChanged(stateChecksums[1], "secure", secureSettingsData, data);
        stateChecksums[5] = writeIfChanged(stateChecksums[5], "global", globalSettingsData, data);
        stateChecksums[2] = writeIfChanged(stateChecksums[2], "locale", locale, data);
        stateChecksums[3] = writeIfChanged(stateChecksums[3], "￭WIFI", wifiSupplicantData, data);
        stateChecksums[4] = writeIfChanged(stateChecksums[4], "￭CONFIG_WIFI", wifiConfigData, data);
        writeNewChecksums(stateChecksums, newState);
    }

    class WifiRestoreRunnable implements Runnable {
        private byte[] restoredSupplicantData;
        private byte[] restoredWifiConfigFile;

        WifiRestoreRunnable() {
        }

        void incorporateWifiSupplicant(BackupDataInput data) {
            this.restoredSupplicantData = new byte[data.getDataSize()];
            if (this.restoredSupplicantData.length > 0) {
                try {
                    data.readEntityData(this.restoredSupplicantData, 0, data.getDataSize());
                } catch (IOException e) {
                    Log.w("SettingsBackupAgent", "Unable to read supplicant data");
                    this.restoredSupplicantData = null;
                }
            }
        }

        void incorporateWifiConfigFile(BackupDataInput data) {
            this.restoredWifiConfigFile = new byte[data.getDataSize()];
            if (this.restoredWifiConfigFile.length > 0) {
                try {
                    data.readEntityData(this.restoredWifiConfigFile, 0, data.getDataSize());
                } catch (IOException e) {
                    Log.w("SettingsBackupAgent", "Unable to read config file");
                    this.restoredWifiConfigFile = null;
                }
            }
        }

        @Override
        public void run() {
            if (this.restoredSupplicantData != null || this.restoredWifiConfigFile != null) {
                ContentResolver cr = SettingsBackupAgent.this.getContentResolver();
                int scanAlways = Settings.Global.getInt(cr, "wifi_scan_always_enabled", 0);
                int retainedWifiState = SettingsBackupAgent.this.enableWifi(false);
                if (scanAlways != 0) {
                    Settings.Global.putInt(cr, "wifi_scan_always_enabled", 0);
                }
                for (int loop = 60; loop > 0; loop--) {
                    try {
                        if (SettingsBackupAgent.this.mWfm == null) {
                            SettingsBackupAgent.this.mWfm = (WifiManager) SettingsBackupAgent.this.getSystemService("wifi");
                        }
                        if (SettingsBackupAgent.this.mWfm != null && SettingsBackupAgent.this.mWfm.getWifiState() == 1) {
                            break;
                        }
                        Thread.sleep(500L);
                    } catch (InterruptedException e) {
                    }
                }
                if (this.restoredSupplicantData != null) {
                    SettingsBackupAgent.this.restoreWifiSupplicant("/data/misc/wifi/wpa_supplicant.conf", this.restoredSupplicantData, this.restoredSupplicantData.length);
                    FileUtils.setPermissions("/data/misc/wifi/wpa_supplicant.conf", 432, Process.myUid(), 1010);
                }
                if (this.restoredWifiConfigFile != null) {
                    SettingsBackupAgent.this.restoreFileData(SettingsBackupAgent.mWifiConfigFile, this.restoredWifiConfigFile, this.restoredWifiConfigFile.length);
                }
                if (scanAlways != 0) {
                    Settings.Global.putInt(cr, "wifi_scan_always_enabled", scanAlways);
                }
                SettingsBackupAgent.this.enableWifi(retainedWifiState == 3 || retainedWifiState == 2);
            }
        }
    }

    void initWifiRestoreIfNecessary() {
        if (this.mWifiRestore == null) {
            this.mWifiRestore = new WifiRestoreRunnable();
        }
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
        HashSet<String> movedToGlobal = new HashSet<>();
        Settings.System.getMovedKeys(movedToGlobal);
        Settings.Secure.getMovedKeys(movedToGlobal);
        while (data.readNextHeader()) {
            String key = data.getKey();
            int size = data.getDataSize();
            if ("system".equals(key)) {
                restoreSettings(data, Settings.System.CONTENT_URI, movedToGlobal);
                this.mSettingsHelper.applyAudioSettings();
            } else if ("secure".equals(key)) {
                restoreSettings(data, Settings.Secure.CONTENT_URI, movedToGlobal);
            } else if ("global".equals(key)) {
                restoreSettings(data, Settings.Global.CONTENT_URI, null);
            } else if ("￭WIFI".equals(key)) {
                initWifiRestoreIfNecessary();
                this.mWifiRestore.incorporateWifiSupplicant(data);
            } else if ("locale".equals(key)) {
                byte[] localeData = new byte[size];
                data.readEntityData(localeData, 0, size);
                this.mSettingsHelper.setLocaleData(localeData, size);
            } else if ("￭CONFIG_WIFI".equals(key)) {
                initWifiRestoreIfNecessary();
                this.mWifiRestore.incorporateWifiConfigFile(data);
            } else {
                data.skipEntityData();
            }
        }
        if (this.mWifiRestore != null) {
            long wifiBounceDelayMillis = Settings.Global.getLong(getContentResolver(), "wifi_bounce_delay_override_ms", 60000L);
            new Handler(getMainLooper()).postDelayed(this.mWifiRestore, wifiBounceDelayMillis);
        }
    }

    @Override
    public void onFullBackup(FullBackupDataOutput data) throws Throwable {
        byte[] systemSettingsData = getSystemSettings();
        byte[] secureSettingsData = getSecureSettings();
        byte[] globalSettingsData = getGlobalSettings();
        byte[] locale = this.mSettingsHelper.getLocaleData();
        byte[] wifiSupplicantData = getWifiSupplicant("/data/misc/wifi/wpa_supplicant.conf");
        byte[] wifiConfigData = getFileData(mWifiConfigFile);
        String root = getFilesDir().getAbsolutePath();
        File stage = new File(root, "flattened-data");
        try {
            FileOutputStream filestream = new FileOutputStream(stage);
            BufferedOutputStream bufstream = new BufferedOutputStream(filestream);
            DataOutputStream out = new DataOutputStream(bufstream);
            out.writeInt(2);
            out.writeInt(systemSettingsData.length);
            out.write(systemSettingsData);
            out.writeInt(secureSettingsData.length);
            out.write(secureSettingsData);
            out.writeInt(globalSettingsData.length);
            out.write(globalSettingsData);
            out.writeInt(locale.length);
            out.write(locale);
            out.writeInt(wifiSupplicantData.length);
            out.write(wifiSupplicantData);
            out.writeInt(wifiConfigData.length);
            out.write(wifiConfigData);
            out.flush();
            fullBackupFile(stage, data);
        } finally {
            stage.delete();
        }
    }

    public void onRestoreFile(ParcelFileDescriptor data, long size, int type, String domain, String relpath, long mode, long mtime) throws IOException {
        FileInputStream instream = new FileInputStream(data.getFileDescriptor());
        DataInputStream in = new DataInputStream(instream);
        int version = in.readInt();
        if (version <= 2) {
            HashSet<String> movedToGlobal = new HashSet<>();
            Settings.System.getMovedKeys(movedToGlobal);
            Settings.Secure.getMovedKeys(movedToGlobal);
            int nBytes = in.readInt();
            byte[] buffer = new byte[nBytes];
            in.readFully(buffer, 0, nBytes);
            restoreSettings(buffer, nBytes, Settings.System.CONTENT_URI, movedToGlobal);
            int nBytes2 = in.readInt();
            if (nBytes2 > buffer.length) {
                buffer = new byte[nBytes2];
            }
            in.readFully(buffer, 0, nBytes2);
            restoreSettings(buffer, nBytes2, Settings.Secure.CONTENT_URI, movedToGlobal);
            if (version >= 2) {
                int nBytes3 = in.readInt();
                if (nBytes3 > buffer.length) {
                    buffer = new byte[nBytes3];
                }
                in.readFully(buffer, 0, nBytes3);
                movedToGlobal.clear();
                restoreSettings(buffer, nBytes3, Settings.Global.CONTENT_URI, movedToGlobal);
            }
            int nBytes4 = in.readInt();
            if (nBytes4 > buffer.length) {
                buffer = new byte[nBytes4];
            }
            in.readFully(buffer, 0, nBytes4);
            this.mSettingsHelper.setLocaleData(buffer, nBytes4);
            int nBytes5 = in.readInt();
            if (nBytes5 > buffer.length) {
                buffer = new byte[nBytes5];
            }
            in.readFully(buffer, 0, nBytes5);
            int retainedWifiState = enableWifi(false);
            restoreWifiSupplicant("/data/misc/wifi/wpa_supplicant.conf", buffer, nBytes5);
            FileUtils.setPermissions("/data/misc/wifi/wpa_supplicant.conf", 432, Process.myUid(), 1010);
            enableWifi(retainedWifiState == 3 || retainedWifiState == 2);
            int nBytes6 = in.readInt();
            if (nBytes6 > buffer.length) {
                buffer = new byte[nBytes6];
            }
            in.readFully(buffer, 0, nBytes6);
            restoreFileData(mWifiConfigFile, buffer, nBytes6);
            return;
        }
        data.close();
        throw new IOException("Invalid file schema");
    }

    private long[] readOldChecksums(ParcelFileDescriptor oldState) throws IOException {
        long[] stateChecksums = new long[6];
        DataInputStream dataInput = new DataInputStream(new FileInputStream(oldState.getFileDescriptor()));
        try {
            int stateVersion = dataInput.readInt();
            for (int i = 0; i < STATE_SIZES[stateVersion]; i++) {
                stateChecksums[i] = dataInput.readLong();
            }
        } catch (EOFException e) {
        }
        dataInput.close();
        return stateChecksums;
    }

    private void writeNewChecksums(long[] checksums, ParcelFileDescriptor newState) throws IOException {
        DataOutputStream dataOutput = new DataOutputStream(new FileOutputStream(newState.getFileDescriptor()));
        dataOutput.writeInt(3);
        for (int i = 0; i < 6; i++) {
            dataOutput.writeLong(checksums[i]);
        }
        dataOutput.close();
    }

    private long writeIfChanged(long oldChecksum, String key, byte[] data, BackupDataOutput output) {
        CRC32 checkSummer = new CRC32();
        checkSummer.update(data);
        long newChecksum = checkSummer.getValue();
        if (oldChecksum != newChecksum) {
            try {
                output.writeEntityHeader(key, data.length);
                output.writeEntityData(data, data.length);
            } catch (IOException e) {
            }
            return newChecksum;
        }
        return oldChecksum;
    }

    private byte[] getSystemSettings() {
        Cursor cursor = getContentResolver().query(Settings.System.CONTENT_URI, PROJECTION, null, null, null);
        try {
            return extractRelevantValues(cursor, Settings.System.SETTINGS_TO_BACKUP);
        } finally {
            cursor.close();
        }
    }

    private byte[] getSecureSettings() {
        Cursor cursor = getContentResolver().query(Settings.Secure.CONTENT_URI, PROJECTION, null, null, null);
        try {
            return extractRelevantValues(cursor, Settings.Secure.SETTINGS_TO_BACKUP);
        } finally {
            cursor.close();
        }
    }

    private byte[] getGlobalSettings() {
        Cursor cursor = getContentResolver().query(Settings.Global.CONTENT_URI, PROJECTION, null, null, null);
        try {
            return extractRelevantValues(cursor, Settings.Global.SETTINGS_TO_BACKUP);
        } finally {
            cursor.close();
        }
    }

    private void restoreSettings(BackupDataInput data, Uri contentUri, HashSet<String> movedToGlobal) {
        byte[] settings = new byte[data.getDataSize()];
        try {
            data.readEntityData(settings, 0, settings.length);
            restoreSettings(settings, settings.length, contentUri, movedToGlobal);
        } catch (IOException e) {
            Log.e("SettingsBackupAgent", "Couldn't read entity data");
        }
    }

    private void restoreSettings(byte[] settings, int bytes, Uri contentUri, HashSet<String> movedToGlobal) {
        String[] whitelist;
        if (contentUri.equals(Settings.Secure.CONTENT_URI)) {
            whitelist = Settings.Secure.SETTINGS_TO_BACKUP;
        } else if (contentUri.equals(Settings.System.CONTENT_URI)) {
            whitelist = Settings.System.SETTINGS_TO_BACKUP;
        } else if (contentUri.equals(Settings.Global.CONTENT_URI)) {
            whitelist = Settings.Global.SETTINGS_TO_BACKUP;
        } else {
            throw new IllegalArgumentException("Unknown URI: " + contentUri);
        }
        int pos = 0;
        Map<String, String> cachedEntries = new HashMap<>();
        ContentValues contentValues = new ContentValues(2);
        SettingsHelper settingsHelper = this.mSettingsHelper;
        for (String key : whitelist) {
            String value = cachedEntries.remove(key);
            if (value == null) {
                while (true) {
                    if (pos >= bytes) {
                        break;
                    }
                    int length = readInt(settings, pos);
                    int pos2 = pos + 4;
                    String dataKey = length > 0 ? new String(settings, pos2, length) : null;
                    int pos3 = pos2 + length;
                    int length2 = readInt(settings, pos3);
                    int pos4 = pos3 + 4;
                    String dataValue = length2 > 0 ? new String(settings, pos4, length2) : null;
                    pos = pos4 + length2;
                    if (key.equals(dataKey)) {
                        value = dataValue;
                        break;
                    }
                    cachedEntries.put(dataKey, dataValue);
                }
            }
            if (value != null) {
                Uri destination = (movedToGlobal == null || !movedToGlobal.contains(key)) ? contentUri : Settings.Global.CONTENT_URI;
                if (settingsHelper.restoreValue(key, value)) {
                    contentValues.clear();
                    contentValues.put("name", key);
                    contentValues.put("value", value);
                    getContentResolver().insert(destination, contentValues);
                }
            }
        }
    }

    private byte[] extractRelevantValues(Cursor cursor, String[] settings) {
        int settingsCount = settings.length;
        byte[][] values = new byte[settingsCount * 2][];
        if (!cursor.moveToFirst()) {
            Log.e("SettingsBackupAgent", "Couldn't read from the cursor");
            return new byte[0];
        }
        int totalSize = 0;
        int backedUpSettingIndex = 0;
        Map<String, String> cachedEntries = new HashMap<>();
        for (String key : settings) {
            String value = cachedEntries.remove(key);
            if (value == null) {
                while (true) {
                    if (cursor.isAfterLast()) {
                        break;
                    }
                    String cursorKey = cursor.getString(1);
                    String cursorValue = cursor.getString(2);
                    cursor.moveToNext();
                    if (key.equals(cursorKey)) {
                        value = cursorValue;
                        break;
                    }
                    cachedEntries.put(cursorKey, cursorValue);
                }
            }
            String value2 = this.mSettingsHelper.onBackupValue(key, value);
            if (value2 != null) {
                byte[] keyBytes = key.getBytes();
                int totalSize2 = totalSize + keyBytes.length + 4;
                values[backedUpSettingIndex * 2] = keyBytes;
                byte[] valueBytes = value2.getBytes();
                totalSize = totalSize2 + valueBytes.length + 4;
                values[(backedUpSettingIndex * 2) + 1] = valueBytes;
                backedUpSettingIndex++;
            }
        }
        byte[] result = new byte[totalSize];
        int pos = 0;
        int keyValuePairCount = backedUpSettingIndex * 2;
        for (int i = 0; i < keyValuePairCount; i++) {
            pos = writeBytes(result, writeInt(result, pos, values[i].length), values[i]);
        }
        return result;
    }

    private byte[] getFileData(String filename) throws Throwable {
        byte[] bytes;
        File file;
        InputStream is;
        int numRead;
        InputStream is2 = null;
        try {
            try {
                file = new File(filename);
                is = new FileInputStream(file);
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e) {
        }
        try {
            bytes = new byte[(int) file.length()];
            int offset = 0;
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            if (offset < bytes.length) {
                Log.w("SettingsBackupAgent", "Couldn't backup " + filename);
                bytes = EMPTY_DATA;
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e2) {
                    }
                }
                is2 = is;
            } else {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e3) {
                    }
                }
                is2 = is;
            }
        } catch (IOException e4) {
            is2 = is;
            Log.w("SettingsBackupAgent", "Couldn't backup " + filename);
            bytes = EMPTY_DATA;
            if (is2 != null) {
                try {
                    is2.close();
                } catch (IOException e5) {
                }
            }
        } catch (Throwable th2) {
            th = th2;
            is2 = is;
            if (is2 != null) {
                try {
                    is2.close();
                } catch (IOException e6) {
                }
            }
            throw th;
        }
        return bytes;
    }

    private void restoreFileData(String filename, byte[] bytes, int size) {
        try {
            File file = new File(filename);
            if (file.exists()) {
                file.delete();
            }
            OutputStream os = new BufferedOutputStream(new FileOutputStream(filename, true));
            os.write(bytes, 0, size);
            os.close();
        } catch (IOException e) {
            Log.w("SettingsBackupAgent", "Couldn't restore " + filename);
        }
    }

    private byte[] getWifiSupplicant(String filename) throws Throwable {
        byte[] byteArray;
        BufferedReader br = null;
        try {
            try {
                File file = new File(filename);
                if (file.exists()) {
                    WifiManager wifi = (WifiManager) getSystemService("wifi");
                    List<WifiConfiguration> configs = wifi.getConfiguredNetworks();
                    WifiNetworkSettings fromFile = new WifiNetworkSettings();
                    BufferedReader br2 = new BufferedReader(new FileReader(file));
                    try {
                        fromFile.readNetworks(br2, configs);
                        if (fromFile.mKnownNetworks.size() > 0) {
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            OutputStreamWriter out = new OutputStreamWriter(bos);
                            fromFile.write(out);
                            out.flush();
                            byteArray = bos.toByteArray();
                            IoUtils.closeQuietly(br2);
                            br = br2;
                        } else {
                            byteArray = EMPTY_DATA;
                            IoUtils.closeQuietly(br2);
                            br = br2;
                        }
                    } catch (IOException e) {
                        br = br2;
                        Log.w("SettingsBackupAgent", "Couldn't backup " + filename);
                        byteArray = EMPTY_DATA;
                        IoUtils.closeQuietly(br);
                    } catch (Throwable th) {
                        th = th;
                        br = br2;
                        IoUtils.closeQuietly(br);
                        throw th;
                    }
                } else {
                    byteArray = EMPTY_DATA;
                    IoUtils.closeQuietly((AutoCloseable) null);
                }
            } catch (IOException e2) {
            }
            return byteArray;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private void restoreWifiSupplicant(String filename, byte[] bytes, int size) {
        try {
            WifiNetworkSettings supplicantImage = new WifiNetworkSettings();
            File supplicantFile = new File("/data/misc/wifi/wpa_supplicant.conf");
            if (supplicantFile.exists()) {
                BufferedReader in = new BufferedReader(new FileReader("/data/misc/wifi/wpa_supplicant.conf"));
                supplicantImage.readNetworks(in, null);
                in.close();
                supplicantFile.delete();
            }
            if (size > 0) {
                char[] restoredAsBytes = new char[size];
                for (int i = 0; i < size; i++) {
                    restoredAsBytes[i] = (char) bytes[i];
                }
                supplicantImage.readNetworks(new BufferedReader(new CharArrayReader(restoredAsBytes)), null);
            }
            BufferedWriter bw = new BufferedWriter(new FileWriter("/data/misc/wifi/wpa_supplicant.conf"));
            copyWifiSupplicantTemplate(bw);
            supplicantImage.write(bw);
            bw.close();
        } catch (IOException e) {
            Log.w("SettingsBackupAgent", "Couldn't restore " + filename);
        }
    }

    private void copyWifiSupplicantTemplate(BufferedWriter bw) {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/system/etc/wifi/wpa_supplicant.conf"));
            char[] temp = new char[1024];
            while (true) {
                int size = br.read(temp);
                if (size > 0) {
                    bw.write(temp, 0, size);
                } else {
                    br.close();
                    return;
                }
            }
        } catch (IOException e) {
            Log.w("SettingsBackupAgent", "Couldn't copy wpa_supplicant file");
        }
    }

    private int writeInt(byte[] out, int pos, int value) {
        out[pos + 0] = (byte) ((value >> 24) & 255);
        out[pos + 1] = (byte) ((value >> 16) & 255);
        out[pos + 2] = (byte) ((value >> 8) & 255);
        out[pos + 3] = (byte) ((value >> 0) & 255);
        return pos + 4;
    }

    private int writeBytes(byte[] out, int pos, byte[] value) {
        System.arraycopy(value, 0, out, pos, value.length);
        return value.length + pos;
    }

    private int readInt(byte[] in, int pos) {
        int result = ((in[pos] & 255) << 24) | ((in[pos + 1] & 255) << 16) | ((in[pos + 2] & 255) << 8) | ((in[pos + 3] & 255) << 0);
        return result;
    }

    private int enableWifi(boolean enable) {
        if (this.mWfm == null) {
            this.mWfm = (WifiManager) getSystemService("wifi");
        }
        if (this.mWfm != null) {
            int state = this.mWfm.getWifiState();
            this.mWfm.setWifiEnabled(enable);
            return state;
        }
        Log.e("SettingsBackupAgent", "Failed to fetch WifiManager instance");
        return 4;
    }
}
