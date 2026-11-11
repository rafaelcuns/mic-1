package br.mic1;

import java.util.*;

public class Assembler {

    public static final int OP_NOP       = 0x00;
    public static final int OP_BIPUSH    = 0x10;
    public static final int OP_ILOAD     = 0x15;
    public static final int OP_ISTORE    = 0x36;
    public static final int OP_POP       = 0x57;
    public static final int OP_DUP       = 0x59;
    public static final int OP_SWAP      = 0x5F;
    public static final int OP_IADD      = 0x60;
    public static final int OP_ISUB      = 0x64;
    public static final int OP_IAND      = 0x7E;
    public static final int OP_IOR       = 0x80;
    public static final int OP_IINC      = 0x84;
    public static final int OP_IFEQ      = 0x99;
    public static final int OP_IFLT      = 0x9B;
    public static final int OP_IF_ICMPEQ = 0x9F;
    public static final int OP_GOTO      = 0xA7;

    private static class ParsedInstruction {
        int address;
        String mnemonic;
        List<String> operands = new ArrayList<>();
        int lineNumber;
    }

    public static byte[] assemble(String sourceCode) {
        return assemble(sourceCode, null, 0);
    }

    public static byte[] assemble(String sourceCode, Memory memory) {
        return assemble(sourceCode, memory, 0);
    }

    public static byte[] assemble(String sourceCode, Memory memory, int startByteAddress) {
        if (sourceCode == null) {
            throw new IllegalArgumentException("O código fonte não pode ser nulo.");
        }

        String[] rawLines = sourceCode.split("\\r?\\n");
        Map<String, Integer> labels = new HashMap<>();
        Map<String, Integer> variables = new HashMap<>();
        List<ParsedInstruction> instructions = new ArrayList<>();

        boolean inVarBlock = false;
        int nextVarIndex = 1;
        int currentAddress = startByteAddress;

        for (int lineNum = 0; lineNum < rawLines.length; lineNum++) {
            String line = stripComments(rawLines[lineNum]).trim();
            if (line.isEmpty()) {
                continue;
            }

            String upperLine = line.toUpperCase();
            if (upperLine.equals(".VAR")) {
                inVarBlock = true;
                continue;
            }
            if (upperLine.equals(".END-VAR") || upperLine.equals(".ENDVAR")) {
                inVarBlock = false;
                continue;
            }

            if (inVarBlock) {
                String varName = line.split("\\s+")[0].toLowerCase();
                if (!variables.containsKey(varName)) {
                    variables.put(varName, nextVarIndex++);
                }
                continue;
            }

            while (line.contains(":")) {
                int colonIndex = line.indexOf(':');
                String labelName = line.substring(0, colonIndex).trim().toLowerCase();
                if (labelName.isEmpty()) {
                    throw new IllegalArgumentException("Rótulo inválido na linha " + (lineNum + 1));
                }
                if (labels.containsKey(labelName)) {
                    throw new IllegalArgumentException("Rótulo duplicado '" + labelName + "' na linha " + (lineNum + 1));
                }
                labels.put(labelName, currentAddress);
                line = line.substring(colonIndex + 1).trim();
            }

            if (line.isEmpty()) {
                continue;
            }

            String[] tokens = line.split("\\s+");
            ParsedInstruction pi = new ParsedInstruction();
            pi.address = currentAddress;
            pi.mnemonic = tokens[0].toUpperCase();
            pi.lineNumber = lineNum + 1;

            for (int i = 1; i < tokens.length; i++) {
                if (!tokens[i].trim().isEmpty()) {
                    pi.operands.add(tokens[i].trim());
                }
            }

            int size = getInstructionSize(pi.mnemonic, pi.lineNumber);
            currentAddress += size;
            instructions.add(pi);
        }

        int totalBytes = currentAddress - startByteAddress;
        byte[] bytecode = new byte[totalBytes];
        int writePos = 0;

        for (ParsedInstruction pi : instructions) {
            byte opcode = getOpcode(pi.mnemonic, pi.lineNumber);
            bytecode[writePos++] = opcode;

            switch (pi.mnemonic) {
                case "NOP":
                case "DUP":
                case "POP":
                case "SWAP":
                case "IADD":
                case "ISUB":
                case "IAND":
                case "IOR":
                    break;

                case "BIPUSH":
                    validateOperandCount(pi, 1);
                    byte pushVal = (byte) parseInteger(pi.operands.get(0), pi.lineNumber);
                    bytecode[writePos++] = pushVal;
                    break;

                case "ILOAD":
                case "ISTORE":
                    validateOperandCount(pi, 1);
                    int varIndex = resolveVariableOrLiteral(pi.operands.get(0), variables, pi.lineNumber);
                    bytecode[writePos++] = (byte) (varIndex & 0xFF);
                    break;

                case "IINC":
                    validateOperandCount(pi, 2);
                    int iincVar = resolveVariableOrLiteral(pi.operands.get(0), variables, pi.lineNumber);
                    int iincConst = parseInteger(pi.operands.get(1), pi.lineNumber);
                    bytecode[writePos++] = (byte) (iincVar & 0xFF);
                    bytecode[writePos++] = (byte) (iincConst & 0xFF);
                    break;

                case "GOTO":
                case "IFEQ":
                case "IFLT":
                case "IF_ICMPEQ":
                    validateOperandCount(pi, 1);
                    String target = pi.operands.get(0);
                    int targetAddress;
                    if (labels.containsKey(target.toLowerCase())) {
                        targetAddress = labels.get(target.toLowerCase());
                    } else {
                        targetAddress = pi.address + parseInteger(target, pi.lineNumber);
                    }
                    int branchOffset = targetAddress - pi.address;
                    bytecode[writePos++] = (byte) ((branchOffset >> 8) & 0xFF);
                    bytecode[writePos++] = (byte) (branchOffset & 0xFF);
                    break;

                default:
                    throw new IllegalArgumentException("Mnemônico desconhecido: " + pi.mnemonic + " na linha " + pi.lineNumber);
            }
        }

        if (memory != null) {
            for (int i = 0; i < bytecode.length; i++) {
                memory.writeByte(startByteAddress + i, bytecode[i] & 0xFF);
            }
        }

        return bytecode;
    }

    private static String stripComments(String line) {
        int idx = -1;
        int idxSlash = line.indexOf("//");
        int idxSemi = line.indexOf(';');
        int idxHash = line.indexOf('#');

        if (idxSlash != -1) idx = idxSlash;
        if (idxSemi != -1 && (idx == -1 || idxSemi < idx)) idx = idxSemi;
        if (idxHash != -1 && (idx == -1 || idxHash < idx)) idx = idxHash;

        if (idx != -1) {
            return line.substring(0, idx);
        }
        return line;
    }

    private static int getInstructionSize(String mnemonic, int lineNumber) {
        return switch (mnemonic) {
            case "NOP", "DUP", "POP", "SWAP", "IADD", "ISUB", "IAND", "IOR" -> 1;
            case "BIPUSH", "ILOAD", "ISTORE" -> 2;
            case "GOTO", "IFEQ", "IFLT", "IF_ICMPEQ", "IINC" -> 3;
            default -> throw new IllegalArgumentException("Mnemônico desconhecido '" + mnemonic + "' na linha " + lineNumber);
        };
    }

    private static byte getOpcode(String mnemonic, int lineNumber) {
        return switch (mnemonic) {
            case "NOP"       -> (byte) OP_NOP;
            case "BIPUSH"    -> (byte) OP_BIPUSH;
            case "ILOAD"     -> (byte) OP_ILOAD;
            case "ISTORE"    -> (byte) OP_ISTORE;
            case "POP"       -> (byte) OP_POP;
            case "DUP"       -> (byte) OP_DUP;
            case "SWAP"      -> (byte) OP_SWAP;
            case "IADD"      -> (byte) OP_IADD;
            case "ISUB"      -> (byte) OP_ISUB;
            case "IAND"      -> (byte) OP_IAND;
            case "IOR"       -> (byte) OP_IOR;
            case "IINC"      -> (byte) OP_IINC;
            case "IFEQ"      -> (byte) OP_IFEQ;
            case "IFLT"      -> (byte) OP_IFLT;
            case "IF_ICMPEQ" -> (byte) OP_IF_ICMPEQ;
            case "GOTO"      -> (byte) OP_GOTO;
            default -> throw new IllegalArgumentException("Mnemônico inválido '" + mnemonic + "' na linha " + lineNumber);
        };
    }

    private static void validateOperandCount(ParsedInstruction pi, int expected) {
        if (pi.operands.size() != expected) {
            throw new IllegalArgumentException(String.format(
                    "Instrução %s requer %d operando(s), mas recebeu %d na linha %d",
                    pi.mnemonic, expected, pi.operands.size(), pi.lineNumber
            ));
        }
    }

    private static int resolveVariableOrLiteral(String operand, Map<String, Integer> variables, int lineNumber) {
        String key = operand.toLowerCase();
        if (variables.containsKey(key)) {
            return variables.get(key);
        }
        return parseInteger(operand, lineNumber);
    }

    private static int parseInteger(String token, int lineNumber) {
        try {
            token = token.trim();
            if (token.startsWith("0x") || token.startsWith("0X")) {
                return (int) Long.parseLong(token.substring(2), 16);
            }
            if (token.startsWith("0b") || token.startsWith("0B")) {
                return Integer.parseInt(token.substring(2), 2);
            }
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor numérico inválido '" + token + "' na linha " + lineNumber);
        }
    }
}
