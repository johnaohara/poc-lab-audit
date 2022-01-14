package redhat.mw.perf.audit;

import io.hyperfoil.tools.qdup.QDup;
import org.eclipse.jgit.api.Git;
import org.jboss.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.Permission;
import java.util.function.Consumer;

public class QdupYaml implements AuditItem {

    private static final Logger logger = Logger.getLogger(QdupYaml.class);

    @Override
    public void audit(Git git, String branch, Path repoPath, Path outputPath, Consumer<String> consumer) {
        File qDupFile = repoPath.resolve("qdup.yaml").toFile();
        File coreScripts = repoPath.resolve("core-scripts").toFile();
        if (qDupFile.exists()) {
            logger.infov("Auditing qdup for branch: {0}", branch);
            String[] args = {"--test", qDupFile.getAbsolutePath(), coreScripts.getAbsolutePath()};

            //Setup env for running qDup in
            SecurityManager securityManager = System.getSecurityManager();
            System.setSecurityManager(new TempSecurityManager());
            PrintStream originalOut = System.out;


            QDup qDup = null;
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final String utf8 = StandardCharsets.UTF_8.name();

            try (PrintStream tempPrintStream = new PrintStream(baos, true, utf8)) {
                System.setOut(tempPrintStream);
                qDup = new QDup(args);
                qDup.run();
            } catch (SecurityException e) {
                String msg = qDup.getRunDebug();
                if (msg != null) {
                    consumer.accept(String.format("Branch: %s; QDup Parser validation failed: \n %s ", branch, qDup.getRunDebug()));
                } else {
                    System.setOut(originalOut);
                    logger.error("qDup failed to start correctly");
                    logger.error("The tail of the terminal output might provide a clue why;");
                    try {
                        consumer.accept(String.format("Branch: %s; QDup failed to start correctly cause by: \n %s ", branch, baos.toString(utf8)));
                    } catch (UnsupportedEncodingException unsupportedEncodingException) {
                        unsupportedEncodingException.printStackTrace();
                    }
                }
            } catch (Throwable e) {
                consumer.accept(String.format("Branch: %s; QDup Parser threw following error: %s ", branch, e.getMessage()));
            } finally {
                //restore SecurityManager
                System.setSecurityManager(securityManager);
                System.setOut(originalOut);

            }


        } else {
            consumer.accept(String.format("Branch: %s; File missing: qdup.yaml", branch));
        }
    }


    @Override
    public String getRuleName() {
        return QdupYaml.class.getName();
    }


    class TempSecurityManager extends SecurityManager {
        @Override
        public void checkExit(int status) {
            throw new SecurityException(); //do not exit JVM if System.exit() is called
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow other activities by default
        }
    }
}
