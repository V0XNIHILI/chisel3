---
layout: docs
title:  "Serialization Cookbook"
section: "chisel3"
---

# Serialization Cookbook

* [Why do I need to serialize Modules](#why-do-i-need-to-serialize-modules)
* [How do I serialize Modules with SerializableModuleGenerator](#how-do-i-seerialize-modules-with-serializablemodulegenerator)

## Why do I need to serialize Modules
Chisel provides a very flexible hardware design experience. However, it sometimes becomes too flexible to design a relative big designs, since the parameter of module might come from: 1. Global variables; 2. Outer class; 3. Entropies(time, random). It becomes really hard or impossible to describe "how to reproduce this single module?". This forbids doing unit-test for a module generator, and introduces issues in post-synthesis when doing ECO: a change to Module A might lead to change in Module B.
Thus the `SerializableModuleGenerator`, `SerializableModule[T <: SerializableModuleParameter]` and `SerializableModuleParameter` is provided to solve this issue.
For any `SerializableModuleGenerator`, Chisel can automatically serialize and de-serialize it by adding a constraints:
1. the `SerializableModule` should not be inner class, since the outer class is parameter to it;
1. the `SerializableModule` has and only has one parameter with `SerializableModuleParameter` as its type.
1. the Module cannot depends on global variables and use non-reproducible function(random, time, etc), and this should be guaranteed by user, since Scala cannot detect it.

It can provide these benefits:
1. user can use `SerializableModuleGenerator(module: class[SerializableModule], parameter: SerializableModuleParameter)` to auto serialize a Module and its parameter.
1. user can nest `SerializableModuleGenerator` in other serializable parameters to represent a relative large parameter.
1. user can elaborate any `SerializableModuleGenerator` into a single module for testing.


## How do I serialize Modules with `SerializableModuleGenerator`
It is pretty simple and illustrated by example below, the GCD Module with width as its parameter.

```scala mdoc:silent
import chisel3._
import chisel3.experimental.{SerializableModule, SerializableModuleGenerator, SerializableModuleParameter}
import upickle.default._

// provide serialization functions to GCDSerializableModuleParameter
object GCDSerializableModuleParameter {
  implicit def rwP: ReadWriter[GCDSerializableModuleParameter] = macroRW
}

// Parameter
case class GCDSerializableModuleParameter(width: Int) extends SerializableModuleParameter

// Module
class GCDSerializableModule(val parameter: GCDSerializableModuleParameter)
    extends Module
    with SerializableModule[GCDSerializableModuleParameter] {
  val io = IO(new Bundle {
    val a = Input(UInt(parameter.width.W))
    val b = Input(UInt(parameter.width.W))
    val e = Input(Bool())
    val z = Output(UInt(parameter.width.W))
  })
  val x = Reg(UInt(parameter.width.W))
  val y = Reg(UInt(parameter.width.W))
  val z = Reg(UInt(parameter.width.W))
  val e = Reg(Bool())
  when(e) {
    x := io.a
    y := io.b
    z := 0.U
  }
  when(x =/= y) {
    when(x > y) {
      x := x - y
    }.otherwise {
      y := y - x
    }
  }.otherwise {
    z := x
  }
  io.z := z
}
```
using `write` function in `upickle`:
```scala
val j = upickle.default.write(
  SerializableModuleGenerator(
    classOf[GCDSerializableModule],
    GCDSerializableModuleParameter(32)
  )
)
println(j)
```

read from json string and elaborate the Module:
```scala
println(chisel3.stage.ChiselStage.emitVerilog(
  upickle.default.read[SerializableModuleGenerator[GCDSerializableModule, GCDSerializableModuleParameter]](
    ujson.read(j)
  ).module()
))
```
```verilog
module GCDSerializableModule(
  input         clock,
  input         reset,
  input  [31:0] io_a,
  input  [31:0] io_b,
  input         io_e,
  output [31:0] io_z
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
`endif
  reg [31:0] x;
  reg [31:0] y;
  reg [31:0] z;
  wire [31:0] _x_T_1 = x - y;
  wire [31:0] _y_T_1 = y - x;
  assign io_z = z;
  always @(posedge clock) begin
    if (x != y) begin
      if (x > y) begin
        x <= _x_T_1;
      end
    end
    if (x != y) begin
      if (!(x > y)) begin
        y <= _y_T_1;
      end
    end
    if (!(x != y)) begin
      z <= x;
    end
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  x = _RAND_0[31:0];
  _RAND_1 = {1{`RANDOM}};
  y = _RAND_1[31:0];
  _RAND_2 = {1{`RANDOM}};
  z = _RAND_2[31:0];
`endif
  `endif
end
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif
endmodule
```
