package com.gdhub.loader;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Typeface;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final int PICK_PACKAGE = 41;
    private LinearLayout root;
    private TextView status;
    private final ArrayList<File> packages = new ArrayList<>();

    static {
        System.loadLibrary("gdhub_loader");
    }

    private native String nativeRunPackage(String absolutePath);

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        scanPackages();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 28, 24, 24);
        root.setBackgroundColor(0xFF101018);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("GD HUB");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Mod Manager • Android ARM32 / ARM64");
        subtitle.setTextColor(0xFFB8B8C8);
        subtitle.setTextSize(13);
        root.addView(subtitle);

        Button add = button("IMPORTAR .GDHB");
        add.setOnClickListener(v -> pickPackage());
        root.addView(add);

        Button refresh = button("ATUALIZAR LISTA");
        refresh.setOnClickListener(v -> scanPackages());
        root.addView(refresh);

        status = new TextView(this);
        status.setTextColor(0xFFE6E6F0);
        status.setTextSize(14);
        status.setPadding(0, 20, 0, 12);
        root.addView(status);

        setContentView(scroll);
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(15);
        return b;
    }

    private void pickPackage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_PACKAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_PACKAGE || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;
        File dir = new File(getFilesDir(), "mods");
        if (!dir.exists() && !dir.mkdirs()) {
            status.setText("Não foi possível criar a pasta de mods.");
            return;
        }
        String name = "imported_" + System.currentTimeMillis() + ".gdhb";
        File destination = new File(dir, name);
        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(destination)) {
            if (in == null) throw new IllegalStateException("Arquivo não pôde ser aberto.");
            byte[] buffer = new byte[8192];
            int read;
            long total = 0;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > 16L * 1024L * 1024L) {
                    throw new IllegalArgumentException("O pacote excede 16 MiB.");
                }
                out.write(buffer, 0, read);
            }
            status.setText("Importado: " + name);
            scanPackages();
        } catch (Exception error) {
            status.setText("Falha ao importar: " + error.getMessage());
            destination.delete();
        }
    }

    private void scanPackages() {
        root.removeViews(5, Math.max(0, root.getChildCount() - 5));
        packages.clear();
        File dir = new File(getFilesDir(), "mods");
        File[] found = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".gdhb"));
        if (found != null) {
            for (File file : found) packages.add(file);
        }
        if (packages.isEmpty()) {
            status.setText("Nenhum pacote importado. Toque em IMPORTAR .GDHB.");
            return;
        }
        status.setText("Pacotes importados: " + packages.size());
        for (File file : packages) {
            TextView label = new TextView(this);
            label.setText("📦 " + file.getName());
            label.setTextColor(0xFFFFFFFF);
            label.setTextSize(16);
            label.setPadding(0, 16, 0, 4);
            root.addView(label);
            Button run = button("VALIDAR E EXECUTAR");
            run.setOnClickListener(v -> runPackage(file));
            root.addView(run);
            Button delete = button("REMOVER PACOTE");
            delete.setOnClickListener(v -> {
                if (file.delete()) {
                    status.setText("Pacote removido: " + file.getName());
                    scanPackages();
                } else {
                    status.setText("Não foi possível remover o pacote.");
                }
            });
            root.addView(delete);
        }
    }

    private void runPackage(File file) {
        try {
            String result = nativeRunPackage(file.getAbsolutePath());
            status.setText(result.replace("\n", "\n"));
        } catch (Throwable error) {
            status.setText("Erro no runtime nativo: " + error.getMessage());
        }
    }
}
