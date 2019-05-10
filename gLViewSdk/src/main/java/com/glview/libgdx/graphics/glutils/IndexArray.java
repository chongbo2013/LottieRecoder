/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.glview.libgdx.graphics.glutils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.glview.utils.BufferUtils;

public class IndexArray implements IndexData {
	final static IntBuffer tmpHandle = BufferUtils.newIntBuffer(1);

	ShortBuffer buffer;
	ByteBuffer byteBuffer;

	/** Creates a new IndexArray to be used with vertex arrays.
	 * 
	 * @param maxIndices the maximum number of indices this buffer can hold */
	public IndexArray (int maxIndices) {
		byteBuffer = BufferUtils.newByteBuffer(maxIndices * 2);
		buffer = byteBuffer.asShortBuffer();
		buffer.flip();
		byteBuffer.flip();
	}

	/** @return the number of indices currently stored in this buffer */
	public int getNumIndices () {
		return buffer.limit();
	}

	/** @return the maximum number of indices this IndexArray can store. */
	public int getNumMaxIndices () {
		return buffer.capacity();
	}

	/** <p>
	 * Sets the indices of this IndexArray, discarding the old indices. The count must equal the number of indices to be copied to
	 * this IndexArray.
	 * </p>
	 * 
	 * <p>
	 * This can be called in between calls to {@link #bind()} and {@link #unbind()}. The index data will be updated instantly.
	 * </p>
	 * 
	 * @param indices the vertex data
	 * @param offset the offset to start copying the data from
	 * @param count the number of shorts to copy */
	public void setIndices (short[] indices, int offset, int count) {
		buffer.clear();
		buffer.put(indices, offset, count);
		buffer.flip();
		byteBuffer.position(0);
		byteBuffer.limit(count << 1);
	}

	/** <p>
	 * Returns the underlying ShortBuffer. If you modify the buffer contents they wil be uploaded on the call to {@link #bind()}.
	 * If you need immediate uploading use {@link #setIndices(short[], int, int)}.
	 * </p>
	 * 
	 * @return the underlying short buffer. */
	public ShortBuffer getBuffer () {
		return buffer;
	}

	/** Binds this IndexArray for rendering with glDrawElements. */
	public void bind () {
	}

	/** Unbinds this IndexArray. */
	public void unbind () {
	}

	/** Invalidates the IndexArray so a new OpenGL buffer handle is created. Use this in case of a context loss. */
	public void invalidate () {
	}

	/** Disposes this IndexArray and all its associated OpenGL resources. */
	public void dispose () {
//		BufferUtils.disposeUnsafeByteBuffer(byteBuffer);
	}
}
