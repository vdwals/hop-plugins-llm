package org.apache.hop.langchain4j.languagemodels;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.langchain4j.languagemodels.ollama.OllamaModel;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.gui.GuiCompositeWidgets;
import org.apache.hop.ui.core.gui.GuiCompositeWidgetsAdapter;
import org.apache.hop.ui.core.metadata.MetadataEditor;
import org.apache.hop.ui.core.metadata.MetadataManager;
import org.apache.hop.ui.hopgui.HopGui;
import org.apache.hop.ui.hopgui.perspective.metadata.MetadataPerspective;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

public class LanguageModelMetaEditor extends MetadataEditor<LanguageModelMeta> {
    private static final Class<?> PKG = LanguageModelMetaEditor.class;

    private int middle;
    private int margin;

    private Text wName;
    private Combo wType;

    private GuiCompositeWidgets guiCompositeWidgets;
    private Composite wSpecificComp;

    private Map<Class<? extends ILanguageModel>, ILanguageModel> metaMap;

    private AtomicBoolean busyChangingType = new AtomicBoolean(false);

    private Listener modifyListener = event -> {
        setChanged();
        MetadataPerspective.getInstance().updateEditor(this);
    };

    public LanguageModelMetaEditor(HopGui hopGui, MetadataManager<LanguageModelMeta> manager,
            LanguageModelMeta metadata) {
        super(hopGui, manager, metadata);

        middle = PropsUi.getInstance().getMiddlePct();
        margin = PropsUi.getMargin();

        metaMap = populateStorageMetaMap();
        ILanguageModel defaultModel = metadata.getModel();
        metaMap.put(defaultModel.getClass(), defaultModel);
    }

    private Map<Class<? extends ILanguageModel>, ILanguageModel> populateStorageMetaMap() {
        metaMap = new HashMap<>();

        metaMap.put(OllamaModel.class, new OllamaModel());

        return metaMap;
    }

    @Override
    public void createControl(Composite parent) {
        Control previous = buildHeaderUI(parent);
        previous = buildModelSelection(parent, previous);
        setWidgetsContent();
    }

    @Override
    public void setWidgetsContent() {
        LanguageModelMeta meta = this.getMetadata();

        wName.setText(Const.NVL(meta.getName(), ""));

        String storageName = meta.getModel().getName();

        busyChangingType.set(true);
        wType.setText(Const.NVL(storageName, OllamaModel.NAME));

        guiCompositeWidgets.setWidgetsContents(
                meta.getModel(),
                wSpecificComp,
                LanguageModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID);
        busyChangingType.set(false);
    }

    @Override
    public void getWidgetsContent(LanguageModelMeta meta) {
        meta.setName(wName.getText());

        ILanguageModel storage = meta.getModel();
        guiCompositeWidgets.getWidgetsContents(storage, LanguageModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID);
        metaMap.put(storage.getClass(), storage);
    }

    private Control buildHeaderUI(Composite parent) {
        // What's the name
        Label wlName = new Label(parent, SWT.RIGHT);
        PropsUi.setLook(wlName);
        wlName.setText(BaseMessages.getString(PKG, "StorageDialog.label.StorageName"));
        FormData fdlName = new FormData();
        fdlName.top = new FormAttachment(0, 0);
        fdlName.left = new FormAttachment(0, 0);
        wlName.setLayoutData(fdlName);

        wName = new Text(parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        PropsUi.setLook(wName);
        FormData fdName = new FormData();
        fdName.top = new FormAttachment(wlName, margin);
        fdName.left = new FormAttachment(0, 0);
        fdName.right = new FormAttachment(100, 0);
        wName.setLayoutData(fdName);

        wName.addListener(SWT.Modify, modifyListener);

        Label spacer = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
        FormData fdSpacer = new FormData();
        fdSpacer.left = new FormAttachment(0, 0);
        fdSpacer.top = new FormAttachment(wName, 15);
        fdSpacer.right = new FormAttachment(100, 0);
        spacer.setLayoutData(fdSpacer);

        return spacer;
    }

    private Control buildModelSelection(Composite parent, Control previous) {
        // What's the type of storage?
        //
        Label wlModelType = new Label(parent, SWT.RIGHT);
        PropsUi.setLook(wlModelType);
        wlModelType.setText(BaseMessages.getString(PKG, "StorageDialog.label.StorageType"));
        FormData fdlConnectionType = new FormData();
        fdlConnectionType.top = new FormAttachment(previous, margin);
        fdlConnectionType.left = new FormAttachment(0, 0); // First one in the left top corner
        fdlConnectionType.right = new FormAttachment(middle, -margin);
        wlModelType.setLayoutData(fdlConnectionType);

        wType = new Combo(parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wType.setItems(
                metaMap.values().stream().map(ILanguageModel::getName).collect(Collectors.toList())
                        .toArray(new String[0]));
        PropsUi.setLook(wType);
        FormData fdConnectionType = new FormData();
        fdConnectionType.top = new FormAttachment(previous, 0);
        fdConnectionType.left = new FormAttachment(wlModelType, margin); // To the right of the label
        wType.setLayoutData(fdConnectionType);
        Control lastControl = wType;

        wType.addListener(SWT.Modify, modifyListener);
        wType.addListener(SWT.Modify, event -> changeStorageType(parent));

        wSpecificComp = new Composite(parent, SWT.BACKGROUND);
        wSpecificComp.setLayout(new FormLayout());
        FormData fdDatabaseSpecificComp = new FormData();
        fdDatabaseSpecificComp.top = new FormAttachment(lastControl, margin);
        fdDatabaseSpecificComp.left = new FormAttachment(0, 0);
        fdDatabaseSpecificComp.right = new FormAttachment(100, 0);
        wSpecificComp.setLayoutData(fdDatabaseSpecificComp);
        PropsUi.setLook(wSpecificComp);
        lastControl = wSpecificComp;

        // Now add the database plugin specific widgets
        //
        guiCompositeWidgets = new GuiCompositeWidgets(manager.getVariables());
        guiCompositeWidgets.createCompositeWidgets(
                metadata.getModel(),
                null,
                wSpecificComp,
                LanguageModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID,
                null);

        // Add listener to detect change
        guiCompositeWidgets.setWidgetsListener(
                new GuiCompositeWidgetsAdapter() {
                    @Override
                    public void widgetModified(
                            GuiCompositeWidgets compositeWidgets, Control changedWidget, String widgetId) {
                        setChanged();
                    }
                });

        return lastControl;
    }

    private void changeStorageType(Composite parent) {
        if (busyChangingType.get()) {
            return;
        }
        busyChangingType.set(true);

        LanguageModelMeta meta = this.getMetadata();

        // Keep track of the old database type since this changes when getting the
        // content
        //
        Class<? extends ILanguageModel> oldClass = meta.getModel().getClass();

        // Save the state of this type, so we can switch back and forth
        //
        metaMap.put(oldClass, meta.getModel());

        String newTypeName = wType.getText();

        // Capture any information on the widgets
        //
        this.getWidgetsContent(meta);
        ILanguageModel oldStorage = meta.getModel();
        metaMap.put(oldStorage.getClass(), oldStorage);

        // Now change the data type
        //
        try {
            meta.setModelByType(newTypeName);
        } catch (HopException e) {
            e.printStackTrace();
        }
        ILanguageModel newStorage = meta.getModel();

        // Get possible information from the metadata map (from previous work)
        //
        if (metaMap.get(newStorage.getClass()) != null)
            meta.setModel(metaMap.get(newStorage.getClass()));
        else
            metaMap.put(newStorage.getClass(), newStorage);

        // Remove existing children
        //
        for (Control child : wSpecificComp.getChildren()) {
            child.dispose();
        }

        // Re-add the widgets
        //
        guiCompositeWidgets = new GuiCompositeWidgets(manager.getVariables());
        guiCompositeWidgets.createCompositeWidgets(
                meta.getModel(),
                null,
                wSpecificComp,
                LanguageModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID,
                null);
        guiCompositeWidgets.setWidgetsListener(
                new GuiCompositeWidgetsAdapter() {
                    @Override
                    public void widgetModified(
                            GuiCompositeWidgets compositeWidgets, Control changedWidget, String widgetId) {
                        setChanged();
                    }
                });

        // Put the data back
        //
        setWidgetsContent();

        parent.layout(true, true);

        busyChangingType.set(false);
    }
}