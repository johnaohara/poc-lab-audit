package redhat.mw.perf.audit;

import io.hyperfoil.tools.parse.ParseCommand;
import org.eclipse.jgit.api.Git;
import org.jboss.logging.Logger;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.function.Consumer;

public class ParseAuditor  implements AuditItem {

    private static final Logger logger = Logger.getLogger(ParseAuditor.class);

    private static final String RULES_FILE = "labRules.yaml";

    @Override
    public void audit(Git git, String branch, Path repoPath, Path outputPath, Consumer<String> failureConsumer) {
        URL rulesUrl = QdupYaml.class.getClassLoader().getResource(RULES_FILE);

        File qDupFile = repoPath.resolve("/qdup.yaml").toFile();
        logger.infov("qDup exists: {0}", qDupFile.exists());
        String[] nameComponents = branch.split("/");

        String[] args = {"-s", repoPath.toString(), "-d",
                outputPath.resolve(nameComponents[nameComponents.length - 1].concat(".out")).toFile().getAbsolutePath(),
                "-r", rulesUrl.getPath()
                , "--disableDefault"
        };

        ParseCommand.main(args);

    }

    @Override
    public String getRuleName() {
        return this.getClass().getName();
    }
}
