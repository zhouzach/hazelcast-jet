/*
 * Copyright (c) 2008-2017, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet.impl.execution;

import com.hazelcast.jet.Inbox;
import com.hazelcast.jet.Outbox;
import com.hazelcast.jet.Processor;
import com.hazelcast.jet.Processor.Context;
import com.hazelcast.jet.impl.util.ArrayDequeOutbox;
import com.hazelcast.jet.impl.util.ProgressState;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

import static com.hazelcast.jet.impl.util.DoneItem.DONE_ITEM;
import static com.hazelcast.jet.impl.util.ProgressState.DONE;
import static com.hazelcast.jet.impl.util.ProgressState.NO_PROGRESS;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@Category(QuickTest.class)
public class ProcessorTaskletTest {

    private static final int MOCK_INPUT_LENGTH = 10;
    private static final int CALL_COUNT_LIMIT = 10;
    private List<Object> mockInput;
    private List<InboundEdgeStream> instreams;
    private List<OutboundEdgeStream> outstreams;
    private MockProcessor processor;
    private Context context;


    @Before
    public void setUp() throws Exception {
        this.mockInput = IntStream.range(0, MOCK_INPUT_LENGTH).boxed().collect(toList());
        this.processor = new MockProcessor();
        this.context = mock(Context.class);
        this.instreams = new ArrayList<>();
        this.outstreams = new ArrayList<>();
    }

    @Test
    public void when_singleInstreamAndOutstream_then_outstreamGetsAll() throws Exception {
        // Given
        mockInput.add(DONE_ITEM);
        MockInboundStream instream1 = new MockInboundStream(0, mockInput, mockInput.size());
        MockOutboundStream outstream1 = new MockOutboundStream(0, mockInput.size());
        instreams.add(instream1);
        outstreams.add(outstream1);
        Tasklet tasklet = createTasklet();

        // When
        callUntil(tasklet, DONE);

        // Then
        assertEquals(mockInput, outstream1.getBuffer());
    }

    @Test
    public void when_oneInstreamAndTwoOutstreams_then_allOutstreamsGetAllItems() throws Exception {
        // Given
        mockInput.add(DONE_ITEM);
        MockInboundStream instream1 = new MockInboundStream(0, mockInput, mockInput.size());
        MockOutboundStream outstream1 = new MockOutboundStream(0, mockInput.size());
        MockOutboundStream outstream2 = new MockOutboundStream(1, mockInput.size());
        instreams.add(instream1);
        outstreams.add(outstream1);
        outstreams.add(outstream2);
        Tasklet tasklet = createTasklet();

        // When
        callUntil(tasklet, DONE);

        // Then
        assertEquals(mockInput, outstream1.getBuffer());
        assertEquals(mockInput, outstream2.getBuffer());
    }

    @Test
    public void when_instreamChunked_then_processAllEventually() throws Exception {
        // Given
        mockInput.add(DONE_ITEM);
        MockInboundStream instream1 = new MockInboundStream(0, mockInput, 4);
        MockOutboundStream outstream1 = new MockOutboundStream(0, mockInput.size());
        instreams.add(instream1);
        outstreams.add(outstream1);
        Tasklet tasklet = createTasklet();

        // When
        callUntil(tasklet, DONE);

        // Then
        assertEquals(mockInput, outstream1.getBuffer());
    }

    @Test
    public void when_3instreams_then_pushAllIntoOutstream() throws Exception {
        // Given
        MockInboundStream instream1 = new MockInboundStream(0, mockInput.subList(0, 4), 4);
        MockInboundStream instream2 = new MockInboundStream(1, mockInput.subList(4, 8), 4);
        MockInboundStream instream3 = new MockInboundStream(2, mockInput.subList(8, 10), 4);
        instream1.push(DONE_ITEM);
        instream2.push(DONE_ITEM);
        instream3.push(DONE_ITEM);
        MockOutboundStream outstream1 = new MockOutboundStream(0, 20);
        instreams.add(instream1);
        instreams.add(instream2);
        instreams.add(instream3);
        outstreams.add(outstream1);
        Tasklet tasklet = createTasklet();

        // When
        callUntil(tasklet, DONE);

        // Then
        mockInput.add(DONE_ITEM);
        assertEquals(new HashSet<>(mockInput), new HashSet<>(outstream1.getBuffer()));
    }

    @Test
    public void when_outstreamRefusesItem_then_noProgress() throws Exception {
        // Given
        MockInboundStream instream1 = new MockInboundStream(0, mockInput, 4);
        MockOutboundStream outstream1 = new MockOutboundStream(0, 4);
        instreams.add(instream1);
        outstreams.add(outstream1);
        Tasklet tasklet = createTasklet();

        // When
        callUntil(tasklet, NO_PROGRESS);

        // Then
        assertTrue("isEmpty", outstream1.getBuffer().equals(mockInput.subList(0, 4)));
    }

    private Tasklet createTasklet() {
        final ProcessorTasklet t = new ProcessorTasklet("mock", context, processor,
                instreams, outstreams);
        t.init();
        return t;
    }

    private static class MockProcessor implements Processor {
        private ArrayDequeOutbox outbox;

        @Override
        public void init(@Nonnull Outbox outbox, @Nonnull Context context) {
            this.outbox = (ArrayDequeOutbox) outbox;
        }

        @Override
        public void process(int ordinal, @Nonnull Inbox inbox) {
            for (Object item; (item = inbox.poll()) != null; ) {
                for (int i = 0; i < outbox.bucketCount(); i++) {
                    outbox.add(i, item);
                }
            }
        }
    }

    private static void callUntil(Tasklet tasklet, ProgressState expectedState) throws Exception {
        int iterCount = 0;
        for (ProgressState r; (r = tasklet.call()) != expectedState; ) {
            System.out.println(r);
            assertTrue("Failed to make progress: " + r, r.isMadeProgress());
            assertTrue(String.format(
                    "tasklet.call() invoked %d times without reaching %s. Last state was %s",
                    CALL_COUNT_LIMIT, expectedState, r),
                    ++iterCount < CALL_COUNT_LIMIT);
        }
    }
}
