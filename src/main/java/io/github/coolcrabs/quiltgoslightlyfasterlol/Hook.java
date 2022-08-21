package io.github.coolcrabs.quiltgoslightlyfasterlol;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.security.SecureClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import zipfast.Native;

public class Hook {
    static Object itf_cached;
    static URLClassLoader urlLoader;
    static ClassLoader originalLoader;
    static ThreadLocal<Long> tl = ThreadLocal.withInitial(() -> Native.libdeflate_inflater_alloc());

    public static byte[] getRawClassByteArray(String name, boolean allowFromParent, Object/* KnotClassLoaderInterface */ itf) throws Throwable {
        if (itf_cached == null) {
            itf_cached = itf;
            Field ulf = itf.getClass().getDeclaredField("urlLoader");
            ulf.setAccessible(true);
            urlLoader = (URLClassLoader) ulf.get(itf);
            Field olf = itf.getClass().getDeclaredField("originalLoader");
            olf.setAccessible(true);
            originalLoader = (ClassLoader) olf.get(itf); 
        }
        if (itf_cached != itf) throw new IllegalStateException();
        SecureClassLoader cl = (SecureClassLoader) itf;
        String f = getClassFileName(name);
        URL url = urlLoader.findResource(f);
        if (url == null && allowFromParent) url = originalLoader.getResource(f);
        if (url == null) return null;
        switch (url.getProtocol()) {
            case "file": {
                try (FileChannel fc = FileChannel.open(Path.of(url.toURI()))) {
                    long s = fc.size();
                    if (s > Integer.MAX_VALUE) throw new IllegalStateException();
                    ByteBuffer r = ByteBuffer.allocate((int) s);
                    fc.read(r);
                    return r.array();
                }
            }
            case "jar": {
                JarURLConnection con = (JarURLConnection) url.openConnection();
                JarFile jf = con.getJarFile();
                JarEntry e = con.getJarEntry();
                Object res = Hax.res.get(jf);
                Object zsrc = Hax.zsrc.get(res);
                int cenpos;
                synchronized (jf) {
                    cenpos = (Integer) Hax.getEntryPos.invoke(zsrc, e.getName(), false);
                }
                FileChannel fc = ((RandomAccessFile)Hax.zfile.get(zsrc)).getChannel();
                byte[] cen = (byte[]) Hax.cen.get(zsrc);
                int how = CENHOW(cen, cenpos);
                long rem = CENSIZ(cen, cenpos);
                long size = CENLEN(cen, cenpos);
                if (size > Integer.MAX_VALUE) throw new IllegalStateException();
                long pos = CENOFF(cen, cenpos);
                // zip64
                if (rem == ZIP64_MAGICVAL || size == ZIP64_MAGICVAL || pos == ZIP64_MAGICVAL) {
                    int off = cenpos + CENHDR + CENNAM(cen, cenpos);
                    int end = off + CENEXT(cen, cenpos);
                    while (off + 4 < end) {
                        int tag = get16(cen, off);
                        int sz = get16(cen, off + 2);
                        off += 4;
                        if (off + sz > end)         // invalid data
                            break;
                        if (tag == EXTID_ZIP64) {
                            if (size == ZIP64_MAGICVAL) {
                                if (sz < 8 || (off + 8) > end)
                                    break;
                                size = get64(cen, off);
                                sz -= 8;
                                off += 8;
                            }
                            if (rem == ZIP64_MAGICVAL) {
                                if (sz < 8 || (off + 8) > end)
                                    break;
                                rem = get64(cen, off);
                                sz -= 8;
                                off += 8;
                            }
                            if (pos == ZIP64_MAGICVAL) {
                                if (sz < 8 || (off + 8) > end)
                                    break;
                                pos = get64(cen, off);
                                sz -= 8;
                                off += 8;
                            }
                            break;
                        }
                        off += sz;
                    }
                }
                pos += (Long) Hax.locpos.get(zsrc);
                byte[] loc = new byte[LOCHDR];
                int len = fc.read(ByteBuffer.wrap(loc), pos);
                if (len != LOCHDR) {
                    throw new ZipException("ZipFile error reading zip file");
                }
                if (LOCSIG(loc) != LOCSIG) {
                    throw new ZipException("ZipFile invalid LOC header (bad signature)");
                }
                pos += LOCHDR + LOCNAM(loc) + LOCEXT(loc);
                switch (how) {
                    case ZipEntry.STORED: {
                        byte[] r = new byte[(int) size];
                        fc.read(ByteBuffer.wrap(r), pos);
                        return r;
                    }
                    case ZipEntry.DEFLATED: {
                        MappedByteBuffer mbb = fc.map(MapMode.READ_ONLY, pos, rem);
                        ByteBuffer r0 = ByteBuffer.allocateDirect((int) size);
                        Native.libdeflate_inflate(tl.get(), mbb, 0, (int) rem, r0, 0, (int) size);
                        byte[] r = new byte[(int) size];
                        r0.get(r);
                        return r;
                    }
                    default:
                        throw new IllegalStateException("" + how);
                }
            }
            default: {
                try (InputStream is = url.openStream()) {
                    ByteArrayOutputStreamEx o = new ByteArrayOutputStreamEx(is.available());
                    byte[] buffer = new byte[1024];
                    int read = 0;
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        o.write(buffer, 0, len);
                        read += len;
                    }
                    if (o.buf().length == read) {
                        return o.buf();
                    } else {
                        return o.toByteArray();
                    }
                }
            }
        }
    }

    static String getClassFileName(String className) {
        return className.replace('.', '/').concat(".class");
    }

    // Stolen code

    // local file (LOC) header fields
    static final long LOCSIG(byte[] b) { return LG(b, 0); } // signature
    static final int  LOCVER(byte[] b) { return SH(b, 4); } // version needed to extract
    static final int  LOCFLG(byte[] b) { return SH(b, 6); } // general purpose bit flags
    static final int  LOCHOW(byte[] b) { return SH(b, 8); } // compression method
    static final long LOCTIM(byte[] b) { return LG(b, 10);} // modification time
    static final long LOCCRC(byte[] b) { return LG(b, 14);} // crc of uncompressed data
    static final long LOCSIZ(byte[] b) { return LG(b, 18);} // compressed data size
    static final long LOCLEN(byte[] b) { return LG(b, 22);} // uncompressed data size
    static final int  LOCNAM(byte[] b) { return SH(b, 26);} // filename length
    static final int  LOCEXT(byte[] b) { return SH(b, 28);} // extra field length

    /**
     * Local file (LOC) header signature.
     */
    static long LOCSIG = 0x04034b50L;   // "PK\003\004"

    /**
     * Extra local (EXT) header signature.
     */
    static long EXTSIG = 0x08074b50L;   // "PK\007\008"

    /**
     * Central directory (CEN) header signature.
     */
    static long CENSIG = 0x02014b50L;   // "PK\001\002"

    /**
     * End of central directory (END) header signature.
     */
    static long ENDSIG = 0x06054b50L;   // "PK\005\006"

    /**
     * Local file (LOC) header size in bytes (including signature).
     */
    static final int LOCHDR = 30;

    /**
     * Extra local (EXT) header size in bytes (including signature).
     */
    static final int EXTHDR = 16;

    /**
     * Central directory (CEN) header size in bytes (including signature).
     */
    static final int CENHDR = 46;

    /**
     * End of central directory (END) header size in bytes (including signature).
     */
    static final int ENDHDR = 22;

    /**
     * Local file (LOC) header version needed to extract field offset.
     */
    static final int LOCVER = 4;

    /**
     * Local file (LOC) header general purpose bit flag field offset.
     */
    static final int LOCFLG = 6;

    /**
     * Local file (LOC) header compression method field offset.
     */
    static final int LOCHOW = 8;

    /**
     * Local file (LOC) header modification time field offset.
     */
    static final int LOCTIM = 10;

    /**
     * Local file (LOC) header uncompressed file crc-32 value field offset.
     */
    static final int LOCCRC = 14;

    /**
     * Local file (LOC) header compressed size field offset.
     */
    static final int LOCSIZ = 18;

    /**
     * Local file (LOC) header uncompressed size field offset.
     */
    static final int LOCLEN = 22;

    /**
     * Local file (LOC) header filename length field offset.
     */
    static final int LOCNAM = 26;

    /**
     * Local file (LOC) header extra field length field offset.
     */
    static final int LOCEXT = 28;

    /**
     * Extra local (EXT) header uncompressed file crc-32 value field offset.
     */
    static final int EXTCRC = 4;

    /**
     * Extra local (EXT) header compressed size field offset.
     */
    static final int EXTSIZ = 8;

    /**
     * Extra local (EXT) header uncompressed size field offset.
     */
    static final int EXTLEN = 12;

    /**
     * Central directory (CEN) header version made by field offset.
     */
    static final int CENVEM = 4;

    /**
     * Central directory (CEN) header version needed to extract field offset.
     */
    static final int CENVER = 6;

    /**
     * Central directory (CEN) header encrypt, decrypt flags field offset.
     */
    static final int CENFLG = 8;

    /**
     * Central directory (CEN) header compression method field offset.
     */
    static final int CENHOW = 10;

    /**
     * Central directory (CEN) header modification time field offset.
     */
    static final int CENTIM = 12;

    /**
     * Central directory (CEN) header uncompressed file crc-32 value field offset.
     */
    static final int CENCRC = 16;

    /**
     * Central directory (CEN) header compressed size field offset.
     */
    static final int CENSIZ = 20;

    /**
     * Central directory (CEN) header uncompressed size field offset.
     */
    static final int CENLEN = 24;

    /**
     * Central directory (CEN) header filename length field offset.
     */
    static final int CENNAM = 28;

    /**
     * Central directory (CEN) header extra field length field offset.
     */
    static final int CENEXT = 30;

    /**
     * Central directory (CEN) header comment length field offset.
     */
    static final int CENCOM = 32;

    /**
     * Central directory (CEN) header disk number start field offset.
     */
    static final int CENDSK = 34;

    /**
     * Central directory (CEN) header internal file attributes field offset.
     */
    static final int CENATT = 36;

    /**
     * Central directory (CEN) header external file attributes field offset.
     */
    static final int CENATX = 38;

    /**
     * Central directory (CEN) header LOC header offset field offset.
     */
    static final int CENOFF = 42;

    /**
     * End of central directory (END) header number of entries on this disk field offset.
     */
    static final int ENDSUB = 8;

    /**
     * End of central directory (END) header total number of entries field offset.
     */
    static final int ENDTOT = 10;

    /**
     * End of central directory (END) header central directory size in bytes field offset.
     */
    static final int ENDSIZ = 12;

    /**
     * End of central directory (END) header offset for the first CEN header field offset.
     */
    static final int ENDOFF = 16;

    /**
     * End of central directory (END) header zip file comment length field offset.
     */
    static final int ENDCOM = 20;

    /*
     * ZIP64 constants
     */
    static final long ZIP64_ENDSIG = 0x06064b50L;  // "PK\006\006"
    static final long ZIP64_LOCSIG = 0x07064b50L;  // "PK\006\007"
    static final int  ZIP64_ENDHDR = 56;           // ZIP64 end header size
    static final int  ZIP64_LOCHDR = 20;           // ZIP64 end loc header size
    static final int  ZIP64_EXTHDR = 24;           // EXT header size
    static final int  ZIP64_EXTID  = 0x0001;       // Extra field Zip64 header ID

    static final int  ZIP64_MAGICCOUNT = 0xFFFF;
    static final long ZIP64_MAGICVAL = 0xFFFFFFFFL;

    /*
     * Zip64 End of central directory (END) header field offsets
     */
    static final int  ZIP64_ENDLEN = 4;       // size of zip64 end of central dir
    static final int  ZIP64_ENDVEM = 12;      // version made by
    static final int  ZIP64_ENDVER = 14;      // version needed to extract
    static final int  ZIP64_ENDNMD = 16;      // number of this disk
    static final int  ZIP64_ENDDSK = 20;      // disk number of start
    static final int  ZIP64_ENDTOD = 24;      // total number of entries on this disk
    static final int  ZIP64_ENDTOT = 32;      // total number of entries
    static final int  ZIP64_ENDSIZ = 40;      // central directory size in bytes
    static final int  ZIP64_ENDOFF = 48;      // offset of first CEN header
    static final int  ZIP64_ENDEXT = 56;      // zip64 extensible data sector

    /*
     * Zip64 End of central directory locator field offsets
     */
    static final int  ZIP64_LOCDSK = 4;       // disk number start
    static final int  ZIP64_LOCOFF = 8;       // offset of zip64 end
    static final int  ZIP64_LOCTOT = 16;      // total number of disks

    /*
     * Zip64 Extra local (EXT) header field offsets
     */
    static final int  ZIP64_EXTCRC = 4;       // uncompressed file crc-32 value
    static final int  ZIP64_EXTSIZ = 8;       // compressed size, 8-byte
    static final int  ZIP64_EXTLEN = 16;      // uncompressed size, 8-byte

    /*
     * Language encoding flag (general purpose flag bit 11)
     *
     * If this bit is set the filename and comment fields for this
     * entry must be encoded using UTF-8.
     */
    static final int USE_UTF8 = 0x800;

    /*
     * Constants below are defined here (instead of in ZipConstants)
     * to avoid being exposed as public fields of ZipFile, ZipEntry,
     * ZipInputStream and ZipOutputstream.
     */

    /*
     * Extra field header ID
     */
    static final int  EXTID_ZIP64 = 0x0001;    // Zip64
    static final int  EXTID_NTFS  = 0x000a;    // NTFS
    static final int  EXTID_UNIX  = 0x000d;    // UNIX
    static final int  EXTID_EXTT  = 0x5455;    // Info-ZIP Extended Timestamp

    /*
     * EXTT timestamp flags
     */
    static final int  EXTT_FLAG_LMT = 0x1;       // LastModifiedTime
    static final int  EXTT_FLAG_LAT = 0x2;       // LastAccessTime
    static final int  EXTT_FLAT_CT  = 0x4;       // CreationTime

    public static final long get64(byte[] b, int off) {
        return get32(b, off) | (get32(b, off+4) << 32);
    }

    public static final long get32(byte[] b, int off) {
        return (get16(b, off) | ((long)get16(b, off+2) << 16)) & 0xffffffffL;
    }

    public static final int get16(byte[] b, int off) {
        return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8);
    }

    // fields access methods
    static final int CH(byte[] b, int n) {
        return b[n] & 0xff ;
    }

    static final int SH(byte[] b, int n) {
        return (b[n] & 0xff) | ((b[n + 1] & 0xff) << 8);
    }

    static final long LG(byte[] b, int n) {
        return ((SH(b, n)) | (SH(b, n + 2) << 16)) & 0xffffffffL;
    }

    static final long LL(byte[] b, int n) {
        return (LG(b, n)) | (LG(b, n + 4) << 32);
    }

    static final long GETSIG(byte[] b) {
        return LG(b, 0);
    }

    // central directory header (CEN) fields
    static final long CENSIG(byte[] b, int pos) { return LG(b, pos + 0); }
    static final int  CENVEM(byte[] b, int pos) { return SH(b, pos + 4); }
    static final int  CENVEM_FA(byte[] b, int pos) { return CH(b, pos + 5); } // file attribute compatibility
    static final int  CENVER(byte[] b, int pos) { return SH(b, pos + 6); }
    static final int  CENFLG(byte[] b, int pos) { return SH(b, pos + 8); }
    static final int  CENHOW(byte[] b, int pos) { return SH(b, pos + 10);}
    static final long CENTIM(byte[] b, int pos) { return LG(b, pos + 12);}
    static final long CENCRC(byte[] b, int pos) { return LG(b, pos + 16);}
    static final long CENSIZ(byte[] b, int pos) { return LG(b, pos + 20);}
    static final long CENLEN(byte[] b, int pos) { return LG(b, pos + 24);}
    static final int  CENNAM(byte[] b, int pos) { return SH(b, pos + 28);}
    static final int  CENEXT(byte[] b, int pos) { return SH(b, pos + 30);}
    static final int  CENCOM(byte[] b, int pos) { return SH(b, pos + 32);}
    static final int  CENDSK(byte[] b, int pos) { return SH(b, pos + 34);}
    static final int  CENATT(byte[] b, int pos) { return SH(b, pos + 36);}
    static final long CENATX(byte[] b, int pos) { return LG(b, pos + 38);}
    static final int  CENATX_PERMS(byte[] b, int pos) { return SH(b, pos + 40);} // posix permission data
    static final long CENOFF(byte[] b, int pos) { return LG(b, pos + 42);}
}
