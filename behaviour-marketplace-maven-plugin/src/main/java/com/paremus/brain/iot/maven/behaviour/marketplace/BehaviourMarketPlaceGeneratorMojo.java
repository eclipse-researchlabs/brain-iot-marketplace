/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.paremus.brain.iot.maven.behaviour.marketplace;

import static aQute.bnd.osgi.resource.ResourceUtils.getContentCapability;
import static aQute.bnd.osgi.resource.ResourceUtils.getIdentity;
import static aQute.bnd.osgi.resource.ResourceUtils.getIdentityCapability;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourDeploymentNamespace.CAPABILITY_REQUIREMENTS_ATTRIBUTE;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourDeploymentNamespace.CAPABILITY_RESOURCES_ATTRIBUTE;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourDeploymentNamespace.CONTENT_MIME_TYPE_INDEX;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourDeploymentNamespace.IDENTITY_TYPE_SMART_BEHAVIOUR;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourDeploymentNamespace.SMART_BEHAVIOUR_DEPLOYMENT_NAMESPACE;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourManifest.BRAIN_IOT_DEPLOY_REQUIREMENTS;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourManifest.BRAIN_IOT_DEPLOY_RESOURCES;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourManifest.BRAIN_IOT_SMART_BEHEAVIOUR_SYMBOLIC_NAME;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourManifest.BRAIN_IOT_SMART_BEHEAVIOUR_VERSION;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourManifest.SMART_BEHAVIOUR_MAVEN_CLASSIFIER;
import static eu.brain.iot.behaviour.namespace.SmartBehaviourNamespace.SMART_BEHAVIOUR_NAMESPACE;
import static org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE;
import static org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;
import static org.osgi.service.repository.ContentNamespace.CAPABILITY_MIME_ATTRIBUTE;
import static org.osgi.service.repository.ContentNamespace.CAPABILITY_URL_ATTRIBUTE;
import static org.osgi.service.repository.ContentNamespace.CONTENT_NAMESPACE;
import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;


@Mojo(name = "generate", defaultPhase=LifecyclePhase.GENERATE_RESOURCES, requiresDependencyResolution=ResolutionScope.TEST)
public class BehaviourMarketPlaceGeneratorMojo extends AbstractMojo {

	private static final Logger logger = LoggerFactory.getLogger(BehaviourMarketPlaceGeneratorMojo.class);
	
	/**
	 * The folder to use for building the artifact based on the gathered dependencies. 
	 * If <code>gatherDependencies</code> is <code>false</code> then this folder must
	 * be populated in some other way. 
	 */
	@Parameter(defaultValue="${project.build.directory}/marketplace", required=false)
	private File targetFolder;
	
	@Parameter( defaultValue = "${project}", readonly = true )
	private MavenProject mavenProject;

	@Parameter( defaultValue = "${session}", readonly = true )
	private MavenSession mavenSession;

	@Component
	private BuildPluginManager pluginManager;
	
	
	private Map<Path, Path> indexes = new TreeMap<>();
	private Map<Path, Manifest> manifests = new TreeMap<>();
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		extractBehaviours();
		
		generateIndexes();
		
		List<Resource> behaviours;
		try {
			behaviours = generateTopLevelIndex();
		} catch (Exception e) {
			logger.error("There was a problem generating the index {}", e.getMessage());
			throw new MojoExecutionException("Failed to generate the top-level-index", e);
		}
		
		try(PrintWriter writer = new PrintWriter(new File(targetFolder, "index.html"))) {
			writer.append("<html>")
				.append("    <ul>");
			
			for(Resource r : behaviours) {
				String identity = getIdentity(getIdentityCapability(r));

				String link = (String) getContentCapability(r)
						.getAttributes().get(CAPABILITY_URL_ATTRIBUTE);
				
				for(Capability cap : r.getCapabilities(SMART_BEHAVIOUR_NAMESPACE)) {
					writer.append("        <li>");
					
					Map<String, Object> attributes = cap.getAttributes();
					Object name = attributes.get("name");
					
					writer.append(name == null ? "Unnamed Behaviour" : String.valueOf(name))
						.append(" : located in bundle <em>")
						.append(identity)
						.append("</em> and found in index <a href=\"")
						.append(link)
						.append("\">)")
						.append(link)
						.append("</a>");
					
					writer.append("        </li>");
				}
			}
				
			writer.append("    </ul>")
				.append("</html>");
		} catch (FileNotFoundException e) {
			logger.error("There was a problem writing the html {}", e.getMessage());
			throw new MojoExecutionException("Failed to generate the html site", e);
		}
	}

	private void extractBehaviours() throws MojoExecutionException {
		executeMojo(
			    plugin(
			        groupId("org.apache.maven.plugins"),
			        artifactId("maven-dependency-plugin"),
			        version("3.1.1")
			    ),
			    goal("unpack-dependencies"),
			    configuration(
			        element(name("outputDirectory"), targetFolder.getAbsolutePath()),
			        element(name("classifier"), SMART_BEHAVIOUR_MAVEN_CLASSIFIER),
			        element(name("useSubDirectoryPerArtifact"), "true")
			    ),
			    executionEnvironment(
			        mavenProject,
			        mavenSession,
			        pluginManager
			    )
			);
	}

	private void generateIndexes() throws MojoExecutionException {
		try {
			for (Path path : Files.newDirectoryStream(targetFolder.getAbsoluteFile().toPath())) {
				
				if(Files.isDirectory(path)) {
					
					indexes.put(path, indexDirectory(path));
					
					Path manifest = path.resolve("META-INF/MANIFEST.MF");
					
					try (InputStream is = Files.newInputStream(manifest)) {
						manifests.put(path, new Manifest(is));
					}
				}
			}
		} catch (IOException e) {
			logger.error("A problem occurred generating the behaviour indexes", e);
			throw new MojoExecutionException("Unable to index the extracted Smart Behaviours", e);
		}
	}

	private Path indexDirectory(Path path) throws MojoExecutionException {
		Path index = path.resolve("index.xml");
		
		executeMojo(
				plugin(
						groupId("biz.aQute.bnd"),
						artifactId("bnd-indexer-maven-plugin"),
						version("4.3.0")
						),
				goal("local-index"),
				configuration(
						element(name("inputDir"), path.toAbsolutePath().toString()),
						element(name("outputFile"), index.toString())
						),
				executionEnvironment(
						mavenProject,
						mavenSession,
						pluginManager
						)
				);
		return index;
	}
	
    private List<Resource> generateTopLevelIndex() throws Exception {
    	
    	Path root = targetFolder.toPath();
    	
        List<Resource> behaviours = new ArrayList<>();

        for (Path folder : indexes.keySet()) {
        	
        	Attributes manifest = manifests.get(folder).getMainAttributes();
        	
        	ResourceBuilder builder = new ResourceBuilder();
        	
        	// Add the identity capability for this resource
        	builder.addCapability(generateIdentityCapability(manifest));
        	
        	// Add the Deployment Capability for this resource
        	builder.addCapability(generateDeploymentCapability(folder, manifest));

        	//Add the content capability for this behaviour
        	Path index = indexes.get(folder);

        	builder.addCapability(generateContentCapability(root, index));
        	
        	// Look for any Smart Behaviours listed in the sub-index
			for (Resource resource : XMLResourceParser.getResources(index.toFile())) {
				builder.addCapabilities(resource.getCapabilities(SMART_BEHAVIOUR_NAMESPACE));
			}
			
			Resource resource = builder.build();
			if(resource.getCapabilities(SMART_BEHAVIOUR_NAMESPACE).isEmpty()) {
				logger.warn("The generated resource for {} has no Smart Behaviour Capabilities advertised",
						ResourceUtils.getIdentityCapability(resource));
			}
			behaviours.add(resource);
        }

        XMLResourceGenerator repository = new XMLResourceGenerator();
        repository.name("Behaviour Marketplace " + mavenProject.getGroupId() + ":" +
        		mavenProject.getArtifactId() + ":" + mavenProject.getVersion());
        repository.resources(behaviours);
        repository.save(root.resolve("index.xml").toFile().getAbsoluteFile());
        
        return behaviours;
    }

	private CapReqBuilder generateIdentityCapability(Attributes manifest) throws Exception {
		return new CapReqBuilder(IDENTITY_NAMESPACE)
				.addAttribute(IDENTITY_NAMESPACE, manifest.getValue(BRAIN_IOT_SMART_BEHEAVIOUR_SYMBOLIC_NAME))
				.addAttribute(CAPABILITY_VERSION_ATTRIBUTE, manifest.getValue(BRAIN_IOT_SMART_BEHEAVIOUR_VERSION))
				.addAttribute(CAPABILITY_TYPE_ATTRIBUTE, IDENTITY_TYPE_SMART_BEHAVIOUR);
	}

	private CapReqBuilder generateDeploymentCapability(Path folder, Attributes manifest)
			throws MojoExecutionException, Exception {
		String attributeKey = CAPABILITY_REQUIREMENTS_ATTRIBUTE;
		String attributeValue = manifest.getValue(BRAIN_IOT_DEPLOY_REQUIREMENTS);
		if(attributeValue == null) {
			attributeValue = manifest.getValue(BRAIN_IOT_DEPLOY_RESOURCES);
			if(attributeValue == null) {
				logger.error("The manifest for {} does not contain any requirements or resources", folder.getFileName());
				throw new MojoExecutionException("Invalid Smart Behaviour Manifest for behaviour extracted to path " + folder);
			}
			attributeKey = CAPABILITY_RESOURCES_ATTRIBUTE;
		}
		
		CapReqBuilder deploymentCap = new CapReqBuilder(SMART_BEHAVIOUR_DEPLOYMENT_NAMESPACE)
				.addAttribute(attributeKey, attributeValue);
		return deploymentCap;
	}

	private CapReqBuilder generateContentCapability(Path root, Path index) throws Exception {
		return new CapReqBuilder(CONTENT_NAMESPACE)
				.addAttribute(CAPABILITY_MIME_ATTRIBUTE, CONTENT_MIME_TYPE_INDEX)
				.addAttribute(CAPABILITY_URL_ATTRIBUTE, root.relativize(index).toString());
	}

}
