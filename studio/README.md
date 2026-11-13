# GD Hub Mod Studio

Site estático para Cloudflare Pages. Não precisa de build command.

## Publicar no Cloudflare Pages

1. No painel Cloudflare, abra **Workers & Pages** → **Create** → **Pages** → **Connect to Git**.
2. Selecione o repositório `daviluccaff34-sys/GD-Hub-Mod-Loader`.
3. Em **Root directory (advanced)**, informe `studio`.
4. Framework preset: **None**.
5. Build command: deixe vazio.
6. Build output directory: `.`.
7. Clique em **Save and Deploy**.

O arquivo `index.html` contém a interface e o empacotador. A geração ocorre localmente no navegador: os arquivos do usuário não são enviados a um servidor.

## O que funciona

- Formulário de manifesto.
- Modelo básico e modelo More Icons.
- Editor do arquivo `src/main.dash`.
- Inclusão de imagens PNG, WebP e JPEG em `assets/icons/`.
- Geração de pacote binário GDHB v1, compatível com o formato documentado em `docs/FORMAT_GDHB.md`.
- Validação de limites e download do `.gdhb`.

## Limitação importante

O Studio **empacota** fontes e recursos; não compila código nativo nem cria APK. O modelo More Icons é um starter package. A funcionalidade de combinar ícones visualmente ainda exige implementação do sistema de ícones no runtime Android do GD Hub.


## GD Hub Mod Creator

Abra `mod-creator.html` para usar o **GD Hub Mod Creator**:

- Linguagem DashPT em português, compilada para comandos que o interpretador DashCode atual já reconhece.
- Validação de blocos e comandos.
- Construtor de rascunho de objetos Geometry Dash.
- UI Builder para montar a estrutura de painéis, textos, botões, imagens, alternadores e barras.
- Empacotamento em GDHB v1, com `manifest.json` e `src/main.dash` obrigatórios.

### Limite de integração atual

O pacote gerado é reconhecido pelo leitor GDHB e o script principal usa a linguagem suportada pelo runtime atual. Os arquivos `level/objects.json` e `ui/layout.json` são salvos como dados extras do projeto; o runtime atual não os desenha no Geometry Dash. Para isso, ainda é necessário implementar e testar o adaptador nativo Cocos2d-x para criar objetos e componentes de UI no processo do jogo. O Creator não afirma que esses recursos já funcionam dentro da gameplay.

## Cloudflare: publicar o Studio

1. Entre no painel Cloudflare.
2. Abra **Workers & Pages** → **Create application** → **Pages** → **Connect to Git**.
3. Autorize o GitHub e escolha `daviluccaff34-sys/GD-Hub-Mod-Loader`.
4. Configure **Root directory** como `studio`.
5. Escolha **None** em Framework preset.
6. Deixe **Build command** vazio e coloque `.` em **Build output directory**.
7. Clique em **Save and Deploy**.

Quando o deploy terminar, o endereço `*.pages.dev` exibido pelo Cloudflare será o endereço público. O criador ficará em `https://SEU-PROJETO.pages.dev/mod-creator.html`.

### O que precisa conectar (e o que não precisa)

O Mod Creator gera os pacotes no próprio navegador, então não precisa de API, token ou Worker para compilar. O Worker `cloudflare/mod-catalog-worker.js` é um serviço separado, usado para publicar mods no catálogo público e moderar envios. Não coloque o segredo `ADMIN_TOKEN` no HTML, no aplicativo Android ou no GitHub. Siga `cloudflare/README.md` para configurar o catálogo.


## Biblioteca de peças e construção
- `code-pieces.js` disponibiliza 3.420 fragmentos independentes DashPT, pesquisáveis e inseríveis no cursor do editor; não são blocos visuais Scratch.
- API pública no navegador: `GDHubPieces.registerPiece`, `getAll`, `search`, `insertPiece`.
- O Mod Creator exporta `level/build.json` com objetos, grupos, triggers e propriedades. O schema fica em `construction-api.json`.
- Esses dados ainda são rascunhos exportáveis; o runtime C++ não os converte em objetos reais no Geometry Dash.

## Catálogo e publicação
Abra `catalogo.html`. Informe a URL HTTPS do Worker Cloudflare implantado, carregue mods aprovados, publique um `.gdhb` e, se for administrador, revise envios pendentes. A URL é salva no armazenamento local do navegador. O token administrativo fica apenas no campo da sessão e não é persistido. O Worker precisa estar implantado com binding KV `MODS`; para moderação, configure o segredo `ADMIN_TOKEN`. O navegador deve permitir CORS para a origem do Pages.
