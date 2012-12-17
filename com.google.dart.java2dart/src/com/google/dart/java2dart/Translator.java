/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.dart.java2dart;

import com.google.common.collect.Lists;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.BooleanLiteral;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.DoubleLiteral;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.ThisExpression;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.TypeParameterList;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.util.ExecutionUtils;
import com.google.dart.java2dart.util.RunnableEx;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

/**
 * Translates Java to Dart source.
 */
public class Translator extends ASTVisitor {
  /**
   * Translates given Java AST into Dart AST.
   */
  public static com.google.dart.engine.ast.CompilationUnit translate(
      org.eclipse.jdt.core.dom.CompilationUnit javaUnit) {
    Translator translator = new Translator();
    javaUnit.accept(translator);
    return (CompilationUnit) translator.result;
  }

  /**
   * @return the class name of the given {@link Object}, even if is <code>null</code>.
   */
  private static String getClassName(Object node) {
    if (node != null) {
      return node.getClass().getCanonicalName();
    } else {
      return "<null>";
    }
  }

  private static Method getMostSpecificMethod(Class<?> argumentType) throws Exception {
    Method resultMethod = null;
    Class<?> resultType = null;
    for (Method method : Translator.class.getMethods()) {
      if (!method.getName().equals("visit")) {
        continue;
      }
      Class<?> parameterType = method.getParameterTypes()[0];
      if (!parameterType.isAssignableFrom(argumentType)) {
        continue;
      }
      if (resultType == null || resultType.isAssignableFrom(parameterType)) {
        resultMethod = method;
        resultType = parameterType;
      }
    }
    Assert.isNotNull(resultMethod);
    return resultMethod;
  }

  private static SimpleIdentifier newSimpleIdentifier(org.eclipse.jdt.core.dom.SimpleName name) {
    return newSimpleIdentifier(name.getIdentifier());
  }

  private static SimpleIdentifier newSimpleIdentifier(String name) {
    return new SimpleIdentifier(new StringToken(TokenType.IDENTIFIER, name, 0));
  }

  @SuppressWarnings("unchecked")
  private static <T extends ASTNode> T translate0(final org.eclipse.jdt.core.dom.ASTNode node) {
    if (node == null) {
      return null;
    }
    final Translator translator = new Translator();
    ExecutionUtils.runRethrow(new RunnableEx() {
      @Override
      public void run() throws Exception {
        Method method = getMostSpecificMethod(node.getClass());
        method.invoke(translator, node);
      }
    });
    Assert.isNotNull(translator.result, "No result for: " + getClassName(node));
    return (T) translator.result;
  }

  private ASTNode result;

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.Block node) {
    Block block = new Block();
    for (Iterator<?> I = node.statements().iterator(); I.hasNext();) {
      org.eclipse.jdt.core.dom.Statement javaStatement = (org.eclipse.jdt.core.dom.Statement) I.next();
      Statement statement = translate0(javaStatement);
      block.getStatements().add(statement);
    }
    return done(block);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.BooleanLiteral node) {
    boolean value = node.booleanValue();
    Token token = value ? new KeywordToken(Keyword.TRUE, 0) : new KeywordToken(Keyword.FALSE, 0);
    BooleanLiteral literal = new BooleanLiteral(token, value);
    return done(literal);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.CastExpression node) {
    Expression expression = translate0(node.getExpression());
    return done(expression);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.CompilationUnit node) {
    CompilationUnit unit = new CompilationUnit();
    for (Iterator<?> I = node.types().iterator(); I.hasNext();) {
      org.eclipse.jdt.core.dom.TypeDeclaration javaType = (org.eclipse.jdt.core.dom.TypeDeclaration) I.next();
      ClassDeclaration dartClass = translate0(javaType);
      unit.getDeclarations().add(dartClass);
    }
    return done(unit);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ExpressionStatement node) {
    ExpressionStatement statement = new ExpressionStatement();
    Expression expression = translate0(node.getExpression());
    statement.setExpression(expression);
    return done(statement);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.MethodDeclaration node) {
    MethodDeclaration method = new MethodDeclaration();
    method.setReturnType((TypeName) translate0(node.getReturnType2()));
    // name
    method.setName(newSimpleIdentifier(node.getName()));
    // parameters
    {
      FormalParameterList parameters = new FormalParameterList();
      method.setParameters(parameters);
      for (Iterator<?> I = node.parameters().iterator(); I.hasNext();) {
        org.eclipse.jdt.core.dom.SingleVariableDeclaration javaParameter = (SingleVariableDeclaration) I.next();
        SimpleFormalParameter parameter = translate0(javaParameter);
        parameters.getParameters().add(parameter);
      }
    }
    // body
    {
      BlockFunctionBody functionBody = new BlockFunctionBody();
      method.setBody(functionBody);
      Block block = translate0(node.getBody());
      functionBody.setBlock(block);
    }
    return done(method);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.MethodInvocation node) {
    MethodInvocation invocation = new MethodInvocation();
    // target
    invocation.setTarget((Expression) translate0(node.getExpression()));
    // name
    invocation.setMethodName(newSimpleIdentifier(node.getName()));
    // set arguments
    ArgumentList argumentList = new ArgumentList(null, Lists.<Expression> newArrayList(), null);
    invocation.setArgumentList(argumentList);
    // fill arguments
    for (Iterator<?> I = node.arguments().iterator(); I.hasNext();) {
      org.eclipse.jdt.core.dom.Expression javaArg = (org.eclipse.jdt.core.dom.Expression) I.next();
      Expression dartArg = translate0(javaArg);
      argumentList.getArguments().add(dartArg);
    }
    return done(invocation);
//    if (node.getExpression() == null) {
//    } else {
//      throw new IllegalStateException("Not implemented");
//    }
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.NumberLiteral node) {
    String token = node.getToken();
    if (token.contains(".")
        || !StringUtils.startsWithIgnoreCase(token, "0x")
        && (StringUtils.endsWithIgnoreCase(token, "F") || StringUtils.endsWithIgnoreCase(token, "D"))) {
      token = StringUtils.removeEndIgnoreCase(token, "F");
      token = StringUtils.removeEndIgnoreCase(token, "D");
      if (!token.contains(".")) {
        token += ".0";
      }
      DoubleLiteral literal = new DoubleLiteral(new StringToken(TokenType.DOUBLE, token, 0), 0);
      return done(literal);
    } else {
      token = StringUtils.removeEndIgnoreCase(token, "L");
      IntegerLiteral literal = new IntegerLiteral(new StringToken(TokenType.INT, token, 0), 0);
      return done(literal);
    }
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.PrimitiveType node) {
    String name = node.toString();
    if ("boolean".equals(name)) {
      name = "bool";
    }
    if ("byte".equals(name) || "char".equals(name) || "short".equals(name) || "long".equals(name)) {
      name = "int";
    }
    if ("float".equals(name)) {
      name = "double";
    }
    TypeName type = new TypeName(newSimpleIdentifier(name), null);
    return done(type);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.SingleVariableDeclaration node) {
    SimpleFormalParameter parameter = new SimpleFormalParameter();
    parameter.setIdentifier(newSimpleIdentifier(node.getName()));
    TypeName type = translate0(node.getType());
    parameter.setType(type);
    return done(parameter);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ThisExpression node) {
    return done(new ThisExpression());
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.TypeDeclaration node) {
    ClassDeclaration dartClass = new ClassDeclaration();
    // name
    dartClass.setName(newSimpleIdentifier(node.getName()));
    // type parameters
    {
      List<?> javaTypeParameters = node.typeParameters();
      if (!javaTypeParameters.isEmpty()) {
        dartClass.setTypeParameters(new TypeParameterList());
        for (Iterator<?> I = javaTypeParameters.iterator(); I.hasNext();) {
          org.eclipse.jdt.core.dom.TypeParameter javaTypeParameter = (org.eclipse.jdt.core.dom.TypeParameter) I.next();
          TypeParameter typeParameter = translate0(javaTypeParameter);
          dartClass.getTypeParameters().getTypeParameters().add(typeParameter);
        }
      }
    }
    // members
    for (Iterator<?> I = node.bodyDeclarations().iterator(); I.hasNext();) {
      org.eclipse.jdt.core.dom.BodyDeclaration javaBodyDecl = (org.eclipse.jdt.core.dom.BodyDeclaration) I.next();
      ClassMember member = translate0(javaBodyDecl);
      dartClass.getMembers().add(member);
    }
    return done(dartClass);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.TypeParameter node) {
    TypeParameter typeParameter = new TypeParameter();
    typeParameter.setName(newSimpleIdentifier(node.getName()));
    {
      List<?> typeBounds = node.typeBounds();
      if (typeBounds.size() == 1) {
        org.eclipse.jdt.core.dom.Type bound = (org.eclipse.jdt.core.dom.Type) typeBounds.get(0);
        typeParameter.setBound((TypeName) translate0(bound));
      }
    }
    return done(typeParameter);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.VariableDeclarationFragment node) {
    VariableDeclaration var = new VariableDeclaration();
    var.setName(newSimpleIdentifier(node.getName()));
    var.setInitializer((Expression) translate0(node.getInitializer()));
    // XXX
    return done(var);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.VariableDeclarationStatement node) {
    VariableDeclarationStatement statement = new VariableDeclarationStatement();
    statement.setVariables(new VariableDeclarationList());
    statement.getVariables().setType((TypeName) translate0(node.getType()));
    for (Iterator<?> I = node.fragments().iterator(); I.hasNext();) {
      org.eclipse.jdt.core.dom.VariableDeclarationFragment javaFragment = (org.eclipse.jdt.core.dom.VariableDeclarationFragment) I.next();
      VariableDeclaration var = translate0(javaFragment);
      statement.getVariables().getVariables().add(var);
    }
    // XXX
    return done(statement);
  }

  @Override
  public boolean visit(SimpleType node) {
    TypeName type = new TypeName(newSimpleIdentifier(node.getName().toString()), null);
    return done(type);
  }

  /**
   * Set {@link #result} and return <code>false</code> - we don't want normal JDT visiting.
   */
  private boolean done(ASTNode node) {
    result = node;
    return false;
  }
}
