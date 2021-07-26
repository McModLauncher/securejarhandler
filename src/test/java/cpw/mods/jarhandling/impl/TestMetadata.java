package cpw.mods.jarhandling.impl;

import cpw.mods.jarhandling.JarMetadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;

public class TestMetadata
{
    @Test
    void testMavenLikeJar()
    {
        var path = Paths.get("startofthepathchain/new-protected-class-1.16.5/1.1_mapped_official_1.17.1/new-protected-class-1.16.5-1.1_mapped_official_1.17.1.jar");
        var meta = JarMetadata.fromFileName(path, new HashSet<>(), new ArrayList<>());
        Assertions.assertEquals("_new._protected._class._1._16._5", meta.name());
    }

    @Test
    void testNumberStart()
    {
        var path = Paths.get("startofthepathchain/1life/1.5/1life-1.5.jar");
        var meta = JarMetadata.fromFileName(path, new HashSet<>(), new ArrayList<>());
        Assertions.assertEquals("_1life", meta.name());
    }
}
