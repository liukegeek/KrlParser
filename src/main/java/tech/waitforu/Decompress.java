package tech.waitforu;

/**
 * ClassName: Decompress
 * Package: tech.waitforu
 * Description:
 * Author: LiuKe
 * Create: 2025/11/10 14:12
 * Version 1.0
 */

import tech.waitforu.pojo.Config;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.nio.file.FileSystems.newFileSystem;

/**
 * 解压压缩包，并获取压缩包中的信息。
 */
public class Decompress {
    private final List<String> pathList = new ArrayList<>();

    private final Map<String, String> pathMap = new HashMap<>();

    public Decompress(String filePath, IgnoreRule fileIgnoreRule) {
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
                        .filter(path -> !fileIgnoreRule.isIgnore(path)) // 过滤出符合规则的文件，即不是应被忽略的文件。
                        .forEach(
                                path -> {
                                    try {
                                        String str = Files.readString(path);
                                        pathMap.put(path.toString(), str);
                                        pathList.add(path.toString());
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

    public String getFileContent(String path) {
        return pathMap.get(path);
    }

    /**
     * 获取解压后的所有文件路径。
     *
     * @return 所有文件路径的列表。
     */
    public List<String> getPathList() {
        return pathList;
    }

    public static void main(String[] args) {
        YamlConfigService yamlConfigService = new YamlConfigService("/Users/liuke/IdeaProjects/KRLParser/src/main/resources/config.yml");
        Config config = yamlConfigService.loadConfig(new Config());
        IgnoreRule ignoreRule = new IgnoreRule(config.fileLoadSection);

        Decompress decompress = new Decompress("/Desktop/EC010_L1.zip", ignoreRule);
        decompress.getPathList().forEach(System.out::println);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(decompress.getFileContent("/am.ini"));
    }

}
