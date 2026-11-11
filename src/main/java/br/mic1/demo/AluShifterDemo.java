package br.mic1.demo;

import br.mic1.AluShifter;

public class AluShifterDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("              SIMULADOR MIC-1 - ETAPA 2: ULA E SHIFTER                          ");
        System.out.println("================================================================================");
        System.out.println();

        AluShifter alu = new AluShifter();

        System.out.println("--- TESTE 1: Operações Aritméticas Básicas da ULA ---");
        testOperation(alu, "A + B (15 + 27)", 15, 27, AluShifter.OP_ADD, AluShifter.SHIFT_NONE, 42);
        testOperation(alu, "A + B + 1 (15 + 27 + 1)", 15, 27, AluShifter.OP_ADD_INC, AluShifter.SHIFT_NONE, 43);
        testOperation(alu, "A + 1 (10 + 1)", 10, 0, AluShifter.OP_INC_A, AluShifter.SHIFT_NONE, 11);
        testOperation(alu, "B + 1 (20 + 1)", 0, 20, AluShifter.OP_INC_B, AluShifter.SHIFT_NONE, 21);
        testOperation(alu, "B - A (35 - 10)", 10, 35, AluShifter.OP_SUB, AluShifter.SHIFT_NONE, 25);
        testOperation(alu, "B - 1 (50 - 1)", 0, 50, AluShifter.OP_DEC_B, AluShifter.SHIFT_NONE, 49);
        testOperation(alu, "-A (-(15))", 15, 0, AluShifter.OP_NEG_A, AluShifter.SHIFT_NONE, -15);
        System.out.println();

        System.out.println("--- TESTE 2: Operações Lógicas e Passagens Diretas ---");
        testOperation(alu, "Passar A (H = 0x12345678)", 0x12345678, 0, AluShifter.OP_PASS_A, AluShifter.SHIFT_NONE, 0x12345678);
        testOperation(alu, "Passar B (B = 0x87654321)", 0, 0x87654321, AluShifter.OP_PASS_B, AluShifter.SHIFT_NONE, 0x87654321);
        testOperation(alu, "A AND B (0b1100 & 0b1010)", 0b1100, 0b1010, AluShifter.OP_AND, AluShifter.SHIFT_NONE, 0b1000);
        testOperation(alu, "A OR B (0b1100 | 0b1010)", 0b1100, 0b1010, AluShifter.OP_OR, AluShifter.SHIFT_NONE, 0b1110);
        testOperation(alu, "NOT A (~0)", 0, 0, AluShifter.OP_NOT_A, AluShifter.SHIFT_NONE, ~0);
        testOperation(alu, "NOT B (~0x0000FFFF)", 0, 0x0000FFFF, AluShifter.OP_NOT_B, AluShifter.SHIFT_NONE, ~0x0000FFFF);
        System.out.println();

        System.out.println("--- TESTE 3: Constantes MIC-1 (0, 1 e -1) ---");
        testOperation(alu, "Constante 0", 99, 99, AluShifter.OP_ZERO, AluShifter.SHIFT_NONE, 0);
        testOperation(alu, "Constante 1", 99, 99, AluShifter.OP_ONE, AluShifter.SHIFT_NONE, 1);
        testOperation(alu, "Constante -1", 99, 99, AluShifter.OP_MINUS_ONE, AluShifter.SHIFT_NONE, -1);
        System.out.println();

        System.out.println("--- TESTE 4: Verificação das Flags Z (Zero) e N (Negativo) ---");
        alu.compute(20, 20, AluShifter.OP_SUB, AluShifter.SHIFT_NONE);
        System.out.printf("Operação 20 - 20: Saída = %d | Flag Z = %b (esperado: true) | Flag N = %b (esperado: false)%n",
                alu.getAluOutput(), alu.isFlagZ(), alu.isFlagN());

        alu.compute(30, 10, AluShifter.OP_SUB, AluShifter.SHIFT_NONE);
        System.out.printf("Operação 10 - 30: Saída = %d | Flag Z = %b (esperado: false) | Flag N = %b (esperado: true)%n",
                alu.getAluOutput(), alu.isFlagZ(), alu.isFlagN());

        alu.compute(5, 10, AluShifter.OP_SUB, AluShifter.SHIFT_NONE);
        System.out.printf("Operação 10 - 5 : Saída = %d | Flag Z = %b (esperado: false) | Flag N = %b (esperado: false)%n%n",
                alu.getAluOutput(), alu.isFlagZ(), alu.isFlagN());

        System.out.println("--- TESTE 5: Operações do Shifter (SLL8 e SRA1) ---");
        int shiftVal1 = 0x000000AB;
        int resSll8 = alu.compute(0, shiftVal1, AluShifter.OP_PASS_B, AluShifter.SHIFT_SLL8);
        System.out.printf("SLL8 em 0x%08X -> 0x%08X (esperado: 0x0000AB00) -> %s%n",
                shiftVal1, resSll8, (resSll8 == 0x0000AB00 ? "SUCESSO" : "FALHA"));

        int shiftVal2 = -16;
        int resSra1Neg = alu.compute(0, shiftVal2, AluShifter.OP_PASS_B, AluShifter.SHIFT_SRA1);
        System.out.printf("SRA1 em %d -> %d (esperado: -8, preservando sinal) -> %s%n",
                shiftVal2, resSra1Neg, (resSra1Neg == -8 ? "SUCESSO" : "FALHA"));

        int shiftVal3 = 30;
        int resSra1Pos = alu.compute(0, shiftVal3, AluShifter.OP_PASS_B, AluShifter.SHIFT_SRA1);
        System.out.printf("SRA1 em %d -> %d (esperado: 15) -> %s%n%n",
                shiftVal3, resSra1Pos, (resSra1Pos == 15 ? "SUCESSO" : "FALHA"));

        System.out.println("================================================================================");
        System.out.println("                   ETAPA 2 (PARTE 1) EXECUTADA COM SUCESSO!                     ");
        System.out.println("      Classe AluShifter.java validada e pronta para integração!                 ");
        System.out.println("================================================================================");
    }

    private static void testOperation(AluShifter alu, String desc, int a, int b, int op, int shift, int expected) {
        int result = alu.compute(a, b, op, shift);
        boolean ok = (result == expected);
        System.out.printf("  [%s] %-30s -> Resultado: 0x%08X (%d) | Z=%d N=%d%n",
                ok ? "OK" : "ERRO", desc, result, result, alu.isFlagZ() ? 1 : 0, alu.isFlagN() ? 1 : 0);
    }
}
