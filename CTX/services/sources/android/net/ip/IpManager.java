package android.net.ip;

import android.content.Context;
import android.net.Network;
import android.net.StaticIpConfiguration;
import android.net.apf.ApfCapabilities;
import android.net.ip.IpClient;

public class IpManager extends IpClient {

    public static class Callback extends IpClient.Callback {
    }

    public static class InitialConfiguration extends IpClient.InitialConfiguration {
    }

    public static class ProvisioningConfiguration extends IpClient.ProvisioningConfiguration {
        public ProvisioningConfiguration(IpClient.ProvisioningConfiguration provisioningConfiguration) {
            super(provisioningConfiguration);
        }

        public static class Builder extends IpClient.ProvisioningConfiguration.Builder {
            @Override
            public Builder withoutIPv4() {
                super.withoutIPv4();
                return this;
            }

            @Override
            public Builder withoutIPv6() {
                super.withoutIPv6();
                return this;
            }

            @Override
            public Builder withoutIpReachabilityMonitor() {
                super.withoutIpReachabilityMonitor();
                return this;
            }

            @Override
            public Builder withPreDhcpAction() {
                super.withPreDhcpAction();
                return this;
            }

            @Override
            public Builder withPreDhcpAction(int i) {
                super.withPreDhcpAction(i);
                return this;
            }

            public Builder withInitialConfiguration(InitialConfiguration initialConfiguration) {
                super.withInitialConfiguration((IpClient.InitialConfiguration) initialConfiguration);
                return this;
            }

            @Override
            public Builder withStaticConfiguration(StaticIpConfiguration staticIpConfiguration) {
                super.withStaticConfiguration(staticIpConfiguration);
                return this;
            }

            @Override
            public Builder withApfCapabilities(ApfCapabilities apfCapabilities) {
                super.withApfCapabilities(apfCapabilities);
                return this;
            }

            @Override
            public Builder withProvisioningTimeoutMs(int i) {
                super.withProvisioningTimeoutMs(i);
                return this;
            }

            @Override
            public Builder withNetwork(Network network) {
                super.withNetwork(network);
                return this;
            }

            @Override
            public Builder withDisplayName(String str) {
                super.withDisplayName(str);
                return this;
            }

            @Override
            public ProvisioningConfiguration build() {
                return new ProvisioningConfiguration(super.build());
            }
        }
    }

    public static ProvisioningConfiguration.Builder buildProvisioningConfiguration() {
        return new ProvisioningConfiguration.Builder();
    }

    public IpManager(Context context, String str, Callback callback) {
        super(context, str, callback);
    }

    public void startProvisioning(ProvisioningConfiguration provisioningConfiguration) {
        super.startProvisioning((IpClient.ProvisioningConfiguration) provisioningConfiguration);
    }
}
