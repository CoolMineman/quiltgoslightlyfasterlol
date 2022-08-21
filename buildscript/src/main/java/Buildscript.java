import io.github.coolcrabs.brachyura.fabric.FabricContext.ModDependencyCollector;
import io.github.coolcrabs.brachyura.fabric.FabricContext.ModDependencyFlag;
import io.github.coolcrabs.brachyura.maven.Maven;
import io.github.coolcrabs.brachyura.maven.MavenId;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import io.github.coolcrabs.accesswidener.AccessWidener;
import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.fabric.FabricContext;
import io.github.coolcrabs.brachyura.fabric.FabricLoader;
import io.github.coolcrabs.brachyura.fabric.FabricModule;
import io.github.coolcrabs.brachyura.minecraft.Minecraft;
import io.github.coolcrabs.brachyura.minecraft.VersionMeta;
import io.github.coolcrabs.brachyura.processing.ProcessorChain;
import io.github.coolcrabs.brachyura.processing.sinks.AtomicZipProcessingSink;
import io.github.coolcrabs.brachyura.processing.sources.DirectoryProcessingSource;
import io.github.coolcrabs.brachyura.processing.sources.ZipProcessingSource;
import io.github.coolcrabs.brachyura.quilt.QuiltContext;
import io.github.coolcrabs.brachyura.quilt.QuiltMaven;
import io.github.coolcrabs.brachyura.quilt.SimpleQuiltProject;
import io.github.coolcrabs.brachyura.util.Lazy;
import io.github.coolcrabs.brachyura.util.Util;
import net.fabricmc.mappingio.tree.MappingTree;

public class Buildscript extends SimpleQuiltProject {
    public static final String SLEEPING_TOWN = "https://repo.sleeping.town";

    public final Lazy<JavaJarDependency> nil = new Lazy<>(() -> Maven.getMavenJarDep(SLEEPING_TOWN, new MavenId("com.unascribed:nilloader:1.2.2")));

    @Override
    public int getJavaVersion() {
        return 17;
    }

    @Override
    public String getModId() {
        return "quiltgoslightlyfasterlol";
    }

    @Override
    public String getVersion() {
        return "0.0.1";
    }

    @Override
    public String getMavenGroup() {
        return "io.github.coolmineman";
    }

    @Override
    public @Nullable AccessWidener createAw() {
        return null;
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
    protected FabricContext createContext() {
        return new SimpleQuiltContext() {
            @Override
            public List<Path> getCompileDependencies() {
                ArrayList<Path> r = new ArrayList<>(super.getCompileDependencies());
                r.add(nil.get().jar);
                return r;
            }

            @Override
            protected List<JavaJarDependency> createIdeDependencies() {
                ArrayList<JavaJarDependency> r = new ArrayList<>(super.createIdeDependencies());
                r.add(nil.get());
                return r;
            }
        };
    }

    @Override
    protected FabricModule createModule() {
        return new SimpleQuiltModule((QuiltContext) context.get()) {
            @Override
            public List<String> ideVmArgs(boolean client) {
                JavaJarDependency nilSlapper = Maven.getMavenJarDep(Maven.MAVEN_LOCAL, new MavenId("io.github.coolmineman", "nilslapper", "0.0.1"));
                ArrayList<String> r = new ArrayList<>(super.ideVmArgs(client));
                r.add("-javaagent:" + nilSlapper.jar);
                r.add("-javaagent:" + nil.get().jar);
                r.add("-Dnilslapper=" + getModuleRoot());
                return r;
            }
        };
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
                new ProcessorChain().apply(out, zipfast);
                out.commit();
            }
            return new JavaJarDependency(getBuildJarPath(), null, getId());
        } catch (Exception e) {
            throw Util.sneak(e);
        }
    }
}
