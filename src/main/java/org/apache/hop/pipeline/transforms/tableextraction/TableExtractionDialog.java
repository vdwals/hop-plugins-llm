/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.hop.pipeline.transforms.tableextraction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.value.ValueMetaFactory;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.langchain4j.LlmMeta;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.ITransformDialog;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.pipeline.transform.stream.IStream;
import org.apache.hop.ui.core.ConstUi;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.dialog.BaseDialog;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.dialog.MessageBox;
import org.apache.hop.ui.core.widget.ColumnInfo;
import org.apache.hop.ui.core.widget.ComboVar;
import org.apache.hop.ui.core.widget.MetaSelectionLine;
import org.apache.hop.ui.core.widget.TableView;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class TableExtractionDialog extends BaseTransformDialog implements ITransformDialog {
  private static final Class<?> PKG = TableExtractionMeta.class; // For Translator

  private CCombo wTransform;

  private ComboVar wTextField;

  private ColumnInfo[] ciReturn;
  private Label wlReturn;
  private TableView wReturn;

  private MetaSelectionLine<LlmMeta> wLlmModel;

  private Button wGetLookup;

  private final TableExtractionMeta input;
  private boolean gotTextFields = false;

  public TableExtractionDialog(Shell parent, IVariables variables, Object in,
      PipelineMeta pipelineMeta, String sname) {
    super(parent, variables, (TableExtractionMeta) in, pipelineMeta, sname);
    input = (TableExtractionMeta) in;
  }

  @Override
  public String open() {
    Shell parent = getParent();

    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
    PropsUi.setLook(shell);
    setShellImage(shell, input);

    changed = input.hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = PropsUi.getFormMargin();
    formLayout.marginHeight = PropsUi.getFormMargin();

    shell.setLayout(formLayout);
    shell.setText(BaseMessages.getString(PKG, "SemanticSearchDialog.Shell.Title"));

    int middle = props.getMiddlePct();
    int margin = PropsUi.getMargin();

    // TransformName line
    wlTransformName = new Label(shell, SWT.RIGHT);
    wlTransformName
        .setText(BaseMessages.getString(PKG, "SemanticSearchDialog.TransformName.Label"));
    PropsUi.setLook(wlTransformName);
    fdlTransformName = new FormData();
    fdlTransformName.left = new FormAttachment(0, 0);
    fdlTransformName.right = new FormAttachment(middle, -margin);
    fdlTransformName.top = new FormAttachment(0, margin);
    wlTransformName.setLayoutData(fdlTransformName);
    wTransformName = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wTransformName.setText(transformName);
    PropsUi.setLook(wTransformName);
    fdTransformName = new FormData();
    fdTransformName.left = new FormAttachment(middle, 0);
    fdTransformName.top = new FormAttachment(0, margin);
    fdTransformName.right = new FormAttachment(100, 0);
    wTransformName.setLayoutData(fdTransformName);

    // /////////////////////////////////
    // START OF Lookup Fields GROUP
    // /////////////////////////////////
    Group wLookupGroup = generateGroup(shell,
        BaseMessages.getString(PKG, "SemanticSearchDialog.Group.SettingsGroup.Label"));

    // LookupFields
    wTextField = generateCombVar(middle, margin, null, wLookupGroup,
        BaseMessages.getString(PKG, "SemanticSearchDialog.wlLookupTextField.Label"),
        e -> setLookupTextField());

    // TODO: Finalize group?

    // TODO: Add groups?
    // Model
    wLlmModel = new MetaSelectionLine<>(variables, metadataProvider,
        LlmMeta.class,
        shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER,
        BaseMessages.getString(PKG, "SemanticSearchDialog.llmodel.Label"),
        BaseMessages.getString(PKG, "SemanticSearchDialog.llmodel.Tooltip"));

    PropsUi.setLook(wLlmModel);
    FormData fdLlm = new FormData();
    fdLlm.left = new FormAttachment(0, 0);
    fdLlm.right = new FormAttachment(100, 0);
    fdLlm.top = new FormAttachment(wTextField, margin);
    wLlmModel.setLayoutData(fdLlm);
    try {
      wLlmModel.fillItems();
    } catch (Exception e) {
      new ErrorDialog(shell, "Error", "Error getting list of models", e);
    }

    // The buttons go at the bottom
    //
    wOk = new Button(shell, SWT.PUSH);
    wOk.setText(BaseMessages.getString(PKG, "System.Button.OK"));
    wOk.addListener(SWT.Selection, e -> ok());
    wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));
    wCancel.addListener(SWT.Selection, e -> cancel());
    setButtonPositions(new Button[] { wOk, wCancel }, margin, null);

    // THE INSERT TABLE
    wlReturn = new Label(shell, SWT.NONE);
    wlReturn.setText(BaseMessages.getString(PKG, "SemanticSearchDialog.ReturnFields.Label"));
    PropsUi.setLook(wlReturn);
    FormData fdlReturn = new FormData();
    fdlReturn.left = new FormAttachment(0, 0);
    fdlReturn.top = new FormAttachment(wLlmModel, margin);
    wlReturn.setLayoutData(fdlReturn);

    wGetLookup = new Button(shell, SWT.PUSH);
    wGetLookup.setText(BaseMessages.getString(PKG, "SemanticSearchDialog.GetLookupFields.Button"));
    FormData fdlGetLookup = new FormData();
    fdlGetLookup.top = new FormAttachment(wlReturn, margin);
    fdlGetLookup.right = new FormAttachment(100, 0);
    wGetLookup.setLayoutData(fdlGetLookup);
    wGetLookup.addListener(SWT.Selection, e -> getlookup());

    ciReturn = new ColumnInfo[2];
    ciReturn[0] = new ColumnInfo(BaseMessages.getString(PKG, "SemanticSearchDialog.ColumnInfo.FieldReturn"),
        ColumnInfo.COLUMN_TYPE_CCOMBO, new String[] { "" }, false);
    ciReturn[1] = new ColumnInfo(
        BaseMessages.getString(PKG, "SelectValuesDialog.ColumnInfo.Type"),
        ColumnInfo.COLUMN_TYPE_CCOMBO,
        ValueMetaFactory.getAllValueMetaNames(),
        false);

    wReturn = new TableView(variables, shell,
        SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL, ciReturn,
        1, null, props);

    FormData fdReturn = new FormData();
    fdReturn.left = new FormAttachment(0, 0);
    fdReturn.top = new FormAttachment(wlReturn, margin);
    fdReturn.right = new FormAttachment(wGetLookup, -margin);
    fdReturn.bottom = new FormAttachment(100, -3 * margin);
    wReturn.setLayoutData(fdReturn);

    getData();
    setComboBoxesLookup();

    BaseDialog.defaultShellHandling(shell, c -> ok(), c -> cancel());

    return transformName;
  }

  private ComboVar generateCombVar(int middle, int margin, Control wPreviousControl,
      Group wLookupGroup, String label, Listener listener) {
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

  private void generateLabel(int middle, int margin, Control wPreviousControl, Group wCurrentGroup,
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

  private Group generateGroup(Composite wParentComp, String label) {
    Group wSettingsGroup = new Group(wParentComp, SWT.SHADOW_NONE);
    PropsUi.setLook(wSettingsGroup);
    wSettingsGroup.setText(label);

    FormLayout settingsGroupLayout = new FormLayout();
    settingsGroupLayout.marginWidth = 10;
    settingsGroupLayout.marginHeight = 10;
    wSettingsGroup.setLayout(settingsGroupLayout);
    return wSettingsGroup;
  }

  /** Copy information from the meta-data input to the dialog fields. */
  public void getData() {
    if (isDebug()) {
      logDebug(BaseMessages.getString(PKG, "SemanticSearchDialog.Log.GettingKeyInfo"));
    }

    wTextField.setText(Const.NVL(input.getTextField(), ""));

    wLlmModel.setText(Const.NVL(input.getLlModelName(), ""));

    for (int i = 0; i < input.getTargetColumns().size(); i++) {
      TableExtractionMeta.TargetColumn targetColumn = input.getTargetColumns().get(i);
      TableItem item = wReturn.table.getItem(i);
      item.setText(1, Const.NVL(targetColumn.getName(), ""));
      item.setText(2, ValueMetaFactory.getValueMetaName(targetColumn.getType()));
    }

    IStream infoStream = input.getTransformIOMeta().getInfoStreams().get(0);
    wTransform.setText(Const.NVL(infoStream.getTransformName(), ""));

    wReturn.optimizeTableView();

    wTransformName.selectAll();
    wTransformName.setFocus();
  }

  private void cancel() {
    transformName = null;
    dispose();
  }

  private void ok() {
    if (Utils.isEmpty(wTransformName.getText())) {
      return;
    }

    input.setTextField(wTextField.getText());

    input.setLlModelName(wLlmModel.getText());

    input.getTargetColumns().clear();
    for (TableItem item : wReturn.getNonEmptyItems()) {
      TableExtractionMeta.TargetColumn lookupValue = new TableExtractionMeta.TargetColumn();
      lookupValue.setName(item.getText(1));
      lookupValue.setType(ValueMetaFactory.getIdForValueMeta(item.getText(2)));
      input.getTargetColumns().add(lookupValue);
    }

    transformName = wTransformName.getText(); // return value
    input.setChanged();
    dispose();
  }

  private void setLookupTextField() {
    if (!gotTextFields) {
      String field = wTextField.getText();
      try {
        wTextField.removeAll();

        IRowMeta r = pipelineMeta.getTransformFields(variables, wTransform.getText());
        if (r != null) {
          String[] stringTypeFieldNames = r.getValueMetaList().stream()
              .filter(meta -> meta.getType() == IValueMeta.TYPE_STRING)
              .map(meta -> meta.getName()).collect(Collectors.toList()).toArray(new String[0]);
          wTextField.setItems(stringTypeFieldNames);
        }
      } catch (HopException ke) {
        new ErrorDialog(shell,
            BaseMessages.getString(PKG, "SemanticSearchDialog.FailedToGetLookupFields.DialogTitle"),
            BaseMessages.getString(PKG,
                "SemanticSearchDialog.FailedToGetLookupFields.DialogMessage"),
            ke);
      }
      if (field != null) {
        wTextField.setText(field);
      }
      gotTextFields = true;
    }
  }

  private void getlookup() {
    try {
      String transformFrom = wTransform.getText();
      if (!Utils.isEmpty(transformFrom)) {
        IRowMeta r = pipelineMeta.getTransformFields(variables, transformFrom);
        if (r != null && !r.isEmpty()) {
          BaseTransformDialog.getFieldsFromPrevious(r, wReturn, 1, new int[] { 1 }, new int[] { 4 }, -1,
              -1, null);
        } else {
          MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
          mb.setMessage(
              BaseMessages.getString(PKG, "SemanticSearchDialog.CouldNotFindFields.DialogMessage"));
          mb.setText(
              BaseMessages.getString(PKG, "SemanticSearchDialog.CouldNotFindFields.DialogTitle"));
          mb.open();
        }
      } else {
        MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
        mb.setMessage(BaseMessages.getString(PKG,
            "SemanticSearchDialog.TransformNameRequired.DialogMessage"));
        mb.setText(
            BaseMessages.getString(PKG, "SemanticSearchDialog.TransformNameRequired.DialogTitle"));
        mb.open();
      }
    } catch (HopException ke) {
      new ErrorDialog(shell,
          BaseMessages.getString(PKG, "SemanticSearchDialog.FailedToGetFields.DialogTitle"),
          BaseMessages.getString(PKG, "SemanticSearchDialog.FailedToGetFields.DialogMessage"), ke);
    }
  }

  protected void setComboBoxesLookup() {
    Runnable fieldLoader = () -> {
      TransformMeta lookupTransformMeta = pipelineMeta.findTransform(wTransform.getText());
      if (lookupTransformMeta != null) {
        try {
          IRowMeta row = pipelineMeta.getTransformFields(variables, lookupTransformMeta);
          List<String> lookupFields = new ArrayList<>();
          // Remember these fields...
          for (int i = 0; i < row.size(); i++) {
            lookupFields.add(row.getValueMeta(i).getName());
          }

          // Something was changed in the row.
          //
          String[] fieldNames = ConstUi.sortFieldNames(lookupFields);
          // return fields
          ciReturn[0].setComboValues(fieldNames);

          gotTextFields = false;
        } catch (HopException e) {
          logError("It was not possible to retrieve the list of fields for transform ["
              + wTransform.getText() + "]!");
        }
      }
    };
    shell.getDisplay().asyncExec(fieldLoader);
  }
}
