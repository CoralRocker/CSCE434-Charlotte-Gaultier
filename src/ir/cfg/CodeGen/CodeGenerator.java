package ir.cfg.CodeGen;

import coco.DLX;
import ir.cfg.BasicBlock;
import ir.cfg.*;
import ir.cfg.registers.RegisterAllocator;
import ir.tac.*;

import java.util.*;

public class CodeGenerator extends TACVisitor<List<DLXCode>> {

    public static final int STACK_PTR = 29, FRAME_PTR = 28, SPILL_DEST = 27, SPILL_LHS = 26, SPILL_RHS = 25, GLOB_VAR = 30, PREV_PC = 31;

    private Map<Assignable, Integer> registers;
    private Map<Integer, Integer> labels; // Associate label number to instruction number at start of label (relative to CFG numbering, not global)

    private boolean isMain;

    private int instrnum;

    public static List<DLXCode> generate(CFG cfg, int nRegs, boolean isMain) {
        CodeGenerator visitor = new CodeGenerator();

        RegisterAllocator allocator = new RegisterAllocator(nRegs);
        visitor.registers = allocator.allocateRegisters(cfg);
        visitor.labels = new HashMap<>();
        visitor.isMain = isMain;
        visitor.instrnum = 0;

        List<DLXCode> instructions = new ArrayList<>();

        // if is main, generate the necessary start of stack bullshit
        if( isMain ) {

            instructions.add( DLXCode.immediateOp(DLXCode.OPCODE.SUBI, STACK_PTR, GLOB_VAR, -2 * cfg.getSymbols().size() ) );
            instructions.add( DLXCode.immediateOp(DLXCode.OPCODE.ADDI, FRAME_PTR, STACK_PTR, 0) );

        }

        for( BasicBlock blk : cfg.allNodes ) {

            visitor.labels.put(blk.getNum(), instructions.size());

            for( var instr : blk.getInstructions() ) {
                List<DLXCode> dlx = instr.accept(visitor);
                if( dlx == null ) {
                    throw new RuntimeException("DLXCode generation returned null for instruction " + instr);
                }
                instructions.addAll(dlx);

                visitor.instrnum = instructions.size();
            }
        }

        if( !instructions.get(instructions.size()-1).getOpcode().equals(DLXCode.OPCODE.RET) ) {
            instructions.add( DLXCode.regOp(DLXCode.OPCODE.RET, 0, 0, 0));
        }

        // Set the address of all branches properly
        var iter = instructions.listIterator();
        int counter = 0;
        while( iter.hasNext() ) {
            var asm = iter.next();

            if( asm.getFormat().equals(DLXCode.FORMAT.UNRESOLVED_BRANCH) ) {
                int bb = visitor.labels.get(asm.immediate);
                int c = bb - counter;
                if( c == 1 ) {
                    iter.remove();
                } else {
                    iter.set(DLXCode.immediateOp(asm.opcode, asm.regA, asm.regB, c));
                }
            }

            counter++;
        }


        return instructions;
    }

    @Override
    public List<DLXCode> visit(Return ret) {
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.RET, 0, 0, 0));
    }

    @Override
    public List<DLXCode> visit(Literal lit) {
        return null;
    }

    @Override
    public List<DLXCode> visit(Call call) {

        // TODO: Generate Code To Save Registers and Whatnot

        switch( call.function.name() ) {
            case "printInt" -> {
                return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.WRI, 0, registers.get(call.args.get(0)), 0));
            }
            case "println" -> {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.WRL, 0, 0, 0));
            }
            case "printBool" -> {
                return List.of( DLXCode.regOp(DLXCode.OPCODE.WRB, 0, registers.get(call.args.get(0)), 0) );
            }

            case "readInt" -> {
                return List.of( DLXCode.regOp( DLXCode.OPCODE.RDI, registers.get(call.dest), 0, 0) );
            }

            case "readBool" -> {
                return List.of( DLXCode.regOp( DLXCode.OPCODE.RDB, registers.get(call.dest), 0, 0) );
            }
        }
        return null;
    }

    @Override
    public List<DLXCode> visit(Variable var) {
        return null;
    }

    @Override
    public List<DLXCode> visit(Add add) {
        int dest = registers.get(add.dest);
        if( add.hasImmediate() ) {
            boolean lit_lhs = add.left instanceof Literal,
                    lit_rhs = add.right instanceof Literal;
            if( lit_rhs && lit_lhs ) {
                return List.of(
                        DLXCode.immediateOp(DLXCode.OPCODE.ADDI, SPILL_LHS, 0, ((Literal) add.left).getInt()),
                        DLXCode.immediateOp(DLXCode.OPCODE.ADDI, dest, SPILL_LHS, ((Literal) add.right).getInt())
                );
            }else if( lit_lhs ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ADDI, dest, registers.get(add.right), ((Literal) add.left).getInt()));
            }
            else {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ADDI, dest, registers.get(add.left), ((Literal) add.right).getInt()));
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.ADD, dest, registers.get(add.left), registers.get(add.right)));
    }

    @Override
    public List<DLXCode> visit(Assign asn) {
        return null;
    }

    @Override
    public List<DLXCode> visit(Div div) {
        int dest = registers.get(div.dest);
        if( div.hasImmediate() ) {
            boolean lit_lhs = div.left instanceof Literal,
                    lit_rhs = div.right instanceof Literal;
            if( lit_rhs && lit_lhs ) {
                return List.of(
                        DLXCode.immediateOp(DLXCode.OPCODE.ADDI, SPILL_LHS, 0, ((Literal) div.left).getInt()),
                        DLXCode.immediateOp(DLXCode.OPCODE.DIVI, dest, SPILL_LHS, ((Literal) div.right).getInt())
                );
            }else if( lit_rhs ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.DIVI, dest, registers.get(div.left), ((Literal) div.right).getInt()));
            }
            else {
                return List.of(
                        DLXCode.immediateOp(DLXCode.OPCODE.ADDI, SPILL_LHS, 0, ((Literal) div.left).getInt()),
                        DLXCode.regOp(DLXCode.OPCODE.DIV, dest, SPILL_LHS, registers.get(div.right))
                );
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.DIV, dest, registers.get(div.left), registers.get(div.right)));
    }

    @Override
    public List<DLXCode> visit(Mod mod) {
        int dest = registers.get(mod.dest);
        if( mod.hasImmediate() ) {
            boolean lit_lhs = mod.left instanceof Literal,
                    lit_rhs = mod.right instanceof Literal;
            if( lit_rhs && lit_lhs ) {
                return List.of(
                        DLXCode.immediateOp(DLXCode.OPCODE.ADDI, SPILL_LHS, 0, ((Literal) mod.left).getInt()),
                        DLXCode.immediateOp(DLXCode.OPCODE.MODI, dest, SPILL_LHS, ((Literal) mod.right).getInt())
                );
            }else if( lit_rhs ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.MODI, dest, registers.get(mod.left), ((Literal) mod.right).getInt()));
            }
            else {
                return List.of(
                        DLXCode.immediateOp(DLXCode.OPCODE.ADDI, SPILL_LHS, 0, ((Literal) mod.left).getInt()),
                        DLXCode.regOp(DLXCode.OPCODE.MOD, dest, SPILL_LHS, registers.get(mod.right))
                );
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.MOD, dest, registers.get(mod.left), registers.get(mod.right)));
    }

    @Override
    public List<DLXCode> visit(Mul mul) {
        int dest = registers.get(mul.dest);
        if( mul.hasImmediate() ) {
            boolean lit_lhs = mul.left instanceof Literal,
                    lit_rhs = mul.right instanceof Literal;
            if( lit_rhs && lit_lhs ) {
                return List.of(
                        DLXCode.immediateOp(DLXCode.OPCODE.ADDI, SPILL_LHS, 0, ((Literal) mul.left).getInt()),
                        DLXCode.immediateOp(DLXCode.OPCODE.MULI, dest, SPILL_LHS, ((Literal) mul.right).getInt())
                );
            }else if( lit_lhs ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.MULI, dest, registers.get(mul.right), ((Literal) mul.left).getInt()));
            }
            else {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.MULI, dest, registers.get(mul.left), ((Literal) mul.right).getInt()));
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.MUL, dest, registers.get(mul.left), registers.get(mul.right)));
    }

    @Override
    public List<DLXCode> visit(Sub sub) {
        int dest = registers.get(sub.dest);
        if( sub.hasImmediate() ) {
            boolean lit_lhs = sub.left instanceof Literal,
                    lit_rhs = sub.right instanceof Literal;
            if( lit_rhs && lit_lhs ) {
                return List.of(
                        DLXCode.immediateOp(DLXCode.OPCODE.ADDI, SPILL_LHS, 0, ((Literal) sub.left).getInt()),
                        DLXCode.immediateOp(DLXCode.OPCODE.SUBI, dest, SPILL_LHS, ((Literal) sub.right).getInt())
                );
            }else if( lit_rhs ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.SUBI, dest, registers.get(sub.left), ((Literal) sub.right).getInt()));
            }
            else {
                return List.of(
                        DLXCode.immediateOp(DLXCode.OPCODE.ADDI, SPILL_LHS, 0, ((Literal) sub.left).getInt()),
                        DLXCode.regOp(DLXCode.OPCODE.SUB, dest, SPILL_LHS, registers.get(sub.right))
                );
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.SUB, dest, registers.get(sub.left), registers.get(sub.right)));
    }

    @Override
    public List<DLXCode> visit(LoadStack lstack) {
        return List.of( DLXCode.immediateOp(DLXCode.OPCODE.LDW, lstack.loc.reg.num, FRAME_PTR, -1 * lstack.loc.spillNo) );
    }

    @Override
    public List<DLXCode> visit(Branch bra) {

        int dest = bra.getJumpTo().getNum();
        DLXCode.OPCODE opcode;
        switch( bra.getRel() ) {
            case ">" -> {
                opcode = DLXCode.OPCODE.BGT;
            }
            case ">=" -> {
                opcode = DLXCode.OPCODE.BGE;
            }
            case "==" -> {
                opcode = DLXCode.OPCODE.BEQ;
            }
            case "!=" -> {
                opcode = DLXCode.OPCODE.BNE;
            }
            case "<" -> {
                opcode = DLXCode.OPCODE.BLT;
            }
            case "<=" -> {
                opcode = DLXCode.OPCODE.BLE;
            }
            default -> {
                opcode = DLXCode.OPCODE.BSR;
            }
        }
        if( !labels.containsKey(dest) ) {
            if( bra.isConditional() ) {
                return List.of(DLXCode.unresolvedBranch(opcode, registers.get((Assignable) bra.getVal()), dest));
            }
            else {
                return List.of(DLXCode.unresolvedBranch(opcode, 0, dest));
            }
        }
        else {
            dest = labels.get(dest) - instrnum;
            if( !opcode.equals(DLXCode.OPCODE.BSR) ) {
                return List.of(DLXCode.immediateOp(opcode, registers.get((Assignable) bra.getVal()), 0, dest));
            }
            else {
                return List.of(DLXCode.immediateOp(opcode, 0, 0, dest));
            }
        }
    }

    @Override
    public List<DLXCode> visit(Cmp cmp) {

        //
        // NOTE! The LHS and RHS are flipped here for a reason:
        // The CMP instruction performs R.b - R.c, followed
        //

        int dest = registers.get(cmp.dest);
        if( cmp.hasImmediate() ) {
            boolean lit_lhs = cmp.left instanceof Literal,
                    lit_rhs = cmp.right instanceof Literal;
            if( lit_rhs && lit_lhs ) {
                return List.of(
                        DLXCode.immediateOp(DLXCode.OPCODE.ADDI, SPILL_LHS, 0, ((Literal) cmp.left).getInt()),
                        DLXCode.immediateOp(DLXCode.OPCODE.CMPI, dest, SPILL_LHS, ((Literal) cmp.right).getInt())
                );
            }else if( lit_rhs ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.CMPI, dest, registers.get(cmp.left), ((Literal) cmp.right).getInt()));
            }
            else {
                return List.of(
                        DLXCode.immediateOp(DLXCode.OPCODE.ADDI, SPILL_LHS, 0, ((Literal) cmp.left).getInt()),
                        DLXCode.regOp(DLXCode.OPCODE.CMP, dest, SPILL_LHS, registers.get(cmp.right))
                );
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.CMP, dest, registers.get(cmp.left), registers.get(cmp.right)));
    }

    @Override
    public List<DLXCode> visit(Store store) {
        if( store.source instanceof Literal ) {
            // DEST = R0(always 0) + literal
            return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ADDI, registers.get(store.dest), 0, ((Literal) store.source).getInt()));
        }

        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.ADD, registers.get(store.dest), 0, registers.get((Assignable) store.source)));
    }

    @Override
    public List<DLXCode> visit(StoreStack sstack) {
        return List.of( DLXCode.immediateOp(DLXCode.OPCODE.STW, sstack.loc.reg.num, FRAME_PTR, -1 * sstack.loc.spillNo) );
    }

    @Override
    public List<DLXCode> visit(Phi phi) {
        return null;
    }

    @Override
    public List<DLXCode> visit(Temporary temporary) {
        return null;
    }

    @Override
    public List<DLXCode> visit(Not not) {
        if( not.src instanceof Literal ) {
            // DEST = !literal
            return List.of(
                    DLXCode.immediateOp(DLXCode.OPCODE.ADDI, SPILL_DEST, 0, 1),
                    DLXCode.immediateOp(DLXCode.OPCODE.SUB, registers.get(not.dest), SPILL_DEST, ((Literal) not.src).getInt() )
            );
        }
        return List.of(
                DLXCode.immediateOp(DLXCode.OPCODE.ADDI, SPILL_DEST, 0, 1),
                DLXCode.immediateOp(DLXCode.OPCODE.SUB, registers.get(not.dest), SPILL_DEST, registers.get((Assignable) not.src) )
        );
    }

    @Override
    public List<DLXCode> visit(And and) {
        int dest = registers.get(and.dest);
        if( and.hasImmediate() ) {
            boolean lit_lhs = and.left instanceof Literal,
                    lit_rhs = and.right instanceof Literal;
            if( lit_rhs && lit_lhs ) {
                return List.of(
                        DLXCode.immediateOp(DLXCode.OPCODE.ADDI, SPILL_LHS, 0, ((Literal) and.left).getInt()),
                        DLXCode.immediateOp(DLXCode.OPCODE.ANDI, dest, SPILL_LHS, ((Literal) and.right).getInt())
                );
            }else if( lit_lhs ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ANDI, dest, registers.get(and.right), ((Literal) and.left).getInt()));
            }
            else {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ANDI, dest, registers.get(and.left), ((Literal) and.right).getInt()));
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.AND, dest, registers.get(and.left), registers.get(and.right)));
    }

    @Override
    public List<DLXCode> visit(Or or) {
        int dest = registers.get(or.dest);
        if (or.hasImmediate()) {
            boolean lit_lhs = or.left instanceof Literal,
                    lit_rhs = or.right instanceof Literal;
            if (lit_rhs && lit_lhs) {
                return List.of(
                        DLXCode.immediateOp(DLXCode.OPCODE.ADDI, SPILL_LHS, 0, ((Literal) or.left).getInt()),
                        DLXCode.immediateOp(DLXCode.OPCODE.ORI, dest, SPILL_LHS, ((Literal) or.right).getInt())
                );
            } else if (lit_lhs) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ORI, dest, registers.get(or.right), ((Literal) or.left).getInt()));
            } else {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ORI, dest, registers.get(or.left), ((Literal) or.right).getInt()));
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.OR, dest, registers.get(or.left), registers.get(or.right)));
    }
}

