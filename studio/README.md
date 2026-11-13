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
