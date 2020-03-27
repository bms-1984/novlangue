grammar Orwell;
top:
    s #Stat
    | e #Expr;
s:
    fun_def
    | val_def
    | val_dec
    | assignment;
e:
    '-' e #NegExpr
    | '(' e ')' #ParenExpr
    | left=e op=('*'|'/') right=e #BinExpr
    | left=e op=('+'|'-') right=e #BinExpr
    | val=NUM #Number
    | name=ID #Identifier
    | fun_call #FunCall;
fun_def: 'fun' name=ID ('(' param_list? ')')?;
fun_call: name=ID(('('param_list?')')+ | '?');
param_list: names+=ID ':' types+='double' (',' names+=ID ':' types+='double')*;
val_dec: 'val' name=ID ':' type='double';
val_def: name=val_dec '=' val=e;
assignment: name=ID '=' val=e;

NUM: DIGIT+('.'DIGIT+)?;
OP_ADD: '+';
OP_SUB: '-';
OP_MUL: '*';
OP_DIV: '/';
fragment DIGIT: [0-9]+;
ID: [a-zA-Z_]+;
WS: [ \n\t\r]+ -> skip;
