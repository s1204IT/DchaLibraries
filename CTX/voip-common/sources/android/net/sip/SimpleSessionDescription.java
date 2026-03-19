package android.net.sip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;

public class SimpleSessionDescription {
    private final Fields mFields = new Fields("voscbtka");
    private final ArrayList<Media> mMedia = new ArrayList<>();

    public SimpleSessionDescription(long j, String str) {
        StringBuilder sb = new StringBuilder();
        sb.append(str.indexOf(58) < 0 ? "IN IP4 " : "IN IP6 ");
        sb.append(str);
        String string = sb.toString();
        this.mFields.parse("v=0");
        this.mFields.parse(String.format(Locale.US, "o=- %d %d %s", Long.valueOf(j), Long.valueOf(System.currentTimeMillis()), string));
        this.mFields.parse("s=-");
        this.mFields.parse("t=0 0");
        this.mFields.parse("c=" + string);
    }

    public SimpleSessionDescription(String str) {
        String[] strArrSplit = str.trim().replaceAll(" +", " ").split("[\r\n]+");
        Fields fields = this.mFields;
        int length = strArrSplit.length;
        ?? NewMedia = fields;
        int i = 0;
        while (i < length) {
            String str2 = strArrSplit[i];
            int i2 = 1;
            try {
                if (str2.charAt(1) != '=') {
                    throw new IllegalArgumentException();
                }
                if (str2.charAt(0) == 'm') {
                    String[] strArrSplit2 = str2.substring(2).split(" ", 4);
                    String[] strArrSplit3 = strArrSplit2[1].split("/", 2);
                    String str3 = strArrSplit2[0];
                    int i3 = Integer.parseInt(strArrSplit3[0]);
                    if (strArrSplit3.length >= 2) {
                        i2 = Integer.parseInt(strArrSplit3[1]);
                    }
                    NewMedia = newMedia(str3, i3, i2, strArrSplit2[2]);
                    for (String str4 : strArrSplit2[3].split(" ")) {
                        NewMedia.setFormat(str4, null);
                    }
                } else {
                    NewMedia.parse(str2);
                }
                i++;
                NewMedia = NewMedia;
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid SDP: " + str2);
            }
        }
    }

    public Media newMedia(String str, int i, int i2, String str2) {
        Media media = new Media(str, i, i2, str2);
        this.mMedia.add(media);
        return media;
    }

    public Media[] getMedia() {
        return (Media[]) this.mMedia.toArray(new Media[this.mMedia.size()]);
    }

    public String encode() {
        StringBuilder sb = new StringBuilder();
        this.mFields.write(sb);
        Iterator<Media> it = this.mMedia.iterator();
        while (it.hasNext()) {
            it.next().write(sb);
        }
        return sb.toString();
    }

    public String getAddress() {
        return this.mFields.getAddress();
    }

    public void setAddress(String str) {
        this.mFields.setAddress(str);
    }

    public String getEncryptionMethod() {
        return this.mFields.getEncryptionMethod();
    }

    public String getEncryptionKey() {
        return this.mFields.getEncryptionKey();
    }

    public void setEncryption(String str, String str2) {
        this.mFields.setEncryption(str, str2);
    }

    public String[] getBandwidthTypes() {
        return this.mFields.getBandwidthTypes();
    }

    public int getBandwidth(String str) {
        return this.mFields.getBandwidth(str);
    }

    public void setBandwidth(String str, int i) {
        this.mFields.setBandwidth(str, i);
    }

    public String[] getAttributeNames() {
        return this.mFields.getAttributeNames();
    }

    public String getAttribute(String str) {
        return this.mFields.getAttribute(str);
    }

    public void setAttribute(String str, String str2) {
        this.mFields.setAttribute(str, str2);
    }

    public static class Media extends Fields {
        private ArrayList<String> mFormats;
        private final int mPort;
        private final int mPortCount;
        private final String mProtocol;
        private final String mType;

        @Override
        public String getAddress() {
            return super.getAddress();
        }

        @Override
        public String getAttribute(String str) {
            return super.getAttribute(str);
        }

        @Override
        public String[] getAttributeNames() {
            return super.getAttributeNames();
        }

        @Override
        public int getBandwidth(String str) {
            return super.getBandwidth(str);
        }

        @Override
        public String[] getBandwidthTypes() {
            return super.getBandwidthTypes();
        }

        @Override
        public String getEncryptionKey() {
            return super.getEncryptionKey();
        }

        @Override
        public String getEncryptionMethod() {
            return super.getEncryptionMethod();
        }

        @Override
        public void setAddress(String str) {
            super.setAddress(str);
        }

        @Override
        public void setAttribute(String str, String str2) {
            super.setAttribute(str, str2);
        }

        @Override
        public void setBandwidth(String str, int i) {
            super.setBandwidth(str, i);
        }

        @Override
        public void setEncryption(String str, String str2) {
            super.setEncryption(str, str2);
        }

        private Media(String str, int i, int i2, String str2) {
            super("icbka");
            this.mFormats = new ArrayList<>();
            this.mType = str;
            this.mPort = i;
            this.mPortCount = i2;
            this.mProtocol = str2;
        }

        public String getType() {
            return this.mType;
        }

        public int getPort() {
            return this.mPort;
        }

        public int getPortCount() {
            return this.mPortCount;
        }

        public String getProtocol() {
            return this.mProtocol;
        }

        public String[] getFormats() {
            return (String[]) this.mFormats.toArray(new String[this.mFormats.size()]);
        }

        public String getFmtp(String str) {
            return get("a=fmtp:" + str, ' ');
        }

        public void setFormat(String str, String str2) {
            this.mFormats.remove(str);
            this.mFormats.add(str);
            set("a=rtpmap:" + str, ' ', null);
            set("a=fmtp:" + str, ' ', str2);
        }

        public void removeFormat(String str) {
            this.mFormats.remove(str);
            set("a=rtpmap:" + str, ' ', null);
            set("a=fmtp:" + str, ' ', null);
        }

        public int[] getRtpPayloadTypes() {
            int[] iArr = new int[this.mFormats.size()];
            Iterator<String> it = this.mFormats.iterator();
            int i = 0;
            while (it.hasNext()) {
                try {
                    iArr[i] = Integer.parseInt(it.next());
                    i++;
                } catch (NumberFormatException e) {
                }
            }
            return Arrays.copyOf(iArr, i);
        }

        public String getRtpmap(int i) {
            return get("a=rtpmap:" + i, ' ');
        }

        public String getFmtp(int i) {
            return get("a=fmtp:" + i, ' ');
        }

        public void setRtpPayload(int i, String str, String str2) {
            String strValueOf = String.valueOf(i);
            this.mFormats.remove(strValueOf);
            this.mFormats.add(strValueOf);
            set("a=rtpmap:" + strValueOf, ' ', str);
            set("a=fmtp:" + strValueOf, ' ', str2);
        }

        public void removeRtpPayload(int i) {
            removeFormat(String.valueOf(i));
        }

        private void write(StringBuilder sb) {
            sb.append("m=");
            sb.append(this.mType);
            sb.append(' ');
            sb.append(this.mPort);
            if (this.mPortCount != 1) {
                sb.append('/');
                sb.append(this.mPortCount);
            }
            sb.append(' ');
            sb.append(this.mProtocol);
            for (String str : this.mFormats) {
                sb.append(' ');
                sb.append(str);
            }
            sb.append("\r\n");
            write(sb);
        }
    }

    private static class Fields {
        private final ArrayList<String> mLines = new ArrayList<>();
        private final String mOrder;

        Fields(String str) {
            this.mOrder = str;
        }

        public String getAddress() {
            String str = get("c", '=');
            if (str == null) {
                return null;
            }
            String[] strArrSplit = str.split(" ");
            if (strArrSplit.length != 3) {
                return null;
            }
            int iIndexOf = strArrSplit[2].indexOf(47);
            return iIndexOf < 0 ? strArrSplit[2] : strArrSplit[2].substring(0, iIndexOf);
        }

        public void setAddress(String str) {
            if (str != null) {
                StringBuilder sb = new StringBuilder();
                sb.append(str.indexOf(58) < 0 ? "IN IP4 " : "IN IP6 ");
                sb.append(str);
                str = sb.toString();
            }
            set("c", '=', str);
        }

        public String getEncryptionMethod() {
            String str = get("k", '=');
            if (str == null) {
                return null;
            }
            int iIndexOf = str.indexOf(58);
            return iIndexOf == -1 ? str : str.substring(0, iIndexOf);
        }

        public String getEncryptionKey() {
            int iIndexOf;
            String str = get("k", '=');
            if (str == null || (iIndexOf = str.indexOf(58)) == -1) {
                return null;
            }
            return str.substring(0, iIndexOf + 1);
        }

        public void setEncryption(String str, String str2) {
            if (str != null && str2 != null) {
                str = str + ':' + str2;
            }
            set("k", '=', str);
        }

        public String[] getBandwidthTypes() {
            return cut("b=", ':');
        }

        public int getBandwidth(String str) {
            String str2 = get("b=" + str, ':');
            if (str2 != null) {
                try {
                    return Integer.parseInt(str2);
                } catch (NumberFormatException e) {
                    setBandwidth(str, -1);
                }
            }
            return -1;
        }

        public void setBandwidth(String str, int i) {
            set("b=" + str, ':', i < 0 ? null : String.valueOf(i));
        }

        public String[] getAttributeNames() {
            return cut("a=", ':');
        }

        public String getAttribute(String str) {
            return get("a=" + str, ':');
        }

        public void setAttribute(String str, String str2) {
            set("a=" + str, ':', str2);
        }

        private void write(StringBuilder sb) {
            for (int i = 0; i < this.mOrder.length(); i++) {
                char cCharAt = this.mOrder.charAt(i);
                for (String str : this.mLines) {
                    if (str.charAt(0) == cCharAt) {
                        sb.append(str);
                        sb.append("\r\n");
                    }
                }
            }
        }

        private void parse(String str) {
            char cCharAt = str.charAt(0);
            if (this.mOrder.indexOf(cCharAt) == -1) {
                return;
            }
            char c = '=';
            if (str.startsWith("a=rtpmap:") || str.startsWith("a=fmtp:")) {
                c = ' ';
            } else if (cCharAt == 'b' || cCharAt == 'a') {
                c = ':';
            }
            int iIndexOf = str.indexOf(c);
            if (iIndexOf == -1) {
                set(str, c, "");
            } else {
                set(str.substring(0, iIndexOf), c, str.substring(iIndexOf + 1));
            }
        }

        private String[] cut(String str, char c) {
            String[] strArr = new String[this.mLines.size()];
            int i = 0;
            for (String str2 : this.mLines) {
                if (str2.startsWith(str)) {
                    int iIndexOf = str2.indexOf(c);
                    if (iIndexOf == -1) {
                        iIndexOf = str2.length();
                    }
                    strArr[i] = str2.substring(str.length(), iIndexOf);
                    i++;
                }
            }
            return (String[]) Arrays.copyOf(strArr, i);
        }

        private int find(String str, char c) {
            int length = str.length();
            for (int size = this.mLines.size() - 1; size >= 0; size--) {
                String str2 = this.mLines.get(size);
                if (str2.startsWith(str) && (str2.length() == length || str2.charAt(length) == c)) {
                    return size;
                }
            }
            return -1;
        }

        private void set(String str, char c, String str2) {
            int iFind = find(str, c);
            if (str2 != null) {
                if (str2.length() != 0) {
                    str = str + c + str2;
                }
                if (iFind == -1) {
                    this.mLines.add(str);
                    return;
                } else {
                    this.mLines.set(iFind, str);
                    return;
                }
            }
            if (iFind != -1) {
                this.mLines.remove(iFind);
            }
        }

        private String get(String str, char c) {
            int iFind = find(str, c);
            if (iFind == -1) {
                return null;
            }
            String str2 = this.mLines.get(iFind);
            int length = str.length();
            return str2.length() == length ? "" : str2.substring(length + 1);
        }
    }
}
