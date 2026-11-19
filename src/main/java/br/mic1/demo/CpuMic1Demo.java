package br.mic1.demo;

import br.mic1.CpuMic1;
import br.mic1.Memory;

public class CpuMic1Demo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("            SIMULADOR MIC-1 - ETAPA 4: NÚCLEO DA CPU E DATAPATH                 ");
        System.out.println("================================================================================");
        System.out.println();

        testProgram1();
        System.out.println();
        testProgram2();

        System.out.println("================================================================================");
        System.out.println("                   ETAPA 4 CONCLUÍDA COM TOTAL SUCESSO!                         ");
        System.out.println("       CpuMic1.java pronto para integração visual com JavaFX por Anna!          ");
        System.out.println("================================================================================");
    }

    private static void testProgram1() {
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println(" TESTE 1: Execução Aritmética Simples (BIPUSH 15 + BIPUSH 27 = 42 -> ISTORE 1) ");
        System.out.println("--------------------------------------------------------------------------------");

        String code = """
            BIPUSH 15
            BIPUSH 27
            IADD
            ISTORE 1
            NOP
            """;

        CpuMic1 cpu = new CpuMic1();
        cpu.loadProgram(code);

        System.out.printf("[ESTADO INICIAL] %s%n%n", cpu);

        System.out.println("Executando macroinstrução por macroinstrução (stepInstruction):");

        // 1. BIPUSH 15
        int cycles1 = cpu.stepInstruction();
        System.out.printf("  1. BIPUSH 15 concluída em %d ciclos -> TOS: %d | SP: 0x%04X%n",
                cycles1, cpu.getTos(), cpu.getSp());

        // 2. BIPUSH 27
        int cycles2 = cpu.stepInstruction();
        System.out.printf("  2. BIPUSH 27 concluída em %d ciclos -> TOS: %d | SP: 0x%04X%n",
                cycles2, cpu.getTos(), cpu.getSp());

        // 3. IADD
        int cycles3 = cpu.stepInstruction();
        System.out.printf("  3. IADD      concluída em %d ciclos -> TOS: %d | SP: 0x%04X%n",
                cycles3, cpu.getTos(), cpu.getSp());

        // 4. ISTORE 1
        int cycles4 = cpu.stepInstruction();
        int var1Value = cpu.getMemory().read(cpu.getLv() + 1);
        System.out.printf("  4. ISTORE 1  concluída em %d ciclos -> Variável 1 na Memória [0x%04X]: %d%n",
                cycles4, cpu.getLv() + 1, var1Value);

        // 5. NOP
        int cycles5 = cpu.stepInstruction();
        System.out.printf("  5. NOP       concluída em %d ciclos -> Ciclos totais: %d%n%n",
                cycles5, cpu.getCycleCount());

        boolean success = (var1Value == 42);
        System.out.printf(">>> RESULTADO TESTE 1: %s (Valor da soma esperado: 42, obtido: %d)%n",
                (success ? "SUCESSO" : "FALHA"), var1Value);

        System.out.printf(">>> Estatísticas da I-Cache: %d acessos, %d hits (%.1f%%)%n",
                cpu.getICache().getAccessCount(), cpu.getICache().getHitCount(), cpu.getICache().getHitRate());
        System.out.printf(">>> Estatísticas da D-Cache: %d acessos, %d hits (%.1f%%)%n",
                cpu.getDCache().getAccessCount(), cpu.getDCache().getHitCount(), cpu.getDCache().getHitRate());
    }

    private static void testProgram2() {
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println(" TESTE 2: Execução de Laço Condicional (Somatório de 1 a 5 = 15 com IFEQ/GOTO) ");
        System.out.println("--------------------------------------------------------------------------------");

        String code = """
            .var
            i
            sum
            .end-var

            BIPUSH 0
            ISTORE sum

            BIPUSH 5
            ISTORE i

            loop:
            ILOAD i
            IFEQ done
            ILOAD sum
            ILOAD i
            IADD
            ISTORE sum
            ILOAD i
            BIPUSH 1
            ISUB
            ISTORE i
            GOTO loop

            done:
            NOP
            """;

        CpuMic1 cpu = new CpuMic1();
        cpu.loadProgram(code);

        // Executa até 10.000 ciclos ou atingir o NOP final
        // Para parar ao final, executamos instruções até que PC alcance o NOP no label done
        int maxInstructions = 1000;
        int instructionsCount = 0;

        while (instructionsCount < maxInstructions) {
            cpu.stepInstruction();
            instructionsCount++;
            // Quando atingir NOP no final do programa
            if (cpu.getMbr() == 0x00 && cpu.getMpc() == br.mic1.ControlStore.ADDR_MAIN1) {
                break;
            }
        }

        int finalSum = cpu.getMemory().read(cpu.getLv() + 2); // sum é a 2ª variável
        int finalI   = cpu.getMemory().read(cpu.getLv() + 1); // i é a 1ª variável

        System.out.printf("[ESTADO FINAL] %s%n", cpu);
        System.out.printf("  Total de Ciclos de Clock Executados : %d%n", cpu.getCycleCount());
        System.out.printf("  Valor final da variável 'i'         : %d (esperado: 0)%n", finalI);
        System.out.printf("  Valor final da variável 'sum'       : %d (esperado: 15)%n", finalSum);

        boolean success = (finalSum == 15 && finalI == 0);
        System.out.printf(">>> RESULTADO TESTE 2: %s (Somatório de 1 a 5 = %d)%n",
                (success ? "SUCESSO" : "FALHA"), finalSum);

        System.out.printf(">>> Estatísticas da I-Cache: %d acessos, %d hits (%.1f%%)%n",
                cpu.getICache().getAccessCount(), cpu.getICache().getHitCount(), cpu.getICache().getHitRate());
        System.out.printf(">>> Estatísticas da D-Cache: %d acessos, %d hits (%.1f%%)%n",
                cpu.getDCache().getAccessCount(), cpu.getDCache().getHitCount(), cpu.getDCache().getHitRate());
    }
}
