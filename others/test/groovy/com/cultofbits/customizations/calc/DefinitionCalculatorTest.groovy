package com.cultofbits.customizations.calc

import org.junit.Test

class DefinitionCalculatorTest {

    @Test
    void smokeTest() {
        println(("\$calc.multiply(var.a, var.b, 10)" =~ /.*[$]calc.[^(]+\(([^(]+)\)/).with { it[0][1].tokenize(",") })
    }
}
