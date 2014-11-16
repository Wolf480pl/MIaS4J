package com.github.wolf480pl.sandbox;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Opcodes;

public enum InvocationType {

    INVOKEVIRTUAL(Opcodes.H_INVOKEVIRTUAL, Opcodes.INVOKEVIRTUAL),
    INVOKESTATIC(Opcodes.H_INVOKESTATIC, Opcodes.INVOKESTATIC),
    INVOKESPECIAL(Opcodes.H_INVOKESPECIAL, Opcodes.INVOKESPECIAL),
    INVOKENEWSPECIAL(Opcodes.H_NEWINVOKESPECIAL, Opcodes.INVOKESPECIAL, true),
    INVOKEINTERFACE(Opcodes.H_INVOKEINTERFACE, Opcodes.INVOKEINTERFACE);


    public final int handleOpcode;
    public final int insnOpcode;
    private final boolean onlyHOpcode;

    private InvocationType(int hopcode, int insn) {
        this(hopcode, insn, false);
    }

    private InvocationType(int hopcode, int insn, boolean onlyHOpcode) {
        this.handleOpcode = hopcode;
        this.insnOpcode = insn;
        this.onlyHOpcode = onlyHOpcode;
    }

    public int id() {
        return handleOpcode; // Subject to change
    }

    private static InvocationType[] byHOpcode = new InvocationType[10];
    private static Map<Integer, InvocationType> byIOpcode = new HashMap<>();

    static {
        for (InvocationType t : InvocationType.values()) {
            register(t);
        }
    }

    private static void register(InvocationType t) {
        byHOpcode[t.handleOpcode] = t;
        if (!t.onlyHOpcode) {
            byIOpcode.put(t.insnOpcode, t);
        }
    }

    public static InvocationType fromHandleOpcode(int handleOpcode) {
        if (handleOpcode < 0 || handleOpcode >= byHOpcode.length) {
            return null;
        }
        return byHOpcode[handleOpcode];
    }

    public static InvocationType fromInstruction(int insnOpcode) {
        return byIOpcode.get(insnOpcode);
    }

    public static InvocationType fromID(int id) {
        return fromHandleOpcode(id); // Subject to change
    }
}
