package framework.upload;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import javax.servlet.http.Part;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class UploadedFile {

    private final String originalFilename; // "holiday.JPG"
    private final String name;             // "holiday"
    private final String extension;        // "jpg" (lower case, no dot)
    private final String contentType;      // "image/jpeg"
    private final byte[] content;
    private String storedName;             
    public UploadedFile(String originalFilename, String contentType, byte[] content) {
        String clean = originalFilename == null ? "" : originalFilename.trim();
        clean = clean.substring(Math.max(clean.lastIndexOf('/'), clean.lastIndexOf('\\')) + 1);
        int dot = clean.lastIndexOf('.');
        this.originalFilename = clean;
        this.name = dot > 0 ? clean.substring(0, dot) : clean;
        this.extension = dot > 0 && dot < clean.length() - 1
                ? clean.substring(dot + 1).toLowerCase(Locale.ROOT)
                : "";
        this.contentType = contentType;
        this.content = content == null ? new byte[0] : content;
    }

    public static UploadedFile fromPart(Part part) throws IOException {
        String submitted = part.getSubmittedFileName();
        if (submitted == null || submitted.isBlank() || part.getSize() == 0) {
            return null;
        }
        try (InputStream in = part.getInputStream()) {
            return new UploadedFile(submitted, part.getContentType(), in.readAllBytes());
        }
    }

    public String getOriginalFilename() { return originalFilename; }
    public String getName() { return name; }
    public String getExtension() { return extension; }
    public String getContentType() { return contentType; }
    public long getSize() { return content.length; }
    public boolean isEmpty() { return content.length == 0; }

    @JsonIgnore
    public byte[] getContent() { return content; }

    public String getStoredName() { return storedName; }
    void setStoredName(String storedName) { this.storedName = storedName; }

    @Override
    public String toString() {
        return "UploadedFile{" + originalFilename + ", " + getSize() + " bytes"
                + (storedName != null ? ", stored as " + storedName : "") + "}";
    }
}