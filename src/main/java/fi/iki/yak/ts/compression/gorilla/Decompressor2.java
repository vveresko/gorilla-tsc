package fi.iki.yak.ts.compression.gorilla;

/**
 * Decompresses a compressed stream done created by the Compressor. Returns pairs of timestamp and flaoting point value.
 *
 * @author Michael Burman
 */
public class Decompressor2 {

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private long storedVal = 0;
    private long storedTimestamp = 0;
    private long storedDelta = 0;

    private long blockTimestamp = 0;

    private boolean endOfStream = false;

    private BitInput in;

    public Decompressor2(BitInput input) {
        in = input;
        readHeader();
    }

    private void readHeader() {
        blockTimestamp = in.getLong(64);
//        first();
    }

    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    public Pair readPair() {
        next();
        if(endOfStream) {
            return null;
        }
        Pair pair = new Pair(storedTimestamp, storedVal);
        return pair;
    }

    private void next() {
        // TODO I could implement a non-streaming solution also.. is there ever a need for streaming solution?
        // I wouldn't have to worry about the first case

        // Of course, if I assume there's more than 0 .. I can always do this in the begin and in the
        // readPair I would return the already compressed value and then read the next one. There's no point
        // in creating new series without the first timestamp (for compression that is)

        if(storedTimestamp == 0) {
            first();
            return;
        }

        nextTimestamp();
        nextValue();
    }

    private void first() {
        // First item to read
        storedDelta = in.getLong(Compressor.FIRST_DELTA_BITS);
        if(storedDelta == (1<<27) - 1) {
            endOfStream = true;
            return;
        }
        storedVal = in.getLong(64);
        storedTimestamp = blockTimestamp + storedDelta;
    }

    private void nextTimestamp() {
        // Next, read timestamp
        int readInstruction = in.nextClearBit(4);
        long deltaDelta;

        switch(readInstruction) {
            case 0x00:
                storedTimestamp = storedDelta + storedTimestamp;
                return;
            case 0x02:
                deltaDelta = in.getLong(7);
                break;
            case 0x06:
                deltaDelta = in.getLong(9);
                break;
            case 0x0e:
                deltaDelta = in.getLong(12);
                break;
            case 0x0F:
                deltaDelta = in.getLong(32);
                // For storage save.. if this is the last available word, check if remaining bits are all 1
                if ((int) deltaDelta == 0xFFFFFFFF) {
                    // End of stream
                    endOfStream = true;
                    return;
                }
                break;
            default:
                return;
        }

        deltaDelta++;
        deltaDelta = decodeZigZag32((int) deltaDelta);
        storedDelta = storedDelta + deltaDelta;

        storedTimestamp = storedDelta + storedTimestamp;
    }

    private void nextValue() {
        int val = in.nextClearBit(2);

        switch(val) {
            case 3:
                // New leading and trailing zeros
//                storedLeadingZeros = (int) in.getLong(6);
                storedLeadingZeros = (int) in.getLong(6);

                // To thesis - use (significantBits - 1) in storage - avoids a branch
                byte significantBits = (byte) in.getLong(6);
                significantBits++;

                storedTrailingZeros = 64 - significantBits - storedLeadingZeros;
                // missing break is intentional, we want to overflow to next one
            case 2:
                long value = in.getLong(64 - storedLeadingZeros - storedTrailingZeros);
                value <<= storedTrailingZeros;
                value = storedVal ^ value;
                storedVal = value;
                break;
        }
    }


    // START: From protobuf

    /**
     * Decode a ZigZag-encoded 32-bit value. ZigZag encodes signed integers into values that can be
     * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
     * to be varint encoded, thus always taking 10 bytes on the wire.)
     *
     * @param n An unsigned 32-bit integer, stored in a signed int because Java has no explicit
     *     unsigned support.
     * @return A signed 32-bit integer.
     */
    public static int decodeZigZag32(final int n) {
        return (n >>> 1) ^ -(n & 1);
    }

    // END: From protobuf

}