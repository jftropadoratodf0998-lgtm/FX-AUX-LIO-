package com.sensilab.ff;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Gravação de tela (clipes em MP4) em primeiro plano.
 */
public class ScreenRecordService extends Service {

    public static boolean gravando = false;
    public static String ultimoArquivo = "nenhum ainda";
    public static int codigo = 0;
    public static Intent dados = null;

    private static final String CANAL = "fx_gravacao";
    private static final int ID_NOTIFICACAO = 8;

    private MediaProjection projecao;
    private MediaRecorder gravador;
    private VirtualDisplay tela;
    private File arquivo;
    private boolean finalizando = false;

    public static void iniciar(Context ctx, int codigoResultado, Intent dadosResultado) {
        codigo = codigoResultado;
        dados = dadosResultado;
        Intent i = new Intent(ctx, ScreenRecordService.class);
        i.putExtra("codigo", codigoResultado);
        i.putExtra("dados", dadosResultado);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i);
        else ctx.startService(i);
    }

    public static void parar(Context ctx) {
        ctx.startService(new Intent(ctx, ScreenRecordService.class).setAction("PARAR"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "PARAR".equals(intent.getAction())) {
            encerrar();
            return START_NOT_STICKY;
        }
        if (gravando) return START_NOT_STICKY;

        subirNotificacao();

        int codigoRecebido = intent != null ? intent.getIntExtra("codigo", 0) : 0;
        Intent dadosRecebidos = intent != null
                ? (Intent) intent.getParcelableExtra("dados")
                : null;

        if (codigoRecebido == 0 || dadosRecebidos == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            comecar(codigoRecebido, dadosRecebidos);
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível gravar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            encerrar();
        }
        return START_NOT_STICKY;
    }

    private void subirNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(CANAL, "Gravação de tela",
                    NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(canal);
        }

        Intent abrir = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, abrir,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CANAL)
                : new Notification.Builder(this);

        Notification n = b.setContentTitle("Gravando a tela")
                .setContentText("Pare no painel para salvar o clipe")
                .setSmallIcon(android.R.drawable.presence_video_online)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(ID_NOTIFICACAO, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(ID_NOTIFICACAO, n);
        }
    }

    private void comecar(int codigoRecebido, Intent dadosRecebidos) throws Exception {
        DisplayMetrics metricas = new DisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(metricas);

        int largura = metricas.widthPixels % 2 == 0 ? metricas.widthPixels : metricas.widthPixels - 1;
        int altura = metricas.heightPixels % 2 == 0 ? metricas.heightPixels : metricas.heightPixels - 1;

        File pasta = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "SensiLab");
        if (!pasta.exists()) pasta.mkdirs();

        String nome = "clipe_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".mp4";
        arquivo = new File(pasta, nome);

        gravador = new MediaRecorder();
        gravador.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        gravador.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        gravador.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        gravador.setVideoSize(largura, altura);
        gravador.setVideoFrameRate(30);
        gravador.setVideoEncodingBitRate(6000000);
        gravador.setOutputFile(arquivo.getAbsolutePath());
        gravador.prepare();

        MediaProjectionManager gerenciador = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        projecao = gerenciador.getMediaProjection(codigoRecebido, dadosRecebidos);

        if (projecao == null) throw new IllegalStateException("permissão expirada");

        projecao.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                encerrar();
            }
        }, null);

        tela = projecao.createVirtualDisplay("fx-tela", largura, altura, metricas.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, gravador.getSurface(), null, null);

        gravador.start();
        gravando = true;
        ultimoArquivo = arquivo.getAbsolutePath();
    }

    private void encerrar() {
        if (finalizando) return;
        finalizando = true;
        gravando = false;

        try {
            if (gravador != null) gravador.stop();
        } catch (Exception ignored) {
        }

        if (gravador != null) {
            gravador.reset();
            gravador.release();
            gravador = null;
        }
        if (tela != null) {
            tela.release();
            tela = null;
        }
        if (projecao != null) {
            projecao.stop();
            projecao = null;
        }

        if (arquivo != null) ultimoArquivo = arquivo.getAbsolutePath();
        stopForeground(true);
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}