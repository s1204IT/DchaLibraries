package android.accounts;

public class NetworkErrorException extends AccountsException {
    public NetworkErrorException() {
    }

    public NetworkErrorException(String str) {
        super(str);
    }

    public NetworkErrorException(String str, Throwable th) {
        super(str, th);
    }

    public NetworkErrorException(Throwable th) {
        super(th);
    }
}
