package br.mic1.demo;

import br.mic1.Assembler;
import br.mic1.Memory;

public class AssemblerDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("            SIMULADOR MIC-1 - ETAPA 3: MONTADOR IJVM (ASSEMBLER)                ");
        System.out.println("================================================================================");
        System.out.println();

        String program1 = """
            BIPUSH 15
            BIPUSH 27
            IADD
            ISTORE 1
            NOP
            """;

        System.out.println("--- PROGRAMA 1: Código Fonte IJVM ---");
        System.out.println(program1);

        Memory mem1 = new Memory(1024);
        byte[] bytes1 = Assembler.assemble(program1, mem1);

        System.out.println("--- PROGRAMA 1: Bytecode Gerado ---");
        printBytecode(bytes1);
        System.out.println();

        System.out.println("--- PROGRAMA 1: Verificação na Memória Principal ---");
        for (int i = 0; i < bytes1.length; i++) {
            System.out.printf("  Memória[Byte %02d]: 0x%02X%n", i, mem1.readByte(i));
        }
        System.out.println();

        String program2 = """
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

        System.out.println("--- PROGRAMA 2: Laço com Rótulos e Variáveis ---");
        System.out.println(program2);

        Memory mem2 = new Memory(1024);
        byte[] bytes2 = Assembler.assemble(program2, mem2);

        System.out.println("--- PROGRAMA 2: Bytecode Gerado ---");
        printBytecode(bytes2);
        System.out.println();

        System.out.println("--- PROGRAMA 2: Desmontagem Instrução por Instrução ---");
        disassemble(bytes2);

        System.out.println("================================================================================");
        System.out.println("                   ETAPA 3 EXECUTADA COM TOTAL SUCESSO!                         ");
        System.out.println("       Assembler.java pronto para integração na GUI por Anna!                   ");
        System.out.println("================================================================================");
    }

    private static void printBytecode(byte[] bytecode) {
        System.out.printf("Total de bytes: %d%n", bytecode.length);
        System.out.print("Bytes (Hex): ");
        for (byte b : bytecode) {
            System.out.printf("%02X ", b);
        }
        System.out.println();
    }

    private static void disassemble(byte[] code) {
        int pc = 0;
        while (pc < code.length) {
            int addr = pc;
            int op = code[pc++] & 0xFF;
            switch (op) {
                case Assembler.OP_NOP       -> System.out.printf("  [0x%02X] NOP%n", addr);
                case Assembler.OP_DUP       -> System.out.printf("  [0x%02X] DUP%n", addr);
                case Assembler.OP_POP       -> System.out.printf("  [0x%02X] POP%n", addr);
                case Assembler.OP_SWAP      -> System.out.printf("  [0x%02X] SWAP%n", addr);
                case Assembler.OP_IADD      -> System.out.printf("  [0x%02X] IADD%n", addr);
                case Assembler.OP_ISUB      -> System.out.printf("  [0x%02X] ISUB%n", addr);
                case Assembler.OP_IAND      -> System.out.printf("  [0x%02X] IAND%n", addr);
                case Assembler.OP_IOR       -> System.out.printf("  [0x%02X] IOR%n", addr);
                case Assembler.OP_BIPUSH    -> {
                    byte val = code[pc++];
                    System.out.printf("  [0x%02X] BIPUSH %d (0x%02X)%n", addr, val, val);
                }
                case Assembler.OP_ILOAD     -> {
                    int var = code[pc++] & 0xFF;
                    System.out.printf("  [0x%02X] ILOAD var_%d%n", addr, var);
                }
                case Assembler.OP_ISTORE    -> {
                    int var = code[pc++] & 0xFF;
                    System.out.printf("  [0x%02X] ISTORE var_%d%n", addr, var);
                }
                case Assembler.OP_GOTO      -> {
                    short offset = (short) (((code[pc++] & 0xFF) << 8) | (code[pc++] & 0xFF));
                    System.out.printf("  [0x%02X] GOTO offset %+d -> Destino: 0x%02X%n", addr, offset, addr + offset);
                }
                case Assembler.OP_IFEQ      -> {
                    short offset = (short) (((code[pc++] & 0xFF) << 8) | (code[pc++] & 0xFF));
                    System.out.printf("  [0x%02X] IFEQ offset %+d -> Destino: 0x%02X%n", addr, offset, addr + offset);
                }
                case Assembler.OP_IFLT      -> {
                    short offset = (short) (((code[pc++] & 0xFF) << 8) | (code[pc++] & 0xFF));
                    System.out.printf("  [0x%02X] IFLT offset %+d -> Destino: 0x%02X%n", addr, offset, addr + offset);
                }
                case Assembler.OP_IF_ICMPEQ -> {
                    short offset = (short) (((code[pc++] & 0xFF) << 8) | (code[pc++] & 0xFF));
                    System.out.printf("  [0x%02X] IF_ICMPEQ offset %+d -> Destino: 0x%02X%n", addr, offset, addr + offset);
                }
                default -> System.out.printf("  [0x%02X] Desconhecido 0x%02X%n", addr, op);
            }
        }
        System.out.println();
    }
}
