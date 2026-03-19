package com.google.android.util;

import com.google.android.util.AbstractMessageParser;
import java.util.HashMap;
import java.util.Set;

public class SmileyResources implements AbstractMessageParser.Resources {
    private HashMap<String, Integer> mSmileyToRes = new HashMap<>();
    private final AbstractMessageParser.TrieNode smileys = new AbstractMessageParser.TrieNode();

    public SmileyResources(String[] strArr, int[] iArr) {
        for (int i = 0; i < strArr.length; i++) {
            AbstractMessageParser.TrieNode.addToTrie(this.smileys, strArr[i], "");
            this.mSmileyToRes.put(strArr[i], Integer.valueOf(iArr[i]));
        }
    }

    public int getSmileyRes(String str) {
        Integer num = this.mSmileyToRes.get(str);
        if (num == null) {
            return -1;
        }
        return num.intValue();
    }

    @Override
    public Set<String> getSchemes() {
        return null;
    }

    @Override
    public AbstractMessageParser.TrieNode getDomainSuffixes() {
        return null;
    }

    @Override
    public AbstractMessageParser.TrieNode getSmileys() {
        return this.smileys;
    }

    @Override
    public AbstractMessageParser.TrieNode getAcronyms() {
        return null;
    }
}
