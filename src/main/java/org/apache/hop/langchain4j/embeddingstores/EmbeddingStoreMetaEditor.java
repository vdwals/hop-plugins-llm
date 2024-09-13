package org.apache.hop.langchain4j.embeddingstores;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.langchain4j.embeddingstores.inmemory.InMemoryStorageMeta;
import org.apache.hop.langchain4j.embeddingstores.neo4j.Neo4jStorageMeta;
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

public class EmbeddingStoreMetaEditor extends MetadataEditor<EmbeddingStoreMeta> {
    private static final Class<?> PKG = EmbeddingStoreMetaEditor.class;

    private int middle;
    private int margin;

    private Text wName;
    private Combo wStorageType;

    private GuiCompositeWidgets guiStorageCompositeWidgets;
    private Composite wStorageSpecificComp;

    private Map<Class<? extends IStorage>, IStorage> storageMetaMap;

    private AtomicBoolean busyChangingStorageType = new AtomicBoolean(false);

    private Listener modifyListener = event -> {
        setChanged();
        MetadataPerspective.getInstance().updateEditor(this);
    };

    public EmbeddingStoreMetaEditor(HopGui hopGui, MetadataManager<EmbeddingStoreMeta> manager,
            EmbeddingStoreMeta metadata) {
        super(hopGui, manager, metadata);

        middle = PropsUi.getInstance().getMiddlePct();
        margin = PropsUi.getMargin();

        storageMetaMap = populateStorageMetaMap();
        IStorage defaultStorage = metadata.getStorage();
        storageMetaMap.put(defaultStorage.getClass(), defaultStorage);
    }

    private Map<Class<? extends IStorage>, IStorage> populateStorageMetaMap() {
        storageMetaMap = new HashMap<>();

        storageMetaMap.put(InMemoryStorageMeta.class, new InMemoryStorageMeta());
        storageMetaMap.put(Neo4jStorageMeta.class, new Neo4jStorageMeta());

        return storageMetaMap;
    }

    @Override
    public void createControl(Composite parent) {
        Control previous = buildHeaderUI(parent);
        previous = buildStorageSelection(parent, previous);
        setWidgetsContent();
    }

    @Override
    public void setWidgetsContent() {
        EmbeddingStoreMeta meta = this.getMetadata();

        wName.setText(Const.NVL(meta.getName(), ""));

        String storageName = meta.getStorage().getName();

        busyChangingStorageType.set(true);
        wStorageType.setText(Const.NVL(storageName, InMemoryStorageMeta.NAME));

        guiStorageCompositeWidgets.setWidgetsContents(
                meta.getStorage(),
                wStorageSpecificComp,
                EmbeddingStoreMeta.GUI_PLUGIN_ELEMENT_PARENT_ID);
        busyChangingStorageType.set(false);
    }

    @Override
    public void getWidgetsContent(EmbeddingStoreMeta meta) {
        meta.setName(wName.getText());

        IStorage storage = meta.getStorage();
        guiStorageCompositeWidgets.getWidgetsContents(storage, EmbeddingStoreMeta.GUI_PLUGIN_ELEMENT_PARENT_ID);
        storageMetaMap.put(storage.getClass(), storage);
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
        guiStorageCompositeWidgets = new GuiCompositeWidgets(manager.getVariables());
        guiStorageCompositeWidgets.createCompositeWidgets(
                metadata.getStorage(),
                null,
                wStorageSpecificComp,
                EmbeddingStoreMeta.GUI_PLUGIN_ELEMENT_PARENT_ID,
                null);

        // Add listener to detect change
        guiStorageCompositeWidgets.setWidgetsListener(
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

        EmbeddingStoreMeta meta = this.getMetadata();

        // Keep track of the old database type since this changes when getting the
        // content
        //
        Class<? extends IStorage> oldClass = meta.getStorage().getClass();

        // Save the state of this type, so we can switch back and forth
        //
        storageMetaMap.put(oldClass, meta.getStorage());

        String newTypeName = wStorageType.getText();

        // Capture any information on the widgets
        //
        this.getWidgetsContent(meta);
        IStorage oldStorage = meta.getStorage();
        storageMetaMap.put(oldStorage.getClass(), oldStorage);

        // Now change the data type
        //
        try {
            meta.setStorageByType(newTypeName);
        } catch (HopException e) {
            e.printStackTrace();
        }
        IStorage newStorage = meta.getStorage();

        // Get possible information from the metadata map (from previous work)
        //
        if (storageMetaMap.get(newStorage.getClass()) != null)
            meta.setStorage(storageMetaMap.get(newStorage.getClass()));
        else
            storageMetaMap.put(newStorage.getClass(), newStorage);

        // Remove existing children
        //
        for (Control child : wStorageSpecificComp.getChildren()) {
            child.dispose();
        }

        // Re-add the widgets
        //
        guiStorageCompositeWidgets = new GuiCompositeWidgets(manager.getVariables());
        guiStorageCompositeWidgets.createCompositeWidgets(
                meta.getStorage(),
                null,
                wStorageSpecificComp,
                EmbeddingStoreMeta.GUI_PLUGIN_ELEMENT_PARENT_ID,
                null);
        guiStorageCompositeWidgets.setWidgetsListener(
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
}