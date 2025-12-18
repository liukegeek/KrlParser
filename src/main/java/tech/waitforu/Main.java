package tech.waitforu;

import tech.waitforu.pojo.Config;
import tech.waitforu.pojo.IgnoreRuleByStr;
import tech.waitforu.pojo.ast.AstNode;
import tech.waitforu.pojo.krl.KrlFile;
import tech.waitforu.pojo.krl.KrlModule;

import java.util.List;

public class Main {
    public static void main(String[] args) {

        YamlConfigService yamlConfigService = new YamlConfigService("/Users/liuke/IdeaProjects/KRLParser/src/main/resources/config.yml");
        Config config = yamlConfigService.loadConfig(new Config());
        IgnoreRuleByStr ignoreRuleByStr = new IgnoreRuleByStr(config.fileLoadSection);

        String zipFilePath = "/Desktop/EC010_L1.zip";

        KrlZipLoader krlZipLoader = new KrlZipLoader(zipFilePath, ignoreRuleByStr);

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
