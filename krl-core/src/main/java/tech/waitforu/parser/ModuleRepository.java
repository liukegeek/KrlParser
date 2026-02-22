package tech.waitforu.parser;

import tech.waitforu.pojo.krl.KrlFile;
import tech.waitforu.pojo.krl.KrlFileType;
import tech.waitforu.pojo.krl.KrlModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 模块仓库。
 * <p>
 * 维护两类索引：
 * 1. 模块名 -> 模块；
 * 2. 可调用程序名 -> 所在模块。
 * <p>
 * 约束：
 * - 每个模块最多一个 src、一个 dat；
 * - 模块名与文件名一致（不含扩展名）。
 */
public class ModuleRepository {
    private final HashMap<String, KrlModule> moduleMap = new HashMap<>(); // 模块名字到模块的映射
    private final HashMap<String, KrlModule> procedureToModuleMap = new HashMap<>();


    /**
     * 创建空仓库。
     */
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
            // 建立模块主索引。
            moduleMap.put(module.getModuleName(), module);
            // 扫描模块中的可调用程序，建立 callable 反向索引。
            new ModuleParser(module).getCallableList().forEach(callable ->
                    procedureToModuleMap.put(callable.getName().toLowerCase(), module)
            );
        });
    }


    /**
     * 根据文件列表组装模块并加入仓库。
     *
     * @param fileList KRL 文件列表（src/dat）
     */
    public void assembleFromFileList(List<KrlFile> fileList) {
        fileList.forEach(this::addModule);
    }

    /**
     * 获取当前模块名列表。
     *
     * @return 模块名列表
     */
    public List<String> getModuleNameList() {
        List<String> moduleNameList = new ArrayList<>();
        moduleMap.forEach((moduleName, module) -> moduleNameList.add(moduleName));
        return moduleNameList;
    }

    /**
     * 获取 callable 名称列表。
     *
     * @return callable 名称列表
     */
    public List<String> getCallableNameList() {
        List<String> callableNameList = new ArrayList<>();
        procedureToModuleMap.forEach((callableName, module) -> callableNameList.add(callableName));
        return callableNameList;
    }

    /**
     * 获取模块列表。
     *
     * @return 仓库中全部模块
     */
    public List<KrlModule> getModuleList() {
        List<KrlModule> moduleList = new ArrayList<>();
        moduleMap.forEach((moduleName, module) -> moduleList.add(module));
        return moduleList;
    }



    /**
     * 获取模块索引表（模块名 -> 模块）。
     *
     * @return 模块索引表
     */
    public HashMap<String, KrlModule> getModuleMap() {
        return moduleMap;
    }

    /**
     * 按模块名查找模块。
     *
     * @param moduleName 模块名（大小写不敏感）
     * @return 匹配模块；不存在返回 null
     */
    public KrlModule findByModuleName(String moduleName) {
        return moduleMap.get(moduleName.toLowerCase());
    }


    /**
     * 按 callable 名称查找模块。
     *
     * @param callableName callable 名称（大小写不敏感）
     * @return 匹配模块；不存在返回 null
     */
    public KrlModule findByCallableName(String callableName) {
        return procedureToModuleMap.get(callableName.toLowerCase());
    }

    /**
     * 按单个文件增量加入仓库。
     * <p>
     * 只接受 src/dat 文件；其他文件类型忽略。
     *
     * @param krlFile 待加入文件
     */
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

                // src 文件加入后，需要同步刷新 callable 到模块的索引映射。
                module.setModuleSrcFile(krlFile);
                new ModuleParser(module).getCallableList().forEach(callable ->
                        procedureToModuleMap.put(callable.getName().toLowerCase(), module)
                );
                // 如果模块存在，且是 dat 文件，且模块中还没有 dat 文件，则添加。
            } else if (krlFile.getType() == KrlFileType.DAT && findByModuleName(moduleName).getModuleDatFile() == null) {
                findByModuleName(moduleName).setModuleDatFile(krlFile);

            } else {
                throw new RuntimeException("模块" + krlFile.getName() + "已经有了" + krlFile.getType() + "文件，无法重复添加");
            }
        }
    }


    /**
     * 将完整模块加入仓库。
     *
     * @param krlModule 待加入模块
     * @return true=加入成功；false=模块已存在
     */
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
