package com.android.providers.telephony;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Environment;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.IApnSourceService;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import mediatek.telephony.MtkTelephony;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class TelephonyProvider extends ContentProvider {

    @VisibleForTesting
    static Boolean s_apnSourceServiceExists;
    private static final ContentValues s_currentNullMap;
    private static final ContentValues s_currentSetMap;

    @GuardedBy("mLock")
    private IApnSourceService mIApnSourceService;
    private Injector mInjector;
    protected final Object mLock;
    private boolean mManagedApnEnforced;
    private DatabaseHelper mOpenHelper;
    private static final UriMatcher s_urlMatcher = new UriMatcher(-1);
    private static final List<String> CARRIERS_UNIQUE_FIELDS = new ArrayList();
    private static final Map<String, String> CARRIERS_UNIQUE_FIELDS_DEFAULTS = new HashMap();

    static {
        CARRIERS_UNIQUE_FIELDS_DEFAULTS.put("numeric", "");
        CARRIERS_UNIQUE_FIELDS_DEFAULTS.put("mcc", "");
        CARRIERS_UNIQUE_FIELDS_DEFAULTS.put("mnc", "");
        CARRIERS_UNIQUE_FIELDS_DEFAULTS.put("apn", "");
        CARRIERS_UNIQUE_FIELDS_DEFAULTS.put("proxy", "");
        CARRIERS_UNIQUE_FIELDS_DEFAULTS.put("port", "");
        CARRIERS_UNIQUE_FIELDS_DEFAULTS.put("mmsproxy", "");
        CARRIERS_UNIQUE_FIELDS_DEFAULTS.put("mmsport", "");
        CARRIERS_UNIQUE_FIELDS_DEFAULTS.put("mmsc", "");
        CARRIERS_UNIQUE_FIELDS_DEFAULTS.put("carrier_enabled", "1");
        CARRIERS_UNIQUE_FIELDS_DEFAULTS.put("bearer", "0");
        CARRIERS_UNIQUE_FIELDS_DEFAULTS.put("mvno_type", "");
        CARRIERS_UNIQUE_FIELDS_DEFAULTS.put("mvno_match_data", "");
        CARRIERS_UNIQUE_FIELDS_DEFAULTS.put("profile_id", "0");
        CARRIERS_UNIQUE_FIELDS_DEFAULTS.put("protocol", "IP");
        CARRIERS_UNIQUE_FIELDS_DEFAULTS.put("roaming_protocol", "IP");
        CARRIERS_UNIQUE_FIELDS_DEFAULTS.put("user_editable", "1");
        CARRIERS_UNIQUE_FIELDS_DEFAULTS.put("owned_by", String.valueOf(1));
        CARRIERS_UNIQUE_FIELDS_DEFAULTS.put("apn_set_id", String.valueOf(0));
        CARRIERS_UNIQUE_FIELDS_DEFAULTS.put("network_type_bitmask", "0");
        CARRIERS_UNIQUE_FIELDS_DEFAULTS.put("sourcetype", "0");
        CARRIERS_UNIQUE_FIELDS.addAll(CARRIERS_UNIQUE_FIELDS_DEFAULTS.keySet());
        s_urlMatcher.addURI("telephony", "carriers", 1);
        s_urlMatcher.addURI("telephony", "carriers/current", 2);
        s_urlMatcher.addURI("telephony", "carriers/#", 3);
        s_urlMatcher.addURI("telephony", "carriers/restore", 4);
        s_urlMatcher.addURI("telephony", "carriers/preferapn", 5);
        s_urlMatcher.addURI("telephony", "carriers/preferapn_no_update", 6);
        s_urlMatcher.addURI("telephony", "carriers/preferapnset", 21);
        s_urlMatcher.addURI("telephony", "siminfo", 7);
        s_urlMatcher.addURI("telephony", "carriers/subId/*", 8);
        s_urlMatcher.addURI("telephony", "carriers/current/subId/*", 9);
        s_urlMatcher.addURI("telephony", "carriers/restore/subId/*", 10);
        s_urlMatcher.addURI("telephony", "carriers/preferapn/subId/*", 11);
        s_urlMatcher.addURI("telephony", "carriers/preferapn_no_update/subId/*", 12);
        s_urlMatcher.addURI("telephony", "carriers/preferapnset/subId/*", 22);
        s_urlMatcher.addURI("telephony", "carriers/update_db", 14);
        s_urlMatcher.addURI("telephony", "carriers/delete", 15);
        s_urlMatcher.addURI("telephony", "carriers/dpc", 16);
        s_urlMatcher.addURI("telephony", "carriers/dpc/#", 17);
        s_urlMatcher.addURI("telephony", "carriers/filtered", 18);
        s_urlMatcher.addURI("telephony", "carriers/filtered/#", 19);
        s_urlMatcher.addURI("telephony", "carriers/enforce_managed", 20);
        s_urlMatcher.addURI("telephony", "carriers/prefertetheringapn", 51);
        s_urlMatcher.addURI("telephony", "carriers_dm", 52);
        s_urlMatcher.addURI("telephony", "carriers_dm/#", 53);
        s_currentNullMap = new ContentValues(1);
        s_currentNullMap.put("current", "0");
        s_currentSetMap = new ContentValues(1);
        s_currentSetMap.put("current", "1");
    }

    @VisibleForTesting
    public static String getStringForCarrierTableCreation(String str) {
        return "CREATE TABLE " + str + "(_id INTEGER PRIMARY KEY,name TEXT DEFAULT '',numeric TEXT DEFAULT '',mcc TEXT DEFAULT '',mnc TEXT DEFAULT '',apn TEXT DEFAULT '',user TEXT DEFAULT '',server TEXT DEFAULT '',password TEXT DEFAULT '',proxy TEXT DEFAULT '',port TEXT DEFAULT '',mmsproxy TEXT DEFAULT '',mmsport TEXT DEFAULT '',mmsc TEXT DEFAULT '',authtype INTEGER DEFAULT -1,type TEXT DEFAULT '',current INTEGER,sourcetype INTEGER DEFAULT 0,csdnum TEXT DEFAULT '',protocol TEXT DEFAULT IP,roaming_protocol TEXT DEFAULT IP,omacpid TEXT DEFAULT '',napid TEXT DEFAULT '',proxyid TEXT DEFAULT '',carrier_enabled BOOLEAN DEFAULT 1,bearer INTEGER DEFAULT 0,bearer_bitmask INTEGER DEFAULT 0,network_type_bitmask INTEGER DEFAULT 0,spn TEXT DEFAULT '',imsi TEXT DEFAULT '',pnn TEXT DEFAULT '',ppp TEXT DEFAULT '',mvno_type TEXT DEFAULT '',mvno_match_data TEXT DEFAULT '',sub_id INTEGER DEFAULT -1,profile_id INTEGER DEFAULT 0,modem_cognitive BOOLEAN DEFAULT 0,max_conns INTEGER DEFAULT 0,wait_time INTEGER DEFAULT 0,max_conns_time INTEGER DEFAULT 0,mtu INTEGER DEFAULT 0,edited INTEGER DEFAULT 0,user_visible BOOLEAN DEFAULT 1,user_editable BOOLEAN DEFAULT 1,owned_by INTEGER DEFAULT 1,apn_set_id INTEGER DEFAULT 0,UNIQUE (" + TextUtils.join(", ", CARRIERS_UNIQUE_FIELDS) + "));";
    }

    @VisibleForTesting
    public static String getStringForSimInfoTableCreation(String str) {
        return "CREATE TABLE " + str + "(_id INTEGER PRIMARY KEY AUTOINCREMENT,icc_id TEXT NOT NULL,sim_id INTEGER DEFAULT -1,display_name TEXT,carrier_name TEXT,name_source INTEGER DEFAULT 0,color INTEGER DEFAULT 0,number TEXT,display_number_format INTEGER NOT NULL DEFAULT 1,data_roaming INTEGER DEFAULT 0,mcc INTEGER DEFAULT 0,mnc INTEGER DEFAULT 0,sim_provisioning_status INTEGER DEFAULT 0,is_embedded INTEGER DEFAULT 0,card_id TEXT NOT NULL,access_rules BLOB,is_removable INTEGER DEFAULT 0,enable_cmas_extreme_threat_alerts INTEGER DEFAULT 1,enable_cmas_severe_threat_alerts INTEGER DEFAULT 1,enable_cmas_amber_alerts INTEGER DEFAULT 1,enable_emergency_alerts INTEGER DEFAULT 1,alert_sound_duration INTEGER DEFAULT 4,alert_reminder_interval INTEGER DEFAULT 0,enable_alert_vibrate INTEGER DEFAULT 1,enable_alert_speech INTEGER DEFAULT 1,enable_etws_test_alerts INTEGER DEFAULT 0,enable_channel_50_alerts INTEGER DEFAULT 1,enable_cmas_test_alerts INTEGER DEFAULT 0,show_cmas_opt_out_dialog INTEGER DEFAULT 1,volte_vt_enabled INTEGER DEFAULT -1,vt_ims_enabled INTEGER DEFAULT -1,wfc_ims_enabled INTEGER DEFAULT -1,wfc_ims_mode INTEGER DEFAULT -1,wfc_ims_roaming_mode INTEGER DEFAULT -1,wfc_ims_roaming_enabled INTEGER DEFAULT -1);";
    }

    @VisibleForTesting
    static class Injector {
        Injector() {
        }

        int binderGetCallingUid() {
            return Binder.getCallingUid();
        }
    }

    public TelephonyProvider() {
        this(new Injector());
    }

    @VisibleForTesting
    public TelephonyProvider(Injector injector) {
        this.mLock = new Object();
        this.mInjector = injector;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private Context mContext;

        public DatabaseHelper(Context context) {
            super(context, "telephony.db", (SQLiteDatabase.CursorFactory) null, getVersion(context));
            this.mContext = context;
            TelephonyProvider.log("Version=" + getVersion(this.mContext));
            setIdleConnectionTimeout(30000L);
        }

        private static int getVersion(Context context) {
            XmlResourceParser xml = context.getResources().getXml(android.R.xml.apns);
            try {
                XmlUtils.beginDocument(xml, "apns");
                return 1704192 | Integer.parseInt(xml.getAttributeValue(null, "version"));
            } catch (Exception e) {
                TelephonyProvider.loge("Can't get version of APN database" + e + " return version=" + Integer.toHexString(1704192));
                return 1704192;
            } finally {
                xml.close();
            }
        }

        @Override
        public void onCreate(SQLiteDatabase sQLiteDatabase) throws Throwable {
            TelephonyProvider.log("dbh.onCreate:+ db=" + sQLiteDatabase);
            createSimInfoTable(sQLiteDatabase, "siminfo");
            createCarriersTable(sQLiteDatabase, "carriers");
            if (TelephonyProvider.apnSourceServiceExists(this.mContext)) {
                TelephonyProvider.log("dbh.onCreate: Skipping apply APNs from xml.");
            } else {
                TelephonyProvider.log("dbh.onCreate: Apply apns from xml.");
                initDatabase(sQLiteDatabase);
            }
            TelephonyProvider.log("dbh.onCreate:- db=" + sQLiteDatabase);
        }

        @Override
        public void onOpen(SQLiteDatabase sQLiteDatabase) {
            try {
                sQLiteDatabase.query("siminfo", null, null, null, null, null, null);
                TelephonyProvider.log("dbh.onOpen: ok, queried table=siminfo");
            } catch (SQLiteException e) {
                TelephonyProvider.loge("Exception siminfoe=" + e);
                if (e.getMessage().startsWith("no such table")) {
                    createSimInfoTable(sQLiteDatabase, "siminfo");
                }
            }
            try {
                sQLiteDatabase.query("carriers", null, null, null, null, null, null);
                TelephonyProvider.log("dbh.onOpen: ok, queried table=carriers");
            } catch (SQLiteException e2) {
                TelephonyProvider.loge("Exception carriers e=" + e2);
                if (e2.getMessage().startsWith("no such table")) {
                    createCarriersTable(sQLiteDatabase, "carriers");
                }
            }
        }

        private void createSimInfoTable(SQLiteDatabase sQLiteDatabase, String str) {
            TelephonyProvider.log("dbh.createSimInfoTable:+ " + str);
            sQLiteDatabase.execSQL(TelephonyProvider.getStringForSimInfoTableCreation(str));
            TelephonyProvider.log("dbh.createSimInfoTable:-");
        }

        private void createCarriersTable(SQLiteDatabase sQLiteDatabase, String str) {
            TelephonyProvider.log("dbh.createCarriersTable: " + str);
            sQLiteDatabase.execSQL(TelephonyProvider.getStringForCarrierTableCreation(str));
            TelephonyProvider.log("dbh.createCarriersTable:-");
            TelephonyProvider.log("dbh.createDmCarriersTable: carriers_dm");
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS carriers_dm");
            sQLiteDatabase.execSQL(TelephonyProvider.getStringForCarrierTableCreation("carriers_dm"));
            TelephonyProvider.log("dbh.createDmCarriersTable:-");
        }

        private long getChecksum(File file) {
            IOException e;
            long jChecksumCrc32;
            FileNotFoundException e2;
            try {
                jChecksumCrc32 = FileUtils.checksumCrc32(file);
            } catch (FileNotFoundException e3) {
                e2 = e3;
                jChecksumCrc32 = -1;
            } catch (IOException e4) {
                e = e4;
                jChecksumCrc32 = -1;
            }
            try {
                TelephonyProvider.log("Checksum for " + file.getAbsolutePath() + " is " + jChecksumCrc32);
            } catch (FileNotFoundException e5) {
                e2 = e5;
                TelephonyProvider.loge("FileNotFoundException for " + file.getAbsolutePath() + ":" + e2);
            } catch (IOException e6) {
                e = e6;
                TelephonyProvider.loge("IOException for " + file.getAbsolutePath() + ":" + e);
            }
            return jChecksumCrc32;
        }

        private long getApnConfChecksum() {
            return this.mContext.getSharedPreferences("telephonyprovider", 0).getLong("apn_conf_checksum", -1L);
        }

        private void setApnConfChecksum(long j) {
            SharedPreferences.Editor editorEdit = this.mContext.getSharedPreferences("telephonyprovider", 0).edit();
            editorEdit.putLong("apn_conf_checksum", j);
            editorEdit.apply();
        }

        private File getApnConfFile() {
            File file = new File(Environment.getRootDirectory(), "etc/apns-conf.xml");
            File file2 = new File(Environment.getOemDirectory(), "telephony/apns-conf.xml");
            return getNewerFile(getNewerFile(file, file2), new File(Environment.getDataDirectory(), "misc/apns/apns-conf.xml"));
        }

        private boolean apnDbUpdateNeeded() {
            long checksum = getChecksum(getApnConfFile());
            long apnConfChecksum = getApnConfChecksum();
            TelephonyProvider.log("newChecksum: " + checksum);
            TelephonyProvider.log("oldChecksum: " + apnConfChecksum);
            if (checksum == apnConfChecksum) {
                return false;
            }
            return true;
        }

        private void initDatabase(SQLiteDatabase sQLiteDatabase) throws Throwable {
            ?? sb;
            ?? r5;
            ?? r3;
            int i;
            FileReader fileReader;
            XmlPullParser xmlPullParserNewPullParser;
            int i2;
            ?? xml = this.mContext.getResources().getXml(android.R.xml.apns);
            int i3 = -1;
            try {
                try {
                    XmlUtils.beginDocument((XmlPullParser) xml, "apns");
                    i2 = Integer.parseInt(xml.getAttributeValue(null, "version"));
                } catch (Exception e) {
                    e = e;
                }
                try {
                    loadApns(sQLiteDatabase, xml);
                    xml.close();
                    i = i2;
                    r3 = i2;
                } catch (Exception e2) {
                    e = e2;
                    i3 = i2;
                    sb = new StringBuilder();
                    r5 = "Got exception while loading APN database.";
                    sb.append("Got exception while loading APN database.");
                    sb.append(e);
                    String string = sb.toString();
                    TelephonyProvider.loge(string);
                }
                try {
                    xml = getApnConfFile();
                    TelephonyProvider.log("confFile = " + xml);
                    r3 = 5;
                    sb = 2;
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    fileReader = new FileReader((File) xml);
                    try {
                        xmlPullParserNewPullParser = Xml.newPullParser();
                        xmlPullParserNewPullParser.setInput(fileReader);
                        XmlUtils.beginDocument(xmlPullParserNewPullParser, "apns");
                    } catch (FileNotFoundException e3) {
                        sQLiteDatabase.delete("carriers", "edited=2 or edited=5", null);
                        ContentValues contentValues = new ContentValues();
                        contentValues.put("edited", (Integer) 2);
                        sQLiteDatabase.update("carriers", contentValues, "edited=3", null);
                        ContentValues contentValues2 = new ContentValues();
                        contentValues2.put("edited", (Integer) 5);
                        sQLiteDatabase.update("carriers", contentValues2, "edited=6", null);
                        if (fileReader != null) {
                            try {
                                fileReader.close();
                            } catch (IOException e4) {
                            }
                        }
                    } catch (Exception e5) {
                        e = e5;
                        TelephonyProvider.loge("initDatabase: Exception while parsing '" + xml.getAbsolutePath() + "'" + e);
                        sQLiteDatabase.delete("carriers", "edited=2 or edited=5", null);
                        ContentValues contentValues3 = new ContentValues();
                        contentValues3.put("edited", (Integer) 2);
                        sQLiteDatabase.update("carriers", contentValues3, "edited=3", null);
                        ContentValues contentValues4 = new ContentValues();
                        contentValues4.put("edited", (Integer) 5);
                        sQLiteDatabase.update("carriers", contentValues4, "edited=6", null);
                        if (fileReader != null) {
                            try {
                                fileReader.close();
                            } catch (IOException e6) {
                            }
                        }
                    }
                } catch (FileNotFoundException e7) {
                    fileReader = null;
                } catch (Exception e8) {
                    e = e8;
                    fileReader = null;
                } catch (Throwable th2) {
                    th = th2;
                    r5 = 0;
                    sQLiteDatabase.delete("carriers", "edited=2 or edited=5", null);
                    ContentValues contentValues5 = new ContentValues();
                    contentValues5.put("edited", Integer.valueOf((int) sb));
                    sQLiteDatabase.update("carriers", contentValues5, "edited=3", null);
                    ContentValues contentValues6 = new ContentValues();
                    contentValues6.put("edited", Integer.valueOf((int) r3));
                    sQLiteDatabase.update("carriers", contentValues6, "edited=6", null);
                    if (r5 != 0) {
                        try {
                            r5.close();
                        } catch (IOException e9) {
                        }
                    }
                    setApnConfChecksum(getChecksum(xml));
                    throw th;
                }
                if (i != Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(null, "version"))) {
                    TelephonyProvider.log("initDatabase: throwing exception due to version mismatch");
                    throw new IllegalStateException("Internal APNS file version doesn't match " + xml.getAbsolutePath());
                }
                sQLiteDatabase.beginTransaction();
                try {
                    loadApns(sQLiteDatabase, xmlPullParserNewPullParser);
                    sQLiteDatabase.setTransactionSuccessful();
                    sQLiteDatabase.delete("carriers", "edited=2 or edited=5", null);
                    ContentValues contentValues7 = new ContentValues();
                    contentValues7.put("edited", (Integer) 2);
                    sQLiteDatabase.update("carriers", contentValues7, "edited=3", null);
                    ContentValues contentValues8 = new ContentValues();
                    contentValues8.put("edited", (Integer) 5);
                    sQLiteDatabase.update("carriers", contentValues8, "edited=6", null);
                    try {
                        fileReader.close();
                    } catch (IOException e10) {
                    }
                    setApnConfChecksum(getChecksum(xml));
                } finally {
                    sQLiteDatabase.endTransaction();
                }
            } finally {
                xml.close();
            }
        }

        private File getNewerFile(File file, File file2) {
            if (file2.exists()) {
                long jLastModified = file2.lastModified();
                long jLastModified2 = file.lastModified();
                TelephonyProvider.log("APNs Timestamp: altFileTime = " + jLastModified + " currFileTime = " + jLastModified2);
                if (jLastModified > jLastModified2) {
                    TelephonyProvider.log("APNs Timestamp: Alternate image " + file2.getPath() + " is greater than System image");
                    return file2;
                }
            } else {
                TelephonyProvider.log("No APNs in OEM image = " + file2.getPath() + " Load APNs from system image");
            }
            return file;
        }

        @Override
        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) throws Throwable {
            Cursor cursorQuery;
            TelephonyProvider.log("dbh.onUpgrade:+ db=" + sQLiteDatabase + " oldV=" + i + " newV=" + i2);
            if (i < 327686) {
                sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN authtype INTEGER DEFAULT -1;");
                i = 327686;
            }
            if (i < 393222) {
                sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN protocol TEXT DEFAULT IP;");
                sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN roaming_protocol TEXT DEFAULT IP;");
                i = 393222;
            }
            if (i < 458758) {
                i = 458758;
            }
            if (i < 524294) {
                sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN carrier_enabled BOOLEAN DEFAULT 1;");
                sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN bearer INTEGER DEFAULT 0;");
                sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN mvno_type TEXT DEFAULT '';");
                sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN mvno_match_data TEXT DEFAULT '';");
                i = 524294;
            }
            if (i < 589830) {
                try {
                    sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN spn TEXT DEFAULT '';");
                    sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN imsi TEXT DEFAULT '';");
                } catch (SQLException e) {
                    e.printStackTrace();
                    Log.e("TelephonyProvider", "Add MVNO columns fail with table carriers.");
                }
                i = 589830;
            }
            if (i < 655366) {
                try {
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN name_source INTEGER DEFAULT 0;");
                } catch (SQLException e2) {
                    e2.printStackTrace();
                    Log.e("TelephonyProvider", "Add SIMInfo name_source columns fail.");
                }
                i = 655366;
            }
            if (i < 720902) {
                try {
                    sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN pnn TEXT DEFAULT '';");
                } catch (SQLException e3) {
                    e3.printStackTrace();
                    Log.e("TelephonyProvider", "Add MVNO columns fail with table carriers.");
                }
                i = 720902;
            }
            if (i < 786438) {
                try {
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN operator TEXT DEFAULT '';");
                } catch (SQLException e4) {
                    e4.printStackTrace();
                    Log.e("TelephonyProvider", "Add SIMInfo operator columns fail.");
                }
                i = 786438;
            }
            if (i < 851974) {
                try {
                    sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN ppp TEXT DEFAULT '';");
                } catch (SQLException e5) {
                    e5.printStackTrace();
                    Log.e("TelephonyProvider", "Add ppp column fail with table carriers.");
                }
                i = 851974;
            }
            if (i < 917510) {
                try {
                    sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN mvno_type TEXT DEFAULT '';");
                    sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN mvno_match_data TEXT DEFAULT '';");
                } catch (SQLException e6) {
                    e6.printStackTrace();
                    Log.e("TelephonyProvider", "Add mvno column fail with table carriers.");
                }
                i = 917510;
            }
            if (i < 983046) {
                try {
                    sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN sub_id INTEGER DEFAULT -1;");
                    sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN profile_id INTEGER DEFAULT 0;");
                    sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN modem_cognitive BOOLEAN DEFAULT 0;");
                    sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN max_conns INTEGER DEFAULT 0;");
                    sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN wait_time INTEGER DEFAULT 0;");
                    sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN max_conns_time INTEGER DEFAULT 0;");
                    sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN mtu INTEGER DEFAULT 0;");
                } catch (SQLException e7) {
                    e7.printStackTrace();
                    Log.e("TelephonyProvider", "Add mvno column fail with table carriers.");
                }
                i = 983046;
            }
            if (i < 1048582) {
                try {
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN carrier_name TEXT DEFAULT '';");
                } catch (SQLiteException e8) {
                    TelephonyProvider.log("onUpgrade skipping siminfo upgrade.  The table will get created in onOpen.");
                }
                i = 1048582;
            }
            if (i < 1048838) {
                new String[]{"_id"};
                preserveUserAndCarrierApns(sQLiteDatabase);
                Cursor cursorQuery2 = sQLiteDatabase.query("carriers", null, null, null, null, null, null);
                createCarriersTable(sQLiteDatabase, "carriers_tmp");
                copyPreservedApnsToNewTable(sQLiteDatabase, cursorQuery2);
                cursorQuery2.close();
                sQLiteDatabase.execSQL("DROP TABLE IF EXISTS carriers");
                sQLiteDatabase.execSQL("ALTER TABLE carriers_tmp rename to carriers;");
                try {
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN enable_cmas_extreme_threat_alerts INTEGER DEFAULT 1;");
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN enable_cmas_severe_threat_alerts INTEGER DEFAULT 1;");
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN enable_cmas_amber_alerts INTEGER DEFAULT 1;");
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN enable_emergency_alerts INTEGER DEFAULT 1;");
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN alert_sound_duration INTEGER DEFAULT 4;");
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN alert_reminder_interval INTEGER DEFAULT 0;");
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN enable_alert_vibrate INTEGER DEFAULT 1;");
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN enable_alert_speech INTEGER DEFAULT 1;");
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN enable_etws_test_alerts INTEGER DEFAULT 0;");
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN enable_channel_50_alerts INTEGER DEFAULT 1;");
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN enable_cmas_test_alerts INTEGER DEFAULT 0;");
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN show_cmas_opt_out_dialog INTEGER DEFAULT 1;");
                } catch (SQLiteException e9) {
                    TelephonyProvider.log("onUpgrade skipping siminfo upgrade.  The table will get created in onOpen.");
                }
                i = 1048838;
            }
            if (i < 1114118) {
                try {
                    cursorQuery = sQLiteDatabase.query("carriers", null, null, null, null, null, null, String.valueOf(1));
                    if (cursorQuery == null) {
                        sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN user_visible BOOLEAN DEFAULT 1;");
                        if (cursorQuery != null) {
                        }
                        i = 1114118;
                    } else {
                        try {
                            if (cursorQuery.getColumnIndex("user_visible") != -1) {
                                TelephonyProvider.log("onUpgrade skipping carriers upgrade.  Column user_visible already exists.");
                                if (cursorQuery != null) {
                                }
                                i = 1114118;
                            } else {
                                sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN user_visible BOOLEAN DEFAULT 1;");
                                if (cursorQuery != null) {
                                    cursorQuery.close();
                                }
                                i = 1114118;
                            }
                        } catch (Throwable th) {
                            th = th;
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            throw th;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    cursorQuery = null;
                }
            }
            if (i < 1179654) {
                try {
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN sim_provisioning_status INTEGER DEFAULT 0;");
                } catch (SQLiteException e10) {
                    TelephonyProvider.log("onUpgrade skipping siminfo upgrade.  The table will get created in onOpen.");
                }
                i = 1179654;
            }
            if (i < 1310726) {
                try {
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN is_embedded INTEGER DEFAULT 0;");
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN access_rules BLOB;");
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN is_removable INTEGER DEFAULT 0;");
                } catch (SQLiteException e11) {
                    TelephonyProvider.log("onUpgrade skipping siminfo upgrade. The table will get created in onOpen.");
                }
                i = 1310726;
            }
            if (i < 1376262) {
                try {
                    sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN user_editable INTEGER DEFAULT 1;");
                } catch (SQLiteException e12) {
                    TelephonyProvider.log("onUpgrade skipping carriers upgrade. The table will get created in onOpen.");
                }
                i = 1376262;
            }
            if (i < 1441798) {
                try {
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN volte_vt_enabled INTEGER DEFAULT -1;");
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN vt_ims_enabled INTEGER DEFAULT -1;");
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN wfc_ims_enabled INTEGER DEFAULT -1;");
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN wfc_ims_mode INTEGER DEFAULT -1;");
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN wfc_ims_roaming_mode INTEGER DEFAULT -1;");
                    sQLiteDatabase.execSQL("ALTER TABLE siminfo ADD COLUMN wfc_ims_roaming_enabled INTEGER DEFAULT -1;");
                } catch (SQLiteException e13) {
                    TelephonyProvider.log("onUpgrade skipping carriers upgrade. The table will get created in onOpen.");
                }
                i = 1441798;
            }
            if (i < 1507334) {
                try {
                    sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN owned_by INTEGER DEFAULT 1;");
                } catch (SQLiteException e14) {
                    TelephonyProvider.log("onUpgrade skipping carriers upgrade. The table will get created in onOpen.");
                }
                i = 1507334;
            }
            if (i < 1572870) {
                recreateDB(null, sQLiteDatabase, new String[]{"_id"}, 24);
                i = 1572870;
            }
            if (i < 1638406) {
                recreateSimInfoDB(null, sQLiteDatabase, new String[]{"_id"});
                i = 1638406;
            }
            if (i < 1703942) {
                try {
                    sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN apn_set_id INTEGER DEFAULT 0;");
                } catch (SQLiteException e15) {
                    TelephonyProvider.log("onUpgrade skipping carriers upgrade. The table will get created in onOpen.");
                }
                i = 1703942;
            }
            if (i < 1704198) {
                try {
                    sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN omacpid TEXT DEFAULT '';");
                    sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN napid TEXT DEFAULT '';");
                    sQLiteDatabase.execSQL("ALTER TABLE carriers ADD COLUMN proxyid TEXT DEFAULT '';");
                } catch (SQLiteException e16) {
                    e16.printStackTrace();
                    Log.e("TelephonyProvider", "Add OMACP column fail with table carriers.");
                }
                i = 1704198;
            }
            TelephonyProvider.log("dbh.onUpgrade:- db=" + sQLiteDatabase + " oldV=" + i + " newV=" + i2);
        }

        private void recreateSimInfoDB(Cursor cursor, SQLiteDatabase sQLiteDatabase, String[] strArr) {
            Cursor cursorQuery = sQLiteDatabase.query("siminfo", null, null, null, null, null, "_id ASC");
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS siminfo_tmp");
            createSimInfoTable(sQLiteDatabase, "siminfo_tmp");
            copySimInfoDataToTmpTable(sQLiteDatabase, cursorQuery);
            cursorQuery.close();
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS siminfo");
            sQLiteDatabase.execSQL("ALTER TABLE siminfo_tmp rename to siminfo;");
        }

        private void copySimInfoDataToTmpTable(SQLiteDatabase sQLiteDatabase, Cursor cursor) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    ContentValues contentValues = new ContentValues();
                    copySimInfoValuesV24(contentValues, cursor);
                    getCardIdfromIccid(contentValues, cursor);
                    try {
                        sQLiteDatabase.insert("siminfo_tmp", null, contentValues);
                    } catch (SQLException e) {
                    }
                }
            }
        }

        private void copySimInfoValuesV24(ContentValues contentValues, Cursor cursor) {
            getStringValueFromCursor(contentValues, cursor, "icc_id");
            getStringValueFromCursor(contentValues, cursor, "display_name");
            getStringValueFromCursor(contentValues, cursor, "carrier_name");
            getStringValueFromCursor(contentValues, cursor, "number");
            getIntValueFromCursor(contentValues, cursor, "sim_id");
            getIntValueFromCursor(contentValues, cursor, "name_source");
            getIntValueFromCursor(contentValues, cursor, "color");
            getIntValueFromCursor(contentValues, cursor, "display_number_format");
            getIntValueFromCursor(contentValues, cursor, "data_roaming");
            getIntValueFromCursor(contentValues, cursor, "mcc");
            getIntValueFromCursor(contentValues, cursor, "mnc");
            getIntValueFromCursor(contentValues, cursor, "sim_provisioning_status");
            getIntValueFromCursor(contentValues, cursor, "is_embedded");
            getIntValueFromCursor(contentValues, cursor, "is_removable");
            getIntValueFromCursor(contentValues, cursor, "enable_cmas_extreme_threat_alerts");
            getIntValueFromCursor(contentValues, cursor, "enable_cmas_severe_threat_alerts");
            getIntValueFromCursor(contentValues, cursor, "enable_cmas_amber_alerts");
            getIntValueFromCursor(contentValues, cursor, "enable_emergency_alerts");
            getIntValueFromCursor(contentValues, cursor, "alert_sound_duration");
            getIntValueFromCursor(contentValues, cursor, "alert_reminder_interval");
            getIntValueFromCursor(contentValues, cursor, "enable_alert_vibrate");
            getIntValueFromCursor(contentValues, cursor, "enable_alert_speech");
            getIntValueFromCursor(contentValues, cursor, "enable_etws_test_alerts");
            getIntValueFromCursor(contentValues, cursor, "enable_channel_50_alerts");
            getIntValueFromCursor(contentValues, cursor, "enable_cmas_test_alerts");
            getIntValueFromCursor(contentValues, cursor, "show_cmas_opt_out_dialog");
            getIntValueFromCursor(contentValues, cursor, "volte_vt_enabled");
            getIntValueFromCursor(contentValues, cursor, "vt_ims_enabled");
            getIntValueFromCursor(contentValues, cursor, "wfc_ims_enabled");
            getIntValueFromCursor(contentValues, cursor, "wfc_ims_mode");
            getIntValueFromCursor(contentValues, cursor, "wfc_ims_roaming_mode");
            getIntValueFromCursor(contentValues, cursor, "wfc_ims_roaming_enabled");
            getBlobValueFromCursor(contentValues, cursor, "access_rules");
        }

        private void getCardIdfromIccid(ContentValues contentValues, Cursor cursor) {
            int columnIndex = cursor.getColumnIndex("icc_id");
            if (columnIndex != -1) {
                String string = cursor.getString(columnIndex);
                if (!TextUtils.isEmpty(string)) {
                    contentValues.put("card_id", string);
                }
            }
        }

        private void recreateDB(Cursor cursor, SQLiteDatabase sQLiteDatabase, String[] strArr, int i) {
            Cursor cursorQuery = sQLiteDatabase.query("carriers", null, null, null, null, null, null);
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS carriers_tmp");
            createCarriersTable(sQLiteDatabase, "carriers_tmp");
            copyDataToTmpTable(sQLiteDatabase, cursorQuery);
            cursorQuery.close();
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS carriers");
            sQLiteDatabase.execSQL("ALTER TABLE carriers_tmp rename to carriers;");
        }

        private void preserveUserAndCarrierApns(SQLiteDatabase sQLiteDatabase) throws Throwable {
            FileReader fileReader;
            File file = new File(Environment.getRootDirectory(), "etc/old-apns-conf.xml");
            ?? r1 = 0;
            r1 = 0;
            r1 = 0;
            r1 = 0;
            try {
                try {
                    try {
                        fileReader = new FileReader(file);
                    } catch (IOException e) {
                    }
                } catch (FileNotFoundException e2) {
                } catch (Exception e3) {
                    e = e3;
                }
            } catch (Throwable th) {
                th = th;
            }
            try {
                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(fileReader);
                XmlUtils.beginDocument(xmlPullParserNewPullParser, "apns");
                deleteMatchingApns(sQLiteDatabase, xmlPullParserNewPullParser);
                fileReader.close();
                r1 = xmlPullParserNewPullParser;
            } catch (FileNotFoundException e4) {
                r1 = fileReader;
                TelephonyProvider.loge("PRESERVEUSERANDCARRIERAPNS: etc/old-apns-conf.xml NOT FOUND. IT IS NEEDED TO UPGRADE FROM OLDER VERSIONS OF APN DB WHILE PRESERVING USER/CARRIER ADDED/EDITED ENTRIES.");
                if (r1 != 0) {
                    r1.close();
                    r1 = r1;
                }
            } catch (Exception e5) {
                e = e5;
                r1 = fileReader;
                TelephonyProvider.loge("preserveUserAndCarrierApns: Exception while parsing '" + file.getAbsolutePath() + "'" + e);
                if (r1 != 0) {
                    r1.close();
                    r1 = r1;
                }
            } catch (Throwable th2) {
                th = th2;
                r1 = fileReader;
                if (r1 != 0) {
                    try {
                        r1.close();
                    } catch (IOException e6) {
                    }
                }
                throw th;
            }
        }

        private void deleteMatchingApns(SQLiteDatabase sQLiteDatabase, XmlPullParser xmlPullParser) {
            if (xmlPullParser != null) {
                try {
                    XmlUtils.nextElement(xmlPullParser);
                    while (xmlPullParser.getEventType() != 1) {
                        ContentValues row = getRow(xmlPullParser);
                        if (row == null) {
                            throw new XmlPullParserException("Expected 'apn' tag", xmlPullParser, null);
                        }
                        deleteRow(sQLiteDatabase, row);
                        XmlUtils.nextElement(xmlPullParser);
                    }
                } catch (SQLException e) {
                    TelephonyProvider.loge("deleteMatchingApns: Got SQLException while deleting apns." + e);
                } catch (IOException e2) {
                    TelephonyProvider.loge("deleteMatchingApns: Got IOException while deleting apns." + e2);
                } catch (XmlPullParserException e3) {
                    TelephonyProvider.loge("deleteMatchingApns: Got XmlPullParserException while deleting apns." + e3);
                }
            }
        }

        private String queryValFirst(String str) {
            return str + "=?";
        }

        private String queryVal(String str) {
            return " and " + str + "=?";
        }

        private String queryValOrNull(String str) {
            return " and (" + str + "=? or " + str + " is null)";
        }

        private String queryVal2OrNull(String str) {
            return " and (" + str + "=? or " + str + "=? or " + str + " is null)";
        }

        private void deleteRow(SQLiteDatabase sQLiteDatabase, ContentValues contentValues) {
            String str = queryValFirst("numeric") + queryVal("mnc") + queryVal("mnc") + queryValOrNull("apn") + queryValOrNull("user") + queryValOrNull("server") + queryValOrNull("password") + queryValOrNull("proxy") + queryValOrNull("port") + queryValOrNull("mmsproxy") + queryValOrNull("mmsport") + queryValOrNull("mmsc") + queryValOrNull("authtype") + queryValOrNull("type") + queryValOrNull("protocol") + queryValOrNull("roaming_protocol") + queryVal2OrNull("carrier_enabled") + queryValOrNull("bearer") + queryValOrNull("mvno_type") + queryValOrNull("mvno_match_data") + queryValOrNull("profile_id") + queryVal2OrNull("modem_cognitive") + queryValOrNull("max_conns") + queryValOrNull("wait_time") + queryValOrNull("max_conns_time") + queryValOrNull("mtu");
            String[] strArr = new String[29];
            strArr[0] = contentValues.getAsString("numeric");
            strArr[1] = contentValues.getAsString("mcc");
            strArr[2] = contentValues.getAsString("mnc");
            strArr[3] = contentValues.getAsString("name");
            strArr[4] = contentValues.containsKey("apn") ? contentValues.getAsString("apn") : "";
            strArr[5] = contentValues.containsKey("user") ? contentValues.getAsString("user") : "";
            strArr[6] = contentValues.containsKey("server") ? contentValues.getAsString("server") : "";
            strArr[7] = contentValues.containsKey("password") ? contentValues.getAsString("password") : "";
            strArr[8] = contentValues.containsKey("proxy") ? contentValues.getAsString("proxy") : "";
            strArr[9] = contentValues.containsKey("port") ? contentValues.getAsString("port") : "";
            strArr[10] = contentValues.containsKey("mmsproxy") ? contentValues.getAsString("mmsproxy") : "";
            strArr[11] = contentValues.containsKey("mmsport") ? contentValues.getAsString("mmsport") : "";
            strArr[12] = contentValues.containsKey("mmsc") ? contentValues.getAsString("mmsc") : "";
            strArr[13] = contentValues.containsKey("authtype") ? contentValues.getAsString("authtype") : "-1";
            strArr[14] = contentValues.containsKey("type") ? contentValues.getAsString("type") : "";
            strArr[15] = contentValues.containsKey("protocol") ? contentValues.getAsString("protocol") : "IP";
            strArr[16] = contentValues.containsKey("roaming_protocol") ? contentValues.getAsString("roaming_protocol") : "IP";
            if (!contentValues.containsKey("carrier_enabled") || (!contentValues.getAsString("carrier_enabled").equalsIgnoreCase("false") && !contentValues.getAsString("carrier_enabled").equals("0"))) {
                strArr[17] = "true";
                strArr[18] = "1";
            } else {
                strArr[17] = "false";
                strArr[18] = "0";
            }
            strArr[19] = contentValues.containsKey("bearer") ? contentValues.getAsString("bearer") : "0";
            strArr[20] = contentValues.containsKey("mvno_type") ? contentValues.getAsString("mvno_type") : "";
            strArr[21] = contentValues.containsKey("mvno_match_data") ? contentValues.getAsString("mvno_match_data") : "";
            strArr[22] = contentValues.containsKey("profile_id") ? contentValues.getAsString("profile_id") : "0";
            if (!contentValues.containsKey("modem_cognitive") || (!contentValues.getAsString("modem_cognitive").equalsIgnoreCase("true") && !contentValues.getAsString("modem_cognitive").equals("1"))) {
                strArr[23] = "false";
                strArr[24] = "0";
            } else {
                strArr[23] = "true";
                strArr[24] = "1";
            }
            strArr[25] = contentValues.containsKey("max_conns") ? contentValues.getAsString("max_conns") : "0";
            strArr[26] = contentValues.containsKey("wait_time") ? contentValues.getAsString("wait_time") : "0";
            strArr[27] = contentValues.containsKey("max_conns_time") ? contentValues.getAsString("max_conns_time") : "0";
            strArr[28] = contentValues.containsKey("mtu") ? contentValues.getAsString("mtu") : "0";
            sQLiteDatabase.delete("carriers", str, strArr);
        }

        private void copyDataToTmpTable(SQLiteDatabase sQLiteDatabase, Cursor cursor) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    ContentValues contentValues = new ContentValues();
                    copyApnValuesV17(contentValues, cursor);
                    getNetworkTypeBitmaskFromCursor(contentValues, cursor);
                    try {
                        sQLiteDatabase.insertWithOnConflict("carriers_tmp", null, contentValues, 2);
                    } catch (SQLException e) {
                    }
                }
            }
        }

        private void copyApnValuesV17(ContentValues contentValues, Cursor cursor) {
            getStringValueFromCursor(contentValues, cursor, "name");
            getStringValueFromCursor(contentValues, cursor, "numeric");
            getStringValueFromCursor(contentValues, cursor, "mcc");
            getStringValueFromCursor(contentValues, cursor, "mnc");
            getStringValueFromCursor(contentValues, cursor, "apn");
            getStringValueFromCursor(contentValues, cursor, "user");
            getStringValueFromCursor(contentValues, cursor, "server");
            getStringValueFromCursor(contentValues, cursor, "password");
            getStringValueFromCursor(contentValues, cursor, "proxy");
            getStringValueFromCursor(contentValues, cursor, "port");
            getStringValueFromCursor(contentValues, cursor, "mmsproxy");
            getStringValueFromCursor(contentValues, cursor, "mmsport");
            getStringValueFromCursor(contentValues, cursor, "mmsc");
            getStringValueFromCursor(contentValues, cursor, "type");
            getStringValueFromCursor(contentValues, cursor, "protocol");
            getStringValueFromCursor(contentValues, cursor, "roaming_protocol");
            getStringValueFromCursor(contentValues, cursor, "mvno_type");
            getStringValueFromCursor(contentValues, cursor, "mvno_match_data");
            getIntValueFromCursor(contentValues, cursor, "authtype");
            getIntValueFromCursor(contentValues, cursor, "current");
            getIntValueFromCursor(contentValues, cursor, "carrier_enabled");
            getIntValueFromCursor(contentValues, cursor, "bearer");
            getIntValueFromCursor(contentValues, cursor, "sub_id");
            getIntValueFromCursor(contentValues, cursor, "profile_id");
            getIntValueFromCursor(contentValues, cursor, "modem_cognitive");
            getIntValueFromCursor(contentValues, cursor, "max_conns");
            getIntValueFromCursor(contentValues, cursor, "wait_time");
            getIntValueFromCursor(contentValues, cursor, "max_conns_time");
            getIntValueFromCursor(contentValues, cursor, "mtu");
            getIntValueFromCursor(contentValues, cursor, "bearer_bitmask");
            getIntValueFromCursor(contentValues, cursor, "edited");
            getIntValueFromCursor(contentValues, cursor, "user_visible");
            getIntValueFromCursor(contentValues, cursor, "sourcetype");
            getStringValueFromCursor(contentValues, cursor, "omacpid");
            getStringValueFromCursor(contentValues, cursor, "napid");
            getStringValueFromCursor(contentValues, cursor, "proxyid");
        }

        private void copyPreservedApnsToNewTable(SQLiteDatabase sQLiteDatabase, Cursor cursor) {
            if (cursor != null) {
                String[] stringArray = this.mContext.getResources().getStringArray(R.array.persist_apns_for_plmn);
                while (cursor.moveToNext()) {
                    int i = cursor.getInt(cursor.getColumnIndex("_id"));
                    if (cursor.getInt(cursor.getColumnIndex("sourcetype")) == 1) {
                        TelephonyProvider.log("copyPreservedApnsToNewTable, id=" + i);
                        ContentValues contentValues = new ContentValues();
                        copyApnValuesV17(contentValues, cursor);
                        String string = cursor.getString(cursor.getColumnIndex("bearer"));
                        if (!TextUtils.isEmpty(string)) {
                            contentValues.put("bearer_bitmask", Integer.valueOf(ServiceState.getBitmaskForTech(Integer.parseInt(string))));
                            contentValues.put("network_type_bitmask", Integer.valueOf(ServiceState.getBitmaskForTech(ServiceState.rilRadioTechnologyToNetworkType(Integer.parseInt(string)))));
                        }
                        int columnIndex = cursor.getColumnIndex("user_edited");
                        if (columnIndex != -1) {
                            String string2 = cursor.getString(columnIndex);
                            if (!TextUtils.isEmpty(string2)) {
                                contentValues.put("edited", new Integer(string2));
                            }
                        } else {
                            contentValues.put("edited", (Integer) 4);
                        }
                        String string3 = cursor.getString(cursor.getColumnIndex("numeric"));
                        for (String str : stringArray) {
                            if (!TextUtils.isEmpty(string3) && string3.equals(str) && (!contentValues.containsKey("mvno_type") || TextUtils.isEmpty(contentValues.getAsString("mvno_type")))) {
                                if (columnIndex == -1 || contentValues.getAsInteger("edited").intValue() == 1) {
                                    contentValues.put("edited", (Integer) 4);
                                }
                            }
                        }
                        try {
                            sQLiteDatabase.insertWithOnConflict("carriers_tmp", null, contentValues, 2);
                        } catch (SQLException e) {
                            Cursor cursorSelectConflictingRow = selectConflictingRow(sQLiteDatabase, "carriers_tmp", contentValues);
                            if (cursorSelectConflictingRow != null) {
                                mergeFieldsAndUpdateDb(sQLiteDatabase, "carriers_tmp", cursorSelectConflictingRow, contentValues, new ContentValues(), true, this.mContext);
                                cursorSelectConflictingRow.close();
                            }
                        }
                    }
                }
            }
        }

        private void getStringValueFromCursor(ContentValues contentValues, Cursor cursor, String str) {
            int columnIndex = cursor.getColumnIndex(str);
            if (columnIndex != -1) {
                String string = cursor.getString(columnIndex);
                if (!TextUtils.isEmpty(string)) {
                    contentValues.put(str, string);
                }
            }
        }

        private void getNetworkTypeBitmaskFromCursor(ContentValues contentValues, Cursor cursor) {
            int columnIndex = cursor.getColumnIndex("network_type_bitmask");
            if (columnIndex != -1) {
                getStringValueFromCursor(contentValues, cursor, "network_type_bitmask");
                String string = cursor.getString(columnIndex);
                if (!TextUtils.isEmpty(string) && string.matches("\\d+")) {
                    contentValues.put("bearer_bitmask", String.valueOf(ServiceState.convertNetworkTypeBitmaskToBearerBitmask(Integer.valueOf(string).intValue())));
                    return;
                }
                return;
            }
            int columnIndex2 = cursor.getColumnIndex("bearer_bitmask");
            if (columnIndex2 != -1) {
                String string2 = cursor.getString(columnIndex2);
                if (!TextUtils.isEmpty(string2) && string2.matches("\\d+")) {
                    contentValues.put("network_type_bitmask", String.valueOf(ServiceState.convertBearerBitmaskToNetworkTypeBitmask(Integer.valueOf(string2).intValue())));
                }
            }
        }

        private void getIntValueFromCursor(ContentValues contentValues, Cursor cursor, String str) {
            int columnIndex = cursor.getColumnIndex(str);
            if (columnIndex != -1) {
                String string = cursor.getString(columnIndex);
                if (!TextUtils.isEmpty(string)) {
                    try {
                        contentValues.put(str, new Integer(string));
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }

        private void getBlobValueFromCursor(ContentValues contentValues, Cursor cursor, String str) {
            byte[] blob;
            int columnIndex = cursor.getColumnIndex(str);
            if (columnIndex != -1 && (blob = cursor.getBlob(columnIndex)) != null) {
                contentValues.put(str, blob);
            }
        }

        private ContentValues getRow(XmlPullParser xmlPullParser) {
            int bitmaskFromString;
            int iConvertNetworkTypeBitmaskToBearerBitmask;
            String attributeValue;
            if (!"apn".equals(xmlPullParser.getName())) {
                return null;
            }
            ContentValues contentValues = new ContentValues();
            String attributeValue2 = xmlPullParser.getAttributeValue(null, "mcc");
            String attributeValue3 = xmlPullParser.getAttributeValue(null, "mnc");
            contentValues.put("numeric", attributeValue2 + attributeValue3);
            contentValues.put("mcc", attributeValue2);
            contentValues.put("mnc", attributeValue3);
            contentValues.put("name", xmlPullParser.getAttributeValue(null, "carrier"));
            addStringAttribute(xmlPullParser, "apn", contentValues, "apn");
            addStringAttribute(xmlPullParser, "user", contentValues, "user");
            addStringAttribute(xmlPullParser, "server", contentValues, "server");
            addStringAttribute(xmlPullParser, "password", contentValues, "password");
            addStringAttribute(xmlPullParser, "proxy", contentValues, "proxy");
            addStringAttribute(xmlPullParser, "port", contentValues, "port");
            addStringAttribute(xmlPullParser, "mmsproxy", contentValues, "mmsproxy");
            addStringAttribute(xmlPullParser, "mmsport", contentValues, "mmsport");
            addStringAttribute(xmlPullParser, "mmsc", contentValues, "mmsc");
            String attributeValue4 = xmlPullParser.getAttributeValue(null, "type");
            if (attributeValue4 != null) {
                contentValues.put("type", attributeValue4.replaceAll("\\s+", ""));
            }
            addStringAttribute(xmlPullParser, "protocol", contentValues, "protocol");
            addStringAttribute(xmlPullParser, "roaming_protocol", contentValues, "roaming_protocol");
            addIntAttribute(xmlPullParser, "authtype", contentValues, "authtype");
            addIntAttribute(xmlPullParser, "bearer", contentValues, "bearer");
            addIntAttribute(xmlPullParser, "profile_id", contentValues, "profile_id");
            addIntAttribute(xmlPullParser, "max_conns", contentValues, "max_conns");
            addIntAttribute(xmlPullParser, "wait_time", contentValues, "wait_time");
            addIntAttribute(xmlPullParser, "max_conns_time", contentValues, "max_conns_time");
            addIntAttribute(xmlPullParser, "mtu", contentValues, "mtu");
            addIntAttribute(xmlPullParser, "apn_set_id", contentValues, "apn_set_id");
            addBoolAttribute(xmlPullParser, "carrier_enabled", contentValues, "carrier_enabled");
            addBoolAttribute(xmlPullParser, "modem_cognitive", contentValues, "modem_cognitive");
            addBoolAttribute(xmlPullParser, "user_visible", contentValues, "user_visible");
            addBoolAttribute(xmlPullParser, "user_editable", contentValues, "user_editable");
            String attributeValue5 = xmlPullParser.getAttributeValue(null, "network_type_bitmask");
            int bitmaskFromString2 = 0;
            if (attributeValue5 != null) {
                bitmaskFromString = ServiceState.getBitmaskFromString(attributeValue5);
            } else {
                bitmaskFromString = 0;
            }
            contentValues.put("network_type_bitmask", Integer.valueOf(bitmaskFromString));
            if (attributeValue5 != null) {
                iConvertNetworkTypeBitmaskToBearerBitmask = ServiceState.convertNetworkTypeBitmaskToBearerBitmask(bitmaskFromString);
            } else {
                String attributeValue6 = xmlPullParser.getAttributeValue(null, "bearer_bitmask");
                if (attributeValue6 != null) {
                    bitmaskFromString2 = ServiceState.getBitmaskFromString(attributeValue6);
                }
                iConvertNetworkTypeBitmaskToBearerBitmask = bitmaskFromString2;
                contentValues.put("network_type_bitmask", Integer.valueOf(ServiceState.convertBearerBitmaskToNetworkTypeBitmask(iConvertNetworkTypeBitmaskToBearerBitmask)));
            }
            contentValues.put("bearer_bitmask", Integer.valueOf(iConvertNetworkTypeBitmaskToBearerBitmask));
            addStringAttribute(xmlPullParser, "spn", contentValues, "spn");
            addStringAttribute(xmlPullParser, "imsi", contentValues, "imsi");
            addStringAttribute(xmlPullParser, "pnn", contentValues, "pnn");
            addStringAttribute(xmlPullParser, "ppp", contentValues, "ppp");
            String attributeValue7 = xmlPullParser.getAttributeValue(null, "mvno_type");
            if (attributeValue7 != null && (attributeValue = xmlPullParser.getAttributeValue(null, "mvno_match_data")) != null) {
                contentValues.put("mvno_type", attributeValue7);
                contentValues.put("mvno_match_data", attributeValue);
            }
            return contentValues;
        }

        private void addStringAttribute(XmlPullParser xmlPullParser, String str, ContentValues contentValues, String str2) {
            String attributeValue = xmlPullParser.getAttributeValue(null, str);
            if (attributeValue != null) {
                contentValues.put(str2, attributeValue);
            }
        }

        private void addIntAttribute(XmlPullParser xmlPullParser, String str, ContentValues contentValues, String str2) {
            String attributeValue = xmlPullParser.getAttributeValue(null, str);
            if (attributeValue != null) {
                contentValues.put(str2, Integer.valueOf(Integer.parseInt(attributeValue)));
            }
        }

        private void addBoolAttribute(XmlPullParser xmlPullParser, String str, ContentValues contentValues, String str2) {
            String attributeValue = xmlPullParser.getAttributeValue(null, str);
            if (attributeValue != null) {
                contentValues.put(str2, Boolean.valueOf(Boolean.parseBoolean(attributeValue)));
            }
        }

        private void loadApns(SQLiteDatabase sQLiteDatabase, XmlPullParser xmlPullParser) {
            try {
                if (xmlPullParser != null) {
                    try {
                        try {
                            sQLiteDatabase.beginTransaction();
                            XmlUtils.nextElement(xmlPullParser);
                            int defaultSubscriptionId = SubscriptionManager.getDefaultSubscriptionId();
                            while (xmlPullParser.getEventType() != 1) {
                                ContentValues row = getRow(xmlPullParser);
                                if (row != null) {
                                    insertAddingDefaults(sQLiteDatabase, row, defaultSubscriptionId);
                                }
                                XmlUtils.nextElement(xmlPullParser);
                            }
                            sQLiteDatabase.setTransactionSuccessful();
                        } catch (XmlPullParserException e) {
                            TelephonyProvider.loge("Got XmlPullParserException while loading apns." + e);
                        }
                    } catch (SQLException e2) {
                        TelephonyProvider.loge("Got SQLException while loading apns." + e2);
                    } catch (IOException e3) {
                        TelephonyProvider.loge("Got IOException while loading apns." + e3);
                    }
                }
            } finally {
                sQLiteDatabase.endTransaction();
            }
        }

        public static ContentValues setDefaultValue(ContentValues contentValues) {
            if (!contentValues.containsKey("sub_id")) {
                contentValues.put("sub_id", Integer.valueOf(SubscriptionManager.getDefaultSubscriptionId()));
            }
            return contentValues;
        }

        private ContentValues setDefaultValueWithSub(ContentValues contentValues, int i) {
            if (!contentValues.containsKey("sub_id")) {
                contentValues.put("sub_id", Integer.valueOf(i));
            }
            return contentValues;
        }

        private void insertAddingDefaults(SQLiteDatabase sQLiteDatabase, ContentValues contentValues, int i) {
            ContentValues defaultValueWithSub = setDefaultValueWithSub(contentValues, i);
            try {
                sQLiteDatabase.insertWithOnConflict("carriers", null, defaultValueWithSub, 2);
            } catch (SQLException e) {
                Cursor cursorSelectConflictingRow = selectConflictingRow(sQLiteDatabase, "carriers", defaultValueWithSub);
                if (cursorSelectConflictingRow != null) {
                    ContentValues contentValues2 = new ContentValues();
                    int i2 = cursorSelectConflictingRow.getInt(cursorSelectConflictingRow.getColumnIndex("edited"));
                    if (i2 != 0) {
                        if (i2 == 2) {
                            i2 = 3;
                        } else if (i2 == 5) {
                            i2 = 6;
                        }
                        contentValues2.put("edited", Integer.valueOf(i2));
                    }
                    mergeFieldsAndUpdateDb(sQLiteDatabase, "carriers", cursorSelectConflictingRow, defaultValueWithSub, contentValues2, false, this.mContext);
                    cursorSelectConflictingRow.close();
                }
            }
        }

        public static void mergeFieldsAndUpdateDb(SQLiteDatabase sQLiteDatabase, String str, Cursor cursor, ContentValues contentValues, ContentValues contentValues2, boolean z, Context context) {
            if (contentValues.containsKey("type")) {
                String string = cursor.getString(cursor.getColumnIndex("type"));
                String asString = contentValues.getAsString("type");
                if (!string.equalsIgnoreCase(asString)) {
                    if (string.equals("") || asString.equals("")) {
                        contentValues.put("type", "");
                    } else {
                        String[] strArrSplit = string.toLowerCase().split(",");
                        String[] strArrSplit2 = asString.toLowerCase().split(",");
                        if (separateRowsNeeded(sQLiteDatabase, str, cursor, contentValues, context, strArrSplit, strArrSplit2)) {
                            return;
                        }
                        ArrayList arrayList = new ArrayList();
                        arrayList.addAll(Arrays.asList(strArrSplit));
                        for (String str2 : strArrSplit2) {
                            if (!arrayList.contains(str2.trim())) {
                                arrayList.add(str2);
                            }
                        }
                        StringBuilder sb = new StringBuilder();
                        int i = 0;
                        while (i < arrayList.size()) {
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append(i == 0 ? "" : ",");
                            sb2.append((String) arrayList.get(i));
                            sb.append(sb2.toString());
                            i++;
                        }
                        contentValues.put("type", sb.toString());
                    }
                }
                contentValues2.put("type", contentValues.getAsString("type"));
            }
            if (contentValues.containsKey("bearer_bitmask")) {
                int i2 = cursor.getInt(cursor.getColumnIndex("bearer_bitmask"));
                int iIntValue = contentValues.getAsInteger("bearer_bitmask").intValue();
                if (i2 != iIntValue) {
                    if (i2 == 0 || iIntValue == 0) {
                        contentValues.put("bearer_bitmask", (Integer) 0);
                    } else {
                        contentValues.put("bearer_bitmask", Integer.valueOf(i2 | iIntValue));
                    }
                }
                contentValues2.put("bearer_bitmask", contentValues.getAsInteger("bearer_bitmask"));
            }
            if (contentValues.containsKey("network_type_bitmask")) {
                int i3 = cursor.getInt(cursor.getColumnIndex("network_type_bitmask"));
                int iIntValue2 = contentValues.getAsInteger("network_type_bitmask").intValue();
                if (i3 != iIntValue2) {
                    if (i3 == 0 || iIntValue2 == 0) {
                        contentValues.put("network_type_bitmask", (Integer) 0);
                    } else {
                        contentValues.put("network_type_bitmask", Integer.valueOf(i3 | iIntValue2));
                    }
                }
                contentValues2.put("network_type_bitmask", contentValues.getAsInteger("network_type_bitmask"));
            }
            if (contentValues.containsKey("bearer_bitmask") && contentValues.containsKey("network_type_bitmask")) {
                TelephonyProvider.syncBearerBitmaskAndNetworkTypeBitmask(contentValues2);
            }
            if (!z) {
                if (contentValues.containsKey("edited")) {
                    int i4 = cursor.getInt(cursor.getColumnIndex("edited"));
                    if (contentValues.getAsInteger("edited").intValue() == 0 && (i4 == 4 || i4 == 5 || i4 == 6 || i4 == 1 || i4 == 2 || i4 == 3)) {
                        contentValues.remove("edited");
                    }
                }
                contentValues2.putAll(contentValues);
            }
            if (contentValues2.size() > 0) {
                sQLiteDatabase.update(str, contentValues2, "_id=" + cursor.getInt(cursor.getColumnIndex("_id")), null);
            }
        }

        private static boolean separateRowsNeeded(SQLiteDatabase sQLiteDatabase, String str, Cursor cursor, ContentValues contentValues, Context context, String[] strArr, String[] strArr2) {
            boolean z;
            boolean z2;
            String[] stringArray = context.getResources().getStringArray(R.array.persist_apns_for_plmn);
            int length = stringArray.length;
            int i = 0;
            while (true) {
                if (i < length) {
                    if (!stringArray[i].equalsIgnoreCase(contentValues.getAsString("numeric"))) {
                        i++;
                    } else {
                        z = true;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (!z) {
                return false;
            }
            ArrayList<String> arrayList = new ArrayList(Arrays.asList(strArr));
            ArrayList arrayList2 = new ArrayList(Arrays.asList(strArr2));
            if (arrayList.size() != arrayList2.size() + 1) {
                if (arrayList.size() + 1 != arrayList2.size()) {
                    return false;
                }
                z2 = false;
                arrayList2 = arrayList;
                arrayList = arrayList2;
            } else {
                z2 = true;
            }
            if (arrayList.contains("dun") && !arrayList2.contains("dun")) {
                arrayList2.add("dun");
                if (!arrayList.containsAll(arrayList2) || cursor.getInt(cursor.getColumnIndex("profile_id")) != 0) {
                    return false;
                }
                if (z2) {
                    ContentValues contentValues2 = new ContentValues();
                    StringBuilder sb = new StringBuilder();
                    boolean z3 = true;
                    for (String str2 : arrayList) {
                        if (!str2.equalsIgnoreCase("dun")) {
                            if (!z3) {
                                str2 = "," + str2;
                            }
                            sb.append(str2);
                            z3 = false;
                        }
                    }
                    contentValues2.put("type", sb.toString());
                    sQLiteDatabase.update(str, contentValues2, "_id=" + cursor.getInt(cursor.getColumnIndex("_id")), null);
                    return true;
                }
                contentValues.put("profile_id", new Integer(1));
                try {
                    sQLiteDatabase.insertWithOnConflict(str, null, contentValues, 5);
                    return true;
                } catch (SQLException e) {
                    TelephonyProvider.loge("Exception on trying to add new row after updating profile_id");
                }
            }
            return false;
        }

        public static Cursor selectConflictingRow(SQLiteDatabase sQLiteDatabase, String str, ContentValues contentValues) {
            String str2;
            String asString;
            if (!contentValues.containsKey("numeric") || !contentValues.containsKey("mcc") || !contentValues.containsKey("mnc")) {
                TelephonyProvider.loge("dbh.selectConflictingRow: called for non-conflicting row: " + contentValues);
                return null;
            }
            String[] strArr = {"_id", "type", "edited", "bearer_bitmask", "network_type_bitmask", "profile_id"};
            String str3 = TextUtils.join("=? AND ", TelephonyProvider.CARRIERS_UNIQUE_FIELDS) + "=?";
            int i = 0;
            String[] strArr2 = new String[TelephonyProvider.CARRIERS_UNIQUE_FIELDS.size()];
            for (String str4 : TelephonyProvider.CARRIERS_UNIQUE_FIELDS) {
                if ("carrier_enabled".equals(str4)) {
                    int i2 = i + 1;
                    if (!contentValues.containsKey("carrier_enabled") || (!contentValues.getAsString("carrier_enabled").equals("0") && !contentValues.getAsString("carrier_enabled").equals("false"))) {
                        str2 = (String) TelephonyProvider.CARRIERS_UNIQUE_FIELDS_DEFAULTS.get("carrier_enabled");
                    } else {
                        str2 = "0";
                    }
                    strArr2[i] = str2;
                    i = i2;
                } else {
                    int i3 = i + 1;
                    if (!contentValues.containsKey(str4)) {
                        asString = (String) TelephonyProvider.CARRIERS_UNIQUE_FIELDS_DEFAULTS.get(str4);
                    } else {
                        asString = contentValues.getAsString(str4);
                    }
                    strArr2[i] = asString;
                    i = i3;
                }
            }
            Cursor cursorQuery = sQLiteDatabase.query(str, strArr, str3, strArr2, null, null, null);
            if (cursorQuery == null) {
                TelephonyProvider.loge("dbh.selectConflictingRow: Error - c is null; no matching row found for cv " + contentValues);
            } else {
                if (cursorQuery.getCount() != 1) {
                    TelephonyProvider.loge("dbh.selectConflictingRow: Expected 1 but found " + cursorQuery.getCount() + " matching rows found for cv " + contentValues);
                } else if (!cursorQuery.moveToFirst()) {
                    TelephonyProvider.loge("dbh.selectConflictingRow: moveToFirst() failed");
                } else {
                    return cursorQuery;
                }
                cursorQuery.close();
            }
            return null;
        }
    }

    SQLiteDatabase getReadableDatabase() {
        return this.mOpenHelper.getReadableDatabase();
    }

    SQLiteDatabase getWritableDatabase() {
        return this.mOpenHelper.getWritableDatabase();
    }

    void initDatabaseWithDatabaseHelper(SQLiteDatabase sQLiteDatabase) throws Throwable {
        this.mOpenHelper.initDatabase(sQLiteDatabase);
    }

    boolean needApnDbUpdate() {
        return this.mOpenHelper.apnDbUpdateNeeded();
    }

    private static boolean apnSourceServiceExists(Context context) {
        if (s_apnSourceServiceExists != null) {
            return s_apnSourceServiceExists.booleanValue();
        }
        try {
            String string = context.getResources().getString(R.string.apn_source_service);
            if (TextUtils.isEmpty(string)) {
                s_apnSourceServiceExists = false;
            } else {
                s_apnSourceServiceExists = Boolean.valueOf(context.getPackageManager().getServiceInfo(ComponentName.unflattenFromString(string), 0) != null);
            }
        } catch (PackageManager.NameNotFoundException e) {
            s_apnSourceServiceExists = false;
        }
        return s_apnSourceServiceExists.booleanValue();
    }

    private void restoreApnsWithService() {
        Context context = getContext();
        Resources resources = context.getResources();
        ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                TelephonyProvider.log("restoreApnsWithService: onServiceConnected");
                synchronized (TelephonyProvider.this.mLock) {
                    TelephonyProvider.this.mIApnSourceService = IApnSourceService.Stub.asInterface(iBinder);
                    TelephonyProvider.this.mLock.notifyAll();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                TelephonyProvider.loge("mIApnSourceService has disconnected unexpectedly");
                synchronized (TelephonyProvider.this.mLock) {
                    TelephonyProvider.this.mIApnSourceService = null;
                }
            }
        };
        Intent intent = new Intent(IApnSourceService.class.getName());
        intent.setComponent(ComponentName.unflattenFromString(resources.getString(R.string.apn_source_service)));
        log("binding to service to restore apns, intent=" + intent);
        try {
            try {
                if (context.bindService(intent, serviceConnection, 1)) {
                    synchronized (this.mLock) {
                        while (this.mIApnSourceService == null) {
                            try {
                                this.mLock.wait();
                            } catch (InterruptedException e) {
                                loge("Error while waiting for service connection: " + e);
                            }
                        }
                        try {
                            ContentValues[] apns = this.mIApnSourceService.getApns();
                            if (apns != null) {
                                unsynchronizedBulkInsert(MtkTelephony.Carriers.CONTENT_URI, apns);
                                log("restoreApnsWithService: restored");
                            }
                        } catch (RemoteException e2) {
                            loge("Error applying apns from service: " + e2);
                        }
                    }
                } else {
                    loge("unable to bind to service from intent=" + intent);
                }
                context.unbindService(serviceConnection);
                synchronized (this.mLock) {
                    this.mIApnSourceService = null;
                }
            } catch (SecurityException e3) {
                loge("Error applying apns from service: " + e3);
                context.unbindService(serviceConnection);
                synchronized (this.mLock) {
                    this.mIApnSourceService = null;
                }
            }
        } catch (Throwable th) {
            context.unbindService(serviceConnection);
            synchronized (this.mLock) {
                this.mIApnSourceService = null;
                throw th;
            }
        }
    }

    @Override
    public boolean onCreate() {
        this.mOpenHelper = new DatabaseHelper(getContext());
        if (!apnSourceServiceExists(getContext())) {
            getReadableDatabase();
            String str = SystemProperties.get("ro.build.id", (String) null);
            if (!TextUtils.isEmpty(str)) {
                SharedPreferences sharedPreferences = getContext().getSharedPreferences("build-id", 0);
                String string = sharedPreferences.getString("ro_build_id", "");
                if (!str.equals(string)) {
                    log("onCreate: build id changed from " + string + " to " + str);
                    SubscriptionManager subscriptionManagerFrom = SubscriptionManager.from(getContext());
                    if (subscriptionManagerFrom != null) {
                        for (SubscriptionInfo subscriptionInfo : subscriptionManagerFrom.getAllSubscriptionInfoList()) {
                            SharedPreferences sharedPreferences2 = getContext().getSharedPreferences("preferred-apn" + subscriptionInfo.getSubscriptionId(), 0);
                            if (sharedPreferences2 != null) {
                                SharedPreferences.Editor editorEdit = sharedPreferences2.edit();
                                editorEdit.clear();
                                editorEdit.apply();
                            }
                        }
                    }
                    updateApnDb();
                }
                sharedPreferences.edit().putString("ro_build_id", str).apply();
            }
        }
        this.mManagedApnEnforced = getContext().getSharedPreferences("dpc-apn-enforced", 0).getBoolean("enforced", false);
        return true;
    }

    private synchronized boolean isManagedApnEnforced() {
        return this.mManagedApnEnforced;
    }

    private void setManagedApnEnforced(boolean z) {
        SharedPreferences.Editor editorEdit = getContext().getSharedPreferences("dpc-apn-enforced", 0).edit();
        editorEdit.putBoolean("enforced", z);
        editorEdit.apply();
        synchronized (this) {
            this.mManagedApnEnforced = z;
        }
    }

    private void setPreferredApnId(Long l, int i, boolean z) {
        SharedPreferences.Editor editorEdit = getContext().getSharedPreferences("preferred-apn", 0).edit();
        editorEdit.putLong("apn_id" + i, l != null ? l.longValue() : -1L);
        editorEdit.putBoolean("explicit_set_called" + i, z);
        editorEdit.apply();
        if (l == null || l.longValue() == -1) {
            deletePreferredApn(i);
        } else if (z) {
            setPreferredApn(l, i);
        }
    }

    private long getPreferredApnId(int i, boolean z) {
        long preferredApnIdFromApn = getContext().getSharedPreferences("preferred-apn", 0).getLong("apn_id" + i, -1L);
        if (preferredApnIdFromApn == -1 && z) {
            preferredApnIdFromApn = getPreferredApnIdFromApn(i);
            if (preferredApnIdFromApn != -1) {
                setPreferredApnId(Long.valueOf(preferredApnIdFromApn), i, false);
            }
        }
        return preferredApnIdFromApn;
    }

    private int getPreferredApnSetId(int i) {
        try {
            return Integer.parseInt(getContext().getSharedPreferences("preferred-full-apn", 0).getString("apn_set_id" + i, null));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void deletePreferredApnId() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("preferred-apn", 0);
        for (String str : sharedPreferences.getAll().keySet()) {
            try {
                int i = Integer.parseInt(str.replace("apn_id", ""));
                long preferredApnId = getPreferredApnId(i, false);
                if (preferredApnId != -1) {
                    setPreferredApn(Long.valueOf(preferredApnId), i);
                }
            } catch (Exception e) {
                loge("Skipping over key " + str + " due to exception " + e);
            }
        }
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        editorEdit.clear();
        editorEdit.apply();
    }

    private void setPreferredApn(Long l, int i) {
        log("setPreferredApn: _id " + l + " subId " + i);
        Cursor cursorQuery = getWritableDatabase().query("carriers", (String[]) CARRIERS_UNIQUE_FIELDS.toArray(new String[CARRIERS_UNIQUE_FIELDS.size()]), "_id=" + l, null, null, null, null);
        if (cursorQuery != null) {
            if (cursorQuery.getCount() == 1) {
                cursorQuery.moveToFirst();
                SharedPreferences.Editor editorEdit = getContext().getSharedPreferences("preferred-full-apn", 0).edit();
                for (String str : CARRIERS_UNIQUE_FIELDS) {
                    editorEdit.putString(str + i, cursorQuery.getString(cursorQuery.getColumnIndex(str)));
                }
                editorEdit.putString("version" + i, "1704192");
                editorEdit.apply();
            } else {
                log("setPreferredApn: # matching APNs found " + cursorQuery.getCount());
            }
            cursorQuery.close();
            return;
        }
        log("setPreferredApn: No matching APN found");
    }

    private long getPreferredApnIdFromApn(int i) {
        log("getPreferredApnIdFromApn: for subId " + i);
        SQLiteDatabase writableDatabase = getWritableDatabase();
        String str = TextUtils.join("=? and ", CARRIERS_UNIQUE_FIELDS) + "=?";
        String[] strArr = new String[CARRIERS_UNIQUE_FIELDS.size()];
        int i2 = 0;
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("preferred-full-apn", 0);
        Iterator<String> it = CARRIERS_UNIQUE_FIELDS.iterator();
        while (true) {
            long j = -1;
            if (it.hasNext()) {
                strArr[i2] = sharedPreferences.getString(it.next() + i, null);
                if (strArr[i2] == null) {
                    return -1L;
                }
                i2++;
            } else {
                Cursor cursorQuery = writableDatabase.query("carriers", new String[]{"_id"}, str, strArr, null, null, null);
                if (cursorQuery != null) {
                    if (cursorQuery.getCount() == 1) {
                        cursorQuery.moveToFirst();
                        j = cursorQuery.getInt(cursorQuery.getColumnIndex("_id"));
                    } else {
                        log("getPreferredApnIdFromApn: returning INVALID. # matching APNs found " + cursorQuery.getCount());
                    }
                    cursorQuery.close();
                } else {
                    log("getPreferredApnIdFromApn: returning INVALID. No matching APN found");
                }
                return j;
            }
        }
    }

    private void deletePreferredApn(int i) {
        log("deletePreferredApn: for subId " + i);
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("preferred-full-apn", 0);
        if (sharedPreferences.contains("version" + i)) {
            log("deletePreferredApn: apn is stored. Deleting it now for subId " + i);
            SharedPreferences.Editor editorEdit = sharedPreferences.edit();
            editorEdit.remove("version" + i);
            Iterator<String> it = CARRIERS_UNIQUE_FIELDS.iterator();
            while (it.hasNext()) {
                editorEdit.remove(it.next() + i);
            }
            editorEdit.apply();
        }
    }

    boolean isCallingFromSystemOrPhoneUid() {
        return this.mInjector.binderGetCallingUid() == 1000 || this.mInjector.binderGetCallingUid() == 1001;
    }

    void ensureCallingFromSystemOrPhoneUid(String str) {
        if (!isCallingFromSystemOrPhoneUid()) {
            throw new SecurityException(str);
        }
    }

    @Override
    public synchronized Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        String str3;
        boolean zContainsSensitiveFields;
        String str4;
        TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService("phone");
        int defaultSubscriptionId = SubscriptionManager.getDefaultSubscriptionId();
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        sQLiteQueryBuilder.setStrict(true);
        sQLiteQueryBuilder.setTables("carriers");
        ArrayList arrayList = new ArrayList();
        int iMatch = s_urlMatcher.match(uri);
        int i = 0;
        Cursor cursorQuery = null;
        if (iMatch != 16) {
            switch (iMatch) {
                case 1:
                    arrayList.add("owned_by!=0");
                    break;
                case 2:
                    arrayList.add("current IS NOT NULL");
                    arrayList.add("owned_by!=0");
                    break;
                case 3:
                    arrayList.add("_id = " + uri.getPathSegments().get(1));
                    arrayList.add("owned_by!=0");
                    break;
                default:
                    switch (iMatch) {
                        case 5:
                        case 6:
                            arrayList.add("_id = " + getPreferredApnId(defaultSubscriptionId, true));
                            break;
                        case 7:
                            sQLiteQueryBuilder.setTables("siminfo");
                            break;
                        case 8:
                            String lastPathSegment = uri.getLastPathSegment();
                            try {
                                int i2 = Integer.parseInt(lastPathSegment);
                                log("query URL_TELEPHONY_USING_SUBID, subIdString=" + lastPathSegment + ", subId=" + i2);
                                StringBuilder sb = new StringBuilder();
                                sb.append("numeric = '");
                                sb.append(telephonyManager.getSimOperator(i2));
                                sb.append("'");
                                arrayList.add(sb.toString());
                                arrayList.add("owned_by!=0");
                            } catch (NumberFormatException e) {
                                loge("NumberFormatException" + e);
                                return null;
                            }
                            break;
                        case 9:
                            String lastPathSegment2 = uri.getLastPathSegment();
                            try {
                                log("query URL_CURRENT_USING_SUBID, subIdString=" + lastPathSegment2 + ", subId=" + Integer.parseInt(lastPathSegment2));
                                arrayList.add("current IS NOT NULL");
                                arrayList.add("owned_by!=0");
                            } catch (NumberFormatException e2) {
                                loge("NumberFormatException" + e2);
                                return null;
                            }
                            break;
                        default:
                            switch (iMatch) {
                                case 11:
                                case 12:
                                    String lastPathSegment3 = uri.getLastPathSegment();
                                    try {
                                        defaultSubscriptionId = Integer.parseInt(lastPathSegment3);
                                        log("query URL_PREFERAPN_USING_SUBID or URL_PREFERAPN_NO_UPDATE_USING_SUBID, subIdString=" + lastPathSegment3 + ", subId=" + defaultSubscriptionId);
                                        arrayList.add("_id = " + getPreferredApnId(defaultSubscriptionId, true));
                                    } catch (NumberFormatException e3) {
                                        loge("NumberFormatException" + e3);
                                        return null;
                                    }
                                    break;
                                default:
                                    switch (iMatch) {
                                        case 19:
                                            arrayList.add("_id = " + uri.getLastPathSegment());
                                        case 18:
                                            if (isManagedApnEnforced()) {
                                                arrayList.add("owned_by=0");
                                            } else {
                                                arrayList.add("owned_by!=0");
                                            }
                                            break;
                                        case 20:
                                            ensureCallingFromSystemOrPhoneUid("URL_ENFORCE_MANAGED called from non SYSTEM_UID.");
                                            MatrixCursor matrixCursor = new MatrixCursor(new String[]{"enforced"});
                                            matrixCursor.addRow(new Object[]{Integer.valueOf(isManagedApnEnforced() ? 1 : 0)});
                                            return matrixCursor;
                                        case 22:
                                            String lastPathSegment4 = uri.getLastPathSegment();
                                            try {
                                                defaultSubscriptionId = Integer.parseInt(lastPathSegment4);
                                                log("query URL_PREFERAPNSET_USING_SUBID, subIdString=" + lastPathSegment4 + ", subId=" + defaultSubscriptionId);
                                                break;
                                            } catch (NumberFormatException e4) {
                                                loge("NumberFormatException" + e4);
                                                return null;
                                            }
                                        case 21:
                                            int preferredApnSetId = getPreferredApnSetId(defaultSubscriptionId);
                                            if (preferredApnSetId != 0) {
                                                arrayList.add("apn_set_id=" + preferredApnSetId);
                                            }
                                            break;
                                        default:
                                            switch (iMatch) {
                                                case 51:
                                                    sQLiteQueryBuilder.appendWhere("_id = " + getPreferredApnId(defaultSubscriptionId, true));
                                                    break;
                                                case 52:
                                                    sQLiteQueryBuilder.setTables("carriers_dm");
                                                    break;
                                                case 53:
                                                    sQLiteQueryBuilder.setTables("carriers_dm");
                                                    sQLiteQueryBuilder.appendWhere("_id = " + uri.getPathSegments().get(1));
                                                    break;
                                                default:
                                                    return null;
                                            }
                                            break;
                                    }
                                    break;
                            }
                            break;
                    }
                    break;
            }
        } else {
            ensureCallingFromSystemOrPhoneUid("URL_DPC called from non SYSTEM_UID.");
            arrayList.add("owned_by=0");
        }
        if (arrayList.size() > 0) {
            sQLiteQueryBuilder.appendWhere(TextUtils.join(" AND ", arrayList));
        }
        try {
            str3 = str2;
        } catch (Exception e5) {
            str3 = str2;
        }
        try {
            zContainsSensitiveFields = containsSensitiveFields(str) | containsSensitiveFields(str3);
        } catch (Exception e6) {
            zContainsSensitiveFields = true;
        }
        if (zContainsSensitiveFields) {
            try {
                checkPermission();
                if (iMatch == 7) {
                    if (strArr != null) {
                        int length = strArr.length;
                        while (true) {
                            if (i < length) {
                                String str5 = strArr[i];
                                if (!"type".equals(str5) && !"mmsc".equals(str5) && !"mmsproxy".equals(str5) && !"mmsport".equals(str5) && !"apn".equals(str5)) {
                                    checkPermission();
                                }
                                i++;
                            }
                        }
                    } else {
                        checkPermission();
                    }
                } else {
                    checkReadSimInfoPermission();
                }
                SQLiteDatabase readableDatabase = getReadableDatabase();
                try {
                    if (!"carriers".equals(sQLiteQueryBuilder.getTables())) {
                        str4 = (TextUtils.isEmpty(str) ? "" : str + " and ") + "edited!=2 and edited!=3 and edited!=5 and edited!=6";
                    } else {
                        str4 = str;
                    }
                    cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, str4, strArr2, null, null, str3);
                } catch (SQLException e7) {
                    loge("got exception when querying: " + e7);
                }
                if (cursorQuery != null) {
                    cursorQuery.setNotificationUri(getContext().getContentResolver(), uri);
                }
                return cursorQuery;
            } catch (SecurityException e8) {
                EventLog.writeEvent(1397638484, "124107808", Integer.valueOf(Binder.getCallingUid()));
                throw e8;
            }
        }
        if (iMatch == 7) {
        }
        SQLiteDatabase readableDatabase2 = getReadableDatabase();
        if (!"carriers".equals(sQLiteQueryBuilder.getTables())) {
        }
        cursorQuery = sQLiteQueryBuilder.query(readableDatabase2, strArr, str4, strArr2, null, null, str3);
        if (cursorQuery != null) {
        }
        return cursorQuery;
    }

    private boolean containsSensitiveFields(String str) {
        try {
            SqlTokenFinder.findTokens(str, new Consumer() {
                @Override
                public final void accept(Object obj) {
                    TelephonyProvider.lambda$containsSensitiveFields$0((String) obj);
                }
            });
            return false;
        } catch (SecurityException e) {
            return true;
        }
    }

    static void lambda$containsSensitiveFields$0(String str) {
        byte b;
        String lowerCase = str.toLowerCase();
        int iHashCode = lowerCase.hashCode();
        if (iHashCode != -906021636) {
            if (iHashCode != 3599307) {
                b = (iHashCode == 1216985755 && lowerCase.equals("password")) ? (byte) 1 : (byte) -1;
            } else if (lowerCase.equals("user")) {
                b = 0;
            }
        } else if (lowerCase.equals("select")) {
            b = 2;
        }
        switch (b) {
            case 0:
            case 1:
            case 2:
                throw new SecurityException();
            default:
                return;
        }
    }

    @Override
    public String getType(Uri uri) {
        if (BenesseExtension.getDchaState() != 0) {
            throw new IllegalArgumentException("Unknown URL " + uri);
        }
        switch (s_urlMatcher.match(uri)) {
            case 1:
            case 8:
                return "vnd.android.cursor.dir/telephony-carrier";
            case 3:
            case 19:
                return "vnd.android.cursor.item/telephony-carrier";
            case 5:
            case 6:
            case 11:
            case 12:
            case 21:
            case 22:
                return "vnd.android.cursor.item/telephony-carrier";
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    @Override
    public synchronized int bulkInsert(Uri uri, ContentValues[] contentValuesArr) {
        return unsynchronizedBulkInsert(uri, contentValuesArr);
    }

    private int unsynchronizedBulkInsert(Uri uri, ContentValues[] contentValuesArr) {
        boolean z = false;
        int i = 0;
        for (ContentValues contentValues : contentValuesArr) {
            Pair<Uri, Boolean> pairInsertSingleRow = insertSingleRow(uri, contentValues);
            if (pairInsertSingleRow.first != null) {
                i++;
            }
            if (((Boolean) pairInsertSingleRow.second).booleanValue()) {
                z = true;
            }
        }
        if (z) {
            getContext().getContentResolver().notifyChange(MtkTelephony.Carriers.CONTENT_URI, null, true, -1);
        }
        return i;
    }

    @Override
    public synchronized Uri insert(Uri uri, ContentValues contentValues) {
        Pair<Uri, Boolean> pairInsertSingleRow;
        log("insert, match=" + s_urlMatcher.match(uri) + ", binderPid=" + Binder.getCallingPid() + ", binderUid=" + Binder.getCallingUid());
        pairInsertSingleRow = insertSingleRow(uri, contentValues);
        if (((Boolean) pairInsertSingleRow.second).booleanValue()) {
            getContext().getContentResolver().notifyChange(MtkTelephony.Carriers.CONTENT_URI, null, true, -1);
        }
        log("insert, rowId=" + pairInsertSingleRow.first + ", notify=" + pairInsertSingleRow.second);
        return (Uri) pairInsertSingleRow.first;
    }

    private Pair<Uri, Boolean> insertRowWithValue(ContentValues contentValues) {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        boolean z = true;
        Uri uriWithAppendedId = null;
        try {
            long jInsertWithOnConflict = writableDatabase.insertWithOnConflict("carriers", null, contentValues, 2);
            if (jInsertWithOnConflict >= 0) {
                uriWithAppendedId = ContentUris.withAppendedId(MtkTelephony.Carriers.CONTENT_URI, jInsertWithOnConflict);
            } else {
                z = false;
            }
        } catch (SQLException e) {
            log("insert: exception " + e);
            Cursor cursorSelectConflictingRow = DatabaseHelper.selectConflictingRow(writableDatabase, "carriers", contentValues);
            if (cursorSelectConflictingRow != null) {
                DatabaseHelper.mergeFieldsAndUpdateDb(writableDatabase, "carriers", cursorSelectConflictingRow, contentValues, new ContentValues(), false, getContext());
                cursorSelectConflictingRow.close();
            } else {
                z = false;
            }
        }
        return Pair.create(uriWithAppendedId, Boolean.valueOf(z));
    }

    private Pair<Uri, Boolean> insertSingleRow(Uri uri, ContentValues contentValues) {
        ContentValues contentValues2;
        ContentValues defaultValue;
        String asString;
        ContentValues contentValues3;
        ContentValues contentValues4;
        int defaultSubscriptionId = SubscriptionManager.getDefaultSubscriptionId();
        checkPermission();
        syncBearerBitmaskAndNetworkTypeBitmask(contentValues);
        SQLiteDatabase writableDatabase = getWritableDatabase();
        boolean zBooleanValue = false;
        Uri uriWithAppendedId = null;
        switch (s_urlMatcher.match(uri)) {
            case 1:
                if (contentValues == null) {
                    contentValues2 = new ContentValues(contentValues);
                } else {
                    contentValues2 = new ContentValues();
                }
                defaultValue = DatabaseHelper.setDefaultValue(contentValues2);
                if (!defaultValue.containsKey("edited")) {
                    defaultValue.put("edited", (Integer) 4);
                }
                defaultValue.put("owned_by", (Integer) 1);
                Pair<Uri, Boolean> pairInsertRowWithValue = insertRowWithValue(defaultValue);
                uriWithAppendedId = (Uri) pairInsertRowWithValue.first;
                zBooleanValue = ((Boolean) pairInsertRowWithValue.second).booleanValue();
                break;
            case 2:
                writableDatabase.update("carriers", s_currentNullMap, "current!=0", null);
                asString = contentValues.getAsString("numeric");
                if (writableDatabase.update("carriers", s_currentSetMap, "numeric = '" + asString + "'", null) <= 0) {
                    loge("Failed setting numeric '" + asString + "' to the current operator");
                }
                break;
            case 5:
            case 6:
                if (contentValues != null && contentValues.containsKey("apn_id")) {
                    setPreferredApnId(contentValues.getAsLong("apn_id"), defaultSubscriptionId, true);
                }
                break;
            case 7:
                uriWithAppendedId = ContentUris.withAppendedId(SubscriptionManager.CONTENT_URI, writableDatabase.insert("siminfo", null, contentValues));
                break;
            case 8:
                String lastPathSegment = uri.getLastPathSegment();
                try {
                    log("insertSingleRow URL_TELEPHONY_USING_SUBID, subIdString=" + lastPathSegment + ", subId=" + Integer.parseInt(lastPathSegment));
                    if (contentValues == null) {
                    }
                    defaultValue = DatabaseHelper.setDefaultValue(contentValues2);
                    if (!defaultValue.containsKey("edited")) {
                    }
                    defaultValue.put("owned_by", (Integer) 1);
                    Pair<Uri, Boolean> pairInsertRowWithValue2 = insertRowWithValue(defaultValue);
                    uriWithAppendedId = (Uri) pairInsertRowWithValue2.first;
                    zBooleanValue = ((Boolean) pairInsertRowWithValue2.second).booleanValue();
                } catch (NumberFormatException e) {
                    loge("NumberFormatException" + e);
                    return Pair.create(null, false);
                }
                break;
            case 9:
                String lastPathSegment2 = uri.getLastPathSegment();
                try {
                    log("insertSingleRow URL_CURRENT_USING_SUBID, subIdString=" + lastPathSegment2 + ", subId=" + Integer.parseInt(lastPathSegment2));
                    writableDatabase.update("carriers", s_currentNullMap, "current!=0", null);
                    asString = contentValues.getAsString("numeric");
                    if (writableDatabase.update("carriers", s_currentSetMap, "numeric = '" + asString + "'", null) <= 0) {
                    }
                } catch (NumberFormatException e2) {
                    loge("NumberFormatException" + e2);
                    return Pair.create(null, false);
                }
                break;
            case 11:
            case 12:
                String lastPathSegment3 = uri.getLastPathSegment();
                try {
                    defaultSubscriptionId = Integer.parseInt(lastPathSegment3);
                    log("insertSingleRow URL_PREFERAPN_USING_SUBID or URL_PREFERAPN_NO_UPDATE_USING_SUBID, subIdString=" + lastPathSegment3 + ", subId=" + defaultSubscriptionId);
                    if (contentValues != null) {
                        setPreferredApnId(contentValues.getAsLong("apn_id"), defaultSubscriptionId, true);
                    }
                } catch (NumberFormatException e3) {
                    loge("NumberFormatException" + e3);
                    return Pair.create(null, false);
                }
                break;
            case 16:
                ensureCallingFromSystemOrPhoneUid("URL_DPC called from non SYSTEM_UID.");
                if (contentValues != null) {
                    contentValues3 = new ContentValues(contentValues);
                } else {
                    contentValues3 = new ContentValues();
                }
                contentValues3.put("owned_by", (Integer) 0);
                contentValues3.put("user_editable", (Boolean) false);
                long jInsertWithOnConflict = writableDatabase.insertWithOnConflict("carriers", null, contentValues3, 4);
                if (jInsertWithOnConflict >= 0) {
                    uriWithAppendedId = ContentUris.withAppendedId(MtkTelephony.Carriers.CONTENT_URI, jInsertWithOnConflict);
                    zBooleanValue = true;
                }
                break;
            case 51:
                if (contentValues != null && contentValues.containsKey("apn_id")) {
                    setPreferredApnId(contentValues.getAsLong("apn_id"), defaultSubscriptionId, true);
                }
                break;
            case 52:
                if (contentValues != null) {
                    contentValues4 = new ContentValues(contentValues);
                } else {
                    contentValues4 = new ContentValues();
                }
                ContentValues defaultValue2 = DatabaseHelper.setDefaultValue(contentValues4);
                if (defaultValue2.containsKey("mcc") && defaultValue2.containsKey("mnc")) {
                    defaultValue2.put("numeric", defaultValue2.getAsString("mcc") + defaultValue2.getAsString("mnc"));
                }
                long jInsert = writableDatabase.insert("carriers_dm", null, defaultValue2);
                if (jInsert > 0) {
                    uriWithAppendedId = ContentUris.withAppendedId(MtkTelephony.Carriers.CONTENT_URI_DM, jInsert);
                    zBooleanValue = true;
                }
                break;
        }
        return Pair.create(null, false);
    }

    @Override
    public synchronized int delete(Uri uri, String str, String[] strArr) {
        int iDelete;
        int defaultSubscriptionId = SubscriptionManager.getDefaultSubscriptionId();
        ContentValues contentValues = new ContentValues();
        contentValues.put("edited", (Integer) 2);
        checkPermission();
        SQLiteDatabase writableDatabase = getWritableDatabase();
        int iMatch = s_urlMatcher.match(uri);
        log("delete, match=" + iMatch + ", binderPid=" + Binder.getCallingPid() + ", binderUid=" + Binder.getCallingUid());
        if (iMatch != 17) {
            switch (iMatch) {
                case 1:
                    iDelete = writableDatabase.delete("carriers", "(" + str + ") and (edited=1 or edited=4) and owned_by!=0", strArr) + writableDatabase.update("carriers", contentValues, "(" + str + ") and (edited!=1 and edited!=4) and owned_by!=0", strArr);
                    break;
                case 2:
                    iDelete = writableDatabase.delete("carriers", "(" + str + ") and (edited=1 or edited=4) and owned_by!=0", strArr) + writableDatabase.update("carriers", contentValues, "(" + str + ") and (edited!=1 and edited!=4) and owned_by!=0", strArr);
                    break;
                case 3:
                    iDelete = writableDatabase.delete("carriers", "(_id=?) and (edited=1 or edited=4) and owned_by!=0", new String[]{uri.getLastPathSegment()}) + writableDatabase.update("carriers", contentValues, "(_id=?) and (edited!=1 and edited!=4) and owned_by!=0", new String[]{uri.getLastPathSegment()});
                    break;
                case 4:
                    restoreDefaultAPN(defaultSubscriptionId);
                    getContext().getContentResolver().notifyChange(Uri.withAppendedPath(MtkTelephony.Carriers.CONTENT_URI, "restore/subId/" + defaultSubscriptionId), null, true, -1);
                    iDelete = 1;
                    break;
                case 5:
                case 6:
                    setPreferredApnId(-1L, defaultSubscriptionId, true);
                    if (iMatch != 5 && iMatch != 11) {
                        iDelete = 0;
                    }
                    iDelete = 1;
                    break;
                case 7:
                    iDelete = writableDatabase.delete("siminfo", str, strArr);
                    break;
                case 8:
                    String lastPathSegment = uri.getLastPathSegment();
                    try {
                        log("delete URL_TELEPHONY_USING_SUBID, subIdString=" + lastPathSegment + ", subId=" + Integer.parseInt(lastPathSegment));
                        iDelete = writableDatabase.delete("carriers", "(" + str + ") and (edited=1 or edited=4) and owned_by!=0", strArr) + writableDatabase.update("carriers", contentValues, "(" + str + ") and (edited!=1 and edited!=4) and owned_by!=0", strArr);
                    } catch (NumberFormatException e) {
                        loge("NumberFormatException" + e);
                        throw new IllegalArgumentException("Invalid subId " + uri);
                    }
                    break;
                case 9:
                    String lastPathSegment2 = uri.getLastPathSegment();
                    try {
                        log("delete URL_CURRENT_USING_SUBID, subIdString=" + lastPathSegment2 + ", subId=" + Integer.parseInt(lastPathSegment2));
                        iDelete = writableDatabase.delete("carriers", "(" + str + ") and (edited=1 or edited=4) and owned_by!=0", strArr) + writableDatabase.update("carriers", contentValues, "(" + str + ") and (edited!=1 and edited!=4) and owned_by!=0", strArr);
                    } catch (NumberFormatException e2) {
                        loge("NumberFormatException" + e2);
                        throw new IllegalArgumentException("Invalid subId " + uri);
                    }
                    break;
                case 10:
                    String lastPathSegment3 = uri.getLastPathSegment();
                    try {
                        defaultSubscriptionId = Integer.parseInt(lastPathSegment3);
                        log("delete URL_RESTOREAPN_USING_SUBID, subIdString=" + lastPathSegment3 + ", subId=" + defaultSubscriptionId);
                        restoreDefaultAPN(defaultSubscriptionId);
                        getContext().getContentResolver().notifyChange(Uri.withAppendedPath(MtkTelephony.Carriers.CONTENT_URI, "restore/subId/" + defaultSubscriptionId), null, true, -1);
                        iDelete = 1;
                    } catch (NumberFormatException e3) {
                        loge("NumberFormatException" + e3);
                        throw new IllegalArgumentException("Invalid subId " + uri);
                    }
                    break;
                case 11:
                case 12:
                    String lastPathSegment4 = uri.getLastPathSegment();
                    try {
                        defaultSubscriptionId = Integer.parseInt(lastPathSegment4);
                        log("delete URL_PREFERAPN_USING_SUBID or URL_PREFERAPN_NO_UPDATE_USING_SUBID, subIdString=" + lastPathSegment4 + ", subId=" + defaultSubscriptionId);
                        setPreferredApnId(-1L, defaultSubscriptionId, true);
                        if (iMatch != 5) {
                            iDelete = 0;
                        }
                        iDelete = 1;
                    } catch (NumberFormatException e4) {
                        loge("NumberFormatException" + e4);
                        throw new IllegalArgumentException("Invalid subId " + uri);
                    }
                    break;
                default:
                    switch (iMatch) {
                        case 14:
                            updateApnDb();
                            iDelete = 1;
                            break;
                        case 15:
                            deletePreferredApnId();
                            iDelete = writableDatabase.delete("carriers", "(" + str + ") and edited=0 and owned_by!=0", strArr);
                            break;
                        default:
                            switch (iMatch) {
                                case 51:
                                    setPreferredApnId(-1L, defaultSubscriptionId, true);
                                    iDelete = 1;
                                    break;
                                case 52:
                                    iDelete = writableDatabase.delete("carriers_dm", str, strArr);
                                    if (iDelete > 0) {
                                        getContext().getContentResolver().notifyChange(MtkTelephony.Carriers.CONTENT_URI_DM, null);
                                    }
                                    break;
                                default:
                                    throw new UnsupportedOperationException("Cannot delete that URL: " + uri);
                            }
                            break;
                    }
                    break;
            }
        } else {
            ensureCallingFromSystemOrPhoneUid("URL_DPC_ID called from non SYSTEM_UID.");
            iDelete = writableDatabase.delete("carriers", "(_id=?) and owned_by=0", new String[]{uri.getLastPathSegment()});
        }
        if (iDelete > 0) {
            getContext().getContentResolver().notifyChange(MtkTelephony.Carriers.CONTENT_URI, null, true, -1);
        }
        return iDelete;
    }

    @Override
    public synchronized int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        int iUpdateWithOnConflict;
        int defaultSubscriptionId = SubscriptionManager.getDefaultSubscriptionId();
        checkPermission();
        syncBearerBitmaskAndNetworkTypeBitmask(contentValues);
        SQLiteDatabase writableDatabase = getWritableDatabase();
        int iMatch = s_urlMatcher.match(uri);
        log("update, match=" + iMatch + ", binderPid=" + Binder.getCallingPid() + ", binderUid=" + Binder.getCallingUid());
        char c = 0;
        if (iMatch == 17) {
            ensureCallingFromSystemOrPhoneUid("URL_DPC_ID called from non SYSTEM_UID.");
            if (str != null || strArr != null) {
                throw new UnsupportedOperationException("Cannot update URL " + uri + " with a where clause");
            }
            iUpdateWithOnConflict = writableDatabase.updateWithOnConflict("carriers", contentValues, "_id=? and owned_by=0", new String[]{uri.getLastPathSegment()}, 4);
        } else if (iMatch != 20) {
            switch (iMatch) {
                case 1:
                    if (!contentValues.containsKey("edited")) {
                        contentValues.put("edited", (Integer) 4);
                    }
                    iUpdateWithOnConflict = writableDatabase.updateWithOnConflict("carriers", contentValues, str + " and owned_by!=0", strArr, 5);
                    break;
                case 2:
                    if (!contentValues.containsKey("edited")) {
                        contentValues.put("edited", (Integer) 4);
                    }
                    iUpdateWithOnConflict = writableDatabase.updateWithOnConflict("carriers", contentValues, str + " and owned_by!=0", strArr, 5);
                    break;
                case 3:
                    String lastPathSegment = uri.getLastPathSegment();
                    if (str != null || strArr != null) {
                        throw new UnsupportedOperationException("Cannot update URL " + uri + " with a where clause");
                    }
                    if (!contentValues.containsKey("edited")) {
                        contentValues.put("edited", (Integer) 4);
                    }
                    try {
                        iUpdateWithOnConflict = writableDatabase.updateWithOnConflict("carriers", contentValues, "_id=? and owned_by!=0", new String[]{lastPathSegment}, 2);
                    } catch (SQLException e) {
                        log("update: exception " + e);
                        Cursor cursorSelectConflictingRow = DatabaseHelper.selectConflictingRow(writableDatabase, "carriers", contentValues);
                        if (cursorSelectConflictingRow != null) {
                            DatabaseHelper.mergeFieldsAndUpdateDb(writableDatabase, "carriers", cursorSelectConflictingRow, contentValues, new ContentValues(), false, getContext());
                            cursorSelectConflictingRow.close();
                            writableDatabase.delete("carriers", "_id=? and owned_by!=0", new String[]{lastPathSegment});
                        }
                        iUpdateWithOnConflict = 0;
                    }
                    break;
                    break;
                default:
                    switch (iMatch) {
                        case 5:
                        case 6:
                            if (contentValues != null && contentValues.containsKey("apn_id")) {
                                setPreferredApnId(contentValues.getAsLong("apn_id"), defaultSubscriptionId, true);
                                if (iMatch != 5 || iMatch == 11) {
                                    iUpdateWithOnConflict = 1;
                                }
                            }
                            iUpdateWithOnConflict = 0;
                            break;
                        case 7:
                            iUpdateWithOnConflict = writableDatabase.update("siminfo", contentValues, str, strArr);
                            c = 7;
                            break;
                        case 8:
                            String lastPathSegment2 = uri.getLastPathSegment();
                            try {
                                log("update URL_TELEPHONY_USING_SUBID, subIdString=" + lastPathSegment2 + ", subId=" + Integer.parseInt(lastPathSegment2));
                                if (!contentValues.containsKey("edited")) {
                                }
                                iUpdateWithOnConflict = writableDatabase.updateWithOnConflict("carriers", contentValues, str + " and owned_by!=0", strArr, 5);
                            } catch (NumberFormatException e2) {
                                loge("NumberFormatException" + e2);
                                throw new IllegalArgumentException("Invalid subId " + uri);
                            }
                            break;
                        case 9:
                            String lastPathSegment3 = uri.getLastPathSegment();
                            try {
                                log("update URL_CURRENT_USING_SUBID, subIdString=" + lastPathSegment3 + ", subId=" + Integer.parseInt(lastPathSegment3));
                                if (!contentValues.containsKey("edited")) {
                                }
                                iUpdateWithOnConflict = writableDatabase.updateWithOnConflict("carriers", contentValues, str + " and owned_by!=0", strArr, 5);
                            } catch (NumberFormatException e3) {
                                loge("NumberFormatException" + e3);
                                throw new IllegalArgumentException("Invalid subId " + uri);
                            }
                            break;
                        default:
                            switch (iMatch) {
                                case 11:
                                case 12:
                                    String lastPathSegment4 = uri.getLastPathSegment();
                                    try {
                                        defaultSubscriptionId = Integer.parseInt(lastPathSegment4);
                                        log("update URL_PREFERAPN_USING_SUBID or URL_PREFERAPN_NO_UPDATE_USING_SUBID, subIdString=" + lastPathSegment4 + ", subId=" + defaultSubscriptionId);
                                        if (contentValues != null) {
                                            setPreferredApnId(contentValues.getAsLong("apn_id"), defaultSubscriptionId, true);
                                            if (iMatch != 5) {
                                            }
                                            iUpdateWithOnConflict = 1;
                                        }
                                        iUpdateWithOnConflict = 0;
                                    } catch (NumberFormatException e4) {
                                        loge("NumberFormatException" + e4);
                                        throw new IllegalArgumentException("Invalid subId " + uri);
                                    }
                                    break;
                                default:
                                    switch (iMatch) {
                                        case 51:
                                            if (contentValues != null && contentValues.containsKey("apn_id")) {
                                                setPreferredApnId(contentValues.getAsLong("apn_id"), defaultSubscriptionId, true);
                                                iUpdateWithOnConflict = 1;
                                            }
                                            iUpdateWithOnConflict = 0;
                                            break;
                                        case 52:
                                            iUpdateWithOnConflict = writableDatabase.update("carriers_dm", contentValues, str, strArr);
                                            if (iUpdateWithOnConflict > 0) {
                                                getContext().getContentResolver().notifyChange(MtkTelephony.Carriers.CONTENT_URI_DM, null);
                                            }
                                            break;
                                        default:
                                            throw new UnsupportedOperationException("Cannot update that URL: " + uri);
                                    }
                                    break;
                            }
                            break;
                    }
                    break;
            }
        } else {
            ensureCallingFromSystemOrPhoneUid("URL_ENFORCE_MANAGED called from non SYSTEM_UID.");
            if (contentValues != null && contentValues.containsKey("enforced")) {
                setManagedApnEnforced(contentValues.getAsBoolean("enforced").booleanValue());
                iUpdateWithOnConflict = 1;
            }
            iUpdateWithOnConflict = 0;
        }
        if (iUpdateWithOnConflict > 0) {
            if (c != 7) {
                getContext().getContentResolver().notifyChange(MtkTelephony.Carriers.CONTENT_URI, null, true, -1);
            } else {
                getContext().getContentResolver().notifyChange(SubscriptionManager.CONTENT_URI, null, true, -1);
            }
        }
        return iUpdateWithOnConflict;
    }

    private void checkPermission() {
        if (getContext().checkCallingOrSelfPermission("android.permission.WRITE_APN_SETTINGS") == 0) {
            return;
        }
        String[] packagesForUid = getContext().getPackageManager().getPackagesForUid(Binder.getCallingUid());
        TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService("phone");
        for (String str : packagesForUid) {
            if (telephonyManager.checkCarrierPrivilegesForPackage(str) == 1) {
                return;
            }
        }
        throw new SecurityException("No permission to write APN settings");
    }

    private void checkReadSimInfoPermission() {
        try {
            checkPermission();
        } catch (SecurityException e) {
            if (getContext().checkCallingOrSelfPermission("android.permission.READ_PHONE_STATE") == 0) {
                return;
            }
            EventLog.writeEvent(1397638484, "124107808", Integer.valueOf(Binder.getCallingUid()));
            throw new SecurityException("No READ_PHONE_STATE permission");
        }
    }

    private void restoreDefaultAPN(int i) throws Throwable {
        String str;
        SQLiteDatabase writableDatabase = getWritableDatabase();
        if (TextUtils.isEmpty(null)) {
            str = "owned_by!=0";
        } else {
            str = null;
        }
        log("restoreDefaultAPN: where: " + str);
        try {
            writableDatabase.delete("carriers", str, null);
        } catch (SQLException e) {
            loge("got exception when deleting to restore: " + e);
        }
        SharedPreferences.Editor editorEdit = getContext().getSharedPreferences("preferred-apn", 0).edit();
        editorEdit.clear();
        editorEdit.apply();
        SharedPreferences.Editor editorEdit2 = getContext().getSharedPreferences("preferred-full-apn", 0).edit();
        editorEdit2.clear();
        editorEdit2.apply();
        if (apnSourceServiceExists(getContext())) {
            restoreApnsWithService();
        } else {
            initDatabaseWithDatabaseHelper(writableDatabase);
        }
    }

    @VisibleForTesting
    IccRecords getIccRecords(int i) {
        return UiccController.getInstance().getIccRecords(SubscriptionManager.getPhoneId(i), TelephonyManager.from(getContext()).createForSubscriptionId(i).getPhoneType() != 1 ? 2 : 1);
    }

    private synchronized void updateApnDb() {
        if (apnSourceServiceExists(getContext())) {
            loge("called updateApnDb when apn source service exists");
            return;
        }
        if (!needApnDbUpdate()) {
            log("Skipping apn db update since apn-conf has not changed.");
            return;
        }
        SQLiteDatabase writableDatabase = getWritableDatabase();
        deletePreferredApnId();
        try {
            writableDatabase.delete("carriers", "edited=0 and owned_by!=0", null);
        } catch (SQLException e) {
            loge("got exception when deleting to update: " + e);
        }
        initDatabaseWithDatabaseHelper(writableDatabase);
        getContext().getContentResolver().notifyChange(MtkTelephony.Carriers.CONTENT_URI, null, true, -1);
    }

    private static void syncBearerBitmaskAndNetworkTypeBitmask(ContentValues contentValues) {
        if (contentValues.containsKey("network_type_bitmask")) {
            int iConvertNetworkTypeBitmaskToBearerBitmask = ServiceState.convertNetworkTypeBitmaskToBearerBitmask(contentValues.getAsInteger("network_type_bitmask").intValue());
            if (contentValues.containsKey("bearer_bitmask") && iConvertNetworkTypeBitmaskToBearerBitmask != contentValues.getAsInteger("bearer_bitmask").intValue()) {
                loge("Network type bitmask and bearer bitmask are not compatible.");
            }
            contentValues.put("bearer_bitmask", Integer.valueOf(ServiceState.convertNetworkTypeBitmaskToBearerBitmask(contentValues.getAsInteger("network_type_bitmask").intValue())));
            return;
        }
        if (contentValues.containsKey("bearer_bitmask")) {
            contentValues.put("network_type_bitmask", Integer.valueOf(ServiceState.convertBearerBitmaskToNetworkTypeBitmask(contentValues.getAsInteger("bearer_bitmask").intValue())));
        }
    }

    private static void log(String str) {
        Log.d("TelephonyProvider", str);
    }

    private static void loge(String str) {
        Log.e("TelephonyProvider", str);
    }
}
