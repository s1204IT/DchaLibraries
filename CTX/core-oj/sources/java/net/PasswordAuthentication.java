package java.net;

public final class PasswordAuthentication {
    private char[] password;
    private String userName;

    public PasswordAuthentication(String str, char[] cArr) {
        this.userName = str;
        this.password = (char[]) cArr.clone();
    }

    public String getUserName() {
        return this.userName;
    }

    public char[] getPassword() {
        return this.password;
    }
}
