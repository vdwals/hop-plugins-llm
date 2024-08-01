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

package de.dvdw.hop.llm;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hop.core.Const;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.dialog.BaseDialog;
import org.apache.hop.ui.core.gui.GuiResource;
import org.apache.hop.ui.core.widget.TextVar;
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

public class LlmPluginDialogModule {
  private static final Class<?> PKG = LlmPluginDialogModule.class; // For Translator

  protected CCombo wModel;

  protected TextVar wOnnxFilename;
  protected Button wbbOnnxFilename;

  protected TextVar wTokenizerFilename;
  protected Button wbbTokenizerFilename;

  private TextVar wOpenAiKey;

  private Group wSettingsGroup;
  private CTabItem wModelTab;

  private LlmPluginMeta<?, ?> input;
  private Shell parent;
  private IVariables variables;

  public LlmPluginDialogModule(Shell parent, IVariables variables, LlmPluginMeta<?, ?> input) {
    this.input = input;
    this.parent = parent;
    this.variables = variables;
  }

  public void activeModel() {
    LlmModel model = LlmModel.lookupDescription(wModel.getText());

    wbbOnnxFilename.setVisible(model == LlmModel.ONNX_MODEL);
    wOnnxFilename.setVisible(model == LlmModel.ONNX_MODEL);
    wbbTokenizerFilename.setVisible(model == LlmModel.ONNX_MODEL);
    wTokenizerFilename.setVisible(model == LlmModel.ONNX_MODEL);

    wOpenAiKey.setVisible(model == LlmModel.OPEN_AI);
  }

  public TextVar generateTextVar(int middle, int margin, Control wPreviousControl, Group wGroup,
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

  public void renderModelTab(CTabFolder wTabFolder, int middle, int margin,
      Control wPreviousControl, ModifyListener lsMod) {

    // ////////////////////////
    // START OF Model TAB ///
    // ////////////////////////
    Pair<Composite, CTabItem> tabItems =
        generateTab(wTabFolder, BaseMessages.getString(PKG, "LlmPluginDialog.Model.Tab"));
    Composite wModelComp = tabItems.getLeft();
    wModelTab = tabItems.getRight();

    // /////////////////////////////////
    // START OF Settings Fields GROUP
    // /////////////////////////////////

    wSettingsGroup = generateGroup(wModelComp,
        BaseMessages.getString(PKG, "LlmPluginDialog.Group.SettingsGroup.Label"));

    // Model
    generateLabel(middle, margin, wPreviousControl, wSettingsGroup,
        BaseMessages.getString(PKG, "LlmPluginDialog.Model.Label"));

    wModel = new CCombo(wSettingsGroup, SWT.BORDER | SWT.READ_ONLY);
    PropsUi.setLook(getwModel());
    FormData fdModel = new FormData();
    fdModel.left = new FormAttachment(middle, 0);
    fdModel.top = new FormAttachment(wPreviousControl, margin);
    fdModel.right = new FormAttachment(100, -margin);
    getwModel().setLayoutData(fdModel);
    getwModel().setItems(LlmModel.getDescriptions());
    getwModel().addListener(SWT.Selection, e -> activeModel());

    // Onnx-File
    Pair<TextVar, Button> inputs = generateFileInput(lsMod, middle, margin, wSettingsGroup,
        getwModel(), BaseMessages.getString(PKG, "LlmPluginDialog.OnnxFilename.Label"),
        new String[] {"*.onnx", "*"},
        new String[] {BaseMessages.getString(PKG, "LlmPluginDialog.FileType.Onnx"),
            BaseMessages.getString(PKG, "System.FileType.AllFiles")});
    wOnnxFilename = inputs.getLeft();
    wbbOnnxFilename = inputs.getRight();

    // Tokenizer-File
    inputs = generateFileInput(lsMod, middle, margin, wSettingsGroup, getwOnnxFilename(),
        BaseMessages.getString(PKG, "LlmPluginDialog.TokenizerFilename.Label"),
        new String[] {"*.json", "*"},
        new String[] {BaseMessages.getString(PKG, "System.FileType.JsonFiles"),
            BaseMessages.getString(PKG, "System.FileType.AllFiles")});
    wbbTokenizerFilename = inputs.getRight();
    wTokenizerFilename = inputs.getLeft();


    this.wOpenAiKey = generateTextVar(middle, margin, wTokenizerFilename, wSettingsGroup,
        BaseMessages.getString(PKG, "LlmPluginDialog.openaikey.Label"), null);

    finalizeGroup(margin, wPreviousControl, wSettingsGroup);

    // ///////////////////////////////////////////////////////////
    // / END OF Settings GROUP
    // ///////////////////////////////////////////////////////////

    wModelComp.layout();
    wModelTab.setControl(wModelComp);

    // ///////////////////////////////////////////////////////////
    // / END OF Model TAB
    // //////////////////////////////////////////////////////////

  }

  private void finalizeGroup(int margin, Control cPreviousControl, Group wGroup) {
    FormData fdSettingsGroup = new FormData();
    fdSettingsGroup.left = new FormAttachment(0, margin);
    fdSettingsGroup.top = new FormAttachment(cPreviousControl, margin);
    fdSettingsGroup.right = new FormAttachment(100, -margin);
    wGroup.setLayoutData(fdSettingsGroup);
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
    fileButton.addListener(SWT.Selection, e -> BaseDialog.presentFileDialog(parent, fileName,
        variables, extensionsForFileSelector, extensionDescirptions, true));

    return new ImmutablePair<TextVar, Button>(fileName, fileButton);
  }

  /** Copy information from the meta-data input to the dialog fields. */
  public void getData() {
    getwModel().setText(
        Const.NVL(input.getLlmModel().getDescription(), LlmModel.ONNX_MODEL.getDescription()));
    activeModel();

    getwOnnxFilename().setText(Const.NVL(input.getOnnxFilename(), ""));
    getwTokenizerFilename().setText(Const.NVL(input.getTokenizerFilename(), ""));
  }

  public void ok() {
    input.setLlmModel(LlmModel.lookupDescription(getwModel().getText()));
    input.setOnnxFilename(getwOnnxFilename().getText());
    input.setTokenizerFilename(getwTokenizerFilename().getText());

    input.setChanged();
  }

  public Group getwSettingsGroup() {
    return wSettingsGroup;
  }

  public CTabItem getwModelTab() {
    return wModelTab;
  }

  public TextVar getwTokenizerFilename() {
    return wTokenizerFilename;
  }

  public CCombo getwModel() {
    return wModel;
  }

  public TextVar getwOnnxFilename() {
    return wOnnxFilename;
  }
}
