package com.sensilab.ff;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Tela principal nativa (sem WebView).
 * Sensibilidade, painel flutuante, gravação de tela, abrir Free Fire e atalho de ADB.
 */
public class MainActivity extends Activity {

    private static final int PEDIDO_GRAVACAO = 11;
    private static final String PACOTE_FF = "com.dts.freefireth";
    private static final String PACOTE_FF_MAX = "com.dts.freefiremax";

    private static final String[] ROTULOS = {"Geral", "Ponto vermelho", "Mira 2x", "Mira 4x", "AWM / Sniper", "Olhadinha"};
    private static final String[] CHAVES = {"geral", "red", "x2", "x4", "awm", "look"};
    private static final int[] PADRAO = {165, 155, 145, 135, 95, 130};

    private SharedPreferences prefs;
    private TextView estadoGravacao;
    private TextView estadoPainel;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        prefs = getSharedPreferences("fx", MODE_PRIVATE);

        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setPadding(dp(16), dp(24), dp(16), dp(32));
        raiz.setBackgroundColor(Color.parseColor("#0B0D09"));

        TextView titulo = new TextView(this);
        titulo.setText("SensiLab FX");
        titulo.setTextColor(Color.parseColor("#F1F4EB"));
        titulo.setTextSize(26);
        raiz.addView(titulo);

        TextView sub = new TextView(this);
        sub.setText("Sensibilidade, painel sobre o jogo, gravação e Free Fire");
        sub.setTextColor(Color.parseColor("#A0AA98"));
        sub.setTextSize(13);
        sub.setPadding(0, dp(4), 0, dp(18));
        raiz.addView(sub);

        cardSensi(raiz);
        cardToque(raiz);
        cardPainel(raiz);
        cardGravacao(raiz);
        cardJogo(raiz);
        cardAdb(raiz);
        cardDicas(raiz);

        ScrollView rolagem = new ScrollView(this);
        rolagem.addView(raiz);
        setContentView(rolagem);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (estadoGravacao != null) {
            estadoGravacao.setText(ScreenRecordService.gravando
                    ? "Gravando agora… toque em parar para salvar o clipe."
                    : "Último arquivo: " + ScreenRecordService.ultimoArquivo);
        }
        if (estadoPainel != null) {
            boolean ligado = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
            estadoPainel.setText(ligado ? "Permissão de sobrepor: liberada" : "Permissão de sobrepor: pendente");
            estadoPainel.setTextColor(Color.parseColor(ligado ? "#C8F169" : "#FF9E55"));
        }
    }

    // ---------------- sensibilidade ----------------

    private void cardSensi(LinearLayout raiz) {
        LinearLayout card = cartao(raiz, "Sensibilidade", "Ajuste cada mira de 0 a 200. Fica salvo no aparelho.");

        for (int i = 0; i < CHAVES.length; i++) {
            final String chave = CHAVES[i];
            final TextView valor = new TextView(this);
            valor.setText(String.valueOf(prefs.getInt("sensi_" + chave, PADRAO[i])));
            valor.setTextColor(Color.parseColor("#C8F169"));
            valor.setGravity(Gravity.END);

            LinearLayout linha = new LinearLayout(this);
            linha.setOrientation(LinearLayout.HORIZONTAL);
            linha.setPadding(0, dp(10), 0, 0);

            TextView nome = new TextView(this);
            nome.setText(ROTULOS[i]);
            nome.setTextColor(Color.parseColor("#E8EDE2"));
            nome.setTextSize(14);
            LinearLayout.LayoutParams larguraNome =
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            linha.addView(nome, larguraNome);
            linha.addView(valor);

            SeekBar barra = new SeekBar(this);
            barra.setMax(200);
            barra.setProgress(prefs.getInt("sensi_" + chave, PADRAO[i]));
            barra.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar sb, int progresso, boolean doUsuario) {
                    valor.setText(String.valueOf(progresso));
                    prefs.edit().putInt("sensi_" + chave, progresso).apply();
                }

                @Override public void onStartTrackingTouch(SeekBar sb) { }

                @Override public void onStopTrackingTouch(SeekBar sb) { }
            });

            card.addView(linha);
            card.addView(barra);
        }

        botao(card, "Restaurar padrão", false, v -> {
            for (int i = 0; i < CHAVES.length; i++) prefs.edit().putInt("sensi_" + CHAVES[i], PADRAO[i]).apply();
            recreate();
        });
    }

    // ---------------- toque e DPI ----------------

    private void cardToque(LinearLayout raiz) {
        LinearLayout card = cartao(raiz, "Clique mais rápido e DPI",
                "Mexe na velocidade do toque do sistema e na densidade da tela.");

        TextView estado = new TextView(this);
        estado.setTextSize(12);
        estado.setTextColor(Color.parseColor(temPermissaoSistema() ? "#C8F169" : "#FF9E55"));
        estado.setPadding(0, 0, 0, dp(6));
        estado.setText(temPermissaoSistema()
                ? "Permissão de alterar configurações: liberada"
                : "Permissão de alterar configurações: pendente");
        card.addView(estado);

        int toque = prefs.getInt("toque", -4);

        final TextView valorToque = new TextView(this);
        valorToque.setText(String.valueOf(toque));
        valorToque.setTextColor(Color.parseColor("#C8F169"));
        valorToque.setGravity(Gravity.END);

        LinearLayout linhaToque = new LinearLayout(this);
        linhaToque.setOrientation(LinearLayout.HORIZONTAL);
        linhaToque.setPadding(0, dp(10), 0, 0);

        TextView nomeToque = new TextView(this);
        nomeToque.setText("Velocidade do toque (-7 = mais rápido)");
        nomeToque.setTextColor(Color.parseColor("#E8EDE2"));
        nomeToque.setTextSize(14);
        linhaToque.addView(nomeToque,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        linhaToque.addView(valorToque);
        card.addView(linhaToque);

        SeekBar barraToque = new SeekBar(this);
        barraToque.setMax(14);
        barraToque.setProgress(toque + 7);
        barraToque.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progresso, boolean doUsuario) {
                int v = progresso - 7;
                valorToque.setText(String.valueOf(v));
                prefs.edit().putInt("toque", v).apply();
                if (doUsuario) aplicarToque(v);
            }

            @Override public void onStartTrackingTouch(SeekBar sb) { }

            @Override public void onStopTrackingTouch(SeekBar sb) { }
        });
        card.addView(barraToque);

        int dpi = prefs.getInt("dpi", 0);

        final TextView valorDpi = new TextView(this);
        valorDpi.setText(String.valueOf(dpi == 0 ? densidadeAtual() : dpi));
        valorDpi.setTextColor(Color.parseColor("#C8F169"));
        valorDpi.setGravity(Gravity.END);

        LinearLayout linhaDpi = new LinearLayout(this);
        linhaDpi.setOrientation(LinearLayout.HORIZONTAL);
        linhaDpi.setPadding(0, dp(14), 0, 0);

        TextView nomeDpi = new TextView(this);
        nomeDpi.setText("DPI da tela");
        nomeDpi.setTextColor(Color.parseColor("#E8EDE2"));
        nomeDpi.setTextSize(14);
        linhaDpi.addView(nomeDpi,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        linhaDpi.addView(valorDpi);
        card.addView(linhaDpi);

        final SeekBar barraDpi = new SeekBar(this);
        barraDpi.setMax(41);
        barraDpi.setProgress(dpi == 0 ? 20 : Math.max(0, Math.min(41, (dpi - 320) / 10)));
        barraDpi.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progresso, boolean doUsuario) {
                valorDpi.setText(String.valueOf(320 + progresso * 10));
            }

            @Override public void onStartTrackingTouch(SeekBar sb) { }

            @Override public void onStopTrackingTouch(SeekBar sb) { }
        });
        card.addView(barraDpi);

        botao(card, "Aplicar DPI", false, v -> {
            int alvo = 320 + barraDpi.getProgress() * 10;
            prefs.edit().putInt("dpi", alvo).apply();
            aplicarDpi(alvo);
        });

        botao(card, "Liberar permissão do sistema", false, v -> {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                Toast.makeText(this, "Abra Ajustes > Apps > SensiLab FX > acesso especial",
                        Toast.LENGTH_LONG).show();
            }
        });

        botao(card, "Restaurar toque e DPI", false, v -> {
            prefs.edit().putInt("dpi", 0).apply();
            aplicarToque(0);
            restaurarDpi();
            recreate();
        });
    }

    private boolean temPermissaoSistema() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        return Settings.System.canWrite(this);
    }

    private int densidadeAtual() {
        return getResources().getDisplayMetrics().densityDpi;
    }

    private void aplicarToque(int valor) {
        try {
            Settings.System.putInt(getContentResolver(), "pointer_speed", valor);
            Toast.makeText(this, "Toque ajustado para " + valor, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Libere 'alterar configurações' para o app", Toast.LENGTH_LONG).show();
        }
    }

    private void aplicarDpi(int dpi) {
        try {
            Settings.Secure.putInt(getContentResolver(), "display_density_forced", dpi);
            Toast.makeText(this, "DPI " + dpi + " aplicado", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            copiar("adb shell wm density " + dpi);
            Toast.makeText(this, "Sem permissão segura: comando de DPI copiado", Toast.LENGTH_LONG).show();
        }
    }

    private void restaurarDpi() {
        try {
            Settings.Secure.putString(getContentResolver(), "display_density_forced", null);
            Toast.makeText(this, "DPI original de volta", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            copiar("adb shell wm density reset");
            Toast.makeText(this, "Comando de reset copiado", Toast.LENGTH_LONG).show();
        }
    }

    private void copiar(String texto) {
        ClipboardManager area = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        area.setPrimaryClip(ClipData.newPlainText("fx", texto));
    }

    // ---------------- painel flutuante ----------------

    private void cardPainel(LinearLayout raiz) {
        LinearLayout card = cartao(raiz, "Painel flutuante", "Fica por cima do Free Fire e roda em segundo plano.");

        estadoPainel = new TextView(this);
        estadoPainel.setTextSize(12);
        estadoPainel.setPadding(0, 0, 0, dp(8));
        card.addView(estadoPainel);

        botao(card, "Ligar painel", true, v -> ligarPainel());
        botao(card, "Desligar painel", false, v -> {
            stopService(new Intent(this, FloatingService.class));
            Toast.makeText(this, "Painel desligado", Toast.LENGTH_SHORT).show();
        });
    }

    private void ligarPainel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
            Toast.makeText(this, "Autorize sobrepor outros apps e volte", Toast.LENGTH_LONG).show();
            return;
        }
        Intent i = new Intent(this, FloatingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i);
        else startService(i);
        Toast.makeText(this, "Painel ativo", Toast.LENGTH_SHORT).show();
    }

    // ---------------- gravação ----------------

    private void cardGravacao(LinearLayout raiz) {
        LinearLayout card = cartao(raiz, "Gravar tela e clipes", "Grava o jogo em MP4 na pasta do app. Sem marca d'água.");

        estadoGravacao = new TextView(this);
        estadoGravacao.setTextSize(12);
        estadoGravacao.setTextColor(Color.parseColor("#A0AA98"));
        estadoGravacao.setPadding(0, 0, 0, dp(8));
        estadoGravacao.setText("Último arquivo: " + ScreenRecordService.ultimoArquivo);
        card.addView(estadoGravacao);

        botao(card, "Gravar tela", true, v -> {
            MediaProjectionManager gerenciador =
                    (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            startActivityForResult(gerenciador.createScreenCaptureIntent(), PEDIDO_GRAVACAO);
        });

        botao(card, "Parar e salvar clipe", false,
                v -> ScreenRecordService.parar(this));
    }

    @Override
    protected void onActivityResult(int codigo, int resultado, Intent dados) {
        super.onActivityResult(codigo, resultado, dados);
        if (codigo == PEDIDO_GRAVACAO && resultado == RESULT_OK && dados != null) {
            ScreenRecordService.iniciar(this, resultado, dados);
            Toast.makeText(this, "Gravando…", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------- jogo ----------------

    private void cardJogo(LinearLayout raiz) {
        LinearLayout card = cartao(raiz, "Free Fire", "Abre o jogo instalado direto daqui.");
        botao(card, "Abrir Free Fire", true, v -> abrirFreeFire());
    }

    private void abrirFreeFire() {
        Intent abrir = getPackageManager().getLaunchIntentForPackage(PACOTE_FF);
        if (abrir == null) abrir = getPackageManager().getLaunchIntentForPackage(PACOTE_FF_MAX);
        if (abrir != null) {
            startActivity(abrir);
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + PACOTE_FF)));
        } catch (Exception e) {
            Toast.makeText(this, "Free Fire não encontrado neste aparelho", Toast.LENGTH_LONG).show();
        }
    }

    // ---------------- ADB ----------------

    private void cardAdb(LinearLayout raiz) {
        LinearLayout card = cartao(raiz, "ADB sem fio",
                "Liga a depuração e copia os comandos para usar no PC.");

        botao(card, "Abrir opções do desenvolvedor", false, v -> {
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, "Abra as opções do desenvolvedor manualmente", Toast.LENGTH_LONG).show();
            }
        });

        botao(card, "Copiar comandos ADB", false, v -> {
            String comandos = "adb pair IP:PORTA\nadb connect IP:PORTA\nadb devices\nadb shell dumpsys SurfaceFlinger --latency com.dts.freefireth";
            ClipboardManager area = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            area.setPrimaryClip(ClipData.newPlainText("adb", comandos));
            Toast.makeText(this, "Comandos copiados", Toast.LENGTH_SHORT).show();
        });
    }

    // ---------------- dicas ----------------

    private void cardDicas(LinearLayout raiz) {
        LinearLayout card = cartao(raiz, "Desempenho", "Checklist rápido antes de jogar.");
        String[] dicas = {
                "Reduza gráficos e efeitos dentro do jogo até o FPS ficar estável.",
                "Feche downloads e apps pesados antes da partida.",
                "Evite jogar carregando: calor derruba o desempenho.",
                "Mude uma mira por vez e teste no campo de treino.",
                "Ative o modo desempenho do aparelho, se existir."
        };
        for (String dica : dicas) {
            TextView item = new TextView(this);
            item.setText("• " + dica);
            item.setTextColor(Color.parseColor("#A0AA98"));
            item.setTextSize(13);
            item.setPadding(0, dp(4), 0, 0);
            card.addView(item);
        }
    }

    // ---------------- utilidades de tela ----------------

    private LinearLayout cartao(LinearLayout raiz, String titulo, String descricao) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable fundo = new GradientDrawable();
        fundo.setColor(Color.parseColor("#151A12"));
        fundo.setCornerRadius(dp(18));
        fundo.setStroke(1, Color.parseColor("#30382D"));
        card.setBackground(fundo);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(12);
        raiz.addView(card, params);

        TextView t = new TextView(this);
        t.setText(titulo);
        t.setTextColor(Color.parseColor("#F1F4EB"));
        t.setTextSize(16);
        card.addView(t);

        TextView d = new TextView(this);
        d.setText(descricao);
        d.setTextColor(Color.parseColor("#A0AA98"));
        d.setTextSize(12);
        d.setPadding(0, dp(2), 0, dp(6));
        card.addView(d);

        return card;
    }

    private void botao(LinearLayout pai, String texto, boolean destaque, View.OnClickListener acao) {
        Button b = new Button(this);
        b.setText(texto);
        b.setAllCaps(false);
        b.setTextColor(Color.parseColor(destaque ? "#1B2112" : "#F1F4EB"));

        GradientDrawable fundo = new GradientDrawable();
        fundo.setColor(Color.parseColor(destaque ? "#C8F169" : "#232A1E"));
        fundo.setCornerRadius(dp(12));
        if (!destaque) fundo.setStroke(1, Color.parseColor("#3B4338"));
        b.setBackground(fundo);

        b.setOnClickListener(acao);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(8);
        pai.addView(b, params);
    }

    private int dp(int valor) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valor,
                getResources().getDisplayMetrics());
    }
}
