package redhat.mw.perf.output;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class JsonOutputPrinter extends AbstractOutputPrinter {
    public JsonOutputPrinter(Path outputPath) {
        super(outputPath);
    }

    @Override
    public void printAuditfailures(Map<String, List<String>> failureMsgs) {
        throw new RuntimeException("Not yet implemented");
    }
}
