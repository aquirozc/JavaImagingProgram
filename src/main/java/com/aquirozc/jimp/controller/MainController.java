package com.aquirozc.jimp.controller;

import com.aquirozc.jimp.data.TimelineRecord;
import com.aquirozc.jimp.data.TimelineStack;
import com.aquirozc.jimp.engine.ColorOp;
import com.aquirozc.jimp.helper.FXImageIO;
import com.aquirozc.jimp.init.FXApp;
import com.aquirozc.jimp.strings.Strings;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;


public class MainController {

    private Parent parent = FXApp.MAIN_EDITOR;

    private MenuItem openBtn = ((MenuBar)parent.lookup("#top_menu")).getMenus().get(0).getItems().get(0);
    private MenuItem saveBtn = ((MenuBar)parent.lookup("#top_menu")).getMenus().get(0).getItems().get(1);
    private MenuItem undoBtn = ((MenuBar)parent.lookup("#top_menu")).getMenus().get(1).getItems().get(0);
    private MenuItem restoreBtn = ((MenuBar)parent.lookup("#top_menu")).getMenus().get(1).getItems().get(1);
    private MenuItem aboutBtn = ((MenuBar)parent.lookup("#top_menu")).getMenus().get(2).getItems().get(0);
    private Slider zoomBar = (Slider) parent.lookup("#zoom_bar");
    private ImageView targetVW = (ImageView) parent.lookup("#target_vw");

    private Image ogImage;
    private Image diskImage;
    private FXImageIO imgHelper;
    private Alert bwPrompt = new Alert(AlertType.CONFIRMATION, Strings.BW_WARNING);
    private TimelineStack history = new TimelineStack(5);

    private GrayScaleOPController grayOPController = new GrayScaleOPController(this);
    private SpatialOPController spatialOPController = new SpatialOPController(this);
    private HistogramController histogramOPController = new HistogramController(this);
    private ConvolveOPController convolveOPController = new ConvolveOPController(this);
    private ColorOPController colorOPController = new ColorOPController(this);
    private OverrideOPController overrideOPController = new OverrideOPController(this);

    private boolean wasBWImage = false;
    private boolean isBWImage = false;

    public MainController (Stage stage){

        openBtn.setOnAction(this::openImageFromDisk);
        saveBtn.setOnAction(e -> imgHelper.saveImageToDisk(ogImage));
        undoBtn.setOnAction(this::undoChanges);
        restoreBtn.setOnAction(this::restoreChanges);
        aboutBtn.setOnAction(e -> new Alert(AlertType.INFORMATION,Strings.PROGRAM_INFO).showAndWait());
        zoomBar.valueProperty().addListener(this::updateZoomLevel);

        imgHelper = new FXImageIO(stage);
        targetVW = (ImageView) parent.lookup("#target_vw");

    }

    public void applyChanges(){

        if(isOGImageNull()){
            return;
        }

        history.add(new TimelineRecord(diskImage, wasBWImage));
        ogImage = targetVW.getImage();
        updateZoomLevel(null, null, zoomBar.getValue());
        wasBWImage = isBWImage;

    }

    public Image getOGImage(){
        return ogImage;
    }

    public ImageView getTargetVW(){
        return targetVW;
    }

    private void onRefresh(){
        grayOPController.resetSliders();
        histogramOPController.onRefresh();
        spatialOPController.onRefresh();
        overrideOPController.onRefresh();
    }

    private void openImageFromDisk(ActionEvent e){
        diskImage = imgHelper.readImage();
        restoreChanges(null);
    }

    public void refreshCanvas(){

        onRefresh();

        if(isOGImageNull()){
            return;
        }

        updateCanvas(ogImage);

    }

    public void updateCanvas(Image img){
        targetVW.setImage(img);
    }

    public boolean isOGImageNull(){
        return ogImage == null;
    }

    public boolean isBWImage(){

        if(!isBWImage){
            isBWImage = bwPrompt.showAndWait().get().equals(ButtonType.OK);
            bwPrompt.close();
            if(isBWImage){
                updateCanvas(ColorOp.toGrayScale(ogImage));
                applyChanges();
            }
        }

        return isBWImage;
    }

    private void restoreChanges(ActionEvent e){

        if(diskImage == null){return;}

        history.clear();

        int w = (int) diskImage.getWidth(); int h = (int) diskImage.getHeight();

        ogImage = new WritableImage(diskImage.getPixelReader(), w, h);
        isBWImage = false;

        targetVW.setImage(ogImage);
        targetVW.setFitHeight(h);
        targetVW.setFitWidth(w);

        zoomBar.setValue(100);
        refreshCanvas();

    }

    public void setIsBWImage(boolean isBWImage){
        this.isBWImage = isBWImage;
    }

    private void undoChanges(ActionEvent e){
        
        if(history.isEmpty()){
            return;
        }

        onRefresh();

        TimelineRecord record = history.pop();
        ogImage = record.img();
        wasBWImage = record.wasBWImage();
        isBWImage = wasBWImage;
        updateCanvas(ogImage);

    }

    private void updateZoomLevel(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
		
		if (ogImage == null) {
	        return;
	    }

        double newFactor = (double) newValue / 100d;

	    targetVW.setFitHeight(ogImage.getHeight() * newFactor);
	    targetVW.setFitWidth(ogImage.getWidth() * newFactor);
		
	}
    
}
