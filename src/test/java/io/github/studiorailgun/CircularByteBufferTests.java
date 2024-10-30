package io.github.studiorailgun;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        buffer.add(new byte[]{
            1,2,3,4,5
        }, 5);

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
        buffer.add(new byte[]{
            1,2,3,4,5
        }, 5);

        //read from buffer
        byte[] output1 = buffer.read(3);

        //write again
        buffer.add(new byte[]{
            6,7,8
        }, 3);

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

    /**
     * Test that data can be written to and read from the buffer without issue when the buffer wraps around on itself
     */
    @Test
    public void testReadWithOverflow(){
        CircularByteBuffer buffer = new CircularByteBuffer(5);

        //cause overflow and cleanup overflow
        buffer.add(new byte[]{
            1,2,3,4,5
        }, 5);
        byte[] output1 = buffer.read(3);
        buffer.add(new byte[]{
            6,7,8,9,10
        }, 5);
        assertEquals(7,buffer.getRemaining());
        byte[] output2 = buffer.read(7);

        //assert data is correct
        assertEquals(output1.length, 3);
        assertEquals(output1[0], 1);
        assertEquals(output1[1], 2);
        assertEquals(output1[2], 3);

        assertEquals(output2.length, 7);
        assertEquals(output2[0], 4);
        assertEquals(output2[1], 5);
        assertEquals(output2[2], 6);
        assertEquals(output2[3], 7);
        assertEquals(output2[4], 8);
        assertEquals(output2[5], 9);
        assertEquals(output2[6], 10);
    }

    /**
     * Test that remaining value is accurate once an overflow happens
     */
    @Test
    public void testRemainingDuringOverflow(){
        CircularByteBuffer buffer = new CircularByteBuffer(5);

        //cause overflow and cleanup overflow
        buffer.add(new byte[]{
            1,2,3,4,5
        }, 5);
        buffer.read(3);
        buffer.add(new byte[]{
            6,7,8,9,10
        }, 5);
        assertEquals(7,buffer.getRemaining());

        //assert data is correct
        assertEquals(buffer.getRemaining(), 7);
    }

    /**
     * Test that remaining value is accurate after an overflow
     */
    @Test
    public void testRemainingAfterOverflowClears(){
        CircularByteBuffer buffer = new CircularByteBuffer(5);

        //cause overflow and cleanup overflow
        buffer.add(new byte[]{
            1,2,3,4,5
        }, 5);
        buffer.read(3);
        buffer.add(new byte[]{
            6,7,8,9,10
        }, 5);
        buffer.read(7);

        //make sure remaining is accurate
        assertEquals(0,buffer.getRemaining());
    }

    /**
     * Test that read/write still works with small blocks after an overflow clears
     */
    @Test
    public void testReadAfterOverflowClears(){
        CircularByteBuffer buffer = new CircularByteBuffer(5);

        //cause overflow and cleanup overflow
        buffer.add(new byte[]{
            1,2,3,4,5
        }, 5);
        buffer.read(3);
        buffer.add(new byte[]{
            6,7,8,9,10
        }, 5);
        buffer.read(7);

        //read and write bytes
        buffer.add(new byte[]{
            47,17
        },2);
        byte[] output = buffer.read(2);

        //assert values are accurate
        assertEquals(47, output[0]);
        assertEquals(17, output[1]);
    }

    /**
     * Test that we can read both blocks if the buffer is written two after it has already overflowed
     */
    @Test
    public void testWritingTwoOverflowBlocks(){
        CircularByteBuffer buffer = new CircularByteBuffer(5);

        //cause overflow and cleanup overflow
        buffer.add(new byte[]{
            1,2,3,4,5
        }, 5);
        buffer.read(3);
        buffer.add(new byte[]{
            6,7,8,9,10
        }, 5);
        buffer.add(new byte[]{
            11,12,13,14,15,16
        }, 6);

        
        //read and write bytes
        byte[] output = buffer.read(14);

        //assert values are accurate
        assertEquals(4, output[0]);
        assertEquals(5, output[1]);
        assertEquals(6, output[2]);
        assertEquals(7, output[3]);
        assertEquals(8, output[4]);
        assertEquals(9, output[5]);
        assertEquals(10, output[6]);
        assertEquals(11, output[7]);
        assertEquals(12, output[8]);
        assertEquals(13, output[9]);
    }

    /**
     * Test that the data is correct if we read and it doesn't fully clear the overflow
     */
    @Test
    public void testNotFullyClearingOverflow(){
        CircularByteBuffer buffer = new CircularByteBuffer(5);

        //cause overflow and cleanup overflow
        buffer.add(new byte[]{
            1,2,3,4,5
        }, 5);
        buffer.read(3);
        buffer.add(new byte[]{
            6,7,8,9,10
        }, 5);
        buffer.add(new byte[]{
            11,12,13,14,15,16
        }, 6);

        
        //read and write bytes
        byte[] output = buffer.read(12);

        //assert values are accurate
        assertEquals(4, output[0]);
        assertEquals(5, output[1]);
        assertEquals(6, output[2]);
        assertEquals(7, output[3]);
        assertEquals(8, output[4]);
        assertEquals(9, output[5]);
        assertEquals(10, output[6]);
        assertEquals(11, output[7]);
    }

    /**
     * Test that the partial data is preserved in the queue after not fully clearing the overflow
     */
    @Test
    public void testNotFullyClearingOverflowDataSecurity(){
        CircularByteBuffer buffer = new CircularByteBuffer(5);

        //cause overflow and cleanup overflow
        buffer.add(new byte[]{
            1,2,3,4,5
        }, 5);
        buffer.read(3);
        buffer.add(new byte[]{
            6,7,8,9,10
        }, 5);
        buffer.add(new byte[]{
            11,12,13,14,15,16
        }, 6);

        
        //read and write bytes
        buffer.read(12);
        byte[] output = buffer.read(2);

        //assert values are accurate
        assertEquals(16, output[0]);
    }

    /**
     * Test that the queue can add new non-overflowing blocks after not fully clearing an overflow
     */
    @Test
    public void testAddNonOverflowBlockAfterNotFullyClearingOverflow(){
        CircularByteBuffer buffer = new CircularByteBuffer(5);

        //cause overflow and cleanup overflow
        buffer.add(new byte[]{
            1,2,3,4,5
        }, 5);
        buffer.read(3);
        buffer.add(new byte[]{
            6,7,8,9,10
        }, 5);
        buffer.add(new byte[]{
            11,12,13,14,15,16
        }, 6);

        
        //read and write bytes
        buffer.read(12);
        buffer.add(new byte[]{
            1,2
        },2);
        buffer.add(new byte[]{
            3,4
        },2);
        byte[] output = buffer.read(5);

        //assert values are accurate
        assertEquals(16, output[0]);
        assertEquals(1, output[1]);
        assertEquals(2, output[2]);
        assertEquals(3, output[3]);
        assertEquals(4, output[4]);
    }

    /**
     * Test that the queue can add new overflowing blocks after not fully clearing an overflow
     */
    @Test
    public void testAddOverflowBlockAfterNotFullyClearingOverflow(){
        CircularByteBuffer buffer = new CircularByteBuffer(5);

        //cause overflow and cleanup overflow
        buffer.add(new byte[]{
            1,2,3,4,5
        }, 5);
        buffer.read(3);
        buffer.add(new byte[]{
            6,7,8,9,10
        }, 5);
        buffer.add(new byte[]{
            11,12,13,14,15,16
        }, 6);

        
        //read and write bytes
        buffer.read(12);
        buffer.add(new byte[]{
            1,2,3,4,5
        },5);
        byte[] output = buffer.read(3);

        //assert values are accurate
        assertEquals(16, output[0]);
        assertEquals(1, output[1]);
        assertEquals(2, output[2]);
    }

    /**
     * Test peek() accuracy during overflow
     */
    @Test
    public void testPeekDuringOverflow(){
        CircularByteBuffer buffer = new CircularByteBuffer(5);

        //cause overflow and cleanup overflow
        buffer.add(new byte[]{
            1,2,3,4,5
        }, 5);
        buffer.read(3);
        buffer.add(new byte[]{
            6,7,8,9,10
        }, 5);
        buffer.add(new byte[]{
            11,12,13,14,15,16
        }, 6);

        
        assertEquals(4,buffer.peek());
        assertEquals(6,buffer.peek(2));
        assertEquals(10,buffer.peek(6));
        assertEquals(11,buffer.peek(7));
        assertEquals(15,buffer.peek(11));
    }

}
