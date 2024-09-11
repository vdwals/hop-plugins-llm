package org.apache.hop.langchain4j.storages;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.hop.core.Const;
import org.apache.hop.i18n.BaseMessages;
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

public class StorageMetaEditor extends MetadataEditor<StorageMeta> {
    private static final Class<?> PKG = StorageMetaEditor.class;

    private int middle;
    private int margin;

    private Text wName;
    private Combo wStorageType;

    private Composite wStorageSpecificComp;
    private GuiCompositeWidgets guiCompositeWidgets;

    private Map<Class<? extends IStorage>, IStorage> metaMap;

    private AtomicBoolean busyChangingStorageType = new AtomicBoolean(false);

    private Listener modifyListener = event -> {
        setChanged();
        MetadataPerspective.getInstance().updateEditor(this);
    };

    public StorageMetaEditor(HopGui hopGui, MetadataManager<StorageMeta> manager, StorageMeta metadata) {
        super(hopGui, manager, metadata);

        middle = PropsUi.getInstance().getMiddlePct();
        margin = PropsUi.getMargin();

        metaMap = populateMetaMap();
        IStorage defaultStorage = metadata.getStorage();
        metaMap.put(defaultStorage.getClass(), defaultStorage);
    }

    private Map<Class<? extends IStorage>, IStorage> populateMetaMap() {
        metaMap = new HashMap<>();

        metaMap.put(InMemoryStorageMeta.class, new InMemoryStorageMeta());
        metaMap.put(Neo4jStorageMeta.class, new Neo4jStorageMeta());

        return metaMap;
    }

    @Override
    public void createControl(Composite parent) {
        Control previous = buildHeaderUI(parent);
        buildStorageSelection(parent, previous);
        setWidgetsContent();
    }

    @Override
    public void setWidgetsContent() {
        StorageMeta meta = this.getMetadata();

        wName.setText(Const.NVL(meta.getName(), ""));

        String storageName = meta.getStorage().getName();

        busyChangingStorageType.set(true);
        wStorageType.setText(Const.NVL(storageName, InMemoryStorageMeta.NAME));

        guiCompositeWidgets.setWidgetsContents(
                meta.getStorage(),
                wStorageSpecificComp,
                StorageMeta.GUI_PLUGIN_ELEMENT_PARENT_ID);
        busyChangingStorageType.set(false);
    }

    @Override
    public void getWidgetsContent(StorageMeta meta) {
        meta.setName(wName.getText());
        IStorage storage = meta.getStorage();
        guiCompositeWidgets.getWidgetsContents(storage, StorageMeta.GUI_PLUGIN_ELEMENT_PARENT_ID);
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
                metaMap.values().stream().map(IStorage::getName).collect(Collectors.toList()).toArray(new String[0]));
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
                StorageMeta.GUI_PLUGIN_ELEMENT_PARENT_ID,
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

        StorageMeta storageMeta = this.getMetadata();

        // Keep track of the old database type since this changes when getting the
        // content
        //
        Class<? extends IStorage> oldClass = storageMeta.getStorage().getClass();

        // Save the state of this type, so we can switch back and forth
        //
        metaMap.put(oldClass, storageMeta.getStorage());

        String newTypeName = wStorageType.getText();

        // Capture any information on the widgets
        //
        this.getWidgetsContent(storageMeta);
        IStorage oldStorage = storageMeta.getStorage();
        metaMap.put(oldStorage.getClass(), oldStorage);

        // Now change the data type
        //
        wStorageType.setText(newTypeName);

        // Get possible information from the metadata map (from previous work)
        //
        if (metaMap.get(storageMeta.getStorage().getClass()) != null)
            storageMeta.setStorage(metaMap.get(storageMeta.getStorage().getClass()));
        else
            metaMap.put(storageMeta.getStorage().getClass(), storageMeta.getStorage());

        // Remove existing children
        //
        for (Control child : wStorageSpecificComp.getChildren()) {
            child.dispose();
        }

        // Re-add the widgets
        //
        guiCompositeWidgets = new GuiCompositeWidgets(manager.getVariables());
        guiCompositeWidgets.createCompositeWidgets(
                storageMeta.getStorage(),
                null,
                wStorageSpecificComp,
                StorageMeta.GUI_PLUGIN_ELEMENT_PARENT_ID,
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

}
