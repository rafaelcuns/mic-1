package br.mic1.demo;

import br.mic1.AluShifter;

public class AluShifterDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("        SIMULADOR MIC-1 (3ª EDIÇÃO) - ETAPA B: AMUX, ULA E SHIFTER (16 BITS)    ");
        System.out.println("================================================================================");
        System.out.println();

        AluShifter alu = new AluShifter();

        System.out.println("--- TESTE 1: Operações da ULA de 16 bits ---");
        testOp(alu, "A + B (15 + 27)", 15, 27, 0, AluShifter.AMUX_LATCH_A, AluShifter.OP_ADD, AluShifter.SHIFT_NONE, 42);
        testOp(alu, "MBR + B (100 + 50)", 0, 50, 100, AluShifter.AMUX_MBR, AluShifter.OP_ADD, AluShifter.SHIFT_NONE, 150);
        testOp(alu, "A & B (0x7123 & 0x0FFF)", 0x7123, 0x0FFF, 0, AluShifter.AMUX_LATCH_A, AluShifter.OP_AND, AluShifter.SHIFT_NONE, 0x0123);
        testOp(alu, "PASS A (0x1234)", 0x1234, 0, 0, AluShifter.AMUX_LATCH_A, AluShifter.OP_PASS_A, AluShifter.SHIFT_NONE, 0x1234);
        testOp(alu, "NOT A (~0x00FF)", 0x00FF, 0, 0, AluShifter.AMUX_LATCH_A, AluShifter.OP_NOT_A, AluShifter.SHIFT_NONE, 0xFF00);
        System.out.println();

        System.out.println("--- TESTE 2: Flags Z e N (16 bits) ---");
        alu.compute(0, 0, 0, AluShifter.AMUX_LATCH_A, AluShifter.OP_PASS_A, AluShifter.SHIFT_NONE);
        System.out.printf("Saída = 0x%04X | Z = %b (esperado: true) | N = %b (esperado: false)%n",
                alu.getAluOutput(), alu.isFlagZ(), alu.isFlagN());

        alu.compute(0xFFFF, 0, 0, AluShifter.AMUX_LATCH_A, AluShifter.OP_PASS_A, AluShifter.SHIFT_NONE);
        System.out.printf("Saída = 0x%04X (-1) | Z = %b (esperado: false) | N = %b (esperado: true)%n%n",
                alu.getAluOutput(), alu.isFlagZ(), alu.isFlagN());

        System.out.println("--- TESTE 3: Shifter de 1 bit (LSHIFT e RSHIFT) ---");
        testOp(alu, "LSHIFT 1 (0x0005 << 1)", 0x0005, 0, 0, AluShifter.AMUX_LATCH_A, AluShifter.OP_PASS_A, AluShifter.SHIFT_LSHIFT, 0x000A);
        testOp(alu, "RSHIFT 1 (0xFFF0 >> 1 = -8)", 0xFFF0, 0, 0, AluShifter.AMUX_LATCH_A, AluShifter.OP_PASS_A, AluShifter.SHIFT_RSHIFT, 0xFFF8);

        System.out.println("================================================================================");
        System.out.println("                ETAPA B (AluShifter 16 bits) CONCLUÍDA!                         ");
        System.out.println("================================================================================");
    }

    private static void testOp(AluShifter alu, String desc, int a, int b, int mbr, int amux, int op, int sh, int expected) {
        int res = alu.compute(a, b, mbr, amux, op, sh);
        System.out.printf("  [%s] %-30s -> 0x%04X (%d) | Z=%d N=%d%n",
                (res == expected ? "OK" : "ERRO"), desc, res, (short) res, alu.isFlagZ() ? 1 : 0, alu.isFlagN() ? 1 : 0);
    }
}
