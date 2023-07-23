package com.cultofbits.customizations.calc

import com.cultofbits.integrationm.service.dictionary.recordm.Definition
class DefinitionCalculator {

    def defName;
    def defVersion;

    // a map with the key the var name (including var.) and the value the list of all fields
    def fdVarByVarName = [:]

    // All field definitions that have $calc.<operation>
    def fdExprById = [:]

    DefinitionCalculator(definition) {
        this.defName = definition.name
        this.defVersion = definition.version
        this.processDefinition(definition)
    }

    protected processDefinition(Definition definition) {
        def fdMapById = definition.fieldDefinitions
                .inject([:]) { map, fd -> map << [(fd.id): fd] }

        // Collect all field definitions with $var
        fdVarByVarName = definition.fieldDefinitions
                .findAll { fId, f -> f.description?.toString() =~ /.*[$]var.*/ }

        // Collect all field definitions with $calc.<operation>
        fdExprById = definition.fieldDefinitions
                .findAll { fd -> fd.description.toString() =~ /[$]calc\./ }
                .inject([:]) { map, fd ->
                    def op = (fd.description =~ /.*[$]calc.([^(]+)/).with { it[0][1] }
                    def args = (fd.description =~ /.*[$]calc.[^(]+\(([^(]+)\)/).with { it[0][1].tokenize(",") }

                    map << [(fd.id): new CalcExpr().tap {
                        it.operation = op
                        it.args = args
                    }]
                }
    }

    Optional<Map<String, String>> calculate() {

    }

    class CalcExpr {
        // one of: sum, multiply, difference, divide
        def operation;

        // All the arguments from the $calc expression like var.xxxx or any constant value
        def args;
    }

}

import com.cultofbits.integrationm.service.dictionary.recordm.FieldDefinition
