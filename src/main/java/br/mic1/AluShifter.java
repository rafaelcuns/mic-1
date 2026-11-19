package br.mic1;

public class AluShifter {

    public static final int AMUX_LATCH_A = 0;
    public static final int AMUX_MBR     = 1;

    public static final int OP_ADD    = 0;
    public static final int OP_AND    = 1;
    public static final int OP_PASS_A = 2;
    public static final int OP_NOT_A  = 3;

    public static final int SHIFT_NONE   = 0;
    public static final int SHIFT_RSHIFT = 1;
    public static final int SHIFT_LSHIFT = 2;

    private int amuxOutput;
    private int aluOutput;
    private int shifterOutput;
    private boolean flagZ;
    private boolean flagN;

    public AluShifter() {
        reset();
    }

    public int compute(int latchA, int latchB, int mbr, int amuxControl, int aluControl, int shiftControl) {
        this.amuxOutput = selectAmux(latchA, mbr, amuxControl);
        int opB = latchB & 0xFFFF;
        this.aluOutput = executeAlu(this.amuxOutput, opB, aluControl);
        this.flagZ = (this.aluOutput == 0);
        this.flagN = (this.aluOutput & 0x8000) != 0;
        this.shifterOutput = executeShift(this.aluOutput, shiftControl);
        return this.shifterOutput;
    }

    private int selectAmux(int latchA, int mbr, int amuxControl) {
        return switch (amuxControl) {
            case AMUX_LATCH_A -> latchA & 0xFFFF;
            case AMUX_MBR     -> mbr & 0xFFFF;
            default -> throw new IllegalArgumentException("Controle AMUX inválido: " + amuxControl);
        };
    }

    private int executeAlu(int opA, int opB, int aluControl) {
        return switch (aluControl) {
            case OP_ADD    -> (opA + opB) & 0xFFFF;
            case OP_AND    -> (opA & opB) & 0xFFFF;
            case OP_PASS_A -> opA & 0xFFFF;
            case OP_NOT_A  -> (~opA) & 0xFFFF;
            default -> throw new IllegalArgumentException("Código de operação ULA inválido: " + aluControl);
        };
    }

    private int executeShift(int value, int shiftControl) {
        return switch (shiftControl) {
            case SHIFT_NONE   -> value & 0xFFFF;
            case SHIFT_RSHIFT -> (((short) value) >> 1) & 0xFFFF;
            case SHIFT_LSHIFT -> (value << 1) & 0xFFFF;
            default -> throw new IllegalArgumentException("Controle de Shifter inválido: " + shiftControl);
        };
    }

    public void reset() {
        this.amuxOutput = 0;
        this.aluOutput = 0;
        this.shifterOutput = 0;
        this.flagZ = false;
        this.flagN = false;
    }

    public int getAmuxOutput() {
        return amuxOutput;
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
        return String.format("[AMUX: 0x%04X | ULA: 0x%04X (%d) | Shifter: 0x%04X | Z: %d | N: %d]",
                amuxOutput, aluOutput, (short) aluOutput, shifterOutput, flagZ ? 1 : 0, flagN ? 1 : 0);
    }
}
