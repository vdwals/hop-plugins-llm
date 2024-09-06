package org.apache.hop.langchain4j.models;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.hop.core.Const;
import org.apache.hop.core.database.DatabaseMeta;
import org.apache.hop.i18n.BaseMessages;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Control;

public class ModelMetaEditor extends MetadataEditor<ModelMeta> {
    private static final Class<?> PKG = ModelMetaEditor.class;

    private int middle;
    private int margin;

    private Text wName;
    private Combo wModelType;

    private Composite wModelSpecificComp;
    private GuiCompositeWidgets guiCompositeWidgets;

    private Map<Class<? extends IModel>, IModel> metaMap;

    private Listener modifyListener = event -> {
        setChanged();
        MetadataPerspective.getInstance().updateEditor(this);
    };

    public ModelMetaEditor(HopGui hopGui, MetadataManager<ModelMeta> manager, ModelMeta metadata) {
        super(hopGui, manager, metadata);
    }

    @Override
    public void createControl(Composite parent) {
        Control previous = buildHeaderUI(parent);
        buildModelSelection(parent, previous);
    }

    @Override
    public void getWidgetsContent(ModelMeta meta) {
        meta.setName(wName.getText());
        guiCompositeWidgets.getWidgetsContents(meta.getModel(), ModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID);
    }

    @Override
    public void setWidgetsContent() {
        ModelMeta meta = this.getMetadata();

        wName.setText(Const.NVL(meta.getName(), ""));
        wModelType.setText(Const.NVL(meta.getModel().getName(), ""));
    }

    private Control buildHeaderUI(Composite parent) {
        // What's the name
        Label wlName = new Label(parent, SWT.RIGHT);
        PropsUi.setLook(wlName);
        wlName.setText(BaseMessages.getString(PKG, "ModelDialog.label.ModelName"));
        FormData fdlName = new FormData();
        fdlName.top = new FormAttachment(0, 0);
        fdlName.left = new FormAttachment(0, 0);
        wlName.setLayoutData(fdlName);

        wName = new Text(parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        PropsUi.setLook(wName);
        FormData fdName = new FormData();
        fdName.top = new FormAttachment(wlName, margin);
        fdName.left = new FormAttachment(0, 0);
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
        wModelType.setItems(new String[] { "OnnxModel" });
        PropsUi.setLook(wModelType);
        FormData fdConnectionType = new FormData();
        fdConnectionType.top = new FormAttachment(wlModelType, 0, SWT.CENTER);
        fdConnectionType.left = new FormAttachment(middle, 0); // To the right of the label
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
                ModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID,
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

    private AtomicBoolean busyChangingModelType = new AtomicBoolean(false);

    private void changeModelType(Composite parent) {
        if (busyChangingModelType.get()) {
            return;
        }
        busyChangingModelType.set(true);

        ModelMeta modelMeta = this.getMetadata();

        // Keep track of the old database type since this changes when getting the
        // content
        //
        Class<? extends IModel> oldClass = modelMeta.getModel().getClass();
        String newTypeName = wModelType.getText();
        wModelType.setText(modelMeta.getPluginName());

        // Capture any information on the widgets
        //
        this.getWidgetsContent(modelMeta);

        // Save the state of this type, so we can switch back and forth
        //
        metaMap.put(oldClass, modelMeta.getModel());

        // Now change the data type
        //
        wModelType.setText(newTypeName);
        modelMeta.setModelType(newTypeName);

        // Get possible information from the metadata map (from previous work)
        //
        modelMeta.setModel(metaMap.get(modelMeta.getModel().getClass()));

        // Remove existing children
        //
        for (Control child : wModelSpecificComp.getChildren()) {
            child.dispose();
        }

        // Re-add the widgets
        //
        guiCompositeWidgets = new GuiCompositeWidgets(manager.getVariables());
        guiCompositeWidgets.createCompositeWidgets(
                modelMeta.getModel(),
                null,
                wModelSpecificComp,
                DatabaseMeta.GUI_PLUGIN_ELEMENT_PARENT_ID,
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
