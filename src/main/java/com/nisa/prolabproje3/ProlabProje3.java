package com.nisa.prolabproje3;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;

public class ProlabProje3 extends Application{

    private CitationGraph graphManager;
    private List<Article> allArticles;
    
    // Which article is where on the screen
    private Map<String, double[]> coordinateMap = new HashMap<>();
    private Map<String, Article> articlesOnScreen = new HashMap<>();
    
    // Coordinate list of points forming the DNA helix
    private List<SpacePoint> dnaPoints = new ArrayList<>();
    private Map<String, VisualObject> dnaVisuals = new HashMap<>();
    private double cameraZ = -600; 
    private boolean isDnaModeActive = false;
    
    private Map<String, Double> betweennessScores = new HashMap<>();
    private List<Article> kCoreList = new ArrayList<>();
    private boolean isKCoreModeActive = false;
    
    // For Back button
    private Stack<Article> historyStack = new Stack<>();
    private Article currentArticle = null; 
    
    // Interface 
    private Pane graphPane;
    private Group drawingGroup; 
    private TextArea infoPane;
    private TextField searchBox;
    private Button backButton;
    
    private Label lblTotalArticles, lblTotalEdges, lblMostCited, lblMostRefs;
    private ToggleButton hCoreModeButton; 
    
    // Analysis Panel
    private TextField kValueBox;
    private Button kCoreAnalyzeButton;
    private Button resetAnalysisButton;
    private Button dnaModeButton; 
    private Button mainScreenButton;

    private static final int WIDTH = 1300;
    private static final int HEIGHT = 850;

    public static void main(String[] args){
        launch(args);
    }

    @Override
    public void start(Stage primaryStage){
        BorderPane mainLayout = new BorderPane();
        
        // Left Panel
        VBox leftPanel = new VBox(15); // 15px space between elements
        leftPanel.setPadding(new Insets(20)); // 20px padding from edges
        leftPanel.setPrefWidth(380); 
        // Background color and border
        leftPanel.setStyle("-fx-background-color: #111827; -fx-border-color: #374151; -fx-border-width: 0 1 0 0;");

        // Title Label
        Label titleLabel = new Label("Citation Graph Analysis");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web("#60a5fa")); 

        // File Load Button
        Button loadButton = new Button("Load JSON File");
        loadButton.setMaxWidth(Double.MAX_VALUE);
        loadButton.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");
        
        // Statistic Info Area
        lblTotalArticles = new Label("Total Papers: -"); 
        lblTotalArticles.setTextFill(Color.LIGHTGRAY);
        
        lblTotalEdges = new Label("Total Links: -"); 
        lblTotalEdges.setTextFill(Color.LIGHTGRAY);
        
        lblMostCited = new Label("Most Popular: -"); 
        lblMostCited.setTextFill(Color.web("#f87171")); 
        lblMostCited.setWrapText(true); // Wrap to next line if text is long.
        
        lblMostRefs = new Label("Most Refs: -"); 
        lblMostRefs.setTextFill(Color.web("#34d399")); 
        lblMostRefs.setWrapText(true);
        
        VBox statsBox = new VBox(5);
        statsBox.getChildren().addAll(lblTotalArticles, lblTotalEdges, new Separator(), lblMostCited, lblMostRefs);

        // H-Core Mode Button
        hCoreModeButton = new ToggleButton("H-CORE MODE (OFF)");
        hCoreModeButton.setMaxWidth(Double.MAX_VALUE);
        hCoreModeButton.setStyle("-fx-background-color: #374151; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        
        hCoreModeButton.setOnAction(e -> {
            if (hCoreModeButton.isSelected()){
                // Mode On
                hCoreModeButton.setText("H-CORE MODE (ON)");
                hCoreModeButton.setStyle("-fx-background-color: #d97706; -fx-text-fill: white; -fx-font-weight: bold;"); 
                infoPane.setText("MODE: H-CORE\nFilter active.");
                backButton.setDisable(true); 
            }else{
                // Mode Off
                hCoreModeButton.setText("H-CORE MODE (OFF)");
                hCoreModeButton.setStyle("-fx-background-color: #374151; -fx-text-fill: white; -fx-font-weight: bold;");
                infoPane.setText("MODE: STANDARD");
                // Enable back button if there is history.
                if (!historyStack.isEmpty()) backButton.setDisable(false);
            }
            updateColors();
        });

        // K-Core Analysis Panel
        Label analysisLabel = new Label("K-Core Analysis (k value):");
        analysisLabel.setTextFill(Color.LIGHTGRAY);
        
        HBox analysisBox = new HBox(5);
        kValueBox = new TextField();
        kValueBox.setPromptText("k=?");
        kValueBox.setPrefWidth(50);
        kValueBox.setStyle("-fx-background-color: #1f2937; -fx-text-fill: white; -fx-border-color: #4b5563;");
        
        kCoreAnalyzeButton = new Button("Analyze"); // Analyze button
        kCoreAnalyzeButton.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: bold;");
        
        resetAnalysisButton = new Button("X"); // Reset button
        resetAnalysisButton.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold;");
        
        analysisBox.getChildren().addAll(kValueBox, kCoreAnalyzeButton, resetAnalysisButton);

        // Article ID Search Button
        Label searchLabel = new Label("Search Paper ID:"); searchLabel.setTextFill(Color.LIGHTGRAY);
        HBox searchBoxPanel = new HBox(5);
        
        // Back Button
        backButton = new Button("⬅");
        backButton.setDisable(true);
        backButton.setStyle("-fx-background-color: #ea580c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        backButton.setTooltip(new Tooltip("Go Back"));
        
        searchBox = new TextField();
        searchBox.setPromptText("Ex: W200...");
        searchBox.setPrefWidth(100); 
        searchBox.setStyle("-fx-background-color: #1f2937; -fx-text-fill: white; -fx-border-color: #4b5563;");
        
        Button findButton = new Button("Find");
        findButton.setStyle("-fx-background-color: #059669; -fx-text-fill: white;");
        
        // DNA Mode Button
        dnaModeButton = new Button("DNA"); 
        dnaModeButton.setStyle("-fx-background-color: #0891b2; -fx-text-fill: white; -fx-font-weight: bold;");
        
        // List Mode Button
        mainScreenButton = new Button("List"); 
        mainScreenButton.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-font-weight: bold;");
        
        searchBoxPanel.getChildren().addAll(backButton, searchBox, findButton, dnaModeButton, mainScreenButton);

        Label detailsLabel = new Label("Details:"); detailsLabel.setTextFill(Color.LIGHTGRAY);
        infoPane = new TextArea();
        infoPane.setEditable(false);
        infoPane.setWrapText(true);
        infoPane.setPrefHeight(250); 
        infoPane.setStyle("-fx-control-inner-background: #1f2937; -fx-text-fill: white; -fx-border-color: #374151;");

        // Add all info to the left panel
        leftPanel.getChildren().addAll(
            titleLabel, new Separator(), 
            loadButton, statsBox, new Separator(), 
            new Label("View Mode:"), hCoreModeButton, new Separator(),
            analysisLabel, analysisBox, new Separator(),
            searchLabel, searchBoxPanel, new Separator(), 
            detailsLabel, infoPane
        );

        // Center Panel
        graphPane = new Pane();
        drawingGroup = new Group(); // Add objects to group for zooming
        graphPane.getChildren().add(drawingGroup);
        graphPane.setStyle("-fx-background-color: #0f172a;"); 
        
        // Prevent objects from spilling over to the left panel when zooming.
        javafx.scene.shape.Rectangle clipRect = new javafx.scene.shape.Rectangle();
        clipRect.widthProperty().bind(graphPane.widthProperty());
        clipRect.heightProperty().bind(graphPane.heightProperty());
        graphPane.setClip(clipRect);
        
        setupMouseInteraction(graphPane, drawingGroup);
        
        mainLayout.setLeft(leftPanel); // Place left menu
        mainLayout.setCenter(graphPane); // Graph in the center

        // File Loading
        loadButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) loadData(file.getAbsolutePath()); // Start reading file.
        });
        
        // ID Search
        findButton.setOnAction(e -> {
            String id = searchBox.getText().trim();
            if (graphManager != null && !id.isEmpty()){
                Article a = graphManager.findArticle(id);
                if (a != null){
                    resetMode(false); // Exit DNA or List mode.
                    if (currentArticle != null){
                        historyStack.push(currentArticle); // Save current article to history.
                        backButton.setDisable(false); // Enable back button.
                    }
                    drawStandardGraph(a); // Draw new article in center.
                    showDetails(a);
                } else infoPane.setText("Not found.");
            }
        });
        
        // Back Button
        backButton.setOnAction(e -> {
            if (!historyStack.isEmpty()){
                Article previous = historyStack.pop(); // Pop last item from stack.
                currentArticle = previous; 
                drawStandardGraph(previous);
                showDetails(previous);
                searchBox.setText(previous.getId());
                if (historyStack.isEmpty()) backButton.setDisable(true); // Disable button if stack empty.
            }
        });
        
        dnaModeButton.setOnAction(e -> {
            resetMode(true); // Clear history when switching to DNA
            drawDnaMode();
            infoPane.setText("DNA TUNNEL MODE.\n\n- Spiral structure sorted by ID.\n- To enter: Mouse Wheel (Forward)");
        });
        
        mainScreenButton.setOnAction(e -> {
            resetMode(true); // Clear history when switching to List
            drawGridMode();
            infoPane.setText("List View (Grid).\n\n- All papers listed by ID order.");
        });

        // K-Core Analysis
        kCoreAnalyzeButton.setOnAction(e -> {
            try {
                int k = Integer.parseInt(kValueBox.getText().trim()); // Get number.
                if (graphManager != null){
                    infoPane.setText("Performing K-Core Analysis (k=" + k + ")...");
                    kCoreList = graphManager.performKCoreAnalysis(k);
                    isKCoreModeActive = true;
                    updateColors();
                    infoPane.setText("K-Core Analysis Completed.\n\nCriteria: At least " + k + " links.\nRemaining Papers: " + kCoreList.size());
                }
            } catch (NumberFormatException ex){
                infoPane.setText("Please enter a valid number.");
            }
        });
        
        // Reset Analysis.
        resetAnalysisButton.setOnAction(e -> {
            isKCoreModeActive = false;
            kCoreList.clear();
            kValueBox.clear();
            updateColors();
            infoPane.setText("Analysis reset.");
        });

        Scene scene = new Scene(mainLayout, WIDTH, HEIGHT);
        primaryStage.setTitle("Citation Graph Visualizer Pro - Final 2.0");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private void resetMode(boolean clearHistory){
        // Turn off H-Core Mode.
        hCoreModeButton.setSelected(false); // Uncheck button.
        isKCoreModeActive = false;
        isDnaModeActive = false;
        hCoreModeButton.setText("H-CORE MODE (OFF)");
        hCoreModeButton.setStyle("-fx-background-color: #374151; -fx-text-fill: white; -fx-font-weight: bold;");
        graphPane.setStyle("-fx-background-color: #0f172a;"); 
        
        // Clear history.
        if (clearHistory){
            historyStack.clear();
            backButton.setDisable(true);
        }
        resetZoom();
    }
    
    private void resetZoom(){
        drawingGroup.setTranslateX(0);
        drawingGroup.setTranslateY(0);
        drawingGroup.setScaleX(1);
        drawingGroup.setScaleY(1);
        cameraZ = -600; 
    }

    // Mouse and zoom interaction
    private void setupMouseInteraction(Pane canvas, Group group) {
        final double[] start = new double[2]; 
        final double[] translate = new double[2]; 

        canvas.setOnMousePressed(event -> {
            // Save mouse coordinates on screen.
            start[0] = event.getX(); 
            start[1] = event.getY();
            // Save current group translation.
            translate[0] = group.getTranslateX(); 
            translate[1] = group.getTranslateY();
        });

        canvas.setOnMouseDragged(event -> {
            if (!isDnaModeActive) {
                // Shift graph as much as mouse moved.
                group.setTranslateX(translate[0] + event.getX() - start[0]);
                group.setTranslateY(translate[1] + event.getY() - start[1]);
            }
        });

        canvas.addEventFilter(ScrollEvent.ANY, event -> {
            // Exit if no movement.
            if (event.getDeltaY() == 0) 
                return;

            if (isDnaModeActive){
                double speed = 60; // How much to move per tick.
                if (event.getDeltaY() > 0) 
                    cameraZ += speed; 
                else cameraZ -= speed; 
                
                double maxZ = allArticles.size() * 45 + 1000; 
                cameraZ = Math.max(-1000, Math.min(cameraZ, maxZ));
                update3DView(); 
            }else{
                double zoomFactor = 1.1; // 10% zoom in/out ratio
                if (event.getDeltaY() < 0) zoomFactor = 1 / zoomFactor;
                // Boundary check to prevent excessive zooming
                double oldScale = group.getScaleX();
                double newScale = oldScale * zoomFactor;
                if (newScale < 0.05 || newScale > 20) 
                    return;
                
                double mouseX = event.getX();
                double mouseY = event.getY();
                double f = (newScale / oldScale) - 1;
                // Shift graph in opposite direction to keep mouse position stable.
                double dx = (mouseX - group.getTranslateX()) * f;
                double dy = (mouseY - group.getTranslateY()) * f;
                
                // Apply new values
                group.setScaleX(newScale); 
                group.setScaleY(newScale);
                group.setTranslateX(group.getTranslateX() - dx);
                group.setTranslateY(group.getTranslateY() - dy);
            }
            event.consume();
        });
    }

    private void loadData(String path){
        infoPane.setText("Loading...");
        new Thread(() -> {
            graphManager = new CitationGraph();
            allArticles = graphManager.readJsonFile(path);
            // Doing betweenness here because it takes time.
            betweennessScores = graphManager.calculateBetweenness();
            javafx.application.Platform.runLater(() -> {
                lblTotalArticles.setText("Total Papers: " + allArticles.size());
                Article pop = graphManager.findMostCited(allArticles);
                if (pop != null) lblMostCited.setText("Most Popular: " + getCleanId(pop.getId()) + " (" + pop.getCitationCount() + ")");
                Article ref = graphManager.findMostReferencing(allArticles);
                if (ref != null) lblMostRefs.setText("Most Refs: " + getCleanId(ref.getId()) + " (" + ref.getReferences().size() + ")");
                drawDnaMode();
                infoPane.setText("Data loaded.");
            });
        }).start();
    }

    // DNA Mode
    private void drawDnaMode() {
        clearScreen();
        isDnaModeActive = true;
        cameraZ = -600; 
        graphPane.setStyle("-fx-background-color: #000000;"); 
        
        dnaPoints.clear(); 
        dnaVisuals.clear(); 
        drawingGroup.getChildren().clear();
        
        // Find center of screen for spiral center.
        double centerX = graphPane.getWidth() / 2;
        double centerY = graphPane.getHeight() / 2;
        if (centerX == 0) { centerX = 600; centerY = 400; 
        }
        
        // Sort articles by ID
        List<Article> sortedList = new ArrayList<>(allArticles);
        sortedList.sort(Comparator.comparing(Article::getId));

        double radius = 400; 
        double angleStep = 0.4; 
        double zStep = 45; 

        // Loop to rotate 3D points.
        for (int i = 0; i < sortedList.size(); i++){
            Article m = sortedList.get(i);
            double angle = i * angleStep;
            double x = Math.cos(angle) * radius;
            double y = Math.sin(angle) * radius;
            double z = i * zStep; 
            dnaPoints.add(new SpacePoint(x, y, z));
            
            Color color = getNodeColor(m, false);
            Circle c = new Circle(0, 0, 20); 
            c.setFill(color);
            c.setStroke(Color.WHITE); c.setStrokeWidth(1); c.setEffect(new Glow(0.6)); 
            
            Text label = new Text(getCleanId(m.getId()) + "\n" + getAuthorInitials(m));
            label.setFont(Font.font("Arial", FontWeight.BOLD, 14)); 
            label.setFill(Color.WHITE);
            label.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            label.setVisible(false); 
            
            Line greenLine = null;
            if (i < sortedList.size() - 1){
                greenLine = new Line();
                greenLine.setStroke(Color.LIMEGREEN); greenLine.setStrokeWidth(3); greenLine.setOpacity(0.5);
                drawingGroup.getChildren().add(greenLine);
            }
            drawingGroup.getChildren().addAll(c, label);
            VisualObject vo = new VisualObject(m, c, greenLine, label);
            dnaVisuals.put(m.getId(), vo);
            c.setOnMouseClicked(e -> { 
                resetMode(false); 
                if (currentArticle != null && !currentArticle.getId().equals(m.getId())){
                    historyStack.push(currentArticle);
                    backButton.setDisable(false);
                }
                drawStandardGraph(m); 
                showDetails(m); 
            });
            // Show info on mouse hover.
            Tooltip.install(c, new Tooltip("ID: " + getCleanId(m.getId()) + "\nCitations: " + m.getCitationCount()));
        }
        drawingGroup.setTranslateX(centerX); 
        drawingGroup.setTranslateY(centerY);
        update3DView(); 
    }
    
    // Scale objects according to camera position.
    private void update3DView(){
        if (!isDnaModeActive) 
            return;
        double focalLength = 600; 
        List<Article> sortedList = new ArrayList<>(allArticles);
        sortedList.sort(Comparator.comparing(Article::getId));

        // Calculate points (circles)
        for (int i = 0; i < sortedList.size(); i++){
            Article m = sortedList.get(i);
            SpacePoint p = dnaPoints.get(i);
            VisualObject vo = dnaVisuals.get(m.getId());
            double distZ = p.z - cameraZ;
            
            if (distZ < 10 || distZ > 4000){
                vo.circle.setVisible(false);
                vo.label.setVisible(false);
                if (vo.greenLine != null) 
                    vo.greenLine.setVisible(false);
                continue;
            }
            // Enable objects if in visible range
            vo.circle.setVisible(true);
            if (vo.greenLine != null) 
                vo.greenLine.setVisible(true);
            
            // Perspective formula - distant objects look small, close ones look large.
            double scale = focalLength / (focalLength + distZ);
            double projX = p.x * scale; double projY = p.y * scale;
            
            vo.circle.setTranslateX(projX); vo.circle.setTranslateY(projY);
            // Hide text if object is too far. Show when close.
            if (scale > 0.6){ 
                vo.label.setVisible(true);
                vo.label.setTranslateX(projX - 20 * scale); 
                vo.label.setTranslateY(projY + 25 * scale); 
                // Scale label based on depth.
                vo.label.setScaleX(scale); 
                vo.label.setScaleY(scale);
            }else{
                vo.label.setVisible(false); 
            }
            
            vo.circle.setScaleX(scale); 
            vo.circle.setScaleY(scale);
            // Fade distant objects.
            double opacity = 1.0 - (distZ / 4000.0);
            opacity = Math.max(0, Math.min(1, opacity));
            
            // K-Core mode coloring
            if(isKCoreModeActive && !kCoreList.contains(m)){
                vo.circle.setFill(Color.TOMATO); opacity *= 0.2; 
            } 
            else if (isKCoreModeActive) {
                vo.circle.setFill(Color.LIMEGREEN); 
            }
            
            vo.circle.setOpacity(opacity);
            if (vo.label.isVisible()) 
                vo.label.setOpacity(opacity);
        }
        
        // Update line between circles since positions changed.
        for (int i = 0; i < sortedList.size() - 1; i++){
            Article m1 = sortedList.get(i);
            VisualObject vo1 = dnaVisuals.get(m1.getId());
            if (vo1.greenLine != null && vo1.circle.isVisible()){
                Article m2 = sortedList.get(i + 1);
                VisualObject vo2 = dnaVisuals.get(m2.getId());
                if (vo2.circle.isVisible()){
                    vo1.greenLine.setStartX(vo1.circle.getTranslateX());
                    vo1.greenLine.setStartY(vo1.circle.getTranslateY());
                    vo1.greenLine.setEndX(vo2.circle.getTranslateX());
                    vo1.greenLine.setEndY(vo2.circle.getTranslateY());
                    vo1.greenLine.setStrokeWidth(5 * vo1.circle.getScaleX());
                    vo1.greenLine.setOpacity(vo1.circle.getOpacity() * 0.7);
                }else{
                    vo1.greenLine.setVisible(false); 
                }
            }
        }
    }

    // Standard Mode
    // Centers the selected article and draws related articles around it.
    private void drawStandardGraph(Article center){
        
        clearScreen(); 
        graphPane.setStyle("-fx-background-color: #0f172a;"); 
        // Reset zoom values.
        drawingGroup.setTranslateX(0); 
        drawingGroup.setTranslateY(0); 
        currentArticle = center; // Update active article.

        double centerX = graphPane.getWidth() / 2;
        double centerY = graphPane.getHeight() / 2;
        if (centerX == 0){
            centerX = 600; centerY = 400; 
        }

        addCoordinate(center, centerX, centerY);
        
        StackPane centerNode = createNode(centerX, centerY, 35, Color.WHITE, center, true);
        centerNode.setEffect(new Glow(1.0)); 
        drawingGroup.getChildren().add(centerNode);

        List<Article> allRelated = new ArrayList<>();
        
        // Outgoing links - References.
        for (String refId : center.getReferences()){
            Article ref = graphManager.findArticle(refId);
            // Add only those in dataset and not already in list.
            if (ref != null && !allRelated.contains(ref)) 
                allRelated.add(ref);
        }
        // Incoming links - Citations
        for (String citingId : center.getCitingPaperIds()){
            Article citing = graphManager.findArticle(citingId);
            if (citing != null && !allRelated.contains(citing)) 
                allRelated.add(citing);
        }
        // H-Core and Standard Mode filtering logic
        if (hCoreModeButton.isSelected()){
            // Keep those in H-Core list, remove others.
            List<Article> hCore = graphManager.getHCoreList(center);
            List<String> hIds = new ArrayList<>();
            for(Article h : hCore) hIds.add(h.getId());
            allRelated.removeIf(m -> !hIds.contains(m.getId()));
        }else{
            // Add previous/next paper for ID-ordered green chain.
            if (center.previous != null && !allRelated.contains(center.previous)) allRelated.add(center.previous);
            if (center.next != null && !allRelated.contains(center.next)) allRelated.add(center.next);
        }
        
        int n = allRelated.size();
        // Expand circle as neighbor count increases.
        double startRadius = 200 + (n * 5); 
        
        for (Article m : allRelated) {
            // Select random angle and distance.
            double angle = Math.random() * 360;
            double rad = Math.toRadians(angle);
            double dist = 150 + Math.random() * startRadius;
            // Convert to Cartesian coordinates.
            double x = centerX + Math.cos(rad) * dist;
            double y = centerY + Math.sin(rad) * dist;
            addCoordinate(m, x, y);
        }
        
        // Prevent node overlapping.
        applyPhysics(1000); 
        
        // Green chain.
        if (!hCoreModeButton.isSelected()){
            if (center.previous != null && articlesOnScreen.containsKey(center.previous.getId()))
                drawGreenArrow(coordinateMap.get(center.previous.getId()), new double[]{centerX,centerY});
            if (center.next != null && articlesOnScreen.containsKey(center.next.getId()))
                drawGreenArrow(new double[]{centerX,centerY}, coordinateMap.get(center.next.getId()));
        }
            
        // Draw neighbor nodes and arrows.
        for (Article m : allRelated){
            double[] pos = coordinateMap.get(m.getId());
            // Decide relationship direction.
            boolean outgoing = center.getReferences().contains(m.getId());
            boolean incoming = center.getCitingPaperIds().contains(m.getId());
            
            // Draw arrows.
            if (outgoing) drawArrow(centerX, centerY, pos[0], pos[1], 20, true);
            if (incoming) drawArrow(pos[0], pos[1], centerX, centerY, 30, true);
            
            // Draw node.
            Color color = getNodeColor(m, false);
            StackPane node = createNode(pos[0], pos[1], 20, color, m, false);
            
            // Make clicked node the center.
            node.setOnMouseClicked(e -> {
                if (currentArticle != null && !currentArticle.getId().equals(m.getId())){
                    historyStack.push(currentArticle); // Send current to history.
                    backButton.setDisable(false);
                }
                drawStandardGraph(m); // Center clicked one and redraw.
                showDetails(m);
            });
            drawingGroup.getChildren().add(node);
        }
    }
    
    // Gets color based on article importance or analysis status.
    private Color getNodeColor(Article m, boolean isCenter){
        // Clicked article should be white.
        if (isCenter) return Color.WHITE;
        if (isKCoreModeActive) {
            return kCoreList.contains(m) ? Color.web("#10b981") : Color.web("#ef4444"); 
        }
        // Color scale based on received citations.
        int citations = m.getCitationCount();
        if (citations > 20) return Color.web("#f43f5e"); 
        if (citations > 10) return Color.web("#f59e0b"); 
        if (citations > 5) return Color.web("#8b5cf6");  
        return Color.web("#3b82f6");                
    }

    // Arrows following each other by ID order.
    private void drawGreenArrow(double[] p1, double[] p2){
        if(p1==null || p2==null) return;
        Line line = new Line(p1[0], p1[1], p2[0], p2[1]);
        line.setStroke(Color.web("#22c55e")); 
        line.setStrokeWidth(2);
        // Dashed line effect.
        line.getStrokeDashArray().addAll(10d, 5d);
        drawingGroup.getChildren().add(line);
    }

    private void expandHCoreGraph(Article focus){
       drawStandardGraph(focus);
    }

    // Clear screen without drawing new.
    private void clearScreen(){
        drawingGroup.getChildren().clear();
        coordinateMap.clear();
        articlesOnScreen.clear();
    }
    
    private void addCoordinate(Article m, double x, double y){
        coordinateMap.put(m.getId(), new double[]{x, y});
        articlesOnScreen.put(m.getId(), m);
    }
    
    // Prevents randomly distributed nodes from overlapping on screen.
     private void applyPhysics(int iterations){
        List<String> keys = new ArrayList<>(coordinateMap.keySet());
        double minDistance = 60.0; 
        
        for (int i = 0; i < iterations; i++){ 
            // Compare every node with every other node.
            for (int a = 0; a < keys.size(); a++){
                for (int b = a + 1; b < keys.size(); b++){
                    String k1 = keys.get(a);
                    String k2 = keys.get(b);
                    
                    double[] p1 = coordinateMap.get(k1);
                    double[] p2 = coordinateMap.get(k2);
                    // Distance vector between two points
                    double dx = p1[0] - p2[0];
                    double dy = p1[1] - p2[1];
                    double distance = Math.sqrt(dx*dx + dy*dy);
                    
                    if (distance < minDistance && distance > 0.1){
                        double overlap = minDistance - distance; 
                        // Push both sides in opposite directions by half the overlap.
                        double force = overlap / 2.0; 
                        double nx = dx / distance;
                        double ny = dy / distance;
                        // Center article should remain fixed.
                        if (!k1.equals(currentArticle.getId())){
                            p1[0] += nx * force;
                            p1[1] += ny * force;
                        }
                        if (!k2.equals(currentArticle.getId())){
                            p2[0] -= nx * force;
                            p2[1] -= ny * force;
                        }
                    }
                }
            }
        }
    }

    private void updateColors(){
        if (!isDnaModeActive) {
            if (drawingGroup.getChildren().size() > 500) 
                drawGridMode();
            else drawScene(); 
            if(currentArticle != null) 
                drawStandardGraph(currentArticle);
        }
    }
    
    private void applyPhysics(){
        applyPhysics(50); 
    }
    private void drawScene(){
    }

    // Draws directional arrow between two points.
    private void drawArrow(double x1, double y1, double x2, double y2, double targetRadius, boolean active){
        Line line = new Line(x1, y1, x2, y2);
        // transparent white
        line.setStroke(Color.rgb(255, 255, 255, 0.2)); 
        line.setStrokeWidth(1.0);
        
        // Find angle between two points.
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double offset = targetRadius + 5; 
        // Calculate new end point.
        double targetX = x2 - Math.cos(angle) * offset;
        double targetY = y2 - Math.sin(angle) * offset;
        line.setEndX(targetX); 
        line.setEndY(targetY);

        // Arrow head (triangle)
        Polygon arrowHead = new Polygon();
        arrowHead.getPoints().addAll(new Double[]{ 0.0, 0.0, -6.0, -3.0, -6.0, 3.0 });
        arrowHead.setFill(Color.rgb(255, 255, 255, 0.5));

        arrowHead.setRotate(Math.toDegrees(angle));
        arrowHead.setLayoutX(targetX); 
        arrowHead.setLayoutY(targetY);
        drawingGroup.getChildren().addAll(line, arrowHead);
    }
    
    // Calculates radius size based on article size.
    private double calculateRadius(Article m){
        if (m == null) return 20;
        return 20 + Math.min(15, m.getCitationCount() * 0.5);
    }

    // Produces a visual object for each article shown on screen.
    private StackPane createNode(double x, double y, double r, Color color, Article m, boolean isCenter){
        StackPane body = new StackPane();
        // Dynamic size based on citation count.
        double realR = calculateRadius(m);
        if(isCenter) realR = 35; 
        
        body.setLayoutX(x - realR); 
        body.setLayoutY(y - realR);

        // K-Core visual filtering
        double opacity = 1.0;
        if (isKCoreModeActive && !kCoreList.contains(m)) opacity = 0.1; 

        // 3D effect
        Circle c = new Circle(realR);
        
        RadialGradient gradient = new RadialGradient(
            0, 0, 0.3, 0.3, 1.0, true, CycleMethod.NO_CYCLE,
            new Stop(0, color.brighter().brighter()), 
            new Stop(1, color)
        );
        c.setFill(gradient);
        c.setStroke(Color.WHITE); 
        c.setStrokeWidth(1.5);
        // Shadow effect
        c.setEffect(new DropShadow(10, color.darker())); 
        c.setOpacity(opacity);

        // Text content
        VBox vb = new VBox(-1); 
        vb.setAlignment(javafx.geometry.Pos.CENTER);
        
        Color textColor = isCenter ? Color.BLACK : Color.WHITE;
        FontWeight weight = isCenter ? FontWeight.EXTRA_BOLD : FontWeight.BOLD;
        // ID
        Text t1 = new Text(getCleanId(m.getId())); 
        t1.setFont(Font.font("Arial", weight, realR * 0.3)); 
        t1.setFill(textColor);
        
        // Author initials
        Text t2 = new Text(getAuthorInitials(m)); 
        t2.setFont(Font.font("Arial", weight, realR * 0.4)); 
        t2.setFill(textColor);
        
        // Citation count
        Text t3 = new Text("(" + m.getCitationCount() + ")");
        t3.setFont(Font.font("Arial", realR * 0.3)); t3.setFill(isCenter ? Color.GRAY : Color.LIGHTGRAY);
        
        t1.setOpacity(opacity); 
        t2.setOpacity(opacity); 
        t3.setOpacity(opacity);
        
        vb.getChildren().addAll(t1, t2, t3);
        body.getChildren().addAll(c, vb); 

        // Box appearing on mouse hover.
        String author = m.getAuthors().isEmpty() ? "-" : m.getAuthors().get(0);
        Double betweenness = betweennessScores.getOrDefault(m.getId(), 0.0);
        String bcText = String.format("%.4f", betweenness);
        String tooltipText = "ID: " + m.getId() + "\nAuthor: " + author + "\nTitle: " + m.getTitle() + "\nCitations: " + m.getCitationCount() + "\nBetweenness: " + bcText;
        
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setStyle("-fx-font-size: 13px;");
        tooltip.setShowDelay(javafx.util.Duration.millis(100)); 
        Tooltip.install(body, tooltip);
        
        return body;
    }

    private String getCleanId(String id){
        return id == null ? "" : id.replaceAll("[^0-9]", "");
    }

    private String getAuthorInitials(Article m){
        if (m == null || m.getAuthors().isEmpty()) 
            return "";
        String[] s = m.getAuthors().get(0).split(" ");
        StringBuilder sb = new StringBuilder();
        
        // Take first letter of each word.
        for (String p : s) 
            if (!p.isEmpty() && Character.isLetter(p.charAt(0))) 
                sb.append(p.charAt(0));
        return sb.length() > 3 ? sb.substring(0, 3).toUpperCase() : sb.toString().toUpperCase();
    }

    // Write Info Panel screen.
    private void showDetails(Article m){
        int hIndex = graphManager.calculateHIndex(m);
        double hMedian = graphManager.calculateHMedian(m);
        int hCoreSize = graphManager.getHCoreList(m).size();
        Double betweenness = betweennessScores.getOrDefault(m.getId(), 0.0);

        String details = "ID: " + m.getId() + "\n\nTITLE:\n" + m.getTitle() + 
                "\n\nYEAR: " + m.getYear() + 
                "\n\nREFS GIVEN (Outgoing): " + m.getReferences().size() + 
                "\nCITATIONS RECEIVED (Incoming): " + m.getCitationCount() + 
                "\n\n------------------------------\nH-INDEX: " + hIndex + 
                "\nH-CORE SIZE: " + hCoreSize + "\nH-MEDIAN: " + hMedian + 
                "\nBETWEENNESS: " + String.format("%.6f", betweenness);
        infoPane.setText(details);
    }
    
    private static class SpacePoint{
        double x, y, z;
        SpacePoint(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z; 
        }
    }
    
    private static class VisualObject{
        Article article;
        Circle circle;
        Line greenLine;
        Text label;
        VisualObject(Article a, Circle c, Line l, Text t){ 
            this.article = a; this.circle = c; this.greenLine = l; this.label = t; 
        }
    }
    
    // Grid mode
    private void drawGridMode(){
        clearScreen();
        graphPane.setStyle("-fx-background-color: #0f172a;"); 
        
        List<Article> sortedList = new ArrayList<>(allArticles);
        sortedList.sort(Comparator.comparing(Article::getId));
        
        int colCount = 25; 
        double startX = 60; 
        double startY = 60; 
        double gapX = 70; 
        double gapY = 90; 
        
        for (int i = 0; i < sortedList.size(); i++){
            // Matrix calculation
            Article m = sortedList.get(i);
            int row = i / colCount; int col = i % colCount;
            double x = startX + col * gapX; 
            double y = startY + row * gapY;
            addCoordinate(m, x, y);
            Color color = getNodeColor(m, false);
            StackPane node = createNode(x, y, 12, color, m, false);
            
            node.setOnMouseClicked(e -> {
                resetMode(false); 
                if(currentArticle != null) historyStack.push(currentArticle); 
                backButton.setDisable(false); 
                drawStandardGraph(m); 
                showDetails(m); 
                resetZoom(); 
            });
            
            // Link only elements in the same row.
            if (i < sortedList.size() - 1){
                int nextI = i + 1; 
                int nextRow = nextI / colCount; 
                int nextCol = nextI % colCount;
                double nextX = startX + nextCol * gapX; 
                double nextY = startY + nextRow * gapY;
                
                // Draw line if moving to next column in same row.
                if (nextRow == row){
                    Line line = new Line(x + 12, y, nextX - 12, nextY);
                    line.setStroke(Color.web("#22c55e")); line.setStrokeWidth(2); line.setOpacity(0.5);
                    drawingGroup.getChildren().add(line);
                }
            }
            drawingGroup.getChildren().add(node);
        }
    }
}