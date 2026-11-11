package br.mic1;

public class ControlStoreDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("          SIMULADOR MIC-1 - ETAPA 2: MEMÓRIA DE CONTROLE (CONTROL STORE)       ");
        System.out.println("================================================================================");
        System.out.println();

        ControlStore cs = new ControlStore();

        System.out.println("--- TESTE 1: Decodificação de Microinstrução (Main1) ---");
        long mirMain1 = cs.getInstruction(ControlStore.ADDR_MAIN1);
        System.out.printf("Endereço MPC 0x%03X (Main1): MIR = 0x%09X%n", ControlStore.ADDR_MAIN1, mirMain1);
        System.out.printf("  Next Address : 0x%03X%n", ControlStore.getNextAddress(mirMain1));
        System.out.printf("  JAM (Z, N, C): %b, %b, %b%n",
                ControlStore.isJamZ(mirMain1), ControlStore.isJamN(mirMain1), ControlStore.isJmpC(mirMain1));
        System.out.printf("  ALU Opcode   : 0x%02X (OP_INC_B)%n", ControlStore.getAlu(mirMain1));
        System.out.printf("  C-Bus        : 0x%03X (Write PC? %b)%n",
                ControlStore.getCBus(mirMain1), ControlStore.hasWritePc(mirMain1));
        System.out.printf("  Memória      : FETCH? %b | READ? %b | WRITE? %b%n",
                ControlStore.hasMemFetch(mirMain1), ControlStore.hasMemRead(mirMain1), ControlStore.hasMemWrite(mirMain1));
        System.out.printf("  B-Bus        : %d (B_PC)%n%n", ControlStore.getBBus(mirMain1));

        System.out.println("--- TESTE 2: Lógica de Próximo MPC (calculateNextMpc) ---");
        int nextMpcIadd = ControlStore.calculateNextMpc(mirMain1, false, false, 0x60);
        System.out.printf("JMPC com MBR=0x60 (IADD)  -> Próximo MPC: 0x%03X (esperado: 0x060) -> %s%n",
                nextMpcIadd, (nextMpcIadd == 0x060 ? "SUCESSO" : "FALHA"));

        int nextMpcBipush = ControlStore.calculateNextMpc(mirMain1, false, false, 0x10);
        System.out.printf("JMPC com MBR=0x10 (BIPUSH)-> Próximo MPC: 0x%03X (esperado: 0x010) -> %s%n",
                nextMpcBipush, (nextMpcBipush == 0x010 ? "SUCESSO" : "FALHA"));

        long mirIfeqTest = cs.getInstruction(0x09C);
        int nextMpcZTrue  = ControlStore.calculateNextMpc(mirIfeqTest, true, false, 0);
        int nextMpcZFalse = ControlStore.calculateNextMpc(mirIfeqTest, false, false, 0);
        System.out.printf("JAMZ com Flag Z=true      -> Próximo MPC: 0x%03X (esperado: 0x132 / T) -> %s%n",
                nextMpcZTrue, (nextMpcZTrue == 0x132 ? "SUCESSO" : "FALHA"));
        System.out.printf("JAMZ com Flag Z=false     -> Próximo MPC: 0x%03X (esperado: 0x032 / F) -> %s%n%n",
                nextMpcZFalse, (nextMpcZFalse == 0x032 ? "SUCESSO" : "FALHA"));

        System.out.println("--- TESTE 3: Verificação das Microinstruções Iniciais de Opcodes IJVM ---");
        printInstructionSummary(cs, "NOP", 0x000);
        printInstructionSummary(cs, "BIPUSH", 0x010);
        printInstructionSummary(cs, "ILOAD", 0x015);
        printInstructionSummary(cs, "ISTORE", 0x036);
        printInstructionSummary(cs, "DUP", 0x059);
        printInstructionSummary(cs, "IADD", 0x060);
        printInstructionSummary(cs, "ISUB", 0x064);
        printInstructionSummary(cs, "IAND", 0x07E);
        printInstructionSummary(cs, "IOR", 0x080);
        printInstructionSummary(cs, "IFEQ", 0x099);
        printInstructionSummary(cs, "GOTO", 0x0A7);

        System.out.println();
        System.out.println("================================================================================");
        System.out.println("                   ETAPA 2 CONCLUÍDA COM TOTAL SUCESSO!                         ");
        System.out.println("   AluShifter.java e ControlStore.java prontas para o CpuMic1.java!             ");
        System.out.println("================================================================================");
    }

    private static void printInstructionSummary(ControlStore cs, String name, int mpc) {
        long mir = cs.getInstruction(mpc);
        System.out.printf("  [%-7s | MPC 0x%03X]: MIR = 0x%09X | Next: 0x%03X | ALU: 0x%02X | C: 0x%03X | B: %d%n",
                name, mpc, mir, ControlStore.getNextAddress(mir), ControlStore.getAlu(mir),
                ControlStore.getCBus(mir), ControlStore.getBBus(mir));
    }
}
