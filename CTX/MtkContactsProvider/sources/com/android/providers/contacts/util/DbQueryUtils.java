package com.android.providers.contacts.util;

import android.content.ContentValues;
import android.database.DatabaseUtils;
import android.text.TextUtils;
import java.util.Map;
import java.util.Set;

public class DbQueryUtils {
    public static String getEqualityClause(String str, String str2) {
        return getClauseWithOperator(str, "=", str2);
    }

    public static String getEqualityClause(String str, long j) {
        return getClauseWithOperator(str, "=", j);
    }

    public static String getInequalityClause(String str, long j) {
        return getClauseWithOperator(str, "!=", j);
    }

    private static String getClauseWithOperator(String str, String str2, String str3) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(str);
        sb.append(" ");
        sb.append(str2);
        sb.append(" ");
        DatabaseUtils.appendEscapedSQLString(sb, str3);
        sb.append(")");
        return sb.toString();
    }

    private static String getClauseWithOperator(String str, String str2, long j) {
        return "(" + str + " " + str2 + " " + j + ")";
    }

    public static String concatenateClauses(String... strArr) {
        StringBuilder sb = new StringBuilder();
        for (String str : strArr) {
            if (!TextUtils.isEmpty(str)) {
                if (sb.length() > 0) {
                    sb.append(" AND ");
                }
                sb.append("(");
                sb.append(str);
                sb.append(")");
            }
        }
        return sb.toString();
    }

    public static void checkForSupportedColumns(Map<String, String> map, ContentValues contentValues) {
        checkForSupportedColumns(map.keySet(), contentValues, "Is invalid.");
    }

    public static void checkForSupportedColumns(Set<String> set, ContentValues contentValues, String str) {
        for (String str2 : contentValues.keySet()) {
            if (!set.contains(str2)) {
                throw new IllegalArgumentException("Column '" + str2 + "'. " + str);
            }
        }
    }

    public static void escapeLikeValue(StringBuilder sb, String str, char c) {
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '%' || cCharAt == '_' || cCharAt == c) {
                sb.append(c);
            }
            sb.append(cCharAt);
        }
    }
}
