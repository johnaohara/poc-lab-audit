package redhat.mw.perf.output;

import java.nio.file.Path;

public abstract class AbstractOutputPrinter implements OutputPrinter{
    protected final Path outputPath;

    public AbstractOutputPrinter(Path outputPath){
        this.outputPath = outputPath;
    }

    @Override
    public String getOutputFile() {
        return outputPath.toString();
    }
}
