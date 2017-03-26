package com.helospark.spark.builder.handlers.codegenerator;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import com.helospark.spark.builder.handlers.BuilderType;

/**
 * Generates the builder to the given compilation unit.
 * @author helospark
 */
public interface BuilderCompilationUnitGenerator {

    void generateBuilder(AST ast, ASTRewrite rewriter, CompilationUnit compilationUnit);

    boolean canHandle(BuilderType builderType);
}