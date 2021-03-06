//$Id:  $
package gsearch.test.solr.fgs24_1010;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
/**
 * Test of fgs24 config
 * 
 * assuming 
 * - all Fedora demo objects are in the repository referenced in
 *   configTestOnSolrFgs24_1010/repository/FgsRepos/repository.properties
 * 
 * the test suite will
 * - set configTestOnSolrFgs24_1010 as current config,
 * - run tests concerning GSearch 2.4. 
 */

@RunWith(Suite.class)
@Suite.SuiteClasses( {
	gsearch.test.solr.fgs24_1010.TestConfigOnSolrFgs24_1010.class
	} )
public class AllTests {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {
        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllTests.class.getName());
        suite.addTest(gsearch.test.solr.fgs24_1010.TestConfigOnSolrFgs24_1010.suite());
        return suite;
    }
}
