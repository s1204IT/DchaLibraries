package com.mediatek.internal.telephony;

public class SrvccCallContext {
    private int mCallDirection;
    private int mCallId;
    private int mCallMode;
    private int mCallState;
    private int mCliValidity;
    private int mEccCategory;
    private String mName;
    private String mNumber;
    private int mNumberType;

    public SrvccCallContext(int callId, int callMode, int callDirection, int callState, int eccCategory, int numberType, String phoneNumber, String name, int cliValidity) {
        this.mCallId = callId;
        this.mCallMode = callMode;
        this.mCallDirection = callDirection;
        this.mCallState = callState;
        this.mEccCategory = eccCategory;
        this.mNumberType = numberType;
        this.mNumber = phoneNumber;
        this.mName = name;
        this.mCliValidity = cliValidity;
    }

    public void setCallId(int callId) {
        this.mCallId = callId;
    }

    public void setCallMode(int callMode) {
        this.mCallMode = callMode;
    }

    public void setCallDirection(int callDirection) {
        this.mCallDirection = callDirection;
    }

    public void setCallState(int callState) {
        this.mCallState = callState;
    }

    public void setEccCategory(int eccCategory) {
        this.mEccCategory = eccCategory;
    }

    public void setNumberType(int numberType) {
        this.mNumberType = numberType;
    }

    public void setCallState(String phoneNumber) {
        this.mNumber = phoneNumber;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public void setCliValidity(int cliValidity) {
        this.mCliValidity = cliValidity;
    }

    public int getCallId() {
        return this.mCallId;
    }

    public int getCallMode() {
        return this.mCallMode;
    }

    public int getCallDirection() {
        return this.mCallDirection;
    }

    public int getCallState() {
        return this.mCallState;
    }

    public int getEccCategory() {
        return this.mEccCategory;
    }

    public int getNumberType() {
        return this.mNumberType;
    }

    public String getNumber() {
        return this.mNumber;
    }

    public String getName() {
        return this.mName;
    }

    public int getCliValidity() {
        return this.mCliValidity;
    }
}
