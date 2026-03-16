package java.net;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import libcore.io.Memory;

class Socks4Message {
    private static final int BUFFER_LENGTH = 256;
    static final int COMMAND_BIND = 2;
    static final int COMMAND_CONNECT = 1;
    private static final int INDEX_COMMAND = 1;
    private static final int INDEX_IP = 4;
    private static final int INDEX_PORT = 2;
    private static final int INDEX_USER_ID = 8;
    static final int INDEX_VERSION = 0;
    private static final int MAX_USER_ID_LENGTH = 248;
    static final int REPLY_LENGTH = 8;
    static final int RETURN_CANNOT_CONNECT_TO_IDENTD = 92;
    static final int RETURN_DIFFERENT_USER_IDS = 93;
    static final int RETURN_FAILURE = 91;
    static final int RETURN_SUCCESS = 90;
    private static final int SOCKS_VERSION = 4;
    protected byte[] buffer = new byte[256];

    public Socks4Message() {
        setVersionNumber(4);
    }

    public int getCommandOrResult() {
        return this.buffer[1];
    }

    public void setCommandOrResult(int command) {
        this.buffer[1] = (byte) command;
    }

    public int getPort() {
        return Memory.peekShort(this.buffer, 2, ByteOrder.BIG_ENDIAN);
    }

    public void setPort(int port) {
        Memory.pokeShort(this.buffer, 2, (short) port, ByteOrder.BIG_ENDIAN);
    }

    public int getIP() {
        return Memory.peekInt(this.buffer, 4, ByteOrder.BIG_ENDIAN);
    }

    public void setIP(byte[] ip) {
        this.buffer[4] = ip[0];
        this.buffer[5] = ip[1];
        this.buffer[6] = ip[2];
        this.buffer[7] = ip[3];
    }

    public String getUserId() {
        return getString(8, 248);
    }

    public void setUserId(String id) {
        setString(8, 248, id);
    }

    public String toString() {
        StringBuilder buf = new StringBuilder(50);
        buf.append("Version: ");
        buf.append(Integer.toHexString(getVersionNumber()));
        buf.append(" Command: ");
        buf.append(Integer.toHexString(getCommandOrResult()));
        buf.append(" Port: ");
        buf.append(getPort());
        buf.append(" IP: ");
        buf.append(Integer.toHexString(getIP()));
        buf.append(" User ID: ");
        buf.append(getUserId());
        return buf.toString();
    }

    public int getLength() {
        int index = 8;
        while (this.buffer[index] != 0) {
            index++;
        }
        return index + 1;
    }

    public String getErrorString(int error) {
        switch (error) {
            case 91:
                return "Failure to connect to SOCKS server";
            case 92:
                return "Unable to connect to identd to verify user";
            case 93:
                return "Failure - user ids do not match";
            default:
                return "Success";
        }
    }

    public byte[] getBytes() {
        return this.buffer;
    }

    private String getString(int offset, int maxLength) {
        int index = offset;
        int lastIndex = index + maxLength;
        while (index < lastIndex && this.buffer[index] != 0) {
            index++;
        }
        return new String(this.buffer, offset, index - offset, StandardCharsets.ISO_8859_1);
    }

    private int getVersionNumber() {
        return this.buffer[0];
    }

    private void setString(int offset, int maxLength, String theString) {
        byte[] stringBytes = theString.getBytes(StandardCharsets.ISO_8859_1);
        int length = Math.min(stringBytes.length, maxLength);
        System.arraycopy(stringBytes, 0, this.buffer, offset, length);
        this.buffer[offset + length] = 0;
    }

    private void setVersionNumber(int number) {
        this.buffer[0] = (byte) number;
    }
}
