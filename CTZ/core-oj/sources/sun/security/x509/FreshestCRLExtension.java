package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class FreshestCRLExtension extends CRLDistributionPointsExtension {
    public static final String NAME = "FreshestCRL";

    public FreshestCRLExtension(List<DistributionPoint> list) throws IOException {
        super(PKIXExtensions.FreshestCRL_Id, false, list, NAME);
    }

    public FreshestCRLExtension(Boolean bool, Object obj) throws IOException {
        super(PKIXExtensions.FreshestCRL_Id, Boolean.valueOf(bool.booleanValue()), obj, NAME);
    }

    @Override
    public void encode(OutputStream outputStream) throws IOException {
        super.encode(outputStream, PKIXExtensions.FreshestCRL_Id, false);
    }
}
