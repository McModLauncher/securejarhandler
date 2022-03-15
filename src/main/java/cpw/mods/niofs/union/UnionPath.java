package cpw.mods.niofs.union;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.function.IntBinaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UnionPath implements Path {
    private final UnionFileSystem fileSystem;
    private final boolean absolute;
    private final boolean empty;
    private final String[] pathParts;
    
    // Store the normalized path after it has been created first
    private UnionPath normalized;
    
    UnionPath(final UnionFileSystem fileSystem, final String... pathParts) {
        this.fileSystem = fileSystem;
        if (pathParts.length == 0) {
            this.absolute = false;
            this.pathParts = new String[]{ "" };
            this.empty = true;
        } else {
            final var longstring = Arrays.stream(pathParts).filter(part -> !part.isEmpty()).collect(Collectors.joining(this.getFileSystem().getSeparator()));
            this.absolute = longstring.startsWith(this.getFileSystem().getSeparator());
            this.pathParts = getPathParts(this.absolute, longstring);
            this.empty = !this.absolute && this.pathParts.length == 1 && this.pathParts[0].isEmpty();
        }
        this.normalized = null;
    }

    // Private constructor only for known correct split and extra value for absolute
    private UnionPath(final UnionFileSystem fileSystem, boolean absolute, final String... pathParts) {
        this(fileSystem, absolute, false, pathParts);
    }
    
    private UnionPath(final UnionFileSystem fileSystem, boolean absolute, boolean isNormalized, final String... pathParts) {
        this.fileSystem = fileSystem;
        this.absolute = absolute;
        this.pathParts = pathParts;
        this.empty = !this.absolute && this.pathParts.length == 1 && this.pathParts[0].isEmpty();
        if (isNormalized)
            this.normalized = this;
        else
            this.normalized = null;
    }

    private String[] getPathParts(final boolean isAbsolute, final String longstring) {
        var sep = "(?:" + Pattern.quote(this.getFileSystem().getSeparator()) + ")";
        String pathname = longstring
                .replace("\\", this.getFileSystem().getSeparator())
                // remove separators from start and end of longstring
                .replaceAll("^" + sep + "*|" + sep + "*$", "")
                // Remove duplicate separators
                .replaceAll(sep + "+(?=" + sep + ")", "");
        if (pathname.isEmpty())
            return isAbsolute ? new String[0] : new String[]{ "" };
        else
            return pathname.split(this.getFileSystem().getSeparator());
    }

    @Override
    public UnionFileSystem getFileSystem() {
        return this.fileSystem;
    }

    @Override
    public boolean isAbsolute() {
        return this.absolute;
    }

    @Override
    public Path getRoot() {
        if (!this.absolute)
            return null;
        return this.fileSystem.getRoot();
    }
    
    @Override
    public Path getFileName() {
        if (this.empty) {
            return null;
        } else if (this.pathParts.length > 0) {
            return new UnionPath(this.getFileSystem(), false, this.pathParts[this.pathParts.length - 1]);
        } else {
            return this.absolute ? null : new UnionPath(this.fileSystem, false);
        }
    }

    @Override
    public Path getParent() {
        if (this.pathParts.length > 1 || (this.absolute && this.pathParts.length == 1)) {
            return new UnionPath(this.fileSystem, this.absolute, Arrays.copyOf(this.pathParts,this.pathParts.length - 1));
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
        return new UnionPath(this.fileSystem, false, this.pathParts[index]);
    }

    @Override
    public UnionPath subpath(final int beginIndex, final int endIndex) {
        if (!this.absolute && this.pathParts.length == 0 && beginIndex == 0 && endIndex == 1)
            return new UnionPath(this.fileSystem, false);
        if (beginIndex < 0 || beginIndex > this.pathParts.length - 1 || endIndex < 0 || endIndex > this.pathParts.length || beginIndex >= endIndex) {
            throw new IllegalArgumentException("Out of range "+beginIndex+" to "+endIndex+" for length "+this.pathParts.length);
        }
        if (!this.absolute && beginIndex == 0 && endIndex == this.pathParts.length) {
            return this;
        }
        return new UnionPath(this.fileSystem, false, Arrays.copyOfRange(this.pathParts, beginIndex, endIndex));
    }

    @Override
    public boolean startsWith(final Path other) {
        if (other.getFileSystem() != this.getFileSystem()) {
            return false;
        }
        if (other instanceof UnionPath bp) {
            if (this.absolute != bp.absolute)
                return false;
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
            if (!this.absolute && bp.absolute)
                return false;
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
        if (normalized != null)
            return normalized;
        Deque<String> normpath = new ArrayDeque<>();
        for (String pathPart : this.pathParts) {
            switch (pathPart) {
                case ".":
                    break;
                case "..":
                    if (!this.absolute && (normpath.isEmpty() || normpath.getLast().equals(".."))) {
                        // .. on an empty path is allowed as long as it is not absolute, so keep it
                        normpath.addLast(pathPart);
                    } else if (!normpath.isEmpty()) {
                        normpath.removeLast();
                    }
                    break;
                default:
                    normpath.addLast(pathPart);
                    break;
            }
        }
        normalized = new UnionPath(this.fileSystem, this.absolute, true, normpath.toArray(new String[0]));
        return normalized;
    }

    @Override
    public Path resolve(final Path other) {
        if (other instanceof UnionPath path) {
            if (path.isAbsolute() || this.empty) {
                return path;
            }
            if (path.empty) {
                return this;
            }
            String[] mergedParts = new String[this.pathParts.length + path.pathParts.length];
            System.arraycopy(this.pathParts, 0, mergedParts, 0, this.pathParts.length);
            System.arraycopy(path.pathParts, 0, mergedParts, this.pathParts.length, path.pathParts.length);
            return new UnionPath(this.fileSystem, this.absolute, mergedParts);
        }
        return other;
    }

    @Override
    public Path relativize(final Path other) {
        if (other.getFileSystem()!=this.getFileSystem()) throw new IllegalArgumentException("Wrong filesystem");
        if (other instanceof UnionPath p) {
            if (this.absolute != p.absolute) {
                throw new IllegalArgumentException("Different types of path");
            }
            var length = Math.min(this.pathParts.length, p.pathParts.length);
            int i = 0;
            while (i < length) {
                if (!Objects.equals(this.pathParts[i], p.pathParts[i]))
                    break;
                i++;
            }

            var remaining = this.pathParts.length - i;
            if (remaining == 0 && i == p.pathParts.length) {
                return new UnionPath(this.getFileSystem(), false);
            } else if (remaining == 0) {
                return p.subpath(i, p.getNameCount());
            } else {
                var updots = IntStream.range(0, remaining).mapToObj(idx -> "..").toArray(String[]::new);
                if (i == p.pathParts.length) {
                    return new UnionPath(this.getFileSystem(), false, updots);
                } else {
                    var subpath = p.subpath(i, p.getNameCount());
                    String[] mergedParts = new String[updots.length + subpath.pathParts.length];
                    System.arraycopy(updots, 0, mergedParts, 0, updots.length);
                    System.arraycopy(subpath.pathParts, 0, mergedParts, updots.length, subpath.pathParts.length);
                    return new UnionPath(this.getFileSystem(), false, mergedParts);
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
        return this.toAbsolutePath().normalize();
    }

    @Override
    public WatchKey register(final WatchService watcher, final WatchEvent.Kind<?>[] events, final WatchEvent.Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(final Path other) {
        if (other instanceof UnionPath path) {
            if (this.absolute && !path.absolute)
                return 1;
            else if (!this.absolute && path.absolute)
                return -1;
            else
                return Arrays.compare(this.pathParts, path.pathParts);
        } else {
            return 0;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof UnionPath p) {
            return p.getFileSystem() == this.getFileSystem() && this.absolute == p.absolute && Arrays.equals(this.pathParts, p.pathParts);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.fileSystem) + 31 * Arrays.hashCode(this.pathParts);
    }

    @Override
    public String toString() {
        return (this.absolute ? fileSystem.getSeparator() : "") + String.join(fileSystem.getSeparator(), this.pathParts);
    }
}
