# mi-lang
![mu-360x](https://user-images.githubusercontent.com/78901876/189482981-64636d3e-5f8e-47ec-80e3-7b500384d576.png)

Scripting language designed to communicate with java, to allow for easy plugins, addons, etc in any project, all without having to create an entire programming language for it. Plugin / Addon developers don't need to create an entire java project for small things either, making life easier for both sides. Syntactically inspired by rust (pretty much only the keywords) but structure-wise it feels like simply writing java code with less code for same output.

## Wiki
To see all features explained in detail: https://github.com/crxyne/mi-lang/wiki

## Java usage for app devs
```java
public static void main(@NotNull final String... args) {
    final Mi mi = new Mi(System.out, true);
    final MessageHandler messageHandler = mi.messageHandler();

    final String code = """
module testing {
    
    fn main {
        std.println("hello world!");
    }
    
}
                """;
        
    final List<ByteCodeInstruction> program = mi.compile(MiStandardLib.standardLib(), code, "testing", "main");
    final ByteCodeInterpreter runtime = new ByteCodeInterpreter(program, messageHandler);
    runtime.run();
    
    // or, alternatively compile from and to a file, read from that file and run the binary instead
    final File inputFile = new File("path_to_input_file/file.mi");
    try {
        mi.compile(MiStandardLib.standardLib(), Files.readString(inputFile.toPath()), new File("path_to_output_file/file.mib"), inputFile, "testing", "main");
    } catch (final IOException e) {
        e.printStackTrace();
    }
}
```

## NOTES
#### Be patient
Barebones version is finished, but to have some more high level language features it will take some more time. Still in BETA, so if you find any bugs, report them. Currently working on bytecode execution, so at this state, mi is unusable. Be patient.
#### The language is bad but not horrible (ha)
While this language may be interpreted in java (which is generally a horrible idea), the purpose of this language is not to be fast. However, whenever possible, the code will be optimized to allow for a faster execution. As a sidenote, this compiles to bytecode, meaning anyone could, in theory, write an interpreter for it in another language to get a better performance.

### helloworld
```
module helloworld {
    
    pub fn main {
        std.println("Hello, world!");
    }

}
```

#### FEATURES:
- basic strongly typed programming language features (types, functions, namespaces / modules / packages whatever you call them, etc)
- (currently one sided) communication with java for easy scripting in any java project

#### TODOS:
- structs and impls similar to rust
- macros, to insert code in front of a function
- assert, typedef and their combination for fast easy types
- operator overloading as an option for already defined functions
- calling mi functions from java, to allow for eventhandlers, etc
