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

package com.hazelcast.jet.datamodel;

import com.hazelcast.jet.impl.serialization.SerializerHookConstants;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.Serializer;
import com.hazelcast.nio.serialization.SerializerHook;
import com.hazelcast.nio.serialization.StreamSerializer;

import java.io.IOException;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

import static com.hazelcast.jet.datamodel.ItemsByTag.NONE;
import static com.hazelcast.jet.datamodel.ThreeBags.threeBags;
import static com.hazelcast.jet.datamodel.Tuple2.tuple2;
import static com.hazelcast.jet.datamodel.Tuple3.tuple3;
import static com.hazelcast.jet.datamodel.TwoBags.twoBags;

/**
 * Hazelcast serializer hooks for the classes in the {@code
 * com.hazelcast.jet.datamodel} package. This is not a public-facing API.
 */
class DataModelSerializerHooks {
    public static final class TimestampedEntryHook implements SerializerHook<TimestampedEntry> {

        @Override
        public Class<TimestampedEntry> getSerializationType() {
            return TimestampedEntry.class;
        }

        @Override
        public Serializer createSerializer() {
            return new StreamSerializer<TimestampedEntry>() {
                @Override
                public void write(ObjectDataOutput out, TimestampedEntry object) throws IOException {
                    out.writeLong(object.getTimestamp());
                    out.writeObject(object.getKey());
                    out.writeObject(object.getValue());
                }

                @Override
                public TimestampedEntry read(ObjectDataInput in) throws IOException {
                    long timestamp = in.readLong();
                    Object key = in.readObject();
                    Object value = in.readObject();
                    return new TimestampedEntry<>(timestamp, key, value);
                }

                @Override
                public int getTypeId() {
                    return SerializerHookConstants.TIMESTAMPED_ENTRY;
                }

                @Override
                public void destroy() {
                }
            };
        }

        @Override public boolean isOverwritable() {
            return false;
        }
    }

    public static final class SessionHook implements SerializerHook<Session> {

        @Override
        public Class<Session> getSerializationType() {
            return Session.class;
        }

        @Override
        public Serializer createSerializer() {
            return new StreamSerializer<Session>() {
                @Override
                public void write(ObjectDataOutput out, Session object) throws IOException {
                    out.writeObject(object.getKey());
                    out.writeLong(object.getStart());
                    out.writeLong(object.getEnd());
                    out.writeObject(object.getResult());
                }

                @Override
                public Session read(ObjectDataInput in) throws IOException {
                    Object key = in.readObject();
                    long start = in.readLong();
                    long end = in.readLong();
                    Object result = in.readObject();
                    return new Session<>(key, start, end, result);
                }

                @Override
                public int getTypeId() {
                    return SerializerHookConstants.SESSION;
                }

                @Override
                public void destroy() {
                }
            };
        }

        @Override public boolean isOverwritable() {
            return false;
        }
    }

    public static final class Tuple2Hook implements SerializerHook<Tuple2> {

        @Override
        public Class<Tuple2> getSerializationType() {
            return Tuple2.class;
        }

        @Override
        public Serializer createSerializer() {
            return new StreamSerializer<Tuple2>() {
                @Override
                public void write(ObjectDataOutput out, Tuple2 t) throws IOException {
                    out.writeObject(t.f0());
                    out.writeObject(t.f1());
                }

                @Override
                public Tuple2 read(ObjectDataInput in) throws IOException {
                    return tuple2(in.readObject(), in.readObject());
                }

                @Override
                public int getTypeId() {
                    return SerializerHookConstants.TUPLE2;
                }

                @Override
                public void destroy() {
                }
            };
        }

        @Override public boolean isOverwritable() {
            return false;
        }
    }

    public static final class Tuple3Hook implements SerializerHook<Tuple3> {

        @Override
        public Class<Tuple3> getSerializationType() {
            return Tuple3.class;
        }

        @Override
        public Serializer createSerializer() {
            return new StreamSerializer<Tuple3>() {
                @Override
                public void write(ObjectDataOutput out, Tuple3 t) throws IOException {
                    out.writeObject(t.f0());
                    out.writeObject(t.f1());
                    out.writeObject(t.f2());
                }

                @Override
                public Tuple3 read(ObjectDataInput in) throws IOException {
                    return tuple3(in.readObject(), in.readObject(), in.readObject());
                }

                @Override
                public int getTypeId() {
                    return SerializerHookConstants.TUPLE3;
                }

                @Override
                public void destroy() {
                }
            };
        }

        @Override public boolean isOverwritable() {
            return false;
        }
    }

    public static final class TwoBagsHook implements SerializerHook<TwoBags> {

        @Override
        public Class<TwoBags> getSerializationType() {
            return TwoBags.class;
        }

        @Override
        public Serializer createSerializer() {
            return new StreamSerializer<TwoBags>() {
                @Override
                public void write(ObjectDataOutput out, TwoBags t) throws IOException {
                    out.writeObject(t.bag0());
                    out.writeObject(t.bag1());
                }

                @Override
                public TwoBags read(ObjectDataInput in) throws IOException {
                    return twoBags(in.readObject(), in.readObject());
                }

                @Override
                public int getTypeId() {
                    return SerializerHookConstants.TWO_BAGS;
                }

                @Override
                public void destroy() {
                }
            };
        }

        @Override public boolean isOverwritable() {
            return false;
        }
    }

    public static final class ThreeBagsHook implements SerializerHook<ThreeBags> {

        @Override
        public Class<ThreeBags> getSerializationType() {
            return ThreeBags.class;
        }

        @Override
        public Serializer createSerializer() {
            return new StreamSerializer<ThreeBags>() {
                @Override
                public void write(ObjectDataOutput out, ThreeBags t) throws IOException {
                    out.writeObject(t.bag0());
                    out.writeObject(t.bag1());
                    out.writeObject(t.bag2());
                }

                @Override
                public ThreeBags read(ObjectDataInput in) throws IOException {
                    return threeBags(in.readObject(), in.readObject(), in.readObject());
                }

                @Override
                public int getTypeId() {
                    return SerializerHookConstants.THREE_BAGS;
                }

                @Override
                public void destroy() {
                }
            };
        }

        @Override public boolean isOverwritable() {
            return false;
        }
    }

    public static final class TagHook implements SerializerHook<Tag> {

        @Override
        public Class<Tag> getSerializationType() {
            return Tag.class;
        }

        @Override
        public Serializer createSerializer() {
            return new StreamSerializer<Tag>() {
                @Override
                public void write(ObjectDataOutput out, Tag tag) throws IOException {
                    out.writeInt(tag.index());
                }

                @Override
                public Tag read(ObjectDataInput in) throws IOException {
                    return Tag.tag(in.readInt());
                }

                @Override
                public int getTypeId() {
                    return SerializerHookConstants.TAG;
                }

                @Override
                public void destroy() {
                }
            };
        }

        @Override public boolean isOverwritable() {
            return false;
        }
    }

    public static final class ItemsByTagHook implements SerializerHook<ItemsByTag> {

        @Override
        public Class<ItemsByTag> getSerializationType() {
            return ItemsByTag.class;
        }

        @Override
        public Serializer createSerializer() {
            return new ItemsByTagSerializer();
        }

        @Override public boolean isOverwritable() {
            return false;
        }

    }

    public static final class BagsByTagHook implements SerializerHook<BagsByTag> {

        @Override
        public Class<BagsByTag> getSerializationType() {
            return BagsByTag.class;
        }

        @Override
        public Serializer createSerializer() {
            return new BagsByTagSerializer();
        }

        @Override public boolean isOverwritable() {
            return false;
        }

    }

    private static class ItemsByTagSerializer implements StreamSerializer<ItemsByTag> {
        @Override
        public void write(ObjectDataOutput out, ItemsByTag ibt) throws IOException {
            Set<Entry<Tag<?>, Object>> entries = ibt.entrySet();
            out.writeInt(entries.size());
            for (Entry<Tag<?>, Object> e : entries) {
                out.writeObject(e.getKey());
                Object val = e.getValue();
                out.writeObject(val != NONE ? val : null);
            }
        }

        @Override
        public ItemsByTag read(ObjectDataInput in) throws IOException {
            int size = in.readInt();
            ItemsByTag ibt = new ItemsByTag();
            for (int i = 0; i < size; i++) {
                ibt.put(in.readObject(), in.readObject());
            }
            return ibt;
        }

        @Override
        public int getTypeId() {
            return SerializerHookConstants.ITEMS_BY_TAG;
        }

        @Override
        public void destroy() {
        }
    }

    private static class BagsByTagSerializer implements StreamSerializer<BagsByTag> {
        @Override
        public void write(ObjectDataOutput out, BagsByTag bbt) throws IOException {
            Set<Entry<Tag<?>, Collection>> entries = bbt.entrySet();
            out.writeInt(entries.size());
            for (Entry<Tag<?>, Collection> e : entries) {
                out.writeObject(e.getKey());
                out.writeObject(e.getValue());
            }
        }

        @Override
        public BagsByTag read(ObjectDataInput in) throws IOException {
            int size = in.readInt();
            BagsByTag bbt = new BagsByTag();
            for (int i = 0; i < size; i++) {
                bbt.put(in.readObject(), in.readObject());
            }
            return bbt;
        }

        @Override
        public int getTypeId() {
            return SerializerHookConstants.BAGS_BY_TAG;
        }

        @Override
        public void destroy() {
        }
    }
}
