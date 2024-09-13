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

import java.util.stream.Collectors;
import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.value.ValueMetaFactory;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.langchain4j.languagemodels.LanguageModelMeta;
import org.apache.hop.langchain4j.utils.GuiUtils;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.ITransformDialog;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.dialog.BaseDialog;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.widget.ColumnInfo;
import org.apache.hop.ui.core.widget.ComboVar;
import org.apache.hop.ui.core.widget.MetaSelectionLine;
import org.apache.hop.ui.core.widget.TableView;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class TableExtractionDialog extends BaseTransformDialog implements ITransformDialog {
  private static final int INDEX_FIELD_NAME = 1;
  private static final int INDEX_TYPE = 3;
  private static final int INDEX_DESCRIPTION = 2;
  private static final int INDEX_FORMAT_FIELD = 4;

  private static final Class<?> PKG = TableExtractionMeta.class; // For Translator

  private ComboVar wTextField;

  private Label wlReturn;
  private TableView wOutput;

  private MetaSelectionLine<LanguageModelMeta> wLlmModel;

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
    shell.setText(BaseMessages.getString(PKG, "TableExtraction.Shell.Title"));

    int middle = props.getMiddlePct();
    int margin = PropsUi.getMargin();

    // TransformName line
    wlTransformName = new Label(shell, SWT.RIGHT);
    wlTransformName
        .setText(BaseMessages.getString(PKG, "TableExtraction.TransformName.Label"));
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

    Group wLookupGroup = getWLookupGroup(wTransformName, middle, margin);

    getFdReturn(margin, wLookupGroup);

    // The buttons go at the bottom
    //
    wOk = new Button(shell, SWT.PUSH);
    wOk.setText(BaseMessages.getString(PKG, "System.Button.OK"));
    wOk.addListener(SWT.Selection, e -> ok());
    wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));
    wCancel.addListener(SWT.Selection, e -> cancel());
    setButtonPositions(new Button[] { wOk, wCancel }, margin, null);

    getData();

    BaseDialog.defaultShellHandling(shell, c -> ok(), c -> cancel());

    return transformName;
  }

  private void getFdReturn(int margin, Control previous) {
    Group wExtractTable = GuiUtils.generateGroup(shell,
        BaseMessages.getString(PKG, "TableExtraction.Group.ExtractTable.Label"));

    wlReturn = new Label(wExtractTable, SWT.NONE);
    wlReturn.setText(BaseMessages.getString(PKG, "TableExtraction.ReturnFields.Label"));
    PropsUi.setLook(wlReturn);
    FormData fdlReturn = new FormData();
    fdlReturn.left = new FormAttachment(0, 0);
    fdlReturn.top = new FormAttachment(previous, margin);
    wlReturn.setLayoutData(fdlReturn);

    ColumnInfo[] colmeta = new ColumnInfo[] {
        new ColumnInfo(BaseMessages.getString(PKG, "TableExtraction.ColumnInfo.FieldReturn"),
            ColumnInfo.COLUMN_TYPE_TEXT, false),
        new ColumnInfo(BaseMessages.getString(PKG, "TableExtraction.ColumnInfo.Description"),
            ColumnInfo.COLUMN_TYPE_TEXT, false),
        new ColumnInfo(
            BaseMessages.getString(PKG, "TableExtraction.ColumnInfo.Type"),
            ColumnInfo.COLUMN_TYPE_CCOMBO,
            ValueMetaFactory.getAllValueMetaNames(),
            false),
        new ColumnInfo(
            BaseMessages.getString(PKG, "SelectValuesDialog.ColumnInfo.Format"),
            ColumnInfo.COLUMN_TYPE_FORMAT,
            3),
    };

    wOutput = new TableView(variables, wExtractTable,
        SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, colmeta,
        15, null, props);

    FormData fdReturn = new FormData();
    fdReturn.left = new FormAttachment(0, 0);
    fdReturn.top = new FormAttachment(wlReturn, margin);
    fdReturn.bottom = new FormAttachment(100, -2 * margin);
    wOutput.setLayoutData(fdReturn);

    GuiUtils.finalizeGroup(margin, previous, wExtractTable, wOk);
  }

  private Group getWLookupGroup(Control previous, int middle, int margin) {
    Group wLookupGroup = GuiUtils.generateGroup(shell,
        BaseMessages.getString(PKG, "TableExtraction.Group.Settings.Label"));

    // LookupFields
    wTextField = GuiUtils.generateCombVar(middle, margin, previous, wLookupGroup,
        BaseMessages.getString(PKG, "TableExtraction.wTextField.Label"),
        e -> setLookupTextField(), variables);

    // Model
    wLlmModel = new MetaSelectionLine<>(variables, metadataProvider,
        LanguageModelMeta.class,
        wLookupGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER,
        BaseMessages.getString(PKG, "TableExtraction.llmodel.Label"),
        BaseMessages.getString(PKG, "TableExtraction.llmodel.Tooltip"));

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

    GuiUtils.finalizeGroup(margin, wTransformName, wLookupGroup, null);
    return wLookupGroup;
  }

  /** Copy information from the meta-data input to the dialog fields. */
  public void getData() {
    if (isDebug()) {
      logDebug(BaseMessages.getString(PKG, "SemanticSearchDialog.Log.GettingKeyInfo"));
    }

    wTextField.setText(Const.NVL(input.getTextField(), ""));

    wLlmModel.setText(Const.NVL(input.getLanguageModelName(), ""));

    for (int i = 0; i < input.getTargetColumns().size(); i++) {
      TableExtractionMeta.TargetColumn targetColumn = input.getTargetColumns().get(i);
      TableItem item = wOutput.table.getItem(i);
      item.setText(INDEX_FIELD_NAME, Const.NVL(targetColumn.getName(), ""));
      item.setText(INDEX_DESCRIPTION, Const.NVL(targetColumn.getDescription(), ""));
      item.setText(INDEX_TYPE, ValueMetaFactory.getValueMetaName(targetColumn.getType()));
      item.setText(INDEX_FORMAT_FIELD, Const.NVL(targetColumn.getFormat(), ""));
    }

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

    input.setLanguageModelName(wLlmModel.getText());

    input.getTargetColumns().clear();
    for (TableItem item : wOutput.getNonEmptyItems()) {
      input.getTargetColumns().add(
          new TableExtractionMeta.TargetColumn(item.getText(INDEX_FIELD_NAME), item.getText(INDEX_DESCRIPTION),
              ValueMetaFactory.getIdForValueMeta(item.getText(INDEX_TYPE)),
              item.getText(INDEX_FORMAT_FIELD)));
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

        IRowMeta r = pipelineMeta.getPrevTransformFields(variables, transformName);
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
}
