package workflowsuite.kpi.client.rabbitmq;

abstract class MessageSerializerBase<T> {
    static final int HEADER_SIZE = Integer.BYTES;
    static final int PACKAGE_VERSION_SIZE = Short.BYTES;

    static final int PAYLOAD_LENGTH_SIZE = Short.BYTES;

    static final int TIMESTAMP_SIZE = Long.BYTES + Integer.BYTES;
    static final int UTF8_LENGTH_SIZE = Short.BYTES;

    static final short PACKAGE_VERSION = 1;

    MessageSerializerBase() { }

    protected abstract byte[] serialize(T message);

    static int putByte(byte[] buffer, int startIndex, byte value) {
        buffer[startIndex] = value;
        return startIndex + 1;
    }

    static int putBytes(byte[] buffer, int startIndex, byte[] src) {
        int endIndex = startIndex + src.length;
        int srcIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            buffer[i] = src[srcIndex];
            srcIndex++;
        }
        return endIndex;
    }

    static int putUtf8String(byte[] buffer, int startIndex, String s) {
        int pos = startIndex + Short.BYTES;
        int endIndex = utf8Encoded(s, buffer, pos, buffer.length - pos);
        putShortLE(buffer, startIndex, (short) (endIndex - pos));

        return endIndex;
    }

    static int putBoolean(byte[] buffer, int startIndex, boolean value) {
        return putByte(buffer, startIndex, value ? (byte) 1 : 0);
    }

    static int putZeroLong(byte[] buffer, int startIndex) {
        buffer[startIndex] = 0;
        buffer[startIndex + 1] = 0;
        buffer[startIndex + 2] = 0;
        buffer[startIndex + 3] = 0;
        buffer[startIndex + 4] = 0;
        buffer[startIndex + 5] = 0;
        buffer[startIndex + 6] = 0;
        buffer[startIndex + 7] = 0;
        return startIndex + Long.BYTES;
    }

    static int putZeroShort(byte[] buffer, int startIndex) {
        buffer[startIndex] = 0;
        buffer[startIndex + 1] = 0;
        return startIndex + Short.BYTES;
    }

    static int putLongLE(byte[] buffer, int startIndex, long x) {
        buffer[startIndex] = (byte) x;
        buffer[startIndex + 1] = (byte) (x >> 8);
        buffer[startIndex + 2] = (byte) (x >> 16);
        buffer[startIndex + 3] = (byte) (x >> 24);
        buffer[startIndex + 4] = (byte) (x >> 32);
        buffer[startIndex + 5] = (byte) (x >> 40);
        buffer[startIndex + 6] = (byte) (x >> 48);
        buffer[startIndex + 7] = (byte) (x >> 56);
        return startIndex + Long.BYTES;
    }

    static int putIntLE(byte[] buffer, int startIndex, int x) {
        buffer[startIndex] = (byte) x;
        buffer[startIndex + 1] = (byte) (x >> 8);
        buffer[startIndex + 2] = (byte) (x >> 16);
        buffer[startIndex + 3] = (byte) (x >> 24);
        return startIndex + Integer.BYTES;
    }

    static int putShortLE(byte[] buffer, int startIndex, short x) {
        buffer[startIndex] = (byte) x;
        buffer[startIndex + 1] = (byte) (x >> 8);
        return startIndex + Short.BYTES;
    }

    static int utf8EncodedLength(String sequence) {
        // Warning to maintainers: this implementation is highly optimized.
        int utf16Length = sequence.length();
        int utf8Length = utf16Length;
        int i = 0;

        // This loop optimizes for pure ASCII.
        while (i < utf16Length && sequence.charAt(i) < 0x80) {
            i++;
        }

        // This loop optimizes for chars less than 0x800.
        for (; i < utf16Length; i++) {
            char c = sequence.charAt(i);
            if (c < 0x800) {
                utf8Length += (0x7f - c) >>> 31;  // branch free!
            } else {
                utf8Length += encodedLengthGeneral(sequence, i);
                break;
            }
        }

        if (utf8Length < utf16Length) {
            // Necessary and sufficient condition for overflow because of maximum 3x expansion
            throw new IllegalArgumentException(
                    "UTF-8 length does not fit in int: " + (utf8Length + (1L << 32)));
        }
        return utf8Length;
    }

    private static int encodedLengthGeneral(String sequence, int start) {
        int utf16Length = sequence.length();
        int utf8Length = 0;
        for (int i = start; i < utf16Length; i++) {
            char c = sequence.charAt(i);
            if (c < 0x800) {
                utf8Length += (0x7f - c) >>> 31; // branch free!
            } else {
                utf8Length += 2;
                if (Character.isSurrogate(c)) {
                    // Check that we have a well-formed surrogate pair.
                    int cp = Character.codePointAt(sequence, i);
                    if (cp < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                        break;
                    }
                    // CHECKSTYLE:OFF ModifiedControlVariableCheck
                    i++;
                    // CHECKSTYLE:ON ModifiedControlVariableCheck
                }
            }
        }
        return utf8Length;
    }

    private static int utf8Encoded(String in, byte[] out, int offset, int length) {
        int utf16Length = in.length();
        int j = offset;
        int i = 0;
        int limit = offset + length;
        // Designed to take advantage of
        // https://wikis.oracle.com/display/HotSpotInternals/RangeCheckElimination
        // ASCII
        for (char c; i < utf16Length && i + j < limit && (c = in.charAt(i)) < 0x80; i++) {
            out[j + i] = (byte) c;
        }
        if (i == utf16Length) {
            return j + utf16Length;
        }
        j += i;
        for (char c; i < utf16Length; i++) {
            c = in.charAt(i);
            if (c < 0x80 && j < limit) {
                out[j++] = (byte) c;
            } else if (c < 0x800 && j <= limit - 2) { // 11 bits, two UTF-8 bytes
                out[j++] = (byte) ((0xF << 6) | (c >>> 6));
                out[j++] = (byte) (0x80 | (0x3F & c));
            } else if ((c < Character.MIN_SURROGATE || Character.MAX_SURROGATE < c) && j <= limit - 3) {
                // Maximum single-char code point is 0xFFFF, 16 bits, three UTF-8 bytes
                out[j++] = (byte) ((0xF << 5) | (c >>> 12));
                out[j++] = (byte) (0x80 | (0x3F & (c >>> 6)));
                out[j++] = (byte) (0x80 | (0x3F & c));
            } else if (j <= limit - 4) {
                // Minimum code point represented by a surrogate pair is 0x10000, 17 bits,
                // four UTF-8 bytes
                final char low;
                if (i + 1 == in.length() || !Character.isSurrogatePair(c, low = in.charAt(++i))) {
                    // stop encoding
                    return j;
                    //throw new UnpairedSurrogateException((i - 1), utf16Length);
                }
                int codePoint = Character.toCodePoint(c, low);
                out[j++] = (byte) ((0xF << 4) | (codePoint >>> 18));
                out[j++] = (byte) (0x80 | (0x3F & (codePoint >>> 12)));
                out[j++] = (byte) (0x80 | (0x3F & (codePoint >>> 6)));
                out[j++] = (byte) (0x80 | (0x3F & codePoint));

            } else {
                // If we are surrogates and we're not a surrogate pair, always throw an
                // UnpairedSurrogateException instead of an ArrayOutOfBoundsException.
                if ((Character.MIN_SURROGATE <= c && c <= Character.MAX_SURROGATE)
                        && (i + 1 == in.length()
                        || !Character.isSurrogatePair(c, in.charAt(i + 1)))) {
                    // stop encoding
                    return j;
                    //throw new UnpairedSurrogateException(i, utf16Length);
                }
                throw new ArrayIndexOutOfBoundsException("Failed writing " + c + " at index " + j);
            }
        }
        return j;
    }
}
