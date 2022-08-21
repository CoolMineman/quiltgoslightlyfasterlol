package io.github.coolcrabs.quiltgoslightlyfasterlol;

import nilloader.api.lib.asm.tree.ClassNode;
import nilloader.api.lib.mini.MiniTransformer;
import nilloader.api.lib.mini.PatchContext;
import nilloader.api.lib.mini.annotation.Patch;

@Patch.Class("org.quiltmc.loader.impl.launch.knot.KnotClassDelegate")
public class Funni extends MiniTransformer {
    // Basically @Overwrite
    @Patch.Method("getRawClassByteArray(Ljava/lang/String;Z)[B")
    public void patchGetRawClassByteArray(PatchContext ctx) {
        ctx.jumpToStart();
        ctx.add(
            ALOAD(1),
            ILOAD(2),
            ALOAD(0), // this
            GETFIELD("org/quiltmc/loader/impl/launch/knot/KnotClassDelegate", "itf", "Lorg/quiltmc/loader/impl/launch/knot/KnotClassLoaderInterface;"),
            INVOKESTATIC("io/github/coolcrabs/quiltgoslightlyfasterlol/Hook", "getRawClassByteArray", "(Ljava/lang/String;ZLjava/lang/Object;)[B"),
            ARETURN()
        );
    }

    // Otherwise verifier complains about stack frames
    @Override
    protected boolean modifyClassStructure(ClassNode clazz) {
        return true;
    }
}
