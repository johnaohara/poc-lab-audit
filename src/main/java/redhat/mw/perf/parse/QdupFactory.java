package redhat.mw.perf.parse;

import io.hyperfoil.tools.parse.Parser;
import io.hyperfoil.tools.parse.factory.ParseFactory;
import org.jboss.logging.Logger;

public class QdupFactory implements ParseFactory {

    private static final Logger logger = Logger.getLogger(QdupFactory.class);

    @Override
    public void addToParser(Parser p) {
    }

    @Override
    public Parser newParser() {
        logger.info("New QdupFactory Parser");
        Parser p = new Parser();
        addToParser(p);
        return p;
    }

}
