package br.mic1;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.layout.Priority;

public class Main extends Application {

    private CpuMic1 cpu;
    private DatapathView datapathView;

    // Componentes UI da Esquerda (Editor + Botões)
    private TextArea txtCode;
    private Button btnStepSubcycle, btnStepCycle, btnStepInstruction, btnRunPause, btnReset;
    private Timeline runTimeline;
    private boolean isRunning = false;

    // Componentes UI da Direita (Registradores, Flags, Cache)
    private Label[] lblRegValues = new Label[16];
    private Label lblFlagN, lblFlagZ;
    private TextArea txtCacheSummary;
    private Label lblCacheStatus;

    // Tabela de Memória (Base)
    private TableView<MemoryRow> tblMemory;

    @Override
    public void start(Stage primaryStage) {
        this.cpu = new CpuMic1();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #202020; -fx-font-family: 'Consolas', monospace;");

        // Construção dos painéis
        root.setLeft(createLeftPanel());
        datapathView = new DatapathView(cpu);
        root.setCenter(datapathView);
        root.setRight(createRightPanel());
        root.setBottom(createBottomPanel());
        
     // Timeline para execução automática com paragem no fim do programa
        runTimeline = new Timeline(new KeyFrame(Duration.millis(300), e -> {
            // Verifica se a instrução atual é o fim do código carregado
            if (cpu.getProgramSize() > 0 && cpu.getRegister(0) >= cpu.getProgramSize()) { // Reg[0] é o PC
                toggleRun(); // Pausa automaticamente a animação
            } else {
                cpu.stepSubcycle();
                updateAllViews();
            }
        }));
        runTimeline.setCycleCount(Timeline.INDEFINITE);

        Scene scene = new Scene(root, 1340, 800);
        primaryStage.setTitle("Simulador Gráfico MIC-1 / MAC-1 (3ª Edição Tanenbaum)");
        primaryStage.setScene(scene);
        primaryStage.show();

        updateAllViews();
    }

    private VBox createLeftPanel() {
        VBox left = new VBox(10);
        left.setPadding(new Insets(10));
        left.setPrefWidth(320);
        left.setStyle("-fx-background-color: #2b2b2b; -fx-border-color: #444444;");

        Label lblTitle = new Label("Editor Assembly MAC-1");
        lblTitle.setStyle("-fx-text-fill: #00ff66; -fx-font-size: 14px; -fx-font-weight: bold;");

        txtCode = new TextArea();
        txtCode.setPromptText("// Digite o código MAC-1 aqui...\nLOCO 5\nSTOD 100\nLODD 100\nADDD 100");
        txtCode.setPrefHeight(380);
        txtCode.setStyle("-fx-control-inner-background: #181818; -fx-text-fill: #00ff66; -fx-font-family: 'Consolas', monospace;");

        Button btnLoad = createStyledButton("1. Carregar em Memória", "#0088cc");
        btnLoad.setOnAction(e -> {
            cpu.loadProgram(txtCode.getText());
            updateAllViews();
        });

        btnStepSubcycle = createStyledButton("Subciclo (1/4)", "#444444");
        btnStepSubcycle.setOnAction(e -> { cpu.stepSubcycle(); updateAllViews(); });
        // Garante expansão proporcional para preencher o painel
        HBox.setHgrow(btnStepSubcycle, Priority.ALWAYS);
        btnStepSubcycle.setMaxWidth(Double.MAX_VALUE);

        btnStepCycle = createStyledButton("Ciclo (4/4)", "#444444");
        btnStepCycle.setOnAction(e -> { cpu.step(); updateAllViews(); });
        // Garante expansão proporcional para preencher o painel
        HBox.setHgrow(btnStepCycle, Priority.ALWAYS);
        btnStepCycle.setMaxWidth(Double.MAX_VALUE);

        HBox stepGroup = new HBox(8, btnStepSubcycle, btnStepCycle);
        stepGroup.setMaxWidth(Double.MAX_VALUE);

        btnStepInstruction = createStyledButton("Próx. Instrução", "#444444");
        btnStepInstruction.setOnAction(e -> { cpu.stepInstruction(); updateAllViews(); });

        btnRunPause = createStyledButton("Executar / Pausar", "#28a745");
        btnRunPause.setOnAction(e -> toggleRun());

        btnReset = createStyledButton("Reset CPU", "#dc3545");
        btnReset.setOnAction(e -> {
            if (isRunning) toggleRun();
            cpu.reset();
            updateAllViews();
        });

        left.getChildren().addAll(lblTitle, txtCode, btnLoad, stepGroup, btnStepInstruction, btnRunPause, btnReset);
        return left;
    }

    private VBox createRightPanel() {
        VBox right = new VBox(10);
        right.setPadding(new Insets(10));
        right.setPrefWidth(340);
        right.setStyle("-fx-background-color: #2b2b2b; -fx-border-color: #444444;");

        // Painel de Registradores
        Label lblRegs = new Label("Registradores (16-bit)");
        lblRegs.setStyle("-fx-text-fill: #00ff66; -fx-font-weight: bold;");

        GridPane gridRegs = new GridPane();
        gridRegs.setHgap(8);
        gridRegs.setVgap(4);

        for (int i = 0; i < 16; i++) {
            Label name = new Label(cpu.getRegisterName(i) + ":");
            name.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 11px;");
            
            Label val = new Label("0x0000");
            val.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-font-weight: bold;");
            lblRegValues[i] = val;

            int row = i % 8;
            int col = (i / 8) * 2;
            gridRegs.add(name, col, row);
            gridRegs.add(val, col + 1, row);
        }

        // Flags N e Z
        HBox flagsBox = new HBox(15);
        flagsBox.setAlignment(Pos.CENTER_LEFT);
        lblFlagN = new Label("N: 0");
        lblFlagZ = new Label("Z: 0");
        lblFlagN.setStyle("-fx-text-fill: #888888; -fx-font-weight: bold;");
        lblFlagZ.setStyle("-fx-text-fill: #888888; -fx-font-weight: bold;");
        Label lblFlagsTitle = new Label("Flags:");
        lblFlagsTitle.setStyle("-fx-text-fill: #00ff66; -fx-font-weight: bold;");

        flagsBox.getChildren().addAll(lblFlagsTitle, lblFlagN, lblFlagZ);

        // Painel Cache L1
        Label lblCacheTitle = new Label("Painel Cache L1");
        lblCacheTitle.setStyle("-fx-text-fill: #00ff66; -fx-font-weight: bold;");

        lblCacheStatus = new Label("Último Acesso: -");
        lblCacheStatus.setStyle("-fx-text-fill: #ffffff;");

        txtCacheSummary = new TextArea();
        txtCacheSummary.setEditable(false);
        txtCacheSummary.setPrefHeight(120);
        txtCacheSummary.setStyle("-fx-control-inner-background: #181818; -fx-text-fill: #00ff66;");

        right.getChildren().addAll(lblRegs, gridRegs, new Separator(), flagsBox, new Separator(), lblCacheTitle, lblCacheStatus, txtCacheSummary);
        return right;
    }

    private VBox createBottomPanel() {
        VBox bottom = new VBox(5);
        bottom.setPadding(new Insets(8));
        bottom.setPrefHeight(200);
        bottom.setStyle("-fx-background-color: #232323; -fx-border-color: #444444;");

        Label lblMemTitle = new Label("Memória Principal (RAM) - 4096 Palavras de 16 bits");
        lblMemTitle.setStyle("-fx-text-fill: #00ff66; -fx-font-size: 12px; -fx-font-weight: bold;");

        tblMemory = new TableView<>();
        tblMemory.setStyle("-fx-background-color: #181818;");

        TableColumn<MemoryRow, String> colAddr = new TableColumn<>("Endereço");
        colAddr.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAddressHex()));

        TableColumn<MemoryRow, String> colHex = new TableColumn<>("Hex");
        colHex.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getValHex()));

        TableColumn<MemoryRow, String> colDec = new TableColumn<>("Dec");
        colDec.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getValDec()));

        TableColumn<MemoryRow, String> colDisasm = new TableColumn<>("Instrução (Desmontada)");
        colDisasm.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDisasm()));
        colDisasm.setPrefWidth(250);

        tblMemory.getColumns().addAll(colAddr, colHex, colDec, colDisasm);
        tblMemory.setPrefHeight(160);

        bottom.getChildren().addAll(lblMemTitle, tblMemory);
        return bottom;
    }

    private Button createStyledButton(String text, String colorHex) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        return b;
    }

    private void toggleRun() {
        if (isRunning) {
            runTimeline.pause();
            btnRunPause.setText("Executar");
            btnRunPause.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        } else {
            runTimeline.play();
            btnRunPause.setText("Pausar");
            btnRunPause.setStyle("-fx-background-color: #ffc107; -fx-text-fill: black;");
        }
        isRunning = !isRunning;
    }

    private void updateAllViews() {
        // Atualiza Registradores
        for (int i = 0; i < 16; i++) {
            lblRegValues[i].setText(String.format("0x%04X", cpu.getRegister(i)));
        }

        // Atualiza Flags
        lblFlagN.setText("N: " + (cpu.isFlagN() ? "1" : "0"));
        lblFlagN.setStyle("-fx-text-fill: " + (cpu.isFlagN() ? "#ff4444" : "#888888") + "; -fx-font-weight: bold;");
        
        lblFlagZ.setText("Z: " + (cpu.isFlagZ() ? "1" : "0"));
        lblFlagZ.setStyle("-fx-text-fill: " + (cpu.isFlagZ() ? "#ff4444" : "#888888") + "; -fx-font-weight: bold;");

        // Atualiza Cache
        if (cpu.getCache() != null) {
            txtCacheSummary.setText(cpu.getCache().getSummaryText());
            String status = cpu.getCache().getLastAccessStatus();
            lblCacheStatus.setText("Último Acesso: " + status);
            if (status.contains("HIT")) {
                lblCacheStatus.setStyle("-fx-text-fill: #00ff66; -fx-font-weight: bold;");
            } else if (status.contains("MISS")) {
                lblCacheStatus.setStyle("-fx-text-fill: #ff4444; -fx-font-weight: bold;");
            } else {
                lblCacheStatus.setStyle("-fx-text-fill: #ffffff;");
            }
        }

        // Atualiza Tabela de Memória - Renderiza TODAS as 4096 palavras da RAM (0x000 a 0xFFF)
        ObservableList<MemoryRow> rows = FXCollections.observableArrayList();
        int totalMemorySize = cpu.getMemory().getSize(); // 4096 posições

        for (int addr = 0; addr < totalMemorySize; addr++) {
            rows.add(new MemoryRow(
                String.format("0x%03X (%d)", addr, addr),
                cpu.getMemory().readHex(addr),
                String.valueOf(cpu.getMemory().readSigned(addr)),
                Assembler.disassembleWord(cpu.getMemory().read(addr))
            ));
        }
        tblMemory.setItems(rows);
        // Atualiza o desenho do Datapath
        datapathView.updateView();
    }

    // Classe auxiliar interna para preencher a TableView da Memória
    public static class MemoryRow {
        private final String addressHex, valHex, valDec, disasm;
        public MemoryRow(String addressHex, String valHex, String valDec, String disasm) {
            this.addressHex = addressHex;
            this.valHex = valHex;
            this.valDec = valDec;
            this.disasm = disasm;
        }
        public String getAddressHex() { return addressHex; }
        public String getValHex() { return valHex; }
        public String getValDec() { return valDec; }
        public String getDisasm() { return disasm; }
    }
}