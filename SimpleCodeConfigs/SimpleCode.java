package SimpleCodeConfigs;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import javax.swing.undo.UndoManager;
import javax.swing.Timer;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

public class SimpleCode {
    public static void autoCompleteVSCodeStyle(JTextArea textArea) {
        final List<String> baseKeywords = new ArrayList<>();
        final List<String> keywords = new ArrayList<>(baseKeywords);
        final JPopupMenu suggestionsPopup = new JPopupMenu();
        suggestionsPopup.setFocusable(false);
        final List<JMenuItem> currentItems = new ArrayList<>();
        final int[] selectedIndex = { -1 };

        Path keywordsFile = Paths.get("keywords.txt");
        if (Files.exists(keywordsFile)) {
            try {
                baseKeywords.addAll(Files.readAllLines(keywordsFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        keywords.addAll(baseKeywords);

        Runnable updateKeywordsFromIncludes = () -> {
            keywords.clear();
            keywords.addAll(baseKeywords);

            String text = textArea.getText();
            Pattern includePattern = Pattern.compile("#include\\s+\"([^\"]+)\"");
            Matcher matcher = includePattern.matcher(text);

            while (matcher.find()) {
                String includePath = matcher.group(1);
                Path headerPath = Paths.get(includePath);

                if (Files.exists(headerPath)) {
                    try {
                        List<String> linhas = Files.readAllLines(headerPath);

                        for (String linha : linhas) {
                            linha = linha.trim();
                            if (linha.isEmpty() || linha.startsWith("//") || linha.startsWith("/*")
                                    || linha.startsWith("*")) {
                                continue;
                            }

                            Matcher funcMatcher = Pattern.compile(
                                    "(?:void|int|double|float|char|bool|std::string)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(")
                                    .matcher(linha);
                            while (funcMatcher.find()) {
                                String nomeFunc = funcMatcher.group(1) + "()";
                                if (!keywords.contains(nomeFunc))
                                    keywords.add(nomeFunc);
                            }

                            Matcher classMatcher = Pattern.compile("\\b(class|struct)\\s+([a-zA-Z_][a-zA-Z0-9_]*)")
                                    .matcher(linha);
                            while (classMatcher.find()) {
                                String nomeClasse = classMatcher.group(2);
                                if (!keywords.contains(nomeClasse))
                                    keywords.add(nomeClasse);
                            }

                            Matcher varMatcher = Pattern.compile(
                                    "(?:int|double|float|char|bool|std::string)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*;")
                                    .matcher(linha);
                            while (varMatcher.find()) {
                                String nomeVar = varMatcher.group(1);
                                if (!keywords.contains(nomeVar))
                                    keywords.add(nomeVar);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        };

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                updateKeywordsFromIncludes.run();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }
        });

        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                int pos = textArea.getCaretPosition();

                if (suggestionsPopup.isVisible()) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        if (!currentItems.isEmpty())
                            selectedIndex[0] = (selectedIndex[0] + 1) % currentItems.size();
                        highlightItem();
                        return;
                    } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                        if (!currentItems.isEmpty())
                            selectedIndex[0] = (selectedIndex[0] - 1 + currentItems.size()) % currentItems.size();
                        highlightItem();
                        return;
                    } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        if (selectedIndex[0] >= 0 && selectedIndex[0] < currentItems.size())
                            currentItems.get(selectedIndex[0]).doClick();
                        e.consume();
                        return;
                    } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        suggestionsPopup.setVisible(false);
                        return;
                    }
                }

                String text = textArea.getText();
                int start = pos - 1;
                while (start >= 0 && Character.isLetterOrDigit(text.charAt(start)))
                    start--;
                start++;
                if (pos <= start) {
                    suggestionsPopup.setVisible(false);
                    return;
                }

                String currentWord = text.substring(start, pos);
                suggestionsPopup.setVisible(false);
                suggestionsPopup.removeAll();
                currentItems.clear();
                selectedIndex[0] = -1;

                if (!currentWord.isEmpty()) {
                    int maxSuggestions = 6;
                    int count = 0;
                    for (String keyword : keywords) {
                        if (keyword.toLowerCase().startsWith(currentWord.toLowerCase())) {
                            int[] startPos = { start };
                            JMenuItem item = new JMenuItem(keyword);
                            item.addActionListener(_ -> {
                                try {
                                    int posAtual = textArea.getCaretPosition();
                                    int length = Math.max(0, Math.min(posAtual - startPos[0],
                                            textArea.getDocument().getLength() - startPos[0]));
                                    textArea.getDocument().remove(startPos[0], length);
                                    textArea.getDocument().insertString(startPos[0], keyword, null);
                                    textArea.setCaretPosition(startPos[0] + keyword.length());
                                    suggestionsPopup.setVisible(false);
                                } catch (BadLocationException ex) {
                                    ex.printStackTrace();
                                }
                            });
                            suggestionsPopup.add(item);
                            currentItems.add(item);
                            count++;
                            if (count >= maxSuggestions)
                                break;
                        }
                    }

                    if (!currentItems.isEmpty()) {
                        try {
                            Rectangle caret = textArea.modelToView2D(pos).getBounds();
                            suggestionsPopup.show(textArea, caret.x, caret.y + caret.height);
                        } catch (BadLocationException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }

            private void highlightItem() {
                for (int i = 0; i < currentItems.size(); i++) {
                    JMenuItem item = currentItems.get(i);
                    item.setArmed(i == selectedIndex[0]);
                    item.setBackground(i == selectedIndex[0] ? Color.LIGHT_GRAY : null);
                }
            }
        });
    }

    private static Path path = Paths.get("ExecSimpleCode.cpp");

    public static void main(String[] args) {
        JFrame main = new JFrame("SimpleCode");
        main.setSize(500, 500);
        main.setExtendedState(JFrame.MAXIMIZED_BOTH);
        main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ImageIcon icon = new ImageIcon("SimpleCodeLogo.jpg");
        main.setIconImage(icon.getImage());

        JTextArea textArea = new JTextArea();
        textArea.setBackground(Color.BLUE);
        textArea.setForeground(Color.WHITE);
        textArea.setCaretColor(Color.WHITE);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 18));
        textArea.setTabSize(4);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFocusTraversalKeysEnabled(false);
        autoCompleteVSCodeStyle(textArea);
        String originalContent = "";
        if (Files.exists(path)) {
            try {
                originalContent = Files.readString(path);
                textArea.setText(originalContent);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Erro ao carregar arquivo: " + e.getMessage(),
                        "SimpleCode", JOptionPane.ERROR_MESSAGE);
            }
        }

        JTextArea lineNumbers = new JTextArea("1");
        lineNumbers.setEditable(false);
        int digitos = Integer.toString(textArea.getLineCount()).length();
        int tamanhoBase = textArea.getFont().getSize();
        int novoTamanho = Math.max(tamanhoBase - (digitos - 1) * 2, 10);
        lineNumbers.setFont(new Font("Consolas", Font.PLAIN, novoTamanho));
        lineNumbers.setFocusable(false);
        lineNumbers.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        lineNumbers.setBackground(Color.WHITE);
        lineNumbers.setForeground(Color.BLUE);
        lineNumbers.setBorder(BorderFactory.createEmptyBorder(2, 12, 2, 6));

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            private void atualizar() {
                int linhas = textArea.getLineCount();
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= linhas; i++) {
                    sb.append(i).append("\n");
                }
                lineNumbers.setText(sb.toString());
                lineNumbers.setFont(textArea.getFont());
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                atualizar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                atualizar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                atualizar();
            }
        });
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setRowHeaderView(lineNumbers);
        main.add(scrollPane, BorderLayout.CENTER);

        comandos(textArea, main, path, originalContent);
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem salvarItem = new JMenuItem("Salvar");
        salvarItem.addActionListener(_ -> {
            try {
                Files.write(path, textArea.getText().getBytes());
                monitorarAlteracoesAtualizaOriginal(textArea, main);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Erro ao salvar: " + ex.getMessage(), "SimpleCode",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        popupMenu.add(salvarItem);

        JMenuItem executarItem = new JMenuItem("Executar");
        executarItem.addActionListener(_ -> {
            textArea.getActionMap().get("Executar").actionPerformed(null);
        });
        popupMenu.add(executarItem);

        JMenuItem SalvarExecutarItem = new JMenuItem("Salvar e Executar");
        SalvarExecutarItem.addActionListener(_ -> {
            textArea.getActionMap().get("Salvar").actionPerformed(null);
            textArea.getActionMap().get("Executar").actionPerformed(null);
        });
        popupMenu.add(SalvarExecutarItem);

        JMenuItem AbrirNovoArquivo = new JMenuItem("Abrir novo arquivo");
        AbrirNovoArquivo.addActionListener(_ -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Abrir arquivo .cpp");

            fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".cpp");
                }

                @Override
                public String getDescription() {
                    return "Arquivos C++ (*.cpp)";
                }
            });

            int resultado = fileChooser.showOpenDialog(main);
            if (resultado == JFileChooser.APPROVE_OPTION) {
                File selecionado = fileChooser.getSelectedFile();
                try {
                    String conteudo = Files.readString(selecionado.toPath());
                    textArea.setText(conteudo);
                    monitorarAlteracoesAtualizaOriginal(textArea, main);

                    path = selecionado.toPath();

                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(main, "Erro ao abrir arquivo: " + ex.getMessage(),
                            "SimpleCode", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        popupMenu.add(AbrirNovoArquivo);

        JMenuItem RecarregarItem = new JMenuItem("Recarregar");
        RecarregarItem.addActionListener(_ -> {
            textArea.getActionMap().get("Recarregar").actionPerformed(null);
        });
        popupMenu.add(RecarregarItem);

        JPopupMenu middlePopupMenu = new JPopupMenu();

        Path libPath = Paths.get("SimpleCodeLIB");

        if (Files.exists(libPath) && Files.isDirectory(libPath)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(libPath)) {
                for (Path folder : stream) {
                    if (Files.isDirectory(folder)) {
                        String folderName = folder.getFileName().toString();
                        JMenuItem folderItem = new JMenuItem(folderName);

                        folderItem.addActionListener(_ -> {
                            try {
                                Path headerFile = folder.resolve("HeaderFileSC.txt");
                                if (Files.exists(headerFile)) {
                                    String headerPath = Files.readString(headerFile).trim();
                                    Path header = Paths.get(headerPath);
                                    if (Files.exists(header)) {
                                        String includeLine = "#include \"" + header.toString().replace("\\", "/")
                                                + "\"\n";

                                        String currentText = textArea.getText();
                                        int insertPos = 0;

                                        Pattern pattern = Pattern.compile("(?m)^\\s*#include\\s+\"[^\"]+\"");
                                        Matcher matcher = pattern.matcher(currentText);

                                        int lastIncludeEnd = -1;
                                        while (matcher.find()) {
                                            lastIncludeEnd = matcher.end();
                                        }

                                        if (lastIncludeEnd != -1) {
                                            insertPos = lastIncludeEnd + 1;
                                        }

                                        textArea.getDocument().insertString(insertPos, includeLine, null);

                                    } else {
                                        JOptionPane.showMessageDialog(main,
                                                "Arquivo .h não encontrado no caminho informado.",
                                                "Erro", JOptionPane.ERROR_MESSAGE);
                                    }
                                } else {
                                    JOptionPane.showMessageDialog(main,
                                            "HeaderFileSC.txt não encontrado na pasta " + folderName,
                                            "Aviso", JOptionPane.WARNING_MESSAGE);
                                }
                            } catch (IOException | BadLocationException ex) {
                                ex.printStackTrace();
                            }
                        });

                        middlePopupMenu.add(folderItem);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        textArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    popupMenu.show(textArea, e.getX(), e.getY());
                } else if (e.getButton() == MouseEvent.BUTTON2) {
                    middlePopupMenu.removeAll();

                    Path libPath = Paths.get("SimpleCodeLIB");
                    if (Files.exists(libPath) && Files.isDirectory(libPath)) {
                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(libPath)) {
                            for (Path folder : stream) {
                                if (Files.isDirectory(folder)) {
                                    String folderName = folder.getFileName().toString();
                                    JMenuItem folderItem = new JMenuItem(folderName);

                                    folderItem.addActionListener(_ -> {
                                        try {
                                            Path headerFile = folder.resolve("HeaderFileSC.txt");
                                            if (Files.exists(headerFile)) {
                                                String headerPath = Files.readString(headerFile).trim();
                                                Path header = Paths.get(headerPath);
                                                if (Files.exists(header)) {
                                                    String includeLine = "#include \""
                                                            + header.toString().replace("\\", "/") + "\"\n";

                                                    String currentText = textArea.getText();
                                                    int insertPos = 0;

                                                    Pattern pattern = Pattern
                                                            .compile("(?m)^\\s*#include\\s+\"[^\"]+\"");
                                                    Matcher matcher = pattern.matcher(currentText);

                                                    int lastIncludeEnd = -1;
                                                    while (matcher.find()) {
                                                        lastIncludeEnd = matcher.end();
                                                    }

                                                    if (lastIncludeEnd != -1) {
                                                        insertPos = lastIncludeEnd + 1;
                                                    }

                                                    textArea.getDocument().insertString(insertPos, includeLine, null);

                                                } else {
                                                    JOptionPane.showMessageDialog(main,
                                                            "Arquivo .h não encontrado no caminho informado.",
                                                            "Erro", JOptionPane.ERROR_MESSAGE);
                                                }
                                            } else {
                                                JOptionPane.showMessageDialog(main,
                                                        "HeaderFileSC.txt não encontrado na pasta " + folderName,
                                                        "Aviso", JOptionPane.WARNING_MESSAGE);
                                            }
                                        } catch (IOException | BadLocationException ex) {
                                            ex.printStackTrace();
                                        }
                                    });

                                    middlePopupMenu.add(folderItem);
                                }
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }

                    middlePopupMenu.show(textArea, e.getX(), e.getY());
                }
            }
        });

        caracteresAutoComplete(textArea);
        monitorarAlteracoes(textArea, main, originalContent);

        main.setVisible(true);
    }

    public static void comandos(JTextArea textArea, JFrame main, Path path, String originalContent) {
        UndoManager undoManager = new UndoManager();
        textArea.getDocument().addUndoableEditListener(undoManager);

        KeyStroke ctrlZ = KeyStroke.getKeyStroke("control Z");
        textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlZ, "Desfazer");
        textArea.getActionMap().put("Desfazer", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canUndo()) {
                    undoManager.undo();
                }
            }
        });

        KeyStroke ctrlAltB = KeyStroke.getKeyStroke("control alt B");
        textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlAltB, "BaseInicial");
        textArea.getActionMap().put("BaseInicial", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.setText(
                        "#include <iostream>\n\nint main()\n{\n    std::cout << \"Hello, World!\" << std::endl;\n    return 0;\n}");
            }
        });

        KeyStroke ctrlY = KeyStroke.getKeyStroke("control Y");
        textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlY, "Refazer");
        textArea.getActionMap().put("Refazer", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canRedo()) {
                    undoManager.redo();
                }
            }
        });

        KeyStroke ctrlS = KeyStroke.getKeyStroke("control S");
        textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlS, "Salvar");
        textArea.getActionMap().put("Salvar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Files.write(path, textArea.getText().getBytes());
                    monitorarAlteracoesAtualizaOriginal(textArea, main);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Erro ao salvar: " + ex.getMessage(), "SimpleCode",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        KeyStroke ctrlE = KeyStroke.getKeyStroke("control E");
        textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlE, "Executar");
        textArea.getActionMap().put("Executar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (!Files.exists(path)) {
                        JOptionPane.showMessageDialog(null, "Arquivo não encontrado. Salve antes de executar.",
                                "SimpleCode", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    String code = Files.readString(path);

                    Pattern pattern = Pattern.compile("#include\\s+\"([^\"]+)\"");
                    Matcher matcher = pattern.matcher(code);
                    List<String> additionalCpp = new ArrayList<>();
                    while (matcher.find()) {
                        String includeName = matcher.group(1);
                        Path cppPath = findCppForHeader(Paths.get("."), includeName.replace(".H", ".cpp"));
                        if (cppPath != null) {
                            additionalCpp.add(cppPath.toString());
                        } else {
                            System.out.println("Não encontrou o .cpp para: " + includeName);
                        }
                    }

                    List<String> command = new ArrayList<>();
                    command.add("g++");
                    command.add(path.toString());
                    command.addAll(additionalCpp);
                    command.add("-o");
                    command.add("ExecSimpleCode.exe");

                    ProcessBuilder compile = new ProcessBuilder(command);
                    compile.redirectErrorStream(true);
                    Process compileProcess = compile.start();
                    String compileOutput = readProcessOutput(compileProcess);
                    int compileResult = compileProcess.waitFor();

                    if (compileResult != 0) {
                        Path logFile = Paths.get("compile_log.txt");
                        Files.writeString(logFile, compileOutput);

                        new ProcessBuilder("cmd.exe", "/c", "start", "cmd.exe", "/k", "type compile_log.txt").start();
                    } else {
                        new ProcessBuilder("cmd.exe", "/c", "start", "cmd.exe", "/k", "ExecSimpleCode.exe").start();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Erro ao executar: " + ex.getMessage(), "SimpleCode",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        KeyStroke f11 = KeyStroke.getKeyStroke("F11");
        textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(f11, "ToggleFullscreen");
        textArea.getActionMap().put("ToggleFullscreen", new AbstractAction() {
            private boolean fullscreen = false;
            private Rectangle windowedBounds;

            @Override
            public void actionPerformed(ActionEvent e) {
                GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                if (!fullscreen) {
                    windowedBounds = main.getBounds();
                    main.dispose();
                    main.setUndecorated(true);
                    main.setVisible(true);
                    device.setFullScreenWindow(main);
                    fullscreen = true;
                } else {
                    device.setFullScreenWindow(null);
                    main.dispose();
                    main.setUndecorated(false);
                    main.setBounds(windowedBounds);
                    main.setVisible(true);
                    fullscreen = false;
                }
            }
        });

        KeyStroke ctrlPlus = KeyStroke.getKeyStroke("control EQUALS");
        textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlPlus, "AumentarTudo");
        textArea.getActionMap().put("AumentarTudo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Font f = textArea.getFont();
                float novoTamanho = f.getSize() + 2f;
                textArea.setFont(f.deriveFont(novoTamanho));

                JTextArea lineNumbers = ((JTextArea) ((JScrollPane) textArea.getParent().getParent()).getRowHeader()
                        .getView());
                lineNumbers.setFont(lineNumbers.getFont().deriveFont(novoTamanho));
            }
        });

        KeyStroke ctrlMinus = KeyStroke.getKeyStroke("control MINUS");
        textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlMinus, "DiminuirTudo");
        textArea.getActionMap().put("DiminuirTudo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Font f = textArea.getFont();
                if (f.getSize() > 4) {
                    float novoTamanho = f.getSize() - 2f;
                    textArea.setFont(f.deriveFont(novoTamanho));

                    JTextArea lineNumbers = ((JTextArea) ((JScrollPane) textArea.getParent().getParent()).getRowHeader()
                            .getView());
                    lineNumbers.setFont(lineNumbers.getFont().deriveFont(novoTamanho));
                }
            }
        });

        KeyStroke ctrlR = KeyStroke.getKeyStroke("control R");
        textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlR, "Recarregar");
        textArea.getActionMap().put("Recarregar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Color corOriginal = textArea.getBackground();
                Color corTextoOriginal = textArea.getForeground();

                textArea.setBackground(corOriginal);
                textArea.setForeground(corOriginal);
                textArea.repaint();

                new Timer(100, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ev) {
                        textArea.setBackground(corOriginal);
                        textArea.setForeground(corTextoOriginal);
                        textArea.repaint();

                        try {
                            if (!Files.exists(path)) {
                                JOptionPane.showMessageDialog(null, "Arquivo não encontrado.",
                                        "SimpleCode", JOptionPane.INFORMATION_MESSAGE);
                                return;
                            }
                            String content = Files.readString(path);
                            textArea.setText(content);
                            monitorarAlteracoesAtualizaOriginal(textArea, main);
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(null, "Erro ao recarregar: " + ex.getMessage(),
                                    "SimpleCode", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }) {
                    {
                        setRepeats(false);
                        start();
                    }
                };
            }
        });

    }

    private static Path findCppForHeader(Path baseDir, String includePath) throws IOException {
        Path headerPath = baseDir.resolve(includePath).normalize();
        System.out.println("Tentando encontrar header: " + headerPath);

        if (Files.exists(headerPath)) {
            Path cppPath = headerPath.getParent()
                    .resolve(headerPath.getFileName().toString().replaceAll("(?i)\\.h$", ".cpp"));
            System.out.println("Tentando usar CPP correspondente: " + cppPath);
            if (Files.exists(cppPath)) {
                System.out.println("CPP encontrado: " + cppPath);
                return cppPath;
            } else {
                System.out.println("CPP NÃO encontrado na mesma pasta.");
            }
        } else {
            System.out.println("Header não encontrado: " + headerPath);
        }
        return null;
    }

    public static void caracteresAutoComplete(JTextArea textArea) {
        Map<Character, Character> pareamentos = new HashMap<>();
        pareamentos.put('\"', '\"');
        pareamentos.put('\'', '\'');
        pareamentos.put('(', ')');
        pareamentos.put('[', ']');
        pareamentos.put('{', '}');

        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char typed = e.getKeyChar();
                if (pareamentos.containsKey(typed)) {
                    e.consume();
                    char fechamento = pareamentos.get(typed);
                    int pos = textArea.getCaretPosition();
                    try {
                        textArea.getDocument().insertString(pos, "" + typed + fechamento, null);
                        textArea.setCaretPosition(pos + 1);
                    } catch (BadLocationException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    public static void monitorarAlteracoes(JTextArea textArea, JFrame main, String originalContent) {
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            private void atualizar() {
                if (textArea.getText().equals(originalContent)) {
                    main.setTitle("SimpleCode - SAVED");
                } else {
                    main.setTitle("SimpleCode - UNSAVED");
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                atualizar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                atualizar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                atualizar();
            }
        });

        main.setTitle("SimpleCode - SAVED");
    }

    private static void monitorarAlteracoesAtualizaOriginal(JTextArea textArea, JFrame main) {
        main.setTitle("SimpleCode - SAVED");
    }

    private static String readProcessOutput(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        return output.toString();
    }
}