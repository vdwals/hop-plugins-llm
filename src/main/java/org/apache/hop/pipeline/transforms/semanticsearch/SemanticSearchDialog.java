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
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.ITransformDialog;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.pipeline.transform.stream.IStream;
import org.apache.hop.pipeline.transforms.semanticsearch.SemanticSearchMeta.SEmbeddingModel;
import org.apache.hop.pipeline.transforms.semanticsearch.SemanticSearchMeta.SEmbeddingStore;
import org.apache.hop.ui.core.ConstUi;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.dialog.BaseDialog;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.dialog.MessageBox;
import org.apache.hop.ui.core.gui.GuiResource;
import org.apache.hop.ui.core.widget.ColumnInfo;
import org.apache.hop.ui.core.widget.ComboVar;
import org.apache.hop.ui.core.widget.TableView;
import org.apache.hop.ui.core.widget.TextVar;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class SemanticSearchDialog extends BaseTransformDialog implements ITransformDialog {
  private static final Class<?> PKG = SemanticSearchMeta.class; // For Translator

  private CCombo wTransform;

  private CCombo wModel;
  
  private CCombo wStore;

  private ComboVar wMainStreamField;

  private ComboVar wLookupField;

  private ColumnInfo[] ciReturn;
  private Label wlReturn;
  private TableView wReturn;

  private TextVar wMatchField;

  private TextVar wOnnxFilename;
  private Button wbbOnnxFilename;

  private TextVar wTokenizerFilename;
  private Button wbbTokenizerFilename;

  private Button wGetLookup;

  private final SemanticSearchMeta input;
  private boolean gotPreviousFields = false;
  private boolean gotLookupFields = false;

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

    ModifyListener lsMod = e -> input.setChanged();
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
    Pair<Composite, CTabItem> tabItems =
        generateTab(wTabFolder, BaseMessages.getString(PKG, "SemanticSearchDialog.General.Tab"));
    Composite wGeneralComp = tabItems.getLeft();
    CTabItem wGeneralTab = tabItems.getRight();

    // /////////////////////////////////
    // START OF Lookup Fields GROUP
    // /////////////////////////////////

    Group wLookupGroup = new Group(wGeneralComp, SWT.SHADOW_NONE);
    PropsUi.setLook(wLookupGroup);
    wLookupGroup.setText(BaseMessages.getString(PKG, "SemanticSearchDialog.Group.Lookup.Label"));

    FormLayout lookupGroupLayout = new FormLayout();
    lookupGroupLayout.marginWidth = 10;
    lookupGroupLayout.marginHeight = 10;
    wLookupGroup.setLayout(lookupGroupLayout);

    // Source transform line...
    Label wlTransform = new Label(wLookupGroup, SWT.RIGHT);
    wlTransform.setText(BaseMessages.getString(PKG, "SemanticSearchDialog.SourceTransform.Label"));
    PropsUi.setLook(wlTransform);
    FormData fdlTransform = new FormData();
    fdlTransform.left = new FormAttachment(0, 0);
    fdlTransform.right = new FormAttachment(middle, -margin);
    fdlTransform.top = new FormAttachment(wTransformName, margin);
    wlTransform.setLayoutData(fdlTransform);
    wTransform = new CCombo(wLookupGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wTransform);

    List<TransformMeta> transforms =
        pipelineMeta.findPreviousTransforms(pipelineMeta.findTransform(transformName), true);
    for (TransformMeta transformMeta : transforms) {
      wTransform.add(transformMeta.getName());
    }

    wTransform.addListener(SWT.Selection, e -> setComboBoxesLookup());

    FormData fdTransform = new FormData();
    fdTransform.left = new FormAttachment(middle, 0);
    fdTransform.top = new FormAttachment(wTransformName, margin);
    fdTransform.right = new FormAttachment(100, 0);
    wTransform.setLayoutData(fdTransform);

    // LookupField
    Label wlLookupField = new Label(wLookupGroup, SWT.RIGHT);
    wlLookupField.setText(BaseMessages.getString(PKG, "SemanticSearchDialog.wlLookupField.Label"));
    PropsUi.setLook(wlLookupField);
    FormData fdlLookupField = new FormData();
    fdlLookupField.left = new FormAttachment(0, 0);
    fdlLookupField.top = new FormAttachment(wTransform, margin);
    fdlLookupField.right = new FormAttachment(middle, -2 * margin);
    wlLookupField.setLayoutData(fdlLookupField);

    wLookupField = new ComboVar(variables, wLookupGroup, SWT.BORDER | SWT.READ_ONLY);
    wLookupField.setEditable(true);
    PropsUi.setLook(wLookupField);
    FormData fdLookupField = new FormData();
    fdLookupField.left = new FormAttachment(middle, 0);
    fdLookupField.top = new FormAttachment(wTransform, margin);
    fdLookupField.right = new FormAttachment(100, -margin);
    wLookupField.setLayoutData(fdLookupField);
    wLookupField.addListener(SWT.FocusIn, e -> setLookupField());

    FormData fdLookupGroup = new FormData();
    fdLookupGroup.left = new FormAttachment(0, margin);
    fdLookupGroup.top = new FormAttachment(wTransformName, margin);
    fdLookupGroup.right = new FormAttachment(100, -margin);
    wLookupGroup.setLayoutData(fdLookupGroup);

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
    Label wlMainStreamField = new Label(wMainStreamGroup, SWT.RIGHT);
    wlMainStreamField
        .setText(BaseMessages.getString(PKG, "SemanticSearchDialog.wlMainStreamField.Label"));
    PropsUi.setLook(wlMainStreamField);
    FormData fdlMainStreamField = new FormData();
    fdlMainStreamField.left = new FormAttachment(0, 0);
    fdlMainStreamField.top = new FormAttachment(wLookupGroup, margin);
    fdlMainStreamField.right = new FormAttachment(middle, -2 * margin);
    wlMainStreamField.setLayoutData(fdlMainStreamField);

    wMainStreamField = new ComboVar(variables, wMainStreamGroup, SWT.BORDER | SWT.READ_ONLY);
    wMainStreamField.setEditable(true);
    PropsUi.setLook(wMainStreamField);
    FormData fdMainStreamField = new FormData();
    fdMainStreamField.left = new FormAttachment(middle, 0);
    fdMainStreamField.top = new FormAttachment(wLookupGroup, margin);
    fdMainStreamField.right = new FormAttachment(100, -margin);
    wMainStreamField.setLayoutData(fdMainStreamField);
    wMainStreamField.addListener(SWT.FocusIn, e -> setMainStreamField());

    FormData fdMainStreamGroup = new FormData();
    fdMainStreamGroup.left = new FormAttachment(0, margin);
    fdMainStreamGroup.top = new FormAttachment(wLookupGroup, margin);
    fdMainStreamGroup.right = new FormAttachment(100, -margin);
    wMainStreamGroup.setLayoutData(fdMainStreamGroup);

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
    setButtonPositions(new Button[] {wOk, wCancel}, margin, null);

    // ////////////////////////
    // START OF Model TAB ///
    // ////////////////////////
    tabItems =
        generateTab(wTabFolder, BaseMessages.getString(PKG, "SemanticSearchDialog.Model.Tab"));
    Composite wModelComp = tabItems.getLeft();
    CTabItem wModelTab = tabItems.getRight();

    // /////////////////////////////////
    // START OF Settings Fields GROUP
    // /////////////////////////////////

    Group wSettingsGroup = generateGroup(wModelComp,
        BaseMessages.getString(PKG, "SemanticSearchDialog.Group.SettingsGroup.Label"));

    // Model
    generateLabel(middle, margin, wMainStreamGroup, wSettingsGroup,
        BaseMessages.getString(PKG, "SemanticSearchDialog.Model.Label"));

    wModel = new CCombo(wSettingsGroup, SWT.BORDER | SWT.READ_ONLY);
    PropsUi.setLook(wModel);
    FormData fdModel = new FormData();
    fdModel.left = new FormAttachment(middle, 0);
    fdModel.top = new FormAttachment(wMainStreamGroup, margin);
    fdModel.right = new FormAttachment(100, -margin);
    wModel.setLayoutData(fdModel);
    wModel.setItems(SemanticSearchMeta.SEmbeddingModel.getDescriptions());
    wModel.addListener(SWT.Selection, e -> activeModel());

    // Onnx-File
    Pair<TextVar, Button> inputs = generateFileInput(lsMod, middle, margin, wSettingsGroup, wModel,
        BaseMessages.getString(PKG, "SemanticSearchDialog.OnnxFilename.Label"),
        new String[] {"*.onnx", "*"},
        new String[] {BaseMessages.getString(PKG, "SemanticSearchDialog.FileType.Onnx"),
            BaseMessages.getString(PKG, "System.FileType.AllFiles")});
    wOnnxFilename = inputs.getLeft();
    wbbOnnxFilename = inputs.getRight();

    // Tokenizer-File
    inputs = generateFileInput(lsMod, middle, margin, wSettingsGroup, wOnnxFilename,
        BaseMessages.getString(PKG, "SemanticSearchDialog.TokenizerFilename.Label"),
        new String[] {"*.json", "*"},
        new String[] {BaseMessages.getString(PKG, "System.FileType.JsonFiles"),
            BaseMessages.getString(PKG, "System.FileType.AllFiles")});
    wbbTokenizerFilename = inputs.getRight();
    wTokenizerFilename = inputs.getLeft();

    FormData fdSettingsGroup = new FormData();
    fdSettingsGroup.left = new FormAttachment(0, margin);
    fdSettingsGroup.top = new FormAttachment(wMainStreamGroup, margin);
    fdSettingsGroup.right = new FormAttachment(100, -margin);
    wSettingsGroup.setLayoutData(fdSettingsGroup);

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
    tabItems =
        generateTab(wTabFolder, BaseMessages.getString(PKG, "SemanticSearchDialog.Store.Tab"));
    Composite wStoreComp = tabItems.getLeft();
    CTabItem wStoreTab = tabItems.getRight();

    // /////////////////////////////////
    // START OF Settings Fields GROUP
    // /////////////////////////////////

    Group wStoreGroup = generateGroup(wStoreComp,
        BaseMessages.getString(PKG, "SemanticSearchDialog.Group.SettingsGroup.Label"));

    // Model
    generateLabel(middle, margin, wSettingsGroup, wStoreGroup,
        BaseMessages.getString(PKG, "SemanticSearchDialog.Model.Label"));

    wStore = new CCombo(wStoreGroup, SWT.BORDER | SWT.READ_ONLY);
    PropsUi.setLook(wStore);
    FormData fdStore = new FormData();
    fdStore.left = new FormAttachment(middle, 0);
    fdStore.top = new FormAttachment(wSettingsGroup, margin);
    fdStore.right = new FormAttachment(100, -margin);
    wStore.setLayoutData(fdStore);
    wStore.setItems(SemanticSearchMeta.SEmbeddingStore.getDescriptions());
    wStore.addListener(SWT.Selection, e -> activeModel());

    // Onnx-File
    inputs = generateFileInput(lsMod, middle, margin, wStoreGroup, wStore,
        BaseMessages.getString(PKG, "SemanticSearchDialog.OnnxFilename.Label"),
        new String[] {"*.onnx", "*"},
        new String[] {BaseMessages.getString(PKG, "SemanticSearchDialog.FileType.Onnx"),
            BaseMessages.getString(PKG, "System.FileType.AllFiles")});
    wOnnxFilename = inputs.getLeft();
    wbbOnnxFilename = inputs.getRight();

    FormData fdStoreGroup = new FormData();
    fdStoreGroup.left = new FormAttachment(0, margin);
    fdStoreGroup.top = new FormAttachment(wSettingsGroup, margin);
    fdStoreGroup.right = new FormAttachment(100, -margin);
    wStoreGroup.setLayoutData(fdStoreGroup);

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
    tabItems =
        generateTab(wTabFolder, BaseMessages.getString(PKG, "SemanticSearchDialog.Fields.Tab"));
    Composite wFieldsComp = tabItems.getLeft();
    CTabItem wFieldsTab = tabItems.getRight();

    // /////////////////////////////////
    // START OF OutputFields Fields GROUP
    // /////////////////////////////////

    Group wOutputFieldsGroup = new Group(wFieldsComp, SWT.SHADOW_NONE);
    PropsUi.setLook(wOutputFieldsGroup);
    wOutputFieldsGroup
        .setText(BaseMessages.getString(PKG, "SemanticSearchDialog.Group.OutputFieldsGroup.Label"));

    FormLayout outputFieldsGroupLayout = new FormLayout();
    outputFieldsGroupLayout.marginWidth = 10;
    outputFieldsGroupLayout.marginHeight = 10;
    wOutputFieldsGroup.setLayout(outputFieldsGroupLayout);

    Label wlMatchField = new Label(wOutputFieldsGroup, SWT.RIGHT);
    wlMatchField.setText(BaseMessages.getString(PKG, "SemanticSearchDialog.MatchField.Label"));
    PropsUi.setLook(wlMatchField);
    FormData fdlMatchField = new FormData();
    fdlMatchField.left = new FormAttachment(0, 0);
    fdlMatchField.top = new FormAttachment(wSettingsGroup, margin);
    fdlMatchField.right = new FormAttachment(middle, -margin);
    wlMatchField.setLayoutData(fdlMatchField);
    wMatchField = new TextVar(variables, wOutputFieldsGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wMatchField);
    FormData fdMatchField = new FormData();
    fdMatchField.left = new FormAttachment(middle, 0);
    fdMatchField.top = new FormAttachment(wSettingsGroup, margin);
    fdMatchField.right = new FormAttachment(100, 0);
    wMatchField.setLayoutData(fdMatchField);

    FormData fdOutputFieldsGroup = new FormData();
    fdOutputFieldsGroup.left = new FormAttachment(0, margin);
    fdOutputFieldsGroup.top = new FormAttachment(wSettingsGroup, margin);
    fdOutputFieldsGroup.right = new FormAttachment(100, -margin);
    wOutputFieldsGroup.setLayoutData(fdOutputFieldsGroup);

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
    ciReturn[0] =
        new ColumnInfo(BaseMessages.getString(PKG, "SemanticSearchDialog.ColumnInfo.FieldReturn"),
            ColumnInfo.COLUMN_TYPE_CCOMBO, new String[] {""}, false);
    ciReturn[1] =
        new ColumnInfo(BaseMessages.getString(PKG, "SemanticSearchDialog.ColumnInfo.NewName"),
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

  private void generateLabel(int middle, int margin, Group wPreviousGroup, Group wCurrentGroup,
      String label) {
    Label wlModel = new Label(wCurrentGroup, SWT.RIGHT);
    wlModel.setText(label);
    PropsUi.setLook(wlModel);
    FormData fdlModel = new FormData();
    fdlModel.left = new FormAttachment(0, 0);
    fdlModel.right = new FormAttachment(middle, -margin);
    fdlModel.top = new FormAttachment(wPreviousGroup, margin);
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

  private Pair<TextVar, Button> generateFileInput(ModifyListener lsMod, int middle, int margin,
      Group formGroup, Control previousControl, String label, String[] extensionsForFileSelector,
      String[] extensionDescirptions) {
    //
    // The filename browse button
    //
    Button fileButton = new Button(formGroup, SWT.PUSH | SWT.CENTER);
    PropsUi.setLook(fileButton);
    fileButton.setText(BaseMessages.getString(PKG, "System.Button.Browse"));
    fileButton
        .setToolTipText(BaseMessages.getString(PKG, "System.Tooltip.BrowseForFileOrDirAndAdd"));
    FormData fdbFilename = new FormData();
    fdbFilename.top = new FormAttachment(previousControl, margin);
    fdbFilename.right = new FormAttachment(100, 0);
    fileButton.setLayoutData(fdbFilename);

    // The field itself...
    //
    Label wlFilename = new Label(formGroup, SWT.RIGHT);
    wlFilename.setText(label);
    PropsUi.setLook(wlFilename);
    FormData fdlFilename = new FormData();
    fdlFilename.top = new FormAttachment(previousControl, margin);
    fdlFilename.left = new FormAttachment(0, 0);
    fdlFilename.right = new FormAttachment(middle, -margin);
    wlFilename.setLayoutData(fdlFilename);
    TextVar fileName = new TextVar(variables, formGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(fileName);
    fileName.addModifyListener(lsMod);
    FormData fdFilename = new FormData();
    fdFilename.top = new FormAttachment(previousControl, margin);
    fdFilename.left = new FormAttachment(middle, 0);
    fdFilename.right = new FormAttachment(fileButton, -margin);
    fileName.setLayoutData(fdFilename);

    // Add Eventlistener
    fileButton.addListener(SWT.Selection, e -> BaseDialog.presentFileDialog(shell, fileName,
        variables, extensionsForFileSelector, extensionDescirptions, true));

    return new ImmutablePair<TextVar, Button>(fileName, fileButton);
  }

  /** Copy information from the meta-data input to the dialog fields. */
  public void getData() {
    if (isDebug()) {
      logDebug(BaseMessages.getString(PKG, "SemanticSearchDialog.Log.GettingKeyInfo"));
    }

    wMainStreamField.setText(Const.NVL(input.getMainStreamField(), ""));
    wLookupField.setText(Const.NVL(input.getLookupField(), ""));
    wMatchField.setText(Const.NVL(input.getOutputMatchField(), ""));
    wModel.setText(Const.NVL(input.getEmbeddingModel().getDescription(),
        SEmbeddingModel.ONNX_MODEL.getDescription()));
    wStore.setText(Const.NVL(input.getEmbeddingStore().getDescription(),
        SEmbeddingStore.IN_MEMORY.getDescription()));
    wOnnxFilename.setText(Const.NVL(input.getOnnxFilename(), ""));
    wTokenizerFilename.setText(Const.NVL(input.getTokenizerFilename(), ""));

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

  private void activeModel() {
    SEmbeddingModel model = SemanticSearchMeta.SEmbeddingModel.lookupDescription(wModel.getText());

    boolean enable = (model == SEmbeddingModel.ONNX_MODEL);

    wbbOnnxFilename.setEnabled(enable);
    wOnnxFilename.setEnabled(enable);
    wbbTokenizerFilename.setEnabled(enable);
    wTokenizerFilename.setEnabled(enable);
  }

  private void ok() {
    if (Utils.isEmpty(wTransformName.getText())) {
      return;
    }

    input.setMainStreamField(wMainStreamField.getText());
    input.setLookupTransformName(wTransform.getText());
    input.setLookupField(wLookupField.getText());

    input.setEmbeddingModel(SEmbeddingModel.lookupDescription(wModel.getText()));
    input.setEmbeddingStore(SEmbeddingStore.lookupDescription(wStore.getText()));
    input.setOnnxFilename(wOnnxFilename.getText());
    input.setTokenizerFilename(wTokenizerFilename.getText());

    input.setOutputMatchField(wMatchField.getText());

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

  private void setLookupField() {
    if (!gotLookupFields) {
      String field = wLookupField.getText();
      try {
        wLookupField.removeAll();

        IRowMeta r = pipelineMeta.getTransformFields(variables, wTransform.getText());
        if (r != null) {
          String[] stringTypeFieldNames =
              r.getValueMetaList().stream().filter(meta -> meta.getType() == IValueMeta.TYPE_STRING)
                  .map(meta -> meta.getName()).toList().toArray(new String[0]);
          wLookupField.setItems(stringTypeFieldNames);
        }
      } catch (HopException ke) {
        new ErrorDialog(shell,
            BaseMessages.getString(PKG, "SemanticSearchDialog.FailedToGetLookupFields.DialogTitle"),
            BaseMessages.getString(PKG,
                "SemanticSearchDialog.FailedToGetLookupFields.DialogMessage"),
            ke);
      }
      if (field != null) {
        wLookupField.setText(field);
      }
      gotLookupFields = true;
    }
  }

  private void getlookup() {
    try {
      String transformFrom = wTransform.getText();
      if (!Utils.isEmpty(transformFrom)) {
        IRowMeta r = pipelineMeta.getTransformFields(variables, transformFrom);
        if (r != null && !r.isEmpty()) {
          BaseTransformDialog.getFieldsFromPrevious(r, wReturn, 1, new int[] {1}, new int[] {4}, -1,
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
        } catch (HopException e) {
          logError("It was not possible to retrieve the list of fields for transform ["
              + wTransform.getText() + "]!");
        }
      }
    };
    shell.getDisplay().asyncExec(fieldLoader);
  }
}
