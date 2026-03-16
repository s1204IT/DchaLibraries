package com.google.android.util;

import android.net.ProxyInfo;
import com.google.android.util.AbstractMessageParser;
import java.util.HashMap;
import java.util.Set;

public class SmileyResources implements AbstractMessageParser.Resources {
    private HashMap<String, Integer> mSmileyToRes = new HashMap<>();
    private final AbstractMessageParser.TrieNode smileys = new AbstractMessageParser.TrieNode();

    public SmileyResources(String[] smilies, int[] smileyResIds) {
        for (int i = 0; i < smilies.length; i++) {
            AbstractMessageParser.TrieNode.addToTrie(this.smileys, smilies[i], ProxyInfo.LOCAL_EXCL_LIST);
            this.mSmileyToRes.put(smilies[i], Integer.valueOf(smileyResIds[i]));
        }
    }

    public int getSmileyRes(String smiley) {
        Integer i = this.mSmileyToRes.get(smiley);
        if (i == null) {
            return -1;
        }
        return i.intValue();
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
