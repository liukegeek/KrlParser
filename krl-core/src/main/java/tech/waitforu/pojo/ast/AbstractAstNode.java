package tech.waitforu.pojo.ast;

import tech.waitforu.pojo.krl.KrlFile;

import java.util.ArrayList;
import java.util.List;

/**
 * AST 节点抽象基类。
 * <p>
 * 提供：
 * 1. 统一的位置信息与文件引用；
 * 2. 父子关系维护；
 * 3. 递归检索工具方法；
 * 4. Builder 基类能力。
 */
public abstract class AbstractAstNode implements AstNode {
    /** 源文本起始索引（含）。 */
    private int startIndex;
    /** 源文本结束索引（含）。 */
    private int stopIndex;
    /** 节点所属 KRL 文件。 */
    private KrlFile krlFile;
    /**
     * 父节点引用。
     */
    private AstNode parent;
    /** 子节点集合。 */
    private final List<AstNode> children = new ArrayList<>();


    /**
     * 通过位置信息直接构建节点。
     *
     * @param startIndex 起始索引
     * @param stopIndex 结束索引
     * @param krlFile 所属文件
     */
    public AbstractAstNode(int startIndex, int stopIndex, KrlFile krlFile) {
        this.startIndex = startIndex;
        this.stopIndex = stopIndex;
        this.krlFile = krlFile;
    }

    /**
     * 通过 Builder 构建节点。
     *
     * @param builder Builder
     */
    protected AbstractAstNode(AstNodeBuilder<?> builder) {
        this.startIndex = builder.startIndex;
        this.stopIndex = builder.stopIndex;
        this.krlFile = builder.krlFile;
    }


    /**
     * 获取当前节点对应原始文本。
     *
     * @return 原始文本片段
     */
    @Override
    public String getTextContent() {
        return krlFile.getContent(startIndex, stopIndex);
    }

    /**
     * 获取起始索引。
     *
     * @return 起始索引
     */
    @Override
    public int getStartIndex() {
        return startIndex;
    }

    /**
     * 获取结束索引。
     *
     * @return 结束索引
     */
    @Override
    public int getStopIndex() {
        return stopIndex;
    }

    /**
     * 设置起始索引。
     *
     * @param startIndex 起始索引
     */
    @Override
    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    /**
     * 设置结束索引。
     *
     * @param stopIndex 结束索引
     */
    @Override
    public void setStopIndex(int stopIndex) {
        this.stopIndex = stopIndex;
    }


    /**
     * 获取父节点。
     *
     * @return 父节点
     */
    @Override
    public AstNode getParent() {
        return parent;
    }


    /**
     * 设置父节点并维护双向关系一致性。
     * <p>
     * 逻辑：
     * 1. 若父节点未变化，直接返回；
     * 2. 若存在旧父节点，先从旧父节点移除；
     * 3. 设置新父节点；
     * 4. 确保新父节点 children 包含当前节点。
     *
     * @param parent 新父节点
     */
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


    /**
     * 获取子节点列表（浅拷贝）。
     *
     * @return 子节点列表
     */
    @Override
    public List<AstNode> getChildren() {
        return new ArrayList<>(children);
    }

    /**
     * 获取指定索引子节点。
     *
     * @param index 子节点索引
     * @return 子节点
     */
    @Override
    public AstNode getChild(int index) {
        return children.get(index);
    }

    /**
     * 添加子节点并维护双向关系一致性。
     *
     * @param child 子节点
     * @return true 表示添加成功
     */
    @Override
    public boolean addChild(AstNode child) {
        if (child == null) return false;
        children.add(child);
        child.setParent(this);  // 设置子节点的父节点
        return true;
    }
    
    /**
     * 移除子节点并清理其父引用。
     *
     * @param child 子节点
     * @return 被移除节点；不存在返回 null
     */
    @Override
    public AstNode removeChild(AstNode child) {
        if (child == null) return null;
        if (children.remove(child)) {
            child.setParent(null);  // 清除子节点的父节点引用
            return child;
        }
        return null;
    }
    

    /**
     * 获取所属 KRL 文件。
     *
     * @return KRL 文件
     */
    @Override
    public KrlFile getKrlFile() {
        return krlFile;
    }

    /**
     * 设置所属 KRL 文件。
     *
     * @param krlFile KRL 文件
     */
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
        // <T extends ast.pojo.tech.waitforu.AstNode>：这是泛型方法的类型参数声明，表示 T 必须是 ast.pojo.tech.waitforu.AstNode 接口的实现类（或子类）。
        // Class<T> clazz：表示方法接受一个 T 类型的 Class 对象，用于在运行时检查节点类型。
        // List<T>：表示方法返回一个元素类型为 T 的列表。这样调用者可以直接获得正确类型的列表，无需手动类型转换。
        List<T> result = new ArrayList<>();
        searchRecursively(this,clazz,result);
        return result;
    }

    /**
     * 深度优先递归搜索指定类型节点。
     *
     * @param curNode 当前节点
     * @param clazz 目标类型
     * @param result 结果容器
     * @param <T> 目标类型
     */
    private <T extends AstNode> void searchRecursively(AstNode curNode, Class<T> clazz, List<T> result){
        // 1. 检查当前节点是否是目标类型（或其子类）
        if(clazz.isInstance(curNode)){
            result.add(clazz.cast(curNode));
        }
        // 2. 递归遍历所有子节点
        for(AstNode child : curNode.getChildren()){
            searchRecursively(child,clazz,result);
        }
    }

    /**
     * 递归查找根节点。
     *
     * @return 根节点
     */
    @Override
    public AstNode findRootNode() {
        if (parent == null) {
            return this;
        }
        return parent.findRootNode();
    }



    public abstract static class AstNodeBuilder<T extends AstNodeBuilder<T>> {
        /** 起始索引。 */
        protected int startIndex;
        /** 结束索引。 */
        protected int stopIndex;
        /** 所属文件。 */
        protected KrlFile krlFile;

        /**
         * 返回当前 builder，用于链式调用。
         *
         * @return 当前 builder
         */
        protected abstract T self();

        /**
         * 设置起始索引。
         *
         * @param startIndex 起始索引
         * @return 当前构建器实例
         */
        public T withStartIndex(int startIndex) {
            this.startIndex = startIndex;
            return self();
        }

        /**
         * 设置结束索引。
         *
         * @param stopIndex 结束索引
         * @return 当前构建器实例
         */
        public T withStopIndex(int stopIndex) {
            this.stopIndex = stopIndex;
            return self();
        }

        /**
         * 设置 KRL 文件。
         *
         * @param krlFile KRL文件
         * @return 当前构建器实例
         */
        public T withKrlFile(KrlFile krlFile) {
            this.krlFile = krlFile;
            return self();
        }


        /**
         * 抽象构建方法。
         *
         * @return 构建后的节点
         */
        public abstract AbstractAstNode build();

    }

}
