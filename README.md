# mu-lang
![mu-360x](https://user-images.githubusercontent.com/78901876/189482981-64636d3e-5f8e-47ec-80e3-7b500384d576.png)

Scripting language designed to communicate with java, to allow for easy plugins, addons, etc in any project, all without having to create an entire programming language for it. Plugin / Addon developers don't need to create an entire java project for small things either, making life easier for both sides. Syntactically inspired by rust (pretty much only the keywords) but structure-wise it feels like simply writing java code with less code for same output.

#### Barebones version is finished, but to have some more high level language features it will take some more time. Still in BETA, so if you find any bugs, report them.

While this language may be interpreted in java (which is generally a horrible idea), the purpose of this language is not to be fast. However, whenever possible, the code will be optimized to allow for a faster execution.

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
- bytecode generator & parser, for faster startup times (especially needed since this is designed for plugin systems or similar)
- classes, inheritance, polymorphism
- generics, for stuff like Option<T>, List<T>, Map<K, V>, Result<T>, etc
- macros / impls, something to insert code in front of a function
- assert, typedef and their combination for fast easy types
- operator overloading as an option for already defined functions
- calling mu functions from java, to allow for eventhandlers, etc

## Java usage for application developers
```java
final Runtime runtime = new Runtime(
                                System.out, // PrintStream, where to log errors, warnings, etc
                                true // boolean, whether to use ansi escape codes for color (will only be useful if the output stream is a terminal environment)
                        );

try {
    runtime.execute(
              MuStandardLib.standardLib(), // String, standardlib to use for the mu script
              code, // String, code to use, obviously
              true, // boolean, whether to print java stacktraces during runtime, useful for debugging (optional, can be left out)
              mainFunc, // String, main function to run (for example "module.othermodule.main")
              passInParams // Object... , passed in parameters for the main function (can only be primitive types) (optional, can be left out)
    );
} catch (Throwable e) {
    e.printStackTrace();
}
```

## Usage for Mu users 
#### (barebones mu execution with no plugin system), example with the rainbow.mu program found in /examples
```sh
java -Xmx2G -Xms2G -jar mu.jar file='examples/rainbow.mu' main=testing.main pass=10 pass='hello, world!'
```
With `file='somefile.mu'` you declare the file input. Using the `main=module.main` argument, you can define which function is executed, along with `pass`, which will be the passed in arguments for the main function, in their order. This example would call the main fn of rainbow.mu like so: `main("10", "hello, world!");`. Note that every passed in argument is a string here.
