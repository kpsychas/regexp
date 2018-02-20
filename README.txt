Library supports the following syntax of regular expressions
     * REGEX := EXPR REGEX | EXPR | ""
     * EXPR := EXPR MOD | RANGE_EXPR | GROUP_EXPR | LITERAL
     * RANGE_EXPR := RANGE_START RANGE RANGE_END
     * RANGE := LITERAL "-" LITERAL RANGE | LITERAL RANGE | ""
     * GROUP_EXPR := "(" EXPR ")"
     * MOD := "*" | "?" | "+"
     * RANGE_START := "[^" | "["
     * RANGE_END := "]"
     * LITERAL := 0-9 | a-z | A-Z

To avoid complicating grammar only latin characters and numbers are supported as literals.
Other characters that don't conflict with the current grammar can easily be added though.

The logic of library is all implemented in Pattern.java which includes
parsing and matching through regular expressions.
All public methods are documented.

A simple application for use of the library is provided in App.java file.
Application demonstrates use of methods Pattern class exposes.
Methods include inspection methods that print details of what is parsed and
matched and cannot be tested.
