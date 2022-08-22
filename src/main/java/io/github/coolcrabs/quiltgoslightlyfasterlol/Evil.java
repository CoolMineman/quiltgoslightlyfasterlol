package io.github.coolcrabs.quiltgoslightlyfasterlol;

import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.Map;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.entrypoint.PreLaunchEntrypoint;



public class Evil implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch(ModContainer mod) {
        boolean worked = true;
        long t1 = System.nanoTime();
        try {
            Evil0.upToNoGood();
        } catch (Throwable t) {
            worked = false;
            System.out.println("Failed to load QuiltGoSlightlyFasterLol :(");
            t.printStackTrace();
        }
        long t2 = System.nanoTime();
        if (worked) {
            System.out.println("Loaded QuiltGoSlightlyFasterLol " + (t2 - t1) + "ns");
        }
    }
    
}
