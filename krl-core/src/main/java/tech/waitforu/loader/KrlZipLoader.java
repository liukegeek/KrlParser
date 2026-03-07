package tech.waitforu.loader;

import tech.waitforu.exception.KrlInputException;
import tech.waitforu.rule.IgnoreRuleByStr;
import tech.waitforu.pojo.krl.KrlFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.nio.file.FileSystems.newFileSystem;


/**
 * KRL 备份压缩包加载器。
 * <p>
 * 该类将 zip 文件作为文件系统进行遍历，按过滤规则读取符合条件的文件，
 * 并将路径、时间、大小、文本内容封装为 {@link KrlFile}。
 */
public class KrlZipLoader {

    // 压缩包中包含的所有KRL文件路径列表
    private final List<String> krlFileList = new ArrayList<>();

    // 压缩包中包含的所有KRL文件路径与KrlFile对象的映射关系
    private final Map<String, KrlFile> krlFileMap = new HashMap<>();

    /** 文件过滤规则（true=忽略，false=保留）。 */
    private IgnoreRuleByStr fileIgnoreRuleByStr;

    /**
     * 构造加载器并立即执行 zip 扫描。
     *
     * @param filePath zip 文件路径
     * @param fileIgnoreRuleByStr 文件过滤规则
     */
    public KrlZipLoader(String filePath, IgnoreRuleByStr fileIgnoreRuleByStr) {
        if (filePath == null || filePath.isBlank()) {
            throw new KrlInputException("备份压缩包路径不能为空");
        }
        this.fileIgnoreRuleByStr = Objects.requireNonNull(fileIgnoreRuleByStr, "文件过滤规则不能为空");

        // 压缩包路径
        Path zipPath = Paths.get(filePath);

        //打开zipPath这一.zip文件，并将其内部视为一个可以独立操作的根目录和文件结构，
        //从而可以通过 Java 代码像访问本地文件夹一样访问 .zip 里的内容。
        try (FileSystem fs = newFileSystem(zipPath)) {

            //用于将一个或多个字符串组件组合起来，并在当前文件系统的上下文中，将其解析为一个不可变的 Path 对象。
            //即这里将字符串"/"转换为一个 Path 对象，代表压缩包的根目录
            Path root = fs.getPath("/");

            try (Stream<Path> pathStream = Files.walk(root)) {
                // 递归遍历 zip 中全部路径，依次过滤出普通文件与符合规则的文件。
                pathStream.filter(Files::isRegularFile)    // 过滤出普通文件，而不是文件夹。
                        .filter(path -> !fileIgnoreRuleByStr.isIgnore(path.toString())) // 过滤出符合规则的文件，即不是应被忽略的文件。
                        .forEach(this::loadKrlFile);
            }

        } catch (KrlInputException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new KrlInputException("读取备份压缩包失败: " + filePath, exception);
        }

    }

    /**
     * 获取解压后的所有KRL文件对象。
     *
     * @return 所有KRL文件对象的列表。
     */
    public List<KrlFile> getKrlFileList() {
        return krlFileList.stream().map(krlFileMap::get).toList();
    }

    /**
     * 获取解压后的所有KRL文件对象。
     *
     * @param path KRL文件的路径。
     * @return KRL文件对象。
     */
    public KrlFile getFile(String path) {
        return krlFileMap.get(path);
    }

    /**
     * 获取解压后的所有文件路径。
     *
     * @return 所有文件路径的列表。
     */
    public List<String> getPathList() {
        return krlFileList;
    }

    /**
     * 获取当前生效的文件过滤规则。
     *
     * @return 过滤规则对象
     */
    public IgnoreRuleByStr getFileIgnoreRuleByStr() {
        return fileIgnoreRuleByStr;
    }

    /**
     * 将 zip 中的 ISO 时间字符串转换为项目统一显示格式。
     *
     * @param fileTime 文件时间
     * @return 格式化后的时间字符串
     */
    private String formatTime(FileTime fileTime) {
        return LocalDateTime.parse(
                fileTime.toString(),
                DateTimeFormatter.ISO_DATE_TIME
        ).format(DateTimeFormatter.ofPattern("yyyy年MM月dd日','HH:mm"));
    }

    /**
     * 读取单个 zip 条目并写入缓存。
     *
     * @param path zip 内部路径
     */
    private void loadKrlFile(Path path) {
        try {
            // 读取文件文本与基础属性，并缓存到索引结构中。
            String content = Files.readString(path, StandardCharsets.ISO_8859_1);
            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
            String createTime = this.formatTime(attr.creationTime());
            String modifyTime = this.formatTime(attr.lastModifiedTime());
            long size = attr.size();

            krlFileList.add(path.toString());
            krlFileMap.put(path.toString(), new KrlFile(path.toString(), createTime, modifyTime, size, content));
        } catch (Exception exception) {
            throw new KrlInputException("读取备份文件失败: " + path, exception);
        }
    }
}
