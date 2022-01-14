package redhat.mw.perf.output;

import org.jboss.logging.Logger;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class TxtOutputPrinter extends AbstractOutputPrinter {

    private static final Logger logger = Logger.getLogger(TxtOutputPrinter.class);

    public TxtOutputPrinter(Path outputPath) {
        super(outputPath);
    }

    @Override
    public void printAuditfailures(Map<String, List<String>> failureMsgs) {

        try(PrintWriter printWriter = new PrintWriter(this.outputPath.toFile())) {

            if (failureMsgs.size() > 0) {
                failureMsgs.forEach((rule, msgs) -> {
                    printWriter.println("Audit Rule failed: " + rule);
                    printWriter.print("\n\n");
                    msgs.forEach(msg -> printWriter.println(msg));
                    printWriter.print("\n\n");
                });
            }
        } catch (FileNotFoundException e) {
            logger.fatalv("Could not write to file: {0]", this.outputPath.toString());
        }

    }

}
