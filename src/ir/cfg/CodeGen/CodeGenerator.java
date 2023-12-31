package ir.cfg.CodeGen;

import coco.DLX;
import coco.FunctionSymbol;
import coco.VariableSymbol;
import ir.cfg.BasicBlock;
import ir.cfg.*;
import ir.cfg.registers.LoadStoreCleaner;
import ir.cfg.registers.RegisterAllocator;
import ir.tac.*;
import types.VoidType;

import java.util.*;

public class CodeGenerator implements TACVisitor<List<DLXCode>> {

    public static final int STACK_PTR = 29, FRAME_PTR = 28, SPILL_DEST = 27, SPILL_LHS = 26, SPILL_RHS = 25, GLOB_VAR = 30, PREV_PC = 31;

    private HashMap<Assignable, Integer> registers;
    private Map<Integer, Integer> labels; // Associate label number to instruction number at start of label (relative to CFG numbering, not global)

    private int numSpills;
    private boolean isMain;

    private CFG cfg;
    private int numSavedRegisters;

    private int instrnum;
    private boolean do_print;

    private int getDest(Assignable dest) {
        int reg = registers.get(dest);
        if( reg <= -1 ) reg = SPILL_DEST;
        return reg;
    }
    private int getRight(Value dest) {
        if( dest instanceof Assignable ) {
            int reg = registers.get(dest);
            if( reg <= -1 ) reg = SPILL_RHS;
            return reg;
        }
        return -1;
    }

    private int getLeft(Value dest) {
        if( dest instanceof Assignable ) {
            int reg = registers.get(dest);
            if( reg <= -1 ) reg = SPILL_LHS;
            return reg;
        }
        return -1;
    }

    private List<DLXCode> move(int destReg, int srcReg, TAC tac) {
        List<DLXCode> code = new ArrayList<>();
        if( destReg == srcReg )
            return Collections.emptyList();

        if( srcReg <= -1 ) {
            // throw new RuntimeException("Cannot move from a spill (src) : " + tac.errMsg());
            int newSrc = SPILL_LHS;
            if( destReg == SPILL_LHS ) newSrc = SPILL_DEST;

            code.add( DLXCode.immediateOp(DLXCode.OPCODE.LDW, newSrc, FRAME_PTR, 4 * srcReg, tac) );
            srcReg = newSrc;
        }
        if( destReg <= -1 )
            throw new RuntimeException("Cannot move to a spill (dest) : " + tac.errMsg());

        code.add( DLXCode.regOp(DLXCode.OPCODE.ADD, destReg, 0, srcReg, tac));
        return code;
    }

    private List<DLXCode> move(int destReg, Literal src, TAC tac) {
        return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ADDI, destReg, 0, src.getInt(), tac));
    }

    private List<DLXCode> move(int destReg, Assignable src, TAC tac) {
        int reg = registers.get(src);
        return move( destReg, reg, tac );
    }

    public static List<DLXCode> generate(CFG cfg, int nRegs, boolean isMain, boolean do_print) {
        cfg.genAllNodes();

        CodeGenerator visitor = new CodeGenerator();

        RegisterAllocator allocator = new RegisterAllocator(nRegs, false);
        visitor.registers = allocator.allocateRegisters(cfg);
        visitor.labels = new HashMap<>();
        visitor.isMain = isMain;
        visitor.instrnum = 0;
        visitor.numSpills = 0;
        visitor.numSavedRegisters = 0;
        visitor.cfg = cfg;
        visitor.do_print = do_print;

        LoadStoreCleaner cleaner = new LoadStoreCleaner(cfg, visitor.registers, false);
        cleaner.clean();

        for( var entry : visitor.registers.entrySet() ) {
            if( entry.getValue() <= -1 ) {
                visitor.numSpills++;

                entry.setValue(-1 * visitor.numSpills);
                entry.getKey().spilled.spillNo = -1;
                if( do_print )
                    System.out.printf( "Variable %s is spilled to location %2d <=> %2d\n", entry.getKey(), entry.getValue(), 4 * entry.getValue() );
            }
        }

        if( do_print ) {
            System.out.printf("Register Allocation: %s\n", visitor.registers);
            // System.out.printf("%s\n", cfg.asDotGraph());

            // for (BasicBlock blk : cfg.allNodes) {
            //     System.out.printf("BB%d:\n", blk.getNum());
            //     for (TAC tac : blk.getInstructions()) {
            //         System.out.printf("%3d: %-20s %15s -> %-15s\n", tac.getId(), tac.genDot(), tac.liveBeforePP, tac.liveAfterPP);
            //     }
            // }
            // System.out.println("\n");
        }

        List<DLXCode> instructions = new ArrayList<>();

        // if is main, generate the necessary start of stack bullshit
        if( isMain ) {
            int varSize = 0;
            for (Map.Entry sym : cfg.getSymbols().entrySet()){
                ((VariableSymbol) sym.getValue()).globalLoc = varSize;
                if( !(((VariableSymbol)sym.getValue()).type().getDims() == null) ){
                    if( do_print )
                        System.out.printf("Global Array %s at offset %d from R30\n", sym.getKey(), varSize);
                    // this means it's an array
                    ArrayList<Integer> dims = ((VariableSymbol)sym.getValue()).type().getDims();
                    varSize += dims.stream().reduce(4, (a, b) -> a*b);
                }else{
                    varSize += 4;
                }
            }
            // store array start addresses into correct vars
            instructions.add( DLXCode.immediateOp(DLXCode.OPCODE.SUBI, STACK_PTR, GLOB_VAR, varSize, null ) );
            instructions.add( DLXCode.immediateOp(DLXCode.OPCODE.ADDI, FRAME_PTR, STACK_PTR, 0, null) );

            varSize = 0;
            for (Map.Entry sym : cfg.getSymbols().entrySet()){
                if( !(((VariableSymbol)sym.getValue()).type().getDims() == null) ){
                    Variable var = new Variable((VariableSymbol)sym.getValue());
                    var.isGlobal = true;
                    if (visitor.registers.containsKey(var)) {
                        int dest = visitor.registers.get(var);
                        int loc = dest;
                        if( dest <= -1 ) {
                            dest = SPILL_DEST;
                        }
                        instructions.add( DLXCode.immediateOp(DLXCode.OPCODE.ADDI, dest, GLOB_VAR, -1 * ((VariableSymbol)sym.getValue()).globalLoc, null) );
                        if( dest == SPILL_DEST ) {
                            instructions.add( DLXCode.immediateOp(DLXCode.OPCODE.STW, dest, FRAME_PTR, 4*loc, null));
                        }
                    }
                }
            }

        }
        else { // Generate Stack Frame Shit
            instructions.add( DLXCode.immediateOp(DLXCode.OPCODE.STW, PREV_PC, FRAME_PTR, 0, null )); // Save return address

            // load local arrays

            int varsize = 0;
            // Load Args
            int arg = 1;
            for( var param : cfg.function.getArgList() ) {
                int dest = visitor.registers.get(new Variable(param));
                int loc = dest;
                if( dest <= 1 ) {
                    dest =SPILL_DEST;
                }
                instructions.add( DLXCode.immediateOp(DLXCode.OPCODE.LDW, dest, FRAME_PTR, -4 * arg, null));
                if( loc <= -1 ) {
                    instructions.add( DLXCode.immediateOp(DLXCode.OPCODE.STW, dest, FRAME_PTR, 4 * loc, null) );
                }

                if( do_print )
                    System.out.printf("R%d <=> %s (R%d)\n", arg, param, dest );
                arg++;
                varsize += 4;
            }

            int varSize = 0;
            // Load Globals And Local Arrays
            for( var sym : cfg.getSymbols().keySet() ) {
                if( sym.globalLoc != -1 ) {
                    var symvar = new Variable(sym);
                    if( !visitor.registers.containsKey(symvar) ) {
                        if( do_print )
                            System.out.printf("Global variable %s not live for function %s\n", sym, cfg.func);
                        continue;
                    }

                    if( !cfg.allNodes.get(0).live_in.contains(symvar) ) {
                        if( do_print )
                            System.out.printf("Global variable %s not live for function %s start\n", sym, cfg.func);
                        continue;
                    }

                    int dest = visitor.getDest(symvar);

                    if( do_print )
                        System.out.printf("Global variable %s load from GLOBL[%d] to reg %d\n", sym, sym.globalLoc, dest);
                    if( sym.type().getDims() != null ) {
                        if( sym.isGlobal ) {
                            instructions.add( DLXCode.immediateOp(DLXCode.OPCODE.SUBI, dest, GLOB_VAR,  sym.globalLoc, null) );
                        }
                        else {
                            throw new RuntimeException("Non-global array");
                        }
                    }
                    else {
                        instructions.add(DLXCode.immediateOp(DLXCode.OPCODE.LDW, dest, GLOB_VAR, -1 * sym.globalLoc, null));
                    }

                    if( dest == SPILL_DEST ) {
                        dest = visitor.registers.get(symvar);
                        instructions.add(DLXCode.immediateOp(DLXCode.OPCODE.STW, SPILL_DEST, FRAME_PTR, 4 * dest, null ));
                    }
                }
                else if( sym.type().getDims() != null ) { // Local Array Time B-)
                    var symvar = new Variable(sym);
                    int dest = visitor.registers.get(symvar);
                    if( dest <= -1 ) throw new RuntimeException("Spilt local array init");

                    instructions.add(DLXCode.immediateOp(DLXCode.OPCODE.SUBI, dest, FRAME_PTR, (4 * visitor.numSpills) + varsize, null));

                    varSize += sym.type().getDims().stream().reduce(4, (a, b) -> a * b);
                }
            }
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
            Return ret = new Return(cfg.instrNumberer.push(), null);
            instructions.addAll( ret.accept(visitor) );
        }

        // Remove all silly branches
        var iter = instructions.listIterator();
        int counter = 0;
        while( iter.hasNext() ) {
            var asm = iter.next();

            if( asm.getFormat().equals(DLXCode.FORMAT.UNRESOLVED_BRANCH) ) {
                int bb = visitor.labels.get(asm.immediate);
                int c = bb - counter;
                if( c == 1 || c == 0 ) {
                    iter.remove();
                    int finalCounter = counter;
                    for (var key : visitor.labels.keySet()) {
                        int val = visitor.labels.get(key);
                        if (val > counter) {
                            visitor.labels.put(key, val - 1);
                        }
                    }
                    continue; // avoid incrementing counter
                }
            }

            counter++;
        }

        // Add all good branches
        iter = instructions.listIterator();
        counter = 0;
        while( iter.hasNext() ) {
            var asm = iter.next();

            if( asm.getFormat().equals(DLXCode.FORMAT.UNRESOLVED_BRANCH) ) {
                int bb = visitor.labels.get(asm.immediate);
                int c = bb - counter;
                if( c == 1 || c == 0 ) {
                    iter.set( DLXCode.immediateOp(asm.opcode, asm.regA, asm.regB, 1, asm.source));
                    // throw new RuntimeException("All silly branches should have been removed");
                } else {
                    iter.set(DLXCode.immediateOp(asm.opcode, asm.regA, asm.regB, c, asm.source));
                }
            }

            counter++;
        }

        if( do_print )
            System.out.printf("Branch Locations: %s\n", visitor.labels);

        return instructions;
    }

    @Override
    public List<DLXCode> visit(Return ret) {
        if( isMain ) {
            return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.RET, 0, 0, 0, ret));
        }
        else {
            List<DLXCode> code = new ArrayList<>();

            // Save Return Value
            if( ret.var instanceof Assignable ) {
                int dest = getDest((Assignable) ret.var);
                code.addAll( move(1, dest, ret) );
            }
            else if( ret.var instanceof Literal ){
                code.addAll( move( 1, (Literal) ret.var, ret));
            }

            // Save global variables
            for( var sym : cfg.getSymbols().keySet() ) {
                if( sym.globalLoc != -1 ) {
                    var symvar = new Variable(sym);
                    if( !registers.containsKey(symvar) ) {
                        if( do_print )
                            System.out.printf("Global variable %s not live for function return %s\n", sym, cfg.func);
                        continue;
                    }

                    if( sym.type().getDims() != null ) continue;

                    int dest = getDest(symvar);

                    if( do_print )
                        System.out.printf("Global variable %s store to GLOBL[%d] from reg %d\n", sym, sym.globalLoc, dest);

                    if( dest == SPILL_DEST ) {
                        int loc = registers.get(symvar);
                        code.add( DLXCode.immediateOp(DLXCode.OPCODE.LDW, dest, FRAME_PTR, 4 * loc, ret));
                    }

                    code.add( DLXCode.immediateOp(DLXCode.OPCODE.STW, dest, GLOB_VAR, -1 * sym.globalLoc, ret ));

                }
            }

            // Restore RA to R31
            code.add( DLXCode.regOp(DLXCode.OPCODE.LDX, 31, FRAME_PTR, 0, ret ) );

            // Restore SP and FP
            // old SP is at [SP + 1] and old FP is at [SP]
            code.add( DLXCode.immediateOp(DLXCode.OPCODE.LDW, STACK_PTR, FRAME_PTR, 8, ret) );

            code.add( DLXCode.immediateOp(DLXCode.OPCODE.LDW, FRAME_PTR, FRAME_PTR, 4, ret) );

            // Issue Return
            code.add( DLXCode.regOp(DLXCode.OPCODE.RET, 0, 0, 31, ret));

            return code;
        }
    }

    @Override
    public List<DLXCode> visit(Literal lit) {
        return null;
    }

    @Override
    public List<DLXCode> visit(Call call) {

        // TODO: Generate Code To Save Registers and Whatnot

        // Default Function Name
        switch( call.function.name() ) {
            case "printInt" -> {
                return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.WRI, 0, getLeft(call.args.get(0)), 0, call));
            }
            case "println" -> {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.WRL, 0, 0, 0, call));
            }
            case "printBool" -> {
                return List.of( DLXCode.regOp(DLXCode.OPCODE.WRB, 0, getLeft(call.args.get(0)), 0, call) );
            }

            case "readInt" -> {
                return List.of( DLXCode.regOp( DLXCode.OPCODE.RDI, getDest(call.dest), 0, 0, call) );
            }

            case "readBool" -> {
                return List.of( DLXCode.regOp( DLXCode.OPCODE.RDB, getDest(call.dest), 0, 0, call) );
            }
        }

        List<DLXCode> callCode = new ArrayList<>();

        int numSaved = 0;

        // Save Each Register
        // for( int i = 1; i <= numSavedRegisters; i++ ) {
        //     callCode.add( DLXCode.immediateOp(DLXCode.OPCODE.STW, i, FRAME_PTR, -1 * 4 * (numSpills+i)) );
        // }
        // Save all variable live past this point
        int saveno = 1;
        for( var sym : call.liveAfterPP ) {
            if( !call.liveBeforePP.contains(sym) ) continue; // If var becomes live at return, don't save

            int dest = registers.get(sym);
            sym.saveLocation = saveno + numSpills;
            boolean isArray = false;

            // If variable is global, no need to save twice
            if( sym instanceof Variable ) {
                Variable var = (Variable) sym;
                if( var.getSym().type().getDims() != null ) {
                   isArray = true;
                   callCode.add( DLXCode.immediateOp(DLXCode.OPCODE.STW, dest, FRAME_PTR, -4 * sym.saveLocation, call));
                }
                else {
                    if (cfg.getSymbols().get(var.getSym()).globalLoc != -1) continue;
                }
            }

            saveno++;
            numSaved++;

            if( ! isArray )
                callCode.add( DLXCode.immediateOp(DLXCode.OPCODE.STW, dest, FRAME_PTR, -4 * sym.saveLocation, call) );
        }

        // Save Globals
        for( var sym : cfg.getSymbols().keySet() ) {
            if( sym.globalLoc != -1 ) {
                var symvar = new Variable(sym);
                if( !registers.containsKey(symvar) ) {
                    if( do_print )
                        System.out.printf("Global variable %s not live for function %s\n", sym, cfg.func);
                    continue;
                }
                if( sym.type().getDims() != null ) continue;
                // if( !((FunctionSymbol)call.function).getCfg().useCounts.containsKey(symvar) ) {
                //     if( do_print )
                //         System.out.printf("Global variable %s is not used by function %s\n", sym, cfg.func);
                //     continue;
                // }

                int dest = registers.get(symvar);
                if( dest <= -1 ) {
                    int loc = dest;
                    dest = SPILL_DEST;
                    callCode.add( DLXCode.immediateOp(DLXCode.OPCODE.LDW, dest, FRAME_PTR, 4 * loc, call ));
                }


                if( do_print )
                    System.out.printf("Global variable %s load from GLOBL[%d] to reg %d\n", sym, sym.globalLoc, dest);
                callCode.add( DLXCode.immediateOp(DLXCode.OPCODE.STW, dest, GLOB_VAR, -1 * sym.globalLoc, call ));
                numSaved++;
            }
        }

        // Save the current SP and FP
        callCode.add( DLXCode.immediateOp(DLXCode.OPCODE.STW, STACK_PTR, FRAME_PTR, -1 * 4 * (saveno + numSpills + 1), call) );
        callCode.add( DLXCode.immediateOp(DLXCode.OPCODE.STW, FRAME_PTR, FRAME_PTR, -1 * 4 * (saveno + numSpills + 2), call) );

        // Set the new SP and FP
        callCode.add( DLXCode.immediateOp(DLXCode.OPCODE.SUBI, STACK_PTR, FRAME_PTR, 4 * (saveno + numSpills + 2), call) );
        callCode.add( DLXCode.immediateOp(DLXCode.OPCODE.SUBI, FRAME_PTR, STACK_PTR, 4, call)); // TODO: Stack spilled args

        // Set the arguments on the stack
        for ( int arg = 0; arg < call.args.size(); arg++ ) {
            int srcReg = getDest(call.args.get(arg));
            callCode.add( DLXCode.immediateOp(DLXCode.OPCODE.STW, srcReg, FRAME_PTR, -4 * (arg + 1), call ) );
            // if( srcReg != (arg + 1) ) {
            //     callCode.addAll( move(arg+1, srcReg) );
            // }
        }

        // Issue jump
        callCode.add(DLXCode.unresolvedCall(DLXCode.OPCODE.JSR, ((FunctionSymbol)call.function).typeSignatures(), call));

        int returnReg = -1;
        // Have Return?
        if( !((FunctionSymbol) call.function).getRealReturnType().equals(new VoidType()) ) {
            // Save Return to proper variable
            if( registers.containsKey(call.dest) ){
                returnReg = registers.get(call.dest);
                if( returnReg != -1 ) {
                    callCode.addAll( move(returnReg, 1, call) );
                }
                else {
                    throw new RuntimeException("Store return value to spill");
                }
            }
            else {
                throw new RuntimeException("Function return not in register allocation?");
            }
        }

        // Restore Saved Variables
        for( var sym : call.liveAfterPP ) {
            if( !call.liveBeforePP.contains(sym) ) continue; // If var becomes live at return, don't restore

            int dest = registers.get(sym);
            if( returnReg != -1 && dest == returnReg ) continue;

            // If variable is global, no need to restore twice
            if( sym instanceof Variable ) {
                Variable var = (Variable) sym;
                if( var.getSym().type().getDims() == null ) {
                    if (cfg.getSymbols().get(var.getSym()).globalLoc != -1) continue;
                }
            }

            callCode.add( DLXCode.immediateOp(DLXCode.OPCODE.LDW, dest, FRAME_PTR, -4 * sym.saveLocation, call) );
        }

        // Restore Global Variables
        for( var sym : cfg.getSymbols().keySet() ) {
            if( sym.globalLoc != -1 ) {
                var symvar = new Variable(sym);
                if( !registers.containsKey(symvar) ) {
                    if( do_print )
                        System.out.printf("Global variable %s not live for function %s\n", sym, cfg.func);
                    continue;
                }

                if( !call.liveBeforePP.contains(symvar) ) continue;

                if( !((FunctionSymbol)call.function).getCfg().useCounts.containsKey(symvar) ) {
                    if( do_print )
                        System.out.printf("Global variable %s is not used by function %s\n", sym, cfg.func);
                    // continue;
                }

                if( sym.type().getDims() != null ) continue;

                int dest = getDest(symvar);
                if( returnReg != -1 && dest == returnReg ) continue;
                if( do_print )
                    System.out.printf("Global variable %s load from GLOBL[%d] to reg %d\n", sym, sym.globalLoc, dest);

                if( sym.type().getDims() != null ) {
                    callCode.add(DLXCode.immediateOp(DLXCode.OPCODE.SUBI, dest, GLOB_VAR, sym.globalLoc, call));
                }
                else {
                    callCode.add(DLXCode.immediateOp(DLXCode.OPCODE.LDW, dest, GLOB_VAR, -1 * sym.globalLoc, call));
                }
            }
        }




        return callCode;
    }

    @Override
    public List<DLXCode> visit(Variable var) {
        return null;
    }

    @Override
    public List<DLXCode> visit(Add add) {
        int dest = getDest(add.dest);
        if( add.hasImmediate() ) {
            boolean lit_lhs = add.left instanceof Literal,
                    lit_rhs = add.right instanceof Literal;
            if( lit_rhs && lit_lhs ) {
                return List.of(
                        DLXCode.immediateOp(DLXCode.OPCODE.ADDI, SPILL_LHS, 0, ((Literal) add.left).getInt(), add),
                        DLXCode.immediateOp(DLXCode.OPCODE.ADDI, dest, SPILL_LHS, ((Literal) add.right).getInt(), add)
                );
            }else if( lit_lhs ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ADDI, dest, getRight(add.right), ((Literal) add.left).getInt(), add));
            }
            else {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ADDI, dest, getLeft(add.left), ((Literal) add.right).getInt(), add));
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.ADD, dest, getLeft(add.left), getRight(add.right), add));
    }

    @Override
    public List<DLXCode> visit(Assign asn) {
        throw new RuntimeException("Cannot generate ASM by polymorphic assign! : " + asn );
    }

    @Override
    public List<DLXCode> visit(Div div) {
        int dest = getDest(div.dest);
        if( div.hasImmediate() ) {
            boolean lit_lhs = div.left instanceof Literal,
                    lit_rhs = div.right instanceof Literal;
            if( lit_rhs && lit_lhs ) {
                return List.of(
                        move( SPILL_LHS, (Literal) div.left, div).get(0),
                        DLXCode.immediateOp(DLXCode.OPCODE.DIVI, dest, SPILL_LHS, ((Literal) div.right).getInt(), div)
                );
            }else if( lit_rhs ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.DIVI, dest, getLeft(div.left), ((Literal) div.right).getInt(), div));
            }
            else {
                return List.of(
                        move( SPILL_LHS, (Literal) div.left, div).get(0),
                        DLXCode.regOp(DLXCode.OPCODE.DIV, dest, SPILL_LHS, getRight(div.right), div)
                );
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.DIV, dest, getLeft(div.left), getRight(div.right), div));
    }

    @Override
    public List<DLXCode> visit(Mod mod) {
        int dest = getDest(mod.dest);
        if( mod.hasImmediate() ) {
            boolean lit_lhs = mod.left instanceof Literal,
                    lit_rhs = mod.right instanceof Literal;
            if( lit_rhs && lit_lhs ) {
                return List.of(
                        move( SPILL_LHS, (Literal) mod.left, mod).get(0),
                        DLXCode.immediateOp(DLXCode.OPCODE.MODI, dest, SPILL_LHS, ((Literal) mod.right).getInt(), mod)
                );
            }else if( lit_rhs ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.MODI, dest, getLeft(mod.left), ((Literal) mod.right).getInt(), mod));
            }
            else {
                return List.of(
                        move( SPILL_LHS, (Literal) mod.left, mod).get(0),
                        DLXCode.regOp(DLXCode.OPCODE.MOD, dest, SPILL_LHS, getRight(mod.right), mod)
                );
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.MOD, dest, getLeft(mod.left), registers.get(mod.right), mod));
    }

    @Override
    public List<DLXCode> visit(Mul mul) {
        int dest = getDest(mul.dest);
        if( dest <= -1 )
            dest = SPILL_DEST;

        if( mul.hasImmediate() ) {
            boolean lit_lhs = mul.left instanceof Literal,
                    lit_rhs = mul.right instanceof Literal;
            if( lit_rhs && lit_lhs ) {
                return List.of(
                        move( SPILL_LHS, (Literal) mul.left, mul).get(0),
                        DLXCode.immediateOp(DLXCode.OPCODE.MULI, dest, SPILL_LHS, ((Literal) mul.right).getInt(), mul)
                );
            }else if( lit_lhs ) {
                int rhs = getRight(mul.right);
                if( rhs <= -1 )
                    rhs = SPILL_RHS;
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.MULI, dest, rhs, ((Literal) mul.left).getInt(), mul));
            }
            else {
                int lhs = getLeft(mul.left);
                if( lhs <= -1 )
                    lhs = SPILL_LHS;
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.MULI, dest, lhs, ((Literal) mul.right).getInt(), mul));
            }
        }

        int lhs = getLeft(mul.left);
        if( lhs <= -1 )
            lhs = SPILL_LHS;
        int rhs = getRight(mul.right);
        if( rhs <= -1 )
            rhs = SPILL_RHS;

        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.MUL, dest, lhs, rhs, mul));
    }

    @Override
    public List<DLXCode> visit(Sub sub) {
        int dest = getDest(sub.dest);
        if( dest <= -1 ) dest = SPILL_DEST;
        if( sub.hasImmediate() ) {
            boolean lit_lhs = sub.left instanceof Literal,
                    lit_rhs = sub.right instanceof Literal;
            if( lit_rhs && lit_lhs ) {
                return List.of(
                        move( SPILL_LHS, (Literal) sub.left, sub).get(0),
                        DLXCode.immediateOp(DLXCode.OPCODE.SUBI, dest, SPILL_LHS, ((Literal) sub.right).getInt(), sub)
                );
            }else if( lit_rhs ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.SUBI, dest, getLeft(sub.left), ((Literal) sub.right).getInt(), sub));
            }
            else {
                return List.of(
                        move( SPILL_LHS, (Literal) sub.left, sub).get(0),
                        DLXCode.regOp(DLXCode.OPCODE.SUB, dest, SPILL_LHS, getRight(sub.right), sub)
                );
            }
        }
        int lhs = getLeft(sub.left);
        if( lhs <= -1 ) lhs = SPILL_LHS;
        int rhs = getRight(sub.right);
        if( rhs <= -1 ) rhs = SPILL_RHS;

        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.SUB, dest, getLeft(sub.left), getRight(sub.right), sub));
    }

    @Override
    public List<DLXCode> visit(LoadStack lstack) {
        if( lstack.isLoadSpill() ) {
            int dest = registers.get(lstack.dest);
            if( dest <= -1 ) {
                dest = lstack.loc.reg.num;
            }
            return List.of( DLXCode.immediateOp(DLXCode.OPCODE.LDW, dest, FRAME_PTR, -4 * lstack.loc.spillNo, lstack));
        }
        else {
            if( lstack.dest.equals(lstack.target) ) {
                int dest = registers.get(lstack.dest);
                int loc = dest;
                if (dest <= -1) { // throw new RuntimeException("Load stack into spill???: " + lstack.errMsg());
                    loc = dest * -1;
                    dest = (lstack.regHint == null ) ? SPILL_DEST : lstack.regHint.num;
                }
                return List.of(DLXCode.immediateOp(DLXCode.OPCODE.LDW, dest, FRAME_PTR, -4 * loc, lstack));
            }

            int dest = registers.get(lstack.dest);
            int loc = registers.get(lstack.target);
            if( loc <= -1 ){
                System.out.printf("Load destination is spilt... %s\n", lstack);
            }
            if (dest <= -1) { // throw new RuntimeException("Load stack into spill???: " + lstack.errMsg());
                loc = dest * -1;
                dest = (lstack.regHint == null ) ? SPILL_DEST : lstack.regHint.num;
            }
            return List.of(DLXCode.immediateOp(DLXCode.OPCODE.LDW, dest, FRAME_PTR, -4 * loc, lstack));
        }
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
        //if( !labels.containsKey(dest) ) {
        if( bra.isConditional() ) {
            return List.of(DLXCode.unresolvedBranch(opcode, getDest((Assignable) bra.getVal()), dest, bra));
        }
        else {
            return List.of(DLXCode.unresolvedBranch(opcode, 0, dest, bra));
        }
        // }
        // else {
        //     dest = labels.get(dest) - instrnum;
        //     if( !opcode.equals(DLXCode.OPCODE.BSR) ) {
        //         return List.of(DLXCode.immediateOp(opcode, registers.get((Assignable) bra.getVal()), 0, dest));
        //     }
        //     else {
        //         return List.of(DLXCode.immediateOp(opcode, 0, 0, dest));
        //     }
        // }
    }

    @Override
    public List<DLXCode> visit(Cmp cmp) {

        //
        // NOTE! The LHS and RHS are flipped here for a reason:
        // The CMP instruction performs R.b - R.c, followed
        //

        int dest = getDest(cmp.dest);
        if( cmp.hasImmediate() ) {
            boolean lit_lhs = cmp.left instanceof Literal,
                    lit_rhs = cmp.right instanceof Literal;
            if( lit_rhs && lit_lhs ) {
                return List.of(
                        move( SPILL_LHS, (Literal) cmp.left, cmp).get(0),
                        DLXCode.immediateOp(DLXCode.OPCODE.CMPI, dest, SPILL_LHS, ((Literal) cmp.right).getInt(), cmp)
                );
            }else if( lit_rhs ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.CMPI, dest, getLeft(cmp.left), ((Literal) cmp.right).getInt(), cmp));
            }
            else {
                return List.of(
                        move( SPILL_LHS, (Literal) cmp.left, cmp).get(0),
                        DLXCode.regOp(DLXCode.OPCODE.CMP, dest, SPILL_LHS, getRight(cmp.right), cmp)
                );
            }
        }
        if( dest <= -1 ) {
            throw new RuntimeException("Unresolved spill: " + cmp.genDot() );
        }

        int lhs = getLeft(cmp.left);
        if( lhs <= -1 )
            lhs = SPILL_LHS;

        int rhs = getRight(cmp.right);
        if( rhs <= -1 )
            rhs = SPILL_RHS;

        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.CMP, dest, lhs, rhs, cmp));
    }

    @Override
    public List<DLXCode> visit(Store store) {
        int dest = getDest(store.dest);
        if( store.source instanceof Literal ) {
            // DEST = R0(always 0) + literal
            return move( dest, (Literal) store.source, store);
        }

        List<DLXCode> code = new ArrayList<>(2);

        int srcReg = registers.get((Assignable) store.source);
        if( srcReg <= -1 && dest == SPILL_DEST ) { // Move should be taken care of by StoreStack
            return Collections.emptyList();
        }
        code.addAll( move( dest, srcReg, store) );
        return code;
    }

    @Override
    public List<DLXCode> visit(Load load) {
        List<DLXCode> code = new ArrayList<>();

        int dest = getDest( load.dest );

        int base = registers.get( load.base );
        if( base <= -1 ) {
            // throw new RuntimeException("Load from spilled array ptr");
            int loc = base;
            base = SPILL_LHS;
            code.add( DLXCode.immediateOp(DLXCode.OPCODE.LDW, base, FRAME_PTR, 4 * loc, load));
        }

        int offset = registers.get( load.offset );
        if( offset <= -1 ) {
            // throw new RuntimeException("Array pointer load spilled offset");
            int loc = offset;
            offset = SPILL_RHS;
            code.add( DLXCode.immediateOp(DLXCode.OPCODE.LDW, offset, FRAME_PTR, 4 * loc, load));
        }

        code.add( DLXCode.regOp(DLXCode.OPCODE.LDX, dest, base, offset, load));

        return code;

    }

    @Override
    public List<DLXCode> visit(StoreStack sstack) {
        if (sstack.isArray()) {
            List<DLXCode> code = new ArrayList<>();
            int base = registers.get(sstack.dest);
            int src;
            int offset;
            if( base <= -1 ) {
                // Array case
                if( sstack.dest instanceof Variable
                    && ((VariableSymbol) ((Variable) sstack.dest).getSym()).type().getDims() != null
                    && ((VariableSymbol) ((Variable) sstack.dest).getSym()).isGlobal ) {
                    base = SPILL_LHS;
                    Variable var = (Variable) sstack.dest;
                    code.add( DLXCode.immediateOp(DLXCode.OPCODE.SUBI, base, GLOB_VAR, ((VariableSymbol) var.getSym()).globalLoc, sstack));
                }
                else {
                    code.add(DLXCode.immediateOp(DLXCode.OPCODE.LDW, SPILL_LHS, FRAME_PTR, 4 * base, sstack));
                    base = SPILL_LHS;
                }
            }

            if( sstack.src instanceof  Literal ) {
                code.add( DLXCode.immediateOp(DLXCode.OPCODE.ADDI, SPILL_DEST, 0, ((Literal) sstack.src).getInt(), sstack) );
                src = SPILL_DEST;
            }
            else {
                src = registers.get((Assignable) sstack.src);
                if( src <= -1 ) {
                    // throw new RuntimeException("Store spilled reg to array ptr on stack");
                    int loc = src;
                    src = SPILL_RHS;
                    code.add( DLXCode.immediateOp(DLXCode.OPCODE.LDW, src, FRAME_PTR, 4 * loc, sstack));
                }
            }

            offset = getRight( sstack.offset );

            code.add( DLXCode.regOp(DLXCode.OPCODE.STX, src, base, offset, sstack) );

            return code;
            // int offset;
            // if(sstack.src instanceof Literal) {
            //     // initing to 0 case
            //     offset = registers.get(sstack.offset);
            //     if (sstack.isArray()) {
            //         return List.of(DLXCode.regOp(DLXCode.OPCODE.STX, 0, GLOB_VAR, offset, sstack));
            //     }
            // }
            // if(sstack.offset instanceof Literal){
            //     offset = ((Literal) sstack.offset).getInt();
            //     if(sstack.isArray()){
            //         return List.of( DLXCode.immediateOp(DLXCode.OPCODE.STW, 0, GLOB_VAR, offset, sstack) );
            //     }
            // }else{
            //     offset = registers.get(sstack.offset);
            // }

            // int src = registers.get(sstack.src);
            // if(sstack.offset.isGlobal) {
            //     return List.of( DLXCode.regOp(DLXCode.OPCODE.STX, src, GLOB_VAR, offset, sstack) );
            // }
            // return List.of( DLXCode.regOp(DLXCode.OPCODE.STX, src, FRAME_PTR, offset, sstack) );
        }
        int loc = registers.get(sstack.dest);
        return List.of( DLXCode.immediateOp(DLXCode.OPCODE.STW, sstack.loc.reg.num, FRAME_PTR, 4 * loc, sstack) );

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
                    move( SPILL_DEST, Literal.get(1), not ).get(0),
                    DLXCode.immediateOp(DLXCode.OPCODE.SUBI, getDest(not.dest), SPILL_DEST, ((Literal) not.src).getInt(), not )
            );
        }
        return List.of(
                move( SPILL_DEST, Literal.get(1), not).get(0),
                DLXCode.regOp(DLXCode.OPCODE.SUB, getDest(not.dest), SPILL_DEST, registers.get((Assignable) not.src), not )
        );
    }

    @Override
    public List<DLXCode> visit(And and) {
        int dest = getDest(and.dest);
        if( and.hasImmediate() ) {
            boolean lit_lhs = and.left instanceof Literal,
                    lit_rhs = and.right instanceof Literal;
            if( lit_rhs && lit_lhs ) {
                return List.of(
                        move( SPILL_LHS, (Literal) and.left, and).get(0),
                        DLXCode.immediateOp(DLXCode.OPCODE.ANDI, dest, SPILL_LHS, ((Literal) and.right).getInt(), and)
                );
            }else if( lit_lhs ) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ANDI, dest, getRight(and.right), ((Literal) and.left).getInt(), and));
            }
            else {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ANDI, dest, getLeft(and.left), ((Literal) and.right).getInt(), and));
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.AND, dest, getLeft(and.left), getRight(and.right), and));
    }

    @Override
    public List<DLXCode> visit(Or or) {
        int dest = getDest(or.dest);
        if (or.hasImmediate()) {
            boolean lit_lhs = or.left instanceof Literal,
                    lit_rhs = or.right instanceof Literal;
            if (lit_rhs && lit_lhs) {
                return List.of(
                        move( SPILL_LHS, (Literal) or.left, or).get(0),
                        DLXCode.immediateOp(DLXCode.OPCODE.ORI, dest, SPILL_LHS, ((Literal) or.right).getInt(), or)
                );
            } else if (lit_lhs) {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ORI, dest, getRight(or.right), ((Literal) or.left).getInt(), or));
            } else {
                return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.ORI, dest, getLeft(or.left), ((Literal) or.right).getInt(), or));
            }
        }
        return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.OR, dest, getLeft(or.left), getRight(or.right), or));
    }

    @Override
    public List<DLXCode> visit(Xor xor) {
        int dest = getDest(xor.dest);
        boolean lit_lhs = xor.left instanceof Literal,
                lit_rhs = xor.right instanceof Literal;
        if( lit_rhs && lit_lhs ) {
            return List.of(
                    move( SPILL_LHS, (Literal) xor.left, xor).get(0),
                    DLXCode.immediateOp(DLXCode.OPCODE.XORI, dest, SPILL_LHS, ((Literal) xor.right).getInt(), xor)
            );
        }else if( lit_lhs ) {
            return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.XORI, dest, getRight(xor.right), ((Literal) xor.left).getInt(), xor));
        }
        else if( lit_rhs ) {
            return Collections.singletonList(DLXCode.immediateOp(DLXCode.OPCODE.XORI, dest, getLeft(xor.left), ((Literal) xor.right).getInt(), xor));
        }
        else {
            return Collections.singletonList(DLXCode.regOp(DLXCode.OPCODE.XOR, dest, getLeft(xor.left), getRight(xor.right), xor));
        }
    }

    @Override
    public List<DLXCode> visit(Lsh lsh) {
        List<DLXCode> code = new ArrayList<>();
        int dest = getDest(lsh.dest);
        if( dest <= -1 ) {
            dest = SPILL_DEST;
        }
        int lhs;
        int rhs;

        if( lsh.hasImmediate() ) {
            boolean lit_lhs = lsh.left instanceof Literal;
            boolean lit_rhs = lsh.right instanceof Literal;

            if( lit_rhs && lit_lhs ) {
                lhs = SPILL_LHS;
                rhs = SPILL_RHS;

                int exp = ((Literal) lsh.right).getInt();
                code.addAll( move( SPILL_LHS, (Literal) lsh.left, lsh) );
                code.add( DLXCode.immediateOp(DLXCode.OPCODE.LSHI, dest, lhs, exp, lsh));

            }
            else if( lit_rhs ) {
                lhs = registers.get((Assignable) lsh.left);
                rhs = SPILL_RHS;

                int exp = ((Literal) lsh.right).getInt();

                code.add( DLXCode.immediateOp(DLXCode.OPCODE.LSHI, dest, lhs, exp, lsh));

            }
            else {
                lhs = SPILL_LHS;
                rhs = registers.get((Assignable) lsh.right);

                int base = ((Literal) lsh.left).getInt();

                code.addAll( move( SPILL_LHS, (Literal) lsh.left, lsh) );
                code.add( DLXCode.regOp(DLXCode.OPCODE.LSH, dest, lhs, rhs, lsh));

            }
        }
        else {
            lhs = getLeft(lsh.left);
            if( lhs < 0 ) lhs = SPILL_LHS;
            rhs = getRight(lsh.right);
            if( rhs < 0 ) rhs = SPILL_RHS;

            code.add( DLXCode.regOp(DLXCode.OPCODE.LSH, dest, lhs, rhs, lsh));
        }
        return code;
    }

    @Override
    public List<DLXCode> visit(Ash ash) {
        List<DLXCode> code = new ArrayList<>();
        int dest = getDest(ash.dest);
        if( dest <= -1 ) {
            dest = SPILL_DEST;
        }
        int lhs;
        int rhs;

        if( ash.hasImmediate() ) {
            boolean lit_lhs = ash.left instanceof Literal;
            boolean lit_rhs = ash.right instanceof Literal;

            if( lit_rhs && lit_lhs ) {
                lhs = SPILL_LHS;
                rhs = SPILL_RHS;

                int exp = ((Literal) ash.right).getInt();
                code.addAll( move( SPILL_LHS, (Literal) ash.left, ash) );
                code.add( DLXCode.immediateOp(DLXCode.OPCODE.ASHI, dest, lhs, exp, ash));

            }
            else if( lit_rhs ) {
                lhs = registers.get((Assignable) ash.left);
                rhs = SPILL_RHS;

                int exp = ((Literal) ash.right).getInt();

                code.add( DLXCode.immediateOp(DLXCode.OPCODE.ASHI, dest, lhs, exp, ash));

            }
            else {
                lhs = SPILL_LHS;
                rhs = registers.get((Assignable) ash.right);

                int base = ((Literal) ash.left).getInt();

                code.addAll( move( SPILL_LHS, (Literal) ash.left, ash) );
                code.add( DLXCode.regOp(DLXCode.OPCODE.ASH, dest, lhs, rhs, ash));

            }
        }
        else {
            lhs = getLeft(ash.left);
            if( lhs < 0 ) lhs = SPILL_LHS;
            rhs = getRight(ash.right);
            if( rhs < 0 ) rhs = SPILL_RHS;

            code.add( DLXCode.regOp(DLXCode.OPCODE.ASH, dest, lhs, rhs, ash));
        }
        return code;
    }

    @Override
    public List<DLXCode> visit(Pow pow) {
        List<DLXCode> code = new ArrayList<>();
        int dest = getDest(pow.dest);
        if( dest <= -1 ) {
            dest = SPILL_DEST;
        }
        int lhs;
        int rhs;

        if( pow.hasImmediate() ) {
            boolean lit_lhs = pow.left instanceof Literal;
            boolean lit_rhs = pow.right instanceof Literal;

            if( lit_rhs && lit_lhs ) {
                lhs = SPILL_LHS;
                rhs = SPILL_RHS;

                int exp = ((Literal) pow.right).getInt();
                code.addAll( move( SPILL_LHS, (Literal) pow.left, pow) );
                code.add( DLXCode.immediateOp(DLXCode.OPCODE.POWI, dest, lhs, exp, pow));

            }
            else if( lit_rhs ) {
                lhs = registers.get((Assignable) pow.left);
                rhs = SPILL_RHS;

                int exp = ((Literal) pow.right).getInt();

                code.add( DLXCode.immediateOp(DLXCode.OPCODE.POWI, dest, lhs, exp, pow));

            }
            else {
                lhs = SPILL_LHS;
                rhs = registers.get((Assignable) pow.right);

                code.addAll( move( SPILL_LHS, (Literal) pow.left, pow) );
                code.add( DLXCode.regOp(DLXCode.OPCODE.POW, dest, lhs, rhs, pow));

            }
        }
        else {
            lhs = getLeft(pow.left);
            if( lhs < 0 ) lhs = SPILL_LHS;
            rhs = getRight(pow.right);
            if( rhs < 0 ) rhs = SPILL_RHS;

            code.add( DLXCode.regOp(DLXCode.OPCODE.POW, dest, lhs, rhs, pow));
        }
        return code;
    }
}

