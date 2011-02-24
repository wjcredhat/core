/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.switchyard.tools.maven.plugins.switchyard;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.selectors.FileSelector;
import org.switchyard.config.model.MergeScanner;
import org.switchyard.config.model.Model;
import org.switchyard.config.model.ModelResourceScanner;
import org.switchyard.config.model.Scanner;
import org.switchyard.config.model.ScannerInput;
import org.switchyard.config.model.switchyard.v1.V1SwitchYardModel;
import org.switchyard.config.util.Classes;

/**
 * ConfiguratorMojo.
 *
 * @goal configurator
 * @phase process-classes
 * @requiresDependencyResolution compile
 *
 * @author David Ward &lt;<a href="mailto:dward@jboss.org">dward@jboss.org</a>&gt; (C) 2011 Red Hat Inc.
 */
public class ConfiguratorMojo<M extends Model> extends AbstractMojo {

    /**
     * @parameter
     */
    private String modelClassName;

    /**
     * @parameter
     */
    private String[] scannerClassNames = new String[]{};

    /**
     * @parameter expression="${project.compileClasspathElements}"
     */
    private List<String> compileClasspathElements;

    /**
     * @parameter expression="${project.resources}"
     */
    private List<Resource> resources;

    /**
     * @parameter expression="${project.basedir}"
     */
    private File basedir;

    /**
     * @parameter expression="${project.artifactId}"
     */
    private String artifactId;

    /**
     * @parameter
     */
    private String[] includes = new String[]{"target/classes"};

    /**
     * @parameter
     */
    private String[] excludes = new String[]{};

    /**
     * @parameter expression="${project.build.outputDirectory}"
     */
    private File outputDirectory;

    /**
     * @parameter
     */
    private File outputFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final List<URL> mojoURLs = new ArrayList<URL>();
        try {
            for (String compileClasspaths : compileClasspathElements) {
                mojoURLs.add(new File(compileClasspaths).toURI().toURL());
            }
            File defaultDir = new File("target/classes");
            if (defaultDir.exists()) {
                Resource defaultResource = new Resource();
                defaultResource.setTargetPath(defaultDir.getAbsolutePath());
                resources.add(defaultResource);
            }
            for (Resource resource : resources) {
                String path = resource.getTargetPath();
                if (path != null) {
                    mojoURLs.add(new File(path).toURI().toURL());
                }
            }
        } catch (MalformedURLException mue) {
            throw new MojoExecutionException(mue.getMessage(), mue);
        }
        ClassLoader loader = AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
            public URLClassLoader run() {
                return new URLClassLoader(mojoURLs.toArray(new URL[mojoURLs.size()]), ConfiguratorMojo.class.getClassLoader());
            }
        });
        ClassLoader previous = Classes.setTCCL(loader);
        Writer writer = null;
        DirectoryScanner ds = new DirectoryScanner();
        ds.setSelectors(new FileSelector[]{new FileSelector() {
            @Override
            public boolean isSelected(File basedir, String filename, File file) throws BuildException {
                return file.exists() && (file.isDirectory() || filename.endsWith(".jar"));
            }
        }});
        ds.setIncludes(includes);
        ds.setExcludes(excludes);
        ds.setBasedir(basedir);
        try {
            ds.scan();
            List<URL> scannerURLs = new ArrayList<URL>();
            for (String includedPath : ds.getIncludedDirectories()) {
                scannerURLs.add(new File(includedPath).toURI().toURL());
            }
            List<Scanner<M>> scanners = new ArrayList<Scanner<M>>();
            for (String scannerClassName : scannerClassNames) {
                @SuppressWarnings("unchecked")
                Class<Scanner<M>> scannerClass = (Class<Scanner<M>>)Classes.forName(scannerClassName, loader);
                if (scannerClass != null) {
                    Scanner<M> scanner = scannerClass.newInstance();
                    scanners.add(scanner);
                }
            }
            scanners.add(new ModelResourceScanner<M>());
            if (modelClassName == null) {
                modelClassName = V1SwitchYardModel.class.getName();
            }
            @SuppressWarnings("unchecked")
            Class<M> modelClass = (Class<M>)Classes.forName(modelClassName, loader);
            MergeScanner<M> merge_scanner = new MergeScanner<M>(modelClass, true, scanners);
            ScannerInput<M> scanner_input = new ScannerInput<M>().setName(artifactId).setURLs(scannerURLs);
            M model = merge_scanner.scan(scanner_input).getModel();
            if (outputFile == null) {
                File od = new File(outputDirectory, "META-INF");
                if (!od.exists()) {
                    if (!od.mkdirs()) {
                        throw new Exception("mkdirs() on " + od + " failed.");
                    }
                }
                outputFile = new File(od, "switchyard.xml");
            }
            writer = new BufferedWriter(new FileWriter(outputFile));
            model.write(writer);
        } catch (Throwable t) {
            throw new MojoExecutionException(t.getMessage(), t);
        } finally {
            Classes.setTCCL(previous);
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ioe) {
                    throw new MojoExecutionException(ioe.getMessage(), ioe);
                }
            }
        }
    }

}
