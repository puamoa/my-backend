package org.scoula.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.util.PresignedUrlService;
import org.scoula.util.S3Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;
    private final PresignedUrlService presignedUrlService;

    /**
     * 파일 업로드 (Multipart — 서버 경유)
     * curl -X POST /api/files/upload -F "file=@test.txt" -F "directory=test"
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "directory", defaultValue = "test") String directory) {

        String key = s3Service.upload(file, directory);
        String url = s3Service.getFileUrl(key);

        Map<String, String> response = new HashMap<>();
        response.put("key", key);
        response.put("url", url);
        return ResponseEntity.ok(response);
    }

    /**
     * 파일 목록 조회
     * curl /api/files?prefix=test/
     */
    @GetMapping
    public ResponseEntity<List<String>> listFiles(
            @RequestParam(value = "prefix", defaultValue = "") String prefix) {
        return ResponseEntity.ok(s3Service.listFiles(prefix));
    }

    /**
     * 파일 삭제
     * curl -X DELETE /api/files?key=test/uuid.txt
     */
    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam("key") String key) {
        s3Service.delete(key);
        return ResponseEntity.noContent().build();
    }

    /**
     * Presigned Upload URL 발급
     * curl -X POST /api/files/presigned-upload -H "Content-Type: application/json" \
     *   -d '{"directory":"test","filename":"hello.txt","contentType":"text/plain"}'
     */
    @PostMapping("/presigned-upload")
    public ResponseEntity<Map<String, String>> presignedUpload(
            @RequestBody Map<String, String> request) {

        String directory = request.get("directory");
        String filename = request.get("filename");
        String contentType = request.get("contentType");

        String uploadUrl = presignedUrlService.generateUploadUrl(directory, filename, contentType);

        Map<String, String> response = new HashMap<>();
        response.put("uploadUrl", uploadUrl);
        return ResponseEntity.ok(response);
    }

    /**
     * Presigned Download URL 발급
     * curl /api/files/presigned-download?key=test/uuid_hello.txt
     */
    @GetMapping("/presigned-download")
    public ResponseEntity<Map<String, String>> presignedDownload(
            @RequestParam("key") String key) {

        String downloadUrl = presignedUrlService.generateDownloadUrl(key);

        Map<String, String> response = new HashMap<>();
        response.put("downloadUrl", downloadUrl);
        return ResponseEntity.ok(response);
    }
}
