grammar Orwell;
top:
    (e|s)+;
s:
    fun_def #FunDef
    | val_def #ValDef
    | val_dec #ValDec
    | assignment #Assign
    | if_block #IfBlock
    | while_loop #While;
e:
    '-' e #NegExpr
    | '(' e ')' #ParenExpr
    | left=e op=('*'|'/') right=e #BinExpr
    | left=e op=('+'|'-') right=e #BinExpr
    | val=NUM #Number
    | val=STRING #String
    | fun_call #FunCall
    | name=ID #Identifier;
fun_def: 'fun' name=ID '(' (names+=ID ':' types+=ID
    (',' names+=ID ':' types+=ID)*)? ')' (':' type=ID)?  '{' top* e '}';
fun_call: name=ID '(' (args+=e (',' args+=e)*)? ')';
val_dec: 'val'  name=ID (':' type=ID)?;
val_def: name=val_dec '=' val=e;
assignment: name=ID '=' val=e;
comparison: left=e op=('=='|'!='|'>'|'<'|'>='|'<=') right=e;
if_block: if_statement else_if_statement* else_statement?;
if_statement: 'if' '(' comparison ')' '{' top+ '}';
else_if_statement: 'else' if_statement;
else_statement: 'else' '{' top+ '}';
while_loop: 'while' '(' comparison ')' '{' top+ '}';

STRING : '"' ( '\\"' | . )*? '"';
NUM: DIGIT+('.'DIGIT+)?;
OP_ADD: '+';
OP_SUB: '-';
OP_MUL: '*';
OP_DIV: '/';
fragment DIGIT: [0-9]+;
ID: [a-zA-Z_]+;
COMMENT: '/*' .*? '*/' -> skip;
LINE_COMMENT: '//' .*? '\n' -> skip;
WS: [ \n\t\r]+ -> skip;

