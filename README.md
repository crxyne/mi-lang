# mu-lang

Scripting language designed to communicate with java, to allow for easy plugins, addons, etc in any project, all without having to create an entire programming language for it. Plugin / Addon developers don't need to create an entire java project for small things either, making life easier for both sides. Syntactically inspired by rust (pretty much only the keywords) but structure-wise it feels like simply writing java code with less code for same output.

#### Still WIP, not really usable and doesn't execute yet (working on parser and AST generation).

While this language may be interpreted in java (which is generally a horrible idea), the purpose of this language is not to be fast. However, whenever possible, the code will be optimized to allow for a faster execution.

### helloworld
```
module helloworld {
    
    pub fn main {
        std.println("Hello, world!");
    }

}

```
