package jp.co.benesse.dcha.databox.db;

import android.net.Uri;
import java.util.Locale;

public enum ContractKvs {
    KVS;

    final String pathName = name().toLowerCase(Locale.JAPAN);
    final int codeForMany = (ordinal() + 1) * 10;
    public final Uri contentUri = Uri.parse("content://" + KvsProvider.AUTHORITY + "/" + this.pathName);
    final String metaTypeForMany = "vnd.android.cursor.dir/vnd." + KvsProvider.AUTHORITY + "." + this.pathName;

    public static ContractKvs[] valuesCustom() {
        ContractKvs[] contractKvsArrValuesCustom = values();
        int length = contractKvsArrValuesCustom.length;
        ContractKvs[] contractKvsArr = new ContractKvs[length];
        System.arraycopy(contractKvsArrValuesCustom, 0, contractKvsArr, 0, length);
        return contractKvsArr;
    }

    ContractKvs() {
    }
}
