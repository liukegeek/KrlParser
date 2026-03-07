package tech.waitforu.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import tech.waitforu.antlr4.krlLexer;
import tech.waitforu.antlr4.krlParser;
import tech.waitforu.exception.KrlParseException;
import tech.waitforu.pojo.ast.AstNode;
import tech.waitforu.pojo.ast.KrlRoot;
import tech.waitforu.pojo.ast.programunit.Callable;
import tech.waitforu.pojo.krl.KrlFile;
import tech.waitforu.pojo.krl.KrlModule;

import java.util.ArrayList;
import java.util.List;

/**
 * 模块解析器。
 * <p>
 * 负责将 {@link KrlModule} 中的 src/dat 文本解析为 AST，
 * 同时提供模块可调用单元（procedure/function）提取能力。
 */
public class ModuleParser {

    /** 待解析模块。 */
    private final KrlModule module;

    /**
     * 构造解析器。
     *
     * @param module 待解析模块
     */
    public ModuleParser(KrlModule module) {
        if (module == null) {
            throw new KrlParseException("模块不能为空，无法继续解析");
        }
        this.module = module;
    }

    /**
     * 解析模块的 src 文件 AST。
     *
     * @return src AST 根节点；无 src 文件时返回 null
     */
    public AstNode getSrcAst() {
        KrlFile srcKrlFile = this.module.getModuleSrcFile();
        if (srcKrlFile == null) {
            return null;
        }
        return parseKrlFile(srcKrlFile);
    }

    /**
     * 解析模块的 dat 文件 AST。
     *
     * @return dat AST 根节点；无 dat 文件时返回 null
     */
    public AstNode getDatAst() {
        KrlFile datKrlFile = this.module.getModuleDatFile();
        if (datKrlFile == null) {
            return null;
        }
        return parseKrlFile(datKrlFile);
    }

    /**
     * 提取模块中全部可调用单元（Callable）。
     *
     * @return Callable 列表；无 src 时返回空列表
     */
    public List<Callable> getCallableList() {
        AstNode srcAst = getSrcAst();
        if (srcAst == null){
            return new ArrayList<>();
        }

        if (!(srcAst instanceof KrlRoot krlRoot)) {
            throw new KrlParseException("模块 " + module.getModuleName() + " 的源文件未解析为 KrlRoot");
        }

        return krlRoot.getBody().getProgramUnitList().stream()
                .filter(programUnit -> programUnit instanceof Callable)
                .map(programUnit -> (Callable) programUnit)
                .toList();
    }

    /**
     * 解析单个 KRL 文件内容为 AST。
     * <p>
     * 解析链路：
     * CharStream -> Lexer -> TokenStream -> Parser -> ParseTree -> AstBuilderVisitor。
     *
     * @param krlFile 待解析文件
     * @return AST 根节点；不是 {@link KrlRoot} 时返回 null
     */
    private AstNode parseKrlFile(KrlFile krlFile) {
        if (krlFile == null) {
            return null;
        }

        String fileContent = krlFile.getContent();
        CharStream charStream = CharStreams.fromString(fileContent);
        krlLexer lexer = new krlLexer(charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        krlParser krlParser = new krlParser(tokens);

        // 先生成解析树，再通过 Visitor 构建业务 AST。
        ParseTree parseTree = krlParser.start();
        tech.waitforu.parser.AstBuilderVisitor astBuilderVisitor = new tech.waitforu.parser.AstBuilderVisitor(krlFile);
        AstNode root = astBuilderVisitor.visit(parseTree);

        if (!(root instanceof KrlRoot)) {
            throw new KrlParseException("文件 " + krlFile.getPath() + " 未解析出 KrlRoot 节点");
        }
        return root;
    }

}
