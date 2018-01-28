/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright (c) 2018, Chris Fraire <cfraire@me.com>.
 */

package org.opensolaris.opengrok.analysis.forth;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a container for a set of Forth keywords.
 */
public class Consts {

    public static final Set<String> kwd = new HashSet<String>();

    static {
//        kwd.add("!");	// Forth 200x Draft 16.1
        kwd.add("#");	// Forth 200x Draft 16.1
        kwd.add("#>");	// Forth 200x Draft 16.1
        kwd.add("#s");	// Forth 200x Draft 16.1
//        kwd.add("’");	// Forth 200x Draft 16.1
//        kwd.add("'");	// Forth 200x Draft 16.1 ("" with apostrophe, --cfraire)
//        kwd.add("(");	// Forth 200x Draft 16.1
        kwd.add("(local)");	// Forth 200x Draft 16.1
//        kwd.add("*");	// Forth 200x Draft 16.1
        kwd.add("*/");	// Forth 200x Draft 16.1
        kwd.add("*/mod");	// Forth 200x Draft 16.1
//        kwd.add("+");	// Forth 200x Draft 16.1
        kwd.add("+!");	// Forth 200x Draft 16.1
        kwd.add("+field");	// Forth 200x Draft 16.1
        kwd.add("+loop");	// Forth 200x Draft 16.1
        kwd.add("+x/string");	// Forth 200x Draft 16.1
//        kwd.add("-");	// Forth 200x Draft 16.1
        kwd.add("-trailing");	// Forth 200x Draft 16.1
        kwd.add("-trailing-garbage");	// Forth 200x Draft 16.1
//        kwd.add(".");	// Forth 200x Draft 16.1
//        kwd.add(".\"");	// Forth 200x Draft 16.1
        kwd.add(".(");	// Forth 200x Draft 16.1
        kwd.add(".r");	// Forth 200x Draft 16.1
        kwd.add(".s");	// Forth 200x Draft 16.1
//        kwd.add("/");	// Forth 200x Draft 16.1
        kwd.add("/mod");	// Forth 200x Draft 16.1
        kwd.add("/string");	// Forth 200x Draft 16.1
        kwd.add("0<");	// Forth 200x Draft 16.1
        kwd.add("0<>");	// Forth 200x Draft 16.1
        kwd.add("0=");	// Forth 200x Draft 16.1
        kwd.add("0>");	// Forth 200x Draft 16.1
        kwd.add("1+");	// Forth 200x Draft 16.1
        kwd.add("1-");	// Forth 200x Draft 16.1
        kwd.add("2!");	// Forth 200x Draft 16.1
        kwd.add("2*");	// Forth 200x Draft 16.1
        kwd.add("2/");	// Forth 200x Draft 16.1
        kwd.add("2>r");	// Forth 200x Draft 16.1
        kwd.add("2@");	// Forth 200x Draft 16.1
        kwd.add("2constant");	// Forth 200x Draft 16.1
        kwd.add("2drop");	// Forth 200x Draft 16.1
        kwd.add("2dup");	// Forth 200x Draft 16.1
        kwd.add("2literal");	// Forth 200x Draft 16.1
        kwd.add("2over");	// Forth 200x Draft 16.1
        kwd.add("2r>");	// Forth 200x Draft 16.1
        kwd.add("2r@");	// Forth 200x Draft 16.1
        kwd.add("2rot");	// Forth 200x Draft 16.1
        kwd.add("2swap");	// Forth 200x Draft 16.1
        kwd.add("2value");	// Forth 200x Draft 16.1
        kwd.add("2variable");	// Forth 200x Draft 16.1
//        kwd.add(":");	// Forth 200x Draft 16.1
        kwd.add(":noname");	// Forth 200x Draft 16.1
//        kwd.add(";");	// Forth 200x Draft 16.1
        kwd.add(";code");	// Forth 200x Draft 16.1
//        kwd.add("<");	// Forth 200x Draft 16.1
        kwd.add("<#");	// Forth 200x Draft 16.1
        kwd.add("<>");	// Forth 200x Draft 16.1
//        kwd.add("=");	// Forth 200x Draft 16.1
//        kwd.add(">");	// Forth 200x Draft 16.1
        kwd.add(">body");	// Forth 200x Draft 16.1
        kwd.add(">float");	// Forth 200x Draft 16.1
        kwd.add(">in");	// Forth 200x Draft 16.1
        kwd.add(">number");	// Forth 200x Draft 16.1
        kwd.add(">r");	// Forth 200x Draft 16.1
        kwd.add("?");	// Forth 200x Draft 16.1
        kwd.add("?do");	// Forth 200x Draft 16.1
        kwd.add("?dup");	// Forth 200x Draft 16.1
//        kwd.add("@");	// Forth 200x Draft 16.1
        kwd.add("abort");	// Forth 200x Draft 16.1
//        kwd.add("abort\"");	// Forth 200x Draft 16.1
        kwd.add("abs");	// Forth 200x Draft 16.1
        kwd.add("accept");	// Forth 200x Draft 16.1
        kwd.add("action-of");	// Forth 200x Draft 16.1
        kwd.add("again");	// Forth 200x Draft 16.1
        kwd.add("ahead");	// Forth 200x Draft 16.1
        kwd.add("align");	// Forth 200x Draft 16.1
        kwd.add("aligned");	// Forth 200x Draft 16.1
        kwd.add("allocate");	// Forth 200x Draft 16.1
        kwd.add("allot");	// Forth 200x Draft 16.1
        kwd.add("also");	// Forth 200x Draft 16.1
        kwd.add("and");	// Forth 200x Draft 16.1
        kwd.add("assembler");	// Forth 200x Draft 16.1
        kwd.add("at-xy");	// Forth 200x Draft 16.1
        kwd.add("base");	// Forth 200x Draft 16.1
        kwd.add("begin");	// Forth 200x Draft 16.1
        kwd.add("begin-structure");	// Forth 200x Draft 16.1
        kwd.add("bin");	// Forth 200x Draft 16.1
        kwd.add("bl");	// Forth 200x Draft 16.1
        kwd.add("blank");	// Forth 200x Draft 16.1
        kwd.add("blk");	// Forth 200x Draft 16.1
        kwd.add("block");	// Forth 200x Draft 16.1
        kwd.add("buffer");	// Forth 200x Draft 16.1
        kwd.add("buffer:");	// Forth 200x Draft 16.1
        kwd.add("bye");	// Forth 200x Draft 16.1
        kwd.add("c!");	// Forth 200x Draft 16.1
//        kwd.add("c\"");	// Forth 200x Draft 16.1
        kwd.add("c");	// Forth 200x Draft 16.1
        kwd.add("c@");	// Forth 200x Draft 16.1
        kwd.add("case");	// Forth 200x Draft 16.1
        kwd.add("catch");	// Forth 200x Draft 16.1
        kwd.add("cell+");	// Forth 200x Draft 16.1
        kwd.add("cells");	// Forth 200x Draft 16.1
        kwd.add("cfield:");	// Forth 200x Draft 16.1
        kwd.add("char");	// Forth 200x Draft 16.1
        kwd.add("char+");	// Forth 200x Draft 16.1
        kwd.add("chars");	// Forth 200x Draft 16.1
        kwd.add("close-file");	// Forth 200x Draft 16.1
        kwd.add("cmove");	// Forth 200x Draft 16.1
        kwd.add("cmove>");	// Forth 200x Draft 16.1
        kwd.add("code");	// Forth 200x Draft 16.1
        kwd.add("compare");	// Forth 200x Draft 16.1
        kwd.add("compile");	// Forth 200x Draft 16.1
        kwd.add("constant");	// Forth 200x Draft 16.1
        kwd.add("count");	// Forth 200x Draft 16.1
        kwd.add("cr");	// Forth 200x Draft 16.1
        kwd.add("create");	// Forth 200x Draft 16.1
        kwd.add("create-file");	// Forth 200x Draft 16.1
        kwd.add("cs-pick");	// Forth 200x Draft 16.1
        kwd.add("cs-roll");	// Forth 200x Draft 16.1
        kwd.add("d+");	// Forth 200x Draft 16.1
        kwd.add("d-");	// Forth 200x Draft 16.1
        kwd.add("d.");	// Forth 200x Draft 16.1
        kwd.add("d.r");	// Forth 200x Draft 16.1
        kwd.add("d0<");	// Forth 200x Draft 16.1
        kwd.add("d0=");	// Forth 200x Draft 16.1
        kwd.add("d2*");	// Forth 200x Draft 16.1
        kwd.add("d2/");	// Forth 200x Draft 16.1
        kwd.add("d<");	// Forth 200x Draft 16.1
        kwd.add("d=");	// Forth 200x Draft 16.1
        kwd.add("d>f");	// Forth 200x Draft 16.1
        kwd.add("d>s");	// Forth 200x Draft 16.1
        kwd.add("dabs");	// Forth 200x Draft 16.1
        kwd.add("decimal");	// Forth 200x Draft 16.1
        kwd.add("defer");	// Forth 200x Draft 16.1
        kwd.add("defer!");	// Forth 200x Draft 16.1
        kwd.add("defer@");	// Forth 200x Draft 16.1
        kwd.add("definitions");	// Forth 200x Draft 16.1
        kwd.add("delete-file");	// Forth 200x Draft 16.1
        kwd.add("depth");	// Forth 200x Draft 16.1
        kwd.add("df!");	// Forth 200x Draft 16.1
        kwd.add("df@");	// Forth 200x Draft 16.1
        kwd.add("dfalign");	// Forth 200x Draft 16.1
        kwd.add("dfaligned");	// Forth 200x Draft 16.1
        kwd.add("dffield:");	// Forth 200x Draft 16.1
        kwd.add("dfloat+");	// Forth 200x Draft 16.1
        kwd.add("dfloats");	// Forth 200x Draft 16.1
        kwd.add("dmax");	// Forth 200x Draft 16.1
        kwd.add("dmin");	// Forth 200x Draft 16.1
        kwd.add("dnegate");	// Forth 200x Draft 16.1
        kwd.add("do");	// Forth 200x Draft 16.1
        kwd.add("does>");	// Forth 200x Draft 16.1
        kwd.add("drop");	// Forth 200x Draft 16.1
        kwd.add("du<");	// Forth 200x Draft 16.1
        kwd.add("dump");	// Forth 200x Draft 16.1
        kwd.add("dup");	// Forth 200x Draft 16.1
        kwd.add("editor");	// Forth 200x Draft 16.1
        kwd.add("ekey");	// Forth 200x Draft 16.1
        kwd.add("ekey>char");	// Forth 200x Draft 16.1
        kwd.add("ekey>fkey");	// Forth 200x Draft 16.1
        kwd.add("ekey>xchar");	// Forth 200x Draft 16.1
        kwd.add("ekey?");	// Forth 200x Draft 16.1
        kwd.add("else");	// Forth 200x Draft 16.1
        kwd.add("emit");	// Forth 200x Draft 16.1
        kwd.add("emit?");	// Forth 200x Draft 16.1
        kwd.add("empty-buffers");	// Forth 200x Draft 16.1
        kwd.add("end-structure");	// Forth 200x Draft 16.1
        kwd.add("endcase");	// Forth 200x Draft 16.1
        kwd.add("endof");	// Forth 200x Draft 16.1
        kwd.add("environment?");	// Forth 200x Draft 16.1
        kwd.add("erase");	// Forth 200x Draft 16.1
        kwd.add("evaluate");	// Forth 200x Draft 16.1
        kwd.add("execute");	// Forth 200x Draft 16.1
        kwd.add("exit");	// Forth 200x Draft 16.1
        kwd.add("f!");	// Forth 200x Draft 16.1
        kwd.add("f*");	// Forth 200x Draft 16.1
        kwd.add("f**");	// Forth 200x Draft 16.1
        kwd.add("f+");	// Forth 200x Draft 16.1
        kwd.add("f-");	// Forth 200x Draft 16.1
        kwd.add("f.");	// Forth 200x Draft 16.1
        kwd.add("f/");	// Forth 200x Draft 16.1
        kwd.add("f0<");	// Forth 200x Draft 16.1
        kwd.add("f0=");	// Forth 200x Draft 16.1
        kwd.add("f<");	// Forth 200x Draft 16.1
        kwd.add("f>d");	// Forth 200x Draft 16.1
        kwd.add("f>s");	// Forth 200x Draft 16.1
        kwd.add("f@");	// Forth 200x Draft 16.1
        kwd.add("fabs");	// Forth 200x Draft 16.1
        kwd.add("facos");	// Forth 200x Draft 16.1
        kwd.add("facosh");	// Forth 200x Draft 16.1
        kwd.add("falign");	// Forth 200x Draft 16.1
        kwd.add("faligned");	// Forth 200x Draft 16.1
        kwd.add("falog");	// Forth 200x Draft 16.1
        kwd.add("false");	// Forth 200x Draft 16.1
        kwd.add("fasin");	// Forth 200x Draft 16.1
        kwd.add("fasinh");	// Forth 200x Draft 16.1
        kwd.add("fatan");	// Forth 200x Draft 16.1
        kwd.add("fatan2");	// Forth 200x Draft 16.1
        kwd.add("fatanh");	// Forth 200x Draft 16.1
        kwd.add("fconstant");	// Forth 200x Draft 16.1
        kwd.add("fcos");	// Forth 200x Draft 16.1
        kwd.add("fcosh");	// Forth 200x Draft 16.1
        kwd.add("fdepth");	// Forth 200x Draft 16.1
        kwd.add("fdrop");	// Forth 200x Draft 16.1
        kwd.add("fdup");	// Forth 200x Draft 16.1
        kwd.add("fe.");	// Forth 200x Draft 16.1
        kwd.add("fexp");	// Forth 200x Draft 16.1
        kwd.add("fexpm1");	// Forth 200x Draft 16.1
        kwd.add("ffield:");	// Forth 200x Draft 16.1
        kwd.add("field:");	// Forth 200x Draft 16.1
        kwd.add("file-position");	// Forth 200x Draft 16.1
        kwd.add("file-size");	// Forth 200x Draft 16.1
        kwd.add("file-status");	// Forth 200x Draft 16.1
        kwd.add("fill");	// Forth 200x Draft 16.1
        kwd.add("find");	// Forth 200x Draft 16.1
        kwd.add("fliteral");	// Forth 200x Draft 16.1
        kwd.add("fln");	// Forth 200x Draft 16.1
        kwd.add("flnp1");	// Forth 200x Draft 16.1
        kwd.add("float+");	// Forth 200x Draft 16.1
        kwd.add("floats");	// Forth 200x Draft 16.1
        kwd.add("flog");	// Forth 200x Draft 16.1
        kwd.add("floor");	// Forth 200x Draft 16.1
        kwd.add("flush");	// Forth 200x Draft 16.1
        kwd.add("flush-file");	// Forth 200x Draft 16.1
        kwd.add("fm/mod");	// Forth 200x Draft 16.1
        kwd.add("fmax");	// Forth 200x Draft 16.1
        kwd.add("fmin");	// Forth 200x Draft 16.1
        kwd.add("fnegate");	// Forth 200x Draft 16.1
        kwd.add("forget");	// Forth 200x Draft 16.1
        kwd.add("forth");	// Forth 200x Draft 16.1
        kwd.add("forth-wordlist");	// Forth 200x Draft 16.1
        kwd.add("fover");	// Forth 200x Draft 16.1
        kwd.add("free");	// Forth 200x Draft 16.1
        kwd.add("frot");	// Forth 200x Draft 16.1
        kwd.add("fround");	// Forth 200x Draft 16.1
        kwd.add("fs.");	// Forth 200x Draft 16.1
        kwd.add("fsin");	// Forth 200x Draft 16.1
        kwd.add("fsincos");	// Forth 200x Draft 16.1
        kwd.add("fsinh");	// Forth 200x Draft 16.1
        kwd.add("fsqrt");	// Forth 200x Draft 16.1
        kwd.add("fswap");	// Forth 200x Draft 16.1
        kwd.add("ftan");	// Forth 200x Draft 16.1
        kwd.add("ftanh");	// Forth 200x Draft 16.1
        kwd.add("ftrunc");	// Forth 200x Draft 16.1
        kwd.add("fvalue");	// Forth 200x Draft 16.1
        kwd.add("fvariable");	// Forth 200x Draft 16.1
        kwd.add("f~");	// Forth 200x Draft 16.1
        kwd.add("get-current");	// Forth 200x Draft 16.1
        kwd.add("get-order");	// Forth 200x Draft 16.1
        kwd.add("here");	// Forth 200x Draft 16.1
        kwd.add("hex");	// Forth 200x Draft 16.1
        kwd.add("hold");	// Forth 200x Draft 16.1
        kwd.add("holds");	// Forth 200x Draft 16.1
        kwd.add("i");	// Forth 200x Draft 16.1
        kwd.add("if");	// Forth 200x Draft 16.1
        kwd.add("immediate");	// Forth 200x Draft 16.1
        kwd.add("include");	// Forth 200x Draft 16.1
        kwd.add("include-file");	// Forth 200x Draft 16.1
        kwd.add("included");	// Forth 200x Draft 16.1
        kwd.add("invert");	// Forth 200x Draft 16.1
        kwd.add("is");	// Forth 200x Draft 16.1
        kwd.add("j");	// Forth 200x Draft 16.1
        kwd.add("k-alt-mask");	// Forth 200x Draft 16.1
        kwd.add("k-ctrl-mask");	// Forth 200x Draft 16.1
        kwd.add("k-delete");	// Forth 200x Draft 16.1
        kwd.add("k-down");	// Forth 200x Draft 16.1
        kwd.add("k-end");	// Forth 200x Draft 16.1
        kwd.add("k-f1");	// Forth 200x Draft 16.1
        kwd.add("k-f10");	// Forth 200x Draft 16.1
        kwd.add("k-f11");	// Forth 200x Draft 16.1
        kwd.add("k-f12");	// Forth 200x Draft 16.1
        kwd.add("k-f2");	// Forth 200x Draft 16.1
        kwd.add("k-f3");	// Forth 200x Draft 16.1
        kwd.add("k-f4");	// Forth 200x Draft 16.1
        kwd.add("k-f5");	// Forth 200x Draft 16.1
        kwd.add("k-f6");	// Forth 200x Draft 16.1
        kwd.add("k-f7");	// Forth 200x Draft 16.1
        kwd.add("k-f8");	// Forth 200x Draft 16.1
        kwd.add("k-f9");	// Forth 200x Draft 16.1
        kwd.add("k-home");	// Forth 200x Draft 16.1
        kwd.add("k-insert");	// Forth 200x Draft 16.1
        kwd.add("k-left");	// Forth 200x Draft 16.1
        kwd.add("k-next");	// Forth 200x Draft 16.1
        kwd.add("k-prior");	// Forth 200x Draft 16.1
        kwd.add("k-right");	// Forth 200x Draft 16.1
        kwd.add("k-shift-mask");	// Forth 200x Draft 16.1
        kwd.add("k-up");	// Forth 200x Draft 16.1
        kwd.add("key");	// Forth 200x Draft 16.1
        kwd.add("key?");	// Forth 200x Draft 16.1
        kwd.add("leave");	// Forth 200x Draft 16.1
        kwd.add("list");	// Forth 200x Draft 16.1
        kwd.add("literal");	// Forth 200x Draft 16.1
        kwd.add("load");	// Forth 200x Draft 16.1
        kwd.add("locals|");	// Forth 200x Draft 16.1
        kwd.add("loop");	// Forth 200x Draft 16.1
        kwd.add("lshift");	// Forth 200x Draft 16.1
        kwd.add("m*");	// Forth 200x Draft 16.1
        kwd.add("m*/");	// Forth 200x Draft 16.1
        kwd.add("m+");	// Forth 200x Draft 16.1
        kwd.add("marker");	// Forth 200x Draft 16.1
        kwd.add("max");	// Forth 200x Draft 16.1
        kwd.add("min");	// Forth 200x Draft 16.1
        kwd.add("mod");	// Forth 200x Draft 16.1
        kwd.add("move");	// Forth 200x Draft 16.1
        kwd.add("ms");	// Forth 200x Draft 16.1
        kwd.add("n>r");	// Forth 200x Draft 16.1
        kwd.add("name>compile");	// Forth 200x Draft 16.1
        kwd.add("name>interpret");	// Forth 200x Draft 16.1
        kwd.add("name>string");	// Forth 200x Draft 16.1
        kwd.add("negate");	// Forth 200x Draft 16.1
        kwd.add("nip");	// Forth 200x Draft 16.1
        kwd.add("nr>");	// Forth 200x Draft 16.1
        kwd.add("of");	// Forth 200x Draft 16.1
        kwd.add("only");	// Forth 200x Draft 16.1
        kwd.add("open-file");	// Forth 200x Draft 16.1
        kwd.add("or");	// Forth 200x Draft 16.1
        kwd.add("order");	// Forth 200x Draft 16.1
        kwd.add("over");	// Forth 200x Draft 16.1
        kwd.add("pad");	// Forth 200x Draft 16.1
        kwd.add("page");	// Forth 200x Draft 16.1
        kwd.add("parse");	// Forth 200x Draft 16.1
        kwd.add("parse-name");	// Forth 200x Draft 16.1
        kwd.add("pick");	// Forth 200x Draft 16.1
        kwd.add("postpone");	// Forth 200x Draft 16.1
        kwd.add("precision");	// Forth 200x Draft 16.1
        kwd.add("previous");	// Forth 200x Draft 16.1
        kwd.add("quit");	// Forth 200x Draft 16.1
        kwd.add("r/o");	// Forth 200x Draft 16.1
        kwd.add("r/w");	// Forth 200x Draft 16.1
        kwd.add("r>");	// Forth 200x Draft 16.1
        kwd.add("r@");	// Forth 200x Draft 16.1
        kwd.add("read-file");	// Forth 200x Draft 16.1
        kwd.add("read-line");	// Forth 200x Draft 16.1
        kwd.add("recurse");	// Forth 200x Draft 16.1
        kwd.add("refill");	// Forth 200x Draft 16.1
        kwd.add("rename-file");	// Forth 200x Draft 16.1
        kwd.add("repeat");	// Forth 200x Draft 16.1
        kwd.add("replaces");	// Forth 200x Draft 16.1
        kwd.add("reposition-file");	// Forth 200x Draft 16.1
        kwd.add("represent");	// Forth 200x Draft 16.1
        kwd.add("require");	// Forth 200x Draft 16.1
        kwd.add("required");	// Forth 200x Draft 16.1
        kwd.add("resize");	// Forth 200x Draft 16.1
        kwd.add("resize-file");	// Forth 200x Draft 16.1
        kwd.add("restore-input");	// Forth 200x Draft 16.1
        kwd.add("roll");	// Forth 200x Draft 16.1
        kwd.add("rot");	// Forth 200x Draft 16.1
        kwd.add("rshift");	// Forth 200x Draft 16.1
//        kwd.add("s\"");	// Forth 200x Draft 16.1
        kwd.add("s>d");	// Forth 200x Draft 16.1
        kwd.add("s>f");	// Forth 200x Draft 16.1
        kwd.add("save-buffers");	// Forth 200x Draft 16.1
        kwd.add("save-input");	// Forth 200x Draft 16.1
        kwd.add("scr");	// Forth 200x Draft 16.1
        kwd.add("search");	// Forth 200x Draft 16.1
        kwd.add("search-wordlist");	// Forth 200x Draft 16.1
        kwd.add("see");	// Forth 200x Draft 16.1
        kwd.add("set-current");	// Forth 200x Draft 16.1
        kwd.add("set-order");	// Forth 200x Draft 16.1
        kwd.add("set-precision");	// Forth 200x Draft 16.1
        kwd.add("sf!");	// Forth 200x Draft 16.1
        kwd.add("sf@");	// Forth 200x Draft 16.1
        kwd.add("sfalign");	// Forth 200x Draft 16.1
        kwd.add("sfaligned");	// Forth 200x Draft 16.1
        kwd.add("sffield:");	// Forth 200x Draft 16.1
        kwd.add("sfloat+");	// Forth 200x Draft 16.1
        kwd.add("sfloats");	// Forth 200x Draft 16.1
        kwd.add("sign");	// Forth 200x Draft 16.1
        kwd.add("sliteral");	// Forth 200x Draft 16.1
        kwd.add("sm/rem");	// Forth 200x Draft 16.1
        kwd.add("source");	// Forth 200x Draft 16.1
        kwd.add("source-id");	// Forth 200x Draft 16.1
        kwd.add("space");	// Forth 200x Draft 16.1
        kwd.add("spaces");	// Forth 200x Draft 16.1
        kwd.add("state");	// Forth 200x Draft 16.1
        kwd.add("substitute");	// Forth 200x Draft 16.1
        kwd.add("swap");	// Forth 200x Draft 16.1
        kwd.add("synonym");	// Forth 200x Draft 16.1
//        kwd.add("s\\\"");	// Forth 200x Draft 16.1
        kwd.add("then");	// Forth 200x Draft 16.1
        kwd.add("throw");	// Forth 200x Draft 16.1
        kwd.add("thru");	// Forth 200x Draft 16.1
        kwd.add("time&date");	// Forth 200x Draft 16.1
        kwd.add("to");	// Forth 200x Draft 16.1
        kwd.add("traverse-wordlist");	// Forth 200x Draft 16.1
        kwd.add("true");	// Forth 200x Draft 16.1
        kwd.add("tuck");	// Forth 200x Draft 16.1
        kwd.add("type");	// Forth 200x Draft 16.1
        kwd.add("u.");	// Forth 200x Draft 16.1
        kwd.add("u.r");	// Forth 200x Draft 16.1
        kwd.add("u<");	// Forth 200x Draft 16.1
        kwd.add("u>");	// Forth 200x Draft 16.1
        kwd.add("um*");	// Forth 200x Draft 16.1
        kwd.add("um/mod");	// Forth 200x Draft 16.1
        kwd.add("unescape");	// Forth 200x Draft 16.1
        kwd.add("unloop");	// Forth 200x Draft 16.1
        kwd.add("until");	// Forth 200x Draft 16.1
        kwd.add("unused");	// Forth 200x Draft 16.1
        kwd.add("update");	// Forth 200x Draft 16.1
        kwd.add("value");	// Forth 200x Draft 16.1
        kwd.add("variable");	// Forth 200x Draft 16.1
        kwd.add("w/o");	// Forth 200x Draft 16.1
        kwd.add("while");	// Forth 200x Draft 16.1
        kwd.add("within");	// Forth 200x Draft 16.1
        kwd.add("word");	// Forth 200x Draft 16.1
        kwd.add("wordlist");	// Forth 200x Draft 16.1
        kwd.add("words");	// Forth 200x Draft 16.1
        kwd.add("write-file");	// Forth 200x Draft 16.1
        kwd.add("write-line");	// Forth 200x Draft 16.1
        kwd.add("x-size");	// Forth 200x Draft 16.1
        kwd.add("x-width");	// Forth 200x Draft 16.1
        kwd.add("xc!+");	// Forth 200x Draft 16.1
        kwd.add("xc!+?");	// Forth 200x Draft 16.1
        kwd.add("xc");	// Forth 200x Draft 16.1
        kwd.add("xc-size");	// Forth 200x Draft 16.1
        kwd.add("xc-width");	// Forth 200x Draft 16.1
        kwd.add("xc@+");	// Forth 200x Draft 16.1
        kwd.add("xchar+");	// Forth 200x Draft 16.1
        kwd.add("xchar-");	// Forth 200x Draft 16.1
        kwd.add("xemit");	// Forth 200x Draft 16.1
        kwd.add("xhold");	// Forth 200x Draft 16.1
        kwd.add("xkey");	// Forth 200x Draft 16.1
        kwd.add("xkey?");	// Forth 200x Draft 16.1
        kwd.add("xor");	// Forth 200x Draft 16.1
        kwd.add("x\\string-");	// Forth 200x Draft 16.1
//        kwd.add("[");	// Forth 200x Draft 16.1
        kwd.add("[’]");	// Forth 200x Draft 16.1
        kwd.add("[']");	// Forth 200x Draft 16.1 ("" with apostrophe, --cfraire)
        kwd.add("[char]");	// Forth 200x Draft 16.1
        kwd.add("[char]");	// Forth 200x Draft 16.1
        kwd.add("[compile]");	// Forth 200x Draft 16.1
        kwd.add("[defined]");	// Forth 200x Draft 16.1
        kwd.add("[else]");	// Forth 200x Draft 16.1
        kwd.add("[if]");	// Forth 200x Draft 16.1
        kwd.add("[then]");	// Forth 200x Draft 16.1
        kwd.add("[undefined]");	// Forth 200x Draft 16.1
//        kwd.add("\\");	// Forth 200x Draft 16.1
//        kwd.add("]");	// Forth 200x Draft 16.1
        kwd.add("{:");	// Forth 200x Draft 16.1
    }

    /** private to enforce static */
    private Consts () {
    }
}
