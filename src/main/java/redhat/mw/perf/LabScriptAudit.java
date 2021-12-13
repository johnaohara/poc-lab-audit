package redhat.mw.perf;

///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.aesh:aesh:2.6
//DEPS org.jboss.logging:jboss-logging:3.4.2.Final
//DEPS org.eclipse.jgit:org.eclipse.jgit:6.0.0.202111291000-r
//DEPS com.fasterxml.jackson.core:jackson-databind:2.13.0
//DEPS io.hyperfoil.tools:parse:0.1.7

import io.hyperfoil.tools.parse.ParseCommand;
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class LabScriptAudit {

    public static void main(String... args) {
        AeshRuntimeRunner.builder().command(Audit.class).args(args).execute();
    }

    @CommandDefinition(name = "audit", description = "Run Audit on lab scripts")
    public static class Audit implements Command {

        private static final Logger logger = Logger.getLogger(Audit.class);

        private static final String RULES_FILE = "labRules.yaml";

        private static Git git;
        private static UsernamePasswordCredentialsProvider uProvider;
        private static List<Ref> remoteBranches;
        private static Path outputDir;

        static {
            try {
                outputDir = Files.createTempDirectory("audit-output-");
                logger.infov("Logging to: {0}", outputDir.toFile().getAbsolutePath());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.exit(1); // TODO:: handle correctly
            }
        }

        @Option(name = "dir", shortName = 'd', description = "Local git repo directory", required = true)
        private String dir;

        @Option(name = "repo", shortName = 'r', description = "Scripts git repo URL", required = true)
        private String repoUrl;

        @Option(name = "user", shortName = 'u', description = "Scripts git repo username", required = true)
        private String user;

        @Option(name = "pass", shortName = 'p', description = "Scripts git repo password", required = true)
        private String password;

        // @Option(name = "config", shortName = 'c', description = "Configuration file
        // path", required = true)
        // private String configFilePath;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws InterruptedException {
            initializeGit();

            fetchRemoteBranches();

            auditBranches();

            return CommandResult.SUCCESS;
        }

        private void auditBranches() {

            remoteBranches.forEach(remoteBranch -> {
                System.out.println(remoteBranch.getName());
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

            URL rulesUrl = LabScriptAudit.class.getClassLoader().getResource(RULES_FILE);

            File qDupFile = new File(dir.concat("/qdup.yaml"));
            logger.infov("qDup exists: {0}", qDupFile.exists());
            String[] nameComponents = name.split("/");

            String[] args = { "-s", dir, "-d",
                    outputDir.resolve(nameComponents[nameComponents.length - 1].concat(".out")).toFile().getAbsolutePath(), 
                    "-r", rulesUrl.getPath()
                    , "--disableDefault"
            };

            ParseCommand.main(args);

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

        public static Path createTempPath(String prefix) {
            try {
                return Files.createTempDirectory(prefix);
            } catch (NullPointerException | IllegalArgumentException | UnsupportedOperationException | IOException
                    | SecurityException exception) {
                logger.error("Could not create temporary directory");
                System.exit(1);
            }
            return null;
        }

    }

}
