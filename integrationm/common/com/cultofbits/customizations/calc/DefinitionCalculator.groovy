package com.cultofbits.customizations.calc

import com.cultofbits.integrationm.service.dictionary.recordm.Definition
import com.cultofbits.integrationm.service.dictionary.recordm.FieldDefinition

class DefinitionCalculator {

    protected String defName;
    protected Integer defVersion;

    // a map with the key the var name (including var.) and the value the list of all fields
    protected Map<String, List<FieldDefinition>> fdVarByVarName = [:]

    // All field definitions that have $calc.<operation>
    protected Map<Integer, CalcExpr> fdExprById = [:]

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
                .findAll { fd -> fd.description?.indexOf("\$var.") != -1 }
                .inject([:] as Map<String, List<FieldDefinition>>) { map, fd ->
                    def words = fd.description =~ /([^\s]+)/
                    def varArg = (0..words.getCount() - 1)
                            .findResult { i -> words[i][0].indexOf("\$var") != -1 ? words[i][0] : null }
                            .substring(1) // remove the initial $

                    def varArgMapEntry = map[varArg]
                    if (varArgMapEntry != null) {
                        varArgMapEntry << fd
                    } else {
                        map << [(varArg): [fd]]
                    }

                    map
                }

        // Collect all field definitions with $calc.<operation>
        fdExprById = definition.fieldDefinitions
                .findAll { fd -> fd.description =~ /[$]calc\./ }
                .inject([:] as Map<Integer, CalcExpr>) { map, fd ->
                    def op = (fd.description =~ /.*[$]calc.([^(]+)/).with { it[0][1] }
                    def args = (fd.description =~ /.*[$]calc.[^(]+\(([^(]+)\)/).with { it[0][1].tokenize(",") }

                    map << [(fd.id): new CalcExpr().with {
                        it.operation = op
                        it.args = args
                        it
                    }]
                }
    }

    Optional<Map<String, String>> calculate(eventMsg) {

    }

    static class CalcExpr {
        // one of: sum, multiply, difference, divide
        String operation;

        // All the arguments from the $calc expression like var.xxxx or any constant value
        List<String> args;

    }

}