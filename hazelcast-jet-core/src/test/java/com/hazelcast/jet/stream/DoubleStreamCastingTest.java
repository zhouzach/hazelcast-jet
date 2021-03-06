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

package com.hazelcast.jet.stream;

import org.junit.Before;
import org.junit.Test;

import java.util.stream.DoubleStream;

public class DoubleStreamCastingTest extends AbstractStreamTest {

    private DoubleStream stream;

    @Before
    public void setUp() {
        IStreamList<Integer> list = getList();
        stream = list.stream().mapToDouble(m -> m);
    }

    @Test(expected = IllegalArgumentException.class)
    public void map() {
        stream.map(m -> m);
    }

    @Test(expected = IllegalArgumentException.class)
    public void flatMap() {
        stream.flatMap(DoubleStream::of);
    }

    @Test(expected = IllegalArgumentException.class)
    public void collect() {
        stream.collect(() -> new Double[]{0D},
                (r, e) -> r[0] += e,
                (a, b) -> a[0] += b[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void forEach() {
        stream.forEach(System.out::println);
    }

    @Test(expected = IllegalArgumentException.class)
    public void forEachOrdered() {
        stream.forEachOrdered(System.out::println);
    }

    @Test(expected = IllegalArgumentException.class)
    public void allMatch() {
        stream.allMatch(m -> true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void anyMatch() {
        stream.anyMatch(m -> true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void noneMatch() {
        stream.noneMatch(m -> true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void filter() {
        stream.filter(m -> true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapToObj() {
        stream.mapToObj(m -> m);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapToInt() {
        stream.mapToInt(m -> (int) m);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapToLong() {
        stream.mapToLong(m -> (long) m);
    }

    @Test(expected = IllegalArgumentException.class)
    public void peek() {
        stream.peek(System.out::println);
    }

    @Test(expected = IllegalArgumentException.class)
    public void reduce() {
        stream.reduce((l, r) -> l + r);
    }

    @Test(expected = IllegalArgumentException.class)
    public void reduce2() {
        stream.reduce(0, (l, r) -> l + r);
    }
}
