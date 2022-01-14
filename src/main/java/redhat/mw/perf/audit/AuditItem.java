package redhat.mw.perf.audit;

import org.eclipse.jgit.api.Git;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface AuditItem {

        void audit(Git git,  String branch, Path repoPath, Path outputPath, Consumer<String> failureConsumer);

    String getRuleName();
}
