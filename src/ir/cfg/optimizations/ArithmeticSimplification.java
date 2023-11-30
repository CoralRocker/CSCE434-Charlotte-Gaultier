package ir.cfg.optimizations;

import ir.cfg.BasicBlock;
import ir.tac.*;

public class ArithmeticSimplification implements TACVisitor<TAC> {

    public static boolean MathSimplify(BasicBlock blk) {
        boolean changed = false;

        ArithmeticSimplification visitor = new ArithmeticSimplification();

        int ctr = -1;
        var iter = blk.getInstructions().listIterator();
        while( iter.hasNext() ) {
            ctr++;
            TAC instr = iter.next();
            TAC ntac = instr.accept(visitor);
            if( ntac != null ) {
                iter.set(ntac);
                changed = true;
            }
        }

        return changed;
    }

    @Override
    public TAC visit(Return ret) {
        return null;
    }

    @Override
    public TAC visit(Literal lit) {
        return null;
    }

    @Override
    public TAC visit(Call call) {
        return null;
    }

    @Override
    public TAC visit(Variable var) {
        return null;
    }

    @Override
    public TAC visit(Add add) {
        if( add.left instanceof Literal ) {
            int val = ((Literal) add.left).getInt();

            if( val == 0 ) {
                return new Store(add.getIdObj(), add.dest, add.right);
            }
        }
        if( add.right instanceof Literal ) {
            int val = ((Literal) add.right).getInt();

            if( val == 0 ) {
                return new Store(add.getIdObj(), add.dest, add.left);
            }
        }
        return null;
    }

    @Override
    public TAC visit(Assign asn) {
        return null;
    }

    @Override
    public TAC visit(Div div) {
        if( div.left instanceof Literal ) {
            int val = ((Literal) div.left).getInt();

            if( val == 0 ) {
                return new Store(div.getIdObj(), div.dest, div.left);
            }
        }
        if( div.right instanceof Literal ) {
            int val = ((Literal) div.right).getInt();

            if( val ==  1 ) {
                return new Store(div.getIdObj(), div.dest, div.left);
            }
        }

        if( div.right instanceof Assignable && div.left instanceof Assignable ) {
            if( div.right.equals(div.left) ) {
                return new Store(div.getIdObj(), div.dest, Literal.get(1));
            }
        }
        return null;
    }

    @Override
    public TAC visit(Mod mod) {
        return null;
    }

    @Override
    public TAC visit(Mul mul) {
        if( mul.left instanceof Literal ) {
            int val = ((Literal) mul.left).getInt();

            if( val == 1 ) {
                return new Store(mul.getIdObj(), mul.dest, mul.right);
            }
            else if( val == 0 ) {
                return new Store(mul.getIdObj(), mul.dest, mul.left);
            }
        }
        if( mul.right instanceof Literal ) {
            int val = ((Literal) mul.right).getInt();

            if( val == 1 ) {
                return new Store(mul.getIdObj(), mul.dest, mul.left);
            }
            else if( val == 0 ) {
                return new Store(mul.getIdObj(), mul.dest, mul.right);
            }
        }
        return null;
    }

    @Override
    public TAC visit(Sub sub) {
        if( sub.right instanceof Literal ) {
            int val = ((Literal) sub.right).getInt();

            if( val == 0 ) {
                return new Store(sub.getIdObj(), sub.dest, sub.left);
            }
        }

        if( sub.right instanceof Assignable && sub.left instanceof Assignable ) {
            if( sub.right.equals(sub.left) ) {
                return new Store(sub.getIdObj(), sub.dest, Literal.get(0));
            }
        }
        return null;
    }

    @Override
    public TAC visit(LoadStack lstack) {
        return null;
    }

    @Override
    public TAC visit(Branch bra) {
        return null;
    }

    @Override
    public TAC visit(Cmp cmp) {
        return null;
    }

    @Override
    public TAC visit(Store store) {
        return null;
    }

    @Override
    public TAC visit(Load load) {
        return null;
    }

    @Override
    public TAC visit(StoreStack sstack) {
        return null;
    }

    @Override
    public TAC visit(Phi phi) {
        return null;
    }

    @Override
    public TAC visit(Temporary temporary) {
        return null;
    }

    @Override
    public TAC visit(Not not) {
        // TODO
        return null;
    }

    @Override
    public TAC visit(And and) {
        // TOOD
        return null;
    }

    @Override
    public TAC visit(Or or) {
        // TODO
        return null;
    }
}
