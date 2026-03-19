package com.android.commands.monkey;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MonkeySourceRandomScript implements MonkeyEventSource {
    private MonkeySourceScript mCurrentSource;
    private Random mRandom;
    private boolean mRandomizeScript;
    private int mScriptCount;
    private ArrayList<MonkeySourceScript> mScriptSources;
    private MonkeySourceScript mSetupSource;
    private int mVerbose;

    public MonkeySourceRandomScript(String str, ArrayList<String> arrayList, long j, boolean z, Random random, long j2, long j3, boolean z2) {
        this.mVerbose = 0;
        this.mSetupSource = null;
        this.mScriptSources = new ArrayList<>();
        this.mCurrentSource = null;
        this.mRandomizeScript = false;
        this.mScriptCount = 0;
        if (str != null) {
            this.mSetupSource = new MonkeySourceScript(random, str, j, z, j2, j3);
            this.mCurrentSource = this.mSetupSource;
        }
        Iterator<String> it = arrayList.iterator();
        while (it.hasNext()) {
            this.mScriptSources.add(new MonkeySourceScript(random, it.next(), j, z, j2, j3));
        }
        this.mRandom = random;
        this.mRandomizeScript = z2;
    }

    public MonkeySourceRandomScript(ArrayList<String> arrayList, long j, boolean z, Random random, long j2, long j3, boolean z2) {
        this(null, arrayList, j, z, random, j2, j3, z2);
    }

    @Override
    public MonkeyEvent getNextEvent() {
        if (this.mCurrentSource == null) {
            int size = this.mScriptSources.size();
            if (size == 1) {
                this.mCurrentSource = this.mScriptSources.get(0);
            } else if (size > 1) {
                if (this.mRandomizeScript) {
                    this.mCurrentSource = this.mScriptSources.get(this.mRandom.nextInt(size));
                } else {
                    this.mCurrentSource = this.mScriptSources.get(this.mScriptCount % size);
                    this.mScriptCount++;
                }
            }
        }
        if (this.mCurrentSource == null) {
            return null;
        }
        MonkeyEvent nextEvent = this.mCurrentSource.getNextEvent();
        if (nextEvent == null) {
            this.mCurrentSource = null;
        }
        return nextEvent;
    }

    @Override
    public void setVerbose(int i) {
        this.mVerbose = i;
        if (this.mSetupSource != null) {
            this.mSetupSource.setVerbose(i);
        }
        Iterator<MonkeySourceScript> it = this.mScriptSources.iterator();
        while (it.hasNext()) {
            it.next().setVerbose(i);
        }
    }

    @Override
    public boolean validate() {
        if (this.mSetupSource != null && !this.mSetupSource.validate()) {
            return false;
        }
        Iterator<MonkeySourceScript> it = this.mScriptSources.iterator();
        while (it.hasNext()) {
            if (!it.next().validate()) {
                return false;
            }
        }
        return true;
    }
}
