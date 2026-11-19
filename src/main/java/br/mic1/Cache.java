package br.mic1;

import java.util.Arrays;

public class Cache {

    public static final int DEFAULT_LINES = 16;
    public static final int DEFAULT_BLOCK_SIZE = 4;

    public static class CacheLine {
        private final int index;
        private boolean valid;
        private int tag;
        private int baseAddress;
        private final int[] words;

        public CacheLine(int index, int blockSize) {
            this.index = index;
            this.valid = false;
            this.tag = 0;
            this.baseAddress = 0;
            this.words = new int[blockSize];
        }

        public int getIndex() {
            return index;
        }

        public boolean isValid() {
            return valid;
        }

        public int getTag() {
            return tag;
        }

        public int getBaseAddress() {
            return baseAddress;
        }

        public int getAddress() {
            return baseAddress;
        }

        public int getData() {
            return words[0];
        }

        public int getWord(int offset) {
            return words[offset];
        }

        public int[] getWords() {
            return Arrays.copyOf(words, words.length);
        }

        public String getWordsHex() {
            if (!valid) {
                return "-";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < words.length; i++) {
                if (i > 0) sb.append(" ");
                sb.append(String.format("%04X", words[i] & 0xFFFF));
            }
            return sb.toString();
        }

        public void invalidate() {
            this.valid = false;
            this.tag = 0;
            this.baseAddress = 0;
            Arrays.fill(this.words, 0);
        }

        @Override
        public String toString() {
            if (!valid) {
                return String.format("[Linha %02d | Inválida]", index);
            }
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("[Linha %02d | Tag: 0x%03X | Base: 0x%03X | Palavras:", index, tag, baseAddress));
            for (int w : words) {
                sb.append(String.format(" 0x%04X", w & 0xFFFF));
            }
            sb.append("]");
            return sb.toString();
        }
    }

    private final String name;
    private final int numLines;
    private final int blockSize;
    private final int offsetBits;
    private final int offsetMask;
    private final int indexBits;
    private final int indexMask;
    private final Memory memory;
    private final CacheLine[] lines;

    private int accessCount;
    private int hitCount;
    private int missCount;
    private int readCount;
    private int writeCount;

    private int lastAccessAddress;
    private int lastAccessIndex;
    private int lastAccessTag;
    private int lastAccessOffset;
    private boolean lastAccessHit;

    public Cache(String name, Memory memory) {
        this(name, DEFAULT_LINES, DEFAULT_BLOCK_SIZE, memory);
    }

    public Cache(String name, int numLines, Memory memory) {
        this(name, numLines, 1, memory);
    }

    public Cache(String name, int numLines, int blockSize, Memory memory) {
        if (memory == null) {
            throw new IllegalArgumentException("A memória principal não pode ser nula.");
        }
        if (numLines <= 0 || (numLines & (numLines - 1)) != 0) {
            throw new IllegalArgumentException("O número de linhas da cache deve ser potência de 2: " + numLines);
        }
        if (blockSize <= 0 || (blockSize & (blockSize - 1)) != 0) {
            throw new IllegalArgumentException("O tamanho do bloco deve ser potência de 2: " + blockSize);
        }

        this.name = name;
        this.numLines = numLines;
        this.blockSize = blockSize;
        this.offsetBits = Integer.numberOfTrailingZeros(blockSize);
        this.offsetMask = blockSize - 1;
        this.indexBits = Integer.numberOfTrailingZeros(numLines);
        this.indexMask = numLines - 1;
        this.memory = memory;

        this.lines = new CacheLine[numLines];
        for (int i = 0; i < numLines; i++) {
            this.lines[i] = new CacheLine(i, blockSize);
        }

        resetStats();
    }

    public int read(int address) {
        accessCount++;
        readCount++;
        lastAccessAddress = address;

        int offset = calculateOffset(address);
        int index = calculateIndex(address);
        int tag = calculateTag(address);
        lastAccessOffset = offset;
        lastAccessIndex = index;
        lastAccessTag = tag;

        CacheLine line = lines[index];

        if (line.valid && line.tag == tag) {
            hitCount++;
            lastAccessHit = true;
            return line.words[offset] & Memory.WORD_MASK;
        } else {
            missCount++;
            lastAccessHit = false;
            loadBlock(address, line, tag);
            return line.words[offset] & Memory.WORD_MASK;
        }
    }

    public void write(int address, int value) {
        int maskedValue = value & Memory.WORD_MASK;
        accessCount++;
        writeCount++;
        lastAccessAddress = address;

        int offset = calculateOffset(address);
        int index = calculateIndex(address);
        int tag = calculateTag(address);
        lastAccessOffset = offset;
        lastAccessIndex = index;
        lastAccessTag = tag;

        memory.write(address, maskedValue);

        CacheLine line = lines[index];
        if (line.valid && line.tag == tag) {
            hitCount++;
            lastAccessHit = true;
            line.words[offset] = maskedValue;
        } else {
            missCount++;
            lastAccessHit = false;
            loadBlock(address, line, tag);
        }
    }

    private void loadBlock(int address, CacheLine line, int tag) {
        int baseAddress = (address >>> offsetBits) << offsetBits;
        line.valid = true;
        line.tag = tag;
        line.baseAddress = baseAddress;
        for (int i = 0; i < blockSize; i++) {
            int wordAddr = baseAddress + i;
            line.words[i] = (wordAddr < memory.getSize()) ? memory.read(wordAddr) : 0;
        }
    }

    public void reset() {
        for (CacheLine line : lines) {
            line.invalidate();
        }
        resetStats();
    }

    public void resetStats() {
        accessCount = 0;
        hitCount = 0;
        missCount = 0;
        readCount = 0;
        writeCount = 0;
        lastAccessAddress = 0;
        lastAccessIndex = 0;
        lastAccessTag = 0;
        lastAccessOffset = 0;
        lastAccessHit = false;
    }

    public void invalidateLine(int index) {
        if (index >= 0 && index < numLines) {
            lines[index].invalidate();
        }
    }

    public int calculateOffset(int address) {
        return address & offsetMask;
    }

    public int calculateIndex(int address) {
        return (address >>> offsetBits) & indexMask;
    }

    public int calculateTag(int address) {
        return address >>> (offsetBits + indexBits);
    }

    public String getName() {
        return name;
    }

    public int getNumLines() {
        return numLines;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public Memory getMemory() {
        return memory;
    }

    public CacheLine[] getLines() {
        return lines;
    }

    public CacheLine getLine(int index) {
        return lines[index];
    }

    public int getAccessCount() {
        return accessCount;
    }

    public int getHitCount() {
        return hitCount;
    }

    public int getMissCount() {
        return missCount;
    }

    public int getReadCount() {
        return readCount;
    }

    public int getWriteCount() {
        return writeCount;
    }

    public double getHitRate() {
        return accessCount == 0 ? 0.0 : ((double) hitCount / accessCount) * 100.0;
    }

    public double getMissRate() {
        return accessCount == 0 ? 0.0 : ((double) missCount / accessCount) * 100.0;
    }

    public int getLastAccessAddress() {
        return lastAccessAddress;
    }

    public int getLastAccessIndex() {
        return lastAccessIndex;
    }

    public int getLastAccessTag() {
        return lastAccessTag;
    }

    public int getLastAccessOffset() {
        return lastAccessOffset;
    }

    public boolean isLastAccessHit() {
        return lastAccessHit;
    }

    public String getLastAccessStatus() {
        if (accessCount == 0) {
            return "IDLE";
        }
        return lastAccessHit ? "HIT" : "MISS";
    }

    public String getSummaryText() {
        return String.format("Acessos: %d | Hits: %d (%.1f%%) | Misses: %d (%.1f%%)",
                accessCount, hitCount, getHitRate(), missCount, getMissRate());
    }
}
