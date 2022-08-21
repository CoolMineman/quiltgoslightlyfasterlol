package io.github.coolcrabs.quiltgoslightlyfasterlol;

import java.lang.instrument.Instrumentation;
import java.nio.file.Paths;
import java.util.jar.JarFile;

public class Yeet {
    public static void agentmain(String agentArgs, Instrumentation ins) throws Exception {
        Funni f = new Funni();
        ins.addTransformer(f, true);
        ins.retransformClasses(Class.forName("org.quiltmc.loader.impl.launch.knot.KnotClassDelegate"));
        ins.removeTransformer(f);
    }
}
