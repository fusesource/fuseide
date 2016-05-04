/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.fusesource.ide.buildtools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.insight.maven.aether.Aether;
import io.fabric8.insight.maven.aether.AetherResult;
import io.fabric8.insight.maven.aether.Repository;
import io.hawt.maven.indexer.ArtifactDTO;
import io.hawt.maven.indexer.MavenIndexerFacade;

public class Downloader {

    public static Logger LOG = LoggerFactory.getLogger(Downloader.class);

    private MavenIndexerFacade indexer;
    private Aether aether;
    private File archetypeDir = new File("fuse-ide-archetypes");
    private File xsdDir = new File("fuse-ide-xsds");
    private boolean delete = true;
    
    private static final String langFilePrefix = "/*******************************************************************************\n" + 
            " * Copyright (c) 2013 Red Hat, Inc.\n" + 
            " * Distributed under license by Red Hat, Inc. All rights reserved.\n" + 
            " * This program is made available under the terms of the\n" + 
            " * Eclipse Public License v1.0 which accompanies this distribution,\n" + 
            " * and is available at http://www.eclipse.org/legal/epl-v10.html\n" + 
            " *\n" + 
            " * Contributors:\n" + 
            " *     Red Hat, Inc. - initial API and implementation\n" + 
            " ******************************************************************************/\n" + 
            "\n" + 
            "package org.fusesource.ide.camel.editor;\n" + 
            "\n" + 
            "import org.eclipse.osgi.util.NLS;\n" + 
            "\n" + 
            "/**\n" + 
            " * NOTE - this file is auto-generated.\n" +
            " *\n" +
            " * DO NOT EDIT!\n" + 
            " *\n" + 
            " * @author lhein\n" + 
            " */\n" + 
            "public class ConnectorsMessages extends NLS {\n" + 
            "\n" + 
            "    private static final String BUNDLE_NAME = \"org.fusesource.ide.camel.editor.l10n.connectorsMessages\";\n\n";
    
    private static final String langFilePostfix = "\n\n" + 
            "    static {\n" + 
            "        // initialize resource bundle\n" + 
            "        NLS.initializeMessages(BUNDLE_NAME, ConnectorsMessages.class);\n" + 
            "    }\n" + 
            "}\n";

    // setup an ignore list for unwanted archetypes
    private static ArrayList<String> ignoredArtifacts = new ArrayList<String>();

    static {
        ignoredArtifacts.add("camel-archetype-component-scala");
        ignoredArtifacts.add("camel-archetype-scala");
        ignoredArtifacts.add("camel-web-osgi-archetype");
        ignoredArtifacts.add("camel-archetype-groovy");
        ignoredArtifacts.add("camel-archetype-api-component");
        ignoredArtifacts.add("camel-archetype-dataformat");
        ignoredArtifacts.add("camel-archetype-scr");
        ignoredArtifacts.add("camel-archetype-war");
        ignoredArtifacts.add("camel-archetype-webconsole");
        ignoredArtifacts.add("wildfly-camel-archetype-cdi");
        
    }

    public static void main(String[] args) {
        try {
            // lets find the eclipse plugins directory
            File rs_editor = new File(targetDir(), "../../../editor/plugins");
            File rs_core = new File(targetDir(), "../../../core/plugins");
            
            if (args.length > 1) {
                rs_editor = new File(args[0]);
                rs_core = new File(args[1]);
            }

            LOG.info("Using editor plugins directory: {}", rs_editor.getAbsolutePath());
            LOG.info("Using core plugins directory: {}", rs_core.getAbsolutePath());

            if (!rs_editor.exists()) {
                fail("IDE editor plugins directory does not exist! " + rs_editor.getAbsolutePath());
            }
            if (!rs_editor.isDirectory()) {
                fail("IDE editor plugins directory is a file, not a directory! " + rs_editor.getAbsolutePath());
            }
            if (!rs_core.exists()) {
                fail("IDE core plugins directory does not exist! " + rs_core.getAbsolutePath());
            }
            if (!rs_core.isDirectory()) {
                fail("IDE core plugins directory is a file, not a directory! "  + rs_core.getAbsolutePath());
            }

            File archetypesDir = new File(rs_editor, "org.fusesource.ide.branding/archetypes");
            File xsdsDir = new File(rs_editor, "org.fusesource.ide.catalogs");

            Downloader app = new Downloader(archetypesDir, xsdsDir);
            app.start();
            LOG.info("Indexer has started, now trying to find stuff");
            app.run();
            app.stop();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    protected static void fail(String message) {
        LOG.info(message);
        System.exit(1);
    }

    public Downloader() {
    }

    public Downloader(File archetypeDir, File xsdDir) {
        this.archetypeDir = archetypeDir;
        this.xsdDir = xsdDir;
    }

    public static File targetDir() {
        String basedir = System.getProperty("basedir", ".");
        return new File(basedir + "/target");
    }

    public void start() throws Exception {
        indexer = new MavenIndexerFacade();
        String[] repositories = { "http://origin-repository.jboss.org/nexus/content/groups/ea", "http://repository.jboss.org/nexus/content/groups/ea/", "http://repository.jboss.org/nexus/content/groups/fs-public/", "http://repo1.maven.org/maven2" };
        indexer.setRepositories(repositories);
        indexer.setCacheDirectory(new File(targetDir(), "mavenIndexer"));
        indexer.start();

        List<Repository> repos = Aether.defaultRepositories();
        repos.add(new Repository("ea.repository.jboss.org", "http://repository.jboss.org/nexus/content/groups/ea"));
        repos.add(new Repository("releases.repository.jboss.org", "http://repository.jboss.org/nexus/content/groups/fs-public/"));
        aether = new Aether(Aether.USER_REPOSITORY, repos);
    }

    public void stop() throws Exception {
        indexer.destroy();
    }

    public void run() throws Exception {
        downloadArchetypes();
        downloadXsds();
    }

    public void downloadArchetypes() throws IOException {
        if (delete) {
            FileUtils.deleteDirectory(archetypeDir);
            archetypeDir.mkdirs();
        }

        PrintWriter out = new PrintWriter(new FileWriter(new File(archetypeDir, "archetypes.xml")));
        out.println("<archetypes>");

        try {
            downloadArchetypesForGroup(out, "org.apache.camel.archetypes", System.getProperty("camel.version"));
            downloadArchetypesForGroup(out, "org.apache.cxf.archetype", System.getProperty("cxf.version"));
            downloadArchetypesForGroup(out, "org.wildfly.camel.archetypes", System.getProperty("wildfly.version"));
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        } finally {
            out.println("</archetypes>");
            out.close();
        }

        LOG.info("Running git add...");
        ProcessBuilder pb = new ProcessBuilder("git", "add", "*");
        pb.directory(archetypeDir);
        pb.start();
    }

    protected void downloadArchetypesForGroup(PrintWriter out, String groupId, String version)
            throws Exception {
        String classifier = null;
        String packaging = "maven-archetype";

        List<ArtifactDTO> answer = indexer.search(groupId, "", "", packaging, classifier, null);
        for (ArtifactDTO artifact : answer) {
            if (ignoredArtifacts.contains(artifact.getArtifactId())) {
                LOG.debug("Ignored: {}", artifact.getArtifactId());
                continue;
            }
            out.println("<archetype groupId='" + artifact.getGroupId() + "' artifactId='" + artifact.getArtifactId() + "' version='" + version + "'>" + artifact.getDescription() + "</archetype>");
            downloadArtifact(artifact, version);
        }
        LOG.debug("Found " + answer.size() + " results for groupId " + groupId + ", version " + version);
    }

    public void downloadXsds() throws Exception {
        new DownloadLatestXsds(xsdDir, true).run();
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public Aether getAether() {
        return aether;
    }

    public void setAether(Aether aether) {
        this.aether = aether;
    }

    public MavenIndexerFacade getIndexer() {
        return indexer;
    }

    public void setIndexer(MavenIndexerFacade indexer) {
        this.indexer = indexer;
    }

    public File getArchetypeDir() {
        return archetypeDir;
    }

    public void setArchetypeDir(File archetypeDir) {
        this.archetypeDir = archetypeDir;
    }

    public File getXsdDir() {
        return xsdDir;
    }

    public void setXsdDir(File xsdDir) {
        this.xsdDir = xsdDir;
    }

    protected void downloadArtifact(ArtifactDTO artifact, String version) {
        try {
            AetherResult result = aether.resolve(artifact.getGroupId(), artifact.getArtifactId(), version, "jar", null);
            if (result != null) {
                List<File> files = result.getResolvedFiles();
                if (files != null && files.size() > 0) {
                    File file = files.get(0);
                    //for (File file : files) {
                    File newFile = new File(archetypeDir, file.getName());
                    FileInputStream input = new FileInputStream(file);
                    FileOutputStream output = new FileOutputStream(newFile);
                    IOUtils.copy(input, output);
                    IOUtils.closeQuietly(input);
                    IOUtils.closeQuietly(output);
                    LOG.info("Copied {}", newFile.getPath());
                }
            }
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
        }
    }
}
