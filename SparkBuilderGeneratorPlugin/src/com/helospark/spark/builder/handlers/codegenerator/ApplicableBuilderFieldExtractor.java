package com.helospark.spark.builder.handlers.codegenerator;

import static com.helospark.spark.builder.preferences.PluginPreferenceList.INCLUDE_VISIBLE_FIELDS_FROM_SUPERCLASS;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.helospark.spark.builder.handlers.codegenerator.component.helper.ApplicableFieldVisibilityFilter;
import com.helospark.spark.builder.handlers.codegenerator.component.helper.FieldNameToBuilderFieldNameConverter;
import com.helospark.spark.builder.handlers.codegenerator.component.helper.TypeDeclarationFromSuperclassExtractor;
import com.helospark.spark.builder.handlers.codegenerator.domain.NamedVariableDeclarationField;
import com.helospark.spark.builder.preferences.PreferencesManager;

/**
 * Filters and converts given fields to {@link NamedVariableDeclarationField}.
 *
 * @author helospark
 */
public class ApplicableBuilderFieldExtractor {
    private FieldNameToBuilderFieldNameConverter fieldNameToBuilderFieldNameConverter;
    private PreferencesManager preferencesManager;
    private TypeDeclarationFromSuperclassExtractor typeDeclarationFromSuperclassExtractor;
    private ApplicableFieldVisibilityFilter applicableFieldVisibilityFilter;

    public ApplicableBuilderFieldExtractor(FieldNameToBuilderFieldNameConverter fieldNameToBuilderFieldNameConverter, PreferencesManager preferencesManager,
            TypeDeclarationFromSuperclassExtractor typeDeclarationFromSuperclassExtractor, ApplicableFieldVisibilityFilter applicableFieldVisibilityFilter) {
        this.fieldNameToBuilderFieldNameConverter = fieldNameToBuilderFieldNameConverter;
        this.preferencesManager = preferencesManager;
        this.typeDeclarationFromSuperclassExtractor = typeDeclarationFromSuperclassExtractor;
        this.applicableFieldVisibilityFilter = applicableFieldVisibilityFilter;
    }

    public List<NamedVariableDeclarationField> findBuilderFields(TypeDeclaration typeDeclaration) {
        return findBuilderFieldsRecursively(typeDeclaration, typeDeclaration);
    }

    private List<NamedVariableDeclarationField> findBuilderFieldsRecursively(TypeDeclaration originalOwnerClasss, TypeDeclaration currentOwnerClass) {
        List<NamedVariableDeclarationField> namedVariableDeclarations = new ArrayList<>();

        if (preferencesManager.getPreferenceValue(INCLUDE_VISIBLE_FIELDS_FROM_SUPERCLASS)) {
            namedVariableDeclarations.addAll(getFieldsFromSuperclass(currentOwnerClass));
        }

        FieldDeclaration[] fields = currentOwnerClass.getFields();
        for (FieldDeclaration field : fields) {
            List<VariableDeclarationFragment> fragments = field.fragments();
            namedVariableDeclarations.addAll(getFilteredDeclarations(field, fragments));
        }
        return namedVariableDeclarations;
    }

    private List<NamedVariableDeclarationField> getFieldsFromSuperclass(TypeDeclaration currentTypeDeclaration) {
        return typeDeclarationFromSuperclassExtractor.extractTypeDeclarationFromSuperClass(currentTypeDeclaration)
                .map(parentTypeDeclaration -> findBuilderFieldsRecursively(currentTypeDeclaration, parentTypeDeclaration))
                .map(fields -> applicableFieldVisibilityFilter.filterSuperClassFieldsToVisibleFields(fields, currentTypeDeclaration))
                .orElse(emptyList());
    }

    private List<NamedVariableDeclarationField> getFilteredDeclarations(FieldDeclaration field, List<VariableDeclarationFragment> fragments) {
        return fragments.stream()
                .filter(variableFragment -> !isStatic(field))
                .map(variableFragment -> createNamedVariableDeclarations(variableFragment, field))
                .collect(Collectors.toList());
    }

    private NamedVariableDeclarationField createNamedVariableDeclarations(VariableDeclarationFragment variableDeclarationFragment, FieldDeclaration fieldDeclaration) {
        String originalFieldName = variableDeclarationFragment.getName().toString();
        String builderFieldName = fieldNameToBuilderFieldNameConverter.convertFieldName(originalFieldName);
        return NamedVariableDeclarationField.builder()
                .withFieldDeclaration(fieldDeclaration)
                .withOriginalFieldName(originalFieldName)
                .withBuilderFieldName(builderFieldName)
                .build();
    }

    private boolean isStatic(FieldDeclaration field) {
        List<IExtendedModifier> fieldModifiers = field.modifiers();
        return fieldModifiers.stream()
                .filter(modifier -> modifier instanceof Modifier)
                .filter(modifer -> ((Modifier) modifer).getKeyword().equals(ModifierKeyword.STATIC_KEYWORD))
                .findFirst()
                .isPresent();
    }
}
