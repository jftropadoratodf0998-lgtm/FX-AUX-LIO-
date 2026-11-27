package com.sensilab.ff;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Painel flutuante sobre outros apps, mantido em primeiro plano.
 */
public class FloatingService extends Service {

    private static final String CANAL = "fx_painel";
    private static final int ID_NOTIFICACAO = 7;

    private WindowManager janela;
    private View painel;
    private WindowManager.LayoutParams params;
    private TextView valorSensi;
    private Button botaoGravacao;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("fx", MODE_PRIVATE);
        criarCanal();
        entrarEmPrimeiroPlano();
        montarPainel();
    }

    private void criarCanal() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel canal = new NotificationChannel(CANAL, "Painel FX", NotificationManager.IMPORTANCE_LOW);
        canal.setDescription("Mantém o painel flutuante ativo");
        getSystemService(NotificationManager.class).createNotificationChannel(canal);
    }

    private void entrarEmPrimeiroPlano() {
        Intent abrir = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, abrir,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CANAL)
                : new Notification.Builder(this);

        Notification n = b.setContentTitle("Painel FX ativo")
                .setContentText("Toque para abrir o SensiLab FX")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(ID_NOTIFICACAO, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(ID_NOTIFICACAO, n);
        }
    }

    private void montarPainel() {
        janela = (WindowManager) getSystemService(WINDOW_SERVICE);

        LinearLayout caixa = new LinearLayout(this);
        caixa.setOrientation(LinearLayout.VERTICAL);
        caixa.setPadding(dp(12), dp(10), dp(12), dp(12));

        GradientDrawable fundo = new GradientDrawable();
        fundo.setColor(Color.parseColor("#F2151A12"));
        fundo.setCornerRadius(dp(16));
        fundo.setStroke(1, Color.parseColor("#C8F169"));
        caixa.setBackground(fundo);

        // cabeçalho arrastável
        LinearLayout cabecalho = new LinearLayout(this);
        cabecalho.setOrientation(LinearLayout.HORIZONTAL);
        cabecalho.setGravity(Gravity.CENTER_VERTICAL);

        TextView titulo = new TextView(this);
        titulo.setText("FX · Painel");
        titulo.setTextColor(Color.parseColor("#C8F169"));
        titulo.setTextSize(13);
        LinearLayout.LayoutParams larguraTitulo =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cabecalho.addView(titulo, larguraTitulo);

        Button fechar = new Button(this);
        fechar.setText("X");
        fechar.setTextSize(11);
        fechar.setPadding(dp(10), 0, dp(10), 0);
        fechar.setOnClickListener(v -> parar());
        cabecalho.addView(fechar);

        caixa.addView(cabecalho);

        // sensi
        LinearLayout linhaSensi = new LinearLayout(this);
        linhaSensi.setOrientation(LinearLayout.HORIZONTAL);
        linhaSensi.setGravity(Gravity.CENTER_VERTICAL);
        linhaSensi.setPadding(0, dp(8), 0, 0);

        TextView rotulo = new TextView(this);
        rotulo.setText("Sensi");
        rotulo.setTextColor(Color.parseColor("#A0AA98"));
        rotulo.setTextSize(12);
        linhaSensi.addView(rotulo);

        Button menos = new Button(this);
        menos.setText("−");
        menos.setOnClickListener(v -> mudarSensi(-5));
        linhaSensi.addView(menos);

        valorSensi = new TextView(this);
        valorSensi.setText(String.valueOf(prefs.getInt("sensi_geral", 165)));
        valorSensi.setTextColor(Color.parseColor("#F1F4EB"));
        valorSensi.setTextSize(16);
        valorSensi.setPadding(dp(10), 0, dp(10), 0);
        linhaSensi.addView(valorSensi);

        Button mais = new Button(this);
        mais.setText("+");
        mais.setOnClickListener(v -> mudarSensi(5));
        linhaSensi.addView(mais);

        caixa.addView(linhaSensi);

        // ações
        botaoGravacao = new Button(this);
        botaoGravacao.setText("Gravar");
        botaoGravacao.setOnClickListener(v -> alternarGravacao());
        caixa.addView(botaoGravacao);

        Button jogo = new Button(this);
        jogo.setText("Abrir Free Fire");
        jogo.setOnClickListener(v -> abrirJogo());
        caixa.addView(jogo);

        painel = caixa;

        int tipo = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                tipo,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(12);
        params.y = dp(160);

        cabecalho.setOnTouchListener(new View.OnTouchListener() {
            private int baseX, baseY;
            private float inicioX, inicioY;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    baseX = params.x;
                    baseY = params.y;
                    inicioX = e.getRawX();
                    inicioY = e.getRawY();
                    return true;
                }
                if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    params.x = baseX + (int) (e.getRawX() - inicioX);
                    params.y = baseY + (int) (e.getRawY() - inicioY);
                    janela.updateViewLayout(painel, params);
                    return true;
                }
                return false;
            }
        });

        janela.addView(painel, params);
    }

    private void mudarSensi(int passo) {
        int atual = prefs.getInt("sensi_geral", 165);
        int novo = Math.max(0, Math.min(200, atual + passo));
        prefs.edit().putInt("sensi_geral", novo).apply();
        valorSensi.setText(String.valueOf(novo));
    }

    private void alternarGravacao() {
        if (ScreenRecordService.gravando) {
            ScreenRecordService.parar(this);
            botaoGravacao.setText("Gravar");
            return;
        }
        if (ScreenRecordService.codigo != 0 && ScreenRecordService.dados != null) {
            try {
                ScreenRecordService.iniciar(this, ScreenRecordService.codigo, ScreenRecordService.dados);
                botaoGravacao.setText("Parar");
                return;
            } catch (Exception e) {
                Toast.makeText(this, "Autorize a gravação novamente", Toast.LENGTH_SHORT).show();
            }
        }
        Intent abrir = new Intent(this, MainActivity.class);
        abrir.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(abrir);
        Toast.makeText(this, "Toque em Gravar tela para autorizar", Toast.LENGTH_LONG).show();
    }

    private void abrirJogo() {
        Intent i = getPackageManager().getLaunchIntentForPackage("com.dts.freefireth");
        if (i == null) i = getPackageManager().getLaunchIntentForPackage("com.dts.freefiremax");
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }
    }

    private void parar() {
        if (painel != null) {
            janela.removeView(painel);
            painel = null;
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (painel != null) {
            janela.removeView(painel);
            painel = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int dp(int valor) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valor,
                getResources().getDisplayMetrics());
    }
}