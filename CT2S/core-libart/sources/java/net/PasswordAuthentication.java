package java.net;

public final class PasswordAuthentication {
    private char[] password;
    private String userName;

    public PasswordAuthentication(String userName, char[] password) {
        this.userName = userName;
        this.password = (char[]) password.clone();
    }

    public char[] getPassword() {
        return (char[]) this.password.clone();
    }

    public String getUserName() {
        return this.userName;
    }
}
