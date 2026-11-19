package br.mic1;

public class CpuMic1 {

    public static final int DEFAULT_PC  = 0x0000;
    public static final int DEFAULT_LV  = 0x0100;
    public static final int DEFAULT_SP  = 0x0110;
    public static final int DEFAULT_CPP = 0x0080;

    private final Memory memory;
    private final Cache iCache;
    private final Cache dCache;
    private final ControlStore controlStore;
    private final AluShifter aluShifter;

    private int pc;
    private int mbr;
    private int sp;
    private int lv;
    private int cpp;
    private int tos;
    private int opc;
    private int h;
    private int mar;
    private int mdr;

    private int mpc;
    private long mir;

    private int busA;
    private int busB;
    private int busC;
    private boolean flagZ;
    private boolean flagN;

    private Integer mbrHolding;

    private long cycleCount;
    private boolean halted;

    public CpuMic1() {
        this(new Memory());
    }

    public CpuMic1(Memory memory) {
        this(memory,
             new Cache("I-Cache (Instruções)", 16, memory),
             new Cache("D-Cache (Dados)", 16, memory),
             new ControlStore(),
             new AluShifter());
    }

    public CpuMic1(Memory memory, Cache iCache, Cache dCache) {
        this(memory, iCache, dCache, new ControlStore(), new AluShifter());
    }

    public CpuMic1(Memory memory, Cache iCache, Cache dCache, ControlStore controlStore, AluShifter aluShifter) {
        if (memory == null) throw new IllegalArgumentException("A memória principal não pode ser nula.");
        if (iCache == null) throw new IllegalArgumentException("A I-Cache não pode ser nula.");
        if (dCache == null) throw new IllegalArgumentException("A D-Cache não pode ser nula.");
        if (controlStore == null) throw new IllegalArgumentException("A ControlStore não pode ser nula.");
        if (aluShifter == null) throw new IllegalArgumentException("O AluShifter não pode ser nulo.");

        this.memory = memory;
        this.iCache = iCache;
        this.dCache = dCache;
        this.controlStore = controlStore;
        this.aluShifter = aluShifter;

        reset();
    }

    public void reset() {
        reset(DEFAULT_PC);
    }

    public void reset(int startByteAddress) {
        this.pc = startByteAddress;
        this.sp = DEFAULT_SP;
        this.lv = DEFAULT_LV;
        this.cpp = DEFAULT_CPP;
        this.tos = 0;
        this.opc = 0;
        this.h = 0;
        this.mar = 0;
        this.mdr = 0;

        this.busA = 0;
        this.busB = 0;
        this.busC = 0;
        this.flagZ = false;
        this.flagN = false;

        this.cycleCount = 0;
        this.halted = false;
        this.mbrHolding = null;

        this.aluShifter.reset();
        this.iCache.reset();
        this.dCache.reset();

        this.mbr = this.iCache.readByte(this.pc);
        this.mpc = ControlStore.ADDR_MAIN1;
        this.mir = this.controlStore.getInstruction(this.mpc);
    }

    public boolean step() {
        if (halted) {
            return false;
        }

        mir = controlStore.getInstruction(mpc);
        busA = h;

        int bBusSel = ControlStore.getBBus(mir);
        busB = switch (bBusSel) {
            case ControlStore.B_MDR  -> mdr;
            case ControlStore.B_PC   -> pc;
            case ControlStore.B_MBR  -> (byte) (mbr & 0xFF);
            case ControlStore.B_MBRU -> (mbr & 0xFF);
            case ControlStore.B_SP   -> sp;
            case ControlStore.B_LV   -> lv;
            case ControlStore.B_CPP  -> cpp;
            case ControlStore.B_TOS  -> tos;
            case ControlStore.B_OPC  -> opc;
            case ControlStore.B_NONE -> 0;
            default                  -> 0;
        };

        if ((bBusSel == ControlStore.B_MBR || bBusSel == ControlStore.B_MBRU) && mbrHolding != null) {
            mbr = mbrHolding;
            mbrHolding = null;
        }

        int aluCtrl = ControlStore.getAlu(mir);
        int shiftCtrl = ControlStore.getShift(mir);
        busC = aluShifter.compute(busA, busB, aluCtrl, shiftCtrl);
        flagZ = aluShifter.isFlagZ();
        flagN = aluShifter.isFlagN();

        if (ControlStore.hasWriteH(mir))   h   = busC;
        if (ControlStore.hasWriteOpc(mir)) opc = busC;
        if (ControlStore.hasWriteTos(mir)) tos = busC;
        if (ControlStore.hasWriteCpp(mir)) cpp = busC;
        if (ControlStore.hasWriteLv(mir))  lv  = busC;
        if (ControlStore.hasWriteSp(mir))  sp  = busC;
        if (ControlStore.hasWritePc(mir))  pc  = busC;
        if (ControlStore.hasWriteMdr(mir)) mdr = busC;
        if (ControlStore.hasWriteMar(mir)) mar = busC;

        if (ControlStore.isJmpC(mir) && mbrHolding != null) {
            mbr = mbrHolding;
            mbrHolding = null;
        }
        int nextMpc = ControlStore.calculateNextMpc(mir, flagZ, flagN, mbr);

        if (ControlStore.hasMemWrite(mir)) {
            dCache.write(mar, mdr);
        }

        if (ControlStore.hasMemRead(mir)) {
            mdr = dCache.read(mar);
        }

        if (ControlStore.hasMemFetch(mir)) {
            int fetched = iCache.readByte(pc);
            long nextMir = controlStore.getInstruction(nextMpc);
            int nextBBus = ControlStore.getBBus(nextMir);

            if (nextBBus == ControlStore.B_MBR || nextBBus == ControlStore.B_MBRU) {
                mbrHolding = fetched;
            } else {
                mbr = fetched;
                mbrHolding = null;
            }
        }

        mpc = nextMpc;
        cycleCount++;

        return true;
    }

    public int stepInstruction() {
        if (halted) return 0;

        int executed = 0;
        do {
            if (!step()) break;
            executed++;
        } while (mpc != ControlStore.ADDR_MAIN1 && !halted);

        return executed;
    }

    public long run(long maxCycles) {
        long count = 0;
        while (!halted && count < maxCycles) {
            if (!step()) break;
            count++;
        }
        return count;
    }

    public void loadProgram(byte[] bytecode) {
        loadProgram(bytecode, 0);
    }

    public void loadProgram(byte[] bytecode, int startAddress) {
        for (int i = 0; i < bytecode.length; i++) {
            memory.writeByte(startAddress + i, bytecode[i] & 0xFF);
        }
        reset(startAddress);
    }

    public void loadProgram(String assemblySource) {
        Assembler.assemble(assemblySource, memory, 0);
        reset(0);
    }

    public int getPc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public int getMbr() {
        return mbr & 0xFF;
    }

    public int getMbrSigned() {
        return (byte) (mbr & 0xFF);
    }

    public void setMbr(int mbr) {
        this.mbr = mbr & 0xFF;
    }

    public int getSp() {
        return sp;
    }

    public void setSp(int sp) {
        this.sp = sp;
    }

    public int getLv() {
        return lv;
    }

    public void setLv(int lv) {
        this.lv = lv;
    }

    public int getCpp() {
        return cpp;
    }

    public void setCpp(int cpp) {
        this.cpp = cpp;
    }

    public int getTos() {
        return tos;
    }

    public void setTos(int tos) {
        this.tos = tos;
    }

    public int getOpc() {
        return opc;
    }

    public void setOpc(int opc) {
        this.opc = opc;
    }

    public int getH() {
        return h;
    }

    public void setH(int h) {
        this.h = h;
    }

    public int getMar() {
        return mar;
    }

    public void setMar(int mar) {
        this.mar = mar;
    }

    public int getMdr() {
        return mdr;
    }

    public void setMdr(int mdr) {
        this.mdr = mdr;
    }

    public int getMpc() {
        return mpc;
    }

    public void setMpc(int mpc) {
        this.mpc = mpc;
        this.mir = controlStore.getInstruction(mpc);
    }

    public long getMir() {
        return mir;
    }

    public int getBusA() {
        return busA;
    }

    public int getBusB() {
        return busB;
    }

    public int getBusC() {
        return busC;
    }

    public boolean isFlagZ() {
        return flagZ;
    }

    public boolean isFlagN() {
        return flagN;
    }

    public int getAluOutput() {
        return aluShifter.getAluOutput();
    }

    public int getShifterOutput() {
        return aluShifter.getShifterOutput();
    }

    public Memory getMemory() {
        return memory;
    }

    public Cache getICache() {
        return iCache;
    }

    public Cache getDCache() {
        return dCache;
    }

    public ControlStore getControlStore() {
        return controlStore;
    }

    public AluShifter getAluShifter() {
        return aluShifter;
    }

    public long getCycleCount() {
        return cycleCount;
    }

    public boolean isHalted() {
        return halted;
    }

    public void setHalted(boolean halted) {
        this.halted = halted;
    }

    public void halt() {
        this.halted = true;
    }

    @Override
    public String toString() {
        return String.format("[CPU MIC-1 | Ciclo: %d | MPC: 0x%03X | PC: 0x%04X | MBR: 0x%02X (%d) | SP: 0x%04X | TOS: 0x%08X (%d) | Z: %d | N: %d]",
                cycleCount, mpc, pc, mbr & 0xFF, (byte) mbr, sp, tos, tos, flagZ ? 1 : 0, flagN ? 1 : 0);
    }
}
