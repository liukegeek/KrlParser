package tech.waitforu.pojo.ast;

import tech.waitforu.pojo.krl.KrlFile;

import java.util.ArrayList;
import java.util.List;

/**
 * ClassName: AbstractAstNode
 * Package: tech.waitforu.pojo.ast
 * Description: 抽象语法树节点抽象类  （关于用对象作为参数实例化时的联动问题没有解决，似乎必须深拷贝或者取消setter方法）
 * Author: LiuKe
 * Create: 2025/12/11 17:28
 * Version 1.0
 */
public abstract class AbstractAstNode implements AstNode {
    private int startIndex;
    private int stopIndex;
    private KrlFile krlFile;
    /**
     * 所属的KRL文件
     */
    private AstNode parent;
    private final List<AstNode> children = new ArrayList<>();


    public AbstractAstNode(int startIndex, int stopIndex, KrlFile krlFile) {
        this.startIndex = startIndex;
        this.stopIndex = stopIndex;
        this.krlFile = krlFile;
    }


    protected AbstractAstNode(AstNodeBuilder<?> builder) {
        this.startIndex = builder.startIndex;
        this.stopIndex = builder.stopIndex;
        this.krlFile = builder.krlFile;
    }


    @Override
    public String getTextContent() {
        return krlFile.getContent(startIndex, stopIndex);
    }

    @Override
    public int getStartIndex() {
        return startIndex;
    }

    @Override
    public int getStopIndex() {
        return stopIndex;
    }

    @Override
    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    @Override
    public void setStopIndex(int stopIndex) {
        this.stopIndex = stopIndex;
    }


    @Override
    public AstNode getParent() {
        return parent;
    }


    // 修改AbstractAstNode.setParent方法
    @Override
    public void setParent(AstNode parent) {
        if (this.parent == parent) return;

        // 移除旧父节点的引用
        if (this.parent != null) {
            this.parent.removeChild(this);
        }

        // 设置新父节点
        this.parent = parent;

        // 将当前节点添加到新父节点的子节点列表
        if (parent != null && !parent.getChildren().contains(this)) {
            parent.addChild(this);
        }
    }



    @Override
    public List<AstNode> getChildren() {
        return new ArrayList<>(children);
    }

    @Override
    public AstNode getChild(int index) {
        return children.get(index);
    }

    // 修改AbstractAstNode.addChild方法
    @Override
    public boolean addChild(AstNode child) {
        if (child == null) return false;
        children.add(child);
        child.setParent(this);  // 设置子节点的父节点
        return true;
    }
    
    // 修改AbstractAstNode.removeChild方法
    @Override
    public AstNode removeChild(AstNode child) {
        if (child == null) return null;
        if (children.remove(child)) {
            child.setParent(null);  // 清除子节点的父节点引用
            return child;
        }
        return null;
    }
    

    @Override
    public KrlFile getKrlFile() {
        return krlFile;
    }

    @Override
    public void setKrlFile(KrlFile krlFile) {
        this.krlFile = krlFile;
    }

    /**
     * 递归查找所有指定类型的子节点
     * @param clazz 目标节点类型的Class对象
     * @return 所有符合类型的子节点列表
     * @param <T> 节点类型参数。
     */
    @Override
    public <T extends AstNode> List<T> findNodesByType(Class<T> clazz) {
        // <T extends AstNode>：这是泛型方法的类型参数声明，表示 T 必须是 AstNode 接口的实现类（或子类）。
        // Class<T> clazz：表示方法接受一个 T 类型的 Class 对象，用于在运行时检查节点类型。
        // List<T>：表示方法返回一个元素类型为 T 的列表。这样调用者可以直接获得正确类型的列表，无需手动类型转换。
        List<T> result = new ArrayList<>();
        searchRecursively(this,clazz,result);
        return result;
    }

    // 私有递归辅助方法，用于深度优先搜索
    private <T extends AstNode> void searchRecursively(AstNode curNode,Class<T> clazz,List<T> result){
        // 1. 检查当前节点是否是目标类型（或其子类）
        if(clazz.isInstance(curNode)){
            result.add(clazz.cast(curNode));
        }
        // 2. 递归遍历所有子节点
        for(AstNode child : curNode.getChildren()){
            searchRecursively(child,clazz,result);
        }
    }



    public abstract static class AstNodeBuilder<T extends AstNodeBuilder<T>> {
        protected int startIndex;
        protected int stopIndex;
        protected KrlFile krlFile;

        // 自引用方法，用于返回当前构建器实例，用于链式调用
        protected abstract T self();
        /**
         * 设置起始索引
         * @param startIndex 起始索引
         * @return 当前构建器实例
         */
        public T withStartIndex(int startIndex) {
            this.startIndex = startIndex;
            return self();
        }
        /**
         * 设置结束索引
         * @param stopIndex 结束索引
         * @return 当前构建器实例
         */
        public T withStopIndex(int stopIndex) {
            this.stopIndex = stopIndex;
            return self();
        }
        /**
         * 设置KRL文件
         * @param krlFile KRL文件
         * @return 当前构建器实例
         */
        public T withKrlFile(KrlFile krlFile) {
            this.krlFile = krlFile;
            return self();
        }


        // 抽象构建方法
        public abstract AbstractAstNode build();

    }

}