package android.app.servertransaction;

import android.app.ClientTransactionHandler;
import android.content.res.Configuration;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public class ConfigurationChangeItem extends ClientTransactionItem {
    public static final Parcelable.Creator<ConfigurationChangeItem> CREATOR = new Parcelable.Creator<ConfigurationChangeItem>() {
        @Override
        public ConfigurationChangeItem createFromParcel(Parcel parcel) {
            return new ConfigurationChangeItem(parcel);
        }

        @Override
        public ConfigurationChangeItem[] newArray(int i) {
            return new ConfigurationChangeItem[i];
        }
    };
    private Configuration mConfiguration;

    @Override
    public void preExecute(ClientTransactionHandler clientTransactionHandler, IBinder iBinder) {
        clientTransactionHandler.updatePendingConfiguration(this.mConfiguration);
    }

    @Override
    public void execute(ClientTransactionHandler clientTransactionHandler, IBinder iBinder, PendingTransactionActions pendingTransactionActions) {
        clientTransactionHandler.handleConfigurationChanged(this.mConfiguration);
    }

    private ConfigurationChangeItem() {
    }

    public static ConfigurationChangeItem obtain(Configuration configuration) {
        ConfigurationChangeItem configurationChangeItem = (ConfigurationChangeItem) ObjectPool.obtain(ConfigurationChangeItem.class);
        if (configurationChangeItem == null) {
            configurationChangeItem = new ConfigurationChangeItem();
        }
        configurationChangeItem.mConfiguration = configuration;
        return configurationChangeItem;
    }

    @Override
    public void recycle() {
        this.mConfiguration = null;
        ObjectPool.recycle(this);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedObject(this.mConfiguration, i);
    }

    private ConfigurationChangeItem(Parcel parcel) {
        this.mConfiguration = (Configuration) parcel.readTypedObject(Configuration.CREATOR);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return Objects.equals(this.mConfiguration, ((ConfigurationChangeItem) obj).mConfiguration);
    }

    public int hashCode() {
        return this.mConfiguration.hashCode();
    }

    public String toString() {
        return "ConfigurationChangeItem{config=" + this.mConfiguration + "}";
    }
}
