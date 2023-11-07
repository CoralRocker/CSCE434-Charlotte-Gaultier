package ir.tac;

import ir.cfg.BasicBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;


public class UsedInBlock extends TACVisitor<List<Variable>> {

    public List<Variable> used = new ArrayList<>();

    public static List<Variable> usedInBlock(BasicBlock blk) {
        ir.tac.UsedInBlock visitor = new ir.tac.UsedInBlock();

        for( TAC tac : blk.getInstructions() ) {
            List<Variable> vars = tac.accept(visitor);
            if( vars != null )
                visitor.used.addAll(vars);
        }

        return visitor.used;
    }

    @Override
    public List<Variable> visit(Return ret) {
        return emptyList();
    }

    @Override
    public List<Variable> visit(Literal lit) {
        return emptyList();
    }

    @Override
    public List<Variable> visit(Call call) {
        return emptyList();
    }

    @Override
    public List<Variable> visit(Variable var) {
        return List.of(var);
    }

    @Override
    public List<Variable> visit(Add add) {
        Stream<Variable> ret = Stream.concat((add.left.accept(this)).stream(), add.right.accept(this).stream());
        return ret.toList();
    }

    @Override
    public List<Variable> visit(Assign asn) {
        Stream<Variable> ret = Stream.concat((asn.left.accept(this)).stream(), asn.right.accept(this).stream());
        return ret.toList();
    }

    @Override
    public List<Variable> visit(Div div) {
        Stream<Variable> ret = Stream.concat((div.left.accept(this)).stream(), div.right.accept(this).stream());
        return ret.toList();
    }

    @Override
    public List<Variable> visit(Mod mod) {
        Stream<Variable> ret = Stream.concat((mod.left.accept(this)).stream(), mod.right.accept(this).stream());
        return ret.toList();
    }

    @Override
    public List<Variable> visit(Mul mul) {
        Stream<Variable> ret = Stream.concat((mul.left.accept(this)).stream(), mul.right.accept(this).stream());
        return ret.toList();
    }

    @Override
    public List<Variable> visit(Sub sub) {
        Stream<Variable> ret = Stream.concat((sub.left.accept(this)).stream(), sub.right.accept(this).stream());
        return ret.toList();
    }

    @Override
    public List<Variable> visit(Branch bra) {
        return emptyList();
    }

    @Override
    public List<Variable> visit(Cmp cmp) {
        Stream<Variable> ret = Stream.concat((cmp.left.accept(this)).stream(), cmp.right.accept(this).stream());
        return ret.toList();
    }

    @Override
    public List<Variable> visit(Store store) {
        return store.source.accept(this);
    }

    @Override
    public List<Variable> visit(Phi phi) {
        return emptyList();
    }

    @Override
    public List<Variable> visit(Temporary temporary) {
        return emptyList();
    }
}

