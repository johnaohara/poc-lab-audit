package redhat.mw.perf.jenkins;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.FolderJob;
import com.offbytwo.jenkins.model.Job;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JenkinsClient {
    private static final Logger logger = Logger.getLogger(JenkinsClient.class);

    private static JenkinsServer jenkins;

    private static final boolean recursive = false;

    private static final String jenkinsUrl = "http://mwperf-server01.perf.lab.eng.rdu2.redhat.com:8080";
    private static final String jenkinsUser;
    private static final String jenkinsPass;

    static {
        jenkinsUser = System.getenv("jenkins_user");
        jenkinsPass = System.getenv("jenkins_password");
        try {
            jenkins = new JenkinsServer(new URI(jenkinsUrl), jenkinsUser, jenkinsPass);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public List<JobDef> getAllJobs(FolderJob parentFolder) throws IOException {
        return getAllJobs(parentFolder, jenkins.getJobs());
    }

    public List<JobDef> getAllJobs(FolderJob parentFolder, Map<String, Job> jobs) throws IOException {

        List<JobDef> listJobs = new ArrayList<>();

        for (Map.Entry<String, Job> jobEntry : jobs.entrySet()) {
            logger.infov("Downloading job definition: {0}", jobEntry.getKey());
            switch (jobEntry.getValue().get_class()) {
                case "com.cloudbees.hudson.plugins.folder.Folder":
                    if (recursive) {
                        FolderJob folderJob = jenkins.getFolderJob(jobEntry.getValue()).get();
                        listJobs.addAll(getAllJobs(folderJob, jenkins.getJobs(folderJob)));
                    }
                    break;
                case "org.jenkinsci.plugins.workflow.job.WorkflowJob":
                    JobWithDetails jobDetails = parentFolder == null ? jenkins.getJob(jobEntry.getValue().getName()) : jenkins.getJob(parentFolder, jobEntry.getValue().getName());

                    String jobXml = null;
                    BuildWithDetails lastBuild = null;
                    try {
                        jobXml = parentFolder == null ? jenkins.getJobXml(jobDetails.getName()) : jenkins.getJobXml(parentFolder, jobDetails.getName());
                        lastBuild = jobDetails.getLastBuild().details();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    listJobs.add(new JobDef(jobDetails, jobXml, lastBuild));
                    break;
                default:
                    logger.warnv("Unknown job type: {0}; for job {1} ", jobEntry.getValue().getClass().getName(), jobEntry.getKey());
            }
        }

        return listJobs;
    }


}
