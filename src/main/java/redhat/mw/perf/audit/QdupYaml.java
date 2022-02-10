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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class QdupYaml implements AuditItem {

    private static final Logger logger = Logger.getLogger(QdupYaml.class);

    @Override
    public void audit(Git git, String branch, Path repoPath, Path outputPath, Consumer<String> consumer) {
        File qDupFile = repoPath.resolve("qdup.yaml").toFile();
        File coreScripts = repoPath.resolve("core-scripts").toFile();
        if (qDupFile.exists()) {
            logger.infov("Auditing qdup for branch: {0}", branch);
            ArrayList<String> args = new ArrayList<>(Arrays.asList("--test", qDupFile.getAbsolutePath()));
            if (coreScripts.exists()) {
                args.add(coreScripts.getAbsolutePath());
            }

            //Setup env for running qDup in
            SecurityManager securityManager = System.getSecurityManager();
            System.setSecurityManager(new TempSecurityManager());
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;


            QDup qDup = null;
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final String utf8 = StandardCharsets.UTF_8.name();

            try (PrintStream tempPrintStream = new PrintStream(baos, true, utf8)) {
                System.setOut(tempPrintStream);
                System.setErr(tempPrintStream);
                String[] progArgs = new String[args.size()];
                args.toArray(progArgs);
                qDup = new QDup(progArgs);
                qDup.run();
            } catch (SecurityException e) {
                if( !e.getMessage().equals("0") ) {
                    String msg = qDup.getRunDebug();
                    if (msg != null) {
                        logger.warnv("qDup failed to start correctly, for branch {0} please check audit report for details", branch);
                        consumer.accept(String.format("Branch: %s; QDup Parser validation failed: \n %s \n\n", branch, msg));
                    } else {
                        try {
                            consumer.accept(String.format("Branch: %s; QDup failed to start correctly caused by: \n %s ", branch, baos.toString(utf8)));
                        } catch (UnsupportedEncodingException ex) {
                            //ignore
                        }
                    }
                }
            } catch (Throwable e) {
                consumer.accept(String.format("Branch: %s; QDup Parser threw following error: %s ", branch, e.getMessage()));
            } finally {
                //restore SecurityManager
                System.setSecurityManager(securityManager);
                System.setOut(originalOut);
                System.setErr(originalErr);

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
            throw new SecurityException(String.valueOf(status)); //do not exit JVM if System.exit() is called
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow other activities by default
        }
    }
}
