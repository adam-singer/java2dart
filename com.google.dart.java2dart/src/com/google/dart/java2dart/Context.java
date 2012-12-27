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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.java2dart.util.ToFormattedSourceVisitor;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Context information for Java to Dart translation.
 */
public class Context {
  private static final String[] JAVA_EXTENSION = {"java"};

  /**
   * @return the formatted Dart source dump of the given {@link ASTNode}.
   */
  private static String getFormattedSource(ASTNode node) {
    PrintStringWriter writer = new PrintStringWriter();
    node.accept(new ToFormattedSourceVisitor(writer));
    return writer.toString();
  }

  private final List<File> sourceFolders = Lists.newArrayList();
  private final List<File> sourceFiles = Lists.newArrayList();
  private final Map<File, CompilationUnit> dartUnits = Maps.newHashMap();
  private final Map<SimpleIdentifier, String> identifierToBinding = Maps.newHashMap();
  private final Map<String, List<SimpleIdentifier>> bindingToIdentifiers = Maps.newHashMap();
  private final Map<String, String> renameMethodMap = Maps.newHashMap();

  /**
   * Specifies that given folder is a source folder (root of Java packages hierarchy), and all its
   * files should be translated.
   */
  public void addSourceFolder(File folder) {
    Assert.isLegal(folder.exists(), "Folder '" + folder + "' does not exist.");
    Assert.isLegal(folder.isDirectory(), "Folder '" + folder + "' is not a folder.");
    folder = folder.getAbsoluteFile();
    sourceFolders.add(folder);
    Collection<File> folderFiles = FileUtils.listFiles(folder, JAVA_EXTENSION, true);
    sourceFiles.addAll(folderFiles);
  }

  /**
   * @return the source files.
   */
  public List<File> getSourceFiles() {
    return sourceFiles;
  }

  /**
   * @return the source Java folders.
   */
  public List<File> getSourceFolders() {
    return sourceFolders;
  }

  public void translate() throws Exception {
    // XXX
    translateSyntax();
    System.out.println(dartUnits);
    System.out.println(identifierToBinding);
    System.out.println(bindingToIdentifiers);
    // XXX
    {
      List<SimpleIdentifier> names = bindingToIdentifiers.get("Ltest/Main;.foo(I)V");
      for (SimpleIdentifier name : names) {
        name.setToken(new StringToken(TokenType.IDENTIFIER, "foo2", 0));
      }
    }
    // XXX
    for (CompilationUnit unit : dartUnits.values()) {
      System.out.println(getFormattedSource(unit));
    }
  }

  /**
   * Remembers that "identifier" is reference to the given Java binding.
   */
  void putReference(org.eclipse.jdt.core.dom.IBinding binding, SimpleIdentifier identifier) {
    String key = null;
    if (binding != null) {
      key = binding.getKey();
    }
    if (key == null) {
      key = identifier.getName();
    }
    // remember binding for reference
    identifierToBinding.put(identifier, key);
    // add reference to binding
    List<SimpleIdentifier> names = bindingToIdentifiers.get(key);
    if (names == null) {
      names = Lists.newLinkedList();
      bindingToIdentifiers.put(key, names);
    }
    names.add(identifier);
  }

  /**
   * @return the Java AST of the given Java {@link File} in context of {@link #sourceFolders}.
   */
  private org.eclipse.jdt.core.dom.CompilationUnit parseJavaFile(File javaFile) throws Exception {
    String javaPath = javaFile.getAbsolutePath();
    String javaName = StringUtils.substringAfterLast(javaPath, "/");
    String javaSource = Files.toString(javaFile, Charsets.UTF_8);
    // prepare Java parser
    ASTParser parser = ASTParser.newParser(AST.JLS4);
    {
      String[] sourceEntries = new String[sourceFolders.size()];
      for (int i = 0; i < sourceFolders.size(); i++) {
        sourceEntries[i] = sourceFolders.get(i).getAbsolutePath();
      }
      parser.setEnvironment(null, sourceEntries, null, true);
    }
    parser.setResolveBindings(true);
    parser.setCompilerOptions(ImmutableMap.of(
        JavaCore.COMPILER_SOURCE,
        JavaCore.VERSION_1_5,
        JavaCore.COMPILER_DOC_COMMENT_SUPPORT,
        JavaCore.ENABLED));
    // do parse
    parser.setUnitName(javaName);
    parser.setSource(javaSource.toCharArray());
    return (org.eclipse.jdt.core.dom.CompilationUnit) parser.createAST(null);
  }

  /**
   * Translate {@link #sourceFiles} into Dart AST in {@link #dartUnits}.
   */
  private void translateSyntax() throws Exception {
    for (File javaFile : sourceFiles) {
      org.eclipse.jdt.core.dom.CompilationUnit javaUnit = parseJavaFile(javaFile);
      CompilationUnit dartUnit = SyntaxTranslator.translate(this, javaUnit);
      dartUnits.put(javaFile, dartUnit);
    }
  }
}
