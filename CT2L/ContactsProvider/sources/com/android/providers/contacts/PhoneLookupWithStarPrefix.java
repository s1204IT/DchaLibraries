package com.android.providers.contacts;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

final class PhoneLookupWithStarPrefix {
    public static Cursor removeNonStarMatchesFromCursor(String number, Cursor cursor) {
        try {
            if (TextUtils.isEmpty(number)) {
                Cursor unreturnedCursor = null;
                if (0 == 0) {
                    return cursor;
                }
                unreturnedCursor.close();
                return cursor;
            }
            String queryPhoneNumberNormalized = normalizeNumberWithStar(number);
            if (!queryPhoneNumberNormalized.startsWith("*") && !matchingNumberStartsWithStar(cursor)) {
                cursor.moveToPosition(-1);
                Cursor unreturnedCursor2 = null;
                if (0 == 0) {
                    return cursor;
                }
                unreturnedCursor2.close();
                return cursor;
            }
            MatrixCursor matrixCursor = new MatrixCursor(cursor.getColumnNames());
            try {
                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    int numberIndex = cursor.getColumnIndex("number");
                    String matchingNumberNormalized = normalizeNumberWithStar(cursor.getString(numberIndex));
                    if ((!matchingNumberNormalized.startsWith("*") && !queryPhoneNumberNormalized.startsWith("*")) || matchingNumberNormalized.equals(queryPhoneNumberNormalized)) {
                        MatrixCursor.RowBuilder b = matrixCursor.newRow();
                        for (int column = 0; column < cursor.getColumnCount(); column++) {
                            b.add(cursor.getColumnName(column), cursorValue(cursor, column));
                        }
                    }
                }
                Cursor unreturnedMatrixCursor = null;
                if (0 != 0) {
                    unreturnedMatrixCursor.close();
                }
                return matrixCursor;
            } catch (Throwable th) {
                if (matrixCursor != null) {
                    matrixCursor.close();
                }
                throw th;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    static String normalizeNumberWithStar(String phoneNumber) {
        if (!TextUtils.isEmpty(phoneNumber)) {
            if (phoneNumber.startsWith("*")) {
                return "*" + PhoneNumberUtils.normalizeNumber(phoneNumber.substring(1).replace("+", ""));
            }
            return PhoneNumberUtils.normalizeNumber(phoneNumber);
        }
        return phoneNumber;
    }

    private static boolean matchingNumberStartsWithStar(Cursor cursor) {
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            int numberIndex = cursor.getColumnIndex("number");
            String phoneNumber = normalizeNumberWithStar(cursor.getString(numberIndex));
            if (phoneNumber.startsWith("*")) {
                return true;
            }
        }
        return false;
    }

    private static Object cursorValue(Cursor cursor, int column) {
        switch (cursor.getType(column)) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
            default:
                Log.d("PhoneLookupWSP", "Invalid value in cursor: " + cursor.getType(column));
                break;
        }
        return null;
    }
}
