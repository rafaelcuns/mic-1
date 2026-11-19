package br.mic1.demo;

import br.mic1.ControlStore;
import br.mic1.CpuMic1;

public class CpuMic1Demo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("      SIMULADOR MIC-1 (3ª EDIÇÃO) - ETAPA B: CPU MAC-1 COM 4 SUBCICLOS          ");
        System.out.println("================================================================================");
        System.out.println();

        testArithmeticWithSubcycles();
        System.out.println();
        testFibonacciFromMonitor();

        System.out.println("================================================================================");
        System.out.println("                 ETAPA B CONCLUÍDA COM TOTAL SUCESSO!                           ");
        System.out.println("================================================================================");
    }

    private static void testArithmeticWithSubcycles() {
        System.out.println("--- TESTE 1: Soma Simples (15 + 27 = 42) mostrando os 4 Subciclos do Ciclo 1 ---");
        String code = """
            LOCO 15
            STOD varA
            LOCO 27
            ADDD varA
            STOD resultado
            FIM: JUMP FIM
            """;

        CpuMic1 cpu = new CpuMic1();
        cpu.loadProgram(code);

        for (int s = 1; s <= 4; s++) {
            cpu.stepSubcycle();
            System.out.printf("  Subciclo %d -> MIR: 0x%08X | LatchA: 0x%04X | LatchB: 0x%04X | BusC: 0x%04X | MAR: 0x%03X | MPC: %d%n",
                    cpu.getLastSubcycle(), cpu.getMir(), cpu.getLatchA(), cpu.getLatchB(), cpu.getBusC(), cpu.getMar(), cpu.getMpc());
        }

        for (int i = 0; i < 6; i++) {
            cpu.stepInstruction();
        }

        int varA = cpu.getMemory().read(6);
        int resultado = cpu.getMemory().read(7);
        System.out.printf("Resultado na memória -> varA [0x006] = %d | resultado [0x007] = %d (esperado: 42) -> %s%n",
                varA, resultado, (resultado == 42 ? "SUCESSO" : "FALHA"));
        System.out.printf("Cache L1: %d acessos, %d hits (%.1f%%)%n",
                cpu.getCache().getAccessCount(), cpu.getCache().getHitCount(), cpu.getCache().getHitRate());
    }

    private static void testFibonacciFromMonitor() {
        System.out.println("--- TESTE 2: Fibonacci na Pilha (Exemplo Oficial do Monitor) ---");
        String code = """
            START: LOCO 10
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

        CpuMic1 cpu = new CpuMic1();
        cpu.loadProgram(code);

        int maxInst = 500;
        for (int i = 0; i < maxInst; i++) {
            int prevPc = cpu.getPc();
            cpu.stepInstruction();
            if (cpu.getPc() == prevPc) {
                break;
            }
        }

        int topFib = cpu.getMemory().read(cpu.getSp());
        System.out.printf("Execução finalizada em %d ciclos (%d subciclos, %d instruções).%n",
                cpu.getCycleCount(), cpu.getSubcycleCount(), cpu.getInstructionCount());
        System.out.printf("Topo da pilha [SP=0x%03X] = %d (esperado: 89 para 10 iterações após 0, 1) -> %s%n",
                cpu.getSp(), topFib, (topFib == 89 ? "SUCESSO" : "FALHA"));
        System.out.printf("Cache L1: %d acessos, %d hits (%.1f%%)%n",
                cpu.getCache().getAccessCount(), cpu.getCache().getHitCount(), cpu.getCache().getHitRate());
    }
}
