# DashFlow — linguagem do GD Hub

**Nome provisório:** DashFlow  
**Extensão dos arquivos-fonte:** `.dash`  
**Pacote distribuído:** `.gdhb`  
**Modelo:** linguagem interpretada, com tipagem dinâmica e recursos opcionais de anotação de tipo.

DashFlow foi pensada para criar mods e ferramentas de Geometry Dash sem exigir que o criador compile o código. O empacotador coloca os arquivos-fonte e o manifesto dentro de um pacote `.gdhb`; o runtime do GD Hub interpreta o código.

## 1. Ideia da linguagem

A linguagem deve ser acessível para iniciantes, mas não ficar presa a comandos simples. Ela terá:
- variáveis e constantes;
- números, textos, booleanos, listas e mapas;
- condições, laços e funções;
- eventos do jogo;
- módulos e importações;
- tratamento de erros;
- operações matemáticas e utilitários;
- API do GD Hub para consultar e alterar recursos permitidos do jogo.

## 2. Exemplo rápido

```dash
mod "MeuPrimeiroMod" {
    id: "example.first-mod"
    version: "1.0.0"
    game: "2.2081"
}

let tentativas = 0
let nome = "DashFlow"

fn mensagem(jogador) {
    log("Olá, " + jogador + "! Bem-vindo ao " + nome)
}

event game.level_started(info) {
    tentativas += 1
    mensagem(info.player_name)
    ui.toast("Boa sorte! Tentativa " + tentativas)
}
```

## 3. Valores e variáveis

```dash
let pontos = 125
let velocidade = 1.25
let nome = "Player"
let ativo = true
let vazio = null

const LIMITE = 100
```

Tipos básicos: `number`, `string`, `boolean`, `null`, `list`, `map`.

Anotações de tipo são opcionais:

```dash
let fase: string = "Stereo Madness"
let recorde: number = 42
```

## 4. Operadores

- Matemática: `+`, `-`, `*`, `/`, `%`, `**`
- Comparação: `==`, `!=`, `<`, `<=`, `>`, `>=`
- Lógica: `and`, `or`, `not`
- Atribuição: `=`, `+=`, `-=`, `*=`, `/=`
- Texto: `+` para concatenar e interpolação com `${expressao}`.

## 5. Condições

```dash
if pontos >= 100 {
    log("Recorde!")
} else if pontos >= 50 {
    log("Quase lá")
} else {
    log("Continue tentando")
}
```

## 6. Laços

```dash
for item in [1, 2, 3, 4] {
    log(item)
}

let contador = 0
while contador < 5 {
    contador += 1
}

repeat 3 {
    log("Dash!")
}
```

Os laços terão limites de execução configuráveis pelo runtime para evitar travamentos.

## 7. Funções e retornos

```dash
fn dobro(valor: number) -> number {
    return valor * 2
}

let resultado = dobro(21)
```

Funções podem receber parâmetros opcionais e valores padrão:

```dash
fn anunciar(texto, duracao = 2) {
    ui.toast(texto, duracao)
}
```

## 8. Listas e mapas

```dash
let fases = ["Stereo Madness", "Back on Track"]
fases.push("Polargeist")
log(fases[0])

let jogador = {
    nome: "Player",
    tentativas: 12,
    pratica: false
}
log(jogador.nome)
```

## 9. Eventos do jogo

Os eventos são registrados pelo runtime. Os nomes abaixo são uma proposta de API, não eventos já implementados.

```dash
event player.death(info) {
    log("O jogador morreu na tentativa " + info.attempt)
}

event level.completed(info) {
    ui.toast("Fase concluída!")
}

event editor.object_selected(obj) {
    log("Objeto selecionado: " + obj.id)
}
```

## 10. API de Geometry Dash

A API será separada por módulos e permissões. Exemplos planejados:

```dash
let fase = game.level.current()
let jogador = game.player.get_info()

ui.toast("Fase: " + fase.name)

if editor.is_open() {
    let selecionados = editor.selection.get()
    log("Selecionados: " + selecionados.length)
}
```

Módulos planejados:
- `game`: fase atual, estado de jogo e informações permitidas;
- `player`: estado e informações do jogador;
- `editor`: seleção e operações de edição autorizadas;
- `ui`: notificações, janelas e componentes do mod;
- `input`: leitura de ações permitidas;
- `storage`: configurações e dados próprios do mod;
- `math`: funções matemáticas;
- `time`: relógio e temporizadores do runtime;
- `audio`: controles de áudio expostos pelo loader.

Cada chamada só funcionará quando a integração correspondente existir e a permissão estiver concedida.

## 11. Erros

```dash
try {
    let dados = storage.read("config")
    log(dados)
} catch erro {
    log("Não foi possível carregar: " + erro.message)
}
```

## 12. Módulos

```dash
import "utils/format"
from "utils/math" import clamp

let valor = clamp(120, 0, 100)
```

## 13. Concorrência assíncrona

Uma sintaxe planejada para tarefas que não devem bloquear a interface:

```dash
async fn carregar_config() {
    let config = await storage.read_async("config")
    return config
}
```

O runtime definirá quais operações podem ser assíncronas.

## 14. Segurança e limites

- Mods executam em ambiente controlado, não como código nativo.
- Sem acesso irrestrito ao sistema de arquivos, memória do processo ou rede.
- Cada pacote declara permissões no manifesto.
- O usuário poderá revisar permissões antes de ativar um mod.
- O runtime limita tempo de execução, memória e profundidade de chamadas.
- APIs que alterem o jogo só serão disponibilizadas por interfaces documentadas e compatíveis.

## 15. Estado de implementação

Este documento define o **rascunho da linguagem**. A sintaxe ainda precisa ser transformada em lexer, parser, representação interna e interpretador. Os exemplos são especificação proposta e ainda não executam no loader atual.
