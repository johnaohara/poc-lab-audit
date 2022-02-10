package redhat.mw.perf.audit;

import org.eclipse.jgit.api.Git;
import org.jboss.logging.Logger;
import redhat.mw.perf.jenkins.JenkinsClient;
import redhat.mw.perf.jenkins.JobDef;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class JenkinsJobAuditor implements AuditItem {


    private static final Logger logger = Logger.getLogger(JenkinsJobAuditor.class);

    private final JenkinsClient jenkinsClient;
    private final List<JobDef> jobs;

    public JenkinsJobAuditor() {
        this.jenkinsClient = new JenkinsClient();
        List<JobDef> jobs = null;
        try {
            jobs = this.jenkinsClient.getAllJobs(null);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        this.jobs = jobs;
    }

    @Override
    public void audit(Git git, String branch, Path repoPath, Path outputPath, Consumer<String> failureConsumer) {

    }

    @Override
    public String getRuleName() {
        return this.getClass().getName();
    }
}
