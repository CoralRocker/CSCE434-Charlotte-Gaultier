package ir.cfg.CodeGen;

import ir.cfg.BasicBlock;
import ir.cfg.*;
import ir.cfg.registers.RegisterAllocator;
import ir.tac.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CodeGenerator extends TACVisitor<List<DLX>> {

    public static final int STACK_PTR = 29, FRAME_PTR = 28, SPILL_DEST = 27, SPILL_LHS = 26, SPILL_RHS = 25, GLOB_VAR = 30, PREV_PC = 31;

    private Map<Assignable, Integer> registers;
    private boolean isMain;

    public static List<DLX> generate(CFG cfg, int nRegs, boolean isMain) {
        CodeGenerator visitor = new CodeGenerator();

        RegisterAllocator allocator = new RegisterAllocator(nRegs);
        visitor.registers = allocator.allocateRegisters(cfg);
        visitor.isMain = isMain;

        List<DLX> instructions = new ArrayList<>();

        // if is main, generate the necessary start of stack bullshit
        if( isMain ) {

            instructions.add( DLX.immediateOp(DLX.OPCODE.SUBI, STACK_PTR, GLOB_VAR, -2 * cfg.getSymbols().size() ) );
            instructions.add( DLX.immediateOp(DLX.OPCODE.ADDI, FRAME_PTR, STACK_PTR, 0) );

        }

        for( BasicBlock blk : cfg.allNodes ) {
            for( var instr : blk.getInstructions() ) {
                List<DLX> dlx = instr.accept(visitor);
                if( dlx == null ) {
                    throw new RuntimeException("DLX generation returned null for instruction " + instr);
                }
                instructions.addAll(dlx);
            }
        }

        if( !instructions.get(instructions.size()-1).getOpcode().equals(DLX.OPCODE.RET) ) {
            instructions.add( DLX.regOp(DLX.OPCODE.RET, 0, 0, 0));
        }

        return instructions;
    }

    @Override
    public List<DLX> visit(Return ret) {
        return Collections.singletonList(DLX.regOp(DLX.OPCODE.RET, 0, 0, 0));
    }

    @Override
    public List<DLX> visit(Literal lit) {
        return null;
    }

    @Override
    public List<DLX> visit(Call call) {

        // TODO: Generate Code To Save Registers and Whatnot

        switch( call.function.name() ) {
            case "printInt" -> {
                return Collections.singletonList(DLX.regOp(DLX.OPCODE.WRI, 0, registers.get(call.args.get(0)), 0));
            }
            case "println" -> {
                return Collections.singletonList(DLX.immediateOp(DLX.OPCODE.WRL, 0, 0, 0));
            }
        }
        return null;
    }

    @Override
    public List<DLX> visit(Variable var) {
        return null;
    }

    @Override
    public List<DLX> visit(Add add) {
        int dest = registers.get(add.dest);
        if( add.hasImmediate() ) {
            if( add.left instanceof Literal ) {
                return Collections.singletonList(DLX.immediateOp(DLX.OPCODE.ADDI, dest, registers.get(add.right), ((Literal) add.left).getInt()));
            }
            else {
                return Collections.singletonList(DLX.immediateOp(DLX.OPCODE.ADDI, dest, registers.get(add.left), ((Literal) add.right).getInt()));
            }
        }
        return Collections.singletonList(DLX.regOp(DLX.OPCODE.ADD, dest, registers.get(add.left), registers.get(add.right)));
    }

    @Override
    public List<DLX> visit(Assign asn) {
        return null;
    }

    @Override
    public List<DLX> visit(Div div) {
        int dest = registers.get(div.dest);
        if( div.hasImmediate() ) {
            if( div.left instanceof Literal ) {
                return Collections.singletonList(DLX.immediateOp(DLX.OPCODE.DIVI, dest, registers.get(div.right), ((Literal) div.left).getInt()));
            }
            else {
                return Collections.singletonList(DLX.immediateOp(DLX.OPCODE.DIVI, dest, registers.get(div.left), ((Literal) div.right).getInt()));
            }
        }
        return Collections.singletonList(DLX.regOp(DLX.OPCODE.DIV, dest, registers.get(div.left), registers.get(div.right)));
    }

    @Override
    public List<DLX> visit(Mod mod) {
        int dest = registers.get(mod.dest);
        if( mod.hasImmediate() ) {
            if( mod.left instanceof Literal ) {
                return Collections.singletonList(DLX.immediateOp(DLX.OPCODE.MODI, dest, registers.get(mod.right), ((Literal) mod.left).getInt()));
            }
            else {
                return Collections.singletonList(DLX.immediateOp(DLX.OPCODE.MODI, dest, registers.get(mod.left), ((Literal) mod.right).getInt()));
            }
        }
        return Collections.singletonList(DLX.regOp(DLX.OPCODE.MOD, dest, registers.get(mod.left), registers.get(mod.right)));
    }

    @Override
    public List<DLX> visit(Mul mul) {
        int dest = registers.get(mul.dest);
        if( mul.hasImmediate() ) {
            if( mul.left instanceof Literal )
                return Collections.singletonList(DLX.immediateOp(DLX.OPCODE.MULI, dest, registers.get(mul.right), ((Literal) mul.left).getInt()));
            else {
                return Collections.singletonList(DLX.immediateOp(DLX.OPCODE.MULI, dest, registers.get(mul.left), ((Literal) mul.right).getInt()));
            }
        }
        return Collections.singletonList(DLX.regOp(DLX.OPCODE.MUL, dest, registers.get(mul.left), registers.get(mul.right)));
    }

    @Override
    public List<DLX> visit(Sub sub) {
        int dest = registers.get(sub.dest);
        if( sub.hasImmediate() ) {
            if( sub.left instanceof Literal ) {
                return Collections.singletonList(DLX.immediateOp(DLX.OPCODE.SUBI, dest, registers.get(sub.right), ((Literal) sub.left).getInt()));
            }
            else {
                return Collections.singletonList(DLX.immediateOp(DLX.OPCODE.SUBI, dest, registers.get(sub.left), ((Literal) sub.right).getInt()));
            }
        }
        return Collections.singletonList(DLX.regOp(DLX.OPCODE.SUB, dest, registers.get(sub.left), registers.get(sub.right)));
    }

    @Override
    public List<DLX> visit(LoadStack lstack) {
        return List.of( DLX.immediateOp(DLX.OPCODE.LDW, lstack.loc.reg.num, FRAME_PTR, -1 * lstack.loc.spillNo) );
    }

    @Override
    public List<DLX> visit(Branch bra) {
        return null;
    }

    @Override
    public List<DLX> visit(Cmp cmp) {
        int dest = registers.get(cmp.dest);
        if( cmp.hasImmediate() ) {
            if( cmp.left instanceof Literal ) {
                return Collections.singletonList(DLX.immediateOp(DLX.OPCODE.CMPI, dest, registers.get(cmp.right), ((Literal) cmp.left).getInt()));
            }
            else {
                return Collections.singletonList(DLX.immediateOp(DLX.OPCODE.CMPI, dest, registers.get(cmp.left), ((Literal) cmp.right).getInt()));
            }
        }
        return Collections.singletonList(DLX.regOp(DLX.OPCODE.CMP, dest, registers.get(cmp.left), registers.get(cmp.right)));
    }

    @Override
    public List<DLX> visit(Store store) {
        if( store.source instanceof Literal ) {
            // DEST = R0(always 0) + literal
            return Collections.singletonList(DLX.immediateOp(DLX.OPCODE.ADDI, registers.get(store.dest), 0, ((Literal) store.source).getInt()));
        }

        return Collections.singletonList(DLX.regOp(DLX.OPCODE.ADD, registers.get(store.dest), 0, registers.get((Assignable) store.source)));
    }

    @Override
    public List<DLX> visit(StoreStack sstack) {
        return List.of( DLX.immediateOp(DLX.OPCODE.STW, registers.get(sstack.dest), FRAME_PTR, -1 * sstack.loc.spillNo) );
    }

    @Override
    public List<DLX> visit(Phi phi) {
        return null;
    }

    @Override
    public List<DLX> visit(Temporary temporary) {
        return null;
    }
}

