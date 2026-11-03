// ======================================
// Lexer.flex
//
// @Entrada: Un flujo de caracteres UTF-8
// @Salida: tokens (lib java_cup.runtime.Symbol) para el parser
//   - sym: codigo del terminal (sym.XXX)
//   - left: linea del token (yyline + 1)
//   - right: columna del token (yycolumn + 1)
//   - value: texto del token (yytext())
//   - Los errores lexicos se reportan en el compilador.ErrorReporter
//
// @Restricciones:
//   - Los delimitadores y operadores son caracteres Unicode y no ASCII
//   - El archivo de entrada debe estar en UTF-8
//   - El orden de las reglas respeta la precedencia de los tokens
//
// @Objetivo: Implementar un analizador léxico para el lenguaje de programación definido en la especificación del proyecto
//
// ==========================================

package compilador.generated;

import java_cup.runtime.Symbol;
import compilador.ErrorReporter;

%%

%class Lexer
%public
%unicode
%cup
%line
%column
%xstate COMENTARIO

/*
 * Auxiliares para la construcción de expresiones regulares
*/
%{
    private int lineComentario;
    private int columnComentario;
    private Symbol symbol(int type) {
        return new Symbol(type, yyline + 1, yycolumn + 1, yytext());
    }
%}

/**
 * ========================================
 *              Definiciones
  * ========================================
 */

// Blancos
blanco = [ \t\n\r\f]+

// Digitos
digito = [0-9]
digitos = {digito}*
digitoN = [1-9]
cero = "0"
punto = "."
ceroF = "0.0"

// Identificadores
restoId = [_a-zA-Z0-9]
partId = [_a-zA-Z]
id = {partId} {restoId}*

// Palabras reservadas
val = "val"
if = "if"
elif = "elif"
else = "else"
while = "while"
for = "for"
return = "return"
break = "break"
read = "read"
write = "write"
principal = "principal"
int = "int"
float = "float"
bool = "bool"
char = "char"
void = "void"
string = "string"
true = "true"
false = "false"

// Delimitadores de bloques y sentencias
asign = "Ͱ"
finSentencia = "»"
openLlaves = "¿:"
closeLlaves = ":?"
openBracket = "ʃ:"
closeBracket = ":ʅ"
openParen = "є:"
closeParen = ":э"

// Operadores
opSuma = "+"
opResta = "-"
opMult = "*"
opDivDecimal = "/"
opDivEntera = "//"
opModulo = "mod"
opPotencia = "pot"
opIncremento = "++"
opDecremento = "--"

// Operadores relacionales
opMenor = "<"
opMayor = ">"
opMenorIgual = "<="
opMayorIgual = ">="
opIgual = "=="
opDiferente = "!="

// Operadores lógicos
opAnd = "λ"
opOr = "θ"
opNegacion = "Σ"

// Comentarios
aperturaLinea = "|"
aperturaBloque = "¡"
cierreBloque = "!"
contenidoLinea = [^\r\n]*
comentarioLinea = {aperturaLinea} {contenidoLinea}

// Literales
partEntera = {digitoN} {digitos}
partDecimal = {digitos} {digitoN}
partChar = [^\'\n\r]
partStr = [^\"\r\n]*
/*" */
comillas = "'"
comillasDobles = "\""
coma = ","

litFloat = {ceroF} | {cero} {punto} {partDecimal} | {partEntera} {punto} {partDecimal} | {partEntera} {punto} {cero}
litInt = {cero} | {partEntera}
litChar = {comillas} {partChar} {comillas}
litStr = {comillasDobles} {partStr} {comillasDobles}

%%
// =============== Palabras Reservadas ===================

{val}               { return symbol(sym.VAL); }
{if}                { return symbol(sym.IF); }
{elif}              { return symbol(sym.ELIF); }
{else}              { return symbol(sym.ELSE); }
{while}             { return symbol(sym.WHILE); }
{for}               { return symbol(sym.FOR); }
{return}            { return symbol(sym.RETURN); }
{break}             { return symbol(sym.BREAK); }
{read}              { return symbol(sym.READ); }
{write}             { return symbol(sym.WRITE); }
{principal}         { return symbol(sym.PRINCIPAL); }
{int}               { return symbol(sym.INT); }
{float}             { return symbol(sym.FLOAT); }
{bool}              { return symbol(sym.BOOL); }
{char}              { return symbol(sym.CHAR); }
{void}              { return symbol(sym.VOID); }
{string}            { return symbol(sym.STRING); }
{true}              { return symbol(sym.TRUE); }
{false}             { return symbol(sym.FALSE); }

// =============== Delimitadores compuestos
{openLlaves}        { return symbol(sym.OPEN_LLAVES); }
{closeLlaves}       { return symbol(sym.CLOSE_LLAVES); }
{openBracket}       { return symbol(sym.OPEN_BRACKET); }
{closeBracket}      { return symbol(sym.CLOSE_BRACKET); }
{openParen}         { return symbol(sym.OPEN_PAREN); }
{closeParen}        { return symbol(sym.CLOSE_PAREN); }

// =============== Operadores simples
{opSuma}            { return symbol(sym.OP_SUMA); }
{opResta}           { return symbol(sym.OP_RESTA); }
{opMult}            { return symbol(sym.OP_MULT); }
{opDivDecimal}      { return symbol(sym.OP_DIV_DECIMAL); }
{opDivEntera}       { return symbol(sym.OP_DIV_ENTERA); }
{opModulo}          { return symbol(sym.OP_MODULO); }
{opPotencia}        { return symbol(sym.OP_POTENCIA); }
{opMenor}           { return symbol(sym.OP_MENOR); }
{opMayor}           { return symbol(sym.OP_MAYOR); }
{opAnd}             { return symbol(sym.OP_AND); }
{opOr}              { return symbol(sym.OP_OR); }
{opNegacion}        { return symbol(sym.OP_NEGACION); }

// =============== Operadores compuestos
{opIncremento}      { return symbol(sym.OP_INCREMENTO); }
{opDecremento}      { return symbol(sym.OP_DECREMENTO); }
{opMenorIgual}      { return symbol(sym.OP_MENOR_IGUAL); }
{opMayorIgual}      { return symbol(sym.OP_MAYOR_IGUAL); }
{opIgual}           { return symbol(sym.OP_IGUAL); }
{opDiferente}       { return symbol(sym.OP_DIFERENTE); }

// =============== Delimitadores simples
{asign}             { return symbol(sym.ASIGN); }
{finSentencia}      { return symbol(sym.FIN_SENTENCIA); }
{coma}              { return symbol(sym.COMA); }

// =============== Literales
{litInt}           { return symbol(sym.LIT_INT); }
{litFloat}         { return symbol(sym.LIT_FLOAT); }
{litChar}          { return symbol(sym.LIT_CHAR); }
{litStr}           { return symbol(sym.LIT_STRING); }

// =============== Identificadores
{id}                { return symbol(sym.ID); }

// =============== Comentarios
{comentarioLinea}   {}
{aperturaBloque}    {
                        lineComentario = yyline + 1;
                        columnComentario = yycolumn + 1;
                        yybegin(COMENTARIO);
                    }

// =============== Blancos
{blanco} {}
[^] { ErrorReporter.lexico("Caracter no reconocido '" + yytext() + "'", yycolumn +1, yyline +1); }

// =============== Estado COMENTARIO
<COMENTARIO> {
    {cierreBloque} { yybegin(YYINITIAL); }
    [^!]+ { }
    <<EOF>> { 
        ErrorReporter.lexico("Comentario sin cerrar", columnComentario, lineComentario);
        yybegin(YYINITIAL);
        return symbol(sym.EOF);
    }
}