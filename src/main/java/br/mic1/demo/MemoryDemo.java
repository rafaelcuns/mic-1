package br.mic1.demo;

import br.mic1.Cache;
import br.mic1.Memory;

public class MemoryDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("        SIMULADOR MIC-1 (3ª EDIÇÃO) - ETAPA A: MEMÓRIA E CACHE L1 (16 BITS)     ");
        System.out.println("================================================================================");
        System.out.println();

        Memory memory = new Memory();
        Cache l1Cache = new Cache("Cache L1 (16 bits)", 8, 4, memory);

        System.out.printf("[INFO] Memória Principal inicializada com %d palavras de 16 bits (endereços 0x000 a 0x%03X).%n",
                memory.getSize(), memory.getSize() - 1);
        System.out.printf("[INFO] %s criada com %d linhas e blocos de %d palavras (Mapeamento Direto).%n%n",
                l1Cache.getName(), l1Cache.getNumLines(), l1Cache.getBlockSize());

        System.out.println("--- TESTE 1: Escrita Write-Through na Cache L1 ---");
        int addr1 = 0x0010;
        int val1 = 0x700A;
        System.out.printf("Gravando 0x%04X no endereço 0x%03X via Cache L1...%n", val1, addr1);
        l1Cache.write(addr1, val1);

        int memVal1 = memory.read(addr1);
        System.out.printf("Verificação direta na RAM [0x%03X]: 0x%04X -> %s%n%n",
                addr1, memVal1, (memVal1 == val1 ? "SUCESSO (Write-Through ativo)" : "FALHA"));

        System.out.println("--- TESTE 2: Localidade Espacial (Bloco de 4 Palavras) ---");
        memory.write(0x0000, 0x7010);
        memory.write(0x0001, 0x1020);
        memory.write(0x0002, 0x7001);
        memory.write(0x0003, 0x1021);

        for (int pc = 0; pc < 4; pc++) {
            int word = l1Cache.read(pc);
            System.out.printf("  PC = 0x%03X -> Palavra: 0x%04X | Hit? %s%n",
                    pc, word, (l1Cache.isLastAccessHit() ? "SIM (HIT)" : "NÃO (MISS - carregou bloco de 4 palavras)"));
        }
        System.out.println();

        System.out.println("--- TESTE 3: Conflito de Linha (Mapeamento Direto) ---");
        int addrA = 0x0000;
        int addrB = 0x0020;
        memory.write(addrB, 0x55AA);
        System.out.printf("Endereço 0x%03X -> Linha %d | Tag 0x%X%n", addrA, l1Cache.calculateIndex(addrA), l1Cache.calculateTag(addrA));
        System.out.printf("Endereço 0x%03X -> Linha %d | Tag 0x%X%n", addrB, l1Cache.calculateIndex(addrB), l1Cache.calculateTag(addrB));

        l1Cache.read(addrB);
        System.out.printf("Após leitura de 0x%03X: %s%n", addrB, l1Cache.getLine(0));
        l1Cache.read(addrA);
        System.out.printf("Releitura de 0x%03X (Hit? %b - Conflict Miss esperado): %s%n%n",
                addrA, l1Cache.isLastAccessHit(), l1Cache.getLine(0));

        printCacheReport(l1Cache);

        System.out.println("================================================================================");
        System.out.println("               ETAPA A (MEMÓRIA E CACHE 16 BITS) CONCLUÍDA!                     ");
        System.out.println("================================================================================");
    }

    private static void printCacheReport(Cache cache) {
        System.out.println("=================================================================");
        System.out.printf(" RELATÓRIO DE STATUS: %s%n", cache.getName());
        System.out.println("=================================================================");
        for (Cache.CacheLine line : cache.getLines()) {
            System.out.println("  " + line);
        }
        System.out.println("-----------------------------------------------------------------");
        System.out.printf("Total de Acessos : %d (Leituras: %d | Escritas: %d)%n",
                cache.getAccessCount(), cache.getReadCount(), cache.getWriteCount());
        System.out.printf("Hits             : %d (%.2f%%)%n", cache.getHitCount(), cache.getHitRate());
        System.out.printf("Misses           : %d (%.2f%%)%n%n", cache.getMissCount(), cache.getMissRate());
    }
}
