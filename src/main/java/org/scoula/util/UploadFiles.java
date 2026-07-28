package org.scoula.util;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;

public class UploadFiles {
    public static String upload(String baseDir, MultipartFile part) throws IOException {
        // 기본 디렉토리가 있는지 확인, 없으면 새로 생성
        File base = new File(baseDir);
        if (!base.exists()) {
            base.mkdirs();  // 중간에 존재하지 않는 디렉토리까지 모두 생성
        }

        String fileName = part.getOriginalFilename();
        File dest = new File(baseDir, UploadFileName.getUniqueName(fileName));
        part.transferTo(dest);      // 지정한 경로로 업로드 파일 이동
        return dest.getPath();      // 저장된 파일 경로 리턴
    }

    public static String getFormatSize(Long size) {
        if (size <= 0)
            return "0";
        final String[] units = new String[]{"Bytes", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static void download(HttpServletResponse response, File file, String orgName) throws Exception {
        String path = file.getPath();
        if (path.contains("public/") || path.contains("avatar/")) {
            // S3 처리 (간이 방식: 경로 패턴으로 판별)
            S3Service s3Service = BeanUtils.getBean(S3Service.class);
            try (software.amazon.awssdk.core.ResponseInputStream<software.amazon.awssdk.services.s3.model.GetObjectResponse> s3Stream = s3Service.download(path)) {
                String filename = URLEncoder.encode(orgName, "UTF-8");
                response.setContentType("application/download");
                response.setHeader("Content-Disposition", "attachment;filename=\"" + filename + "\"");
                response.setContentLengthLong(s3Stream.response().contentLength());
                s3Stream.transferTo(response.getOutputStream());
                return;
            }
        }
        
        response.setContentType("application/download");
        response.setContentLength((int) file.length());

        String filename = URLEncoder.encode(orgName, "UTF-8");  // 한글 파일명인 경우 인코딩 필수
        response.setHeader("Content-Disposition", "attachment;filename=\"" + filename + "\"");

        try (OutputStream os = response.getOutputStream();
             BufferedOutputStream bos = new BufferedOutputStream(os)) {
            Files.copy(Paths.get(file.getPath()), bos);
        }
    }

    public static void downloadImage(HttpServletResponse response, File file) {
        try {
            String pathStr = file.getPath();
            if (pathStr.contains("public/") || pathStr.contains("avatar/") || !new File(pathStr).exists()) {
                // S3 또는 존재하지 않는 로컬 파일 (S3 key일 가능성)
                try {
                    S3Service s3Service = BeanUtils.getBean(S3Service.class);
                    try (software.amazon.awssdk.core.ResponseInputStream<software.amazon.awssdk.services.s3.model.GetObjectResponse> s3Stream = s3Service.download(pathStr)) {
                        response.setContentType(s3Stream.response().contentType());
                        response.setContentLengthLong(s3Stream.response().contentLength());
                        s3Stream.transferTo(response.getOutputStream());
                        return;
                    }
                } catch (Exception e) {
                    // S3에도 없으면 기본 로직으로 진행 (에러 발생)
                }
            }

            Path path = Paths.get(file.getPath());
            String mimeType = Files.probeContentType(path);
            response.setContentType(mimeType);
            response.setContentLength((int) file.length());

            try (OutputStream os = response.getOutputStream();
                 BufferedOutputStream bos = new BufferedOutputStream(os)) {
                Files.copy(path, bos);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
