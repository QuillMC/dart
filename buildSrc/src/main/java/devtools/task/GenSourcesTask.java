package devtools.task;

import com.github.quillmc.tinymcp.TinyMCP;
import com.github.quillmc.tinymcp.Version;
import devtools.task.DartTask;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class GenSourcesTask extends DartTask {
    public Property<Version> serverVersion;
    public File sourcesDir;
    public File resourcesDir;

    @Override
    public void run() {
        if (serverVersion.isPresent()) {
            Version v = serverVersion.get();
            System.out.println("Setting up development environment for Minecraft version " + v);

            TinyMCP mcp = v.server();
            plugin.setMcp(mcp);

            File jar = mcp.getMappedJar();

            File srcDir = plugin.getSourceDir();
            if (srcDir.exists() || srcDir.mkdirs()) {
                // decompile sources
                extractResources(jar, plugin.getResourceDir());
                Options options = OptionsImpl.getFactory().create(Map.of(
                        "renameillegalidents", "true",
                        "outputdir", srcDir.getAbsolutePath()
                ));

                CfrDriver cfrDriver = new CfrDriver.Builder().withBuiltOptions(options).build();
                cfrDriver.analyse(List.of(jar.getAbsolutePath()));
                File cfrSummary = new File(srcDir, "summary.txt");
                if (cfrSummary.exists()) cfrSummary.delete();

                plugin.getGitManager().setupGit();
            } else throw new GradleException("Failed to setup project sources at " + srcDir);
        }
    }

    private void extractResources(File jar, File out) {
        if (out.exists() || out.mkdirs()) {
            try {
                ZipInputStream zis = new ZipInputStream(new FileInputStream(jar));
                ZipEntry entry = zis.getNextEntry();
                while (entry != null) {
                    File output = new File(out, entry.getName());
                    System.out.println("Extracting " + entry.getName());
                    if (entry.isDirectory()) {
                        if (!out.exists() && !out.mkdirs())
                            throw new IOException("Failed creating directory " + output);
                    } else if (!entry.getName().endsWith(".class")) {
                        if (output.getParentFile().exists() || output.getParentFile().mkdirs()) {
                            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output));
                            byte[] bytesIn = new byte[4096];
                            int read;
                            while ((read = zis.read(bytesIn)) != -1) bos.write(bytesIn, 0, read);
                            bos.close();
                        } else throw new IOException("Failed creating file " + output);
                    }
                    zis.closeEntry();
                    entry = zis.getNextEntry();
                }
            } catch (IOException e) {
                throw new GradleException("Failed to extract jar resources", e);
            }
        } else throw new GradleException("Failed to setup project sources at " + out);
    }
}
