package f.defuncCPS;

import java.util.NoSuchElementException;

import util.List;
import f.defuncCPS.IntArithmetic.Operation;

public class Interpreter {
  
  static void interpret (Program p, Environment env) {
    evaluate(p.e, env, new Cprint());
  }
  
  static Continuation cont(Continuation c, Value v) {
    if (c instanceof CArithInner) {
      CArithInner k = (CArithInner) c;

      IntValue op2 = (IntValue) v;
      Value result;
      switch (k.arith.op) {
        case PLUS:
          result = new IntValue(k.op1.javaInt + op2.javaInt);
          break;
        case MINUS:
          result = new IntValue(k.op1.javaInt - op2.javaInt);
          break;
        case MULT:
          result = new IntValue(k.op1.javaInt * op2.javaInt);
          break;
        case DIV:
          result = new IntValue(k.op1.javaInt / op2.javaInt);
          break;
        default:
          throw new Error();
      }

      return cont(k.c, result);
    } else if (c instanceof CArithOuter) {
      CArithOuter k = (CArithOuter) c;

      IntValue op1 = (IntValue) v;
      return evaluate(k.arith.operand2, k.env, new CArithInner(k.arith, op1, k.c));
    } else if (c instanceof CComparisonOuter) {
      CComparisonOuter k = (CComparisonOuter) c;
      IntValue op1 = (IntValue) v;

      return evaluate(k.intComp.operand2, k.env, new CComparisonInner(op1, k.c));
    } else if (c instanceof CComparisonInner) {
      CComparisonInner k = (CComparisonInner) c;

      IntValue op2 = (IntValue) v;
      return cont(k.c, new BoolValue(k.op1.javaInt == op2.javaInt));
    } else if (c instanceof CIf) {
      CIf k = (CIf) c;
      BoolValue guard = (BoolValue) v;
      if (guard.javaBool)
        return evaluate(k.ifE.t, k.env, k.c);
      else
        return evaluate(k.ifE.f, k.env, k.c);
    } else if (c instanceof CLambdaInner) {
      CLambdaInner k = (CLambdaInner) c;
      return apply(k.lambda.cl, v, k.c);
    } else if (c instanceof CLambdaOuter) {
      CLambdaOuter k = (CLambdaOuter) c;
        
      LambdaValue lambda = (LambdaValue) v;
      return evaluate(k.lambdaApp.argument, k.env, new CLambdaInner(lambda, k.c));
    } else if (c instanceof Cprint) {
      System.out.println(v);
      return null;
    }

    throw new Error();
  }
  
  static Continuation apply (Closure c, Value argument, Continuation k) {
    if (c instanceof CL1) {
      CL1 cl1 = (CL1) c;

      Environment newEnv = cl1.env.bind(cl1.lambdaDef.formalArgument, argument);

      return evaluate(cl1.lambdaDef.body, newEnv, k);
    }
    
    throw new Error();
  }
  
  static Continuation evaluate (Expression e, final Environment env, final Continuation c) {
    if (e instanceof IntConstant) {
      IntConstant intE = (IntConstant) e;

      return cont(c, new IntValue(intE.javaInt));
    } else if (e instanceof BoolConstant) {
      BoolConstant boolE = (BoolConstant) e;

      return cont(c, new BoolValue(boolE.javaBool));
    } else if (e instanceof IntArithmetic) {
      final IntArithmetic arith = (IntArithmetic) e;

      return evaluate(arith.operand1, env, new CArithOuter(arith, c, env));
        
    } else if (e instanceof IntComparison) {
      IntComparison intComp = (IntComparison) e;
      
      return evaluate(intComp.operand1, env, new CComparisonOuter(intComp, c, env));
    } else if (e instanceof If) {
      final If ifE = (If) e;
      
      return evaluate(ifE.guard, env, new CIf(ifE, c, env));
      
    } else if (e instanceof Variable) {
      Variable v = (Variable) e;
      
      return cont(c, env.lookup(v));
    } else if (e instanceof LambdaDef) {
      LambdaDef lambdaDef = (LambdaDef) e;
      
      return cont(c, new LambdaValue(new CL1(lambdaDef, env)));
    } else if (e instanceof LambdaApp) {
      LambdaApp lambdaApp = (LambdaApp) e;

      return evaluate(lambdaApp.lambda, env, new CLambdaOuter(lambdaApp, c, env));
    }
    
    throw new Error();
  }

  public static void main(String[] args) {
    // Simple lambda definition and application
    interpret (new Program(
        new LambdaApp(
            new LambdaDef(
                new Variable("x"),
                new IntArithmetic(
                    Operation.PLUS,
                      new Variable("x"),
                      new Variable("y"))),
            new IntConstant(3))),
        Environment.EMPTY.bind(new Variable("y"), new IntValue(3)));
  }

}

// Continuations

class Continuation {
}

class CArithInner extends Continuation {
  final IntArithmetic arith;
  final IntValue op1;
  final Continuation c;

  public CArithInner(IntArithmetic arith, IntValue op1, Continuation c) {
    this.arith = arith;
    this.op1 = op1;
    this.c = c;
  }
}

class CArithOuter extends Continuation {
  final IntArithmetic arith;
  final Continuation c;
  final Environment env;

  public CArithOuter(IntArithmetic arith, Continuation c, Environment env) {
    this.arith = arith;
    this.c = c;
    this.env = env;
  }
}

class CComparisonInner extends Continuation {
  final IntValue op1;
  final Continuation c;

  public CComparisonInner(IntValue op1, Continuation c) {
    this.op1 = op1;
    this.c = c;
  }
}

class CComparisonOuter extends Continuation {
  final IntComparison intComp;
  final Continuation c;
  final Environment env;

  public CComparisonOuter(IntComparison intComp, Continuation c, Environment env) {
    this.intComp = intComp;
    this.c = c;
    this.env = env;
  }
}

class CIf extends Continuation {
  final If ifE;
  final Environment env;
  final Continuation c;

  public CIf(If ifE, Continuation c, Environment env) {
    this.ifE = ifE;
    this.c = c;
    this.env = env;
  }
}

class CLambdaOuter extends Continuation {
  final LambdaApp lambdaApp;
  final Continuation c;
  final Environment env;
  
  public CLambdaOuter(LambdaApp lambdaApp, Continuation c, Environment env) {
    this.lambdaApp = lambdaApp;
    this.c = c;
    this.env = env;
  }
}

class CLambdaInner extends Continuation {
  final LambdaValue lambda;
  final Continuation c;
  
  public CLambdaInner(LambdaValue lambda, Continuation c) {
    this.lambda = lambda;
    this.c = c;
  }
}

class Cprint extends Continuation {
}

// Closures

class Closure {
}

class CL1 extends Closure {
  final LambdaDef lambdaDef;
  final Environment env;

  public CL1(LambdaDef lambdaDef, Environment env) {
    this.lambdaDef = lambdaDef;
    this.env = env;
  }
}

// Environments

class Bind {
	final Variable var;
	final Value	 val;

	public Bind(Variable var, Value val) {
		this.var = var;
		this.val = val;
	}
}

class Environment {
  public static Environment EMPTY = new Environment(List.EMPTY);
	private List binds;

	private Environment(List binds) { this.binds = binds; }
	
	public Environment bind(Variable va, Value vv) {
	  return new Environment(binds.add(new Bind(va,vv)));
	}

	public Value lookup(Variable v) {
		return lookup(v, binds);
	}

	public Value lookup(Variable v, List rest) {
		if (rest == null)
			throw new NoSuchElementException();

		Bind b = (Bind) rest.el;

		if (v.name.equals(b.var.name))
			return b.val;
		else
			return lookup(v, rest.rest);
	}
}

// Expressions

class Expression {
}

class IntConstant extends Expression {
  final int javaInt;

  public IntConstant(int javaInt) {
    this.javaInt = javaInt;
  }
}

class BoolConstant extends Expression {
  final boolean javaBool;

  public BoolConstant(boolean javaBool) {
    this.javaBool = javaBool;
  }
}

class IntArithmetic extends Expression {
  enum Operation { PLUS, MINUS, MULT, DIV }
  
  final Operation op;
  final Expression operand1, operand2;
  
  public IntArithmetic(Operation op, Expression operand1, Expression operand2) {
    this.op = op;
    this.operand1 = operand1;
    this.operand2 = operand2;
  }
}

class IntComparison extends Expression {
  final Expression operand1, operand2;
  
  public IntComparison(Expression operand1, Expression operand2) {
    this.operand1 = operand1;
    this.operand2 = operand2;
  }
}

class If extends Expression {
  final Expression guard, t, f;

  public If(Expression guard, Expression t, Expression f) {
    this.guard = guard;
    this.t = t;
    this.f = f;
  }
}

class Variable extends Expression {
  final String name;

  public Variable(String name) {
    this.name = name;
  }
}

class LambdaDef extends Expression {
  final Variable formalArgument;
  final Expression body;

  public LambdaDef(Variable formalArgument, Expression body) {
    this.formalArgument = formalArgument;
    this.body = body;
  }
}

class LambdaApp extends Expression {
  final Expression lambda;
  final Expression argument;

  public LambdaApp(Expression lambda, Expression argument) {
    this.lambda = lambda;
    this.argument = argument;
  }
}

// Values

class Value {
}

class IntValue extends Value {
  final int javaInt;

  public IntValue(int javaInt) {
    this.javaInt = javaInt;
  }

  @Override
  public String toString() {
    return "" + this.javaInt;
  }
}

class BoolValue extends Value {
  final boolean javaBool;

  public BoolValue(boolean javaBool) {
    this.javaBool = javaBool;
  }

  @Override
  public String toString() {
    return "" + this.javaBool;
  }
  
}

interface JavaLambda { public Object l(Object arg); }

class LambdaValue extends Value {
  final Closure cl;

  public LambdaValue(Closure cl) {
    this.cl = cl;
  }
}

class Program {
  final Expression e;

  public Program(Expression e) {
    this.e = e;
  }
}