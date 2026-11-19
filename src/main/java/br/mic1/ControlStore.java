package br.mic1;

import java.util.Arrays;

public class ControlStore {

    public static final int CAPACITY = 256;

    public static final int SHIFT_AMUX = 31;
    public static final int SHIFT_COND = 29;
    public static final int SHIFT_ALU  = 27;
    public static final int SHIFT_SH   = 25;
    public static final int SHIFT_MBR  = 24;
    public static final int SHIFT_MAR  = 23;
    public static final int SHIFT_RD   = 22;
    public static final int SHIFT_WR   = 21;
    public static final int SHIFT_ENC  = 20;
    public static final int SHIFT_C    = 16;
    public static final int SHIFT_B    = 12;
    public static final int SHIFT_A    = 8;
    public static final int SHIFT_ADDR = 0;

    public static final int COND_NONE = 0;
    public static final int COND_N    = 1;
    public static final int COND_Z    = 2;
    public static final int COND_JUMP = 3;

    public static final int REG_PC    = 0;
    public static final int REG_AC    = 1;
    public static final int REG_SP    = 2;
    public static final int REG_IR    = 3;
    public static final int REG_TIR   = 4;
    public static final int REG_ZERO  = 5;
    public static final int REG_P1    = 6;
    public static final int REG_M1    = 7;
    public static final int REG_AMASK = 8;
    public static final int REG_SMASK = 9;
    public static final int REG_A     = 10;
    public static final int REG_B     = 11;
    public static final int REG_C     = 12;
    public static final int REG_D     = 13;
    public static final int REG_E     = 14;
    public static final int REG_F     = 15;

    public static final String[] REG_NAMES = {
        "PC", "AC", "SP", "IR", "TIR", "0", "+1", "-1",
        "AMASK", "SMASK", "A", "B", "C", "D", "E", "F"
    };

    public static final int ADDR_FETCH = 0;

    private final int[] rom;

    public ControlStore() {
        this.rom = new int[CAPACITY];
        loadDefaultMicroprogram();
    }

    public int getInstruction(int mpc) {
        validateAddress(mpc);
        return rom[mpc];
    }

    public void setInstruction(int mpc, int instruction) {
        validateAddress(mpc);
        rom[mpc] = instruction;
    }

    public void clear() {
        Arrays.fill(rom, 0);
    }

    public static int getAmux(int mir) { return (mir >>> SHIFT_AMUX) & 0x1; }
    public static int getCond(int mir) { return (mir >>> SHIFT_COND) & 0x3; }
    public static int getAlu(int mir)  { return (mir >>> SHIFT_ALU)  & 0x3; }
    public static int getSh(int mir)   { return (mir >>> SHIFT_SH)   & 0x3; }
    public static boolean hasMbr(int mir) { return ((mir >>> SHIFT_MBR) & 0x1) != 0; }
    public static boolean hasMar(int mir) { return ((mir >>> SHIFT_MAR) & 0x1) != 0; }
    public static boolean hasRd(int mir)  { return ((mir >>> SHIFT_RD)  & 0x1) != 0; }
    public static boolean hasWr(int mir)  { return ((mir >>> SHIFT_WR)  & 0x1) != 0; }
    public static boolean hasEnc(int mir) { return ((mir >>> SHIFT_ENC) & 0x1) != 0; }
    public static int getC(int mir)    { return (mir >>> SHIFT_C)    & 0xF; }
    public static int getB(int mir)    { return (mir >>> SHIFT_B)    & 0xF; }
    public static int getA(int mir)    { return (mir >>> SHIFT_A)    & 0xF; }
    public static int getAddr(int mir) { return mir & 0xFF; }

    public static int calculateNextMpc(int currentMpc, int mir, boolean flagZ, boolean flagN) {
        int cond = getCond(mir);
        int addr = getAddr(mir);
        return switch (cond) {
            case COND_NONE -> (currentMpc + 1) & 0xFF;
            case COND_N    -> flagN ? addr : (currentMpc + 1) & 0xFF;
            case COND_Z    -> flagZ ? addr : (currentMpc + 1) & 0xFF;
            case COND_JUMP -> addr;
            default        -> (currentMpc + 1) & 0xFF;
        };
    }

    public static int buildInstruction(int amux, int cond, int alu, int sh,
                                       boolean mbr, boolean mar, boolean rd, boolean wr, boolean enc,
                                       int c, int b, int a, int addr) {
        int mir = 0;
        mir |= (amux & 0x1) << SHIFT_AMUX;
        mir |= (cond & 0x3) << SHIFT_COND;
        mir |= (alu  & 0x3) << SHIFT_ALU;
        mir |= (sh   & 0x3) << SHIFT_SH;
        if (mbr) mir |= (1 << SHIFT_MBR);
        if (mar) mir |= (1 << SHIFT_MAR);
        if (rd)  mir |= (1 << SHIFT_RD);
        if (wr)  mir |= (1 << SHIFT_WR);
        if (enc) mir |= (1 << SHIFT_ENC);
        mir |= (c & 0xF) << SHIFT_C;
        mir |= (b & 0xF) << SHIFT_B;
        mir |= (a & 0xF) << SHIFT_A;
        mir |= (addr & 0xFF);
        return mir;
    }

    public void loadDefaultMicroprogram() {
        clear();

        rom[0]  = buildInstruction(0, COND_NONE, AluShifter.OP_PASS_A, 0, false, true,  true,  false, false, 0,       REG_PC,    0,         0);
        rom[1]  = buildInstruction(0, COND_NONE, AluShifter.OP_ADD,    0, false, false, true,  false, true,  REG_PC,  REG_P1,    REG_PC,    0);
        rom[2]  = buildInstruction(1, COND_N,    AluShifter.OP_PASS_A, 0, false, false, false, false, true,  REG_IR,  0,         0,         28);
        rom[3]  = buildInstruction(0, COND_N,    AluShifter.OP_ADD,    0, false, false, false, false, true,  REG_TIR, REG_IR,    REG_IR,    20);
        rom[4]  = buildInstruction(0, COND_N,    AluShifter.OP_ADD,    0, false, false, false, false, true,  REG_TIR, REG_TIR,   REG_TIR,   11);
        rom[5]  = buildInstruction(0, COND_N,    AluShifter.OP_ADD,    0, false, false, false, false, false, 0,       REG_TIR,   REG_TIR,   9);

        rom[6]  = buildInstruction(0, COND_NONE, AluShifter.OP_PASS_A, 0, false, true,  true,  false, false, 0,       REG_IR,    0,         0);
        rom[7]  = buildInstruction(0, COND_NONE, AluShifter.OP_PASS_A, 0, false, false, true,  false, false, 0,       0,         0,         0);
        rom[8]  = buildInstruction(1, COND_JUMP, AluShifter.OP_PASS_A, 0, false, false, false, false, true,  REG_AC,  0,         0,         0);

        rom[9]  = buildInstruction(0, COND_NONE, AluShifter.OP_PASS_A, 0, true,  true,  false, true,  false, 0,       REG_IR,    REG_AC,    0);
        rom[10] = buildInstruction(0, COND_JUMP, AluShifter.OP_PASS_A, 0, false, false, false, true,  false, 0,       0,         0,         0);

        rom[11] = buildInstruction(0, COND_N,    AluShifter.OP_ADD,    0, false, false, false, false, false, 0,       REG_TIR,   REG_TIR,   15);

        rom[12] = buildInstruction(0, COND_NONE, AluShifter.OP_PASS_A, 0, false, true,  true,  false, false, 0,       REG_IR,    0,         0);
        rom[13] = buildInstruction(0, COND_NONE, AluShifter.OP_PASS_A, 0, false, false, true,  false, false, 0,       0,         0,         0);
        rom[14] = buildInstruction(1, COND_JUMP, AluShifter.OP_ADD,    0, false, false, false, false, true,  REG_AC,  REG_AC,    0,         0);

        rom[15] = buildInstruction(0, COND_NONE, AluShifter.OP_PASS_A, 0, false, true,  true,  false, false, 0,       REG_IR,    0,         0);
        rom[16] = buildInstruction(0, COND_NONE, AluShifter.OP_PASS_A, 0, false, false, true,  false, false, 0,       0,         0,         0);
        rom[17] = buildInstruction(1, COND_NONE, AluShifter.OP_NOT_A,  0, false, false, false, false, true,  REG_A,   0,         0,         0);
        rom[18] = buildInstruction(0, COND_NONE, AluShifter.OP_ADD,    0, false, false, false, false, true,  REG_A,   REG_P1,    REG_A,     0);
        rom[19] = buildInstruction(0, COND_JUMP, AluShifter.OP_ADD,    0, false, false, false, false, true,  REG_AC,  REG_AC,    REG_A,     0);

        rom[20] = buildInstruction(0, COND_N,    AluShifter.OP_ADD,    0, false, false, false, false, true,  REG_TIR, REG_TIR,   REG_TIR,   26);
        rom[21] = buildInstruction(0, COND_N,    AluShifter.OP_ADD,    0, false, false, false, false, false, 0,       REG_TIR,   REG_TIR,   24);
        rom[22] = buildInstruction(0, COND_N,    AluShifter.OP_PASS_A, 0, false, false, false, false, false, 0,       0,         REG_AC,    0);
        rom[23] = buildInstruction(0, COND_JUMP, AluShifter.OP_AND,    0, false, false, false, false, true,  REG_PC,  REG_AMASK, REG_IR,    0);
        rom[24] = buildInstruction(0, COND_Z,    AluShifter.OP_PASS_A, 0, false, false, false, false, false, 0,       0,         REG_AC,    23);
        rom[25] = buildInstruction(0, COND_JUMP, AluShifter.OP_PASS_A, 0, false, false, false, false, false, 0,       0,         0,         0);
        rom[26] = buildInstruction(0, COND_N,    AluShifter.OP_ADD,    0, false, false, false, false, false, 0,       REG_TIR,   REG_TIR,   77);
        rom[27] = buildInstruction(0, COND_JUMP, AluShifter.OP_AND,    0, false, false, false, false, true,  REG_PC,  REG_AMASK, REG_IR,    0);

        rom[28] = buildInstruction(0, COND_N,    AluShifter.OP_ADD,    0, false, false, false, false, true,  REG_TIR, REG_IR,    REG_IR,    40);
        rom[29] = buildInstruction(0, COND_N,    AluShifter.OP_ADD,    0, false, false, false, false, true,  REG_TIR, REG_TIR,   REG_TIR,   36);
        rom[30] = buildInstruction(0, COND_N,    AluShifter.OP_ADD,    0, false, false, false, false, false, 0,       REG_TIR,   REG_TIR,   33);

        rom[31] = buildInstruction(0, COND_NONE, AluShifter.OP_ADD,    0, false, false, false, false, true,  REG_A,   REG_SP,    REG_IR,    0);
        rom[32] = buildInstruction(0, COND_JUMP, AluShifter.OP_PASS_A, 0, false, true,  true,  false, false, 0,       REG_A,     0,         7);

        rom[33] = buildInstruction(0, COND_NONE, AluShifter.OP_ADD,    0, false, false, false, false, true,  REG_A,   REG_SP,    REG_IR,    0);
        rom[34] = buildInstruction(0, COND_JUMP, AluShifter.OP_PASS_A, 0, true,  true,  false, true,  false, 0,       REG_A,     REG_AC,    10);

        rom[36] = buildInstruction(0, COND_NONE, AluShifter.OP_ADD,    0, false, false, false, false, true,  REG_A,   REG_SP,    REG_IR,    0);
        rom[37] = buildInstruction(0, COND_N,    AluShifter.OP_ADD,    0, false, false, false, false, false, 0,       REG_TIR,   REG_TIR,   39);
        rom[38] = buildInstruction(0, COND_JUMP, AluShifter.OP_PASS_A, 0, false, true,  true,  false, false, 0,       REG_A,     0,         13);
        rom[39] = buildInstruction(0, COND_JUMP, AluShifter.OP_PASS_A, 0, false, true,  true,  false, false, 0,       REG_A,     0,         16);

        rom[40] = buildInstruction(0, COND_N,    AluShifter.OP_ADD,    0, false, false, false, false, true,  REG_TIR, REG_TIR,   REG_TIR,   46);
        rom[41] = buildInstruction(0, COND_N,    AluShifter.OP_ADD,    0, false, false, false, false, false, 0,       REG_TIR,   REG_TIR,   44);

        rom[42] = buildInstruction(0, COND_N,    AluShifter.OP_PASS_A, 0, false, false, false, false, false, 0,       0,         REG_AC,    27);
        rom[43] = buildInstruction(0, COND_JUMP, AluShifter.OP_PASS_A, 0, false, false, false, false, false, 0,       0,         0,         0);

        rom[44] = buildInstruction(0, COND_Z,    AluShifter.OP_PASS_A, 0, false, false, false, false, false, 0,       0,         REG_AC,    0);
        rom[45] = buildInstruction(0, COND_JUMP, AluShifter.OP_PASS_A, 0, false, false, false, false, false, 0,       0,         0,         27);

        rom[46] = buildInstruction(0, COND_N,    AluShifter.OP_ADD,    0, false, false, false, false, true,  REG_TIR, REG_TIR,   REG_TIR,   50);

        rom[47] = buildInstruction(0, COND_NONE, AluShifter.OP_ADD,    0, false, false, false, false, true,  REG_SP,  REG_SP,    REG_M1,    0);
        rom[48] = buildInstruction(0, COND_NONE, AluShifter.OP_PASS_A, 0, true,  true,  false, true,  false, 0,       REG_SP,    REG_PC,    0);
        rom[49] = buildInstruction(0, COND_JUMP, AluShifter.OP_AND,    0, false, false, false, true,  true,  REG_PC,  REG_AMASK, REG_IR,    0);

        rom[50] = buildInstruction(0, COND_N,    AluShifter.OP_ADD,    0, false, false, false, false, true,  REG_TIR, REG_TIR,   REG_TIR,   65);
        rom[51] = buildInstruction(0, COND_N,    AluShifter.OP_ADD,    0, false, false, false, false, true,  REG_TIR, REG_TIR,   REG_TIR,   59);
        rom[52] = buildInstruction(0, COND_N,    AluShifter.OP_ADD,    0, false, false, false, false, false, 0,       REG_TIR,   REG_TIR,   56);

        rom[53] = buildInstruction(0, COND_NONE, AluShifter.OP_PASS_A, 0, false, true,  true,  false, false, 0,       REG_AC,    0,         0);
        rom[54] = buildInstruction(0, COND_NONE, AluShifter.OP_ADD,    0, false, false, true,  false, true,  REG_SP,  REG_SP,    REG_M1,    0);
        rom[55] = buildInstruction(0, COND_JUMP, AluShifter.OP_PASS_A, 0, false, true,  false, true,  false, 0,       REG_SP,    0,         10);

        rom[56] = buildInstruction(0, COND_NONE, AluShifter.OP_ADD,    0, false, true,  true,  false, true,  REG_SP,  REG_SP,    REG_P1,    0);
        rom[57] = buildInstruction(0, COND_NONE, AluShifter.OP_PASS_A, 0, false, false, true,  false, false, 0,       0,         0,         0);
        rom[58] = buildInstruction(0, COND_JUMP, AluShifter.OP_PASS_A, 0, false, true,  false, true,  false, 0,       REG_AC,    0,         10);

        rom[59] = buildInstruction(0, COND_N,    AluShifter.OP_ADD,    0, false, false, false, false, false, 0,       REG_TIR,   REG_TIR,   62);

        rom[60] = buildInstruction(0, COND_NONE, AluShifter.OP_ADD,    0, false, false, false, false, true,  REG_SP,  REG_SP,    REG_M1,    0);
        rom[61] = buildInstruction(0, COND_JUMP, AluShifter.OP_PASS_A, 0, true,  true,  false, true,  false, 0,       REG_SP,    REG_AC,    10);

        rom[62] = buildInstruction(0, COND_NONE, AluShifter.OP_ADD,    0, false, true,  true,  false, true,  REG_SP,  REG_SP,    REG_P1,    0);
        rom[63] = buildInstruction(0, COND_NONE, AluShifter.OP_PASS_A, 0, false, false, true,  false, false, 0,       0,         0,         0);
        rom[64] = buildInstruction(1, COND_JUMP, AluShifter.OP_PASS_A, 0, false, false, false, false, true,  REG_AC,  0,         0,         0);

        rom[65] = buildInstruction(0, COND_N,    AluShifter.OP_ADD,    0, false, false, false, false, true,  REG_TIR, REG_TIR,   REG_TIR,   72);
        rom[66] = buildInstruction(0, COND_N,    AluShifter.OP_ADD,    0, false, false, false, false, false, 0,       REG_TIR,   REG_TIR,   69);

        rom[67] = buildInstruction(0, COND_NONE, AluShifter.OP_ADD,    0, false, true,  true,  false, true,  REG_SP,  REG_SP,    REG_P1,    0);
        rom[68] = buildInstruction(0, COND_JUMP, AluShifter.OP_PASS_A, 0, false, false, true,  false, false, 0,       0,         0,         78);
        rom[78] = buildInstruction(1, COND_JUMP, AluShifter.OP_PASS_A, 0, false, false, false, false, true,  REG_PC,  0,         0,         0);

        rom[69] = buildInstruction(0, COND_NONE, AluShifter.OP_PASS_A, 0, false, false, false, false, true,  REG_A,   0,         REG_AC,    0);
        rom[70] = buildInstruction(0, COND_NONE, AluShifter.OP_PASS_A, 0, false, false, false, false, true,  REG_AC,  0,         REG_SP,    0);
        rom[71] = buildInstruction(0, COND_JUMP, AluShifter.OP_PASS_A, 0, false, false, false, false, true,  REG_SP,  0,         REG_A,     0);

        rom[72] = buildInstruction(0, COND_N,    AluShifter.OP_ADD,    0, false, false, false, false, false, 0,       REG_TIR,   REG_TIR,   75);

        rom[73] = buildInstruction(0, COND_NONE, AluShifter.OP_AND,    0, false, false, false, false, true,  REG_A,   REG_SMASK, REG_IR,    0);
        rom[74] = buildInstruction(0, COND_JUMP, AluShifter.OP_ADD,    0, false, false, false, false, true,  REG_SP,  REG_SP,    REG_A,     0);

        rom[75] = buildInstruction(0, COND_NONE, AluShifter.OP_AND,    0, false, false, false, false, true,  REG_A,   REG_SMASK, REG_IR,    0);
        rom[76] = buildInstruction(0, COND_JUMP, AluShifter.OP_NOT_A,  0, false, false, false, false, true,  REG_A,   0,         REG_A,     79);
        rom[79] = buildInstruction(0, COND_NONE, AluShifter.OP_ADD,    0, false, false, false, false, true,  REG_A,   REG_P1,    REG_A,     0);
        rom[80] = buildInstruction(0, COND_JUMP, AluShifter.OP_ADD,    0, false, false, false, false, true,  REG_SP,  REG_SP,    REG_A,     0);

        rom[77] = buildInstruction(0, COND_JUMP, AluShifter.OP_AND,    0, false, false, false, false, true,  REG_AC,  REG_AMASK, REG_IR,    0);
    }

    public static String formatMicroinstruction(int mir) {
        StringBuilder sb = new StringBuilder();
        String aSrc = getAmux(mir) == 1 ? "MBR" : REG_NAMES[getA(mir)];
        String bSrc = REG_NAMES[getB(mir)];
        String aluExpr = switch (getAlu(mir)) {
            case AluShifter.OP_ADD    -> aSrc + " + " + bSrc;
            case AluShifter.OP_AND    -> aSrc + " & " + bSrc;
            case AluShifter.OP_PASS_A -> aSrc;
            case AluShifter.OP_NOT_A  -> "~" + aSrc;
            default                   -> "?";
        };
        String shExpr = switch (getSh(mir)) {
            case AluShifter.SHIFT_RSHIFT -> "rshift(" + aluExpr + ")";
            case AluShifter.SHIFT_LSHIFT -> "lshift(" + aluExpr + ")";
            default                      -> aluExpr;
        };

        if (hasMar(mir)) sb.append("MAR:=").append(bSrc).append("; ");
        if (hasEnc(mir)) sb.append(REG_NAMES[getC(mir)]).append(":=").append(shExpr).append("; ");
        if (hasMbr(mir)) sb.append("MBR:=").append(shExpr).append("; ");
        if (hasRd(mir))  sb.append("RD; ");
        if (hasWr(mir))  sb.append("WR; ");

        switch (getCond(mir)) {
            case COND_N    -> sb.append("if N goto ").append(getAddr(mir));
            case COND_Z    -> sb.append("if Z goto ").append(getAddr(mir));
            case COND_JUMP -> sb.append("goto ").append(getAddr(mir));
            default        -> { }
        }
        return sb.toString().trim();
    }

    private void validateAddress(int address) {
        if (address < 0 || address >= CAPACITY) {
            throw new IndexOutOfBoundsException(
                String.format("Endereço MPC fora dos limites da Control Store: %d (capacidade: %d)", address, CAPACITY)
            );
        }
    }

    @Override
    public String toString() {
        return String.format("[ControlStore MAC-1: %d microinstruções de 32 bits]", CAPACITY);
    }
}
