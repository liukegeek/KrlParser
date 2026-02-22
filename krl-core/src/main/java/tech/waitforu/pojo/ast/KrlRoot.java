package tech.waitforu.pojo.ast;

import tech.waitforu.pojo.krl.KrlFile;

import java.util.ArrayList;
import java.util.List;

/**
 * KRL AST 根节点。
 * <p>
 * 包含文件头控制行与主体节点。
 */
public class KrlRoot extends AbstractAstNode {
    /** 文件头控制行列表。 */
    private final List<KrlControlLine> krlControlLineList = new ArrayList<>();
    /** 文件主体节点。 */
    private KrlBody body;


    /**
     * 构建根节点。
     *
     * @param startIndex 起始索引
     * @param endLine 结束索引
     * @param krlFile 所属文件
     */
    public KrlRoot(int startIndex, int endLine, KrlFile krlFile) {
        super(startIndex, endLine, krlFile);
    }

    /**
     * 获取控制行列表。
     *
     * @return 控制行列表拷贝
     */
    public List<KrlControlLine> getKrlControlLineList() {
        return new ArrayList<>(krlControlLineList);
    }

    /**
     * 添加控制行并建立 AST 子节点关系。
     *
     * @param krlControlLine 控制行节点
     */
    public void addKrlControlLine(KrlControlLine krlControlLine) {
        krlControlLineList.add(krlControlLine);
        addChild(krlControlLine);
    }

    /**
     * 获取主体节点。
     *
     * @return 主体节点
     */
    public KrlBody getBody() {
        return body;
    }

    /**
     * 设置主体节点并更新父子关系。
     *
     * @param body 主体节点
     */
    public void setBody(KrlBody body) {
        // 移除旧的 body 节点。
        removeChild(body);
        this.body = body;
        addChild(body);
    }

    /**
     * 获取模块名（代理到 body）。
     *
     * @return 模块名
     */
    public String getModuleName() {
        return body.getModuleName();
    }

    /**
     * 设置模块名（代理到 body）。
     *
     * @param moduleName 模块名
     */
    public void setModuleName(String moduleName) {
        body.setModuleName(moduleName);
    }
}
