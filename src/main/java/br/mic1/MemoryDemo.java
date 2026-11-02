package br.mic1;

public class MemoryDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("          SIMULADOR MIC-1 - ETAPA 1: SISTEMA DE MEMÓRIA E CACHES L1            ");
        System.out.println("================================================================================");
        System.out.println();

        Memory memory = new Memory(4096);
        Cache dCache = new Cache("D-Cache (Dados)", 8, memory);
        Cache iCache = new Cache("I-Cache (Instruções)", 8, memory);

        System.out.printf("[INFO] Memória Principal inicializada com %d palavras (%d bytes).%n",
                memory.getSize(), memory.getByteSize());
        System.out.printf("[INFO] D-Cache criada com %d linhas (Mapeamento Direto).%n", dCache.getNumLines());
        System.out.printf("[INFO] I-Cache criada com %d linhas (Mapeamento Direto).%n%n", iCache.getNumLines());

        System.out.println("--- TESTE 1: Escrita Write-Through na D-Cache ---");
        int testAddr1 = 0x000A;
        int testVal1  = 0xCAFEBABE;

        System.out.printf("Gravando 0x%08X no endereço de palavra 0x%04X via D-Cache...%n", testVal1, testAddr1);
        dCache.write(testAddr1, testVal1);

        int memVal1 = memory.read(testAddr1);
        System.out.printf("Verificação direta na Memória Principal [0x%04X]: 0x%08X -> %s%n%n",
                testAddr1, memVal1, (memVal1 == testVal1 ? "SUCESSO (Write-Through ativo)" : "FALHA"));

        System.out.println("--- TESTE 2: Cold Miss e Cache Hit na D-Cache ---");
        System.out.printf("1. Leitura 1 de [0x%04X]: 0x%08X (Hit? %b)%n",
                testAddr1, dCache.read(testAddr1), dCache.isLastAccessHit());

        int testAddr2 = 0x0002;
        memory.write(testAddr2, 0x12345678);
        System.out.printf("2. Leitura 1 de [0x%04X] (nunca acessado na cache): 0x%08X (Hit? %b)%n",
                testAddr2, dCache.read(testAddr2), dCache.isLastAccessHit());

        System.out.printf("3. Leitura 2 de [0x%04X] (agora residente na cache): 0x%08X (Hit? %b)%n%n",
                testAddr2, dCache.read(testAddr2), dCache.isLastAccessHit());

        System.out.println("--- TESTE 3: Conflito de Linha (Mapeamento Direto) ---");
        int indexAddr2  = dCache.calculateIndex(testAddr2);
        int indexAddr10 = dCache.calculateIndex(testAddr1);
        System.out.printf("Endereço 0x%04X -> Linha %d | Tag 0x%X%n", testAddr2, indexAddr2, dCache.calculateTag(testAddr2));
        System.out.printf("Endereço 0x%04X -> Linha %d | Tag 0x%X%n", testAddr1, indexAddr10, dCache.calculateTag(testAddr1));

        System.out.println("Acessando 0x000A (substitui a tag do 0x0002 na Linha 2)...");
        dCache.read(testAddr1);
        System.out.printf("Linha 2 após acesso ao 0x000A: %s%n", dCache.getLine(2));

        System.out.println("Acessando novamente 0x0002 (deve sofrer Conflict Miss por ter sido substituído)...");
        dCache.read(testAddr2);
        System.out.printf("Leitura de [0x%04X]: Hit? %b (Miss esperado devido ao conflito)%n",
                testAddr2, dCache.isLastAccessHit());
        System.out.printf("Linha 2 após reassumir 0x0002: %s%n%n", dCache.getLine(2));

        System.out.println("--- TESTE 4: I-Cache - Leitura de Instruções IJVM (Localidade Espacial) ---");
        memory.writeByte(0, 0x10);
        memory.writeByte(1, 0x05);
        memory.writeByte(2, 0x60);
        memory.writeByte(3, 0x00);

        System.out.println("Buscando 4 bytes sequenciais do fluxo de instruções via I-Cache (PC = 0, 1, 2, 3):");
        for (int pc = 0; pc < 4; pc++) {
            int b = iCache.readByte(pc);
            System.out.printf("  PC = %d -> Opcode/Byte: 0x%02X | Hit na I-Cache? %s%n",
                    pc, b, (iCache.isLastAccessHit() ? "SIM (HIT)" : "NÃO (MISS - carregou a palavra inteira)"));
        }
        System.out.println("-> Note que o byte 0 gerou 1 MISS (trouxe os 4 bytes da memória),");
        System.out.println("   e os bytes 1, 2 e 3 geraram 3 HITS consecutivos (localidade espacial)!\n");

        printCacheReport(dCache);
        printCacheReport(iCache);

        System.out.println("================================================================================");
        System.out.println("                   ETAPA 1 EXECUTADA COM SUCESSO!                              ");
        System.out.println("   Classes Memory.java e Cache.java prontas para integração por Francisco!      ");
        System.out.println("================================================================================");
    }

    private static void printCacheReport(Cache cache) {
        System.out.println("=======================================================");
        System.out.printf(" RELATÓRIO DE STATUS: %s%n", cache.getName());
        System.out.println("=======================================================");
        System.out.println("Linha | Val |  Tag   | Endereço | Dado (Hex)   | Dado (Dec)");
        System.out.println("------+-----+--------+----------+--------------+-----------");
        for (Cache.CacheLine line : cache.getLines()) {
            if (line.isValid()) {
                System.out.printf("  %02d  |  1  | 0x%04X |  0x%04X  |  0x%08X  | %d%n",
                        line.getIndex(), line.getTag(), line.getAddress(), line.getData(), line.getData());
            } else {
                System.out.printf("  %02d  |  0  | ------ |  ------  |  ----------  | -----%n", line.getIndex());
            }
        }
        System.out.println("------+-----+--------+----------+--------------+-----------");
        System.out.printf("Total de Acessos : %d%n", cache.getAccessCount());
        System.out.printf("Total de Leituras: %d%n", cache.getReadCount());
        System.out.printf("Total de Escritas: %d%n", cache.getWriteCount());
        System.out.printf("Hits             : %d (%.2f%%)%n", cache.getHitCount(), cache.getHitRate());
        System.out.printf("Misses           : %d (%.2f%%)%n", cache.getMissCount(), cache.getMissRate());
        System.out.println();
    }
}
