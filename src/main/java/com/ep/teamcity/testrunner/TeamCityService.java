package com.ep.teamcity.testrunner;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.apache.log4j.Logger;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import us.monoid.web.Resty;
import us.monoid.web.XMLResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class TeamCityService
{
    private static final Logger logger = Logger.getLogger(TeamCityService.class);
    private static String url;
    private static String port;
    private static String login;
    private static String pass;
    private static String buildType;
    private static String maxTestsAmount;
    private static String localFwdPort;
    private static String remoteFwdPort;

    public TeamCityService()
    {
        Properties properties = new Properties();
        try
        {
            properties.load(new FileInputStream("src/test/resources/teamcity.properties"));
        }
        catch (IOException e)
        {
            logger.error("Can't load 'teamcity.properties'");
        }
        url = System.getProperty("server", properties.getProperty("server"));
        port = System.getProperty("port", properties.getProperty("port"));
        login = System.getProperty("login", properties.getProperty("login"));
        pass = System.getProperty("password", properties.getProperty("password"));
        buildType = System.getProperty("build.type", properties.getProperty("build.type"));
        maxTestsAmount = System.getProperty("max.texts.amount", properties.getProperty("max.texts.amount"));
        localFwdPort = System.getProperty("remote.forward.port", properties.getProperty("remote.forward.port"));
        remoteFwdPort = System.getProperty("local.forward.port", properties.getProperty("local.forward.port"));
    }

    public TeamCityService openSSHTunnel() throws Exception
    {
        JSch jsch = new JSch();
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        Session session = jsch.getSession(login, url);
        session.setPassword(pass);
        session.setPortForwardingL(Integer.parseInt(localFwdPort), url, Integer.parseInt(remoteFwdPort));

        session.setConfig(config);
        try
        {
            session.connect((int) TimeUnit.SECONDS.toMillis(10));
        }
        catch (Exception e)
        {
            logger.error("Cannot obtain SSH connection for " + url + " Reason: " + e.getMessage());
        }
        return this;
    }

    public Result getLatestBuildResults() throws Exception {
        openSSHTunnel();

        Result result = new Result();
        try
        {
            Resty resty = new Resty();
            resty.authenticate(url, login, pass.toCharArray());
            XMLResource resource = resty.xml(url + "/app/rest/builds/buildType:" + buildType + ",lookupLimit:1");
            Boolean hasNext = true;
            String testOccurrences = url + resource.get("//testOccurrences/@href").item(0).getNodeValue() + ",count:" + maxTestsAmount;
            while (hasNext)
            {
                XMLResource results = resty.xml(testOccurrences);
                NodeList nodeList = results.get("//testOccurrences/@nextHref");
                hasNext = (nodeList.getLength() > 0 && (testOccurrences = url + nodeList.item(0).getNodeValue()) != null);
                nodeList = results.get("//testOccurrence");
                if (nodeList != null)
                {
                    for (int i = 0; i < nodeList.getLength(); i++)
                    {
                        NamedNodeMap attributes = nodeList.item(i).getAttributes();
                        result.addResult(attributes.getNamedItem("name").getNodeValue(), attributes.getNamedItem("status").getNodeValue());
                    }
                }
            }
        }
        catch (Exception e)
        {
            logger.error("Cannot obtain latest build results. Reason: " + e.getMessage());
        }
        return result;
    }

    public void printLatestBuildResults()
    {
        Set<Class> classes = null;
        List<String> classNames = new ArrayList<String>();
        try
        {
            classes = this.getLatestBuildResults().getFailedTestClasses();
        }
        catch (Exception e)
        {
            logger.error("Error reading failed test classes. Reason: " + e.getMessage());
        }
        if (classes != null)
        {
            for (Class failedTestClass : classes) {
                classNames.add(failedTestClass.getName() + ".class,");
            }
        }
        Collections.sort(classNames);
        logger.info("-------------------------------------------------");
        logger.info("Latest build failed tests:");

        for (String name : classNames)
        {
            logger.info(name);
        }
    }
}