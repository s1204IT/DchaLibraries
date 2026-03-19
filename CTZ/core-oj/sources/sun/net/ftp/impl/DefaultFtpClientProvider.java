package sun.net.ftp.impl;

import sun.net.ftp.FtpClientProvider;

public class DefaultFtpClientProvider extends FtpClientProvider {
    @Override
    public sun.net.ftp.FtpClient createFtpClient() {
        return FtpClient.create();
    }
}
