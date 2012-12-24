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

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.java2dart.util.ToFormattedSourceVisitor;

import junit.framework.TestCase;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * Test for general Java semantics to Dart translation.
 */
public class SemanticTest extends TestCase {

  /**
   * @return the formatted Dart source dump of the given {@link ASTNode}.
   */
  private static String getFormattedSource(ASTNode node) {
    PrintStringWriter writer = new PrintStringWriter();
    node.accept(new ToFormattedSourceVisitor(writer));
    return writer.toString();
  }

  /**
   * @return the single {@link String} with "\n" separated lines.
   */
  private static String toString(String... lines) {
    return Joiner.on("\n").join(lines);
  }

  private File tmpFolder;
  private String tmpFolderPath;
  private org.eclipse.jdt.core.dom.CompilationUnit javaUnit;
  private com.google.dart.engine.ast.CompilationUnit dartUnit;

  public void test_rename() throws Exception {
    setFileLines(
        "test/Main.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Main {",
            "  static void foo() {}",
            "  static void foo(int p) {}",
            "}",
            ""));
    setFileLines(
        "test/Second.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Second {",
            "  static void bar() {",
            "    Main.foo(42);",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    System.out.println(context.getSourceFolders());
    System.out.println(context.getSourceFiles());
    context.translate();
//    parseJavaSource("test/Main.java");
//    parseJavaSource("test/Second.java");
//    javaUnit.accept(new ASTVisitor() {
//      @Override
//      public boolean visit(MethodInvocation node) {
//        IMethodBinding binding = node.resolveMethodBinding();
//        System.out.println(binding);
//        return super.visit(node);
//      }
//    });
    // XXX
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    tmpFolder = Files.createTempDir();
    tmpFolderPath = tmpFolder.getCanonicalPath();
  }

  @Override
  protected void tearDown() throws Exception {
    FileUtils.deleteDirectory(tmpFolder);
    super.tearDown();
  }

//  /**
//   * Translates {@link #javaUnit} into {@link #dartUnit} and check that it produces given Dart
//   * source.
//   */
//  private void assertDartSource(String... lines) {
//    dartUnit = SyntaxTranslator.translate(javaUnit);
//    String actualDartSource = getFormattedSource(dartUnit);
//    String expectedDartSource = toString(lines);
//    assertEquals(expectedDartSource, actualDartSource);
//  }
//
//  /**
//   * Parse Java source into {@link #javaUnit}.
//   */
//  private void parseJavaSource(String path) throws Exception {
//    String unitName = path.contains("/") ? StringUtils.substringAfterLast(path, "/") : path;
//    String source = Files.toString(new File(tmpFolder, path), Charsets.UTF_8);
//    ASTParser parser = ASTParser.newParser(AST.JLS4);
//    parser.setEnvironment(null, new String[] {tmpFolderPath}, null, true);
//    parser.setResolveBindings(true);
//    parser.setCompilerOptions(ImmutableMap.of(
//        JavaCore.COMPILER_SOURCE,
//        JavaCore.VERSION_1_5,
//        JavaCore.COMPILER_DOC_COMMENT_SUPPORT,
//        JavaCore.ENABLED));
//    parser.setUnitName(unitName);
//    parser.setSource(source.toCharArray());
//    javaUnit = (CompilationUnit) parser.createAST(null);
//    assertThat(javaUnit.getProblems()).isEmpty();
//  }

  /**
   * Sets the content of the file with given path relative to {@link #tmpFolder}.
   */
  private void setFileLines(String path, String content) throws Exception {
    File toFile = new File(tmpFolder, path);
    Files.createParentDirs(toFile);
    Files.write(content, toFile, Charsets.UTF_8);
  }
}
