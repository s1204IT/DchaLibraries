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

    public NameLookupBuilder(NameSplitter splitter) {
        this.mSplitter = splitter;
    }

    public void insertNameLookup(long rawContactId, long dataId, String name, int fullNameStyle) {
        int tokenCount = this.mSplitter.tokenize(this.mNames, name);
        if (tokenCount != 0) {
            for (int i = 0; i < tokenCount; i++) {
                this.mNames[i] = normalizeName(this.mNames[i]);
            }
            boolean tooManyTokens = tokenCount > 4;
            if (tooManyTokens) {
                insertNameVariant(rawContactId, dataId, tokenCount, 0, true);
                Arrays.sort(this.mNames, 0, tokenCount, new Comparator<String>() {
                    @Override
                    public int compare(String s1, String s2) {
                        return s2.length() - s1.length();
                    }
                });
                String firstToken = this.mNames[0];
                for (int i2 = 4; i2 < tokenCount; i2++) {
                    this.mNames[0] = this.mNames[i2];
                    insertCollationKey(rawContactId, dataId, 4);
                }
                this.mNames[0] = firstToken;
                tokenCount = 4;
            }
            for (int i3 = 0; i3 < tokenCount; i3++) {
                this.mNicknameClusters[i3] = getCommonNicknameClusters(this.mNames[i3]);
            }
            insertNameVariants(rawContactId, dataId, 0, tokenCount, !tooManyTokens, true);
            insertNicknamePermutations(rawContactId, dataId, 0, tokenCount);
        }
    }

    public void appendToSearchIndex(SearchIndexManager.IndexBuilder builder, String name, int fullNameStyle) {
        int tokenCount = this.mSplitter.tokenize(this.mNames, name);
        if (tokenCount != 0) {
            for (int i = 0; i < tokenCount; i++) {
                builder.appendName(this.mNames[i]);
            }
            appendNameShorthandLookup(builder, name, fullNameStyle);
            appendNameLookupForLocaleBasedName(builder, name, fullNameStyle);
        }
    }

    private void appendNameLookupForLocaleBasedName(SearchIndexManager.IndexBuilder builder, String fullName, int fullNameStyle) {
        if (fullNameStyle == 5) {
            NameSplitter.Name name = new NameSplitter.Name();
            this.mSplitter.split(name, fullName, fullNameStyle);
            if (name.givenNames != null) {
                builder.appendName(name.givenNames);
                appendKoreanNameConsonantsLookup(builder, name.givenNames);
            }
            appendKoreanNameConsonantsLookup(builder, fullName);
        }
    }

    private void appendKoreanNameConsonantsLookup(SearchIndexManager.IndexBuilder builder, String name) {
        int position = 0;
        int consonantLength = 0;
        int stringLength = name.length();
        this.mStringBuilder.setLength(0);
        while (true) {
            int position2 = position + 1;
            int character = name.codePointAt(position);
            if (character != 32 && character != 44 && character != 46) {
                if (character < 4352 || ((character > 4370 && character < 12593) || ((character > 12622 && character < 44032) || character > 55203))) {
                    break;
                }
                if (character >= 44032) {
                    character = ((character - 44032) / 588) + 4352;
                } else if (character >= 12593 && (character - 12593 >= KOREAN_JAUM_CONVERT_MAP.length || (character = KOREAN_JAUM_CONVERT_MAP[character - 12593]) == 0)) {
                    break;
                }
                this.mStringBuilder.appendCodePoint(character);
                consonantLength++;
                if (position2 < stringLength) {
                }
            } else if (position2 < stringLength) {
                break;
            } else {
                position = position2;
            }
        }
        if (consonantLength > 1) {
            builder.appendName(this.mStringBuilder.toString());
        }
    }

    protected String normalizeName(String name) {
        return NameNormalizer.normalize(name);
    }

    private void insertNameVariants(long rawContactId, long dataId, int fromIndex, int toIndex, boolean initiallyExact, boolean buildCollationKey) {
        if (fromIndex == toIndex) {
            insertNameVariant(rawContactId, dataId, toIndex, initiallyExact ? 0 : 1, buildCollationKey);
            return;
        }
        String firstToken = this.mNames[fromIndex];
        int i = fromIndex;
        while (i < toIndex) {
            this.mNames[fromIndex] = this.mNames[i];
            this.mNames[i] = firstToken;
            insertNameVariants(rawContactId, dataId, fromIndex + 1, toIndex, initiallyExact && i == fromIndex, buildCollationKey);
            this.mNames[i] = this.mNames[fromIndex];
            this.mNames[fromIndex] = firstToken;
            i++;
        }
    }

    private void insertNameVariant(long rawContactId, long dataId, int tokenCount, int lookupType, boolean buildCollationKey) {
        this.mStringBuilder.setLength(0);
        for (int i = 0; i < tokenCount; i++) {
            if (i != 0) {
                this.mStringBuilder.append('.');
            }
            this.mStringBuilder.append(this.mNames[i]);
        }
        insertNameLookup(rawContactId, dataId, lookupType, this.mStringBuilder.toString());
        if (buildCollationKey) {
            insertCollationKey(rawContactId, dataId, tokenCount);
        }
    }

    private void insertCollationKey(long rawContactId, long dataId, int tokenCount) {
        this.mStringBuilder.setLength(0);
        for (int i = 0; i < tokenCount; i++) {
            this.mStringBuilder.append(this.mNames[i]);
        }
        insertNameLookup(rawContactId, dataId, 2, this.mStringBuilder.toString());
    }

    private void insertNicknamePermutations(long rawContactId, long dataId, int fromIndex, int tokenCount) {
        for (int i = fromIndex; i < tokenCount; i++) {
            String[] clusters = this.mNicknameClusters[i];
            if (clusters != null) {
                String token = this.mNames[i];
                for (String str : clusters) {
                    this.mNames[i] = str;
                    insertNameVariants(rawContactId, dataId, 0, tokenCount, false, false);
                    insertNicknamePermutations(rawContactId, dataId, i + 1, tokenCount);
                }
                this.mNames[i] = token;
            }
        }
    }

    public void appendNameShorthandLookup(SearchIndexManager.IndexBuilder builder, String name, int fullNameStyle) {
        Iterator<String> it = ContactLocaleUtils.getInstance().getNameLookupKeys(name, fullNameStyle);
        if (it != null) {
            while (it.hasNext()) {
                builder.appendName(it.next());
            }
        }
    }
}
