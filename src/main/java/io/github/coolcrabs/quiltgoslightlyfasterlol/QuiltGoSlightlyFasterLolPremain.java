package io.github.coolcrabs.quiltgoslightlyfasterlol;

import nilloader.api.ClassTransformer;
import nilloader.api.NilLogger;

public class QuiltGoSlightlyFasterLolPremain implements Runnable {
    public static final NilLogger log = NilLogger.get("QuiltGoSlightlyFaster");

    @Override
    public void run() {
        log.info("Doin a little trolling!");
        ClassTransformer.register(new Funni());
    }
}
