package tech.waitforu.parser;

import tech.waitforu.pojo.krl.KrlFile;
import tech.waitforu.pojo.krl.KrlFileType;
import tech.waitforu.pojo.krl.KrlModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * ClassName: parser.tech.waitforu.ModuleRepository
 * Package: tech.waitforu.pojo.krl
 * Description: 模块仓库。注意，每个模块只能有一个src文件和一个dat文件，模块名不能重复，必须与src文件或dat文件的文件名相同。
 * Author: LiuKe
 * Create: 2025/12/17 14:57
 * Version 1.0
 */
public class ModuleRepository {
    private final HashMap<String, KrlModule> moduleMap = new HashMap<>(); // 模块名字到模块的映射
    private final HashMap<String, KrlModule> procedureToModuleMap = new HashMap<>();


    public ModuleRepository() {
    }

    /**
     * 从模块列表中创建一个模块仓库
     *
     * @param moduleList 模块列表
     */
    public ModuleRepository(List<KrlModule> moduleList) {
        moduleList.forEach(module ->
        {
            moduleMap.put(module.getModuleName(), module);
            new ModuleParser(module).getCallableList().forEach(callable ->
                    procedureToModuleMap.put(callable.getName().toLowerCase(), module)
            );
        });
    }


    // 根据提供的文件列表，组合成模块并添加进仓库中。将仓库中的模块返回。
    public void assembleFromFileList(List<KrlFile> fileList) {
        fileList.forEach(this::addModule);
    }

    public List<String> getModuleNameList() {
        List<String> moduleNameList = new ArrayList<>();
        moduleMap.forEach((moduleName, module) -> moduleNameList.add(moduleName));
        return moduleNameList;
    }

    public List<String> getCallableNameList() {
        List<String> callableNameList = new ArrayList<>();
        procedureToModuleMap.forEach((callableName, module) -> callableNameList.add(callableName));
        return callableNameList;
    }

    public List<KrlModule> getModuleList() {
        List<KrlModule> moduleList = new ArrayList<>();
        moduleMap.forEach((moduleName, module) -> moduleList.add(module));
        return moduleList;
    }




    public HashMap<String, KrlModule> getModuleMap() {
        return moduleMap;
    }

    //Map中的name全是小写字母，需要先转换成小写再进行查询。
    public KrlModule findByModuleName(String moduleName) {
        return moduleMap.get(moduleName.toLowerCase());
    }


    //Map中的name全是小写字母，需要先转换成小写再进行查询。
    public KrlModule findByCallableName(String callableName) {
        return procedureToModuleMap.get(callableName.toLowerCase());
    }


    public void addModule(KrlFile krlFile) {
        //只有src和dat文件会得到模块。
        if (krlFile.getType() == KrlFileType.SRC || krlFile.getType() == KrlFileType.DAT) {
            //模块名与文件名相同
            String moduleName = krlFile.getName().toLowerCase();

            // 如果模块不存在，则创建一个新模块
            if (findByModuleName(krlFile.getName()) == null) {
                KrlModule module = new KrlModule(moduleName);
                boolean addSuccess = addModule(module);
                if (!addSuccess) {
                    throw new RuntimeException("模块" + moduleName + "已经存在，无法重复添加");
                }
            }


            if (krlFile.getType() == KrlFileType.SRC && findByModuleName(moduleName).getModuleSrcFile() == null) {
                KrlModule module = findByModuleName(moduleName);

                // 为模块中添加src文件，也需要解析出其中的callable并添加到procedureToModuleMap中
                module.setModuleSrcFile(krlFile);
                new ModuleParser(module).getCallableList().forEach(callable ->
                        procedureToModuleMap.put(callable.getName().toLowerCase(), module)
                );
                // 如果模块存在，且是dat文件，且模块中还没有dat文件，则添加dat文件
            } else if (krlFile.getType() == KrlFileType.DAT && findByModuleName(moduleName).getModuleDatFile() == null) {
                findByModuleName(moduleName).setModuleDatFile(krlFile);

            } else {
                throw new RuntimeException("模块" + krlFile.getName() + "已经有了" + krlFile.getType() + "文件，无法重复添加");
            }
        }
    }


    // 将一个模块添加到仓库中。
    public boolean addModule(KrlModule krlModule) {
        //已经存在，则返回false。
        if (moduleMap.containsKey(krlModule.getModuleName())) return false;

        // 否则，添加到仓库中。
        moduleMap.put(krlModule.getModuleName().toLowerCase(), krlModule);
        // 同时，将模块中的callable添加到procedureToModuleMap中。
        new ModuleParser(krlModule).getCallableList().forEach(callable ->
                procedureToModuleMap.put(callable.getName().toLowerCase(), krlModule)
        );
        return true;
    }
}
