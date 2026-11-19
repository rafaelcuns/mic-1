package br.mic1;

import java.util.Arrays;

public class CpuMic1 {

    public static final int DEFAULT_PC = 0x000;
    public static final int DEFAULT_SP = 0xFFC;

    private final Memory memory;
    private final Cache cache;
    private final ControlStore controlStore;
    private final AluShifter aluShifter;

    private final int[] regs = new int[16];
    private int mar;
    private int mbr;
    private int mpc;
    private int mir;

    private int latchA;
    private int latchB;
    private int busC;
    private boolean flagZ;
    private boolean flagN;

    private int nextSubcycle;
    private int lastSubcycle;
    private boolean rdPending;
    private boolean wrPending;

    private long subcycleCount;
    private long cycleCount;
    private long instructionCount;
    private boolean halted;

    public CpuMic1() {
        this(new Memory());
    }

    public CpuMic1(Memory memory) {
        this(memory,
             new Cache("Cache L1", Cache.DEFAULT_LINES, Cache.DEFAULT_BLOCK_SIZE, memory),
             new ControlStore(),
             new AluShifter());
    }

    public CpuMic1(Memory memory, Cache cache) {
        this(memory, cache, new ControlStore(), new AluShifter());
    }

    public CpuMic1(Memory memory, Cache cache, ControlStore controlStore, AluShifter aluShifter) {
        if (memory == null) throw new IllegalArgumentException("A memória principal não pode ser nula.");
        if (cache == null) throw new IllegalArgumentException("A Cache L1 não pode ser nula.");
        if (controlStore == null) throw new IllegalArgumentException("A ControlStore não pode ser nula.");
        if (aluShifter == null) throw new IllegalArgumentException("O AluShifter não pode ser nulo.");

        this.memory = memory;
        this.cache = cache;
        this.controlStore = controlStore;
        this.aluShifter = aluShifter;

        reset();
    }

    public void reset() {
        reset(DEFAULT_PC);
    }

    public void reset(int startAddress) {
        Arrays.fill(this.regs, 0);
        this.regs[ControlStore.REG_PC]    = startAddress & 0x0FFF;
        this.regs[ControlStore.REG_SP]    = DEFAULT_SP;
        this.regs[ControlStore.REG_ZERO]  = 0x0000;
        this.regs[ControlStore.REG_P1]    = 0x0001;
        this.regs[ControlStore.REG_M1]    = 0xFFFF;
        this.regs[ControlStore.REG_AMASK] = 0x0FFF;
        this.regs[ControlStore.REG_SMASK] = 0x00FF;

        this.mar = 0;
        this.mbr = 0;
        this.mpc = ControlStore.ADDR_FETCH;
        this.mir = this.controlStore.getInstruction(this.mpc);

        this.latchA = 0;
        this.latchB = 0;
        this.busC = 0;
        this.flagZ = false;
        this.flagN = false;

        this.nextSubcycle = 1;
        this.lastSubcycle = 0;
        this.rdPending = false;
        this.wrPending = false;

        this.subcycleCount = 0;
        this.cycleCount = 0;
        this.instructionCount = 0;
        this.halted = false;

        this.aluShifter.reset();
        this.cache.reset();
    }

    public boolean stepSubcycle() {
        if (halted) {
            return false;
        }

        switch (nextSubcycle) {
            case 1 -> executeSubcycle1();
            case 2 -> executeSubcycle2();
            case 3 -> executeSubcycle3();
            case 4 -> executeSubcycle4();
            default -> throw new IllegalStateException("Subciclo inválido: " + nextSubcycle);
        }

        lastSubcycle = nextSubcycle;
        nextSubcycle = (nextSubcycle == 4) ? 1 : nextSubcycle + 1;
        subcycleCount++;

        return true;
    }

    private void executeSubcycle1() {
        mir = controlStore.getInstruction(mpc);
    }

    private void executeSubcycle2() {
        latchA = regs[ControlStore.getA(mir)] & 0xFFFF;
        latchB = regs[ControlStore.getB(mir)] & 0xFFFF;
    }

    private void executeSubcycle3() {
        int amux = ControlStore.getAmux(mir);
        int alu  = ControlStore.getAlu(mir);
        int sh   = ControlStore.getSh(mir);

        busC = aluShifter.compute(latchA, latchB, mbr, amux, alu, sh);
        flagZ = aluShifter.isFlagZ();
        flagN = aluShifter.isFlagN();

        if (ControlStore.hasMar(mir)) {
            mar = latchB & 0x0FFF;
        }
    }

    private void executeSubcycle4() {
        if (ControlStore.hasEnc(mir)) {
            int cReg = ControlStore.getC(mir);
            if (isWritableRegister(cReg)) {
                regs[cReg] = busC & 0xFFFF;
            }
        }

        if (ControlStore.hasMbr(mir)) {
            mbr = busC & 0xFFFF;
        }

        if (ControlStore.hasRd(mir)) {
            if (rdPending) {
                mbr = cache.read(mar & 0x0FFF) & 0xFFFF;
                rdPending = false;
            } else {
                rdPending = true;
            }
        } else {
            rdPending = false;
        }

        if (ControlStore.hasWr(mir)) {
            if (wrPending) {
                cache.write(mar & 0x0FFF, mbr & 0xFFFF);
                wrPending = false;
            } else {
                wrPending = true;
            }
        } else {
            wrPending = false;
        }

        mpc = ControlStore.calculateNextMpc(mpc, mir, flagZ, flagN);
        cycleCount++;

        if (mpc == ControlStore.ADDR_FETCH) {
            instructionCount++;
        }
    }

    private boolean isWritableRegister(int regIndex) {
        return regIndex < ControlStore.REG_ZERO || regIndex > ControlStore.REG_SMASK;
    }

    public boolean step() {
        if (halted) {
            return false;
        }
        do {
            if (!stepSubcycle()) {
                return false;
            }
        } while (nextSubcycle != 1 && !halted);
        return true;
    }

    public int stepInstruction() {
        if (halted) {
            return 0;
        }
        int executedCycles = 0;
        do {
            if (!step()) {
                break;
            }
            executedCycles++;
        } while (mpc != ControlStore.ADDR_FETCH && !halted);
        return executedCycles;
    }

    public long run(long maxCycles) {
        long count = 0;
        while (!halted && count < maxCycles) {
            if (!step()) {
                break;
            }
            count++;
        }
        return count;
    }

    public void loadProgram(int[] words) {
        loadProgram(words, DEFAULT_PC);
    }

    public void loadProgram(int[] words, int startAddress) {
        memory.clear();
        memory.loadWords(startAddress, words);
        reset(startAddress);
    }

    public int[] loadProgram(String assemblySource) {
        memory.clear();
        int[] words = Assembler.assemble(assemblySource, memory, DEFAULT_PC);
        reset(DEFAULT_PC);
        return words;
    }

    public int getRegister(int index) {
        return regs[index] & 0xFFFF;
    }

    public int getRegisterSigned(int index) {
        return (short) regs[index];
    }

    public void setRegister(int index, int value) {
        if (isWritableRegister(index)) {
            regs[index] = value & 0xFFFF;
        }
    }

    public int[] getRegistersCopy() {
        return Arrays.copyOf(regs, regs.length);
    }

    public int getPc()    { return regs[ControlStore.REG_PC] & 0xFFFF; }
    public int getAc()    { return regs[ControlStore.REG_AC] & 0xFFFF; }
    public int getAcSigned() { return (short) regs[ControlStore.REG_AC]; }
    public int getSp()    { return regs[ControlStore.REG_SP] & 0xFFFF; }
    public int getIr()    { return regs[ControlStore.REG_IR] & 0xFFFF; }
    public int getTir()   { return regs[ControlStore.REG_TIR] & 0xFFFF; }
    public int getRegA()  { return regs[ControlStore.REG_A] & 0xFFFF; }
    public int getRegB()  { return regs[ControlStore.REG_B] & 0xFFFF; }
    public int getRegC()  { return regs[ControlStore.REG_C] & 0xFFFF; }
    public int getRegD()  { return regs[ControlStore.REG_D] & 0xFFFF; }
    public int getRegE()  { return regs[ControlStore.REG_E] & 0xFFFF; }
    public int getRegF()  { return regs[ControlStore.REG_F] & 0xFFFF; }

    public int getMar() { return mar & 0x0FFF; }
    public int getMbr() { return mbr & 0xFFFF; }
    public int getMpc() { return mpc & 0xFF; }
    public int getMir() { return mir; }

    public int getLatchA() { return latchA & 0xFFFF; }
    public int getLatchB() { return latchB & 0xFFFF; }
    public int getBusC()   { return busC & 0xFFFF; }

    public int getAmuxOutput()    { return aluShifter.getAmuxOutput(); }
    public int getAluOutput()     { return aluShifter.getAluOutput(); }
    public int getShifterOutput() { return aluShifter.getShifterOutput(); }

    public boolean isFlagZ() { return flagZ; }
    public boolean isFlagN() { return flagN; }

    public int getNextSubcycle() { return nextSubcycle; }
    public int getLastSubcycle() { return lastSubcycle; }
    public boolean isRdPending() { return rdPending; }
    public boolean isWrPending() { return wrPending; }

    public long getSubcycleCount()    { return subcycleCount; }
    public long getCycleCount()       { return cycleCount; }
    public long getInstructionCount() { return instructionCount; }

    public Memory getMemory()             { return memory; }
    public Cache getCache()               { return cache; }
    public ControlStore getControlStore() { return controlStore; }
    public AluShifter getAluShifter()     { return aluShifter; }

    public boolean isHalted() { return halted; }
    public void setHalted(boolean halted) { this.halted = halted; }
    public void halt() { this.halted = true; }

    @Override
    public String toString() {
        return String.format(
                "[MIC-1 3ª Ed | Ciclo: %d (Sub: %d) | MPC: %03d | PC: 0x%03X | AC: 0x%04X (%d) | SP: 0x%03X | IR: 0x%04X | Z:%d N:%d]",
                cycleCount, lastSubcycle, mpc, getPc(), getAc(), getAcSigned(), getSp(), getIr(), flagZ ? 1 : 0, flagN ? 1 : 0
        );
    }
}
