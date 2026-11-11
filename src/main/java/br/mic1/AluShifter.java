package br.mic1;

public class AluShifter {

    public static final int ALU_INC  = 0b000001;
    public static final int ALU_INVA = 0b000010;
    public static final int ALU_ENB  = 0b000100;
    public static final int ALU_ENA  = 0b001000;
    public static final int ALU_F1   = 0b010000;
    public static final int ALU_F0   = 0b100000;

    public static final int OP_PASS_A    = 0b011000;
    public static final int OP_PASS_B    = 0b010100;
    public static final int OP_NOT_A     = 0b011010;
    public static final int OP_NOT_B     = 0b101100;
    public static final int OP_ADD       = 0b111100;
    public static final int OP_ADD_INC   = 0b111101;
    public static final int OP_INC_A     = 0b111001;
    public static final int OP_INC_B     = 0b110101;
    public static final int OP_SUB       = 0b111111;
    public static final int OP_DEC_B     = 0b110110;
    public static final int OP_NEG_A     = 0b111011;
    public static final int OP_AND       = 0b001100;
    public static final int OP_OR        = 0b011100;
    public static final int OP_ZERO      = 0b010000;
    public static final int OP_ONE       = 0b110001;
    public static final int OP_MINUS_ONE = 0b110010;

    public static final int SHIFT_NONE = 0b00;
    public static final int SHIFT_SLL8 = 0b01;
    public static final int SHIFT_SRA1 = 0b10;

    private int aluOutput;
    private int shifterOutput;
    private boolean flagZ;
    private boolean flagN;

    public AluShifter() {
        reset();
    }

    public int compute(int busA, int busB, int aluControl, int shiftControl) {
        this.aluOutput = executeAlu(busA, busB, aluControl);
        this.flagZ = (this.aluOutput == 0);
        this.flagN = (this.aluOutput < 0);
        this.shifterOutput = executeShift(this.aluOutput, shiftControl);
        return this.shifterOutput;
    }

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
