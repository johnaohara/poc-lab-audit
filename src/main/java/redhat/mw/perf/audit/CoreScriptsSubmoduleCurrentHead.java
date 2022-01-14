package redhat.mw.perf.audit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.submodule.SubmoduleStatus;
import org.jboss.logging.Logger;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

public class CoreScriptsSubmoduleCurrentHead implements AuditItem {

    private static final Logger logger = Logger.getLogger(CoreScriptsSubmoduleCurrentHead.class);

    @Override
    public void audit(Git git, String branch, Path repoPath, Path outputPath, Consumer<String> consumer) {

        logger.infov("Auditing core scripts submodule : {0}", branch);

        try {
            final Map<String, SubmoduleStatus> submodules = git.submoduleStatus().call();

            if (submodules.size() > 0) {
                submodules.forEach((name, status) -> {
                    if (!status.getIndexId().equals(status.getHeadId())) {
                        consumer.accept(String.format("Branch: %s; Submodule: %s is not currently head, status: %s", branch, name, status.getIndexId()));
                    }
                });
            }
        } catch (GitAPIException exception) {
            exception.printStackTrace();
        }

    }

    @Override
    public String getRuleName() {
        return CoreScriptsSubmoduleCurrentHead.class.getName();
    }

}
