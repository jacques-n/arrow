/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.arrow.ipc;

import org.apache.arrow.memory.RootAllocator;

import io.netty.buffer.ArrowBuf;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

public class NettyPatcher {

  public static void main(String[] args) throws Exception {
    patchNetty();
    RootAllocator allocator = new RootAllocator(1000000);
    ArrowBuf buf = allocator.buffer(1024);
    buf.setByte(0, 8);

  }

  public static void patchNetty() {

    try {
      final ClassPool cp = ClassPool.getDefault();
      final CtClass cc = cp.get("io.netty.buffer.PoolArena$DirectArena");
      for(CtMethod m : cc.getMethods()){
        System.out.println(m.getName() + " " + m.getSignature());
      }
      final CtMethod create = cc.getMethod("newChunk", "(IIII)Lio/netty/buffer/PoolChunk;");
      create.setBody(
          "return new io.netty.buffer.PoolChunk(this, org.apache.arrow.ipc.MBuf.allocate($4), $1, $2, $3, $4);");
      final CtMethod release = cc.getMethod("destroyChunk", "(Lio/netty/buffer/PoolChunk;)V");
      release.setBody("org.apache.arrow.ipc.MBuf.release("
          + "(java.nio.ByteBuffer) $1.memory);");

      cc.toClass();
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
  }
}
