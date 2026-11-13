# Teste do GD Hub no Android

## O que o app atual faz
O primeiro APK é um gerenciador complementar. Ele importa arquivos `.gdhb`, guarda cópias na pasta privada do app, lista/remover pacotes e executa o DashCode pelo runtime nativo.

Ele ainda **não abre uma interface dentro do Geometry Dash** nem modifica o jogo.

## Conseguir os arquivos de teste
1. Abra a aba **Actions** do repositório.
2. Entre na execução mais recente de **Build and test** que terminou com sucesso.
3. Baixe o artefato `GD-Hub-Android-APK`.
4. Baixe também o artefato `GD-Hub-Hello-Dash-Package` da etapa de testes nativos, se ele estiver disponível.

## Testar no celular
1. Instale o APK de debug no Android.
2. Abra o GD Hub.
3. Toque em **IMPORTAR .GDHB** e escolha o pacote de exemplo.
4. Toque em **VALIDAR E EXECUTAR**.
5. O resultado esperado do exemplo é uma linha semelhante a: `Olá, Cube! Bem-vindo ao GD Hub.`

Se o Android bloquear a instalação, use somente o APK gerado pelo próprio repositório e revise a opção de permitir instalação para o app que abriu o arquivo. Não é necessário root.

## Limitação atual
O teste valida o empacotador, o leitor, o interpretador e a ponte JNI no app separado. Ele não comprova integração com Geometry Dash 2.2081.
