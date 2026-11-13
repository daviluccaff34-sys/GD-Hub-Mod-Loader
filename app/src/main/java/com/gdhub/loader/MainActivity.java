package com.gdhub.loader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_PACKAGE=41, PICK_PUBLISH_PACKAGE=42;
    private static final int BG=0xFF0B0D18, PANEL=0xFF171A2B, PURPLE=0xFF895CFF, TEXT=0xFFF5F3FF, MUTED=0xFFA7A8C0;
    private static final long MAX_PACKAGE_BYTES=16L*1024*1024;
    private static final ExecutorService NETWORK=Executors.newSingleThreadExecutor();
    private LinearLayout list;
    private TextView status;
    private TextView publishStatus, selectedPackageLabel;
    private EditText publishName, publishVersion, publishAuthor, publishDescription;
    private File selectedPublishFile;
    private View managerPage, publishPage;
    private Button managerTab, publishTab, launchButton;
    private final ArrayList<File> packages=new ArrayList<>();
    private SharedPreferences modPrefs;
    private static final String PREFS_MODS="gdhub_mod_states";
    private static final String PREFS_DRAFT="gdhub_publish_draft";

    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable box(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private Button button(String text,int color){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(13);b.setTypeface(Typeface.DEFAULT_BOLD);b.setTextColor(Color.WHITE);b.setBackground(box(color,13));b.setMinHeight(0);b.setMinimumHeight(0);return b;}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextColor(color);t.setTextSize(size);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private EditText field(String hint,boolean multiline){EditText e=new EditText(this);e.setSingleLine(!multiline);e.setTextSize(14);e.setTextColor(TEXT);e.setHintTextColor(MUTED);e.setHint(hint);e.setPadding(dp(14),dp(11),dp(14),dp(11));e.setBackground(box(PANEL,12));if(multiline){e.setMinLines(3);e.setGravity(Gravity.TOP|Gravity.START);}return e;}

    public void onCreate(Bundle state){
        super.onCreate(state);
        modPrefs=getSharedPreferences(PREFS_MODS,MODE_PRIVATE);
        buildUi();
        scanPackages();
    }

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(dp(18),dp(20),dp(18),dp(12));
        root.setBackgroundColor(BG);
        LinearLayout brand=new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo=text("GD",21,Color.WHITE,true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(box(PURPLE,17));
        brand.addView(logo,new LinearLayout.LayoutParams(dp(54),dp(54)));
        LinearLayout names=new LinearLayout(this);
        names.setOrientation(1); names.setPadding(dp(13),0,0,0);
        names.addView(text("GD HUB",25,TEXT,true));
        names.addView(text("MOD MANAGER  •  ANDROID",10,MUTED,true));
        brand.addView(names);
        root.addView(brand,new LinearLayout.LayoutParams(-1,dp(62)));
        managerPage=buildManagerPage();
        root.addView(managerPage,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }
    private View buildManagerPage(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setPadding(0,dp(8),0,dp(10));scroll.addView(page);
        page.addView(text("Seus pacotes. Seu controle.",19,TEXT,true));
        TextView hint=text("Importe e execute pacotes .GDHB compatíveis.",12,MUTED,false);hint.setPadding(0,dp(5),0,dp(12));page.addView(hint);
        launchButton=button("▶  INICIAR JOGO COM MOD LOADER",0xFF6842D8);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.bottomMargin=dp(10);page.addView(launchButton,lp);launchButton.setOnClickListener(v->launchGeometryDash());
        LinearLayout actions=new LinearLayout(this);
        Button add=button("+  Importar pacote",PURPLE);add.setOnClickListener(v->pickPackage());actions.addView(add,new LinearLayout.LayoutParams(0,dp(48),1));
        Button refresh=button("↻",PANEL);refresh.setOnClickListener(v->scanPackages());LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(dp(56),dp(48));rp.leftMargin=dp(8);actions.addView(refresh,rp);page.addView(actions);
        LinearLayout summary=new LinearLayout(this);summary.setOrientation(1);summary.setPadding(dp(15),dp(12),dp(15),dp(12));summary.setBackground(box(PANEL,14));
        summary.addView(text("BIBLIOTECA DE PACOTES",10,MUTED,true));
        status=text("Carregando…",13,TEXT,false);status.setPadding(0,dp(5),0,0);summary.addView(status);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(10);page.addView(summary,sp);
        list=new LinearLayout(this);list.setOrientation(1);list.setPadding(0,dp(12),0,0);page.addView(list);
        TextView footer=text("GD HUB  •  ARM32 + ARM64  •  v0.2.0-alpha",10,0xFF666982,false);footer.setGravity(Gravity.CENTER);footer.setPadding(0,dp(18),0,dp(8));page.addView(footer);
        return scroll;
    }

    private View buildPublishPage(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setPadding(0,dp(8),0,dp(18));scroll.addView(page);
        page.addView(text("PUBLICAR UM MOD",20,TEXT,true));
        TextView intro=text("Envie seu pacote GDHB para entrar no catálogo da comunidade. Os envios ficam pendentes de revisão antes de aparecer para todos.",12,MUTED,false);intro.setPadding(0,dp(6),0,dp(16));page.addView(intro);
        Button choose=button("＋  ESCOLHER ARQUIVO .GDHB",PURPLE);page.addView(choose,new LinearLayout.LayoutParams(-1,dp(48)));choose.setOnClickListener(v->pickPublishPackage());
        selectedPackageLabel=text("Nenhum pacote selecionado",11,MUTED,false);selectedPackageLabel.setPadding(dp(4),dp(8),dp(4),dp(14));page.addView(selectedPackageLabel);
        page.addView(text("NOME DO MOD",10,MUTED,true));publishName=field("Ex.: Better Practice Mode",false);addField(page,publishName);
        page.addView(text("VERSÃO",10,MUTED,true));publishVersion=field("Ex.: 1.0.0",false);addField(page,publishVersion);
        page.addView(text("AUTOR",10,MUTED,true));publishAuthor=field("Seu nome ou apelido de criador",false);addField(page,publishAuthor);
        page.addView(text("DESCRIÇÃO",10,MUTED,true));publishDescription=field("O que seu mod adiciona?",true);addField(page,publishDescription);
        Button publish=button("PUBLICAR NO CATÁLOGO",0xFF6842D8);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(52));pp.topMargin=dp(7);page.addView(publish,pp);publish.setOnClickListener(v->publishOrSaveDraft());
        publishStatus=text("O catálogo online ainda precisa ser conectado.",11,MUTED,false);publishStatus.setPadding(dp(3),dp(10),dp(3),0);page.addView(publishStatus);
        return scroll;
    }

    private void addField(LinearLayout page,EditText edit){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(5);p.bottomMargin=dp(12);page.addView(edit,p);}

    private void pickPackage(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,PICK_PACKAGE);}
    private void pickPublishPackage(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,PICK_PUBLISH_PACKAGE);}

    protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        if(request==PICK_PACKAGE){importManagerPackage(data.getData());}
        else if(request==PICK_PUBLISH_PACKAGE){importPublishPackage(data.getData());}
    }

    private String displayName(Uri uri){
        try(android.database.Cursor cursor=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){
            if(cursor!=null&&cursor.moveToFirst()){String name=cursor.getString(0);if(name!=null)return name;}
        }catch(Exception ignored){}
        return "package_"+System.currentTimeMillis()+".gdhb";
    }

    private File copyPackage(Uri uri,File directory) throws Exception {
        if(!directory.exists()&&!directory.mkdirs())throw new IOException("Não foi possível criar a pasta.");
        String name=displayName(uri).replaceAll("[^A-Za-z0-9._-]","_");
        if(!name.toLowerCase().endsWith(".gdhb"))throw new IllegalArgumentException("Escolha um arquivo .gdhb.");
        File dest=new File(directory,name);
        if(dest.exists())dest=new File(directory,System.currentTimeMillis()+"_"+name);
        try(InputStream in=getContentResolver().openInputStream(uri);FileOutputStream out=new FileOutputStream(dest)){
            if(in==null)throw new IOException("Arquivo não pôde ser aberto.");
            byte[] buf=new byte[8192];int n;long total=0;
            while((n=in.read(buf))!=-1){total+=n;if(total>MAX_PACKAGE_BYTES)throw new IllegalArgumentException("Pacote maior que 16 MiB.");out.write(buf,0,n);}
        }catch(Exception e){dest.delete();throw e;}
        if(!isGdhb(dest)){dest.delete();throw new IllegalArgumentException("Arquivo inválido: cabeçalho GDHB não encontrado.");}
        return dest;
    }

    private boolean isGdhb(File file){
        try(InputStream in=new FileInputStream(file)){return in.read()=='G'&&in.read()=='D'&&in.read()=='H'&&in.read()=='B';}
        catch(Exception e){return false;}
    }

    private void importManagerPackage(Uri uri){
        try{copyPackage(uri,new File(getFilesDir(),"mods"));scanPackages();status.setText("Pacote importado!");}
        catch(Exception e){status.setText("Falha ao importar: "+e.getMessage());}
    }

    private void importPublishPackage(Uri uri){
        try{
            selectedPublishFile=copyPackage(uri,new File(getFilesDir(),"publish_staging"));
            selectedPackageLabel.setText("✓  "+selectedPublishFile.getName()+"  •  "+(selectedPublishFile.length()/1024)+" KB");
            publishStatus.setText("Pacote validado. Preencha os dados para publicar.");
            saveDraft();
        }catch(Exception e){publishStatus.setText("Falha ao selecionar: "+e.getMessage());}
    }

    private void restorePublishDraft(){
        SharedPreferences p=getSharedPreferences(PREFS_DRAFT,MODE_PRIVATE);
        if(publishName!=null)publishName.setText(p.getString("name",""));
        if(publishVersion!=null)publishVersion.setText(p.getString("version",""));
        if(publishAuthor!=null)publishAuthor.setText(p.getString("author",""));
        if(publishDescription!=null)publishDescription.setText(p.getString("description",""));
        String path=p.getString("file","");
        if(path!=null&&!path.isEmpty()){File f=new File(path);if(f.isFile()&&isGdhb(f)){selectedPublishFile=f;selectedPackageLabel.setText("✓  "+f.getName()+"  •  "+(f.length()/1024)+" KB");}}
    }

    private void saveDraft(){
        getSharedPreferences(PREFS_DRAFT,MODE_PRIVATE).edit()
            .putString("name",publishName.getText().toString().trim())
            .putString("version",publishVersion.getText().toString().trim())
            .putString("author",publishAuthor.getText().toString().trim())
            .putString("description",publishDescription.getText().toString().trim())
            .putString("file",selectedPublishFile==null?"":selectedPublishFile.getAbsolutePath()).apply();
    }

    private void publishOrSaveDraft(){
        String name=publishName.getText().toString().trim(), version=publishVersion.getText().toString().trim();
        String author=publishAuthor.getText().toString().trim(), description=publishDescription.getText().toString().trim();
        if(selectedPublishFile==null||!selectedPublishFile.isFile()){publishStatus.setText("Escolha primeiro um pacote .gdhb.");return;}
        if(name.isEmpty()||version.isEmpty()||author.isEmpty()||description.isEmpty()){publishStatus.setText("Preencha nome, versão, autor e descrição.");return;}
        saveDraft();
        String endpoint=BuildConfig.GDHUB_PUBLISH_API.trim();
        if(endpoint.isEmpty()){
            publishStatus.setText("Rascunho salvo! O servidor público ainda não está conectado. Nenhum mod foi enviado.");
            return;
        }
        publishStatus.setText("Enviando pacote…");
        setPublishControlsEnabled(false);
        NETWORK.execute(()->{
            String result;
            try{result=uploadMod(endpoint,selectedPublishFile,name,version,author,description);}
            catch(Exception e){result="Falha no envio: "+e.getMessage();}
            final String message=result;
            runOnUiThread(()->{publishStatus.setText(message);setPublishControlsEnabled(true);});
        });
    }

    private void setPublishControlsEnabled(boolean enabled){
        publishName.setEnabled(enabled);publishVersion.setEnabled(enabled);publishAuthor.setEnabled(enabled);publishDescription.setEnabled(enabled);
    }

    private String uploadMod(String endpoint,File file,String name,String version,String author,String description) throws Exception {
        String base=endpoint.endsWith("/")?endpoint.substring(0,endpoint.length()-1):endpoint;
        String boundary="----GDHub"+UUID.randomUUID().toString().replace("-","");
        HttpURLConnection connection=(HttpURLConnection)new URL(base+"/v1/mods").openConnection();
        connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setConnectTimeout(15000);connection.setReadTimeout(30000);
        connection.setRequestProperty("Content-Type","multipart/form-data; boundary="+boundary);
        connection.setRequestProperty("Accept","application/json");
        try(OutputStream out=connection.getOutputStream()){
            writePart(out,boundary,"name",name);writePart(out,boundary,"version",version);
            writePart(out,boundary,"author",author);writePart(out,boundary,"description",description);
            String fileHeader="--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\""+file.getName()+"\"\r\nContent-Type: application/octet-stream\r\n\r\n";
            out.write(fileHeader.getBytes(StandardCharsets.UTF_8));
            try(InputStream in=new FileInputStream(file)){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);}
            out.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
        }
        int code=connection.getResponseCode();
        InputStream response=code>=200&&code<400?connection.getInputStream():connection.getErrorStream();
        String body="";
        if(response!=null)try(InputStream in=response;ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)bytes.write(b,0,n);body=new String(bytes.toByteArray(),StandardCharsets.UTF_8);}
        connection.disconnect();
        if(code==201){JSONObject json=new JSONObject(body);return "Enviado para revisão! ID: "+json.optString("id","recebido")+". Ainda não aparece no catálogo público.";}
        return "Servidor respondeu HTTP "+code+": "+(body.length()>220?body.substring(0,220):body);
    }

    private void writePart(OutputStream out,String boundary,String name,String value) throws IOException {
        String part="--"+boundary+"\r\nContent-Disposition: form-data; name=\""+name+"\"\r\n\r\n"+value+"\r\n";
        out.write(part.getBytes(StandardCharsets.UTF_8));
    }

    private void scanPackages(){
        list.removeAllViews();packages.clear();File dir=new File(getFilesDir(),"mods");
        File[] files=dir.listFiles((d,n)->n.toLowerCase().endsWith(".gdhb"));
        if(files!=null)for(File f:files)packages.add(f);
        status.setText(packages.size()+(packages.size()==1?" pacote instalado":" pacotes instalados"));
        if(packages.isEmpty()){TextView empty=text("Ainda está vazio por aqui.\nToque em “Importar pacote” para começar.",12,MUTED,false);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(16),dp(24),dp(16),dp(24));empty.setBackground(box(PANEL,14));list.addView(empty);return;}
        for(File file:packages){
            LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(box(PANEL,14));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(9);list.addView(card,cp);
            card.addView(text("▣  "+file.getName(),13,TEXT,true));
            TextView meta=text("PACOTE GDHB  •  ARMAZENADO NO APP",9,MUTED,false);meta.setPadding(0,dp(5),0,dp(10));card.addView(meta);
            Switch enabled=new Switch(this);enabled.setText("Ativado no gerenciador");enabled.setTextColor(TEXT);enabled.setTextSize(11);enabled.setChecked(modPrefs.getBoolean(file.getName(),false));
            enabled.setOnCheckedChangeListener((button,checked)->modPrefs.edit().putBoolean(file.getName(),checked).apply());card.addView(enabled);
            LinearLayout row=new LinearLayout(this);Button run=button("Validar e executar",PURPLE);run.setOnClickListener(v->runPackage(file));row.addView(run,new LinearLayout.LayoutParams(0,dp(42),1));
            Button del=button("Remover",0xFF30202D);del.setTextColor(0xFFFFA6B5);del.setOnClickListener(v->{if(file.delete())scanPackages();else status.setText("Não foi possível remover.");});
            LinearLayout.LayoutParams deleteParams=new LinearLayout.LayoutParams(dp(92),dp(42));deleteParams.leftMargin=dp(8);row.addView(del,deleteParams);card.addView(row);
        }
    }

    private void launchGeometryDash(){
        if(launchButton!=null) launchButton.setEnabled(false);
        status.setText("Verificando Geometry Dash…");
        GameInstallProbe.Result game=GameInstallProbe.inspect(this);
        String failure=null;
        if(!game.installed) {
            failure="Geometry Dash não está instalado. Instale a versão oficial e tente novamente.";
        } else if(game.versionCode!=41L) {
            failure="Esta versão do GD Hub foi preparada para Geometry Dash 2.2081 (versionCode 41).\nVersão detectada: "+game.unifiedVersion+" (code "+game.versionCode+").";
        } else {
            String abi=android.os.Process.is64Bit()?"arm64-v8a":"armeabi-v7a";
            if(!GameInstallProbe.hasGameLibraries(game,abi)) {
                failure="O APK instalado não contém as bibliotecas nativas do Geometry Dash para "+abi+".\nABIs detectadas: "+android.text.TextUtils.join(", ",game.supportedAbis);
            }
        }
        if(failure!=null) {
            status.setText(failure);
            if(launchButton!=null) launchButton.setEnabled(true);
            new android.app.AlertDialog.Builder(this).setTitle("Não foi possível iniciar").setMessage(failure)
                .setPositiveButton("OK",null).show();
            return;
        }
        status.setText("Abrindo o host interno do GD Hub…");
        try {
            Intent intent=new Intent(this,GeometryDashActivity.class);
            intent.putExtra("game_version_code",game.versionCode);
            startActivity(intent);
        } catch (Throwable error) {
            String message="Falha ao abrir o launcher interno: "+(error.getMessage()==null?error.getClass().getSimpleName():error.getMessage());
            status.setText(message);
            new android.app.AlertDialog.Builder(this).setTitle("Erro ao iniciar").setMessage(message)
                .setPositiveButton("OK",null).show();
            if(launchButton!=null) launchButton.setEnabled(true);
        }
    }

    @Override protected void onResume(){
        super.onResume();
        if(launchButton!=null) launchButton.setEnabled(true);
        if(list!=null) scanPackages();
        showPreviousCrashDiagnostic();
    }

    private void showPreviousCrashDiagnostic(){
        if(android.os.Build.VERSION.SDK_INT<30 || status==null) return;
        try{
            android.app.ActivityManager manager=(android.app.ActivityManager)getSystemService(ACTIVITY_SERVICE);
            java.util.List<android.app.ApplicationExitInfo> exits=manager.getHistoricalProcessExitReasons(getPackageName(),0,5);
            if(exits==null||exits.isEmpty()) return;
            android.app.ApplicationExitInfo exit=exits.get(0);
            int reason=exit.getReason();
            if(reason!=android.app.ApplicationExitInfo.REASON_CRASH_NATIVE &&
               reason!=android.app.ApplicationExitInfo.REASON_CRASH &&
               reason!=android.app.ApplicationExitInfo.REASON_ANR) return;
            SharedPreferences diag=getSharedPreferences("gdhub_boot_diagnostics",MODE_PRIVATE);
            long stageTime=diag.getLong("time",0L);
            if(exit.getTimestamp()<stageTime) return;
            String stage=diag.getString("stage","etapa desconhecida");
            String detail=exit.getDescription();
            String report="Falha anterior detectada. Etapa: "+stage+
                "\\nMotivo Android: "+reason+
                "\\nDetalhe: "+(detail==null||detail.isEmpty()?"não informado":detail);
            status.setText(report);
        }catch(Throwable ignored){}
    }

    private void runPackage(File file){try{status.setText(GdHubRuntime.nativeRunPackage(file.getAbsolutePath()));}catch(Throwable e){status.setText("Erro no runtime nativo: "+e.getMessage());}}
}
