package com.cultofbits.customizations.utils

import com.cultofbits.integrationm.service.dictionary.recordm.Definition
import com.cultofbits.integrationm.service.dictionary.recordm.FieldDefinition
import com.cultofbits.integrationm.service.dictionary.recordm.RecordmInstance

class InstanceBuilder {

    def instance = [
            id    : null,
            fields: []
    ];

    static InstanceBuilder anInstance(Definition definition, Integer id) {
        return new InstanceBuilder()
    }

    def withField(FieldDefinition fieldDefinition, Long fieldId, String value) {
        def field = [
                id             : fieldId,
                fieldDefinition: fieldDefinition,
                value          : value
        ]

        instance.fields << field

        this
    }

    def build() {
        return new RecordmInstance(instance)
    }
}


//public class InstanceBuilder {
//
//    private Instance instance;
//
//    public static InstanceBuilder anInstance(Definition definition) {
//        InstanceBuilder builder = new InstanceBuilder();
//        builder.instance = new Instance(definition, true, true);
//        return builder;
//    }
//
//    public InstanceBuilder id(Integer id) {
//        instance.id = id;
//        return this;
//    }
//
//    public InstanceBuilder version(Integer version) {
//        instance.version = version;
//        return this;
//    }
//
//    public InstanceBuilder fieldValue(String field, String value) {
//        instance.getFieldsByName(field).forEach(f -> f.setValue(value));
//        return this;
//    }
//
//    public InstanceBuilder fieldValue(FieldDefinition field, String value) {
//        instance.getFields(field).forEach(f -> f.setValue(value));
//        return this;
//    }
//
//    public Instance build() {
//        return instance;
//    }
//}
