package tech.waitforu.antlr4;// Generated from /Users/liuke/IdeaProjects/KRLParser/src/main/resources/krl.g4 by ANTLR 4.13.2

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link krlParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface krlVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by the {@code dataFile}
	 * labeled alternative in {@link krlParser#start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataFile(krlParser.DataFileContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sourceFile}
	 * labeled alternative in {@link krlParser#start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSourceFile(krlParser.SourceFileContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#krlControlHead}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKrlControlHead(krlParser.KrlControlHeadContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#moduleData}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModuleData(krlParser.ModuleDataContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#moduleName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModuleName(krlParser.ModuleNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#dataList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataList(krlParser.DataListContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#dataLine}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataLine(krlParser.DataLineContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#forwardDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForwardDeclaration(krlParser.ForwardDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#parameterList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterList(krlParser.ParameterListContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#parameterWithType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterWithType(krlParser.ParameterWithTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#parameterName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterName(krlParser.ParameterNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#parameterCallType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterCallType(krlParser.ParameterCallTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#enumDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumDefinition(krlParser.EnumDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#enumValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumValue(krlParser.EnumValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#structureDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructureDefinition(krlParser.StructureDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code commonVariableDecl}
	 * labeled alternative in {@link krlParser#variableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommonVariableDecl(krlParser.CommonVariableDeclContext ctx);
	/**
	 * Visit a parse tree produced by the {@code signalVariableDecl}
	 * labeled alternative in {@link krlParser#variableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSignalVariableDecl(krlParser.SignalVariableDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#variableList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableList(krlParser.VariableListContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#variableName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableName(krlParser.VariableNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#arrayVariableSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayVariableSuffix(krlParser.ArrayVariableSuffixContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#importStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportStatement(krlParser.ImportStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#moduleSource}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModuleSource(krlParser.ModuleSourceContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#mainRoutine}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMainRoutine(krlParser.MainRoutineContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#subRoutine}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubRoutine(krlParser.SubRoutineContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#procedureDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedureDefinition(krlParser.ProcedureDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#procedureName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedureName(krlParser.ProcedureNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#functionDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDefinition(krlParser.FunctionDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#functionName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionName(krlParser.FunctionNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#routineBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRoutineBody(krlParser.RoutineBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#routineDataSection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRoutineDataSection(krlParser.RoutineDataSectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#routineImplementationSection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRoutineImplementationSection(krlParser.RoutineImplementationSectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#modifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModifier(krlParser.ModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#statementList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatementList(krlParser.StatementListContext ctx);
	/**
	 * Visit a parse tree produced by the {@code emptyStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEmptyStatement(krlParser.EmptyStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code continueStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinueStatement(krlParser.ContinueStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exitStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExitStatement(krlParser.ExitStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ifStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStatement(krlParser.IfStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code forStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForStatement(krlParser.ForStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code loopStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoopStatement(krlParser.LoopStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code repeatStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRepeatStatement(krlParser.RepeatStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code whileStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileStatement(krlParser.WhileStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code gotoStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGotoStatement(krlParser.GotoStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code switchStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchStatement(krlParser.SwitchStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code waitForStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWaitForStatement(krlParser.WaitForStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code waitSecStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWaitSecStatement(krlParser.WaitSecStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code returnStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnStatement(krlParser.ReturnStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code expressionStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionStatement(krlParser.ExpressionStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code labelStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabelStatement(krlParser.LabelStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ptpMoveStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPtpMoveStatement(krlParser.PtpMoveStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code linMoveStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLinMoveStatement(krlParser.LinMoveStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code linRelMoveStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLinRelMoveStatement(krlParser.LinRelMoveStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code circMoveStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCircMoveStatement(krlParser.CircMoveStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code brakeStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBrakeStatement(krlParser.BrakeStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code interruptDeclStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterruptDeclStatement(krlParser.InterruptDeclStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code interruptControlStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterruptControlStatement(krlParser.InterruptControlStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code asyPtpStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAsyPtpStatement(krlParser.AsyPtpStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code asyCancelStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAsyCancelStatement(krlParser.AsyCancelStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code haltStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHaltStatement(krlParser.HaltStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code triggerStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTriggerStatement(krlParser.TriggerStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code aiStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAiStatement(krlParser.AiStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code aoStatement}
	 * labeled alternative in {@link krlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAoStatement(krlParser.AoStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#gotoLabel}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGotoLabel(krlParser.GotoLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code aiOnStatement}
	 * labeled alternative in {@link krlParser#analogInputStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAiOnStatement(krlParser.AiOnStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code aiOffStatement}
	 * labeled alternative in {@link krlParser#analogInputStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAiOffStatement(krlParser.AiOffStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code aoOnStatement}
	 * labeled alternative in {@link krlParser#analogOutputStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAoOnStatement(krlParser.AoOnStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code aoOffStatement}
	 * labeled alternative in {@link krlParser#analogOutputStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAoOffStatement(krlParser.AoOffStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#switchBlockStatementGroups}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchBlockStatementGroups(krlParser.SwitchBlockStatementGroupsContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#caseLabels}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCaseLabels(krlParser.CaseLabelsContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#defaultLabel}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaultLabel(krlParser.DefaultLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code primaryExpression}
	 * labeled alternative in {@link krlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryExpression(krlParser.PrimaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code orExpression}
	 * labeled alternative in {@link krlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrExpression(krlParser.OrExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code andExpression}
	 * labeled alternative in {@link krlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndExpression(krlParser.AndExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code plusMinusExpression}
	 * labeled alternative in {@link krlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlusMinusExpression(krlParser.PlusMinusExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code negAndPosExpression}
	 * labeled alternative in {@link krlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNegAndPosExpression(krlParser.NegAndPosExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code multiplyDivideExpression}
	 * labeled alternative in {@link krlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplyDivideExpression(krlParser.MultiplyDivideExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code geometryExpression}
	 * labeled alternative in {@link krlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeometryExpression(krlParser.GeometryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exorExpression}
	 * labeled alternative in {@link krlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExorExpression(krlParser.ExorExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code assignmentExpression}
	 * labeled alternative in {@link krlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentExpression(krlParser.AssignmentExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code notExpression}
	 * labeled alternative in {@link krlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotExpression(krlParser.NotExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code relationExpression}
	 * labeled alternative in {@link krlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationExpression(krlParser.RelationExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code bracketPrimary}
	 * labeled alternative in {@link krlParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBracketPrimary(krlParser.BracketPrimaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code literalPrimary}
	 * labeled alternative in {@link krlParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteralPrimary(krlParser.LiteralPrimaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code variablePrimary}
	 * labeled alternative in {@link krlParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariablePrimary(krlParser.VariablePrimaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code structMemberPrimary}
	 * labeled alternative in {@link krlParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructMemberPrimary(krlParser.StructMemberPrimaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code invokeCallablePrimary}
	 * labeled alternative in {@link krlParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInvokeCallablePrimary(krlParser.InvokeCallablePrimaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#typeName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeName(krlParser.TypeNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#primitiveType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimitiveType(krlParser.PrimitiveTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#userType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUserType(krlParser.UserTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(krlParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#enumLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumLiteral(krlParser.EnumLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#structLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructLiteral(krlParser.StructLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#structField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructField(krlParser.StructFieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link krlParser#krlIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKrlIdentifier(krlParser.KrlIdentifierContext ctx);
}