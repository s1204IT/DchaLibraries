package jp.co.benesse.dcha.databox.db;

import android.provider.BaseColumns;

public class KvsColumns implements BaseColumns {
    public static final String APP_ID = "appid";
    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String CREATE_TABLE = "CREATE TABLE " + ContractKvs.KVS.pathName + " (" + APP_ID + " TEXT NOT NULL, " + KEY + " TEXT NOT NULL, " + VALUE + " TEXT, PRIMARY KEY(" + APP_ID + ", " + KEY + "));";
}
