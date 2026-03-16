package com.android.providers.telephony;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.SystemProperties;
import android.provider.Telephony;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class TelephonyProvider extends ContentProvider {
    private static final ContentValues s_currentNullMap;
    private static final ContentValues s_currentSetMap;
    private static final UriMatcher s_urlMatcher = new UriMatcher(-1);
    private DatabaseHelper mOpenHelper;

    static {
        s_urlMatcher.addURI("telephony", "carriers", 1);
        s_urlMatcher.addURI("telephony", "carriers/current", 2);
        s_urlMatcher.addURI("telephony", "carriers/#", 3);
        s_urlMatcher.addURI("telephony", "carriers/restore", 4);
        s_urlMatcher.addURI("telephony", "carriers/preferapn", 5);
        s_urlMatcher.addURI("telephony", "carriers/preferapn_no_update", 6);
        s_urlMatcher.addURI("telephony", "siminfo", 7);
        s_urlMatcher.addURI("telephony", "carriers/subId/*", 8);
        s_urlMatcher.addURI("telephony", "carriers/current/subId/*", 9);
        s_urlMatcher.addURI("telephony", "carriers/restore/subId/*", 10);
        s_urlMatcher.addURI("telephony", "carriers/preferapn/subId/*", 11);
        s_urlMatcher.addURI("telephony", "carriers/preferapn_no_update/subId/*", 12);
        s_currentNullMap = new ContentValues(1);
        s_currentNullMap.put("current", (Long) null);
        s_currentSetMap = new ContentValues(1);
        s_currentSetMap.put("current", "1");
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private Context mContext;

        public DatabaseHelper(Context context) {
            super(context, "telephony.db", (SQLiteDatabase.CursorFactory) null, getVersion(context));
            this.mContext = context;
        }

        private static int getVersion(Context context) {
            int version = 851968;
            Resources r = context.getResources();
            XmlResourceParser parser = r.getXml(android.R.bool.config_sendPackageName);
            try {
                XmlUtils.beginDocument(parser, "apns");
                int publicversion = Integer.parseInt(parser.getAttributeValue(null, "version"));
                version = 851968 | publicversion;
            } catch (Exception e) {
                TelephonyProvider.loge("Can't get version of APN database" + e + " return version=" + Integer.toHexString(851968));
            } finally {
                parser.close();
            }
            return version;
        }

        @Override
        public void onCreate(SQLiteDatabase db) throws Throwable {
            TelephonyProvider.log("dbh.onCreate:+ db=" + db);
            createSimInfoTable(db);
            createCarriersTable(db);
            initDatabase(db);
            TelephonyProvider.log("dbh.onCreate:- db=" + db);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            try {
                db.query("siminfo", null, null, null, null, null, null);
                TelephonyProvider.log("dbh.onOpen: ok, queried table=siminfo");
            } catch (SQLiteException e) {
                TelephonyProvider.loge("Exception siminfoe=" + e);
                if (e.getMessage().startsWith("no such table")) {
                    createSimInfoTable(db);
                }
            }
            try {
                db.query("carriers", null, null, null, null, null, null);
                TelephonyProvider.log("dbh.onOpen: ok, queried table=carriers");
            } catch (SQLiteException e2) {
                TelephonyProvider.loge("Exception carriers e=" + e2);
                if (e2.getMessage().startsWith("no such table")) {
                    createCarriersTable(db);
                }
            }
        }

        private void createSimInfoTable(SQLiteDatabase db) {
            TelephonyProvider.log("dbh.createSimInfoTable:+");
            db.execSQL("CREATE TABLE siminfo(_id INTEGER PRIMARY KEY AUTOINCREMENT,icc_id TEXT NOT NULL,sim_id INTEGER DEFAULT -1,display_name TEXT,carrier_name TEXT,name_source INTEGER DEFAULT 0,color INTEGER DEFAULT 0,number TEXT,display_number_format INTEGER NOT NULL DEFAULT 1,data_roaming INTEGER DEFAULT 0,mcc INTEGER DEFAULT 0,mnc INTEGER DEFAULT 0);");
            TelephonyProvider.log("dbh.createSimInfoTable:-");
        }

        private void createCarriersTable(SQLiteDatabase db) {
            TelephonyProvider.log("dbh.createCarriersTable:+");
            db.execSQL("CREATE TABLE carriers(_id INTEGER PRIMARY KEY,name TEXT,numeric TEXT,mcc TEXT,mnc TEXT,apn TEXT,user TEXT,server TEXT,password TEXT,proxy TEXT,port TEXT,mmsproxy TEXT,mmsport TEXT,mmsc TEXT,authtype INTEGER,type TEXT,current INTEGER,protocol TEXT,roaming_protocol TEXT,carrier_enabled BOOLEAN,bearer INTEGER,mvno_type TEXT,mvno_match_data TEXT,sub_id INTEGER DEFAULT -1,profile_id INTEGER default 0,modem_cognitive BOOLEAN default 0,max_conns INTEGER default 0,wait_time INTEGER default 0,max_conns_time INTEGER default 0,mtu INTEGER);");
            TelephonyProvider.log("dbh.createCarriersTable:-");
        }

        private void initDatabase(SQLiteDatabase db) throws Throwable {
            FileReader confreader;
            XmlPullParser confparser;
            int confversion;
            Resources r = this.mContext.getResources();
            XmlResourceParser parser = r.getXml(android.R.bool.config_sendPackageName);
            int publicversion = -1;
            try {
                XmlUtils.beginDocument(parser, "apns");
                publicversion = Integer.parseInt(parser.getAttributeValue(null, "version"));
                loadApns(db, parser);
            } catch (Exception e) {
                TelephonyProvider.loge("Got exception while loading APN database." + e);
            } finally {
                parser.close();
            }
            File confFile = new File(Environment.getRootDirectory(), "etc/apns-conf.xml");
            File oemConfFile = new File(Environment.getOemDirectory(), "telephony/apns-conf.xml");
            if (!oemConfFile.exists()) {
                TelephonyProvider.log("No APNs in OEM image = " + oemConfFile.getPath() + " Load APNs from system image");
            } else {
                long oemApnTime = oemConfFile.lastModified();
                long sysApnTime = confFile.lastModified();
                TelephonyProvider.log("APNs Timestamp: oemTime = " + oemApnTime + " sysTime = " + sysApnTime);
                if (oemApnTime > sysApnTime) {
                    TelephonyProvider.log("APNs Timestamp: OEM image is greater than System image");
                    confFile = oemConfFile;
                }
            }
            int testOptions = SystemProperties.getInt("persist.radio.vzwtestoptions", 0);
            int testOptions2 = SystemProperties.getInt("persist.radio.vzwtestoptions2", 0);
            if ((testOptions & 2) == 2 || (testOptions2 & 2) == 2) {
                Log.d("TelephonyProvider", "get confFile form etc/apns-conf-vzw.xml");
                confFile = new File(Environment.getRootDirectory(), "etc/apns-conf-vzw.xml");
            }
            FileReader confreader2 = null;
            TelephonyProvider.log("confFile = " + confFile);
            try {
                try {
                    confreader = new FileReader(confFile);
                } catch (Throwable th) {
                    th = th;
                }
            } catch (FileNotFoundException e2) {
            } catch (Exception e3) {
                e = e3;
            }
            try {
                confparser = Xml.newPullParser();
                confparser.setInput(confreader);
                XmlUtils.beginDocument(confparser, "apns");
                confversion = Integer.parseInt(confparser.getAttributeValue(null, "version"));
            } catch (FileNotFoundException e4) {
                confreader2 = confreader;
                if (confreader2 != null) {
                    try {
                        confreader2.close();
                        return;
                    } catch (IOException e5) {
                        return;
                    }
                }
                return;
            } catch (Exception e6) {
                e = e6;
                confreader2 = confreader;
                TelephonyProvider.loge("Exception while parsing '" + confFile.getAbsolutePath() + "'" + e);
                if (confreader2 != null) {
                    try {
                        confreader2.close();
                    } catch (IOException e7) {
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                confreader2 = confreader;
                if (confreader2 != null) {
                    try {
                        confreader2.close();
                    } catch (IOException e8) {
                    }
                }
                throw th;
            }
            if (publicversion != confversion) {
                throw new IllegalStateException("Internal APNS file version doesn't match " + confFile.getAbsolutePath());
            }
            loadApns(db, confparser);
            if (confreader != null) {
                try {
                    confreader.close();
                    confreader2 = confreader;
                } catch (IOException e9) {
                    confreader2 = confreader;
                }
            } else {
                confreader2 = confreader;
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            TelephonyProvider.log("dbh.onUpgrade:+ db=" + db + " oldV=" + oldVersion + " newV=" + newVersion);
            if (oldVersion < 327686) {
                db.execSQL("ALTER TABLE carriers ADD COLUMN authtype INTEGER DEFAULT -1;");
                oldVersion = 327686;
            }
            if (oldVersion < 393222) {
                db.execSQL("ALTER TABLE carriers ADD COLUMN protocol TEXT DEFAULT IP;");
                db.execSQL("ALTER TABLE carriers ADD COLUMN roaming_protocol TEXT DEFAULT IP;");
                oldVersion = 393222;
            }
            if (oldVersion < 458758) {
                db.execSQL("ALTER TABLE carriers ADD COLUMN carrier_enabled BOOLEAN DEFAULT 1;");
                db.execSQL("ALTER TABLE carriers ADD COLUMN bearer INTEGER DEFAULT 0;");
                oldVersion = 458758;
            }
            if (oldVersion < 524294) {
                db.execSQL("ALTER TABLE carriers ADD COLUMN mvno_type TEXT DEFAULT '';");
                db.execSQL("ALTER TABLE carriers ADD COLUMN mvno_match_data TEXT DEFAULT '';");
                oldVersion = 524294;
            }
            if (oldVersion < 589830) {
                db.execSQL("ALTER TABLE carriers ADD COLUMN sub_id INTEGER DEFAULT -1;");
                oldVersion = 589830;
            }
            if (oldVersion < 655366) {
                db.execSQL("ALTER TABLE carriers ADD COLUMN profile_id INTEGER DEFAULT 0;");
                db.execSQL("ALTER TABLE carriers ADD COLUMN modem_cognitive BOOLEAN DEFAULT 0;");
                db.execSQL("ALTER TABLE carriers ADD COLUMN max_conns INTEGER DEFAULT 0;");
                db.execSQL("ALTER TABLE carriers ADD COLUMN wait_time INTEGER DEFAULT 0;");
                db.execSQL("ALTER TABLE carriers ADD COLUMN max_conns_time INTEGER DEFAULT 0;");
                oldVersion = 655366;
            }
            if (oldVersion < 720902) {
                db.execSQL("ALTER TABLE carriers ADD COLUMN mtu INTEGER DEFAULT 0;");
                oldVersion = 720902;
            }
            if (oldVersion < 786438) {
                try {
                    db.execSQL("ALTER TABLE siminfo ADD COLUMN mcc INTEGER DEFAULT 0;");
                    db.execSQL("ALTER TABLE siminfo ADD COLUMN mnc INTEGER DEFAULT 0;");
                } catch (SQLiteException e) {
                    TelephonyProvider.log("onUpgrade skipping siminfo upgrade.  The table will get created in onOpen.");
                }
                oldVersion = 786438;
            }
            if (oldVersion < 851974) {
                try {
                    db.execSQL("ALTER TABLE siminfo ADD COLUMN carrier_name TEXT DEFAULT '';");
                } catch (SQLiteException e2) {
                    TelephonyProvider.log("onUpgrade skipping siminfo upgrade.  The table will get created in onOpen.");
                }
                oldVersion = 851974;
            }
            TelephonyProvider.log("dbh.onUpgrade:- db=" + db + " oldV=" + oldVersion + " newV=" + newVersion);
        }

        private ContentValues getRow(XmlPullParser parser) {
            String mvno_match_data;
            if (!"apn".equals(parser.getName())) {
                return null;
            }
            ContentValues map = new ContentValues();
            String mcc = parser.getAttributeValue(null, "mcc");
            String mnc = parser.getAttributeValue(null, "mnc");
            String numeric = mcc + mnc;
            map.put("numeric", numeric);
            map.put("mcc", mcc);
            map.put("mnc", mnc);
            map.put("name", parser.getAttributeValue(null, "carrier"));
            map.put("apn", parser.getAttributeValue(null, "apn"));
            map.put("user", parser.getAttributeValue(null, "user"));
            map.put("server", parser.getAttributeValue(null, "server"));
            map.put("password", parser.getAttributeValue(null, "password"));
            String proxy = parser.getAttributeValue(null, "proxy");
            if (proxy != null) {
                map.put("proxy", proxy);
            }
            String port = parser.getAttributeValue(null, "port");
            if (port != null) {
                map.put("port", port);
            }
            String mmsproxy = parser.getAttributeValue(null, "mmsproxy");
            if (mmsproxy != null) {
                map.put("mmsproxy", mmsproxy);
            }
            String mmsport = parser.getAttributeValue(null, "mmsport");
            if (mmsport != null) {
                map.put("mmsport", mmsport);
            }
            map.put("mmsc", parser.getAttributeValue(null, "mmsc"));
            String type = parser.getAttributeValue(null, "type");
            if (type != null) {
                map.put("type", type);
            }
            String auth = parser.getAttributeValue(null, "authtype");
            if (auth != null) {
                map.put("authtype", Integer.valueOf(Integer.parseInt(auth)));
            }
            String protocol = parser.getAttributeValue(null, "protocol");
            if (protocol != null) {
                map.put("protocol", protocol);
            }
            String roamingProtocol = parser.getAttributeValue(null, "roaming_protocol");
            if (roamingProtocol != null) {
                map.put("roaming_protocol", roamingProtocol);
            }
            String carrierEnabled = parser.getAttributeValue(null, "carrier_enabled");
            if (carrierEnabled != null) {
                map.put("carrier_enabled", Boolean.valueOf(Boolean.parseBoolean(carrierEnabled)));
            }
            String bearer = parser.getAttributeValue(null, "bearer");
            if (bearer != null) {
                map.put("bearer", Integer.valueOf(Integer.parseInt(bearer)));
            }
            String mvno_type = parser.getAttributeValue(null, "mvno_type");
            if (mvno_type != null && (mvno_match_data = parser.getAttributeValue(null, "mvno_match_data")) != null) {
                map.put("mvno_type", mvno_type);
                map.put("mvno_match_data", mvno_match_data);
            }
            String profileId = parser.getAttributeValue(null, "profile_id");
            if (profileId != null) {
                map.put("profile_id", Integer.valueOf(Integer.parseInt(profileId)));
            }
            String modemCognitive = parser.getAttributeValue(null, "modem_cognitive");
            if (modemCognitive != null) {
                map.put("modem_cognitive", Boolean.valueOf(Boolean.parseBoolean(modemCognitive)));
            }
            String maxConns = parser.getAttributeValue(null, "max_conns");
            if (maxConns != null) {
                map.put("max_conns", Integer.valueOf(Integer.parseInt(maxConns)));
            }
            String waitTime = parser.getAttributeValue(null, "wait_time");
            if (waitTime != null) {
                map.put("wait_time", Integer.valueOf(Integer.parseInt(waitTime)));
            }
            String maxConnsTime = parser.getAttributeValue(null, "max_conns_time");
            if (maxConnsTime != null) {
                map.put("max_conns_time", Integer.valueOf(Integer.parseInt(maxConnsTime)));
            }
            String mtu = parser.getAttributeValue(null, "mtu");
            if (mtu != null) {
                map.put("mtu", Integer.valueOf(Integer.parseInt(mtu)));
                return map;
            }
            return map;
        }

        private void loadApns(SQLiteDatabase db, XmlPullParser parser) {
            try {
                if (parser != null) {
                    db.beginTransaction();
                    XmlUtils.nextElement(parser);
                    while (parser.getEventType() != 1) {
                        ContentValues row = getRow(parser);
                        if (row == null) {
                            throw new XmlPullParserException("Expected 'apn' tag", parser, null);
                        }
                        insertAddingDefaults(db, "carriers", row);
                        XmlUtils.nextElement(parser);
                    }
                    db.setTransactionSuccessful();
                }
            } catch (SQLException e) {
                TelephonyProvider.loge("Got SQLException while loading apns." + e);
            } catch (IOException e2) {
                TelephonyProvider.loge("Got IOException while loading apns." + e2);
            } catch (XmlPullParserException e3) {
                TelephonyProvider.loge("Got XmlPullParserException while loading apns." + e3);
            } finally {
                db.endTransaction();
            }
        }

        public static ContentValues setDefaultValue(ContentValues values) {
            boolean isLte = !SystemProperties.get("sys.lte.mode").isEmpty();
            String defaultProtocol = isLte ? "IPV4V6" : "IP";
            if (!values.containsKey("name")) {
                values.put("name", "");
            }
            if (!values.containsKey("apn")) {
                values.put("apn", "");
            }
            if (!values.containsKey("port")) {
                values.put("port", "");
            }
            if (!values.containsKey("proxy")) {
                values.put("proxy", "");
            }
            if (!values.containsKey("user")) {
                values.put("user", "");
            }
            if (!values.containsKey("server")) {
                values.put("server", "");
            }
            if (!values.containsKey("password")) {
                values.put("password", "");
            }
            if (!values.containsKey("mmsport")) {
                values.put("mmsport", "");
            }
            if (!values.containsKey("mmsproxy")) {
                values.put("mmsproxy", "");
            }
            if (!values.containsKey("authtype")) {
                values.put("authtype", (Integer) (-1));
            }
            if (!values.containsKey("protocol")) {
                values.put("protocol", defaultProtocol);
            }
            if (!values.containsKey("roaming_protocol")) {
                values.put("roaming_protocol", defaultProtocol);
            }
            if (!values.containsKey("carrier_enabled")) {
                values.put("carrier_enabled", (Boolean) true);
            }
            if (!values.containsKey("bearer")) {
                values.put("bearer", (Integer) 0);
            }
            if (!values.containsKey("mvno_type")) {
                values.put("mvno_type", "");
            }
            if (!values.containsKey("mvno_match_data")) {
                values.put("mvno_match_data", "");
            }
            int subId = SubscriptionManager.getDefaultSubId();
            if (!values.containsKey("sub_id")) {
                values.put("sub_id", Integer.valueOf(subId));
            }
            if (!values.containsKey("profile_id")) {
                values.put("profile_id", (Integer) 0);
            }
            if (!values.containsKey("modem_cognitive")) {
                values.put("modem_cognitive", (Boolean) false);
            }
            if (!values.containsKey("max_conns")) {
                values.put("max_conns", (Integer) 0);
            }
            if (!values.containsKey("wait_time")) {
                values.put("wait_time", (Integer) 0);
            }
            if (!values.containsKey("max_conns_time")) {
                values.put("max_conns_time", (Integer) 0);
            }
            return values;
        }

        private void insertAddingDefaults(SQLiteDatabase db, String table, ContentValues row) {
            db.insert("carriers", null, setDefaultValue(row));
        }
    }

    @Override
    public boolean onCreate() {
        this.mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    private void setPreferredApnId(Long id, int subId) {
        SharedPreferences sp = getContext().getSharedPreferences("preferred-apn" + subId, 0);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong("apn_id", id != null ? id.longValue() : -1L);
        editor.apply();
    }

    private long getPreferredApnId(int subId) {
        SharedPreferences sp = getContext().getSharedPreferences("preferred-apn" + subId, 0);
        return sp.getLong("apn_id", -1L);
    }

    @Override
    public Cursor query(Uri url, String[] projectionIn, String selection, String[] selectionArgs, String sort) {
        Cursor ret;
        TelephonyManager mTelephonyManager = (TelephonyManager) getContext().getSystemService("phone");
        int subId = SubscriptionManager.getDefaultSubId();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setStrict(true);
        qb.setTables("carriers");
        int match = s_urlMatcher.match(url);
        switch (match) {
            case 1:
                if (match != 7) {
                    if (projectionIn != null) {
                        int len$ = projectionIn.length;
                        int i$ = 0;
                        while (true) {
                            if (i$ < len$) {
                                String column = projectionIn[i$];
                                if ("type".equals(column) || "mmsc".equals(column) || "mmsproxy".equals(column) || "mmsport".equals(column) || "apn".equals(column)) {
                                    i$++;
                                } else {
                                    checkPermission();
                                }
                            }
                        }
                    } else {
                        checkPermission();
                    }
                }
                SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
                ret = null;
                try {
                    ret = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);
                } catch (SQLException e) {
                    loge("got exception when querying: " + e);
                }
                if (ret == null) {
                    ret.setNotificationUri(getContext().getContentResolver(), url);
                }
                break;
            case 2:
                qb.appendWhere("current IS NOT NULL");
                if (match != 7) {
                }
                SQLiteDatabase db2 = this.mOpenHelper.getReadableDatabase();
                ret = null;
                ret = qb.query(db2, projectionIn, selection, selectionArgs, null, null, sort);
                if (ret == null) {
                }
                break;
            case 3:
                qb.appendWhere("_id = " + url.getPathSegments().get(1));
                if (match != 7) {
                }
                SQLiteDatabase db22 = this.mOpenHelper.getReadableDatabase();
                ret = null;
                ret = qb.query(db22, projectionIn, selection, selectionArgs, null, null, sort);
                if (ret == null) {
                }
                break;
            case 5:
            case 6:
                qb.appendWhere("_id = " + getPreferredApnId(subId));
                if (match != 7) {
                }
                SQLiteDatabase db222 = this.mOpenHelper.getReadableDatabase();
                ret = null;
                ret = qb.query(db222, projectionIn, selection, selectionArgs, null, null, sort);
                if (ret == null) {
                }
                break;
            case 7:
                qb.setTables("siminfo");
                if (match != 7) {
                }
                SQLiteDatabase db2222 = this.mOpenHelper.getReadableDatabase();
                ret = null;
                ret = qb.query(db2222, projectionIn, selection, selectionArgs, null, null, sort);
                if (ret == null) {
                }
                break;
            case 8:
                String subIdString = url.getLastPathSegment();
                try {
                    int subId2 = Integer.parseInt(subIdString);
                    log("subIdString = " + subIdString + " subId = " + subId2);
                    qb.appendWhere("numeric = '" + mTelephonyManager.getSimOperator(subId2) + "'");
                    if (match != 7) {
                    }
                    SQLiteDatabase db22222 = this.mOpenHelper.getReadableDatabase();
                    ret = null;
                    ret = qb.query(db22222, projectionIn, selection, selectionArgs, null, null, sort);
                    if (ret == null) {
                    }
                } catch (NumberFormatException e2) {
                    loge("NumberFormatException" + e2);
                    return null;
                }
                break;
            case 9:
                String subIdString2 = url.getLastPathSegment();
                try {
                    int subId3 = Integer.parseInt(subIdString2);
                    log("subIdString = " + subIdString2 + " subId = " + subId3);
                    qb.appendWhere("current IS NOT NULL");
                    if (match != 7) {
                    }
                    SQLiteDatabase db222222 = this.mOpenHelper.getReadableDatabase();
                    ret = null;
                    ret = qb.query(db222222, projectionIn, selection, selectionArgs, null, null, sort);
                    if (ret == null) {
                    }
                } catch (NumberFormatException e3) {
                    loge("NumberFormatException" + e3);
                    return null;
                }
                break;
            case 11:
            case 12:
                String subIdString3 = url.getLastPathSegment();
                try {
                    subId = Integer.parseInt(subIdString3);
                    log("subIdString = " + subIdString3 + " subId = " + subId);
                    qb.appendWhere("_id = " + getPreferredApnId(subId));
                    if (match != 7) {
                    }
                    SQLiteDatabase db2222222 = this.mOpenHelper.getReadableDatabase();
                    ret = null;
                    ret = qb.query(db2222222, projectionIn, selection, selectionArgs, null, null, sort);
                    if (ret == null) {
                    }
                } catch (NumberFormatException e4) {
                    loge("NumberFormatException" + e4);
                    return null;
                }
                break;
        }
        return null;
    }

    @Override
    public String getType(Uri url) {
        switch (s_urlMatcher.match(url)) {
            case 1:
            case 8:
                return "vnd.android.cursor.dir/telephony-carrier";
            case 2:
            case 4:
            case 7:
            case 9:
            case 10:
            default:
                throw new IllegalArgumentException("Unknown URL " + url);
            case 3:
                return "vnd.android.cursor.item/telephony-carrier";
            case 5:
            case 6:
            case 11:
            case 12:
                return "vnd.android.cursor.item/telephony-carrier";
        }
    }

    @Override
    public Uri insert(Uri url, ContentValues initialValues) {
        int updated;
        ContentValues values;
        long rowID;
        Uri result = null;
        int subId = SubscriptionManager.getDefaultSubId();
        checkPermission();
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        int match = s_urlMatcher.match(url);
        boolean notify = false;
        switch (match) {
            case 1:
                if (initialValues == null) {
                    values = new ContentValues(initialValues);
                } else {
                    values = new ContentValues();
                }
                rowID = db.insert("carriers", null, DatabaseHelper.setDefaultValue(values));
                if (rowID > 0) {
                    result = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, rowID);
                    notify = true;
                }
                if (notify) {
                    getContext().getContentResolver().notifyChange(Telephony.Carriers.CONTENT_URI, null, true, -1);
                }
                return result;
            case 2:
                db.update("carriers", s_currentNullMap, "current IS NOT NULL", null);
                String numeric = initialValues.getAsString("numeric");
                updated = db.update("carriers", s_currentSetMap, "numeric = '" + numeric + "'", null);
                if (updated <= 0) {
                    loge("Failed setting numeric '" + numeric + "' to the current operator");
                }
                if (notify) {
                }
                return result;
            case 3:
            case 4:
            case 10:
            default:
                if (notify) {
                }
                return result;
            case 5:
            case 6:
                if (initialValues != null && initialValues.containsKey("apn_id")) {
                    setPreferredApnId(initialValues.getAsLong("apn_id"), subId);
                }
                if (notify) {
                }
                return result;
            case 7:
                long id = db.insert("siminfo", null, initialValues);
                result = ContentUris.withAppendedId(SubscriptionManager.CONTENT_URI, id);
                if (notify) {
                }
                return result;
            case 8:
                String subIdString = url.getLastPathSegment();
                try {
                    int subId2 = Integer.parseInt(subIdString);
                    log("subIdString = " + subIdString + " subId = " + subId2);
                    if (initialValues == null) {
                    }
                    rowID = db.insert("carriers", null, DatabaseHelper.setDefaultValue(values));
                    if (rowID > 0) {
                    }
                    if (notify) {
                    }
                    return result;
                } catch (NumberFormatException e) {
                    loge("NumberFormatException" + e);
                    return null;
                }
            case 9:
                String subIdString2 = url.getLastPathSegment();
                try {
                    int subId3 = Integer.parseInt(subIdString2);
                    log("subIdString = " + subIdString2 + " subId = " + subId3);
                    db.update("carriers", s_currentNullMap, "current IS NOT NULL", null);
                    String numeric2 = initialValues.getAsString("numeric");
                    updated = db.update("carriers", s_currentSetMap, "numeric = '" + numeric2 + "'", null);
                    if (updated <= 0) {
                    }
                    if (notify) {
                    }
                    return result;
                } catch (NumberFormatException e2) {
                    loge("NumberFormatException" + e2);
                    return null;
                }
            case 11:
            case 12:
                String subIdString3 = url.getLastPathSegment();
                try {
                    subId = Integer.parseInt(subIdString3);
                    log("subIdString = " + subIdString3 + " subId = " + subId);
                    if (initialValues != null) {
                        setPreferredApnId(initialValues.getAsLong("apn_id"), subId);
                    }
                    if (notify) {
                    }
                    return result;
                } catch (NumberFormatException e3) {
                    loge("NumberFormatException" + e3);
                    return null;
                }
        }
    }

    @Override
    public int delete(Uri url, String where, String[] whereArgs) throws Throwable {
        int count = 0;
        int subId = SubscriptionManager.getDefaultSubId();
        checkPermission();
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        int match = s_urlMatcher.match(url);
        switch (match) {
            case 1:
                count = db.delete("carriers", where, whereArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(Telephony.Carriers.CONTENT_URI, null, true, -1);
                }
                return count;
            case 2:
                count = db.delete("carriers", where, whereArgs);
                if (count > 0) {
                }
                return count;
            case 3:
                count = db.delete("carriers", "_id=?", new String[]{url.getLastPathSegment()});
                if (count > 0) {
                }
                return count;
            case 4:
                count = 1;
                restoreDefaultAPN(subId);
                if (count > 0) {
                }
                return count;
            case 5:
            case 6:
                setPreferredApnId(-1L, subId);
                if (match != 5 || match == 11) {
                    count = 1;
                }
                if (count > 0) {
                }
                return count;
            case 7:
                count = db.delete("siminfo", where, whereArgs);
                if (count > 0) {
                }
                return count;
            case 8:
                String subIdString = url.getLastPathSegment();
                try {
                    log("subIdString = " + subIdString + " subId = " + Integer.parseInt(subIdString));
                    count = db.delete("carriers", where, whereArgs);
                    if (count > 0) {
                    }
                    return count;
                } catch (NumberFormatException e) {
                    loge("NumberFormatException" + e);
                    throw new IllegalArgumentException("Invalid subId " + url);
                }
            case 9:
                String subIdString2 = url.getLastPathSegment();
                try {
                    log("subIdString = " + subIdString2 + " subId = " + Integer.parseInt(subIdString2));
                    count = db.delete("carriers", where, whereArgs);
                    if (count > 0) {
                    }
                    return count;
                } catch (NumberFormatException e2) {
                    loge("NumberFormatException" + e2);
                    throw new IllegalArgumentException("Invalid subId " + url);
                }
            case 10:
                String subIdString3 = url.getLastPathSegment();
                try {
                    subId = Integer.parseInt(subIdString3);
                    log("subIdString = " + subIdString3 + " subId = " + subId);
                    count = 1;
                    restoreDefaultAPN(subId);
                    if (count > 0) {
                    }
                    return count;
                } catch (NumberFormatException e3) {
                    loge("NumberFormatException" + e3);
                    throw new IllegalArgumentException("Invalid subId " + url);
                }
            case 11:
            case 12:
                String subIdString4 = url.getLastPathSegment();
                try {
                    subId = Integer.parseInt(subIdString4);
                    log("subIdString = " + subIdString4 + " subId = " + subId);
                    setPreferredApnId(-1L, subId);
                    if (match != 5) {
                        count = 1;
                    }
                    if (count > 0) {
                    }
                    return count;
                } catch (NumberFormatException e4) {
                    loge("NumberFormatException" + e4);
                    throw new IllegalArgumentException("Invalid subId " + url);
                }
            default:
                throw new UnsupportedOperationException("Cannot delete that URL: " + url);
        }
    }

    @Override
    public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
        int count = 0;
        int uriType = 0;
        int subId = SubscriptionManager.getDefaultSubId();
        checkPermission();
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        int match = s_urlMatcher.match(url);
        switch (match) {
            case 1:
                count = db.update("carriers", values, where, whereArgs);
                if (count > 0) {
                    switch (uriType) {
                        case 7:
                            getContext().getContentResolver().notifyChange(SubscriptionManager.CONTENT_URI, null, true, -1);
                            break;
                        default:
                            getContext().getContentResolver().notifyChange(Telephony.Carriers.CONTENT_URI, null, true, -1);
                            break;
                    }
                }
                return count;
            case 2:
                count = db.update("carriers", values, where, whereArgs);
                if (count > 0) {
                }
                return count;
            case 3:
                if (where != null || whereArgs != null) {
                    throw new UnsupportedOperationException("Cannot update URL " + url + " with a where clause");
                }
                count = db.update("carriers", values, "_id=?", new String[]{url.getLastPathSegment()});
                if (count > 0) {
                }
                return count;
            case 4:
            case 10:
            default:
                throw new UnsupportedOperationException("Cannot update that URL: " + url);
            case 5:
            case 6:
                if (values != null && values.containsKey("apn_id")) {
                    setPreferredApnId(values.getAsLong("apn_id"), subId);
                    if (match != 5 || match == 11) {
                        count = 1;
                    }
                }
                if (count > 0) {
                }
                return count;
            case 7:
                count = db.update("siminfo", values, where, whereArgs);
                uriType = 7;
                if (count > 0) {
                }
                return count;
            case 8:
                String subIdString = url.getLastPathSegment();
                try {
                    int subId2 = Integer.parseInt(subIdString);
                    log("subIdString = " + subIdString + " subId = " + subId2);
                    count = db.update("carriers", values, where, whereArgs);
                    if (count > 0) {
                    }
                    return count;
                } catch (NumberFormatException e) {
                    loge("NumberFormatException" + e);
                    throw new IllegalArgumentException("Invalid subId " + url);
                }
            case 9:
                String subIdString2 = url.getLastPathSegment();
                try {
                    int subId3 = Integer.parseInt(subIdString2);
                    log("subIdString = " + subIdString2 + " subId = " + subId3);
                    count = db.update("carriers", values, where, whereArgs);
                    if (count > 0) {
                    }
                    return count;
                } catch (NumberFormatException e2) {
                    loge("NumberFormatException" + e2);
                    throw new IllegalArgumentException("Invalid subId " + url);
                }
            case 11:
            case 12:
                String subIdString3 = url.getLastPathSegment();
                try {
                    subId = Integer.parseInt(subIdString3);
                    log("subIdString = " + subIdString3 + " subId = " + subId);
                    if (values != null) {
                        setPreferredApnId(values.getAsLong("apn_id"), subId);
                        if (match != 5) {
                            count = 1;
                        }
                    }
                    if (count > 0) {
                    }
                    return count;
                } catch (NumberFormatException e3) {
                    loge("NumberFormatException" + e3);
                    throw new IllegalArgumentException("Invalid subId " + url);
                }
        }
    }

    private void checkPermission() {
        int status = getContext().checkCallingOrSelfPermission("android.permission.WRITE_APN_SETTINGS");
        if (status != 0) {
            PackageManager packageManager = getContext().getPackageManager();
            String[] packages = packageManager.getPackagesForUid(Binder.getCallingUid());
            TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService("phone");
            for (String pkg : packages) {
                if (telephonyManager.checkCarrierPrivilegesForPackage(pkg) == 1) {
                    return;
                }
            }
            throw new SecurityException("No permission to write APN settings");
        }
    }

    private void restoreDefaultAPN(int subId) throws Throwable {
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        try {
            db.delete("carriers", null, null);
        } catch (SQLException e) {
            loge("got exception when deleting to restore: " + e);
        }
        setPreferredApnId(-1L, subId);
        this.mOpenHelper.initDatabase(db);
    }

    private static void log(String s) {
        Log.d("TelephonyProvider", s);
    }

    private static void loge(String s) {
        Log.e("TelephonyProvider", s);
    }
}
