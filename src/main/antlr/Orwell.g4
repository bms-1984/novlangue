grammar Orwell;
top:
    (e|s)+;
s:
    fun_def #FunDef
    | val_def #ValDef
    | val_dec #ValDec
    | assignment #Assign
    | if_block #IfBlock;
e:
    '-' e #NegExpr
    | '(' e ')' #ParenExpr
    | left=e op=('*'|'/') right=e #BinExpr
    | left=e op=('+'|'-') right=e #BinExpr
    | val=NUM #Number
    | fun_call #FunCall
    | name=ID #Identifier;
fun_def: 'fun' name=ID '(' (names+=ID (',' names+=ID)*)? ')'  '{' e '}';
fun_call: name=ID '(' (args+=e (',' args+=e)*)? ')';
val_dec: 'val' name=ID;
val_def: name=val_dec '=' val=e;
assignment: name=ID '=' val=e;
comparison: left=e op=('=='|'!=') right=e;
if_block: if_statement else_statement?;
if_statement: 'if' '(' comparison ')' '{' top+ '}';
//else_if_statement: 'else' if_statement;
else_statement: 'else' '{' top+ '}';

NUM: DIGIT+('.'DIGIT+)?;
OP_ADD: '+';
OP_SUB: '-';
OP_MUL: '*';
OP_DIV: '/';
fragment DIGIT: [0-9]+;
ID: [a-zA-Z_]+;
WS: [ \n\t\r]+ -> skip;
