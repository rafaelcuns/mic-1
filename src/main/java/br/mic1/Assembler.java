package br.mic1;

import java.util.*;

public class Assembler {

    public static final int OP_LODD = 0x0000;
    public static final int OP_STOD = 0x1000;
    public static final int OP_ADDD = 0x2000;
    public static final int OP_SUBD = 0x3000;
    public static final int OP_JPOS = 0x4000;
    public static final int OP_JZER = 0x5000;
    public static final int OP_JUMP = 0x6000;
    public static final int OP_LOCO = 0x7000;
    public static final int OP_LODL = 0x8000;
    public static final int OP_STOL = 0x9000;
    public static final int OP_ADDL = 0xA000;
    public static final int OP_SUBL = 0xB000;
    public static final int OP_JNEG = 0xC000;
    public static final int OP_JNZE = 0xD000;
    public static final int OP_CALL = 0xE000;
    public static final int OP_PSHI = 0xF000;
    public static final int OP_POPI = 0xF200;
    public static final int OP_PUSH = 0xF400;
    public static final int OP_POP  = 0xF600;
    public static final int OP_RETN = 0xF800;
    public static final int OP_SWAP = 0xFA00;
    public static final int OP_INSP = 0xFC00;
    public static final int OP_DESP = 0xFE00;

    private static class ParsedInstruction {
        int address;
        String mnemonic;
        String operand;
        int lineNumber;
    }

    public static int[] assemble(String sourceCode) {
        return assemble(sourceCode, null, 0);
    }

    public static int[] assemble(String sourceCode, Memory memory) {
        return assemble(sourceCode, memory, 0);
    }

    public static int[] assemble(String sourceCode, Memory memory, int startAddress) {
        if (sourceCode == null) {
            throw new IllegalArgumentException("O código fonte não pode ser nulo.");
        }

        String[] rawLines = sourceCode.split("\\r?\\n");
        Map<String, Integer> labels = new HashMap<>();
        List<String> declaredVars = new ArrayList<>();
        List<ParsedInstruction> instructions = new ArrayList<>();

        boolean inVarBlock = false;
        int currentAddress = startAddress;

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
                if (!declaredVars.contains(varName)) {
                    declaredVars.add(varName);
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

            if (tokens.length > 2) {
                throw new IllegalArgumentException(String.format(
                        "Número excessivo de operandos para %s na linha %d", pi.mnemonic, pi.lineNumber
                ));
            }
            if (tokens.length == 2) {
                pi.operand = tokens[1].trim();
            }

            validateMnemonic(pi.mnemonic, pi.lineNumber);
            currentAddress++;
            instructions.add(pi);
        }

        Map<String, Integer> variables = new LinkedHashMap<>();
        int nextVarAddress = currentAddress;
        for (String varName : declaredVars) {
            variables.put(varName, nextVarAddress++);
        }

        int[] words = new int[instructions.size()];
        for (int i = 0; i < instructions.size(); i++) {
            ParsedInstruction pi = instructions.get(i);
            words[i] = encodeInstruction(pi, labels, variables, nextVarAddress);
            nextVarAddress = currentAddress + variables.size();
        }

        if (memory != null) {
            memory.loadWords(startAddress, words);
        }

        return words;
    }

    private static int encodeInstruction(ParsedInstruction pi, Map<String, Integer> labels,
                                         Map<String, Integer> variables, int nextVarAddress) {
        return switch (pi.mnemonic) {
            case "PSHI" -> { requireNoOperand(pi); yield OP_PSHI; }
            case "POPI" -> { requireNoOperand(pi); yield OP_POPI; }
            case "PUSH" -> { requireNoOperand(pi); yield OP_PUSH; }
            case "POP"  -> { requireNoOperand(pi); yield OP_POP;  }
            case "RETN" -> { requireNoOperand(pi); yield OP_RETN; }
            case "SWAP" -> { requireNoOperand(pi); yield OP_SWAP; }

            case "LODD" -> OP_LODD | resolveDataOperand(pi, labels, variables, nextVarAddress);
            case "STOD" -> OP_STOD | resolveDataOperand(pi, labels, variables, nextVarAddress);
            case "ADDD" -> OP_ADDD | resolveDataOperand(pi, labels, variables, nextVarAddress);
            case "SUBD" -> OP_SUBD | resolveDataOperand(pi, labels, variables, nextVarAddress);

            case "JPOS" -> OP_JPOS | resolveJumpOperand(pi, labels);
            case "JZER" -> OP_JZER | resolveJumpOperand(pi, labels);
            case "JUMP" -> OP_JUMP | resolveJumpOperand(pi, labels);
            case "JNEG" -> OP_JNEG | resolveJumpOperand(pi, labels);
            case "JNZE" -> OP_JNZE | resolveJumpOperand(pi, labels);
            case "CALL" -> OP_CALL | resolveJumpOperand(pi, labels);

            case "LOCO" -> OP_LOCO | resolveUnsignedOperand(pi, 0x0FFF);
            case "LODL" -> OP_LODL | resolveUnsignedOperand(pi, 0x0FFF);
            case "STOL" -> OP_STOL | resolveUnsignedOperand(pi, 0x0FFF);
            case "ADDL" -> OP_ADDL | resolveUnsignedOperand(pi, 0x0FFF);
            case "SUBL" -> OP_SUBL | resolveUnsignedOperand(pi, 0x0FFF);

            case "INSP" -> OP_INSP | resolveUnsignedOperand(pi, 0x00FF);
            case "DESP" -> OP_DESP | resolveUnsignedOperand(pi, 0x00FF);

            default -> throw new IllegalArgumentException(
                    "Mnemônico desconhecido '" + pi.mnemonic + "' na linha " + pi.lineNumber
            );
        };
    }

    private static void validateMnemonic(String mnemonic, int lineNumber) {
        switch (mnemonic) {
            case "LODD", "STOD", "ADDD", "SUBD",
                 "JPOS", "JZER", "JUMP", "LOCO",
                 "LODL", "STOL", "ADDL", "SUBL",
                 "JNEG", "JNZE", "CALL",
                 "PSHI", "POPI", "PUSH", "POP", "RETN", "SWAP",
                 "INSP", "DESP" -> { }
            default -> throw new IllegalArgumentException(
                    "Mnemônico desconhecido '" + mnemonic + "' na linha " + lineNumber
            );
        }
    }

    private static void requireNoOperand(ParsedInstruction pi) {
        if (pi.operand != null) {
            throw new IllegalArgumentException(String.format(
                    "Instrução %s não aceita operandos na linha %d", pi.mnemonic, pi.lineNumber
            ));
        }
    }

    private static void requireOperand(ParsedInstruction pi) {
        if (pi.operand == null || pi.operand.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Instrução %s requer 1 operando na linha %d", pi.mnemonic, pi.lineNumber
            ));
        }
    }

    private static int resolveDataOperand(ParsedInstruction pi, Map<String, Integer> labels,
                                          Map<String, Integer> variables, int nextVarAddress) {
        requireOperand(pi);
        String key = pi.operand.toLowerCase();
        if (labels.containsKey(key)) {
            return labels.get(key) & 0x0FFF;
        }
        if (variables.containsKey(key)) {
            return variables.get(key) & 0x0FFF;
        }
        Integer numeric = tryParseInteger(pi.operand);
        if (numeric != null) {
            validateRange(numeric, 0, 0x0FFF, pi);
            return numeric & 0x0FFF;
        }
        if (isValidIdentifier(pi.operand)) {
            validateRange(nextVarAddress, 0, 0x0FFF, pi);
            variables.put(key, nextVarAddress);
            return nextVarAddress & 0x0FFF;
        }
        throw new IllegalArgumentException(
                "Operando inválido '" + pi.operand + "' na linha " + pi.lineNumber
        );
    }

    private static int resolveJumpOperand(ParsedInstruction pi, Map<String, Integer> labels) {
        requireOperand(pi);
        String key = pi.operand.toLowerCase();
        if (labels.containsKey(key)) {
            return labels.get(key) & 0x0FFF;
        }
        Integer numeric = tryParseInteger(pi.operand);
        if (numeric != null) {
            validateRange(numeric, 0, 0x0FFF, pi);
            return numeric & 0x0FFF;
        }
        throw new IllegalArgumentException(
                "Rótulo não definido '" + pi.operand + "' na linha " + pi.lineNumber
        );
    }

    private static int resolveUnsignedOperand(ParsedInstruction pi, int maxVal) {
        requireOperand(pi);
        Integer numeric = tryParseInteger(pi.operand);
        if (numeric == null) {
            throw new IllegalArgumentException(
                    "Valor numérico inválido '" + pi.operand + "' na linha " + pi.lineNumber
            );
        }
        validateRange(numeric, 0, maxVal, pi);
        return numeric & maxVal;
    }

    private static void validateRange(int value, int min, int max, ParsedInstruction pi) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(String.format(
                    "Operando fora do intervalo [%d, %d] para %s na linha %d: %d",
                    min, max, pi.mnemonic, pi.lineNumber, value
            ));
        }
    }

    private static boolean isValidIdentifier(String token) {
        if (token.isEmpty() || !Character.isLetter(token.charAt(0))) {
            return false;
        }
        for (int i = 1; i < token.length(); i++) {
            char c = token.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }
        return true;
    }

    private static Integer tryParseInteger(String token) {
        try {
            token = token.trim();
            if (token.startsWith("0x") || token.startsWith("0X")) {
                return Integer.parseInt(token.substring(2), 16);
            }
            if (token.startsWith("0b") || token.startsWith("0B")) {
                return Integer.parseInt(token.substring(2), 2);
            }
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            return null;
        }
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

    public static String disassembleWord(int word) {
        int w = word & 0xFFFF;
        int high4 = w & 0xF000;
        int addr12 = w & 0x0FFF;

        return switch (high4) {
            case OP_LODD -> String.format("LODD 0x%03X", addr12);
            case OP_STOD -> String.format("STOD 0x%03X", addr12);
            case OP_ADDD -> String.format("ADDD 0x%03X", addr12);
            case OP_SUBD -> String.format("SUBD 0x%03X", addr12);
            case OP_JPOS -> String.format("JPOS 0x%03X", addr12);
            case OP_JZER -> String.format("JZER 0x%03X", addr12);
            case OP_JUMP -> String.format("JUMP 0x%03X", addr12);
            case OP_LOCO -> String.format("LOCO %d", addr12);
            case OP_LODL -> String.format("LODL %d", addr12);
            case OP_STOL -> String.format("STOL %d", addr12);
            case OP_ADDL -> String.format("ADDL %d", addr12);
            case OP_SUBL -> String.format("SUBL %d", addr12);
            case OP_JNEG -> String.format("JNEG 0x%03X", addr12);
            case OP_JNZE -> String.format("JNZE 0x%03X", addr12);
            case OP_CALL -> String.format("CALL 0x%03X", addr12);
            case 0xF000 -> {
                int high8 = w & 0xFF00;
                int val8 = w & 0x00FF;
                yield switch (high8) {
                    case 0xFC00 -> String.format("INSP %d", val8);
                    case 0xFE00 -> String.format("DESP %d", val8);
                    default -> switch (w) {
                        case OP_PSHI -> "PSHI";
                        case OP_POPI -> "POPI";
                        case OP_PUSH -> "PUSH";
                        case OP_POP  -> "POP";
                        case OP_RETN -> "RETN";
                        case OP_SWAP -> "SWAP";
                        default -> String.format("DATA 0x%04X", w);
                    };
                };
            }
            default -> String.format("DATA 0x%04X", w);
        };
    }
}
