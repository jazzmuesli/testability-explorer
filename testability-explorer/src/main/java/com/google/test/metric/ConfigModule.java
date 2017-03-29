// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.test.metric;

import com.google.classpath.ClassPath;
import com.google.classpath.ClassPathFactory;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.test.metric.ReportGeneratorProvider.ReportFormat;
import com.google.test.metric.report.ReportOptions;
import java.io.File;
import java.io.IOException;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.PrintStream;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ConfigModule extends AbstractModule {

  @Retention(RUNTIME) @BindingAnnotation public @interface Error {}
  @Retention(RUNTIME) @BindingAnnotation public @interface Output {}

  private final String[] args;
  private final PrintStream out;
  private final PrintStream err;

  public ConfigModule(String[] args, PrintStream out, PrintStream err) {
    this.args = args;
    this.out = out;
    this.err = err;
  }

  @Override
  protected void configure() {
    // For printing errors to the caller
    bind(PrintStream.class).annotatedWith(Output.class).toInstance(System.out);
    bind(PrintStream.class).annotatedWith(Error.class).toInstance(System.err);
    CommandLineConfig config = new CommandLineConfig(out, err);
    CmdLineParser parser = new CmdLineParser(config);
    // We actually do this work in configure, since we want to bind the parsed command line
    // options at this time
    try {
      parser.parseArgument(args);
      config.validate();
      final ClassPathFactory factory = new ClassPathFactory();
      final String[] expandedClasspath = expandWildcard(factory.parseClasspath(config.cp));
      bind(ClassPath.class).toInstance(factory.createFromPaths(expandedClasspath));
      bind(ReportFormat.class).toInstance(config.format);      
    } catch (CmdLineException e) {
      err.println(e.getMessage() + "\n");
      parser.setUsageWidth(120);
      parser.printUsage(err);
      err.println("Exiting...");
    }
    bind(CommandLineConfig.class).toInstance(config);
    bind(ReportOptions.class).toInstance(new ReportOptions(
        config.cyclomaticMultiplier, config.globalMultiplier, config.constructorMultiplier,
        config.maxExcellentCost, config.maxAcceptableCost, config.worstOffenderCount,
        config.maxMethodCount, config.maxLineCount, config.printDepth, config.minCost,
        config.srcFileLineUrl, config.srcFileUrl));
    bindConstant().annotatedWith(Names.named("printDepth")).to(config.printDepth);
    bind(new TypeLiteral<List<String>>() {}).toInstance(config.entryList);

    //TODO: install the appropriate language-specific module
    install(new JavaTestabilityModule(config));
  }
  
   private String[] expandWildcard(String[] paths) {
      final List<Path> c = new ArrayList<>();
        for(String s : paths){
            c.addAll(fromGlob(s));
        }
        final String[] retval = new String[c.size()];
        for(int i=0; i<c.size(); i++){
            retval[i] = String.valueOf(c.get(i));
        }
        return retval;
    }
   
   private static List<Path> fromGlob(String glob){
        final List<Path> paths = new ArrayList<>();
        if(glob.indexOf('*')==-1){
            paths.add(Paths.get(glob));
        }else{
            final int index = glob.indexOf('*');
            final int separatorBefore = glob.lastIndexOf(File.separator, index);
            final Path start = identify(glob, separatorBefore);
            final String afterGlob = afterGlob(glob);
            try {
                final DirectoryStream<Path> str = Files.newDirectoryStream(start, afterGlob);
                str.forEach(paths::add);
            } catch (IOException ex) {
                Logger.getLogger(ConfigModule.class.getName()).log(Level.SEVERE, "Unable to walk through directory " + start + " with " + afterGlob, ex);
            }
        }
        return paths;
    }
   
   private static Path identify(String glob, int toIndex) {
        if(toIndex==-1){
            return Paths.get(".");
        }else if(glob.startsWith("~")){
            final String substr = glob.substring(1, toIndex);
            final String s = System.getProperty("user.home") + substr;
            return Paths.get(s);
        }else{
            final String subStr = glob.substring(0, toIndex);
            return Paths.get(subStr);
        }
    }
   
   private static String afterGlob(String glob) {
        final int index = glob.indexOf('*');
        final int separatorBefore = glob.lastIndexOf(File.separator, index);
        final String retval = glob.substring(separatorBefore==-1 ? 0 : separatorBefore+1);
        return retval;
    }
}
