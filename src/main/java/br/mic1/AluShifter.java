package br.mic1;

/**
 * Modela a ULA (Unidade Lógica e Aritmética) e o Shifter da microarquitetura MIC-1 (Tanenbaum).
 * 
 * <p>A ULA opera sobre duas entradas de 32 bits (Barramento A / Registrador H e Barramento B)
 * controlada por 6 linhas de controle (F0, F1, ENA, ENB, INVA, INC).
 * As flags de condição Z (Zero) e N (Negativo) são atualizadas na saída direta da ULA.</p>
 * 
 * <p>O Shifter recebe a saída da ULA e aplica deslocamento nulo, lógico à esquerda de 8 bits (SLL8)
 * ou aritmético à direita de 1 bit (SRA1), enviando o resultado para o Barramento C.</p>
 */
public class AluShifter {

    // =========================================================================
    // MÁSCARAS E BITS DE CONTROLE DA ULA (6 BITS: F0, F1, ENA, ENB, INVA, INC)
    // =========================================================================
    public static final int ALU_INC  = 0b000001; // Bit 0: Carry-in (+1)
    public static final int ALU_INVA = 0b000010; // Bit 1: Inverte a entrada A
    public static final int ALU_ENB  = 0b000100; // Bit 2: Habilita entrada B (se 0, força zero)
    public static final int ALU_ENA  = 0b001000; // Bit 3: Habilita entrada A (se 0, força zero)
    public static final int ALU_F1   = 0b010000; // Bit 4: Função da ULA (bit 1)
    public static final int ALU_F0   = 0b100000; // Bit 5: Função da ULA (bit 0)

    // =========================================================================
    // CÓDIGOS DE CONTROLE DA ULA NO MIC-1 (Tabela 4-1 de Tanenbaum)
    // =========================================================================
    public static final int OP_PASS_A    = 0b011000; // A (Passagem direta de H)
    public static final int OP_PASS_B    = 0b010100; // B (Passagem direta do B-Bus)
    public static final int OP_NOT_A     = 0b011010; // ~A
    public static final int OP_NOT_B     = 0b101100; // ~B
    public static final int OP_ADD       = 0b111100; // A + B
    public static final int OP_ADD_INC   = 0b111101; // A + B + 1
    public static final int OP_INC_A     = 0b111001; // A + 1
    public static final int OP_INC_B     = 0b110101; // B + 1
    public static final int OP_SUB       = 0b111111; // B - A (B + ~A + 1)
    public static final int OP_DEC_B     = 0b110110; // B - 1 (B + ~0)
    public static final int OP_NEG_A     = 0b111011; // -A (~A + 1)
    public static final int OP_AND       = 0b001100; // A AND B
    public static final int OP_OR        = 0b011100; // A OR B
    public static final int OP_ZERO      = 0b010000; // 0
    public static final int OP_ONE       = 0b110001; // 1
    public static final int OP_MINUS_ONE = 0b110010; // -1

    // =========================================================================
    // CONTROLE DO SHIFTER (2 BITS: SLL8 e SRA1)
    // =========================================================================
    public static final int SHIFT_NONE = 0b00; // Sem deslocamento
    public static final int SHIFT_SLL8 = 0b01; // Shift Left Logical 8 bits (<< 8)
    public static final int SHIFT_SRA1 = 0b10; // Shift Right Arithmetic 1 bit (>> 1)

    // =========================================================================
    // ESTADO INTERNO
    // =========================================================================
    private int aluOutput;
    private int shifterOutput;
    private boolean flagZ;
    private boolean flagN;

    /**
     * Cria uma nova instância de AluShifter com saídas e flags zeradas.
     */
    public AluShifter() {
        reset();
    }

    /**
     * Executa o ciclo completo da ULA e do Shifter a partir dos sinais de controle codificados em inteiros.
     *
     * @param busA Valor do Barramento A (normalmente vindo do registrador H)
     * @param busB Valor do Barramento B (registrador selecionado no datapath)
     * @param aluControl Código de 6 bits da ULA (F0, F1, ENA, ENB, INVA, INC)
     * @param shiftControl Código de 2 bits do Shifter (SHIFT_NONE, SHIFT_SLL8, SHIFT_SRA1)
     * @return O resultado final de 32 bits disponibilizado no Barramento C
     */
    public int compute(int busA, int busB, int aluControl, int shiftControl) {
        this.aluOutput = executeAlu(busA, busB, aluControl);
        this.flagZ = (this.aluOutput == 0);
        this.flagN = (this.aluOutput < 0);
        this.shifterOutput = executeShift(this.aluOutput, shiftControl);
        return this.shifterOutput;
    }

    /**
     * Sobrecarga de conveniência que recebe os sinais booleanos individuais da microinstrução (MIR).
     *
     * @param busA Valor do Barramento A
     * @param busB Valor do Barramento B
     * @param f0 Sinal F0 da ULA
     * @param f1 Sinal F1 da ULA
     * @param ena Habilita entrada A
     * @param enb Habilita entrada B
     * @param inva Inverte entrada A
     * @param inc Carry-in (+1)
     * @param sll8 Habilita deslocamento lógico para a esquerda de 8 bits
     * @param sra1 Habilita deslocamento aritmético para a direita de 1 bit
     * @return O resultado final de 32 bits para o Barramento C
     */
    public int compute(int busA, int busB,
                       boolean f0, boolean f1, boolean ena, boolean enb, boolean inva, boolean inc,
                       boolean sll8, boolean sra1) {
        int aluCtrl = (f0   ? ALU_F0   : 0)
                    | (f1   ? ALU_F1   : 0)
                    | (ena  ? ALU_ENA  : 0)
                    | (enb  ? ALU_ENB  : 0)
                    | (inva ? ALU_INVA : 0)
                    | (inc  ? ALU_INC  : 0);

        int shiftCtrl = (sll8 ? SHIFT_SLL8 : 0)
                      | (sra1 ? SHIFT_SRA1 : 0);

        return compute(busA, busB, aluCtrl, shiftCtrl);
    }

    /**
     * Processa a lógica de 32 bits da ULA seguindo as portas e o somador de Tanenbaum (Fig. 4-2).
     */
    private int executeAlu(int busA, int busB, int aluControl) {
        int opA = (aluControl & ALU_ENA) != 0 ? busA : 0;
        int opB = (aluControl & ALU_ENB) != 0 ? busB : 0;

        if ((aluControl & ALU_INVA) != 0) {
            opA = ~opA;
        }

        boolean f0  = (aluControl & ALU_F0) != 0;
        boolean f1  = (aluControl & ALU_F1) != 0;
        boolean inc = (aluControl & ALU_INC) != 0;

        int func = (f0 ? 2 : 0) | (f1 ? 1 : 0);
        return switch (func) {
            case 0 -> opA & opB;
            case 1 -> opA | opB;
            case 2 -> ~opB;
            case 3 -> opA + opB + (inc ? 1 : 0);
            default -> throw new IllegalStateException("Função ALU inválida: " + func);
        };
    }

    /**
     * Aplica a operação de deslocamento selecionada ao valor fornecido pela ULA.
     */
    private int executeShift(int value, int shiftControl) {
        return switch (shiftControl) {
            case SHIFT_NONE -> value;
            case SHIFT_SLL8 -> value << 8;
            case SHIFT_SRA1 -> value >> 1;
            default -> throw new IllegalArgumentException(
                String.format("Controle de Shifter inválido ou conflitante: 0x%X (SLL8 e SRA1 não podem estar ambos ativos)", shiftControl)
            );
        };
    }

    /**
     * Reseta as flags e os valores calculados da ULA e do Shifter.
     */
    public void reset() {
        this.aluOutput = 0;
        this.shifterOutput = 0;
        this.flagZ = false;
        this.flagN = false;
    }

    public int getAluOutput() {
        return aluOutput;
    }

    public int getShifterOutput() {
        return shifterOutput;
    }

    public boolean isFlagZ() {
        return flagZ;
    }

    public boolean isFlagN() {
        return flagN;
    }

    @Override
    public String toString() {
        return String.format("[ULA: 0x%08X (%d) | Shifter: 0x%08X (%d) | Z: %d | N: %d]",
                aluOutput, aluOutput, shifterOutput, shifterOutput, flagZ ? 1 : 0, flagN ? 1 : 0);
    }
}
