package com.android.se.security.ara;

import com.android.se.Channel;
import com.android.se.security.CommandApdu;
import com.android.se.security.ResponseApdu;
import com.android.se.security.arf.pkcs15.EF;
import com.android.se.security.gpac.BerTlv;
import com.android.se.security.gpac.ParserException;
import com.android.se.security.gpac.Response_DO_Factory;
import com.android.se.security.gpac.Response_RefreshTag_DO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.AccessControlException;

public class AccessRuleApplet {
    private static final CommandApdu GET_ALL_CMD = new CommandApdu(128, 202, 255, 64, 0);
    private static final CommandApdu GET_NEXT_CMD = new CommandApdu(128, 202, 255, 96, 0);
    private static final CommandApdu GET_REFRESH_TAG = new CommandApdu(128, 202, 223, 32, 0);
    private static final int MAX_LEN = 0;
    private Channel mChannel;
    private final String mTag = "SecureElement-AccessRuleApplet";

    public AccessRuleApplet(Channel channel) {
        this.mChannel = null;
        this.mChannel = channel;
    }

    public byte[] readAllAccessRules() throws AccessControlException, IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ResponseApdu responseApduSend = send(GET_ALL_CMD.m2clone());
        if (responseApduSend.isStatus(EF.APDU_SUCCESS)) {
            try {
                BerTlv berTlvDecode = BerTlv.decode(responseApduSend.getData(), 0, false);
                int valueLength = berTlvDecode.getValueLength() + berTlvDecode.getValueIndex();
                try {
                    byteArrayOutputStream.write(responseApduSend.getData());
                    while (byteArrayOutputStream.size() < valueLength) {
                        int size = valueLength - byteArrayOutputStream.size();
                        if (size > 0) {
                            size = 0;
                        }
                        CommandApdu commandApduM2clone = GET_NEXT_CMD.m2clone();
                        commandApduM2clone.setLe(size);
                        ResponseApdu responseApduSend2 = send(commandApduM2clone);
                        if (responseApduSend2.isStatus(EF.APDU_SUCCESS)) {
                            try {
                                byteArrayOutputStream.write(responseApduSend2.getData());
                            } catch (IOException e) {
                                throw new AccessControlException("GET DATA (next) IO problem. " + e.getMessage());
                            }
                        } else {
                            throw new AccessControlException("GET DATA (next) not successfull, SW1SW2=" + responseApduSend2.getSW1SW2());
                        }
                    }
                    return byteArrayOutputStream.toByteArray();
                } catch (IOException e2) {
                    throw new AccessControlException("GET DATA (all) IO problem. " + e2.getMessage());
                }
            } catch (ParserException e3) {
                throw new AccessControlException("GET DATA (all) not successfull. Tlv encoding wrong.");
            }
        }
        if (responseApduSend.isStatus(27272)) {
            return null;
        }
        throw new AccessControlException("GET DATA (all) not successfull. SW1SW2=" + responseApduSend.getSW1SW2());
    }

    public byte[] readRefreshTag() throws AccessControlException, IOException {
        ResponseApdu responseApduSend = send(GET_REFRESH_TAG.m2clone());
        if (responseApduSend.isStatus(EF.APDU_SUCCESS)) {
            try {
                ?? CreateDO = Response_DO_Factory.createDO(responseApduSend.getData());
                if (CreateDO instanceof Response_RefreshTag_DO) {
                    return CreateDO.getRefreshTagArray();
                }
                throw new AccessControlException("GET REFRESH TAG returned invalid Tlv.");
            } catch (ParserException e) {
                throw new AccessControlException("GET REFRESH TAG not successfull. Tlv encoding wrong.");
            }
        }
        throw new AccessControlException("GET REFRESH TAG not successfull.");
    }

    private ResponseApdu send(CommandApdu commandApdu) throws IOException {
        return new ResponseApdu(this.mChannel.transmit(commandApdu.toBytes()));
    }
}
