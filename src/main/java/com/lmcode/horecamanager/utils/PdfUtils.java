package com.lmcode.horecamanager.utils;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PdfUtils {
    private PdfUtils() {
    }

    public static void writeText(Path path, String content) {
        try {
            createParent(path);
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Export texte impossible: " + path, exception);
        }
    }

    public static void writePdf(Path path, String title, String content) {
        try {
            createParent(path);
            Document document = new Document();
            OutputStream outputStream = Files.newOutputStream(path);
            try {
                PdfWriter.getInstance(document, outputStream);
                document.open();
                document.add(new Paragraph(title, new Font(Font.HELVETICA, 16, Font.BOLD)));
                document.add(new Paragraph(" "));
                document.add(new Paragraph(content, new Font(Font.COURIER, 9)));
                document.close();
            } finally {
                if (document.isOpen()) {
                    document.close();
                }
                outputStream.close();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Export PDF impossible: " + path, exception);
        }
    }

    private static void createParent(Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
