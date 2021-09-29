package cpw.mods.niofs.union;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.function.IntBinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UnionPath implements Path {
    private final UnionFileSystem fileSystem;
    private final String[] pathParts;
    static final String ROOT = "";

    UnionPath(final UnionFileSystem fileSystem, boolean knownCorrectSplit, final String... pathParts) {
        this.fileSystem = fileSystem;
        if (pathParts.length == 0)
            this.pathParts = new String[0];
        else if (knownCorrectSplit)
            this.pathParts = pathParts;
        else {
            final var longstring = String.join(fileSystem.getSeparator(), pathParts);
            this.pathParts = getPathParts(longstring);
        }
    }

    private String[] getPathParts(final String longstring) {
        return longstring.replace("\\", this.getFileSystem().getSeparator()).split(this.getFileSystem().getSeparator());
    }

    @Override
    public UnionFileSystem getFileSystem() {
        return this.fileSystem;
    }

    @Override
    public boolean isAbsolute() {
        return ROOT.equals(this.pathParts[0]);
    }

    @Override
    public Path getRoot() {
        return new UnionPath(this.fileSystem, true, ROOT);
    }


    @Override
    public Path getFileName() {
        return this.pathParts.length > 0 ? new UnionPath(this.getFileSystem(), true, this.pathParts[this.pathParts.length-1]) : null;
    }


    @Override
    public Path getParent() {
        if (this.pathParts.length > 0) {
            return new UnionPath(this.fileSystem, true, Arrays.copyOf(this.pathParts,this.pathParts.length - 1));
        } else {
            return null;
        }
    }

    @Override
    public int getNameCount() {
        return this.pathParts.length;
    }

    @Override
    public Path getName(final int index) {
        if (index < 0 || index > this.pathParts.length -1) throw new IllegalArgumentException();
        return new UnionPath(this.fileSystem, true, this.pathParts[index]);
    }

    @Override
    public Path subpath(final int beginIndex, final int endIndex) {
        if (beginIndex < 0 || beginIndex > this.pathParts.length - 1 || endIndex < 0 || endIndex > this.pathParts.length || beginIndex > endIndex) {
            throw new IllegalArgumentException("Out of range "+beginIndex+" to "+endIndex+" for length "+this.pathParts.length);
        }
        return new UnionPath(this.fileSystem, true, Arrays.copyOfRange(this.pathParts, beginIndex, endIndex));
    }

    @Override
    public boolean startsWith(final Path other) {
        if (other.getFileSystem() != this.getFileSystem()) {
            return false;
        }
        if (other instanceof UnionPath bp) {
            return checkArraysMatch(this.pathParts, bp.pathParts, false);
        }
        return false;
    }


    @Override
    public boolean endsWith(final Path other) {
        if (other.getFileSystem() != this.getFileSystem()) {
            return false;
        }
        if (other instanceof UnionPath bp) {
            return checkArraysMatch(this.pathParts, bp.pathParts, true);
        }
        return false;
    }

    private static boolean checkArraysMatch(String[] array1, String[] array2, boolean reverse) {
        var length = Math.min(array1.length, array2.length);
        IntBinaryOperator offset = reverse ? (l, i) -> l - i - 1 : (l, i) -> i;
        for (int i = 0; i < length; i++) {
            if (!Objects.equals(array1[offset.applyAsInt(array1.length, i)], array2[offset.applyAsInt(array2.length, i)]))
                return false;
        }
        return true;
    }

    @Override
    public Path normalize() {
        Deque<String> normpath = new ArrayDeque<>();
        for (String pathPart : this.pathParts) {
            switch (pathPart) {
                case ".":
                    break;
                case "..":
                    normpath.removeLast();
                    break;
                default:
                    normpath.addLast(pathPart);
                    break;
            }
        }
        return new UnionPath(this.fileSystem, true, normpath.toArray(new String[0]));
    }

    @Override
    public Path resolve(final Path other) {
        if (other instanceof UnionPath path) {
            if (path.isAbsolute()) {
                return path;
            }
            return new UnionPath(this.fileSystem, false, this+fileSystem.getSeparator()+other);
        }
        return other;
    }

    @Override
    public Path relativize(final Path other) {
        if (other.getFileSystem()!=this.getFileSystem()) throw new IllegalArgumentException("Wrong filesystem");
        if (other instanceof UnionPath p) {
            var poff = p.isAbsolute() ? 1 : 0;
            var meoff = this.isAbsolute() ? 1 : 0;
            var length = Math.min(this.pathParts.length - meoff, p.pathParts.length - poff);
            int i = 0;
            while (i < length) {
                if (!Objects.equals(this.pathParts[i + meoff], p.pathParts[i + poff]))
                    break;
                i++;
            }

            var remaining = this.pathParts.length - i - meoff;
            if (remaining == 0 && (i + poff) == p.pathParts.length) {
                return new UnionPath(this.getFileSystem(), false);
            } else if (remaining == 0) {
                return p.subpath(i + poff, p.getNameCount());
            } else {
                var updots = IntStream.range(0, remaining).mapToObj(idx -> "..").collect(Collectors.joining(getFileSystem().getSeparator()));
                if ((i + poff) == p.pathParts.length) {
                    return new UnionPath(this.getFileSystem(), false, updots);
                } else {
                    return new UnionPath(this.getFileSystem(), false, updots + getFileSystem().getSeparator() + p.subpath((i + poff), p.getNameCount()));
                }
            }
        }
        throw new IllegalArgumentException("Wrong filesystem");
    }

    @Override
    public URI toUri() {
        try {
            return new URI(
                fileSystem.provider().getScheme(),
                null,
                fileSystem.getKey() + '!' + toAbsolutePath(),
                null
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Path toAbsolutePath() {
        if (isAbsolute())
            return this;
        else
            return fileSystem.getRoot().resolve(this);
    }

    @Override
    public Path toRealPath(final LinkOption... options) throws IOException {
        return null;
    }

    @Override
    public WatchKey register(final WatchService watcher, final WatchEvent.Kind<?>[] events, final WatchEvent.Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(final Path other) {
        return 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof UnionPath p) {
            return p.getFileSystem() == this.getFileSystem() && Arrays.equals(this.pathParts, p.pathParts);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.fileSystem) + 31 * Arrays.hashCode(this.pathParts);
    }

    @Override
    public String toString() {
        return String.join(fileSystem.getSeparator(), this.pathParts);
    }
}
