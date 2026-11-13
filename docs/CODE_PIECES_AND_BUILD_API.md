# Peças de código independentes e API de construção

## Biblioteca DashPT
`studio/code-pieces.js` gera e disponibiliza 3.420 fragmentos independentes: ações, palavras-chave, elementos, operadores, lógica, eventos, tipos de objetos, propriedades e valores. São peças de código parciais inseridas no cursor do editor, não blocos visuais Scratch nem 3.420 comandos fechados.

```js
GDHubPieces.search("portal", "Construção GD");
GDHubPieces.insertPiece("base.0", document.querySelector("#source"));
GDHubPieces.registerPiece({id:"ext.minha-acao",label:"minha ação",insert:"minha ação",category:"Extensão"});
```
IDs precisam ser únicos. Peças registradas por extensões permanecem na sessão do navegador.

## Construção
O Mod Creator exporta `level/build.json` no formato `gdhub-build-draft-1`: objetos (ID, tipo, posição, propriedades), grupos e triggers (tipo, posição, alvo e configuração). O contrato está em `studio/construction-api.json`. O editor permite inserir objetos/grupos/triggers, configurar propriedades de objetos por ID e remover elementos.

## Limite técnico
Esta entrega implementa a biblioteca, busca, inserção, registro de extensões, editor de construção e exportação JSON. Ela **não** implementa 3.420 funções nativas do jogo nem executa objetos/triggers dentro de Geometry Dash. O runtime C++ atual não lê `level/build.json`; o adaptador nativo específico para GD 2.2081 ainda precisa converter esses dados em objetos reais e usar APIs verificadas do editor/engine na thread correta. Até essa etapa e os testes no jogo, a construção é um rascunho exportável.
