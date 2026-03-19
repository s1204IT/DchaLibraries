package android.ext.services.autofill;

import android.os.Bundle;
import android.service.autofill.AutofillFieldClassificationService;
import android.util.Log;
import android.view.autofill.AutofillValue;
import com.android.internal.util.ArrayUtils;
import java.util.List;

public class AutofillFieldClassificationServiceImpl extends AutofillFieldClassificationService {
    public float[][] onGetScores(String str, Bundle bundle, List<AutofillValue> list, List<String> list2) {
        if (ArrayUtils.isEmpty(list) || ArrayUtils.isEmpty(list2)) {
            Log.w("AutofillFieldClassificationServiceImpl", "getScores(): empty currentvalues (" + list + ") or userValues (" + list2 + ")");
            return null;
        }
        if (str != null && !str.equals("EDIT_DISTANCE")) {
            Log.w("AutofillFieldClassificationServiceImpl", "Ignoring invalid algorithm (" + str + ") and using EDIT_DISTANCE instead");
        }
        return EditDistanceScorer.getScores(list, list2);
    }
}
