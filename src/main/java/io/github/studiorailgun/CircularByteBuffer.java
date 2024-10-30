package io.github.studiorailgun;

import java.util.concurrent.Semaphore;

/**
 * <p>
 * A circular byte buffer optimized for high throughput (relative to a list) and peaking at early elements of the current position.
 * </p>
 * <p>
 * When the sizes of the reads are small enough, the buffer will not perform any reallocations.
 * </p>
 */
public class CircularByteBuffer {
    
    //The array backing this circular byte buffer
    byte[] backingArray;
    //the current read position of the buffer in the backing array
    int position;
    //the remaining bytes to read before the read position equals the write position
    int remaining;
    //the capacity of the backing array
    int capacity;
    //Lock to make the structure threadsafe
    Semaphore lock = new Semaphore(1);

    /**
     * Constructs a CircularByteBuffer
     * @param capacity The capacity of the backing array in bytes
     */
    public CircularByteBuffer(int capacity){
        backingArray = new byte[capacity];
        position = 0;
        remaining = 0;
        this.capacity = capacity;
    }

    /**
     * Adds an array of bytes to the circular buffer
     * @param bytes The bytes
     * @param len The number of bytes to pull from the array bytes
     */
    public void add(byte[] bytes, int len){
        lock.acquireUninterruptibly();
        // System.out.println("Add start");
        int writePosition = (position + remaining) % capacity;
        //amount possible to write before wrapping
        int writeBeforeWrap = capacity - writePosition;
        //only run wrapping logic if necessary
        if(len > writeBeforeWrap){
            System.arraycopy(bytes, 0, backingArray, writePosition, writeBeforeWrap);
            System.arraycopy(bytes, writeBeforeWrap, backingArray, 0, len - writeBeforeWrap);
        } else {
            System.arraycopy(bytes, 0, backingArray, writePosition, len);
        }
        remaining = remaining + len;
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
        byte rVal = backingArray[(position + offset) % capacity];
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
        lock.release();
        return rVal;
    }

    /**
     * Gets the capacity of the buffer
     * @return The capacity
     */
    public int getCapacity(){
        lock.acquireUninterruptibly();
        int rVal = capacity;
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
        //amount possible to read before loop
        int toReadBeforeLoop = capacity - position;
        if(len > capacity - position){
            System.arraycopy(backingArray, position, rVal, 0, toReadBeforeLoop);
            System.arraycopy(backingArray, 0, rVal, toReadBeforeLoop, len - toReadBeforeLoop);
        } else {
            System.arraycopy(backingArray, position, rVal, 0, len);
        }
        position = (position + len) % capacity;
        remaining = remaining - len;
        lock.release();
        return rVal;
    }

}
