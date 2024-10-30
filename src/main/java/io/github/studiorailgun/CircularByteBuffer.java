package io.github.studiorailgun;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;

/**
 * <p>
 * A circular byte buffer optimized for high throughput (relative to a list) and peaking at early elements of the current position.
 * </p>
 * <p>
 * When the sizes of the reads are small enough, the buffer will not perform any reallocations.
 * If the backing array overflows, it will fall back to using a queue to store blocks of bytes until it can recover.
 * The backing array will not be re-filled until the queue has fully cleared.
 * This means reads/writes will be slow until the queue has fully cleared.
 * </p>
 * <p>
 * The buffer is threadsafe.
 * </p>
 */
public class CircularByteBuffer {
    
    /**
     * The array backing this circular byte buffer
     */
    byte[] backingArray;

    /**
     * The current read position of the buffer in the backing array
     */
    int position;

    /**
     * The remaining bytes to read before the read position equals the write position
     */
    int remaining;

    /**
     * The capacity of the backing array
     */
    int primaryCapacity;
    
    /**
     * Lock to make the structure threadsafe
     */
    Semaphore lock = new Semaphore(1);

    /**
     * Stores bytes when the array is overloaded
     */
    Deque<byte[]> overflowQueue = new LinkedBlockingDeque<byte[]>();

    /**
     * Used for cleaning up the overflowQueue after a read
     */
    List<byte[]> clearQueue = new LinkedList<byte[]>();

    /**
     * Constructs a CircularByteBuffer
     * @param primaryCapacity The capacity of the backing array in bytes
     */
    public CircularByteBuffer(int primaryCapacity){
        backingArray = new byte[primaryCapacity];
        position = 0;
        remaining = 0;
        this.primaryCapacity = primaryCapacity;
    }

    /**
     * Adds an array of bytes to the circular buffer
     * @param bytes The bytes
     * @param len The number of bytes to pull from the array bytes
     */
    public void add(byte[] bytes, int len){
        lock.acquireUninterruptibly();

        //if adding directly to array would overload, put into overflow queue instead
        if(overflowQueue.size() > 0 || primaryCapacity - remaining < len){
            byte[] storage = new byte[len];
            System.arraycopy(bytes, 0, storage, 0, len);
            overflowQueue.add(storage);
        } else {
            int writePosition = (position + remaining) % primaryCapacity;
            //amount possible to write before wrapping
            int writeBeforeWrap = primaryCapacity - writePosition;
            //only run wrapping logic if necessary
            if(len > writeBeforeWrap){
                System.arraycopy(bytes, 0, backingArray, writePosition, writeBeforeWrap);
                System.arraycopy(bytes, writeBeforeWrap, backingArray, 0, len - writeBeforeWrap);
            } else {
                System.arraycopy(bytes, 0, backingArray, writePosition, len);
            }
            remaining = remaining + len;
        }
        lock.release();
    }

    /**
     * Peeks at the next element in the buffer
     * @return The value of the byte next in the buffer
     */
    public byte peek(){
        byte rVal = peek(0);
        return rVal;
    }

    /**
     * Peeks at an element @param offset elements further along the buffer from the current position
     * @param offset The offset, in bytes, to look forward in the buffer
     * @return The value of the byte at the current position + @param offset
     */
    public byte peek(int offset){
        lock.acquireUninterruptibly();
        byte rVal = -1;
        if(offset < remaining){
            rVal = backingArray[(position + offset) % primaryCapacity];
        } else if(overflowQueue.size() > 0){
            int accumulatedOffset = remaining;
            byte[] readTarget = null;
            //search through the overflow queue until we've found the array to offset into
            for(byte[] storage : overflowQueue){
                if(storage.length + accumulatedOffset > offset){
                    readTarget = storage;
                    break;
                } else {
                    accumulatedOffset = accumulatedOffset + storage.length;
                }
            }

            if(readTarget != null){
                int relativePosition = offset - accumulatedOffset;
                rVal = readTarget[relativePosition];
            }
        }
        lock.release();
        return rVal;
    }

    /**
     * Gets the remaining number of bytes in the buffer
     * @return The remaining number of bytes
     */
    public int getRemaining(){
        lock.acquireUninterruptibly();
        int rVal = remaining;
        if(overflowQueue.size() > 0){
            for(byte[] storage : overflowQueue){
                rVal = rVal + storage.length;
            }
        }
        lock.release();
        return rVal;
    }

    /**
     * Gets the capacity of the primary buffer
     * @return The capacity
     */
    public int getCapacity(){
        lock.acquireUninterruptibly();
        int rVal = primaryCapacity;
        lock.release();
        return rVal;
    }

    /**
     * Reads a given number of bytes from the buffer
     * @param len The number of bytes to read
     * @return The bytes in an array
     */
    public byte[] read(int len){
        lock.acquireUninterruptibly();
        byte[] rVal = new byte[len];
        if(len > remaining && overflowQueue.size() > 0){
            //read from primary array
            //amount possible to read before loop
            int toReadBeforeLoop = primaryCapacity - position;
            if(remaining > primaryCapacity - position){
                System.arraycopy(backingArray, position, rVal, 0, toReadBeforeLoop);
                System.arraycopy(backingArray, 0, rVal, toReadBeforeLoop, remaining - toReadBeforeLoop);
            } else {
                System.arraycopy(backingArray, position, rVal, 0, remaining);
            }
            
            //read from overflow queues
            int posToWriteTo = remaining;
            int remainingToRead = len - remaining;
            clearQueue.clear();
            for(byte[] storage : overflowQueue){
                if(remainingToRead == 0){
                    break;
                }
                if(storage.length > remainingToRead){
                    System.arraycopy(storage, 0, rVal, posToWriteTo, remainingToRead);
                    //reallocate remaining bytes
                    byte[] realloc = new byte[storage.length - remainingToRead];
                    System.arraycopy(storage, remainingToRead, realloc, 0, storage.length - remainingToRead);
                    overflowQueue.addFirst(realloc);

                    clearQueue.add(storage);
                    posToWriteTo = -1;
                    remainingToRead = 0;
                    break;

                } else {
                    System.arraycopy(storage, 0, rVal, posToWriteTo, storage.length);
                    posToWriteTo = posToWriteTo + storage.length;
                    remainingToRead = remainingToRead - storage.length;
                    clearQueue.add(storage);
                }
            }
            //remove from overflow queue
            overflowQueue.removeAll(clearQueue);

            //update tracked values
            position = (position + remaining) % primaryCapacity;
            remaining = 0;

        } else {
            //amount possible to read before loop
            int toReadBeforeLoop = primaryCapacity - position;
            if(len > primaryCapacity - position){
                System.arraycopy(backingArray, position, rVal, 0, toReadBeforeLoop);
                System.arraycopy(backingArray, 0, rVal, toReadBeforeLoop, len - toReadBeforeLoop);
            } else {
                System.arraycopy(backingArray, position, rVal, 0, len);
            }
            position = (position + len) % primaryCapacity;
            remaining = remaining - len;
        }
        lock.release();
        return rVal;
    }

}
