

import vocabtest.ui.TestFrame;

import javax.swing.SwingUtilities;

/**
 * 英语词汇量测试程序入口。
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TestFrame frame = new TestFrame();
            frame.setVisible(true);
        });
    }
}
