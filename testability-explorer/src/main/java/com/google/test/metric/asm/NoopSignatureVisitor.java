/*
 * Copyright 2007 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.test.metric.asm;

import org.objectweb.asm.signature.SignatureVisitor;

public class NoopSignatureVisitor extends SignatureVisitor {

    public NoopSignatureVisitor(int i) {
        super(i);
    }

  public SignatureVisitor visitArrayType() {
    return new NoopSignatureVisitor(ASMVersionHelper.getVersion());
  }

  public void visitBaseType(char descriptor) {
  }

  public SignatureVisitor visitClassBound() {
    return new NoopSignatureVisitor(ASMVersionHelper.getVersion());
  }

  public void visitClassType(String name) {
  }

  public void visitEnd() {
  }

  public SignatureVisitor visitExceptionType() {
    return new NoopSignatureVisitor(ASMVersionHelper.getVersion());
  }

  public void visitFormalTypeParameter(String name) {
  }

  public void visitInnerClassType(String name) {
  }

  public SignatureVisitor visitInterface() {
    return new NoopSignatureVisitor(ASMVersionHelper.getVersion());
  }

  public SignatureVisitor visitInterfaceBound() {
    return new NoopSignatureVisitor(ASMVersionHelper.getVersion());
  }

  public SignatureVisitor visitParameterType() {
    return new NoopSignatureVisitor(ASMVersionHelper.getVersion());
  }

  public SignatureVisitor visitReturnType() {
    return new NoopSignatureVisitor(ASMVersionHelper.getVersion());
  }

  public SignatureVisitor visitSuperclass() {
    return new NoopSignatureVisitor(ASMVersionHelper.getVersion());
  }

  public void visitTypeArgument() {
  }

  public SignatureVisitor visitTypeArgument(char wildcard) {
    return new NoopSignatureVisitor(ASMVersionHelper.getVersion());
  }

  public void visitTypeVariable(String name) {
  }

}