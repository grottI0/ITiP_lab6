import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;

public class FractalExplorer {
    private int display;
    private JImageDisplay image;
    private FractalGenerator fractalGenerator;
    private Rectangle2D.Double planeRange;
    private int rowsRemaining;

    private JComboBox<FractalGenerator> box;
    private JButton resetButton;
    private JButton saveButton;

    public static void main(String[] args) {
        FractalExplorer fractalExplorer = new FractalExplorer(700);
        fractalExplorer.createAndShowGUI();
        fractalExplorer.drawFractal();
    }

    // конструктор
    public FractalExplorer(int display) {
        this.display = display;
        this.planeRange = new Rectangle2D.Double(0, 0, 0, 0);
        this.fractalGenerator = new Mandelbrot();
        fractalGenerator.getInitialRange(planeRange);
    }

    // создает графический интерфейс
    public void createAndShowGUI() {
        image = new JImageDisplay(display, display);

        box = new JComboBox<>();
        box.addItem(new Mandelbrot());
        box.addItem(new Tricorn());
        box.addItem(new BurningShip());

        JLabel label = new JLabel("Fractals: ");

        resetButton = new JButton("Reset");
        saveButton = new JButton("Save");
        JFrame frame = new JFrame("Fractal generator");
        frame.setLayout(new BorderLayout());

        JPanel upper = new JPanel();
        upper.add(label, BorderLayout.CENTER);
        upper.add(box, BorderLayout.CENTER);

        JPanel lower = new JPanel();
        lower.add(resetButton, BorderLayout.CENTER);
        lower.add(saveButton, BorderLayout.CENTER);

        image.addMouseListener(new MouseListener());
        resetButton.addActionListener(new ResetListener());
        saveButton.addActionListener(new SaveListener());
        box.addActionListener(new BoxListener());

        frame.add(image, BorderLayout.CENTER);
        frame.add(lower, BorderLayout.SOUTH);
        frame.add(upper, BorderLayout.NORTH);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        frame.setResizable(false);
    }

    // увключает/выключает элементы интерфейса
    public void enableUI(boolean val) {
        resetButton.setEnabled(val);
        saveButton.setEnabled(val);
        box.setEnabled(val);
    }

    // отрисовывает фрактал
    private void drawFractal() {
        enableUI(false);
        rowsRemaining = display;
        for (int y = 0; y < display; y++) {
            FractalWorker fractalWorker = new FractalWorker(y);
            fractalWorker.execute();
        }
    }

    // отслеживает нажатия кнопки reset
    private class ResetListener implements java.awt.event.ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            image.clearImage();
            fractalGenerator.getInitialRange(planeRange);
            drawFractal();
        }
    }

    // отслеживает нажатия кнопки save
    private class SaveListener implements java.awt.event.ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            FileFilter filter = new FileNameExtensionFilter("PNG Images", "png");
            fileChooser.setFileFilter(filter);
            fileChooser.setAcceptAllFileFilterUsed(false);

            int option = fileChooser.showSaveDialog(image);

            if (option == JFileChooser.APPROVE_OPTION) {
                try {
                    ImageIO.write(image.image, "png", fileChooser.getSelectedFile());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(image, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    // отслеживает клики мыши
    private class MouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (rowsRemaining == 0) {
                double x = FractalGenerator.getCoord(planeRange.x,planeRange.x + planeRange.width, display, e.getX());
                double y = FractalGenerator.getCoord(planeRange.y,planeRange.y + planeRange.width, display, e.getY());
                fractalGenerator.recenterAndZoomRange(planeRange, x, y, 0.5);
                drawFractal();
            }
        }
    }

    // отслеживает взаимодействия с элементами ComboBox
    private class BoxListener implements java.awt.event.ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            fractalGenerator = (FractalGenerator) box.getSelectedItem();
            fractalGenerator.getInitialRange(planeRange);
            drawFractal();
        }
    }

    // процесс в фоновом потоке
    public class FractalWorker extends SwingWorker<Object, Object> {
        private int yCoordinate;
        private int[] rgbArray = new int[display];

        // конструктор
        FractalWorker(int y){
            yCoordinate = y;
        }

        // собирает в массив rgb коды для отрисовки строк
        public Object doInBackground() {
            for (int i = 0; i < rgbArray.length; i++) {
                double xCoord = FractalGenerator.getCoord (planeRange.x, planeRange.x + planeRange.width, display, i);
                double yCoord = FractalGenerator.getCoord (planeRange.y, planeRange.y + planeRange.height, display, yCoordinate);
                int numIterations = fractalGenerator.numIterations(xCoord, yCoord);

                if (numIterations == -1) {
                    rgbArray[i] = 0;
                } else {
                    float hue = 0.7f + (float) numIterations / 200f;
                    int rgbColor = Color.HSBtoRGB(hue, 1f, 1f);
                    rgbArray[i] = rgbColor;
                }
            }
            return null;
        }

        // выполняется после окончания расчетов
        public void done() {
            for (int i = 0; i < rgbArray.length; i++) {
                image.drawPixel(i, yCoordinate, rgbArray[i]);
            }
            image.repaint(0, 0, yCoordinate, display, 1);
            rowsRemaining--;
            if (rowsRemaining == 0) {
                enableUI(true);
            }
        }
    }
}
