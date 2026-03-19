package android.database.sqlite;

import android.bluetooth.BluetoothHidDevice;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiScanner;
import android.os.Build;
import android.os.CancellationSignal;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.midi.MidiConstants;
import com.android.internal.util.ArrayUtils;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libcore.util.EmptyArray;

public class SQLiteQueryBuilder {
    private static final int STRICT_COLUMNS = 2;
    private static final int STRICT_GRAMMAR = 4;
    private static final int STRICT_PARENTHESES = 1;
    private static final String TAG = "SQLiteQueryBuilder";
    private static final Pattern sAggregationPattern = Pattern.compile("(?i)(AVG|COUNT|MAX|MIN|SUM|TOTAL|GROUP_CONCAT)\\((.+)\\)");
    private int mStrictFlags;
    private Map<String, String> mProjectionMap = null;
    private List<Pattern> mProjectionGreylist = null;
    private String mTables = "";
    private StringBuilder mWhereClause = null;
    private boolean mDistinct = false;
    private SQLiteDatabase.CursorFactory mFactory = null;

    public void setDistinct(boolean z) {
        this.mDistinct = z;
    }

    public String getTables() {
        return this.mTables;
    }

    public void setTables(String str) {
        this.mTables = str;
    }

    public void appendWhere(CharSequence charSequence) {
        if (this.mWhereClause == null) {
            this.mWhereClause = new StringBuilder(charSequence.length() + 16);
        }
        this.mWhereClause.append(charSequence);
    }

    public void appendWhereEscapeString(String str) {
        if (this.mWhereClause == null) {
            this.mWhereClause = new StringBuilder(str.length() + 16);
        }
        DatabaseUtils.appendEscapedSQLString(this.mWhereClause, str);
    }

    public void setProjectionMap(Map<String, String> map) {
        this.mProjectionMap = map;
    }

    public Map<String, String> getProjectionMap() {
        return this.mProjectionMap;
    }

    public void setProjectionGreylist(List<Pattern> list) {
        this.mProjectionGreylist = list;
    }

    public List<Pattern> getProjectionGreylist() {
        return this.mProjectionGreylist;
    }

    public void setCursorFactory(SQLiteDatabase.CursorFactory cursorFactory) {
        this.mFactory = cursorFactory;
    }

    public void setStrict(boolean z) {
        if (z) {
            this.mStrictFlags |= 1;
        } else {
            this.mStrictFlags &= -2;
        }
    }

    public boolean isStrict() {
        return (this.mStrictFlags & 1) != 0;
    }

    public void setStrictColumns(boolean z) {
        if (z) {
            this.mStrictFlags |= 2;
        } else {
            this.mStrictFlags &= -3;
        }
    }

    public boolean isStrictColumns() {
        return (this.mStrictFlags & 2) != 0;
    }

    public void setStrictGrammar(boolean z) {
        if (z) {
            this.mStrictFlags |= 4;
        } else {
            this.mStrictFlags &= -5;
        }
    }

    public boolean isStrictGrammar() {
        return (this.mStrictFlags & 4) != 0;
    }

    public static String buildQueryString(boolean z, String str, String[] strArr, String str2, String str3, String str4, String str5, String str6) {
        if (TextUtils.isEmpty(str3) && !TextUtils.isEmpty(str4)) {
            throw new IllegalArgumentException("HAVING clauses are only permitted when using a groupBy clause");
        }
        StringBuilder sb = new StringBuilder(120);
        sb.append("SELECT ");
        if (z) {
            sb.append("DISTINCT ");
        }
        if (strArr != null && strArr.length != 0) {
            appendColumns(sb, strArr);
        } else {
            sb.append("* ");
        }
        sb.append("FROM ");
        sb.append(str);
        appendClause(sb, " WHERE ", str2);
        appendClause(sb, " GROUP BY ", str3);
        appendClause(sb, " HAVING ", str4);
        appendClause(sb, " ORDER BY ", str5);
        appendClause(sb, " LIMIT ", str6);
        return sb.toString();
    }

    private static void appendClause(StringBuilder sb, String str, String str2) {
        if (!TextUtils.isEmpty(str2)) {
            sb.append(str);
            sb.append(str2);
        }
    }

    public static void appendColumns(StringBuilder sb, String[] strArr) {
        int length = strArr.length;
        for (int i = 0; i < length; i++) {
            String str = strArr[i];
            if (str != null) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(str);
            }
        }
        sb.append(' ');
    }

    public Cursor query(SQLiteDatabase sQLiteDatabase, String[] strArr, String str, String[] strArr2, String str2, String str3, String str4) {
        return query(sQLiteDatabase, strArr, str, strArr2, str2, str3, str4, null, null);
    }

    public Cursor query(SQLiteDatabase sQLiteDatabase, String[] strArr, String str, String[] strArr2, String str2, String str3, String str4, String str5) {
        return query(sQLiteDatabase, strArr, str, strArr2, str2, str3, str4, str5, null);
    }

    public Cursor query(SQLiteDatabase sQLiteDatabase, String[] strArr, String str, String[] strArr2, String str2, String str3, String str4, String str5, CancellationSignal cancellationSignal) {
        String[] strArr3;
        SQLiteDatabase sQLiteDatabase2;
        CancellationSignal cancellationSignal2;
        if (this.mTables == null) {
            return null;
        }
        String strBuildQuery = buildQuery(strArr, str, str2, str3, str4, str5);
        if (isStrictColumns()) {
            strArr3 = strArr;
            enforceStrictColumns(strArr3);
        } else {
            strArr3 = strArr;
        }
        if (isStrictGrammar()) {
            enforceStrictGrammar(str, str2, str3, str4, str5);
        }
        if (isStrict()) {
            sQLiteDatabase2 = sQLiteDatabase;
            cancellationSignal2 = cancellationSignal;
            sQLiteDatabase2.validateSql(strBuildQuery, cancellationSignal2);
            strBuildQuery = buildQuery(strArr3, wrap(str), str2, wrap(str3), str4, str5);
        } else {
            sQLiteDatabase2 = sQLiteDatabase;
            cancellationSignal2 = cancellationSignal;
        }
        if (Log.isLoggable(TAG, 3)) {
            if (Build.IS_DEBUGGABLE) {
                Log.d(TAG, strBuildQuery + " with args " + Arrays.toString(strArr2));
            } else {
                Log.d(TAG, strBuildQuery);
            }
        }
        return sQLiteDatabase2.rawQueryWithFactory(this.mFactory, strBuildQuery, strArr2, SQLiteDatabase.findEditTable(this.mTables), cancellationSignal2);
    }

    public int update(SQLiteDatabase sQLiteDatabase, ContentValues contentValues, String str, String[] strArr) {
        Objects.requireNonNull(this.mTables, "No tables defined");
        Objects.requireNonNull(sQLiteDatabase, "No database defined");
        Objects.requireNonNull(contentValues, "No values defined");
        String strBuildUpdate = buildUpdate(contentValues, str);
        if (isStrictColumns()) {
            enforceStrictColumns(contentValues);
        }
        if (isStrictGrammar()) {
            enforceStrictGrammar(str, null, null, null, null);
        }
        if (isStrict()) {
            sQLiteDatabase.validateSql(strBuildUpdate, null);
            strBuildUpdate = buildUpdate(contentValues, wrap(str));
        }
        if (strArr == null) {
            strArr = EmptyArray.STRING;
        }
        String[] strArr2 = (String[]) contentValues.keySet().toArray(EmptyArray.STRING);
        int length = strArr2.length;
        Object[] objArr = new Object[strArr.length + length];
        for (int i = 0; i < objArr.length; i++) {
            if (i < length) {
                objArr[i] = contentValues.get(strArr2[i]);
            } else {
                objArr[i] = strArr[i - length];
            }
        }
        if (Log.isLoggable(TAG, 3)) {
            if (Build.IS_DEBUGGABLE) {
                Log.d(TAG, strBuildUpdate + " with args " + Arrays.toString(objArr));
            } else {
                Log.d(TAG, strBuildUpdate);
            }
        }
        return sQLiteDatabase.executeSql(strBuildUpdate, objArr);
    }

    public int delete(SQLiteDatabase sQLiteDatabase, String str, String[] strArr) {
        Objects.requireNonNull(this.mTables, "No tables defined");
        Objects.requireNonNull(sQLiteDatabase, "No database defined");
        String strBuildDelete = buildDelete(str);
        if (isStrictGrammar()) {
            enforceStrictGrammar(str, null, null, null, null);
        }
        if (isStrict()) {
            sQLiteDatabase.validateSql(strBuildDelete, null);
            strBuildDelete = buildDelete(wrap(str));
        }
        if (Log.isLoggable(TAG, 3)) {
            if (Build.IS_DEBUGGABLE) {
                Log.d(TAG, strBuildDelete + " with args " + Arrays.toString(strArr));
            } else {
                Log.d(TAG, strBuildDelete);
            }
        }
        return sQLiteDatabase.executeSql(strBuildDelete, strArr);
    }

    private void enforceStrictColumns(String[] strArr) {
        Objects.requireNonNull(this.mProjectionMap, "No projection map defined");
        computeProjection(strArr);
    }

    private void enforceStrictColumns(ContentValues contentValues) {
        Objects.requireNonNull(this.mProjectionMap, "No projection map defined");
        for (String str : contentValues.keySet()) {
            if (!this.mProjectionMap.containsKey(str)) {
                throw new IllegalArgumentException("Invalid column " + str);
            }
        }
    }

    private void enforceStrictGrammar(String str, String str2, String str3, String str4, String str5) {
        SQLiteTokenizer.tokenize(str, 0, new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.enforceStrictGrammarWhereHaving((String) obj);
            }
        });
        SQLiteTokenizer.tokenize(str2, 0, new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.enforceStrictGrammarGroupBy((String) obj);
            }
        });
        SQLiteTokenizer.tokenize(str3, 0, new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.enforceStrictGrammarWhereHaving((String) obj);
            }
        });
        SQLiteTokenizer.tokenize(str4, 0, new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.enforceStrictGrammarOrderBy((String) obj);
            }
        });
        SQLiteTokenizer.tokenize(str5, 0, new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.enforceStrictGrammarLimit((String) obj);
            }
        });
    }

    private void enforceStrictGrammarWhereHaving(String str) {
        if (isTableOrColumn(str) || SQLiteTokenizer.isFunction(str) || SQLiteTokenizer.isType(str)) {
            return;
        }
        String upperCase = str.toUpperCase(Locale.US);
        byte b = -1;
        switch (upperCase.hashCode()) {
            case -2125979215:
                if (upperCase.equals("ISNULL")) {
                    b = MidiConstants.STATUS_CHANNEL_MASK;
                }
                break;
            case -1986874255:
                if (upperCase.equals("NOCASE")) {
                    b = 18;
                }
                break;
            case -1881469687:
                if (upperCase.equals("REGEXP")) {
                    b = 23;
                }
                break;
            case -1447470406:
                if (upperCase.equals("NOTNULL")) {
                    b = 20;
                }
                break;
            case 2098:
                if (upperCase.equals("AS")) {
                    b = 1;
                }
                break;
            case 2341:
                if (upperCase.equals("IN")) {
                    b = 13;
                }
                break;
            case 2346:
                if (upperCase.equals("IS")) {
                    b = BluetoothHidDevice.ERROR_RSP_UNKNOWN;
                }
                break;
            case 2531:
                if (upperCase.equals("OR")) {
                    b = 22;
                }
                break;
            case 64951:
                if (upperCase.equals("AND")) {
                    b = 0;
                }
                break;
            case 68795:
                if (upperCase.equals("END")) {
                    b = 9;
                }
                break;
            case 77491:
                if (upperCase.equals("NOT")) {
                    b = 19;
                }
                break;
            case 2061104:
                if (upperCase.equals("CASE")) {
                    b = 4;
                }
                break;
            case 2061119:
                if (upperCase.equals("CAST")) {
                    b = 5;
                }
                break;
            case 2131257:
                if (upperCase.equals("ELSE")) {
                    b = 8;
                }
                break;
            case 2190712:
                if (upperCase.equals("GLOB")) {
                    b = 12;
                }
                break;
            case 2336663:
                if (upperCase.equals("LIKE")) {
                    b = WifiScanner.PnoSettings.PnoNetwork.FLAG_SAME_NETWORK;
                }
                break;
            case 2407815:
                if (upperCase.equals(WifiEnterpriseConfig.EMPTY_VALUE)) {
                    b = 21;
                }
                break;
            case 2573853:
                if (upperCase.equals("THEN")) {
                    b = 25;
                }
                break;
            case 2663226:
                if (upperCase.equals("WHEN")) {
                    b = 26;
                }
                break;
            case 73130405:
                if (upperCase.equals("MATCH")) {
                    b = 17;
                }
                break;
            case 78312308:
                if (upperCase.equals("RTRIM")) {
                    b = 24;
                }
                break;
            case 501348328:
                if (upperCase.equals("BETWEEN")) {
                    b = 2;
                }
                break;
            case 1071324924:
                if (upperCase.equals("DISTINCT")) {
                    b = 7;
                }
                break;
            case 1667424262:
                if (upperCase.equals("COLLATE")) {
                    b = 6;
                }
                break;
            case 1959329793:
                if (upperCase.equals("BINARY")) {
                    b = 3;
                }
                break;
            case 2054124673:
                if (upperCase.equals("ESCAPE")) {
                    b = 10;
                }
                break;
            case 2058938460:
                if (upperCase.equals("EXISTS")) {
                    b = 11;
                }
                break;
        }
        switch (b) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
                return;
            default:
                throw new IllegalArgumentException("Invalid token " + str);
        }
    }

    private void enforceStrictGrammarGroupBy(String str) {
        if (isTableOrColumn(str)) {
            return;
        }
        throw new IllegalArgumentException("Invalid token " + str);
    }

    private void enforceStrictGrammarOrderBy(String str) {
        if (isTableOrColumn(str)) {
            return;
        }
        switch (str.toUpperCase(Locale.US)) {
            case "COLLATE":
            case "ASC":
            case "DESC":
            case "BINARY":
            case "RTRIM":
            case "NOCASE":
                return;
            default:
                throw new IllegalArgumentException("Invalid token " + str);
        }
    }

    private void enforceStrictGrammarLimit(String str) {
        String upperCase = str.toUpperCase(Locale.US);
        if (((upperCase.hashCode() == -1966450541 && upperCase.equals("OFFSET")) ? (byte) 0 : (byte) -1) == 0) {
            return;
        }
        throw new IllegalArgumentException("Invalid token " + str);
    }

    public String buildQuery(String[] strArr, String str, String str2, String str3, String str4, String str5) {
        return buildQueryString(this.mDistinct, this.mTables, computeProjection(strArr), computeWhere(str), str2, str3, str4, str5);
    }

    @Deprecated
    public String buildQuery(String[] strArr, String str, String[] strArr2, String str2, String str3, String str4, String str5) {
        return buildQuery(strArr, str, str2, str3, str4, str5);
    }

    public String buildUpdate(ContentValues contentValues, String str) {
        if (contentValues == null || contentValues.size() == 0) {
            throw new IllegalArgumentException("Empty values");
        }
        StringBuilder sb = new StringBuilder(120);
        sb.append("UPDATE ");
        sb.append(SQLiteDatabase.findEditTable(this.mTables));
        sb.append(" SET ");
        String[] strArr = (String[]) contentValues.keySet().toArray(EmptyArray.STRING);
        for (int i = 0; i < strArr.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(strArr[i]);
            sb.append("=?");
        }
        appendClause(sb, " WHERE ", computeWhere(str));
        return sb.toString();
    }

    public String buildDelete(String str) {
        StringBuilder sb = new StringBuilder(120);
        sb.append("DELETE FROM ");
        sb.append(SQLiteDatabase.findEditTable(this.mTables));
        appendClause(sb, " WHERE ", computeWhere(str));
        return sb.toString();
    }

    public String buildUnionSubQuery(String str, String[] strArr, Set<String> set, int i, String str2, String str3, String str4, String str5) {
        int length = strArr.length;
        String[] strArr2 = new String[length];
        for (int i2 = 0; i2 < length; i2++) {
            String str6 = strArr[i2];
            if (str6.equals(str)) {
                strArr2[i2] = "'" + str2 + "' AS " + str;
            } else if (i2 <= i || set.contains(str6)) {
                strArr2[i2] = str6;
            } else {
                strArr2[i2] = "NULL AS " + str6;
            }
        }
        return buildQuery(strArr2, str3, str4, str5, null, null);
    }

    @Deprecated
    public String buildUnionSubQuery(String str, String[] strArr, Set<String> set, int i, String str2, String str3, String[] strArr2, String str4, String str5) {
        return buildUnionSubQuery(str, strArr, set, i, str2, str3, str4, str5);
    }

    public String buildUnionQuery(String[] strArr, String str, String str2) {
        StringBuilder sb = new StringBuilder(128);
        int length = strArr.length;
        String str3 = this.mDistinct ? " UNION " : " UNION ALL ";
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                sb.append(str3);
            }
            sb.append(strArr[i]);
        }
        appendClause(sb, " ORDER BY ", str);
        appendClause(sb, " LIMIT ", str2);
        return sb.toString();
    }

    private static String maybeWithOperator(String str, String str2) {
        if (str != null) {
            return str + "(" + str2 + ")";
        }
        return str2;
    }

    public String[] computeProjection(String[] strArr) {
        int i = 0;
        if (!ArrayUtils.isEmpty(strArr)) {
            String[] strArr2 = new String[strArr.length];
            while (i < strArr.length) {
                strArr2[i] = computeSingleProjectionOrThrow(strArr[i]);
                i++;
            }
            return strArr2;
        }
        if (this.mProjectionMap != null) {
            Set<Map.Entry<String, String>> setEntrySet = this.mProjectionMap.entrySet();
            String[] strArr3 = new String[setEntrySet.size()];
            for (Map.Entry<String, String> entry : setEntrySet) {
                if (!entry.getKey().equals(BaseColumns._COUNT)) {
                    strArr3[i] = entry.getValue();
                    i++;
                }
            }
            return strArr3;
        }
        return null;
    }

    private String computeSingleProjectionOrThrow(String str) {
        String strComputeSingleProjection = computeSingleProjection(str);
        if (strComputeSingleProjection != null) {
            return strComputeSingleProjection;
        }
        throw new IllegalArgumentException("Invalid column " + str);
    }

    private String computeSingleProjection(String str) {
        String str2;
        String strGroup;
        if (this.mProjectionMap == null) {
            return str;
        }
        String str3 = this.mProjectionMap.get(str);
        if (str3 == null) {
            Matcher matcher = sAggregationPattern.matcher(str);
            if (matcher.matches()) {
                strGroup = matcher.group(1);
                String strGroup2 = matcher.group(2);
                str2 = strGroup2;
                str3 = this.mProjectionMap.get(strGroup2);
            } else {
                str2 = str;
                strGroup = null;
            }
        }
        if (str3 != null) {
            return maybeWithOperator(strGroup, str3);
        }
        if (this.mStrictFlags == 0 && (str2.contains(" AS ") || str2.contains(" as "))) {
            return maybeWithOperator(strGroup, str2);
        }
        if (this.mProjectionGreylist != null) {
            boolean z = false;
            Iterator<Pattern> it = this.mProjectionGreylist.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                if (it.next().matcher(str2).matches()) {
                    z = true;
                    break;
                }
            }
            if (z) {
                Log.w(TAG, "Allowing abusive custom column: " + str2);
                return maybeWithOperator(strGroup, str2);
            }
        }
        return null;
    }

    private boolean isTableOrColumn(String str) {
        return this.mTables.equals(str) || computeSingleProjection(str) != null;
    }

    public String computeWhere(String str) {
        boolean z = !TextUtils.isEmpty(this.mWhereClause);
        boolean z2 = !TextUtils.isEmpty(str);
        if (z || z2) {
            StringBuilder sb = new StringBuilder();
            if (z) {
                sb.append('(');
                sb.append((CharSequence) this.mWhereClause);
                sb.append(')');
            }
            if (z && z2) {
                sb.append(" AND ");
            }
            if (z2) {
                sb.append('(');
                sb.append(str);
                sb.append(')');
            }
            return sb.toString();
        }
        return null;
    }

    private String wrap(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        return "(" + str + ")";
    }
}
