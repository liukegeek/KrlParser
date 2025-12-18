package tech.waitforu;

/**
 * ClassName: KrlZipLoader
 * Package: tech.waitforu
 * Description: 解压压缩包，并获取压缩包中的信息。
 * Author: LiuKe
 * Create: 2025/11/10 14:12
 * Version 1.0
 */

import tech.waitforu.pojo.Config;
import tech.waitforu.pojo.IgnoreRuleByStr;
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
import java.util.stream.Stream;

import static java.nio.file.FileSystems.newFileSystem;


public class KrlZipLoader {

    // 压缩包中包含的所有KRL文件路径列表
    private final List<String> krlFileList = new ArrayList<>();

    // 压缩包中包含的所有KRL文件路径与KrlFile对象的映射关系
    private final Map<String, KrlFile> krlFileMap = new HashMap<>();

    private  IgnoreRuleByStr fileIgnoreRuleByStr;

    public KrlZipLoader(String filePath, IgnoreRuleByStr fileIgnoreRuleByStr) {
        this.fileIgnoreRuleByStr = fileIgnoreRuleByStr;

        String userHome = System.getProperty("user.home");

        // 压缩包路径
        Path zipPath = Paths.get(userHome, filePath);

        //打开zipPath这一.zip文件，并将其内部视为一个可以独立操作的根目录和文件结构，
        //从而可以通过 Java 代码像访问本地文件夹一样访问 .zip 里的内容。
        try (FileSystem fs = newFileSystem(zipPath)) {

            //用于将一个或多个字符串组件组合起来，并在当前文件系统的上下文中，将其解析为一个不可变的 Path 对象。
            //即这里将字符串"/"转换为一个 Path 对象，代表压缩包的根目录
            Path root = fs.getPath("/");

            try (Stream<Path> pathStream = Files.walk(root)) {//递归遍历文件树，从指定的起始路径开始，包括所有子目录和文件。它返回一个包含所有遍历到的路径的Stream<Path>，方便进行后续处理。
                pathStream.filter(Files::isRegularFile)    // 过滤出普通文件，而不是文件夹。
                        .filter(path -> !fileIgnoreRuleByStr.isIgnore(path.toString())) // 过滤出符合规则的文件，即不是应被忽略的文件。
                        .forEach(
                                path -> {
                                    try {
                                        String content = Files.readString(path, StandardCharsets.ISO_8859_1);
                                        BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
                                        // 获取时间，并解析成我们指定的格式。
                                        String createTime = this.formatTime(attr.creationTime());
                                        String modifyTime = this.formatTime(attr.lastModifiedTime());
                                        long size = attr.size();

                                        krlFileList.add(path.toString());
                                        krlFileMap.put(path.toString(), new KrlFile(path.toString(), createTime, modifyTime, size, content));

                                    } catch (Exception e) {
                                        throw new RuntimeException("读取文件" + path + "时出错", e);
                                    }
                                }
                        );
            }

        } catch (Exception e1) {
            e1.printStackTrace();
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

    public IgnoreRuleByStr getFileIgnoreRuleByStr() {
        return fileIgnoreRuleByStr;
    }

    public void setFileIgnoreRuleByStr(IgnoreRuleByStr fileIgnoreRuleByStr) {
        this.fileIgnoreRuleByStr = fileIgnoreRuleByStr;
    }

    private String formatTime(FileTime fileTime) {
        return LocalDateTime.parse(
                fileTime.toString(),
                DateTimeFormatter.ISO_DATE_TIME
        ).format(DateTimeFormatter.ofPattern("yyyy年MM月dd日','HH:mm"));
    }


    public static void main(String[] args) {
        YamlConfigService yamlConfigService = new YamlConfigService("/Users/liuke/IdeaProjects/KRLParser/src/main/resources/config.yml");
        Config config = yamlConfigService.loadConfig(new Config());
        IgnoreRuleByStr ignoreRuleByStr = new IgnoreRuleByStr(config.fileLoadSection);

        KrlZipLoader krlZipLoader = new KrlZipLoader("/Desktop/EC010_L1.zip", ignoreRuleByStr);
        krlZipLoader.getPathList().forEach(System.out::println);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("========");
        System.out.println(krlZipLoader.getFile("/am.ini").getContent());
    }

}