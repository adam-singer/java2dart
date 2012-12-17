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

import junit.framework.TestCase;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * Test that we can translate Java syntax to Dart syntax.
 */
public class SyntaxTest extends TestCase {

  /**
   * @return the Dart source of the given {@link com.google.dart.engine.ast.CompilationUnit}.
   */
  private static String toSource(com.google.dart.engine.ast.CompilationUnit dartUnit) {
    return dartUnit.toSource();
  }

  private org.eclipse.jdt.core.dom.CompilationUnit javaUnit;

  private com.google.dart.engine.ast.CompilationUnit dartUnit;

  public void test_classEmpty() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "}");
    assertDartSource("class A {}");
  }

  public void test_classTypeParameters() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A<K, V extends String> {",
        "  A(K k, V v) {",
        "  }",
        "}");
    assertDartSource("class A<K, V extends String> {A(K k, V v) {}}");
  }

  public void test_expressionCast() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    byte b = (byte) 0;",
        "  }",
        "}");
    assertDartSource("class A {A() {int b = 0;}}");
  }

  public void test_expressionInvocation_qualified() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    this.foo(0);",
        "  }",
        "  void foo(int p) {}",
        "}");
    assertDartSource("class A {A() {this.foo(0);} void foo(int p) {}}");
  }

  public void test_expressionInvocation_unqualified() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    print(0);",
        "    print(1);",
        "  }",
        "}");
    assertDartSource("class A {A() {print(0); print(1);}}");
  }

  public void test_literalBoolean() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    print(true);",
        "    print(false);",
        "  }",
        "}");
    assertDartSource("class A {A() {print(true); print(false);}}");
  }

  public void test_literalDouble() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    print(0.0);",
        "    print(1d);",
        "  }",
        "}");
    assertDartSource("class A {A() {print(0.0); print(1.0);}}");
  }

  public void test_literalInt() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    print(0);",
        "    print(1);",
        "    print(0xDEAD);",
        "    print(0xBEAF);",
        "  }",
        "}");
    assertDartSource("class A {A() {print(0); print(1); print(0xDEAD); print(0xBEAF);}}");
  }

  public void test_literalLong() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    print(0l);",
        "    print(1L);",
        "    print(0xDEADl);",
        "    print(0xBEAFL);",
        "  }",
        "}");
    assertDartSource("class A {A() {print(0); print(1); print(0xDEAD); print(0xBEAF);}}");
  }

  public void test_methodEmpty() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "  }",
        "}");
    assertDartSource("class A {A() {}}");
  }

  public void test_methodParameters() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A(boolean b, int i, double d, String s) {",
        "  }",
        "}");
    assertDartSource("class A {A(bool b, int i, double d, String s) {}}");
  }

  public void test_methodReturnType() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  void foo() {",
        "  }",
        "}");
    assertDartSource("class A {void foo() {}}");
  }

  public void test_typePrimitive() throws Exception {
    parseJava(
        "// filler filler filler filler filler filler filler filler filler filler",
        "public class A {",
        "  A() {",
        "    boolean v1;",
        "    byte v2;",
        "    char v3;",
        "    short v4;",
        "    int v5;",
        "    long v6;",
        "    float v7;",
        "    double v8;",
        "  }",
        "}");
    assertDartSource("class A {A() {bool v1; int v2; int v3; int v4; int v5; int v6; double v7; double v8;}}");
  }

  public void test_unitEmpty() throws Exception {
    parseJava();
    assertDartSource("");
  }

  /**
   * Translates {@link #javaUnit} into {@link #dartUnit} and check that it produces given Dart
   * source.
   */
  private void assertDartSource(String... lines) {
    dartUnit = Translator.translate(javaUnit);
    String actualDartSource = toSource(dartUnit);
    String expectedDartSource = Joiner.on("\n").join(lines);
    assertEquals(expectedDartSource, actualDartSource);
  }

  /**
   * Parse Java source lines into {@link #javaUnit}.
   */
  private void parseJava(String... lines) {
    String source = Joiner.on("\n").join(lines);
    ASTParser parser = ASTParser.newParser(AST.JLS4);
    parser.setSource(source.toCharArray());
    javaUnit = (CompilationUnit) parser.createAST(null);
  }
}
