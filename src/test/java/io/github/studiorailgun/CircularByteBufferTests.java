package io.github.studiorailgun;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * <p>
 * Tests for the CircularByteBuffer.
 * </p>
 */
public class CircularByteBufferTests {
    
    /**
     * Test that the structure can be created at all
     */
    @Test
    public void testInstantiationNoThrow(){
        assertDoesNotThrow(() -> {
            new CircularByteBuffer(50);
        });
    }

    /**
     * Test that data can be written to and read from the buffer without issue
     */
    @Test
    public void testRead(){
        CircularByteBuffer buffer = new CircularByteBuffer(5);

        //write to buffer
        byte[] input = new byte[]{
            1,2,3,4,5
        };
        buffer.add(input, 5);

        //read from buffer
        byte[] output = buffer.read(5);

        //assert data is correct
        assertEquals(output.length, 5);
        assertEquals(output[0], 1);
        assertEquals(output[1], 2);
        assertEquals(output[2], 3);
        assertEquals(output[3], 4);
        assertEquals(output[4], 5);
    }

    /**
     * Test that data can be written to and read from the buffer without issue when the buffer wraps around on itself
     */
    @Test
    public void testReadWithWrap(){
        CircularByteBuffer buffer = new CircularByteBuffer(5);

        //write to buffer
        byte[] input1 = new byte[]{
            1,2,3,4,5
        };
        buffer.add(input1, 5);

        //read from buffer
        byte[] output1 = buffer.read(3);

        //write again
        byte[] input2 = new byte[]{
            6,7,8
        };
        buffer.add(input2, 3);

        //read again
        byte[] output2 = buffer.read(5);

        //assert data is correct
        assertEquals(output1.length, 3);
        assertEquals(output1[0], 1);
        assertEquals(output1[1], 2);
        assertEquals(output1[2], 3);

        assertEquals(output2.length, 5);
        assertEquals(output2[0], 4);
        assertEquals(output2[1], 5);
        assertEquals(output2[2], 6);
        assertEquals(output2[3], 7);
        assertEquals(output2[4], 8);
    }

}
