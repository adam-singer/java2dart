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

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.java2dart.util.ToFormattedSourceVisitor;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

import java.io.File;
import java.nio.charset.Charset;

public class Main {
  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      printUsage();
    }
    String fileName = args[0];
    File file = new File(fileName);
    if (!file.exists() || !file.isFile()) {
      System.out.println("File does not exist: " + fileName);
      System.exit(1);
    }
    //
    String javaSource = Files.toString(file, Charset.forName("UTF-8"));
    org.eclipse.jdt.core.dom.CompilationUnit javaUnit = parseJava(javaSource);
    CompilationUnit dartUnit = Translator.translate(javaUnit);
    System.out.println(getFormattedSource(dartUnit));
  }

  /**
   * @return the formatted Dart source dump of the given {@link ASTNode}.
   */
  private static String getFormattedSource(ASTNode node) {
    PrintStringWriter writer = new PrintStringWriter();
    node.accept(new ToFormattedSourceVisitor(writer));
    return writer.toString();
  }

  private static org.eclipse.jdt.core.dom.CompilationUnit parseJava(String source) {
    ASTParser parser = ASTParser.newParser(AST.JLS4);
    parser.setCompilerOptions(ImmutableMap.of(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_5));
    parser.setSource(source.toCharArray());
    return (org.eclipse.jdt.core.dom.CompilationUnit) parser.createAST(null);
  }

  private static void printUsage() {
    System.out.println("Usage: java2dart <file>");
    System.exit(1);
  }
}
