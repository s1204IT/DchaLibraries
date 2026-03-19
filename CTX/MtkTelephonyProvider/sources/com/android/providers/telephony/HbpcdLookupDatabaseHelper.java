package com.android.providers.telephony;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HbpcdLookupDatabaseHelper extends SQLiteOpenHelper {
    private Context mContext;

    public HbpcdLookupDatabaseHelper(Context context) {
        super(context, "HbpcdLookup.db", (SQLiteDatabase.CursorFactory) null, 1);
        this.mContext = context;
        setIdleConnectionTimeout(30000L);
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE mcc_idd(_id INTEGER PRIMARY KEY,MCC INTEGER,IDD TEXT);");
        sQLiteDatabase.execSQL("CREATE TABLE mcc_lookup_table(_id INTEGER PRIMARY KEY,MCC INTEGER,Country_Code TEXT,Country_Name TEXT,NDD TEXT,NANPS BOOLEAN,GMT_Offset_Low REAL,GMT_Offset_High REAL,GMT_DST_Low REAL,GMT_DST_High REAL);");
        sQLiteDatabase.execSQL("CREATE TABLE mcc_sid_conflict(_id INTEGER PRIMARY KEY,MCC INTEGER,SID_Conflict INTEGER);");
        sQLiteDatabase.execSQL("CREATE TABLE mcc_sid_range(_id INTEGER PRIMARY KEY,MCC INTEGER,SID_Range_Low INTEGER,SID_Range_High INTEGER);");
        sQLiteDatabase.execSQL("CREATE TABLE nanp_area_code(_id INTEGER PRIMARY KEY,AREA_CODE INTEGER UNIQUE);");
        sQLiteDatabase.execSQL("CREATE TABLE arbitrary_mcc_sid_match(_id INTEGER PRIMARY KEY,MCC INTEGER,SID INTEGER UNIQUE);");
        initDatabase(sQLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
    }

    private void initDatabase(SQLiteDatabase sQLiteDatabase) {
        XmlResourceParser xml = this.mContext.getResources().getXml(R.xml.hbpcd_lookup_tables);
        try {
            if (xml == null) {
                Log.e("HbpcdLockupDatabaseHelper", "error to load the HBPCD resource");
                return;
            }
            try {
                sQLiteDatabase.beginTransaction();
                XmlUtils.beginDocument(xml, "hbpcd_info");
                int eventType = xml.getEventType();
                String name = xml.getName();
                while (eventType != 1) {
                    if (eventType == 2 && name.equalsIgnoreCase("table")) {
                        loadTable(sQLiteDatabase, xml, xml.getAttributeValue(null, "name"));
                    }
                    xml.next();
                    eventType = xml.getEventType();
                    name = xml.getName();
                }
                sQLiteDatabase.setTransactionSuccessful();
            } catch (SQLException e) {
                Log.e("HbpcdLockupDatabaseHelper", "Got SQLException when load hbpcd info");
            } catch (IOException e2) {
                Log.e("HbpcdLockupDatabaseHelper", "Got IOException when load hbpcd info");
            } catch (XmlPullParserException e3) {
                Log.e("HbpcdLockupDatabaseHelper", "Got XmlPullParserException when load hbpcd info");
            }
        } finally {
            sQLiteDatabase.endTransaction();
            xml.close();
        }
    }

    private void loadTable(SQLiteDatabase sQLiteDatabase, XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException {
        ContentValues tableArbitraryMccSidMatch;
        int eventType = xmlPullParser.getEventType();
        String name = xmlPullParser.getName();
        while (true) {
            if (eventType != 3 || !name.equalsIgnoreCase("table")) {
                if (str.equalsIgnoreCase("mcc_idd")) {
                    tableArbitraryMccSidMatch = getTableMccIddRow(xmlPullParser);
                } else if (str.equalsIgnoreCase("mcc_lookup_table")) {
                    tableArbitraryMccSidMatch = getTableMccLookupTableRow(xmlPullParser);
                } else if (str.equalsIgnoreCase("mcc_sid_conflict")) {
                    tableArbitraryMccSidMatch = getTableMccSidConflictRow(xmlPullParser);
                } else if (str.equalsIgnoreCase("mcc_sid_range")) {
                    tableArbitraryMccSidMatch = getTableMccSidRangeRow(xmlPullParser);
                } else if (str.equalsIgnoreCase("nanp_area_code")) {
                    tableArbitraryMccSidMatch = getTableNanpAreaCodeRow(xmlPullParser);
                } else if (str.equalsIgnoreCase("arbitrary_mcc_sid_match")) {
                    tableArbitraryMccSidMatch = getTableArbitraryMccSidMatch(xmlPullParser);
                } else {
                    Log.e("HbpcdLockupDatabaseHelper", "unrecognized table name" + str);
                    return;
                }
                if (tableArbitraryMccSidMatch != null) {
                    sQLiteDatabase.insert(str, null, tableArbitraryMccSidMatch);
                }
                xmlPullParser.next();
                eventType = xmlPullParser.getEventType();
                name = xmlPullParser.getName();
            } else {
                return;
            }
        }
    }

    private ContentValues getTableMccIddRow(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int eventType = xmlPullParser.getEventType();
        String name = xmlPullParser.getName();
        ContentValues contentValues = new ContentValues();
        while (true) {
            if (eventType != 3 || !name.equalsIgnoreCase("row")) {
                if (eventType == 2) {
                    if (name.equalsIgnoreCase("MCC")) {
                        contentValues.put("MCC", Integer.valueOf(Integer.parseInt(xmlPullParser.nextText())));
                    } else if (name.equalsIgnoreCase("IDD")) {
                        contentValues.put("IDD", xmlPullParser.nextText());
                    }
                }
                xmlPullParser.next();
                eventType = xmlPullParser.getEventType();
                name = xmlPullParser.getName();
            } else {
                return contentValues;
            }
        }
    }

    private ContentValues getTableMccLookupTableRow(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int eventType = xmlPullParser.getEventType();
        String name = xmlPullParser.getName();
        ContentValues contentValues = new ContentValues();
        while (true) {
            if (eventType != 3 || !name.equalsIgnoreCase("row")) {
                if (eventType == 2) {
                    if (name.equalsIgnoreCase("MCC")) {
                        contentValues.put("MCC", Integer.valueOf(Integer.parseInt(xmlPullParser.nextText())));
                    } else if (name.equalsIgnoreCase("Country_Code")) {
                        contentValues.put("Country_Code", Integer.valueOf(Integer.parseInt(xmlPullParser.nextText())));
                    } else if (name.equalsIgnoreCase("Country_Name")) {
                        contentValues.put("Country_Name", xmlPullParser.nextText());
                    } else if (name.equalsIgnoreCase("NDD")) {
                        contentValues.put("NDD", xmlPullParser.nextText());
                    } else if (name.equalsIgnoreCase("NANPS")) {
                        contentValues.put("NANPS", Boolean.valueOf(Boolean.parseBoolean(xmlPullParser.nextText())));
                    } else if (name.equalsIgnoreCase("GMT_Offset_Low")) {
                        contentValues.put("GMT_Offset_Low", Float.valueOf(Float.parseFloat(xmlPullParser.nextText())));
                    } else if (name.equalsIgnoreCase("GMT_Offset_High")) {
                        contentValues.put("GMT_Offset_High", Float.valueOf(Float.parseFloat(xmlPullParser.nextText())));
                    } else if (name.equalsIgnoreCase("GMT_DST_Low")) {
                        contentValues.put("GMT_DST_Low", Float.valueOf(Float.parseFloat(xmlPullParser.nextText())));
                    } else if (name.equalsIgnoreCase("GMT_DST_High")) {
                        contentValues.put("GMT_DST_High", Float.valueOf(Float.parseFloat(xmlPullParser.nextText())));
                    }
                }
                xmlPullParser.next();
                eventType = xmlPullParser.getEventType();
                name = xmlPullParser.getName();
            } else {
                return contentValues;
            }
        }
    }

    private ContentValues getTableMccSidConflictRow(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int eventType = xmlPullParser.getEventType();
        String name = xmlPullParser.getName();
        ContentValues contentValues = new ContentValues();
        while (true) {
            if (eventType != 3 || !name.equalsIgnoreCase("row")) {
                if (eventType == 2) {
                    if (name.equalsIgnoreCase("MCC")) {
                        contentValues.put("MCC", Integer.valueOf(Integer.parseInt(xmlPullParser.nextText())));
                    } else if (name.equalsIgnoreCase("SID_Conflict")) {
                        contentValues.put("SID_Conflict", Integer.valueOf(Integer.parseInt(xmlPullParser.nextText())));
                    }
                }
                xmlPullParser.next();
                eventType = xmlPullParser.getEventType();
                name = xmlPullParser.getName();
            } else {
                return contentValues;
            }
        }
    }

    private ContentValues getTableMccSidRangeRow(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int eventType = xmlPullParser.getEventType();
        String name = xmlPullParser.getName();
        ContentValues contentValues = new ContentValues();
        while (true) {
            if (eventType != 3 || !name.equalsIgnoreCase("row")) {
                if (eventType == 2) {
                    if (name.equalsIgnoreCase("MCC")) {
                        contentValues.put("MCC", Integer.valueOf(Integer.parseInt(xmlPullParser.nextText())));
                    } else if (name.equalsIgnoreCase("SID_Range_Low")) {
                        contentValues.put("SID_Range_Low", Integer.valueOf(Integer.parseInt(xmlPullParser.nextText())));
                    } else if (name.equalsIgnoreCase("SID_Range_High")) {
                        contentValues.put("SID_Range_High", Integer.valueOf(Integer.parseInt(xmlPullParser.nextText())));
                    }
                }
                xmlPullParser.next();
                eventType = xmlPullParser.getEventType();
                name = xmlPullParser.getName();
            } else {
                return contentValues;
            }
        }
    }

    private ContentValues getTableNanpAreaCodeRow(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int eventType = xmlPullParser.getEventType();
        String name = xmlPullParser.getName();
        ContentValues contentValues = new ContentValues();
        while (true) {
            if (eventType != 3 || !name.equalsIgnoreCase("row")) {
                if (eventType == 2 && name.equalsIgnoreCase("Area_Code")) {
                    contentValues.put("Area_Code", Integer.valueOf(Integer.parseInt(xmlPullParser.nextText())));
                }
                xmlPullParser.next();
                eventType = xmlPullParser.getEventType();
                name = xmlPullParser.getName();
            } else {
                return contentValues;
            }
        }
    }

    private ContentValues getTableArbitraryMccSidMatch(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int eventType = xmlPullParser.getEventType();
        String name = xmlPullParser.getName();
        ContentValues contentValues = new ContentValues();
        while (true) {
            if (eventType != 3 || !name.equalsIgnoreCase("row")) {
                if (eventType == 2) {
                    if (name.equalsIgnoreCase("MCC")) {
                        contentValues.put("MCC", Integer.valueOf(Integer.parseInt(xmlPullParser.nextText())));
                    } else if (name.equalsIgnoreCase("SID")) {
                        contentValues.put("SID", Integer.valueOf(Integer.parseInt(xmlPullParser.nextText())));
                    }
                }
                xmlPullParser.next();
                eventType = xmlPullParser.getEventType();
                name = xmlPullParser.getName();
            } else {
                return contentValues;
            }
        }
    }
}
