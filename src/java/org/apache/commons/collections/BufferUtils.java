/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//collections/src/java/org/apache/commons/collections/BufferUtils.java,v 1.9 2002/10/13 00:38:36 scolebourne Exp $
 * $Revision: 1.9 $
 * $Date: 2002/10/13 00:38:36 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.apache.commons.collections;

import java.util.Collection;
/**
 * Contains static utility methods for operating on {@link Buffer} objects.
 *
 * @author Paul Jack
 * @author Stephen Colebourne
 * @version $Id: BufferUtils.java,v 1.9 2002/10/13 00:38:36 scolebourne Exp $
 * @since 2.1
 */
public class BufferUtils {

    /**
     * Restrictive constructor
     */
    private BufferUtils() {
    }


    /**
     * Returns a synchronized buffer backed by the given buffer.
     * Much like the synchronized collections returned by 
     * {@link java.util.Collections}, you must manually synchronize on 
     * the returned buffer's iterator to avoid non-deterministic behavior:
     *  
     * <pre>
     * Buffer b = BufferUtils.synchronizedBuffer(myBuffer);
     * synchronized (b) {
     *     Iterator i = b.iterator();
     *     while (i.hasNext()) {
     *         process (i.next());
     *     }
     * }
     * </pre>
     *
     * @param buffer  the buffer to synchronize, must not be null
     * @return a synchronized buffer backed by that buffer
     * @throws IllegalArgumentException  if the Buffer is null
     */
    public static Buffer synchronizedBuffer(final Buffer buffer) {
        return new SynchronizedBuffer(buffer);
    }

    /**
     * Returns a synchronized buffer backed by the given buffer that will
     * block on {@link Buffer#get()} and {@link Buffer#remove()} operations.
     * If the buffer is empty, then the {@link Buffer#get()} and 
     * {@link Buffer#remove()} operations will block until new elements
     * are added to the buffer, rather than immediately throwing a 
     * <code>BufferUnderflowException</code>.
     *
     * @param buffer  the buffer to synchronize, must not be null
     * @return a blocking buffer backed by that buffer
     * @throws IllegalArgumentException  if the Buffer is null
     */
    public static Buffer blockingBuffer(Buffer buffer) {
        return new SynchronizedBuffer(buffer) {

            public synchronized boolean add(Object o) {
                boolean r = collection.add(o);
                notify();
                return r;
            }

            public synchronized boolean addAll(Collection c) {
                boolean r = collection.addAll(c);
                notifyAll();
                return r;
            }

            public synchronized Object get() {
                while (collection.isEmpty()) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new BufferUnderflowException();
                    }
                }
                return ((Buffer)collection).get();
            }

            public synchronized Object remove() {
                while (collection.isEmpty()) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new BufferUnderflowException();
                    }
                }
                return ((Buffer)collection).remove();
            }
        };
    }

    /**
     * Returns an unmodifiable buffer backed by the given buffer.
     *
     * @param buffer  the buffer to make unmodifiable, must not be null
     * @return an unmodifiable buffer backed by that buffer
     * @throws IllegalArgumentException  if the Buffer is null
     */
    public static Buffer unmodifiableBuffer(Buffer buffer) {
        return new UnmodifiableBuffer(buffer);
    }

    /**
     * Returns a predicated buffer backed by the given buffer.  Elements are
     * evaluated with the given predicate before being added to the buffer.
     * If the predicate evaluation returns false, then an 
     * IllegalArgumentException is raised and the element is not added to
     * the buffer.
     *
     * @param buffer  the buffer to predicate, must not be null
     * @param predicate  the predicate used to evaluate new elements, must not be null
     * @return a predicated buffer
     * @throws IllegalArgumentException  if the Buffer or Predicate is null
     */
    public static Buffer predicatedBuffer(Buffer buffer, final Predicate predicate) {
        return new PredicatedBuffer(buffer, predicate);
    }



    static class SynchronizedBuffer 
            extends CollectionUtils.SynchronizedCollection
            implements Buffer {

        public SynchronizedBuffer(Buffer b) {
            super(b);
        }

        public synchronized Object get() {
            return ((Buffer)collection).get();
        }

        public synchronized Object remove() {
            return ((Buffer)collection).remove();
        }        
    }


    static class UnmodifiableBuffer 
            extends CollectionUtils.UnmodifiableCollection
            implements Buffer {

        public UnmodifiableBuffer(Buffer b) {
            super(b);
        }

        public Object get() {
            return ((Buffer)collection).get();
        }

        public Object remove() {
            throw new UnsupportedOperationException();
        }

    }


    static class PredicatedBuffer 
            extends CollectionUtils.PredicatedCollection
            implements Buffer {

        public PredicatedBuffer(Buffer b, Predicate p) {
            super(b, p);
        }

        public Object get() {
            return ((Buffer)collection).get();
        }

        public Object remove() {
            return ((Buffer)collection).remove();
        }

    }


}
