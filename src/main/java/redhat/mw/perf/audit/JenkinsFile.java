package redhat.mw.perf.audit;

import org.eclipse.jgit.api.Git;
import org.jboss.logging.Logger;

import java.nio.file.Path;
import java.util.function.Consumer;

public class JenkinsFile implements AuditItem{

    private static final Logger logger = Logger.getLogger(JenkinsFile.class);

    @Override
    public void audit(Git git, String branch, Path repoPath, Path outputPath, Consumer<String> consumer) {

        if(repoPath.resolve("Jenkinsfile").toFile().exists()) {
            logger.infov("Auditing Jenkinsfile for branch: {0}", branch);

        } else {
            consumer.accept(String.format("Branch: %s; does not have a Jenkinsfile", branch));
        }

    }


    @Override
    public String getRuleName() {
        return JenkinsFile.class.getName();
    }
}
