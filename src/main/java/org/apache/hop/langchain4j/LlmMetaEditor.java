package org.apache.hop.langchain4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.langchain4j.models.IModel;
import org.apache.hop.langchain4j.models.onnx.OnnxModelMeta;
import org.apache.hop.langchain4j.storages.IStorage;
import org.apache.hop.langchain4j.storages.inmemory.InMemoryStorageMeta;
import org.apache.hop.langchain4j.storages.neo4j.Neo4jStorageMeta;
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

public class LlmMetaEditor extends MetadataEditor<LlmMeta> {
    private static final Class<?> PKG = LlmMetaEditor.class;

    private int middle;
    private int margin;

    private Text wName;
    private Combo wStorageType;
    private Combo wModelType;

    private Composite wStorageSpecificComp;
    private GuiCompositeWidgets guiCompositeWidgets;
    private Composite wModelSpecificComp;

    private Map<Class<? extends IStorage>, IStorage> storageMetaMap;
    private Map<Class<? extends IModel>, IModel> metaMap;

    private AtomicBoolean busyChangingStorageType = new AtomicBoolean(false);
    private AtomicBoolean busyChangingModelType = new AtomicBoolean(false);

    private Listener modifyListener = event -> {
        setChanged();
        MetadataPerspective.getInstance().updateEditor(this);
    };

    public LlmMetaEditor(HopGui hopGui, MetadataManager<LlmMeta> manager, LlmMeta metadata) {
        super(hopGui, manager, metadata);

        middle = PropsUi.getInstance().getMiddlePct();
        margin = PropsUi.getMargin();

        storageMetaMap = populateStorageMetaMap();
        IStorage defaultStorage = metadata.getStorage();
        storageMetaMap.put(defaultStorage.getClass(), defaultStorage);

        metaMap = populateModelMetaMap();
        IModel defaultModel = metadata.getModel();
        metaMap.put(defaultModel.getClass(), defaultModel);
    }

    private Map<Class<? extends IStorage>, IStorage> populateStorageMetaMap() {
        storageMetaMap = new HashMap<>();

        storageMetaMap.put(InMemoryStorageMeta.class, new InMemoryStorageMeta());
        storageMetaMap.put(Neo4jStorageMeta.class, new Neo4jStorageMeta());

        return storageMetaMap;
    }

    private Map<Class<? extends IModel>, IModel> populateModelMetaMap() {
        metaMap = new HashMap<>();

        metaMap.put(OnnxModelMeta.class, new OnnxModelMeta());

        return metaMap;
    }

    @Override
    public void createControl(Composite parent) {
        Control previous = buildHeaderUI(parent);
        previous = buildStorageSelection(parent, previous);
        buildModelSelection(parent, previous);
        setWidgetsContent();
    }

    @Override
    public void setWidgetsContent() {
        LlmMeta meta = this.getMetadata();

        wName.setText(Const.NVL(meta.getName(), ""));

        String storageName = meta.getStorage().getName();

        busyChangingStorageType.set(true);
        wStorageType.setText(Const.NVL(storageName, InMemoryStorageMeta.NAME));

        guiCompositeWidgets.setWidgetsContents(
                meta.getStorage(),
                wStorageSpecificComp,
                LlmMeta.GUI_PLUGIN_ELEMENT_PARENT_ID);
        busyChangingStorageType.set(false);

        String modelName = meta.getModel().getName();
        busyChangingModelType.set(true);
        wModelType.setText(Const.NVL(modelName, OnnxModelMeta.NAME));

        guiCompositeWidgets.setWidgetsContents(
                meta.getModel(),
                wModelSpecificComp,
                LlmMeta.GUI_PLUGIN_ELEMENT_PARENT_ID);
        busyChangingModelType.set(false);
    }

    @Override
    public void getWidgetsContent(LlmMeta meta) {
        meta.setName(wName.getText());

        IStorage storage = meta.getStorage();
        guiCompositeWidgets.getWidgetsContents(storage, LlmMeta.GUI_PLUGIN_ELEMENT_PARENT_ID);
        storageMetaMap.put(storage.getClass(), storage);

        IModel model = meta.getModel();
        guiCompositeWidgets.getWidgetsContents(model, LlmMeta.GUI_PLUGIN_ELEMENT_PARENT_ID);
        metaMap.put(model.getClass(), model);
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

    private Control buildStorageSelection(Composite parent, Control previous) {
        // What's the type of storage?
        //
        Label wlStorageType = new Label(parent, SWT.RIGHT);
        PropsUi.setLook(wlStorageType);
        wlStorageType.setText(BaseMessages.getString(PKG, "StorageDialog.label.StorageType"));
        FormData fdlConnectionType = new FormData();
        fdlConnectionType.top = new FormAttachment(previous, margin);
        fdlConnectionType.left = new FormAttachment(0, 0); // First one in the left top corner
        fdlConnectionType.right = new FormAttachment(middle, -margin);
        wlStorageType.setLayoutData(fdlConnectionType);

        wStorageType = new Combo(parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wStorageType.setItems(
                storageMetaMap.values().stream().map(IStorage::getName).collect(Collectors.toList())
                        .toArray(new String[0]));
        PropsUi.setLook(wStorageType);
        FormData fdConnectionType = new FormData();
        fdConnectionType.top = new FormAttachment(previous, 0);
        fdConnectionType.left = new FormAttachment(wlStorageType, margin); // To the right of the label
        wStorageType.setLayoutData(fdConnectionType);
        Control lastControl = wStorageType;

        wStorageType.addListener(SWT.Modify, modifyListener);
        wStorageType.addListener(SWT.Modify, event -> changeStorageType(parent));

        wStorageSpecificComp = new Composite(parent, SWT.BACKGROUND);
        wStorageSpecificComp.setLayout(new FormLayout());
        FormData fdDatabaseSpecificComp = new FormData();
        fdDatabaseSpecificComp.top = new FormAttachment(lastControl, margin);
        fdDatabaseSpecificComp.left = new FormAttachment(0, 0);
        fdDatabaseSpecificComp.right = new FormAttachment(100, 0);
        wStorageSpecificComp.setLayoutData(fdDatabaseSpecificComp);
        PropsUi.setLook(wStorageSpecificComp);
        lastControl = wStorageSpecificComp;

        // Now add the database plugin specific widgets
        //
        guiCompositeWidgets = new GuiCompositeWidgets(manager.getVariables());
        guiCompositeWidgets.createCompositeWidgets(
                metadata.getStorage(),
                null,
                wStorageSpecificComp,
                LlmMeta.GUI_PLUGIN_ELEMENT_PARENT_ID,
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

    private Control buildModelSelection(Composite parent, Control previous) {
        // What's the type of model?
        //
        Label wlModelType = new Label(parent, SWT.RIGHT);
        PropsUi.setLook(wlModelType);
        wlModelType.setText(BaseMessages.getString(PKG, "ModelDialog.label.ModelType"));
        FormData fdlConnectionType = new FormData();
        fdlConnectionType.top = new FormAttachment(previous, margin);
        fdlConnectionType.left = new FormAttachment(0, 0); // First one in the left top corner
        fdlConnectionType.right = new FormAttachment(middle, -margin);
        wlModelType.setLayoutData(fdlConnectionType);

        wModelType = new Combo(parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wModelType.setItems(new String[] { OnnxModelMeta.NAME });
        PropsUi.setLook(wModelType);
        FormData fdConnectionType = new FormData();
        fdConnectionType.top = new FormAttachment(previous, 0);
        fdConnectionType.left = new FormAttachment(wlModelType, margin); // To the right of the label
        wModelType.setLayoutData(fdConnectionType);
        Control lastControl = wModelType;

        wModelType.addListener(SWT.Modify, modifyListener);
        wModelType.addListener(SWT.Modify, event -> changeModelType(parent));

        wModelSpecificComp = new Composite(parent, SWT.BACKGROUND);
        wModelSpecificComp.setLayout(new FormLayout());
        FormData fdDatabaseSpecificComp = new FormData();
        fdDatabaseSpecificComp.top = new FormAttachment(lastControl, margin);
        fdDatabaseSpecificComp.left = new FormAttachment(0, 0);
        fdDatabaseSpecificComp.right = new FormAttachment(100, 0);
        wModelSpecificComp.setLayoutData(fdDatabaseSpecificComp);
        PropsUi.setLook(wModelSpecificComp);
        lastControl = wModelSpecificComp;

        // Now add the database plugin specific widgets
        //
        guiCompositeWidgets = new GuiCompositeWidgets(manager.getVariables());
        guiCompositeWidgets.createCompositeWidgets(
                new OnnxModelMeta(),
                null,
                wModelSpecificComp,
                LlmMeta.GUI_PLUGIN_ELEMENT_PARENT_ID,
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
        if (busyChangingStorageType.get()) {
            return;
        }
        busyChangingStorageType.set(true);

        LlmMeta llmMeta = this.getMetadata();

        // Keep track of the old database type since this changes when getting the
        // content
        //
        Class<? extends IStorage> oldClass = llmMeta.getStorage().getClass();

        // Save the state of this type, so we can switch back and forth
        //
        storageMetaMap.put(oldClass, llmMeta.getStorage());

        String newTypeName = wStorageType.getText();

        // Capture any information on the widgets
        //
        this.getWidgetsContent(llmMeta);
        IStorage oldStorage = llmMeta.getStorage();
        storageMetaMap.put(oldStorage.getClass(), oldStorage);

        // Now change the data type
        //
        try {
            llmMeta.setStorageByType(newTypeName);
        } catch (HopException e) {
            e.printStackTrace();
        }
        IStorage newStorage = llmMeta.getStorage();

        // Get possible information from the metadata map (from previous work)
        //
        if (storageMetaMap.get(newStorage.getClass()) != null)
            llmMeta.setStorage(storageMetaMap.get(newStorage.getClass()));
        else
            storageMetaMap.put(newStorage.getClass(), newStorage);

        // Remove existing children
        //
        for (Control child : wStorageSpecificComp.getChildren()) {
            child.dispose();
        }

        // Re-add the widgets
        //
        guiCompositeWidgets = new GuiCompositeWidgets(manager.getVariables());
        guiCompositeWidgets.createCompositeWidgets(
                llmMeta.getStorage(),
                null,
                wStorageSpecificComp,
                LlmMeta.GUI_PLUGIN_ELEMENT_PARENT_ID,
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

        busyChangingStorageType.set(false);
    }

    private void changeModelType(Composite parent) {
        if (busyChangingModelType.get()) {
            return;
        }
        busyChangingModelType.set(true);

        LlmMeta llmMeta = this.getMetadata();

        // Keep track of the old database type since this changes when getting the
        // content
        //
        IModel oldModel = llmMeta.getModel();

        String newTypeName = wModelType.getText();
        try {
            llmMeta.setModelByType(newTypeName);
        } catch (HopException e) {
            e.printStackTrace();
        }
        IModel newModel = llmMeta.getModel();

        // Capture any information on the widgets
        //
        this.getWidgetsContent(llmMeta);
        metaMap.put(oldModel.getClass(), oldModel);

        // Get possible information from the metadata map (from previous work)
        //
        if (metaMap.get(newModel.getClass()) != null)
            llmMeta.setModel(metaMap.get(newModel.getClass()));
        else
            metaMap.put(newModel.getClass(), newModel);

        // Remove existing children
        //
        for (Control child : wModelSpecificComp.getChildren()) {
            child.dispose();
        }

        // Re-add the widgets
        //
        guiCompositeWidgets = new GuiCompositeWidgets(manager.getVariables());
        guiCompositeWidgets.createCompositeWidgets(
                oldModel,
                null,
                wModelSpecificComp,
                LlmMeta.GUI_PLUGIN_ELEMENT_PARENT_ID,
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

        busyChangingModelType.set(false);
    }
}
