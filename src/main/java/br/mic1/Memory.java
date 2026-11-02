package br.mic1;

import java.util.Arrays;

public class Memory {

    public static final int DEFAULT_SIZE = 65536;

    private final int[] words;
    private final int size;

    public Memory() {
        this(DEFAULT_SIZE);
    }

    public Memory(int sizeInWords) {
        if (sizeInWords <= 0) {
            throw new IllegalArgumentException("O tamanho da memória deve ser maior que zero: " + sizeInWords);
        }
        this.size = sizeInWords;
        this.words = new int[sizeInWords];
    }

    public int read(int wordAddress) {
        validateWordAddress(wordAddress);
        return words[wordAddress];
    }

    public void write(int wordAddress, int value) {
        validateWordAddress(wordAddress);
        words[wordAddress] = value;
    }

    public int readByte(int byteAddress) {
        validateByteAddress(byteAddress);
        int wordAddress = byteAddress >>> 2;
        int byteOffset = byteAddress & 0x03;
        int word = words[wordAddress];
        int shift = (3 - byteOffset) * 8;
        return (word >>> shift) & 0xFF;
    }

    public void writeByte(int byteAddress, int byteValue) {
        validateByteAddress(byteAddress);
        int wordAddress = byteAddress >>> 2;
        int byteOffset = byteAddress & 0x03;
        int shift = (3 - byteOffset) * 8;
        int mask = ~(0xFF << shift);
        int val = (byteValue & 0xFF) << shift;

        words[wordAddress] = (words[wordAddress] & mask) | val;
    }

    public void clear() {
        Arrays.fill(words, 0);
    }

    public int getSize() {
        return size;
    }

    public int getByteSize() {
        return size * 4;
    }

    public int[] getWordsCopy() {
        return Arrays.copyOf(words, words.length);
    }

    private void validateWordAddress(int wordAddress) {
        if (wordAddress < 0 || wordAddress >= size) {
            throw new IndexOutOfBoundsException(
                String.format("Endereço de palavra fora dos limites da memória: 0x%X (tamanho: %d palavras)",
                    wordAddress, size)
            );
        }
    }

    private void validateByteAddress(int byteAddress) {
        int maxBytes = size * 4;
        if (byteAddress < 0 || byteAddress >= maxBytes) {
            throw new IndexOutOfBoundsException(
                String.format("Endereço de byte fora dos limites da memória: 0x%X (tamanho: %d bytes)",
                    byteAddress, maxBytes)
            );
        }
    }
}
