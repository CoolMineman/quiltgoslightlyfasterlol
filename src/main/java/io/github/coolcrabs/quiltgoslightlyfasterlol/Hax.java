package io.github.coolcrabs.quiltgoslightlyfasterlol;

import java.io.RandomAccessFile;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.jar.JarFile;

import sun.misc.Unsafe;

public class Hax {
    public static final VarHandle savedProps;
    public static final VarHandle res;
    public static final VarHandle zsrc;
    public static final MethodHandle getEntryPos;
    public static final VarHandle cen;
    public static final VarHandle locpos;
    public static final VarHandle zfile;

    static {
        try {
            Unsafe e = getUnsafe();
            MethodHandles.Lookup trustedLookup = (MethodHandles.Lookup) e.getObject(MethodHandles.Lookup.class, e.staticFieldOffset(MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP")));
            savedProps = trustedLookup.findStaticVarHandle(Class.forName("jdk.internal.misc.VM"), "savedProps", Map.class);
            res = trustedLookup.findVarHandle(JarFile.class, "res", Class.forName("java.util.zip.ZipFile$CleanableResource"));
            zsrc = trustedLookup.findVarHandle(Class.forName("java.util.zip.ZipFile$CleanableResource"), "zsrc", Class.forName("java.util.zip.ZipFile$Source"));
            getEntryPos = trustedLookup.findVirtual(Class.forName("java.util.zip.ZipFile$Source"), "getEntryPos", MethodType.methodType(int.class, String.class, boolean.class));
            cen = trustedLookup.findVarHandle(Class.forName("java.util.zip.ZipFile$Source"), "cen", byte[].class);
            locpos = trustedLookup.findVarHandle(Class.forName("java.util.zip.ZipFile$Source"), "locpos", long.class);
            zfile = trustedLookup.findVarHandle(Class.forName("java.util.zip.ZipFile$Source"), "zfile", RandomAccessFile.class);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Unsafe getUnsafe() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        return (Unsafe) theUnsafe.get(null);
    }
}
