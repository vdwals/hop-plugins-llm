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

package org.apache.hop.pipeline.transforms.semanticsearch;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hop.core.Const;
import org.apache.hop.core.Props;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.langchain4j.EmbeddingStore;
import org.apache.hop.langchain4j.LlmMeta;
import org.apache.hop.neo4j.shared.NeoConnection;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.ITransformDialog;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.pipeline.transform.stream.IStream;
import org.apache.hop.ui.core.ConstUi;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.dialog.BaseDialog;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.dialog.MessageBox;
import org.apache.hop.ui.core.gui.GuiResource;
import org.apache.hop.ui.core.widget.ColumnInfo;
import org.apache.hop.ui.core.widget.ComboVar;
import org.apache.hop.ui.core.widget.MetaSelectionLine;
import org.apache.hop.ui.core.widget.TableView;
import org.apache.hop.ui.core.widget.TextVar;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
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

public class SemanticSearchDialog extends BaseTransformDialog implements ITransformDialog {
  private static final Class<?> PKG = SemanticSearchMeta.class; // For Translator

  private CCombo wTransform;

  private CCombo wStore;
  private TextVar wChromaUrl;

  private ComboVar wMainStreamField;

  private ComboVar wLookupTextField;
  private ComboVar wLookupKeyField;

  private ColumnInfo[] ciReturn;
  private Label wlReturn;
  private TableView wReturn;

  private TextVar wMatchField;
  private TextVar wKeyField;
  private TextVar wMaxValue;

  private MetaSelectionLine<LlmMeta> wLlmModel;

  private Button wGetLookup;

  private MetaSelectionLine<NeoConnection> wNeo4JConnection;

  private final SemanticSearchMeta input;
  private boolean gotPreviousFields = false;
  private boolean gotLookupTextFields = false;
  private boolean gotLookupKeyFields = false;

  private TextVar wDistanceField;

  public SemanticSearchDialog(Shell parent, IVariables variables, Object in,
      PipelineMeta pipelineMeta, String sname) {
    super(parent, variables, (SemanticSearchMeta) in, pipelineMeta, sname);
    input = (SemanticSearchMeta) in;
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

    CTabFolder wTabFolder = new CTabFolder(shell, SWT.BORDER);
    PropsUi.setLook(wTabFolder, Props.WIDGET_STYLE_TAB);

    // ////////////////////////
    // START OF General TAB ///
    // ////////////////////////
    Pair<Composite, CTabItem> tabItems = generateTab(wTabFolder,
        BaseMessages.getString(PKG, "SemanticSearchDialog.General.Tab"));
    Composite wGeneralComp = tabItems.getLeft();
    CTabItem wGeneralTab = tabItems.getRight();

    // /////////////////////////////////
    // START OF Lookup Fields GROUP
    // /////////////////////////////////
    Group wLookupGroup = generateGroup(wGeneralComp,
        BaseMessages.getString(PKG, "SemanticSearchDialog.Group.SettingsGroup.Label"));

    // Source transform line...
    List<String> collect = pipelineMeta.findPreviousTransforms(pipelineMeta.findTransform(transformName), true)
        .stream().map(TransformMeta::getName).collect(Collectors.toList());
    String[] transformNames = (String[]) collect.toArray(new String[collect.size()]);

    wTransform = generateCCombo(middle, margin, wTransformName, wLookupGroup, transformNames,
        e -> setComboBoxesLookup(),
        BaseMessages.getString(PKG, "SemanticSearchDialog.SourceTransform.Label"));

    // LookupFields
    wLookupTextField = generateCombVar(middle, margin, wTransform, wLookupGroup,
        BaseMessages.getString(PKG, "SemanticSearchDialog.wlLookupTextField.Label"),
        e -> setLookupTextField());

    wLookupKeyField = generateCombVar(middle, margin, wLookupTextField, wLookupGroup,
        BaseMessages.getString(PKG, "SemanticSearchDialog.wlLookupKeyField.Label"),
        e -> setLookupKeyField());

    finalizeGroup(margin, wTransformName, wLookupGroup);

    // ///////////////////////////////////////////////////////////
    // / END OF Lookup GROUP
    // ///////////////////////////////////////////////////////////

    // /////////////////////////////////
    // START OF MainStream Fields GROUP
    // /////////////////////////////////

    Group wMainStreamGroup = new Group(wGeneralComp, SWT.SHADOW_NONE);
    PropsUi.setLook(wMainStreamGroup);
    wMainStreamGroup
        .setText(BaseMessages.getString(PKG, "SemanticSearchDialog.Group.MainStreamGroup.Label"));

    FormLayout mainStreamGroupLayout = new FormLayout();
    mainStreamGroupLayout.marginWidth = 10;
    mainStreamGroupLayout.marginHeight = 10;
    wMainStreamGroup.setLayout(mainStreamGroupLayout);

    // MainStreamFieldName field
    wMainStreamField = generateCombVar(middle, margin, wLookupGroup, wMainStreamGroup,
        BaseMessages.getString(PKG, "SemanticSearchDialog.wlMainStreamField.Label"),
        e -> setMainStreamField());

    finalizeGroup(margin, wLookupGroup, wMainStreamGroup);

    // ///////////////////////////////////////////////////////////
    // / END OF MainStream GROUP
    // ///////////////////////////////////////////////////////////

    wGeneralComp.layout();
    wGeneralTab.setControl(wGeneralComp);

    // ///////////////////////////////////////////////////////////
    // / END OF General TAB
    // ///////////////////////////////////////////////////////////

    // The buttons go at the bottom
    //
    wOk = new Button(shell, SWT.PUSH);
    wOk.setText(BaseMessages.getString(PKG, "System.Button.OK"));
    wOk.addListener(SWT.Selection, e -> ok());
    wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));
    wCancel.addListener(SWT.Selection, e -> cancel());
    setButtonPositions(new Button[] { wOk, wCancel }, margin, null);

    // ////////////////////////
    // START OF Model TAB ///
    // ////////////////////////
    tabItems = generateTab(wTabFolder, BaseMessages.getString(PKG, "SemanticSearchDialog.Model.Tab"));
    Composite wModelComp = tabItems.getLeft();
    CTabItem wModelTab = tabItems.getRight();

    // /////////////////////////////////
    // START OF Settings Fields GROUP
    // /////////////////////////////////

    Group wSettingsGroup = generateGroup(wModelComp,
        BaseMessages.getString(PKG, "SemanticSearchDialog.Group.SettingsGroup.Label"));

    // Model
    wLlmModel = new MetaSelectionLine<>(variables, metadataProvider, 
        LlmMeta.class,
        wSettingsGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER,
        BaseMessages.getString(PKG, "SemanticSearchDialog.llmodel.Label"),
        BaseMessages.getString(PKG, "SemanticSearchDialog.llmodel.Tooltip"));

    this.wMaxValue = generateTextVar(middle, margin, wLlmModel, wSettingsGroup,
        BaseMessages.getString(PKG, "SemanticSearchDialog.maxValue.Label"),
        BaseMessages.getString(PKG, "SemanticSearchDialog.maxValue.Tooltip"));

    PropsUi.setLook(wLlmModel);
    FormData fdLlm = new FormData();
    fdLlm.left = new FormAttachment(0, 0);
    fdLlm.right = new FormAttachment(100, 0);
    fdLlm.top = new FormAttachment(wStore, margin);
    wLlmModel.setLayoutData(fdLlm);
    try {
      wLlmModel.fillItems();
    } catch (Exception e) {
      new ErrorDialog(shell, "Error", "Error getting list of models", e);
    }

    finalizeGroup(margin, wMainStreamGroup, wSettingsGroup);

    // ///////////////////////////////////////////////////////////
    // / END OF Settings GROUP
    // ///////////////////////////////////////////////////////////

    wModelComp.layout();
    wModelTab.setControl(wModelComp);

    // ///////////////////////////////////////////////////////////
    // / END OF Model TAB
    // //////////////////////////////////////////////////////////

    // ////////////////////////
    // START OF Storage TAB ///
    // ////////////////////////
    tabItems = generateTab(wTabFolder, BaseMessages.getString(PKG, "SemanticSearchDialog.Store.Tab"));
    Composite wStoreComp = tabItems.getLeft();
    CTabItem wStoreTab = tabItems.getRight();

    // /////////////////////////////////
    // START OF Settings Fields GROUP
    // /////////////////////////////////

    Group wStoreGroup = generateGroup(wStoreComp,
        BaseMessages.getString(PKG, "SemanticSearchDialog.Group.SettingsGroup.Label"));

    // Storage
    wStore = generateCCombo(middle, margin, wSettingsGroup, wStoreGroup,
        EmbeddingStore.getDescriptions(), e -> activeStore(),
        BaseMessages.getString(PKG, "SemanticSearchDialog.Model.Label"));

    wNeo4JConnection = new MetaSelectionLine<>(variables, metadataProvider, NeoConnection.class,
        wStoreGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER,
        BaseMessages.getString(PKG, "SemanticSearchDialog.neo4j.Label"),
        BaseMessages.getString(PKG, "SemanticSearchDialog.neo4j.Tooltip"));

    PropsUi.setLook(wNeo4JConnection);
    FormData fdConnection = new FormData();
    fdConnection.left = new FormAttachment(0, 0);
    fdConnection.right = new FormAttachment(100, 0);
    fdConnection.top = new FormAttachment(wStore, margin);
    wNeo4JConnection.setLayoutData(fdConnection);
    try {
      wNeo4JConnection.fillItems();
    } catch (Exception e) {
      new ErrorDialog(shell, "Error", "Error getting list of connections", e);
    }

    this.wChromaUrl = generateTextVar(middle, margin, wNeo4JConnection, wStoreGroup,
        BaseMessages.getString(PKG, "SemanticSearchDialog.chroma.Label"),
        BaseMessages.getString(PKG, "SemanticSearchDialog.chroma.Tooltip"));

    finalizeGroup(margin, wSettingsGroup, wStoreGroup);

    // ///////////////////////////////////////////////////////////
    // / END OF Settings GROUP
    // ///////////////////////////////////////////////////////////

    wStoreComp.layout();
    wStoreTab.setControl(wStoreComp);

    // ///////////////////////////////////////////////////////////
    // / END OF Storage TAB
    // ///////////////////////////////////////////////////////////

    // ////////////////////////
    // START OF Fields TAB ///
    // ////////////////////////
    tabItems = generateTab(wTabFolder, BaseMessages.getString(PKG, "SemanticSearchDialog.Fields.Tab"));
    Composite wFieldsComp = tabItems.getLeft();
    CTabItem wFieldsTab = tabItems.getRight();

    // /////////////////////////////////
    // START OF OutputFields Fields GROUP
    // /////////////////////////////////

    Group wOutputFieldsGroup = generateGroup(wFieldsComp,
        BaseMessages.getString(PKG, "SemanticSearchDialog.Group.OutputFieldsGroup.Label"));

    this.wMatchField = generateTextVar(middle, margin, wSettingsGroup, wOutputFieldsGroup,
        BaseMessages.getString(PKG, "SemanticSearchDialog.MatchField.Label"), null);

    this.wKeyField = generateTextVar(middle, margin, wMatchField, wOutputFieldsGroup,
        BaseMessages.getString(PKG, "SemanticSearchDialog.KeyField.Label"), null);

    this.wDistanceField = generateTextVar(middle, margin, wKeyField, wOutputFieldsGroup,
        BaseMessages.getString(PKG, "SemanticSearchDialog.DistanceField.Label"), null);

    finalizeGroup(margin, wSettingsGroup, wOutputFieldsGroup);

    // ///////////////////////////////////////////////////////////
    // / END OF OutputFields GROUP
    // ///////////////////////////////////////////////////////////

    // THE UPDATE/INSERT TABLE
    wlReturn = new Label(wFieldsComp, SWT.NONE);
    wlReturn.setText(BaseMessages.getString(PKG, "SemanticSearchDialog.ReturnFields.Label"));
    PropsUi.setLook(wlReturn);
    FormData fdlReturn = new FormData();
    fdlReturn.left = new FormAttachment(0, 0);
    fdlReturn.top = new FormAttachment(wOutputFieldsGroup, margin);
    wlReturn.setLayoutData(fdlReturn);

    wGetLookup = new Button(wFieldsComp, SWT.PUSH);
    wGetLookup.setText(BaseMessages.getString(PKG, "SemanticSearchDialog.GetLookupFields.Button"));
    FormData fdlGetLookup = new FormData();
    fdlGetLookup.top = new FormAttachment(wlReturn, margin);
    fdlGetLookup.right = new FormAttachment(100, 0);
    wGetLookup.setLayoutData(fdlGetLookup);
    wGetLookup.addListener(SWT.Selection, e -> getlookup());

    int upInsCols = 2;
    int upInsRows = input.getLookupValues().size();

    ciReturn = new ColumnInfo[upInsCols];
    ciReturn[0] = new ColumnInfo(BaseMessages.getString(PKG, "SemanticSearchDialog.ColumnInfo.FieldReturn"),
        ColumnInfo.COLUMN_TYPE_CCOMBO, new String[] { "" }, false);
    ciReturn[1] = new ColumnInfo(BaseMessages.getString(PKG, "SemanticSearchDialog.ColumnInfo.NewName"),
        ColumnInfo.COLUMN_TYPE_TEXT, false);

    wReturn = new TableView(variables, wFieldsComp,
        SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL, ciReturn,
        upInsRows, null, props);

    FormData fdReturn = new FormData();
    fdReturn.left = new FormAttachment(0, 0);
    fdReturn.top = new FormAttachment(wlReturn, margin);
    fdReturn.right = new FormAttachment(wGetLookup, -margin);
    fdReturn.bottom = new FormAttachment(100, -3 * margin);
    wReturn.setLayoutData(fdReturn);

    wFieldsComp.layout();
    wFieldsTab.setControl(wFieldsComp);

    // ///////////////////////////////////////////////////////////
    // / END OF Fields TAB
    // ///////////////////////////////////////////////////////////

    FormData fdTabFolder = new FormData();
    fdTabFolder.left = new FormAttachment(0, 0);
    fdTabFolder.top = new FormAttachment(wTransformName, margin);
    fdTabFolder.right = new FormAttachment(100, 0);
    fdTabFolder.bottom = new FormAttachment(wOk, -margin);
    wTabFolder.setLayoutData(fdTabFolder);

    wTabFolder.setSelection(0);

    getData();
    setComboBoxesLookup();

    BaseDialog.defaultShellHandling(shell, c -> ok(), c -> cancel());

    return transformName;
  }

  private TextVar generateTextVar(int middle, int margin, Control wPreviousControl, Group wGroup,
      String label, String tooltip) {

    generateLabel(middle, margin, wPreviousControl, wGroup, label);

    TextVar wTextField = new TextVar(variables, wGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wTextField);

    if (tooltip != null)
      wTextField.setToolTipText(tooltip);

    FormData fdMatchField = new FormData();
    fdMatchField.left = new FormAttachment(middle, 0);
    fdMatchField.top = new FormAttachment(wPreviousControl, margin);
    fdMatchField.right = new FormAttachment(100, 0);
    wTextField.setLayoutData(fdMatchField);
    return wTextField;
  }

  private void finalizeGroup(int margin, Control cPreviousControl, Group wGroup) {
    FormData fdSettingsGroup = new FormData();
    fdSettingsGroup.left = new FormAttachment(0, margin);
    fdSettingsGroup.top = new FormAttachment(cPreviousControl, margin);
    fdSettingsGroup.right = new FormAttachment(100, -margin);
    wGroup.setLayoutData(fdSettingsGroup);
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

  private CCombo generateCCombo(int middle, int margin, Control previousControl, Group wGroup,
      String[] items, Listener listener, String label) {

    generateLabel(middle, margin, previousControl, wGroup, label);

    CCombo wCCombo = new CCombo(wGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wCCombo);
    for (String transformMeta : items) {
      wCCombo.add(transformMeta);
    }
    wCCombo.addListener(SWT.Selection, listener);

    FormData fdTransform = new FormData();
    fdTransform.left = new FormAttachment(middle, 0);
    fdTransform.top = new FormAttachment(previousControl, margin);
    fdTransform.right = new FormAttachment(100, 0);
    wCCombo.setLayoutData(fdTransform);

    return wCCombo;
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

  private Pair<Composite, CTabItem> generateTab(CTabFolder wTabFolder, String label) {
    CTabItem wTab = new CTabItem(wTabFolder, SWT.NONE);
    wTab.setFont(GuiResource.getInstance().getFontDefault());
    wTab.setText(label);

    Composite wComp = new Composite(wTabFolder, SWT.NONE);
    PropsUi.setLook(wComp);

    FormLayout generalLayout = new FormLayout();
    generalLayout.marginWidth = 3;
    generalLayout.marginHeight = 3;
    wComp.setLayout(generalLayout);

    FormData fdGeneralComp = new FormData();
    fdGeneralComp.left = new FormAttachment(0, 0);
    fdGeneralComp.top = new FormAttachment(0, 0);
    fdGeneralComp.right = new FormAttachment(100, 0);
    fdGeneralComp.bottom = new FormAttachment(100, 0);
    wComp.setLayoutData(fdGeneralComp);

    return new ImmutablePair<Composite, CTabItem>(wComp, wTab);
  }

  /** Copy information from the meta-data input to the dialog fields. */
  public void getData() {
    if (isDebug()) {
      logDebug(BaseMessages.getString(PKG, "SemanticSearchDialog.Log.GettingKeyInfo"));
    }

    wMainStreamField.setText(Const.NVL(input.getMainStreamField(), ""));
    wLookupTextField.setText(Const.NVL(input.getLookupTextField(), ""));
    wLookupKeyField.setText(Const.NVL(input.getLookupKeyField(), ""));
    wMatchField.setText(Const.NVL(input.getOutputMatchField(), ""));
    wKeyField.setText(Const.NVL(input.getOutputKeyField(), ""));
    wMaxValue.setText(Const.NVL(input.getMaximalValue(), ""));
    wDistanceField.setText(Const.NVL(input.getOutputDistanceField(), ""));

    wLlmModel.setText(Const.NVL(input.getLlModelName(), ""));

    wStore.setText(Const.NVL(input.getEmbeddingStore().getDescription(),
        EmbeddingStore.IN_MEMORY.getDescription()));
    activeStore();

    wNeo4JConnection.setText(Const.NVL(input.getNeo4JConnectionName(), ""));
    wChromaUrl.setText(Const.NVL(input.getChromaUrl(), ""));
    wMaxValue.setText(Const.NVL(input.getMaximalValue(), "1"));

    for (int i = 0; i < input.getLookupValues().size(); i++) {
      SemanticSearchMeta.SLookupValue lookupValue = input.getLookupValues().get(i);
      TableItem item = wReturn.table.getItem(i);
      item.setText(1, Const.NVL(lookupValue.getName(), ""));
      item.setText(2, Const.NVL(lookupValue.getRename(), ""));
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

  private void activeStore() {
    EmbeddingStore store = EmbeddingStore.lookupDescription(wStore.getText());

    wNeo4JConnection.setVisible(store == EmbeddingStore.NEO4J);
    wChromaUrl.setVisible(store == EmbeddingStore.CHROMA);
  }

  private void ok() {
    if (Utils.isEmpty(wTransformName.getText())) {
      return;
    }

    input.setNeo4JConnectionName(wNeo4JConnection.getText());
    input.setChromaUrl(wChromaUrl.getText());

    input.setMainStreamField(wMainStreamField.getText());
    input.setLookupTransformName(wTransform.getText());
    input.setLookupTextField(wLookupTextField.getText());
    input.setLookupKeyField(wLookupKeyField.getText());

    input.setLlModelName(wLlmModel.getText());
    input.setEmbeddingStore(EmbeddingStore.lookupDescription(wStore.getText()));
    input.setMaximalValue(wMaxValue.getText());

    input.setOutputMatchField(wMatchField.getText());
    input.setOutputKeyField(wKeyField.getText());
    input.setMaximalValue(wMaxValue.getText());
    input.setOutputDistanceField(wDistanceField.getText());

    input.getLookupValues().clear();
    for (TableItem item : wReturn.getNonEmptyItems()) {
      SemanticSearchMeta.SLookupValue lookupValue = new SemanticSearchMeta.SLookupValue();
      lookupValue.setName(item.getText(1));
      lookupValue.setRename(item.getText(2));
      input.getLookupValues().add(lookupValue);
    }

    transformName = wTransformName.getText(); // return value
    input.setChanged();
    dispose();
  }

  private void setMainStreamField() {
    if (!gotPreviousFields) {
      String field = wMainStreamField.getText();
      try {
        wMainStreamField.removeAll();

        IRowMeta r = pipelineMeta.getPrevTransformFields(variables, transformName);
        if (r != null) {
          wMainStreamField.setItems(r.getFieldNames());
        }
      } catch (HopException ke) {
        new ErrorDialog(shell,
            BaseMessages.getString(PKG, "SemanticSearchDialog.FailedToGetFields.DialogTitle"),
            BaseMessages.getString(PKG, "SemanticSearchDialog.FailedToGetFields.DialogMessage"),
            ke);
      }
      if (field != null) {
        wMainStreamField.setText(field);
      }
      gotPreviousFields = true;
    }
  }

  private void setLookupTextField() {
    if (!gotLookupTextFields) {
      String field = wLookupTextField.getText();
      try {
        wLookupTextField.removeAll();

        IRowMeta r = pipelineMeta.getTransformFields(variables, wTransform.getText());
        if (r != null) {
          String[] stringTypeFieldNames = r.getValueMetaList().stream()
              .filter(meta -> meta.getType() == IValueMeta.TYPE_STRING)
              .map(meta -> meta.getName()).collect(Collectors.toList()).toArray(new String[0]);
          wLookupTextField.setItems(stringTypeFieldNames);
        }
      } catch (HopException ke) {
        new ErrorDialog(shell,
            BaseMessages.getString(PKG, "SemanticSearchDialog.FailedToGetLookupFields.DialogTitle"),
            BaseMessages.getString(PKG,
                "SemanticSearchDialog.FailedToGetLookupFields.DialogMessage"),
            ke);
      }
      if (field != null) {
        wLookupTextField.setText(field);
      }
      gotLookupTextFields = true;
    }
  }

  private void setLookupKeyField() {
    if (!gotLookupKeyFields) {
      String field = wLookupKeyField.getText();
      try {
        wLookupKeyField.removeAll();

        IRowMeta r = pipelineMeta.getTransformFields(variables, wTransform.getText());
        if (r != null) {
          String[] fieldNames = r.getValueMetaList().stream()
              .map(meta -> meta.getName()).collect(Collectors.toList()).toArray(new String[0]);
          wLookupKeyField.setItems(fieldNames);
        }
      } catch (HopException ke) {
        new ErrorDialog(shell,
            BaseMessages.getString(PKG, "SemanticSearchDialog.FailedToGetLookupFields.DialogTitle"),
            BaseMessages.getString(PKG,
                "SemanticSearchDialog.FailedToGetLookupFields.DialogMessage"),
            ke);
      }
      if (field != null) {
        wLookupKeyField.setText(field);
      }
      gotLookupKeyFields = true;
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

          gotLookupKeyFields = false;
          gotLookupTextFields = false;
        } catch (HopException e) {
          logError("It was not possible to retrieve the list of fields for transform ["
              + wTransform.getText() + "]!");
        }
      }
    };
    shell.getDisplay().asyncExec(fieldLoader);
  }
}
