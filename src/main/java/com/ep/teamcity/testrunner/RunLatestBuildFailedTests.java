package com.ep.teamcity.testrunner;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import java.util.Set;

@RunWith(AllTests.class)
public final class RunLatestBuildFailedTests
{
    private static final Logger logger = Logger.getLogger(RunLatestBuildFailedTests.class);
    private static TeamCityService service = new TeamCityService();

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite();
        Set<Class> classes = null;
        try
        {
            classes = service.getLatestBuildResults().getFailedTestClasses();
        }
        catch (Exception e)
        {
            logger.error("Error reading failed test classes. Reason: " + e.getMessage());
        }
        if (classes != null)
        {
            service.printLatestBuildResults();
            for (Class failedTestClass : classes)
            {
                if (failedTestClass != null)
                {
                    suite.addTest(new JUnit4TestAdapter(failedTestClass));
                }

            }
        }

        return suite;
    }
}