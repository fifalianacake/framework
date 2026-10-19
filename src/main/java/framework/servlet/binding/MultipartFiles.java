package framework.servlet.binding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import framework.upload.UploadedFile;

final class MultipartFiles {

    private static final String CACHE_ATTRIBUTE = MultipartFiles.class.getName();

    private MultipartFiles() {
    }

    static boolean isMultipart(HttpServletRequest req) {
        String type = req.getContentType();
        return type != null && type.toLowerCase(Locale.ROOT).startsWith("multipart/form-data");
    }

    @SuppressWarnings("unchecked")
    static Map<String, List<UploadedFile>> of(HttpServletRequest req) {
        if (!isMultipart(req)) {
            return Collections.emptyMap();
        }
        Object cached = req.getAttribute(CACHE_ATTRIBUTE);
        if (cached != null) {
            return (Map<String, List<UploadedFile>>) cached;
        }

        Map<String, List<UploadedFile>> files = new LinkedHashMap<>();
        try {
            for (Part part : req.getParts()) {
                if (part.getSubmittedFileName() == null) {
                    continue; 
                }
                UploadedFile file = UploadedFile.fromPart(part);
                if (file != null) {
                    files.computeIfAbsent(part.getName(), k -> new ArrayList<>()).add(file);
                }
            }
        } catch (IOException | ServletException e) {
            throw new IllegalStateException("Unable to read uploaded files. Is the servlet configured "
                    + "with @MultipartConfig or <multipart-config> in web.xml?", e);
        }
        req.setAttribute(CACHE_ATTRIBUTE, files);
        return files;
    }

    static UploadedFile first(HttpServletRequest req, String fieldName) {
        List<UploadedFile> list = of(req).get(fieldName);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    static UploadedFile[] all(HttpServletRequest req, String fieldName) {
        List<UploadedFile> list = of(req).get(fieldName);
        return list == null ? new UploadedFile[0] : list.toArray(new UploadedFile[0]);
    }
}
