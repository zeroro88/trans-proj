package com.transproj.service;

import com.transproj.config.AppProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class StoragePaths {

    private final AppProperties appProperties;

    public StoragePaths(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public Path baseDir() {
        return Path.of(appProperties.getStorage().getBaseDir()).toAbsolutePath().normalize();
    }

    public Path uploadsDir() {
        return baseDir().resolve("uploads");
    }

    /** Path stored in DB: relative to baseDir for portability */
    public String toStoredRelativePath(Path absoluteFile) {
        Path rel = baseDir().relativize(absoluteFile.normalize());
        return rel.toString().replace('\\', '/');
    }

    public Path resolveStored(String storedRelativePath) {
        return baseDir().resolve(storedRelativePath).normalize();
    }
}
