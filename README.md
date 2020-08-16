# Novlangue
## 1. Introduction
**_Novlangue_** is a simple and clean programming language, written in Kotlin and designed to compile to bare metal.

## 2. Usage
`java -jar novlangue-0.1.4.jar code.sw` compiles the contents of `code.sw` down to LLVM IR, output to `code.ll`.
Add `-noStd` to prevent Novlangue from expecting C library routines (currently just `printf`).
Add `-noMain` to prevent Novlangue from defining a main function, for linkage purposes.

### Examples
**Interpreted IR**
```
java -jar novlangue-0.1.4.jar code.sw
lli code.ll
```
**Native Executable**
```
java -jar novlangue-0.1.4.jar code.sw
llc -filetype=obj code.ll
clang code.o -o code
```
**Freestanding Native Executable**
```
java -jar novlangue-0.1.4.jar code.sw -noStd
llc -filetype=obj code.ll
clang code.o -o code -nostdlib
```
**Called from C**
```
java -jar novlangue-0.1.4.jar code.sw -noMain
llc -filetype=obj code.ll
clang code.o main.c -o code
```

## 3. Design
_**TODO:** Outline the language._

## 4. Obtaining Novlangue
One can build an executable Novlangue jarfile from source using `./gradlew jar` on Unix-like systems (Linux, MacOS) and `gradlew.bat jar` on Windows.
Periodically, we release executables through the repository.