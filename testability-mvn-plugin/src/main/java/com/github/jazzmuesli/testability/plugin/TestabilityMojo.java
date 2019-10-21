package com.github.jazzmuesli.testability.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import com.google.classpath.ClassPath;
import com.google.classpath.ClassPathFactory;
import com.google.inject.Guice;
import com.google.test.metric.JavaTestabilityRunner;

@Mojo(name = "testability", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.NONE)
public class TestabilityMojo extends AbstractMojo {

	/**
	 * The root package to inspect.
	 *
	 */
	@Parameter(defaultValue=".", readonly = true, required = false)
	String filter;

	/**
	 * The output directory for the intermediate XML report.
	 *
	 * @parameter property="${project.build.directory}"
	 * @required
	 */
	@Parameter(defaultValue = "${project.build.directory}", readonly = true, required = false)
	File targetDirectory;

	/**
	 * The output directory for the final HTML report. Note that this parameter is
	 * only evaluated if the goal is run directly from the command line or during
	 * the default lifecycle. If the goal is run indirectly as part of a site
	 * generation, the output directory configured in the Maven Site Plugin is used
	 * instead.
	 *
	 * @parameter property="${project.reporting.outputDirectory}"
	 * @required
	 */
	@Parameter(defaultValue = "${project.reporting.outputDirectory}", readonly = true, required = false)
	File outputDirectory;

	/**
	 * Filename of the output file, without the extension
	 *
	 * @parameter default-value="testability"
	 */
	
	@Parameter(defaultValue = "testability", readonly = true, required = false)
	String resultfile = "testability";

	/**
	 * Where to write errors from execution
	 *
	 * @parameter
	 */
	@Parameter(readonly = true, required = false)
	File errorfile;

	/**
	 * Weight of cyclomatic complexity cost
	 *
	 * @parameter default-value=1
	 */
	@Parameter(defaultValue = "1", readonly = true, required = false)
	Integer cyclomatic;

	/**
	 * Weight of global state cost
	 *
	 * @parameter default-value=10
	 */
	@Parameter(defaultValue = "10", readonly = true, required = false)
	Integer global;

	/**
	 * Extra multiplier applied to any costs where work is in a constructor
	 *
	 * @parameter default-value=1
	 */
	@Parameter(defaultValue = "1", readonly = true, required = false)
	Integer constructor;

	/**
	 * Maximum recursion depth of printed result
	 *
	 * @parameter default-value=2
	 */
	@Parameter(defaultValue = "2", readonly = true, required = false)
	Integer printDepth;

	/**
	 * Minimum cost to print a class metrics
	 *
	 * @parameter default-value=1
	 */
	@Parameter(defaultValue = "1", readonly = true, required = false)
	Integer minCost;

	/**
	 * Max cost for a class to be called excellent
	 *
	 * @parameter default-value=50
	 */
	@Parameter(defaultValue = "50", readonly = true, required = false)
	Integer maxExcellentCost;

	/**
	 * Max cost for a class to be called acceptable
	 *
	 * @parameter default-value=100
	 */
	@Parameter(defaultValue = "100", readonly = true, required = false)
	Integer maxAcceptableCost;

	/**
	 * Print this many of the worst classes
	 *
	 * @parameter default-value=20
	 */
	@Parameter(defaultValue = "20", readonly = true, required = false)
	Integer worstOffenderCount;

	/**
	 * Colon-delimited packages to whitelist
	 *
	 * @parameter default-value=" "
	 */
	@Parameter(defaultValue = " ", readonly = true, required = false)
	String whiteList;

	/**
	 * Set the output format type, in addition to the HTML report. Must be one of:
	 * "xml", "summary", "source", "detail". XML is required if the
	 * testability:check goal is being used.
	 *
	 * @parameter property="${format}" default-value="xml"
	 */
	@Parameter(defaultValue = "xml", readonly = true, required = false)
	String format = "xml";

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	MavenProject mavenProject;

	public void execute() throws MojoExecutionException {
		for (String dirName : mavenProject.getCompileSourceRoots()) {
			processSourceDirectory(dirName);
		}
		for (String dirName : mavenProject.getTestCompileSourceRoots()) {
			processSourceDirectory(dirName);
		}
	}

	private void processSourceDirectory(String dirName) {
		try {
			getLog().info("Processing " + dirName);
			if (new File(dirName).exists()) {
				MavenConfigModule mavenConfigModule = new MavenConfigModule(this);
				JavaTestabilityRunner runner = Guice.createInjector(mavenConfigModule).getInstance(JavaTestabilityRunner.class);
				runner.run();
			}
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
		}
	}

	@Parameter(defaultValue = "${plugin.artifactMap}", required = true, readonly = true)
	private Map<String, Artifact> pluginArtifactMap;

	@Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
	private ArtifactRepository localRepository;

	@Component
	private RepositorySystem repositorySystem;

	ClassPath getProjectClasspath() {
		// order matters
		Set<String> compileClasspathElements = new LinkedHashSet<String>();
		try {
			compileClasspathElements.addAll(mavenProject.getCompileClasspathElements());
			compileClasspathElements.addAll(DependencyHelper.prepareClasspath(mavenProject, 
					localRepository, repositorySystem, pluginArtifactMap, getLog()));

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		ClassPath classpath = new ClassPathFactory()
				.createFromPaths(compileClasspathElements.toArray(new String[compileClasspathElements.size()]));
		getLog().info("compileClasspathElements: " + compileClasspathElements);
		getLog().info("classpath: " + classpath);
		return classpath;
	}
}
