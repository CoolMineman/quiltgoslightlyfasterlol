package io.github.coolcrabs.quiltgoslightlyfasterlol;

import java.io.ByteArrayOutputStream;

public class ByteArrayOutputStreamEx extends ByteArrayOutputStream {
    public ByteArrayOutputStreamEx(int cap) {
        super(cap);
    }

    public byte[] buf() {
        return this.buf;
    }
}
