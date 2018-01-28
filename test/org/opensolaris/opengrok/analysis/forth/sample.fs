\ Copyright (c) 2015, James Bowman
\ All rights reserved.
\ 
\ Redistribution and use in source and binary forms, with or without
\ modification, are permitted provided that the following conditions are met:
\ 
\ * Redistributions of source code must retain the above copyright notice, this
\   list of conditions and the following disclaimer.
\ 
\ * Redistributions in binary form must reproduce the above copyright notice,
\   this list of conditions and the following disclaimer in the documentation
\   and/or other materials provided with the distribution.
\ 
\ * Neither the name of swapforth nor the names of its
\   contributors may be used to endorse or promote products derived from
\   this software without specific prior written permission.
\ 
\ THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
\ AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
\ IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
\ DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
\ FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
\ DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
\ SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
\ CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
\ OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
\ OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

0 value fib

: refill
    source-id 0> if
        fib 256 source-id read-line throw
        if
            fib swap tosource
            >in off
            true
        else
            drop false
        then
    else
        refill
    then
;

: include-file ( fileid -- )
    fib >r
    256 allocate throw to fib
    source 2>r
    >in @ >r
    source-id >r
    \ Store fileid in SOURCE-ID.
    \ Make the file specified by fileid the input source.
    \ Store zero in BLK.

    (source-id) !
    1 >r
    begin
        fib 256 (source-id) @ read-line throw
    while
        fib swap tosource
        >in off
        ['] interpret catch
\ cr .s [char] | emit source type
        ?dup if
            \ produce a friendly error message
            \ XXX - how to generate filename?
            \ XXX - should try to match a vim errorformat
            decimal
            cr ." At line " r@ u. ." column " >inwas @ u.
            cr source type
            throw
        then
        r> 1+ >r
    repeat
    r> 2drop
    (source-id) @ close-file throw

    r> (source-id) !
    r> >in !
    2r> tosource
    fib free throw
    r> to fib
;

: included  ( c-addr u -- )
    r/o open-file throw
    include-file
;

: include                         ( "filename" -- )
    parse-name included decimal
;

\ #######   TOOLS EXT   #######################################

( [IF] [ELSE] [THEN]                         JCB 10:59 07/18/14)
\ From ANS specification A.15.6.2.2533

: [ELSE]  ( -- )
    1 BEGIN                               \ level
      BEGIN
        BL WORD COUNT DUP  WHILE          \ level adr len
        2DUP  S" [IF]"  COMPARE 0=
        IF                                \ level adr len
          2DROP 1+                        \ level'
        ELSE                              \ level adr len
          2DUP  S" [ELSE]"
          COMPARE 0= IF                   \ level adr len
             2DROP 1- DUP IF 1+ THEN      \ level'
          ELSE                            \ level adr len
            S" [THEN]"  COMPARE 0= IF     \ level
              1-                          \ level'
            THEN
          THEN
        THEN ?DUP 0=  IF EXIT THEN        \ level'
      REPEAT  2DROP                       \ level
    REFILL 0= UNTIL                       \ level
    DROP
;  IMMEDIATE

: [IF]  ( flag -- )
0= IF POSTPONE [ELSE] THEN ;  IMMEDIATE

: [THEN]  ( -- )  ;  IMMEDIATE

hex
: array create cells allot does> swap cells + ;

: parity ( x -- p ) 8 1 do dup 2/ xor loop invert 1 and ;
:noname 0100 0 do i parity c, loop ;
create even_parity execute

create ram
include 8080pre.inc
10000 allot

variable pc         variable rA
variable f_sp variable f_z
variable f_ac variable f_cy
4 array reg_pair
0 reg_pair constant rBC 1 reg_pair constant rDE
2 reg_pair constant rHL 3 reg_pair constant rSP

: pc!   ram + pc ! ;    : pc@   pc @ ram - ;
: rA!   rA c! ;         : rA@   rA c@ ;
: 8bit  0ff and ;       : 16bit 0ffff and ;
: split ( x -- lo hi )  dup 0ff and swap 8 rshift ;
: merge ( lo hi -- x )  8 lshift + ;

: SET_SZP ( byte -- )   dup f_sp ! f_z ! ;
: Z    ( -- zero )      f_z @ 0= 1 and ;
: S    ( -- sign )      f_sp @ 7 rshift ;
: P    ( -- parity )    even_parity f_sp @ + c@ ;
: AC   ( -- auxcarry )  f_ac @ 4 rshift 1 and ;
: CY   ( -- carry )     f_cy @ 1 and ;
: nCY  ( -- not-carry ) f_cy @ invert 1 and ;
: nybs ( x y -- x y x' y' ) over $f and over $f and ;
: +ac ( x y ) + f_ac ! ;  \ Set AC from sum of nybbles
: set_f
    dup >r 5 rshift r@ invert xor $04 and
    r@ $80 and or f_sp !
    r@ invert 6 rshift 1 and f_z !
    r@ f_ac !
    r> 1 and f_cy ! ;
: get_f
    ( S ) f_sp @ $80 and
    ( Z ) f_z @ 0= $40 and or
    ( AC ) f_ac @ $10 and or
    P 2 lshift or 2 or CY or ;

: ram@  ram + c@ ;  : ram!  ram + c! ;
: fetch ( -- byte ) pc @ c@ 1 pc +! ;
: ram!16   ram + over 8 rshift over 1+ c! c! ;
: ram@16   ram + count swap c@ merge ;
: fetch16  pc @ count swap count swap pc ! merge ;

: (reg8)
    case
    0   of rBC 1+ endof         1   of rBC endof
    2   of rDE 1+ endof         3   of rDE endof
    4   of rHL 1+ endof         5   of rHL endof
                                7   of rA  endof
    endcase postpone literal ;
: w8 ( rrr -- )
    case
    6   of  postpone rHL postpone @ postpone ram! endof
            dup (reg8) postpone c!
    endcase ;
: r8 ( rrr -- )
    case
    6   of  postpone rHL postpone @ postpone ram@ endof
            dup (reg8) postpone c@
    endcase ;
: (reg16)   ( rp -- ) reg_pair postpone literal ;
: r16 (reg16) postpone @ ;      : w16 (reg16) postpone ! ;

$100 array dx  : ;op postpone ; swap dx ! ; immediate

: b210 7 and ; : b543 3 rshift b210 ; : b54 b543 2/ ;
: _dcr ( a -- b )   dup $f and $f +ac 1- 8bit dup SET_SZP ;
: _inr ( a -- b )   dup $f and 1 +ac 1+ 8bit dup SET_SZP ;
: mk_DCR            b543 dup r8 postpone _dcr w8 ;
: mk_INR            b543 dup r8 postpone _inr w8 ;
: mk_MVI ( op -- )  postpone fetch b543 w8 ;
: mk_LXI            postpone fetch16 b54 w16 ;
: 1+/16  1 + 16bit ;            : 1-/16 1 - 16bit ;
: mk_INX            b54 dup r16 postpone 1+/16 w16 ;
: mk_DCX            b54 dup r16 postpone 1-/16 w16 ;
: _dad              rHL @ + dup 16bit rHL ! $10 rshift f_cy ! ;
: mk_DAD            b54 r16 postpone _dad ;
: mk_MOV ( op -- )  dup b210 r8 b543 w8 ;
: _push ( n -- )    -2 rSp +! rSP @ ram!16 ;
: _pop ( -- n )     rSP @ ram@16 2 rSP +!  ;
: mk_POP            b54 postpone _pop w16 ;
: mk_PUSH           b54 r16 postpone _push ;

: _jsr  pc@ _push pc! ;     : _call fetch16 pc@ _push pc! ;
: _ret _pop pc! ;           : _jmp  fetch16 pc! ;
: _skip 2 pc +! ;
: cond ( op xtF xtT -- )
    2>r dup b54 case
    0    of postpone Z endof
    1    of postpone CY endof
    2    of postpone P endof
    3    of postpone S endof
    endcase
    8 and 0= if postpone 0= then
    postpone if r> compile,
    postpone else r> compile, postpone then ;
: mk_CC ['] _skip ['] _call cond ;
: mk_RC ['] noop ['] _ret cond ;
: mk_JC ['] _skip ['] _jmp cond ;
: mk_RST b543 8 * postpone literal postpone _jsr ;

: aresult ( x -- ) ( after arithmetic. Write to A and flags )
    split f_cy ! dup rA! SET_SZP ;
: lresult ( byte -- ) ( after logical. Write to A and flags )
    0 f_cy ! dup rA! SET_SZP ;

( Each of 8 ALU words applies n to A: ( n -- )
: _add rA@ nybs +ac + aresult ;
: _adc rA@ nybs CY + +ac + CY + aresult ;
: _sub invert rA@ nybs 1+ +ac + 1+ aresult ;
: _sbb invert rA@ nybs + nCY +ac + nCY + aresult ;
: _ana rA@ 2dup or 2* f_ac ! and lresult ;
: _xra 0 f_ac ! rA@ xor lresult ;
: _ora 0 f_ac ! rA@ or lresult ;
: _cmp invert rA@ nybs 1+ +ac + 1+
       split f_cy ! SET_SZP ;
0 array aluops  ' _add , ' _adc , ' _sub , ' _sbb ,
                ' _ana , ' _xra , ' _ora , ' _cmp ,
: mk_ALU b543 aluops @ compile, ;

:noname
    $40 $00 do
        i :noname
        i $f and case
            $0 of endof
            $1 of i mk_LXI endof
            $3 of i mk_INX endof
            $4 of i mk_INR endof
            $5 of i mk_DCR endof
            $6 of i mk_MVI endof
            $9 of i mk_DAD endof
            $b of i mk_DCX endof
            $c of i mk_INR endof
            $d of i mk_DCR endof
            $e of i mk_MVI endof
        endcase
        postpone ;op
    loop
    $80 $40 do
        i :noname i mk_MOV postpone ;op
    loop
    $c0 $80 do
        i :noname i b210 r8 i mk_ALU postpone ;op
    loop
    $100 $c0 do
        i :noname
        i $f and case
            $0 of i mk_RC endof
            $1 of i mk_POP endof
            $2 of i mk_JC endof
            $4 of i mk_CC endof
            $5 of i mk_PUSH endof
            $6 of i postpone fetch mk_ALU endof
            $7 of i mk_RST endof
            $8 of i mk_RC endof
            $a of i mk_JC endof
            $c of i mk_CC endof
            $e of i postpone fetch mk_ALU endof
            $f of i mk_RST endof
        endcase
        postpone ;op
    loop ; execute

: aa ( -- a )  rA@ dup merge ;  ( double ACC, for rotates )
: alo   rA@ $f and ;            ( ACC low nybble, for DAA )
: ahi   rA@ 4 rshift ;          ( ACC high nybble, for DAA )
$27 :noname ( DAA )
    alo 9 > AC or 0<> $06 and       ( low adjust )
    alo 9 > f_ac !                  ( update AC )
    ahi CY AC or + 9 > CY or
    0<> $60 and +                   ( high+low adjust )
    ahi AC + 9 > CY or f_cy !       ( new CY )
    rA@ + 8bit dup rA! SET_SZP ;op

$02 :noname ( STAX B   ) rA@ rBC @ ram! ;op
$07 :noname ( RLC      ) aa 7 rshift dup f_cy ! rA! ;op
$0f :noname ( RRC      ) aa dup f_cy ! 2/ rA! ;op
$0a :noname ( LDAX B   ) rBC @ ram@ rA! ;op
$12 :noname ( STAX D   ) rA@ rDE @ ram! ;op
$17 :noname ( RAL      ) rA@ 2* CY + split f_cy ! rA! ;op
$1a :noname ( LDAX D   ) rDE @ ram@ rA! ;op
$1f :noname ( RAR      ) rA@ CY merge dup f_cy ! 2/ rA! ;op
$22 :noname ( SHLD     ) rHL @ fetch16 ram!16 ;op
$2a :noname ( LHLD     ) fetch16 ram@16 rHL ! ;op
$2f :noname ( CMA      ) rA@ $ff xor rA! ;op
$32 :noname ( STA      ) rA@ fetch16 ram! ;op
$37 :noname ( STC      ) 1 f_cy ! ;op
$3a :noname ( LDA      ) fetch16 ram@ rA! ;op
$3f :noname ( CMC      ) CY invert f_cy ! ;op
$c3         ( JMP      ) ' _jmp  swap dx !
$c9         ( RET      ) ' _ret  swap dx !
$cd         ( CALL     ) ' _call swap dx !
$e3 :noname ( XTHL     ) rHL @ _pop rHL ! _push ;op
$e9 :noname ( PCHL     ) rHL @ pc! ;op
$eb :noname ( XCHG     ) rHL @ rDE @ rHL ! rDE ! ;op
$f1 :noname ( POP PSW  ) _pop split rA! set_f ;op
$f3 :noname ;op
$f5 :noname ( PUSH PSW ) get_f rA@ merge _push ;op
$f9 :noname ( SPHL     ) rHL @ rSP ! ;op
$fb :noname ;op
$fd :noname ( BDOS )
    rBC c@ case
    0 of bye endof
    9 of
        rDE @ begin
            dup ram@ dup [char] $ <>
        while
            emit 1+
        repeat
        2drop endof
    endcase ;op
$ed :noname cr bye ;op

$100 pc!  0 lresult  0 f_ac !  rBC 4 cells erase
:noname
    0 dx
    begin
        fetch cells over + @ execute
    again ; execute

\ This is fake Forth but just for testing floating point:
$FFF 1E 1.E 1.E0 +1.23E-1 -1.23E+1
