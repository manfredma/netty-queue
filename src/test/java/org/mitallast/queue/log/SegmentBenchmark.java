package org.mitallast.queue.log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mitallast.queue.common.BaseTest;
import org.mitallast.queue.common.settings.ImmutableSettings;
import org.mitallast.queue.common.stream.InternalStreamService;
import org.mitallast.queue.common.stream.StreamService;
import org.mitallast.queue.common.unit.ByteSizeUnit;
import org.mitallast.queue.log.entry.LogEntry;


public class SegmentBenchmark extends BaseTest {
    private StreamService streamService;
    private SegmentDescriptor descriptor;
    private Segment segment;
    private SegmentIndex segmentIndex;

    @Override
    protected int max() {
        return 1000000;
    }

    @Before
    public void setUp() throws Exception {
        streamService = new InternalStreamService(ImmutableSettings.EMPTY);
        new LogStreamService(streamService);
        descriptor = SegmentDescriptor.builder()
            .setId(0)
            .setIndex(1)
            .setMaxEntries(max())
            .setMaxEntrySize(1000)
            .setMaxSegmentSize(ByteSizeUnit.GB.toBytes(1))
            .setVersion(0)
            .build();

        segmentIndex = new SegmentIndex(testFolder.newFile(), (int) descriptor.maxEntries());
        segment = new Segment(testFolder.newFile(), descriptor, segmentIndex, streamService);
    }

    @After
    public void tearDown() throws Exception {
        segment.close();
        segmentIndex.close();
    }

    @Test
    public void testAppend() throws Exception {
        LogEntry[] entries = LogEntryGenerator.generate(max());
        long start = System.currentTimeMillis();
        for (LogEntry entry : entries) {
            segment.appendEntry(entry);
        }
        long end = System.currentTimeMillis();
        printQps("append", max(), start, end);
    }
}
