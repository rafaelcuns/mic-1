package br.mic1;

public class Cache {

    public static final int DEFAULT_LINES = 16;

    public static class CacheLine {
        private final int index;
        private boolean valid;
        private int tag;
        private int data;
        private int address;

        public CacheLine(int index) {
            this.index = index;
            this.valid = false;
            this.tag = 0;
            this.data = 0;
            this.address = 0;
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

        public int getData() {
            return data;
        }

        public int getAddress() {
            return address;
        }

        public void invalidate() {
            this.valid = false;
            this.tag = 0;
            this.data = 0;
            this.address = 0;
        }

        @Override
        public String toString() {
            if (!valid) {
                return String.format("[Linha %02d | Inválida]", index);
            }
            return String.format("[Linha %02d | Tag: 0x%04X | Endereço: 0x%04X | Dado: 0x%08X (%d)]",
                    index, tag, address, data, data);
        }
    }

    private final String name;
    private final int numLines;
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
    private boolean lastAccessHit;

    public Cache(String name, Memory memory) {
        this(name, DEFAULT_LINES, memory);
    }

    public Cache(String name, int numLines, Memory memory) {
        if (memory == null) {
            throw new IllegalArgumentException("A memória principal não pode ser nula.");
        }
        if (numLines <= 0 || (numLines & (numLines - 1)) != 0) {
            throw new IllegalArgumentException("O número de linhas da cache deve ser potência de 2: " + numLines);
        }

        this.name = name;
        this.numLines = numLines;
        this.indexBits = Integer.numberOfTrailingZeros(numLines);
        this.indexMask = numLines - 1;
        this.memory = memory;

        this.lines = new CacheLine[numLines];
        for (int i = 0; i < numLines; i++) {
            this.lines[i] = new CacheLine(i);
        }

        resetStats();
    }

    public int read(int wordAddress) {
        accessCount++;
        readCount++;
        lastAccessAddress = wordAddress;

        int index = calculateIndex(wordAddress);
        int tag = calculateTag(wordAddress);
        lastAccessIndex = index;
        lastAccessTag = tag;

        CacheLine line = lines[index];

        if (line.valid && line.tag == tag) {
            hitCount++;
            lastAccessHit = true;
            return line.data;
        } else {
            missCount++;
            lastAccessHit = false;

            int fetchedData = memory.read(wordAddress);
            line.valid = true;
            line.tag = tag;
            line.data = fetchedData;
            line.address = wordAddress;

            return fetchedData;
        }
    }

    public void write(int wordAddress, int value) {
        accessCount++;
        writeCount++;
        lastAccessAddress = wordAddress;

        int index = calculateIndex(wordAddress);
        int tag = calculateTag(wordAddress);
        lastAccessIndex = index;
        lastAccessTag = tag;

        memory.write(wordAddress, value);

        CacheLine line = lines[index];
        if (line.valid && line.tag == tag) {
            hitCount++;
            lastAccessHit = true;
            line.data = value;
        } else {
            missCount++;
            lastAccessHit = false;
            line.valid = true;
            line.tag = tag;
            line.data = value;
            line.address = wordAddress;
        }
    }

    public int readByte(int byteAddress) {
        int wordAddress = byteAddress >>> 2;
        int byteOffset = byteAddress & 0x03;
        int word = read(wordAddress);
        int shift = (3 - byteOffset) * 8;
        return (word >>> shift) & 0xFF;
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
        lastAccessHit = false;
    }

    public void invalidateLine(int index) {
        if (index >= 0 && index < numLines) {
            lines[index].invalidate();
        }
    }

    public int calculateIndex(int wordAddress) {
        return wordAddress & indexMask;
    }

    public int calculateTag(int wordAddress) {
        return wordAddress >>> indexBits;
    }

    public String getName() {
        return name;
    }

    public int getNumLines() {
        return numLines;
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

    public boolean isLastAccessHit() {
        return lastAccessHit;
    }
}
