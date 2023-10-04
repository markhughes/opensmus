/**
 * 
 */
package net.sf.opensmus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.junit.Test;

import junit.framework.Assert;

/**
 * @author jmayrbaeurl
 *
 */
public class TestCaseMUSServerProperties {

	@Test
	public void testCreationFromDefaultLocation() throws IOException {
		
		File propsFile = new File("./OpenSMUS.cfg");
		propsFile.delete();
		Assert.assertTrue(propsFile.createNewFile());

		try {
			Properties props = new Properties();
			props.put("ServerOwnerName", "JUnit test case");
			
			OutputStream os = new FileOutputStream(propsFile);
			props.store(os, "JUnit test case of OpenSMUS");
			os.close();
			
			MUSServerProperties musprops = new MUSServerProperties();
			Assert.assertNotNull(musprops);
			Assert.assertNotNull(musprops);
			Assert.assertTrue("JUnit test case".equals(musprops.getProperty("ServerOwnerName")));
		}
		finally {
			propsFile.delete();
		}	
	}
	
	@Test
	public void testCreationFromSysPropsLocation() throws FileNotFoundException, IOException {
		
		File propsFile = new File("./TestOpenSMUS.cfg");
		propsFile.delete();
		Assert.assertTrue(propsFile.createNewFile());
		
		try {
			Properties props = new Properties();
			props.put("ServerOwnerName", "JUnit test case");
			
			OutputStream os = new FileOutputStream(propsFile);
			props.store(os, "JUnit test case of OpenSMUS");
			os.close();
			
			System.getProperties().setProperty("OpenSMUSConfigFile", "./TestOpenSMUS.cfg");
			MUSServerProperties musprops = new MUSServerProperties();
			Assert.assertNotNull(musprops);
			Assert.assertTrue("JUnit test case".equals(musprops.getProperty("ServerOwnerName")));
		}
		finally {
			propsFile.delete();
		}
	}
}
