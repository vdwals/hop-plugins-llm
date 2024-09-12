package org.apache.hop.langchain4j.utils;

import org.apache.hop.core.variables.IVariables;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.widget.ComboVar;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

public final class GuiUtils {

    public static void generateLabel(int middle, int margin, Control wPreviousControl, Group wCurrentGroup,
            String label) {
        Label wlModel = new Label(wCurrentGroup, SWT.RIGHT);
        wlModel.setText(label);
        PropsUi.setLook(wlModel);
        FormData fdlModel = new FormData();
        fdlModel.left = new FormAttachment(0, 0);
        fdlModel.right = new FormAttachment(middle, -margin);
        fdlModel.top = new FormAttachment(wPreviousControl, margin);
        wlModel.setLayoutData(fdlModel);
    }

    public static ComboVar generateCombVar(int middle, int margin, Control wPreviousControl,
            Group wLookupGroup, String label, Listener listener, IVariables variables) {
        generateLabel(middle, margin, wPreviousControl, wLookupGroup, label);

        ComboVar wComboVarField = new ComboVar(variables, wLookupGroup, SWT.BORDER | SWT.READ_ONLY);
        wComboVarField.setEditable(true);
        PropsUi.setLook(wComboVarField);
        FormData fdLookupTextField = new FormData();
        fdLookupTextField.left = new FormAttachment(middle, 0);
        fdLookupTextField.top = new FormAttachment(wPreviousControl, margin);
        fdLookupTextField.right = new FormAttachment(100, -margin);
        wComboVarField.setLayoutData(fdLookupTextField);
        wComboVarField.addListener(SWT.FocusIn, listener);

        return wComboVarField;
    }

    public static Group generateGroup(Composite wParentComp, String label) {
        Group wSettingsGroup = new Group(wParentComp, SWT.SHADOW_NONE);
        PropsUi.setLook(wSettingsGroup);
        wSettingsGroup.setText(label);

        FormLayout settingsGroupLayout = new FormLayout();
        settingsGroupLayout.marginWidth = 10;
        settingsGroupLayout.marginHeight = 10;
        wSettingsGroup.setLayout(settingsGroupLayout);
        return wSettingsGroup;
    }

    public static void finalizeGroup(int margin, Control cPreviousControl, Group wGroup) {
        FormData fdSettingsGroup = new FormData();
        fdSettingsGroup.left = new FormAttachment(0, margin);
        fdSettingsGroup.top = new FormAttachment(cPreviousControl, margin);
        fdSettingsGroup.right = new FormAttachment(100, -margin);
        wGroup.setLayoutData(fdSettingsGroup);
    }
}
