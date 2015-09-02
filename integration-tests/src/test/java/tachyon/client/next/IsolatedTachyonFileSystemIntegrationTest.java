/*
 * Licensed to the University of California, Berkeley under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package tachyon.client.next;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import tachyon.Constants;
import tachyon.client.next.file.TachyonFileSystem;
import tachyon.client.next.file.TachyonFile;
import tachyon.conf.TachyonConf;
import tachyon.master.next.LocalTachyonCluster;
import tachyon.thrift.FileInfo;
import tachyon.util.CommonUtils;
import tachyon.util.io.PathUtils;

/**
 * Integration tests on TachyonClient (Do not reuse the LocalTachyonCluster).
 */
public class IsolatedTachyonFileSystemIntegrationTest {
  private static final int WORKER_CAPACITY_BYTES = 20000;
  private static final int USER_QUOTA_UNIT_BYTES = 1000;
  private LocalTachyonCluster mLocalTachyonCluster = null;
  private TachyonFileSystem mTfs = null;
  private TachyonConf mMasterTachyonConf;
  private TachyonConf mWorkerTachyonConf;
  private int mWorkerToMasterHeartbeatIntervalMs;
  private ClientOptions mWriteBoth;
  private ClientOptions mWriteUnderStorage;

  @After
  public final void after() throws Exception {
    mLocalTachyonCluster.stop();
  }

  @Before
  public final void before() throws Exception {
    mLocalTachyonCluster =
        new LocalTachyonCluster(WORKER_CAPACITY_BYTES, USER_QUOTA_UNIT_BYTES, Constants.GB);
    mLocalTachyonCluster.start();
    mTfs = mLocalTachyonCluster.getClient();
    mMasterTachyonConf = mLocalTachyonCluster.getMasterTachyonConf();
    mWorkerTachyonConf = mLocalTachyonCluster.getWorkerTachyonConf();
    mWorkerTachyonConf.set(Constants.MAX_COLUMNS, "257");
    mWorkerToMasterHeartbeatIntervalMs =
        mWorkerTachyonConf.getInt(Constants.WORKER_TO_MASTER_HEARTBEAT_INTERVAL_MS);
    mWriteBoth = new ClientOptions.Builder(mWorkerTachyonConf).setCacheType(CacheType.CACHE)
        .setUnderStorageType(UnderStorageType.PERSIST).build();
    mWriteUnderStorage =
        new ClientOptions.Builder(mWorkerTachyonConf).setCacheType(CacheType.NO_CACHE)
            .setUnderStorageType(UnderStorageType.PERSIST).build();
  }

  @Test
  public void lockBlockTest1() throws IOException {
    String uniqPath = PathUtils.uniqPath();
    int numOfFiles = 5;
    int fileSize = WORKER_CAPACITY_BYTES / numOfFiles;
    List<TachyonFile> files = new ArrayList<TachyonFile>();
    for (int k = 0; k < numOfFiles; k ++) {
      files.add(TachyonFSTestUtils.createByteFile(mTfs, uniqPath + k, mWriteBoth, fileSize));
    }
    for (int k = 0; k < numOfFiles; k ++) {
      Assert.assertTrue(mTfs.getInfo(files.get(k)).getInMemoryPercentage() == 100);
    }
    files.add(TachyonFSTestUtils.createByteFile(mTfs, uniqPath + numOfFiles,
        mWriteBoth, fileSize));

    CommonUtils.sleepMs(mWorkerToMasterHeartbeatIntervalMs);

    Assert.assertFalse(mTfs.getInfo(files.get(0)).getInMemoryPercentage() == 100);
    for (int k = 1; k <= numOfFiles; k ++) {
      Assert.assertTrue(mTfs.getInfo(files.get(0)).getInMemoryPercentage() == 100);
    }
  }

  @Test
  public void lockBlockTest2() throws IOException {
    String uniqPath = PathUtils.uniqPath();
    TachyonFile tFile = null;
    tachyon.client.next.InStream is = null;
    ByteBuffer buf = null;
    int numOfFiles = 5;
    int fileSize = WORKER_CAPACITY_BYTES / numOfFiles;
    List<TachyonFile> files = new ArrayList<TachyonFile>();
    for (int k = 0; k < numOfFiles; k ++) {
      files.add(TachyonFSTestUtils.createByteFile(mTfs, uniqPath + k, mWriteBoth, fileSize));
    }
    for (int k = 0; k < numOfFiles; k ++) {
      FileInfo info = mTfs.getInfo(files.get(k));
      Assert.assertTrue(info.getInMemoryPercentage() == 100);
      is = mTfs.getInStream(files.get(k), mWriteBoth);
      buf = ByteBuffer.allocate((int) info.getBlockSizeByte());
      Assert.assertTrue(is.read(buf.array()) != -1);
      is.close();
    }
    files.add(TachyonFSTestUtils.createByteFile(mTfs, uniqPath + numOfFiles, mWriteBoth, fileSize));

    for (int k = 0; k < numOfFiles; k ++) {
      FileInfo info = mTfs.getInfo(files.get(k));
      Assert.assertTrue(info.getInMemoryPercentage() == 100);
    }
    CommonUtils.sleepMs(getSleepMs());
    FileInfo info = mTfs.getInfo(files.get(numOfFiles));;
    Assert.assertFalse(info.getInMemoryPercentage() == 100);
  }

  @Test
  public void lockBlockTest3() throws IOException {
    String uniqPath = PathUtils.uniqPath();
    TachyonFile tFile = null;
    tachyon.client.next.InStream is = null;
    ByteBuffer buf = null;
    int numOfFiles = 5;
    int fileSize = WORKER_CAPACITY_BYTES / numOfFiles;
    List<TachyonFile> files = new ArrayList<TachyonFile>();
    for (int k = 0; k < numOfFiles; k ++) {
      files.add(TachyonFSTestUtils.createByteFile(mTfs, uniqPath + k, mWriteBoth, fileSize));
    }
    for (int k = 0; k < numOfFiles; k ++) {
      FileInfo info = mTfs.getInfo(files.get(k));
      Assert.assertTrue(info.getInMemoryPercentage() == 100);
      is = mTfs.getInStream(files.get(k), mWriteBoth);
      buf = ByteBuffer.allocate((int) info.getBlockSizeByte());
      int r = is.read(buf.array());
      if (k < numOfFiles - 1) {
        Assert.assertTrue(r != -1);
      }
      is.close();
    }
    files.add(TachyonFSTestUtils.createByteFile(mTfs, uniqPath + numOfFiles, mWriteBoth, fileSize));

    for (int k = 0; k <= numOfFiles; k ++) {
      FileInfo info = mTfs.getInfo(files.get(k));
      if (k != numOfFiles - 1) {
        Assert.assertTrue(info.getInMemoryPercentage() == 100);
      } else {
        CommonUtils.sleepMs(null, getSleepMs());
        Assert.assertFalse(info.getInMemoryPercentage() == 100);
      }
    }
  }

  @Test
  public void unlockBlockTest1() throws IOException {
    String uniqPath = PathUtils.uniqPath();
    TachyonFile tFile = null;
    InStream is = null;
    ByteBuffer buf = null;
    int numOfFiles = 5;
    int fileSize = WORKER_CAPACITY_BYTES / numOfFiles;
    List<TachyonFile> files = new ArrayList<TachyonFile>();
    for (int k = 0; k < numOfFiles; k ++) {
      files.add(TachyonFSTestUtils.createByteFile(mTfs, uniqPath + k, mWriteBoth, fileSize));
    }
    for (int k = 0; k < numOfFiles; k ++) {
      FileInfo info = mTfs.getInfo(files.get(k));
      is = mTfs.getInStream(files.get(k), mWriteBoth);
      buf = ByteBuffer.allocate((int) info.getBlockSizeByte());
      Assert.assertTrue(info.getInMemoryPercentage() == 100);
      Assert.assertTrue(is.read(buf.array()) != -1);
      is.close();
    }
    files.add(TachyonFSTestUtils.createByteFile(mTfs, uniqPath + numOfFiles, mWriteBoth, fileSize));

    CommonUtils.sleepMs(getSleepMs());
    FileInfo info = mTfs.getInfo(files.get(0));
    Assert.assertFalse(info.getInMemoryPercentage() == 100);
    for (int k = 1; k <= numOfFiles; k ++) {
      FileInfo in = mTfs.getInfo(files.get(k));
      Assert.assertTrue(in.getInMemoryPercentage() == 100);
    }
  }

  @Test
  public void unlockBlockTest2() throws IOException {
    String uniqPath = PathUtils.uniqPath();
    TachyonFile tFile = null;
    InStream is = null;
    ByteBuffer buf = null;
    int numOfFiles = 5;
    int fileSize = WORKER_CAPACITY_BYTES / numOfFiles;
    List<TachyonFile> files = new ArrayList<TachyonFile>();
    for (int k = 0; k < numOfFiles; k ++) {
      files.add(TachyonFSTestUtils.createByteFile(mTfs, uniqPath + k, mWriteBoth, fileSize));
    }
    for (int k = 0; k < numOfFiles; k ++) {
      FileInfo info = mTfs.getInfo(files.get(k));
      Assert.assertTrue(info.getInMemoryPercentage() == 100);
      is = mTfs.getInStream(files.get(k), mWriteBoth);
      buf = ByteBuffer.allocate((int) info.getBlockSizeByte());
      Assert.assertTrue(is.read(buf.array()) != -1);
      is.seek(0);
      buf.clear();
      Assert.assertTrue(is.read(buf.array()) != -1);
      is.close();
    }
    files.add(TachyonFSTestUtils.createByteFile(mTfs, uniqPath + numOfFiles, mWriteBoth, fileSize));

    for (int k = 0; k < numOfFiles; k ++) {
      FileInfo info = mTfs.getInfo(files.get(k));
      Assert.assertTrue(info.getInMemoryPercentage() == 100);
    }
    CommonUtils.sleepMs(null, getSleepMs());
    FileInfo info = mTfs.getInfo(files.get(numOfFiles));
    Assert.assertFalse(info.getInMemoryPercentage() == 100);
  }

  @Test
  public void unlockBlockTest3() throws IOException {
    String uniqPath = PathUtils.uniqPath();
    TachyonFile tFile = null;
    InStream is = null;
    ByteBuffer buf1 = null;
    ByteBuffer buf2 = null;
    int numOfFiles = 5;
    int fileSize = WORKER_CAPACITY_BYTES / numOfFiles;
    List<TachyonFile> files = new ArrayList<TachyonFile>();
    for (int k = 0; k < numOfFiles; k ++) {
      files.add(TachyonFSTestUtils.createByteFile(mTfs, uniqPath + k, mWriteBoth,
          fileSize));
    }
    for (int k = 0; k < numOfFiles; k ++) {
      FileInfo info = mTfs.getInfo(files.get(k));
      Assert.assertTrue(info.getInMemoryPercentage() == 100);
      is = mTfs.getInStream(files.get(k), mWriteBoth);
      buf1 = ByteBuffer.allocate((int) info.getBlockSizeByte());
      Assert.assertTrue(is.read(buf1.array()) != -1);
      buf2 = ByteBuffer.allocate((int) info.getBlockSizeByte());
      is.seek(0);
      Assert.assertTrue(is.read(buf2.array()) != -1);
      is.close();
    }
    files.add(TachyonFSTestUtils.createByteFile(mTfs, uniqPath + numOfFiles, mWriteBoth, fileSize));

    CommonUtils.sleepMs(null, getSleepMs());
    FileInfo info = mTfs.getInfo(files.get(0));
    Assert.assertFalse(info.getInMemoryPercentage() == 100);
    for (int k = 1; k <= numOfFiles; k ++) {
      FileInfo in = mTfs.getInfo(files.get(k));
      Assert.assertTrue(in.getInMemoryPercentage() == 100);
    }
  }

  private long getSleepMs() {
    return mWorkerToMasterHeartbeatIntervalMs * 2 + 10;
  }
}