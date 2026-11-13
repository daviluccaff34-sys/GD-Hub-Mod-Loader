# GD Hub Mod Studio

O Studio está publicado diretamente como Cloudflare Worker estático, sem integração do repositório GitHub.

- Studio: https://gdhub-studio.daviluccaff34.workers.dev/
- Catálogo/API: https://gdhub-mod-catalog.daviluccaff34.workers.dev/
- O Worker do Studio serve os arquivos da pasta `studio/` e a documentação pública selecionada.
- Para atualizar o site, gere novamente o Worker `gdhub-studio` com os arquivos atuais da pasta `studio/`.

O deploy foi criado pela API Cloudflare em 13/11/2026; o subdomínio workers.dev está habilitado. Consulte `cloudflare/DEPLOYMENT.md` para os detalhes e o segredo de moderação pendente.

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

O Studio e o catálogo estão publicados como Workers separados, sem conexão com o repositório GitHub. O catálogo já tem o binding KV `MODS` configurado. Para habilitar aprovação de envios, configure o segredo `ADMIN_TOKEN` seguindo `cloudflare/DEPLOYMENT.md`.

### O que precisa conectar (e o que não precisa)

O Mod Creator gera os pacotes no próprio navegador, então não precisa de API, token ou Worker para compilar. O Worker `cloudflare/mod-catalog-worker.js` é um serviço separado, usado para publicar mods no catálogo público e moderar envios. Não coloque o segredo `ADMIN_TOKEN` no HTML, no aplicativo Android ou no GitHub. Siga `cloudflare/README.md` para configurar o catálogo.


## Biblioteca de peças e construção
- `code-pieces.js` disponibiliza 3.420 fragmentos independentes DashPT, pesquisáveis e inseríveis no cursor do editor; não são blocos visuais Scratch.
- API pública no navegador: `GDHubPieces.registerPiece`, `getAll`, `search`, `insertPiece`.
- O Mod Creator exporta `level/build.json` com objetos, grupos, triggers e propriedades. O schema fica em `construction-api.json`.
- Esses dados ainda são rascunhos exportáveis; o runtime C++ não os converte em objetos reais no Geometry Dash.

## Catálogo e publicação
Abra `catalogo.html`. Informe a URL HTTPS do Worker Cloudflare implantado, carregue mods aprovados, publique um `.gdhb` e, se for administrador, revise envios pendentes. A URL é salva no armazenamento local do navegador. O token administrativo fica apenas no campo da sessão e não é persistido. O Worker precisa estar implantado com binding KV `MODS`; para moderação, configure o segredo `ADMIN_TOKEN`. O Worker do catálogo responde com CORS aberto para o Studio.

### Construction JavaScript API
`construction-api.js` exports `GDHubBuild` with createObject, setObjectProperty, createGroup, addToGroup, createTrigger, configureTrigger, remove, reset and getData. The editor uses this API to create the exported draft. It does not call native Geometry Dash functions.

## Exemplo inicial
O diretório `examples/hello_dashpt/` contém `main.dashpt` (fonte DashPT), `main.dash` (saída DashCode executável pelo interpretador atual) e manifesto GDHB. O exemplo executa mensagens/variáveis; não modifica a fase do jogo.
