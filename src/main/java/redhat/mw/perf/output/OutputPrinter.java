package redhat.mw.perf.output;

import java.util.List;
import java.util.Map;

public interface OutputPrinter {
    void printAuditfailures(Map<String, List<String>> failureMsgs);
    String getOutputFile();
}
