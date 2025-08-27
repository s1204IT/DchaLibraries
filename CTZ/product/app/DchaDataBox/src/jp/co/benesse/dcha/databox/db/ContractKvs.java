package jp.co.benesse.dcha.databox.db;

import android.net.Uri;
import java.util.Locale;

/* loaded from: classes.dex */
public enum ContractKvs {
    KVS;

    final String pathName = name().toLowerCase(Locale.JAPAN);
    final int codeForMany = (ordinal() + 1) * 10;
    public final Uri contentUri = Uri.parse("content://" + KvsProvider.AUTHORITY + "/" + this.pathName);
    final String metaTypeForMany = "vnd.android.cursor.dir/vnd." + KvsProvider.AUTHORITY + "." + this.pathName;

    ContractKvs() {
    }
}
