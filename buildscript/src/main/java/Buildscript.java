import java.util.Arrays;
import java.util.stream.Collectors;

import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.fabric.FabricContext.ModDependencyCollector;
import io.github.coolcrabs.brachyura.fabric.FabricContext.ModDependencyFlag;
import io.github.coolcrabs.brachyura.fabric.FabricLoader;
import io.github.coolcrabs.brachyura.maven.Maven;
import io.github.coolcrabs.brachyura.maven.MavenId;
import io.github.coolcrabs.brachyura.minecraft.Minecraft;
import io.github.coolcrabs.brachyura.minecraft.VersionMeta;
import io.github.coolcrabs.brachyura.processing.ProcessorChain;
import io.github.coolcrabs.brachyura.processing.sinks.AtomicZipProcessingSink;
import io.github.coolcrabs.brachyura.processing.sources.DirectoryProcessingSource;
import io.github.coolcrabs.brachyura.processing.sources.ZipProcessingSource;
import io.github.coolcrabs.brachyura.quilt.QuiltMaven;
import io.github.coolcrabs.brachyura.quilt.SimpleQuiltProject;
import io.github.coolcrabs.brachyura.util.Util;
import net.fabricmc.mappingio.tree.MappingTree;

public class Buildscript extends SimpleQuiltProject {
    @Override
    public int getJavaVersion() {
        return 17;
    }

    @Override
    public VersionMeta createMcVersion() {
        return Minecraft.getVersion("1.19.2");
    }

    @Override
    public MappingTree createMappings() {
        return createMojmap();
    }

    @Override
    public FabricLoader getLoader() {
        return new FabricLoader(QuiltMaven.URL, QuiltMaven.loader("0.17.3"));
    }

    JavaJarDependency zipFast;

    @Override
    public void getModDependencies(ModDependencyCollector d) {
        zipFast = d.addMaven(Maven.MAVEN_LOCAL, new MavenId("io.github.coolmineman", "zipfast", "0.0.1"), ModDependencyFlag.COMPILE, ModDependencyFlag.RUNTIME);
    }

    @Override
    public JavaJarDependency build() {
        try {
            context.get().modDependencies.get(); // Ugly hack
            try (
                AtomicZipProcessingSink out = new AtomicZipProcessingSink(getBuildJarPath());
                ZipProcessingSource zipfast = new ZipProcessingSource(zipFast.jar);
            ) {
                resourcesProcessingChain().apply(out, Arrays.stream(getResourceDirs()).map(DirectoryProcessingSource::new).collect(Collectors.toList()));
                context.get().getRemappedClasses(module.get()).values().forEach(s -> s.getInputs(out));
                zipfast.getInputs((in, id) -> {
                    if (!id.path.startsWith("META-INF")) {
                        out.sink(in, id);
                    }
                });
                out.commit();
            }
            return new JavaJarDependency(getBuildJarPath(), null, getId());
        } catch (Exception e) {
            throw Util.sneak(e);
        }
    }
}
