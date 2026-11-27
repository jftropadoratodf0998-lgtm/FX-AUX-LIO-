Projeto Android — SensiLab FX
================================

App Android NATIVO (sem WebView): sensibilidade, painel flutuante sobre outros
apps, gravação de tela em MP4, velocidade do toque, DPI, atalho de ADB e botão
que abre o Free Fire.

SEM PC: GERAR O APK PELO GITHUB (grátis, uns 5 minutos)
-------------------------------------------------------
Esta pasta já vem com o build automático pronto (.github/workflows/apk.yml).

1. No navegador do celular, entre em github.com e crie um repositório novo
   (pode ser privado).
2. Baixe o .zip no site do SensiLab, extraia com o app "Arquivos" do Android e
   entre na pasta SensiLabFX.
3. No repositório: Add file > Upload files > envie TODO o conteúdo da pasta
   (incluindo a pasta .github).
4. Abra a aba "Actions": o build "Gerar APK" começa sozinho e leva ~4 minutos.
5. Abra o build concluído e baixe o arquivo "SensiLabFX-apk" no celular.
6. Descompacte esse arquivo e instale o app-debug.apk (autorize "instalar apps
   desconhecidos" quando o Android pedir).

COM PC (Android Studio)
-----------------------
1. Instale o Android Studio (grátis): developer.android.com/studio
2. Android Studio > File > Open > selecione a pasta SensiLabFX.
3. Espere o Gradle sincronizar.
4. Build > Build Bundle(s) / APK(s) > Build APK(s).
5. O APK fica em app/build/outputs/apk/debug/app-debug.apk

Para um APK assinado (Play Store ou instalar em qualquer aparelho):
   Build > Generate Signed Bundle / APK > APK > crie sua chave > release.

O QUE O APP PEDE E O QUE CADA PERMISSÃO FAZ
-------------------------------------------
- Sobrepor outros apps: painel em cima do Free Fire.
- Notificações: mantém o painel e a gravação ativos em segundo plano.
- Gravação de tela: pedida na hora, ao tocar em "Gravar tela".
- Alterar configurações do sistema: muda a velocidade do toque do sistema.
  Caminho: Ajustes > Apps > SensiLab FX > Alterar configurações do sistema.
- DPI: o Android só deixa mudar com permissão segura, que não aparece nos
  ajustes normais. Se o app não conseguir, ele copia o comando
  "adb shell wm density 400" (e "adb shell wm density reset" para voltar).
- ADB: não existe permissão de ADB para apps comuns. O app abre as opções do
  desenvolvedor e copia os comandos; a conexão é feita com "adb pair" e
  "adb connect".

ONDE FICAM OS CLIPES
--------------------
Android/data/com.sensilab.ff/files/Movies/SensiLab/
O caminho completo aparece na tela principal depois de gravar.

AVISO
-----
O app não altera arquivos do Free Fire, nem FPS, nem conexão, e não é banido
por ninguém: nada aqui injeta código no jogo. Os valores de sensibilidade são
sugestões para você digitar no jogo. Usar app de terceiros com o Free Fire é
por sua conta e risco, respeitando os termos da Garena.