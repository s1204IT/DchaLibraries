package com.android.providers.contacts.util;

import android.content.ContentValues;
import android.database.DatabaseUtils;
import android.text.TextUtils;
import java.util.HashMap;
import java.util.Set;

public class DbQueryUtils {
    public static String getEqualityClause(String field, String value) {
        return getClauseWithOperator(field, "=", value);
    }

    public static String getEqualityClause(String field, long value) {
        return getClauseWithOperator(field, "=", value);
    }

    public static String getInequalityClause(String field, long value) {
        return getClauseWithOperator(field, "!=", value);
    }

    private static String getClauseWithOperator(String field, String operator, String value) {
        StringBuilder clause = new StringBuilder();
        clause.append("(");
        clause.append(field);
        clause.append(" ").append(operator).append(" ");
        DatabaseUtils.appendEscapedSQLString(clause, value);
        clause.append(")");
        return clause.toString();
    }

    private static String getClauseWithOperator(String field, String operator, long value) {
        StringBuilder clause = new StringBuilder();
        clause.append("(");
        clause.append(field);
        clause.append(" ").append(operator).append(" ");
        clause.append(value);
        clause.append(")");
        return clause.toString();
    }

    public static String concatenateClauses(String... clauses) {
        StringBuilder builder = new StringBuilder();
        for (String clause : clauses) {
            if (!TextUtils.isEmpty(clause)) {
                if (builder.length() > 0) {
                    builder.append(" AND ");
                }
                builder.append("(");
                builder.append(clause);
                builder.append(")");
            }
        }
        return builder.toString();
    }

    public static void checkForSupportedColumns(HashMap<String, String> projectionMap, ContentValues values) {
        checkForSupportedColumns(projectionMap.keySet(), values, "Is invalid.");
    }

    public static void checkForSupportedColumns(Set<String> allowedColumns, ContentValues values, String msgSuffix) {
        for (String requestedColumn : values.keySet()) {
            if (!allowedColumns.contains(requestedColumn)) {
                throw new IllegalArgumentException("Column '" + requestedColumn + "'. " + msgSuffix);
            }
        }
    }

    public static void escapeLikeValue(StringBuilder sb, String value, char escapeChar) {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '%' || ch == '_') {
                sb.append(escapeChar);
            }
            sb.append(ch);
        }
    }
}
