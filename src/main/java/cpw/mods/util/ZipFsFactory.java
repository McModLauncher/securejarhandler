package cpw.mods.util;

import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 * Utilities for dealing with ZipFilesystems.
 */
@ApiStatus.Internal
public final class ZipFsFactory {
    private static final MethodHandle ZIPFS_CH;
    private static final MethodHandle FCI_UNINTERUPTIBLE;

    static {
        try {
            var hackfield = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            hackfield.setAccessible(true);
            MethodHandles.Lookup hack = (MethodHandles.Lookup) hackfield.get(null);

            var clz = Class.forName("jdk.nio.zipfs.ZipFileSystem");
            ZIPFS_CH = hack.findGetter(clz, "ch", SeekableByteChannel.class);

            clz = Class.forName("sun.nio.ch.FileChannelImpl");
            FCI_UNINTERUPTIBLE = hack.findSpecial(clz, "setUninterruptible", MethodType.methodType(void.class), clz);
        } catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private ZipFsFactory() {
    }

    public static FileSystem create(Path path) {
        try {
            var zfs = FileSystems.newFileSystem(path);
            SeekableByteChannel fci = (SeekableByteChannel) ZIPFS_CH.invoke(zfs);
            if (fci instanceof FileChannel) { // we only make file channels uninterruptible because byte channels (JIJ) already are
                FCI_UNINTERUPTIBLE.invoke(fci);
            }
            return zfs;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to open file system from path " + path, e);
        } catch (Throwable t) {
            // Rethrow to include the path that caused it
            throw new IllegalStateException("Failed to open file system from path " + path, t);
        }
    }
}
