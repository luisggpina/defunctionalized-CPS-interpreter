package e.CPS;

import java.util.NoSuchElementException;

import util.List;
import e.CPS.IntArithmetic.Operation;

public class Interpreter {
  
  static void interpret (Program p, Environment env) {
    evaluate(p.e, env, new Continuation() {
      @Override
      public Continuation cont(Value v) {
        System.out.println(v);
        return null;
      }
    });
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

      return c.cont(new IntValue(intE.javaInt));
    } else if (e instanceof BoolConstant) {
      BoolConstant boolE = (BoolConstant) e;

      return c.cont(new BoolValue(boolE.javaBool));
    } else if (e instanceof IntArithmetic) {
      final IntArithmetic arith = (IntArithmetic) e;

      return evaluate(arith.operand1, env, new Continuation() {
        
        @Override
        public Continuation cont(Value v) {
          final IntValue op1 = (IntValue) v;
          return evaluate(arith.operand2, env, new Continuation() {

            @Override
            public Continuation cont(Value v) {
              IntValue op2 = (IntValue) v;
              Value result;
              switch (arith.op) {
                case PLUS:
                  result = new IntValue(op1.javaInt + op2.javaInt);
                  break;
                case MINUS:
                  result = new IntValue(op1.javaInt - op2.javaInt);
                  break;
                case MULT:
                  result = new IntValue(op1.javaInt * op2.javaInt);
                  break;
                case DIV:
                  result = new IntValue(op1.javaInt / op2.javaInt);
                  break;
                default:
                  throw new Error();
              }
              
              return c.cont(result);
            }
          });
        }
      });
      
    } else if (e instanceof IntComparison) {
      final IntComparison intComp = (IntComparison) e;
      
      return evaluate(intComp.operand1, env, new Continuation() {
        
        @Override
        public Continuation cont(Value v) {
          final IntValue op1 = (IntValue) v;
          return evaluate(intComp.operand2, env, new Continuation() {

            @Override
            public Continuation cont(Value v) {
              IntValue op2 = (IntValue) v;
              return c.cont(new BoolValue(op1.javaInt == op2.javaInt));
            }

          });
        }

      });

      
    } else if (e instanceof If) {
      final If ifE = (If) e;
      
      return evaluate(ifE.guard, env, new Continuation() {
        
        @Override
        public Continuation cont(Value v) {
          BoolValue guard = (BoolValue) v;
          if (guard.javaBool)
            return evaluate(ifE.t, env, c);
          else
            return evaluate(ifE.f, env, c);
        }

      });
      
    } else if (e instanceof Variable) {
      Variable v = (Variable) e;
      
      return c.cont(env.lookup(v));
    } else if (e instanceof LambdaDef) {
      LambdaDef lambdaDef = (LambdaDef) e;
      
      return c.cont(new LambdaValue(new CL1(lambdaDef, env)));
    } else if (e instanceof LambdaApp) {
      final LambdaApp lambdaApp = (LambdaApp) e;

      return evaluate(lambdaApp.lambda, env, new Continuation() {
        
        @Override
        public Continuation cont(Value v) {
          final LambdaValue lambda = (LambdaValue) v;
          return evaluate(lambdaApp.argument, env, new Continuation() {

            @Override
            public Continuation cont(Value v) {
              return apply(lambda.cl, v, c);
            }

          });
        }

      });
      
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

    // Lambdas are first-order that capture state
    // Where is the bug here?
//    interpret (new Program(
//        new LambdaApp(
//            new Variable("l"),
//            new IntConstant(3))),
//            Environment.EMPTY
//              .bind(new Variable("y"), new IntValue(3))
//              .bind(new Variable("l"),
//                evaluate(
//                    new LambdaDef(
//                        new Variable("x"),
//                        new IntArithmetic(
//                            Operation.PLUS,
//                            new Variable("x"),
//                            new Variable("y"))),
//                    Environment.EMPTY)));
  }

}

// Continuations

interface Continuation {
  Continuation cont(Value v);
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