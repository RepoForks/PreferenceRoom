/*
 * Copyright (C) 2017 skydoves
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.skydoves.processor;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.List;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

public class PreferenceEntityGenerator {

    private final PreferenceEntityAnnotatedClass annotatedClazz;

    private static final String CLAZZ_PREFIX = "Preference_";
    private static final String FIELD_PREFERENCE = "preference";
    private static final String FIELD_INSTANCE = "instance";
    private static final String CONSTRUCTOR_CONTEXT = "context";

    private static final String EDIT_METHOD = "edit()";
    private static final String CLEAR_METHOD = "clear()";
    private static final String APPLY_METHOD = "apply()";

    public PreferenceEntityGenerator(@NonNull PreferenceEntityAnnotatedClass annotatedClass) {
        this.annotatedClazz = annotatedClass;
    }

    public TypeSpec generate() {
        return TypeSpec.classBuilder(getClazzName())
                .addJavadoc("Generated by PreferenceRoom. (https://github.com/skydoves/PreferenceRoom).\n")
                .addModifiers(PUBLIC)
                .superclass(ClassName.get(annotatedClazz.annotatedElement))
                .addFields(getFieldSpecs())
                .addMethod(getConstructorSpec())
                .addMethod(getInstanceSpec())
                .addMethods(getFieldMethodSpecs())
                .addMethod(getClearSpec())
                .build();
    }

    private List<FieldSpec> getFieldSpecs() {
        List<FieldSpec> fieldSpecs = new ArrayList<>();
        fieldSpecs.add(FieldSpec.builder(SharedPreferences.class, FIELD_PREFERENCE, PRIVATE, FINAL).build());
        fieldSpecs.add(FieldSpec.builder(getClassType(), FIELD_INSTANCE, PRIVATE, STATIC).build());
        return fieldSpecs;
    }

    private MethodSpec getConstructorSpec() {
        return MethodSpec.constructorBuilder()
                .addModifiers(PRIVATE)
                .addParameter(ParameterSpec.builder(Context.class, CONSTRUCTOR_CONTEXT).addAnnotation(NonNull.class).build())
                .addStatement("$N = $N.getSharedPreferences($S, Context.MODE_PRIVATE)", FIELD_PREFERENCE, CONSTRUCTOR_CONTEXT, annotatedClazz.preferenceName)
                .build();
    }

    private MethodSpec getInstanceSpec() {
        return MethodSpec.methodBuilder("getInstance")
                .addModifiers(PUBLIC, STATIC)
                .addParameter(ParameterSpec.builder(Context.class, CONSTRUCTOR_CONTEXT).addAnnotation(NonNull.class).build())
                .addStatement("if($N != null) return $N", FIELD_INSTANCE, FIELD_INSTANCE)
                .addStatement("$N = new $N($N)", FIELD_INSTANCE, getClazzName(), CONSTRUCTOR_CONTEXT)
                .addStatement("return $N", FIELD_INSTANCE)
                .returns(getClassType())
                .build();
    }

    private List<MethodSpec> getFieldMethodSpecs() {
        List<MethodSpec> methodSpecs = new ArrayList<>();
        this.annotatedClazz.keyFields.forEach(annotatedFields -> {
            PreferenceFieldMethodGenerator methodGenerator = new PreferenceFieldMethodGenerator(annotatedFields, annotatedClazz, FIELD_PREFERENCE);
            methodSpecs.addAll(methodGenerator.getFieldMethods());
        });
        return methodSpecs;
    }

    private MethodSpec getClearSpec() {
        return MethodSpec.methodBuilder("clear")
                .addModifiers(PUBLIC)
                .addStatement("$N.$N.$N.$N", FIELD_PREFERENCE, EDIT_METHOD, CLEAR_METHOD, APPLY_METHOD)
                .build();
    }

    private ClassName getClassType() {
        return ClassName.get(annotatedClazz.packageName, getClazzName());
    }

    private String getClazzName() {
        return CLAZZ_PREFIX + annotatedClazz.preferenceName;
    }
}
