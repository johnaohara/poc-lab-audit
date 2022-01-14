package redhat.mw.perf;

import redhat.mw.perf.audit.AuditItem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class Util {

    public static List<AuditItem> loadAuditItems(){
        List<AuditItem> items = new ArrayList<>();
        ServiceLoader<AuditItem> loader = ServiceLoader.load(AuditItem.class);
        Iterator<AuditItem> loaderIterator = loader.iterator();
        loaderIterator.forEachRemaining( auditItem -> items.add(auditItem));
        return items;
    }
}
