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
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MBuf {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MBuf.class);

//  @Override
//  protected PoolChunk<ByteBuffer> newChunk(int pageSize, int maxOrder, int pageShifts, int chunkSize) {
//      return new PoolChunk<ByteBuffer>(
//              this, ByteBuffer.allocateDirect(chunkSize), pageSize, maxOrder, pageShifts, chunkSize);
//  }


  private final static ChunkCreator INSTANCE;
  private static final int CHUNK_SIZE = 16*1024*1024;

  public static void main(String[] args) throws Exception {

  }

  static {

    ChunkCreator creator = null;
    try{
      creator = new ChunkCreator(1024*1024*1024);
    }catch(Exception e){
      logger.error("Failure while configuring MBuf.");
      e.printStackTrace(System.err);
    }

    INSTANCE = creator;

  }
  private static class ChunkCreator{

    private ConcurrentLinkedQueue<ByteBuffer> freeList = new ConcurrentLinkedQueue<ByteBuffer>();

    public ChunkCreator(long size) throws IOException{
      final FileChannel fc = new RandomAccessFile(new File("/tmp/mapped.txt"), "rw").getChannel();
      MappedByteBuffer mem = fc.map(FileChannel.MapMode.READ_ONLY, 0, size);
      for(int i = 0; i < (size - CHUNK_SIZE); i+=CHUNK_SIZE){
        ByteBuffer dupe = mem.duplicate();
        mem.position(i);
        mem.limit(i + CHUNK_SIZE);
        freeList.add(mem.slice());
      }

    }

    public ByteBuffer allocate(int chunkSize){
      assert chunkSize == 16*1024*1024;
      return freeList.remove();
    }

    public void release(ByteBuffer buffer){
      freeList.add(buffer);
    }
  }

  public static ByteBuffer allocate(int chunkSize){
    return INSTANCE.allocate(chunkSize);
  }

  public static void release(ByteBuffer buffer){
    INSTANCE.release(buffer);
  }
}
