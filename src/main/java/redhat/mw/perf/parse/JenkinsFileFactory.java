package redhat.mw.perf.parse;

import io.hyperfoil.tools.parse.Exp;
import io.hyperfoil.tools.parse.Parser;
import io.hyperfoil.tools.parse.factory.ParseFactory;
import org.jboss.logging.Logger;

public class JenkinsFileFactory implements ParseFactory {

    private static final Logger logger = Logger.getLogger(JenkinsFileFactory.class);

    public Exp isPipeline(){
        return  new Exp("isPipeline", "pipeline\\s*{");
    }

    public Exp hasLock(){
        return  new Exp("lock", "lock\\(label:\\s*'(?<name>\\D+)'\\)\\s+\\{");
    }


    @Override
    public void addToParser(Parser p) {
        p.add(isPipeline());
        p.add(hasLock());
    }

    @Override
    public Parser newParser() {
        logger.info("New JenkinsFileFactory Parser");
        Parser p = new Parser();
        addToParser(p);
        return p;
    }

}
