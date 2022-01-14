package redhat.mw.perf;

import org.aesh.AeshRuntimeRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jboss.logging.Logger;
import redhat.mw.perf.audit.AuditItem;
import redhat.mw.perf.output.JsonOutputPrinter;
import redhat.mw.perf.output.OutputPrinter;
import redhat.mw.perf.output.TxtOutputPrinter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class LabScriptAudit {

    public static void main(String... args) {
        AeshRuntimeRunner.builder().command(Audit.class).args(args).execute();
    }

    @CommandDefinition(name = "audit", description = "Run Audit on lab script repository")
    public static class Audit implements Command {

        private static final Logger logger = Logger.getLogger(Audit.class);

        private static Git git;
        private static UsernamePasswordCredentialsProvider uProvider;
        private static List<Ref> remoteBranches;
        private static Path repoDir;
        private static Path outputDir;
        private static OutputPrinter outputPrinter;

        private static final List<AuditItem> auditItems = new ArrayList<>();
        private static final Map<String, List<String>> failureMsgs = new ConcurrentHashMap<>();

        private static final ExecutorService executorService = Executors.newWorkStealingPool();

        static {
            try {
                outputDir = Files.createTempDirectory("audit-output-");
                logger.infov("Logging to: {0}", outputDir.toFile().getAbsolutePath());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.exit(1); // TODO:: handle correctly
            }

            auditItems.addAll(Util.loadAuditItems());

        }

        @Option(name = "dir", shortName = 'd', description = "Git repo local directory", required = true)
        private String dir;

        @Option(name = "repo", shortName = 'r', description = "Git repo remote URL", required = true)
        private String repoUrl;

        @Option(name = "user", shortName = 'u', description = "Remote git repo username", required = true)
        private String user;

        @Option(name = "pass", shortName = 'p', description = "Remote git repo password", required = true)
        private String password;

        @Option(name = "format", shortName = 'f', description = "Output format: [txt, json]. Default: txt", defaultValue = "txt")
        private String outputType;

        // @Option(name = "config", shortName = 'c', description = "Configuration file
        // path", required = true)
        // private String configFilePath;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws InterruptedException {

            long startTime = System.currentTimeMillis();

            repoDir = Path.of(dir);

            initializeGit();

            initializeOutputPrinter();

            fetchRemoteBranches();

            auditBranches();

            printAuditfailures();

            long duration = System.currentTimeMillis() - startTime;

            logger.infov("Audit took: {0} (s)", (float)duration/1_000);
            logger.infov("Audit File: {0}", outputPrinter.getOutputFile()) ;

            return CommandResult.SUCCESS;
        }

        private void initializeOutputPrinter() {
            switch (outputType.toLowerCase()) {
                case "txt":
                    outputPrinter = new TxtOutputPrinter(outputDir.resolve("output.txt"));
                    break;
                case "json":
                    outputPrinter = new JsonOutputPrinter(outputDir.resolve("output.json"));
                    break;
                default:
                    throw new RuntimeException("Unknown output type: " + outputType);
            }
        }

        private void printAuditfailures() {
            outputPrinter.printAuditfailures(failureMsgs);
        }

        private void auditBranches() {

            remoteBranches.forEach(remoteBranch -> {
                try {
                    git.checkout().setName(remoteBranch.getName()).call();
                    auditBranch(remoteBranch.getName());
                } catch (GitAPIException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            });
        }

        private void auditBranch(String name) {

            CountDownLatch countDownLatch = new CountDownLatch(auditItems.size());

            auditItems.forEach(auditItem -> executorService.submit(() -> {
                auditItem.audit(git, name, repoDir, outputDir, msg -> {
                            if (!failureMsgs.containsKey(auditItem.getRuleName())) {
                                failureMsgs.put(auditItem.getRuleName(), new ArrayList<>());
                            }
                            failureMsgs.get(auditItem.getRuleName()).add(msg);
                        }
                );
                countDownLatch.countDown();
            }));

            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        private void fetchRemoteBranches() {

            try {
                logger.infov("Fetching list of branches");
                List<RemoteConfig> remotes = git.remoteList().call();
                for (RemoteConfig remote : remotes) {
                    logger.infov("Fetching branch: {0}", remote.getName());
                    remoteBranches = git.branchList().setListMode(ListMode.ALL).call();
                    logger.infov("Found: {0} remote branches", remoteBranches.size());
                }

            } catch (GitAPIException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        private void initializeGit() {

            uProvider = new UsernamePasswordCredentialsProvider(user, password);

            Path repoPath = Path.of(dir);
            File gitRepoPath = repoPath.toFile();

            if (!gitRepoPath.exists()) {
                try {
                    Files.createDirectories(repoPath);
                } catch (IOException e) {
                    throw new RuntimeException(
                            String.format("Could not create directory: {0}", gitRepoPath.getAbsolutePath()));
                }
            }
            if (!gitRepoPath.isDirectory()) {
                throw new RuntimeException(
                        String.format("Git repo directory is not a directory: {0}", gitRepoPath.getAbsolutePath()));
            }

            logger.infov("Opening repo at: {0}", gitRepoPath.getAbsolutePath());

            try {
                logger.infov("Trying to open git repo: {0}", gitRepoPath.getAbsolutePath());
                File gitFile = repoPath.resolve(".git").toFile();
                if (gitFile.exists()) {
                    git = Git.open(repoPath.resolve(".git").toFile());
                } else {
                    logger.infov("Cloning repo from: {0}", repoUrl);
                    checkoutGitrepo(gitRepoPath);
                }

            } catch (IOException e) {

            }

        }

        private void checkoutGitrepo(File gitRepoPath) {

            try {
                git = Git.cloneRepository()
                        .setURI(repoUrl)
                        .setCredentialsProvider(uProvider)
                        .setDirectory(gitRepoPath)
                        .call();
            } catch (GitAPIException e) {
                throw new RuntimeException("Failed to clone repository", e);
            }
        }

    }

}
