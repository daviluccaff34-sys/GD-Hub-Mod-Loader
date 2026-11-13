package com.gdhub.loader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import java.io.*;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final int PICK_PACKAGE=41, BG=0xFF0B0D18, PANEL=0xFF171A2B, PURPLE=0xFF895CFF, TEXT=0xFFF5F3FF, MUTED=0xFFA7A8C0;
    private LinearLayout list;
    private TextView status;
    private final ArrayList<File> packages=new ArrayList<>();
    static { System.loadLibrary("gdhub_loader"); }
    private native String nativeRunPackage(String absolutePath);
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable box(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private Button button(String text,int color){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(14);b.setTypeface(Typeface.DEFAULT_BOLD);b.setTextColor(Color.WHITE);b.setBackground(box(color,13));b.setMinHeight(0);b.setMinimumHeight(0);return b;}
    public void onCreate(Bundle state){super.onCreate(state);buildUi();scanPackages();}
    private void buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(22),dp(24),dp(22),dp(22));scroll.addView(root);
        LinearLayout brand=new LinearLayout(this);brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo=new TextView(this);logo.setText("GD");logo.setTextColor(-1);logo.setTextSize(21);logo.setTypeface(null,Typeface.BOLD);logo.setGravity(Gravity.CENTER);logo.setBackground(box(PURPLE,17));brand.addView(logo,new LinearLayout.LayoutParams(dp(58),dp(58)));
        LinearLayout names=new LinearLayout(this);names.setOrientation(1);names.setPadding(dp(14),0,0,0);
        TextView title=new TextView(this);title.setText("GD HUB");title.setTextColor(TEXT);title.setTextSize(27);title.setTypeface(null,Typeface.BOLD);names.addView(title);
        TextView sub=new TextView(this);sub.setText("MOD MANAGER  •  ANDROID");sub.setTextColor(MUTED);sub.setTextSize(11);sub.setLetterSpacing(.08f);names.addView(sub);brand.addView(names);root.addView(brand,new LinearLayout.LayoutParams(-1,dp(68)));
        TextView headline=new TextView(this);headline.setText("Seus pacotes. Seu controle.");headline.setTextColor(TEXT);headline.setTextSize(19);headline.setTypeface(null,Typeface.BOLD);headline.setPadding(0,dp(28),0,dp(5));root.addView(headline);
        TextView hint=new TextView(this);hint.setText("Importe e execute pacotes .GDHB compatíveis.");hint.setTextColor(MUTED);hint.setTextSize(13);root.addView(hint);
        Button launch=button("▶  INICIAR JOGO COM MOD LOADER",0xFF6842D8);
        launch.setTextSize(14);
        LinearLayout.LayoutParams launchParams=new LinearLayout.LayoutParams(-1,dp(54));
        launchParams.topMargin=dp(22);
        launchParams.bottomMargin=dp(10);
        root.addView(launch,launchParams);
        launch.setOnClickListener(v->launchGeometryDash());

        LinearLayout actions=new LinearLayout(this);actions.setPadding(0,dp(8),0,dp(18));
        Button add=button("+  Importar pacote",PURPLE);add.setOnClickListener(v->pickPackage());actions.addView(add,new LinearLayout.LayoutParams(0,dp(52),1));
        Button refresh=button("↻",PANEL);refresh.setOnClickListener(v->scanPackages());LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(dp(58),dp(52));rp.leftMargin=dp(9);actions.addView(refresh,rp);root.addView(actions);
        LinearLayout summary=new LinearLayout(this);summary.setOrientation(1);summary.setPadding(dp(16),dp(13),dp(16),dp(13));summary.setBackground(box(PANEL,14));
        TextView label=new TextView(this);label.setText("BIBLIOTECA DE PACOTES");label.setTextColor(MUTED);label.setTextSize(11);label.setTypeface(null,Typeface.BOLD);summary.addView(label);
        status=new TextView(this);status.setTextColor(TEXT);status.setTextSize(14);status.setPadding(0,dp(6),0,0);summary.addView(status);root.addView(summary,new LinearLayout.LayoutParams(-1,-2));
        list=new LinearLayout(this);list.setOrientation(1);list.setPadding(0,dp(14),0,0);root.addView(list);
        TextView footer=new TextView(this);footer.setText("GD HUB  •  ARM32 + ARM64  •  v0.1.0-alpha");footer.setTextColor(0xFF666982);footer.setTextSize(10);footer.setGravity(Gravity.CENTER);footer.setPadding(0,dp(28),0,dp(12));root.addView(footer);
        setContentView(scroll);
    }
    private void pickPackage(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,PICK_PACKAGE);}
    protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=PICK_PACKAGE||result!=RESULT_OK||data==null)return;Uri uri=data.getData();if(uri==null)return;
        File dir=new File(getFilesDir(),"mods");if(!dir.exists()&&!dir.mkdirs()){status.setText("Não foi possível criar a pasta.");return;}
        File dest=new File(dir,"imported_"+System.currentTimeMillis()+".gdhb");
        try(InputStream in=getContentResolver().openInputStream(uri);FileOutputStream out=new FileOutputStream(dest)){if(in==null)throw new IllegalStateException("Arquivo não pôde ser aberto.");byte[] buf=new byte[8192];int n;long total=0;while((n=in.read(buf))!=-1){total+=n;if(total>16L*1024*1024)throw new IllegalArgumentException("Pacote maior que 16 MiB.");out.write(buf,0,n);}scanPackages();status.setText("Pacote importado!");}catch(Exception e){dest.delete();status.setText("Falha ao importar: "+e.getMessage());}}
    private void scanPackages(){list.removeAllViews();packages.clear();File dir=new File(getFilesDir(),"mods");File[] files=dir.listFiles((d,n)->n.toLowerCase().endsWith(".gdhb"));if(files!=null)for(File f:files)packages.add(f);
        status.setText(packages.size()+(packages.size()==1?" pacote instalado":" pacotes instalados"));
        if(packages.isEmpty()){TextView empty=new TextView(this);empty.setText("Ainda está vazio por aqui.\nToque em “Importar pacote” para começar.");empty.setTextColor(MUTED);empty.setTextSize(13);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(16),dp(24),dp(16),dp(24));empty.setBackground(box(PANEL,14));list.addView(empty);return;}
        for(File file:packages){LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(15),dp(13),dp(15),dp(13));card.setBackground(box(PANEL,14));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(10);list.addView(card,cp);
            TextView name=new TextView(this);name.setText("▣  "+file.getName());name.setTextColor(TEXT);name.setTextSize(14);name.setTypeface(null,Typeface.BOLD);card.addView(name);
            TextView meta=new TextView(this);meta.setText("PACOTE GDHB  •  ARMAZENADO NO APP");meta.setTextColor(MUTED);meta.setTextSize(10);meta.setPadding(0,dp(5),0,dp(12));card.addView(meta);
            LinearLayout row=new LinearLayout(this);Button run=button("Validar e executar",PURPLE);run.setOnClickListener(v->runPackage(file));row.addView(run,new LinearLayout.LayoutParams(0,dp(44),1));
            Button del=button("Remover",0xFF30202D);del.setTextColor(0xFFFFA6B5);del.setOnClickListener(v->{if(file.delete())scanPackages();else status.setText("Não foi possível remover.");});LinearLayout.LayoutParams deleteParams=new LinearLayout.LayoutParams(dp(100),dp(44));deleteParams.leftMargin=dp(8);row.addView(del,deleteParams);card.addView(row);
        }}
    private void launchGeometryDash(){
        final String gamePackage="com.robtopx.geometryjump";
        try{
            Intent launch=getPackageManager().getLaunchIntentForPackage(gamePackage);
            if(launch==null){
                status.setText("Geometry Dash não encontrado. Instale o jogo oficial primeiro.");
                return;
            }
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            status.setText("Abrindo Geometry Dash… O botão ainda não injeta o loader: a integração dentro do jogo está em desenvolvimento.");
            startActivity(launch);
        }catch(Exception e){
            status.setText("Não foi possível iniciar o Geometry Dash: "+e.getMessage());
        }
    }
    private void runPackage(File file){try{status.setText(nativeRunPackage(file.getAbsolutePath()));}catch(Throwable e){status.setText("Erro no runtime nativo: "+e.getMessage());}}
}