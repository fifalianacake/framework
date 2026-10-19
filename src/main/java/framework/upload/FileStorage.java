package framework.upload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

import framework.config.FrameworkConfig;

/**
 * Saves uploaded files in the folder configured by upload.dir in
 * application.properties, and reads them back.
 * Controllers receive it by declaring a FileStorage parameter.
 */
public class FileStorage {

    private final Path root;
    private final long maxSize;
    private final Set<String> allowedExtensions;

    public FileStorage(Path root, long maxSize, Set<String> allowedExtensions) {
        this.root = root.toAbsolutePath().normalize();
        this.maxSize = maxSize;
        this.allowedExtensions = allowedExtensions;
    }

    public static FileStorage from(FrameworkConfig config) {
        String dir = config.get("upload.dir", System.getProperty("java.io.tmpdir") + "/uploads");
        long maxSize = config.getLong("upload.max-size", 5L * 1024 * 1024);
        Set<String> allowed = config.getLowerCaseSet("upload.allowed-extensions", "jpg,jpeg,png,gif,webp");
        return new FileStorage(Paths.get(dir), maxSize, allowed);
    }

    /** Validates and writes the file under a unique name; returns that name. */
    public String save(UploadedFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file was uploaded.");
        }
        if (!allowedExtensions.isEmpty() && !allowedExtensions.contains(file.getExtension())) {
            throw new IllegalArgumentException("File type ." + file.getExtension()
                    + " is not allowed. Allowed: " + allowedExtensions);
        }
        if (maxSize > 0 && file.getSize() > maxSize) {
            throw new IllegalArgumentException("File is too large (" + file.getSize()
                    + " bytes, max " + maxSize + ").");
        }

        Files.createDirectories(root);
        String storedName = UUID.randomUUID() + "." + file.getExtension();
        Files.write(resolveInsideRoot(storedName), file.getContent());
        file.setStoredName(storedName);
        return storedName;
    }

    /** Reads a previously saved file back, e.g. to send it to the browser. */
    public UploadedFile load(String storedName) throws IOException {
        Path path = resolveInsideRoot(storedName);
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("File not found: " + storedName);
        }
        UploadedFile file = new UploadedFile(storedName, Files.probeContentType(path), Files.readAllBytes(path));
        file.setStoredName(storedName);
        return file;
    }

    public Path getRoot() {
        return root;
    }

    /** Blocks names like "../../etc/passwd" from escaping the upload folder. */
    private Path resolveInsideRoot(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name is required.");
        }
        Path target = root.resolve(fileName).normalize();
        if (!target.getParent().equals(root)) {
            throw new IllegalArgumentException("Invalid file name: " + fileName);
        }
        return target;
    }
}
