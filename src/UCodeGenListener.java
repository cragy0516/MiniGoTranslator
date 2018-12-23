import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

public class UCodeGenListener extends MiniGoBaseListener {
	ParseTreeProperty<String> ucodes;
	ArrayList<HashMap<String, Integer>> variables;
	int localNum;
	int funcNum;
	int labelNum;
	int globalNum;
	
	public UCodeGenListener() {
		ucodes = new ParseTreeProperty<String>();
		variables = new ArrayList<HashMap<String, Integer>>();
		variables.add(new HashMap<String, Integer>());
		variables.add(new HashMap<String, Integer>());
		localNum = 1;
		globalNum = 1;
		funcNum = 2;
		labelNum = 0;
	}
	
	@Override
	public void exitProgram(MiniGoParser.ProgramContext ctx) {
		StringBuffer buf = new StringBuffer();
		
		// �Լ� �� ���� ���� ����
		// ���� ������ ���� �����ؾ� ���� ������?
		/*
		 * To-do : �������� ���� ����
		 * ���������� ���� ��ġ�� �Լ� ������ �ϸ� �ذ�� �� ���� ��.
		 */
		for (int i=0; i<ctx.decl().size(); i++) {
			if(ctx.decl().get(i).fun_decl() != null) {
				
				buf.append(ucodes.get(ctx.decl().get(i)));
			}
		}
		
		// ���������� ������ 0�̶� bgn ���ڰ� 0��.
		// �������� ���� ��� �߰��ϸ� ���⵵ �߰��ؾ� ��.
		buf.append(makeUcode("", "bgn", "" + (globalNum-1)));
		
		for (int i=0; i<ctx.decl().size(); i++) {
			if(ctx.decl().get(i).var_decl() != null) {
				buf.append(ucodes.get(ctx.decl().get(i)));
			}
		}
		
		// main �Լ� ����
		buf.append(makeUcode("", "ldp", null));
		buf.append(makeUcode("", "call", "main"));
		buf.append(makeUcode("", "end", null));
		
		// ��� ��� �� ���� ���� (result.uco)
		System.out.println(buf.toString());
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("result.uco"));
			writer.write(buf.toString());
			writer.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}
	
	// �ǵ帮�� �� ��
	@Override
	public void exitStmt (MiniGoParser.StmtContext ctx) {
		ucodes.put(ctx, ucodes.get(ctx.getChild(0)));
	}
	
	@Override
	public void exitExpr_stmt (MiniGoParser.Expr_stmtContext ctx) {
		ucodes.put(ctx, ucodes.get(ctx.getChild(0)));
	}
	
	@Override
	public void exitDecl (MiniGoParser.DeclContext ctx) {
		ucodes.put(ctx,  ucodes.get(ctx.getChild(0)));
	}
	
	@Override
	public void exitParam (MiniGoParser.ParamContext ctx) {
		//ucodes.put(ctx, ucodes.get(ctx.getChild(0)));
		StringBuffer buf = new StringBuffer();
		
		if (ctx.getChildCount() == 1 || ctx.getChildCount() == 2) {
			variables.get(funcNum).put(ctx.IDENT().getText(), localNum);
			localNum ++;
		} else {
			// ���� ���̽�
			System.out.println("no");
		}
		ucodes.put(ctx, buf.toString());
	}
	
	@Override
	public void exitParams (MiniGoParser.ParamsContext ctx) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<ctx.getChildCount(); i++) {
			buf.append(ucodes.get(ctx.getChild(i)));
		}
		ucodes.put(ctx, buf.toString());
	}
	// �������
	
	// expression
	@Override
	public void exitExpr (MiniGoParser.ExprContext ctx) {
		String s1 = null, s2 = null, op = null;
		StringBuffer buf = new StringBuffer();

		if(isArrayExpression(ctx)) {
			// Array ������ ���
			String localName = ctx.IDENT().getText();
			
			buf.append(ucodes.get(ctx.expr(0)));
			
			if (isVariableLocal(localName)) {
				// ���� ����
				Integer location = variables.get(funcNum).get(localName);
				buf.append(makeUcode("", "lda", funcNum + " " + location));
			} else {
				// ���� ����
				Integer location = variables.get(0).get(localName);
				buf.append(makeUcode("", "lda", 1 + " " + location));
			}

			buf.append(makeUcode("", "add", null));
			buf.append(makeUcode("", "ldi", null));
			
		} else if (isFunctionCallExpression(ctx)) {
		
			// �Լ� ȣ�� ǥ����
			// ���� ��� (ldp)
			buf.append(makeUcode("", "ldp", null));
			
			// ucodes���� ���ڸ� �����ͼ� �ٿ�����
			// ���ڰ� �������� ��츦 �����ؼ� i�� 2�� �����ؾ���
			for (int i=0; i<ctx.args().getChildCount(); i=i+2) {	
				buf.append(ucodes.get(ctx.args().getChild(i)));
			}
			
			// call�� �̿��� ȣ��
			buf.append(makeUcode("", "call", ctx.IDENT().getText()));
		} else if (isLoopExpression(ctx)) {
			// �ݺ� ��
			String localName = ctx.expr(0).IDENT().getText();
			
			buf.append(ucodes.get(ctx.expr(0)));

			switch (ctx.op.getText()) {
			case "++":
				buf.append(makeUcode("", "inc", null));
				break;
			case "--":
				buf.append(makeUcode("", "dec", null));
				break;
			default:
				System.out.println("operation error!");
				System.exit(0);
			}
			
			if (isVariableLocal(localName)) {
				// ���� ����
				Integer location = variables.get(funcNum).get(localName);
				buf.append(makeUcode("", "str", funcNum + " " + location));
			} else {
				// ���� ����
				Integer location = variables.get(0).get(localName);
				buf.append(makeUcode("", "str", 1 + " " + location));
			}
			
		} else if (isPrefixOperation(ctx)) {
			// example : '++8', '-3', '!true'
			/*
			 * To-do : add some unable prefix operations
			 */
			op = ctx.op.getText();
			s1 = ucodes.get(ctx.expr(0));
			switch (op) {
			case "-":
				buf.append(makeUcode("", "neg", s1));
				break;
			case "+":
				buf.append(s1);
				break;
			case "--":
				buf.append(makeUcode("", "dec", s1));
				break;
			case "++":
				buf.append(makeUcode("", "inc", s1));
				break;
			case "!":
				buf.append(makeUcode("", "not", s1));
				break;
			}
		} else if (isBinaryOperation(ctx)) {
			// example : '1 + 3'
			s1 = ucodes.get(ctx.expr(0));
			op = ctx.op.getText();
			s2 = ucodes.get(ctx.expr(1));
			switch (op) {
			case "+":
				buf.append(s1);
				buf.append(s2);
				buf.append(makeUcode("", "add", null));
				break;
			case "-":
				buf.append(s1);
				buf.append(s2);
				buf.append(makeUcode("", "sub", null));
				break;
			case "*":
				buf.append(s1);
				buf.append(s2);
				buf.append(makeUcode("", "mult", null));
				break;
			case "/":
				buf.append(s1);
				buf.append(s2);
				buf.append(makeUcode("", "div", null));
				break;
			case "%":
				buf.append(s1);
				buf.append(s2);
				buf.append(makeUcode("", "mod", null));
				break;
			case ">":
				buf.append(s1);
				buf.append(s2);
				buf.append(makeUcode("", "gt", null));
				break;
			case "<":
				buf.append(s1);
				buf.append(s2);
				buf.append(makeUcode("", "lt", null));
				break;
			case "==":
				buf.append(s1);
				buf.append(s2);
				buf.append(makeUcode("", "eq", null));
				break;
			case "!=":
				buf.append(s1);
				buf.append(s2);
				buf.append(makeUcode("", "ne", null));
				break;
			case ">=":
				buf.append(s1);
				buf.append(s2);
				buf.append(makeUcode("", "ge", null));
				break;
			case "<=":
				buf.append(s1);
				buf.append(s2);
				buf.append(makeUcode("", "le", null));
				break;
			}
			
		} else if (isAssignmentExpression(ctx)) {
			// '=' ����
			// example : 'A = 2'
			
			String localName = ctx.IDENT().getText();
			buf.append(ucodes.get(ctx.expr(0)));
			
			if (isVariableLocal(localName)) {
				// ���� ����
				Integer location = variables.get(funcNum).get(localName);
				buf.append(makeUcode("", "str", funcNum + " " + location));	
			} else {
				// ���� ����
				Integer location = variables.get(0).get(localName);
				buf.append(makeUcode("", "str", "1 " + location));	
			}
			
		} else if(isArrayAssignment(ctx)) {
			//�迭 '=' ����
			// b[5] = 100
			
			String localName = ctx.IDENT().getText();
			
			buf.append(ucodes.get(ctx.expr(0)));
			
			if (isVariableLocal(localName)) {
				// ���� ����
				Integer location = variables.get(funcNum).get(localName);
				buf.append(makeUcode("", "lda", funcNum + " " + location));
				buf.append(makeUcode("", "add", null));
			} else {
				// ���� ����
				Integer location = variables.get(0).get(localName);
				buf.append(makeUcode("", "lda", 1 + " " + location));
				buf.append(makeUcode("", "add", null));
			}
			
			buf.append(ucodes.get(ctx.expr(1)));
			buf.append(makeUcode("", "sti", null));
			
		
		} else if (isLiteralExpression(ctx)) {
			// Literal�� ���
			buf.append(makeUcode("", "ldc", ctx.LITERAL().getText()));
			
		} else if (isIdentExpression(ctx)) {
			// IDENT�� ���
			String localName = ctx.IDENT().getText();
			
			if (isVariableLocal(localName)) {
				// ���� ����
				Integer location = variables.get(funcNum).get(localName);
				buf.append(makeUcode("", "lod", funcNum + " " + location));	
			} else {
				// ���� ����
				Integer location = variables.get(0).get(localName);
				buf.append(makeUcode("", "lod", "1 " + location));	
			}
			
			// ���̺��� �ε��ؼ� append
			// ���� ã�� ���ϸ�?
			
			/*
			 * To-do : read �Լ��� ȣ���ϸ� ldp �ؾ� ��
			 * ��, ������ �ּҷ� �Ѱ��ִ� �κ� ���� �ʿ�.
			 * �迭�� ���ڷ� �ѱ�� �κа� ����.
			 */
		} else {
			//System.out.println("To-do");
			//ucodes.put(ctx, ctx.getText());
		}
		ucodes.put(ctx, buf.toString());
	}
	
	@Override
	public void exitVar_decl(MiniGoParser.Var_declContext ctx) {
		/*
		 * To-do : global variable
		 * ���� ������ �����ϴ� �κ�
		 * ���� ���� ����θ� �����ϸ� ����
		 * variables (hash table list)���� 0��°�� ���� ���� ���̺�
		 */
		int tmp_size = 1;
		StringBuffer buf = new StringBuffer();
		
		if (isInitDeclaration(ctx)) {
			buf.append(makeUcode("", "sym", "1 " + globalNum + " " + tmp_size));
			buf.append(makeUcode("", "ldc", ctx.LITERAL().getText()));
			buf.append(makeUcode("", "str", "1 " + globalNum));
			variables.get(0).put(ctx.IDENT().getText(), globalNum);
		} else if (isArrayDeclaration(ctx)) {
			tmp_size = Integer.parseInt(ctx.getChild(3).toString());
			buf.append(makeUcode("", "sym", "1 " + globalNum + " " + tmp_size));
			variables.get(0).put(ctx.IDENT().getText(), globalNum);
		} else {
			buf.append(makeUcode("", "sym", "1 " + globalNum + " " + tmp_size));
			variables.get(0).put(ctx.IDENT().getText(), globalNum);
		}
		globalNum += tmp_size;
		
		ucodes.put(ctx, buf.toString());
	}
	
	@Override
	public void exitLocal_decl(MiniGoParser.Local_declContext ctx) {
		int tmp_size = 1;
		StringBuffer buf = new StringBuffer();
		
		/*
		 * To-do : revise the following rules
		 * only 1 block can be accepted
		 * only constant can be accpeted for init declaration
		 * array cannot be accepted
		 */
		if (isInitDeclaration(ctx)) {
			buf.append(makeUcode("", "sym", funcNum + " " + localNum + " " + tmp_size));
			buf.append(makeUcode("", "ldc", ctx.LITERAL().getText()));
			buf.append(makeUcode("", "str", funcNum + " " + localNum));
			variables.get(funcNum).put(ctx.IDENT().getText(), localNum);
		} else if (isArrayDeclaration(ctx)) {
			tmp_size = Integer.parseInt(ctx.getChild(3).toString());
			buf.append(makeUcode("", "sym", funcNum + " " + localNum + " " + tmp_size));
			variables.get(funcNum).put(ctx.IDENT().getText(), localNum);
		} else {
			buf.append(makeUcode("", "sym", funcNum + " " + localNum + " " + tmp_size));
			variables.get(funcNum).put(ctx.IDENT().getText(), localNum);
		}
		localNum += tmp_size;
		
		ucodes.put(ctx, buf.toString());
	}
	
	@Override
	public void enterFun_decl(MiniGoParser.Fun_declContext ctx) {
		// �������� ����
		localNum = 1;
		
		// �Լ� ��ȣ�� ������Ű��, variable table�� ������Ŵ.
		variables.add(new HashMap<String, Integer>());
		
	}
	
	@Override
	public void exitFun_decl(MiniGoParser.Fun_declContext ctx) {
		StringBuffer buf = new StringBuffer();
		String funcName = ctx.getChild(1).getText();
		String funcContent = null;
	
		if (ucodes.get(ctx.compound_stmt()) == null) {
			funcContent = "";
		} else {
			funcContent = ucodes.get(ctx.compound_stmt());
		}
		
		// param�� ���� ucode�� �ٿ����� �ʿ䰡 ����.
		//buf.append(params);
		buf.append(makeUcode(funcName, "proc", (localNum-1) + " " + funcNum + " 2"));
		buf.append(funcContent);
		
		//System.out.println(localNum);
		
		buf.append(makeUcode("", "end", null));
		ucodes.put(ctx, buf.toString());
	}
	
	@Override
	public void exitIf_stmt(MiniGoParser.If_stmtContext ctx) {
		String s1 = null, s2 = null, s3 = null;
		StringBuffer buf = new StringBuffer();

		s1 = ucodes.get(ctx.expr()); // condition
		s2 = ucodes.get(ctx.stmt(0).compound_stmt()); // stmt
		
		buf.append(s1);
		int end_point = labelNum;
		
		if (isElseThere(ctx)) {
			int else_point = labelNum;
			end_point = labelNum + 1;
			s3 = ucodes.get(ctx.stmt(1).compound_stmt());
			
			buf.append(makeUcode("", "fjp", "$" + else_point));
			buf.append(s2);
			buf.append(makeUcode("", "ujp", "$" + end_point));
			buf.append(makeUcode("$" + (labelNum++), "nop", null)); // else point
			buf.append(s3);
			buf.append(makeUcode("$" + (labelNum++), "nop", null)); // end point
		} else {
			buf.append(makeUcode("", "fjp", "$" + end_point));
			buf.append(s2);
			buf.append(makeUcode("$" + (labelNum++), "nop", null));
		}
		ucodes.put(ctx, buf.toString());
	}
	
	@Override
	public void exitFor_stmt(MiniGoParser.For_stmtContext ctx) {
		String s1 = null, s2 = null;
		StringBuffer buf = new StringBuffer();
		
		int start_loop = labelNum;
		int end_loop = labelNum + 1;
		
		
		if (ctx.loop_expr() != null) {
			buf.append(ucodes.get(ctx.loop_expr().start));
		}
		buf.append(makeUcode("$"+(labelNum++), "nop", null));
		
		if (ctx.loop_expr() != null) {
			s1 = ucodes.get(ctx.loop_expr());
		} else {
			s1 = ucodes.get(ctx.expr());
		}
		buf.append(s1);
		buf.append(makeUcode("", "fjp", "$" + end_loop));
		
		if (ctx.stmt() != null) {
			s2 = ucodes.get(ctx.stmt());
			buf.append(s2);
		}
		
		if (ctx.loop_expr() != null) {
			buf.append(ucodes.get(ctx.loop_expr().loop_expression));
		}
		
		buf.append(makeUcode("", "ujp", "$" + start_loop));
		buf.append(makeUcode("$"+(labelNum++), "nop", null)); // end of loop
		
		ucodes.put(ctx, buf.toString());
	}
	
	@Override
	public void exitLoop_expr(MiniGoParser.Loop_exprContext ctx) {
		StringBuffer buf = new StringBuffer();

		//buf.append(ucodes.get(ctx.expr(0))); // ���� ����
		buf.append(ucodes.get(ctx.expr(1))); // ���� ����
		//buf.append(ucodes.get(ctx.loop_condition()));
		
		ucodes.put(ctx, buf.toString());
	}
	
	@Override
	public void exitArgs(MiniGoParser.ArgsContext ctx) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<ctx.expr().size(); i++) {
			buf.append(ucodes.get(ctx.expr(i)));
		}
		ucodes.put(ctx, buf.toString());
	}
	
	@Override
	public void enterCompound_stmt(MiniGoParser.Compound_stmtContext ctx) {
		
	}
	
	@Override
	public void exitCompound_stmt(MiniGoParser.Compound_stmtContext ctx) {
		StringBuffer buf = new StringBuffer();

		for (int i=0; i<ctx.getChildCount(); i++) {
			if (ucodes.get(ctx.getChild(i)) != null) {
				buf.append(ucodes.get(ctx.getChild(i)));
			}
		}

		ucodes.put(ctx, buf.toString());
	}
	
	@Override
	public void exitReturn_stmt(MiniGoParser.Return_stmtContext ctx) {
		StringBuffer buf = new StringBuffer();
		if (ctx.getChildCount() == 1) {
			buf.append(makeUcode("", "ret", null));
		} else if (ctx.getChildCount() == 2) {
			String localName = ctx.expr(0).getText();
			Integer location = variables.get(funcNum).get(localName);
			buf.append(makeUcode("", "lod", funcNum + " " + location));
			buf.append(makeUcode("", "retv", null));
		} else if (ctx.getChildCount() == 3) {
			/*
			 * To-do : return 2 value
			 */
		}
		
		ucodes.put(ctx, buf.toString());
	}
	
	private boolean isBinaryOperation(MiniGoParser.ExprContext ctx) {
		return ctx.getChildCount() == 3 && ctx.getChild(1) != ctx.expr(0) && !ctx.getChild(1).getText().equals("=");
	}
	private boolean isPrefixOperation(MiniGoParser.ExprContext ctx) {
		return ctx.getChildCount() == 2 && ctx.getStart() == ctx.op;
	}
	private boolean isBracketExpression(MiniGoParser.ExprContext ctx) {
		return ctx.getChildCount() == 3 && ctx.getChild(1) == ctx.expr(0);
	}
	private boolean isAssignmentExpression(MiniGoParser.ExprContext ctx) {
		return (ctx.getChildCount() == 3 && ctx.getChild(1).getText().equals("="));
	}
	private boolean isLiteralExpression(MiniGoParser.ExprContext ctx) {
		return ((ctx.getChildCount() == 1) && (ctx.LITERAL() != null));
	}
	private boolean isIdentExpression(MiniGoParser.ExprContext ctx) {
		return ((ctx.getChildCount() == 1) && (ctx.IDENT() != null));
	}
	private boolean isFunctionCallExpression(MiniGoParser.ExprContext ctx) {
		return ((ctx.getChildCount() == 4) && 
				(ctx.getChild(1).getText().equals("(")) && 
				(ctx.getChild(3).getText().equals(")")));
	}
	private boolean isInitDeclaration(ParserRuleContext ctx) {
		return ctx.getChildCount() == 5;
	}
	private boolean isArrayDeclaration(ParserRuleContext ctx) {
		return ctx.getChildCount() == 6;
	}
	private boolean isElseThere(MiniGoParser.If_stmtContext ctx) {
		return ctx.getChildCount() != 3;
	}
	private boolean isArrayAssignment(MiniGoParser.ExprContext ctx) {
		return (ctx.getChildCount() == 6 && ctx.getChild(4).getText().equals("="));
	}
	private boolean isArrayExpression(MiniGoParser.ExprContext ctx) {
		return ((ctx.getChildCount() == 4) && 
				(ctx.getChild(1).getText().equals("[")) &&
				(ctx.getChild(3).getText().equals("]")));
	}
	private boolean isLoopExpression(MiniGoParser.ExprContext ctx) {
		return ((ctx.getChildCount() == 2) &&
				(ctx.getStop() == ctx.op));
	}
	
	private boolean isVariableLocal (String var_name) {
		if (funcNum != 0 && variables.get(funcNum).containsKey(var_name)) {
			// local variable
			return true;
		} else if (variables.get(0).containsKey(var_name)) {
			return false;
		} else {
			System.out.println("Undeclared identifier");
			System.exit(0);
			return false;
		}
	}
	
	private String makeUcode (String label, String opcode, String operands) {
		StringBuffer buf = new StringBuffer();
		if (label.length() > 10) {
			System.out.println("writeUcode error : label length must be 10 or lower.");
			return "";
		}
		int wslen = 11 - (label.length());
		buf.append(label); // append label
		for (int i=0; i<wslen; i++) buf.append(" "); // append whitespace
		buf.append(opcode);
		
		if (operands != null) {
			buf.append(" ");
			buf.append(operands);
		}
		
		buf.append("\n");
		
		return buf.toString();
	}
}
