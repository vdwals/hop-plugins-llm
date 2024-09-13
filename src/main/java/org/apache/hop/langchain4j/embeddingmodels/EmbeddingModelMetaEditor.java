package org.apache.hop.langchain4j.embeddingmodels;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.langchain4j.embeddingmodels.onnx.OnnxModelMeta;
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

public class EmbeddingModelMetaEditor extends MetadataEditor<EmbeddingModelMeta> {
    private static final Class<?> PKG = EmbeddingModelMetaEditor.class;

    private int middle;
    private int margin;

    private Text wName;
    private Combo wModelType;

    private GuiCompositeWidgets guiModelCompositeWidgets;
    private Composite wModelSpecificComp;

    private Map<Class<? extends IModel>, IModel> modelMetaMap;

    private AtomicBoolean busyChangingModelType = new AtomicBoolean(false);

    private Listener modifyListener = event -> {
        setChanged();
        MetadataPerspective.getInstance().updateEditor(this);
    };

    public EmbeddingModelMetaEditor(HopGui hopGui, MetadataManager<EmbeddingModelMeta> manager,
            EmbeddingModelMeta metadata) {
        super(hopGui, manager, metadata);

        middle = PropsUi.getInstance().getMiddlePct();
        margin = PropsUi.getMargin();

        modelMetaMap = populateModelMetaMap();
        IModel defaultModel = metadata.getModel();
        modelMetaMap.put(defaultModel.getClass(), defaultModel);
    }

    private Map<Class<? extends IModel>, IModel> populateModelMetaMap() {
        modelMetaMap = new HashMap<>();

        modelMetaMap.put(OnnxModelMeta.class, new OnnxModelMeta());

        return modelMetaMap;
    }

    @Override
    public void createControl(Composite parent) {
        Control previous = buildHeaderUI(parent);
        buildModelSelection(parent, previous);
        setWidgetsContent();
    }

    @Override
    public void setWidgetsContent() {
        EmbeddingModelMeta meta = this.getMetadata();

        wName.setText(Const.NVL(meta.getName(), ""));

        String modelName = meta.getModel().getName();
        busyChangingModelType.set(true);
        wModelType.setText(Const.NVL(modelName, OnnxModelMeta.NAME));

        guiModelCompositeWidgets.setWidgetsContents(
                meta.getModel(),
                wModelSpecificComp,
                EmbeddingModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID);
        busyChangingModelType.set(false);
    }

    @Override
    public void getWidgetsContent(EmbeddingModelMeta meta) {
        meta.setName(wName.getText());

        IModel model = meta.getModel();
        guiModelCompositeWidgets.getWidgetsContents(model, EmbeddingModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID);
        modelMetaMap.put(model.getClass(), model);
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
        guiModelCompositeWidgets = new GuiCompositeWidgets(manager.getVariables());
        guiModelCompositeWidgets.createCompositeWidgets(
                new OnnxModelMeta(),
                null,
                wModelSpecificComp,
                EmbeddingModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID,
                null);

        // Add listener to detect change
        guiModelCompositeWidgets.setWidgetsListener(
                new GuiCompositeWidgetsAdapter() {
                    @Override
                    public void widgetModified(
                            GuiCompositeWidgets compositeWidgets, Control changedWidget, String widgetId) {
                        setChanged();
                    }
                });

        return lastControl;
    }

    private void changeModelType(Composite parent) {
        if (busyChangingModelType.get()) {
            return;
        }
        busyChangingModelType.set(true);

        EmbeddingModelMeta llmMeta = this.getMetadata();

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
        modelMetaMap.put(oldModel.getClass(), oldModel);

        // Get possible information from the metadata map (from previous work)
        //
        if (modelMetaMap.get(newModel.getClass()) != null)
            llmMeta.setModel(modelMetaMap.get(newModel.getClass()));
        else
            modelMetaMap.put(newModel.getClass(), newModel);

        // Remove existing children
        //
        for (Control child : wModelSpecificComp.getChildren()) {
            child.dispose();
        }

        // Re-add the widgets
        //
        guiModelCompositeWidgets = new GuiCompositeWidgets(manager.getVariables());
        guiModelCompositeWidgets.createCompositeWidgets(
                oldModel,
                null,
                wModelSpecificComp,
                EmbeddingModelMeta.GUI_PLUGIN_ELEMENT_PARENT_ID,
                null);
        guiModelCompositeWidgets.setWidgetsListener(
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
