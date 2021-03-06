package org.mitallast.queue.queue.transactional.mmap.data;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mitallast.queue.common.BaseTest;
import org.mitallast.queue.common.mmap.MemoryMappedFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MMapQueueMessageAppendSegmentTest extends BaseTest {

    private MemoryMappedFile mmapFile;
    private QueueMessageAppendSegment segment;
    private List<ByteBuf> bufferList;
    private long[] offsets;
    private int length;

    @Before
    public void setUp() throws Exception {
        mmapFile = new MemoryMappedFile(testFolder.newFile(), 65536, 50);
        segment = new MMapQueueMessageAppendSegment(mmapFile);
        bufferList = new ArrayList<>(max());
        offsets = new long[max()];
        length = 256;
        for (int i = 0; i < max(); i++) {
            ByteBuf buffer = Unpooled.buffer(length);
            random.nextBytes(buffer.array());
            buffer.writerIndex(buffer.writerIndex() + length);
            bufferList.add(buffer);
        }
    }

    @After
    public void tearDown() throws Exception {
        mmapFile.close();
    }

    @Test
    public void testReadWrite() throws Exception {
        for (int i = 0; i < max(); i++) {
            offsets[i] = segment.append(bufferList.get(i));
        }
        ByteBuf buffer = Unpooled.buffer(length);
        for (int i = 0; i < max(); i++) {
            buffer.clear();
            segment.read(buffer, offsets[i], length);
            buffer.resetReaderIndex();
            ByteBuf expected = bufferList.get(i);
            expected.resetReaderIndex();
            Assert.assertTrue(ByteBufUtil.equals(expected, buffer));
        }
    }

    @Test
    public void testReadWriteConcurrent() throws Exception {
        executeConcurrent((thread, concurrency) -> {
            try {
                for (int i = thread; i < max(); i += concurrency) {
                    offsets[i] = segment.append(bufferList.get(i));
                }
                ByteBuf buffer = Unpooled.buffer();
                for (int i = thread; i < max(); i += concurrency) {
                    buffer.resetWriterIndex();
                    segment.read(buffer, offsets[i], length);
                    buffer.resetReaderIndex();
                    ByteBuf expected = bufferList.get(i);
                    expected.resetReaderIndex();
                    Assert.assertTrue(ByteBufUtil.equals(expected, buffer));
                }
                buffer.release();
            } catch (IOException e) {
                assert false : e;
            }
        });
    }
}
