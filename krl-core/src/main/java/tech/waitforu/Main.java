package tech.waitforu;

import org.ini4j.Ini;
import tech.waitforu.loader.KrlZipLoader;
import tech.waitforu.loader.YamlConfigLoad;
import tech.waitforu.parser.ModuleParser;
import tech.waitforu.parser.ModuleRepository;
import tech.waitforu.pojo.config.Config;
import tech.waitforu.pojo.ast.AstNode;
import tech.waitforu.pojo.krl.KrlFile;
import tech.waitforu.pojo.krl.KrlModule;
import tech.waitforu.rule.IgnoreRuleByStr;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {

        YamlConfigLoad yamlConfigLoad = new YamlConfigLoad("/Users/liuke/IdeaProjects/KRLParser/krl-core/src/main/resources/config.yml");
        Config config = yamlConfigLoad.loadConfig(new Config());
        IgnoreRuleByStr ignoreRuleByStr = new IgnoreRuleByStr(config.getFileLoadSection());

        String robotInfo = config.getRobotInfo().getFilePath();



        String zipFilePath = "/Desktop/EC010_L1.zip";

        KrlZipLoader krlZipLoader = new KrlZipLoader(zipFilePath, ignoreRuleByStr);

        KrlFile robotInfoFile = krlZipLoader.getFile(robotInfo);
        String robotInfoContent = robotInfoFile.getContent();



        Ini ini = new Ini(new StringReader(robotInfoContent));
        String ID = ini.get("Archive","ID");
        System.out.println(ID);


        List<KrlFile> krlFileList = krlZipLoader.getKrlFileList();




        ModuleRepository moduleRepository = new ModuleRepository();
        moduleRepository.assembleFromFileList(krlFileList);


        KrlModule cell = moduleRepository.findByModuleName("cell");

        ModuleParser moduleParser = new ModuleParser(cell);
        AstNode astNode = moduleParser.getSrcAst();

        System.out.println("=========================");

        moduleRepository.getCallableNameList().forEach(
                System.out::println
        );
        System.out.println("=========================");



        System.out.println(astNode);


    }
}
