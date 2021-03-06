package org.mitallast.queue.log;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mitallast.queue.common.BaseTest;
import org.mitallast.queue.common.settings.ImmutableSettings;
import org.mitallast.queue.common.stream.InternalStreamService;
import org.mitallast.queue.common.stream.StreamService;
import org.mitallast.queue.common.unit.ByteSizeUnit;
import org.mitallast.queue.log.entry.LogEntry;
import org.mitallast.queue.log.entry.TextLogEntry;
import org.unitils.reflectionassert.ReflectionAssert;

import java.io.File;

public class SegmentTest extends BaseTest {
    private StreamService streamService;
    private SegmentDescriptor descriptor;
    private Segment segment;
    private SegmentIndex segmentIndex;

    @Before
    public void setUp() throws Exception {
        streamService = new InternalStreamService(ImmutableSettings.EMPTY);
        new LogStreamService(streamService);

        descriptor = SegmentDescriptor.builder()
            .setId(0)
            .setIndex(1)
            .setMaxEntries(100)
            .setMaxEntrySize(1000)
            .setMaxSegmentSize(ByteSizeUnit.MB.toBytes(10))
            .setVersion(0)
            .build();

        File file = testFolder.newFile();
        segmentIndex = new SegmentIndex(testFolder.newFile(), (int) descriptor.maxEntries());
        segment = new Segment(file, descriptor, segmentIndex, streamService);
    }

    @After
    public void tearDown() throws Exception {
        segment.close();
        segmentIndex.close();
    }

    @Test
    public void testAppendEntry() throws Exception {
        LogEntry[] entries = generate((int) segment.descriptor().maxEntries());
        Assert.assertEquals(segment.size(), segmentIndex.nextPosition());
        for (LogEntry entry : entries) {
            segment.appendEntry(entry);
            Assert.assertEquals(segment.size(), segmentIndex.nextPosition());
        }

        for (LogEntry entry : entries) {
            LogEntry actual = segment.getEntry(entry.index());
            Assert.assertNotNull(actual);
            ReflectionAssert.assertReflectionEquals(actual, entry);
        }
    }

    private LogEntry[] generate(int max) {
        LogEntry[] logEntries = new LogEntry[max];
        for (int i = 0; i < max; i++) {
            logEntries[i] = TextLogEntry.builder()
                .setIndex(i + 1)
                .setMessage(randomUUID().toString())
                .build();
        }
        return logEntries;
    }

    @Test
    public void testReopen() throws Exception {
        LogEntry[] entries = generate((int) segment.descriptor().maxEntries());
        for (LogEntry entry : entries) {
            segment.appendEntry(entry);
        }

        segment.flush();

        try (SegmentIndex reopenSegmentIndex = new SegmentIndex(segmentIndex.file(), (int) descriptor.maxEntries());
             Segment reopenSegment = new Segment(segment.file(), descriptor, reopenSegmentIndex, streamService)
        ) {
            ReflectionAssert.assertReflectionEquals(descriptor, reopenSegment.descriptor());
            Assert.assertFalse(reopenSegment.isEmpty());
            Assert.assertTrue(reopenSegment.isFull());
            Assert.assertEquals(segmentIndex.nextPosition(), reopenSegmentIndex.nextPosition());
            Assert.assertEquals(segment.size(), reopenSegment.size());
            Assert.assertEquals(segment.length(), reopenSegment.length());
            Assert.assertEquals(segment.firstIndex(), reopenSegment.firstIndex());
            Assert.assertEquals(segment.lastIndex(), reopenSegment.lastIndex());
            Assert.assertEquals(segment.nextIndex(), reopenSegment.nextIndex());

            for (LogEntry entry : entries) {
                Assert.assertTrue(segment.containsIndex(entry.index()));
                Assert.assertTrue(segment.containsEntry(entry.index()));
                ReflectionAssert.assertReflectionEquals(entry, segment.getEntry(entry.index()));
            }
        }
    }
}
