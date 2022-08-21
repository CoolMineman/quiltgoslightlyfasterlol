package io.github.coolcrabs.quiltgoslightlyfasterlol;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class Funni implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!"org/quiltmc/loader/impl/launch/knot/KnotClassDelegate".equals(className)) return null;
        ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        new ClassReader(classfileBuffer).accept(new CV(Opcodes.ASM9, w), 0);
        return w.toByteArray();
    }

    static class CV extends ClassVisitor {

        protected CV(int api, ClassVisitor classVisitor) {
            super(api, classVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if ("getRawClassByteArray".equals(name)) {
                return new MV(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions));
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
        
    }

    static class MV extends MethodVisitor {

        protected MV(int api, MethodVisitor methodVisitor) {
            super(api, methodVisitor);
        }

        @Override
        public void visitCode() {
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ILOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, "org/quiltmc/loader/impl/launch/knot/KnotClassDelegate", "itf", "Lorg/quiltmc/loader/impl/launch/knot/KnotClassLoaderInterface;");
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "io/github/coolcrabs/quiltgoslightlyfasterlol/Hook", "getRawClassByteArray", "(Ljava/lang/String;ZLjava/lang/Object;)[B", false);
            mv.visitInsn(Opcodes.ARETURN);
        }

    }
}
