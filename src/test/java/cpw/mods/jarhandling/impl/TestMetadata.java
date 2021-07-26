package cpw.mods.jarhandling.impl;

import cpw.mods.jarhandling.JarMetadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;

public class TestMetadata
{
    @Test
    void testDevMappedClass() throws MalformedURLException
    {
        var path = Paths.get("startofthepathchain/new-mod-1.16.5/1.1_mapped_official_1.17.1/new-mod-1.16.5-1.1_mapped_official_1.17.1.jar");
        var meta = JarMetadata.fromFileName(path, new HashSet<>(), new ArrayList<>());
        Assertions.assertEquals("_new.mod._1._16._5", meta.name());
    }
}
