package com.android.providers.contacts;

import com.android.providers.contacts.NameSplitter;
import com.android.providers.contacts.SearchIndexManager;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

public abstract class NameLookupBuilder {
    private static final int[] KOREAN_JAUM_CONVERT_MAP = {4352, 4353, 0, 4354, 0, 0, 4355, 4356, 4357, 0, 0, 0, 0, 0, 0, 0, 4358, 4359, 4360, 0, 4361, 4362, 4363, 4364, 4365, 4366, 4367, 4368, 4369, 4370};
    private final NameSplitter mSplitter;
    private String[][] mNicknameClusters = new String[4][];
    private StringBuilder mStringBuilder = new StringBuilder();
    private String[] mNames = new String[10];

    protected abstract String[] getCommonNicknameClusters(String str);

    protected abstract void insertNameLookup(long j, long j2, int i, String str);

    public NameLookupBuilder(NameSplitter nameSplitter) {
        this.mSplitter = nameSplitter;
    }

    public void insertNameLookup(long j, long j2, String str, int i) {
        int i2 = this.mSplitter.tokenize(this.mNames, str);
        if (i2 == 0) {
            return;
        }
        for (int i3 = 0; i3 < i2; i3++) {
            this.mNames[i3] = normalizeName(this.mNames[i3]);
        }
        int i4 = 4;
        boolean z = i2 > 4;
        if (z) {
            insertNameVariant(j, j2, i2, 0, true);
            Arrays.sort(this.mNames, 0, i2, new Comparator<String>() {
                @Override
                public int compare(String str2, String str3) {
                    return str3.length() - str2.length();
                }
            });
            String str2 = this.mNames[0];
            for (int i5 = 4; i5 < i2; i5++) {
                this.mNames[0] = this.mNames[i5];
                insertCollationKey(j, j2, 4);
            }
            this.mNames[0] = str2;
        } else {
            i4 = i2;
        }
        for (int i6 = 0; i6 < i4; i6++) {
            this.mNicknameClusters[i6] = getCommonNicknameClusters(this.mNames[i6]);
        }
        int i7 = i4;
        insertNameVariants(j, j2, 0, i7, !z, true);
        insertNicknamePermutations(j, j2, 0, i7);
    }

    public void appendToSearchIndex(SearchIndexManager.IndexBuilder indexBuilder, String str, int i) {
        int i2 = this.mSplitter.tokenize(this.mNames, str);
        if (i2 == 0) {
            return;
        }
        for (int i3 = 0; i3 < i2; i3++) {
            indexBuilder.appendName(this.mNames[i3]);
        }
        appendNameShorthandLookup(indexBuilder, str, i);
        appendNameLookupForLocaleBasedName(indexBuilder, str, i);
    }

    private void appendNameLookupForLocaleBasedName(SearchIndexManager.IndexBuilder indexBuilder, String str, int i) {
        if (i == 5) {
            NameSplitter.Name name = new NameSplitter.Name();
            this.mSplitter.split(name, str, i);
            if (name.givenNames != null) {
                indexBuilder.appendName(name.givenNames);
                appendKoreanNameConsonantsLookup(indexBuilder, name.givenNames);
            }
            appendKoreanNameConsonantsLookup(indexBuilder, str);
        }
    }

    private void appendKoreanNameConsonantsLookup(SearchIndexManager.IndexBuilder indexBuilder, String str) {
        int i;
        int length = str.length();
        int i2 = 0;
        this.mStringBuilder.setLength(0);
        int i3 = 0;
        while (true) {
            int i4 = i2 + 1;
            int iCodePointAt = str.codePointAt(i2);
            if (iCodePointAt != 32 && iCodePointAt != 44 && iCodePointAt != 46) {
                if (iCodePointAt < 4352 || ((iCodePointAt > 4370 && iCodePointAt < 12593) || ((iCodePointAt > 12622 && iCodePointAt < 44032) || iCodePointAt > 55203))) {
                    break;
                }
                if (iCodePointAt >= 44032) {
                    iCodePointAt = ((iCodePointAt - 44032) / 588) + 4352;
                } else if (iCodePointAt >= 12593 && (iCodePointAt - 12593 >= KOREAN_JAUM_CONVERT_MAP.length || (iCodePointAt = KOREAN_JAUM_CONVERT_MAP[i]) == 0)) {
                    break;
                }
                this.mStringBuilder.appendCodePoint(iCodePointAt);
                i3++;
                if (i4 < length) {
                }
            } else if (i4 < length) {
                break;
            } else {
                i2 = i4;
            }
        }
        if (i3 > 1) {
            indexBuilder.appendName(this.mStringBuilder.toString());
        }
    }

    protected String normalizeName(String str) {
        return NameNormalizer.normalize(str);
    }

    private void insertNameVariants(long j, long j2, int i, int i2, boolean z, boolean z2) {
        if (i == i2) {
            insertNameVariant(j, j2, i2, !z ? 1 : 0, z2);
            return;
        }
        String str = this.mNames[i];
        int i3 = i;
        while (i3 < i2) {
            this.mNames[i] = this.mNames[i3];
            this.mNames[i3] = str;
            insertNameVariants(j, j2, i + 1, i2, z && i3 == i, z2);
            this.mNames[i3] = this.mNames[i];
            this.mNames[i] = str;
            i3++;
        }
    }

    private void insertNameVariant(long j, long j2, int i, int i2, boolean z) {
        this.mStringBuilder.setLength(0);
        for (int i3 = 0; i3 < i; i3++) {
            if (i3 != 0) {
                this.mStringBuilder.append('.');
            }
            this.mStringBuilder.append(this.mNames[i3]);
        }
        insertNameLookup(j, j2, i2, this.mStringBuilder.toString());
        if (z) {
            insertCollationKey(j, j2, i);
        }
    }

    private void insertCollationKey(long j, long j2, int i) {
        this.mStringBuilder.setLength(0);
        for (int i2 = 0; i2 < i; i2++) {
            this.mStringBuilder.append(this.mNames[i2]);
        }
        insertNameLookup(j, j2, 2, this.mStringBuilder.toString());
    }

    private void insertNicknamePermutations(long j, long j2, int i, int i2) {
        for (int i3 = i; i3 < i2; i3++) {
            String[] strArr = this.mNicknameClusters[i3];
            if (strArr != null) {
                String str = this.mNames[i3];
                for (String str2 : strArr) {
                    this.mNames[i3] = str2;
                    insertNameVariants(j, j2, 0, i2, false, false);
                    insertNicknamePermutations(j, j2, i3 + 1, i2);
                }
                this.mNames[i3] = str;
            }
        }
    }

    public void appendNameShorthandLookup(SearchIndexManager.IndexBuilder indexBuilder, String str, int i) {
        Iterator<String> nameLookupKeys = ContactLocaleUtils.getInstance().getNameLookupKeys(str, i);
        if (nameLookupKeys != null) {
            while (nameLookupKeys.hasNext()) {
                indexBuilder.appendName(nameLookupKeys.next());
            }
        }
    }
}
