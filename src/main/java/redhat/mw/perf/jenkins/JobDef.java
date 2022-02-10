package redhat.mw.perf.jenkins;

import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.FolderJob;
import com.offbytwo.jenkins.model.Job;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;

public class JobDef {

    private final String name;
    private final String url;
    private final JobWithDetails job;
    private final String jobXml;
    private final BuildWithDetails lastBuild;
    private final Date lastBuildDate;
    private final String scriptBranch;

    private static final DocumentBuilder builder;
    private static final XPath xPath = XPathFactory.newInstance().newXPath();


    static {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = null;
        try {
            documentBuilder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        builder = documentBuilder;
    }


    public JobDef(JobWithDetails job, String jobXml, BuildWithDetails lastBuild) {
        this.name = job.getName();
        this.url = job.getUrl();
        this.job = job;

        this.jobXml = jobXml;
        this.lastBuild = lastBuild;
        this.lastBuildDate = new Date(lastBuild.getTimestamp());

        Document xmlDocument = null;
        try {
            xmlDocument = builder.parse(new ByteArrayInputStream(jobXml.getBytes()));
            String expression = "/flow-definition/definition";
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
            nodeList.getLength();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }

        scriptBranch = null;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public Job getJob() {
        return job;
    }

    public String getJobXml() {
        return jobXml;
    }

    public BuildWithDetails getLastBuild() {
        return lastBuild;
    }

    public Date getLastBuildDate() {
        return lastBuildDate;
    }

}
