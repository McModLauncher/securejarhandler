package cpw.mods.jarhandling.impl;

import cpw.mods.niofs.union.UnionFileSystem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSecureJar {
    private static FileSystem withZfs;
    private static FileSystem withUnion;

    @BeforeAll
    static void setup() {
        final var zipPath = Path.of("src/test/resources/dir1.zip");
        withZfs = new JarContentsImpl(new Path[]{zipPath}, Manifest::new, null).filesystem;
        // Using a filter will force it to use a UnionFS
        withUnion = new JarContentsImpl(new Path[]{zipPath}, Manifest::new, (entry, basePath) -> true).filesystem;
        // check the assumptions
        assertThat(withZfs).isNotInstanceOf(UnionFileSystem.class);
        assertThat(withUnion).isInstanceOf(UnionFileSystem.class);
    }

    @AfterAll
    static void cleanup() throws Exception {
        withZfs.close();
        withUnion.close();
    }

    @Test
    void testBothSeparatorsWork() {
        for (final String path : List.of(
                "masktest.txt",
                "/masktest.txt",
                "\\masktest.txt",

                "subdir1/masktestsd1.txt",
                "subdir1\\masktestsd1.txt",
                "/subdir1\\masktestsd1.txt"
        )) {
            var union = assertThat(withUnion.getPath(path)).withFailMessage("Union path " + path + " doesn't exist").exists();
            var zfsPath = withZfs.getPath(path);
            assertThat(zfsPath).withFailMessage("ZipFS path " + path + " doesn't exist").exists();
            union.hasSameBinaryContentAs(zfsPath);
        }
    }

    @Test
    void testAbsolutePathMatch() {
        assertThat(withUnion.getPath("subdir1").resolve("./masktestsd1.txt").toAbsolutePath())
                .extracting(Path::toString)
                .isEqualTo(withZfs.getPath("subdir1").resolve("./masktestsd1.txt").toAbsolutePath().toString());

        assertThat(withUnion.getPath("subdir1").resolve("./masktestsd1.txt").toAbsolutePath().normalize())
                .extracting(Path::toString)
                .isEqualTo(withZfs.getPath("subdir1").resolve("./masktestsd1.txt").toAbsolutePath().normalize().toString())
                .isEqualTo("/subdir1/masktestsd1.txt");
    }

    @Test
    void testRelativePathMatch() {
        assertThat(withUnion.getPath("/subdir1").relativize(withUnion.getPath("/masktest.txt")))
                .isRelative()
                .isEqualTo(withUnion.getPath("../masktest.txt"));

        assertThat(withZfs.getPath("/subdir1").relativize(withZfs.getPath("/masktest.txt")))
                .isRelative()
                .isEqualTo(withZfs.getPath("../masktest.txt"));
    }

    @Test
    void testRelativeInversion() {
        for (FileSystem fs : List.of(withUnion, withZfs)) {
            assertThat(fs.getPath("/subdir1").relativize(fs.getPath("/subdir1").resolve(fs.getPath("masktestsd1.txt"))))
                    .isRelative().isEqualTo(fs.getPath("masktestsd1.txt"));
        }
    }
}
