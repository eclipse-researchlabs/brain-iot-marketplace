import aQute.bnd.osgi.repository.*;
import aQute.bnd.osgi.resource.*;
import java.net.*;
import java.util.*;
import org.osgi.resource.*;
import org.osgi.service.repository.*;

println 'Tests for behaviour-marketplace-maven-plugin \'simple\''
println " basedir: ${basedir}"

// Check the behaviour exists!
File behaviour = new File(basedir, 'behaviour/target/behaviour-0.0.1-SNAPSHOT-brain-iot-smart-behaviour.jar')
assert behaviour.isFile()

// Check the top level index exists!
File index = new File(basedir, 'marketplace/target/marketplace/index.xml')
assert index.isFile()


XMLResourceParser xrp = new XMLResourceParser(index.toURI());
List<Resource> resources = xrp.parse();

assert xrp.check();
assert resources != null;
assert resources.size() == 1;
assert xrp.name() != null;

Resource res = resources.get(0)
	
assert "com.paremus.brain.iot.maven.test.behaviour" == ResourceUtils.getIdentityCapability(res).getAttributes().get("osgi.identity");

Capability cap = res.getCapabilities("eu.brain.iot.behaviour").get(0)

assert "Test Behaviour" == cap.getAttributes().get("name")
assert "Paremus" == cap.getAttributes().get("author")

// Check the existence of the sub-index
cap = ResourceUtils.getContentCapability(res)
File subIndex = new File(new URI(cap.getAttributes().get("url")))
assert subIndex.isFile()
