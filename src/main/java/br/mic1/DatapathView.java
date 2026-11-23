package br.mic1;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;

/**
 * Desenho animado do Datapath MAC-1 (3ª Edição) por subciclo.
 * Inclui os blocos de controle (MPC e MIR), linhas alinhadas e destaque por subciclo.
 */
public class DatapathView extends Pane {

    private final CpuMic1 cpu;
    
    // Labels de cabeçalho
    private Label lblSubcycleInfo;
    private Label lblMicroinstruction;
    private Label lblInstruction;

    // Blocos Gráficos
    private Rectangle boxMpc, boxMir, boxLatchA, boxLatchB, boxAmux, boxAlu, boxShifter, boxMar, boxMbr, boxBusC;
    private Label lblMpcVal, lblMirVal, lblLatchAVal, lblLatchBVal, lblMarVal, lblMbrVal, lblBusCVal;

    // Conexões (Linhas e Polylines)
    private Line lineLatchA_Amux, lineAmux_Alu, lineLatchB_Alu, lineAlu_Shifter, lineShifter_BusC, lineBusC_Mbr;
    private Polyline polyMbr_Amux, polyLatchB_Mar, polyMpc_Mir;

    public DatapathView(CpuMic1 cpu) {
        this.cpu = cpu;
        this.setStyle("-fx-background-color: #161616; -fx-border-color: #444444; -fx-border-width: 1;");
        this.setPrefSize(620, 500);

        // Header Informativo
        lblSubcycleInfo = createHeaderLabel("Subciclo Ativo: Aguardando (Pronto para iniciar)", 15, 12, "#00ff66", 13, true);
        lblMicroinstruction = createHeaderLabel("MIR: -", 15, 32, "#00e5ff", 11, false);
        lblInstruction = createHeaderLabel("Instrução Atual: -", 15, 50, "#ffffff", 11, false);

        this.getChildren().addAll(lblSubcycleInfo, lblMicroinstruction, lblInstruction);

        // Desenhar Conexões (Linhas e Polylines)
        buildBusLines();

        // Desenhar Blocos e Valores
        buildDatapathNodes();

        updateView();
    }

    private void buildDatapathNodes() {
        // --- Unidade de Controle (Topo Direito: Subciclo 1) ---
        boxMpc = createBlock("MPC (ROM)", 380, 80, 100, 40);
        lblMpcVal = createSubLabel("0x00", 380, 100, 100, "#00e5ff");

        boxMir = createBlock("MIR (Controle)", 380, 140, 100, 40);
        lblMirVal = createSubLabel("32-bit", 380, 160, 100, "#00e5ff");

        // --- Datapath de Dados ---
        // Coluna 1: Latch A e AMUX
        boxLatchA = createBlock("Latch A", 70, 95, 110, 42);
        lblLatchAVal = createSubLabel("0x0000", 70, 117, 110, "#00ff66");

        boxAmux = createBlock("AMUX", 70, 165, 110, 36);

        // Coluna 2: Latch B, ULA e Shifter
        boxLatchB = createBlock("Latch B", 230, 95, 110, 42);
        lblLatchBVal = createSubLabel("0x0000", 230, 117, 110, "#00ff66");

        boxAlu = createBlock("ULA (16-bit)", 200, 230, 140, 42);
        boxShifter = createBlock("Shifter 1-bit", 200, 300, 140, 42);

        // Coluna 3: MAR e MBR (Subciclo 3 e 4)
        boxMar = createBlock("MAR", 380, 230, 100, 42);
        lblMarVal = createSubLabel("0x000", 380, 252, 100, "#00ff66");

        boxMbr = createBlock("MBR", 380, 300, 100, 42);
        lblMbrVal = createSubLabel("0x0000", 380, 322, 100, "#00ff66");

        // Barramento C (Base)
        boxBusC = createBlock("", 50, 385, 430, 28);
        lblBusCVal = createSubLabel("BARRAMENTO C  [0x0000]", 50, 391, 430, "#ffffff");
        lblBusCVal.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-font-weight: bold; -fx-alignment: center;");
    }

    private void buildBusLines() {
        // Conexão Controle: MPC -> MIR
        polyMpc_Mir = createPolyline(430, 120, 430, 140);

        // 1. Latch A -> AMUX
        lineLatchA_Amux = createLine(125, 137, 125, 165);
        
        // 2. MBR -> AMUX (Curva em Polyline da MBR subindo até a lateral da AMUX)
        polyMbr_Amux = createPolyline(380, 321, 360, 321, 360, 183, 180, 183);
        
        // 3. AMUX -> ULA
        lineAmux_Alu = createLine(125, 201, 200, 251);
        
        // 4. Latch B -> ULA
        lineLatchB_Alu = createLine(285, 137, 285, 230);
        
        // 5. Latch B -> MAR
        polyLatchB_Mar = createPolyline(285, 137, 285, 150, 430, 150, 430, 230);
        
        // 6. ULA -> Shifter
        lineAlu_Shifter = createLine(270, 272, 270, 300);
        
        // 7. Shifter -> Barramento C
        lineShifter_BusC = createLine(270, 342, 270, 385);
        
        // 8. Barramento C -> MBR
        lineBusC_Mbr = createLine(430, 385, 430, 342);

        this.getChildren().addAll(
            polyMpc_Mir, lineLatchA_Amux, polyMbr_Amux, lineAmux_Alu, 
            lineLatchB_Alu, polyLatchB_Mar, lineAlu_Shifter, lineShifter_BusC, lineBusC_Mbr
        );
    }

    public void updateView() {
        int subcycle = cpu.getLastSubcycle();

        lblSubcycleInfo.setText("Subciclo Ativo: " + (subcycle == 0 ? "Aguardando" : subcycle + "/4") + " (" + cpu.getSubcycleDescription() + ")");
        lblMicroinstruction.setText("MIR: " + cpu.getCurrentMicroinstructionText());
        lblInstruction.setText("Instrução Atual: " + cpu.getCurrentInstructionText());

        // Atualizar valores exatos da CPU
        lblMpcVal.setText(String.format("0x%02X", cpu.getMpc()));
        lblMirVal.setText(cpu.isMemReadActive() ? "MEM_RD" : (cpu.isMemWriteActive() ? "MEM_WR" : "EXEC"));
        lblLatchAVal.setText(String.format("0x%04X", cpu.getLatchA()));
        lblLatchBVal.setText(String.format("0x%04X", cpu.getLatchB()));
        lblMarVal.setText(String.format("0x%03X", cpu.getMar()));
        lblMbrVal.setText(String.format("0x%04X", cpu.getMbr()));
        lblBusCVal.setText("BARRAMENTO C  [" + String.format("0x%04X", cpu.getBusC()) + "]");

        // Destaque dos Blocos por Subciclo (1 a 4)
        highlightBlock(boxMpc, subcycle == 1);
        highlightBlock(boxMir, subcycle == 1);
        highlightBlock(boxLatchA, subcycle == 2);
        highlightBlock(boxLatchB, subcycle == 2);
        highlightBlock(boxAmux, subcycle == 3 && cpu.isAmuxMbrSelected());
        highlightBlock(boxAlu, subcycle == 3);
        highlightBlock(boxShifter, subcycle == 3);
        highlightBlock(boxMar, subcycle == 3 && cpu.isMarActive());
        highlightBlock(boxMbr, subcycle == 4 && cpu.isMbrWriteActive());
        highlightBlock(boxBusC, subcycle == 4 && cpu.isWriteCActive());

        // Destaque de Linhas de Barramento
        highlightPolyline(polyMpc_Mir, subcycle == 1);
        highlightLine(lineLatchA_Amux, subcycle == 2 || (subcycle == 3 && !cpu.isAmuxMbrSelected()));
        highlightPolyline(polyMbr_Amux, subcycle == 3 && cpu.isAmuxMbrSelected());
        highlightLine(lineAmux_Alu, subcycle == 3);
        highlightLine(lineLatchB_Alu, subcycle == 3);
        highlightPolyline(polyLatchB_Mar, subcycle == 3 && cpu.isMarActive());
        highlightLine(lineAlu_Shifter, subcycle == 3);
        highlightLine(lineShifter_BusC, subcycle == 3 || subcycle == 4);
        highlightLine(lineBusC_Mbr, subcycle == 4 && cpu.isMbrWriteActive());
    }

    private Rectangle createBlock(String title, double x, double y, double w, double h) {
        Rectangle r = new Rectangle(x, y, w, h);
        r.setArcWidth(8);
        r.setArcHeight(8);
        r.setStroke(Color.web("#555555"));
        r.setFill(Color.web("#222222"));

        if (!title.isEmpty()) {
            Label l = new Label(title);
            l.setLayoutX(x);
            l.setLayoutY(y + 4);
            l.setPrefWidth(w);
            l.setAlignment(Pos.CENTER);
            l.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-font-weight: bold; -fx-font-family: 'Consolas', monospace;");
            this.getChildren().addAll(r, l);
        } else {
            this.getChildren().add(r);
        }
        return r;
    }

    private Label createSubLabel(String text, double x, double y, double w, String colorHex) {
        Label l = new Label(text);
        l.setLayoutX(x);
        l.setLayoutY(y);
        l.setPrefWidth(w);
        l.setAlignment(Pos.CENTER);
        l.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-size: 10px; -fx-font-family: 'Consolas', monospace;");
        this.getChildren().add(l);
        return l;
    }

    private Label createHeaderLabel(String text, double x, double y, String colorHex, double size, boolean bold) {
        Label l = new Label(text);
        l.setLayoutX(x);
        l.setLayoutY(y);
        l.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-size: " + size + "px; " + (bold ? "-fx-font-weight: bold; " : "") + "-fx-font-family: 'Consolas', monospace;");
        return l;
    }

    private Line createLine(double startX, double startY, double endX, double endY) {
        Line line = new Line(startX, startY, endX, endY);
        line.setStroke(Color.web("#3d3d3d"));
        line.setStrokeWidth(2);
        return line;
    }

    private Polyline createPolyline(double... points) {
        Polyline poly = new Polyline(points);
        poly.setStroke(Color.web("#3d3d3d"));
        poly.setStrokeWidth(2);
        return poly;
    }

    private void highlightBlock(Rectangle r, boolean active) {
        if (active) {
            r.setStroke(Color.web("#00ff66"));
            r.setFill(Color.web("#093318"));
        } else {
            r.setStroke(Color.web("#555555"));
            r.setFill(Color.web("#222222"));
        }
    }

    private void highlightLine(Line line, boolean active) {
        if (active) {
            line.setStroke(Color.web("#00ff66"));
            line.setStrokeWidth(3);
        } else {
            line.setStroke(Color.web("#3d3d3d"));
            line.setStrokeWidth(2);
        }
    }

    private void highlightPolyline(Polyline poly, boolean active) {
        if (active) {
            poly.setStroke(Color.web("#00ff66"));
            poly.setStrokeWidth(3);
        } else {
            poly.setStroke(Color.web("#3d3d3d"));
            poly.setStrokeWidth(2);
        }
    }
}