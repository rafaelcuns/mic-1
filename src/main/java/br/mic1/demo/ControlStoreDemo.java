package br.mic1.demo;

import br.mic1.ControlStore;

public class ControlStoreDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("        SIMULADOR MIC-1 (3ª EDIÇÃO) - ETAPA B: CONTROL STORE (32 BITS)          ");
        System.out.println("================================================================================");
        System.out.println();

        ControlStore cs = new ControlStore();

        System.out.println("--- TESTE 1: Ciclo de Busca e Decodificação (MPC 0 a 5) ---");
        for (int mpc = 0; mpc <= 5; mpc++) {
            int mir = cs.getInstruction(mpc);
            System.out.printf("  MPC %03d | MIR: 0x%08X | %s%n",
                    mpc, mir, ControlStore.formatMicroinstruction(mir));
        }
        System.out.println();

        System.out.println("--- TESTE 2: Lógica de Desvio (COND_NONE, COND_N, COND_Z, COND_JUMP) ---");
        int mir2 = cs.getInstruction(2);
        System.out.printf("MPC 2 (if N goto 28) com N=false -> %d (esperado: 3)%n",
                ControlStore.calculateNextMpc(2, mir2, false, false));
        System.out.printf("MPC 2 (if N goto 28) com N=true  -> %d (esperado: 28)%n",
                ControlStore.calculateNextMpc(2, mir2, false, true));

        int mir24 = cs.getInstruction(24);
        System.out.printf("MPC 24 (if Z goto 23) com Z=true -> %d (esperado: 23)%n",
                ControlStore.calculateNextMpc(24, mir24, true, false));

        int mir77 = cs.getInstruction(77);
        System.out.printf("MPC 77 (LOCO: goto 0)            -> %d (esperado: 0) | %s%n%n",
                ControlStore.calculateNextMpc(77, mir77, false, false),
                ControlStore.formatMicroinstruction(mir77));

        System.out.println("================================================================================");
        System.out.println("                ETAPA B (ControlStore 32 bits) CONCLUÍDA!                       ");
        System.out.println("================================================================================");
    }
}
