package com.mediatek.internal.telephony.ppl;

import android.content.Context;
import java.util.Arrays;
import java.util.regex.Pattern;

public class PplMessageManager {
    public static final String SMS_SENT_ACTION = "com.mediatek.ppl.SMS_SENT";
    private final Context mContext;
    private final Pattern[] mMessagePatterns;
    private final String[] mMessageTemplates;
    private static final String[] SMS_TEMPLATES = {"我的手机可能被盗，请保留发送此短信的号码。", "#suoding#", "已接受到您的锁屏指令，锁屏成功。", "#jiesuo#", "已接受到您的解锁指令，解锁成功。", "#mima#", "您的手机防盗密码为%s。", "#xiaohui#", "远程删除数据已开始。", "远程数据删除已完成，您的隐私得到保护，请放心。", "我开启了手机防盗功能，已将你的手机号码设置为紧急联系人号码，这样手机丢失也能够远程控制啦。\n以下是相关指令：\n远程锁定： #suoding#\n远程销毁数据： #xiaohui#\n找回密码： #mima#"};
    private static final String[] SMS_PATTERNS = {"我的手机可能被盗，请保留发送此短信的号码。", " *#suoding# *", "已接受到您的锁屏指令，锁屏成功。", " *#jiesuo# *", "已接受到您的解锁指令，解锁成功。", " *#mima# *", "您的手机防盗密码为[0-9]*。", " *#xiaohui# *", "远程删除数据已开始。", "远程数据删除已完成，您的隐私得到保护，请放心。", "我开启了手机防盗功能，已将你的手机号码设置为紧急联系人号码，这样手机丢失也能够远程控制啦。\n以下是相关指令：\n远程锁定： #suoding#\n远程销毁数据： #xiaohui#\n找回密码： #mima#"};

    public static class Type {
        public static final byte INSTRUCTION_DESCRIPTION = 10;
        public static final byte INSTRUCTION_DESCRIPTION2 = 11;
        public static final byte INVALID = -1;
        public static final byte LOCK_REQUEST = 1;
        public static final byte LOCK_RESPONSE = 2;
        public static final byte RESET_PW_REQUEST = 5;
        public static final byte RESET_PW_RESPONSE = 6;
        public static final byte SIM_CHANGED = 0;
        public static final byte UNLOCK_REQUEST = 3;
        public static final byte UNLOCK_RESPONSE = 4;
        public static final byte WIPE_COMPLETED = 9;
        public static final byte WIPE_REQUEST = 7;
        public static final byte WIPE_STARTED = 8;
    }

    public static class PendingMessage {
        public static final int ALL_SIM_ID = -2;
        public static final int ANY_SIM_ID = -1;
        public static final long INVALID_ID = -1;
        public static final int INVALID_SIM_ID = -3;
        public static final String KEY_FIRST_TRIAL = "firstTrial";
        public static final String KEY_ID = "id";
        public static final String KEY_NUMBER = "number";
        public static final String KEY_SEGMENT_INDEX = "segmentIndex";
        public static final String KEY_SIM_ID = "simId";
        public static final String KEY_TYPE = "type";
        public static final int PENDING_MESSAGE_LENGTH = 49;
        public String content;
        public long id;
        public String number;
        public int simId;
        public byte type;

        public static long getNextId() {
            return System.currentTimeMillis();
        }

        public PendingMessage(long j, byte b, String str, int i, String str2) {
            this.id = j;
            this.type = b;
            this.number = str;
            this.simId = i;
            this.content = str2;
        }

        public PendingMessage() {
            this.id = -1L;
            this.type = (byte) -1;
            this.number = null;
            this.simId = -1;
            this.content = null;
        }

        public PendingMessage(byte[] bArr, int i) {
            decode(bArr, i);
        }

        public PendingMessage m4clone() {
            return new PendingMessage(this.id, this.type, this.number, this.simId, this.content);
        }

        public String toString() {
            return "PendingMessage " + hashCode() + " {" + this.id + ", " + ((int) this.type) + ", " + this.number + ", " + this.simId + ", " + this.content + "}";
        }

        public void encode(byte[] bArr, int i) {
            int i2 = i + 1;
            bArr[i] = this.type;
            byte[] bArrLong2bytes = long2bytes(this.id);
            System.arraycopy(bArrLong2bytes, 0, bArr, i2, bArrLong2bytes.length);
            int i3 = i2 + 8;
            byte[] bytes = this.number.getBytes();
            if (bytes.length > 40) {
                throw new Error("Destination number is too long");
            }
            byte[] bArrCopyOf = Arrays.copyOf(bytes, 40);
            System.arraycopy(bArrCopyOf, 0, bArr, i3, bArrCopyOf.length);
        }

        public void decode(byte[] bArr, int i) {
            int i2 = i + 1;
            this.type = bArr[i];
            this.id = bytes2long(bArr, i2);
            int i3 = i2 + 8;
            int i4 = i3;
            while (i4 < i3 + 40 && bArr[i4] != 0) {
                i4++;
            }
            this.number = new String(bArr, i3, i4 - i3);
        }

        private static long bytes2long(byte[] bArr, int i) {
            long j = 0;
            for (int i2 = 0; i2 < 8; i2++) {
                j = (j << 8) | ((long) (bArr[i2 + i] & Type.INVALID));
            }
            return j;
        }

        private static byte[] long2bytes(long j) {
            byte[] bArr = new byte[8];
            for (int i = 0; i < 8; i++) {
                bArr[i] = (byte) (j >>> (56 - (i * 8)));
            }
            return bArr;
        }
    }

    public PplMessageManager(Context context) {
        this.mContext = context;
        this.mContext.getResources();
        this.mMessageTemplates = SMS_TEMPLATES;
        String[] strArr = SMS_PATTERNS;
        this.mMessagePatterns = new Pattern[strArr.length];
        for (int i = 0; i < strArr.length; i++) {
            this.mMessagePatterns[i] = Pattern.compile(strArr[i], 2);
        }
    }

    public byte getMessageType(String str) {
        byte b = 0;
        while (true) {
            if (b < this.mMessagePatterns.length) {
                if (this.mMessagePatterns[b].matcher(str).matches()) {
                    break;
                }
                b = (byte) (b + 1);
            } else {
                b = -1;
                break;
            }
        }
        if (b == 11) {
            return (byte) 10;
        }
        return b;
    }

    public String getMessageTemplate(byte b) {
        return this.mMessageTemplates[b];
    }

    public String buildMessage(byte b, Object... objArr) {
        return String.format(getMessageTemplate(b), objArr);
    }
}
