# GD Hub Mod Studio

Site estático para Cloudflare Pages. Não precisa de build command.

## Publicar no Cloudflare Pages sem conectar ao GitHub

A pasta `studio/` é um site estático. Para publicar diretamente, sem integração com GitHub:

1. No painel Cloudflare, abra **Workers & Pages** e crie uma aplicação **Pages**.
2. Escolha a opção de **Direct Upload / Upload assets** (não Connect to Git).
3. Envie o conteúdo da pasta `studio/` como os arquivos do site.
4. Confirme que `index.html` está na raiz do upload.
5. Finalize o deploy. O endereço `*.pages.dev` será exibido pelo Cloudflare.

A página inicial `index.html` agora é o próprio Mod Creator. O catálogo e a documentação ficam nos links do topo. Para atualizar, envie novamente os arquivos atualizados da pasta `studio/`.

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

A página inicial `index.html` já abre o **GD Hub Mod Creator**. `mod-creator.html` mantém a mesma ferramenta como rota direta:

- Linguagem DashPT em português, compilada para comandos que o interpretador DashCode atual já reconhece.
- Validação de blocos e comandos.
- Construtor de rascunho de objetos Geometry Dash.
- UI Builder para montar a estrutura de painéis, textos, botões, imagens, alternadores e barras.
- Empacotamento em GDHB v1, com `manifest.json` e `src/main.dash` obrigatórios.

### Limite de integração atual

O pacote gerado é reconhecido pelo leitor GDHB e o script principal usa a linguagem suportada pelo runtime atual. Os arquivos `level/objects.json` e `ui/layout.json` são salvos como dados extras do projeto; o runtime atual não os desenha no Geometry Dash. Para isso, ainda é necessário implementar e testar o adaptador nativo Cocos2d-x para criar objetos e componentes de UI no processo do jogo. O Creator não afirma que esses recursos já funcionam dentro da gameplay.

## Cloudflare: catálogo e Worker

O site Pages é publicado por upload direto e não precisa conectar o repositório GitHub. O Worker do catálogo é um serviço separado. Informe a URL HTTPS dele em `catalogo.html`. O Worker precisa ter o binding KV `MODS`; para aprovar envios, configure o segredo `ADMIN_TOKEN`.

### O que precisa conectar (e o que não precisa)

O Mod Creator gera os pacotes no próprio navegador, então não precisa de API, token ou Worker para compilar. O Worker `cloudflare/mod-catalog-worker.js` é um serviço separado, usado para publicar mods no catálogo público e moderar envios. Não coloque o segredo `ADMIN_TOKEN` no HTML, no aplicativo Android ou no GitHub. Siga `cloudflare/README.md` para configurar o catálogo.


## Biblioteca de peças e construção
- `code-pieces.js` disponibiliza 3.420 fragmentos independentes DashPT, pesquisáveis e inseríveis no cursor do editor; não são blocos visuais Scratch.
- API pública no navegador: `GDHubPieces.registerPiece`, `getAll`, `search`, `insertPiece`.
- O Mod Creator exporta `level/build.json` com objetos, grupos, triggers e propriedades. O schema fica em `construction-api.json`.
- Esses dados ainda são rascunhos exportáveis; o runtime C++ não os converte em objetos reais no Geometry Dash.

## Catálogo e publicação
Abra `catalogo.html`. Informe a URL HTTPS do Worker Cloudflare implantado, carregue mods aprovados, publique um `.gdhb` e, se for administrador, revise envios pendentes. A URL é salva no armazenamento local do navegador. O token administrativo fica apenas no campo da sessão e não é persistido. O Worker precisa estar implantado com binding KV `MODS`; para moderação, configure o segredo `ADMIN_TOKEN`. O navegador deve permitir CORS para a origem do Pages.

### Construction JavaScript API
`construction-api.js` exports `GDHubBuild` with createObject, setObjectProperty, createGroup, addToGroup, createTrigger, configureTrigger, remove, reset and getData. The editor uses this API to create the exported draft. It does not call native Geometry Dash functions.
