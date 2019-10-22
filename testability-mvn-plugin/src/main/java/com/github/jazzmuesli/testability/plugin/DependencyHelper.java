package com.github.jazzmuesli.testability.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

/**
 * copied from saaremaa.
 * 
 * @author preich
 *
 */
public class DependencyHelper {

	private static final String PLUGIN_NAME = "com.google.testability-explorer:testability-mvn-plugin";

	private static Set<Artifact> getDevArtifacts(ArtifactRepository localRepository, RepositorySystem repositorySystem,
			Map<String, Artifact> pluginArtifactMap) {
		Artifact artifact = pluginArtifactMap.get(PLUGIN_NAME);
		if (artifact == null) {
			throw new IllegalArgumentException(PLUGIN_NAME + " not found in  " + pluginArtifactMap.keySet());
		}

		ArtifactResolutionRequest request = new ArtifactResolutionRequest().setArtifact(artifact)
				.setResolveTransitively(true).setLocalRepository(localRepository);
		ArtifactResolutionResult result = repositorySystem.resolve(request);
		return result.getArtifacts();
	}

	//TODO: re-use BuildClasspathMojo, https://github.com/apache/maven-dependency-plugin/blob/master/src/main/java/org/apache/maven/plugins/dependency/resolvers/ResolveDependenciesMojo.java
//	static class MyMojo extends BuildClasspathMojo {
//
//		private MavenProject mavenProject;
//
//
//		public void setMavenProject(MavenProject mavenProject) {
//			this.mavenProject = mavenProject;
//		}
//		
//		@Override
//		public MavenProject getProject() {
//			return this.mavenProject;
//		}
//
//		public String getClasspath() {
//			StringBuilder sb = new StringBuilder();
//			Set<Artifact> deps;
//			try {
//				deps = getResolvedDependencies(true);
//				deps.forEach(dep -> appendArtifactPath(dep, sb));
//			} catch (MojoExecutionException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			return sb.toString();
//		}
//	}
	/**
	 * TODO: make it less hacky.
	 * 
	 * Here we need classpath of the target project + target/test-clases,
	 * target/classes + dependencies of this plugin. Note that this plugin won't be
	 * added to target project's pom.xml, but rather executed mvn
	 * org.pavelreich.saaremaa:plugin:analyse
	 * 
	 * @param log
	 * @param project
	 * @param pluginArtifactMap
	 * @param repositorySystem
	 * @param localRepository
	 * @return
	 * @throws MojoExecutionException
	 */
	public static LinkedHashSet<String> prepareClasspath(MavenProject project, ArtifactRepository localRepository,
			RepositorySystem repositorySystem, Map<String, Artifact> pluginArtifactMap, Log log)
			throws MojoExecutionException {
		LinkedHashSet<String> classpath = new LinkedHashSet<String>();
		classpath.addAll(generateClasspath(project, log));
		if (!classpath.isEmpty()) {
			return classpath;
		}
		
		try {
			log.info("project: " + project);
			String cpProp = System.getProperty("java.class.path");
			if (cpProp!=null) {
				classpath.addAll(Arrays.asList(cpProp.split(File.pathSeparator)));				
			}
			
			log.info("class.path: " + cpProp);
			project.getArtifacts().stream().filter(x -> x.getGroupId().equals(project.getGroupId()))
					.forEach(x -> log.info("artifact: " + x));
			log.info("artifacts: " + project.getArtifacts());
			classpath.addAll(project.getTestClasspathElements());
			log.info("depArtifacts: " + project.getDependencyArtifacts());
			classpath.addAll(getCoverageClasspath(project));

			classpath.addAll(project.getCompileClasspathElements());
			classpath.addAll(project.getRuntimeClasspathElements());
			// copied from
			// https://github.com/tbroyer/gwt-maven-plugin/blob/54fe4621d1ee5127b14030f6e1462de44bace901/src/main/java/net/ltgt/gwt/maven/CompileMojo.java#L295
			ClassWorld world = new ClassWorld();
			ClassRealm realm;
			try {
				realm = world.newRealm("gwt", null);
				for (String elt : project.getCompileSourceRoots()) {
					URL url = new File(elt).toURI().toURL();
					realm.addURL(url);
//					log.info("Source root: " + url);
				}
				for (String elt : project.getCompileClasspathElements()) {
					URL url = new File(elt).toURI().toURL();
					realm.addURL(url);
//					log.info("Compile classpath: " + url);
				}
				for (Artifact elt : getDevArtifacts(localRepository, repositorySystem, pluginArtifactMap)) {
					URL url = elt.getFile().toURI().toURL();
					realm.addURL(url);
					log.info("transitive classpath: " + url);
				}
				URL pluginUrls = pluginArtifactMap.get(PLUGIN_NAME).getFile().toURI().toURL();
				realm.addURL(pluginUrls);
				List<String> urls = Arrays.asList(realm.getURLs()).stream().map(x -> {
					try {
						return new File(x.toURI()).getAbsolutePath();
					} catch (Exception e) {
						throw new IllegalArgumentException(e.getMessage(), e);
					}
				}).collect(Collectors.toList());
				urls.stream().forEach(x -> classpath.add(x));
			} catch (Exception e) {
				throw new MojoExecutionException(e.getMessage(), e);
			}

		} catch (DependencyResolutionRequiredException e1) {
			log.error(e1.getMessage(), e1);
		}
		classpath.forEach(e->log.info("classpathElem: " +e));
		return classpath;
	}

	/**
	 * TODO: it's very dirty to call maven build-classpath
	 * @param project
	 * @param log
	 * @return
	 */
	static LinkedHashSet<String> generateClasspath(MavenProject project, Log log) {
		LinkedHashSet<String> classpath = new LinkedHashSet<>();
		try {
			InvocationRequest request = new DefaultInvocationRequest();
			request.setPomFile(project.getFile());
			request.setGoals(Collections.singletonList("dependency:build-classpath"));
			Properties properties = new Properties();

			String fileName = project.getBasedir().getAbsolutePath()+File.separator+"cpdep.txt";
			new File(fileName).delete();
			properties.setProperty("mdep.outputFile", fileName);
			request.setProperties(properties);

			Invoker invoker = new DefaultInvoker();

			InvocationResult res = invoker.execute(request);
			log.info("result: " + res);
			BufferedReader fr = new BufferedReader(new FileReader(fileName));
			List<String> lines = fr.lines().collect(Collectors.toList());
			for (String line : lines) {
				classpath.addAll(Arrays.asList(line.split(File.pathSeparator)));
			}
			log.info("cp: " + classpath);
			fr.close();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return classpath;
	}

	/**
	 * copied from MavenProject::getTestClasspath
	 * 
	 * @param project
	 * @return
	 */
	private static void addArtifactPath(Artifact artifact, List<String> classpath) {
		File file = artifact.getFile();
		if (file != null) {
			classpath.add(file.getPath());
		}
	}

	public static List<String> getCoverageClasspath(MavenProject project) {
		Set<Artifact> artifacts = new HashSet(project.getArtifacts());
		artifacts.addAll(project.getDependencyArtifacts());
		List<String> list = new ArrayList<>(artifacts.size() + 2);

		String d = project.getBuild().getTestOutputDirectory();
		if (d != null) {
			list.add(d);
		}

		d = project.getBuild().getOutputDirectory();
		if (d != null) {
			list.add(d);
		}

		
		for (Artifact a : artifacts) {
			//if (a.getGroupId().equals(project.getGroupId()) && a.getArtifactHandler().isAddedToClasspath()) {
				addArtifactPath(a, list);
			//}
		}

		return list;

	}
}
