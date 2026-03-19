package com.android.se.security.arf.pkcs15;

import android.util.Log;
import com.android.se.security.ApduFilter;
import com.android.se.security.ChannelAccess;
import com.android.se.security.arf.ASN1;
import com.android.se.security.arf.DERParser;
import com.android.se.security.arf.SecureElement;
import com.android.se.security.gpac.AID_REF_DO;
import com.android.se.security.gpac.Hash_REF_DO;
import java.io.IOException;
import java.util.Vector;

public class EFACConditions extends EF {
    public static final String TAG = "ACE ARF EF_ACConditions";
    private AID_REF_DO mAidRefDo;
    private byte[] mData;

    public EFACConditions(SecureElement secureElement, AID_REF_DO aid_ref_do) {
        super(secureElement);
        this.mAidRefDo = null;
        this.mData = null;
        this.mAidRefDo = aid_ref_do;
    }

    private void decodeDER(byte[] bArr) throws PKCS15Exception {
        Hash_REF_DO hash_REF_DO;
        ChannelAccess.ACCESS access;
        ChannelAccess.ACCESS access2;
        DERParser dERParser = new DERParser(bArr);
        ChannelAccess channelAccess = new ChannelAccess();
        Hash_REF_DO hash_REF_DO2 = new Hash_REF_DO();
        if (dERParser.isEndofBuffer()) {
            channelAccess.setAccess(ChannelAccess.ACCESS.DENIED, "Empty ACCondition");
            channelAccess.setNFCEventAccess(ChannelAccess.ACCESS.DENIED);
            channelAccess.setApduAccess(ChannelAccess.ACCESS.DENIED);
            channelAccess.setUseApduFilter(false);
            channelAccess.setApduFilter(null);
            Log.i(TAG, "Empty ACCondition: Access Deny for all apps");
            this.mSEHandle.putAccessRule(this.mAidRefDo, hash_REF_DO2, channelAccess);
            return;
        }
        while (!dERParser.isEndofBuffer()) {
            ChannelAccess channelAccess2 = new ChannelAccess();
            channelAccess2.setAccess(ChannelAccess.ACCESS.ALLOWED, "");
            channelAccess2.setApduAccess(ChannelAccess.ACCESS.ALLOWED);
            channelAccess2.setNFCEventAccess(ChannelAccess.ACCESS.UNDEFINED);
            channelAccess2.setUseApduFilter(false);
            if (dERParser.parseTLV(ASN1.TAG_Sequence) > 0) {
                byte[] tLVData = dERParser.getTLVData();
                DERParser dERParser2 = new DERParser(tLVData);
                if (tLVData[0] == 4) {
                    dERParser2.parseTLV((byte) 4);
                    byte[] tLVData2 = dERParser2.getTLVData();
                    if (tLVData2.length != 20 && tLVData2.length != 0) {
                        throw new PKCS15Exception("Invalid hash found!");
                    }
                    hash_REF_DO = new Hash_REF_DO(tLVData2);
                } else {
                    if (tLVData[0] == -1) {
                        throw new PKCS15Exception("Invalid hash found!");
                    }
                    Log.i(TAG, "No hash included");
                    hash_REF_DO = new Hash_REF_DO(null);
                }
                if (!dERParser2.isEndofBuffer() && dERParser2.parseTLV() == -96) {
                    DERParser dERParser3 = new DERParser(dERParser2.getTLVData());
                    while (!dERParser3.isEndofBuffer()) {
                        switch (dERParser3.parseTLV()) {
                            case -96:
                                DERParser dERParser4 = new DERParser(dERParser3.getTLVData());
                                byte tlv = dERParser4.parseTLV();
                                if (tlv == -128) {
                                    if (dERParser4.getTLVData()[0] == 1) {
                                        access2 = ChannelAccess.ACCESS.ALLOWED;
                                    } else {
                                        access2 = ChannelAccess.ACCESS.DENIED;
                                    }
                                    channelAccess2.setApduAccess(access2);
                                } else if (tlv == -95) {
                                    DERParser dERParser5 = new DERParser(dERParser4.getTLVData());
                                    if (dERParser5.parseTLV() == 4) {
                                        Vector vector = new Vector();
                                        vector.add(new ApduFilter(dERParser5.getTLVData()));
                                        while (!dERParser5.isEndofBuffer()) {
                                            if (dERParser5.parseTLV() == 4) {
                                                vector.add(new ApduFilter(dERParser5.getTLVData()));
                                            }
                                        }
                                        channelAccess2.setUseApduFilter(true);
                                        channelAccess2.setApduFilter((ApduFilter[]) vector.toArray(new ApduFilter[vector.size()]));
                                    } else {
                                        throw new PKCS15Exception("Invalid element found!");
                                    }
                                } else {
                                    throw new PKCS15Exception("Invalid element found!");
                                }
                                break;
                            case -95:
                                DERParser dERParser6 = new DERParser(dERParser3.getTLVData());
                                if (dERParser6.parseTLV() == -128) {
                                    if (dERParser6.getTLVData()[0] == 1) {
                                        access = ChannelAccess.ACCESS.ALLOWED;
                                    } else {
                                        access = ChannelAccess.ACCESS.DENIED;
                                    }
                                    channelAccess2.setNFCEventAccess(access);
                                } else {
                                    throw new PKCS15Exception("Invalid element found!");
                                }
                                break;
                            default:
                                throw new PKCS15Exception("Invalid element found!");
                        }
                    }
                }
                hash_REF_DO2 = hash_REF_DO;
            } else if (channelAccess2.getNFCEventAccess() == ChannelAccess.ACCESS.UNDEFINED && channelAccess2.getApduAccess() != ChannelAccess.ACCESS.UNDEFINED) {
                channelAccess2.setNFCEventAccess(channelAccess2.getApduAccess());
            }
            this.mSEHandle.putAccessRule(this.mAidRefDo, hash_REF_DO2, channelAccess2);
        }
    }

    public void addRestrictedHashes(byte[] bArr) throws IOException, PKCS15Exception {
        try {
            Log.i(TAG, "Reading and analysing EF_ACConditions...");
            if (selectFile(bArr) == 36864) {
                this.mData = readBinary(0, -1);
                decodeDER(this.mData);
            } else {
                Log.e(TAG, "EF_ACConditions not found!");
            }
        } catch (Exception e) {
            throw new PKCS15Exception(e.getMessage());
        }
    }

    public void addRestrictedHashesFromData(byte[] bArr) throws PKCS15Exception {
        try {
            Log.i(TAG, "Analysing cached EF_ACConditions data...");
            if (bArr != null) {
                this.mData = bArr;
                decodeDER(this.mData);
            } else {
                Log.e(TAG, "EF_ACConditions data not available!");
            }
        } catch (Exception e) {
            throw new PKCS15Exception(e.getMessage());
        }
    }

    public byte[] getData() {
        return this.mData;
    }
}
