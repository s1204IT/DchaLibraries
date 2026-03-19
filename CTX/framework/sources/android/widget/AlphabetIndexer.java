package android.widget;

import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.wifi.WifiEnterpriseConfig;
import android.util.SparseIntArray;
import java.text.Collator;

public class AlphabetIndexer extends DataSetObserver implements SectionIndexer {
    private SparseIntArray mAlphaMap;
    protected CharSequence mAlphabet;
    private String[] mAlphabetArray;
    private int mAlphabetLength;
    private Collator mCollator;
    protected int mColumnIndex;
    protected Cursor mDataCursor;

    public AlphabetIndexer(Cursor cursor, int i, CharSequence charSequence) {
        this.mDataCursor = cursor;
        this.mColumnIndex = i;
        this.mAlphabet = charSequence;
        this.mAlphabetLength = charSequence.length();
        this.mAlphabetArray = new String[this.mAlphabetLength];
        for (int i2 = 0; i2 < this.mAlphabetLength; i2++) {
            this.mAlphabetArray[i2] = Character.toString(this.mAlphabet.charAt(i2));
        }
        this.mAlphaMap = new SparseIntArray(this.mAlphabetLength);
        if (cursor != null) {
            cursor.registerDataSetObserver(this);
        }
        this.mCollator = Collator.getInstance();
        this.mCollator.setStrength(0);
    }

    @Override
    public Object[] getSections() {
        return this.mAlphabetArray;
    }

    public void setCursor(Cursor cursor) {
        if (this.mDataCursor != null) {
            this.mDataCursor.unregisterDataSetObserver(this);
        }
        this.mDataCursor = cursor;
        if (cursor != null) {
            this.mDataCursor.registerDataSetObserver(this);
        }
        this.mAlphaMap.clear();
    }

    protected int compare(String str, String str2) {
        String strSubstring;
        if (str.length() == 0) {
            strSubstring = WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER;
        } else {
            strSubstring = str.substring(0, 1);
        }
        return this.mCollator.compare(strSubstring, str2);
    }

    @Override
    public int getPositionForSection(int i) {
        int i2;
        int i3;
        SparseIntArray sparseIntArray = this.mAlphaMap;
        Cursor cursor = this.mDataCursor;
        int iAbs = 0;
        if (cursor == null || this.mAlphabet == null || i <= 0) {
            return 0;
        }
        if (i >= this.mAlphabetLength) {
            i = this.mAlphabetLength - 1;
        }
        int position = cursor.getPosition();
        int count = cursor.getCount();
        char cCharAt = this.mAlphabet.charAt(i);
        String string = Character.toString(cCharAt);
        int i4 = sparseIntArray.get(cCharAt, Integer.MIN_VALUE);
        if (Integer.MIN_VALUE != i4) {
            if (i4 < 0) {
                i2 = -i4;
            } else {
                return i4;
            }
        } else {
            i2 = count;
        }
        if (i > 0 && (i3 = sparseIntArray.get(this.mAlphabet.charAt(i - 1), Integer.MIN_VALUE)) != Integer.MIN_VALUE) {
            iAbs = Math.abs(i3);
        }
        int i5 = (i2 + iAbs) / 2;
        while (true) {
            if (i5 >= i2) {
                break;
            }
            cursor.moveToPosition(i5);
            String string2 = cursor.getString(this.mColumnIndex);
            if (string2 == null) {
                if (i5 == 0) {
                    break;
                }
                i5--;
            } else {
                int iCompare = compare(string2, string);
                if (iCompare != 0) {
                    if (iCompare < 0) {
                        int i6 = i5 + 1;
                        if (i6 < count) {
                            iAbs = i6;
                        } else {
                            i5 = count;
                            break;
                        }
                    } else {
                        i2 = i5;
                    }
                } else {
                    if (iAbs == i5) {
                        break;
                    }
                    i2 = i5;
                }
                i5 = (iAbs + i2) / 2;
            }
        }
        sparseIntArray.put(cCharAt, i5);
        cursor.moveToPosition(position);
        return i5;
    }

    @Override
    public int getSectionForPosition(int i) {
        int position = this.mDataCursor.getPosition();
        this.mDataCursor.moveToPosition(i);
        String string = this.mDataCursor.getString(this.mColumnIndex);
        this.mDataCursor.moveToPosition(position);
        for (int i2 = 0; i2 < this.mAlphabetLength; i2++) {
            if (compare(string, Character.toString(this.mAlphabet.charAt(i2))) == 0) {
                return i2;
            }
        }
        return 0;
    }

    @Override
    public void onChanged() {
        super.onChanged();
        this.mAlphaMap.clear();
    }

    @Override
    public void onInvalidated() {
        super.onInvalidated();
        this.mAlphaMap.clear();
    }
}
