package tech.waitforu.parser;

import org.ini4j.Ini;

import java.io.IOException;
import java.io.StringReader;

/**
 * INI 文本解析器。
 * <p>
 * 用于从机器人备份中的 ini 文本中读取元信息字段。
 */
public class IniParser {
    /** INI4j 解析后的对象。 */
    private Ini ini;

    /**
     * 使用字符串内容构建 INI 解析对象。
     *
     * @param iniContent ini 文本内容
     * @throws IOException ini 语法错误或读取异常时抛出
     */
    public IniParser(String iniContent) throws IOException {
        this.ini = new Ini(new StringReader(iniContent));
    }


    /**
     * 获取INI文件中指定section和key的值
     * @param section 节名
     * @param key 键名
     * @return 值
     */
    public String get(String section, String key){
        return ini.get(section, key);
    }
}
