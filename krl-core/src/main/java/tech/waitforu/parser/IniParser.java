package tech.waitforu.parser;

import org.ini4j.Ini;

import java.io.IOException;
import java.io.StringReader;

/**
 * ClassName: IniParser
 * Package: tech.waitforu.parser
 * Description: 从INI格式的字符串中解析出包含目标信息的INI对象
 * Author: LiuKe
 * Create: 2025/12/22 21:56
 * Version 1.0
 */
public class IniParser {
    private Ini ini;

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
