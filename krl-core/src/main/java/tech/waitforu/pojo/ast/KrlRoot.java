package tech.waitforu.pojo.ast;

import tech.waitforu.pojo.krl.KrlFile;

import java.util.ArrayList;
import java.util.List;

/**
 * ClassName: ast.pojo.tech.waitforu.KrlRoot
 * Package: tech.waitforu.pojo.ast
 * Description:
 * Author: LiuKe
 * Create: 2025/12/12 11:44
 * Version 1.0
 */
public class KrlRoot extends tech.waitforu.pojo.ast.AbstractAstNode {
    private final List<tech.waitforu.pojo.ast.KrlControlLine> krlControlLineList = new ArrayList<>();
    private KrlBody body;


    public KrlRoot(int startIndex, int endLine, KrlFile krlFile) {
        super(startIndex, endLine, krlFile);
    }

    public List<tech.waitforu.pojo.ast.KrlControlLine> getKrlControlLineList() {
        return new ArrayList<>(krlControlLineList);
    }

    public void addKrlControlLine(tech.waitforu.pojo.ast.KrlControlLine krlControlLine) {
        krlControlLineList.add(krlControlLine);
        addChild(krlControlLine);
    }

    public KrlBody getBody() {
        return body;
    }

    public void setBody(KrlBody body) {
        //移除旧的body节点
        removeChild(body);
        this.body = body;
        addChild(body);
    }

    public String getModuleName() {
        return body.getModuleName();
    }

    public void setModuleName(String moduleName) {
        body.setModuleName(moduleName);
    }
}
