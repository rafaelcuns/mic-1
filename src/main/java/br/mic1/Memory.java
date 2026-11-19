package br.mic1;

import java.util.Arrays;

public class Memory {

    public static final int DEFAULT_SIZE = 4096;
    public static final int WORD_MASK = 0xFFFF;

    private final int[] words;
    private final int size;

    public Memory() {
        this(DEFAULT_SIZE);
    }

    public Memory(int sizeInWords) {
        if (sizeInWords <= 0 || sizeInWords > 4096) {
            throw new IllegalArgumentException("O tamanho da memória deve estar entre 1 e 4096 palavras: " + sizeInWords);
        }
        this.size = sizeInWords;
        this.words = new int[sizeInWords];
    }

    public int read(int address) {
        validateAddress(address);
        return words[address] & WORD_MASK;
    }

    public int readSigned(int address) {
        return (short) read(address);
    }

    public String readHex(int address) {
        return String.format("%04X", read(address));
    }

    public void write(int address, int value) {
        validateAddress(address);
        words[address] = value & WORD_MASK;
    }

    public void loadWords(int startAddress, int[] data) {
        if (data == null) {
            throw new IllegalArgumentException("O vetor de palavras não pode ser nulo.");
        }
        for (int i = 0; i < data.length; i++) {
            write(startAddress + i, data[i]);
        }
    }

    public void clear() {
        Arrays.fill(words, 0);
    }

    public int getSize() {
        return size;
    }

    public int[] getWordsCopy() {
        return Arrays.copyOf(words, words.length);
    }

    private void validateAddress(int address) {
        if (address < 0 || address >= size) {
            throw new IndexOutOfBoundsException(
                String.format("Endereço fora dos limites da memória: 0x%03X (tamanho: %d palavras)",
                    address, size)
            );
        }
    }
}
