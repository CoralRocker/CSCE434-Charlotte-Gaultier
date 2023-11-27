package ir.tac;

import ir.cfg.BasicBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;


public class UsedInBlock extends TACVisitor<List<Assignable>> {

    public List<Assignable> used = new ArrayList<>();

    public static List<Assignable> usedInBlock(BasicBlock blk) {
        ir.tac.UsedInBlock visitor = new ir.tac.UsedInBlock();

        for( TAC tac : blk.getInstructions() ) {
            List<Assignable> vars = tac.accept(visitor);
            if( vars != null )
                visitor.used.addAll(vars);
        }

        return visitor.used;
    }

    public static List<Assignable> usedInInstr(TAC instr) {
        ir.tac.UsedInBlock visitor = new ir.tac.UsedInBlock();
        List<Assignable> vars = instr.accept(visitor);
        if( vars != null )
            visitor.used.addAll(vars);

        return visitor.used;
    }

    @Override
    public List<Assignable> visit(Return ret) {
        return emptyList();
    }

    @Override
    public List<Assignable> visit(Literal lit) {
        return emptyList();
    }

    @Override
    public List<Assignable> visit(Call call) {
        // TODO add function parameters to list of used variables here
        return emptyList();
    }

    @Override
    public List<Assignable> visit(Variable var) {
        return List.of(var);
    }

    @Override
    public List<Assignable> visit(Add add) {
        Stream<Assignable> ret = Stream.concat((add.left.accept(this)).stream(), add.right.accept(this).stream());
        return ret.toList();
    }

    @Override
    public List<Assignable> visit(Assign asn) {
        Stream<Assignable> ret = Stream.concat((asn.left.accept(this)).stream(), asn.right.accept(this).stream());
        return ret.toList();
    }

    @Override
    public List<Assignable> visit(Div div) {
        Stream<Assignable> ret = Stream.concat((div.left.accept(this)).stream(), div.right.accept(this).stream());
        return ret.toList();
    }

    @Override
    public List<Assignable> visit(Mod mod) {
        Stream<Assignable> ret = Stream.concat((mod.left.accept(this)).stream(), mod.right.accept(this).stream());
        return ret.toList();
    }

    @Override
    public List<Assignable> visit(Mul mul) {
        Stream<Assignable> ret = Stream.concat((mul.left.accept(this)).stream(), mul.right.accept(this).stream());
        return ret.toList();
    }

    @Override
    public List<Assignable> visit(Sub sub) {
        Stream<Assignable> ret = Stream.concat((sub.left.accept(this)).stream(), sub.right.accept(this).stream());
        return ret.toList();
    }

    @Override
    public List<Assignable> visit(LoadStack lstack) {
        return null;
    }

    @Override
    public List<Assignable> visit(Branch bra) {
        return emptyList();
    }

    @Override
    public List<Assignable> visit(Cmp cmp) {
        Stream<Assignable> ret = Stream.concat((cmp.left.accept(this)).stream(), cmp.right.accept(this).stream());
        return ret.toList();
    }

    @Override
    public List<Assignable> visit(Store store) {
        return store.source.accept(this);
    }

    @Override
    public List<Assignable> visit(StoreStack sstack) {
        return null;
    }

    @Override
    public List<Assignable> visit(Phi phi) {
        return emptyList();
    }

    @Override
    public List<Assignable> visit(Temporary temporary) {
        return emptyList();
    }

    @Override
    public List<Assignable> visit(Not not) {
        return not.src.accept(this);
    }

    @Override
    public List<Assignable> visit(And and) {
        Stream<Assignable> ret = Stream.concat((and.left.accept(this)).stream(), and.right.accept(this).stream());
        return ret.toList();
    }

    @Override
    public List<Assignable> visit(Or or) {
        Stream<Assignable> ret = Stream.concat((or.left.accept(this)).stream(), or.right.accept(this).stream());
        return ret.toList();
    }
}


