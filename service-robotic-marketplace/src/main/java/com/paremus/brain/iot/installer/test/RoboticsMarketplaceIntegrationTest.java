/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.paremus.brain.iot.installer.test;

import static org.junit.Assert.assertEquals;
import static org.osgi.framework.Constants.FRAMEWORK_UUID;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

import com.paremus.brain.iot.management.api.BehaviourManagement;
import com.paremus.brain.iot.management.api.ManagementResponseDTO;

import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;
import eu.brain.iot.eventing.api.SmartBehaviour;
import eu.brain.iot.installer.api.BehaviourDTO;


public class RoboticsMarketplaceIntegrationTest implements SmartBehaviour<ManagementResponseDTO> {
	
    private final BlockingQueue<ManagementResponseDTO> queue = new LinkedBlockingQueue<>();
    private BundleContext context = FrameworkUtil.getBundle(RoboticsMarketplaceIntegrationTest.class).getBundleContext();
	
	private ServiceTracker<BehaviourManagement, BehaviourManagement> bmsTracker;
	private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> cmTracker;

	private BehaviourManagement bms;
	private ConfigurationAdmin cm;
	private ServiceRegistration<?> listener;
	private String frameworkId;
	
	@SuppressWarnings("serial")
	@Before
    public void setUp() throws Exception {
		frameworkId = (String) context.getProperty(FRAMEWORK_UUID);
    	
    	bmsTracker = new ServiceTracker<>(context, BehaviourManagement.class, null);
    	bmsTracker.open();
    	cmTracker = new ServiceTracker<>(context, ConfigurationAdmin.class, null);
    	cmTracker.open();
    	
        cm = cmTracker.waitForService(5000);

        configureBMS();
        
        bms = bmsTracker.waitForService(5000);
        
        listener = context.registerService(SmartBehaviour.class.getName(), this, new Hashtable<String, Object>(){
        	{
        		put(SmartBehaviourDefinition.PREFIX_ + "consumed", ManagementResponseDTO.class.getName());
        	}
        });
        
    }
    
    @After
    public void tearDown() throws Exception {
		bms.resetNode(frameworkId);
    	
    	
    	listener.unregister();
    	bmsTracker.close();
    	cmTracker.close();
    }

	private void configureBMS() throws IOException, InterruptedException {

		File base = new File("").getAbsoluteFile();
		while(base != null && !new File(base, "pom.xml").exists()) {
			base = base.getParentFile();
		}
		
		File marketplace = new File(base, "target/marketplace/index.xml");
		Dictionary<String, Object> props = new Hashtable<>();
        props.put("indexes", marketplace.getAbsoluteFile().toURI().toString());
        

        Configuration config = cm.getConfiguration("eu.brain.iot.BehaviourManagementService", "?");
        config.update(props);
        
        Thread.sleep(1000);
	}

	@Override
    public void notify(ManagementResponseDTO response) {
        System.err.printf("TEST response code=%s \n", response.code);
        queue.add(response);
    }

    @Test
    public void testMarketplaceDoor() throws Exception {
    	
    	Collection<BehaviourDTO> findBehaviours = bms.findBehaviours("(name=Smart Door)");
    	
    	assertEquals(1, findBehaviours.size());
    	
    	bms.installBehaviour(findBehaviours.iterator().next(), frameworkId);
    	
    	ManagementResponseDTO response;
    	
    	response = queue.poll(10, TimeUnit.SECONDS);
    	assertEquals(ManagementResponseDTO.ResponseCode.INSTALL_OK, response.code);
    }

    @Test
    public void testMarketplaceOrchestrator() throws Exception {
    	
    	Collection<BehaviourDTO> findBehaviours = bms.findBehaviours("(name=Smart Orchestrator)");
    	
    	assertEquals(1, findBehaviours.size());
    	
    	bms.installBehaviour(findBehaviours.iterator().next(), frameworkId);
    	
    	ManagementResponseDTO response;
    	
    	response = queue.poll(10, TimeUnit.SECONDS);
    	assertEquals(ManagementResponseDTO.ResponseCode.INSTALL_OK, response.code);
    }

    @Test
    public void testMarketplaceRobot() throws Exception {
    	
    	Collection<BehaviourDTO> findBehaviours = bms.findBehaviours("(name=Smart Robot)");
    	
    	assertEquals(1, findBehaviours.size());
    	
    	bms.installBehaviour(findBehaviours.iterator().next(), frameworkId);
    	
    	ManagementResponseDTO response;
    	
    	response = queue.poll(10, TimeUnit.SECONDS);
    	assertEquals(ManagementResponseDTO.ResponseCode.INSTALL_OK, response.code);
    }

   
    
}