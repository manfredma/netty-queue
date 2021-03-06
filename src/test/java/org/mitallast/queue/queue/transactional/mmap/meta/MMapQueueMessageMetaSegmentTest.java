package org.mitallast.queue.queue.transactional.mmap.meta;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mitallast.queue.common.BaseTest;
import org.mitallast.queue.common.mmap.MemoryMappedFile;
import org.mitallast.queue.queue.QueueMessageStatus;
import org.mitallast.queue.queue.QueueMessageType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MMapQueueMessageMetaSegmentTest extends BaseTest {

    private final Random random = new Random();

    private MemoryMappedFile mmapFile;

    @Before
    public void setUp() throws Exception {
        mmapFile = new MemoryMappedFile(testFolder.newFile());
    }

    @After
    public void tearDown() throws Exception {
        mmapFile.close();
    }

    @Test
    public void testReadWrite() throws Exception {
        final MMapQueueMessageMetaSegment messageMetaSegment = new MMapQueueMessageMetaSegment(mmapFile, total(), 0.7f);
        final List<QueueMessageMeta> metaList = new ArrayList<>(total());
        long start, end;
        start = System.currentTimeMillis();
        for (int i = 0; i < total(); i++) {
            QueueMessageMeta meta = meta();
            metaList.add(meta);
            int pos = messageMetaSegment.insert(meta.getUuid());
            assert pos >= 0;
            assert messageMetaSegment.writeLock(pos);
            assert messageMetaSegment.writeMeta(meta, pos);
        }
        end = System.currentTimeMillis();
        printQps("write", total(), start, end);

        start = System.currentTimeMillis();
        for (int i = 0; i < total(); i++) {
            QueueMessageMeta expected = metaList.get(i);
            QueueMessageMeta actual = messageMetaSegment.readMeta(expected.getUuid());
            Assert.assertEquals(expected, actual);
        }
        end = System.currentTimeMillis();
        printQps("read", total(), start, end);
    }

    @Test
    public void testReadWriteConcurrent() throws Exception {
        long start, end;
        final MMapQueueMessageMetaSegment messageMetaSegment = new MMapQueueMessageMetaSegment(mmapFile, total(), 0.7f);
        final List<QueueMessageMeta> metaList = new ArrayList<>(total());
        for (int i = 0; i < total(); i++) {
            QueueMessageMeta meta = meta();
            metaList.add(meta);
        }

        start = System.currentTimeMillis();
        executeConcurrent((thread, concurrency) -> {
            try {
                for (int i = thread; i < total(); i += concurrency) {
                    QueueMessageMeta expected = metaList.get(i);
                    int pos = messageMetaSegment.insert(expected.getUuid());
                    assert pos >= 0;
                    assert messageMetaSegment.writeLock(pos);
                    assert messageMetaSegment.writeMeta(expected, pos);
                }
                for (int i = thread; i < total(); i += concurrency) {
                    QueueMessageMeta expected = metaList.get(i);
                    QueueMessageMeta actual = messageMetaSegment.readMeta(expected.getUuid());
                    Assert.assertEquals("i=" + i, expected, actual);
                }
            } catch (IOException e) {
                assert false : e;
            }
        });
        end = System.currentTimeMillis();
        printQps("read/write concurrent", total() * concurrency() * 2, start, end);
    }

    @Test
    public void testLock() throws IOException {
        final MMapQueueMessageMetaSegment messageMetaSegment = new MMapQueueMessageMetaSegment(mmapFile, total(), 0.7f);
        QueueMessageMeta meta = meta();

        int pos = messageMetaSegment.insert(meta.getUuid());
        assert pos >= 0;
        assert messageMetaSegment.writeLock(pos);
        assert messageMetaSegment.writeMeta(meta, pos);
        QueueMessageMeta metaLocked = messageMetaSegment.lock(meta.getUuid());
        Assert.assertEquals(meta, metaLocked);
        Assert.assertEquals(QueueMessageStatus.LOCKED, metaLocked.getStatus());
    }

    @Test
    public void testUnlockAndDelete() throws IOException {
        final MMapQueueMessageMetaSegment messageMetaSegment = new MMapQueueMessageMetaSegment(mmapFile, total(), 0.7f);
        QueueMessageMeta meta = meta();

        int pos = messageMetaSegment.insert(meta.getUuid());
        assert pos >= 0;
        assert messageMetaSegment.writeLock(pos);
        assert messageMetaSegment.writeMeta(meta, pos);
        assert messageMetaSegment.lock(meta.getUuid()) != null;
        QueueMessageMeta deleted = messageMetaSegment.unlockAndDelete(meta.getUuid());

        Assert.assertEquals(meta, deleted);
        Assert.assertEquals(QueueMessageStatus.DELETED, deleted.getStatus());
    }

    @Test
    public void testUnlockAndQueue() throws IOException {
        final MMapQueueMessageMetaSegment messageMetaSegment = new MMapQueueMessageMetaSegment(mmapFile, total(), 0.7f);
        QueueMessageMeta meta = meta();

        int pos = messageMetaSegment.insert(meta.getUuid());
        assert pos >= 0;
        assert messageMetaSegment.writeLock(pos);
        assert messageMetaSegment.writeMeta(meta, pos);
        assert messageMetaSegment.lock(meta.getUuid()) != null;
        QueueMessageMeta deleted = messageMetaSegment.unlockAndQueue(meta.getUuid());

        Assert.assertEquals(meta, deleted);
        Assert.assertEquals(QueueMessageStatus.QUEUED, deleted.getStatus());
    }

    @Test
    public void testPeek() throws IOException {
        final MMapQueueMessageMetaSegment messageMetaSegment = new MMapQueueMessageMetaSegment(mmapFile, total(), 0.7f);

        Assert.assertNull(messageMetaSegment.peek());

        QueueMessageMeta meta = meta();
        int pos = messageMetaSegment.insert(meta.getUuid());
        assert pos >= 0;
        assert messageMetaSegment.writeLock(pos);
        assert messageMetaSegment.writeMeta(meta, pos);

        Assert.assertEquals(meta, messageMetaSegment.peek());
        Assert.assertEquals(meta, messageMetaSegment.peek());
    }

    @Test
    public void testIsGarbage() throws IOException {
        final MMapQueueMessageMetaSegment messageMetaSegment = new MMapQueueMessageMetaSegment(mmapFile, 3, 0.7f);

        QueueMessageMeta meta1 = meta();
        QueueMessageMeta meta2 = meta();
        QueueMessageMeta meta3 = meta();

        int pos1 = messageMetaSegment.insert(meta1.getUuid());
        int pos2 = messageMetaSegment.insert(meta2.getUuid());
        int pos3 = messageMetaSegment.insert(meta3.getUuid());

        messageMetaSegment.writeLock(pos1);
        messageMetaSegment.writeLock(pos2);
        messageMetaSegment.writeLock(pos3);

        messageMetaSegment.writeMeta(meta1, pos1);
        messageMetaSegment.writeMeta(meta2, pos2);
        messageMetaSegment.writeMeta(meta3, pos3);

        messageMetaSegment.lock(meta1.getUuid());
        messageMetaSegment.lock(meta2.getUuid());
        messageMetaSegment.lock(meta3.getUuid());

        messageMetaSegment.unlockAndDelete(meta1.getUuid());
        assert !messageMetaSegment.isGarbage();
        messageMetaSegment.unlockAndDelete(meta2.getUuid());
        assert !messageMetaSegment.isGarbage();
        messageMetaSegment.unlockAndDelete(meta3.getUuid());
        assert messageMetaSegment.isGarbage();
    }

    @Test
    public void testReopen() throws IOException {
        final MMapQueueMessageMetaSegment messageMetaSegment = new MMapQueueMessageMetaSegment(mmapFile, 3, 0.7f);
        Assert.assertEquals(0, messageMetaSegment.size());

        QueueMessageMeta meta1 = meta();
        QueueMessageMeta meta2 = meta();
        QueueMessageMeta meta3 = meta();

        int pos1 = messageMetaSegment.insert(meta1.getUuid());
        int pos2 = messageMetaSegment.insert(meta2.getUuid());
        int pos3 = messageMetaSegment.insert(meta3.getUuid());

        messageMetaSegment.writeLock(pos1);
        messageMetaSegment.writeLock(pos2);
        messageMetaSegment.writeLock(pos3);

        messageMetaSegment.writeMeta(meta1, pos1);
        messageMetaSegment.writeMeta(meta2, pos2);
        messageMetaSegment.writeMeta(meta3, pos3);

        messageMetaSegment.close();

        MemoryMappedFile mmapFileReopen = new MemoryMappedFile(mmapFile.getFile());
        final MMapQueueMessageMetaSegment messageMetaSegmentReopen = new MMapQueueMessageMetaSegment(mmapFileReopen, 3, 0.7f);
        Assert.assertEquals(3, messageMetaSegment.size());


        Assert.assertEquals(meta1, messageMetaSegmentReopen.readMeta(meta1.getUuid()));
        Assert.assertEquals(meta2, messageMetaSegmentReopen.readMeta(meta2.getUuid()));
        Assert.assertEquals(meta3, messageMetaSegmentReopen.readMeta(meta3.getUuid()));
    }

    private QueueMessageMeta meta() {
        return new QueueMessageMeta(
            randomUUID(),
            QueueMessageStatus.QUEUED,
            random.nextInt(),
            random.nextInt(),
            QueueMessageType.STRING
        );
    }
}
