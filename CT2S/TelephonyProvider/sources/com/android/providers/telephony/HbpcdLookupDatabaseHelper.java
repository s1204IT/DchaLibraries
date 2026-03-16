package com.android.providers.telephony;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
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
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE mcc_idd(_id INTEGER PRIMARY KEY,MCC INTEGER,IDD TEXT);");
        db.execSQL("CREATE TABLE mcc_lookup_table(_id INTEGER PRIMARY KEY,MCC INTEGER,Country_Code TEXT,Country_Name TEXT,NDD TEXT,NANPS BOOLEAN,GMT_Offset_Low REAL,GMT_Offset_High REAL,GMT_DST_Low REAL,GMT_DST_High REAL);");
        db.execSQL("CREATE TABLE mcc_sid_conflict(_id INTEGER PRIMARY KEY,MCC INTEGER,SID_Conflict INTEGER);");
        db.execSQL("CREATE TABLE mcc_sid_range(_id INTEGER PRIMARY KEY,MCC INTEGER,SID_Range_Low INTEGER,SID_Range_High INTEGER);");
        db.execSQL("CREATE TABLE nanp_area_code(_id INTEGER PRIMARY KEY,AREA_CODE INTEGER UNIQUE);");
        db.execSQL("CREATE TABLE arbitrary_mcc_sid_match(_id INTEGER PRIMARY KEY,MCC INTEGER,SID INTEGER UNIQUE);");
        initDatabase(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    private void initDatabase(SQLiteDatabase db) {
        Resources r = this.mContext.getResources();
        XmlResourceParser parser = r.getXml(R.xml.hbpcd_lookup_tables);
        try {
            if (parser == null) {
                Log.e("HbpcdLockupDatabaseHelper", "error to load the HBPCD resource");
                return;
            }
            db.beginTransaction();
            XmlUtils.beginDocument(parser, "hbpcd_info");
            int eventType = parser.getEventType();
            String tagName = parser.getName();
            while (eventType != 1) {
                if (eventType == 2 && tagName.equalsIgnoreCase("table")) {
                    String tableName = parser.getAttributeValue(null, "name");
                    loadTable(db, parser, tableName);
                }
                parser.next();
                eventType = parser.getEventType();
                tagName = parser.getName();
            }
            db.setTransactionSuccessful();
        } catch (IOException e) {
            Log.e("HbpcdLockupDatabaseHelper", "Got IOException when load hbpcd info");
        } catch (XmlPullParserException e2) {
            Log.e("HbpcdLockupDatabaseHelper", "Got XmlPullParserException when load hbpcd info");
        } catch (SQLException e3) {
            Log.e("HbpcdLockupDatabaseHelper", "Got SQLException when load hbpcd info");
        } finally {
            db.endTransaction();
            parser.close();
        }
    }

    private void loadTable(SQLiteDatabase db, XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
        ContentValues row;
        int eventType = parser.getEventType();
        String tagName = parser.getName();
        while (true) {
            if (eventType != 3 || !tagName.equalsIgnoreCase("table")) {
                if (tableName.equalsIgnoreCase("mcc_idd")) {
                    row = getTableMccIddRow(parser);
                } else if (tableName.equalsIgnoreCase("mcc_lookup_table")) {
                    row = getTableMccLookupTableRow(parser);
                } else if (tableName.equalsIgnoreCase("mcc_sid_conflict")) {
                    row = getTableMccSidConflictRow(parser);
                } else if (tableName.equalsIgnoreCase("mcc_sid_range")) {
                    row = getTableMccSidRangeRow(parser);
                } else if (tableName.equalsIgnoreCase("nanp_area_code")) {
                    row = getTableNanpAreaCodeRow(parser);
                } else if (tableName.equalsIgnoreCase("arbitrary_mcc_sid_match")) {
                    row = getTableArbitraryMccSidMatch(parser);
                } else {
                    Log.e("HbpcdLockupDatabaseHelper", "unrecognized table name" + tableName);
                    return;
                }
                if (row != null) {
                    db.insert(tableName, null, row);
                }
                parser.next();
                eventType = parser.getEventType();
                tagName = parser.getName();
            } else {
                return;
            }
        }
    }

    private ContentValues getTableMccIddRow(XmlPullParser parser) throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        String tagName = parser.getName();
        ContentValues row = new ContentValues();
        while (true) {
            if (eventType != 3 || !tagName.equalsIgnoreCase("row")) {
                if (eventType == 2) {
                    if (tagName.equalsIgnoreCase("MCC")) {
                        row.put("MCC", Integer.valueOf(Integer.parseInt(parser.nextText())));
                    } else if (tagName.equalsIgnoreCase("IDD")) {
                        row.put("IDD", parser.nextText());
                    }
                }
                parser.next();
                eventType = parser.getEventType();
                tagName = parser.getName();
            } else {
                return row;
            }
        }
    }

    private ContentValues getTableMccLookupTableRow(XmlPullParser parser) throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        String tagName = parser.getName();
        ContentValues row = new ContentValues();
        while (true) {
            if (eventType != 3 || !tagName.equalsIgnoreCase("row")) {
                if (eventType == 2) {
                    if (tagName.equalsIgnoreCase("MCC")) {
                        row.put("MCC", Integer.valueOf(Integer.parseInt(parser.nextText())));
                    } else if (tagName.equalsIgnoreCase("Country_Code")) {
                        row.put("Country_Code", Integer.valueOf(Integer.parseInt(parser.nextText())));
                    } else if (tagName.equalsIgnoreCase("Country_Name")) {
                        row.put("Country_Name", parser.nextText());
                    } else if (tagName.equalsIgnoreCase("NDD")) {
                        row.put("NDD", parser.nextText());
                    } else if (tagName.equalsIgnoreCase("NANPS")) {
                        row.put("NANPS", Boolean.valueOf(Boolean.parseBoolean(parser.nextText())));
                    } else if (tagName.equalsIgnoreCase("GMT_Offset_Low")) {
                        row.put("GMT_Offset_Low", Float.valueOf(Float.parseFloat(parser.nextText())));
                    } else if (tagName.equalsIgnoreCase("GMT_Offset_High")) {
                        row.put("GMT_Offset_High", Float.valueOf(Float.parseFloat(parser.nextText())));
                    } else if (tagName.equalsIgnoreCase("GMT_DST_Low")) {
                        row.put("GMT_DST_Low", Float.valueOf(Float.parseFloat(parser.nextText())));
                    } else if (tagName.equalsIgnoreCase("GMT_DST_High")) {
                        row.put("GMT_DST_High", Float.valueOf(Float.parseFloat(parser.nextText())));
                    }
                }
                parser.next();
                eventType = parser.getEventType();
                tagName = parser.getName();
            } else {
                return row;
            }
        }
    }

    private ContentValues getTableMccSidConflictRow(XmlPullParser parser) throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        String tagName = parser.getName();
        ContentValues row = new ContentValues();
        while (true) {
            if (eventType != 3 || !tagName.equalsIgnoreCase("row")) {
                if (eventType == 2) {
                    if (tagName.equalsIgnoreCase("MCC")) {
                        row.put("MCC", Integer.valueOf(Integer.parseInt(parser.nextText())));
                    } else if (tagName.equalsIgnoreCase("SID_Conflict")) {
                        row.put("SID_Conflict", Integer.valueOf(Integer.parseInt(parser.nextText())));
                    }
                }
                parser.next();
                eventType = parser.getEventType();
                tagName = parser.getName();
            } else {
                return row;
            }
        }
    }

    private ContentValues getTableMccSidRangeRow(XmlPullParser parser) throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        String tagName = parser.getName();
        ContentValues row = new ContentValues();
        while (true) {
            if (eventType != 3 || !tagName.equalsIgnoreCase("row")) {
                if (eventType == 2) {
                    if (tagName.equalsIgnoreCase("MCC")) {
                        row.put("MCC", Integer.valueOf(Integer.parseInt(parser.nextText())));
                    } else if (tagName.equalsIgnoreCase("SID_Range_Low")) {
                        row.put("SID_Range_Low", Integer.valueOf(Integer.parseInt(parser.nextText())));
                    } else if (tagName.equalsIgnoreCase("SID_Range_High")) {
                        row.put("SID_Range_High", Integer.valueOf(Integer.parseInt(parser.nextText())));
                    }
                }
                parser.next();
                eventType = parser.getEventType();
                tagName = parser.getName();
            } else {
                return row;
            }
        }
    }

    private ContentValues getTableNanpAreaCodeRow(XmlPullParser parser) throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        String tagName = parser.getName();
        ContentValues row = new ContentValues();
        while (true) {
            if (eventType != 3 || !tagName.equalsIgnoreCase("row")) {
                if (eventType == 2 && tagName.equalsIgnoreCase("Area_Code")) {
                    row.put("Area_Code", Integer.valueOf(Integer.parseInt(parser.nextText())));
                }
                parser.next();
                eventType = parser.getEventType();
                tagName = parser.getName();
            } else {
                return row;
            }
        }
    }

    private ContentValues getTableArbitraryMccSidMatch(XmlPullParser parser) throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        String tagName = parser.getName();
        ContentValues row = new ContentValues();
        while (true) {
            if (eventType != 3 || !tagName.equalsIgnoreCase("row")) {
                if (eventType == 2) {
                    if (tagName.equalsIgnoreCase("MCC")) {
                        row.put("MCC", Integer.valueOf(Integer.parseInt(parser.nextText())));
                    } else if (tagName.equalsIgnoreCase("SID")) {
                        row.put("SID", Integer.valueOf(Integer.parseInt(parser.nextText())));
                    }
                }
                parser.next();
                eventType = parser.getEventType();
                tagName = parser.getName();
            } else {
                return row;
            }
        }
    }
}
