package iuh.fit.se.servicemusic.util;

import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

/**
 * Migration tool to fix filenames with special characters in MinIO.
 * This will run on application startup and rename files in the "raw/" folder
 * that contain URL-encoded or Unicode characters.
 *
 * USAGE:
 * 1. Enable this migration by setting: spring.profiles.active=migration
 * 2. Run the application once
 * 3. Disable after migration is complete
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MinioFilenameMigration implements CommandLineRunner {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${migration.enabled:false}")
    private boolean migrationEnabled;

    @Override
    public void run(String... args) throws Exception {
        if (!migrationEnabled) {
            log.info("MinIO filename migration is DISABLED. Set 'migration.enabled=true' to enable.");
            return;
        }

        log.info("========================================");
        log.info("Starting MinIO filename migration...");
        log.info("========================================");

        try {
            migrateRawFolder();
            log.info("========================================");
            log.info("Migration completed successfully!");
            log.info("========================================");
        } catch (Exception e) {
            log.error("Migration failed!", e);
        }
    }

    private void migrateRawFolder() throws Exception {
        log.info("Scanning 'raw/' folder in bucket: {}", bucketName);

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix("raw/")
                        .recursive(true)
                        .build()
        );

        int totalFiles = 0;
        int migratedFiles = 0;
        int skippedFiles = 0;

        for (Result<Item> result : results) {
            Item item = result.get();
            String objectKey = item.objectName();
            totalFiles++;

            log.debug("Processing: {}", objectKey);

            String newObjectKey = sanitizeObjectKey(objectKey);

            if (objectKey.equals(newObjectKey)) {
                log.debug("File already has valid name, skipping: {}", objectKey);
                skippedFiles++;
                continue;
            }

            log.info("Migrating file:");
            log.info("  FROM: {}", objectKey);
            log.info("  TO:   {}", newObjectKey);

            // Copy object with new name
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(newObjectKey)
                            .source(CopySource.builder()
                                    .bucket(bucketName)
                                    .object(objectKey)
                                    .build())
                            .build()
            );

            // Delete old object
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );

            migratedFiles++;
            log.info("✅ Migrated successfully!");
        }

        log.info("========================================");
        log.info("Migration Summary:");
        log.info("  Total files scanned: {}", totalFiles);
        log.info("  Migrated: {}", migratedFiles);
        log.info("  Skipped: {}", skippedFiles);
        log.info("========================================");
    }

    private String sanitizeObjectKey(String objectKey) {
        // Extract folder and filename
        int lastSlashIndex = objectKey.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            return sanitizeFilename(objectKey);
        }

        String folder = objectKey.substring(0, lastSlashIndex + 1); // "raw/"
        String filename = objectKey.substring(lastSlashIndex + 1);  // "uuid_filename.mp3"

        // Extract UUID and actual filename
        int firstUnderscoreIndex = filename.indexOf('_');
        if (firstUnderscoreIndex == -1) {
            return folder + sanitizeFilename(filename);
        }

        String uuid = filename.substring(0, firstUnderscoreIndex);
        String actualFilename = filename.substring(firstUnderscoreIndex + 1);

        String sanitizedFilename = sanitizeFilename(actualFilename);

        return folder + uuid + "_" + sanitizedFilename;
    }

    private String sanitizeFilename(String fileName) {
        // Decode URL encoding if present
        try {
            fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Could not decode filename: {}", fileName);
        }

        // Extract base name and extension
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) {
            return sanitize(fileName);
        }

        String baseName = fileName.substring(0, dotIndex);
        String extension = fileName.substring(dotIndex + 1).toLowerCase();

        // Normalize Unicode characters
        baseName = Normalizer.normalize(baseName, Normalizer.Form.NFD);
        baseName = baseName.replaceAll("\\p{M}", ""); // Remove diacritics

        // Keep only alphanumeric, dots, hyphens, underscores
        baseName = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Remove consecutive underscores
        baseName = baseName.replaceAll("_{2,}", "_");

        // Remove leading/trailing underscores
        baseName = baseName.replaceAll("^_+|_+$", "");

        // Limit length
        if (baseName.length() > 200) {
            baseName = baseName.substring(0, 200);
        }

        return baseName + "." + extension;
    }

    private String sanitize(String text) {
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        text = text.replaceAll("\\p{M}", "");
        text = text.replaceAll("[^a-zA-Z0-9._-]", "_");
        text = text.replaceAll("_{2,}", "_");
        text = text.replaceAll("^_+|_+$", "");
        return text;
    }
}

