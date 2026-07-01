package vocabtest.ui;

import vocabtest.model.TestItem;
import vocabtest.model.TestResult;
import vocabtest.service.DataLoader;
import vocabtest.service.VocabularyEstimator;
import vocabtest.service.WordSampler;
import vocabtest.model.WordEntry;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 词汇量测试主界面。
 * 以网格形式展示测试单词（5列），每个单词下方有"认识"复选框，
 * 提供刷新、更多单词、提交测试按钮，以及词汇量估算结果展示。
 */
public class TestFrame extends JFrame {

    private static final String DATA_DIR = "data";
    private static final int GRID_COLS = 5;
    private static final String[] BAND_LABELS = {
        "101-500", "501-1500", "1501-3000", "3001-5000",
        "5001-7500", "7501-10000", "10001-13000", "13001-16000", "16001-20000"
    };

    // 数据层
    private List<List<WordEntry>> bands;
    private List<String> fakeWords;
    private int[] bandTotalCounts;
    private WordSampler sampler;

    // 测试项与对应的复选框
    private final List<TestItem> testItems = new ArrayList<>();
    private final List<JCheckBox> checkBoxes = new ArrayList<>();

    // UI 组件
    private JPanel wordGridPanel;
    private JScrollPane gridScrollPane;
    private JLabel resultLabel;
    private JTextArea detailArea;
    private JButton refreshButton;
    private JButton moreButton;
    private JButton submitButton;

    public TestFrame() {
        setTitle("英语词汇量测试");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(680, 720);
        setLocationRelativeTo(null);

        initData();
        initUI();
        startTest();
    }

    /** 加载数据并初始化抽样器 */
    private void initData() {
        try {
            List<WordEntry> entries = DataLoader.loadCOCA(DATA_DIR);
            bands = DataLoader.groupIntoBands(entries);
            bandTotalCounts = DataLoader.getBandTotalCounts(bands);
            fakeWords = DataLoader.loadFakeWords(DATA_DIR);
            sampler = new WordSampler();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "数据加载失败：" + e.getMessage(),
                "错误", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    /** 构建界面 */
    private void initUI() {
        setLayout(new BorderLayout(8, 8));

        // === 顶部面板：提示 + 按钮 ===
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));

        JLabel hintLabel = new JLabel(
            "基础测试100个单词，不过测试得越多，结果越准。",
            SwingConstants.CENTER);
        hintLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        hintLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 4, 10));
        topPanel.add(hintLabel, BorderLayout.NORTH);

        // 按钮行
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        refreshButton = new JButton("刷新");
        refreshButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
        moreButton = new JButton("更多单词（+10）");
        moreButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
        submitButton = new JButton("提交测试");
        submitButton.setFont(new Font("SansSerif", Font.BOLD, 14));

        refreshButton.addActionListener(e -> onRefresh());
        moreButton.addActionListener(e -> onMoreWords());
        submitButton.addActionListener(e -> onSubmit());

        buttonPanel.add(refreshButton);
        buttonPanel.add(moreButton);
        buttonPanel.add(submitButton);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        // === 中间：可滚动的单词网格 ===
        wordGridPanel = new JPanel();
        wordGridPanel.setLayout(new GridLayout(0, GRID_COLS, 8, 8));
        wordGridPanel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        
        gridScrollPane = new JScrollPane(wordGridPanel);
        gridScrollPane.setBorder(BorderFactory.createEmptyBorder());
        gridScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        add(gridScrollPane, BorderLayout.CENTER);

        // === 底部面板：结果展示 ===
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 10, 10, 10));

        JPanel resultPanel = new JPanel(new BorderLayout(5, 5));
        resultPanel.setBorder(BorderFactory.createTitledBorder("测试结果"));

        resultLabel = new JLabel("点击\"提交测试\"查看结果", SwingConstants.CENTER);
        resultLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        resultPanel.add(resultLabel, BorderLayout.NORTH);

        detailArea = new JTextArea(6, 50);
        detailArea.setEditable(false);
        detailArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane detailScroll = new JScrollPane(detailArea);
        resultPanel.add(detailScroll, BorderLayout.CENTER);

        bottomPanel.add(resultPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /** 刷新：清空并重新生成100词测试 */
    private void onRefresh() {
        sampler.reset();
        testItems.clear();
        checkBoxes.clear();
        wordGridPanel.removeAll();

        List<TestItem> initial = sampler.sampleInitial(bands, fakeWords);
        for (TestItem item : initial) {
            testItems.add(item);
            wordGridPanel.add(createWordCell(item.getWord()));
        }

        wordGridPanel.revalidate();
        wordGridPanel.repaint();
        updateMoreButtonState();
        clearResults();
    }

    /** 开始测试：抽取初始100词并显示 */
    private void startTest() {
        sampler.reset();
        testItems.clear();

        List<TestItem> initial = sampler.sampleInitial(bands, fakeWords);
        for (TestItem item : initial) {
            testItems.add(item);
            wordGridPanel.add(createWordCell(item.getWord()));
        }

        updateMoreButtonState();
        clearResults();
    }

    /** "更多单词"按钮回调 */
    private void onMoreWords() {
        List<TestItem> more = sampler.sampleMore(bands, fakeWords);
        if (more.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "可用单词已用完，无法生成更多测试词。",
                "提示", JOptionPane.INFORMATION_MESSAGE);
        } else {
            // 先保存已有勾选状态
            syncCheckBoxesToItems();
            // 添加新词
            for (TestItem item : more) {
                testItems.add(item);
                wordGridPanel.add(createWordCell(item.getWord()));
            }
            wordGridPanel.revalidate();
            wordGridPanel.repaint();
        }
        updateMoreButtonState();
    }

    /** 创建单个单词卡片：上方单词，下方复选框 */
    private JPanel createWordCell(String word) {
        JPanel cell = new JPanel();
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        cell.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(6, 6, 4, 6)
        ));

        JLabel wordLabel = new JLabel(word, SwingConstants.CENTER);
        wordLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        wordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JCheckBox checkBox = new JCheckBox("认识");
        checkBox.setFont(new Font("SansSerif", Font.PLAIN, 12));
        checkBox.setAlignmentX(Component.CENTER_ALIGNMENT);

        cell.add(wordLabel);
        cell.add(Box.createVerticalStrut(3));
        cell.add(checkBox);

        checkBoxes.add(checkBox);
        return cell;
    }

    /** 将复选框的勾选状态同步回 TestItem 列表 */
    private void syncCheckBoxesToItems() {
        int n = Math.min(testItems.size(), checkBoxes.size());
        for (int i = 0; i < n; i++) {
            testItems.get(i).setChecked(checkBoxes.get(i).isSelected());
        }
    }

    /** 根据可用单词更新"更多单词"按钮状态 */
    private void updateMoreButtonState() {
        int available = 0;
        for (int b = 0; b < DataLoader.BAND_COUNT; b++) {
            int used = 0;
            for (TestItem item : testItems) {
                if (!item.isFake() && item.getBandIndex() == b) {
                    used++;
                }
            }
            available += Math.max(0, bands.get(b).size() - used);
        }
        int fakeUsed = 0;
        for (TestItem item : testItems) {
            if (item.isFake()) fakeUsed++;
        }
        available += Math.max(0, fakeWords.size() - fakeUsed);

        moreButton.setEnabled(available >= 7);
        moreButton.setText(moreButton.isEnabled() ? "更多单词（+10）" : "单词已用完");
    }

    /** "提交测试"按钮回调 */
    private void onSubmit() {
        // 将复选框状态同步到 TestItem
        syncCheckBoxesToItems();

        TestResult result = VocabularyEstimator.estimate(testItems, bandTotalCounts);

        int estVocab = result.getEstimatedVocabulary();
        double lower = result.getLowerCI();
        double upper = result.getUpperCI();

        resultLabel.setText(String.format(
            "估算词汇量：%d   95%%置信区间：[%.0f, %.0f]   总测试词数：%d",
            estVocab, lower, upper, testItems.size()));

        // 详细信息
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-18s %6s %6s %8s %8s\n",
            "频段", "测试数", "认识数", "原始率", "矫正率"));
        sb.append("─".repeat(54)).append("\n");

        double[] rawRates = result.getBandRawRates();
        double[] corrRates = result.getBandCorrectedRates();
        int[] sampleCounts = result.getBandSampleCounts();
        int[] knownCounts = result.getBandKnownCounts();

        for (int i = 0; i < DataLoader.BAND_COUNT; i++) {
            sb.append(String.format("%-18s %6d %6d %7.1f%% %7.1f%%\n",
                BAND_LABELS[i],
                sampleCounts[i],
                knownCounts[i],
                rawRates[i] * 100,
                corrRates[i] * 100));
        }

        // 伪词行
        int fakeTotal = 0, fakeKnown = 0;
        for (TestItem item : testItems) {
            if (item.isFake()) {
                fakeTotal++;
                if (item.isChecked()) fakeKnown++;
            }
        }
        sb.append("─".repeat(54)).append("\n");
        sb.append(String.format("%-18s %6d %6d %7.1f%% %8s\n",
            "伪词(诱饵)", fakeTotal, fakeKnown,
            result.getFakeHitRate() * 100, "—"));
        sb.append(String.format("\n默认功能词（1-100）：%d（已掌握）\n", 100));

        detailArea.setText(sb.toString());
        detailArea.setCaretPosition(0);
    }

    /** 清空结果展示 */
    private void clearResults() {
        resultLabel.setText("点击\"提交测试\"查看结果");
        detailArea.setText("");
    }
}
