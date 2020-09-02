/*
Copyright Ben M. Sutter 2020

This file is part of Novlangue.

Novlangue is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Novlangue is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Novlangue.  If not, see <https://www.gnu.org/licenses/>.
*/
grammar Novlangue;

top
   : (e | s)+
   ;

s
   : fun_def # FunDef
   | val_def # ValDef
   | val_dec # ValDec
   | assignment # Assign
   | if_block # IfBlock
   | while_loop # While
   ;

e
   : fun_call # FunCall
   | '-' e # NegExpr
   | left = e op = ('*' | '/' | '%') right = e # BinExpr
   | left = e op = ('+' | '-') right = e # BinExpr
   | '(' e ')' # ParenExpr
   | val = FLOAT # FloatNumber
   | val = NUM # IntNumber
   | val = STRING # String
   | name = ID # Identifier
   ;

fun_def
   : 'fun' name = ID '(' (names += ID ':' types += ID (',' names += ID ':' types += ID)*)? ')' (':' type = ID)? '{' top+ '}'
   ;

fun_call
   : name = ID '(' (args += e (',' args += e)*)? ')'
   ;

val_dec
   : 'val' name = ID (':' type = ID)?
   ;

val_def
   : name = val_dec '=' val = e
   ;

assignment
   : name = ID '=' val = e
   ;

comparison
   : left = e op = ('==' | '!=' | '>' | '<' | '>=' | '<=') right = e
   ;

if_block
   : if_statement else_if_statement* else_statement?
   ;

if_statement
   : 'if' '(' comparison ')' '{' top+ '}'
   ;

else_if_statement
   : 'else' if_statement
   ;

else_statement
   : 'else' '{' top+ '}'
   ;

while_loop
   : 'while' '(' comparison ')' '{' top+ '}'
   ;

STRING
   : '"' ('\\"' | .)*? '"'
   ;

FLOAT
   : NUM ('.' DIGIT+)
   ;
   //fragment HEXNUM: '0x'[a-fA-F0-9]+;
   
   //fragment BINNUM: '0b'[0-1]+;
   
   //fragment OCTNUM: '0o'[0-7]+;
   
NUM
   : DIGIT+ /*| OCTNUM | BINNUM | HEXNUM*/
   
   
   ;

OP_ADD
   : '+'
   ;

OP_SUB
   : '-'
   ;

OP_MUL
   : '*'
   ;

OP_DIV
   : '/'
   ;

OP_MOD
   : '%'
   ;

fragment DIGIT
   : [0-9]+
   ;

ID
   : [a-zA-Z_]+
   ;

COMMENT
   : '/*' .*? '*/' -> skip
   ;

LINE_COMMENT
   : '//' .*? '\n' -> skip
   ;

WS
   : [ \n\t\r]+ -> skip
   ;

