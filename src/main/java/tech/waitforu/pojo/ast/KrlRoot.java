package tech.waitforu.pojo.ast;

import tech.waitforu.pojo.krl.KrlFile;

import java.util.ArrayList;
import java.util.List;

/**
 * ClassName: KrlRoot
 * Package: tech.waitforu.pojo.ast
 * Description:
 * Author: LiuKe
 * Create: 2025/12/12 11:44
 * Version 1.0
 */
public class KrlRoot extends AbstractAstNode {
    private final List<KrlControlLine> krlControlLineList = new ArrayList<>();
    private KrlBody body;


    public KrlRoot(int startIndex, int endLine, KrlFile krlFile) {
        super(startIndex, endLine, krlFile);
    }

    public List<KrlControlLine> getKrlControlLineList() {
        return new ArrayList<>(krlControlLineList);
    }

    public void addKrlControlLine(KrlControlLine krlControlLine) {
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
