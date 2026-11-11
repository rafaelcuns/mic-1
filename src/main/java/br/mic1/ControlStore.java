package br.mic1;

import java.util.Arrays;

public class ControlStore {

    public static final int CAPACITY = 512;

    public static final int SHIFT_NEXT_ADDRESS = 27;
    public static final long MASK_NEXT_ADDRESS = 0x1FFL;

    public static final long BIT_JAMZ = 1L << 26;
    public static final long BIT_JAMN = 1L << 25;
    public static final long BIT_JMPC = 1L << 24;

    public static final long BIT_SLL8 = 1L << 23;
    public static final long BIT_SRA1 = 1L << 22;

    public static final int SHIFT_ALU = 16;
    public static final long MASK_ALU = 0x3FL;

    public static final int SHIFT_C_BUS = 7;
    public static final long MASK_C_BUS = 0x1FFL;
    public static final long C_H   = 1L << 15;
    public static final long C_OPC = 1L << 14;
    public static final long C_TOS = 1L << 13;
    public static final long C_CPP = 1L << 12;
    public static final long C_LV  = 1L << 11;
    public static final long C_SP  = 1L << 10;
    public static final long C_PC  = 1L << 9;
    public static final long C_MDR = 1L << 8;
    public static final long C_MAR = 1L << 7;

    public static final int SHIFT_MEM = 4;
    public static final long MASK_MEM = 0x07L;
    public static final long MEM_WRITE = 1L << 6;
    public static final long MEM_READ  = 1L << 5;
    public static final long MEM_FETCH = 1L << 4;

    public static final long MASK_B_BUS = 0x0FL;
    public static final int B_MDR  = 0;
    public static final int B_PC   = 1;
    public static final int B_MBR  = 2;
    public static final int B_MBRU = 3;
    public static final int B_SP   = 4;
    public static final int B_LV   = 5;
    public static final int B_CPP  = 6;
    public static final int B_TOS  = 7;
    public static final int B_OPC  = 8;
    public static final int B_NONE = 9;

    public static final int ADDR_MAIN1 = 0x002;

    private final long[] rom;

    public ControlStore() {
        this.rom = new long[CAPACITY];
        loadDefaultMicroprogram();
    }

    public long getInstruction(int mpc) {
        validateAddress(mpc);
        return rom[mpc];
    }

    public void setInstruction(int mpc, long instruction) {
        validateAddress(mpc);
        rom[mpc] = instruction & 0xFFFFFFFFFL;
    }

    public void clear() {
        Arrays.fill(rom, 0L);
    }

    public static int getNextAddress(long mir) {
        return (int) ((mir >>> SHIFT_NEXT_ADDRESS) & MASK_NEXT_ADDRESS);
    }

    public static boolean isJamZ(long mir) {
        return (mir & BIT_JAMZ) != 0;
    }

    public static boolean isJamN(long mir) {
        return (mir & BIT_JAMN) != 0;
    }

    public static boolean isJmpC(long mir) {
        return (mir & BIT_JMPC) != 0;
    }

    public static boolean isSll8(long mir) {
        return (mir & BIT_SLL8) != 0;
    }

    public static boolean isSra1(long mir) {
        return (mir & BIT_SRA1) != 0;
    }

    public static int getShift(long mir) {
        int shift = AluShifter.SHIFT_NONE;
        if (isSll8(mir)) shift |= AluShifter.SHIFT_SLL8;
        if (isSra1(mir)) shift |= AluShifter.SHIFT_SRA1;
        return shift;
    }

    public static int getAlu(long mir) {
        return (int) ((mir >>> SHIFT_ALU) & MASK_ALU);
    }

    public static int getCBus(long mir) {
        return (int) ((mir >>> SHIFT_C_BUS) & MASK_C_BUS);
    }

    public static boolean hasWriteH(long mir)   { return (mir & C_H) != 0; }
    public static boolean hasWriteOpc(long mir) { return (mir & C_OPC) != 0; }
    public static boolean hasWriteTos(long mir) { return (mir & C_TOS) != 0; }
    public static boolean hasWriteCpp(long mir) { return (mir & C_CPP) != 0; }
    public static boolean hasWriteLv(long mir)  { return (mir & C_LV) != 0; }
    public static boolean hasWriteSp(long mir)  { return (mir & C_SP) != 0; }
    public static boolean hasWritePc(long mir)  { return (mir & C_PC) != 0; }
    public static boolean hasWriteMdr(long mir) { return (mir & C_MDR) != 0; }
    public static boolean hasWriteMar(long mir) { return (mir & C_MAR) != 0; }

    public static int getMem(long mir) {
        return (int) ((mir >>> SHIFT_MEM) & MASK_MEM);
    }

    public static boolean hasMemWrite(long mir) { return (mir & MEM_WRITE) != 0; }
    public static boolean hasMemRead(long mir)  { return (mir & MEM_READ) != 0; }
    public static boolean hasMemFetch(long mir) { return (mir & MEM_FETCH) != 0; }

    public static int getBBus(long mir) {
        return (int) (mir & MASK_B_BUS);
    }

    public static int calculateNextMpc(long mir, boolean flagZ, boolean flagN, int mbr) {
        int nextAddr = getNextAddress(mir);
        int highBit = nextAddr & 0x100;

        if ((isJamZ(mir) && flagZ) || (isJamN(mir) && flagN)) {
            highBit = 0x100;
        }

        int low8Bits = nextAddr & 0x0FF;
        if (isJmpC(mir)) {
            low8Bits |= (mbr & 0xFF);
        }

        return highBit | low8Bits;
    }

    public static long buildInstruction(int nextAddress, boolean jamz, boolean jamn, boolean jmpc,
                                        int shiftCtrl, int aluCtrl, long cBusMask, long memMask, int bBus) {
        long mir = ((long) (nextAddress & 0x1FF)) << SHIFT_NEXT_ADDRESS;

        if (jamz) mir |= BIT_JAMZ;
        if (jamn) mir |= BIT_JAMN;
        if (jmpc) mir |= BIT_JMPC;

        if ((shiftCtrl & AluShifter.SHIFT_SLL8) != 0) mir |= BIT_SLL8;
        if ((shiftCtrl & AluShifter.SHIFT_SRA1) != 0) mir |= BIT_SRA1;

        mir |= ((long) (aluCtrl & 0x3F)) << SHIFT_ALU;
        mir |= (cBusMask & (MASK_C_BUS << SHIFT_C_BUS));
        mir |= (memMask & (MASK_MEM << SHIFT_MEM));
        mir |= ((long) (bBus & 0x0F));

        return mir;
    }

    public void loadDefaultMicroprogram() {
        clear();

        rom[ADDR_MAIN1] = buildInstruction(0x000, false, false, true,
                AluShifter.SHIFT_NONE, AluShifter.OP_INC_B, C_PC, MEM_FETCH, B_PC);

        rom[0x000] = buildInstruction(ADDR_MAIN1, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, 0, 0, B_NONE);

        rom[0x060] = buildInstruction(0x061, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_DEC_B, C_MAR | C_SP, MEM_READ, B_SP);
        rom[0x061] = buildInstruction(0x062, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, C_H, 0, B_TOS);
        rom[0x062] = buildInstruction(ADDR_MAIN1, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_ADD, C_MDR | C_TOS, MEM_WRITE, B_MDR);

        rom[0x064] = buildInstruction(0x065, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_DEC_B, C_MAR | C_SP, MEM_READ, B_SP);
        rom[0x065] = buildInstruction(0x066, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, C_H, 0, B_TOS);
        rom[0x066] = buildInstruction(ADDR_MAIN1, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_SUB, C_MDR | C_TOS, MEM_WRITE, B_MDR);

        rom[0x07E] = buildInstruction(0x07F, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_DEC_B, C_MAR | C_SP, MEM_READ, B_SP);
        rom[0x07F] = buildInstruction(0x120, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, C_H, 0, B_TOS);
        rom[0x120] = buildInstruction(ADDR_MAIN1, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_AND, C_MDR | C_TOS, MEM_WRITE, B_MDR);

        rom[0x080] = buildInstruction(0x081, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_DEC_B, C_MAR | C_SP, MEM_READ, B_SP);
        rom[0x081] = buildInstruction(0x121, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, C_H, 0, B_TOS);
        rom[0x121] = buildInstruction(ADDR_MAIN1, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_OR, C_MDR | C_TOS, MEM_WRITE, B_MDR);

        rom[0x059] = buildInstruction(0x05A, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_INC_B, C_MAR | C_SP, 0, B_SP);
        rom[0x05A] = buildInstruction(ADDR_MAIN1, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, C_MDR, MEM_WRITE, B_TOS);

        rom[0x010] = buildInstruction(0x011, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_INC_B, C_MAR | C_SP, 0, B_SP);
        rom[0x011] = buildInstruction(0x012, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_INC_B, C_PC, MEM_FETCH, B_PC);
        rom[0x012] = buildInstruction(ADDR_MAIN1, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, C_MDR | C_TOS, MEM_WRITE, B_MBR);

        rom[0x015] = buildInstruction(0x016, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, C_H, 0, B_LV);
        rom[0x016] = buildInstruction(0x017, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_ADD, C_MAR, MEM_READ, B_MBRU);
        rom[0x017] = buildInstruction(0x018, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_INC_B, C_MAR | C_SP, 0, B_SP);
        rom[0x018] = buildInstruction(0x019, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_INC_B, C_PC, MEM_FETCH | MEM_WRITE, B_PC);
        rom[0x019] = buildInstruction(ADDR_MAIN1, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, C_TOS, 0, B_MDR);

        rom[0x036] = buildInstruction(0x037, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, C_H, 0, B_LV);
        rom[0x037] = buildInstruction(0x038, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_ADD, C_MAR, 0, B_MBRU);
        rom[0x038] = buildInstruction(0x039, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, C_MDR, MEM_WRITE, B_TOS);
        rom[0x039] = buildInstruction(0x03A, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_DEC_B, C_MAR | C_SP, MEM_READ, B_SP);
        rom[0x03A] = buildInstruction(0x03B, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_INC_B, C_PC, MEM_FETCH, B_PC);
        rom[0x03B] = buildInstruction(ADDR_MAIN1, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, C_TOS, 0, B_MDR);

        rom[0x0A7] = buildInstruction(0x0A8, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_DEC_B, C_OPC, 0, B_PC);
        rom[0x0A8] = buildInstruction(0x0A9, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_INC_B, C_PC, MEM_FETCH, B_PC);
        rom[0x0A9] = buildInstruction(0x0AA, false, false, false,
                AluShifter.SHIFT_SLL8, AluShifter.OP_PASS_B, C_H, 0, B_MBR);
        rom[0x0AA] = buildInstruction(0x0AB, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_OR, C_H, 0, B_MBRU);
        rom[0x0AB] = buildInstruction(0x0AC, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_ADD, C_PC, MEM_FETCH, B_OPC);
        rom[0x0AC] = buildInstruction(ADDR_MAIN1, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, 0, 0, B_NONE);

        rom[0x099] = buildInstruction(0x09A, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_DEC_B, C_MAR | C_SP, MEM_READ, B_SP);
        rom[0x09A] = buildInstruction(0x09B, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, C_OPC, 0, B_TOS);
        rom[0x09B] = buildInstruction(0x09C, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, C_TOS, 0, B_MDR);
        rom[0x09C] = buildInstruction(0x032, true, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, 0, 0, B_OPC);

        rom[0x032] = buildInstruction(0x033, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_INC_B, C_PC, 0, B_PC);
        rom[0x033] = buildInstruction(ADDR_MAIN1, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_INC_B, C_PC, MEM_FETCH, B_PC);

        rom[0x132] = buildInstruction(0x0A8, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_DEC_B, C_OPC, 0, B_PC);

        rom[0x09D] = buildInstruction(0x09E, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_DEC_B, C_MAR | C_SP, MEM_READ, B_SP);
        rom[0x09E] = buildInstruction(0x09F, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, C_OPC, 0, B_TOS);
        rom[0x09F] = buildInstruction(0x133, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, C_TOS, 0, B_MDR);
        rom[0x133] = buildInstruction(0x032, false, true, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, 0, 0, B_OPC);

        rom[0x0A0] = buildInstruction(0x0A1, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_DEC_B, C_MAR | C_SP, MEM_READ, B_SP);
        rom[0x0A1] = buildInstruction(0x0A2, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, C_H, 0, B_TOS);
        rom[0x0A2] = buildInstruction(0x0A3, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_DEC_B, C_MAR | C_SP, MEM_READ, B_SP);
        rom[0x0A3] = buildInstruction(0x0A4, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, C_OPC, 0, B_TOS);
        rom[0x0A4] = buildInstruction(0x0A5, false, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_PASS_B, C_TOS, 0, B_MDR);
        rom[0x0A5] = buildInstruction(0x032, true, false, false,
                AluShifter.SHIFT_NONE, AluShifter.OP_SUB, 0, 0, B_OPC);
    }

    private void validateAddress(int address) {
        if (address < 0 || address >= CAPACITY) {
            throw new IndexOutOfBoundsException(
                String.format("Endereço MPC fora dos limites da Control Store: 0x%03X (capacidade: %d)", address, CAPACITY)
            );
        }
    }

    @Override
    public String toString() {
        return String.format("[ControlStore: %d microinstruções carregadas]", CAPACITY);
    }
}
