package com.github.ryan.facility.file;

import com.github.ryan.facility.error.FacilityErrorType;
import com.github.ryan.facility.error.WrappedError;
import com.github.ryan.facility.log.LogUtil;
import com.github.ryan.facility.pattern.RegPatternUtil;
import com.github.ryan.facility.result.Result;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * <b>文件处理工具类</b>
 * <p>
 * 提供文件上传、下载、类型检测、路径安全、哈希计算等功能。
 * 使用 Apache Tika 进行精确的文件类型检测（基于魔数）。
 * 所有可能失败的操作返回 {@link Result} 类型，确保错误处理的健壮性。
 * </p>
 *
 * <h3>设计原则：</h3>
 * <ul>
 *     <li><b>安全优先</b>：路径穿越检查、文件名清洗、白名单校验</li>
 *     <li><b>类型安全</b>：使用 Tika 进行魔数检测，不依赖扩展名</li>
 *     <li><b>健壮性</b>：返回 Result 类型，明确错误处理</li>
 *     <li><b>零依赖异常</b>：不抛出受检异常，全部封装到 Result</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 安全保存文件
 * Result<Path, WrappedError> result = FileUtil.saveFile(multipartFile, "/upload")
 *     .peek(path -> log.info("保存成功: {}", path))
 *     .peekErr(err -> log.error("保存失败: {}", err.getException()));
 *
 * // 精确类型检测
 * boolean isImage = FileUtil.isImage(file);  // 基于魔数
 * String mimeType = FileUtil.detectMimeType(file).orElse("application/octet-stream");
 *
 * // 文件哈希
 * Result<String, WrappedError> md5 = FileUtil.md5(file);
 * }</pre>
 *
 * @author ryan
 * @since 1.0
 */
public final class FileUtil {

    // ==================== 常量定义 ====================

    /**
     * 默认最大文件大小：100MB
     */
    public static final long DEFAULT_MAX_SIZE = 100 * 1024 * 1024;
    /**
     * Tika 实例（线程安全）
     */
    private static final Tika TIKA = new Tika();
    /**
     * MimeTypes 实例
     */
    private static final MimeTypes MIME_TYPES = MimeTypes.getDefaultMimeTypes();
    /**
     * 常用图片 MIME 类型
     */
    private static final Set<String> IMAGE_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/bmp",
            "image/webp", "image/svg+xml", "image/tiff", "image/x-icon"
    );

    /**
     * 常用文档 MIME 类型
     */
    private static final Set<String> DOCUMENT_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain", "text/csv"
    );

    /**
     * 常用视频 MIME 类型
     */
    private static final Set<String> VIDEO_MIME_TYPES = Set.of(
            "video/mp4", "video/mpeg", "video/quicktime", "video/x-msvideo",
            "video/x-flv", "video/webm", "video/x-matroska"
    );

    /**
     * 常用音频 MIME 类型
     */
    private static final Set<String> AUDIO_MIME_TYPES = Set.of(
            "audio/mpeg", "audio/wav", "audio/ogg", "audio/flac",
            "audio/aac", "audio/x-m4a", "audio/webm"
    );

    /**
     * 危险文件扩展名黑名单
     */
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "sh", "ps1", "vbs", "js",
            "jar", "msi", "dll", "com", "scr", "pif"
    );

    private FileUtil() {
    }

    // ==================== 1. 文件类型检测（Tika） ====================

    /**
     * <b>检测文件的 MIME 类型</b>
     * <p>基于文件内容魔数检测，不依赖扩展名。</p>
     *
     * @param file 文件对象
     *
     * @return 检测结果，包含 MIME 类型字符串或错误
     */
    public static Result<String, WrappedError> detectMimeType(File file) {
        if (file == null || !file.exists()) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_NOT_FOUND));
        }
        try {
            String mimeType = TIKA.detect(file);
            return Result.ok(mimeType);
        } catch (IOException e) {
            return Result.err(WrappedError.of(
                    FacilityErrorType.FILE_TYPE_DETECT_ERROR, e, new Object[]{file.getName()}));
        }
    }

    /**
     * <b>检测输入流的 MIME 类型</b>
     *
     * @param inputStream 输入流（必须支持 mark/reset）
     *
     * @return MIME 类型字符串
     */
    public static Result<String, WrappedError> detectMimeType(InputStream inputStream) {
        if (inputStream == null) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_READ_ERROR));
        }
        try {
            String mimeType = TIKA.detect(inputStream);
            return Result.ok(mimeType);
        } catch (IOException e) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_TYPE_DETECT_ERROR, e));
        }
    }

    /**
     * <b>检测字节数组的 MIME 类型</b>
     *
     * @param bytes 文件字节数组
     *
     * @return MIME 类型字符串
     */
    public static String detectMimeType(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "application/octet-stream";
        }
        return TIKA.detect(bytes);
    }

    /**
     * <b>检测 MultipartFile 的 MIME 类型</b>
     */
    public static Result<String, WrappedError> detectMimeType(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_UPLOAD_EMPTY));
        }
        try (InputStream is = new BufferedInputStream(file.getInputStream())) {
            return Result.ok(TIKA.detect(is));
        } catch (IOException e) {
            return Result.err(WrappedError.of(
                    FacilityErrorType.FILE_TYPE_DETECT_ERROR, e, new Object[]{file.getOriginalFilename()}));
        }
    }

    /**
     * <b>根据 MIME 类型获取建议的文件扩展名</b>
     *
     * @param mimeType MIME 类型
     *
     * @return 扩展名（带点），如 ".jpg"
     */
    public static Optional<String> getExtensionByMimeType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return Optional.empty();
        }
        try {
            MimeType type = MIME_TYPES.forName(mimeType);
            return Optional.ofNullable(type.getExtension())
                    .filter(StringUtils::hasText);
        } catch (MimeTypeException e) {
            return Optional.empty();
        }
    }

    // ==================== 2. 文件类型判断 ====================

    /**
     * <b>判断文件是否为图片</b>
     * <p>基于文件魔数检测，而非扩展名。</p>
     */
    public static boolean isImage(File file) {
        return detectMimeType(file)
                .map(IMAGE_MIME_TYPES::contains)
                .orElse(false);
    }

    /**
     * <b>判断文件是否为图片</b>
     */
    public static boolean isImage(MultipartFile file) {
        return detectMimeType(file)
                .map(IMAGE_MIME_TYPES::contains)
                .orElse(false);
    }

    /**
     * <b>判断文件是否为文档</b>
     */
    public static boolean isDocument(File file) {
        return detectMimeType(file)
                .map(DOCUMENT_MIME_TYPES::contains)
                .orElse(false);
    }

    /**
     * <b>判断文件是否为视频</b>
     */
    public static boolean isVideo(File file) {
        return detectMimeType(file)
                .map(VIDEO_MIME_TYPES::contains)
                .orElse(false);
    }

    /**
     * <b>判断文件是否为音频</b>
     */
    public static boolean isAudio(File file) {
        return detectMimeType(file)
                .map(AUDIO_MIME_TYPES::contains)
                .orElse(false);
    }

    /**
     * <b>判断文件类型是否在允许列表中</b>
     *
     * @param file             文件
     * @param allowedMimeTypes 允许的 MIME 类型列表
     */
    public static boolean isMimeTypeAllowed(File file, Set<String> allowedMimeTypes) {
        if (allowedMimeTypes == null || allowedMimeTypes.isEmpty()) {
            return true;
        }
        return detectMimeType(file)
                .map(allowedMimeTypes::contains)
                .orElse(false);
    }

    /**
     * <b>判断文件扩展名是否危险</b>
     */
    public static boolean isDangerousExtension(String fileName) {
        String ext = getExtension(fileName);
        return ext != null && DANGEROUS_EXTENSIONS.contains(ext.toLowerCase());
    }

    // ==================== 3. MultipartFile 上传与存储 ====================

    /**
     * <b>保存上传文件到指定目录</b>
     * <p>包含路径安全检查、目录自动创建。</p>
     *
     * @param file     上传的文件对象
     * @param destPath 目标目录路径
     *
     * @return 保存后的完整文件路径
     */
    public static Result<Path, WrappedError> saveFile(MultipartFile file, String destPath) {
        return saveFile(file, destPath, null);
    }

    /**
     * <b>保存上传文件，支持自定义文件名</b>
     *
     * @param file           上传的文件
     * @param destPath       目标目录
     * @param customFileName 自定义文件名（传 null 则使用原始名）
     */
    public static Result<Path, WrappedError> saveFile(MultipartFile file, String destPath, String customFileName) {
        // 1. 检查文件是否为空
        if (file == null || file.isEmpty()) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_UPLOAD_EMPTY));
        }

        // 2. 文件名处理与安全检查
        String fileName = StringUtils.hasText(customFileName) ? customFileName : file.getOriginalFilename();
        Result<String, WrappedError> cleanResult = sanitizeFileName(fileName);
        if (cleanResult.isErr()) {
            return Result.err(cleanResult.getErr());
        }
        String cleanFileName = cleanResult.get();

        // 3. 检查危险扩展名
        if (isDangerousExtension(cleanFileName)) {
            return Result.err(WrappedError.of(
                    FacilityErrorType.FILE_TYPE_NOT_SUPPORTED, null, new Object[]{cleanFileName}));
        }

        try {
            // 4. 确保目录存在
            Path dir = Paths.get(destPath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            // 5. 路径穿越检查
            Path targetLocation = dir.resolve(cleanFileName).normalize().toAbsolutePath();
            if (!targetLocation.startsWith(dir.toAbsolutePath())) {
                return Result.err(WrappedError.of(
                        FacilityErrorType.FILE_NAME_INVALID, null, new Object[]{cleanFileName}));
            }

            // 6. 执行存储
            file.transferTo(targetLocation);
            return Result.ok(targetLocation);

        } catch (IOException e) {
            return Result.err(WrappedError.of(
                    FacilityErrorType.FILE_WRITE_ERROR, e, new Object[]{cleanFileName}));
        }
    }

    /**
     * <b>保存文件并进行类型校验</b>
     *
     * @param file             上传的文件
     * @param destPath         目标目录
     * @param allowedMimeTypes 允许的 MIME 类型
     */
    public static Result<Path, WrappedError> saveFileWithTypeCheck(
            MultipartFile file, String destPath, Set<String> allowedMimeTypes) {

        if (file == null || file.isEmpty()) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_UPLOAD_EMPTY));
        }

        // 检测并校验文件类型
        Result<String, WrappedError> mimeResult = detectMimeType(file);
        if (mimeResult.isErr()) {
            return Result.err(mimeResult.getErr());
        }

        String mimeType = mimeResult.get();
        if (allowedMimeTypes != null && !allowedMimeTypes.isEmpty() && !allowedMimeTypes.contains(mimeType)) {
            return Result.err(WrappedError.of(
                    FacilityErrorType.FILE_TYPE_NOT_SUPPORTED, null,
                    new Object[]{mimeType, allowedMimeTypes}));
        }

        return saveFile(file, destPath);
    }

    /**
     * <b>保存文件并进行大小校验</b>
     */
    public static Result<Path, WrappedError> saveFileWithSizeCheck(
            MultipartFile file, String destPath, long maxSizeBytes) {

        if (file == null || file.isEmpty()) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_UPLOAD_EMPTY));
        }

        if (file.getSize() > maxSizeBytes) {
            return Result.err(WrappedError.of(
                    FacilityErrorType.FILE_SIZE_EXCEEDED, null,
                    new Object[]{file.getSize(), maxSizeBytes}));
        }

        return saveFile(file, destPath);
    }

    /**
     * <b>MultipartFile 转 Java IO File</b>
     * <p>会产生临时文件，使用完建议删除。</p>
     */
    public static Result<File, WrappedError> toTempFile(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_UPLOAD_EMPTY));
        }

        String fileName = multipartFile.getOriginalFilename();
        String prefix = getFileNameNoEx(fileName);
        String suffix = "." + getExtension(fileName);

        try {
            File tempFile = File.createTempFile(
                    StringUtils.hasText(prefix) ? prefix : "temp",
                    StringUtils.hasText(suffix) ? suffix : ".tmp"
            );
            multipartFile.transferTo(tempFile);
            return Result.ok(tempFile);
        } catch (IOException e) {
            return Result.err(WrappedError.of(
                    FacilityErrorType.FILE_WRITE_ERROR, e, new Object[]{fileName}));
        }
    }

    // ==================== 4. HTTP 下载与预览 ====================

    /**
     * <b>文件下载（Attachment 模式）</b>
     * <p>自动处理中文文件名乱码、Content-Length 等。</p>
     */
    public static Result<Void, WrappedError> download(HttpServletResponse response, File file) {
        return download(response, file, file.getName());
    }

    /**
     * <b>文件下载，支持自定义下载文件名</b>
     */
    public static Result<Void, WrappedError> download(HttpServletResponse response, File file, String downloadName) {
        if (!file.exists()) {
            return Result.err(WrappedError.of(
                    FacilityErrorType.FILE_NOT_FOUND, null, new Object[]{file.getPath()}));
        }

        // 1. 检测并设置 Content-Type
        String mimeType = detectMimeType(file)
                .orElseGet(() -> MediaTypeFactory.getMediaType(downloadName)
                        .orElse(MediaType.APPLICATION_OCTET_STREAM).toString());

        response.setContentType(mimeType);
        response.setContentLengthLong(file.length());

        // 2. 设置 Content-Disposition (RFC 5987 解决中文乱码)
        String encodedFileName = UriUtils.encode(downloadName, StandardCharsets.UTF_8);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encodedFileName + "\"; filename*=utf-8''" + encodedFileName);

        // 3. 流拷贝
        try (InputStream in = new FileInputStream(file);
             ServletOutputStream out = response.getOutputStream()) {
            FileCopyUtils.copy(in, out);
            out.flush();
            return Result.ok();
        } catch (IOException e) {
            LogUtil.debug("Download canceled/failed: {}", e.getMessage());
            return Result.err(WrappedError.of(FacilityErrorType.FILE_READ_ERROR, e, new Object[]{file.getName()}));
        }
    }

    /**
     * <b>在线预览（Inline 模式）</b>
     * <p>适用于图片、PDF 等浏览器可直接打开的文件类型。</p>
     */
    public static Result<Void, WrappedError> preview(HttpServletResponse response, File file) {
        if (!file.exists()) {
            return Result.err(WrappedError.of(
                    FacilityErrorType.FILE_NOT_FOUND, null, new Object[]{file.getPath()}));
        }

        String mimeType = detectMimeType(file)
                .orElseGet(() -> MediaTypeFactory.getMediaType(file.getName())
                        .orElse(MediaType.APPLICATION_OCTET_STREAM).toString());

        response.setContentType(mimeType);
        response.setContentLengthLong(file.length());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline");

        try (InputStream in = new FileInputStream(file);
             ServletOutputStream out = response.getOutputStream()) {
            FileCopyUtils.copy(in, out);
            out.flush();
            return Result.ok();
        } catch (IOException e) {
            LogUtil.debug("Preview failed: {}", e.getMessage());
            return Result.err(WrappedError.of(FacilityErrorType.FILE_READ_ERROR, e, new Object[]{file.getName()}));
        }
    }

    /**
     * <b>下载字节数据</b>
     */
    public static Result<Void, WrappedError> downloadBytes(
            HttpServletResponse response, byte[] data, String fileName, String contentType) {

        if (data == null || data.length == 0) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_READ_ERROR));
        }

        response.setContentType(contentType != null ? contentType : "application/octet-stream");
        response.setContentLength(data.length);

        String encodedFileName = UriUtils.encode(fileName, StandardCharsets.UTF_8);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encodedFileName + "\"; filename*=utf-8''" + encodedFileName);

        try (ServletOutputStream out = response.getOutputStream()) {
            out.write(data);
            out.flush();
            return Result.ok();
        } catch (IOException e) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_WRITE_ERROR, e, new Object[]{fileName}));
        }
    }

    // ==================== 5. 文件名与路径工具 ====================

    /**
     * <b>清洗文件名</b>
     * <p>移除路径穿越字符、特殊字符，确保文件名安全。</p>
     */
    public static Result<String, WrappedError> sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_NAME_INVALID));
        }

        // 清洗路径
        String cleaned = StringUtils.cleanPath(fileName);

        // 检查路径穿越
        if (cleaned.contains("..")) {
            return Result.err(WrappedError.of(
                    FacilityErrorType.FILE_NAME_INVALID, null, new Object[]{fileName}));
        }

        // 只保留文件名部分（去除路径）
        int lastSeparator = Math.max(cleaned.lastIndexOf('/'), cleaned.lastIndexOf('\\'));
        if (lastSeparator >= 0) {
            cleaned = cleaned.substring(lastSeparator + 1);
        }

        // 移除不安全字符
        cleaned = cleaned.replaceAll("[<>:\"/\\\\|?*\\x00-\\x1f]", "_");

        if (!StringUtils.hasText(cleaned)) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_NAME_INVALID));
        }

        return Result.ok(cleaned);
    }

    /**
     * <b>获取文件扩展名</b>
     * <p>不带点，如 "test.jpg" -&gt; "jpg"</p>
     */
    public static String getExtension(String fileName) {
        return StringUtils.getFilenameExtension(fileName);
    }

    /**
     * <b>获取不带扩展名的文件名</b>
     */
    public static String getFileNameNoEx(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return fileName;
        }
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    /**
     * <b>生成基于日期的存储路径</b>
     * <p>用于分散文件存储压力，如 "2023/10/24"</p>
     */
    public static String generateDatePath() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    }

    /**
     * <b>生成唯一文件名</b>
     *
     * @param originalFileName 原始文件名
     *
     * @return UUID 格式的新文件名
     */
    public static String generateUniqueFileName(String originalFileName) {
        String ext = getExtension(originalFileName);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return StringUtils.hasText(ext) ? uuid + "." + ext : uuid;
    }

    /**
     * <b>生成带时间戳的文件名</b>
     */
    public static String generateTimestampFileName(String originalFileName) {
        String ext = getExtension(originalFileName);
        String timestamp = String.valueOf(System.currentTimeMillis());
        return StringUtils.hasText(ext) ? timestamp + "." + ext : timestamp;
    }

    // ==================== 6. 文件哈希计算 ====================

    /**
     * <b>计算文件 MD5 哈希</b>
     */
    public static Result<String, WrappedError> md5(File file) {
        return hash(file, "MD5");
    }

    /**
     * <b>计算文件 SHA-256 哈希</b>
     */
    public static Result<String, WrappedError> sha256(File file) {
        return hash(file, "SHA-256");
    }

    /**
     * <b>计算文件哈希</b>
     *
     * @param file      文件
     * @param algorithm 哈希算法（MD5, SHA-1, SHA-256 等）
     */
    public static Result<String, WrappedError> hash(File file, String algorithm) {
        if (file == null || !file.exists()) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_NOT_FOUND));
        }

        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return Result.ok(bytesToHex(digest.digest()));
        } catch (NoSuchAlgorithmException e) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_HASH_ERROR, e, new Object[]{algorithm}));
        } catch (IOException e) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_HASH_ERROR, e, new Object[]{file.getName()}));
        }
    }

    /**
     * <b>计算字节数组 MD5</b>
     */
    public static Result<String, WrappedError> md5(byte[] data) {
        return hashBytes(data, "MD5");
    }

    /**
     * <b>计算字节数组哈希</b>
     */
    public static Result<String, WrappedError> hashBytes(byte[] data, String algorithm) {
        if (data == null || data.length == 0) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_READ_ERROR));
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            return Result.ok(bytesToHex(digest.digest(data)));
        } catch (NoSuchAlgorithmException e) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_HASH_ERROR, e, new Object[]{algorithm}));
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // ==================== 7. 文件操作 ====================

    /**
     * <b>复制文件</b>
     */
    public static Result<Path, WrappedError> copy(Path source, Path target) {
        if (source == null || !Files.exists(source)) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_NOT_FOUND));
        }
        try {
            Path parent = target.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            return Result.ok(Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING));
        } catch (IOException e) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_COPY_ERROR, e,
                    new Object[]{source.toString(), target.toString()}));
        }
    }

    /**
     * <b>移动文件</b>
     */
    public static Result<Path, WrappedError> move(Path source, Path target) {
        if (source == null || !Files.exists(source)) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_NOT_FOUND));
        }
        try {
            Path parent = target.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            return Result.ok(Files.move(source, target, StandardCopyOption.REPLACE_EXISTING));
        } catch (IOException e) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_COPY_ERROR, e,
                    new Object[]{source.toString(), target.toString()}));
        }
    }

    /**
     * <b>删除文件</b>
     */
    public static Result<Void, WrappedError> delete(Path path) {
        if (path == null || !Files.exists(path)) {
            return Result.ok(); // 不存在视为删除成功
        }
        try {
            Files.delete(path);
            return Result.ok();
        } catch (IOException e) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_DELETE_ERROR, e, new Object[]{path.toString()}));
        }
    }

    /**
     * <b>递归删除目录</b>
     */
    public static Result<Void, WrappedError> deleteDirectory(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return Result.ok();
        }
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                    Files.delete(d);
                    return FileVisitResult.CONTINUE;
                }
            });
            return Result.ok();
        } catch (IOException e) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_DELETE_ERROR, e, new Object[]{dir.toString()}));
        }
    }

    /**
     * <b>创建目录</b>
     */
    public static Result<Path, WrappedError> createDirectories(Path dir) {
        if (dir == null) {
            return Result.err(WrappedError.of(FacilityErrorType.DIRECTORY_CREATE_ERROR));
        }
        try {
            return Result.ok(Files.createDirectories(dir));
        } catch (IOException e) {
            return Result.err(WrappedError.of(FacilityErrorType.DIRECTORY_CREATE_ERROR, e, new Object[]{dir.toString()}));
        }
    }

    // ==================== 8. 文件读写 ====================

    /**
     * <b>读取文件为字节数组</b>
     */
    public static Result<byte[], WrappedError> readBytes(Path path) {
        if (path == null || !Files.exists(path)) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_NOT_FOUND));
        }
        try {
            return Result.ok(Files.readAllBytes(path));
        } catch (IOException e) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_READ_ERROR, e, new Object[]{path.toString()}));
        }
    }

    /**
     * <b>读取文件为字符串</b>
     */
    public static Result<String, WrappedError> readString(Path path) {
        if (path == null || !Files.exists(path)) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_NOT_FOUND));
        }
        try {
            return Result.ok(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_READ_ERROR, e, new Object[]{path.toString()}));
        }
    }

    /**
     * <b>读取文件所有行</b>
     */
    public static Result<List<String>, WrappedError> readLines(Path path) {
        if (path == null || !Files.exists(path)) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_NOT_FOUND));
        }
        try {
            return Result.ok(Files.readAllLines(path, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_READ_ERROR, e, new Object[]{path.toString()}));
        }
    }

    /**
     * <b>写入字节数据到文件</b>
     */
    public static Result<Path, WrappedError> writeBytes(Path path, byte[] data) {
        try {
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            return Result.ok(Files.write(path, data));
        } catch (IOException e) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_WRITE_ERROR, e, new Object[]{path.toString()}));
        }
    }

    /**
     * <b>写入字符串到文件</b>
     */
    public static Result<Path, WrappedError> writeString(Path path, String content) {
        try {
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            return Result.ok(Files.writeString(path, content, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_WRITE_ERROR, e, new Object[]{path.toString()}));
        }
    }

    // ==================== 9. 文件查询 ====================

    /**
     * <b>获取文件大小</b>
     */
    public static Result<Long, WrappedError> size(Path path) {
        if (path == null || !Files.exists(path)) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_NOT_FOUND));
        }
        try {
            return Result.ok(Files.size(path));
        } catch (IOException e) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_READ_ERROR, e, new Object[]{path.toString()}));
        }
    }

    /**
     * <b>计算目录总大小</b>
     */
    public static Result<Long, WrappedError> directorySize(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_NOT_FOUND));
        }
        try {
            AtomicLong size = new AtomicLong(0);
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    size.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }
            });
            return Result.ok(size.get());
        } catch (IOException e) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_READ_ERROR, e, new Object[]{dir.toString()}));
        }
    }

    /**
     * <b>列出目录下的文件</b>
     *
     * @param dir    目录路径
     * @param filter 文件过滤器（可为 null）
     */
    public static Result<List<Path>, WrappedError> listFiles(Path dir, Predicate<Path> filter) {
        if (dir == null || !Files.exists(dir)) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_NOT_FOUND));
        }
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> files = filter != null
                    ? stream.filter(filter).toList()
                    : stream.toList();
            return Result.ok(files);
        } catch (IOException e) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_READ_ERROR, e, new Object[]{dir.toString()}));
        }
    }

    /**
     * <b>递归列出目录下的所有文件</b>
     */
    public static Result<List<Path>, WrappedError> walkFiles(Path dir, Predicate<Path> filter) {
        if (dir == null || !Files.exists(dir)) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_NOT_FOUND));
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(filter != null ? filter : p -> true)
                    .toList();
            return Result.ok(files);
        } catch (IOException e) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_READ_ERROR, e, new Object[]{dir.toString()}));
        }
    }

    public static List<File> searchFilesBySuffix(Path directory, String suffix) {
        return searchFiles(directory, file ->
                Optional.ofNullable(file)
                        .map(File::getName)
                        .map(fileName -> fileName.endsWith(suffix))
                        .orElseGet(() -> Boolean.FALSE)
        );
    }

    public static List<File> searchFilesByPattern(Path directory, String regex) {
        return searchFiles(directory, file -> RegPatternUtil.matches(regex, file.getName()));
    }

    public static List<File> searchFilesByFileType(Path directory, String type) {
        return searchFiles(directory, file -> detectMimeType(file).isOkAnd(type::equals));
    }

    public static List<File> searchFiles(Path directory, Predicate<File> predicate) {
        if (directory == null || !directory.toFile().exists()) {
            return Collections.emptyList();
        }
        if (directory.toFile().isFile() && predicate.test(directory.toFile())) {
            return Collections.singletonList(directory.toFile());
        }
        return List.of(Objects.requireNonNull(directory.toFile().listFiles((dir, name) -> dir.isFile() && predicate.test(dir))));
    }

    // ==================== 10. 压缩功能 ====================

    /**
     * <b>压缩文件列表为 ZIP</b>
     *
     * @param files      要压缩的文件列表
     * @param outputPath 输出 ZIP 文件路径
     */
    public static Result<Path, WrappedError> zipFiles(List<Path> files, Path outputPath) {
        if (files == null || files.isEmpty()) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_READ_ERROR));
        }

        try {
            Path parent = outputPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            try (ZipOutputStream zos = new ZipOutputStream(
                    new BufferedOutputStream(new FileOutputStream(outputPath.toFile())))) {
                for (Path file : files) {
                    if (Files.exists(file) && Files.isRegularFile(file)) {
                        ZipEntry entry = new ZipEntry(file.getFileName().toString());
                        zos.putNextEntry(entry);
                        Files.copy(file, zos);
                        zos.closeEntry();
                    }
                }
            }
            return Result.ok(outputPath);
        } catch (IOException e) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_WRITE_ERROR, e, new Object[]{outputPath.toString()}));
        }
    }

    /**
     * <b>压缩目录为 ZIP</b>
     */
    public static Result<Path, WrappedError> zipDirectory(Path sourceDir, Path outputPath) {
        if (sourceDir == null || !Files.exists(sourceDir)) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_NOT_FOUND));
        }

        try {
            Path parent = outputPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            try (ZipOutputStream zos = new ZipOutputStream(
                    new BufferedOutputStream(new FileOutputStream(outputPath.toFile())))) {
                Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        String entryName = sourceDir.relativize(file).toString().replace("\\", "/");
                        zos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(file, zos);
                        zos.closeEntry();
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            return Result.ok(outputPath);
        } catch (IOException e) {
            return Result.err(WrappedError.of(FacilityErrorType.FILE_WRITE_ERROR, e, new Object[]{outputPath.toString()}));
        }
    }

    // ==================== 11. 安全校验 ====================

    /**
     * <b>校验文件扩展名是否在白名单中</b>
     */
    public static boolean checkExtension(String fileName, String... allowedExtensions) {
        String ext = getExtension(fileName);
        if (!StringUtils.hasText(ext)) {
            return false;
        }
        return Arrays.stream(allowedExtensions)
                .anyMatch(allowed -> allowed.equalsIgnoreCase(ext));
    }

    /**
     * <b>校验文件扩展名是否在白名单中</b>
     */
    public static boolean checkExtension(String fileName, Set<String> allowedExtensions) {
        String ext = getExtension(fileName);
        if (!StringUtils.hasText(ext) || allowedExtensions == null) {
            return false;
        }
        return allowedExtensions.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(ext));
    }

    // ==================== 12. 工具方法 ====================

    /**
     * <b>格式化文件大小</b>
     *
     * @param bytes 字节数
     *
     * @return 可读形式，如 "1.5 MB"
     */
    public static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String[] units = {"KB", "MB", "GB", "TB", "PB"};
        return String.format("%.2f %s", bytes / Math.pow(1024, exp), units[exp - 1]);
    }

    /**
     * <b>解析文件大小字符串</b>
     *
     * @param sizeStr 大小字符串，如 "10MB", "1.5GB"
     *
     * @return 字节数
     */
    public static Optional<Long> parseSize(String sizeStr) {
        if (!StringUtils.hasText(sizeStr)) {
            return Optional.empty();
        }
        try {
            String str = sizeStr.trim().toUpperCase();
            long multiplier = 1;
            if (str.endsWith("KB")) {
                multiplier = 1024;
                str = str.substring(0, str.length() - 2);
            } else if (str.endsWith("MB")) {
                multiplier = 1024 * 1024;
                str = str.substring(0, str.length() - 2);
            } else if (str.endsWith("GB")) {
                multiplier = 1024L * 1024 * 1024;
                str = str.substring(0, str.length() - 2);
            } else if (str.endsWith("TB")) {
                multiplier = 1024L * 1024 * 1024 * 1024;
                str = str.substring(0, str.length() - 2);
            } else if (str.endsWith("B")) {
                str = str.substring(0, str.length() - 1);
            }
            return Optional.of((long) (Double.parseDouble(str.trim()) * multiplier));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}