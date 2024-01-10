MemoRete OPS5 is a OPS5 like Interpreter, implementing a custom version of the Rete Algorithm.

State of the project:
    - experimental use only. Do not use in production
    - it requires more tests
    - clearly specify the language
    - write proper parser based on said specifiaction

Requirements for Program Developement:
    1. Reflection is used in the program. Enable Compiler Flags in your IDE so that the program can use reflection.
    https://stackoverflow.com/questions/6759880/getting-the-name-of-a-method-parameter/6759953#6759953
    2. At runtime disable assertions for performance. Otherwise performance is significantly worse. For developement or debugging always enable assertions.
    3. Project requires Maven to load dependencies (for example to load the Guava library).
    4. Java Version 21 or later
    
Build Program with maven:
    1. Have maven installed and Java 21 (in your PATH variable) or later
    2. Build:
    $ mvn clean package
    
Run program to print help commands:
    $ java -jar target/MemoReteOPS5-0.0.1-SNAPSHOT.jar
    
Run the demo.ops program:
    $ java -jar target/MemoReteOPS5-0.0.1-SNAPSHOT.jar -f src/test/ops5/Ops5Programs/demo.ops
    
Run the demo.ops program interactively and print help for interacive mode:
    $ java -jar target/MemoReteOPS5-0.0.1-SNAPSHOT.jar --norunNow -f src/test/ops5/Ops5Programs/demo.ops
    $ help

Read Documentation (unfinished, better look directly at source code):
Generate Documentation in Eclipse (Project->Generate Javadoc)

Open Javadoc in Eclipse:
    open doc/index.html

Open Javadoc in Browser:
    $ cd doc/
    $ python3 -m http.server
    open webbrowser on adress http://localhost:8000
