package tech.waitforu.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import tech.waitforu.antlr4.krlLexer;
import tech.waitforu.antlr4.krlParser;
import tech.waitforu.pojo.ast.AstNode;
import tech.waitforu.pojo.ast.KrlRoot;
import tech.waitforu.pojo.ast.programunit.Callable;
import tech.waitforu.pojo.krl.KrlFile;
import tech.waitforu.pojo.krl.KrlModule;

import java.util.ArrayList;
import java.util.List;

/**
 * ClassName: parser.tech.waitforu.ModuleParser
 * Package: tech.waitforu.pojo.ast
 * Description:从模块中解析出AST节点。
 * Author: LiuKe
 * Create: 2025/12/17 17:34
 * Version 1.0
 */
public class ModuleParser {

    KrlModule module;

    public ModuleParser(KrlModule module) {
        if (module == null) {
            System.out.println("krlModule is null");
            return;
        }
        this.module = module;
    }

    public AstNode getSrcAst() {
        KrlFile srcKrlFile = this.module.getModuleSrcFile();

        if (srcKrlFile == null) {
//            System.out.println("krlModule:" + this.module.getModuleName() + " has no src file");
            String dataPath = this.module.getModuleDatFile()==null? "null":this.module.getModuleDatFile().getPath();
//            System.out.println("其dat文件地址为:" + dataPath);
            return null;
        }
        return parseKrlFile(srcKrlFile);
    }

    public AstNode getDatAst() {
        KrlFile datKrlFile = this.module.getModuleDatFile();
        if (datKrlFile == null) {
//            System.out.println("krlModule:" + this.module.getModuleName() + " has no dat file");
//            System.out.println("其src文件地址为:" + this.module.getModuleSrcFile().getPath());
            return null;
        }
        return parseKrlFile(datKrlFile);
    }

    //得到模块中的的可调用程序的Ast结点
    public List<Callable> getCallableList() {
        AstNode srcAst = getSrcAst();
        if (srcAst == null){
            return new ArrayList<>();
        }

        if (!(srcAst instanceof KrlRoot krlRoot)) {
            throw new RuntimeException("srcAst is not ast.pojo.tech.waitforu.KrlRoot");
        }

        return krlRoot.getBody().getProgramUnitList().stream()
                .filter(programUnit -> programUnit instanceof Callable)
                .map(programUnit -> (Callable) programUnit)
                .toList();
    }

    private AstNode parseKrlFile(KrlFile krlFile) {
        if (krlFile == null) {
            return null;
        }

        String fileContent = krlFile.getContent();
        CharStream charStream = CharStreams.fromString(fileContent);
        krlLexer lexer = new krlLexer(charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        krlParser krlParser = new krlParser(tokens);

        // 生成语法树
        ParseTree parseTree = krlParser.start();
        tech.waitforu.parser.AstBuilderVisitor astBuilderVisitor = new tech.waitforu.parser.AstBuilderVisitor(krlFile);
        AstNode root = astBuilderVisitor.visit(parseTree);

        if (root instanceof KrlRoot) {
            //AST为root根结点，正常返回。
            return root;
        }
        return null;
    }

}
