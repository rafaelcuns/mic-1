package br.mic1.demo;

import br.mic1.Assembler;
import br.mic1.Memory;

public class AssemblerDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("        SIMULADOR MIC-1 (3ª EDIÇÃO) - ETAPA A: MONTADOR MAC-1 (16 BITS)         ");
        System.out.println("================================================================================");
        System.out.println();

        String program1 = """
            LOCO 15
            STOD varA
            LOCO 27
            ADDD varA
            STOD resultado
            FIM: JUMP FIM
            """;

        System.out.println("--- PROGRAMA 1: Soma Simples em MAC-1 ---");
        System.out.println(program1);

        Memory mem1 = new Memory();
        int[] words1 = Assembler.assemble(program1, mem1);
        printDisassembly(words1, mem1);

        String program2 = """
            START: LOCO 16
            STOD var1
            LOCO 1
            STOD var2
            LOCO 0
            PUSH
            LOCO 1
            PUSH
            LOOP: LODL 1
            ADDL 0
            PUSH
            LODD var1
            SUBD var2
            STOD var1
            JZER END
            JUMP LOOP
            END: JUMP END
            """;

        System.out.println("--- PROGRAMA 2: Sequência de Fibonacci em MAC-1 (Exemplo do Monitor) ---");
        System.out.println(program2);

        Memory mem2 = new Memory();
        int[] words2 = Assembler.assemble(program2, mem2);
        printDisassembly(words2, mem2);

        System.out.println("================================================================================");
        System.out.println("                 ETAPA A (MONTADOR MAC-1) CONCLUÍDA!                            ");
        System.out.println("================================================================================");
    }

    private static void printDisassembly(int[] words, Memory mem) {
        System.out.printf("Total de palavras (16 bits): %d%n", words.length);
        for (int i = 0; i < words.length; i++) {
            int memWord = mem.read(i);
            System.out.printf("  [0x%03X] 0x%04X  ->  %s%n", i, memWord, Assembler.disassembleWord(memWord));
        }
        System.out.println();
    }
}
