package net.sf.opensmus;

import java.io.File;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Test;

public class TestCaseMUSServer {

	@Test
	public void testMUSServerCreation() throws InterruptedException {
		
		MUSServerProperties props = new MUSServerProperties();
		props.m_props.setProperty("LogDebugExtInformation", String.valueOf(1));
		
		MUSServer serverInstance = new MUSServer(props);
		Assert.assertNotNull(serverInstance);
		
		//Thread.sleep(10000);
		
		serverInstance.killServer();
	}
	
	@After
	public void cleanup() {
		
		File serverLogFile = new File(MUSServerProperties.DEFAULT_LOGFILENAME);
		if (serverLogFile.exists())
			serverLogFile.delete();
	}
}
