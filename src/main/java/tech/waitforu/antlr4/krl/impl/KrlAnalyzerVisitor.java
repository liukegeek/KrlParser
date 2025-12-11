package tech.waitforu.antlr4.krl.impl;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import tech.waitforu.Decompress;
import tech.waitforu.IgnoreRule;
import tech.waitforu.YamlConfigService;
import tech.waitforu.antlr4.krl.krlBaseVisitor;
import tech.waitforu.antlr4.krl.krlLexer;
import tech.waitforu.antlr4.krl.krlParser;
import tech.waitforu.pojo.Config;

import java.sql.SQLOutput;

/**
 * ClassName: KrlAnalyzerVisitor
 * Package: tech.waitforu.antlr4.krl.impl
 * Description:
 * Author: LiuKe
 * Create: 2025/12/9 15:17
 * Version 1.0
 */
public class KrlAnalyzerVisitor extends krlBaseVisitor<Void> {



    @Override
    public Void visitInvokeCallable(krlParser.InvokeCallableContext ctx) {
        krlParser.VariableNameContext callableName = ctx.callableName;
        String callableNameText = callableName.getText();
        System.out.println("callableNameText: " + callableNameText);
        return null;
    }

//    @Override
//    public Void visitSwitchStatement(krlParser.SwitchStatementContext ctx) {
//
//        for (int i = 0; i < ctx.getChildCount(); i++) {
//            System.out.println("子项:" + i);
//            System.out.println(ctx.getChild(i).getText());
//        }
//        return null;
//    }



    @Override
    public Void visitSwitchStatement(krlParser.SwitchStatementContext ctx) {
        System.out.println("-=========-");
//        System.out.println(visitChildren(ctx));
        System.out.println(ctx.body.getText());
        return null;
    }

    public static void main(String[] args) {
        YamlConfigService yamlConfigService = new YamlConfigService("/Users/liuke/IdeaProjects/KRLParser/src/main/resources/config.yml");
        Config config = yamlConfigService.loadConfig(new Config());
        IgnoreRule ignoreRule = new IgnoreRule(config.fileLoadSection);

        String zipFilePath = "/Desktop/EC010_L1.zip";

        Decompress decompress = new Decompress(zipFilePath, ignoreRule);

        String cellPath = "/KRC/R1/cell.src";
        String fileContent = decompress.getFileContent(cellPath);

        CharStream charStream = CharStreams.fromString(fileContent);
        krlLexer lexer = new krlLexer(charStream);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        krlParser krlParser = new krlParser(tokens);

        // 生成语法树
        ParseTree parseTree = krlParser.start();

        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println(krlParser.start());
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

        // 打印语法树结构（调试用）
        System.out.println(parseTree.toStringTree(krlParser));

        System.out.println("");
        // 创建访问者实例并启动遍历
        KrlAnalyzerVisitor visitor = new KrlAnalyzerVisitor();
        visitor.visit(parseTree);  // 启动语法树遍历

    }
}